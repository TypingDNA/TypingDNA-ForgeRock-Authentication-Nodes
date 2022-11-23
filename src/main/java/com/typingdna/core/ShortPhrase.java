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

import com.google.common.collect.ImmutableList;
import com.typingdna.core.statechanges.DisplayFormStateChange;
import com.typingdna.core.statechanges.SingleOutcomeStateChange;
import com.typingdna.core.statechanges.StateChange;
import com.typingdna.util.ConfigAdapter;
import com.typingdna.util.HelperFunctions;
import com.typingdna.util.Messages;
import org.forgerock.util.Strings;

import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.TextOutputCallback;

public class ShortPhrase extends AbstractCore {

    public ShortPhrase(ConfigAdapter config) {
        super(config);
    }

    @Override
    public boolean isFormDisplayed() {
        boolean isFormDisplayed = state.getCallbacks(NameCallback.class)
                .map(NameCallback::getName)
                .anyMatch(result -> !Strings.isNullOrEmpty(result));
        logger.debug(String.format("In TypingDNAShortPhraseCollector: short phrase input %s inserted", isFormDisplayed ? "is" : "is not"));
        return isFormDisplayed;
    }

    @Override
    public StateChange displayForm() {
        logger.debug("In TypingDNAShortPhraseCollector: inserting short phrase input");

        TextOutputCallback prompt = new TextOutputCallback(TextOutputCallback.INFORMATION, Messages.SHORT_PHRASE_PROMPT);
        TextOutputCallback textToEnter = new TextOutputCallback(TextOutputCallback.INFORMATION, config.textToEnter());
        NameCallback input = new NameCallback(Messages.SHORT_PHRASE_PLACEHOLDER);

        state.setTextToEnter(config.textToEnter());
        state.setTextId(HelperFunctions.fnv1a32(config.textToEnter()));

        return new DisplayFormStateChange(ImmutableList.of(prompt, textToEnter, input)).setSharedState(state.getSharedState()).setTransientState(state.getTransientState());
    }

    @Override
    public StateChange displayForm(ActionType actionType) {
        throw new NoSuchMethodError("displayForm(ActionType actionType) is not implemented");
    }

    @Override
    public StateChange handleForm() {
        return new SingleOutcomeStateChange().setSharedState(state.getSharedState()).setTransientState(state.getTransientState());
    }
}
