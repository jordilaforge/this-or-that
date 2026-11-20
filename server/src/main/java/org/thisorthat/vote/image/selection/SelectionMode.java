package org.thisorthat.vote.image.selection;

import org.thisorthat.dto.Survey;

public enum SelectionMode {
    AUTO,
    EXHAUSTIVE,
    ADAPTIVE;

    public static final int EXHAUSTIVE_THRESHOLD = 10;

    public static SelectionMode resolveEffectiveMode(Survey survey) {
        if (survey == null) {
            return EXHAUSTIVE;
        }
        SelectionMode configured = survey.getSelectionMode();
        if (configured == null || configured == AUTO) {
            int imageCount = survey.getImages() != null ? survey.getImages().size() : 0;
            return imageCount <= EXHAUSTIVE_THRESHOLD ? EXHAUSTIVE : ADAPTIVE;
        }
        return configured;
    }
}
