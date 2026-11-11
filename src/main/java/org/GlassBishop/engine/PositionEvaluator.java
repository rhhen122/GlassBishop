package org.GlassBishop.engine;

import org.GlassBishop.model.Board;
import org.GlassBishop.model.Color;
import org.GlassBishop.model.Piece;
import org.GlassBishop.model.PieceType;
import org.GlassBishop.model.Square;

public final class PositionEvaluator {
    private static final int MATE_SCORE = 100_000;

    private final AttackDetector attacks = new AttackDetector();

    public int evaluate(Board board) {
        int score = 0;
        for (int file = 0; file < 8; file++) {
            for (int rank = 0; rank < 8; rank++) {
                Square square = new Square(file, rank);
                Piece piece = board.pieceAt(square);
                if (piece == null) {
                    continue;
                }
                int sign = piece.color() == Color.WHITE ? 1 : -1;
                score += sign * piece.type().value() * 100;
                score += sign * positionalBonus(piece, square);
                if (attacks.isAttacked(board, square, piece.color().opposite())) {
                    score -= sign * attackedPenalty(piece.type());
                }
            }
        }
        return score;
    }

    public int checkmateScore(Color matedSide, int ply) {
        return matedSide == Color.WHITE ? -MATE_SCORE + ply : MATE_SCORE - ply;
    }

    private static int positionalBonus(Piece piece, Square square) {
        int centerDistance = Math.abs(3 - square.file()) + Math.abs(3 - square.rank());
        int centerBonus = Math.max(0, 6 - centerDistance) * 3;
        return switch (piece.type()) {
            case PAWN -> {
                int advancement = piece.color() == Color.WHITE ? square.rank() : 7 - square.rank();
                yield advancement * 5 + (square.file() >= 2 && square.file() <= 5 ? 5 : 0);
            }
            case KNIGHT, BISHOP -> centerBonus;
            case QUEEN -> centerBonus / 2;
            case ROOK -> piece.color() == Color.WHITE && square.rank() == 6
                    || piece.color() == Color.BLACK && square.rank() == 1 ? 12 : 0;
            case KING -> 0;
        };
    }

    private static int attackedPenalty(PieceType type) {
        return switch (type) {
            case PAWN -> 8;
            case KNIGHT, BISHOP -> 18;
            case ROOK -> 28;
            case QUEEN -> 45;
            case KING -> 120;
        };
    }
}
