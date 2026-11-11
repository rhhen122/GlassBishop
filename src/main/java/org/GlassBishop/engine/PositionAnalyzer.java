package org.GlassBishop.engine;

import org.GlassBishop.model.Board;
import org.GlassBishop.model.Color;
import org.GlassBishop.model.Move;
import org.GlassBishop.model.Piece;
import org.GlassBishop.model.Square;

import java.util.List;

public final class PositionAnalyzer {
    private final AttackDetector attacks = new AttackDetector();
    private final MoveGenerator moves = new MoveGenerator(attacks);

    public int whiteMaterial(Board board) {
        int material = 0;
        for (int file = 0; file < 8; file++) {
            for (int rank = 0; rank < 8; rank++) {
                Piece piece = board.pieceAt(new Square(file, rank));
                if (piece != null && piece.color() == Color.WHITE) {
                    material += piece.type().value();
                }
            }
        }
        return material;
    }

    public int attackedWhiteMaterial(Board board) {
        int material = 0;
        for (int file = 0; file < 8; file++) {
            for (int rank = 0; rank < 8; rank++) {
                Square square = new Square(file, rank);
                Piece piece = board.pieceAt(square);
                if (piece != null && piece.color() == Color.WHITE
                        && attacks.isAttacked(board, square, Color.BLACK)) {
                    material += piece.type().attackValue();
                }
            }
        }
        return material;
    }

    public boolean whiteCanMateInOne(Board board) {
        if (board.sideToMove() != Color.WHITE) {
            return false;
        }
        for (Move move : moves.legalMoves(board, Color.WHITE)) {
            Board afterMove = board.apply(move);
            if (attacks.isInCheck(afterMove, Color.BLACK)
                    && moves.legalMoves(afterMove, Color.BLACK).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public boolean isCheckmate(Board board, Color color) {
        return attacks.isInCheck(board, color) && moves.legalMoves(board, color).isEmpty();
    }
}
