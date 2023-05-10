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
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import com.typingdna.api.model.DeviceType;
import com.typingdna.core.statechanges.DisplayFormStateChange;
import com.typingdna.core.statechanges.SingleOutcomeStateChange;
import com.typingdna.core.statechanges.StateChange;
import com.typingdna.util.ConfigAdapter;
import com.typingdna.util.Constants;
import com.typingdna.util.State;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.TextOutputCallback;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Recorder extends AbstractCore {

    public Recorder(ConfigAdapter config, State state) {
        super(config, state);
    }

    @Override
    public boolean isFormDisplayed() {
        boolean isFormDisplayed = state.getCallbacks(HiddenValueCallback.class)
                .filter(callback -> callback.getId().equals(Constants.PATTERN_OUTPUT_VARIABLE))
                .map(HiddenValueCallback::getValue)
                .anyMatch(result -> !Strings.isNullOrEmpty(result) && !result.equals("default value"));
        logger.debug(String.format("In TypingDNARecorder: recorder %s inserted", isFormDisplayed ? "is" : "is not"));
        return isFormDisplayed;
    }

    @Override
    public StateChange displayForm() {
        return displayForm(ActionType.VERIFY);
    }

    @Override
    public StateChange displayForm(ActionType actionType) {
        logger.debug(String.format("In TypingDNARecorder: Inserting recorder actionType=%d", actionType.getAction()));

        List<Callback> callbacks = new ArrayList<>();

        String recorderScript = getRecorderScript();
        ScriptTextOutputCallback recorderJsScript = new ScriptTextOutputCallback(recorderScript);
        HiddenValueCallback deviceTypeCallback = new HiddenValueCallback(Constants.DEVICE_TYPE_OUTPUT_VARIABLE, "default value");
        HiddenValueCallback typingPatternCallback = new HiddenValueCallback(Constants.PATTERN_OUTPUT_VARIABLE, "default value");
        HiddenValueCallback textIdCallback = new HiddenValueCallback(Constants.TEXT_ID_OUTPUT_VARIABLE, "default value");

        if (config.displayMessage()) {
            String message = state.getMessage();
            if (!Strings.isNullOrEmpty(message)) {
                callbacks.add(new TextOutputCallback(TextOutputCallback.INFORMATION, message));
            }
        }

        callbacks.add(recorderJsScript);
        callbacks.add(deviceTypeCallback);
        callbacks.add(typingPatternCallback);
        callbacks.add(textIdCallback);

        return new DisplayFormStateChange(callbacks);
    }

    @Override
    public StateChange handleForm() {
        DeviceType deviceType = getDeviceType();
        String typingPattern = getTypingPattern();
        String textId = getTextId();

        if (typingPattern.equals("undefined")) {
            typingPattern = "";
        }
        if (textId.equals("undefined")) {
            textId = "";
        }

        state.setDeviceType(deviceType);
        state.setTypingPattern(typingPattern);
        state.setTextId(textId);

        return new SingleOutcomeStateChange().setSharedState(state.getSharedState()).setTransientState(state.getTransientState());
    }

    private DeviceType getDeviceType() {
        String deviceType = state.getCallbacks(HiddenValueCallback.class)
                .filter(result -> result.getId().equals(Constants.DEVICE_TYPE_OUTPUT_VARIABLE))
                .map(HiddenValueCallback::getValue)
                .filter(result -> !Strings.isNullOrEmpty(result))
                .findFirst()
                .orElse("0");

        return deviceType.equals("0") ? DeviceType.DESKTOP : DeviceType.MOBILE;
    }

    private String getTypingPattern() {
        return state.getCallbacks(HiddenValueCallback.class)
                .filter(result -> result.getId().equals(Constants.PATTERN_OUTPUT_VARIABLE))
                .map(HiddenValueCallback::getValue)
                .filter(result -> !Strings.isNullOrEmpty(result))
                .findFirst()
                .orElse("");
    }

    private String getTextId() {
        String textId;
        if (Strings.isNullOrEmpty(state.getTextId())) {
            textId = state.getCallbacks(HiddenValueCallback.class)
                    .filter(result -> result.getId().equals(Constants.TEXT_ID_OUTPUT_VARIABLE))
                    .map(HiddenValueCallback::getValue)
                    .filter(result -> !Strings.isNullOrEmpty(result))
                    .findFirst()
                    .orElse("");
        } else {
            textId = state.getTextId();
        }

        return textId;
    }

    private String getRecorderScript() {
        String dataCollectors = String.format(
                "function triggerEvent(element) {\n" +
                "   if ('createEvent' in document) {\n" +
                "       var evt = document.createEvent('HTMLEvents');\n" +
                "       evt.initEvent('change', false, true);\n" +
                "       element.dispatchEvent(evt);\n" +
                "   } else {\n" +
                "       element.fireEvent('onchange');\n" +
                "   }\n" +
                "}\n" +
                "function dataCollectors() {\n" +
                        "%s\n%s\n" +
                        "}\n",
                getRecordDeviceTypeScript(),
                getRecordTypingPatternScript()
        );

        return String.format("if (typeof TypingDNA === 'undefined') { %s }\n" +
                        "%s\n" +
                        "if (typeof TypingDNA !== 'undefined') {\n" +
                        "    if (!document.getElementById('%s')) {\n" +
                        "        var target = document.querySelector('body');\n" +
                        "        var observer = new MutationObserver(function () {\n" +
                        "            if (document.getElementById('%s')) {\n" +
                        "                dataCollectors();\n" +
                        "                observer.disconnect();\n" +
                        "             }\n" +
                        "        });\n" +
                        "        observer.observe(target, { childList: true, subtree: true });\n" +
                        "    } else {\n" +
                        "        dataCollectors();\n" +
                        "    }\n" +
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
                dataCollectors,
                Constants.DEVICE_TYPE_OUTPUT_VARIABLE,
                Constants.DEVICE_TYPE_OUTPUT_VARIABLE
        );
    }

    private String getRecordDeviceTypeScript() {
        return String.format(
                "(function(output) {\n" +
                        "  var tdna = new TypingDNA();\n" +
                        "  var hiddenInput = document.getElementById('%s');\n" +
                        "  hiddenInput.value = tdna.isMobile();\n" +
                        "  triggerEvent(hiddenInput);\n" +
                        "})(document.forms[0].elements['%s']);\n",
                Constants.DEVICE_TYPE_OUTPUT_VARIABLE,
                Constants.DEVICE_TYPE_OUTPUT_VARIABLE);
    }

    private String getRecordTypingPatternScript() {
        String textId = state.getTextId();
        String textToEnter = state.getTextToEnter();
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
                        "  var tdna = new TypingDNA();\n" +
                        "  tdna.start();\n" +
                        "  var inputs = Array.from(document.getElementsByTagName('input')).filter(input => input && input.id && [%s].find(function (x) { return x == input.id; }));\n" +
                        "  var targets = [];\n" +
                        "  for (var i = 0; i < inputs.length; i++) {\n" +
                        "       var input = inputs[i];\n" +
                        "       input.value = '';\n" +
                        "       if (%s) {\n" +
                        "           typingVisualizer.addTarget([input.id]);\n" +
                        "       }\n" +
                        "       tdna.addTarget(input.id);\n" +
                        "       targets.push(input.id);\n" +
                        "  }\n" +
                        "  typingVisualizer.correctPosition();\n" +
                        "  var button = document.getElementById('%s');\n" +
                        "  if (!button) { console.error('Submit button not found! Set the correct id in the TypingDNA Recorder Node configuration.'); return; }\n" +
                        "  button.onclick = function (e) {\n" +
                        "      var patternHiddenInput = document.getElementById('%s');\n" +
                        "      var textIdHiddenInput = document.getElementById('%s');\n" +
                        "      var textId = %s;\n" +
                        "      var textToEnter = %s;\n" +
                        "      var inputs = [];\n" +
                        "      var type = 2;\n" +
                        "      for (var inputId of [%s]) { var input = document.getElementById(inputId); if (input) { inputs.push(input); if (input.type == 'password') { type = 1; } } }\n" +
                        "      if (!textId) { textId = tdna.getTextId(inputs.map(input => input.value).join('')); }\n" +
                        "      if (!textToEnter) { textToEnter = inputs.map(input => input.value).join(''); }\n" +
                        "      var typingPattern = tdna.getTypingPattern({type: type, text: textToEnter});\n" +
                        "      if (!typingPattern) { typingPattern = '%s'; }\n" +
                        "      patternHiddenInput.value = typingPattern;\n" +
                        "      textIdHiddenInput.value = textId;\n" +
                        "      triggerEvent(patternHiddenInput); triggerEvent(textIdHiddenInput);\n" +
                        "      tdna.reset();\n" +
                        "  };\n" +
                        "})(document.forms[0].elements['%s']);\n",
                Constants.typingPatternVisualizer,
                additionalScripts,
                getTargetIds(),
                config.showVisualizer(),
                config.submitButtonId(),
                Constants.PATTERN_OUTPUT_VARIABLE,
                Constants.TEXT_ID_OUTPUT_VARIABLE,
                textId,
                textToEnter,
                getTargetIds(),
                Constants.PATTERN_OUTPUT_VARIABLE,
                Constants.PATTERN_OUTPUT_VARIABLE
        );
    }

    private String getChangeLoginButtonTextScript() {
        ActionType previousAction = state.getPreviousAction();

        if (previousAction == ActionType.ENROLL || previousAction == ActionType.ENROLL_POSITION) {
            return String.format("var lgnBtn = document.getElementById('%s');\n" +
                    "if (lgnBtn) {\n" +
                    "    lgnBtn.value = 'Enroll';\n" +
                    "}\n", config.submitButtonId());
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
                "        return e.type && e.type === 'text' && e.id && [%s].find(function (x) { return x == e.id; });\n" +
                "    })[0];\n" +
                "    input.addEventListener('input', function (e) {\n" +
                "        var enteredText = e.target.value;\n" +
                "        enteredText = enteredText.toLowerCase();\n" +
                "        setHighlight(enteredText);\n" +
                "    });\n" +
                "} catch (e) {console.error(e);}\n",
                textToEnter,
                textToEnter,
                getTargetIds()
        );
    }

    private String getDisableCopyAndPastScript() {
        return String.format("(function () {\n" +
                "    var inputs = Array.from(document.getElementsByTagName('input')).filter(input => input && input.id && [%s].find(function (x) { return x == input.id; }));\n" +
                "    if (inputs[0]) { var i = inputs[0]; while (i && i.tagName != 'FORM') { i = i.parentElement; } if (i) { i.setAttribute('autocomplete', 'off'); } }\n" +
                "    inputs.forEach(input => {\n" +
                "        input.oncopy = function () { return false; };\n" +
                "        input.onpaste = function () { return false; }\n;" +
                "    });\n" +
                "})();\n", getTargetIds());
    }

    private String getTargetIds() {
        return config.targetIds().stream().map(t -> "'" + t + "'").collect(Collectors.joining(","));
    }
}
