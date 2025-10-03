package de.felixstaude.roguelike.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * Drawing helpers for rounded shapes, centered text and shared render hints.
 */
public final class Draw {
    private Draw() {}

    /** High-quality render hints for shapes & text. */
    public static void globalHints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,       RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,  RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,           RenderingHints.VALUE_RENDER_QUALITY);
    }

    /** Back-compat alias used by older code paths. */
    public static void applyQualityHints(Graphics2D g) {
        globalHints(g);
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
            Color old = g.getColor();
            g.setColor(border);
            drawRounded(g, r, arc);
            g.setColor(old);
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
        int arc = Math.min(18, r.height);
        g.setColor(background);
        g.fillRoundRect(r.x, r.y, r.width, r.height, arc, arc);
        Color old = g.getColor();
        g.setColor(textColor);
        drawCenteredString(g, text, r);
        g.setColor(old);
    }

    public static void drawCenteredString(Graphics2D g, String text, Rectangle r) {
        if (text == null || text.isEmpty()) return;
        int w = g.getFontMetrics().stringWidth(text);
        int ascent = g.getFontMetrics().getAscent();
        int descent = g.getFontMetrics().getDescent();
        int x = r.x + (r.width - w) / 2;
        int y = r.y + (r.height - (ascent + descent)) / 2 + ascent;
        g.drawString(text, x, y);
    }

    public static void drawCenteredString(Graphics2D g, String text, int containerWidth, int baselineY) {
        int w = g.getFontMetrics().stringWidth(text);
        int x = (containerWidth - w) / 2;
        g.drawString(text, x, baselineY);
    }

    /** Draws an image clipped to a rounded rect with optional border. */
    public static void drawIcon(Graphics2D g, BufferedImage image, Rectangle area, int arc, Color border) {
        if (image == null || area.width <= 0 || area.height <= 0) return;

        Object oldInterp = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        Shape oldClip = g.getClip();
        RoundRectangle2D.Float clip = new RoundRectangle2D.Float(area.x, area.y, area.width, area.height, arc, arc);
        g.setClip(clip);
        g.drawImage(image, area.x, area.y, area.width, area.height, null);
        g.setClip(oldClip);

        if (border != null) {
            Color oldCol = g.getColor();
            Stroke oldStroke = g.getStroke();
            g.setColor(border);
            g.setStroke(new BasicStroke(1.2f));
            g.draw(clip);
            g.setStroke(oldStroke);
            g.setColor(oldCol);
        }

        if (oldInterp != null) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterp);
        }
    }
}
