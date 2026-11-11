package org.GlassBishop;

import org.GlassBishop.engine.MoveRecommendation;
import org.GlassBishop.engine.MoveGenerator;
import org.GlassBishop.engine.AttackDetector;
import org.GlassBishop.engine.PositionAnalyzer;
import org.GlassBishop.engine.Difficulty;
import org.GlassBishop.engine.SmartMoveSelector;
import org.GlassBishop.io.PositionJsonReader;
import org.GlassBishop.model.Board;
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
        if (args[0].equalsIgnoreCase("play") || args[0].equalsIgnoreCase("selfplay")) {
            Difficulty difficulty = parseDepth(args, 1);
            if (GraphicsEnvironment.isHeadless()) {
                throw new IllegalStateException("Cannot start the Swing game in a headless environment");
            }
            boolean selfPlay = args[0].equalsIgnoreCase("selfplay");
            SwingUtilities.invokeLater(() -> new ChessGameWindow(difficulty, selfPlay).show());
            return;
        }
        PositionArguments positionArguments;
        try {
            positionArguments = parsePositionArguments(args);
        } catch (IllegalArgumentException exception) {
            System.err.println(exception.getMessage());
            return;
        }
        System.out.println("Loading position from " + positionArguments.path() + "...");
        Board board = new PositionJsonReader().read(positionArguments.path());
        System.out.println("Analyzing White's material, attacked pieces, and mate-in-one...");
        ScoreResult result = new StupidityScorer().score(board);
        System.out.println("Current position evaluated. Searching for the strongest legal move to depth "
                + positionArguments.depth().searchDepth() + "...");
        var bestMove = new SmartMoveSelector(positionArguments.depth()).bestMove(board, System.out::println);
        int legalMoveCount = new MoveGenerator(new AttackDetector())
                .legalMoves(board, board.sideToMove()).size();
        var recommendation = bestMove.map(move -> new MoveRecommendation(move,
                new StupidityScorer().score(board.apply(move)), legalMoveCount));

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
        System.out.println("  java -jar target/GlassBishop-1.1.0.jar <position.json> [-depth=1..5]");
        System.out.println("  java -jar target/GlassBishop-1.1.0.jar play [-depth=1..5]");
        System.out.println("  java -jar target/GlassBishop-1.1.0.jar selfplay [-depth=1..5]");
        System.out.println("  java -jar target/GlassBishop-1.1.0.jar help");
        System.out.println();
        System.out.println("Search depth defaults to 4. Use -depth=1 through -depth=5; deeper searches take longer.");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar target/GlassBishop-1.1.0.jar examples/starting-position.json");
        System.out.println("  java -jar target/GlassBishop-1.1.0.jar examples/white-mate-in-one.json");
        System.out.println();
        System.out.println("Uses the play-mode search to recommend a move; reports the Stupidity Score separately.");
        System.out.println("See README.md for JSON format, scoring details, and chess rules.");
    }

    private static Difficulty parseDepth(String[] args, int startIndex) {
        if (args.length == startIndex) {
            return Difficulty.LEVEL_4;
        }
        String value = depthValue(args, startIndex);
        return Difficulty.parse(value);
    }

    private static PositionArguments parsePositionArguments(String[] args) {
        Path path = null;
        Difficulty depth = Difficulty.LEVEL_4;
        boolean depthSpecified = false;
        for (int index = 0; index < args.length; index++) {
            String argument = args[index];
            if (argument.startsWith("-depth=") || argument.startsWith("--depth=")) {
                if (depthSpecified) {
                    throw new IllegalArgumentException("Specify the search depth only once.");
                }
                depth = Difficulty.parse(argument.substring(argument.indexOf('=') + 1));
                depthSpecified = true;
            } else if (argument.equals("-depth") || argument.equals("--depth")) {
                if (depthSpecified || index + 1 >= args.length) {
                    throw new IllegalArgumentException("Usage: <position.json> [-depth=1..5]");
                }
                depth = Difficulty.parse(args[++index]);
                depthSpecified = true;
            } else if (argument.startsWith("-")) {
                throw new IllegalArgumentException("Unknown option: " + argument
                        + ". Use -depth=1..5.");
            } else if (path == null) {
                path = Path.of(argument);
            } else {
                throw new IllegalArgumentException("Expected one position JSON file and an optional -depth=1..5.");
            }
        }
        if (path == null) {
            throw new IllegalArgumentException(
                    "Expected one position JSON file, 'play', or 'selfplay'. Run with 'help' for usage.");
        }
        return new PositionArguments(path, depth);
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
                "Usage: java -jar target/GlassBishop-1.1.0.jar play|selfplay [-depth=1..5]");
    }

    private record PositionArguments(Path path, Difficulty depth) {
    }
}