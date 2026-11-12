package org.GlassBishop.engine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class StockfishEngineTest {
    @TempDir
    Path tempDir;

    @Test
    void speaksUciAndReturnsTheBestMove() throws Exception {
        assumeTrue(System.getProperty("os.name").toLowerCase().contains("mac")
                || System.getProperty("os.name").toLowerCase().contains("nix")
                || System.getProperty("os.name").toLowerCase().contains("nux"));
        Path fakeStockfish = tempDir.resolve("fake-stockfish");
        Files.writeString(fakeStockfish, """
                #!/bin/sh
                while IFS= read -r command; do
                  case "$command" in
                    uci) echo "id name test"; echo "uciok" ;;
                    isready) echo "readyok" ;;
                    "go depth "*) echo "bestmove e7e5" ;;
                    quit) exit 0 ;;
                  esac
                done
                """);
        assumeTrue(fakeStockfish.toFile().setExecutable(true));

        try (StockfishEngine engine = new StockfishEngine(fakeStockfish)) {
            assertEquals("e7e5", engine.bestMove(List.of("e2e4"), 2));
        }
    }
}
