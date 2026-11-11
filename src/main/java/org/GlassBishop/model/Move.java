package org.GlassBishop.model;

import java.util.Locale;

public record Move(Square from, Square to, PieceType promotion, boolean enPassant, boolean castling) {
    public Move(Square from, Square to) {
        this(from, to, null, false, false);
    }

    public String toUci() {
        String move = from + to.toString();
        if (promotion == null) {
            return move;
        }
        String suffix = switch (promotion) {
            case QUEEN -> "q";
            case ROOK -> "r";
            case BISHOP -> "b";
            case KNIGHT -> "n";
            default -> throw new IllegalStateException("Invalid promotion piece: " + promotion);
        };
        return move + suffix;
    }
}
