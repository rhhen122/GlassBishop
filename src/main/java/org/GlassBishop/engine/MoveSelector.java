package org.GlassBishop.engine;

import org.GlassBishop.model.Board;
import org.GlassBishop.model.Color;
import org.GlassBishop.model.Move;
import org.GlassBishop.scoring.ScoreResult;
import org.GlassBishop.scoring.StupidityScorer;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class MoveSelector {
    private final MoveGenerator moveGenerator = new MoveGenerator(new AttackDetector());
    private final StupidityScorer scorer = new StupidityScorer();

    public Optional<MoveRecommendation> leastStupidMove(Board board) {
        return leastStupidMove(board, message -> { });
    }

    public Optional<MoveRecommendation> leastStupidMove(Board board, Consumer<String> progress) {
        return leastStupidMove(board, 1, progress);
    }

    public Optional<MoveRecommendation> leastStupidMove(Board board, int depth,
                                                        Consumer<String> progress) {
        if (depth < 1 || depth > 5) {
            throw new IllegalArgumentException("Search depth must be between 1 and 5.");
        }
        if (depth == 1) {
            return select(board, false, progress);
        }
        progress.accept("Finding all legal moves...");
        List<Move> legalMoves = moveGenerator.legalMoves(board, board.sideToMove());
        progress.accept("Searching " + legalMoves.size() + " legal moves to depth " + depth + "...");
        Move bestMove = null;
        int bestSearchScore = Integer.MAX_VALUE;
        ScoreResult bestImmediateResult = null;

        for (int index = 0; index < legalMoves.size(); index++) {
            Move move = legalMoves.get(index);
            progress.accept("Searching move " + (index + 1) + "/" + legalMoves.size()
                    + ": " + move.toUci());
            Board next = board.apply(move);
            ScoreResult immediateResult = scorer.score(next);
            int searchScore = minimumScore(next, depth - 1, bestSearchScore);
            if (bestMove == null || searchScore < bestSearchScore) {
                bestMove = move;
                bestSearchScore = searchScore;
                bestImmediateResult = immediateResult;
            }
        }

        if (bestMove == null) {
            return Optional.empty();
        }
        progress.accept("Search complete. Best move: " + bestMove.toUci());
        return Optional.of(new MoveRecommendation(bestMove, bestImmediateResult, legalMoves.size()));
    }

    public Optional<MoveRecommendation> mostStupidMove(Board board) {
        return mostStupidMove(board, message -> { });
    }

    public Optional<MoveRecommendation> mostStupidMove(Board board, Consumer<String> progress) {
        return select(board, true, progress);
    }

    private Optional<MoveRecommendation> select(Board board, boolean maximizeScore,
                                                Consumer<String> progress) {
        progress.accept("Finding all legal moves...");
        Color sideToMove = board.sideToMove();
        List<Move> legalMoves = moveGenerator.legalMoves(board, sideToMove);
        progress.accept("Evaluating " + legalMoves.size() + " legal moves...");
        Move bestMove = null;
        ScoreResult bestResult = null;

        for (int index = 0; index < legalMoves.size(); index++) {
            Move move = legalMoves.get(index);
            progress.accept("Scoring move " + (index + 1) + "/" + legalMoves.size()
                    + ": " + move.toUci());
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

    private int minimumScore(Board board, int remainingDepth, int alpha) {
        if (remainingDepth == 0) {
            return scorer.score(board).score();
        }
        List<Move> legalMoves = moveGenerator.legalMoves(board, board.sideToMove());
        if (legalMoves.isEmpty()) {
            return scorer.score(board).score();
        }
        int bestScore = Integer.MAX_VALUE;
        for (Move move : legalMoves) {
            bestScore = Math.min(bestScore,
                    minimumScore(board.apply(move), remainingDepth - 1, alpha));
            if (bestScore <= alpha) {
                break;
            }
        }
        return bestScore;
    }
}
