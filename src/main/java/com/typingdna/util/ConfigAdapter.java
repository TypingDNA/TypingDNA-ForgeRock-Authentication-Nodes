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

import org.forgerock.openam.scripting.service.ScriptConfiguration;

public interface ConfigAdapter {

    default String apiUrl() {
        throw new NoSuchMethodError("apiUrl() method is not implemented");
    }

    default String apiKey() {
        throw new NoSuchMethodError("apiKey() method is not implemented");
    }

    default char[] apiSecret() {
        throw new NoSuchMethodError("apiSecret() method is not implemented");
    }

    default int retries() {
        throw new NoSuchMethodError("retries() method is not implemented");
    }

    default int matchScore() {
        throw new NoSuchMethodError("passScore() method is not implemented");
    }

    default int autoEnrollScore() {
        throw new NoSuchMethodError("autoEnrollScore() method is not implemented");
    }

    default int enrollmentsNecessary() {
        throw new NoSuchMethodError("enrollmentsNecessary() method is not implemented");
    }

    default boolean verifyAfterEnroll() {
        throw new NoSuchMethodError("quietEnrollment() method is not implemented");
    }

    default String textToEnter() {
        throw new NoSuchMethodError("textToEnter() method is not implemented");
    }

    default String usernameSalt() {
        throw new NoSuchMethodError("usernameSalt() method is not implemented");
    }

    default String requestIdentifier() {
        throw new NoSuchMethodError("requestIdentifier() method is not implemented");
    }

    default ScriptConfiguration script() {
        throw new NoSuchMethodError("script() method is not implemented");
    }

    default boolean displayMessage() {
        throw new NoSuchMethodError("displayMessage() method is not implemented");
    }

    default boolean showVisualizer() {
        throw new NoSuchMethodError("showVisualizer() method is not implemented");
    }

    default boolean disableCopyAndPaste() {
        throw new NoSuchMethodError("disableCopyAndPaste() method is not implemented");
    }

    default int requestTimeout() {
        throw new NoSuchMethodError("requestTimeout() method is not implemented");
    }
}
