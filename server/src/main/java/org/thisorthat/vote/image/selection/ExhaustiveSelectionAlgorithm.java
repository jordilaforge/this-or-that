package org.thisorthat.vote.image.selection;

import org.springframework.stereotype.Component;
import org.thisorthat.db.InMemoryDatabase;
import org.thisorthat.dto.Image;
import org.thisorthat.dto.Pair;
import org.thisorthat.dto.Survey;
import org.thisorthat.dto.Vote;

import java.util.*;

@Component
public class ExhaustiveSelectionAlgorithm implements ImageSelectionAlgorithm {

    private final InMemoryDatabase inMemoryDatabase;
    private final Random random = new Random();

    public ExhaustiveSelectionAlgorithm(InMemoryDatabase inMemoryDatabase) {
        this.inMemoryDatabase = inMemoryDatabase;
    }

    @Override
    public Pair<Image> getNextImagePair(String surveyCode, String userId) {
        Survey survey = inMemoryDatabase.getSurvey(surveyCode);
        List<Image> images = survey.getImages();
        if (images == null || images.size() < 2) {
            return null;
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

        if (unseenPairs.isEmpty()) {
            return null;
        }

        Pair<Image> chosenPair = unseenPairs.get(random.nextInt(unseenPairs.size()));
        if (random.nextBoolean()) {
            return new Pair<>(chosenPair.getT2(), chosenPair.getT1());
        }
        return chosenPair;
    }

    public static String buildPairKey(String idA, String idB) {
        return idA.compareTo(idB) <= 0 ? idA + ":" + idB : idB + ":" + idA;
    }

    public static Set<String> getSeenPairKeysForUser(Survey survey, String userId) {
        Set<String> keys = new HashSet<>();
        if (survey.getVotes() != null && userId != null) {
            for (Vote vote : survey.getVotes()) {
                if (userId.equals(vote.getUserId())) {
                    keys.add(buildPairKey(vote.getWinner(), vote.getLoser()));
                }
            }
        }
        return keys;
    }
}
