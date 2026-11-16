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
package com.baloise.open.thisorthat.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Survey score results and statistics")
public record ScoreResponse(
        @Schema(description = "List of image scores ordered from highest to lowest")
        List<Score> scores,
        @Schema(description = "Whether the survey is currently active", example = "false")
        Boolean surveyIsRunning,
        @Schema(description = "Perspective / question of the survey", example = "Which logo is better?")
        String perspective,
        @Schema(description = "Total number of votes registered", example = "10")
        Integer numberOfVotes,
        @Schema(description = "Total number of unique users who voted", example = "3")
        Integer numberOfUsers
) {
    public List<Score> getScores() {
        return scores;
    }

    public Boolean getSurveyIsRunning() {
        return surveyIsRunning;
    }

    public String getPerspective() {
        return perspective;
    }

    public Integer getNumberOfVotes() {
        return numberOfVotes;
    }

    public Integer getNumberOfUsers() {
        return numberOfUsers;
    }
}
