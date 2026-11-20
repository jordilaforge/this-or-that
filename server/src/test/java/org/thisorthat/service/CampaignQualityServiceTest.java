package org.thisorthat.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thisorthat.dto.Image;
import org.thisorthat.dto.Survey;
import org.thisorthat.dto.Vote;
import org.thisorthat.vote.image.selection.SelectionMode;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CampaignQualityServiceTest {

    private final CampaignQualityService qualityService = new CampaignQualityService();

    @Test
    @DisplayName("Quality metrics calculation on active survey")
    void testQualityMetrics() {
        Image img0 = new Image("0", "f0");
        Image img1 = new Image("1", "f1");
        Image img2 = new Image("2", "f2");
        List<Image> images = List.of(img0, img1, img2);

        List<Vote> votes = List.of(
                new Vote("0", "1", "u1"),
                new Vote("0", "2", "u1"),
                new Vote("1", "2", "u2")
        );

        Survey survey = new Survey(new Date(), "srv-1", images, true, new ArrayList<>(), votes, "Perspective");
        survey.setSelectionMode(SelectionMode.EXHAUSTIVE);

        Map<String, Integer> exposure = qualityService.getExposureCounts(survey);
        assertEquals(2, exposure.get("0"));
        assertEquals(2, exposure.get("1"));
        assertEquals(2, exposure.get("2"));

        assertEquals(2, qualityService.getMinComparisons(survey));
        assertEquals(2, qualityService.getMaxComparisons(survey));
        assertEquals(2.0, qualityService.getAverageComparisons(survey), 0.001);
        assertEquals(1.0, qualityService.getExposureBalanceRatio(survey), 0.001);
        assertTrue(qualityService.isReliable(survey));
    }
}
