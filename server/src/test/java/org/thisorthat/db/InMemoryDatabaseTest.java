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
package org.thisorthat.db;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thisorthat.dto.*;
import org.thisorthat.exception.DeleteFailedException;
import org.thisorthat.exception.ImageNotFoundException;
import org.thisorthat.exception.SurveyNotFoundException;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryDatabaseTest {

    private InMemoryDatabase db;

    @BeforeEach
    void setUp() {
        db = new InMemoryDatabase();
    }

    @Test
    @DisplayName("addSurvey and getSurvey should store and retrieve survey")
    void testAddAndGetSurvey() {
        String code = "test-code-" + System.nanoTime();
        Survey survey = new Survey(code, "Test Perspective");

        db.addSurvey(survey);
        Survey retrieved = db.getSurvey(code);

        assertNotNull(retrieved);
        assertEquals(code, retrieved.getId());
        assertEquals("Test Perspective", retrieved.getPerspective());
    }

    @Test
    @DisplayName("getSurvey should throw SurveyNotFoundException for unknown code")
    void testGetSurveyNotFound() {
        assertThrows(SurveyNotFoundException.class, () -> db.getSurvey("unknown-code"));
    }

    @Test
    @DisplayName("removeSurvey should delete existing survey")
    void testRemoveSurveySuccess() {
        String code = "delete-code-" + System.nanoTime();
        Survey survey = new Survey(code, "Perspective");
        db.addSurvey(survey);

        db.removeSurvey(code);
        assertThrows(SurveyNotFoundException.class, () -> db.getSurvey(code));
    }

    @Test
    @DisplayName("removeSurvey should throw DeleteFailedException when code not found")
    void testRemoveSurveyNotFound() {
        assertThrows(DeleteFailedException.class, () -> db.removeSurvey("not-existing"));
    }

    @Test
    @DisplayName("addImageToSurvey should assign sequential IDs and store image")
    void testAddImageToSurvey() {
        String code = "image-test-" + System.nanoTime();
        db.addSurvey(new Survey(code, "Perspective"));

        String id1 = db.addImageToSurvey(code, new Image(null, "base64-1"));
        String id2 = db.addImageToSurvey(code, new Image(null, "base64-2"));

        assertEquals("0", id1);
        assertEquals("1", id2);

        Image retrieved = db.getImageFromSurvey(code, "0");
        assertNotNull(retrieved);
        assertEquals("0", retrieved.id());
        assertEquals("base64-1", retrieved.file());
    }

    @Test
    @DisplayName("getImageFromSurvey should throw ImageNotFoundException when imageId does not exist")
    void testGetImageNotFound() {
        String code = "img-not-found-" + System.nanoTime();
        db.addSurvey(new Survey(code, "Perspective"));

        assertThrows(ImageNotFoundException.class, () -> db.getImageFromSurvey(code, "nonexistent"));
    }

    @Test
    @DisplayName("startSurvey and stopSurvey should toggle survey started state")
    void testStartAndStopSurvey() {
        String code = "toggle-test-" + System.nanoTime();
        db.addSurvey(new Survey(code, "Perspective"));

        assertFalse(db.getSurvey(code).getStarted());

        db.startSurvey(code);
        assertTrue(db.getSurvey(code).getStarted());

        db.stopSurvey(code);
        assertFalse(db.getSurvey(code).getStarted());
    }

    @Test
    @DisplayName("addScore and persistVote should append items to survey")
    void testAddScoreAndPersistVote() {
        String code = "score-vote-test-" + System.nanoTime();
        db.addSurvey(new Survey(code, "Perspective"));

        db.addScore(code, new ScoreItem("img-0", 15));
        assertEquals(1, db.getSurvey(code).getScores().size());
        assertEquals(15, db.getSurvey(code).getScores().get(0).score());

        db.persistVote(code, new Vote("0", "1", "user-1"));
        assertEquals(1, db.getSurvey(code).getVotes().size());
        assertEquals("user-1", db.getSurvey(code).getVotes().get(0).userId());
    }

    @Test
    @DisplayName("getSurveysOlderThan should filter surveys by creation date")
    void testGetSurveysOlderThan() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -2);
        Date twoDaysAgo = cal.getTime();

        String oldCode = "old-" + System.nanoTime();
        Survey oldSurvey = new Survey(twoDaysAgo, oldCode, List.of(), false, List.of(), List.of(), "Old");
        db.addSurvey(oldSurvey);

        String newCode = "new-" + System.nanoTime();
        Survey newSurvey = new Survey(new Date(), newCode, List.of(), false, List.of(), List.of(), "New");
        db.addSurvey(newSurvey);

        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_MONTH, -1);
        List<Survey> olderSurveys = db.getSurveysOlderThan(yesterday.getTime());

        assertTrue(olderSurveys.stream().anyMatch(s -> s.getId().equals(oldCode)));
        assertFalse(olderSurveys.stream().anyMatch(s -> s.getId().equals(newCode)));
    }
}
