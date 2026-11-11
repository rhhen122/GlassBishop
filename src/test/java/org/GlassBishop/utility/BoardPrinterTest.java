package org.GlassBishop.utility;

import org.GlassBishop.model.Board;
import org.GlassBishop.model.CastlingRight;
import org.GlassBishop.model.Color;
import org.GlassBishop.model.Piece;
import org.GlassBishop.model.PieceType;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardPrinterTest {
    @Test
    void printsBoardWithWhiteUppercaseBlackLowercaseAndCoordinates() {
        Piece[][] squares = new Piece[8][8];
        squares[4][0] = new Piece(Color.WHITE, PieceType.KING);
        squares[4][7] = new Piece(Color.BLACK, PieceType.KING);
        squares[0][1] = new Piece(Color.WHITE, PieceType.PAWN);
        squares[7][6] = new Piece(Color.BLACK, PieceType.PAWN);
        Board board = new Board(squares, Color.WHITE, EnumSet.noneOf(CastlingRight.class), null);

        String rendered = new BoardPrinter().print(board);

        assertTrue(rendered.startsWith("8 . . . . k . . . \n"));
        assertTrue(rendered.contains("7 . . . . . . . p \n"));
        assertTrue(rendered.contains("2 P . . . . . . . \n"));
        assertTrue(rendered.contains("1 . . . . K . . . \n"));
        assertTrue(rendered.endsWith("  a b c d e f g h"));
    }
}
