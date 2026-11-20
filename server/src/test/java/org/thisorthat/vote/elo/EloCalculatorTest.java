package org.thisorthat.vote.elo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EloCalculatorTest {

    @Test
    @DisplayName("calculateExpectedScore should return 0.5 when both ratings are equal")
    void testExpectedScoreEqualRatings() {
        double expected = EloCalculator.calculateExpectedScore(1500, 1500);
        assertEquals(0.5, expected, 0.0001);
    }

    @Test
    @DisplayName("calculateExpectedScore should favor higher rated image")
    void testExpectedScoreDifferentRatings() {
        double expectedHigher = EloCalculator.calculateExpectedScore(1700, 1500);
        double expectedLower = EloCalculator.calculateExpectedScore(1500, 1700);

        assertTrue(expectedHigher > 0.5);
        assertTrue(expectedLower < 0.5);
        assertEquals(1.0, expectedHigher + expectedLower, 0.0001);
    }

    @Test
    @DisplayName("calculate should award winner and deduct loser symmetrically")
    void testCalculateEqualRatings() {
        EloDelta delta = EloCalculator.calculate(1500, 1500, 32.0);

        assertEquals(1516.0, delta.newWinnerRating(), 0.001);
        assertEquals(1484.0, delta.newLoserRating(), 0.001);
    }

    @Test
    @DisplayName("calculate with upset victory should produce larger rating adjustment")
    void testCalculateUpsetVictory() {
        EloDelta normalWin = EloCalculator.calculate(1700, 1300, 32.0);
        EloDelta upsetWin = EloCalculator.calculate(1300, 1700, 32.0);

        double normalDelta = normalWin.newWinnerRating() - 1700;
        double upsetDelta = upsetWin.newWinnerRating() - 1300;

        assertTrue(upsetDelta > normalDelta);
    }
}
