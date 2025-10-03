package de.felixstaude.roguelike.entity;

import de.felixstaude.roguelike.math.Vec2;

import java.awt.*;
import java.util.List;

public class Bullet {
    public final Vec2 pos = new Vec2();
    public Vec2 vel = new Vec2();
    public double life = 1.0;
    public boolean dead=false;

    public int radius = 4;
    public double damage = 10.0;

    // Neu: Durchschlag & Homing
    public int pierce = 0;                // wie viele Gegner zusätzlich nach dem ersten Treffer
    public boolean homing = false;        // hat diese Kugel Homing?
    public double homingStrength = 6.0;   // Turn-Rate pro Sekunde (0..)
    public double homingRange = 260.0;    // Reichweite für Zielsuche

    public void update(double dt, List<Enemy> enemies){
        // Homing-Steuerung
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
        // Bewegung & Life
        pos.x += vel.x*dt; pos.y += vel.y*dt;
        life -= dt;
        if (life<=0) dead=true;
    }

    private Enemy nearestEnemy(List<Enemy> enemies){
        Enemy best=null; double bestD2 = homingRange*homingRange;
        for (Enemy e: enemies){
            if (e.dead) continue;
            double dx=e.pos.x-pos.x, dy=e.pos.y-pos.y;
            double d2=dx*dx+dy*dy;
            if (d2<bestD2){ bestD2=d2; best=e; }
        }
        return best;
    }

    public void render(Graphics2D g){
        int s = radius*2;
        g.setColor(new Color(240,250,255));
        g.fillOval((int)(pos.x - radius),(int)(pos.y - radius), s, s);
        if (homing){
            g.setColor(new Color(120,200,255,120));
            g.drawOval((int)(pos.x - radius-2),(int)(pos.y - radius-2), s+4, s+4);
        }
    }
}
