package org.thisorthat.service;

import org.springframework.stereotype.Service;
import org.thisorthat.dto.Image;
import org.thisorthat.dto.Survey;
import org.thisorthat.vote.image.selection.AdaptiveSelectionAlgorithm;
import org.thisorthat.vote.image.selection.SelectionMode;

import java.util.Map;

@Service
public class CampaignQualityService {

    public static final int RELIABILITY_MIN_COMPARISONS_ADAPTIVE = 15;
    public static final double RELIABILITY_MIN_BALANCE_RATIO = 0.5;

    public Map<String, Integer> getExposureCounts(Survey survey) {
        return AdaptiveSelectionAlgorithm.calculateImageExposure(survey);
    }

    public int getMinComparisons(Survey survey) {
        Map<String, Integer> exposures = getExposureCounts(survey);
        return exposures.values().stream().min(Integer::compare).orElse(0);
    }

    public int getMaxComparisons(Survey survey) {
        Map<String, Integer> exposures = getExposureCounts(survey);
        return exposures.values().stream().max(Integer::compare).orElse(0);
    }

    public double getAverageComparisons(Survey survey) {
        Map<String, Integer> exposures = getExposureCounts(survey);
        return exposures.values().stream().mapToInt(Integer::intValue).average().orElse(0.0);
    }

    public double getExposureBalanceRatio(Survey survey) {
        int min = getMinComparisons(survey);
        int max = getMaxComparisons(survey);
        if (max == 0) {
            return 1.0;
        }
        return (double) min / (double) max;
    }

    public boolean isReliable(Survey survey) {
        if (survey.getImages() == null || survey.getImages().size() < 2) {
            return false;
        }

        SelectionMode mode = SelectionMode.resolveEffectiveMode(survey);
        int imageCount = survey.getImages().size();

        if (mode == SelectionMode.EXHAUSTIVE) {
            int uniquePairs = (imageCount * (imageCount - 1)) / 2;
            int totalVotes = survey.getVotes() != null ? survey.getVotes().size() : 0;
            return totalVotes >= uniquePairs && getMinComparisons(survey) >= (imageCount - 1);
        } else {
            return getMinComparisons(survey) >= RELIABILITY_MIN_COMPARISONS_ADAPTIVE
                    && getExposureBalanceRatio(survey) >= RELIABILITY_MIN_BALANCE_RATIO;
        }
    }
}
