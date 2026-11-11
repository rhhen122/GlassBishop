package org.GlassBishop.model;

import java.util.EnumSet;
import java.util.Set;

public final class Board {
    private final Piece[][] squares;
    private final Color sideToMove;
    private final EnumSet<CastlingRight> castlingRights;
    private final Square enPassantTarget;

    public Board(Piece[][] squares, Color sideToMove, Set<CastlingRight> castlingRights,
                 Square enPassantTarget) {
        this.squares = copySquares(squares);
        this.sideToMove = sideToMove;
        this.castlingRights = castlingRights.isEmpty()
                ? EnumSet.noneOf(CastlingRight.class) : EnumSet.copyOf(castlingRights);
        this.enPassantTarget = enPassantTarget;
    }

    public static Board standardStartingPosition() {
        Piece[][] squares = new Piece[8][8];
        PieceType[] backRank = {
                PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
                PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK
        };
        for (int file = 0; file < 8; file++) {
            squares[file][0] = new Piece(Color.WHITE, backRank[file]);
            squares[file][1] = new Piece(Color.WHITE, PieceType.PAWN);
            squares[file][6] = new Piece(Color.BLACK, PieceType.PAWN);
            squares[file][7] = new Piece(Color.BLACK, backRank[file]);
        }
        return new Board(squares, Color.WHITE, EnumSet.allOf(CastlingRight.class), null);
    }

    public Piece pieceAt(Square square) {
        return squares[square.file()][square.rank()];
    }

    public Color sideToMove() {
        return sideToMove;
    }

    public Set<CastlingRight> castlingRights() {
        return Set.copyOf(castlingRights);
    }

    public Square enPassantTarget() {
        return enPassantTarget;
    }

    public Square kingSquare(Color color) {
        for (int file = 0; file < 8; file++) {
            for (int rank = 0; rank < 8; rank++) {
                Piece piece = squares[file][rank];
                if (piece != null && piece.color() == color && piece.type() == PieceType.KING) {
                    return new Square(file, rank);
                }
            }
        }
        throw new IllegalStateException("Position has no " + color + " king");
    }

    public Board apply(Move move) {
        Piece[][] next = copySquares(squares);
        Piece moving = next[move.from().file()][move.from().rank()];
        next[move.from().file()][move.from().rank()] = null;
        if (move.enPassant()) {
            next[move.to().file()][move.from().rank()] = null;
        }

        Piece placed = move.promotion() == null
                ? moving : new Piece(moving.color(), move.promotion());
        next[move.to().file()][move.to().rank()] = placed;

        EnumSet<CastlingRight> rights = castlingRights.isEmpty()
                ? EnumSet.noneOf(CastlingRight.class) : EnumSet.copyOf(castlingRights);
        updateCastlingRights(rights, moving, move.from());
        Piece captured = squares[move.to().file()][move.to().rank()];
        if (captured != null) {
            updateCastlingRights(rights, captured, move.to());
        }
        if (move.castling()) {
            int rank = move.from().rank();
            boolean kingSide = move.to().file() > move.from().file();
            int rookFrom = kingSide ? 7 : 0;
            int rookTo = kingSide ? 5 : 3;
            next[rookTo][rank] = next[rookFrom][rank];
            next[rookFrom][rank] = null;
        }

        Square nextEnPassantTarget = null;
        if (moving.type() == PieceType.PAWN && Math.abs(move.to().rank() - move.from().rank()) == 2) {
            nextEnPassantTarget = new Square(move.from().file(),
                    (move.from().rank() + move.to().rank()) / 2);
        }
        return new Board(next, sideToMove.opposite(), rights, nextEnPassantTarget);
    }

    private static void updateCastlingRights(EnumSet<CastlingRight> rights, Piece piece, Square square) {
        if (piece.type() == PieceType.KING) {
            rights.remove(piece.color() == Color.WHITE ? CastlingRight.WHITE_KINGSIDE : CastlingRight.BLACK_KINGSIDE);
            rights.remove(piece.color() == Color.WHITE ? CastlingRight.WHITE_QUEENSIDE : CastlingRight.BLACK_QUEENSIDE);
        } else if (piece.type() == PieceType.ROOK) {
            if (piece.color() == Color.WHITE && square.equals(new Square(0, 0))) {
                rights.remove(CastlingRight.WHITE_QUEENSIDE);
            } else if (piece.color() == Color.WHITE && square.equals(new Square(7, 0))) {
                rights.remove(CastlingRight.WHITE_KINGSIDE);
            } else if (piece.color() == Color.BLACK && square.equals(new Square(0, 7))) {
                rights.remove(CastlingRight.BLACK_QUEENSIDE);
            } else if (piece.color() == Color.BLACK && square.equals(new Square(7, 7))) {
                rights.remove(CastlingRight.BLACK_KINGSIDE);
            }
        }
    }

    private static Piece[][] copySquares(Piece[][] source) {
        Piece[][] copy = new Piece[8][8];
        for (int file = 0; file < 8; file++) {
            System.arraycopy(source[file], 0, copy[file], 0, 8);
        }
        return copy;
    }
}
