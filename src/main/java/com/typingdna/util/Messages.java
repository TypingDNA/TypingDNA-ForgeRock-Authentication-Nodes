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

public final class Messages {
    public static final String TEXT_TO_ENTER = "I am authenticated by the way I type";
    public static final String SHORT_PHRASE_PROMPT = "Please type the text below (typos allowed):";
    public static final String SHORT_PHRASE_PLACEHOLDER = "Enter text above"; // not displayed
    public static final String TOO_MANY_TYPOS = "The entered text had too many typos. Please try again.";
    public static final String AUTH_FAILED = "Authentication failed. Try again...";
    public static final String NOT_ENOUGH_PATTERNS = "Not enough typing patterns to perform authentication. You need to enroll %s more.";
    public static final String NOT_ENOUGH_PATTERNS_POSITION = "Not enough typing patterns in this typing position. You need to enroll %s more.";
    public static final String ENROLL_DONE = "Successfully enrolled! You will be able to verify your typing pattern now.";
}
