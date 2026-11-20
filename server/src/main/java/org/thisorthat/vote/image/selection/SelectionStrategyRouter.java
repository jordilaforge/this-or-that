package org.thisorthat.vote.image.selection;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.thisorthat.db.InMemoryDatabase;
import org.thisorthat.dto.Image;
import org.thisorthat.dto.Pair;
import org.thisorthat.dto.Survey;

@Service
@Primary
public class SelectionStrategyRouter implements ImageSelectionAlgorithm {

    private final InMemoryDatabase inMemoryDatabase;
    private final ExhaustiveSelectionAlgorithm exhaustiveAlgorithm;
    private final AdaptiveSelectionAlgorithm adaptiveAlgorithm;

    public SelectionStrategyRouter(InMemoryDatabase inMemoryDatabase,
                                   ExhaustiveSelectionAlgorithm exhaustiveAlgorithm,
                                   AdaptiveSelectionAlgorithm adaptiveAlgorithm) {
        this.inMemoryDatabase = inMemoryDatabase;
        this.exhaustiveAlgorithm = exhaustiveAlgorithm;
        this.adaptiveAlgorithm = adaptiveAlgorithm;
    }

    @Override
    public Pair<Image> getNextImagePair(String surveyCode, String userId) {
        Survey survey = inMemoryDatabase.getSurvey(surveyCode);
        SelectionMode effectiveMode = SelectionMode.resolveEffectiveMode(survey);

        if (effectiveMode == SelectionMode.EXHAUSTIVE) {
            return exhaustiveAlgorithm.getNextImagePair(surveyCode, userId);
        } else {
            return adaptiveAlgorithm.getNextImagePair(surveyCode, userId);
        }
    }
}
