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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thisorthat.db.InMemoryDatabase;
import org.thisorthat.dto.Image;
import org.thisorthat.dto.Pair;
import org.thisorthat.dto.Survey;
import org.thisorthat.dto.Vote;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RandomSelectionAlgorithmTest {

    @Mock
    private InMemoryDatabase inMemoryDatabase;

    @InjectMocks
    private RandomSelectionAlgorithm selectionAlgorithm;

    @Test
    @DisplayName("Should return pair of distinct images when user has not voted yet")
    void testGetNextImagePairNoPriorVotes() {
        String code = "srv-1";
        String userId = "user-1";
        Image img0 = new Image("0", "f0");
        Image img1 = new Image("1", "f1");
        Image img2 = new Image("2", "f2");
        Survey survey = new Survey(new Date(), code, List.of(img0, img1, img2), true, new ArrayList<>(), new ArrayList<>(), "P");

        when(inMemoryDatabase.getSurvey(code)).thenReturn(survey);
        when(inMemoryDatabase.getImageFromSurvey(eq(code), anyString())).thenAnswer(invocation -> {
            String id = invocation.getArgument(1);
            return survey.getImages().stream().filter(img -> img.id().equals(id)).findFirst().orElse(null);
        });

        Pair<Image> pair = selectionAlgorithm.getNextImagePair(code, userId);

        assertNotNull(pair);
        assertNotNull(pair.t1());
        assertNotNull(pair.t2());
        assertNotEquals(pair.t1().id(), pair.t2().id(), "Selected image pair must contain distinct images");
    }

    @Test
    @DisplayName("Should select unvoted image when only one image is unvoted")
    void testGetNextImagePairOneUnvoted() {
        String code = "srv-1";
        String userId = "user-1";
        Image img0 = new Image("0", "f0");
        Image img1 = new Image("1", "f1");
        Image img2 = new Image("2", "f2");
        List<Vote> votes = List.of(new Vote("0", "1", userId));
        Survey survey = new Survey(new Date(), code, List.of(img0, img1, img2), true, new ArrayList<>(), votes, "P");

        when(inMemoryDatabase.getSurvey(code)).thenReturn(survey);
        when(inMemoryDatabase.getImageFromSurvey(eq(code), anyString())).thenAnswer(invocation -> {
            String id = invocation.getArgument(1);
            return survey.getImages().stream().filter(img -> img.id().equals(id)).findFirst().orElse(null);
        });

        Pair<Image> pair = selectionAlgorithm.getNextImagePair(code, userId);

        assertNotNull(pair);
        assertNotEquals(pair.t1().id(), pair.t2().id());
        assertTrue(pair.t1().id().equals("2") || pair.t2().id().equals("2"),
                "Unvoted image '2' should be included in the pair");
    }

    @Test
    @DisplayName("Should return random pair of distinct images when user has already voted on all images")
    void testGetNextImagePairAllVoted() {
        String code = "srv-1";
        String userId = "user-1";
        Image img0 = new Image("0", "f0");
        Image img1 = new Image("1", "f1");
        List<Vote> votes = List.of(new Vote("0", "1", userId));
        Survey survey = new Survey(new Date(), code, List.of(img0, img1), true, new ArrayList<>(), votes, "P");

        when(inMemoryDatabase.getSurvey(code)).thenReturn(survey);

        Pair<Image> pair = selectionAlgorithm.getNextImagePair(code, userId);

        assertNotNull(pair);
        assertNotNull(pair.t1());
        assertNotNull(pair.t2());
        assertNotEquals(pair.t1().id(), pair.t2().id(), "Selected image pair must be distinct");
    }
}
