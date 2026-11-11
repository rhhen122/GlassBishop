package org.GlassBishop.model;

public record Square(int file, int rank) {
    public Square {
        if (file < 0 || file > 7 || rank < 0 || rank > 7) {
            throw new IllegalArgumentException("Square is outside the chess board");
        }
    }

    public static Square parse(String notation) {
        if (notation == null || !notation.matches("[a-h][1-8]")) {
            throw new IllegalArgumentException("Invalid square: " + notation);
        }
        return new Square(notation.charAt(0) - 'a', notation.charAt(1) - '1');
    }

    @Override
    public String toString() {
        return "" + (char) ('a' + file) + (char) ('1' + rank);
    }
}
