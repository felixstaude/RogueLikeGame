package de.felixstaude.roguelike.entity;

import de.felixstaude.roguelike.math.Vec2;
import de.felixstaude.roguelike.stats.InstabilityEffects;
import de.felixstaude.roguelike.util.Colors;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Flux Core Player - The player IS the unstable flux core.
 */
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

    // Stats
    public double lifesteal = 0.0;             // z.B. 0.06 = 6% vom Schaden heilt
    public int multishot = 0;                  // +1 = eine zusätzliche Kugel
    public double multishotSpreadDeg = 10.0;   // Winkelabstand zwischen Kugeln
    public int pierce = 0;                     // Anzahl zusätzlicher Gegner pro Kugel
    public double homingChance = 0.0;          // 0..1 Chance, dass eine Kugel homing ist
    public double homingStrength = 6.0;        // Turn-Rate pro Sekunde
    public double homingRange = 260.0;         // Reichweite

    // Flux Core System
    public int instability = 0;                // 0-100%, set by Engine from EffectiveStats

    // Visual state
    private double hitFlash = 0.0;
    private double time = 0.0;                 // For animations

    public void update(double dt, Vec2 moveDir, double mouseX, double mouseY,
                       List<Bullet> bullets, List<Particle> particles){
        time += dt;

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
            b.instability = instability; // Pass instability to bullet for visual effects

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

    /**
     * Flux Core Rendering:
     * - Central glowing core (color based on instability)
     * - Pulsating glow effect
     * - 3 rotating energy rings (different speeds)
     * - Glitch effects at high instability (>75%)
     */
    public void render(Graphics2D g){
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color coreColor = Colors.fluxCoreColor(instability);
        double pulseSpeed = InstabilityEffects.getGlowPulseSpeed(instability);
        double glitchIntensity = InstabilityEffects.getGlitchIntensity(instability);

        // Glitch offset for high instability
        double glitchX = 0, glitchY = 0;
        if (glitchIntensity > 0.1) {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            double magnitude = glitchIntensity * 4.0;
            glitchX = (rnd.nextDouble() - 0.5) * magnitude;
            glitchY = (rnd.nextDouble() - 0.5) * magnitude;
        }

        double renderX = pos.x + glitchX;
        double renderY = pos.y + glitchY;

        // 1. Outer glow (pulsing)
        double pulseFactor = Math.sin(time * pulseSpeed * Math.PI) * 0.5 + 0.5; // 0..1
        int glowRadius = (int) (radius + 8 + pulseFactor * 5);
        Color glowColor = Colors.withAlpha(coreColor, (int) (40 + pulseFactor * 40));
        g.setColor(glowColor);
        g.fillOval((int)(renderX - glowRadius), (int)(renderY - glowRadius), glowRadius * 2, glowRadius * 2);

        // 2. Energy Rings (3 rings rotating at different speeds)
        drawEnergyRing(g, renderX, renderY, radius + 10, time * 0.5, coreColor, 80);
        drawEnergyRing(g, renderX, renderY, radius + 15, -time * 0.7, coreColor, 60);
        drawEnergyRing(g, renderX, renderY, radius + 20, time * 0.3, coreColor, 40);

        // 3. Core sphere (main body)
        int r = radius;
        // Darker inner shadow
        Color shadow = Colors.withAlpha(Color.BLACK, 120);
        g.setColor(shadow);
        g.fillOval((int)(renderX - r), (int)(renderY - r), r * 2, r * 2);

        // Bright core
        Color brightCore = Colors.withAlpha(coreColor, 200);
        g.setColor(brightCore);
        g.fillOval((int)(renderX - r + 2), (int)(renderY - r + 2), (r - 2) * 2, (r - 2) * 2);

        // Inner glow
        Color innerGlow = Colors.withAlpha(Colors.CORE_GLOW, 150);
        g.setColor(innerGlow);
        g.fillOval((int)(renderX - r/2), (int)(renderY - r/2), r, r);

        // 4. Hit flash overlay
        if (hitFlash > 0.1) {
            Color flash = new Color(255, 255, 255, (int)(hitFlash * 180));
            g.setColor(flash);
            g.fillOval((int)(renderX - r), (int)(renderY - r), r * 2, r * 2);
        }

        // 5. Outer border
        g.setColor(coreColor);
        g.setStroke(new BasicStroke(2f));
        g.drawOval((int)(renderX - r), (int)(renderY - r), r * 2, r * 2);

        // 6. Glitch artifacts at very high instability
        if (glitchIntensity > 0.5) {
            drawGlitchArtifacts(g, renderX, renderY, r, coreColor, glitchIntensity);
        }
    }

    /**
     * Draws a rotating energy ring around the core.
     */
    private void drawEnergyRing(Graphics2D g, double cx, double cy, int ringRadius, double angle, Color baseColor, int alpha) {
        int segments = 12;
        double angleStep = (Math.PI * 2) / segments;

        for (int i = 0; i < segments; i++) {
            double a = angle + i * angleStep;
            double x = cx + Math.cos(a) * ringRadius;
            double y = cy + Math.sin(a) * ringRadius;

            // Alternate bright/dim segments
            int segmentAlpha = (i % 2 == 0) ? alpha : alpha / 2;
            Color ringColor = Colors.withAlpha(baseColor, segmentAlpha);

            g.setColor(ringColor);
            g.fillOval((int)(x - 3), (int)(y - 3), 6, 6);
        }
    }

    /**
     * Draws glitch artifacts (RGB shift, scanlines) at critical instability.
     */
    private void drawGlitchArtifacts(Graphics2D g, double cx, double cy, int r, Color coreColor, double intensity) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        // RGB shift effect
        if (rnd.nextDouble() < intensity * 0.3) {
            int offset = (int)(intensity * 3);

            // Red channel
            Color red = new Color(coreColor.getRed(), 0, 0, 100);
            g.setColor(red);
            g.fillOval((int)(cx - r + offset), (int)(cy - r), r * 2, r * 2);

            // Blue channel
            Color blue = new Color(0, 0, coreColor.getBlue(), 100);
            g.setColor(blue);
            g.fillOval((int)(cx - r - offset), (int)(cy - r), r * 2, r * 2);
        }

        // Random pixel flickers
        for (int i = 0; i < intensity * 10; i++) {
            double px = cx + (rnd.nextDouble() - 0.5) * r * 3;
            double py = cy + (rnd.nextDouble() - 0.5) * r * 3;
            g.setColor(Colors.withAlpha(coreColor, rnd.nextInt(100, 200)));
            g.fillRect((int)px, (int)py, 2, 2);
        }
    }
}
