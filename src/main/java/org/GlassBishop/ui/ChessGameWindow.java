package org.GlassBishop.ui;

import org.GlassBishop.engine.ChessGame;
import org.GlassBishop.engine.Difficulty;
import org.GlassBishop.engine.PositionEvaluator;
import org.GlassBishop.engine.StockfishEngine;
import org.GlassBishop.model.Board;
import org.GlassBishop.model.Color;
import org.GlassBishop.model.Move;
import org.GlassBishop.model.Piece;
import org.GlassBishop.model.PieceType;
import org.GlassBishop.model.Square;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public final class ChessGameWindow {
    private static final java.awt.Color LIGHT_SQUARE = new java.awt.Color(238, 238, 210);
    private static final java.awt.Color DARK_SQUARE = new java.awt.Color(118, 150, 86);
    private static final java.awt.Color SELECTED_SQUARE = new java.awt.Color(240, 200, 80);
    private static final java.awt.Color LEGAL_DESTINATION = new java.awt.Color(130, 180, 110);
    private static final java.awt.Color APP_BACKGROUND = new java.awt.Color(36, 41, 46);

    private final int searchDepth;
    private final ChessGame game;
    private final ChessPieceButton[][] squares = new ChessPieceButton[8][8];
    private final JLabel status = new JLabel("", SwingConstants.LEFT);
    private final JLabel evaluationLabel = new JLabel("", SwingConstants.CENTER);
    private final JTextArea moveLog = new JTextArea();
    private final EvaluationBar evaluationBar = new EvaluationBar();
    private final PositionEvaluator positionEvaluator = new PositionEvaluator();
    private final JFrame frame = new JFrame("Glass Bishop - Play");
    private final boolean selfPlay;
    private final StockfishEngine stockfish;
    private final Color humanColor;
    private final Timer selfPlayTimer = new Timer(700, event -> startBotTurn());
    private Square selectedSquare;
    private boolean engineThinking;
    private boolean handingOffToNewGame;

    public ChessGameWindow() {
        this(Difficulty.LEVEL_4.searchDepth(), false);
    }

    public ChessGameWindow(Difficulty difficulty) {
        this(difficulty.searchDepth(), false);
    }

    public ChessGameWindow(Difficulty difficulty, boolean selfPlay) {
        this(difficulty.searchDepth(), selfPlay);
    }

    public ChessGameWindow(int searchDepth, boolean selfPlay) {
        this(searchDepth, selfPlay, Color.WHITE, null);
    }

    public ChessGameWindow(int searchDepth, boolean selfPlay, Color humanColor) {
        this(searchDepth, selfPlay, humanColor, null);
    }

    public ChessGameWindow(int searchDepth, StockfishEngine stockfish) {
        this(searchDepth, false, Color.WHITE, stockfish);
    }

    private ChessGameWindow(int searchDepth, boolean selfPlay, Color humanColor,
                            StockfishEngine stockfish) {
        this.searchDepth = searchDepth;
        this.selfPlay = selfPlay;
        this.stockfish = stockfish;
        this.humanColor = humanColor;
        this.game = new ChessGame(searchDepth);
        selfPlayTimer.setRepeats(false);
        createWindow();
        refreshBoard();
        updateStatus();
    }

    public void show() {
        frame.setVisible(true);
        if (isAutomatedGame()) {
            selfPlayTimer.start();
        } else if (game.board().sideToMove() != humanColor) {
            startBotTurn();
        }
    }

    private void createWindow() {
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout(16, 16));
        frame.getContentPane().setBackground(APP_BACKGROUND);

        JLabel title = new JLabel("GLASS BISHOP", SwingConstants.LEFT);
        title.setForeground(java.awt.Color.WHITE);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        status.setForeground(new java.awt.Color(218, 223, 228));
        status.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        JPanel header = new JPanel(new BorderLayout(6, 6));
        header.setBackground(APP_BACKGROUND);
        header.setBorder(BorderFactory.createEmptyBorder(12, 16, 4, 16));
        header.add(title, BorderLayout.NORTH);
        header.add(status, BorderLayout.SOUTH);
        frame.add(header, BorderLayout.NORTH);

        JPanel board = new JPanel(new GridLayout(8, 8));
        board.setBorder(BorderFactory.createLineBorder(new java.awt.Color(20, 24, 28), 4));
        for (int row = 0; row < 8; row++) {
            int rank = 7 - row;
            for (int file = 0; file < 8; file++) {
                Square square = new Square(file, rank);
                ChessPieceButton button = new ChessPieceButton();
                button.addActionListener(event -> handleSquareClick(square));
                squares[file][rank] = button;
                board.add(button);
            }
        }
        JPanel boardContainer = new JPanel(new BorderLayout());
        boardContainer.setBackground(APP_BACKGROUND);
        boardContainer.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 8));
        boardContainer.add(board, BorderLayout.CENTER);
        frame.add(boardContainer, BorderLayout.CENTER);

        JLabel blackLabel = new JLabel("BLACK", SwingConstants.CENTER);
        JLabel whiteLabel = new JLabel("WHITE", SwingConstants.CENTER);
        blackLabel.setForeground(java.awt.Color.WHITE);
        whiteLabel.setForeground(java.awt.Color.WHITE);
        blackLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        whiteLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        evaluationLabel.setForeground(java.awt.Color.WHITE);
        evaluationLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        JPanel evaluationPanel = new JPanel(new BorderLayout(4, 8));
        evaluationPanel.setBackground(APP_BACKGROUND);
        evaluationPanel.setBorder(BorderFactory.createEmptyBorder(0, 4, 8, 16));
        evaluationPanel.add(blackLabel, BorderLayout.NORTH);
        evaluationPanel.add(evaluationBar, BorderLayout.CENTER);
        JPanel evaluationBottom = new JPanel(new BorderLayout(2, 4));
        evaluationBottom.setBackground(APP_BACKGROUND);
        evaluationBottom.add(whiteLabel, BorderLayout.NORTH);
        evaluationBottom.add(evaluationLabel, BorderLayout.SOUTH);
        evaluationPanel.add(evaluationBottom, BorderLayout.SOUTH);

        JLabel moveLogTitle = new JLabel("MOVE HISTORY");
        moveLogTitle.setForeground(java.awt.Color.WHITE);
        moveLogTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        moveLog.setEditable(false);
        moveLog.setFocusable(false);
        moveLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        moveLog.setForeground(new java.awt.Color(232, 236, 239));
        moveLog.setBackground(new java.awt.Color(47, 53, 59));
        moveLog.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JScrollPane historyScroll = new JScrollPane(moveLog);
        historyScroll.setPreferredSize(new Dimension(190, 190));
        historyScroll.setBorder(BorderFactory.createLineBorder(new java.awt.Color(70, 77, 84)));
        JPanel historyPanel = new JPanel(new BorderLayout(4, 6));
        historyPanel.setBackground(APP_BACKGROUND);
        historyPanel.add(moveLogTitle, BorderLayout.NORTH);
        historyPanel.add(historyScroll, BorderLayout.CENTER);

        JPanel sidePanel = new JPanel(new BorderLayout(8, 14));
        sidePanel.setBackground(APP_BACKGROUND);
        sidePanel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 16));
        sidePanel.add(evaluationPanel, BorderLayout.NORTH);
        sidePanel.add(historyPanel, BorderLayout.CENTER);
        frame.add(sidePanel, BorderLayout.EAST);

        JButton newGame = new JButton("New Game");
        newGame.addActionListener(event -> {
            if (engineThinking) {
                return;
            }
            handingOffToNewGame = stockfish != null;
            frame.dispose();
            if (stockfish == null) {
                new ChessGameWindow(searchDepth, selfPlay, humanColor).show();
            } else {
                new ChessGameWindow(searchDepth, stockfish).show();
            }
        });
        newGame.setFocusPainted(false);
        JPanel controls = new JPanel(new BorderLayout());
        controls.setBackground(APP_BACKGROUND);
        controls.setBorder(BorderFactory.createEmptyBorder(4, 16, 12, 16));
        controls.add(newGame);
        frame.add(controls, BorderLayout.SOUTH);

        frame.pack();
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent event) {
                frame.requestFocus();
            }

            @Override
            public void windowClosed(WindowEvent event) {
                selfPlayTimer.stop();
                if (stockfish != null && !handingOffToNewGame) {
                    stockfish.close();
                }
            }
        });
    }

    private void handleSquareClick(Square square) {
        if (isAutomatedGame() || engineThinking || game.board().sideToMove() != humanColor
                || game.legalMoves().isEmpty()) {
            return;
        }
        Piece clicked = game.board().pieceAt(square);
        if (selectedSquare == null) {
            selectHumanPiece(square, clicked);
            return;
        }
        if (selectedSquare.equals(square)) {
            selectedSquare = null;
            refreshBoard();
            updateStatus();
            return;
        }
        if (clicked != null && clicked.color() == humanColor) {
            selectHumanPiece(square, clicked);
            return;
        }

        List<Move> candidates = game.legalMoves().stream()
                .filter(move -> move.from().equals(selectedSquare) && move.to().equals(square))
                .toList();
        if (candidates.isEmpty()) {
            selectedSquare = null;
            refreshBoard();
            updateStatus();
            return;
        }
        Move move = choosePromotion(candidates);
        if (move == null) {
            selectedSquare = null;
            refreshBoard();
            updateStatus();
            return;
        }

        game.playHumanMove(move, humanColor);
        selectedSquare = null;
        refreshBoard();
        if (game.legalMoves().isEmpty()) {
            updateStatus();
            return;
        }
        startBotTurn();
    }

    private void selectHumanPiece(Square square, Piece piece) {
        if (piece == null || piece.color() != humanColor) {
            selectedSquare = null;
        } else {
            selectedSquare = square;
        }
        refreshBoard();
        updateStatus();
    }

    private Move choosePromotion(List<Move> candidates) {
        if (candidates.size() == 1) {
            return candidates.getFirst();
        }
        PieceType[] choices = {PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT};
        String[] symbols = humanColor == Color.WHITE
                ? new String[]{"♕", "♖", "♗", "♘"}
                : new String[]{"♛", "♜", "♝", "♞"};
        int selected = JOptionPane.showOptionDialog(
                frame,
                "Choose a promotion piece:",
                "Pawn promotion",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                symbols,
                symbols[0]);
        if (selected < 0) {
            return null;
        }
        PieceType selectedType = choices[selected];
        return candidates.stream().filter(move -> move.promotion() == selectedType).findFirst().orElseThrow();
    }

    private void startBotTurn() {
        engineThinking = true;
        status.setText(stockfish != null ? "Glass Bishop and Stockfish are playing..."
                : selfPlay ? "Self-play: bot is thinking..." : "Starting engine search...");
        refreshBoard();
        new SwingWorker<Move, Void>() {
            @Override
            protected Move doInBackground() throws Exception {
                if (stockfish != null && game.board().sideToMove() == Color.BLACK) {
                    String uciMove = stockfish.bestMove(game.uciMoveHistory(), searchDepth);
                    Move move = game.legalMoves().stream()
                            .filter(candidate -> candidate.toUci().equalsIgnoreCase(uciMove))
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException(
                                    "Stockfish returned an illegal move: " + uciMove));
                    return game.playExternalMove(move);
                }
                return game.playBotMove(message ->
                        {
                            System.out.println("[Engine] " + message);
                            SwingUtilities.invokeLater(() -> status.setText(message));
                        });
            }

            @Override
            protected void done() {
                engineThinking = false;
                try {
                    get();
                    refreshBoard();
                    updateStatus();
                    if (isAutomatedGame() && !game.legalMoves().isEmpty()) {
                        selfPlayTimer.start();
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    status.setText("Engine move was interrupted.");
                    refreshBoard();
                } catch (ExecutionException exception) {
                    status.setText("Engine error: " + exception.getCause().getMessage());
                    refreshBoard();
                }
            }
        }.execute();
    }

    private void refreshBoard() {
        Board position = game.board();
        List<Move> legalMoves = selectedSquare == null
                ? List.of()
                : game.legalMoves().stream()
                .filter(move -> move.from().equals(selectedSquare))
                .toList();
        for (int file = 0; file < 8; file++) {
            for (int rank = 0; rank < 8; rank++) {
                Square square = new Square(file, rank);
                ChessPieceButton button = squares[file][rank];
                Piece piece = position.pieceAt(square);
                button.setPieceSymbol(piece == null ? "" : pieceSymbol(piece),
                        piece != null && piece.color() == Color.WHITE);
                java.awt.Color background = (file + rank) % 2 == 0 ? LIGHT_SQUARE : DARK_SQUARE;
                if (selectedSquare != null && selectedSquare.equals(square)) {
                    background = SELECTED_SQUARE;
                } else if (legalMoves.stream().anyMatch(move -> move.to().equals(square))) {
                    background = LEGAL_DESTINATION;
                }
                button.setBackground(background);
                button.setEnabled(!isAutomatedGame());
            }
        }
        int evaluation = positionEvaluator.evaluate(position);
        evaluationBar.setEvaluation(evaluation);
        evaluationLabel.setText(formatEvaluation(evaluation));
        moveLog.setText(String.join("\n", game.moveHistory()));
        moveLog.setCaretPosition(moveLog.getDocument().getLength());
    }

    private static String formatEvaluation(int evaluation) {
        if (evaluation == 0) {
            return "0.0";
        }
        return (evaluation > 0 ? "+" : "-")
                + String.format(Locale.ROOT, "%.1f", Math.abs(evaluation) / 100.0);
    }

    private void updateStatus() {
        if (engineThinking) {
            status.setText(stockfish != null
                    ? (game.board().sideToMove() == Color.BLACK
                    ? "Stockfish is thinking..." : "Glass Bishop is thinking...")
                    : selfPlay ? "Self-play: bot is thinking..." : "Engine is thinking...");
            return;
        }
        if (game.legalMoves().isEmpty()) {
            if (game.isCheckmate()) {
                String winner;
                if (stockfish != null) {
                    winner = game.board().sideToMove() == Color.WHITE
                            ? "Stockfish wins" : "Glass Bishop wins";
                } else if (selfPlay) {
                    winner = game.board().sideToMove() == Color.WHITE
                            ? "Black wins" : "White wins";
                } else {
                    winner = game.board().sideToMove() == humanColor
                            ? "Engine wins" : "You win";
                }
                status.setText("Checkmate. " + winner + ".");
            } else {
                status.setText("Stalemate. The game is a draw.");
            }
            return;
        }
        if (stockfish != null) {
            status.setText("Glass Bishop vs Stockfish  •  " + game.board().sideToMove()
                    + " to move  •  depth " + searchDepth);
        } else if (selfPlay) {
            status.setText("Self-play  •  " + game.board().sideToMove() + " to move  •  depth " + searchDepth);
        } else {
            status.setText(game.board().sideToMove() == humanColor
                    ? "Your turn (" + humanColor + "). Select a piece, then a highlighted square.  •  depth "
                    + searchDepth
                    : "Engine's turn (" + game.board().sideToMove() + ").  •  depth " + searchDepth);
        }
    }

    private boolean isAutomatedGame() {
        return selfPlay || stockfish != null;
    }

    static String pieceSymbol(Piece piece) {
        return switch (piece.color()) {
            case WHITE -> switch (piece.type()) {
                case PAWN -> "♙";
                case KNIGHT -> "♘";
                case BISHOP -> "♗";
                case ROOK -> "♖";
                case QUEEN -> "♕";
                case KING -> "♔";
            };
            case BLACK -> switch (piece.type()) {
                case PAWN -> "♟";
                case KNIGHT -> "♞";
                case BISHOP -> "♝";
                case ROOK -> "♜";
                case QUEEN -> "♛";
                case KING -> "♚";
            };
        };
    }
}
