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
import com.typingdna.api.model.CheckUserResponse;
import com.typingdna.api.model.DeviceType;
import com.typingdna.api.model.PatternType;
import com.typingdna.api.model.VerifyResponse;
import com.typingdna.nodes.outcomeproviders.TypingDNADecisionOutcomeProvider.TypingDNADecisionOutcome;
import com.typingdna.core.statechanges.ExitNodeStateChange;
import com.typingdna.core.statechanges.StateChange;
import com.typingdna.api.TypingDNAAPI;
import com.typingdna.util.ConfigAdapter;
import com.typingdna.util.Constants;
import com.typingdna.util.HashAlgorithm;
import com.typingdna.util.HelperFunctions;
import com.typingdna.util.State;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.scripting.service.ScriptConfiguration;
import org.forgerock.openam.utils.JsonObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.security.auth.callback.Callback;
import java.util.List;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.mockito.Mockito.*;

public class DecisionIntegrationTest {

    @Mock
    private ScriptConfiguration scriptConfiguration;
    @Mock
    private ConfigAdapter config;
    @Mock
    private TypingDNAAPI api;

    private static final String typingPattern = "0,3.2,0,0,11,1004326382,1,77,-1,1,61,-1,1,150,-1,2,68,23,2,27,6,1,0,0,1,2,1,4224896695,1,1,0,0,0,1,1080,1920,1,1015,106,0,1495460187|2983,119|210,74|180,92|75,47|89,150|165,76|90,107|123,72|193,94|93,101|116,105";

    @Before
    public void setUp() {
        scriptConfiguration = mock(ScriptConfiguration.class);
        config = mock(ConfigAdapter.class);
        api = mock(TypingDNAAPI.class);

        when(config.script()).thenReturn(scriptConfiguration);
        when(config.retries()).thenReturn(2);
        when(config.usernameSalt()).thenReturn("1234");
        when(config.requestIdentifier()).thenReturn("");
        when(config.hashAlgorithm()).thenReturn(HashAlgorithm.MD5);
        when(scriptConfiguration.getScript()).thenReturn("script");
    }

    @Test
    public void test_DisplayForm() {
        State state = new State(new JsonObject().build(), new JsonObject().build(), ImmutableList.of());
        Decision useCase = new Decision(config, state, api);
        Assert.assertThrows(NoSuchMethodError.class, () -> useCase.displayForm());
        Assert.assertThrows(NoSuchMethodError.class, () -> useCase.displayForm(ActionType.VERIFY));
    }

    @Test
    public void test_HandleForm_Verify_Enroll_NotEnoughPatternsDesktop() throws NodeProcessException {
        /** SET UP **/
        CheckUserResponse checkUserResponse = new CheckUserResponse();
        checkUserResponse.setPatternCount(1);
        when(api.checkUser(HelperFunctions.hashText("test_user", "1234", HashAlgorithm.MD5), PatternType.SAME_TEXT, null, DeviceType.DESKTOP, "")).thenReturn(checkUserResponse);

        VerifyResponse verifyResponse = new VerifyResponse();
        verifyResponse.setNeedsEnroll(true);
        verifyResponse.setPatternEnrolled(true);
        when(api.verify(HelperFunctions.hashText("test_user", "1234", HashAlgorithm.MD5), typingPattern, "")).thenReturn(verifyResponse);

        JsonValue sharedState = new JsonObject()
                .put(USERNAME, "test_user")
                .put(Constants.DEVICE_TYPE, 0)
                .build();
        JsonValue transientState = new JsonObject()
                .put(Constants.TYPING_PATTERN, typingPattern)
                .build();
        List<Callback> callbacks = ImmutableList.of();

        State state = new State(sharedState, transientState, ImmutableList.of());
        Decision useCase = new Decision(config, state, api);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'enroll'", TypingDNADecisionOutcome.ENROLL.name(), exitStateChange.outcome);

        verifyState(
                exitStateChange.sharedState,
                5,
                exitStateChange.transientState,
                1,
                ActionType.ENROLL.getAction(),
                2,
                null,
                typingPattern,
                null,
                null,
                DeviceType.DESKTOP,
                "Not enough typing patterns to perform authentication. 2 pattern(s) enrolled.");
    }

    @Test
    public void test_HandleForm_Verify_Enroll_NotEnoughPatternsMobile() throws NodeProcessException {
        /** SET UP **/
        CheckUserResponse checkUserResponse = new CheckUserResponse();
        checkUserResponse.setPatternCount(0);
        when(api.checkUser(HelperFunctions.hashText("test_user", "1234", HashAlgorithm.MD5), PatternType.SAME_TEXT, null, DeviceType.MOBILE, "")).thenReturn(checkUserResponse);

        VerifyResponse verifyResponse = new VerifyResponse();
        verifyResponse.setPatternEnrolled(true);
        verifyResponse.setNeedsEnroll(true);
        when(api.verify(HelperFunctions.hashText("test_user", "1234", HashAlgorithm.MD5), typingPattern, "")).thenReturn(verifyResponse);

        JsonValue sharedState = new JsonObject()
                .put(USERNAME, "test_user")
                .put(Constants.DEVICE_TYPE, 1)
                .build();
        JsonValue transientState = new JsonObject()
                .put(Constants.TYPING_PATTERN, typingPattern)
                .build();
        List<Callback> callbacks = ImmutableList.of();

        State state = new State(sharedState, transientState, ImmutableList.of());
        Decision useCase = new Decision(config, state, api);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'ENROLL'", TypingDNADecisionOutcome.ENROLL.name(), exitStateChange.outcome);

        verifyState(
                exitStateChange.sharedState,
                5,
                exitStateChange.transientState,
                1,
                ActionType.ENROLL.getAction(),
                1,
                null,
                typingPattern,
                null,
                null,
                DeviceType.MOBILE,
                "Not enough typing patterns to perform authentication. 1 pattern(s) enrolled.");
    }

    @Test
    public void test_HandleForm_Verify_Enroll_SaveFails() throws NodeProcessException {
        /** SET UP **/
        CheckUserResponse checkUserResponse = new CheckUserResponse();
        checkUserResponse.setPatternCount(2);
        when(api.checkUser(HelperFunctions.hashText("test_user", "1234", HashAlgorithm.MD5), PatternType.SAME_TEXT, null, DeviceType.DESKTOP, "")).thenReturn(checkUserResponse);

        VerifyResponse verifyResponse = new VerifyResponse(48, true);
        when(api.verify(HelperFunctions.hashText("test_user", "1234", HashAlgorithm.MD5), typingPattern, "")).thenReturn(verifyResponse);

        JsonValue sharedState = new JsonObject()
                .put(USERNAME, "test_user")
                .put(Constants.DEVICE_TYPE, 0)
                .build();
        JsonValue transientState = new JsonObject()
                .put(Constants.TYPING_PATTERN, typingPattern)
                .build();
        List<Callback> callbacks = ImmutableList.of();

        State state = new State(sharedState, transientState, ImmutableList.of());
        Decision useCase = new Decision(config, state, api);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'enroll'", TypingDNADecisionOutcome.RETRY.name(), exitStateChange.outcome);

        verifyState(
                exitStateChange.sharedState,
                6,
                exitStateChange.transientState,
                1,
                ActionType.RETRY.getAction(),
                2,
                1,
                typingPattern,
                null,
                null,
                DeviceType.DESKTOP,
                "An authentication error occurred, please try again. (code: 48)");
    }

    @Test
    public void test_HandleForm_Verify_Match() throws NodeProcessException {
        /** SET UP **/
        CheckUserResponse checkUserResponse = new CheckUserResponse();
        checkUserResponse.setPatternCount(3);
        when(api.checkUser(HelperFunctions.hashText("test_user", "1234", HashAlgorithm.MD5), PatternType.SAME_TEXT, null, DeviceType.DESKTOP, "")).thenReturn(checkUserResponse);

        VerifyResponse verifyResponse = new VerifyResponse();
        verifyResponse.setMatch(true);
        when(api.verify(HelperFunctions.hashText("test_user", "1234", HashAlgorithm.MD5), typingPattern, "")).thenReturn(verifyResponse);

        JsonValue sharedState = new JsonObject()
                .put(USERNAME, "test_user")
                .put(Constants.DEVICE_TYPE, 0)
                .build();
        JsonValue transientState = new JsonObject()
                .put(Constants.TYPING_PATTERN, typingPattern)
                .build();
        List<Callback> callbacks = ImmutableList.of();

        State state = new State(sharedState, transientState, ImmutableList.of());
        Decision useCase = new Decision(config, state, api);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'match'", TypingDNADecisionOutcome.MATCH.name(), exitStateChange.outcome);

        verifyState(
                exitStateChange.sharedState,
                5,
                exitStateChange.transientState,
                1,
                ActionType.VERIFY.getAction(),
                3,
                null,
                typingPattern,
                null,
                null,
                DeviceType.DESKTOP,
                null);
    }

    @Test
    public void test_HandleForm_Verify_Match_AutoEnrolled() throws NodeProcessException {
        /** SET UP **/
        CheckUserResponse checkUserResponse = new CheckUserResponse();
        checkUserResponse.setPatternCount(3);
        when(api.checkUser(HelperFunctions.hashText("test_user", "1234", HashAlgorithm.MD5), PatternType.SAME_TEXT, null, DeviceType.DESKTOP, "")).thenReturn(checkUserResponse);

        VerifyResponse verifyResponse = new VerifyResponse();
        verifyResponse.setMatch(true);
        verifyResponse.setPatternEnrolled(true);
        when(api.verify(HelperFunctions.hashText("test_user", "1234", HashAlgorithm.MD5), typingPattern, "")).thenReturn(verifyResponse);

        JsonValue sharedState = new JsonObject()
                .put(USERNAME, "test_user")
                .put(Constants.DEVICE_TYPE, 0)
                .build();
        JsonValue transientState = new JsonObject()
                .put(Constants.TYPING_PATTERN, typingPattern)
                .build();
        List<Callback> callbacks = ImmutableList.of();

        State state = new State(sharedState, transientState, ImmutableList.of());
        Decision useCase = new Decision(config, state, api);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'match'", TypingDNADecisionOutcome.MATCH.name(), exitStateChange.outcome);

        verifyState(
                exitStateChange.sharedState,
                5,
                exitStateChange.transientState,
                1,
                ActionType.VERIFY.getAction(),
                4,
                null,
                typingPattern,
                null,
                null,
                DeviceType.DESKTOP,
                null);
    }

    @Test
    public void test_HandleForm_Verify_NoMatch_Retry() throws NodeProcessException {
        /** SET UP **/
        CheckUserResponse checkUserResponse = new CheckUserResponse();
        checkUserResponse.setPatternCount(3);
        when(api.checkUser(HelperFunctions.hashText("test_user", "1234", HashAlgorithm.MD5), PatternType.SAME_TEXT, null, DeviceType.DESKTOP, "")).thenReturn(checkUserResponse);

        VerifyResponse verifyResponse = new VerifyResponse();
        verifyResponse.setMatch(false);
        when(api.verify(HelperFunctions.hashText("test_user", "1234", HashAlgorithm.MD5), typingPattern, "")).thenReturn(verifyResponse);

        JsonValue sharedState = new JsonObject()
                .put(USERNAME, "test_user")
                .put(Constants.DEVICE_TYPE, 0)
                .build();
        JsonValue transientState = new JsonObject()
                .put(Constants.TYPING_PATTERN, typingPattern)
                .build();
        List<Callback> callbacks = ImmutableList.of();

        State state = new State(sharedState, transientState, ImmutableList.of());
        Decision useCase = new Decision(config, state, api);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'retry'", TypingDNADecisionOutcome.RETRY.name(), exitStateChange.outcome);

        verifyState(
                exitStateChange.sharedState,
                6,
                exitStateChange.transientState,
                2,
                ActionType.RETRY.getAction(),
                3,
                1,
                typingPattern,
                typingPattern,
                null,
                DeviceType.DESKTOP,
                "Authentication failed. Try again...");
    }

    @Test
    public void test_HandleForm_Verify_NoMatch_NoRetry() throws NodeProcessException {
        /** SET UP **/
        CheckUserResponse checkUserResponse = new CheckUserResponse();
        checkUserResponse.setPatternCount(3);
        when(api.checkUser(HelperFunctions.hashText("test_user", "1234", HashAlgorithm.MD5), PatternType.SAME_TEXT, null, DeviceType.DESKTOP, "")).thenReturn(checkUserResponse);

        VerifyResponse verifyResponse = new VerifyResponse();
        verifyResponse.setMatch(false);
        when(api.verify(HelperFunctions.hashText("test_user", "1234", HashAlgorithm.MD5), typingPattern, "")).thenReturn(verifyResponse);

        JsonValue sharedState = new JsonObject()
                .put(USERNAME, "test_user")
                .put(Constants.VERIFY_RETRIES, 2)
                .put(Constants.DEVICE_TYPE, 0)
                .build();
        JsonValue transientState = new JsonObject()
                .put(Constants.TYPING_PATTERN, typingPattern)
                .build();
        List<Callback> callbacks = ImmutableList.of();

        State state = new State(sharedState, transientState, ImmutableList.of());
        Decision useCase = new Decision(config, state, api);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'no match'", TypingDNADecisionOutcome.NO_MATCH.name(), exitStateChange.outcome);

        verifyState(
                exitStateChange.sharedState,
                6,
                exitStateChange.transientState,
                1,
                ActionType.VERIFY.getAction(),
                3,
                2,
                typingPattern,
                null,
                null,
                DeviceType.DESKTOP,
                null);
    }

    @Test
    public void test_HandleForm_Verify_Enroll_MessageCode4() throws NodeProcessException {
        /** SET UP **/
        CheckUserResponse checkUserResponse = new CheckUserResponse();
        checkUserResponse.setPatternCount(3);
        when(api.checkUser(HelperFunctions.hashText("test_user", "1234", HashAlgorithm.MD5), PatternType.SAME_TEXT, null, DeviceType.MOBILE, "")).thenReturn(checkUserResponse);

        VerifyResponse verifyResponse = new VerifyResponse();
        verifyResponse.setPatternEnrolled(true);
        verifyResponse.setNeedsEnroll(true);
        when(api.verify(HelperFunctions.hashText("test_user", "1234", HashAlgorithm.MD5), typingPattern, "")).thenReturn(verifyResponse);

        JsonValue sharedState = new JsonObject()
                .put(USERNAME, "test_user")
                .put(Constants.DEVICE_TYPE, 1)
                .build();
        JsonValue transientState = new JsonObject()
                .put(Constants.TYPING_PATTERN, typingPattern)
                .build();
        List<Callback> callbacks = ImmutableList.of();

        State state = new State(sharedState, transientState, ImmutableList.of());
        Decision useCase = new Decision(config, state, api);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'enroll'", TypingDNADecisionOutcome.ENROLL.name(), exitStateChange.outcome);

        verifyState(
                exitStateChange.sharedState,
                5,
                exitStateChange.transientState,
                1,
                ActionType.ENROLL.getAction(),
                4,
                null,
                typingPattern,
                null,
                null,
                DeviceType.MOBILE,
                "Not enough typing patterns to perform authentication. 4 pattern(s) enrolled.");
    }

    @Test
    public void test_HandleForm_Verify_EnrollPositions_PreviouslyEnoughPatterns() throws NodeProcessException {
        /** SET UP **/
        CheckUserResponse checkUserResponse = new CheckUserResponse();
        checkUserResponse.setPatternCount(8);
        when(api.checkUser(HelperFunctions.hashText("test_user", "1234", HashAlgorithm.MD5), PatternType.SAME_TEXT, null, DeviceType.MOBILE, "")).thenReturn(checkUserResponse);

        VerifyResponse verifyResponse = new VerifyResponse();
        verifyResponse.setPatternEnrolled(true);
        verifyResponse.setNeedsEnrollPosition(true);
        when(api.verify(HelperFunctions.hashText("test_user", "1234", HashAlgorithm.MD5), typingPattern, "")).thenReturn(verifyResponse);

        JsonValue sharedState = new JsonObject()
                .put(USERNAME, "test_user")
                .put(Constants.DEVICE_TYPE, 1)
                .build();
        JsonValue transientState = new JsonObject()
                .put(Constants.TYPING_PATTERN, typingPattern)
                .build();
        List<Callback> callbacks = ImmutableList.of();

        State state = new State(sharedState, transientState, ImmutableList.of());
        Decision useCase = new Decision(config, state, api);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'enroll'", TypingDNADecisionOutcome.ENROLL.name(), exitStateChange.outcome);

        verifyState(
                exitStateChange.sharedState,
                5,
                exitStateChange.transientState,
                1,
                ActionType.ENROLL_POSITION.getAction(),
                9,
                null,
                typingPattern,
                null,
                null,
                DeviceType.MOBILE,
                "Not enough typing patterns in this typing position. 9 pattern(s) enrolled.");
    }

    @Test
    public void test_HandleForm_Verify_Enroll_MessageCode3() throws NodeProcessException {
        /** SET UP **/
        CheckUserResponse checkUserResponse = new CheckUserResponse();
        checkUserResponse.setPatternCount(3);
        when(api.checkUser(HelperFunctions.hashText("test_user", "1234", HashAlgorithm.MD5), PatternType.SAME_TEXT, null, DeviceType.MOBILE, "")).thenReturn(checkUserResponse);

        VerifyResponse verifyResponse = new VerifyResponse();
        verifyResponse.setPatternEnrolled(true);
        verifyResponse.setNeedsEnrollPosition(true);
        when(api.verify(HelperFunctions.hashText("test_user", "1234", HashAlgorithm.MD5), typingPattern, "")).thenReturn(verifyResponse);

        JsonValue sharedState = new JsonObject()
                .put(USERNAME, "test_user")
                .put(Constants.DEVICE_TYPE, 1)
                .build();
        JsonValue transientState = new JsonObject()
                .put(Constants.TYPING_PATTERN, typingPattern)
                .build();
        List<Callback> callbacks = ImmutableList.of();

        State state = new State(sharedState, transientState, ImmutableList.of());
        Decision useCase = new Decision(config, state, api);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'enroll'", TypingDNADecisionOutcome.ENROLL.name(), exitStateChange.outcome);

        verifyState(
                exitStateChange.sharedState,
                5,
                exitStateChange.transientState,
                1,
                ActionType.ENROLL_POSITION.getAction(),
                4,
                null,
                typingPattern,
                null,
                null,
                DeviceType.MOBILE,
                "Not enough typing patterns in this typing position. 4 pattern(s) enrolled.");
    }

    @Test
    public void test_HandleForm_Verify_InvalidCredentials() throws NodeProcessException {
        /** SET UP **/
        CheckUserResponse checkUserResponse = new CheckUserResponse();
        checkUserResponse.setPatternCount(3);
        when(api.checkUser(HelperFunctions.hashText("test_user", "1234", HashAlgorithm.MD5), PatternType.SAME_TEXT, null, DeviceType.DESKTOP, "")).thenReturn(checkUserResponse);

        VerifyResponse verifyResponse = new VerifyResponse(32, false);
        when(api.verify(HelperFunctions.hashText("test_user", "1234", HashAlgorithm.MD5), typingPattern, "")).thenReturn(verifyResponse);

        JsonValue sharedState = new JsonObject()
                .put(USERNAME, "test_user")
                .put(Constants.DEVICE_TYPE, 0)
                .build();
        JsonValue transientState = new JsonObject()
                .put(Constants.TYPING_PATTERN, typingPattern)
                .build();
        List<Callback> callbacks = ImmutableList.of();

        State state = new State(sharedState, transientState, ImmutableList.of());
        Decision useCase = new Decision(config, state, api);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'failure'", TypingDNADecisionOutcome.FAIL.name(), exitStateChange.outcome);

        verifyState(
                exitStateChange.sharedState,
                4,
                exitStateChange.transientState,
                1,
                null,
                3,
                null,
                typingPattern,
                null,
                null,
                DeviceType.DESKTOP,
                "An authentication error occurred. (code: 32)");
    }

    @Test
    public void test_HandleForm_Verify_InvalidTypingPattern() throws NodeProcessException {
        /** SET UP **/
        CheckUserResponse checkUserResponse = new CheckUserResponse();
        checkUserResponse.setPatternCount(3);
        when(api.checkUser(HelperFunctions.hashText("test_user", "1234", HashAlgorithm.MD5), PatternType.SAME_TEXT, null, DeviceType.DESKTOP, "")).thenReturn(checkUserResponse);

        VerifyResponse verifyResponse = new VerifyResponse(36, true);
        when(api.verify(HelperFunctions.hashText("test_user", "1234", HashAlgorithm.MD5), typingPattern, "")).thenReturn(verifyResponse);

        JsonValue sharedState = new JsonObject()
                .put(USERNAME, "test_user")
                .put(Constants.DEVICE_TYPE, 0)
                .build();
        JsonValue transientState = new JsonObject()
                .put(Constants.TYPING_PATTERN, typingPattern)
                .build();
        List<Callback> callbacks = ImmutableList.of();

        State state = new State(sharedState, transientState, ImmutableList.of());
        Decision useCase = new Decision(config, state, api);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'retry'", TypingDNADecisionOutcome.RETRY.name(), exitStateChange.outcome);

        verifyState(
                exitStateChange.sharedState,
                6,
                exitStateChange.transientState,
                1,
                ActionType.RETRY.getAction(),
                3,
                1,
                typingPattern,
                null,
                null,
                DeviceType.DESKTOP,
                "An authentication error occurred, please try again. (code: 36)");
    }

    @Test
    public void test_HandleForm_Verify_InvalidTypingPattern_NoRetriesLeft() throws NodeProcessException {
        /** SET UP **/
        CheckUserResponse checkUserResponse = new CheckUserResponse();
        checkUserResponse.setPatternCount(3);
        when(api.checkUser(HelperFunctions.hashText("test_user", "1234", HashAlgorithm.MD5), PatternType.SAME_TEXT, null, DeviceType.DESKTOP, "")).thenReturn(checkUserResponse);

        VerifyResponse verifyResponse = new VerifyResponse(36, true);
        when(api.verify(HelperFunctions.hashText("test_user", "1234", HashAlgorithm.MD5), typingPattern, "")).thenReturn(verifyResponse);

        JsonValue sharedState = new JsonObject()
                .put(USERNAME, "test_user")
                .put(Constants.VERIFY_RETRIES, 3)
                .put(Constants.DEVICE_TYPE, 0)
                .build();
        JsonValue transientState = new JsonObject()
                .put(Constants.TYPING_PATTERN, typingPattern)
                .build();
        List<Callback> callbacks = ImmutableList.of();

        State state = new State(sharedState, transientState, ImmutableList.of());
        Decision useCase = new Decision(config, state, api);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'failure'", TypingDNADecisionOutcome.FAIL.name(), exitStateChange.outcome);

        verifyState(
                exitStateChange.sharedState,
                6,
                exitStateChange.transientState,
                1,
                ActionType.VERIFY.getAction(),
                3,
                3,
                typingPattern,
                null,
                null,
                DeviceType.DESKTOP,
                "An authentication error occurred, please try again. (code: 36)");
    }

    @Test
    public void test_HandleForm_Enroll_Enroll_SaveFails() throws NodeProcessException {
        /** SET UP **/
        VerifyResponse verifyResponse = new VerifyResponse(48, true);
        when(api.verify(HelperFunctions.hashText("test_user", "1234", HashAlgorithm.MD5), typingPattern, "")).thenReturn(verifyResponse);

        JsonValue sharedState = new JsonObject()
                .put(Constants.PREVIOUS_ACTION, ActionType.ENROLL.getAction())
                .put(USERNAME, "test_user")
                .put(Constants.DEVICE_TYPE, 0)
                .put(Constants.PATTERNS_ENROLLED, 2)
                .build();
        JsonValue transientState = new JsonObject()
                .put(Constants.TYPING_PATTERN, typingPattern)
                .build();
        List<Callback> callbacks = ImmutableList.of();

        State state = new State(sharedState, transientState, ImmutableList.of());
        Decision useCase = new Decision(config, state, api);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'enroll'", TypingDNADecisionOutcome.ENROLL.name(), exitStateChange.outcome);

        verifyState(
                exitStateChange.sharedState,
                6,
                exitStateChange.transientState,
                1,
                ActionType.ENROLL.getAction(),
                2,
                1,
                typingPattern,
                null,
                null,
                DeviceType.DESKTOP,
                "An authentication error occurred, please try again. (code: 48)");
    }

    @Test
    public void test_HandleForm_Enroll_Enroll_SaveDoneNotEnoughPatterns() throws NodeProcessException {
        /** SET UP **/
        VerifyResponse verifyResponse = new VerifyResponse();
        verifyResponse.setNeedsEnroll(true);
        verifyResponse.setPatternEnrolled(true);
        when(api.verify(HelperFunctions.hashText("test_user", "1234", HashAlgorithm.MD5), typingPattern, "")).thenReturn(verifyResponse);

        JsonValue sharedState = new JsonObject()
                .put(Constants.PREVIOUS_ACTION, ActionType.ENROLL.getAction())
                .put(USERNAME, "test_user")
                .put(Constants.PATTERNS_ENROLLED, 1)
                .put(Constants.DEVICE_TYPE, 0)
                .build();
        JsonValue transientState = new JsonObject()
                .put(Constants.TYPING_PATTERN, typingPattern)
                .build();
        List<Callback> callbacks = ImmutableList.of();

        State state = new State(sharedState, transientState, ImmutableList.of());
        Decision useCase = new Decision(config, state, api);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'enroll'", TypingDNADecisionOutcome.ENROLL.name(), exitStateChange.outcome);

        verifyState(
                exitStateChange.sharedState,
                5,
                exitStateChange.transientState,
                1,
                ActionType.ENROLL.getAction(),
                2,
                null,
                typingPattern,
                null,
                null,
                DeviceType.DESKTOP,
                "Not enough typing patterns to perform authentication. 2 pattern(s) enrolled.");
    }

    @Test
    public void test_HandleForm_Enroll_Enroll_InitialEnrollmentComplete() throws NodeProcessException {
        /** SET UP **/
        VerifyResponse verifyResponse = new VerifyResponse();
        verifyResponse.setMatch(false);
        verifyResponse.setNeedsEnroll(false);
        verifyResponse.setPatternEnrolled(true);
        when(api.verify(HelperFunctions.hashText("test_user", "1234", HashAlgorithm.MD5), typingPattern, "")).thenReturn(verifyResponse);

        JsonValue sharedState = new JsonObject()
                .put(Constants.PREVIOUS_ACTION, ActionType.ENROLL.getAction())
                .put(USERNAME, "test_user")
                .put(Constants.PATTERNS_ENROLLED, 2)
                .put(Constants.DEVICE_TYPE, 0)
                .build();
        JsonValue transientState = new JsonObject()
                .put(Constants.TYPING_PATTERN, typingPattern)
                .build();
        List<Callback> callbacks = ImmutableList.of();

        State state = new State(sharedState, transientState, ImmutableList.of());
        Decision useCase = new Decision(config, state, api);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'initial enrollment complete'", TypingDNADecisionOutcome.INITIAL_ENROLLMENT_COMPLETE.name(), exitStateChange.outcome);

        verifyState(
                exitStateChange.sharedState,
                5,
                exitStateChange.transientState,
                1,
                ActionType.VERIFY.getAction(),
                3,
                null,
                typingPattern,
                null,
                null,
                DeviceType.DESKTOP,
                "Successfully enrolled!");
    }

    @Test
    public void test_HandleForm_EnrollPosition_EnrollPosition_SaveFails() throws NodeProcessException {
        /** SET UP **/
        VerifyResponse verifyResponse = new VerifyResponse(48, true);
        when(api.verify(HelperFunctions.hashText("test_user", "1234", HashAlgorithm.MD5), typingPattern, "")).thenReturn(verifyResponse);

        JsonValue sharedState = new JsonObject()
                .put(Constants.PREVIOUS_ACTION, ActionType.ENROLL_POSITION.getAction())
                .put(USERNAME, "test_user")
                .put(Constants.PATTERNS_ENROLLED, 1)
                .put(Constants.DEVICE_TYPE, 1)
                .build();
        JsonValue transientState = new JsonObject()
                .put(Constants.TYPING_PATTERN, typingPattern)
                .build();
        List<Callback> callbacks = ImmutableList.of();

        State state = new State(sharedState, transientState, ImmutableList.of());
        Decision useCase = new Decision(config, state, api);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'enroll'", TypingDNADecisionOutcome.ENROLL.name(), exitStateChange.outcome);

        verifyState(
                exitStateChange.sharedState,
                6,
                exitStateChange.transientState,
                1,
                ActionType.ENROLL_POSITION.getAction(),
                1,
                1,
                typingPattern,
                null,
                null,
                DeviceType.MOBILE,
                "An authentication error occurred, please try again. (code: 48)");
    }

    @Test
    public void test_HandleForm_EnrollPosition_EnrollPosition_SaveDoneNotEnoughPatterns() throws NodeProcessException {
        /** SET UP **/
        VerifyResponse verifyResponse = new VerifyResponse();
        verifyResponse.setPatternEnrolled(true);
        verifyResponse.setNeedsEnrollPosition(true);
        when(api.verify(HelperFunctions.hashText("test_user", "1234", HashAlgorithm.MD5), typingPattern, "")).thenReturn(verifyResponse);

        JsonValue sharedState = new JsonObject()
                .put(Constants.PREVIOUS_ACTION, ActionType.ENROLL_POSITION.getAction())
                .put(USERNAME, "test_user")
                .put(Constants.PATTERNS_ENROLLED, 0)
                .put(Constants.DEVICE_TYPE, 1)
                .build();
        JsonValue transientState = new JsonObject()
                .put(Constants.TYPING_PATTERN, typingPattern)
                .build();
        List<Callback> callbacks = ImmutableList.of();

        State state = new State(sharedState, transientState, ImmutableList.of());
        Decision useCase = new Decision(config, state, api);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'enroll'", TypingDNADecisionOutcome.ENROLL.name(), exitStateChange.outcome);

        verifyState(
                exitStateChange.sharedState,
                5,
                exitStateChange.transientState,
                1,
                ActionType.ENROLL_POSITION.getAction(),
                1,
                null,
                typingPattern,
                null,
                null,
                DeviceType.MOBILE,
                "Not enough typing patterns in this typing position. 1 pattern(s) enrolled.");
    }

    @Test
    public void test_HandleForm_Retry_NoMatch_Retry() throws NodeProcessException {
        /** SET UP **/
        CheckUserResponse checkUserResponse = new CheckUserResponse();
        checkUserResponse.setPatternCount(3);
        when(api.checkUser(HelperFunctions.hashText("test_user", "1234", HashAlgorithm.MD5), PatternType.SAME_TEXT, null, DeviceType.DESKTOP, "")).thenReturn(checkUserResponse);

        VerifyResponse verifyResponse = new VerifyResponse();
        verifyResponse.setMatch(false);
        when(api.verify(HelperFunctions.hashText("test_user", "1234", HashAlgorithm.MD5), String.format("%s;%s", typingPattern, typingPattern), "")).thenReturn(verifyResponse);

        JsonValue sharedState = new JsonObject()
                .put(USERNAME, "test_user")
                .put(Constants.PREVIOUS_ACTION, ActionType.RETRY.getAction())
                .put(Constants.VERIFY_RETRIES, 1)
                .put(Constants.DEVICE_TYPE, 0)
                .build();
        JsonValue transientState = new JsonObject()
                .put(Constants.PREVIOUS_TYPING_PATTERNS, typingPattern)
                .put(Constants.TYPING_PATTERN, typingPattern)
                .build();
        List<Callback> callbacks = ImmutableList.of();

        State state = new State(sharedState, transientState, ImmutableList.of());
        Decision useCase = new Decision(config, state, api);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'RETRY'", TypingDNADecisionOutcome.RETRY.name(), exitStateChange.outcome);

        verifyState(
                exitStateChange.sharedState,
                5,
                exitStateChange.transientState,
                2,
                ActionType.RETRY.getAction(),
                null,
                2,
                typingPattern,
                typingPattern,
                null,
                DeviceType.DESKTOP,
                "Authentication failed. Try again...");
    }

    @Test
    public void test_HandleForm_Retry_NoMatch_NoRetriesLeft() throws NodeProcessException {
        /** SET UP **/
        CheckUserResponse checkUserResponse = new CheckUserResponse();
        checkUserResponse.setPatternCount(3);
        when(api.checkUser(HelperFunctions.hashText("test_user", "1234", HashAlgorithm.MD5), PatternType.SAME_TEXT, null, DeviceType.DESKTOP, "")).thenReturn(checkUserResponse);

        VerifyResponse verifyResponse = new VerifyResponse();
        verifyResponse.setMatch(false);
        when(api.verify(HelperFunctions.hashText("test_user", "1234", HashAlgorithm.MD5), String.format("%s;%s", typingPattern, typingPattern), "")).thenReturn(verifyResponse);

        JsonValue sharedState = new JsonObject()
                .put(USERNAME, "test_user")
                .put(Constants.PREVIOUS_ACTION, ActionType.RETRY.getAction())
                .put(Constants.VERIFY_RETRIES, 2)
                .put(Constants.DEVICE_TYPE, 0)
                .build();
        JsonValue transientState = new JsonObject()
                .put(Constants.PREVIOUS_TYPING_PATTERNS, typingPattern)
                .put(Constants.TYPING_PATTERN, typingPattern)
                .build();
        List<Callback> callbacks = ImmutableList.of();

        State state = new State(sharedState, transientState, ImmutableList.of());
        Decision useCase = new Decision(config, state, api);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'NO MATCH'", TypingDNADecisionOutcome.NO_MATCH.name(), exitStateChange.outcome);

        verifyState(
                exitStateChange.sharedState,
                5,
                exitStateChange.transientState,
                2,
                ActionType.VERIFY.getAction(),
                null,
                2,
                typingPattern,
                "previous typing pattern",
                null,
                DeviceType.DESKTOP,
                null);
    }

    @Test
    public void test_HandleForm_Retry_Match() throws NodeProcessException {
        /** SET UP **/
        CheckUserResponse checkUserResponse = new CheckUserResponse();
        checkUserResponse.setPatternCount(3);
        when(api.checkUser(HelperFunctions.hashText("test_user", "1234", HashAlgorithm.MD5), PatternType.SAME_TEXT, null, DeviceType.DESKTOP, "")).thenReturn(checkUserResponse);

        VerifyResponse verifyResponse = new VerifyResponse();
        verifyResponse.setMatch(true);
        when(api.verify(HelperFunctions.hashText("test_user", "1234", HashAlgorithm.MD5), String.format("%s;%s", typingPattern, typingPattern), "")).thenReturn(verifyResponse);

        JsonValue sharedState = new JsonObject()
                .put(USERNAME, "test_user")
                .put(Constants.PREVIOUS_ACTION, ActionType.RETRY.getAction())
                .put(Constants.DEVICE_TYPE, 0)
                .build();
        JsonValue transientState = new JsonObject()
                .put(Constants.PREVIOUS_TYPING_PATTERNS, typingPattern)
                .put(Constants.TYPING_PATTERN, typingPattern)
                .build();
        List<Callback> callbacks = ImmutableList.of();

        State state = new State(sharedState, transientState, ImmutableList.of());
        Decision useCase = new Decision(config, state, api);

        /** TEST **/
        StateChange stateChange = useCase.handleForm();

        Assert.assertNotNull("stateChange can't be null", stateChange);
        Assert.assertEquals("stateChange must be an instance of ExitNodeStateChange", ExitNodeStateChange.class, stateChange.getClass());

        ExitNodeStateChange exitStateChange = (ExitNodeStateChange) stateChange;
        Assert.assertEquals("outcome must be 'match'", TypingDNADecisionOutcome.MATCH.name(), exitStateChange.outcome);

        verifyState(
                exitStateChange.sharedState,
                4,
                exitStateChange.transientState,
                2,
                ActionType.VERIFY.getAction(),
                null,
                null,
                typingPattern,
                "previous typing pattern",
                null,
                DeviceType.DESKTOP,
                null);
    }

    private void verifyState(JsonValue sharedState,
                             Integer sharedStateLength,
                             JsonValue transientState,
                             Integer transientStateLength,
                             Integer previousAction,
                             Integer patternsEnrolled,
                             Integer verifyRetries,
                             String typingPattern,
                             String previousTypingPatterns,
                             String textId,
                             DeviceType deviceType,
                             String message) {

        Assert.assertNotNull("sharedState cannot be null", sharedState);
        Assert.assertEquals(String.format("sharedState must have %s key", sharedStateLength), (int) sharedStateLength, sharedState.keys().size());

        Assert.assertNotNull("transientState cannot be null", transientState);
        Assert.assertEquals(String.format("transientState must have %s key", transientStateLength), (int) transientStateLength, transientState.keys().size());

        Assert.assertEquals("previousAction must be " + previousAction, previousAction, sharedState.get(Constants.PREVIOUS_ACTION).asInteger());
        Assert.assertEquals(String.format("verifyRetries must be %s", verifyRetries), verifyRetries, sharedState.get(Constants.VERIFY_RETRIES).asInteger());
        Assert.assertEquals(String.format("patternsEnrolled must be %s", patternsEnrolled), patternsEnrolled, sharedState.get(Constants.PATTERNS_ENROLLED).asInteger());
        Assert.assertEquals("deviceType must be " + deviceType, deviceType.ordinal(), (int) sharedState.get(Constants.DEVICE_TYPE).asInteger());

        Assert.assertEquals("textId must be " + textId, textId, sharedState.get(Constants.TEXT_ID).asString());

        Assert.assertEquals("message must be " + message, message, sharedState.get(Constants.MESSAGE).asString());
    }
}
