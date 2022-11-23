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


package com.typingdna.core.statechanges;

import com.typingdna.api.model.APIResponse;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;

import java.util.Optional;


public class ExitNodeStateChange implements StateChange {
    public final String outcome;
    public JsonValue sharedState = null;
    public JsonValue transientState = null;
    public Optional<APIResponse> apiResponse = Optional.empty();
    public String action = "n/a";
    public boolean isAutoEnroll = false;

    public ExitNodeStateChange(String outcome) {
        this.outcome = outcome;
    }

    public ExitNodeStateChange setSharedState(JsonValue sharedState) {
        this.sharedState = sharedState;
        return this;
    }

    public ExitNodeStateChange setTransientState(JsonValue transientState) {
        this.transientState = transientState;
        return this;
    }

    public ExitNodeStateChange setApiResponse(APIResponse apiResponse) {
        this.apiResponse = Optional.of(apiResponse);
        return this;
    }

    public String getAction() {
        return action;
    }

    public ExitNodeStateChange setAction(String action) {
        this.action = action;
        return this;
    }

    public boolean isAutoEnroll() {
        return isAutoEnroll;
    }

    public ExitNodeStateChange setAutoEnroll(boolean autoEnroll) {
        isAutoEnroll = autoEnroll;
        return this;
    }

    @Override
    public Action build() {
        Action.ActionBuilder actionBuilder = Action.goTo(outcome);
        if (sharedState != null) {
            actionBuilder.replaceSharedState(sharedState.copy());
        }
        if (transientState != null) {
            actionBuilder.replaceTransientState(transientState.copy());
        }

        return actionBuilder.build();
    }

    @Override
    public String getOutcome() {
        return outcome;
    }
}
