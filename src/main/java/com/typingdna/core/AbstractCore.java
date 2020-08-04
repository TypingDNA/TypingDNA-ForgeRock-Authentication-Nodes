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
import com.typingdna.core.statechanges.StateChange;
import com.typingdna.util.*;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.NodeProcessException;

import javax.security.auth.callback.Callback;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Stream;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

public abstract class AbstractCore {

    protected static final Logger logger = Logger.getInstance();

    public enum ActionType {
        CHECK_USER(0),
        VERIFY(1),
        RETRY(2),
        ENROLL(3),
        ENROLL_POSITION(4);

        private final int action;

        ActionType(int action) {
            this.action = action;
        }

        public static ActionType toActionType(int action) {
            switch (action) {
                case 0:
                    return ActionType.CHECK_USER;
                case 1:
                    return ActionType.VERIFY;
                case 2:
                    return ActionType.RETRY;
                case 3:
                    return ActionType.ENROLL;
                case 4:
                    return ActionType.ENROLL_POSITION;
                default:
                    return ActionType.VERIFY;
            }
        }

        public int getAction() {
            return action;
        }
    }

    public enum DeviceType {
        DESKTOP,
        MOBILE
    }

    public enum VerifyResponse {
        MATCH,
        NO_MATCH,
        NEEDS_ENROLL,
        NEEDS_ENROLL_POSITION,
        REQUEST_ERROR,
        INVALID_CREDENTIALS,
    }

    public enum SaveResponse {
        DONE,
        FAIL
    }

    private String nodeId = null;
    private boolean debugEnabled = false;

    protected ConfigAdapter config;
    protected TypingDNAApi api;

    /**
     * Node state
     **/
    protected JsonValue sharedState;
    protected JsonValue transientState;
    protected List<? extends Callback> callbacks;

    public AbstractCore(ConfigAdapter config) {
        this.config = config;
        this.api = null;
    }

    public AbstractCore(ConfigAdapter config, TypingDNAApi api) {
        this.config = config;
        this.api = api;
    }

    public void setCurrentState(JsonValue sharedState, JsonValue transientState, List<? extends Callback> callbacks) {
        this.sharedState = sharedState;
        this.transientState = transientState;
        this.callbacks = callbacks;
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

    protected int doCheckUser(String username, TypingDNAApi.PatternType patternType, String textId) {
        DeviceType deviceType = getDeviceType();

        debug(String.format("doCheckUser username=%s deviceType=%s patternType=%s", username, deviceType, patternType));

        int patternCount = Integer.MAX_VALUE;
        JsonValue response;
        try {
            response = api.checkUser(username, patternType, textId, getRequestIdentifier());
            debug("Response: " + response.toString());

            if (getValueFromJson(response, "success", 0) == 1) {
                int desktopCount = getValueFromJson(response, "count", 0);
                int mobileCount = getValueFromJson(response, "mobilecount", 0);
                patternCount = deviceType == DeviceType.DESKTOP ? desktopCount : mobileCount;
            }
        } catch (NodeProcessException e) {
            debug(e.getMessage());
        }

        return patternCount;
    }

    protected VerifyResponse doVerify(String username, String typingPattern) {
        debug(String.format("doVerify username=%s", username));

        JsonValue response;
        try {
            response = api.verify(username, typingPattern, getRequestIdentifier());
            debug("Response: " + response.toString());
        } catch (NodeProcessException e) {
            debug(e.getMessage());
            return VerifyResponse.REQUEST_ERROR;
        }

        VerifyResponse verifyResponse = VerifyResponse.NO_MATCH;
        int statusCode = getValueFromJson(response, "status", 200);
        int messageCode = getValueFromJson(response, "message_code", -1);
        debug(String.format("Message code %d", messageCode));

        if (messageCode == 32 || messageCode == 33) {
            verifyResponse = VerifyResponse.INVALID_CREDENTIALS;
        } else if (statusCode != 200) {
            verifyResponse = VerifyResponse.REQUEST_ERROR;
        } else if (messageCode == 4) {
            verifyResponse = VerifyResponse.NEEDS_ENROLL;
        } else if (messageCode == 3) {
            verifyResponse = VerifyResponse.NEEDS_ENROLL_POSITION;
        } else {
            int score = getValueFromJson(response, "score", 0);
            int netScore = getValueFromJson(response, "net_score", 0);
            String action = getValueFromJson(response, "action", "");

            if (netScore >= config.matchScore()) {
                if (action.contains("enroll")) {
                    incrementPatternsEnrolled();
                } else {
                    if (canAutoEnroll(netScore, score)) {
                        doSave(username, typingPattern);
                    }
                }

                verifyResponse = VerifyResponse.MATCH;
            } else if (action.equals("enroll")) {
                incrementPatternsEnrolled();
                verifyResponse = VerifyResponse.NEEDS_ENROLL;
            }
        }

        logger.debug(String.format("Username %s verify response %s", username, verifyResponse.name()));

        return verifyResponse;
    }

    protected SaveResponse doSave(String username, String typingPattern) {
        debug(String.format("doSave username=%s", username));

        JsonValue response = null;
        try {
            response = api.save(username, typingPattern, getRequestIdentifier());
            debug("Response: " + response.toString());
        } catch (NodeProcessException e) {
            debug(e.getMessage());
        }

        SaveResponse saveResponse = SaveResponse.FAIL;
        if (getValueFromJson(response, "success", 0) == 1) {
            incrementPatternsEnrolled();
            saveResponse = SaveResponse.DONE;
        }

        return saveResponse;
    }

    private boolean canAutoEnroll(int net_score, int score) {
        return net_score >= config.matchScore() && score >= config.autoEnrollScore()
                && getPreviousAction() != ActionType.RETRY;
    }

    protected void addPatternToPreviousPatterns(String typingPattern) {
        String previousPatterns = sharedState.get(Constants.PREVIOUS_TYPING_PATTERNS).asString();
        previousPatterns = previousPatterns == null ? "" : ";" + previousPatterns;
        sharedState.put(Constants.PREVIOUS_TYPING_PATTERNS, typingPattern + previousPatterns);
    }

    public <T extends Callback> Stream<T> getCallbacks(Class<T> type) {
        return callbacks
                .stream()
                .filter((c) -> type.isAssignableFrom(c.getClass()))
                .map(type::cast);
    }

    protected DeviceType getDeviceType() {
        int deviceTypeValue = sharedState.get(Constants.DEVICE_TYPE).asInteger();
        DeviceType deviceType = deviceTypeValue == 0 ? DeviceType.DESKTOP : DeviceType.MOBILE;
        debug("Get device type: " + deviceType);

        return deviceType;
    }

    protected String getTypingPattern() {
        String typingPattern = sharedState.get(Constants.TYPING_PATTERN).asString();
        if (typingPattern == null) {
            typingPattern = "";
        }
        debug("Get typing pattern");

        return typingPattern;
    }

    protected String getTextId() {
        return sharedState.get(Constants.TEXT_ID).asString();
    }

    protected String getUsername() {
        String username = sharedState.get(USERNAME).asString();
        return hashText(username, config.usernameSalt());
    }

    protected int getRetries() {
        Integer retries = sharedState.get(Constants.VERIFY_RETRIES).asInteger();
        int r = (retries == null) ? 0 : retries;
        debug("Get retries from shared state: " + r);

        return r;
    }

    protected int getPatternsEnrolled() {
        Integer patternsEnrolled = sharedState.get(Constants.PATTERNS_ENROLLED).asInteger();
        int pe = (patternsEnrolled) == null ? 0 : patternsEnrolled;
        debug("Get patterns enrolled from shared state: " + pe);

        return pe;
    }

    protected int getEnrollmentsLeft() {
        Integer enrollmentsLeft = sharedState.get(Constants.ENROLLMENTS_LEFT).asInteger();
        int ef = (enrollmentsLeft) == null ? 0 : enrollmentsLeft;
        debug("Get enrollments left from shared state: " + ef);

        return ef;
    }

    protected void setPatternsEnrolled(int patternsEnrolled) {
        debug("Set patterns enrolled in shared state: " + patternsEnrolled);
        sharedState
                .put(Constants.ENROLLMENTS_LEFT, config.enrollmentsNecessary() - patternsEnrolled)
                .put(Constants.PATTERNS_ENROLLED, patternsEnrolled);
    }

    protected void incrementPatternsEnrolled() {
        debug("Increment patterns enrolled in shared state: ");
        int patternsEnrolled = getPatternsEnrolled() + 1;
        sharedState
                .put(Constants.ENROLLMENTS_LEFT, config.enrollmentsNecessary() - patternsEnrolled)
                .put(Constants.PATTERNS_ENROLLED, patternsEnrolled);
    }

    protected ActionType getPreviousAction() {
        Integer actionType = sharedState.get(Constants.PREVIOUS_ACTION).asInteger();
        ActionType at = ActionType.toActionType(actionType == null ? -1 : actionType);
        debug("Get previous action from shared state: " + at.getAction());

        return at;
    }

    protected static String hashText(String text, String salt) {
        String hashedText = "";
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(String.format("%s%s", salt, text).getBytes());
            hashedText = HelperFunctions.bytesToHex(md5.digest());
        } catch (NoSuchAlgorithmException e) {
            logger.debug(e.getMessage());
        }

        return hashedText;
    }

    protected static String fnv1a32(String text) {
        if (text == null) {
            return "";
        }

        String data = text.toLowerCase();
        BigInteger hash = new BigInteger("721b5ad4", 16);

        for (byte b : data.getBytes()) {
            hash = hash.xor(BigInteger.valueOf((int) b & 0xff));
            hash = hash.multiply(new BigInteger("01000193", 16)).mod(new BigInteger("2").pow(32));
        }
        return hash.toString();
    }

    protected String getRecorderScript() {
        String dataCollectors = String.format(
                "function dataCollectors() {\n" +
                        "%s\n%s\n" +
                        "}\n",
                getRecordDeviceTypeScript(),
                getRecordTypingPatternScript()
        );

        return String.format("%s\n" +
                        "%s\n" +
                        "if (typeof TypingDNA !== 'undefined') {\n" +
                        "    dataCollectors();\n" +
                        "} else {\n" +
                        "      var root = document.head;\n" +
                        "      var scriptElem = document.createElement('script');\n" +
                        "      scriptElem.src = 'https://typingdna.com/scripts/typingdna.js';\n" +
                        "      scriptElem.async = false;\n" +
                        "      root.insertBefore(scriptElem, root.firstChild);\n" +
                        "      scriptElem.addEventListener('error', function() {\n" +
                        "          throw new Error('TypingDNA recorder not loaded');\n" +
                        "      });\n" +
                        "      scriptElem.addEventListener('load', function() {\n" +
                        "          dataCollectors();\n" +
                        "      });\n" +
                        "}",
                config.script().getScript(),
                dataCollectors
        );
    }

    private String getRecordDeviceTypeScript() {
        return String.format(
                "(function(output) {\n" +
                        "  var tdna = new TypingDNA();\n" +
                        "  var hiddenInput = document.getElementById('%s');\n" +
                        "  hiddenInput.value = tdna.isMobile();\n" +
                        "})(document.forms[0].elements['%s']);\n",
                Constants.DEVICE_TYPE_OUTPUT_VARIABLE,
                Constants.DEVICE_TYPE_OUTPUT_VARIABLE);
    }

    private String getRecordTypingPatternScript() {
        String textId = sharedState.get(Constants.TEXT_ID).asString();
        String textToEnter = sharedState.get(Constants.TEXT_TO_ENTER).asString();
        String additionalScripts = "";

        if (Strings.isNullOrEmpty(textToEnter)) {
            textToEnter = "undefined";
        } else {
            additionalScripts += getTextHighlightScript(textToEnter);
            textToEnter = String.format("\"%s\"", textToEnter);
        }

        if (Strings.isNullOrEmpty(textId)) {
            textId = "undefined";
        } else {
            textId = String.format("\"%s\"", textId);
        }

        if (config.disableCopyAndPaste()) {
            additionalScripts += getDisableCopyAndPastScript();
        }

        additionalScripts += getChangeLoginButtonTextScript();

        return String.format(
                "(function(output) {\n" +
                        "  %s\n\n" +
                        "  %s\n\n" +
                        "  var typingVisualizer = new TypingVisualizer();\n" +
                        "  var tdna = new TypingDNA(); " +
                        "  tdna.start();\n" +
                        "  var inputs = Array.from(document.forms[0].getElementsByTagName('input')).filter(input => input && input.id && input.id.indexOf('idToken') >= 0 && (input.type === 'text' || input.type === 'password'));\n" +
                        "  var targets = [];\n" +
                        "  for (var i = 0; i < inputs.length; i++) {\n" +
                        "       var input = inputs[i];\n" +
                        "       if (%s) {\n" +
                        "           typingVisualizer.addTarget([input.id]);\n" +
                        "       }\n" +
                        "       tdna.addTarget(input.id);\n" +
                        "       targets.push(input.id);\n" +
                        "  }\n" +
                        "  var button = document.getElementById('loginButton_0');\n" +
                        "  button.onclick = function (e) {\n" +
                        "      var hiddenInput = document.getElementById('%s');\n" +
                        "      var typingPattern = tdna.getTypingPattern({type: 1, textId: %s, text: %s});\n" +
                        "      var inputs = Array.from(document.forms[0].getElementsByTagName('input')).filter(input => input && input.id && input.id.indexOf('idToken') >= 0 && (input.type === 'text' || input.type === 'password'));\n" +
                        "      var textId = tdna.getTextId(inputs.map(input => input.value).join(''));\n" +
                        "      if (hiddenInput) {\n" +
                        "          hiddenInput.value = typingPattern + '<<>>' + textId;\n" +
                        "      }\n" +
                        "  };\n" +
                        "})(document.forms[0].elements['%s']);\n",
                Constants.typingPatternVisualizer,
                additionalScripts,
                String.valueOf(config.showVisualizer()),
                Constants.PATTERN_OUTPUT_VARIABLE,
                textId,
                textToEnter,
                Constants.PATTERN_OUTPUT_VARIABLE
        );
    }

    private String getChangeLoginButtonTextScript() {
        ActionType action = getPreviousAction();

        if (action == ActionType.ENROLL || action == ActionType.ENROLL_POSITION) {
            return "var lgnBtn = Array.from(document.getElementsByTagName('input')).filter(btn => btn.type === 'submit')[0];\n" +
                    "if (lgnBtn) {\n" +
                    "    lgnBtn.value = 'Enroll';\n" +
                    "}\n";
        }

        return "";
    }

    private String getTextHighlightScript(String textToEnter) {
        return String.format("try {\n" +
                "function setHighlight(enteredText) {\n" +
                "        var shortPhraseText = \"%s\";\n" +
                "        var shortPhraseTextLowercased = shortPhraseText.toLowerCase();\n" +
                "        var shortPhraseDisplay = Array.from(document.getElementsByTagName('div')).filter(function(e) {\n" +
                "            return e.id && e.id.indexOf('callback_') >= 0 && e.textContent === '%s';\n" +
                "        })[0];\n" +
                "    \n" +
                "        for (var i = 0; i < enteredText.length && i < shortPhraseTextLowercased.length && enteredText[i] === shortPhraseTextLowercased[i]; ++i);\n" +
                "        var entered = document.createElement('span');\n" +
                "        entered.textContent = shortPhraseText.slice(0, i);\n" +
                "        var toBeEntered = document.createElement('span');\n" +
                "        toBeEntered.style = 'background-color: rgb(255, 156, 82, 0.4);';\n" +
                "        toBeEntered.textContent = shortPhraseText.slice(i);\n" +
                "    \n" +
                "        shortPhraseDisplay.textContent = \"\";\n" +
                "        shortPhraseDisplay.appendChild(entered);\n" +
                "        shortPhraseDisplay.appendChild(toBeEntered);\n" +
                "}\n" +
                "    setHighlight('');\n" +
                "    var input = Array.from(document.getElementsByTagName('input')).filter(function(e) {\n" +
                "        return e.type && e.type === 'text' && e.id && e.id.indexOf('idToken') >= 0;\n" +
                "    })[0];\n" +
                "    input.addEventListener('input', function (e) {\n" +
                "        var enteredText = e.target.value;\n" +
                "        enteredText = enteredText.toLowerCase();\n" +
                "        setHighlight(enteredText);\n" +
                "    });\n" +
                "} catch (e) {console.error(e);}\n", textToEnter, textToEnter);
    }

    private String getDisableCopyAndPastScript() {
        return "(function () {\n" +
                "    var inputs = Array.from(document.forms[0].getElementsByTagName('input')).filter(input => input && input.id && input.id.indexOf('idToken') >= 0 && (input.type === 'text' || input.type === 'password'));\n" +
                "    inputs.forEach(input => {\n" +
                "        input.oncopy = function () { return false; };\n" +
                "        input.onpaste = function () { return false; }\n;" +
                "    });\n" +
                "})();\n";
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    private String getRequestIdentifier() {
        String requestIdentifier = config.requestIdentifier();
        if (!Strings.isNullOrEmpty(nodeId)) {
            requestIdentifier += "-" + nodeId;
        }

        return requestIdentifier;
    }

    public void setDebug(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }

    protected void debug(String message) {
        if (debugEnabled) {
            logger.debug(message);
        }
    }

    private static <T> T getValueFromJson(JsonValue json, String key, T defaultValue) {
        if (json == null) {
            return defaultValue;
        }

        Object value = json.get(key).getObject();
        if (value == null) {
            return defaultValue;
        }

        return (T) value;
    }
}
