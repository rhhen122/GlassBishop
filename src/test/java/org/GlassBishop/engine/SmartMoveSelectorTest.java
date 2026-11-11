package org.GlassBishop.engine;

import org.GlassBishop.model.Board;
import org.GlassBishop.model.CastlingRight;
import org.GlassBishop.model.Color;
import org.GlassBishop.model.Piece;
import org.GlassBishop.model.PieceType;
import org.GlassBishop.model.Square;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmartMoveSelectorTest {
    @Test
    void blackSearchCapturesTheCheckingWhiteQueen() {
        Piece[][] squares = new Piece[8][8];
        place(squares, "a1", Color.WHITE, PieceType.KING);
        place(squares, "d4", Color.WHITE, PieceType.QUEEN);
        place(squares, "h8", Color.BLACK, PieceType.KING);
        place(squares, "d8", Color.BLACK, PieceType.ROOK);
        Board board = new Board(squares, Color.BLACK, EnumSet.noneOf(CastlingRight.class), null);

        var move = new SmartMoveSelector().bestMove(board).orElseThrow();

        assertEquals("d8d4", move.toUci());
    }

    @Test
    void evaluationRewardsMaterialForWhiteAndPenalizesMaterialLoss() {
        Piece[][] withWhiteQueen = new Piece[8][8];
        place(withWhiteQueen, "a1", Color.WHITE, PieceType.KING);
        place(withWhiteQueen, "h8", Color.BLACK, PieceType.KING);
        place(withWhiteQueen, "d4", Color.WHITE, PieceType.QUEEN);
        Board whiteAdvantage = new Board(withWhiteQueen, Color.WHITE,
                EnumSet.noneOf(CastlingRight.class), null);

        Piece[][] withoutQueen = new Piece[8][8];
        place(withoutQueen, "a1", Color.WHITE, PieceType.KING);
        place(withoutQueen, "h8", Color.BLACK, PieceType.KING);
        Board equalMaterial = new Board(withoutQueen, Color.WHITE,
                EnumSet.noneOf(CastlingRight.class), null);

        PositionEvaluator evaluator = new PositionEvaluator();
        assertTrue(evaluator.evaluate(whiteAdvantage) > evaluator.evaluate(equalMaterial));
    }

    private static void place(Piece[][] squares, String notation, Color color, PieceType type) {
        Square square = Square.parse(notation);
        squares[square.file()][square.rank()] = new Piece(color, type);
    }
}
