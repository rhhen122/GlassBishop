package org.GlassBishop.engine;

import org.GlassBishop.model.Board;
import org.GlassBishop.model.CastlingRight;
import org.GlassBishop.model.Color;
import org.GlassBishop.model.Piece;
import org.GlassBishop.model.PieceType;
import org.GlassBishop.model.Square;
import org.GlassBishop.scoring.StupidityScorer;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoveSelectorTest {
    private final AttackDetector attacks = new AttackDetector();
    private final MoveGenerator moves = new MoveGenerator(attacks);
    private final StupidityScorer scorer = new StupidityScorer();

    @Test
    void evaluatesEveryLegalMoveAndReturnsOneWithTheLowestResultingScore() {
        Board board = board(Color.WHITE,
                at("e1", Color.WHITE, PieceType.KING),
                at("d1", Color.WHITE, PieceType.QUEEN),
                at("e8", Color.BLACK, PieceType.KING),
                at("d8", Color.BLACK, PieceType.ROOK));
        var legalMoves = moves.legalMoves(board, board.sideToMove());
        int lowestScore = legalMoves.stream()
                .mapToInt(move -> scorer.score(board.apply(move)).score())
                .min()
                .orElseThrow();

        var recommendation = new MoveSelector().leastStupidMove(board).orElseThrow();

        assertEquals(legalMoves.size(), recommendation.legalMovesEvaluated());
        assertTrue(legalMoves.contains(recommendation.move()));
        assertEquals(lowestScore, recommendation.resultingPosition().score());
    }

    @Test
    void formatsPromotionMovesUsingUciNotation() {
        var move = new org.GlassBishop.model.Move(
                Square.parse("a7"), Square.parse("a8"), PieceType.KNIGHT, false, false);

        assertEquals("a7a8n", move.toUci());
    }

    private static Board board(Color sideToMove, At... pieces) {
        Piece[][] squares = new Piece[8][8];
        for (At piece : pieces) {
            Square square = Square.parse(piece.square());
            squares[square.file()][square.rank()] = piece.piece();
        }
        return new Board(squares, sideToMove, EnumSet.noneOf(CastlingRight.class), null);
    }

    private static At at(String square, Color color, PieceType type) {
        return new At(square, new Piece(color, type));
    }

    private record At(String square, Piece piece) {
    }
}
