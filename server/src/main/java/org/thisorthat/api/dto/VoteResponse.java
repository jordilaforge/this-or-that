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
        String perspective,
        @Schema(description = "Whether the participant has finished all assigned comparisons", example = "false")
        Boolean isParticipantFinished,
        @Schema(description = "Current comparison index for this participant", example = "1")
        Integer comparisonIndex,
        @Schema(description = "Total expected comparisons for this participant", example = "20")
        Integer totalComparisons
) {
    public VoteResponse(String id1, String id2, Boolean surveyIsRunning, String perspective) {
        this(id1, id2, surveyIsRunning, perspective, false, null, null);
    }

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

    public Boolean getIsParticipantFinished() {
        return isParticipantFinished;
    }

    public Integer getComparisonIndex() {
        return comparisonIndex;
    }

    public Integer getTotalComparisons() {
        return totalComparisons;
    }
}
