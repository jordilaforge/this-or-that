package org.thisorthat.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.thisorthat.dto.VotingMode;
import org.thisorthat.vote.image.selection.SelectionMode;

@Schema(description = "Payload to create a new survey")
public record CreateSurveyRequest(
        @Schema(description = "Question or topic for the survey", example = "Which logo looks better?")
        String perspective,
        @Schema(description = "Image selection mode (AUTO, EXHAUSTIVE, ADAPTIVE)", example = "AUTO")
        SelectionMode selectionMode,
        @Schema(description = "Maximum comparisons per user for adaptive mode", example = "20")
        Integer maxComparisonsPerUser,
        @Schema(description = "Voting mode of the survey (EXHAUSTIVE, ENDLESS)", example = "EXHAUSTIVE")
        VotingMode votingMode
) {
    public CreateSurveyRequest(String perspective) {
        this(perspective, SelectionMode.AUTO, null, null);
    }

    public CreateSurveyRequest(String perspective, SelectionMode selectionMode, Integer maxComparisonsPerUser) {
        this(perspective, selectionMode, maxComparisonsPerUser, null);
    }

    public String getPerspective() {
        return perspective;
    }

    public SelectionMode getSelectionMode() {
        return selectionMode;
    }

    public Integer getMaxComparisonsPerUser() {
        return maxComparisonsPerUser;
    }

    public VotingMode getVotingMode() {
        return votingMode;
    }
}
