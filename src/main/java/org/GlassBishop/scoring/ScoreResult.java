package org.GlassBishop.scoring;

public record ScoreResult(int score, StupidityRating rating, int whiteMaterial,
                          int attackedWhiteMaterial, boolean whiteCanMateInOne) {
}
