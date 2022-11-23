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

import com.typingdna.core.businesslogic.AuthenticationData;
import com.typingdna.core.businesslogic.TDNAAuthentication;
import com.typingdna.core.statechanges.ExitNodeStateChange;
import com.typingdna.core.statechanges.StateChange;
import com.typingdna.util.ConfigAdapter;
import com.typingdna.util.HelperFunctions;
import com.typingdna.api.TypingDNAAPI;

public class Decision extends AbstractCore {

    public Decision(ConfigAdapter config, TypingDNAAPI api) {
        super(config, api);
    }

    @Override
    public StateChange handleForm() {
        AuthenticationData authData = new AuthenticationData(
                HelperFunctions.hashText(state.getUsername(), config.usernameSalt(), config.hashAlgorithm()),
                state.getTypingPattern(),
                state.getDeviceType(),
                state.getTextId(),
                state.getPreviousAction(),
                getRequestIdentifier()
        );

        TDNAAuthentication authentication = new TDNAAuthentication(config, state, api);
        final StateChange stateChange = authentication.performAuthentication(authData);

        if (stateChange instanceof ExitNodeStateChange) {
            final ExitNodeStateChange exitNodeStateChange = (ExitNodeStateChange) stateChange;
            this.setAction(exitNodeStateChange.getAction());
            this.setAutoEnroll(exitNodeStateChange.isAutoEnroll());
        }

        return stateChange;
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
