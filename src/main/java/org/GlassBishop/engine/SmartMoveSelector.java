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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public final class SmartMoveSelector {
    private static final int INFINITY = 1_000_000;
    private static final int MAX_QUIESCENCE_PLY = 5;

    private final int searchDepth;
    private final AttackDetector attacks = new AttackDetector();
    private final MoveGenerator moveGenerator = new MoveGenerator(attacks);
    private final PositionEvaluator evaluator = new PositionEvaluator();
    private final Map<String, TranspositionEntry> transpositions = new ConcurrentHashMap<>();
    private final int parallelism;

    public SmartMoveSelector(Difficulty difficulty) {
        this(difficulty.searchDepth());
    }

    public SmartMoveSelector(int searchDepth) {
        this(searchDepth, Math.max(1, Runtime.getRuntime().availableProcessors()));
    }

    SmartMoveSelector(int searchDepth, int parallelism) {
        if (searchDepth < 1) {
            throw new IllegalArgumentException("Search depth must be a positive integer.");
        }
        if (parallelism < 1) {
            throw new IllegalArgumentException("Search parallelism must be a positive integer.");
        }
        this.searchDepth = searchDepth;
        this.parallelism = parallelism;
    }

    public Optional<Move> bestMove(Board board) {
        return bestMove(board, message -> { });
    }

    public Optional<Move> bestMove(Board board, Consumer<String> progress) {
        transpositions.clear();
        Consumer<String> safeProgress = message -> {
            synchronized (progress) {
                progress.accept(message);
            }
        };
        ExecutorService workers = parallelism > 1
                ? Executors.newFixedThreadPool(parallelism) : null;
        try {
            safeProgress.accept("Finding legal moves...");
            List<Move> legalMoves = orderedMoves(board, moveGenerator.legalMoves(board, board.sideToMove()));
            if (legalMoves.isEmpty()) {
                safeProgress.accept("No legal moves found.");
                return Optional.empty();
            }
            safeProgress.accept("Considering " + legalMoves.size() + " legal moves...");
            Move bestMove = legalMoves.getFirst();
            for (int depth = 1; ; depth++) {
                safeProgress.accept("Searching depth " + depth + "/" + searchDepth + "...");
                List<Move> iterationMoves = orderedMoves(board,
                        moveGenerator.legalMoves(board, board.sideToMove()));
                Move preferredMove = bestMove;
                iterationMoves.sort(Comparator.comparing((Move move) -> !move.equals(preferredMove)));
                bestMove = searchRoot(board, iterationMoves, depth, workers, safeProgress);
                if (depth == searchDepth) {
                    break;
                }
            }
            safeProgress.accept("Search complete. Best move: " + bestMove.toUci());
            return Optional.of(bestMove);
        } finally {
            if (workers != null) {
                workers.shutdownNow();
            }
        }
    }

    private Move searchRoot(Board board, List<Move> moves, int depth,
                            ExecutorService workers, Consumer<String> progress) {
        boolean maximizing = board.sideToMove() == Color.WHITE;
        if (workers != null && moves.size() > 1) {
            return searchRootParallel(board, moves, depth, workers, progress);
        }
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

    private Move searchRootParallel(Board board, List<Move> moves, int depth,
                                    ExecutorService workers, Consumer<String> progress) {
        int workerCount = Math.min(parallelism, moves.size());
        progress.accept("Searching " + moves.size() + " root moves in parallel with "
                + workerCount + " workers...");
        List<List<IndexedMove>> assignments = new ArrayList<>();
        for (int worker = 0; worker < workerCount; worker++) {
            assignments.add(new ArrayList<>());
        }
        for (int index = 0; index < moves.size(); index++) {
            assignments.get(index % workerCount).add(new IndexedMove(index, moves.get(index)));
        }
        List<Callable<List<RootResult>>> tasks = new ArrayList<>();
        for (int workerIndex = 0; workerIndex < workerCount; workerIndex++) {
            int workerNumber = workerIndex + 1;
            List<IndexedMove> assignment = assignments.get(workerIndex);
            tasks.add(() -> {
                List<RootResult> workerResults = new ArrayList<>();
                WorkerProgress workerProgress = new WorkerProgress(workerNumber, workerCount, progress);
                progress.accept("Worker " + workerNumber + "/" + workerCount + " assigned "
                        + assignment.size() + " root move(s).");
                for (IndexedMove indexedMove : assignment) {
                    Move move = indexedMove.move();
                    workerProgress.startRoot(move, depth);
                    int score = search(board.apply(move), depth - 1,
                            -INFINITY, INFINITY, 1, workerProgress);
                    progress.accept("Worker " + workerNumber + "/" + workerCount
                            + " finished root " + move.toUci() + " with score " + score + ".");
                    workerResults.add(new RootResult(indexedMove.index(), move, score));
                }
                return workerResults;
            });
        }
        List<Future<List<RootResult>>> results;
        try {
            results = workers.invokeAll(tasks);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Parallel search was interrupted.", exception);
        }

        boolean maximizing = board.sideToMove() == Color.WHITE;
        Move bestMove = moves.getFirst();
        int bestScore = maximizing ? -INFINITY : INFINITY;
        List<RootResult> scoredMoves = new ArrayList<>(moves.size());
        for (Future<List<RootResult>> workerResults : results) {
            try {
                scoredMoves.addAll(workerResults.get());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Parallel search was interrupted.", exception);
            } catch (ExecutionException exception) {
                throw new IllegalStateException("A parallel search worker failed.",
                        exception.getCause());
            }
        }
        scoredMoves.sort(Comparator.comparingInt(RootResult::index));
        for (RootResult rootResult : scoredMoves) {
            if (maximizing ? rootResult.score() > bestScore : rootResult.score() < bestScore) {
                bestScore = rootResult.score();
                bestMove = rootResult.move();
            }
        }
        return bestMove;
    }

    private int search(Board board, int depth, int alpha, int beta, int ply) {
        return search(board, depth, alpha, beta, ply, null);
    }

    private int search(Board board, int depth, int alpha, int beta, int ply,
                       WorkerProgress workerProgress) {
        if (workerProgress != null) {
            workerProgress.visit(depth, ply, "search");
        }
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
            return quiescence(board, alpha, beta, ply, 0, workerProgress);
        }

        boolean maximizing = board.sideToMove() == Color.WHITE;
        int bestScore = maximizing ? -INFINITY : INFINITY;
        Move bestMove = null;
        List<Move> ordered = orderedMoves(board, legalMoves);
        if (cached != null && cached.bestMove() != null) {
            ordered.sort(Comparator.comparing((Move move) -> !move.equals(cached.bestMove())));
        }
        for (Move move : ordered) {
            if (workerProgress != null) {
                workerProgress.exploring(move);
            }
            int score = search(board.apply(move), depth - 1, alpha, beta, ply + 1,
                    workerProgress);
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
        return quiescence(board, alpha, beta, ply, qply, null);
    }

    private int quiescence(Board board, int alpha, int beta, int ply, int qply,
                           WorkerProgress workerProgress) {
        if (workerProgress != null) {
            workerProgress.visit(0, ply, "quiescence");
        }
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
            if (workerProgress != null) {
                workerProgress.exploring(move);
            }
            int score = quiescence(board.apply(move), alpha, beta, ply + 1, qply + 1,
                    workerProgress);
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

    private record IndexedMove(int index, Move move) {
    }

    private record RootResult(int index, Move move, int score) {
    }

    private static final class WorkerProgress {
        private static final int REPORT_INTERVAL = 1_000;

        private final int workerNumber;
        private final int workerCount;
        private final Consumer<String> progress;
        private int nodes;
        private int nextReport = REPORT_INTERVAL;
        private int rootDepth;
        private Move rootMove;
        private String currentMove = "starting";

        private WorkerProgress(int workerNumber, int workerCount, Consumer<String> progress) {
            this.workerNumber = workerNumber;
            this.workerCount = workerCount;
            this.progress = progress;
        }

        private void startRoot(Move move, int depth) {
            rootMove = move;
            rootDepth = depth;
            nodes = 0;
            nextReport = REPORT_INTERVAL;
            currentMove = "starting";
            progress.accept(label() + " searching root " + rootMove.toUci()
                    + " to depth " + rootDepth + ".");
        }

        private void visit(int remainingDepth, int ply, String phase) {
            nodes++;
            if (nodes >= nextReport) {
                progress.accept(label() + " root " + rootMove.toUci()
                        + ": depth " + Math.max(0, rootDepth - remainingDepth)
                        + ", ply " + ply + ", " + nodes + " nodes in " + phase
                        + "; exploring " + currentMove + ".");
                nextReport += REPORT_INTERVAL;
            }
        }

        private void exploring(Move move) {
            currentMove = move.toUci();
        }

        private String label() {
            return "Worker " + workerNumber + "/" + workerCount;
        }
    }
}
