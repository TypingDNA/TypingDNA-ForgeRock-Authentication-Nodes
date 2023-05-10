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

import com.typingdna.core.statechanges.DisplayFormStateChange;
import com.typingdna.core.statechanges.SingleOutcomeStateChange;
import com.typingdna.core.statechanges.StateChange;
import com.typingdna.util.ConfigAdapter;
import com.typingdna.util.Constants;
import com.typingdna.util.State;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.utils.JsonObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.TextOutputCallback;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

public class ShortPhraseIntegrationTest {

    @Mock
    private ConfigAdapter config;

    @Before
    public void setUp() {
        config = mock(ConfigAdapter.class);

        when(config.textToEnter()).thenReturn("text to enter");
    }

    @Test
    public void test_DisplayForm() {
        State state = new State(new JsonObject().build(), new JsonObject().build(), new ArrayList<>());
        ShortPhrase useCase = new ShortPhrase(config, state);
        StateChange stateChange = useCase.displayForm();

        Assert.assertNotNull("StateChange cannot be null", stateChange);
        Assert.assertEquals("StateChange must be an instance of DisplayFormStateChange", DisplayFormStateChange.class, stateChange.getClass());

        DisplayFormStateChange displayFormStateChange = (DisplayFormStateChange) stateChange;

        Assert.assertNotNull("callbacks cannot be null", displayFormStateChange.callbacks);
        Assert.assertEquals("callbacks must contain 3 callbacks", 3, displayFormStateChange.callbacks.size());
        Assert.assertEquals("1st callback must be a TextOutputCallback",
                TextOutputCallback.class, displayFormStateChange.callbacks.get(0).getClass());
        Assert.assertEquals("2st callback must be a TextOutputCallback",
                TextOutputCallback.class, displayFormStateChange.callbacks.get(1).getClass());
        Assert.assertEquals("2nd callback must be a NameCallback",
                NameCallback.class, displayFormStateChange.callbacks.get(2).getClass());
    }

    @Test
    public void test_HandelForm_CheckUser_NoPatterns() throws IOException {
        JsonValue sharedState = new JsonObject()
                .put(Constants.TEXT_TO_ENTER, "text to enter")
                .put(Constants.TEXT_ID, "text id")
                .build();

        JsonValue transientState = new JsonObject().build();

        List<Callback> callbacks = new ArrayList<>();
        callbacks.add(new TextOutputCallback(TextOutputCallback.INFORMATION, "text to enter"));
        callbacks.add(new NameCallback("enter text above", "text to enter"));

        /** Test **/
        State state = new State(sharedState, transientState, callbacks);
        ShortPhrase useCase = new ShortPhrase(config, state);

        StateChange stateChange = useCase.handleForm();
        Assert.assertNotNull("StateChange cannot be null", stateChange);
        Assert.assertEquals("StateChange must be an instance of SingleOutcomeStateChange", SingleOutcomeStateChange.class, stateChange.getClass());

        SingleOutcomeStateChange singleOutcomeStateChange = (SingleOutcomeStateChange) stateChange;

        Assert.assertNotNull("sharedState cannot be null", singleOutcomeStateChange.sharedState);
        Assert.assertEquals("sharedState must have 2 keys", 2, singleOutcomeStateChange.sharedState.keys().size());
        Assert.assertEquals("textToEnter must be: \"text to enter\"", "text to enter",
                singleOutcomeStateChange.sharedState.get(Constants.TEXT_TO_ENTER).asString());
        Assert.assertEquals("textId must be: \"text id\"", "text id",
                singleOutcomeStateChange.sharedState.get(Constants.TEXT_ID).asString());
    }

    @Test
    public void test_isFormDisplayed_False() {
        JsonValue sharedState = new JsonObject()
                .build();

        JsonValue transientState = new JsonObject().build();

        List<Callback> callbacks = new ArrayList<>();
        callbacks.add(new TextOutputCallback(TextOutputCallback.INFORMATION, "text to enter"));
        callbacks.add(new NameCallback("enter text above"));

        /** Test **/
        State state = new State(sharedState, transientState, callbacks);
        ShortPhrase useCase = new ShortPhrase(config, state);

        Assert.assertFalse("form is not displayed", useCase.isFormDisplayed());
    }

    @Test
    public void test_isFormDisplayed_True() {
        JsonValue sharedState = new JsonObject()
                .build();

        JsonValue transientState = new JsonObject().build();

        List<Callback> callbacks = new ArrayList<>();
        callbacks.add(new TextOutputCallback(TextOutputCallback.INFORMATION, "text to enter"));
        NameCallback nameCallback = new NameCallback("enter text above");
        nameCallback.setName("text to enter");
        callbacks.add(nameCallback);

        /** Test **/
        State state = new State(sharedState, transientState, callbacks);
        ShortPhrase useCase = new ShortPhrase(config, state);

        Assert.assertTrue("form is displayed", useCase.isFormDisplayed());
    }
}
