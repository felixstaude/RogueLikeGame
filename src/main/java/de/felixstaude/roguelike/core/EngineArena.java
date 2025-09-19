package de.felixstaude.roguelike.core;

import de.felixstaude.roguelike.math.Vec2;
import de.felixstaude.roguelike.util.Colors;
import de.felixstaude.roguelike.util.Mathx;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

/**
 * Handles mapping between world-space and canvas-space with a fixed logical arena size.
 */
public final class EngineArena {
    public static final int ARENA_W = 1600;
    public static final int ARENA_H = 900;

    private final Rectangle viewport = new Rectangle();
    private double scale = 1.0;

    public void resizeToCanvas(int canvasW, int canvasH) {
        if (canvasW <= 0 || canvasH <= 0) {
            viewport.setBounds(0, 0, 0, 0);
            scale = 1.0;
            return;
        }
        double sx = canvasW / (double) ARENA_W;
        double sy = canvasH / (double) ARENA_H;
        scale = Math.min(sx, sy);
        int viewW = (int) Math.round(ARENA_W * scale);
        int viewH = (int) Math.round(ARENA_H * scale);
        int x = (canvasW - viewW) / 2;
        int y = (canvasH - viewH) / 2;
        viewport.setBounds(x, y, viewW, viewH);
    }

    public Rectangle getViewportRect() {
        return new Rectangle(viewport);
    }

    public double getScale() {
        return scale;
    }

    public Point2D.Double toWorld(double screenX, double screenY) {
        double x = (screenX - viewport.x) / scale;
        double y = (screenY - viewport.y) / scale;
        x = Mathx.clamp(x, 0.0, ARENA_W);
        y = Mathx.clamp(y, 0.0, ARENA_H);
        return new Point2D.Double(x, y);
    }

    public Point2D.Double toScreen(double worldX, double worldY) {
        double x = viewport.x + worldX * scale;
        double y = viewport.y + worldY * scale;
        return new Point2D.Double(x, y);
    }

    public void clampWorld(Vec2 pos, double radius) {
        pos.x = Mathx.clamp(pos.x, radius, ARENA_W - radius);
        pos.y = Mathx.clamp(pos.y, radius, ARENA_H - radius);
    }

    public void clampWorld(Point2D.Double pos, double radius) {
        pos.x = Mathx.clamp(pos.x, radius, ARENA_W - radius);
        pos.y = Mathx.clamp(pos.y, radius, ARENA_H - radius);
    }

    public void applyWorldTransform(Graphics2D g) {
        AffineTransform transform = g.getTransform();
        transform.translate(viewport.x, viewport.y);
        transform.scale(scale, scale);
        g.setTransform(transform);
    }

    public void renderBackground(Graphics2D g) {
        g.setColor(Colors.FLOOR);
        g.fillRect(viewport.x, viewport.y, viewport.width, viewport.height);
        g.setColor(Colors.FLOOR_BORDER);
        g.setStroke(new BasicStroke(3f));
        g.drawRect(viewport.x, viewport.y, viewport.width - 1, viewport.height - 1);
    }
}
