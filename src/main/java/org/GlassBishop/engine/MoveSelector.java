package org.GlassBishop.engine;

import org.GlassBishop.model.Board;
import org.GlassBishop.model.Color;
import org.GlassBishop.model.Move;
import org.GlassBishop.scoring.ScoreResult;
import org.GlassBishop.scoring.StupidityScorer;

import java.util.List;
import java.util.Optional;

public final class MoveSelector {
    private final MoveGenerator moveGenerator = new MoveGenerator(new AttackDetector());
    private final StupidityScorer scorer = new StupidityScorer();

    public Optional<MoveRecommendation> leastStupidMove(Board board) {
        return select(board, false);
    }

    public Optional<MoveRecommendation> mostStupidMove(Board board) {
        return select(board, true);
    }

    private Optional<MoveRecommendation> select(Board board, boolean maximizeScore) {
        Color sideToMove = board.sideToMove();
        List<Move> legalMoves = moveGenerator.legalMoves(board, sideToMove);
        Move bestMove = null;
        ScoreResult bestResult = null;

        for (Move move : legalMoves) {
            ScoreResult result = scorer.score(board.apply(move));
            if (bestResult == null
                    || (maximizeScore && result.score() > bestResult.score())
                    || (!maximizeScore && result.score() < bestResult.score())) {
                bestMove = move;
                bestResult = result;
            }
        }

        if (bestMove == null) {
            return Optional.empty();
        }
        return Optional.of(new MoveRecommendation(bestMove, bestResult, legalMoves.size()));
    }
}
