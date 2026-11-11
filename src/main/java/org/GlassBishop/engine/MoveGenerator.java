package org.GlassBishop.engine;

import org.GlassBishop.model.Board;
import org.GlassBishop.model.CastlingRight;
import org.GlassBishop.model.Color;
import org.GlassBishop.model.Move;
import org.GlassBishop.model.Piece;
import org.GlassBishop.model.PieceType;
import org.GlassBishop.model.Square;

import java.util.ArrayList;
import java.util.List;

public final class MoveGenerator {
    private static final int[][] KNIGHT_STEPS = {
            {1, 2}, {2, 1}, {2, -1}, {1, -2},
            {-1, -2}, {-2, -1}, {-2, 1}, {-1, 2}
    };
    private static final int[][] KING_STEPS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };
    private static final int[][] ROOK_DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private static final int[][] BISHOP_DIRECTIONS = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
    private static final PieceType[] PROMOTIONS = {
            PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT
    };

    private final AttackDetector attacks;

    public MoveGenerator(AttackDetector attacks) {
        this.attacks = attacks;
    }

    public List<Move> legalMoves(Board board, Color color) {
        List<Move> legal = new ArrayList<>();
        for (Move move : pseudoLegalMoves(board, color)) {
            if (!attacks.isInCheck(board.apply(move), color)) {
                legal.add(move);
            }
        }
        return legal;
    }

    private List<Move> pseudoLegalMoves(Board board, Color color) {
        List<Move> moves = new ArrayList<>();
        for (int file = 0; file < 8; file++) {
            for (int rank = 0; rank < 8; rank++) {
                Square from = new Square(file, rank);
                Piece piece = board.pieceAt(from);
                if (piece == null || piece.color() != color) {
                    continue;
                }
                switch (piece.type()) {
                    case PAWN -> addPawnMoves(board, color, from, moves);
                    case KNIGHT -> addStepMoves(board, color, from, KNIGHT_STEPS, moves);
                    case BISHOP -> addSlidingMoves(board, color, from, BISHOP_DIRECTIONS, moves);
                    case ROOK -> addSlidingMoves(board, color, from, ROOK_DIRECTIONS, moves);
                    case QUEEN -> {
                        addSlidingMoves(board, color, from, BISHOP_DIRECTIONS, moves);
                        addSlidingMoves(board, color, from, ROOK_DIRECTIONS, moves);
                    }
                    case KING -> {
                        addStepMoves(board, color, from, KING_STEPS, moves);
                        addCastlingMoves(board, color, from, moves);
                    }
                }
            }
        }
        return moves;
    }

    private void addPawnMoves(Board board, Color color, Square from, List<Move> moves) {
        int direction = color == Color.WHITE ? 1 : -1;
        int startRank = color == Color.WHITE ? 1 : 6;
        int promotionRank = color == Color.WHITE ? 7 : 0;
        int nextRank = from.rank() + direction;
        if (inside(from.file(), nextRank) && board.pieceAt(new Square(from.file(), nextRank)) == null) {
            addPawnMove(from, new Square(from.file(), nextRank), promotionRank, moves);
            int doubleRank = from.rank() + 2 * direction;
            if (from.rank() == startRank && board.pieceAt(new Square(from.file(), doubleRank)) == null) {
                moves.add(new Move(from, new Square(from.file(), doubleRank)));
            }
        }
        for (int fileStep : new int[]{-1, 1}) {
            int targetFile = from.file() + fileStep;
            if (!inside(targetFile, nextRank)) {
                continue;
            }
            Square target = new Square(targetFile, nextRank);
            Piece targetPiece = board.pieceAt(target);
            if (targetPiece != null && targetPiece.color() != color
                    && targetPiece.type() != PieceType.KING) {
                addPawnMove(from, target, promotionRank, moves);
            } else if (targetPiece == null && target.equals(board.enPassantTarget())) {
                moves.add(new Move(from, target, null, true, false));
            }
        }
    }

    private void addPawnMove(Square from, Square to, int promotionRank, List<Move> moves) {
        if (to.rank() == promotionRank) {
            for (PieceType promotion : PROMOTIONS) {
                moves.add(new Move(from, to, promotion, false, false));
            }
        } else {
            moves.add(new Move(from, to));
        }
    }

    private void addStepMoves(Board board, Color color, Square from, int[][] steps, List<Move> moves) {
        for (int[] step : steps) {
            int file = from.file() + step[0];
            int rank = from.rank() + step[1];
            if (!inside(file, rank)) {
                continue;
            }
            Piece target = board.pieceAt(new Square(file, rank));
            if (target == null || (target.color() != color && target.type() != PieceType.KING)) {
                moves.add(new Move(from, new Square(file, rank)));
            }
        }
    }

    private void addSlidingMoves(Board board, Color color, Square from,
                                 int[][] directions, List<Move> moves) {
        for (int[] direction : directions) {
            int file = from.file() + direction[0];
            int rank = from.rank() + direction[1];
            while (inside(file, rank)) {
                Square to = new Square(file, rank);
                Piece target = board.pieceAt(to);
                if (target == null) {
                    moves.add(new Move(from, to));
                } else {
                    if (target.color() != color && target.type() != PieceType.KING) {
                        moves.add(new Move(from, to));
                    }
                    break;
                }
                file += direction[0];
                rank += direction[1];
            }
        }
    }

    private void addCastlingMoves(Board board, Color color, Square king, List<Move> moves) {
        int homeRank = color == Color.WHITE ? 0 : 7;
        if (king.file() != 4 || king.rank() != homeRank || attacks.isInCheck(board, color)) {
            return;
        }
        CastlingRight kingSide = color == Color.WHITE
                ? CastlingRight.WHITE_KINGSIDE : CastlingRight.BLACK_KINGSIDE;
        CastlingRight queenSide = color == Color.WHITE
                ? CastlingRight.WHITE_QUEENSIDE : CastlingRight.BLACK_QUEENSIDE;
        Piece rook = board.pieceAt(new Square(7, homeRank));
        if (board.castlingRights().contains(kingSide) && isRook(rook, color)
                && board.pieceAt(new Square(5, homeRank)) == null
                && board.pieceAt(new Square(6, homeRank)) == null
                && !attacks.isAttacked(board, new Square(5, homeRank), color.opposite())
                && !attacks.isAttacked(board, new Square(6, homeRank), color.opposite())) {
            moves.add(new Move(king, new Square(6, homeRank), null, false, true));
        }
        rook = board.pieceAt(new Square(0, homeRank));
        if (board.castlingRights().contains(queenSide) && isRook(rook, color)
                && board.pieceAt(new Square(1, homeRank)) == null
                && board.pieceAt(new Square(2, homeRank)) == null
                && board.pieceAt(new Square(3, homeRank)) == null
                && !attacks.isAttacked(board, new Square(3, homeRank), color.opposite())
                && !attacks.isAttacked(board, new Square(2, homeRank), color.opposite())) {
            moves.add(new Move(king, new Square(2, homeRank), null, false, true));
        }
    }

    private static boolean isRook(Piece piece, Color color) {
        return piece != null && piece.color() == color && piece.type() == PieceType.ROOK;
    }

    private static boolean inside(int file, int rank) {
        return file >= 0 && file < 8 && rank >= 0 && rank < 8;
    }
}
