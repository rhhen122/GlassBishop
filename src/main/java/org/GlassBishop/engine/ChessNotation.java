package org.GlassBishop.engine;

import org.GlassBishop.model.Board;
import org.GlassBishop.model.Color;
import org.GlassBishop.model.Move;
import org.GlassBishop.model.Piece;
import org.GlassBishop.model.PieceType;
import org.GlassBishop.model.Square;

import java.util.List;

public final class ChessNotation {
    private final AttackDetector attacks = new AttackDetector();
    private final MoveGenerator moveGenerator = new MoveGenerator(attacks);

    public String format(Board board, Move move, List<Move> legalMoves) {
        Piece movingPiece = board.pieceAt(move.from());
        StringBuilder notation = new StringBuilder();
        if (move.castling()) {
            notation.append(move.to().file() > move.from().file() ? "O-O" : "O-O-O");
        } else {
            boolean capture = move.enPassant() || board.pieceAt(move.to()) != null;
            if (movingPiece.type() == PieceType.PAWN) {
                if (capture) {
                    notation.append((char) ('a' + move.from().file()));
                }
            } else {
                notation.append(pieceLetter(movingPiece.type()));
                appendDisambiguation(notation, board, move, movingPiece, legalMoves);
            }
            if (capture) {
                notation.append('x');
            }
            notation.append(move.to());
            if (move.promotion() != null) {
                notation.append('=').append(pieceLetter(move.promotion()));
            }
        }

        Board resultingBoard = board.apply(move);
        Color checkedSide = resultingBoard.sideToMove();
        if (attacks.isInCheck(resultingBoard, checkedSide)) {
            notation.append(moveGenerator.legalMoves(resultingBoard, checkedSide).isEmpty() ? '#' : '+');
        }
        return notation.toString();
    }

    private static void appendDisambiguation(StringBuilder notation, Board board, Move move,
                                             Piece movingPiece, List<Move> legalMoves) {
        List<Square> alternatives = legalMoves.stream()
                .filter(candidate -> !candidate.from().equals(move.from()))
                .filter(candidate -> candidate.to().equals(move.to()))
                .filter(candidate -> {
                    Piece other = board.pieceAt(candidate.from());
                    return other != null && other.color() == movingPiece.color()
                            && other.type() == movingPiece.type();
                })
                .map(Move::from)
                .toList();
        if (alternatives.isEmpty()) {
            return;
        }
        boolean sameFileExists = alternatives.stream().anyMatch(square -> square.file() == move.from().file());
        boolean sameRankExists = alternatives.stream().anyMatch(square -> square.rank() == move.from().rank());
        if (!sameFileExists) {
            notation.append((char) ('a' + move.from().file()));
        } else if (!sameRankExists) {
            notation.append((char) ('1' + move.from().rank()));
        } else {
            notation.append((char) ('a' + move.from().file()));
            notation.append((char) ('1' + move.from().rank()));
        }
    }

    private static char pieceLetter(PieceType type) {
        return switch (type) {
            case KNIGHT -> 'N';
            case BISHOP -> 'B';
            case ROOK -> 'R';
            case QUEEN -> 'Q';
            case KING -> 'K';
            case PAWN -> throw new IllegalArgumentException("Pawn has no SAN piece letter");
        };
    }
}
