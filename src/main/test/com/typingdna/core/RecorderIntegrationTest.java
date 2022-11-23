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

import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import com.typingdna.api.model.DeviceType;
import com.typingdna.core.statechanges.DisplayFormStateChange;
import com.typingdna.core.statechanges.SingleOutcomeStateChange;
import com.typingdna.core.statechanges.StateChange;
import com.typingdna.util.ConfigAdapter;
import com.typingdna.util.Constants;
import com.typingdna.util.State;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.scripting.service.ScriptConfiguration;
import org.forgerock.openam.utils.JsonObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.TextOutputCallback;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

public class RecorderIntegrationTest {

    @Mock
    private ScriptConfiguration scriptConfiguration;
    @Mock
    private ConfigAdapter config;

    private Recorder useCase;
    private State state = State.getInstance();

    @Before
    public void setUp() {
        scriptConfiguration = mock(ScriptConfiguration.class);
        config = mock(ConfigAdapter.class);

        when(config.script()).thenReturn(scriptConfiguration);
        when(scriptConfiguration.getScript()).thenReturn("script");

        useCase = new Recorder(config);
    }

    @Test
    public void test_DisplayForm_Verify() {
        state.setState(new JsonObject().build(), new JsonObject().build(), new ArrayList<>());
        StateChange stateChange = useCase.displayForm(ActionType.VERIFY);

        Assert.assertNotNull("StateChange cannot be null", stateChange);
        Assert.assertEquals("StateChange must be an instance of DisplayFormStateChange", DisplayFormStateChange.class, stateChange.getClass());

        DisplayFormStateChange displayFormStateChange = (DisplayFormStateChange) stateChange;

        Assert.assertNotNull("callbacks cannot be null", displayFormStateChange.callbacks);
        Assert.assertEquals("callbacks must contain 4 callbacks", 4, displayFormStateChange.callbacks.size());

        Assert.assertEquals("1st callback must be a ScriptTextOutputCallback",
                ScriptTextOutputCallback.class, displayFormStateChange.callbacks.get(0).getClass());

        Assert.assertEquals("2nd callback must be a HiddenValueCallback",
                HiddenValueCallback.class, displayFormStateChange.callbacks.get(1).getClass());
        Assert.assertEquals("2nd callback id must match Constants.DEVICE_TYPE_OUTPUT_VARIABLE",
                Constants.DEVICE_TYPE_OUTPUT_VARIABLE, ((HiddenValueCallback) displayFormStateChange.callbacks.get(1)).getId());

        Assert.assertEquals("3rd callback must be a HiddenValueCallback",
                HiddenValueCallback.class, displayFormStateChange.callbacks.get(2).getClass());
        Assert.assertEquals("3rd callback id must match Constants.DEVICE_TYPE_OUTPUT_VARIABLE",
                Constants.PATTERN_OUTPUT_VARIABLE, ((HiddenValueCallback) displayFormStateChange.callbacks.get(2)).getId());
    }

    @Test
    public void test_DisplayForm_Verify_PreviousOutcome_Retry() {
        when(config.displayMessage()).thenReturn(true);

        JsonValue sharedState = new JsonObject()
                .put(Constants.PREVIOUS_ACTION, ActionType.RETRY.getAction())
                .put(Constants.MESSAGE, "Authentication failed. Try again...")
                .build();

        state.setState(sharedState, new JsonObject().build(), new ArrayList<>());
        StateChange stateChange = useCase.displayForm(ActionType.VERIFY);

        Assert.assertNotNull("StateChange cannot be null", stateChange);
        Assert.assertEquals("StateChange must be an instance of DisplayFormStateChange", DisplayFormStateChange.class, stateChange.getClass());

        DisplayFormStateChange displayFormStateChange = (DisplayFormStateChange) stateChange;

        Assert.assertNotNull("callbacks cannot be null", displayFormStateChange.callbacks);
        Assert.assertEquals("callbacks must contain 5 callbacks", 5, displayFormStateChange.callbacks.size());

        Assert.assertEquals("1st callback must be a TextOutputCallback",
                TextOutputCallback.class, displayFormStateChange.callbacks.get(0).getClass());
        Assert.assertEquals("1st callback text must match 'Authentication failed. Try again...'",
                "Authentication failed. Try again...", ((TextOutputCallback) displayFormStateChange.callbacks.get(0)).getMessage());
    }

    @Test
    public void test_DisplayForm_Verify_PreviousOutcome_Enroll() {
        when(config.displayMessage()).thenReturn(true);

        JsonValue sharedState = new JsonObject()
                .put(Constants.PREVIOUS_ACTION, ActionType.ENROLL.getAction())
                .put(Constants.MESSAGE, "Not enough patterns to perform matching. We need to enroll 3 more.")
                .build();

        state.setState(sharedState, new JsonObject().build(), new ArrayList<>());
        StateChange stateChange = useCase.displayForm(ActionType.VERIFY);

        Assert.assertNotNull("StateChange cannot be null", stateChange);
        Assert.assertEquals("StateChange must be an instance of DisplayFormStateChange", DisplayFormStateChange.class, stateChange.getClass());

        DisplayFormStateChange displayFormStateChange = (DisplayFormStateChange) stateChange;

        Assert.assertNotNull("callbacks cannot be null", displayFormStateChange.callbacks);
        Assert.assertEquals("callbacks must contain 5 callbacks", 5, displayFormStateChange.callbacks.size());

        Assert.assertEquals("1st callback must be a TextOutputCallback",
                TextOutputCallback.class, displayFormStateChange.callbacks.get(0).getClass());
        Assert.assertEquals("1st callback text must match 'Not enough patterns to perform matching. We need to enroll 3 more.'",
                "Not enough patterns to perform matching. We need to enroll 3 more.",
                ((TextOutputCallback) displayFormStateChange.callbacks.get(0)).getMessage());
    }

    @Test
    public void test_DisplayForm_Verify_PreviousOutcome_EnrollPosition() {
        when(config.displayMessage()).thenReturn(true);

        JsonValue sharedState = new JsonObject()
                .put(Constants.PREVIOUS_ACTION, ActionType.ENROLL_POSITION.getAction())
                .put(Constants.MESSAGE, "Not enough patterns with this typing position. We need to enroll 3 more.")
                .build();

        state.setState(sharedState, new JsonObject().build(), new ArrayList<>());
        StateChange stateChange = useCase.displayForm(ActionType.VERIFY);

        Assert.assertNotNull("StateChange cannot be null", stateChange);
        Assert.assertEquals("StateChange must be an instance of DisplayFormStateChange", DisplayFormStateChange.class, stateChange.getClass());

        DisplayFormStateChange displayFormStateChange = (DisplayFormStateChange) stateChange;

        Assert.assertNotNull("callbacks cannot be null", displayFormStateChange.callbacks);
        Assert.assertEquals("callbacks must contain 5 callbacks", 5, displayFormStateChange.callbacks.size());

        Assert.assertEquals("1st callback must be a TextOutputCallback",
                TextOutputCallback.class, displayFormStateChange.callbacks.get(0).getClass());
        Assert.assertEquals("1st callback text must match 'Not enough patterns with this typing position. We need to enroll 3 more.'",
                "Not enough patterns with this typing position. We need to enroll 3 more.",
                ((TextOutputCallback) displayFormStateChange.callbacks.get(0)).getMessage());
    }

    @Test
    public void test_HandelForm_Verify() throws IOException {
        /** Set up **/
        JsonValue sharedState = new JsonObject()
                .build();

        JsonValue transientState = new JsonObject().build();

        List<Callback> callbacks = new ArrayList<>();
        callbacks.add(new ScriptTextOutputCallback("device type script"));
        callbacks.add(new HiddenValueCallback(Constants.DEVICE_TYPE_OUTPUT_VARIABLE, "0"));
        callbacks.add(new ScriptTextOutputCallback("pattern script"));
        callbacks.add(new HiddenValueCallback(Constants.PATTERN_OUTPUT_VARIABLE, "typing pattern"));
        callbacks.add(new HiddenValueCallback(Constants.TEXT_ID_OUTPUT_VARIABLE, "text id"));

        /** Test **/
        state.setState(sharedState, transientState, callbacks);

        StateChange stateChange = useCase.handleForm();
        Assert.assertNotNull("StateChange cannot be null", stateChange);
        Assert.assertEquals("StateChange must be an instance of SingleOutcomeStateChange", SingleOutcomeStateChange.class, stateChange.getClass());

        SingleOutcomeStateChange singleOutcomeStateChange = (SingleOutcomeStateChange) stateChange;
        verifySharedState(singleOutcomeStateChange.sharedState);
        verifyTransientState(singleOutcomeStateChange.transientState);
    }

    @Test
    public void test_isFormDisplayed_False() {
        /** Set up **/
        JsonValue sharedState = new JsonObject()
                .build();

        JsonValue transientState = new JsonObject().build();

        List<Callback> callbacks = new ArrayList<>();
        callbacks.add(new ScriptTextOutputCallback("device type script"));
        callbacks.add(new HiddenValueCallback(Constants.DEVICE_TYPE_OUTPUT_VARIABLE));
        callbacks.add(new ScriptTextOutputCallback("pattern script"));
        callbacks.add(new HiddenValueCallback(Constants.PATTERN_OUTPUT_VARIABLE));

        /** Test **/
        state.setState(sharedState, transientState, callbacks);

        Assert.assertFalse("form is not displayed", useCase.isFormDisplayed());
    }

    @Test
    public void test_isFormDisplayed_True() {
        /** Set up **/
        JsonValue sharedState = new JsonObject()
                .build();

        JsonValue transientState = new JsonObject().build();

        List<Callback> callbacks = new ArrayList<>();
        callbacks.add(new ScriptTextOutputCallback("device type script"));
        callbacks.add(new HiddenValueCallback(Constants.DEVICE_TYPE_OUTPUT_VARIABLE, "0"));
        callbacks.add(new ScriptTextOutputCallback("pattern script"));
        callbacks.add(new HiddenValueCallback(Constants.PATTERN_OUTPUT_VARIABLE, "typing pattern"));

        /** Test **/
        state.setState(sharedState, transientState, callbacks);

        Assert.assertTrue("form is displayed", useCase.isFormDisplayed());
    }

    private void verifySharedState(JsonValue sharedState) {
        Assert.assertNotNull("sharedState cannot be null", sharedState);
        Assert.assertEquals(String.format("sharedState must have %s key", 1), 1, sharedState.keys().size());
        Assert.assertEquals("DEVICE_TYPE should be 'DESKTOP'", DeviceType.DESKTOP.ordinal(), (int) sharedState.get(Constants.DEVICE_TYPE).asInteger());
    }

    private void verifyTransientState(JsonValue transientState) {
        Assert.assertNotNull("transientState cannot be null", transientState);
        Assert.assertEquals(String.format("transientState must have %s key", 2), 2, transientState.keys().size());
        Assert.assertEquals("TYPING_PATTERN should be 'typing pattern'", "typing pattern", transientState.get(Constants.TYPING_PATTERN).asString());
        Assert.assertEquals("TEXT_ID should be 'text id'", "text id", transientState.get(Constants.TEXT_ID).asString());
    }
}
