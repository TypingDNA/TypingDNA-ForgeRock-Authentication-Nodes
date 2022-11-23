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


package com.typingdna.nodes.outcomeproviders;

import com.google.common.collect.ImmutableList;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.util.i18n.PreferredLocales;

import java.util.List;

public class TypingDNAResetProfileOutcomeProvider implements OutcomeProvider {
    public enum TypingDNAResetProfileOutcome {
        SUCCESS,
        ERROR,
    }

    @Override
    public List<Outcome> getOutcomes(PreferredLocales preferredLocales, JsonValue jsonValue) throws NodeProcessException {
        return ImmutableList.of(
                new Outcome(TypingDNAResetProfileOutcomeProvider.TypingDNAResetProfileOutcome.SUCCESS.name(), "Success"),
                new Outcome(TypingDNAResetProfileOutcomeProvider.TypingDNAResetProfileOutcome.ERROR.name(), "Error"));
    }
}
