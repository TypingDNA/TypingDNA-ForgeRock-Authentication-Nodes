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
import org.forgerock.util.time.Duration;

import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.forgerock.http.protocol.Responses.noopExceptionFunction;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.util.CloseSilentlyFunction.closeSilently;
import static org.forgerock.util.Closeables.closeSilentlyAsync;

public class HTTPRequest {

    private final HttpClientHandler httpClientHandler;

    public HTTPRequest(int requestTimeout) throws NodeProcessException {
        Options options = Options.defaultOptions();
        options.set(HttpClientHandler.OPTION_CONNECT_TIMEOUT, Duration.duration(requestTimeout, TimeUnit.SECONDS));
        options.set(HttpClientHandler.OPTION_SO_TIMEOUT, Duration.duration(requestTimeout, TimeUnit.SECONDS));

        try {
            this.httpClientHandler = new HttpClientHandler(options);
        } catch (HttpApplicationException e) {
            throw new NodeProcessException("Failed to create the HttpClientHandler");
        }
    }

    public JSONData get(String url, Map<String, String> headers) throws NodeProcessException {
        Request request = createRequest(url, "GET", headers);

        JsonValue reqResponse;
        try {
            reqResponse = httpClientHandler.handle(new RootContext(), request)
                    .thenAlways(closeSilentlyAsync(request))
                    .then(closeSilently(mapToJsonValue()), noopExceptionFunction())
                    .getOrThrow();
        } catch (InterruptedException | RuntimeException e) {
            throw new NodeProcessException("Failed to process API request " + request.getUri() + e.getMessage());
        }

        return new JSONData(reqResponse);
    }

    public JSONData delete(String url, Map<String, String> headers) throws NodeProcessException {
        Request request = createRequest(url, "DELETE", headers);

        JsonValue reqResponse;
        try {
            reqResponse = httpClientHandler.handle(new RootContext(), request)
                    .thenAlways(closeSilentlyAsync(request))
                    .then(closeSilently(mapToJsonValue()), noopExceptionFunction())
                    .getOrThrow();
        } catch (InterruptedException | RuntimeException e) {
            throw new NodeProcessException("Failed to process API request " + request.getUri() + e.getMessage());
        }

        return new JSONData(reqResponse);
    }

    public JSONData post(String url, Map<String, String> headers, Map<String, String> data) throws NodeProcessException {
        Request request = createRequest(url, "POST", headers);

        final Form form = new Form();
        data.forEach(form::add);
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

        return new JSONData(reqResponse);
    }

    private Request createRequest(String uri, String method, Map<String, String> headers) throws NodeProcessException {
        Request request;
        try {
            request = new Request().setUri(uri);
        } catch (URISyntaxException e) {
            throw new NodeProcessException("Failed to set URI " + uri);
        }

        request.setMethod(method);
        request.getHeaders().add("Content-Type", "application/json");
        headers.forEach((k, v) -> request.getHeaders().add(k, v));

        return request;
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
