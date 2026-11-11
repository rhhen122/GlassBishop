package org.GlassBishop.ui;

import org.GlassBishop.engine.ChessGame;
import org.GlassBishop.engine.PositionEvaluator;
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
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public final class ChessGameWindow {
    private static final java.awt.Color LIGHT_SQUARE = new java.awt.Color(238, 238, 210);
    private static final java.awt.Color DARK_SQUARE = new java.awt.Color(118, 150, 86);
    private static final java.awt.Color SELECTED_SQUARE = new java.awt.Color(240, 200, 80);
    private static final java.awt.Color LEGAL_DESTINATION = new java.awt.Color(130, 180, 110);
    private static final java.awt.Color APP_BACKGROUND = new java.awt.Color(36, 41, 46);

    private final ChessGame game = new ChessGame();
    private final JButton[][] squares = new JButton[8][8];
    private final JLabel status = new JLabel("", SwingConstants.LEFT);
    private final JLabel evaluationLabel = new JLabel("", SwingConstants.CENTER);
    private final EvaluationBar evaluationBar = new EvaluationBar();
    private final PositionEvaluator positionEvaluator = new PositionEvaluator();
    private final JFrame frame = new JFrame("Glass Bishop - Play");
    private Square selectedSquare;
    private boolean engineThinking;

    public ChessGameWindow() {
        createWindow();
        refreshBoard();
        updateStatus();
    }

    public void show() {
        frame.setVisible(true);
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
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(76, 76));
                button.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 48));
                button.setFocusPainted(false);
                button.setOpaque(true);
                button.setBorderPainted(false);
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
        evaluationPanel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 16));
        evaluationPanel.add(blackLabel, BorderLayout.NORTH);
        evaluationPanel.add(evaluationBar, BorderLayout.CENTER);
        JPanel evaluationBottom = new JPanel(new BorderLayout(2, 4));
        evaluationBottom.setBackground(APP_BACKGROUND);
        evaluationBottom.add(whiteLabel, BorderLayout.NORTH);
        evaluationBottom.add(evaluationLabel, BorderLayout.SOUTH);
        evaluationPanel.add(evaluationBottom, BorderLayout.SOUTH);
        frame.add(evaluationPanel, BorderLayout.EAST);

        JButton newGame = new JButton("New Game");
        newGame.addActionListener(event -> {
            if (engineThinking) {
                return;
            }
            frame.dispose();
            new ChessGameWindow().show();
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
        });
    }

    private void handleSquareClick(Square square) {
        if (engineThinking || game.board().sideToMove() != Color.WHITE
                || game.legalMoves().isEmpty()) {
            return;
        }
        Piece clicked = game.board().pieceAt(square);
        if (selectedSquare == null) {
            selectWhitePiece(square, clicked);
            return;
        }
        if (selectedSquare.equals(square)) {
            selectedSquare = null;
            refreshBoard();
            updateStatus();
            return;
        }
        if (clicked != null && clicked.color() == Color.WHITE) {
            selectWhitePiece(square, clicked);
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

        game.playHumanMove(move);
        selectedSquare = null;
        refreshBoard();
        if (game.legalMoves().isEmpty()) {
            updateStatus();
            return;
        }
        startEngineTurn();
    }

    private void selectWhitePiece(Square square, Piece piece) {
        if (piece == null || piece.color() != Color.WHITE) {
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
        String[] symbols = {"♕", "♖", "♗", "♘"};
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

    private void startEngineTurn() {
        engineThinking = true;
        updateStatus();
        refreshBoard();
        new SwingWorker<Move, Void>() {
            @Override
            protected Move doInBackground() {
                return game.playEngineMove();
            }

            @Override
            protected void done() {
                engineThinking = false;
                try {
                    get();
                    refreshBoard();
                    updateStatus();
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
                JButton button = squares[file][rank];
                Piece piece = position.pieceAt(square);
                button.setText(piece == null ? "" : pieceSymbol(piece));
                button.setForeground(piece != null && piece.color() == Color.WHITE
                        ? new java.awt.Color(35, 38, 42) : java.awt.Color.WHITE);
                java.awt.Color background = (file + rank) % 2 == 0 ? LIGHT_SQUARE : DARK_SQUARE;
                if (selectedSquare != null && selectedSquare.equals(square)) {
                    background = SELECTED_SQUARE;
                } else if (legalMoves.stream().anyMatch(move -> move.to().equals(square))) {
                    background = LEGAL_DESTINATION;
                }
                button.setBackground(background);
                button.setEnabled(true);
            }
        }
        int evaluation = positionEvaluator.evaluate(position);
        evaluationBar.setEvaluation(evaluation);
        evaluationLabel.setText(formatEvaluation(evaluation));
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
            status.setText("Engine is thinking...");
            return;
        }
        if (game.legalMoves().isEmpty()) {
            if (game.isCheckmate()) {
                String winner = game.board().sideToMove() == Color.WHITE
                        ? "Engine wins" : "You win";
                status.setText("Checkmate. " + winner + ".");
            } else {
                status.setText("Stalemate. The game is a draw.");
            }
            return;
        }
        status.setText(game.board().sideToMove() == Color.WHITE
                ? "Your turn (White). Select a piece, then a highlighted square."
                : "Engine's turn (Black).");
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
