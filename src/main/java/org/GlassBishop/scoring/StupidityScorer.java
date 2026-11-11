package org.GlassBishop.scoring;

import org.GlassBishop.engine.PositionAnalyzer;
import org.GlassBishop.model.Board;

public final class StupidityScorer {
    private static final int STARTING_WHITE_MATERIAL = 39;
    private final PositionAnalyzer analyzer = new PositionAnalyzer();

    public ScoreResult score(Board board) {
        int material = analyzer.whiteMaterial(board);
        int attackedMaterial = analyzer.attackedWhiteMaterial(board);
        boolean mateInOne = analyzer.whiteCanMateInOne(board);

        int missingMaterial = Math.min(STARTING_WHITE_MATERIAL,
                Math.max(0, STARTING_WHITE_MATERIAL - material));
        int exposedMaterial = Math.min(STARTING_WHITE_MATERIAL, attackedMaterial);
        double scoreValue = 45.0 * missingMaterial / STARTING_WHITE_MATERIAL
                + 45.0 * exposedMaterial / STARTING_WHITE_MATERIAL
                + (mateInOne ? 10.0 : 0.0);
        int score = Math.max(0, Math.min(100, (int) Math.round(scoreValue)));
        return new ScoreResult(score, StupidityRating.forScore(score), material,
                attackedMaterial, mateInOne);
    }
}
