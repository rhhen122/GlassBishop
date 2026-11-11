package org.GlassBishop.engine;

import org.GlassBishop.model.Board;
import org.GlassBishop.model.CastlingRight;
import org.GlassBishop.model.Color;
import org.GlassBishop.model.Move;
import org.GlassBishop.model.Piece;
import org.GlassBishop.model.PieceType;
import org.GlassBishop.model.Square;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public final class SmartMoveSelector {
    private static final int INFINITY = 1_000_000;
    private static final int MAX_QUIESCENCE_PLY = 5;

    private final Difficulty difficulty;
    private final AttackDetector attacks = new AttackDetector();
    private final MoveGenerator moveGenerator = new MoveGenerator(attacks);
    private final PositionEvaluator evaluator = new PositionEvaluator();
    private final Map<String, TranspositionEntry> transpositions = new HashMap<>();

    public SmartMoveSelector(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public Optional<Move> bestMove(Board board) {
        return bestMove(board, message -> { });
    }

    public Optional<Move> bestMove(Board board, Consumer<String> progress) {
        transpositions.clear();
        progress.accept("Finding legal moves...");
        List<Move> legalMoves = orderedMoves(board, moveGenerator.legalMoves(board, board.sideToMove()));
        if (legalMoves.isEmpty()) {
            progress.accept("No legal moves found.");
            return Optional.empty();
        }
        progress.accept("Considering " + legalMoves.size() + " legal moves...");
        Move bestMove = legalMoves.getFirst();
        for (int depth = 1; depth <= difficulty.searchDepth(); depth++) {
            progress.accept("Searching depth " + depth + "/" + difficulty.searchDepth() + "...");
            List<Move> iterationMoves = orderedMoves(board, moveGenerator.legalMoves(board, board.sideToMove()));
            Move preferredMove = bestMove;
            iterationMoves.sort(Comparator.comparing((Move move) -> !move.equals(preferredMove)));
            bestMove = searchRoot(board, iterationMoves, depth, progress);
        }
        progress.accept("Search complete. Best move: " + bestMove.toUci());
        return Optional.of(bestMove);
    }

    private Move searchRoot(Board board, List<Move> moves, int depth, Consumer<String> progress) {
        boolean maximizing = board.sideToMove() == Color.WHITE;
        Move bestMove = moves.getFirst();
        int bestScore = maximizing ? -INFINITY : INFINITY;
        int alpha = -INFINITY;
        int beta = INFINITY;
        for (int index = 0; index < moves.size(); index++) {
            Move move = moves.get(index);
            progress.accept("Depth " + depth + ": considering move " + (index + 1) + "/"
                    + moves.size() + " (" + move.toUci() + ")");
            int score = search(board.apply(move), depth - 1, alpha, beta, 1);
            if (maximizing ? score > bestScore : score < bestScore) {
                bestScore = score;
                bestMove = move;
            }
            if (maximizing) {
                alpha = Math.max(alpha, bestScore);
            } else {
                beta = Math.min(beta, bestScore);
            }
        }
        return bestMove;
    }

    private int search(Board board, int depth, int alpha, int beta, int ply) {
        int originalAlpha = alpha;
        int originalBeta = beta;
        String key = positionKey(board);
        TranspositionEntry cached = transpositions.get(key);
        if (cached != null && cached.depth() >= depth) {
            int cachedScore = scoreFromTable(cached.score(), ply);
            if (cached.bound() == Bound.EXACT) {
                return cachedScore;
            }
            if (cached.bound() == Bound.LOWER) {
                alpha = Math.max(alpha, cachedScore);
            } else {
                beta = Math.min(beta, cachedScore);
            }
            if (alpha >= beta) {
                return cachedScore;
            }
        }

        List<Move> legalMoves = moveGenerator.legalMoves(board, board.sideToMove());
        if (legalMoves.isEmpty()) {
            return attacks.isInCheck(board, board.sideToMove())
                    ? evaluator.checkmateScore(board.sideToMove(), ply) : 0;
        }
        if (depth == 0) {
            return quiescence(board, alpha, beta, ply, 0);
        }

        boolean maximizing = board.sideToMove() == Color.WHITE;
        int bestScore = maximizing ? -INFINITY : INFINITY;
        Move bestMove = null;
        List<Move> ordered = orderedMoves(board, legalMoves);
        if (cached != null && cached.bestMove() != null) {
            ordered.sort(Comparator.comparing((Move move) -> !move.equals(cached.bestMove())));
        }
        for (Move move : ordered) {
            int score = search(board.apply(move), depth - 1, alpha, beta, ply + 1);
            if (maximizing) {
                if (score > bestScore) {
                    bestScore = score;
                    bestMove = move;
                }
                alpha = Math.max(alpha, bestScore);
            } else {
                if (score < bestScore) {
                    bestScore = score;
                    bestMove = move;
                }
                beta = Math.min(beta, bestScore);
            }
            if (alpha >= beta) {
                break;
            }
        }
        Bound bound = bestScore <= originalAlpha ? Bound.UPPER
                : bestScore >= originalBeta ? Bound.LOWER : Bound.EXACT;
        transpositions.put(key, new TranspositionEntry(depth, scoreForTable(bestScore, ply), bound, bestMove));
        return bestScore;
    }

    private int quiescence(Board board, int alpha, int beta, int ply, int qply) {
        List<Move> legalMoves = moveGenerator.legalMoves(board, board.sideToMove());
        if (legalMoves.isEmpty()) {
            return attacks.isInCheck(board, board.sideToMove())
                    ? evaluator.checkmateScore(board.sideToMove(), ply) : 0;
        }
        boolean inCheck = attacks.isInCheck(board, board.sideToMove());
        int standPat = evaluator.evaluate(board);
        if (qply >= MAX_QUIESCENCE_PLY) {
            return standPat;
        }

        boolean maximizing = board.sideToMove() == Color.WHITE;
        if (!inCheck) {
            if (maximizing) {
                if (standPat >= beta) {
                    return standPat;
                }
                alpha = Math.max(alpha, standPat);
            } else {
                if (standPat <= alpha) {
                    return standPat;
                }
                beta = Math.min(beta, standPat);
            }
        }

        List<Move> forcingMoves = inCheck ? legalMoves : orderedMoves(board, legalMoves.stream()
                .filter(move -> board.pieceAt(move.to()) != null || move.enPassant() || move.promotion() != null)
                .toList());
        int bestScore = inCheck ? (maximizing ? -INFINITY : INFINITY) : standPat;
        for (Move move : forcingMoves) {
            int score = quiescence(board.apply(move), alpha, beta, ply + 1, qply + 1);
            if (maximizing) {
                bestScore = Math.max(bestScore, score);
                alpha = Math.max(alpha, bestScore);
            } else {
                bestScore = Math.min(bestScore, score);
                beta = Math.min(beta, bestScore);
            }
            if (alpha >= beta) {
                break;
            }
        }
        return bestScore;
    }

    private static List<Move> orderedMoves(Board board, List<Move> legalMoves) {
        List<Move> ordered = new ArrayList<>(legalMoves);
        ordered.sort(Comparator.comparingInt((Move move) -> moveOrderScore(board, move)).reversed());
        return ordered;
    }

    private static int moveOrderScore(Board board, Move move) {
        Piece moving = board.pieceAt(move.from());
        Piece captured = board.pieceAt(move.to());
        int score = 0;
        if (captured != null) {
            score += captured.type().value() * 10 - moving.type().value();
        } else if (move.enPassant()) {
            score += PieceType.PAWN.value() * 10 - moving.type().value();
        }
        if (move.promotion() != null) {
            score += 80 + move.promotion().value();
        }
        return score;
    }

    private static String positionKey(Board board) {
        StringBuilder key = new StringBuilder(80);
        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                Piece piece = board.pieceAt(new Square(file, rank));
                if (piece == null) {
                    key.append('.');
                } else {
                    char symbol = switch (piece.type()) {
                        case PAWN -> 'P';
                        case KNIGHT -> 'N';
                        case BISHOP -> 'B';
                        case ROOK -> 'R';
                        case QUEEN -> 'Q';
                        case KING -> 'K';
                    };
                    key.append(piece.color() == Color.WHITE
                            ? symbol : Character.toLowerCase(symbol));
                }
            }
        }
        key.append(board.sideToMove() == Color.WHITE ? 'w' : 'b');
        EnumSet<CastlingRight> rights = board.castlingRights().isEmpty()
                ? EnumSet.noneOf(CastlingRight.class) : EnumSet.copyOf(board.castlingRights());
        for (CastlingRight right : CastlingRight.values()) {
            key.append(rights.contains(right) ? '1' : '0');
        }
        key.append(board.enPassantTarget() == null ? "--" : board.enPassantTarget().toString());
        return key.toString();
    }

    private static int scoreForTable(int score, int ply) {
        if (score > 90_000) {
            return score + ply;
        }
        if (score < -90_000) {
            return score - ply;
        }
        return score;
    }

    private static int scoreFromTable(int score, int ply) {
        if (score > 90_000) {
            return score - ply;
        }
        if (score < -90_000) {
            return score + ply;
        }
        return score;
    }

    private enum Bound {
        EXACT,
        LOWER,
        UPPER
    }

    private record TranspositionEntry(int depth, int score, Bound bound, Move bestMove) {
    }
}
