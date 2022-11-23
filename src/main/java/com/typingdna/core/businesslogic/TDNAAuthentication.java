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


package com.typingdna.core.businesslogic;

import com.typingdna.api.TypingDNAAPI;
import com.typingdna.api.model.APIResponse;
import com.typingdna.api.model.CheckUserResponse;
import com.typingdna.api.model.PatternType;
import com.typingdna.api.model.VerifyResponse;
import com.typingdna.core.ActionType;
import com.typingdna.core.statechanges.ExitNodeStateChange;
import com.typingdna.core.statechanges.StateChange;
import com.typingdna.nodes.outcomeproviders.TypingDNADecisionOutcomeProvider.TypingDNADecisionOutcome;
import com.typingdna.util.ConfigAdapter;
import com.typingdna.util.Constants;
import com.typingdna.util.Logger;
import com.typingdna.util.Messages;
import com.typingdna.util.State;
import org.forgerock.util.Strings;

import java.util.Optional;

public class TDNAAuthentication {
    private static final Logger logger = Logger.getInstance();

    private final ConfigAdapter config;
    private final State state;
    private final TypingDNAAPI api;

    public TDNAAuthentication(ConfigAdapter config, State state, TypingDNAAPI api) {
        this.config = config;
        this.state = state;
        this.api = api;
    }

    public StateChange performAuthentication(AuthenticationData authData) {
        logger.debug(String.format("In TypingDNADecisionNode: preparing to authenticate username=%s previousAction=%s", authData.getUsername(), authData.getPreviousAction()));

        if (isTypingPatternInvalid(authData.getTypingPattern())) {
            logger.debug(String.format("In TypingDNADecisionNode: pattern received is invalid username=%s tp=%s", authData.getUsername(), authData.getTypingPattern()));

            state.setPreviousAction(ActionType.VERIFY);
            state.setMessage(Messages.TOO_MANY_TYPOS);

            logger.info(String.format("username %s, action VERIFY, outcome FAIL", state.getUsername()));

            return new ExitNodeStateChange(TypingDNADecisionOutcome.RETRY.name())
                    .setSharedState(state.getSharedState())
                    .setTransientState(state.getTransientState())
                    .setAction("VERIFY");
        }

        PatternType patternType = getPatternType(authData.getTypingPattern());
        if (patternType == PatternType.INVALID) {
            logger.debug(String.format("In TypingDNADecisionNode: invalid pattern type username=%s tp=%s", authData.getUsername(), authData.getTypingPattern()));

            state.setPreviousAction(ActionType.VERIFY);
            state.setMessage(Messages.TOO_MANY_TYPOS);

            logger.info(String.format("username %s, action VERIFY, outcome FAIL", state.getUsername()));

            return new ExitNodeStateChange(TypingDNADecisionOutcome.RETRY.name())
                    .setSharedState(state.getSharedState())
                    .setTransientState(state.getTransientState())
                    .setAction("VERIFY");
        }

        StateChange stateChange;
        if (authData.getPreviousAction() == ActionType.VERIFY) {
            CheckUserResponse response = api.checkUser(authData.getUsername(), patternType, authData.getTextId(), authData.getDeviceType(), authData.getRequestIdentifier());
            setPatternsEnrolled(response.getPatternCount());
        }
        stateChange = handleVerify(authData);

        setMessage(stateChange, authData);
        logger.debug(String.format("In TypingDNADecisionNode: username %s new outcome %s", authData.getUsername(), stateChange.getOutcome()));

        return stateChange;
    }

    private StateChange handleVerify(AuthenticationData authData) {
        StateChange stateChange = null;

        String patternsToVerify = getPatternsToVerify(authData);
        VerifyResponse verifyResponse = api.verify(authData.getUsername(), patternsToVerify, authData.getRequestIdentifier());

        if (verifyResponse.isError() && !verifyResponse.isTemporary()) {
            String action;
            if (state.getPreviousAction() == ActionType.ENROLL || state.getPreviousAction() == ActionType.ENROLL_POSITION) {
                action = "ENROLL";
            } else {
                action = "VERIFY";
            }
            logger.info(String.format("username %s, action %s, outcome FAIL", state.getUsername(), action));

            stateChange = new ExitNodeStateChange(TypingDNADecisionOutcome.FAIL.name())
                    .setSharedState(state.getSharedState())
                    .setTransientState(state.getTransientState())
                    .setApiResponse(verifyResponse)
                    .setAction("VERIFY");
        } else if (verifyResponse.isError()) {
            if (state.getRetries() < config.retries()) {
                state.incrementRetries();

                ActionType previousAction = state.getPreviousAction();
                if (previousAction == ActionType.ENROLL || previousAction == ActionType.ENROLL_POSITION) {
                    logger.info(String.format("username %s, action ENROLL, outcome FAIL", state.getUsername()));
                    stateChange = new ExitNodeStateChange(TypingDNADecisionOutcome.ENROLL.name())
                            .setSharedState(state.getSharedState())
                            .setTransientState(state.getTransientState())
                            .setApiResponse(verifyResponse)
                            .setAction("ENROLL");
                } else {
                    logger.info(String.format("username %s, action VERIFY, outcome FAIL, autoenroll FALSE", state.getUsername()));
                    state.setPreviousAction(ActionType.RETRY);
                    stateChange = new ExitNodeStateChange(TypingDNADecisionOutcome.RETRY.name())
                            .setSharedState(state.getSharedState())
                            .setTransientState(state.getTransientState())
                            .setApiResponse(verifyResponse)
                            .setAction("VERIFY");
                }
            } else {
                logger.info(String.format("username %s, action VERIFY, outcome FAIL, autoenroll FALSE", state.getUsername()));
                stateChange = new ExitNodeStateChange(TypingDNADecisionOutcome.FAIL.name())
                        .setSharedState(state.getSharedState())
                        .setTransientState(state.getTransientState())
                        .setApiResponse(verifyResponse)
                        .setAction("VERIFY");
            }
        } else if (verifyResponse.isNeedsEnroll() || verifyResponse.isNeedsEnrollPosition()) {
            if (verifyResponse.isPatternEnrolled()) {
                logger.info(String.format("username %s, action ENROLL, outcome SUCCESS", state.getUsername()));
                incrementPatternsEnrolled();
            }

            state.setPreviousAction(verifyResponse.isNeedsEnrollPosition() ? ActionType.ENROLL_POSITION : ActionType.ENROLL);
            stateChange = new ExitNodeStateChange(TypingDNADecisionOutcome.ENROLL.name())
                    .setSharedState(state.getSharedState())
                    .setTransientState(state.getTransientState())
                    .setAction("ENROLL");
        } else if (!verifyResponse.isMatch() && !(verifyResponse.isNeedsEnroll() || verifyResponse.isNeedsEnrollPosition()) && verifyResponse.isPatternEnrolled()) {
            logger.info(String.format("username %s, action ENROLL, outcome ENROLL_COMPLETE", state.getUsername()));

            incrementPatternsEnrolled();

            state.setPreviousAction(ActionType.VERIFY);
            stateChange = new ExitNodeStateChange(TypingDNADecisionOutcome.INITIAL_ENROLLMENT_COMPLETE.name())
                    .setSharedState(state.getSharedState())
                    .setTransientState(state.getTransientState())
                    .setAction("ENROLL");
        } else if (verifyResponse.isMatch()) {
            logger.info(String.format("username %s, action VERIFY, outcome MATCH, autoenroll %s", state.getUsername(), verifyResponse.isPatternEnrolled() ? "TRUE" : "FALSE"));
            if (verifyResponse.isPatternEnrolled()) {
                incrementPatternsEnrolled();
            }

            stateChange = new ExitNodeStateChange(TypingDNADecisionOutcome.MATCH.name())
                    .setSharedState(state.getSharedState())
                    .setTransientState(state.getTransientState())
                    .setAction("VERIFY")
                    .setAutoEnroll(verifyResponse.isPatternEnrolled());
        } else {
            logger.info(String.format("username %s, action VERIFY, outcome NO_MATCH, autoenroll FALSE", state.getUsername()));
            stateChange = handleNoMatch(authData);
        }

        return stateChange;
    }

    private StateChange handleNoMatch(AuthenticationData authData) {
        StateChange stateChange;

        if (state.getRetries() < config.retries()) {
            logger.debug(String.format("In TypingDNADecisionNode: verification failed but enough retries left username=%s", authData.getUsername()));

            state.setPreviousTypingPatterns(authData.getTypingPattern());
            state.incrementRetries();
            state.setPreviousAction(ActionType.RETRY);
            stateChange = new ExitNodeStateChange(TypingDNADecisionOutcome.RETRY.name())
                    .setSharedState(state.getSharedState())
                    .setTransientState(state.getTransientState())
                    .setAction("VERIFY");
        } else {
            logger.debug(String.format("In TypingDNADecisionNode: verification failed, no retries left username=%s", authData.getUsername()));

            stateChange = new ExitNodeStateChange(TypingDNADecisionOutcome.NO_MATCH.name())
                    .setSharedState(state.getSharedState())
                    .setTransientState(state.getTransientState())
                    .setAction("VERIFY");
        }

        return stateChange;
    }

    private boolean isTypingPatternInvalid(String typingPattern) {
        return Strings.isNullOrEmpty(typingPattern) || typingPattern.equalsIgnoreCase(Constants.PATTERN_OUTPUT_VARIABLE);
    }

    private PatternType getPatternType(String typingPattern) {
        final String[] patternProps = typingPattern.split(",", 5);
        if (patternProps.length < 5) {
            return PatternType.INVALID;
        } else {
            return patternProps[3].equals("0") ? PatternType.SAME_TEXT : PatternType.SAME_TEXT_EXTENDED;
        }
    }

    private void setPatternsEnrolled(int patternsEnrolled) {
        logger.debug(String.format("In TypingDNADecisionNode: Set patterns enrolled in shared state patternsEnrolled=%d ", patternsEnrolled));
        state.setPatternsEnrolled(patternsEnrolled);
    }

    private void incrementPatternsEnrolled() {
        int patternsEnrolled = state.getPatternsEnrolled() + 1;
        logger.debug(String.format("In TypingDNADecisionNode: Incrementing patterns enrolled in shared state patternsEnrolled=%d ", patternsEnrolled));
        state.setPatternsEnrolled(patternsEnrolled);
    }

    private String getPatternsToVerify(AuthenticationData authData) {
        String patternsToVerify;

        if (authData.getPreviousAction() == ActionType.RETRY) {
            logger.debug(String.format("In TypingDNADecisionNode: Adding failed typing pattern to the new typing pattern username=%s", authData.getUsername()));

            String previousTypingPatterns = state.getPreviousTypingPatterns();
            patternsToVerify = Strings.isNullOrEmpty(previousTypingPatterns) ?
                    authData.getTypingPattern() : String.format("%s;%s", authData.getTypingPattern(), previousTypingPatterns);
        } else {
            patternsToVerify = authData.getTypingPattern();
        }

        return patternsToVerify;
    }

    private void setMessage(StateChange stateChange, AuthenticationData authData) {
        String message = null;

        ExitNodeStateChange exitNodeStateChange = (ExitNodeStateChange) stateChange;
        String errorMessage = getErrorMessage(exitNodeStateChange.apiResponse);

        if (errorMessage != null) {
            message = errorMessage;
        } else {
            Integer actionId = exitNodeStateChange.sharedState != null ? exitNodeStateChange.sharedState.get(Constants.PREVIOUS_ACTION).asInteger() : null;
            if (actionId == null) {
                return;
            }

            ActionType action = ActionType.toActionType(actionId);

            if (action == ActionType.RETRY) {
                message = Messages.AUTH_FAILED;
            } else if (action == ActionType.ENROLL) {
                message = String.format(Messages.NOT_ENOUGH_PATTERNS, state.getPatternsEnrolled());
            } else if (action == ActionType.ENROLL_POSITION) {
                message = String.format(Messages.NOT_ENOUGH_PATTERNS_POSITION, state.getPatternsEnrolled());
            } else if ((stateChange.getOutcome().equalsIgnoreCase(TypingDNADecisionOutcome.INITIAL_ENROLLMENT_COMPLETE.toString()) || (authData.getPreviousAction() == ActionType.ENROLL || authData.getPreviousAction() == ActionType.ENROLL_POSITION) && action == ActionType.VERIFY)) {
                message = Messages.ENROLL_DONE;
            }
        }

        exitNodeStateChange.sharedState.put(Constants.MESSAGE, message);
    }

    private String getErrorMessage(Optional<APIResponse> apiResponseOptional) {
        if (apiResponseOptional.isEmpty()) {
            return null;
        }

        APIResponse apiResponse = apiResponseOptional.get();
        String message = null;

        if (apiResponse.isError()) {
            if (apiResponse.isTemporary()) {
                message = Messages.temporaryError(apiResponse.getCode());
            } else {
                message = Messages.permanentError(apiResponse.getCode());
            }
        }

        return message;
    }
}
