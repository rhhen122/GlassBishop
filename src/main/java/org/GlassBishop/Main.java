package org.GlassBishop;

import org.GlassBishop.engine.MoveRecommendation;
import org.GlassBishop.engine.MoveSelector;
import org.GlassBishop.engine.PositionAnalyzer;
import org.GlassBishop.io.PositionJsonReader;
import org.GlassBishop.model.Board;
import org.GlassBishop.scoring.ScoreResult;
import org.GlassBishop.scoring.StupidityScorer;
import org.GlassBishop.utility.BoardPrinter;
import org.GlassBishop.utility.PrintASCII;

import java.io.IOException;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("help"))) {
            printHelp();
            return;
        }
        if (args.length != 1) {
            System.err.println("Expected one position JSON file. Run with 'help' for usage.");
            return;
        }
        Board board = new PositionJsonReader().read(Path.of(args[0]));
        ScoreResult result = new StupidityScorer().score(board);
        var recommendation = new MoveSelector().leastStupidMove(board);

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
            System.out.println("Legal moves evaluated: " + choice.legalMovesEvaluated());
            System.out.println("Least stupid move: " + choice.move().toUci());
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
        System.out.println("  java -jar target/GlassBishop-0.1.jar <position.json>");
        System.out.println("  java -jar target/GlassBishop-0.1.jar help");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar target/GlassBishop-0.1.jar examples/starting-position.json");
        System.out.println("  java -jar target/GlassBishop-0.1.jar examples/white-mate-in-one.json");
        System.out.println();
        System.out.println("Evaluates every legal move and recommends the lowest-scoring resulting position.");
        System.out.println("See README.md for JSON format, scoring details, and chess rules.");
    }
}