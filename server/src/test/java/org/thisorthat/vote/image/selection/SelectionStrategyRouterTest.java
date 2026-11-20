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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SelectionStrategyRouterTest {

    @Mock
    private InMemoryDatabase inMemoryDatabase;

    @Mock
    private ExhaustiveSelectionAlgorithm exhaustiveAlgorithm;

    @Mock
    private AdaptiveSelectionAlgorithm adaptiveAlgorithm;

    @InjectMocks
    private SelectionStrategyRouter router;

    @Test
    @DisplayName("Router should select exhaustive algorithm for surveys with <= 10 images by default")
    void testAutoRouteToExhaustive() {
        String code = "srv-small";
        List<Image> images = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            images.add(new Image(String.valueOf(i), "f" + i));
        }
        Survey survey = new Survey(new Date(), code, images, true, new ArrayList<>(), new ArrayList<>(), "P");
        survey.setSelectionMode(SelectionMode.AUTO);

        Pair<Image> expectedPair = new Pair<>(images.get(0), images.get(1));
        when(inMemoryDatabase.getSurvey(code)).thenReturn(survey);
        when(exhaustiveAlgorithm.getNextImagePair(code, "u1")).thenReturn(expectedPair);

        Pair<Image> actual = router.getNextImagePair(code, "u1");
        assertSame(expectedPair, actual);
        verify(exhaustiveAlgorithm).getNextImagePair(code, "u1");
        verifyNoInteractions(adaptiveAlgorithm);
    }

    @Test
    @DisplayName("Router should select adaptive algorithm for surveys with > 10 images by default")
    void testAutoRouteToAdaptive() {
        String code = "srv-large";
        List<Image> images = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            images.add(new Image(String.valueOf(i), "f" + i));
        }
        Survey survey = new Survey(new Date(), code, images, true, new ArrayList<>(), new ArrayList<>(), "P");
        survey.setSelectionMode(SelectionMode.AUTO);

        Pair<Image> expectedPair = new Pair<>(images.get(0), images.get(1));
        when(inMemoryDatabase.getSurvey(code)).thenReturn(survey);
        when(adaptiveAlgorithm.getNextImagePair(code, "u1")).thenReturn(expectedPair);

        Pair<Image> actual = router.getNextImagePair(code, "u1");
        assertSame(expectedPair, actual);
        verify(adaptiveAlgorithm).getNextImagePair(code, "u1");
        verifyNoInteractions(exhaustiveAlgorithm);
    }
}
