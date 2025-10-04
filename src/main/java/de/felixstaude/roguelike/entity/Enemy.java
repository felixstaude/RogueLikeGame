package de.felixstaude.roguelike.entity;

import de.felixstaude.roguelike.math.Vec2;
import de.felixstaude.roguelike.util.Colors;

import java.awt.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Corrupted Core Enemy - Dark purple corrupted energy entities.
 */
public class Enemy {
    public final Vec2 pos = new Vec2();
    public double speed = 120.0;
    public int radius = 12;

    public double maxHp = 24.0;
    public double hp = 24.0;
    public double contactDamage = 10.0;
    public boolean dead = false;

    public double touchCooldown = 0.0;
    public double touchCooldownMax = 0.60;

    public double dodgeFactor = 0.35;
    public double lookahead = 380.0;

    // Flux Core Visual
    public Color color = Colors.CORRUPTION; // Purple corruption theme
    private double time = 0.0;
    private double hitFlash = 0.0;

    public void update(double dt, Player player, List<Bullet> bullets) {
        time += dt;

        Vec2 toPlayer = new Vec2(player.pos.x - pos.x, player.pos.y - pos.y).normalized();
        Vec2 dodge = computeDodge(bullets);
        Vec2 desired = toPlayer.add(dodge.mul(dodgeFactor)).normalized();
        pos.x += desired.x * speed * dt;
        pos.y += desired.y * speed * dt;

        hitFlash = Math.max(0.0, hitFlash - dt * 4.0);
    }

    public void damage(double dmg) {
        hp -= dmg;
        hitFlash = 1.0;
        if (hp <= 0) {
            hp = 0;
            dead = true;
        }
    }

    private Vec2 computeDodge(List<Bullet> bullets) {
        double bestPerp = Double.MAX_VALUE;
        Vec2 bestDir = new Vec2(0, 0);
        for (Bullet b : bullets) {
            Vec2 bdir = b.vel.normalized();
            if (bdir.len() < 1e-6) continue;
            Vec2 rel = new Vec2(pos.x - b.pos.x, pos.y - b.pos.y);
            double along = rel.dot(bdir);
            if (along < 0 || along > lookahead) continue;
            Vec2 perp = rel.sub(bdir.mul(along));
            double dperp = perp.len();
            if (dperp < bestPerp) {
                bestPerp = dperp;
                bestDir = (dperp > 1e-6) ? perp.normalized() : new Vec2(-bdir.y, bdir.x);
            }
        }
        return bestDir;
    }

    /**
     * Corrupted Core Rendering:
     * - Dark purple sphere with corruption cracks
     * - Flicker outline effect
     * - RGB shift glitch (constant low-level)
     * - Hit flash
     */
    public void render(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Glitch offset (constant low-level corruption effect)
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        double glitchX = (rnd.nextDouble() - 0.5) * 1.5;
        double glitchY = (rnd.nextDouble() - 0.5) * 1.5;

        double renderX = pos.x + glitchX;
        double renderY = pos.y + glitchY;

        int r = radius;

        // 1. RGB Shift (chromatic aberration)
        if (rnd.nextDouble() < 0.3) {
            // Red channel shift
            Color red = new Color(color.getRed(), 0, 0, 80);
            g.setColor(red);
            g.fillOval((int)(renderX - r + 2), (int)(renderY - r), r * 2, r * 2);

            // Blue channel shift
            Color blue = new Color(0, 0, color.getBlue(), 80);
            g.setColor(blue);
            g.fillOval((int)(renderX - r - 2), (int)(renderY - r), r * 2, r * 2);
        }

        // 2. Dark outer shadow
        Color shadow = new Color(10, 10, 15, 180);
        g.setColor(shadow);
        g.fillOval((int)(renderX - r - 2), (int)(renderY - r - 2), (r * 2) + 4, (r * 2) + 4);

        // 3. Main corruption body
        g.setColor(color);
        g.fillOval((int)(renderX - r), (int)(renderY - r), r * 2, r * 2);

        // 4. Darker corruption core
        Color darkCore = Colors.withAlpha(new Color(0x40, 0, 0x40), 200);
        g.setColor(darkCore);
        int innerR = r / 2;
        g.fillOval((int)(renderX - innerR), (int)(renderY - innerR), innerR * 2, innerR * 2);

        // 5. Corruption "cracks" (thin lines radiating from center)
        double pulse = Math.sin(time * 3.0) * 0.5 + 0.5; // 0..1
        if (pulse > 0.3) {
            g.setColor(Colors.withAlpha(Colors.CORRUPTION, (int)(pulse * 150)));
            g.setStroke(new BasicStroke(1.5f));
            int crackCount = 6;
            for (int i = 0; i < crackCount; i++) {
                double angle = (Math.PI * 2 / crackCount) * i + time * 0.5;
                int x1 = (int)(renderX + Math.cos(angle) * (r * 0.3));
                int y1 = (int)(renderY + Math.sin(angle) * (r * 0.3));
                int x2 = (int)(renderX + Math.cos(angle) * (r + 3));
                int y2 = (int)(renderY + Math.sin(angle) * (r + 3));
                g.drawLine(x1, y1, x2, y2);
            }
        }

        // 6. Flicker outline
        double flickerChance = Math.sin(time * 7.0 + pos.x) * 0.5 + 0.5;
        if (flickerChance > 0.6) {
            Color flickerColor = Colors.withAlpha(Colors.CORRUPTION, (int)(flickerChance * 200));
            g.setColor(flickerColor);
            g.setStroke(new BasicStroke(2f));
            g.drawOval((int)(renderX - r), (int)(renderY - r), r * 2, r * 2);
        } else {
            // Normal border
            g.setColor(new Color(5, 8, 12));
            g.setStroke(new BasicStroke(1.5f));
            g.drawOval((int)(renderX - r), (int)(renderY - r), r * 2, r * 2);
        }

        // 7. Hit flash
        if (hitFlash > 0.1) {
            Color flash = new Color(255, 255, 255, (int)(hitFlash * 120));
            g.setColor(flash);
            g.fillOval((int)(renderX - r), (int)(renderY - r), r * 2, r * 2);
        }

        // 8. HP Bar (if damaged)
        if (hp < maxHp) {
            drawHealthBar(g, renderX, renderY);
        }
    }

    /**
     * Draws a small health bar above the enemy.
     */
    private void drawHealthBar(Graphics2D g, double cx, double cy) {
        int barWidth = 24;
        int barHeight = 3;
        int barY = (int)(cy - radius - 8);
        int barX = (int)(cx - barWidth / 2);

        // Background
        g.setColor(new Color(40, 40, 50));
        g.fillRect(barX, barY, barWidth, barHeight);

        // HP fill
        int hpWidth = (int)(barWidth * (hp / maxHp));
        Color hpColor = hp > maxHp * 0.5 ? Colors.SUCCESS : Colors.DANGER;
        g.setColor(hpColor);
        g.fillRect(barX, barY, hpWidth, barHeight);

        // Border
        g.setColor(Colors.CORRUPTION);
        g.setStroke(new BasicStroke(1f));
        g.drawRect(barX, barY, barWidth, barHeight);
    }
}
