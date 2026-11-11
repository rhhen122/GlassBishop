package org.GlassBishop.engine;

import org.GlassBishop.model.Board;
import org.GlassBishop.model.CastlingRight;
import org.GlassBishop.model.Color;
import org.GlassBishop.model.Move;
import org.GlassBishop.model.Piece;
import org.GlassBishop.model.PieceType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DifficultyTest {
    @Test
    void configuresIncreasingSearchDepths() {
        assertEquals(1, Difficulty.LEVEL_1.searchDepth());
        assertEquals(2, Difficulty.LEVEL_2.searchDepth());
        assertEquals(3, Difficulty.LEVEL_3.searchDepth());
        assertEquals(4, Difficulty.LEVEL_4.searchDepth());
        assertEquals(5, Difficulty.LEVEL_5.searchDepth());
    }

    @Test
    void levelOneAlwaysChoosesALegalMove() {
        Board board = Board.standardStartingPosition();
        var legalMoves = new MoveGenerator(new AttackDetector()).legalMoves(board, Color.WHITE);

        Move selected = new SmartMoveSelector(Difficulty.LEVEL_1).bestMove(board).orElseThrow();

        assertTrue(legalMoves.contains(selected));
    }

    @Test
    void parsesDifficultyNamesWithoutCaseSensitivity() {
        assertEquals(Difficulty.LEVEL_4, Difficulty.parse("4"));
        assertEquals(Difficulty.LEVEL_1, Difficulty.parse("1"));
        assertEquals(Difficulty.LEVEL_5, Difficulty.parse("5"));
    }

    @Test
    void rejectsUnknownDifficulty() {
        assertThrows(IllegalArgumentException.class, () -> Difficulty.parse("6"));
    }

    @Test
    void acceptsSearchDepthAboveFive() {
        Piece[][] squares = new Piece[8][8];
        squares[0][0] = new Piece(Color.WHITE, PieceType.KING);
        squares[7][7] = new Piece(Color.BLACK, PieceType.KING);
        Board sparseBoard = new Board(squares, Color.WHITE,
                java.util.EnumSet.noneOf(CastlingRight.class), null);
        var messages = new ArrayList<String>();

        var move = new SmartMoveSelector(6).bestMove(sparseBoard, messages::add).orElseThrow();

        assertTrue(new MoveGenerator(new AttackDetector()).legalMoves(sparseBoard, Color.WHITE)
                .contains(move));
        assertTrue(messages.stream().anyMatch(message -> message.equals("Searching depth 6/6...")));
    }

    @Test
    void rejectsNonPositiveSearchDepth() {
        assertThrows(IllegalArgumentException.class, () -> new SmartMoveSelector(0));
    }

    @Test
    void reportsSearchProgressForTheSwingBot() {
        var messages = new ArrayList<String>();

        new SmartMoveSelector(Difficulty.LEVEL_2)
                .bestMove(Board.standardStartingPosition(), messages::add)
                .orElseThrow();

        assertTrue(messages.stream().anyMatch(message -> message.contains("Considering 20 legal moves")));
        assertTrue(messages.stream().anyMatch(message -> message.contains("Searching depth 2/2")));
        assertTrue(messages.getLast().startsWith("Search complete"));
    }
}
