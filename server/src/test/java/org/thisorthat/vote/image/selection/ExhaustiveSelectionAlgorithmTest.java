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

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExhaustiveSelectionAlgorithmTest {

    @Mock
    private InMemoryDatabase inMemoryDatabase;

    @InjectMocks
    private ExhaustiveSelectionAlgorithm algorithm;

    @Test
    @DisplayName("getNextImagePair should return unseen pair and return null when all combinations completed")
    void testExhaustiveCycle() {
        String code = "survey-1";
        String userId = "user-1";
        Image img0 = new Image("0", "f0");
        Image img1 = new Image("1", "f1");
        Image img2 = new Image("2", "f2");
        List<Image> images = List.of(img0, img1, img2);

        List<Vote> votes = new ArrayList<>();
        Survey survey = new Survey(new Date(), code, images, true, new ArrayList<>(), votes, "Perspective");
        when(inMemoryDatabase.getSurvey(code)).thenReturn(survey);

        Set<String> seenKeys = new HashSet<>();
        for (int i = 0; i < 3; i++) {
            Pair<Image> pair = algorithm.getNextImagePair(code, userId);
            assertNotNull(pair);
            String key = ExhaustiveSelectionAlgorithm.buildPairKey(pair.getT1().getId(), pair.getT2().getId());
            assertFalse(seenKeys.contains(key));
            seenKeys.add(key);
            votes.add(new Vote(pair.getT1().getId(), pair.getT2().getId(), userId));
        }

        assertEquals(3, seenKeys.size());
        Pair<Image> finalPair = algorithm.getNextImagePair(code, userId);
        assertNull(finalPair, "Participant should have finished all exhaustive combinations");
    }

    @Test
    @DisplayName("getNextImagePair should return null when less than two images exist")
    void testInsufficientImages() {
        String code = "survey-1";
        Survey survey = new Survey(new Date(), code, List.of(new Image("0", "f0")), true, new ArrayList<>(), new ArrayList<>(), "P");
        when(inMemoryDatabase.getSurvey(code)).thenReturn(survey);

        assertNull(algorithm.getNextImagePair(code, "user-1"));
    }
}
