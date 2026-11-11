package org.GlassBishop.engine;

import org.GlassBishop.model.Move;
import org.GlassBishop.scoring.ScoreResult;

public record MoveRecommendation(Move move, ScoreResult resultingPosition, int legalMovesEvaluated) {
}
