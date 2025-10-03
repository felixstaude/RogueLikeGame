package de.felixstaude.roguelike.entity;

import de.felixstaude.roguelike.math.Vec2;

import java.awt.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Player {
    public final Vec2 pos = new Vec2();
    public final Vec2 vel = new Vec2();
    public double speed = 300;
    public double friction = 12;

    public double hp = 100, maxHp = 100;
    public int radius = 12;

    // Fire / Bullet Base
    public double fireRate = 8.0;
    public double fireCooldown = 0.0;
    public double bulletSpeed = 700;
    public double bulletLife = 1.2;
    public double bulletDamage = 20.0;

    // Progression
    public int xp = 0;
    public int gold = 0;

    // Neu: Stats
    public double lifesteal = 0.0;             // z.B. 0.06 = 6% vom Schaden heilt
    public int multishot = 0;                  // +1 = eine zusätzliche Kugel
    public double multishotSpreadDeg = 10.0;   // Winkelabstand zwischen Kugeln
    public int pierce = 0;                     // Anzahl zusätzlicher Gegner pro Kugel
    public double homingChance = 0.0;          // 0..1 Chance, dass eine Kugel homing ist
    public double homingStrength = 6.0;        // Turn-Rate pro Sekunde
    public double homingRange = 260.0;         // Reichweite

    private double hitFlash = 0.0;

    public void update(double dt, Vec2 moveDir, double mouseX, double mouseY,
                       List<Bullet> bullets, List<Particle> particles){
        vel.x = moveDir.x * speed; vel.y = moveDir.y * speed;
        if (moveDir.x==0) vel.x *= Math.max(0, 1 - dt*friction);
        if (moveDir.y==0) vel.y *= Math.max(0, 1 - dt*friction);
        pos.x += vel.x * dt; pos.y += vel.y * dt;

        Vec2 aim = new Vec2(mouseX - pos.x, mouseY - pos.y).normalized();
        fireCooldown -= dt;
        while (fireCooldown <= 0.0) {
            fireCooldown += 1.0 / fireRate;
            spawnBullets(bullets, particles, aim);
        }
        hitFlash = Math.max(0.0, hitFlash - dt*3.0);
    }

    private void spawnBullets(List<Bullet> bullets, List<Particle> particles, Vec2 aim){
        if (aim.x==0 && aim.y==0) return;
        int n = 1 + Math.max(0, multishot);
        double baseAng = aim.angle();
        double step = Math.toRadians(multishotSpreadDeg);
        double start = -step * (n-1)/2.0;

        for (int i=0;i<n;i++){
            Vec2 dir = Vec2.rotate(aim, start + i*step).normalized();
            Bullet b = new Bullet();
            b.pos.set(pos.x, pos.y);
            b.vel = dir.mul(bulletSpeed);
            b.life = bulletLife;
            b.damage = bulletDamage;
            b.radius = 4;
            b.pierce = Math.max(0, pierce);
            // Homing Roll
            if (ThreadLocalRandom.current().nextDouble() < homingChance) {
                b.homing = true;
                b.homingStrength = homingStrength;
                b.homingRange = homingRange;
            }
            bullets.add(b);
        }
        for (int i=0;i<4;i++) particles.add(Particle.muzzle(pos.x, pos.y, aim));
    }

    public void addXp(int amount){ xp += amount; }
    public void addGold(int amount){ gold += amount; }
    public boolean spendGold(int cost){ if (gold >= cost){ gold -= cost; return true; } return false; }

    public void damage(double dmg){ hp = Math.max(0, hp - dmg); hitFlash = 1.0; }
    public void heal(double v){ hp = Math.min(maxHp, hp + v); }
    public boolean isDead(){ return hp <= 0; }

    public void render(Graphics2D g){
        int r=radius;
        Color body = new Color(80,210,255);
        Color flash = new Color(255,255,255, (int)(hitFlash*180));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(24,120,160)); g.fillOval((int)(pos.x-r),(int)(pos.y-r), r*2, r*2);
        g.setColor(body); g.fillOval((int)(pos.x-r+3),(int)(pos.y-r+3), r*2-6, r*2-6);
        g.setColor(flash); g.fillOval((int)(pos.x-r),(int)(pos.y-r), r*2, r*2);
        g.setColor(new Color(5,8,12)); g.setStroke(new BasicStroke(2f)); g.drawOval((int)(pos.x-r),(int)(pos.y-r), r*2, r*2);
    }
}
