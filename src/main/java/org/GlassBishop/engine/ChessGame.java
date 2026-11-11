package org.GlassBishop.engine;

import org.GlassBishop.model.Board;
import org.GlassBishop.model.Color;
import org.GlassBishop.model.Move;

import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;

public final class ChessGame {
    private final MoveGenerator moveGenerator = new MoveGenerator(new AttackDetector());
    private final SmartMoveSelector moveSelector;
    private final ChessNotation notation = new ChessNotation();
    private final List<String> moveHistory = new ArrayList<>();
    private Board board = Board.standardStartingPosition();

    public ChessGame() {
        this(Difficulty.LEVEL_4);
    }

    public ChessGame(Difficulty difficulty) {
        moveSelector = new SmartMoveSelector(difficulty);
    }

    public Board board() {
        return board;
    }

    public List<Move> legalMoves() {
        return moveGenerator.legalMoves(board, board.sideToMove());
    }

    public List<String> moveHistory() {
        return List.copyOf(moveHistory);
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
        return playEngineMove(message -> { });
    }

    public Move playEngineMove(Consumer<String> progress) {
        if (board.sideToMove() != Color.BLACK) {
            throw new IllegalStateException("It is not the engine's turn");
        }
        return playBotMove(progress);
    }

    public Move playBotMove() {
        return playBotMove(message -> { });
    }

    public Move playBotMove(Consumer<String> progress) {
        Move move = moveSelector.bestMove(board, progress)
                .orElseThrow(() -> new IllegalStateException("The engine has no legal move"));
        return playLegalMove(move);
    }

    private Move playLegalMove(Move move) {
        List<Move> legalMoves = legalMoves();
        if (!legalMoves.contains(move)) {
            throw new IllegalArgumentException("Move is not legal in the current position: " + move);
        }
        String san = notation.format(board, move, legalMoves);
        int moveNumber = moveHistory.size() / 2 + 1;
        moveHistory.add(board.sideToMove() == Color.WHITE
                ? moveNumber + ". " + san
                : moveNumber + "... " + san);
        board = board.apply(move);
        return move;
    }
}
