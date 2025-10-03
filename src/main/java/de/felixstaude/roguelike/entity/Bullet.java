package de.felixstaude.roguelike.entity;

import de.felixstaude.roguelike.math.Vec2;
import de.felixstaude.roguelike.util.Colors;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Flux Core Bullet - Energy orb projectile with trail effects.
 */
public class Bullet {
    public final Vec2 pos = new Vec2();
    public Vec2 vel = new Vec2();
    public double life = 1.0;
    public boolean dead = false;

    public int radius = 4;
    public double damage = 10.0;

    // Pierce & Homing
    public int pierce = 0;                // how many additional enemies after first hit
    public boolean homing = false;        // does this bullet have homing?
    public double homingStrength = 6.0;   // turn rate per second
    public double homingRange = 260.0;    // targeting range

    // Flux Core Visual
    public int instability = 0;           // 0-100%, set by Player when spawned
    private final List<TrailPoint> trail = new ArrayList<>();
    private static final int MAX_TRAIL_LENGTH = 5;

    /**
     * Trail point for visual effect.
     */
    private static class TrailPoint {
        double x, y;
        double alpha; // 0..1

        TrailPoint(double x, double y, double alpha) {
            this.x = x;
            this.y = y;
            this.alpha = alpha;
        }
    }

    public void update(double dt, List<Enemy> enemies){
        // Store trail point before moving
        if (trail.size() >= MAX_TRAIL_LENGTH) {
            trail.remove(0);
        }
        trail.add(new TrailPoint(pos.x, pos.y, 1.0));

        // Homing control
        if (homing) {
            Enemy target = nearestEnemy(enemies);
            if (target != null) {
                double speed = vel.len();
                Vec2 curDir = vel.normalized();
                Vec2 desired = new Vec2(target.pos.x - pos.x, target.pos.y - pos.y).normalized();
                double t = Math.min(1.0, homingStrength * dt);
                Vec2 newDir = Vec2.lerp(curDir, desired, t).normalized();
                vel = newDir.mul(speed);
            }
        }

        // Movement & Life
        pos.x += vel.x * dt;
        pos.y += vel.y * dt;
        life -= dt;
        if (life <= 0) dead = true;

        // Fade trail
        for (TrailPoint point : trail) {
            point.alpha *= 0.92; // fade out
        }
    }

    private Enemy nearestEnemy(List<Enemy> enemies){
        Enemy best = null;
        double bestD2 = homingRange * homingRange;
        for (Enemy e : enemies) {
            if (e.dead) continue;
            double dx = e.pos.x - pos.x, dy = e.pos.y - pos.y;
            double d2 = dx * dx + dy * dy;
            if (d2 < bestD2) {
                bestD2 = d2;
                best = e;
            }
        }
        return best;
    }

    /**
     * Flux Core Bullet Rendering:
     * - Energy orb (color based on instability)
     * - Trail effect (5 fading copies)
     * - Homing glow ring
     */
    public void render(Graphics2D g){
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color bulletColor = Colors.bulletColor(instability);

        // 1. Draw trail (from oldest to newest)
        for (int i = 0; i < trail.size(); i++) {
            TrailPoint point = trail.get(i);
            float ageFactor = (float) i / (float) MAX_TRAIL_LENGTH; // 0..1
            int trailAlpha = (int) (point.alpha * ageFactor * 120);

            if (trailAlpha > 10) {
                Color trailColor = Colors.withAlpha(bulletColor, trailAlpha);
                g.setColor(trailColor);
                int trailRadius = (int) (radius * (0.6 + ageFactor * 0.4)); // smaller at tail
                g.fillOval((int)(point.x - trailRadius), (int)(point.y - trailRadius),
                        trailRadius * 2, trailRadius * 2);
            }
        }

        // 2. Main bullet orb (outer glow)
        int glowRadius = radius + 3;
        Color glowColor = Colors.withAlpha(bulletColor, 80);
        g.setColor(glowColor);
        g.fillOval((int)(pos.x - glowRadius), (int)(pos.y - glowRadius),
                glowRadius * 2, glowRadius * 2);

        // 3. Core orb
        g.setColor(bulletColor);
        int s = radius * 2;
        g.fillOval((int)(pos.x - radius), (int)(pos.y - radius), s, s);

        // 4. Inner bright spot
        Color brightSpot = Colors.withAlpha(Colors.CORE_GLOW, 200);
        g.setColor(brightSpot);
        int innerRadius = radius / 2;
        g.fillOval((int)(pos.x - innerRadius), (int)(pos.y - innerRadius),
                innerRadius * 2, innerRadius * 2);

        // 5. Homing indicator ring
        if (homing) {
            Color homingRing = Colors.withAlpha(bulletColor, 150);
            g.setColor(homingRing);
            g.setStroke(new BasicStroke(1.5f));
            int ringRadius = radius + 4;
            g.drawOval((int)(pos.x - ringRadius), (int)(pos.y - ringRadius),
                    ringRadius * 2, ringRadius * 2);
        }
    }
}
