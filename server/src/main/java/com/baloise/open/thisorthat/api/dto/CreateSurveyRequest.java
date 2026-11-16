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

@Schema(description = "Payload to create a new voting survey")
public record CreateSurveyRequest(
        @Schema(description = "Question or topic perspective for this survey", example = "Which logo is better?", requiredMode = Schema.RequiredMode.REQUIRED)
        String perspective
) {
    public String getPerspective() {
        return perspective;
    }
}
