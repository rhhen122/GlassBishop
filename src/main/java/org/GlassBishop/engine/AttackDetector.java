package org.GlassBishop.engine;

import org.GlassBishop.model.Board;
import org.GlassBishop.model.Color;
import org.GlassBishop.model.Piece;
import org.GlassBishop.model.PieceType;
import org.GlassBishop.model.Square;

public final class AttackDetector {
    private static final int[][] KNIGHT_STEPS = {
            {1, 2}, {2, 1}, {2, -1}, {1, -2},
            {-1, -2}, {-2, -1}, {-2, 1}, {-1, 2}
    };
    private static final int[][] KING_STEPS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    public boolean isAttacked(Board board, Square target, Color attacker) {
        int pawnSourceRank = target.rank() - (attacker == Color.WHITE ? 1 : -1);
        for (int fileOffset : new int[]{-1, 1}) {
            Piece piece = pieceAt(board, target.file() + fileOffset, pawnSourceRank);
            if (is(piece, attacker, PieceType.PAWN)) {
                return true;
            }
        }
        for (int[] step : KNIGHT_STEPS) {
            if (is(pieceAt(board, target.file() + step[0], target.rank() + step[1]),
                    attacker, PieceType.KNIGHT)) {
                return true;
            }
        }
        for (int[] step : KING_STEPS) {
            if (is(pieceAt(board, target.file() + step[0], target.rank() + step[1]),
                    attacker, PieceType.KING)) {
                return true;
            }
        }
        return rayAttacked(board, target, attacker, 1, 0, PieceType.ROOK)
                || rayAttacked(board, target, attacker, -1, 0, PieceType.ROOK)
                || rayAttacked(board, target, attacker, 0, 1, PieceType.ROOK)
                || rayAttacked(board, target, attacker, 0, -1, PieceType.ROOK)
                || rayAttacked(board, target, attacker, 1, 1, PieceType.BISHOP)
                || rayAttacked(board, target, attacker, 1, -1, PieceType.BISHOP)
                || rayAttacked(board, target, attacker, -1, 1, PieceType.BISHOP)
                || rayAttacked(board, target, attacker, -1, -1, PieceType.BISHOP);
    }

    public boolean isInCheck(Board board, Color color) {
        return isAttacked(board, board.kingSquare(color), color.opposite());
    }

    private boolean rayAttacked(Board board, Square target, Color attacker,
                                int fileStep, int rankStep, PieceType compatibleType) {
        int file = target.file() + fileStep;
        int rank = target.rank() + rankStep;
        while (inside(file, rank)) {
            Piece piece = board.pieceAt(new Square(file, rank));
            if (piece != null) {
                return piece.color() == attacker
                        && (piece.type() == compatibleType || piece.type() == PieceType.QUEEN);
            }
            file += fileStep;
            rank += rankStep;
        }
        return false;
    }

    private static Piece pieceAt(Board board, int file, int rank) {
        return inside(file, rank) ? board.pieceAt(new Square(file, rank)) : null;
    }

    private static boolean is(Piece piece, Color color, PieceType type) {
        return piece != null && piece.color() == color && piece.type() == type;
    }

    private static boolean inside(int file, int rank) {
        return file >= 0 && file < 8 && rank >= 0 && rank < 8;
    }
}
