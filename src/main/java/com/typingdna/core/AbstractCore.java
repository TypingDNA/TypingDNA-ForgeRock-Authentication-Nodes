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

import com.google.common.base.Strings;
import com.typingdna.api.TypingDNAAPI;
import com.typingdna.core.statechanges.StateChange;
import com.typingdna.util.ConfigAdapter;
import com.typingdna.util.Logger;
import com.typingdna.util.State;

public abstract class AbstractCore {

    protected static final Logger logger = Logger.getInstance();

    private String nodeId = null;
    private boolean debugEnabled = false;
    private String action = "n/a";
    private boolean autoEnroll = false;

    protected ConfigAdapter config;
    protected State state;
    protected TypingDNAAPI api;

    public AbstractCore(ConfigAdapter config, State state) {
        this.config = config;
        this.state = state;
        this.api = null;
    }

    public AbstractCore(ConfigAdapter config, State state, TypingDNAAPI api) {
        this.config = config;
        this.state = state;
        this.api = api;
    }

    /**
     * Send one or more callbacks to the user to interact with.
     *
     * @return StateChange - should be of type DisplayFormStateChange
     */
    public abstract StateChange displayForm();

    /**
     * Send one or more callbacks to the user to interact with, depending on the current action.
     *
     * @param actionType the current action type (e.g. VERIFY, ENROLL, etc.)
     * @return StateChange - should be of type DisplayFormStateChange
     */
    public abstract StateChange displayForm(ActionType actionType);

    /**
     * Handle users interaction with the previously sent callbacks.
     *
     * @return StateChange - one of the defined state changes (e.g. ExistNodeStateChange if we want to leave the current
     * node)
     */
    public abstract StateChange handleForm();

    /**
     * Check if the callbacks sent were displayed and the user interacted with them.
     *
     * @return true or false
     */
    public abstract boolean isFormDisplayed();

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getRequestIdentifier() {
        String requestIdentifier = config.requestIdentifier();
        if (!Strings.isNullOrEmpty(nodeId)) {
            requestIdentifier += "-" + nodeId;
        }

        return requestIdentifier;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public boolean isAutoEnroll() {
        return autoEnroll;
    }

    public void setAutoEnroll(boolean autoEnroll) {
        this.autoEnroll = autoEnroll;
    }
}
