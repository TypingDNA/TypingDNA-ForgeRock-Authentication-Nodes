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

import com.typingdna.api.DeveloperAPI;
import com.typingdna.api.ProEnterpriseAPI;
import com.typingdna.api.TypingDNAAPI;
import com.typingdna.core.Decision;
import com.typingdna.core.statechanges.ExitNodeStateChange;
import com.typingdna.nodes.outcomeproviders.TypingDNADecisionOutcomeProvider;
import com.typingdna.nodes.outcomeproviders.TypingDNADecisionOutcomeProvider.TypingDNADecisionOutcome;
import com.typingdna.util.ConfigAdapter;
import com.typingdna.util.Constants;
import com.typingdna.util.HashAlgorithm;
import com.typingdna.util.HelperFunctions;
import com.typingdna.util.Logger;
import com.typingdna.util.State;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;

import com.google.inject.assistedinject.Assisted;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.sm.annotations.adapters.Password;

import java.util.UUID;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

@Node.Metadata(outcomeProvider = TypingDNADecisionOutcomeProvider.class,
        configClass = TypingDNADecisionNode.Config.class)
public class TypingDNADecisionNode extends AbstractDecisionNode {

    private final Config config;
    private final UUID nodeId;
    private String actionPerformed = "";
    private boolean isAutoEnroll = false;

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
        @Attribute(order = 310)
        default Configuration authAPIConfiguration() {
            return Configuration.Basic;
        }

        @Override
        @Attribute(order = 400)
        default int retries() {
            return 0;
        }

        @Override
        @Attribute(order = 410)
        default HashAlgorithm hashAlgorithm() {
            return HashAlgorithm.MD5;
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
        this.nodeId = nodeId;
        this.config = config;

        Logger.getInstance().setDebug(Constants.DEBUG);
    }

    @Override
    public Action process(TreeContext context) {
        State state = new State(context.sharedState.copy(),
                context.transientState.copy(),
                context.getAllCallbacks());

        try {
            Logger.getInstance().debug("In TypingDNADecisionNode");

            TypingDNAAPI api;
            String apiUrl = HelperFunctions.trimUrl(config.apiUrl());
            if (config.authAPIConfiguration() == ConfigAdapter.Configuration.Basic) {
                api = new DeveloperAPI(apiUrl, config.apiKey(), new String(config.apiSecret()), config.requestTimeout());
            } else {
                api = new ProEnterpriseAPI(apiUrl, config.apiKey(), new String(config.apiSecret()), config.requestTimeout());
            }

            Decision useCase = new Decision(config, state, api);
            useCase.setNodeId(nodeId.toString());

            Action action = useCase.handleForm().build();
            actionPerformed = useCase.getAction();
            isAutoEnroll = useCase.isAutoEnroll();

            api.close();

            return action;
        } catch (Exception e) {
            Logger.getInstance().error(String.format("TypingDNADecisionNode unexpected error %s", e.getMessage()));

            state.setMessage("TypingDNA unknown error. Please try again.");
            return new ExitNodeStateChange(TypingDNADecisionOutcome.FAIL.name())
                    .setSharedState(state.getSharedState())
                    .setTransientState(state.getTransientState())
                    .build();
        }
    }

    @Override
    public JsonValue getAuditEntryDetail() {
        if (actionPerformed.equalsIgnoreCase("ENROLL")) {
            return json(object(field("action", actionPerformed)));
        }

        return json(object(field("action", actionPerformed), field("autoenroll", String.valueOf(isAutoEnroll))));
    }
}
