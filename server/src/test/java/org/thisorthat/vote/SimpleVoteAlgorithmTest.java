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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thisorthat.db.InMemoryDatabase;
import org.thisorthat.dto.*;
import org.thisorthat.exception.SurveyStoppedException;
import org.thisorthat.vote.image.selection.ImageSelectionAlgorithm;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SimpleVoteAlgorithmTest {

    @Mock
    private ImageSelectionAlgorithm imageSelectionAlgorithm;

    @Mock
    private InMemoryDatabase inMemoryDatabase;

    @InjectMocks
    private SimpleVoteAlgorithm voteAlgorithm;

    @Test
    @DisplayName("initialize should set initial score of 0 for every image in survey")
    void testInitialize() {
        String code = "survey-1";
        Image img0 = new Image("0", "f0");
        Image img1 = new Image("1", "f1");
        Survey survey = new Survey(new Date(), code, List.of(img0, img1), true, new ArrayList<>(), new ArrayList<>(), "P");
        when(inMemoryDatabase.getSurvey(code)).thenReturn(survey);

        voteAlgorithm.initialize(code);

        assertEquals(2, survey.getScores().size());
        assertEquals(0, survey.getScores().get(0).score());
        assertEquals(0, survey.getScores().get(1).score());
    }

    @Test
    @DisplayName("getVote should fetch pair from ImageSelectionAlgorithm and return VoteItem")
    void testGetVote() {
        String code = "survey-1";
        String userId = "user-1";
        Image img0 = new Image("0", "f0");
        Image img1 = new Image("1", "f1");
        when(imageSelectionAlgorithm.getNextImagePair(code, userId)).thenReturn(new Pair<>(img0, img1));

        VoteItem voteItem = voteAlgorithm.getVote(code, userId);

        assertNotNull(voteItem);
        assertEquals(img0, voteItem.file1());
        assertEquals(img1, voteItem.file2());
    }

    @Test
    @DisplayName("setVote should persist vote in database")
    void testSetVote() {
        String code = "survey-1";
        voteAlgorithm.setVote(code, "0", "1", "user-1");

        ArgumentCaptor<Vote> captor = ArgumentCaptor.forClass(Vote.class);
        verify(inMemoryDatabase).persistVote(eq(code), captor.capture());
        Vote persisted = captor.getValue();
        assertEquals("0", persisted.winner());
        assertEquals("1", persisted.loser());
        assertEquals("user-1", persisted.userId());
    }

    @Test
    @DisplayName("calculateScores should calculate positive and negative weights for 1 vote")
    void testCalculateScoresSingleVote() {
        String code = "survey-1";
        Image img0 = new Image("0", "f0");
        Image img1 = new Image("1", "f1");
        List<Vote> votes = List.of(new Vote("0", "1", "user-1"));
        Survey survey = new Survey(new Date(), code, List.of(img0, img1), true, new ArrayList<>(), votes, "P");
        when(inMemoryDatabase.getSurvey(code)).thenReturn(survey);

        List<ScoreItem> scores = voteAlgorithm.calculateScores(code);

        assertEquals(2, scores.size());
        ScoreItem score0 = scores.stream().filter(s -> s.imageId().equals("0")).findFirst().orElseThrow();
        ScoreItem score1 = scores.stream().filter(s -> s.imageId().equals("1")).findFirst().orElseThrow();

        // 1 vote * 2 <= 2 images -> weight = 10.0
        assertEquals(10, score0.score());
        assertEquals(-10, score1.score());
    }

    @Test
    @DisplayName("calculateScores should normalize weight when a user votes more times than image count")
    void testCalculateScoresNormalizedWeight() {
        String code = "survey-1";
        Image img0 = new Image("0", "f0");
        Image img1 = new Image("1", "f1");
        // 2 votes for 2 images -> votes.size() * 2 = 4 > images.size() (2)
        // weight per vote = 10.0 * 2 / 4 = 5.0
        List<Vote> votes = List.of(
                new Vote("0", "1", "user-1"),
                new Vote("0", "1", "user-1")
        );
        Survey survey = new Survey(new Date(), code, List.of(img0, img1), true, new ArrayList<>(), votes, "P");
        when(inMemoryDatabase.getSurvey(code)).thenReturn(survey);

        List<ScoreItem> scores = voteAlgorithm.calculateScores(code);

        ScoreItem score0 = scores.stream().filter(s -> s.imageId().equals("0")).findFirst().orElseThrow();
        ScoreItem score1 = scores.stream().filter(s -> s.imageId().equals("1")).findFirst().orElseThrow();

        assertEquals(10, score0.score()); // 5.0 + 5.0
        assertEquals(-10, score1.score()); // -5.0 + -5.0
    }

    @Test
    @DisplayName("calculateScoresAndStopSurvey should calculate scores and stop running survey")
    void testCalculateScoresAndStopSurveySuccess() {
        String code = "survey-1";
        Image img0 = new Image("0", "f0");
        Image img1 = new Image("1", "f1");
        List<Vote> votes = List.of(new Vote("0", "1", "user-1"));
        Survey survey = new Survey(new Date(), code, List.of(img0, img1), true, new ArrayList<>(), votes, "P");
        when(inMemoryDatabase.getSurvey(code)).thenReturn(survey);

        voteAlgorithm.calculateScoresAndStopSurvey(code);

        verify(inMemoryDatabase).stopSurvey(code);
        assertFalse(survey.getScores().isEmpty());
    }

    @Test
    @DisplayName("calculateScoresAndStopSurvey should throw SurveyStoppedException if survey is already stopped")
    void testCalculateScoresAndStopSurveyAlreadyStopped() {
        String code = "survey-1";
        Survey survey = new Survey(new Date(), code, new ArrayList<>(), false, new ArrayList<>(), new ArrayList<>(), "P");
        when(inMemoryDatabase.getSurvey(code)).thenReturn(survey);

        assertThrows(SurveyStoppedException.class, () -> voteAlgorithm.calculateScoresAndStopSurvey(code));
        verify(inMemoryDatabase, never()).stopSurvey(code);
    }
}
