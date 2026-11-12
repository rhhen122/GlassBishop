package org.GlassBishop;

import org.GlassBishop.io.PositionJsonReader;
import org.GlassBishop.model.Color;
import org.GlassBishop.model.PieceType;
import org.GlassBishop.model.Square;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {
    @TempDir
    Path tempDir;

    @Test
    void convertCommandWritesAnEngineReadablePosition() throws Exception {
        Path output = tempDir.resolve("game.json");
        PrintStream originalOut = System.out;
        ByteArrayOutputStream console = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(console, true, StandardCharsets.UTF_8));
            Main.main(new String[]{"convert", output.toString(), "e2e4", "e7e5", "g1f3"});
        } finally {
            System.setOut(originalOut);
        }

        var board = new PositionJsonReader().read(output);
        assertEquals(Color.BLACK, board.sideToMove());
        assertEquals(PieceType.KNIGHT, board.pieceAt(Square.parse("f3")).type());
        assertTrue(console.toString(StandardCharsets.UTF_8).contains("Wrote position after 3 move(s)"));
    }
}
