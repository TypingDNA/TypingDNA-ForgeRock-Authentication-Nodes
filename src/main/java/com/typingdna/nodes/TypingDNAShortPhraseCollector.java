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


package com.typingdna.nodes;

import javax.inject.Inject;

import com.typingdna.core.ShortPhrase;
import com.typingdna.util.ConfigAdapter;
import com.typingdna.util.Constants;
import com.typingdna.util.Logger;
import com.typingdna.util.Messages;
import com.typingdna.util.State;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.*;

import com.google.inject.assistedinject.Assisted;

@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = TypingDNAShortPhraseCollector.Config.class)
public class TypingDNAShortPhraseCollector extends SingleOutcomeNode {

    private final Config config;

    /**
     * Configuration for the node.
     */
    public interface Config extends ConfigAdapter {
        @Override
        @Attribute(order = 100)
        default String textToEnter() {
            return Messages.TEXT_TO_ENTER;
        }
    }


    /**
     * Create the node using Guice injection. Just-in-time bindings can be used to obtain instances of other classes
     * from the plugin.
     *
     * @param config The service config.
     */
    @Inject
    public TypingDNAShortPhraseCollector(@Assisted Config config) {
        this.config = config;

        Logger.getInstance().setDebug(Constants.DEBUG);
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        State state = new State(context.sharedState.copy(),
                context.transientState.copy(),
                context.getAllCallbacks());

        try {
            Logger.getInstance().debug("In TypingDNAShortPhraseCollector");

            ShortPhrase useCase = new ShortPhrase(config, state);

            if (useCase.isFormDisplayed()) {
                return useCase.handleForm().build();
            } else {
                return useCase.displayForm().build();
            }
        } catch (Exception e) {
            Logger.getInstance().error(String.format("TypingDNAShortPhraseCollector unexpected error %s", e.getMessage()));
            throw new NodeProcessException(e);
        }
    }
}
