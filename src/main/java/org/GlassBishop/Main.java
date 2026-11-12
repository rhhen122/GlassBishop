package org.GlassBishop;

import org.GlassBishop.engine.MoveRecommendation;
import org.GlassBishop.engine.MoveGenerator;
import org.GlassBishop.engine.AttackDetector;
import org.GlassBishop.engine.MoveSequenceConverter;
import org.GlassBishop.engine.PositionAnalyzer;
import org.GlassBishop.engine.SmartMoveSelector;
import org.GlassBishop.engine.StockfishEngine;
import org.GlassBishop.io.PositionJsonReader;
import org.GlassBishop.io.PositionJsonWriter;
import org.GlassBishop.model.Board;
import org.GlassBishop.model.Color;
import org.GlassBishop.scoring.ScoreResult;
import org.GlassBishop.scoring.StupidityScorer;
import org.GlassBishop.ui.ChessGameWindow;
import org.GlassBishop.utility.BoardPrinter;
import org.GlassBishop.utility.PrintASCII;

import java.io.IOException;
import java.awt.GraphicsEnvironment;
import java.nio.file.Path;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("help"))) {
            printHelp();
            return;
        }
        if (args[0].equalsIgnoreCase("convert")) {
            convertMoves(args);
            return;
        }
        if (args[0].equalsIgnoreCase("stockfish")) {
            playStockfish(args);
            return;
        }
        if (args[0].equalsIgnoreCase("play") || args[0].equalsIgnoreCase("selfplay")) {
            boolean selfPlay = args[0].equalsIgnoreCase("selfplay");
            PlayArguments playArguments;
            try {
                playArguments = parsePlayArguments(args, !selfPlay);
            } catch (IllegalArgumentException exception) {
                System.err.println(exception.getMessage());
                return;
            }
            if (GraphicsEnvironment.isHeadless()) {
                throw new IllegalStateException("Cannot start the Swing game in a headless environment");
            }
            SwingUtilities.invokeLater(() -> new ChessGameWindow(
                    playArguments.depth(), selfPlay, playArguments.humanColor()).show());
            return;
        }
        PositionArguments positionArguments;
        try {
            positionArguments = parsePositionArguments(args);
        } catch (IllegalArgumentException exception) {
            System.err.println(exception.getMessage());
            return;
        }
        if (!positionArguments.simple()) {
            System.out.println("Loading position from " + positionArguments.path() + "...");
        }
        Board board = new PositionJsonReader().read(positionArguments.path());
        if (positionArguments.color() != null) {
            board = board.forSideToMove(positionArguments.color());
        }
        Board analyzedBoard = board;
        var bestMove = new SmartMoveSelector(positionArguments.depth()).bestMove(
                board, positionArguments.simple() ? message -> { } : System.out::println);
        if (positionArguments.simple()) {
            bestMove.ifPresentOrElse(move -> System.out.println(move.toUci()),
                    () -> System.err.println("No legal move available."));
            return;
        }
        System.out.println("Analyzing White's material, attacked pieces, and mate-in-one...");
        ScoreResult result = new StupidityScorer().score(board);
        System.out.println("Current position evaluated. Searching for the strongest legal move to depth "
                + positionArguments.depth() + "...");
        int legalMoveCount = new MoveGenerator(new AttackDetector())
                .legalMoves(board, board.sideToMove()).size();
        var recommendation = bestMove.map(move -> new MoveRecommendation(move,
                new StupidityScorer().score(analyzedBoard.apply(move)), legalMoveCount));

        System.out.println(new PrintASCII().printSmallerASCII());
        System.out.println("Side to move: " + board.sideToMove());
        System.out.println();
        System.out.println(new BoardPrinter().print(board));
        System.out.println();
        System.out.println("White material: " + result.whiteMaterial());
        System.out.println("Attacked White material: " + result.attackedWhiteMaterial());
        System.out.println("White can checkmate in 1: " + result.whiteCanMateInOne());
        System.out.println("Stupidity Score: " + result.score());
        System.out.println("Stupidity Rating: " + result.rating());
        System.out.println();
        if (recommendation.isPresent()) {
            MoveRecommendation choice = recommendation.orElseThrow();
            System.out.println("Search complete.");
            System.out.println("Legal moves evaluated: " + choice.legalMovesEvaluated());
            System.out.println("Recommended move: " + choice.move().toUci());
            System.out.println("Resulting Stupidity Score: " + choice.resultingPosition().score());
            System.out.println("Resulting Stupidity Rating: " + choice.resultingPosition().rating());
            System.out.println();
            System.out.println("Board after recommended move ("
                    + board.sideToMove().opposite() + " to move):");
            System.out.println(new BoardPrinter().print(board.apply(choice.move())));
        } else if (new PositionAnalyzer().isCheckmate(board, board.sideToMove())) {
            System.out.println("No legal moves: " + board.sideToMove() + " is checkmated.");
        } else {
            System.out.println("No legal moves: the position is stalemate.");
        }
    }

    private static void printHelp() {
        System.out.println(PrintASCII.printSmallerASCII());
        System.out.println("Glass Bishop - Chess Position Evaluator");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  java -jar target/GlassBishop-X.X.X.jar <position.json> [-depth=<positive integer>] [-color=white|black] [-simple]");
        System.out.println("  java -jar target/GlassBishop-X.X.X.jar play [-depth=<positive integer>] [-color=white|black]");
        System.out.println("  java -jar target/GlassBishop-X.X.X.jar selfplay [-depth=<positive integer>]");
        System.out.println("  java -jar target/GlassBishop-X.X.X.jar convert <output.json> [uci-move ...]");
        System.out.println("  java -jar target/GlassBishop-X.X.X.jar stockfish <binary> [-depth=<positive integer>]");
        System.out.println("  java -jar target/GlassBishop-X.X.X.jar help");
        System.out.println();
        System.out.println("Search depth defaults to 4. Any positive integer is accepted; deeper searches take longer.");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar target/GlassBishop-X.X.X.jar examples/starting-position.json");
        System.out.println("  java -jar target/GlassBishop-X.X.X.jar examples/white-mate-in-one.json");
        System.out.println("  java -jar target/GlassBishop-X.X.X.jar play -color=black");
        System.out.println("  java -jar target/GlassBishop-X.X.X.jar examples/starting-position.json -simple");
        System.out.println("  java -jar target/GlassBishop-X.X.X.jar convert game.json e2e4 e7e5 g1f3");
        System.out.println("  java -jar target/GlassBishop-X.X.X.jar stockfish /path/to/stockfish -depth=3");
        System.out.println();
        System.out.println("Uses the play-mode search to recommend a move; reports the Stupidity Score separately.");
        System.out.println("See README.md for JSON format, scoring details, and chess rules.");
    }

    private static void convertMoves(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: convert <output.json> [uci-move ...]");
            return;
        }
        try {
            Board board = new MoveSequenceConverter().convert(
                    java.util.Arrays.asList(args).subList(2, args.length));
            new PositionJsonWriter().write(board, Path.of(args[1]));
            System.out.println("Wrote position after " + (args.length - 2)
                    + " move(s) to " + args[1]);
        } catch (IllegalArgumentException exception) {
            System.err.println(exception.getMessage());
        }
    }

    private static void playStockfish(String[] args) throws IOException {
        Path binary;
        int searchDepth;
        try {
            if (args.length < 2 || args.length > 3) {
                throw new IllegalArgumentException(
                        "Usage: stockfish <binary> [-depth=<positive integer>]");
            }
            binary = Path.of(args[1]);
            searchDepth = parseDepth(args, 2);
        } catch (IllegalArgumentException exception) {
            System.err.println(exception.getMessage());
            return;
        }
        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException("Cannot start the Swing game in a headless environment");
        }
        StockfishEngine stockfish = new StockfishEngine(binary);
        SwingUtilities.invokeLater(() ->
                new ChessGameWindow(searchDepth, stockfish).show());
    }

    private static int parseDepth(String[] args, int startIndex) {
        if (args.length == startIndex) {
            return 4;
        }
        String value = depthValue(args, startIndex);
        return parsePositiveDepth(value);
    }

    private static PlayArguments parsePlayArguments(String[] args, boolean allowColor) {
        int depth = 4;
        boolean depthSpecified = false;
        Color humanColor = Color.WHITE;
        boolean colorSpecified = false;
        for (int index = 1; index < args.length; index++) {
            String argument = args[index];
            if (argument.startsWith("-depth=") || argument.startsWith("--depth=")) {
                if (depthSpecified) {
                    throw new IllegalArgumentException("Specify the search depth only once.");
                }
                depth = parsePositiveDepth(argument.substring(argument.indexOf('=') + 1));
                depthSpecified = true;
            } else if (argument.equals("-depth") || argument.equals("--depth")) {
                if (depthSpecified || index + 1 >= args.length) {
                    throw new IllegalArgumentException(playUsage());
                }
                depth = parsePositiveDepth(args[++index]);
                depthSpecified = true;
            } else if (argument.startsWith("-color=") || argument.startsWith("--color=")) {
                if (!allowColor) {
                    throw new IllegalArgumentException("The -color option is only available in play mode.");
                }
                if (colorSpecified) {
                    throw new IllegalArgumentException("Specify the player color only once.");
                }
                humanColor = parsePlayerColor(argument.substring(argument.indexOf('=') + 1));
                colorSpecified = true;
            } else if (argument.equals("-color") || argument.equals("--color")) {
                if (!allowColor) {
                    throw new IllegalArgumentException("The -color option is only available in play mode.");
                }
                if (colorSpecified || index + 1 >= args.length) {
                    throw new IllegalArgumentException(playUsage());
                }
                humanColor = parsePlayerColor(args[++index]);
                colorSpecified = true;
            } else {
                throw new IllegalArgumentException("Unknown play option: " + argument
                        + ". " + playUsage());
            }
        }
        return new PlayArguments(depth, humanColor);
    }

    private static Color parsePlayerColor(String value) {
        return switch (value.toLowerCase(java.util.Locale.ROOT)) {
            case "white" -> Color.WHITE;
            case "black" -> Color.BLACK;
            default -> throw new IllegalArgumentException(
                    "Invalid player color '" + value + "'. Choose white or black.");
        };
    }

    private static String playUsage() {
        return "Usage: play [-depth=<positive integer>] [-color=white|black]";
    }

    private static PositionArguments parsePositionArguments(String[] args) {
        Path path = null;
        int depth = 4;
        boolean depthSpecified = false;
        boolean simple = false;
        boolean simpleSpecified = false;
        Color color = null;
        boolean colorSpecified = false;
        for (int index = 0; index < args.length; index++) {
            String argument = args[index];
            if (argument.equals("-simple") || argument.equals("--simple")) {
                if (simpleSpecified) {
                    throw new IllegalArgumentException("Specify simple mode only once.");
                }
                simple = true;
                simpleSpecified = true;
            } else if (argument.startsWith("-color=") || argument.startsWith("--color=")) {
                if (colorSpecified) {
                    throw new IllegalArgumentException("Specify the analysis color only once.");
                }
                color = parsePlayerColor(argument.substring(argument.indexOf('=') + 1));
                colorSpecified = true;
            } else if (argument.equals("-color") || argument.equals("--color")) {
                if (colorSpecified || index + 1 >= args.length) {
                    throw new IllegalArgumentException(
                            "Usage: <position.json> [-depth=<positive integer>] [-color=white|black] [-simple]");
                }
                color = parsePlayerColor(args[++index]);
                colorSpecified = true;
            } else if (argument.startsWith("-depth=") || argument.startsWith("--depth=")) {
                if (depthSpecified) {
                    throw new IllegalArgumentException("Specify the search depth only once.");
                }
                depth = parsePositiveDepth(argument.substring(argument.indexOf('=') + 1));
                depthSpecified = true;
            } else if (argument.equals("-depth") || argument.equals("--depth")) {
                if (depthSpecified || index + 1 >= args.length) {
                    throw new IllegalArgumentException(
                            "Usage: <position.json> [-depth=<positive integer>]");
                }
                depth = parsePositiveDepth(args[++index]);
                depthSpecified = true;
            } else if (argument.startsWith("-")) {
                throw new IllegalArgumentException("Unknown option: " + argument
                        + ". Use -depth=<positive integer>.");
            } else if (path == null) {
                path = Path.of(argument);
            } else {
                throw new IllegalArgumentException(
                        "Expected one position JSON file and an optional -depth=<positive integer>.");
            }
        }
        if (path == null) {
            throw new IllegalArgumentException(
                    "Expected one position JSON file, 'play', or 'selfplay'. Run with 'help' for usage.");
        }
        return new PositionArguments(path, depth, simple, color);
    }

    private static String depthValue(String[] args, int startIndex) {
        if (args.length == startIndex + 1) {
            String argument = args[startIndex];
            if (argument.startsWith("-depth=") || argument.startsWith("--depth=")) {
                return argument.substring(argument.indexOf('=') + 1);
            }
        } else if (args.length == startIndex + 2
                && (args[startIndex].equals("-depth") || args[startIndex].equals("--depth"))) {
            return args[startIndex + 1];
        }
        throw new IllegalArgumentException(
                "Usage: stockfish <binary> [-depth=<positive integer>]");
    }

    private static int parsePositiveDepth(String value) {
        try {
            int depth = Integer.parseInt(value);
            if (depth < 1) {
                throw new IllegalArgumentException("Search depth must be a positive integer.");
            }
            return depth;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Invalid search depth '" + value + "'. Use a positive integer.", exception);
        }
    }

    private record PositionArguments(Path path, int depth, boolean simple, Color color) {
    }

    private record PlayArguments(int depth, Color humanColor) {
    }
}