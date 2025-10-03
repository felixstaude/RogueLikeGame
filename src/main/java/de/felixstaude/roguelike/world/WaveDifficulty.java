package de.felixstaude.roguelike.world;

/** Berechnet alle skalierenden Parameter aus der aktuellen Wave. */
public class WaveDifficulty {
    public final int wave;

    public final double spawnInterval; // Sekunden zwischen Spawns (Start der Wave)
    public final double minInterval;   // Untergrenze, zu der innerhalb der Wave langsam „driftet“
    public final int batchSize;        // wie viele Enemies pro Spawn-Tick
    public final int maxEnemies;       // hartes Limit gleichzeitig

    public final double enemyHpMul;    // Multiplikator auf Enemy-HP
    public final double enemySpeedMul; // Multiplikator auf Enemy-Speed
    public final double enemyDodgeMul; // Multiplikator auf Dodge-Faktor

    private WaveDifficulty(int wave,
                           double spawnInterval, double minInterval, int batchSize, int maxEnemies,
                           double enemyHpMul, double enemySpeedMul, double enemyDodgeMul) {
        this.wave = wave;
        this.spawnInterval = spawnInterval;
        this.minInterval = minInterval;
        this.batchSize = batchSize;
        this.maxEnemies = maxEnemies;
        this.enemyHpMul = enemyHpMul;
        this.enemySpeedMul = enemySpeedMul;
        this.enemyDodgeMul = enemyDodgeMul;
    }

    public static WaveDifficulty forWave(int wave) {
        int w = Math.max(1, wave);

        // Mehr Enemies: schnellere Spawns + Batch-Spawn + höheres Cap
        double spawnInterval = 1.60 / (1.0 + 0.18 * (w - 1));                 // Wave 1: 1.60s, Wave 6: ~0.84s
        double minInterval   = Math.max(0.28, 0.90 / (1.0 + 0.14 * (w - 1))); // nie schneller als ~0.28s
        int batchSize        = 1 + (w - 1) / 3;                                // alle 3 Waves +1
        int maxEnemies       = Math.min(200, 45 + (w - 1) * 6);

        // Leichte Stat-Skalierung: fühlt sich progressiv an, ohne unfair
        double enemyHpMul    = Math.min(4.0, 1.0 + 0.12 * (w - 1));            // bis 4x HP
        double enemySpeedMul = Math.min(1.8, 1.0 + 0.035 * (w - 1));           // bis 1.8x Speed
        double enemyDodgeMul = Math.min(1.6, 1.0 + 0.02  * (w - 1));           // bis 1.6x Dodge

        return new WaveDifficulty(w, spawnInterval, minInterval, batchSize, maxEnemies,
                enemyHpMul, enemySpeedMul, enemyDodgeMul);
    }
}
