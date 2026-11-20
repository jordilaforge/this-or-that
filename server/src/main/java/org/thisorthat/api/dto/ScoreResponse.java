package org.thisorthat.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.thisorthat.vote.image.selection.SelectionMode;

import java.util.List;

@Schema(description = "Current or final scores and statistics for a survey")
public record ScoreResponse(
        @Schema(description = "List of image scores sorted descending")
        List<Score> scores,
        @Schema(description = "Whether the survey is currently open for voting", example = "false")
        Boolean surveyIsRunning,
        @Schema(description = "Survey perspective/question", example = "Which logo is better?")
        String perspective,
        @Schema(description = "Total number of votes cast", example = "42")
        Integer numberOfVotes,
        @Schema(description = "Total number of unique participants", example = "5")
        Integer numberOfUsers,
        @Schema(description = "Whether ranking data is deemed statistically reliable", example = "true")
        Boolean isReliable,
        @Schema(description = "Exposure balance ratio across images (min / max comparisons)", example = "0.85")
        Double exposureBalance,
        @Schema(description = "Minimum comparison count for any single image", example = "15")
        Integer minComparisonsPerImage,
        @Schema(description = "Active image selection mode for the survey", example = "EXHAUSTIVE")
        SelectionMode selectionMode
) {
    public ScoreResponse(List<Score> scores, Boolean surveyIsRunning, String perspective, Integer numberOfVotes, Integer numberOfUsers) {
        this(scores, surveyIsRunning, perspective, numberOfVotes, numberOfUsers, null, null, null, null);
    }

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

    public Boolean getIsReliable() {
        return isReliable;
    }

    public Double getExposureBalance() {
        return exposureBalance;
    }

    public Integer getMinComparisonsPerImage() {
        return minComparisonsPerImage;
    }

    public SelectionMode getSelectionMode() {
        return selectionMode;
    }
}
