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

import com.google.common.collect.ImmutableList;
import com.typingdna.core.AbstractCore.ActionType;
import com.typingdna.nodes.outcomeproviders.TypingDNADecisionOutcomeProvider.TypingDNAOutcome;
import com.typingdna.core.statechanges.ExitNodeStateChange;
import com.typingdna.core.statechanges.StateChange;
import com.typingdna.util.ConfigAdapter;
import com.typingdna.util.Constants;
import com.typingdna.util.TypingDNAApi;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.scripting.service.ScriptConfiguration;
import org.forgerock.openam.utils.JsonObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.security.auth.callback.Callback;
import java.util.ArrayList;
import java.util.List;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.mockito.Mockito.*;

public class DecisionIntegrationTest {

    @Mock
    private ScriptConfiguration scriptConfiguration;
    @Mock
    private ConfigAdapter config;
    @Mock
    private TypingDNAApi api;

    private Decision useCase;

    @Before
    public void setUp() {
        scriptConfiguration = mock(ScriptConfiguration.class);
        config = mock(ConfigAdapter.class);
        api = mock(TypingDNAApi.class);

        when(config.script()).thenReturn(scriptConfiguration);
        when(config.enrollmentsNecessary()).thenReturn(3);
        when(config.retries()).thenReturn(2);
        when(config.usernameSalt()).thenReturn("1234");
        when(config.matchScore()).thenReturn(70);
        when(config.autoEnrollScore()).thenReturn(90);
        when(config.requestIdentifier()).thenReturn("");
        when(scriptConfiguration.getScript()).thenReturn("script");

        useCase = new Decision(config, api);
    }

    @Test
    public void test_DisplayForm() {
        useCase.setCurrentState(new JsonObject().build(), new JsonObject().build(), new ArrayList<>());
        Assert.assertThrows(NoSuchMethodError.class, () -> useCase.displayForm());
        Assert.assertThrows(NoSuchMethodError.class, () -> useCase.displayForm(ActionType.VERIFY));
    }

    @Test
    public void test_HandleForm_Verify_Enroll_NotEnoughPatternsDesktop() throws NodeProcessException {
        /** SET UP **/
        JsonValue checkUserResponse = new JsonObject()
                .put("success", 1)
                .put("count", 1)
                .put("mobilecount", 1)
                .build();
        when(api.checkUser(AbstractCore.hashText("test_user", "1234"), TypingDNAApi.PatternType.SAME_TEXT, null, "")).thenReturn(checkUserResponse);

        JsonValue saveResponse = new JsonObject()
                .put("success", 1)
                .build();
        when(api.save(AbstractCore.hashText("test_user", "1234"), "typing pattern", "")).thenReturn(saveResponse);

        JsonValue sharedState = new JsonObject()
                .put(USERNAME, "test_user")
                .put(Constants.TYPING_PATTERN, "typing pattern")
                .put(Constants.DEVICE_TYPE, 0)
                .build();
        JsonValue transientState = new JsonObject().build();
        List<Callback> callbacks = ImmutableList.of();

        useCase.setCurrentState(sharedState, transientState, callbacks);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'enroll'", TypingDNAOutcome.ENROLL.name(), exitStateChange.outcome);

        verifySharedState(
                exitStateChange.sharedState,
                7,
                ActionType.ENROLL.getAction(),
                2,
                1,
                null,
                "typing pattern",
                null,
                null,
                AbstractCore.DeviceType.DESKTOP);
        checkTransientState(exitStateChange.transientState);
    }

    @Test
    public void test_HandleForm_Verify_Enroll_NotEnoughPatternsMobile() throws NodeProcessException {
        /** SET UP **/
        JsonValue checkUserResponse = new JsonObject()
                .put("success", 1)
                .put("count", 1)
                .put("mobilecount", 0)
                .build();
        when(api.checkUser(AbstractCore.hashText("test_user", "1234"), TypingDNAApi.PatternType.SAME_TEXT, null, "")).thenReturn(checkUserResponse);

        JsonValue saveResponse = new JsonObject()
                .put("success", 1)
                .build();
        when(api.save(AbstractCore.hashText("test_user", "1234"), "typing pattern", "")).thenReturn(saveResponse);

        JsonValue sharedState = new JsonObject()
                .put(USERNAME, "test_user")
                .put(Constants.TYPING_PATTERN, "typing pattern")
                .put(Constants.DEVICE_TYPE, 1)
                .build();
        JsonValue transientState = new JsonObject().build();
        List<Callback> callbacks = ImmutableList.of();

        useCase.setCurrentState(sharedState, transientState, callbacks);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'ENROLL'", TypingDNAOutcome.ENROLL.name(), exitStateChange.outcome);

        verifySharedState(
                exitStateChange.sharedState,
                7,
                ActionType.ENROLL.getAction(),
                1,
                2,
                null,
                "typing pattern",
                null,
                null,
                AbstractCore.DeviceType.MOBILE);
        checkTransientState(exitStateChange.transientState);
    }

    @Test
    public void test_HandleForm_Verify_Enroll_SaveFails() throws NodeProcessException {
        /** SET UP **/
        JsonValue checkUserResponse = new JsonObject()
                .put("success", 1)
                .put("count", 2)
                .put("mobilecount", 1)
                .build();
        when(api.checkUser(AbstractCore.hashText("test_user", "1234"), TypingDNAApi.PatternType.SAME_TEXT, null, "")).thenReturn(checkUserResponse);

        JsonValue saveResponse = new JsonObject()
                .put("success", 0)
                .build();
        when(api.save(AbstractCore.hashText("test_user", "1234"), "typing pattern", "")).thenReturn(saveResponse);

        JsonValue sharedState = new JsonObject()
                .put(USERNAME, "test_user")
                .put(Constants.TYPING_PATTERN, "typing pattern")
                .put(Constants.DEVICE_TYPE, 0)
                .build();
        JsonValue transientState = new JsonObject().build();
        List<Callback> callbacks = ImmutableList.of();

        useCase.setCurrentState(sharedState, transientState, callbacks);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'enroll'", TypingDNAOutcome.ENROLL.name(), exitStateChange.outcome);

        verifySharedState(
                exitStateChange.sharedState,
                7,
                ActionType.ENROLL.getAction(),
                2,
                1,
                null,
                "typing pattern",
                null,
                null,
                AbstractCore.DeviceType.DESKTOP);
        checkTransientState(exitStateChange.transientState);
    }

    @Test
    public void test_HandleForm_Verify_Verify_EnoughPatternsAfterSave_VerifyAfterEnroll() throws NodeProcessException {
        /** SET UP **/
        JsonValue checkUserResponse = new JsonObject()
                .put("success", 1)
                .put("count", 2)
                .put("mobilecount", 1)
                .build();
        when(api.checkUser(AbstractCore.hashText("test_user", "1234"), TypingDNAApi.PatternType.SAME_TEXT, null, "")).thenReturn(checkUserResponse);

        JsonValue saveResponse = new JsonObject()
                .put("success", 1)
                .build();
        when(api.save(AbstractCore.hashText("test_user", "1234"), "typing pattern", "")).thenReturn(saveResponse);

        JsonValue sharedState = new JsonObject()
                .put(USERNAME, "test_user")
                .put(Constants.TYPING_PATTERN, "typing pattern")
                .put(Constants.DEVICE_TYPE, 0)
                .build();
        JsonValue transientState = new JsonObject().build();
        List<Callback> callbacks = ImmutableList.of();

        when(config.verifyAfterEnroll()).thenReturn(true);

        useCase.setCurrentState(sharedState, transientState, callbacks);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'retry'", TypingDNAOutcome.RETRY.name(), exitStateChange.outcome);

        verifySharedState(
                exitStateChange.sharedState,
                7,
                ActionType.VERIFY.getAction(),
                3,
                0,
                null,
                "typing pattern",
                null,
                null,
                AbstractCore.DeviceType.DESKTOP);
        checkTransientState(exitStateChange.transientState);
    }

    @Test
    public void test_HandleForm_Verify_Verify_EnoughPatternsAfterSave_NoVerifyAfterEnroll() throws NodeProcessException {
        /** SET UP **/
        JsonValue checkUserResponse = new JsonObject()
                .put("success", 1)
                .put("count", 2)
                .put("mobilecount", 1)
                .build();
        when(api.checkUser(AbstractCore.hashText("test_user", "1234"), TypingDNAApi.PatternType.SAME_TEXT, null, "")).thenReturn(checkUserResponse);

        JsonValue saveResponse = new JsonObject()
                .put("success", 1)
                .build();
        when(api.save(AbstractCore.hashText("test_user", "1234"), "typing pattern", "")).thenReturn(saveResponse);

        JsonValue sharedState = new JsonObject()
                .put(USERNAME, "test_user")
                .put(Constants.TYPING_PATTERN, "typing pattern")
                .put(Constants.DEVICE_TYPE, 0)
                .build();
        JsonValue transientState = new JsonObject().build();
        List<Callback> callbacks = ImmutableList.of();

        when(config.verifyAfterEnroll()).thenReturn(false);

        useCase.setCurrentState(sharedState, transientState, callbacks);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'MATCH'", TypingDNAOutcome.MATCH.name(), exitStateChange.outcome);

        verifySharedState(
                exitStateChange.sharedState,
                5,
                null,
                3,
                0,
                null,
                "typing pattern",
                null,
                null,
                AbstractCore.DeviceType.DESKTOP);
        checkTransientState(exitStateChange.transientState);
    }

    @Test
    public void test_HandleForm_Verify_Match() throws NodeProcessException {
        /** SET UP **/
        JsonValue checkUserResponse = new JsonObject()
                .put("success", 1)
                .put("count", 3)
                .put("mobilecount", 3)
                .build();
        when(api.checkUser(AbstractCore.hashText("test_user", "1234"), TypingDNAApi.PatternType.SAME_TEXT, null, "")).thenReturn(checkUserResponse);

        JsonValue verifyResponse = new JsonObject()
                .put("message_code", 0)
                .put("result", 1)
                .put("score", 85)
                .put("net_score", 71)
                .put("action", "")
                .build();
        when(api.verify(AbstractCore.hashText("test_user", "1234"), "typing pattern", "")).thenReturn(verifyResponse);

        JsonValue sharedState = new JsonObject()
                .put(USERNAME, "test_user")
                .put(Constants.TYPING_PATTERN, "typing pattern")
                .put(Constants.DEVICE_TYPE, 0)
                .build();
        JsonValue transientState = new JsonObject().build();
        List<Callback> callbacks = ImmutableList.of();

        useCase.setCurrentState(sharedState, transientState, callbacks);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'match'", TypingDNAOutcome.MATCH.name(), exitStateChange.outcome);

        verifySharedState(
                exitStateChange.sharedState,
                3,
                null,
                null,
                null,
                null,
                "typing pattern",
                null,
                null,
                AbstractCore.DeviceType.DESKTOP);
        checkTransientState(exitStateChange.transientState);
    }

    @Test
    public void test_HandleForm_Verify_Match_AutoEnrolled() throws NodeProcessException {
        /** SET UP **/
        JsonValue checkUserResponse = new JsonObject()
                .put("success", 1)
                .put("count", 3)
                .put("mobilecount", 3)
                .build();
        when(api.checkUser(AbstractCore.hashText("test_user", "1234"), TypingDNAApi.PatternType.SAME_TEXT, null, "")).thenReturn(checkUserResponse);

        JsonValue verifyResponse = new JsonObject()
                .put("message_code", 0)
                .put("result", 1)
                .put("score", 95)
                .put("net_score", 80)
                .put("action", "")
                .build();
        when(api.verify(AbstractCore.hashText("test_user", "1234"), "typing pattern", "")).thenReturn(verifyResponse);

        JsonValue saveResponse = new JsonObject()
                .put("success", 1)
                .build();
        when(api.save(AbstractCore.hashText("test_user", "1234"), "typing pattern", "")).thenReturn(saveResponse);

        JsonValue sharedState = new JsonObject()
                .put(USERNAME, "test_user")
                .put(Constants.TYPING_PATTERN, "typing pattern")
                .put(Constants.DEVICE_TYPE, 0)
                .build();
        JsonValue transientState = new JsonObject().build();
        List<Callback> callbacks = ImmutableList.of();

        useCase.setCurrentState(sharedState, transientState, callbacks);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'match'", TypingDNAOutcome.MATCH.name(), exitStateChange.outcome);

        verifySharedState(
                exitStateChange.sharedState,
                5,
                null,
                1,
                2,
                null,
                "typing pattern",
                null,
                null,
                AbstractCore.DeviceType.DESKTOP);
        checkTransientState(exitStateChange.transientState);
    }

    @Test
    public void test_HandleForm_Verify_Match_SaveFailsNoAutoEnroll() throws NodeProcessException {
        /** SET UP **/
        JsonValue checkUserResponse = new JsonObject()
                .put("success", 1)
                .put("count", 3)
                .put("mobilecount", 3)
                .build();
        when(api.checkUser(AbstractCore.hashText("test_user", "1234"), TypingDNAApi.PatternType.SAME_TEXT, null, "")).thenReturn(checkUserResponse);

        JsonValue verifyResponse = new JsonObject()
                .put("message_code", 0)
                .put("result", 1)
                .put("score", 80)
                .put("net_score", 71)
                .put("action", "")
                .build();
        when(api.verify(AbstractCore.hashText("test_user", "1234"), "typing pattern", "")).thenReturn(verifyResponse);

        JsonValue saveResponse = new JsonObject()
                .put("success", 0)
                .build();
        when(api.save(AbstractCore.hashText("test_user", "1234"), "typing pattern", "")).thenReturn(saveResponse);

        JsonValue sharedState = new JsonObject()
                .put(USERNAME, "test_user")
                .put(Constants.TYPING_PATTERN, "typing pattern")
                .put(Constants.DEVICE_TYPE, 0)
                .build();
        JsonValue transientState = new JsonObject().build();
        List<Callback> callbacks = ImmutableList.of();

        useCase.setCurrentState(sharedState, transientState, callbacks);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'match'", TypingDNAOutcome.MATCH.name(), exitStateChange.outcome);

        verifySharedState(
                exitStateChange.sharedState,
                3,
                null,
                null,
                null,
                null,
                "typing pattern",
                null,
                null,
                AbstractCore.DeviceType.DESKTOP);
        checkTransientState(exitStateChange.transientState);
    }

    @Test
    public void test_HandleForm_Verify_NoMatch_Retry() throws NodeProcessException {
        /** SET UP **/
        JsonValue checkUserResponse = new JsonObject()
                .put("success", 1)
                .put("count", 3)
                .put("mobilecount", 3)
                .build();
        when(api.checkUser(AbstractCore.hashText("test_user", "1234"), TypingDNAApi.PatternType.SAME_TEXT, null, "")).thenReturn(checkUserResponse);

        JsonValue verifyResponse = new JsonObject()
                .put("message_code", 0)
                .put("result", 1)
                .put("score", 50)
                .put("net_score", 30)
                .put("action", "")
                .build();
        when(api.verify(AbstractCore.hashText("test_user", "1234"), "typing pattern", "")).thenReturn(verifyResponse);

        JsonValue sharedState = new JsonObject()
                .put(USERNAME, "test_user")
                .put(Constants.TYPING_PATTERN, "typing pattern")
                .put(Constants.DEVICE_TYPE, 0)
                .build();
        JsonValue transientState = new JsonObject().build();
        List<Callback> callbacks = ImmutableList.of();

        useCase.setCurrentState(sharedState, transientState, callbacks);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'retry'", TypingDNAOutcome.RETRY.name(), exitStateChange.outcome);

        verifySharedState(
                exitStateChange.sharedState,
                7,
                ActionType.RETRY.getAction(),
                null,
                null,
                1,
                "typing pattern",
                "typing pattern",
                null,
                AbstractCore.DeviceType.DESKTOP);
        checkTransientState(exitStateChange.transientState);
    }

    @Test
    public void test_HandleForm_Verify_NoMatch_NoRetry() throws NodeProcessException {
        /** SET UP **/
        JsonValue checkUserResponse = new JsonObject()
                .put("success", 1)
                .put("count", 3)
                .put("mobilecount", 3)
                .build();
        when(api.checkUser(AbstractCore.hashText("test_user", "1234"), TypingDNAApi.PatternType.SAME_TEXT, null, "")).thenReturn(checkUserResponse);

        JsonValue verifyResponse = new JsonObject()
                .put("message_code", 0)
                .put("result", 1)
                .put("score", 20)
                .put("net_score", 5)
                .put("action", "")
                .build();
        when(api.verify(AbstractCore.hashText("test_user", "1234"), "typing pattern", "")).thenReturn(verifyResponse);

        JsonValue sharedState = new JsonObject()
                .put(USERNAME, "test_user")
                .put(Constants.TYPING_PATTERN, "typing pattern")
                .put(Constants.DEVICE_TYPE, 0)
                .put(Constants.VERIFY_RETRIES, 2)
                .build();
        JsonValue transientState = new JsonObject().build();
        List<Callback> callbacks = ImmutableList.of();

        useCase.setCurrentState(sharedState, transientState, callbacks);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'no match'", TypingDNAOutcome.NO_MATCH.name(), exitStateChange.outcome);

        verifySharedState(
                exitStateChange.sharedState,
                4,
                null,
                null,
                null,
                2,
                "typing pattern",
                null,
                null,
                AbstractCore.DeviceType.DESKTOP);
        checkTransientState(exitStateChange.transientState);
    }

    @Test
    public void test_HandleForm_Verify_Enroll_MessageCode4() throws NodeProcessException {
        /** SET UP **/
        JsonValue checkUserResponse = new JsonObject()
                .put("success", 1)
                .put("count", 3)
                .put("mobilecount", 3)
                .build();
        when(api.checkUser(AbstractCore.hashText("test_user", "1234"), TypingDNAApi.PatternType.SAME_TEXT, null, "")).thenReturn(checkUserResponse);

        JsonValue verifyResponse = new JsonObject()
                .put("message_code", 4)
                .put("result", 0)
                .put("action", "")
                .build();
        when(api.verify(AbstractCore.hashText("test_user", "1234"), "typing pattern", "")).thenReturn(verifyResponse);

        JsonValue saveResponse = new JsonObject()
                .put("success", 1)
                .build();
        when(api.save(AbstractCore.hashText("test_user", "1234"), "typing pattern", "")).thenReturn(saveResponse);

        JsonValue sharedState = new JsonObject()
                .put(USERNAME, "test_user")
                .put(Constants.TYPING_PATTERN, "typing pattern")
                .put(Constants.DEVICE_TYPE, 1)
                .build();
        JsonValue transientState = new JsonObject().build();
        List<Callback> callbacks = ImmutableList.of();

        useCase.setCurrentState(sharedState, transientState, callbacks);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'enroll'", TypingDNAOutcome.ENROLL.name(), exitStateChange.outcome);

        verifySharedState(
                exitStateChange.sharedState,
                7,
                ActionType.ENROLL.getAction(),
                1,
                2,
                null,
                "typing pattern",
                null,
                null,
                AbstractCore.DeviceType.MOBILE);
        checkTransientState(exitStateChange.transientState);
    }

    @Test
    public void test_HandleForm_Verify_EnrollPositions_PreviouslyEnoughPatterns() throws NodeProcessException {
        /** SET UP **/
        JsonValue checkUserResponse = new JsonObject()
                .put("success", 1)
                .put("count", 5)
                .put("mobilecount", 8)
                .build();
        when(api.checkUser(AbstractCore.hashText("test_user", "1234"), TypingDNAApi.PatternType.SAME_TEXT, null, "")).thenReturn(checkUserResponse);

        JsonValue verifyResponse = new JsonObject()
                .put("message_code", 3)
                .put("result", 0)
                .put("action", "")
                .build();
        when(api.verify(AbstractCore.hashText("test_user", "1234"), "typing pattern", "")).thenReturn(verifyResponse);

        JsonValue saveResponse = new JsonObject()
                .put("success", 1)
                .build();
        when(api.save(AbstractCore.hashText("test_user", "1234"), "typing pattern", "")).thenReturn(saveResponse);

        JsonValue sharedState = new JsonObject()
                .put(USERNAME, "test_user")
                .put(Constants.TYPING_PATTERN, "typing pattern")
                .put(Constants.DEVICE_TYPE, 1)
                .build();
        JsonValue transientState = new JsonObject().build();
        List<Callback> callbacks = ImmutableList.of();

        useCase.setCurrentState(sharedState, transientState, callbacks);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'enroll'", TypingDNAOutcome.ENROLL.name(), exitStateChange.outcome);

        verifySharedState(
                exitStateChange.sharedState,
                7,
                ActionType.ENROLL_POSITION.getAction(),
                1,
                2,
                null,
                "typing pattern",
                null,
                null,
                AbstractCore.DeviceType.MOBILE);
        checkTransientState(exitStateChange.transientState);
    }

    @Test
    public void test_HandleForm_Verify_Enroll_MessageCode3() throws NodeProcessException {
        /** SET UP **/
        JsonValue checkUserResponse = new JsonObject()
                .put("success", 1)
                .put("count", 3)
                .put("mobilecount", 3)
                .build();
        when(api.checkUser(AbstractCore.hashText("test_user", "1234"), TypingDNAApi.PatternType.SAME_TEXT, null, "")).thenReturn(checkUserResponse);

        JsonValue verifyResponse = new JsonObject()
                .put("message_code", 3)
                .put("result", 0)
                .put("action", "")
                .build();
        when(api.verify(AbstractCore.hashText("test_user", "1234"), "typing pattern", "")).thenReturn(verifyResponse);

        JsonValue saveResponse = new JsonObject()
                .put("success", 1)
                .build();
        when(api.save(AbstractCore.hashText("test_user", "1234"), "typing pattern", "")).thenReturn(saveResponse);

        JsonValue sharedState = new JsonObject()
                .put(USERNAME, "test_user")
                .put(Constants.TYPING_PATTERN, "typing pattern")
                .put(Constants.DEVICE_TYPE, 1)
                .build();
        JsonValue transientState = new JsonObject().build();
        List<Callback> callbacks = ImmutableList.of();

        useCase.setCurrentState(sharedState, transientState, callbacks);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'enroll'", TypingDNAOutcome.ENROLL.name(), exitStateChange.outcome);

        verifySharedState(
                exitStateChange.sharedState,
                7,
                ActionType.ENROLL_POSITION.getAction(),
                1,
                2,
                null,
                "typing pattern",
                null,
                null,
                AbstractCore.DeviceType.MOBILE);
        checkTransientState(exitStateChange.transientState);
    }

    @Test
    public void test_HandleForm_Verify_InvalidCredentials() throws NodeProcessException {
        /** SET UP **/
        JsonValue checkUserResponse = new JsonObject()
                .put("success", 1)
                .put("count", 3)
                .put("mobilecount", 3)
                .build();
        when(api.checkUser(AbstractCore.hashText("test_user", "1234"), TypingDNAApi.PatternType.SAME_TEXT, null, "")).thenReturn(checkUserResponse);

        JsonValue verifyResponse = new JsonObject()
                .put("status", 403)
                .put("message_code", 33)
                .build();
        when(api.verify(AbstractCore.hashText("test_user", "1234"), "typing pattern", "")).thenReturn(verifyResponse);

        JsonValue sharedState = new JsonObject()
                .put(USERNAME, "test_user")
                .put(Constants.TYPING_PATTERN, "typing pattern")
                .put(Constants.DEVICE_TYPE, 0)
                .build();
        JsonValue transientState = new JsonObject().build();
        List<Callback> callbacks = ImmutableList.of();

        useCase.setCurrentState(sharedState, transientState, callbacks);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'failure'", TypingDNAOutcome.FAIL.name(), exitStateChange.outcome);

        verifySharedState(
                exitStateChange.sharedState,
                3,
                null,
                null,
                null,
                null,
                "typing pattern",
                null,
                null,
                AbstractCore.DeviceType.DESKTOP);
        checkTransientState(exitStateChange.transientState);
    }

    @Test
    public void test_HandleForm_Verify_InvalidTypingPattern() throws NodeProcessException {
        /** SET UP **/
        JsonValue checkUserResponse = new JsonObject()
                .put("success", 1)
                .put("count", 3)
                .put("mobilecount", 3)
                .build();
        when(api.checkUser(AbstractCore.hashText("test_user", "1234"), TypingDNAApi.PatternType.SAME_TEXT, null, "")).thenReturn(checkUserResponse);

        JsonValue verifyResponse = new JsonObject()
                .put("status", 445)
                .put("message_code", 36)
                .build();
        when(api.verify(AbstractCore.hashText("test_user", "1234"), "typing pattern", "")).thenReturn(verifyResponse);

        JsonValue sharedState = new JsonObject()
                .put(USERNAME, "test_user")
                .put(Constants.TYPING_PATTERN, "typing pattern")
                .put(Constants.DEVICE_TYPE, 0)
                .build();
        JsonValue transientState = new JsonObject().build();
        List<Callback> callbacks = ImmutableList.of();

        useCase.setCurrentState(sharedState, transientState, callbacks);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'retry'", TypingDNAOutcome.RETRY.name(), exitStateChange.outcome);

        verifySharedState(
                exitStateChange.sharedState,
                6,
                ActionType.RETRY.getAction(),
                null,
                null,
                1,
                "typing pattern",
                null,
                null,
                AbstractCore.DeviceType.DESKTOP);
        checkTransientState(exitStateChange.transientState);
    }

    @Test
    public void test_HandleForm_Verify_InvalidTypingPattern_NoRetriesLeft() throws NodeProcessException {
        /** SET UP **/
        JsonValue checkUserResponse = new JsonObject()
                .put("success", 1)
                .put("count", 3)
                .put("mobilecount", 3)
                .build();
        when(api.checkUser(AbstractCore.hashText("test_user", "1234"), TypingDNAApi.PatternType.SAME_TEXT, null, "")).thenReturn(checkUserResponse);

        JsonValue verifyResponse = new JsonObject()
                .put("status", 445)
                .put("message_code", 36)
                .build();
        when(api.verify(AbstractCore.hashText("test_user", "1234"), "typing pattern", "")).thenReturn(verifyResponse);

        JsonValue sharedState = new JsonObject()
                .put(USERNAME, "test_user")
                .put(Constants.TYPING_PATTERN, "typing pattern")
                .put(Constants.DEVICE_TYPE, 0)
                .put(Constants.VERIFY_RETRIES, 3)
                .build();
        JsonValue transientState = new JsonObject().build();
        List<Callback> callbacks = ImmutableList.of();

        useCase.setCurrentState(sharedState, transientState, callbacks);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'failure'", TypingDNAOutcome.FAIL.name(), exitStateChange.outcome);

        verifySharedState(
                exitStateChange.sharedState,
                4,
                null,
                null,
                null,
                3,
                "typing pattern",
                null,
                null,
                AbstractCore.DeviceType.DESKTOP);
        checkTransientState(exitStateChange.transientState);
    }

    @Test
    public void test_HandleForm_Enroll_Enroll_SaveFails() throws NodeProcessException {
        /** SET UP **/
        JsonValue saveResponse = new JsonObject()
                .put("success", 0)
                .build();
        when(api.save(AbstractCore.hashText("test_user", "1234"), "typing pattern", "")).thenReturn(saveResponse);

        JsonValue sharedState = new JsonObject()
                .put(Constants.PREVIOUS_ACTION, ActionType.ENROLL.getAction())
                .put(USERNAME, "test_user")
                .put(Constants.TYPING_PATTERN, "typing pattern")
                .put(Constants.DEVICE_TYPE, 0)
                .put(Constants.PATTERNS_ENROLLED, 2)
                .put(Constants.ENROLLMENTS_LEFT, 1)
                .build();
        JsonValue transientState = new JsonObject().build();
        List<Callback> callbacks = ImmutableList.of();

        useCase.setCurrentState(sharedState, transientState, callbacks);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'enroll'", TypingDNAOutcome.ENROLL.name(), exitStateChange.outcome);

        verifySharedState(
                exitStateChange.sharedState,
                7,
                ActionType.ENROLL.getAction(),
                2,
                1,
                null,
                "typing pattern",
                null,
                null,
                AbstractCore.DeviceType.DESKTOP);
        checkTransientState(exitStateChange.transientState);
    }

    @Test
    public void test_HandleForm_Enroll_Enroll_SaveDoneNotEnoughPatterns() throws NodeProcessException {
        /** SET UP **/
        JsonValue saveResponse = new JsonObject()
                .put("success", 1)
                .build();
        when(api.save(AbstractCore.hashText("test_user", "1234"), "typing pattern", "")).thenReturn(saveResponse);

        JsonValue sharedState = new JsonObject()
                .put(Constants.PREVIOUS_ACTION, ActionType.ENROLL.getAction())
                .put(USERNAME, "test_user")
                .put(Constants.TYPING_PATTERN, "typing pattern")
                .put(Constants.DEVICE_TYPE, 0)
                .put(Constants.PATTERNS_ENROLLED, 1)
                .put(Constants.ENROLLMENTS_LEFT, 2)
                .build();
        JsonValue transientState = new JsonObject().build();
        List<Callback> callbacks = ImmutableList.of();

        useCase.setCurrentState(sharedState, transientState, callbacks);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'enroll'", TypingDNAOutcome.ENROLL.name(), exitStateChange.outcome);

        verifySharedState(
                exitStateChange.sharedState,
                7,
                ActionType.ENROLL.getAction(),
                2,
                1,
                null,
                "typing pattern",
                null,
                null,
                AbstractCore.DeviceType.DESKTOP);
        checkTransientState(exitStateChange.transientState);
    }

    @Test
    public void test_HandleForm_Enroll_Verify_EnoughPatternsAfterSave() throws NodeProcessException {
        /** SET UP **/
        JsonValue saveResponse = new JsonObject()
                .put("success", 1)
                .build();
        when(api.save(AbstractCore.hashText("test_user", "1234"), "typing pattern", "")).thenReturn(saveResponse);

        JsonValue sharedState = new JsonObject()
                .put(Constants.PREVIOUS_ACTION, ActionType.ENROLL.getAction())
                .put(USERNAME, "test_user")
                .put(Constants.TYPING_PATTERN, "typing pattern")
                .put(Constants.DEVICE_TYPE, 0)
                .put(Constants.PATTERNS_ENROLLED, 2)
                .put(Constants.ENROLLMENTS_LEFT, 1)
                .build();
        JsonValue transientState = new JsonObject().build();
        List<Callback> callbacks = ImmutableList.of();

        when(config.verifyAfterEnroll()).thenReturn(true);

        useCase.setCurrentState(sharedState, transientState, callbacks);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'retry'", TypingDNAOutcome.RETRY.name(), exitStateChange.outcome);

        verifySharedState(
                exitStateChange.sharedState,
                7,
                ActionType.VERIFY.getAction(),
                3,
                0,
                null,
                "typing pattern",
                null,
                null,
                AbstractCore.DeviceType.DESKTOP);
        checkTransientState(exitStateChange.transientState);
    }

    @Test
    public void test_HandleForm_Enroll_Exit_EnoughPatternsAfterSave() throws NodeProcessException {
        /** SET UP **/
        JsonValue saveResponse = new JsonObject()
                .put("success", 1)
                .build();
        when(api.save(AbstractCore.hashText("test_user", "1234"), "typing pattern", "")).thenReturn(saveResponse);

        JsonValue sharedState = new JsonObject()
                .put(Constants.PREVIOUS_ACTION, ActionType.ENROLL.getAction())
                .put(USERNAME, "test_user")
                .put(Constants.TYPING_PATTERN, "typing pattern")
                .put(Constants.DEVICE_TYPE, 0)
                .put(Constants.PATTERNS_ENROLLED, 2)
                .put(Constants.ENROLLMENTS_LEFT, 1)
                .build();
        JsonValue transientState = new JsonObject().build();
        List<Callback> callbacks = ImmutableList.of();

        when(config.verifyAfterEnroll()).thenReturn(false);

        useCase.setCurrentState(sharedState, transientState, callbacks);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'MATCH'", TypingDNAOutcome.MATCH.name(), exitStateChange.outcome);

        verifySharedState(
                exitStateChange.sharedState,
                7,
                ActionType.ENROLL.getAction(),
                3,
                0,
                null,
                "typing pattern",
                null,
                null,
                AbstractCore.DeviceType.DESKTOP);
        checkTransientState(exitStateChange.transientState);
    }

    @Test
    public void test_HandleForm_EnrollPosition_EnrollPosition_SaveFails() throws NodeProcessException {
        /** SET UP **/
        JsonValue saveResponse = new JsonObject()
                .put("success", 0)
                .build();
        when(api.save(AbstractCore.hashText("test_user", "1234"), "typing pattern", "")).thenReturn(saveResponse);

        JsonValue sharedState = new JsonObject()
                .put(Constants.PREVIOUS_ACTION, ActionType.ENROLL_POSITION.getAction())
                .put(USERNAME, "test_user")
                .put(Constants.TYPING_PATTERN, "typing pattern")
                .put(Constants.DEVICE_TYPE, 1)
                .put(Constants.PATTERNS_ENROLLED, 1)
                .put(Constants.ENROLLMENTS_LEFT, 2)
                .build();
        JsonValue transientState = new JsonObject().build();
        List<Callback> callbacks = ImmutableList.of();

        useCase.setCurrentState(sharedState, transientState, callbacks);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'enroll'", TypingDNAOutcome.ENROLL.name(), exitStateChange.outcome);

        verifySharedState(
                exitStateChange.sharedState,
                7,
                ActionType.ENROLL_POSITION.getAction(),
                1,
                2,
                null,
                "typing pattern",
                null,
                null,
                AbstractCore.DeviceType.MOBILE);
        checkTransientState(exitStateChange.transientState);
    }

    @Test
    public void test_HandleForm_EnrollPosition_EnrollPosition_SaveDoneNotEnoughPatterns() throws NodeProcessException {
        /** SET UP **/
        JsonValue saveResponse = new JsonObject()
                .put("success", 1)
                .build();
        when(api.save(AbstractCore.hashText("test_user", "1234"), "typing pattern", "")).thenReturn(saveResponse);

        JsonValue sharedState = new JsonObject()
                .put(Constants.PREVIOUS_ACTION, ActionType.ENROLL_POSITION.getAction())
                .put(USERNAME, "test_user")
                .put(Constants.TYPING_PATTERN, "typing pattern")
                .put(Constants.DEVICE_TYPE, 1)
                .put(Constants.PATTERNS_ENROLLED, 0)
                .put(Constants.ENROLLMENTS_LEFT, 3)
                .build();
        JsonValue transientState = new JsonObject().build();
        List<Callback> callbacks = ImmutableList.of();

        useCase.setCurrentState(sharedState, transientState, callbacks);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'enroll'", TypingDNAOutcome.ENROLL.name(), exitStateChange.outcome);

        verifySharedState(
                exitStateChange.sharedState,
                7,
                ActionType.ENROLL_POSITION.getAction(),
                1,
                2,
                null,
                "typing pattern",
                null,
                null,
                AbstractCore.DeviceType.MOBILE);
        checkTransientState(exitStateChange.transientState);
    }

    @Test
    public void test_HandleForm_EnrollPosition_Verify_EnoughPatternsAfterSave() throws NodeProcessException {
        /** SET UP **/
        JsonValue saveResponse = new JsonObject()
                .put("success", 1)
                .build();
        when(api.save(AbstractCore.hashText("test_user", "1234"), "typing pattern", "")).thenReturn(saveResponse);

        JsonValue sharedState = new JsonObject()
                .put(Constants.PREVIOUS_ACTION, ActionType.ENROLL_POSITION.getAction())
                .put(USERNAME, "test_user")
                .put(Constants.TYPING_PATTERN, "typing pattern")
                .put(Constants.DEVICE_TYPE, 1)
                .put(Constants.PATTERNS_ENROLLED, 2)
                .put(Constants.ENROLLMENTS_LEFT, 1)
                .build();
        JsonValue transientState = new JsonObject().build();
        List<Callback> callbacks = ImmutableList.of();

        when(config.verifyAfterEnroll()).thenReturn(true);

        useCase.setCurrentState(sharedState, transientState, callbacks);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'retry'", TypingDNAOutcome.RETRY.name(), exitStateChange.outcome);

        verifySharedState(
                exitStateChange.sharedState,
                7,
                ActionType.VERIFY.getAction(),
                3,
                0,
                null,
                "typing pattern",
                null,
                null,
                AbstractCore.DeviceType.MOBILE);
        checkTransientState(exitStateChange.transientState);
    }

    @Test
    public void test_HandleForm_EnrollPosition_Exit_EnoughPatternsAfterSave() throws NodeProcessException {
        /** SET UP **/
        JsonValue saveResponse = new JsonObject()
                .put("success", 1)
                .build();
        when(api.save(AbstractCore.hashText("test_user", "1234"), "typing pattern", "")).thenReturn(saveResponse);

        JsonValue sharedState = new JsonObject()
                .put(Constants.PREVIOUS_ACTION, ActionType.ENROLL_POSITION.getAction())
                .put(USERNAME, "test_user")
                .put(Constants.TYPING_PATTERN, "typing pattern")
                .put(Constants.DEVICE_TYPE, 1)
                .put(Constants.PATTERNS_ENROLLED, 2)
                .put(Constants.ENROLLMENTS_LEFT, 1)
                .build();
        JsonValue transientState = new JsonObject().build();
        List<Callback> callbacks = ImmutableList.of();

        when(config.verifyAfterEnroll()).thenReturn(false);

        useCase.setCurrentState(sharedState, transientState, callbacks);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'match'", TypingDNAOutcome.MATCH.name(), exitStateChange.outcome);

        verifySharedState(
                exitStateChange.sharedState,
                7,
                ActionType.ENROLL_POSITION.getAction(),
                3,
                0,
                null,
                "typing pattern",
                null,
                null,
                AbstractCore.DeviceType.MOBILE);
        checkTransientState(exitStateChange.transientState);
    }

    @Test
    public void test_HandleForm_Retry_NoMatch_Retry() throws NodeProcessException {
        /** SET UP **/
        JsonValue checkUserResponse = new JsonObject()
                .put("success", 1)
                .put("count", 3)
                .put("mobilecount", 3)
                .build();
        when(api.checkUser(AbstractCore.hashText("test_user", "1234"), TypingDNAApi.PatternType.SAME_TEXT, null, "")).thenReturn(checkUserResponse);

        JsonValue verifyResponse = new JsonObject()
                .put("message_code", 0)
                .put("result", 1)
                .put("score", 20)
                .put("net_score", 20)
                .put("action", "")
                .build();
        when(api.verify(AbstractCore.hashText("test_user", "1234"), "typing pattern;previous typing pattern", "")).thenReturn(verifyResponse);

        JsonValue sharedState = new JsonObject()
                .put(USERNAME, "test_user")
                .put(Constants.TYPING_PATTERN, "typing pattern")
                .put(Constants.DEVICE_TYPE, 0)
                .put(Constants.PREVIOUS_ACTION, ActionType.RETRY.getAction())
                .put(Constants.PREVIOUS_TYPING_PATTERNS, "previous typing pattern")
                .put(Constants.VERIFY_RETRIES, 1)
                .build();
        JsonValue transientState = new JsonObject().build();
        List<Callback> callbacks = ImmutableList.of();

        useCase.setCurrentState(sharedState, transientState, callbacks);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'RETRY'", TypingDNAOutcome.RETRY.name(), exitStateChange.outcome);

        verifySharedState(
                exitStateChange.sharedState,
                7,
                ActionType.RETRY.getAction(),
                null,
                null,
                2,
                "typing pattern",
                "typing pattern;previous typing pattern",
                null,
                AbstractCore.DeviceType.DESKTOP);
        checkTransientState(exitStateChange.transientState);
    }

    @Test
    public void test_HandleForm_Retry_NoMatch_NoRetriesLeft() throws NodeProcessException {
        /** SET UP **/
        JsonValue checkUserResponse = new JsonObject()
                .put("success", 1)
                .put("count", 3)
                .put("mobilecount", 3)
                .build();
        when(api.checkUser(AbstractCore.hashText("test_user", "1234"), TypingDNAApi.PatternType.SAME_TEXT, null, "")).thenReturn(checkUserResponse);

        JsonValue verifyResponse = new JsonObject()
                .put("message_code", 0)
                .put("result", 1)
                .put("score", 20)
                .put("net_score", 20)
                .put("action", "")
                .build();
        when(api.verify(AbstractCore.hashText("test_user", "1234"), "typing pattern;previous typing pattern", "")).thenReturn(verifyResponse);

        JsonValue sharedState = new JsonObject()
                .put(USERNAME, "test_user")
                .put(Constants.TYPING_PATTERN, "typing pattern")
                .put(Constants.DEVICE_TYPE, 0)
                .put(Constants.PREVIOUS_ACTION, ActionType.RETRY.getAction())
                .put(Constants.PREVIOUS_TYPING_PATTERNS, "previous typing pattern")
                .put(Constants.VERIFY_RETRIES, 2)
                .build();
        JsonValue transientState = new JsonObject().build();
        List<Callback> callbacks = ImmutableList.of();

        useCase.setCurrentState(sharedState, transientState, callbacks);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'NO MATCH'", TypingDNAOutcome.NO_MATCH.name(), exitStateChange.outcome);

        verifySharedState(
                exitStateChange.sharedState,
                7,
                ActionType.RETRY.getAction(),
                null,
                null,
                2,
                "typing pattern",
                "previous typing pattern",
                null,
                AbstractCore.DeviceType.DESKTOP);
        checkTransientState(exitStateChange.transientState);
    }

    @Test
    public void test_HandleForm_Retry_Match() throws NodeProcessException {
        /** SET UP **/
        JsonValue checkUserResponse = new JsonObject()
                .put("success", 1)
                .put("count", 3)
                .put("mobilecount", 3)
                .build();
        when(api.checkUser(AbstractCore.hashText("test_user", "1234"), TypingDNAApi.PatternType.SAME_TEXT, null, "")).thenReturn(checkUserResponse);

        JsonValue verifyResponse = new JsonObject()
                .put("message_code", 0)
                .put("result", 1)
                .put("score", 99)
                .put("net_score", 99)
                .put("action", "")
                .build();
        when(api.verify(AbstractCore.hashText("test_user", "1234"), "typing pattern;previous typing pattern", "")).thenReturn(verifyResponse);

        JsonValue sharedState = new JsonObject()
                .put(USERNAME, "test_user")
                .put(Constants.TYPING_PATTERN, "typing pattern")
                .put(Constants.DEVICE_TYPE, 0)
                .put(Constants.PREVIOUS_ACTION, ActionType.RETRY.getAction())
                .put(Constants.PREVIOUS_TYPING_PATTERNS, "previous typing pattern")
                .build();
        JsonValue transientState = new JsonObject().build();
        List<Callback> callbacks = ImmutableList.of();

        useCase.setCurrentState(sharedState, transientState, callbacks);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'match'", TypingDNAOutcome.MATCH.name(), exitStateChange.outcome);

        verifySharedState(
                exitStateChange.sharedState,
                6,
                ActionType.RETRY.getAction(),
                null,
                null,
                null,
                "typing pattern",
                "previous typing pattern",
                null,
                AbstractCore.DeviceType.DESKTOP);
        checkTransientState(exitStateChange.transientState);
    }

    private void verifySharedState(JsonValue sharedState,
                                   Integer length,
                                   Integer previousAction,
                                   Integer patternsEnrolled,
                                   Integer enrollmentsLeft,
                                   Integer verifyRetries,
                                   String typingPattern,
                                   String previousTypingPatterns,
                                   String textId,
                                   AbstractCore.DeviceType deviceType) {

        Assert.assertNotNull("sharedState cannot be null", sharedState);
        Assert.assertEquals(String.format("sharedState must have %s key", length), (int) length, sharedState.keys().size());

        Assert.assertEquals("previousAction must be " + previousAction, previousAction, sharedState.get(Constants.PREVIOUS_ACTION).asInteger());
        Assert.assertEquals(String.format("verifyRetries must be %s", verifyRetries), verifyRetries, sharedState.get(Constants.VERIFY_RETRIES).asInteger());
        Assert.assertEquals(String.format("patternsEnrolled must be %s", patternsEnrolled), patternsEnrolled, sharedState.get(Constants.PATTERNS_ENROLLED).asInteger());
        Assert.assertEquals(String.format("enrollments left must be %s", enrollmentsLeft), enrollmentsLeft, sharedState.get(Constants.ENROLLMENTS_LEFT).asInteger());
        Assert.assertEquals("typingPattern must be " + typingPattern, typingPattern, sharedState.get(Constants.TYPING_PATTERN).asString());
        Assert.assertEquals("previousTypingPatterns must be " + previousTypingPatterns, previousTypingPatterns, sharedState.get(Constants.PREVIOUS_TYPING_PATTERNS).asString());
        Assert.assertEquals("textId must be " + textId, textId, sharedState.get(Constants.TEXT_ID).asString());
        Assert.assertEquals("deviceType must be " + deviceType, deviceType.ordinal(), (int) sharedState.get(Constants.DEVICE_TYPE).asInteger());
    }

    private void checkTransientState(JsonValue transientState) {
        Assert.assertNull("transientState is null (unchanged)", transientState);
    }
}
