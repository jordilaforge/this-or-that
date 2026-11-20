package org.thisorthat.vote.image.selection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thisorthat.db.InMemoryDatabase;
import org.thisorthat.dto.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdaptiveSelectionAlgorithmTest {

    @Mock
    private InMemoryDatabase inMemoryDatabase;

    @InjectMocks
    private AdaptiveSelectionAlgorithm algorithm;

    @Test
    @DisplayName("getNextImagePair should stop when user reaches max comparison limit")
    void testSessionLimitReached() {
        String code = "srv-1";
        String userId = "user-1";
        List<Image> images = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            images.add(new Image(String.valueOf(i), "f" + i));
        }

        List<Vote> votes = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            votes.add(new Vote(String.valueOf(i), String.valueOf(i + 1), userId));
        }

        Survey survey = new Survey(new Date(), code, images, true, new ArrayList<>(), votes, "Perspective");
        survey.setMaxComparisonsPerUser(5);
        when(inMemoryDatabase.getSurvey(code)).thenReturn(survey);

        Pair<Image> nextPair = algorithm.getNextImagePair(code, userId);
        assertNull(nextPair, "Adaptive mode must respect max comparisons session limit");
    }

    @Test
    @DisplayName("getNextImagePair should prioritize underexposed images during exploration")
    void testPrioritizeUnderexposedImages() {
        String code = "srv-1";
        String userId = "user-1";
        Image img0 = new Image("0", "f0");
        Image img1 = new Image("1", "f1");
        Image img2 = new Image("2", "f2");
        List<Image> images = List.of(img0, img1, img2);

        List<Vote> votes = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            votes.add(new Vote("0", "1", "other-user"));
        }

        Survey survey = new Survey(new Date(), code, images, true, new ArrayList<>(), votes, "P");
        when(inMemoryDatabase.getSurvey(code)).thenReturn(survey);

        Pair<Image> pair = algorithm.getNextImagePair(code, userId);
        assertNotNull(pair);
        assertTrue(pair.getT1().getId().equals("2") || pair.getT2().getId().equals("2"),
                "Underexposed image '2' should be included in the candidate pair");
    }
}
