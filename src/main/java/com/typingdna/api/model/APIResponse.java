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

public abstract class APIResponse {

    private boolean error = false;
    private int code = 0;
    private boolean isTemporary = false;

    public APIResponse() {
        this.error = false;
    }

    public APIResponse(int code, boolean isTemporary) {
        this.error = true;
        this.code = code;
        this.isTemporary = isTemporary;
    }

    public boolean isError() {
        return error;
    }

    public int getCode() {
        return code;
    }

    public boolean isTemporary() {
        return isTemporary;
    }
}
