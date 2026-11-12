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

    @Test
    void simpleFlagPrintsOnlyTheRecommendedMove() throws Exception {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream console = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(console, true, StandardCharsets.UTF_8));
            Main.main(new String[]{"examples/starting-position.json", "-depth=1", "-simple"});
        } finally {
            System.setOut(originalOut);
        }

        assertTrue(console.toString(StandardCharsets.UTF_8)
                .matches("[a-h][1-8][a-h][1-8][qrbn]?\\R"));
    }

    @Test
    void regularAnalysisPrintsTheBoardAfterTheRecommendedMove() throws Exception {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream console = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(console, true, StandardCharsets.UTF_8));
            Main.main(new String[]{"examples/starting-position.json", "-depth=1"});
        } finally {
            System.setOut(originalOut);
        }

        String output = console.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Board after recommended move (BLACK to move):"));
        assertTrue(output.indexOf("Board after recommended move")
                > output.indexOf("Recommended move:"));
    }

    @Test
    void colorFlagSelectsBlackMoveEvenWhenInputSaysWhiteToMove() throws Exception {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream console = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(console, true, StandardCharsets.UTF_8));
            Main.main(new String[]{
                    "examples/starting-position.json", "-depth=1", "-color=black", "-simple"
            });
        } finally {
            System.setOut(originalOut);
        }

        assertTrue(console.toString(StandardCharsets.UTF_8)
                .matches("[a-h][78][a-h][1-8][qrbn]?\\R"));
    }
}
