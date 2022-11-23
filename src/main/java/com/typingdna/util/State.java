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


package com.typingdna.util;

import com.typingdna.api.model.DeviceType;
import com.typingdna.core.ActionType;
import org.forgerock.json.JsonValue;

import javax.security.auth.callback.Callback;
import java.util.List;
import java.util.stream.Stream;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

public final class State {

    private static State instance = null;

    private JsonValue sharedState;
    private JsonValue transientState;
    private List<? extends Callback> callbacks;

    private State() { }

    public static State getInstance() {
        if (instance == null) {
            instance = new State();
        }

        return instance;
    }

    public void setState(JsonValue sharedState, JsonValue transientState, List<? extends Callback> callbacks) {
        this.sharedState = sharedState;
        this.transientState = transientState;
        this.callbacks = callbacks;
    }

    public JsonValue getSharedState() {
        return sharedState.copy();
    }

    public JsonValue getTransientState() {
        return transientState.copy();
    }

    public String getUsername() {
        return sharedState.get(USERNAME).asString();
    }

    public String getTypingPattern() {
        String typingPattern = transientState.get(Constants.TYPING_PATTERN).asString();
        if (typingPattern == null) {
            typingPattern = "";
        }
        return typingPattern;
    }

    public void setTypingPattern(String typingPattern) {
        transientState.put(Constants.TYPING_PATTERN, typingPattern);
    }

    public String getPreviousTypingPatterns() {
        return transientState.get(Constants.PREVIOUS_TYPING_PATTERNS).asString();
    }

    public void setPreviousTypingPatterns(String previousTypingPatterns) {
        transientState.put(Constants.PREVIOUS_TYPING_PATTERNS, previousTypingPatterns);
    }

    public String getTextId() {
        return transientState.get(Constants.TEXT_ID).asString();
    }

    public void setTextId(String textId) {
        transientState.put(Constants.TEXT_ID, textId);
    }

    public DeviceType getDeviceType() {
        int deviceTypeValue = sharedState.get(Constants.DEVICE_TYPE).asInteger();
        return deviceTypeValue == 0 ? DeviceType.DESKTOP : DeviceType.MOBILE;
    }

    public void setDeviceType(DeviceType deviceType) {
        sharedState.put(Constants.DEVICE_TYPE, deviceType.ordinal());
    }

    public int getPatternsEnrolled() {
        Integer patternsEnrolled = sharedState.get(Constants.PATTERNS_ENROLLED).asInteger();
        return (patternsEnrolled) == null ? 0 : patternsEnrolled;
    }

    public void setPatternsEnrolled(int patternsEnrolled) {
        sharedState.put(Constants.PATTERNS_ENROLLED, patternsEnrolled);
    }

    public int getRetries() {
        Integer retries = sharedState.get(Constants.VERIFY_RETRIES).asInteger();
        return (retries == null) ? 0 : retries;
    }

    public void incrementRetries() {
        sharedState.put(Constants.VERIFY_RETRIES, getRetries() + 1);
    }

    public ActionType getPreviousAction() {
        Integer actionType = sharedState.get(Constants.PREVIOUS_ACTION).asInteger();
        return ActionType.toActionType(actionType == null ? -1 : actionType);
    }

    public void setPreviousAction(ActionType previousAction) {
        sharedState.put(Constants.PREVIOUS_ACTION, previousAction.getAction());
    }

    public String getTextToEnter() {
        return sharedState.get(Constants.TEXT_TO_ENTER).asString();
    }

    public void setTextToEnter(String textToEnter) {
        sharedState.put(Constants.TEXT_TO_ENTER, textToEnter);
    }

    public String getMessage() {
        return sharedState.get(Constants.MESSAGE).asString();
    }

    public void setMessage(String message) {
        sharedState.put(Constants.MESSAGE, message);
    }

    public <T extends Callback> Stream<T> getCallbacks(Class<T> type) {
        return callbacks
                .stream()
                .filter((c) -> type.isAssignableFrom(c.getClass()))
                .map(type::cast);
    }
}
