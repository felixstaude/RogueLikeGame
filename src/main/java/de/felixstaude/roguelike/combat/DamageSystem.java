package de.felixstaude.roguelike.combat;

import de.felixstaude.roguelike.entity.*;

import java.util.Iterator;
import java.util.List;

public class DamageSystem {
    private final Player player;
    private final List<Bullet> bullets;
    private final List<Enemy> enemies;
    private final List<Particle> particles;

    public DamageSystem(Player player, List<Bullet> bullets, List<Enemy> enemies, List<Particle> particles) {
        this.player = player;
        this.bullets = bullets;
        this.enemies = enemies;
        this.particles = particles;
    }

    public void update(double dt) {
        // Bullet -> Enemy
        for (Bullet b : bullets) {
            if (b.dead) continue;
            for (Enemy e : enemies) {
                if (e.dead) continue;
                double r = e.radius + b.radius;
                double dx = e.pos.x - b.pos.x, dy = e.pos.y - b.pos.y;
                if (dx*dx + dy*dy <= r*r) {
                    e.damage(b.damage);
                    // Lifesteal direkt beim Hit
                    if (player.lifesteal > 0) player.heal(b.damage * player.lifesteal);

                    for (int i=0;i<6;i++) particles.add(Particle.hit(b.pos.x, b.pos.y));
                    if (e.dead) {
                        player.addXp(2);
                        player.addGold(1);
                        spawnDeath(e);
                    }
                    // Pierce-Logik
                    if (b.pierce > 0) {
                        b.pierce--;
                    } else {
                        b.dead = true;
                    }
                    break; // pro Frame nur ein Treffer pro Kugel
                }
            }
        }

        // Enemy touch -> Player (mit Cooldown)
        for (Enemy e : enemies) {
            e.touchCooldown -= dt;
            if (e.dead || e.touchCooldown > 0) continue;
            double r = e.radius + player.radius;
            double dx = player.pos.x - e.pos.x, dy = player.pos.y - e.pos.y;
            if (dx*dx + dy*dy <= r*r) {
                player.damage(e.contactDamage);
                e.touchCooldown = e.touchCooldownMax;
                for (int i=0;i<5;i++) particles.add(Particle.hit(player.pos.x, player.pos.y));
            }
        }

        // Tote Enemies entfernen
        for (Iterator<Enemy> it = enemies.iterator(); it.hasNext();) {
            if (it.next().dead) it.remove();
        }
    }

    private void spawnDeath(Enemy e) {
        for (int i=0;i<12;i++) {
            particles.add(Particle.burst(e.pos.x, e.pos.y, e.color));
        }
    }
}
