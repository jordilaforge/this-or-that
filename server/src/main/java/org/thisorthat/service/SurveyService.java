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
package org.thisorthat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.thisorthat.api.dto.Score;
import org.thisorthat.api.dto.ScoreResponse;
import org.thisorthat.api.dto.VoteRequest;
import org.thisorthat.api.dto.VoteResponse;
import org.thisorthat.db.InMemoryDatabase;
import org.thisorthat.dto.Image;
import org.thisorthat.dto.Survey;
import org.thisorthat.dto.VoteItem;
import org.thisorthat.exception.*;
import org.thisorthat.vote.SimpleVoteAlgorithm;
import org.thisorthat.vote.image.selection.AdaptiveSelectionAlgorithm;
import org.thisorthat.vote.image.selection.SelectionMode;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SurveyService {

    private static final Logger log = LoggerFactory.getLogger(SurveyService.class);

    private final InMemoryDatabase inMemoryDatabase;
    private final SimpleVoteAlgorithm randomAlgorithm;
    private final CampaignQualityService campaignQualityService;

    public SurveyService(InMemoryDatabase inMemoryDatabase,
                         SimpleVoteAlgorithm randomAlgorithm,
                         CampaignQualityService campaignQualityService) {
        this.inMemoryDatabase = inMemoryDatabase;
        this.randomAlgorithm = randomAlgorithm;
        this.campaignQualityService = campaignQualityService;
    }

    public Survey createSurvey(String perspective) {
        return createSurvey(perspective, SelectionMode.AUTO, null);
    }

    public Survey createSurvey(String perspective, SelectionMode selectionMode, Integer maxComparisonsPerUser) {
        log.info("create survey with perspective {}, mode {}", perspective, selectionMode);
        long index = inMemoryDatabase.surveyCount();
        String code = CodeCreator.createCode(index);
        log.info("code created: {}", code);
        Survey survey = new Survey(code, perspective);
        survey.setSelectionMode(selectionMode != null ? selectionMode : SelectionMode.AUTO);
        survey.setMaxComparisonsPerUser(maxComparisonsPerUser);
        inMemoryDatabase.addSurvey(survey);
        log.info("added survey to inMemory");
        return survey;
    }

    public void startSurvey(String surveyCode) {
        checkIfSurveyIsAlreadyStarted(surveyCode);
        inMemoryDatabase.startSurvey(surveyCode);
        randomAlgorithm.initialize(surveyCode);
        log.info("survey {} started and initialized", surveyCode);
    }

    public void stopSurvey(String surveyCode) {
        checkIfSurveyIsStopped(surveyCode);
        randomAlgorithm.calculateScoresAndStopSurvey(surveyCode);
        log.info("survey {} stopped", surveyCode);
    }

    public VoteResponse getVote(String surveyCode, String userId) {
        checkIfSurveyIsStopped(surveyCode);
        Survey survey = inMemoryDatabase.getSurvey(surveyCode);
        VoteItem voteItem = randomAlgorithm.getVote(surveyCode, userId);

        int userVoteCount = getComparisonCountForUser(survey, userId);
        int totalExpected = getTotalExpectedComparisons(survey);

        if (voteItem == null) {
            log.info("survey {} participant {} has completed comparisons", surveyCode, userId);
            return new VoteResponse(
                    null,
                    null,
                    true,
                    survey.getPerspective(),
                    true,
                    userVoteCount,
                    totalExpected
            );
        }

        log.info("survey {} get vote from {} image1: {} image2: {}",
                surveyCode, userId, voteItem.getFile1().getId(), voteItem.getFile2().getId());
        return new VoteResponse(
                voteItem.getFile1().getId(),
                voteItem.getFile2().getId(),
                true,
                survey.getPerspective(),
                false,
                userVoteCount + 1,
                totalExpected
        );
    }

    public void setVote(String surveyCode, VoteRequest voteRequest, String userId) {
        checkIfSurveyIsStopped(surveyCode);
        randomAlgorithm.setVote(surveyCode, voteRequest.getWinner(), voteRequest.getLoser(), userId);
        log.info("survey {} set vote by {} winnerId: {} looserId: {}",
                surveyCode, userId, voteRequest.getWinner(), voteRequest.getLoser());
    }

    public ScoreResponse getScore(String surveyCode) {
        Survey survey = getSurvey(surveyCode);
        if (survey.getStarted() != null && survey.getStarted()) {
            throw new SurveyStillRunningException("survey " + surveyCode + " is still running:");
        }

        if (survey.getVotes() == null || survey.getScores() == null) {
            throw new SurveyIncompleteException("Incomplete survey");
        }

        Set<String> userIdSet = new HashSet<>();
        survey.getVotes().forEach(vote -> userIdSet.add(vote.getUserId()));

        List<Score> scores = survey.getScores().stream()
                .map(scoreItem -> {
                    Image file = getImageFromSurvey(survey, scoreItem.getImageId());
                    return new Score(file.getId(), scoreItem.getScore());
                })
                .sorted((score1, score2) -> score2.getScore().compareTo(score1.getScore()))
                .collect(Collectors.toList());

        log.info("survey {} getScore", surveyCode);
        return new ScoreResponse(
                scores,
                false,
                survey.getPerspective(),
                survey.getVotes().size(),
                userIdSet.size(),
                campaignQualityService.isReliable(survey),
                campaignQualityService.getExposureBalanceRatio(survey),
                campaignQualityService.getMinComparisons(survey),
                SelectionMode.resolveEffectiveMode(survey)
        );
    }

    private Survey getSurvey(String surveyCode) {
        return inMemoryDatabase.getSurvey(surveyCode);
    }

    private Image getImageFromSurvey(Survey survey, String imageId) {
        log.info("survey {} get image {}", survey.getId(), imageId);
        return survey.getImages().stream()
                .filter(image -> image.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new ImageNotFoundException("survey " + survey.getId() + " image " + imageId + " not found"));
    }

    public Image getImageFromSurvey(String surveyCode, String imageId) {
        Survey survey = getSurvey(surveyCode);
        return getImageFromSurvey(survey, imageId);
    }

    public String addImageToSurvey(String surveyCode, Image image) {
        log.info("survey {} added image", surveyCode);
        return inMemoryDatabase.addImageToSurvey(surveyCode, image);
    }

    public void deleteSurvey(String surveyCode) {
        checkIfSurveyIsStopped(surveyCode);
        log.info("survey {} deleted", surveyCode);
        inMemoryDatabase.removeSurvey(surveyCode);
    }

    private void checkIfSurveyIsAlreadyStarted(String surveyCode) {
        if (inMemoryDatabase.getSurvey(surveyCode).getStarted() != null && inMemoryDatabase.getSurvey(surveyCode).getStarted()) {
            log.warn("survey {} is not running", surveyCode);
            throw new SurveyAlreadyStartedException("survey " + surveyCode + " is already started");
        }
    }

    private void checkIfSurveyIsStopped(String surveyCode) {
        if (inMemoryDatabase.getSurvey(surveyCode).getStarted() != null && !inMemoryDatabase.getSurvey(surveyCode).getStarted()) {
            log.warn("survey {} is not running", surveyCode);
            throw new SurveyStoppedException("survey " + surveyCode + " is already stopped");
        }
    }

    private int getComparisonCountForUser(Survey survey, String userId) {
        if (survey.getVotes() == null || userId == null) {
            return 0;
        }
        return (int) survey.getVotes().stream().filter(v -> userId.equals(v.getUserId())).count();
    }

    private int getTotalExpectedComparisons(Survey survey) {
        SelectionMode mode = SelectionMode.resolveEffectiveMode(survey);
        int imageCount = survey.getImages() != null ? survey.getImages().size() : 0;
        int totalPairs = imageCount > 1 ? (imageCount * (imageCount - 1)) / 2 : 0;

        if (mode == SelectionMode.EXHAUSTIVE) {
            return totalPairs;
        } else {
            int maxAdaptive = survey.getMaxComparisonsPerUser() != null
                    ? survey.getMaxComparisonsPerUser()
                    : AdaptiveSelectionAlgorithm.DEFAULT_MAX_COMPARISONS_PER_USER;
            return Math.min(totalPairs, maxAdaptive);
        }
    }
}
