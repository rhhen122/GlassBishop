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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PositionAnalyzerTest {
    private final PositionAnalyzer analyzer = new PositionAnalyzer();
    private final AttackDetector attacks = new AttackDetector();

    @Test
    void detectsQueenMateInOne() {
        Board board = board(Color.WHITE,
                at("f6", Color.WHITE, PieceType.KING),
                at("g6", Color.WHITE, PieceType.QUEEN),
                at("h8", Color.BLACK, PieceType.KING));

        assertTrue(analyzer.whiteCanMateInOne(board));
    }

    @Test
    void detectsCheckAndDoesNotTreatAnAttackedKingSquareAsACapture() {
        Board board = board(Color.WHITE,
                at("e1", Color.WHITE, PieceType.KING),
                at("e2", Color.BLACK, PieceType.KING));

        assertTrue(attacks.isInCheck(board, Color.WHITE));
        assertFalse(new MoveGenerator(attacks).legalMoves(board, Color.WHITE).stream()
                .anyMatch(move -> move.to().equals(Square.parse("e2"))));
    }

    @Test
    void detectsEnPassantAndRemovesTheCapturedPawn() {
        Board board = board(Color.WHITE, Square.parse("d6"),
                at("a1", Color.WHITE, PieceType.KING),
                at("e5", Color.WHITE, PieceType.PAWN),
                at("h8", Color.BLACK, PieceType.KING),
                at("d5", Color.BLACK, PieceType.PAWN));

        var move = new MoveGenerator(attacks).legalMoves(board, Color.WHITE).stream()
                .filter(candidate -> candidate.from().equals(Square.parse("e5"))
                        && candidate.to().equals(Square.parse("d6")))
                .findFirst().orElseThrow();

        Board afterMove = board.apply(move);
        assertTrue(afterMove.pieceAt(Square.parse("d6")).type() == PieceType.PAWN);
        assertTrue(afterMove.pieceAt(Square.parse("d5")) == null);
    }

    @Test
    void castlesAndMovesTheRookWhenThePathIsSafe() {
        Board board = board(Color.WHITE, (Square) null,
                at("e1", Color.WHITE, PieceType.KING),
                at("h1", Color.WHITE, PieceType.ROOK),
                at("a8", Color.BLACK, PieceType.KING));
        board = new Board(toArray(board), Color.WHITE, EnumSet.of(CastlingRight.WHITE_KINGSIDE), null);

        var castle = new MoveGenerator(attacks).legalMoves(board, Color.WHITE).stream()
                .filter(move -> move.castling() && move.to().equals(Square.parse("g1")))
                .findFirst().orElseThrow();

        Board afterMove = board.apply(castle);
        assertEquals(PieceType.KING, afterMove.pieceAt(Square.parse("g1")).type());
        assertEquals(PieceType.ROOK, afterMove.pieceAt(Square.parse("f1")).type());
        assertFalse(afterMove.castlingRights().contains(CastlingRight.WHITE_KINGSIDE));
    }

    @Test
    void cannotCastleThroughAnAttackedSquare() {
        Board position = board(Color.WHITE,
                at("e1", Color.WHITE, PieceType.KING),
                at("h1", Color.WHITE, PieceType.ROOK),
                at("a8", Color.BLACK, PieceType.KING),
                at("f8", Color.BLACK, PieceType.ROOK));
        Board board = new Board(toArray(position), Color.WHITE,
                EnumSet.of(CastlingRight.WHITE_KINGSIDE), null);

        assertFalse(new MoveGenerator(attacks).legalMoves(board, Color.WHITE).stream()
                .anyMatch(move -> move.castling()));
    }

    @Test
    void generatesAllFourPawnPromotions() {
        Board board = board(Color.WHITE,
                at("e1", Color.WHITE, PieceType.KING),
                at("a7", Color.WHITE, PieceType.PAWN),
                at("h8", Color.BLACK, PieceType.KING));

        long promotions = new MoveGenerator(attacks).legalMoves(board, Color.WHITE).stream()
                .filter(move -> move.from().equals(Square.parse("a7"))
                        && move.to().equals(Square.parse("a8")) && move.promotion() != null)
                .count();

        assertEquals(4, promotions);
    }

    private static At at(String square, Color color, PieceType type) {
        return new At(Square.parse(square), new Piece(color, type));
    }

    private static Board board(Color turn, At... pieces) {
        return board(turn, null, pieces);
    }

    private static Board board(Color turn, Square enPassantTarget, At... pieces) {
        Piece[][] squares = new Piece[8][8];
        for (At at : pieces) {
            squares[at.square().file()][at.square().rank()] = at.piece();
        }
        return new Board(squares, turn, EnumSet.noneOf(CastlingRight.class), enPassantTarget);
    }

    private record At(Square square, Piece piece) {
    }

    private static Piece[][] toArray(Board board) {
        Piece[][] squares = new Piece[8][8];
        for (int file = 0; file < 8; file++) {
            for (int rank = 0; rank < 8; rank++) {
                Square square = new Square(file, rank);
                squares[file][rank] = board.pieceAt(square);
            }
        }
        return squares;
    }
}
