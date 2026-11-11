package org.GlassBishop.engine;

import org.GlassBishop.model.Board;
import org.GlassBishop.model.CastlingRight;
import org.GlassBishop.model.Color;
import org.GlassBishop.model.Move;
import org.GlassBishop.model.Piece;
import org.GlassBishop.model.PieceType;
import org.GlassBishop.model.Square;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChessNotationTest {
    private final AttackDetector attacks = new AttackDetector();
    private final MoveGenerator moveGenerator = new MoveGenerator(attacks);
    private final ChessNotation notation = new ChessNotation();

    @Test
    void formatsOrdinaryPawnMoveAndCapture() {
        Board board = board(Color.WHITE,
                at("e1", Color.WHITE, PieceType.KING),
                at("e2", Color.WHITE, PieceType.PAWN),
                at("d3", Color.BLACK, PieceType.PAWN),
                at("e8", Color.BLACK, PieceType.KING));

        assertEquals("e4", format(board, "e2", "e4"));
        assertEquals("exd3", format(board, "e2", "d3"));
    }

    @Test
    void formatsCastlingAndCheckmate() {
        Board castlingBoard = new Board(toSquares(
                at("e1", Color.WHITE, PieceType.KING),
                at("h1", Color.WHITE, PieceType.ROOK),
                at("e8", Color.BLACK, PieceType.KING)),
                Color.WHITE, EnumSet.of(CastlingRight.WHITE_KINGSIDE), null);
        Move castle = moveGenerator.legalMoves(castlingBoard, Color.WHITE).stream()
                .filter(Move::castling)
                .findFirst().orElseThrow();
        assertEquals("O-O", notation.format(castlingBoard, castle,
                moveGenerator.legalMoves(castlingBoard, Color.WHITE)));

        Board mateBoard = board(Color.WHITE,
                at("f6", Color.WHITE, PieceType.KING),
                at("g6", Color.WHITE, PieceType.QUEEN),
                at("h8", Color.BLACK, PieceType.KING));
        assertEquals("Qg7#", format(mateBoard, "g6", "g7"));
    }

    @Test
    void recordsWhiteAndBlackMovesWithMoveNumbers() {
        ChessGame game = new ChessGame();
        MoveGenerator generator = new MoveGenerator(attacks);
        Move whiteMove = generator.legalMoves(game.board(), Color.WHITE).stream()
                .filter(move -> move.from().equals(Square.parse("e2"))
                        && move.to().equals(Square.parse("e4")))
                .findFirst().orElseThrow();
        game.playHumanMove(whiteMove);
        game.playEngineMove();

        assertEquals("1. e4", game.moveHistory().getFirst());
        assertTrue(game.moveHistory().get(1).startsWith("1... "));
    }

    private String format(Board board, String from, String to) {
        Move move = moveGenerator.legalMoves(board, board.sideToMove()).stream()
                .filter(candidate -> candidate.from().equals(Square.parse(from))
                        && candidate.to().equals(Square.parse(to)))
                .findFirst().orElseThrow();
        return notation.format(board, move, moveGenerator.legalMoves(board, board.sideToMove()));
    }

    private static Board board(Color turn, At... pieces) {
        return new Board(toSquares(pieces), turn, EnumSet.noneOf(CastlingRight.class), null);
    }

    private static Piece[][] toSquares(At... pieces) {
        Piece[][] squares = new Piece[8][8];
        for (At piece : pieces) {
            Square square = Square.parse(piece.square());
            squares[square.file()][square.rank()] = piece.piece();
        }
        return squares;
    }

    private static At at(String square, Color color, PieceType type) {
        return new At(square, new Piece(color, type));
    }

    private record At(String square, Piece piece) {
    }
}
