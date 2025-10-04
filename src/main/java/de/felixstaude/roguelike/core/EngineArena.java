package de.felixstaude.roguelike.core;

import de.felixstaude.roguelike.math.Vec2;
import de.felixstaude.roguelike.util.Colors;
import de.felixstaude.roguelike.util.Mathx;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

/**
 * Flux Core Arena - Energy grid with pulsing lines and dynamic background.
 */
public final class EngineArena {
    public static final int ARENA_W = 1600;
    public static final int ARENA_H = 900;

    private final Rectangle viewport = new Rectangle();
    private double scale = 1.0;
    private double time = 0.0; // For animated effects

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

    /**
     * Updates animation time.
     */
    public void update(double dt) {
        time += dt;
    }

    /**
     * Flux Core Arena Background:
     * - Dark backdrop
     * - Pulsing energy grid
     * - Animated border lines
     * - Scattered energy sparks
     */
    public void renderBackground(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Dark backdrop
        g.setColor(Colors.BACKDROP);
        g.fillRect(viewport.x, viewport.y, viewport.width, viewport.height);

        // 2. Save transform, apply world transform for grid rendering
        AffineTransform savedTransform = g.getTransform();
        applyWorldTransform(g);

        // 3. Energy grid (pulsing)
        renderEnergyGrid(g);

        // 4. Scattered energy sparks
        renderEnergySparks(g);

        // 5. Restore transform for border rendering
        g.setTransform(savedTransform);

        // 6. Animated border
        renderAnimatedBorder(g);
    }

    /**
     * Renders pulsing energy grid lines.
     */
    private void renderEnergyGrid(Graphics2D g) {
        int gridSpacing = 100; // Grid cell size
        double pulse = Math.sin(time * 1.5) * 0.3 + 0.7; // 0.4..1.0

        g.setStroke(new BasicStroke(1.5f));
        Color gridColor = Colors.withAlpha(Colors.ENERGY_GRID, (int)(pulse * 80));
        g.setColor(gridColor);

        // Vertical lines
        for (int x = 0; x <= ARENA_W; x += gridSpacing) {
            // Vary alpha based on position for visual interest
            double lineIntensity = pulse * (0.7 + 0.3 * Math.sin(x * 0.01 + time * 0.5));
            g.setColor(Colors.withAlpha(Colors.ENERGY_GRID, (int)(lineIntensity * 80)));
            g.drawLine(x, 0, x, ARENA_H);
        }

        // Horizontal lines
        for (int y = 0; y <= ARENA_H; y += gridSpacing) {
            double lineIntensity = pulse * (0.7 + 0.3 * Math.sin(y * 0.01 + time * 0.5));
            g.setColor(Colors.withAlpha(Colors.ENERGY_GRID, (int)(lineIntensity * 80)));
            g.drawLine(0, y, ARENA_W, y);
        }
    }

    /**
     * Renders scattered energy sparks across the arena.
     */
    private void renderEnergySparks(Graphics2D g) {
        // Fixed pattern based on time for animated sparks
        int sparkCount = 15;
        for (int i = 0; i < sparkCount; i++) {
            double angle = (time * 0.3 + i * 2.4) % (Math.PI * 2);
            double radius = 300 + Math.sin(time * 0.5 + i) * 100;

            double x = ARENA_W / 2.0 + Math.cos(angle) * radius;
            double y = ARENA_H / 2.0 + Math.sin(angle) * radius;

            // Clamp to arena bounds
            x = Mathx.clamp(x, 50, ARENA_W - 50);
            y = Mathx.clamp(y, 50, ARENA_H - 50);

            double sparkPulse = Math.sin(time * 2.0 + i * 0.7) * 0.5 + 0.5;
            int alpha = (int)(sparkPulse * 120);

            if (alpha > 30) {
                g.setColor(Colors.withAlpha(Colors.ENERGY_PULSE, alpha));
                int size = (int)(3 + sparkPulse * 2);
                g.fillOval((int)(x - size/2), (int)(y - size/2), size, size);

                // Glow
                g.setColor(Colors.withAlpha(Colors.ENERGY_PULSE, alpha / 3));
                int glowSize = size + 4;
                g.fillOval((int)(x - glowSize/2), (int)(y - glowSize/2), glowSize, glowSize);
            }
        }
    }

    /**
     * Renders animated pulsing border.
     */
    private void renderAnimatedBorder(Graphics2D g) {
        double pulse = Math.sin(time * 2.0) * 0.4 + 0.6; // 0.2..1.0

        // Outer border (pulsing energy)
        g.setColor(Colors.withAlpha(Colors.ENERGY_PULSE, (int)(pulse * 150)));
        g.setStroke(new BasicStroke(4f));
        g.drawRect(viewport.x, viewport.y, viewport.width - 1, viewport.height - 1);

        // Inner border (static)
        g.setColor(Colors.FLOOR_BORDER);
        g.setStroke(new BasicStroke(2f));
        g.drawRect(viewport.x + 2, viewport.y + 2, viewport.width - 5, viewport.height - 5);
    }
}
