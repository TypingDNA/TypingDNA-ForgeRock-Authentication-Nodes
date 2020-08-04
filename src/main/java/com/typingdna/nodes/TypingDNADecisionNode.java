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

import com.typingdna.core.AbstractCore;
import com.typingdna.core.Decision;
import com.typingdna.nodes.outcomeproviders.TypingDNADecisionOutcomeProvider;
import com.typingdna.util.ConfigAdapter;
import com.typingdna.util.Constants;
import com.typingdna.util.HelperFunctions;
import com.typingdna.util.TypingDNAApi;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.*;

import com.google.inject.assistedinject.Assisted;
import org.forgerock.openam.sm.annotations.adapters.Password;

import java.util.UUID;

@Node.Metadata(outcomeProvider = TypingDNADecisionOutcomeProvider.class,
        configClass = TypingDNADecisionNode.Config.class)
public class TypingDNADecisionNode extends AbstractDecisionNode {

    private final AbstractCore useCase;

    /**
     * Configuration for the node.
     */
    public interface Config extends ConfigAdapter {
        @Override
        @Attribute(order = 100)
        default String apiUrl() {
            return "https://api.typingdna.com";
        }

        @Override
        @Attribute(order = 200)
        String apiKey();

        @Override
        @Attribute(order = 300)
        @Password
        char[] apiSecret();

        @Override
        @Attribute(order = 400)
        default int retries() {
            return 0;
        }

        @Override
        @Attribute(order = 500)
        default int enrollmentsNecessary() {
            return 3;
        }

        @Override
        @Attribute(order = 600)
        default boolean verifyAfterEnroll() {
            return false;
        }

        @Override
        @Attribute(order = 700)
        default int matchScore() {
            return 70;
        }

        @Override
        @Attribute(order = 800)
        default int autoEnrollScore() {
            return 90;
        }

        @Override
        @Attribute(order = 900)
        default String usernameSalt() {
            return "";
        }

        @Override
        @Attribute(order = 1000)
        default String requestIdentifier() {
            return "ForgeRock";
        }

        @Override
        @Attribute(order = 1100)
        default int requestTimeout() {
            return 8000;
        }
    }


    /**
     * Create the node using Guice injection. Just-in-time bindings can be used to obtain instances of other classes
     * from the plugin.
     *
     * @param config The service config.
     */
    @Inject
    public TypingDNADecisionNode(@Assisted Config config, @Assisted UUID nodeId) throws NodeProcessException {
        String apiUrl = HelperFunctions.trimUrl(config.apiUrl());
        TypingDNAApi api = new TypingDNAApi(apiUrl, config.apiKey(), new String(config.apiSecret()), config.requestTimeout());
        this.useCase = new Decision(config, api);
        this.useCase.setNodeId(nodeId.toString());
        this.useCase.setDebug(Constants.DEBUG);
    }

    @Override
    public Action process(TreeContext context) {
        useCase.setCurrentState(
                context.sharedState.copy(),
                context.transientState.copy(),
                context.getAllCallbacks()
        );

        return useCase.handleForm().build();
    }
}
