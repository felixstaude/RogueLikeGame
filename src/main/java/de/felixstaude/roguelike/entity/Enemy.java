package de.felixstaude.roguelike.entity;

import de.felixstaude.roguelike.math.Vec2;

import java.awt.*;
import java.util.List;

public class Enemy {
    public final Vec2 pos = new Vec2();
    public double speed = 120.0;
    public int radius = 12;

    // ↓ leichter zu töten
    public double maxHp = 24.0;  // vorher 40
    public double hp = 24.0;     // vorher 40
    public double contactDamage = 10.0;
    public boolean dead = false;

    public double touchCooldown = 0.0;
    public double touchCooldownMax = 0.60;

    public double dodgeFactor = 0.35;
    public double lookahead = 380.0;

    public Color color = new Color(230, 95, 95);

    public void update(double dt, Player player, List<Bullet> bullets) {
        Vec2 toPlayer = new Vec2(player.pos.x - pos.x, player.pos.y - pos.y).normalized();
        Vec2 dodge = computeDodge(bullets);
        Vec2 desired = toPlayer.add(dodge.mul(dodgeFactor)).normalized();
        pos.x += desired.x * speed * dt;
        pos.y += desired.y * speed * dt;
    }

    public void damage(double dmg) {
        hp -= dmg;
        if (hp <= 0) { hp = 0; dead = true; }
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

    public void render(Graphics2D g) {
        int r = radius;
        g.setColor(new Color(20, 22, 28));
        g.fillOval((int)(pos.x - r - 2), (int)(pos.y - r - 2), (r * 2) + 4, (r * 2) + 4);
        g.setColor(color);
        g.fillOval((int)(pos.x - r), (int)(pos.y - r), r * 2, r * 2);
        g.setColor(new Color(5, 8, 12));
        g.drawOval((int)(pos.x - r), (int)(pos.y - r), r * 2, r * 2);
    }
}
