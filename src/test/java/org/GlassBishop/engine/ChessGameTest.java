package org.GlassBishop.engine;

import org.GlassBishop.model.Color;
import org.GlassBishop.model.PieceType;
import org.GlassBishop.model.Square;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChessGameTest {
    @Test
    void startsFromStandardPositionAndPlaysWhiteThenEngineMoves() {
        ChessGame game = new ChessGame();

        assertEquals(20, game.legalMoves().size());
        game.playHumanMove(new MoveGenerator(new AttackDetector()).legalMoves(
                        game.board(), Color.WHITE).stream()
                .filter(move -> move.from().equals(Square.parse("e2"))
                        && move.to().equals(Square.parse("e4")))
                .findFirst().orElseThrow());
        assertEquals(Color.BLACK, game.board().sideToMove());

        game.playEngineMove();

        assertEquals(Color.WHITE, game.board().sideToMove());
        assertEquals(PieceType.PAWN, game.board().pieceAt(Square.parse("e4")).type());
    }

    @Test
    void rejectsIllegalHumanMoves() {
        ChessGame game = new ChessGame();
        var illegalMove = new org.GlassBishop.model.Move(Square.parse("e2"), Square.parse("e5"));

        assertThrows(IllegalArgumentException.class, () -> game.playHumanMove(illegalMove));
    }

    @Test
    void acceptsDifficultyForGameSetup() {
        ChessGame game = new ChessGame(Difficulty.LEVEL_1);

        assertEquals(20, game.legalMoves().size());
    }

    @Test
    void selfPlayCanApplyBotMovesForBothColors() {
        ChessGame game = new ChessGame(Difficulty.LEVEL_1);

        game.playBotMove();
        assertEquals(Color.BLACK, game.board().sideToMove());
        game.playBotMove();

        assertEquals(Color.WHITE, game.board().sideToMove());
        assertEquals(2, game.moveHistory().size());
    }
}
