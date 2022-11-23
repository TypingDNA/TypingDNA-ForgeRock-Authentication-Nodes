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


package com.typingdna.api.model;

public class VerifyResponse extends APIResponse {

    public VerifyResponse() {
        super();
    }

    public VerifyResponse(int code, boolean isTemporary) {
        super(code, isTemporary);
    }

    private boolean match = false;
    private boolean needsEnroll = false;
    private boolean needsEnrollPosition = false;
    private boolean patternEnrolled = false;

    public boolean isMatch() {
        return match;
    }

    public void setMatch(boolean match) {
        this.match = match;
    }

    public boolean isNeedsEnroll() {
        return needsEnroll;
    }

    public void setNeedsEnroll(boolean needsEnroll) {
        this.needsEnroll = needsEnroll;
    }

    public boolean isPatternEnrolled() {
        return patternEnrolled;
    }

    public boolean isNeedsEnrollPosition() {
        return needsEnrollPosition;
    }

    public void setNeedsEnrollPosition(boolean needsEnrollPosition) {
        this.needsEnrollPosition = needsEnrollPosition;
    }

    public void setPatternEnrolled(boolean patternEnrolled) {
        this.patternEnrolled = patternEnrolled;
    }
}
