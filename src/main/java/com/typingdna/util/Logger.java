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

import com.typingdna.nodes.TypingDNAPlugin;
import org.slf4j.LoggerFactory;

import java.util.Date;

public final class Logger {
    private static Logger instance = null;
    private final org.slf4j.Logger logger = LoggerFactory.getLogger(TypingDNAPlugin.class);

    private Logger() {}

    public static Logger getInstance() {
        if (instance == null) {
            instance = new Logger();
        }
        return instance;
    }

    public void debug(String message) {
        logger.debug(String.format("[TypingDNA: %s] %s", new Date().toString(), message));
    }
}
