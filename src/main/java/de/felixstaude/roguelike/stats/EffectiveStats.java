// src/main/java/de/felixstaude/roguelike/stats/EffectiveStats.java
package de.felixstaude.roguelike.stats;

/**
 * Effektive, zur Engine gemappte Werte (nach Caps/Floors/Regeln).
 * Erzeugst du über {@link #from(Base, Stats)} aus Basiswerten + Roh-Stats.
 */
public final class EffectiveStats {

    // ---- Runtime-Werte, die die Engine/Player direkt nutzen kann ------------
    public final int maxHp;
    public final double incomingDamageMul; // auf eingehenden Schaden (Armor bereits eingerechnet)
    public final int dodgePct;             // 0..60
    public final double hpRegenPerSec;

    // Offensiv / Ranged
    public final double rangedDamageMul;   // global + typed, min 1.0
    public final int critChancePct;        // 0..100
    public final double critMultiplier;    // >= 1.0

    public final double fireRate;          // Schüsse/Sek
    public final int rangePx;              // Reichweite der Projektile in px
    public final double projectileSpeedMul;
    public final double projectileSizeMul;

    public final int multishot;            // >=0, zusätzliche Kugeln
    public final int pierce;               // >=0, zusätzliche Treffer
    public final double homingChance01;    // 0..1
    public final double homingStrengthMul; // >=0.2

    // Movement / Sustain / Meta
    public final double moveSpeedMul;
    public final double lifestealFrac;     // 0.. (Anteil)
    public final int luck;                 // darf negativ sein
    public final int harvesting;           // darf negativ sein

    public final double bossDamageMul;     // 1.0 + BossDamage%

    private EffectiveStats(
            int maxHp, double incomingDamageMul, int dodgePct, double hpRegenPerSec,
            double rangedDamageMul, int critChancePct, double critMultiplier,
            double fireRate, int rangePx, double projectileSpeedMul, double projectileSizeMul,
            int multishot, int pierce, double homingChance01, double homingStrengthMul,
            double moveSpeedMul, double lifestealFrac, int luck, int harvesting, double bossDamageMul) {
        this.maxHp = maxHp;
        this.incomingDamageMul = incomingDamageMul;
        this.dodgePct = dodgePct;
        this.hpRegenPerSec = hpRegenPerSec;
        this.rangedDamageMul = rangedDamageMul;
        this.critChancePct = critChancePct;
        this.critMultiplier = critMultiplier;
        this.fireRate = fireRate;
        this.rangePx = rangePx;
        this.projectileSpeedMul = projectileSpeedMul;
        this.projectileSizeMul = projectileSizeMul;
        this.multishot = multishot;
        this.pierce = pierce;
        this.homingChance01 = homingChance01;
        this.homingStrengthMul = homingStrengthMul;
        this.moveSpeedMul = moveSpeedMul;
        this.lifestealFrac = lifestealFrac;
        this.luck = luck;
        this.harvesting = harvesting;
        this.bossDamageMul = bossDamageMul;
    }

    /** Basiswerte des Spielers / der aktuell aktiven Waffe. */
    public static final class Base {
        public int baseMaxHp = 100;

        public double baseFireRate = 8.0;          // Schüsse/s
        public double baseBulletDamage = 20.0;     // wird mit rangedDamageMul multipliziert
        public int baseRangePx = 700;              // ~ bulletSpeed * bulletLife
        public double baseProjectileSpeedMul = 1.0;// 1.0 = unverändert
        public double baseProjectileSizeMul  = 1.0;

        public int baseCritChancePct = 0;          // Default 0%
        public int baseCritDamageExtraPct = 0;     // additiv zu +50% Basis

        public int baseMultishot = 0;
        public int basePierce = 0;
        public int baseHomingChancePct = 0;
        public int baseHomingStrengthPct = 0;

        public double baseMoveSpeedMul = 1.0;
        public double baseLifestealFrac = 0.0;

        public int baseLuck = 0;
        public int baseHarvesting = 0;
        public int baseArmorPct = 0;
        public int baseDodgePct = 0;
        public int baseRangePoints = 0;            // zusätzliche Range-Punkte (1P ≈ 6px)
        public int baseRangedPct = 0;              // Basis-Typmultiplikator
        public int baseDamagePct = 0;              // Basis-Globalmultiplikator
    }

    /** Rechnet alle Regeln/Caps und kombiniert Base + Stats. */
    public static EffectiveStats from(Base b, Stats s) {
        // Defensiv
        int maxHp = StatRules.effectiveMaxHp(b.baseMaxHp, s.get(Stat.MAX_HP));
        int armorPct = StatRules.clampArmorPct(b.baseArmorPct + s.get(Stat.ARMOR_PCT));
        double incomingMul = StatRules.incomingDamageMultiplier(armorPct);
        int dodgePct = StatRules.clampDodgePct(b.baseDodgePct + s.get(Stat.DODGE_PCT));
        double hpRegen = StatRules.effectiveHpRegenPerSec(s.get(Stat.HP_REGEN_PS));

        // Offensiv (Ranged aktiv)
        int critChance = StatRules.clampCritChancePct(b.baseCritChancePct + s.get(Stat.CRIT_CHANCE_PCT));
        int extraCritDmg = b.baseCritDamageExtraPct + s.get(Stat.CRIT_DAMAGE_PCT);
        double critMul = StatRules.critMultiplier(extraCritDmg);

        double asMul = StatRules.attackSpeedMultiplier(s.get(Stat.ATTACK_SPEED_PCT));
        double fireRate = Math.max(0.1, b.baseFireRate * asMul);

        int rangePoints = b.baseRangePoints + s.get(Stat.RANGE_PX);
        int rangePx = StatRules.effectiveRangePx(b.baseRangePx, rangePoints);

        double projSpeedMul = b.baseProjectileSpeedMul * StatRules.projectileSpeedMultiplier(s.get(Stat.PROJECTILE_SPEED_PCT));
        double projSizeMul  = b.baseProjectileSizeMul  * StatRules.projectileSizeMultiplier(s.get(Stat.PROJECTILE_SIZE_PCT));

        int multishot = b.baseMultishot + StatRules.multishotFlat(s.get(Stat.MULTISHOT_FLAT));
        int pierce    = b.basePierce + StatRules.pierceFlat(s.get(Stat.PIERCE_FLAT));

        double homingChance01 = StatRules.homingChance01(b.baseHomingChancePct + s.get(Stat.HOMING_CHANCE_PCT));
        double homingStrength = StatRules.homingStrengthMultiplier(b.baseHomingStrengthPct + s.get(Stat.HOMING_STRENGTH_PCT));

        // Damage-Multi (global + ranged)
        int dmgPct = b.baseDamagePct + s.get(Stat.DAMAGE_PCT);
        int rgdPct = b.baseRangedPct + s.get(Stat.RANGED_PCT);
        double rangedMul = Math.max(1.0, StatRules.damageMultiplierPct(dmgPct, rgdPct));

        // Movement / Sustain / Meta
        double moveMul = b.baseMoveSpeedMul * StatRules.moveSpeedMultiplier(s.get(Stat.MOVE_SPEED_PCT));
        double lifesteal = Math.max(b.baseLifestealFrac, StatRules.lifestealFraction(s.get(Stat.LIFESTEAL_PCT)));
        int luck = StatRules.effectiveLuck(b.baseLuck + s.get(Stat.LUCK_FLAT));
        int harvesting = StatRules.effectiveHarvesting(b.baseHarvesting + s.get(Stat.HARVESTING_FLAT));
        double bossMul = StatRules.bossDamageMultiplier(s.get(Stat.BOSS_DAMAGE_PCT));

        return new EffectiveStats(
                maxHp, incomingMul, dodgePct, hpRegen,
                rangedMul, critChance, critMul,
                fireRate, rangePx, projSpeedMul, projSizeMul,
                multishot, pierce, homingChance01, homingStrength,
                moveMul, lifesteal, luck, harvesting, bossMul
        );
    }
}
