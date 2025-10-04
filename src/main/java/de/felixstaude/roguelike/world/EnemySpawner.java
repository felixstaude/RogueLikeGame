package de.felixstaude.roguelike.world;

import de.felixstaude.roguelike.entity.Enemy;
import de.felixstaude.roguelike.entity.Player;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class EnemySpawner {
    private final Arena arena;

    private double timer = 0.0;
    private double currentInterval = 1.60;

    private WaveDifficulty diff = WaveDifficulty.forWave(1);

    public EnemySpawner(Arena arena) {
        this.arena = arena;
        onWaveStart(1);
    }

    /** Bei Restart auf Wave 1 zurücksetzen. */
    public void reset() {
        timer = 0.0;
        onWaveStart(1);
    }

    /** Wird vom Engine bei Start einer neuen Wave aufgerufen. */
    public void onWaveStart(int wave) {
        diff = WaveDifficulty.forWave(wave);
        timer = 0.0;
        currentInterval = diff.spawnInterval;
    }

    public void update(double dt, List<Enemy> enemies, Player player) {
        timer += dt;

        // Wenn sehr voll, keine neuen Spawns
        if (enemies.size() >= diff.maxEnemies) return;

        while (timer >= currentInterval) {
            timer -= currentInterval;

            // kleine Beschleunigung innerhalb der Wave, bis Untergrenze
            currentInterval = Math.max(diff.minInterval, currentInterval * 0.985);

            int toSpawn = Math.min(diff.batchSize, diff.maxEnemies - enemies.size());
            for (int i = 0; i < toSpawn; i++) {
                enemies.add(spawnAtEdge(player));
                if (enemies.size() >= diff.maxEnemies) break;
            }

            if (enemies.size() >= diff.maxEnemies) break;
        }
    }

    private Enemy spawnAtEdge(Player player) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int side = rnd.nextInt(4); // 0=top, 1=right, 2=bottom, 3=left
        double x = 0, y = 0;
        int margin = 10;

        if (side == 0) { x = rnd.nextInt(arena.x + margin, arena.x + arena.w - margin); y = arena.y + margin; }
        if (side == 1) { x = arena.x + arena.w - margin; y = rnd.nextInt(arena.y + margin, arena.y + arena.h - margin); }
        if (side == 2) { x = rnd.nextInt(arena.x + margin, arena.x + arena.w - margin); y = arena.y + arena.h - margin; }
        if (side == 3) { x = arena.x + margin; y = rnd.nextInt(arena.y + margin, arena.y + arena.h - margin); }

        double minDist = 180;
        int tries = 16;
        while (tries-- > 0) {
            double dx = x - player.pos.x, dy = y - player.pos.y;
            if (dx * dx + dy * dy >= minDist * minDist) break;
            side = rnd.nextInt(4);
            if (side == 0) { x = rnd.nextInt(arena.x + margin, arena.x + arena.w - margin); y = arena.y + margin; }
            if (side == 1) { x = arena.x + arena.w - margin; y = rnd.nextInt(arena.y + margin, arena.y + arena.h - margin); }
            if (side == 2) { x = rnd.nextInt(arena.x + margin, arena.x + arena.w - margin); y = arena.y + arena.h - margin; }
            if (side == 3) { x = arena.x + margin; y = rnd.nextInt(arena.y + margin, arena.y + arena.h - margin); }
        }

        Enemy e = new Enemy();
        // Basestats + Wave-Skalierung
        e.pos.set(x, y);
        e.speed = (100 + rnd.nextDouble() * 40) * diff.enemySpeedMul;
        e.maxHp = e.maxHp * diff.enemyHpMul;
        e.hp = e.maxHp;
        e.dodgeFactor = Math.min(0.7, e.dodgeFactor * diff.enemyDodgeMul); // nicht übertrieben dodgen

        return e;
    }

    public int getMaxEnemies() { return diff.maxEnemies; }
    public double getCurrentInterval() { return currentInterval; }
    public WaveDifficulty getDifficulty() { return diff; }
}
