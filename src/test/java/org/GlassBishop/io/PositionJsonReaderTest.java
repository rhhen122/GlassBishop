package org.GlassBishop.io;

import org.GlassBishop.model.Board;
import org.GlassBishop.model.Color;
import org.GlassBishop.model.PieceType;
import org.GlassBishop.model.Square;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PositionJsonReaderTest {
    @Test
    void readsUserPositionJson() throws IOException, URISyntaxException {
        Path position = Path.of(getClass().getResource("/position.json").toURI());
        Board board = new PositionJsonReader().read(position);

        assertEquals(Color.WHITE, board.sideToMove());
        assertEquals(PieceType.QUEEN, board.pieceAt(Square.parse("d1")).type());
        assertEquals(Color.BLACK, board.pieceAt(Square.parse("a8")).color());
        assertNull(board.enPassantTarget());
    }
}
