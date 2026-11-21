package org.thisorthat.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.thisorthat.dto.VotingMode;

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
        @Schema(description = "Voting mode: EXHAUSTIVE or ENDLESS", example = "EXHAUSTIVE")
        VotingMode votingMode,
        @Schema(description = "Total number of pairwise comparisons (exhaustive mode only)", example = "45")
        Integer totalComparisons,
        @Schema(description = "Number of pairwise comparisons completed by this user", example = "5")
        Integer completedComparisons,
        @Schema(description = "Whether the user has completed all comparisons in exhaustive mode", example = "false")
        Boolean completed,
        @Schema(description = "Whether the participant has finished all assigned comparisons", example = "false")
        Boolean isParticipantFinished,
        @Schema(description = "Current comparison index for this participant", example = "1")
        Integer comparisonIndex
) {
    public VoteResponse(String id1, String id2, Boolean surveyIsRunning, String perspective) {
        this(id1, id2, surveyIsRunning, perspective, VotingMode.ENDLESS, null, 0, false, false, null);
    }

    public VoteResponse(String id1, String id2, Boolean surveyIsRunning, String perspective,
                        Boolean isParticipantFinished, Integer comparisonIndex, Integer totalComparisons) {
        this(id1, id2, surveyIsRunning, perspective,
                VotingMode.EXHAUSTIVE,
                totalComparisons,
                comparisonIndex != null ? Math.max(0, comparisonIndex - 1) : 0,
                isParticipantFinished != null && isParticipantFinished,
                isParticipantFinished,
                comparisonIndex);
    }

    public VoteResponse(String id1, String id2, Boolean surveyIsRunning, String perspective,
                        VotingMode votingMode, Integer totalComparisons, Integer completedComparisons, Boolean completed) {
        this(id1, id2, surveyIsRunning, perspective, votingMode, totalComparisons, completedComparisons, completed,
                completed,
                completedComparisons != null ? (completed != null && completed ? completedComparisons : completedComparisons + 1) : null);
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

    public VotingMode getVotingMode() {
        return votingMode;
    }

    public Integer getTotalComparisons() {
        return totalComparisons;
    }

    public Integer getCompletedComparisons() {
        return completedComparisons;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public Boolean getIsParticipantFinished() {
        return isParticipantFinished;
    }

    public Integer getComparisonIndex() {
        return comparisonIndex;
    }
}
