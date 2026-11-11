package org.GlassBishop.engine;

import org.GlassBishop.model.Board;
import org.GlassBishop.model.Color;
import org.GlassBishop.model.Move;

import java.util.List;

public final class ChessGame {
    private final MoveGenerator moveGenerator = new MoveGenerator(new AttackDetector());
    private final SmartMoveSelector moveSelector = new SmartMoveSelector();
    private Board board = Board.standardStartingPosition();

    public Board board() {
        return board;
    }

    public List<Move> legalMoves() {
        return moveGenerator.legalMoves(board, board.sideToMove());
    }

    public boolean isCheckmate() {
        return legalMoves().isEmpty() && new PositionAnalyzer().isCheckmate(board, board.sideToMove());
    }

    public Move playHumanMove(Move move) {
        if (board.sideToMove() != Color.WHITE) {
            throw new IllegalStateException("It is not the human player's turn");
        }
        return playLegalMove(move);
    }

    public Move playEngineMove() {
        if (board.sideToMove() != Color.BLACK) {
            throw new IllegalStateException("It is not the engine's turn");
        }
        Move move = moveSelector.bestMove(board)
                .orElseThrow(() -> new IllegalStateException("The engine has no legal move"));
        board = board.apply(move);
        return move;
    }

    private Move playLegalMove(Move move) {
        if (!legalMoves().contains(move)) {
            throw new IllegalArgumentException("Move is not legal in the current position: " + move);
        }
        board = board.apply(move);
        return move;
    }
}
