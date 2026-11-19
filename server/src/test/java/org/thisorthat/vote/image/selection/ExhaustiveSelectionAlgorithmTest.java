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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thisorthat.db.InMemoryDatabase;
import org.thisorthat.dto.ExhaustiveVoteState;
import org.thisorthat.dto.Image;
import org.thisorthat.dto.Pair;
import org.thisorthat.dto.Survey;
import org.thisorthat.dto.Vote;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExhaustiveSelectionAlgorithmTest {

    private ExhaustiveSelectionAlgorithm algorithm;

    @BeforeEach
    void setUp() {
        algorithm = new ExhaustiveSelectionAlgorithm();
    }

    @Test
    @DisplayName("calculateTotalComparisons should match n*(n-1)/2 formula")
    void testCalculateTotalComparisons() {
        assertEquals(0, ExhaustiveSelectionAlgorithm.calculateTotalComparisons(0));
        assertEquals(0, ExhaustiveSelectionAlgorithm.calculateTotalComparisons(1));
        assertEquals(1, ExhaustiveSelectionAlgorithm.calculateTotalComparisons(2));
        assertEquals(3, ExhaustiveSelectionAlgorithm.calculateTotalComparisons(3));
        assertEquals(6, ExhaustiveSelectionAlgorithm.calculateTotalComparisons(4));
        assertEquals(10, ExhaustiveSelectionAlgorithm.calculateTotalComparisons(5));
        assertEquals(45, ExhaustiveSelectionAlgorithm.calculateTotalComparisons(10));
        assertEquals(190, ExhaustiveSelectionAlgorithm.calculateTotalComparisons(20));
    }

    @Test
    @DisplayName("canonicalPairKey should produce identical key regardless of order")
    void testCanonicalPairKey() {
        assertEquals("0::1", ExhaustiveSelectionAlgorithm.canonicalPairKey("0", "1"));
        assertEquals("0::1", ExhaustiveSelectionAlgorithm.canonicalPairKey("1", "0"));
        assertEquals("apple::banana", ExhaustiveSelectionAlgorithm.canonicalPairKey("banana", "apple"));
        assertEquals("", ExhaustiveSelectionAlgorithm.canonicalPairKey(null, "1"));
    }

    @Test
    @DisplayName("generateAllPairs should generate all combinations of images")
    void testGenerateAllPairs() {
        List<Image> images = List.of(
                new Image("0", "f0"),
                new Image("1", "f1"),
                new Image("2", "f2")
        );
        List<Pair<Image>> pairs = ExhaustiveSelectionAlgorithm.generateAllPairs(images);
        assertEquals(3, pairs.size());

        Set<String> keys = new HashSet<>();
        for (Pair<Image> p : pairs) {
            keys.add(ExhaustiveSelectionAlgorithm.canonicalPairKey(p.getT1().getId(), p.getT2().getId()));
        }
        assertTrue(keys.contains("0::1"));
        assertTrue(keys.contains("0::2"));
        assertTrue(keys.contains("1::2"));
    }

    @Test
    @DisplayName("evaluate should return first pair with 0 completed comparisons when no votes exist")
    void testEvaluateNoVotes() {
        List<Image> images = List.of(
                new Image("0", "f0"),
                new Image("1", "f1"),
                new Image("2", "f2")
        );
        Survey survey = new Survey(new Date(), "srv-1", images, true, new ArrayList<>(), new ArrayList<>(), "P");

        ExhaustiveVoteState state = algorithm.evaluate(survey, "user-1");

        assertNotNull(state);
        assertFalse(state.completed());
        assertEquals(3, state.totalComparisons());
        assertEquals(0, state.completedComparisons());
        assertNotNull(state.image1());
        assertNotNull(state.image2());
        assertNotEquals(state.image1().getId(), state.image2().getId());
    }

    @Test
    @DisplayName("evaluate should accurately reflect progress as user votes on pairs")
    void testEvaluateIncrementalProgress() {
        List<Image> images = List.of(
                new Image("0", "f0"),
                new Image("1", "f1"),
                new Image("2", "f2")
        );
        List<Vote> votes = new ArrayList<>();
        Survey survey = new Survey(new Date(), "srv-1", images, true, new ArrayList<>(), votes, "P");

        // User 1 votes on pair {0, 1}
        votes.add(new Vote("0", "1", "user-1"));
        ExhaustiveVoteState state1 = algorithm.evaluate(survey, "user-1");

        assertFalse(state1.completed());
        assertEquals(3, state1.totalComparisons());
        assertEquals(1, state1.completedComparisons());
        String selectedKey1 = ExhaustiveSelectionAlgorithm.canonicalPairKey(state1.image1().getId(), state1.image2().getId());
        assertNotEquals("0::1", selectedKey1, "Should not select already-voted pair {0, 1}");

        // User 1 votes on pair {0, 2}
        votes.add(new Vote("2", "0", "user-1"));
        ExhaustiveVoteState state2 = algorithm.evaluate(survey, "user-1");

        assertFalse(state2.completed());
        assertEquals(3, state2.totalComparisons());
        assertEquals(2, state2.completedComparisons());
        String selectedKey2 = ExhaustiveSelectionAlgorithm.canonicalPairKey(state2.image1().getId(), state2.image2().getId());
        assertEquals("1::2", selectedKey2, "Must select remaining pair {1, 2}");

        // User 1 votes on pair {1, 2}
        votes.add(new Vote("1", "2", "user-1"));
        ExhaustiveVoteState state3 = algorithm.evaluate(survey, "user-1");

        assertTrue(state3.completed());
        assertEquals(3, state3.totalComparisons());
        assertEquals(3, state3.completedComparisons());
        assertNull(state3.image1());
        assertNull(state3.image2());
    }

    @Test
    @DisplayName("evaluate should keep voters isolated from each other")
    void testEvaluateUserIsolation() {
        List<Image> images = List.of(
                new Image("0", "f0"),
                new Image("1", "f1"),
                new Image("2", "f2")
        );
        List<Vote> votes = new ArrayList<>();
        // user-1 has finished all 3 pairs
        votes.add(new Vote("0", "1", "user-1"));
        votes.add(new Vote("0", "2", "user-1"));
        votes.add(new Vote("1", "2", "user-1"));

        Survey survey = new Survey(new Date(), "srv-1", images, true, new ArrayList<>(), votes, "P");

        // user-1 is completed
        ExhaustiveVoteState stateUser1 = algorithm.evaluate(survey, "user-1");
        assertTrue(stateUser1.completed());
        assertEquals(3, stateUser1.completedComparisons());

        // user-2 has not voted yet and has 0 comparisons
        ExhaustiveVoteState stateUser2 = algorithm.evaluate(survey, "user-2");
        assertFalse(stateUser2.completed());
        assertEquals(0, stateUser2.completedComparisons());
        assertEquals(3, stateUser2.totalComparisons());
        assertNotNull(stateUser2.image1());
        assertNotNull(stateUser2.image2());
    }

    @Test
    @DisplayName("getNextImagePair and getVoteState via ImageSelectionAlgorithm interface")
    void testImageSelectionAlgorithmInterfaceMethods() {
        InMemoryDatabase db = mock(InMemoryDatabase.class);
        ExhaustiveSelectionAlgorithm exhaustiveAlgo = new ExhaustiveSelectionAlgorithm(db);

        // Verify it implements ImageSelectionAlgorithm
        assertTrue(exhaustiveAlgo instanceof ImageSelectionAlgorithm);

        List<Image> images = List.of(new Image("0", "f0"), new Image("1", "f1"));
        Survey survey = new Survey(new Date(), "srv-code", images, true, new ArrayList<>(), new ArrayList<>(), "P");
        when(db.getSurvey("srv-code")).thenReturn(survey);

        // getNextImagePair should return a pair when comparisons remain
        Pair<Image> pair = exhaustiveAlgo.getNextImagePair("srv-code", "user-1");
        assertNotNull(pair);
        assertNotNull(pair.getT1());
        assertNotNull(pair.getT2());

        // getVoteState by surveyCode
        ExhaustiveVoteState stateByCode = exhaustiveAlgo.getVoteState("srv-code", "user-1");
        assertNotNull(stateByCode);
        assertEquals(1, stateByCode.totalComparisons());
        assertEquals(0, stateByCode.completedComparisons());
        assertFalse(stateByCode.completed());

        // getVoteState by Survey
        ExhaustiveVoteState stateBySurvey = exhaustiveAlgo.getVoteState(survey, "user-1");
        assertNotNull(stateBySurvey);
        assertEquals(1, stateBySurvey.totalComparisons());

        // When all comparisons are completed, getNextImagePair should return null
        survey.getVotes().add(new Vote("0", "1", "user-1"));
        Pair<Image> completedPair = exhaustiveAlgo.getNextImagePair("srv-code", "user-1");
        assertNull(completedPair);

        ExhaustiveVoteState completedState = exhaustiveAlgo.getVoteState("srv-code", "user-1");
        assertTrue(completedState.completed());
    }

    @Test
    @DisplayName("getVoteState without inMemoryDatabase should throw IllegalStateException")
    void testGetVoteStateWithoutDatabase() {
        ExhaustiveSelectionAlgorithm algoWithoutDb = new ExhaustiveSelectionAlgorithm();
        assertThrows(IllegalStateException.class, () -> algoWithoutDb.getVoteState("code", "u1"));
    }
}
