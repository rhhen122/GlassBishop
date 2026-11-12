package org.GlassBishop.engine;

import org.GlassBishop.model.CastlingRight;
import org.GlassBishop.model.Color;
import org.GlassBishop.model.PieceType;
import org.GlassBishop.model.Square;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoveSequenceConverterTest {
    private final MoveSequenceConverter converter = new MoveSequenceConverter();

    @Test
    void convertsLegalMovesAndRetainsPositionMetadata() {
        var board = converter.convert(List.of("e2e4", "e7e5", "g1f3"));

        assertEquals(Color.BLACK, board.sideToMove());
        assertEquals(PieceType.PAWN, board.pieceAt(Square.parse("e4")).type());
        assertEquals(PieceType.KNIGHT, board.pieceAt(Square.parse("f3")).type());
        assertNull(board.enPassantTarget());
        assertEquals(4, board.castlingRights().size());
    }

    @Test
    void updatesEnPassantAndCastlingRights() {
        var afterPawnMove = converter.convert(List.of("e2e4"));
        assertEquals(Square.parse("e3"), afterPawnMove.enPassantTarget());

        var afterKingMove = converter.convert(List.of("e2e4", "e7e5", "e1e2"));
        assertEquals(Color.BLACK, afterKingMove.sideToMove());
        assertFalse(afterKingMove.castlingRights().contains(CastlingRight.WHITE_KINGSIDE));
        assertFalse(afterKingMove.castlingRights().contains(CastlingRight.WHITE_QUEENSIDE));
    }

    @Test
    void rejectsIllegalMovesWithPlyNumber() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> converter.convert(List.of("e2e5")));
        assertEquals("Invalid or illegal UCI move at ply 1: e2e5", error.getMessage());
    }
}
