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
package org.thisorthat.vote;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.thisorthat.db.InMemoryDatabase;
import org.thisorthat.dto.*;
import org.thisorthat.exception.SurveyStoppedException;
import org.thisorthat.vote.elo.EloCalculator;
import org.thisorthat.vote.elo.EloDelta;
import org.thisorthat.vote.image.selection.ImageSelectionAlgorithm;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class SimpleVoteAlgorithm implements VoteAlgorithm {

    private static final Logger log = LoggerFactory.getLogger(SimpleVoteAlgorithm.class);

    private final ImageSelectionAlgorithm imageSelectionAlgorithm;
    private final InMemoryDatabase inMemoryDatabase;

    public SimpleVoteAlgorithm(@Qualifier("randomSelectionAlgorithm") ImageSelectionAlgorithm imageSelectionAlgorithm,
                               InMemoryDatabase inMemoryDatabase) {
        this.imageSelectionAlgorithm = imageSelectionAlgorithm;
        this.inMemoryDatabase = inMemoryDatabase;
    }

    @Override
    public void initialize(String surveyCode) {
        Survey survey = inMemoryDatabase.getSurvey(surveyCode);
        List<ScoreItem> scores = new ArrayList<>();
        survey.getImages().forEach(image -> scores.add(new ScoreItem(image.getId(), EloCalculator.DEFAULT_INITIAL_RATING)));
        survey.setScores(scores);
        log.info("Initialized survey {} with initial Elo score {}", survey.getId(), EloCalculator.DEFAULT_INITIAL_RATING);
    }

    @Override
    public VoteItem getVote(String surveyCode, String userId) {
        Pair<Image> images = imageSelectionAlgorithm.getNextImagePair(surveyCode, userId);
        if (images == null) {
            return null;
        }
        return new VoteItem(images.getT1(), images.getT2());
    }

    @Override
    public void setVote(String surveyCode, String imageIdWinner, String imageIdLooser, String userId) {
        Survey survey = inMemoryDatabase.getSurvey(surveyCode);
        applyLiveVote(survey, imageIdWinner, imageIdLooser);
        inMemoryDatabase.persistVote(surveyCode, new Vote(imageIdWinner, imageIdLooser, userId));
        log.info("voted for {} winner {} looser {}", surveyCode, imageIdWinner, imageIdLooser);
    }

    @Override
    public List<ScoreItem> calculateScores(String surveyCode) {
        Survey survey = inMemoryDatabase.getSurvey(surveyCode);
        Map<String, Double> ratings = new HashMap<>();

        for (Image image : survey.getImages()) {
            ratings.put(image.getId(), (double) EloCalculator.DEFAULT_INITIAL_RATING);
        }

        for (Vote vote : survey.getVotes()) {
            double currentWinner = ratings.getOrDefault(vote.getWinner(), (double) EloCalculator.DEFAULT_INITIAL_RATING);
            double currentLoser = ratings.getOrDefault(vote.getLoser(), (double) EloCalculator.DEFAULT_INITIAL_RATING);
            EloDelta delta = EloCalculator.calculate(currentWinner, currentLoser, EloCalculator.DEFAULT_K_FACTOR);
            ratings.put(vote.getWinner(), delta.newWinnerRating());
            ratings.put(vote.getLoser(), delta.newLoserRating());
        }

        return ratings.entrySet().stream()
                .map(entry -> new ScoreItem(entry.getKey(), (int) Math.round(entry.getValue())))
                .sorted((a, b) -> Integer.compare(b.score(), a.score()))
                .collect(Collectors.toList());
    }

    public void calculateScoresAndStopSurvey(String surveyCode) {
        Survey survey = inMemoryDatabase.getSurvey(surveyCode);
        if (!survey.getStarted()) {
            throw new SurveyStoppedException("survey " + surveyCode + " already stopped");
        }
        survey.setScores(calculateScores(surveyCode));
        inMemoryDatabase.stopSurvey(surveyCode);
    }

    private void applyLiveVote(Survey survey, String winnerId, String loserId) {
        Map<String, Integer> scoreMap = new HashMap<>();
        if (survey.getScores() != null) {
            for (ScoreItem item : survey.getScores()) {
                scoreMap.put(item.getImageId(), item.getScore());
            }
        }

        int winnerScore = scoreMap.getOrDefault(winnerId, EloCalculator.DEFAULT_INITIAL_RATING);
        int loserScore = scoreMap.getOrDefault(loserId, EloCalculator.DEFAULT_INITIAL_RATING);

        EloDelta delta = EloCalculator.calculate(winnerScore, loserScore, EloCalculator.DEFAULT_K_FACTOR);
        scoreMap.put(winnerId, (int) Math.round(delta.newWinnerRating()));
        scoreMap.put(loserId, (int) Math.round(delta.newLoserRating()));

        List<ScoreItem> updatedScores = scoreMap.entrySet().stream()
                .map(entry -> new ScoreItem(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> Integer.compare(b.score(), a.score()))
                .collect(Collectors.toList());
        survey.setScores(updatedScores);
    }
}
