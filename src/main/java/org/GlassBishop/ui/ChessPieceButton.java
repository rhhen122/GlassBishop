package org.GlassBishop.ui;

import javax.swing.JButton;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

final class ChessPieceButton extends JButton {
    private static final Color WHITE_PIECE = new Color(255, 253, 235);
    private static final Color WHITE_OUTLINE = new Color(38, 42, 34);
    private static final Color BLACK_PIECE = new Color(25, 29, 25);
    private static final Color BLACK_OUTLINE = new Color(255, 255, 239);

    private String symbol = "";
    private boolean whitePiece;

    ChessPieceButton() {
        setPreferredSize(new Dimension(76, 76));
        setFont(new Font(Font.SERIF, Font.PLAIN, 52));
        setFocusPainted(false);
        setOpaque(true);
        setBorderPainted(false);
        setContentAreaFilled(true);
        setRolloverEnabled(false);
    }

    void setPieceSymbol(String symbol, boolean whitePiece) {
        this.symbol = symbol;
        this.whitePiece = whitePiece;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (symbol.isEmpty()) {
            return;
        }

        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        GlyphVector glyph = getFont().createGlyphVector(g.getFontRenderContext(), symbol);
        Rectangle2D bounds = glyph.getVisualBounds();
        AffineTransform placement = AffineTransform.getTranslateInstance(
                (getWidth() - bounds.getWidth()) / 2 - bounds.getX(),
                (getHeight() - bounds.getHeight()) / 2 - bounds.getY());
        Shape shape = placement.createTransformedShape(glyph.getOutline());

        g.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(whitePiece ? WHITE_OUTLINE : BLACK_OUTLINE);
        g.draw(shape);
        g.setColor(whitePiece ? WHITE_PIECE : BLACK_PIECE);
        g.fill(shape);
        g.dispose();
    }
}
