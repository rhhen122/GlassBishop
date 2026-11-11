package org.GlassBishop.model;

public enum PieceType {
    PAWN(1),
    KNIGHT(3),
    BISHOP(3),
    ROOK(5),
    QUEEN(9),
    KING(0);

    private final int value;

    PieceType(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public int attackValue() {
        return this == KING ? 9 : value;
    }
}
