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

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;

import javax.security.auth.callback.Callback;
import java.util.List;

import static org.forgerock.openam.auth.node.api.Action.send;

public class DisplayFormStateChange implements StateChange {
    public final List<? extends Callback> callbacks;
    public JsonValue sharedState = null;
    public JsonValue transientState = null;

    public DisplayFormStateChange(List<? extends Callback> callbacks) {
        this.callbacks = callbacks;
    }

    public DisplayFormStateChange setSharedState(JsonValue sharedState) {
        this.sharedState = sharedState;
        return this;
    }

    public DisplayFormStateChange setTransientState(JsonValue transientState) {
        this.transientState = transientState;
        return this;
    }

    @Override
    public Action build() {
        Action.ActionBuilder actionBuilder = send(callbacks);
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
        return "DISPLAY FORM";
    }
}
