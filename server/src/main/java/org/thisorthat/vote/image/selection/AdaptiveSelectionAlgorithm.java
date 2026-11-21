package org.thisorthat.vote.image.selection;

import org.springframework.stereotype.Component;
import org.thisorthat.db.InMemoryDatabase;
import org.thisorthat.dto.*;
import org.thisorthat.vote.elo.EloCalculator;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class AdaptiveSelectionAlgorithm implements ImageSelectionAlgorithm {

    public static final int DEFAULT_MAX_COMPARISONS_PER_USER = 20;
    public static final int EXPLORATION_EXPOSURE_THRESHOLD = 3;
    public static final double RANDOM_EXPLORATION_PROBABILITY = 0.15;

    private final InMemoryDatabase inMemoryDatabase;
    private final Random random = new Random();

    public AdaptiveSelectionAlgorithm(InMemoryDatabase inMemoryDatabase) {
        this.inMemoryDatabase = inMemoryDatabase;
    }

    @Override
    public Pair<Image> getNextImagePair(String surveyCode, String userId) {
        Survey survey = inMemoryDatabase.getSurvey(surveyCode);
        List<Image> images = survey.getImages();
        if (images == null || images.size() < 2) {
            return null;
        }

        if (survey.getMaxComparisonsPerUser() != null) {
            long userVoteCount = survey.getVotes().stream()
                    .filter(vote -> userId.equals(vote.getUserId()))
                    .count();

            if (userVoteCount >= survey.getMaxComparisonsPerUser()) {
                return null;
            }
        }

        Set<String> seenPairKeys = ExhaustiveSelectionAlgorithm.getSeenPairKeysForUser(survey, userId);
        List<Pair<Image>> allPairs = new ArrayList<>();
        List<Pair<Image>> unseenCandidatePairs = new ArrayList<>();

        for (int i = 0; i < images.size() - 1; i++) {
            for (int j = i + 1; j < images.size(); j++) {
                Image img1 = images.get(i);
                Image img2 = images.get(j);
                Pair<Image> pair = new Pair<>(img1, img2);
                allPairs.add(pair);
                String key = ExhaustiveSelectionAlgorithm.buildPairKey(img1.getId(), img2.getId());
                if (!seenPairKeys.contains(key)) {
                    unseenCandidatePairs.add(pair);
                }
            }
        }

        List<Pair<Image>> candidatePairs = unseenCandidatePairs.isEmpty() ? allPairs : unseenCandidatePairs;
        if (candidatePairs.isEmpty()) {
            return null;
        }

        Map<String, Integer> exposureCounts = calculateImageExposure(survey);
        Map<String, Integer> ratings = getImageRatings(survey);

        Pair<Image> chosenPair = selectPairByAdaptiveStrategy(candidatePairs, exposureCounts, ratings);

        if (random.nextBoolean()) {
            return new Pair<>(chosenPair.getT2(), chosenPair.getT1());
        }
        return chosenPair;
    }

    private Pair<Image> selectPairByAdaptiveStrategy(
            List<Pair<Image>> candidates,
            Map<String, Integer> exposureCounts,
            Map<String, Integer> ratings) {

        boolean hasUnderexposedImages = exposureCounts.values().stream()
                .anyMatch(count -> count < EXPLORATION_EXPOSURE_THRESHOLD);

        if (hasUnderexposedImages) {
            int minCombinedExposure = Integer.MAX_VALUE;
            List<Pair<Image>> lowestExposurePairs = new ArrayList<>();

            for (Pair<Image> pair : candidates) {
                int exp1 = exposureCounts.getOrDefault(pair.getT1().getId(), 0);
                int exp2 = exposureCounts.getOrDefault(pair.getT2().getId(), 0);
                int combined = exp1 + exp2;

                if (exp1 < EXPLORATION_EXPOSURE_THRESHOLD || exp2 < EXPLORATION_EXPOSURE_THRESHOLD) {
                    if (combined < minCombinedExposure) {
                        minCombinedExposure = combined;
                        lowestExposurePairs.clear();
                        lowestExposurePairs.add(pair);
                    } else if (combined == minCombinedExposure) {
                        lowestExposurePairs.add(pair);
                    }
                }
            }

            if (!lowestExposurePairs.isEmpty()) {
                return lowestExposurePairs.get(random.nextInt(lowestExposurePairs.size()));
            }
        }

        if (random.nextDouble() < RANDOM_EXPLORATION_PROBABILITY) {
            return candidates.get(random.nextInt(candidates.size()));
        }

        int minRatingDelta = Integer.MAX_VALUE;
        List<Pair<Image>> closestRatingPairs = new ArrayList<>();

        for (Pair<Image> pair : candidates) {
            int r1 = ratings.getOrDefault(pair.getT1().getId(), EloCalculator.DEFAULT_INITIAL_RATING);
            int r2 = ratings.getOrDefault(pair.getT2().getId(), EloCalculator.DEFAULT_INITIAL_RATING);
            int delta = Math.abs(r1 - r2);

            if (delta < minRatingDelta) {
                minRatingDelta = delta;
                closestRatingPairs.clear();
                closestRatingPairs.add(pair);
            } else if (delta == minRatingDelta) {
                closestRatingPairs.add(pair);
            }
        }

        return closestRatingPairs.get(random.nextInt(closestRatingPairs.size()));
    }

    public static Map<String, Integer> calculateImageExposure(Survey survey) {
        Map<String, Integer> counts = new HashMap<>();
        if (survey.getImages() != null) {
            for (Image img : survey.getImages()) {
                counts.put(img.getId(), 0);
            }
        }
        if (survey.getVotes() != null) {
            for (Vote vote : survey.getVotes()) {
                counts.computeIfPresent(vote.getWinner(), (k, v) -> v + 1);
                counts.computeIfPresent(vote.getLoser(), (k, v) -> v + 1);
            }
        }
        return counts;
    }

    private Map<String, Integer> getImageRatings(Survey survey) {
        Map<String, Integer> map = new HashMap<>();
        if (survey.getScores() != null) {
            for (ScoreItem scoreItem : survey.getScores()) {
                map.put(scoreItem.getImageId(), scoreItem.getScore());
            }
        }
        return map;
    }
}
