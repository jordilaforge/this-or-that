package org.thisorthat.vote.image.selection;

import org.springframework.stereotype.Component;
import org.thisorthat.db.InMemoryDatabase;
import org.thisorthat.dto.*;

import java.util.*;
import java.util.stream.Collectors;

@Component("exhaustiveSelectionAlgorithm")
public class ExhaustiveSelectionAlgorithm implements ImageSelectionAlgorithm {

    private final InMemoryDatabase inMemoryDatabase;
    private final Random random = new Random();

    public ExhaustiveSelectionAlgorithm() {
        this(null);
    }

    public ExhaustiveSelectionAlgorithm(InMemoryDatabase inMemoryDatabase) {
        this.inMemoryDatabase = inMemoryDatabase;
    }

    public static int calculateTotalComparisons(int imageCount) {
        if (imageCount < 2) {
            return 0;
        }
        return (imageCount * (imageCount - 1)) / 2;
    }

    public static String buildPairKey(String idA, String idB) {
        if (idA == null || idB == null) {
            return "";
        }
        return idA.compareTo(idB) <= 0 ? idA + ":" + idB : idB + ":" + idA;
    }

    public static String canonicalPairKey(String id1, String id2) {
        return buildPairKey(id1, id2);
    }

    public static Set<String> getSeenPairKeysForUser(Survey survey, String userId) {
        Set<String> keys = new HashSet<>();
        if (survey != null && survey.getVotes() != null && userId != null) {
            for (Vote vote : survey.getVotes()) {
                if (userId.equals(vote.getUserId())) {
                    keys.add(buildPairKey(vote.getWinner(), vote.getLoser()));
                }
            }
        }
        return keys;
    }

    public static Set<String> getVotedPairKeys(List<Vote> votes, String userId) {
        if (votes == null || votes.isEmpty() || userId == null) {
            return Collections.emptySet();
        }
        return votes.stream()
                .filter(v -> userId.equals(v.getUserId()))
                .map(v -> buildPairKey(v.getWinner(), v.getLoser()))
                .collect(Collectors.toSet());
    }

    public static List<Pair<Image>> generateAllPairs(List<Image> images) {
        List<Pair<Image>> pairs = new ArrayList<>();
        if (images == null || images.size() < 2) {
            return pairs;
        }
        for (int i = 0; i < images.size() - 1; i++) {
            for (int j = i + 1; j < images.size(); j++) {
                pairs.add(new Pair<>(images.get(i), images.get(j)));
            }
        }
        return pairs;
    }

    public ExhaustiveVoteState evaluate(Survey survey, String userId) {
        if (survey == null) {
            return new ExhaustiveVoteState(null, null, 0, 0, true);
        }
        List<Image> images = survey.getImages() != null ? survey.getImages() : Collections.emptyList();
        int totalComparisons = calculateTotalComparisons(images.size());
        if (images.size() < 2) {
            return new ExhaustiveVoteState(null, null, totalComparisons, 0, true);
        }

        Set<String> seenPairKeys = getSeenPairKeysForUser(survey, userId);
        List<Pair<Image>> unseenPairs = new ArrayList<>();

        for (int i = 0; i < images.size() - 1; i++) {
            for (int j = i + 1; j < images.size(); j++) {
                Image img1 = images.get(i);
                Image img2 = images.get(j);
                String key = buildPairKey(img1.getId(), img2.getId());
                if (!seenPairKeys.contains(key)) {
                    unseenPairs.add(new Pair<>(img1, img2));
                }
            }
        }

        int completedComparisons = totalComparisons - unseenPairs.size();

        if (unseenPairs.isEmpty()) {
            return new ExhaustiveVoteState(null, null, totalComparisons, totalComparisons, true);
        }

        Pair<Image> chosenPair = unseenPairs.get(random.nextInt(unseenPairs.size()));
        Image img1 = chosenPair.getT1();
        Image img2 = chosenPair.getT2();
        if (random.nextBoolean()) {
            img1 = chosenPair.getT2();
            img2 = chosenPair.getT1();
        }

        return new ExhaustiveVoteState(img1, img2, totalComparisons, completedComparisons, false);
    }

    public ExhaustiveVoteState getVoteState(String surveyCode, String userId) {
        if (inMemoryDatabase == null) {
            throw new IllegalStateException("InMemoryDatabase is required to evaluate by surveyCode");
        }
        Survey survey = inMemoryDatabase.getSurvey(surveyCode);
        return evaluate(survey, userId);
    }

    public ExhaustiveVoteState getVoteState(Survey survey, String userId) {
        return evaluate(survey, userId);
    }

    @Override
    public Pair<Image> getNextImagePair(String surveyCode, String userId) {
        if (inMemoryDatabase == null) {
            return null;
        }
        Survey survey = inMemoryDatabase.getSurvey(surveyCode);
        ExhaustiveVoteState state = evaluate(survey, userId);
        if (state.completed() || state.image1() == null || state.image2() == null) {
            return null;
        }
        return new Pair<>(state.image1(), state.image2());
    }
}
