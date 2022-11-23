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

import com.typingdna.api.model.VerifyResponse;
import com.typingdna.util.JSONData;
import org.forgerock.openam.auth.node.api.NodeProcessException;

import java.util.Map;

public class DeveloperAPI extends TypingDNAAPI {

    public DeveloperAPI(String apiUrl, String apiKey, String apiSecret, int requestTimeout) throws NodeProcessException {
        super(apiUrl, apiKey, apiSecret, requestTimeout);
    }

    @Override
    public VerifyResponse verify(String username, String typingPattern, String requestIdentifier) {
        logger.debug(String.format("In TypingDNADecisionNode: verify typing pattern (auto) username=%s", username));

        VerifyResponse response;

        JSONData body;
        try {
            body = doAuto(username, typingPattern, requestIdentifier);
        } catch (NodeProcessException e) {
            logger.debug(String.format("In TypingDNADecisionNode: failed to verify (auto) username=%s reason=%s", username, e.getMessage()));

            response = new VerifyResponse(-1, false);
            logger.error(String.format("Unknown error on POST /auto: %s", e.getMessage()));

            return response;
        }

        int messageCode = body.getValue("message_code", -1);
        String action = body.getValue("action", "");
        boolean result = body.getValue("result", 0) == 1;

        if (messageCode == 2 || messageCode == 32 || messageCode == 33) {
            response = new VerifyResponse(messageCode, false);
        } else if (!action.equals("")) {
            response = new VerifyResponse();
            response.setMatch(result);

            if (action.equals("enroll")) {
                response.setNeedsEnroll(!body.getValue("has_minimum_enrollments", false));
                response.setPatternEnrolled(true);
            } else if (action.contains("enroll")) {
                response.setPatternEnrolled(true);
            }
        } else {
            response = new VerifyResponse(messageCode, true);
        }

        if (response.isError()) {
            logError(body, response);
        }

        logger.debug(String.format("In TypingDNADecisionNode: verify response (auto) username=%s match=%b enroll=%b", username, response.isMatch(),
                response.isPatternEnrolled()));

        return response;
    }

    private JSONData doAuto(String username, String typingPattern, String requestIdentifier) throws NodeProcessException {
        String url = String.format("%s/auto/%s", apiUrl, username);

        Map<String, String> headers = getRequestHeaders();
        Map<String, String> data = getRequestBody(typingPattern, requestIdentifier);

        return httpRequest.post(url, headers, data);
    }
}
