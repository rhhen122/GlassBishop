package org.GlassBishop.engine;

import org.GlassBishop.model.Board;
import org.GlassBishop.model.Color;
import org.GlassBishop.model.Move;
import org.GlassBishop.model.Piece;
import org.GlassBishop.model.PieceType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class SmartMoveSelector {
    private static final int SEARCH_DEPTH = 4;
    private static final int INFINITY = 1_000_000;

    private final AttackDetector attacks = new AttackDetector();
    private final MoveGenerator moveGenerator = new MoveGenerator(attacks);
    private final PositionEvaluator evaluator = new PositionEvaluator();

    public Optional<Move> bestMove(Board board) {
        List<Move> legalMoves = orderedMoves(board, moveGenerator.legalMoves(board, board.sideToMove()));
        if (legalMoves.isEmpty()) {
            return Optional.empty();
        }

        boolean maximizing = board.sideToMove() == Color.WHITE;
        Move bestMove = legalMoves.getFirst();
        int bestScore = maximizing ? -INFINITY : INFINITY;
        int alpha = -INFINITY;
        int beta = INFINITY;

        for (Move move : legalMoves) {
            int score = search(board.apply(move), SEARCH_DEPTH - 1, alpha, beta, 1);
            if (maximizing ? score > bestScore : score < bestScore) {
                bestScore = score;
                bestMove = move;
            }
            if (maximizing) {
                alpha = Math.max(alpha, bestScore);
            } else {
                beta = Math.min(beta, bestScore);
            }
        }
        return Optional.of(bestMove);
    }

    private int search(Board board, int depth, int alpha, int beta, int ply) {
        List<Move> legalMoves = moveGenerator.legalMoves(board, board.sideToMove());
        if (legalMoves.isEmpty()) {
            return attacks.isInCheck(board, board.sideToMove())
                    ? evaluator.checkmateScore(board.sideToMove(), ply) : 0;
        }
        if (depth == 0) {
            return evaluator.evaluate(board);
        }

        boolean maximizing = board.sideToMove() == Color.WHITE;
        int bestScore = maximizing ? -INFINITY : INFINITY;
        for (Move move : orderedMoves(board, legalMoves)) {
            int score = search(board.apply(move), depth - 1, alpha, beta, ply + 1);
            if (maximizing) {
                bestScore = Math.max(bestScore, score);
                alpha = Math.max(alpha, bestScore);
            } else {
                bestScore = Math.min(bestScore, score);
                beta = Math.min(beta, bestScore);
            }
            if (alpha >= beta) {
                break;
            }
        }
        return bestScore;
    }

    private static List<Move> orderedMoves(Board board, List<Move> legalMoves) {
        List<Move> ordered = new ArrayList<>(legalMoves);
        ordered.sort(Comparator.comparingInt((Move move) -> moveOrderScore(board, move)).reversed());
        return ordered;
    }

    private static int moveOrderScore(Board board, Move move) {
        Piece moving = board.pieceAt(move.from());
        Piece captured = board.pieceAt(move.to());
        int score = 0;
        if (captured != null) {
            score += captured.type().value() * 10 - moving.type().value();
        } else if (move.enPassant()) {
            score += PieceType.PAWN.value() * 10 - moving.type().value();
        }
        if (move.promotion() != null) {
            score += 80 + move.promotion().value();
        }
        return score;
    }
}
