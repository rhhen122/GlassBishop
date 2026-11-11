package org.GlassBishop.ui;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public final class EvaluationBar extends JComponent {
    private static final int DISPLAY_LIMIT = 1_200;
    private int evaluation;

    public EvaluationBar() {
        setPreferredSize(new Dimension(28, 560));
        setMinimumSize(new Dimension(24, 240));
        setToolTipText("Position evaluation from White's perspective");
    }

    public void setEvaluation(int evaluation) {
        this.evaluation = Math.max(-DISPLAY_LIMIT, Math.min(DISPLAY_LIMIT, evaluation));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        double whiteShare = (evaluation + DISPLAY_LIMIT) / (2.0 * DISPLAY_LIMIT);
        int whiteHeight = (int) Math.round(height * whiteShare);
        g.setColor(Color.BLACK);
        g.fillRoundRect(0, 0, width, height, 8, 8);
        g.setColor(Color.WHITE);
        g.fillRoundRect(0, height - whiteHeight, width, whiteHeight, 8, 8);
        g.setColor(new Color(70, 70, 70));
        g.drawRoundRect(0, 0, width - 1, height - 1, 8, 8);
        g.dispose();
    }
}
