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

import com.google.inject.assistedinject.Assisted;
import com.typingdna.api.DeveloperAPI;
import com.typingdna.api.TypingDNAAPI;
import com.typingdna.core.AbstractCore;
import com.typingdna.core.ResetProfile;
import com.typingdna.core.statechanges.ExitNodeStateChange;
import com.typingdna.nodes.outcomeproviders.TypingDNAResetProfileOutcomeProvider;
import com.typingdna.util.ConfigAdapter;
import com.typingdna.util.Constants;
import com.typingdna.util.HashAlgorithm;
import com.typingdna.util.HelperFunctions;
import com.typingdna.util.Logger;
import com.typingdna.util.State;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.sm.annotations.adapters.Password;

import javax.inject.Inject;
import java.util.UUID;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

@Node.Metadata(outcomeProvider = TypingDNAResetProfileOutcomeProvider.class,
        configClass = TypingDNAResetProfile.Config.class)
public class TypingDNAResetProfile extends AbstractDecisionNode {

    private final AbstractCore useCase;

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
        default HashAlgorithm hashAlgorithm() {
            return HashAlgorithm.MD5;
        }

        @Attribute(order = 400)
        default String usernameSalt() {
            return "";
        }

        @Override
        @Attribute(order = 500)
        default String requestIdentifier() {
            return "ForgeRock";
        }

        @Override
        @Attribute(order = 600)
        default int requestTimeout() {
            return 8000;
        }
    }

    @Inject
    public TypingDNAResetProfile(@Assisted TypingDNAResetProfile.Config config, @Assisted UUID nodeId) throws NodeProcessException {
        String apiUrl = HelperFunctions.trimUrl(config.apiUrl());
        TypingDNAAPI api = new DeveloperAPI(apiUrl, config.apiKey(), new String(config.apiSecret()), config.requestTimeout());

        this.useCase = new ResetProfile(config, api);
        this.useCase.setNodeId(nodeId.toString());
        Logger.getInstance().setDebug(Constants.DEBUG);
    }

    @Override
    public Action process(TreeContext context) {
        try {
            Logger.getInstance().debug("In TypingDNAResetProfile");

            State.getInstance().setState(
                    context.sharedState.copy(),
                    context.transientState.copy(),
                    context.getAllCallbacks()
            );

            return useCase.handleForm().build();
        } catch (Exception e) {
            Logger.getInstance().error(String.format("TypingDNAResetProfile unexpected error %s", e.getMessage()));

            State.getInstance().setMessage("TypingDNA unknown error. Please try again.");
            return new ExitNodeStateChange(TypingDNAResetProfileOutcomeProvider.TypingDNAResetProfileOutcome.ERROR.name())
                    .setSharedState(State.getInstance().getSharedState())
                    .setTransientState(State.getInstance().getTransientState())
                    .build();
        }
    }

    @Override
    public JsonValue getAuditEntryDetail() {
        return json(object(field("action", useCase.getAction())));
    }
}
