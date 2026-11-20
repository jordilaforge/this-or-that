package org.thisorthat.vote.elo;

public final class EloCalculator {

    public static final int DEFAULT_INITIAL_RATING = 1500;
    public static final double DEFAULT_K_FACTOR = 32.0;

    private EloCalculator() {
    }

    public static double calculateExpectedScore(double ratingA, double ratingB) {
        return 1.0 / (1.0 + Math.pow(10.0, (ratingB - ratingA) / 400.0));
    }

    public static EloDelta calculate(double winnerRating, double loserRating, double kFactor) {
        double expectedWinner = calculateExpectedScore(winnerRating, loserRating);
        double expectedLoser = calculateExpectedScore(loserRating, winnerRating);

        double newWinnerRating = winnerRating + kFactor * (1.0 - expectedWinner);
        double newLoserRating = loserRating + kFactor * (0.0 - expectedLoser);

        return new EloDelta(newWinnerRating, newLoserRating);
    }
}
