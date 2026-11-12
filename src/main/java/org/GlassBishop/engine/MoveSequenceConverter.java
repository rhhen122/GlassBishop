package org.GlassBishop.engine;

import org.GlassBishop.model.Board;
import org.GlassBishop.model.Move;

import java.util.List;

public final class MoveSequenceConverter {
    private final MoveGenerator moveGenerator = new MoveGenerator(new AttackDetector());

    public Board convert(List<String> uciMoves) {
        Board board = Board.standardStartingPosition();
        for (int index = 0; index < uciMoves.size(); index++) {
            String uciMove = uciMoves.get(index);
            int ply = index + 1;
            Move move = moveGenerator.legalMoves(board, board.sideToMove()).stream()
                    .filter(candidate -> candidate.toUci().equalsIgnoreCase(uciMove))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Invalid or illegal UCI move at ply " + ply + ": " + uciMove));
            board = board.apply(move);
        }
        return board;
    }
}
