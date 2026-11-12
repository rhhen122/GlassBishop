package org.GlassBishop.io;

import org.GlassBishop.engine.MoveSequenceConverter;
import org.GlassBishop.model.Color;
import org.GlassBishop.model.PieceType;
import org.GlassBishop.model.Square;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PositionJsonWriterTest {
    @TempDir
    Path tempDir;

    @Test
    void writesAReadablePositionIncludingSpecialMoveState() throws Exception {
        var expected = new MoveSequenceConverter().convert(List.of("e2e4"));
        Path output = tempDir.resolve("position.json");

        new PositionJsonWriter().write(expected, output);
        var actual = new PositionJsonReader().read(output);

        assertEquals(expected.sideToMove(), actual.sideToMove());
        assertEquals(expected.pieceAt(Square.parse("e4")), actual.pieceAt(Square.parse("e4")));
        assertEquals(expected.castlingRights(), actual.castlingRights());
        assertEquals(Square.parse("e3"), actual.enPassantTarget());
        assertEquals(Color.BLACK, actual.sideToMove());
        assertEquals(PieceType.PAWN, actual.pieceAt(Square.parse("e4")).type());
    }
}
