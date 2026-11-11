package org.GlassBishop.ui;

import org.GlassBishop.model.Color;
import org.GlassBishop.model.Piece;
import org.GlassBishop.model.PieceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChessPieceSymbolTest {
    @Test
    void rendersUnicodeChessGlyphsForBothColors() {
        assertEquals("♔", ChessGameWindow.pieceSymbol(new Piece(Color.WHITE, PieceType.KING)));
        assertEquals("♟", ChessGameWindow.pieceSymbol(new Piece(Color.BLACK, PieceType.PAWN)));
        assertEquals("♕", ChessGameWindow.pieceSymbol(new Piece(Color.WHITE, PieceType.QUEEN)));
        assertEquals("♛", ChessGameWindow.pieceSymbol(new Piece(Color.BLACK, PieceType.QUEEN)));
    }
}
