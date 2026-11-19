/*
 * Copyright 2018 Baloise Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thisorthat.vote.image.selection;

import org.thisorthat.db.InMemoryDatabase;
import org.thisorthat.dto.ExhaustiveVoteState;
import org.thisorthat.dto.Image;
import org.thisorthat.dto.Pair;
import org.thisorthat.dto.Survey;
import org.thisorthat.dto.Vote;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service("exhaustiveSelectionAlgorithm")
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

    public static String canonicalPairKey(String id1, String id2) {
        if (id1 == null || id2 == null) {
            return "";
        }
        return id1.compareTo(id2) < 0 ? (id1 + "::" + id2) : (id2 + "::" + id1);
    }

    public static List<Pair<Image>> generateAllPairs(List<Image> images) {
        List<Pair<Image>> pairs = new ArrayList<>();
        if (images == null || images.size() < 2) {
            return pairs;
        }
        for (int i = 0; i < images.size(); i++) {
            for (int j = i + 1; j < images.size(); j++) {
                pairs.add(new Pair<>(images.get(i), images.get(j)));
            }
        }
        return pairs;
    }

    public static Set<String> getVotedPairKeys(List<Vote> votes, String userId) {
        if (votes == null || votes.isEmpty() || userId == null) {
            return Collections.emptySet();
        }
        return votes.stream()
                .filter(v -> userId.equals(v.getUserId()))
                .map(v -> canonicalPairKey(v.getWinner(), v.getLoser()))
                .collect(Collectors.toSet());
    }

    @Override
    public Pair<Image> getNextImagePair(String surveyCode, String userId) {
        ExhaustiveVoteState state = getVoteState(surveyCode, userId);
        if (state.completed() || state.image1() == null || state.image2() == null) {
            return null;
        }
        return new Pair<>(state.image1(), state.image2());
    }

    @Override
    public ExhaustiveVoteState getVoteState(String surveyCode, String userId) {
        if (inMemoryDatabase == null) {
            throw new IllegalStateException("InMemoryDatabase is required to evaluate by surveyCode");
        }
        Survey survey = inMemoryDatabase.getSurvey(surveyCode);
        return evaluate(survey, userId);
    }

    @Override
    public ExhaustiveVoteState getVoteState(Survey survey, String userId) {
        return evaluate(survey, userId);
    }

    public ExhaustiveVoteState evaluate(Survey survey, String userId) {
        List<Image> images = survey.getImages() != null ? survey.getImages() : Collections.emptyList();
        int totalComparisons = calculateTotalComparisons(images.size());
        List<Pair<Image>> allPairs = generateAllPairs(images);
        Set<String> votedPairKeys = getVotedPairKeys(survey.getVotes(), userId);

        int completedComparisons = (int) allPairs.stream()
                .filter(p -> votedPairKeys.contains(canonicalPairKey(p.getT1().getId(), p.getT2().getId())))
                .count();

        List<Pair<Image>> remainingPairs = allPairs.stream()
                .filter(p -> !votedPairKeys.contains(canonicalPairKey(p.getT1().getId(), p.getT2().getId())))
                .collect(Collectors.toList());

        if (remainingPairs.isEmpty()) {
            return new ExhaustiveVoteState(null, null, totalComparisons, completedComparisons, true);
        }

        Pair<Image> chosen = remainingPairs.get(random.nextInt(remainingPairs.size()));
        boolean flip = random.nextBoolean();
        Image img1 = flip ? chosen.getT2() : chosen.getT1();
        Image img2 = flip ? chosen.getT1() : chosen.getT2();

        return new ExhaustiveVoteState(img1, img2, totalComparisons, completedComparisons, false);
    }
}
