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

import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.handler.HttpClientHandler;
import org.forgerock.http.protocol.Form;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.Function;
import org.forgerock.util.Options;
import org.forgerock.util.Strings;
import org.forgerock.util.time.Duration;

import static org.forgerock.util.CloseSilentlyFunction.closeSilently;
import static org.forgerock.util.Closeables.closeSilentlyAsync;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.http.protocol.Responses.noopExceptionFunction;

import java.net.URISyntaxException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

public class TypingDNAApi {

    public enum PatternType {
        ANY_TEXT(0),
        SAME_TEXT(1),
        SAME_TEXT_EXTENDED(2);

        private final int type;

        PatternType(int type) {
            this.type = type;
        }

        public int getType() {
            return type;
        }
    }

    private final String apiUrl;
    private final String apiKey;
    private final String apiSecret;
    private final HttpClientHandler httpClientHandler;

    public TypingDNAApi(String apiUrl, String apiKey, String apiSecret, int requestTimeout) throws NodeProcessException {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;

        Options options = Options.defaultOptions();
        options.set(HttpClientHandler.OPTION_CONNECT_TIMEOUT, Duration.duration(requestTimeout, TimeUnit.SECONDS));
        options.set(HttpClientHandler.OPTION_SO_TIMEOUT, Duration.duration(requestTimeout, TimeUnit.SECONDS));

        try {
            this.httpClientHandler = new HttpClientHandler(options);
        } catch (HttpApplicationException e) {
            throw new NodeProcessException("Failed to create the HttpClientHandler");
        }
    }

    public JsonValue checkUser(String username, PatternType patternType, String textId, String requestIdentifier) throws NodeProcessException {
        StringBuilder uri = new StringBuilder(String.format("%s/user/%s?type=%d&custom_field=%s", apiUrl, username,
                patternType.getType(), requestIdentifier));
        if (!Strings.isNullOrEmpty(textId)) {
            uri.append(String.format("&textid=%s", textId));
        }

        Request request = createRequest(uri.toString(), "GET");

        JsonValue reqResponse;
        try {
            reqResponse = httpClientHandler.handle(new RootContext(), request)
                    .thenAlways(closeSilentlyAsync(request))
                    .then(closeSilently(mapToJsonValue()), noopExceptionFunction())
                    .getOrThrow();
        } catch (InterruptedException | RuntimeException e) {
            throw new NodeProcessException("Failed to process API request " + request.getUri() + e.getMessage());
        }

        return reqResponse;
    }

    public JsonValue save(String username, String typingPattern, String requestIdentifier) throws NodeProcessException {
        return saveOrVerify("save", username, typingPattern, requestIdentifier);
    }

    public JsonValue verify(String username, String typingPattern, String requestIdentifier) throws NodeProcessException {
        return saveOrVerify("verify", username, typingPattern, requestIdentifier);
    }

    private JsonValue saveOrVerify(String action, String username, String typingPattern, String requestIdentifier) throws NodeProcessException {
        String uri = String.format("%s/%s/%s", apiUrl, action, username);
        Request request = createRequest(uri, "POST");

        final Form form = new Form();
        form.add("tp", typingPattern);
        form.add("custom_field", requestIdentifier);
        form.toRequestEntity(request);

        JsonValue reqResponse;
        try {
            reqResponse = httpClientHandler.handle(new RootContext(), request)
                    .thenAlways(closeSilentlyAsync(request))
                    .then(closeSilently(mapToJsonValue()), noopExceptionFunction())
                    .getOrThrow();
        } catch (InterruptedException | RuntimeException e) {
            throw new NodeProcessException("Failed to process API request " + request.getUri() + e.getMessage());
        }

        return reqResponse;
    }

    private Request createRequest(String uri, String method) throws NodeProcessException {
        Request request;
        try {
            request = new Request().setUri(uri);
        } catch (URISyntaxException e) {
            throw new NodeProcessException("Failed to set URI " + uri);
        }

        request.setMethod(method);
        if (!method.equals("GET")) {
            request.getHeaders().add("Content-Type", "application/json");
        }
        request.getHeaders().add("Accept", "application/json");
        request.getHeaders().add("Authorization", getAuthString());

        return request;
    }

    private String getAuthString() {
        StringBuilder authString = new StringBuilder();
        authString.append("Basic ");

        Base64.Encoder encoder = Base64.getEncoder();
        authString.append(encoder.encodeToString(String.format("%s:%s", apiKey, apiSecret).getBytes()));

        return authString.toString();
    }

    private static Function<Response, JsonValue, NodeProcessException> mapToJsonValue() {
        return response -> {
            try {
                if (response.getStatus().isServerError()) {
                    throw response.getCause();
                }
                return json(response.getEntity().getJson());
            } catch (Exception e) {
                throw new NodeProcessException("Unable to process request. " + response.getEntity().toString(), e);
            }
        };
    }
}
