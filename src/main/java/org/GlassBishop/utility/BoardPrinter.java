package org.GlassBishop.utility;

import org.GlassBishop.model.Board;
import org.GlassBishop.model.Color;
import org.GlassBishop.model.Piece;
import org.GlassBishop.model.PieceType;
import org.GlassBishop.model.Square;

public final class BoardPrinter {
    public String print(Board board) {
        StringBuilder output = new StringBuilder();
        for (int rank = 7; rank >= 0; rank--) {
            output.append(rank + 1).append(' ');
            for (int file = 0; file < 8; file++) {
                Piece piece = board.pieceAt(new Square(file, rank));
                output.append(piece == null ? '.' : symbol(piece)).append(' ');
            }
            output.append('\n');
        }
        output.append("  a b c d e f g h");
        return output.toString();
    }

    private static char symbol(Piece piece) {
        char symbol = switch (piece.type()) {
            case PAWN -> 'p';
            case KNIGHT -> 'n';
            case BISHOP -> 'b';
            case ROOK -> 'r';
            case QUEEN -> 'q';
            case KING -> 'k';
        };
        return piece.color() == Color.WHITE
                ? Character.toUpperCase(symbol) : symbol;
    }
}
