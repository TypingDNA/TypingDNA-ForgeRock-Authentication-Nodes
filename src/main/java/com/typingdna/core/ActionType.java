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

public enum ActionType {
    CHECK_USER(0),
    VERIFY(1),
    RETRY(2),
    ENROLL(3),
    ENROLL_POSITION(4);

    private final int action;

    ActionType(int action) {
        this.action = action;
    }

    public static ActionType toActionType(int action) {
        switch (action) {
            case 0:
                return ActionType.CHECK_USER;
            case 1:
                return ActionType.VERIFY;
            case 2:
                return ActionType.RETRY;
            case 3:
                return ActionType.ENROLL;
            case 4:
                return ActionType.ENROLL_POSITION;
            default:
                return ActionType.VERIFY;
        }
    }

    public int getAction() {
        return action;
    }
}
