/*
 * Copyright 2018 Baloise Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thisorthat.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Pair of images presented to a user for voting")
public record VoteResponse(
        @Schema(description = "First image ID", example = "0")
        String id1,
        @Schema(description = "Second image ID", example = "1")
        String id2,
        @Schema(description = "Whether the survey is currently open for voting", example = "true")
        Boolean surveyIsRunning,
        @Schema(description = "Survey perspective/question", example = "Which logo is better?")
        String perspective
) {
    public String getId1() {
        return id1;
    }

    public String getId2() {
        return id2;
    }

    public Boolean getSurveyIsRunning() {
        return surveyIsRunning;
    }

    public String getPerspective() {
        return perspective;
    }
}
