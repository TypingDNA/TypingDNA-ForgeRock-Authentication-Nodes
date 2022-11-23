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


package com.typingdna.api;

import com.typingdna.api.model.*;
import com.typingdna.util.HTTPRequest;
import com.typingdna.util.JSONData;
import com.typingdna.util.Logger;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.util.Strings;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public abstract class TypingDNAAPI {

    protected final String apiUrl;
    protected final String apiKey;
    protected final String apiSecret;
    protected final HTTPRequest httpRequest;
    protected final Logger logger = Logger.getInstance();

    public TypingDNAAPI(String apiUrl, String apiKey, String apiSecret, int requestTimeout) throws NodeProcessException {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.httpRequest = new HTTPRequest(requestTimeout);
    }

    public CheckUserResponse checkUser(String username, PatternType patternType, String textId, DeviceType deviceType, String requestIdentifier) {
        logger.debug(String.format("In TypingDNADecisionNode: check user username=%s deviceType=%s", username, deviceType.name()));

        CheckUserResponse response;

        JSONData body;
        try {
            body = doCheckUser(username, patternType, textId, requestIdentifier);
        } catch (NodeProcessException e) {
            logger.debug(String.format("In TypingDNADecisionNode: failed to check user username=%s reason=%s", username, e.getMessage()));

            response = new CheckUserResponse(-1, false);
            logger.error(String.format("Unknown error on GET /user: %s", e.getMessage()));

            return response;
        }

        int messageCode = body.getValue("message_code", -1);
        if (messageCode == 32 || messageCode == 33) {
            response = new CheckUserResponse(messageCode, false);
        } else if (body.getValue("success", 0) == 1) {
            int desktopCount = body.getValue("count", 0);
            int mobileCount = body.getValue("mobilecount", 0);
            int patternCount = deviceType == DeviceType.DESKTOP ? desktopCount : mobileCount;

            response = new CheckUserResponse();
            response.setPatternCount(patternCount);

            logger.debug(String.format("In TypingDNADecisionNode: pattern count=%d username=%s", patternCount, username));
        } else {
            response = new CheckUserResponse(messageCode, true);
        }

        if (response.isTemporary()) {
            logError(body, response);
        }

        return response;
    }

    public DeleteUserResponse deleteUser(String username, String requestIdentifier) {
        logger.debug(String.format("In TypingDNAResetProfile: delete user username=%s", username));

        DeleteUserResponse response;

        JSONData body;
        try {
            body = doDeleteUser(username, requestIdentifier);
        } catch (NodeProcessException e) {
            logger.debug(String.format("In TypingDNAResetProfile: failed to delete user username=%s reason=%s", username, e.getMessage()));

            response = new DeleteUserResponse(-1, false);
            logger.error(String.format("Unknown error on DELETE /user: %s", e.getMessage()));

            return response;
        }

        int messageCode = body.getValue("message_code", -1);
        if (messageCode == 32 || messageCode == 33) {
            response = new DeleteUserResponse(messageCode, false);
        } else if (messageCode != 1) {
            response = new DeleteUserResponse(messageCode, true);
        } else {
            response = new DeleteUserResponse();
        }

        if (response.isTemporary()) {
            logError(body, response);
        }

        return response;
    }

    public abstract VerifyResponse verify(String username, String typingPattern, String requestIdentifier);

    private JSONData doCheckUser(String username, PatternType patternType, String textId, String requestIdentifier) throws NodeProcessException {
        StringBuilder uri = new StringBuilder(String.format("%s/user/%s?type=%d&custom_field=%s", apiUrl, username,
                patternType.getType(), requestIdentifier));
        if (!Strings.isNullOrEmpty(textId)) {
            uri.append(String.format("&textid=%s", textId));
        }

        return httpRequest.get(uri.toString(), getRequestHeaders());
    }

    private JSONData doDeleteUser(String username, String requestIdentifier) throws NodeProcessException {
        return httpRequest.delete(String.format("%s/user/%s?custom_field=%s", apiUrl, username, requestIdentifier), getRequestHeaders());
    }

    private String getAuthString() {
        StringBuilder authString = new StringBuilder();
        authString.append("Basic ");

        Base64.Encoder encoder = Base64.getEncoder();
        authString.append(encoder.encodeToString(String.format("%s:%s", apiKey, apiSecret).getBytes()));

        return authString.toString();
    }

    protected void logError(JSONData body, APIResponse response) {
        if (response.isError()) {
            String type = response.isTemporary() ? "temporary" : "permanent";
            int code = body.getValue("message_code", -1);
            String name = body.getValue("name", "");
            String message = body.getValue("message", "");

            logger.error(String.format("TypingDNA Authentication API %s error: code=%d name=%s message=%s", type, code, name, message));
        }
    }

    protected Map<String, String> getRequestHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        headers.put("Authorization", getAuthString());
        headers.put("tdna-advanced", "1");

        return headers;
    }

    protected Map<String, String> getRequestBody(String typingPattern, String customField) {
        Map<String, String> data = new HashMap<>();

        data.put("tp", typingPattern);
        data.put("custom_field", customField);

        return data;
    }
}
