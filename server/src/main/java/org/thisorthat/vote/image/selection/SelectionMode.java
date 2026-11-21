package org.thisorthat.vote.image.selection;

import org.thisorthat.dto.Survey;
import org.thisorthat.dto.VotingMode;

public enum SelectionMode {
    AUTO,
    EXHAUSTIVE,
    ADAPTIVE;

    public static final int EXHAUSTIVE_THRESHOLD = 10;

    public static SelectionMode resolveEffectiveMode(Survey survey) {
        if (survey == null) {
            return EXHAUSTIVE;
        }
        if (survey.getVotingMode() == VotingMode.EXHAUSTIVE) {
            return EXHAUSTIVE;
        }
        if (survey.getVotingMode() == VotingMode.ENDLESS) {
            return ADAPTIVE;
        }
        SelectionMode configured = survey.getSelectionMode();
        if (configured == null || configured == AUTO) {
            int imageCount = survey.getImages() != null ? survey.getImages().size() : 0;
            return imageCount <= EXHAUSTIVE_THRESHOLD ? EXHAUSTIVE : ADAPTIVE;
        }
        return configured;
    }
}
