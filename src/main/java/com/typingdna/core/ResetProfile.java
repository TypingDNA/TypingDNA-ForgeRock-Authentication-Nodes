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

import com.typingdna.api.model.DeleteUserResponse;
import com.typingdna.core.statechanges.ExitNodeStateChange;
import com.typingdna.core.statechanges.StateChange;
import com.typingdna.nodes.outcomeproviders.TypingDNAResetProfileOutcomeProvider;
import com.typingdna.util.ConfigAdapter;
import com.typingdna.api.TypingDNAAPI;
import com.typingdna.util.HelperFunctions;
import com.typingdna.util.Logger;
import com.typingdna.util.Messages;

public class ResetProfile extends AbstractCore {

    public ResetProfile(ConfigAdapter config, TypingDNAAPI api) {
        super(config, api);
    }

    @Override
    public StateChange handleForm() {
        logger.debug(String.format("In TypingDNAResetProfile: resetting username=%s", state.getUsername()));

        DeleteUserResponse response = api.deleteUser(HelperFunctions.hashText(state.getUsername(), config.usernameSalt(), config.hashAlgorithm()), getRequestIdentifier());
        if (response.isError()) {
            this.setAction("RESET_PROFILE");
            Logger.getInstance().info(String.format("username %s, action RESET_PROFILE, outcome FAIL", state.getUsername()));

            state.setMessage(Messages.RESET_FAIL);
            return new ExitNodeStateChange(TypingDNAResetProfileOutcomeProvider.TypingDNAResetProfileOutcome.ERROR.name()).setSharedState(state.getSharedState()).setTransientState(state.getTransientState());
        } else {
            this.setAction("RESET_PROFILE");
            Logger.getInstance().info(String.format("username %s, action RESET_PROFILE, outcome SUCCESS", state.getUsername()));

            state.setMessage(Messages.RESET_SUCCESS);
            return new ExitNodeStateChange(TypingDNAResetProfileOutcomeProvider.TypingDNAResetProfileOutcome.SUCCESS.name()).setSharedState(state.getSharedState()).setTransientState(state.getTransientState());
        }
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
