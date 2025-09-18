package de.felixstaude.roguelike.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

/**
 * Drawing helpers for rounded shapes, centered text and shared render hints.
 */
public final class Draw {
    private Draw() {
    }

    public static void applyQualityHints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    public static void fillRounded(Graphics2D g, Rectangle r, int arc) {
        g.fillRoundRect(r.x, r.y, r.width, r.height, arc, arc);
    }

    public static void drawRounded(Graphics2D g, Rectangle r, int arc) {
        g.drawRoundRect(r.x, r.y, r.width, r.height, arc, arc);
    }

    public static void drawPanel(Graphics2D g, Rectangle r, int arc, Color background, Color border) {
        g.setColor(background);
        fillRounded(g, r, arc);
        if (border != null) {
            g.setColor(border);
            drawRounded(g, r, arc);
        }
    }

    public static void drawShadowedPanel(Graphics2D g, Rectangle r, int arc, Color shadow, int offsetX, int offsetY,
                                         Color background, Color border) {
        g.setColor(shadow);
        g.fillRoundRect(r.x + offsetX, r.y + offsetY, r.width, r.height, arc, arc);
        drawPanel(g, r, arc, background, border);
    }

    public static void drawPill(Graphics2D g, Rectangle r, String text, Color background, Color textColor) {
        g.setColor(background);
        g.fillRoundRect(r.x, r.y, r.width, r.height, r.height, r.height);
        Color old = g.getColor();
        g.setColor(textColor);
        drawCenteredString(g, text, r);
        g.setColor(old);
    }

    public static void drawBadge(Graphics2D g, Rectangle r, String text, Color background, Color textColor) {
        g.setColor(background);
        g.fillRoundRect(r.x, r.y, r.width, r.height, r.height, r.height);
        Color old = g.getColor();
        g.setColor(textColor);
        drawCenteredString(g, text, r);
        g.setColor(old);
    }

    public static void drawButton(Graphics2D g, Rectangle r, String text, Color background, Color textColor) {
        g.setColor(background);
        g.fillRoundRect(r.x, r.y, r.width, r.height, Math.min(18, r.height), Math.min(18, r.height));
        Color old = g.getColor();
        g.setColor(textColor);
        drawCenteredString(g, text, r);
        g.setColor(old);
    }

    public static void drawCenteredString(Graphics2D g, String text, Rectangle r) {
        int w = g.getFontMetrics().stringWidth(text);
        int ascent = g.getFontMetrics().getAscent();
        int x = r.x + (r.width - w) / 2;
        int y = r.y + (r.height + ascent) / 2 - 2;
        g.drawString(text, x, y);
    }

    public static void drawCenteredString(Graphics2D g, String text, int containerWidth, int baselineY) {
        int w = g.getFontMetrics().stringWidth(text);
        int x = (containerWidth - w) / 2;
        g.drawString(text, x, baselineY);
    }
}
