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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thisorthat.api.dto.ScoreResponse;
import org.thisorthat.api.dto.VoteRequest;
import org.thisorthat.api.dto.VoteResponse;
import org.thisorthat.db.InMemoryDatabase;
import org.thisorthat.dto.*;
import org.thisorthat.exception.*;
import org.thisorthat.vote.SimpleVoteAlgorithm;
import org.thisorthat.vote.image.selection.ExhaustiveSelectionAlgorithm;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SurveyServiceTest {

    @Mock
    private InMemoryDatabase inMemoryDatabase;

    @Mock
    private SimpleVoteAlgorithm randomAlgorithm;

    @Mock
    private ExhaustiveSelectionAlgorithm exhaustiveSelectionAlgorithm;

    @Spy
    private CampaignQualityService campaignQualityService = new CampaignQualityService();

    private SurveyService surveyService;

    @BeforeEach
    void setUp() {
        surveyService = new SurveyService(inMemoryDatabase, randomAlgorithm, campaignQualityService, exhaustiveSelectionAlgorithm, 45);
    }

    @Test
    @DisplayName("createSurvey should generate code and save survey to database")
    void testCreateSurvey() {
        when(inMemoryDatabase.surveyCount()).thenReturn(0L);

        Survey created = surveyService.createSurvey("Test Perspective");

        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals("Test Perspective", created.getPerspective());
        assertFalse(created.getStarted());

        ArgumentCaptor<Survey> captor = ArgumentCaptor.forClass(Survey.class);
        verify(inMemoryDatabase).addSurvey(captor.capture());
        assertEquals(created.getId(), captor.getValue().getId());
    }

    @Test
    @DisplayName("startSurvey should start and initialize survey when not yet started")
    void testStartSurveySuccess() {
        String code = "srv-1";
        Survey survey = new Survey(new Date(), code, new ArrayList<>(), false, new ArrayList<>(), new ArrayList<>(), "P");
        when(inMemoryDatabase.getSurvey(code)).thenReturn(survey);

        surveyService.startSurvey(code);

        verify(inMemoryDatabase).startSurvey(code);
        verify(randomAlgorithm).initialize(code);
    }

    @Test
    @DisplayName("startSurvey should throw SurveyAlreadyStartedException when already started")
    void testStartSurveyAlreadyStarted() {
        String code = "srv-1";
        Survey survey = new Survey(new Date(), code, new ArrayList<>(), true, new ArrayList<>(), new ArrayList<>(), "P");
        when(inMemoryDatabase.getSurvey(code)).thenReturn(survey);

        assertThrows(SurveyAlreadyStartedException.class, () -> surveyService.startSurvey(code));
        verify(inMemoryDatabase, never()).startSurvey(code);
    }

    @Test
    @DisplayName("stopSurvey should calculate scores and stop survey when running")
    void testStopSurveySuccess() {
        String code = "srv-1";
        Survey survey = new Survey(new Date(), code, new ArrayList<>(), true, new ArrayList<>(), new ArrayList<>(), "P");
        when(inMemoryDatabase.getSurvey(code)).thenReturn(survey);

        surveyService.stopSurvey(code);

        verify(randomAlgorithm).calculateScoresAndStopSurvey(code);
    }

    @Test
    @DisplayName("stopSurvey should throw SurveyStoppedException when already stopped")
    void testStopSurveyAlreadyStopped() {
        String code = "srv-1";
        Survey survey = new Survey(new Date(), code, new ArrayList<>(), false, new ArrayList<>(), new ArrayList<>(), "P");
        when(inMemoryDatabase.getSurvey(code)).thenReturn(survey);

        assertThrows(SurveyStoppedException.class, () -> surveyService.stopSurvey(code));
        verify(randomAlgorithm, never()).calculateScoresAndStopSurvey(code);
    }

    @Test
    @DisplayName("getVote should return VoteResponse when survey is running in endless mode")
    void testGetVoteSuccess() {
        String code = "srv-1";
        String userId = "user-1";
        Survey survey = new Survey(new Date(), code, new ArrayList<>(), true, new ArrayList<>(), new ArrayList<>(), "Perspective");
        survey.setVotingMode(VotingMode.ENDLESS);
        when(inMemoryDatabase.getSurvey(code)).thenReturn(survey);

        Image img1 = new Image("0", "file0");
        Image img2 = new Image("1", "file1");
        when(randomAlgorithm.getVote(code, userId)).thenReturn(new VoteItem(img1, img2));

        VoteResponse response = surveyService.getVote(code, userId);

        assertNotNull(response);
        assertEquals("0", response.id1());
        assertEquals("1", response.id2());
        assertTrue(response.surveyIsRunning());
        assertEquals("Perspective", response.perspective());
        assertEquals(VotingMode.ENDLESS, response.votingMode());
    }

    @Test
    @DisplayName("getVote in endless mode should return participant finished when limit reached and null pairs")
    void testGetVoteParticipantFinished() {
        String code = "srv-1";
        String userId = "user-1";
        Survey survey = new Survey(new Date(), code, new ArrayList<>(), true, new ArrayList<>(), new ArrayList<>(), "Perspective");
        survey.setVotingMode(VotingMode.ENDLESS);
        when(inMemoryDatabase.getSurvey(code)).thenReturn(survey);
        when(randomAlgorithm.getVote(code, userId)).thenReturn(null);

        VoteResponse response = surveyService.getVote(code, userId);

        assertNotNull(response);
        assertNull(response.id1());
        assertNull(response.id2());
        assertTrue(response.getIsParticipantFinished());
    }

    @Test
    @DisplayName("determineVotingMode should return EXHAUSTIVE when comparisons <= threshold")
    void testDetermineVotingModeExhaustive() {
        List<Image> images = List.of(
                new Image("0", "f0"),
                new Image("1", "f1"),
                new Image("2", "f2"),
                new Image("3", "f3"),
                new Image("4", "f4")
        );
        Survey survey = new Survey(new Date(), "srv-1", images, false, new ArrayList<>(), new ArrayList<>(), "P");
        assertEquals(VotingMode.EXHAUSTIVE, surveyService.determineVotingMode(survey));
    }

    @Test
    @DisplayName("determineVotingMode should return ENDLESS when comparisons > threshold")
    void testDetermineVotingModeEndless() {
        List<Image> images = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            images.add(new Image(String.valueOf(i), "f" + i));
        }
        Survey survey = new Survey(new Date(), "srv-1", images, false, new ArrayList<>(), new ArrayList<>(), "P");
        assertEquals(VotingMode.ENDLESS, surveyService.determineVotingMode(survey));
    }

    @Test
    @DisplayName("getVote in EXHAUSTIVE mode should return progress and pairs from ExhaustiveSelectionAlgorithm")
    void testGetVoteExhaustiveMode() {
        String code = "srv-ex";
        String userId = "user-1";
        List<Image> images = List.of(new Image("0", "f0"), new Image("1", "f1"), new Image("2", "f2"));
        Survey survey = new Survey(new Date(), code, images, true, new ArrayList<>(), new ArrayList<>(), "P");
        survey.setVotingMode(VotingMode.EXHAUSTIVE);
        when(inMemoryDatabase.getSurvey(code)).thenReturn(survey);

        when(exhaustiveSelectionAlgorithm.evaluate(survey, userId))
                .thenReturn(new ExhaustiveVoteState(new Image("0", "f0"), new Image("1", "f1"), 3, 1, false));

        VoteResponse response = surveyService.getVote(code, userId);

        assertNotNull(response);
        assertEquals(VotingMode.EXHAUSTIVE, response.votingMode());
        assertEquals("0", response.id1());
        assertEquals("1", response.id2());
        assertEquals(3, response.totalComparisons());
        assertEquals(1, response.completedComparisons());
        assertFalse(response.completed());
    }

    @Test
    @DisplayName("getVote in EXHAUSTIVE mode when completed should return completed flag and null IDs")
    void testGetVoteExhaustiveModeCompleted() {
        String code = "srv-ex";
        String userId = "user-1";
        List<Image> images = List.of(new Image("0", "f0"), new Image("1", "f1"), new Image("2", "f2"));
        Survey survey = new Survey(new Date(), code, images, true, new ArrayList<>(), new ArrayList<>(), "P");
        survey.setVotingMode(VotingMode.EXHAUSTIVE);
        when(inMemoryDatabase.getSurvey(code)).thenReturn(survey);

        when(exhaustiveSelectionAlgorithm.evaluate(survey, userId))
                .thenReturn(new ExhaustiveVoteState(null, null, 3, 3, true));

        VoteResponse response = surveyService.getVote(code, userId);

        assertNotNull(response);
        assertEquals(VotingMode.EXHAUSTIVE, response.votingMode());
        assertNull(response.id1());
        assertNull(response.id2());
        assertEquals(3, response.totalComparisons());
        assertEquals(3, response.completedComparisons());
        assertTrue(response.completed());
    }

    @Test
    @DisplayName("startSurvey should set votingMode on survey")
    void testStartSurveySetsVotingMode() {
        String code = "srv-start";
        List<Image> images = List.of(new Image("0", "f0"), new Image("1", "f1"));
        Survey survey = new Survey(new Date(), code, images, false, new ArrayList<>(), new ArrayList<>(), "P");
        when(inMemoryDatabase.getSurvey(code)).thenReturn(survey);

        surveyService.startSurvey(code);

        assertEquals(VotingMode.EXHAUSTIVE, survey.getVotingMode());
        verify(inMemoryDatabase).startSurvey(code);
        verify(randomAlgorithm).initialize(code);
    }

    @Test
    @DisplayName("getVote should throw SurveyStoppedException when survey is stopped")
    void testGetVoteStopped() {
        String code = "srv-1";
        Survey survey = new Survey(new Date(), code, new ArrayList<>(), false, new ArrayList<>(), new ArrayList<>(), "P");
        when(inMemoryDatabase.getSurvey(code)).thenReturn(survey);

        assertThrows(SurveyStoppedException.class, () -> surveyService.getVote(code, "u1"));
    }

    @Test
    @DisplayName("setVote should persist vote when survey is running")
    void testSetVoteSuccess() {
        String code = "srv-1";
        String userId = "u1";
        Survey survey = new Survey(new Date(), code, new ArrayList<>(), true, new ArrayList<>(), new ArrayList<>(), "P");
        when(inMemoryDatabase.getSurvey(code)).thenReturn(survey);

        VoteRequest voteRequest = new VoteRequest("0", "1");
        surveyService.setVote(code, voteRequest, userId);

        verify(randomAlgorithm).setVote(code, "0", "1", userId);
    }

    @Test
    @DisplayName("setVote should throw SurveyStoppedException when survey is stopped")
    void testSetVoteStopped() {
        String code = "srv-1";
        Survey survey = new Survey(new Date(), code, new ArrayList<>(), false, new ArrayList<>(), new ArrayList<>(), "P");
        when(inMemoryDatabase.getSurvey(code)).thenReturn(survey);

        VoteRequest voteRequest = new VoteRequest("0", "1");
        assertThrows(SurveyStoppedException.class, () -> surveyService.setVote(code, voteRequest, "u1"));
    }

    @Test
    @DisplayName("getScore should return scores sorted descending when survey has ended")
    void testGetScoreSuccess() {
        String code = "srv-1";
        Image img0 = new Image("0", "f0");
        Image img1 = new Image("1", "f1");
        List<Image> images = List.of(img0, img1);
        List<ScoreItem> scoreItems = List.of(new ScoreItem("0", 5), new ScoreItem("1", 20));
        List<Vote> votes = List.of(
                new Vote("1", "0", "u1"),
                new Vote("1", "0", "u2"),
                new Vote("0", "1", "u1")
        );
        Survey survey = new Survey(new Date(), code, images, false, scoreItems, votes, "Persp");
        when(inMemoryDatabase.getSurvey(code)).thenReturn(survey);

        ScoreResponse response = surveyService.getScore(code);

        assertNotNull(response);
        assertFalse(response.surveyIsRunning());
        assertEquals("Persp", response.perspective());
        assertEquals(3, response.numberOfVotes());
        assertEquals(2, response.numberOfUsers()); // u1, u2
        assertEquals(2, response.scores().size());
        assertEquals("1", response.scores().get(0).imageId());
        assertEquals(20, response.scores().get(0).score());
        assertEquals("0", response.scores().get(1).imageId());
        assertEquals(5, response.scores().get(1).score());
    }

    @Test
    @DisplayName("getScore should throw SurveyStillRunningException when survey is still running")
    void testGetScoreStillRunning() {
        String code = "srv-1";
        Survey survey = new Survey(new Date(), code, new ArrayList<>(), true, new ArrayList<>(), new ArrayList<>(), "P");
        when(inMemoryDatabase.getSurvey(code)).thenReturn(survey);

        assertThrows(SurveyStillRunningException.class, () -> surveyService.getScore(code));
    }

    @Test
    @DisplayName("getScore should throw SurveyIncompleteException when scores or votes are null")
    void testGetScoreIncomplete() {
        String code = "srv-1";
        Survey survey = new Survey(new Date(), code, new ArrayList<>(), false, new ArrayList<>(), new ArrayList<>(), "P");
        survey.setVotes(null);
        when(inMemoryDatabase.getSurvey(code)).thenReturn(survey);

        assertThrows(SurveyIncompleteException.class, () -> surveyService.getScore(code));
    }

    @Test
    @DisplayName("getImageFromSurvey should return image if present")
    void testGetImageFromSurveySuccess() {
        String code = "srv-1";
        Image img = new Image("0", "data:image/jpeg;base64,xyz");
        Survey survey = new Survey(new Date(), code, List.of(img), false, new ArrayList<>(), new ArrayList<>(), "P");
        when(inMemoryDatabase.getSurvey(code)).thenReturn(survey);

        Image result = surveyService.getImageFromSurvey(code, "0");
        assertNotNull(result);
        assertEquals("0", result.id());
    }

    @Test
    @DisplayName("getImageFromSurvey should throw ImageNotFoundException if image not present")
    void testGetImageFromSurveyNotFound() {
        String code = "srv-1";
        Survey survey = new Survey(new Date(), code, new ArrayList<>(), false, new ArrayList<>(), new ArrayList<>(), "P");
        when(inMemoryDatabase.getSurvey(code)).thenReturn(survey);

        assertThrows(ImageNotFoundException.class, () -> surveyService.getImageFromSurvey(code, "99"));
    }

    @Test
    @DisplayName("addImageToSurvey should delegate to inMemoryDatabase")
    void testAddImageToSurvey() {
        String code = "srv-1";
        Image image = new Image(null, "content");
        when(inMemoryDatabase.addImageToSurvey(code, image)).thenReturn("0");

        String id = surveyService.addImageToSurvey(code, image);
        assertEquals("0", id);
        verify(inMemoryDatabase).addImageToSurvey(code, image);
    }

    @Test
    @DisplayName("deleteSurvey should delete survey when running")
    void testDeleteSurveySuccess() {
        String code = "srv-1";
        Survey survey = new Survey(new Date(), code, new ArrayList<>(), true, new ArrayList<>(), new ArrayList<>(), "P");
        when(inMemoryDatabase.getSurvey(code)).thenReturn(survey);

        surveyService.deleteSurvey(code);
        verify(inMemoryDatabase).removeSurvey(code);
    }

    @Test
    @DisplayName("deleteSurvey should throw SurveyStoppedException when stopped")
    void testDeleteSurveyStopped() {
        String code = "srv-1";
        Survey survey = new Survey(new Date(), code, new ArrayList<>(), false, new ArrayList<>(), new ArrayList<>(), "P");
        when(inMemoryDatabase.getSurvey(code)).thenReturn(survey);

        assertThrows(SurveyStoppedException.class, () -> surveyService.deleteSurvey(code));
        verify(inMemoryDatabase, never()).removeSurvey(code);
    }
}
