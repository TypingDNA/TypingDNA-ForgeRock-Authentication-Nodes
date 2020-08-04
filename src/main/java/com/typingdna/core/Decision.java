/*
  Copyright 2020 TypingDNA Inc. (https://www.typingdna.com)

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/

package com.typingdna.core;

import com.typingdna.nodes.outcomeproviders.TypingDNADecisionOutcomeProvider.TypingDNAOutcome;
import com.typingdna.core.statechanges.ExitNodeStateChange;
import com.typingdna.core.statechanges.StateChange;
import com.typingdna.util.ConfigAdapter;
import com.typingdna.util.Constants;
import com.typingdna.util.Messages;
import com.typingdna.util.TypingDNAApi;
import org.forgerock.util.Strings;

public class Decision extends AbstractCore {

    private String username;
    private String typingPattern;
    private ActionType previousAction;

    public Decision(ConfigAdapter config, TypingDNAApi api) {
        super(config, api);
    }

    @Override
    public StateChange handleForm() {
        username = getUsername();
        typingPattern = getTypingPattern();
        previousAction = getPreviousAction();

        debug(String.format("DecisionNode handelForm() username=%s previousAction=%s", username, previousAction));

        if (isTypingPatternInvalid()) {
            sharedState.put(Constants.PREVIOUS_ACTION, ActionType.VERIFY.getAction())
                    .put(Constants.MESSAGE, Messages.TOO_MANY_TYPOS);
            return new ExitNodeStateChange(TypingDNAOutcome.RETRY.name()).setSharedState(sharedState);
        }

        StateChange stateChange;
        if (previousAction == ActionType.VERIFY || previousAction == ActionType.RETRY) {
            int patternCount = doCheckUser(username, TypingDNAApi.PatternType.SAME_TEXT, getTextId());

            if (patternCount < config.enrollmentsNecessary()) {
                setPatternsEnrolled(patternCount);
                stateChange = handleEnroll();
            } else {
                stateChange = handleVerify();
            }
        } else {
            stateChange = handleEnroll();
        }

        setMessage(stateChange);
        debug(String.format("Username %s new outcome %s", username, stateChange.getOutcome()));

        return stateChange;
    }

    private boolean isTypingPatternInvalid() {
        return Strings.isNullOrEmpty(typingPattern) || typingPattern.equalsIgnoreCase(Constants.PATTERN_OUTPUT_VARIABLE);
    }

    private void setMessage(StateChange stateChange) {
        String message = null;

        ExitNodeStateChange exitNodeStateChange = (ExitNodeStateChange) stateChange;
        Integer actionId = exitNodeStateChange.sharedState.get(Constants.PREVIOUS_ACTION).asInteger();
        if (actionId == null) {
            return;
        }

        ActionType action = ActionType.toActionType(actionId);

        if (action == ActionType.RETRY) {
            message = Messages.AUTH_FAILED;
        } else if (action == ActionType.ENROLL) {
            message = String.format(Messages.NOT_ENOUGH_PATTERNS, getEnrollmentsLeft());
        } else if (action == ActionType.ENROLL_POSITION) {
            message = String.format(Messages.NOT_ENOUGH_PATTERNS_POSITION, getEnrollmentsLeft());
        } else if ((previousAction == ActionType.ENROLL || previousAction == ActionType.ENROLL_POSITION) && action == ActionType.VERIFY) {
            message = Messages.ENROLL_DONE;
        }

        exitNodeStateChange.sharedState.put(Constants.MESSAGE, message);
    }

    private StateChange handleEnroll() {
        doSave(username, typingPattern);

        TypingDNAOutcome outcome;
        if (getPatternsEnrolled() < config.enrollmentsNecessary()) {
            int previousAction = this.previousAction == ActionType.ENROLL || this.previousAction == ActionType.ENROLL_POSITION ? this.previousAction.getAction() :
                    ActionType.ENROLL.getAction();
            sharedState.put(Constants.PREVIOUS_ACTION, previousAction);
            outcome = TypingDNAOutcome.ENROLL;
        } else if (config.verifyAfterEnroll()) {
            previousAction = ActionType.ENROLL;
            sharedState.put(Constants.PREVIOUS_ACTION, ActionType.VERIFY.getAction());
            outcome = TypingDNAOutcome.RETRY;
        } else {
            outcome = TypingDNAOutcome.MATCH;
        }

        return new ExitNodeStateChange(outcome.name()).setSharedState(sharedState);
    }

    private StateChange handleVerify() {
        StateChange stateChange = null;

        String patternsToVerify = getPatternsToVerify();
        VerifyResponse verifyResponse = doVerify(username, patternsToVerify);

        if (verifyResponse == VerifyResponse.MATCH) {
            stateChange = new ExitNodeStateChange(TypingDNAOutcome.MATCH.name()).setSharedState(sharedState);
        } else if (verifyResponse == VerifyResponse.NO_MATCH) {
            stateChange = handleNoMatch();
        } else if (verifyResponse == VerifyResponse.NEEDS_ENROLL || verifyResponse == VerifyResponse.NEEDS_ENROLL_POSITION) {
            doSave(username, typingPattern);
            sharedState.put(Constants.PREVIOUS_ACTION, verifyResponse == VerifyResponse.NEEDS_ENROLL ?
                    ActionType.ENROLL.getAction() : ActionType.ENROLL_POSITION.getAction());
            stateChange = new ExitNodeStateChange(TypingDNAOutcome.ENROLL.name()).setSharedState(sharedState);
        } else if (verifyResponse == VerifyResponse.INVALID_CREDENTIALS) {
            stateChange = new ExitNodeStateChange(TypingDNAOutcome.FAIL.name()).setSharedState(sharedState);
        } else if (verifyResponse == VerifyResponse.REQUEST_ERROR) {
            int retries = getRetries();
            if (retries < config.retries()) {
                sharedState.put(Constants.VERIFY_RETRIES, retries + 1)
                        .put(Constants.PREVIOUS_ACTION, ActionType.RETRY.getAction());
                stateChange = new ExitNodeStateChange(TypingDNAOutcome.RETRY.name()).setSharedState(sharedState);
            } else {
                stateChange = new ExitNodeStateChange(TypingDNAOutcome.FAIL.name()).setSharedState(sharedState);
            }
        }

        return stateChange;
    }

    private StateChange handleNoMatch() {
        StateChange stateChange;

        int retries = getRetries();
        if (retries < config.retries()) {
            addPatternToPreviousPatterns(typingPattern);
            sharedState.put(Constants.VERIFY_RETRIES, retries + 1)
                    .put(Constants.PREVIOUS_ACTION, ActionType.RETRY.getAction());
            stateChange = new ExitNodeStateChange(TypingDNAOutcome.RETRY.name()).setSharedState(sharedState);
        } else {
            stateChange = new ExitNodeStateChange(TypingDNAOutcome.NO_MATCH.name()).setSharedState(sharedState);
        }

        return stateChange;
    }

    private String getPatternsToVerify() {
        String patternsToVerify;

        if (previousAction == ActionType.RETRY) {
            String previousTypingPatterns = sharedState.get(Constants.PREVIOUS_TYPING_PATTERNS).asString();
            patternsToVerify = Strings.isNullOrEmpty(previousTypingPatterns) ?
                    typingPattern : String.format("%s;%s", typingPattern, previousTypingPatterns);
        } else {
            patternsToVerify = typingPattern;
        }

        return patternsToVerify;
    }

    @Override
    public boolean isFormDisplayed() {
        throw new NoSuchMethodError("isFormDisplayed() is not implemented");
    }

    @Override
    public StateChange displayForm() {
        throw new NoSuchMethodError("displayForm() is not implemented");
    }

    @Override
    public StateChange displayForm(ActionType actionType) {
        throw new NoSuchMethodError("displayForm(ActionType actionType) is not implemented");
    }
}
