// src/main/java/de/felixstaude/roguelike/stats/StatRules.java
package de.felixstaude.roguelike.stats;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Zentrale Regeln, Caps & Helper zur Umrechnung von Roh-Stats in effektive Werte.
 * Prozentwerte interpretieren wir als "1 Punkt = 1%".
 */
public final class StatRules {

    private StatRules() {}

    // --- Caps / Konstanten ---------------------------------------------------

    /** Armor wird hart auf [-80%, +80%] gecappt. */
    public static final int ARMOR_MIN_PCT = -80;
    public static final int ARMOR_MAX_PCT =  80;

    /** Dodge wird hart auf [0%, 60%] gecappt. */
    public static final int DODGE_MAX_PCT = 60;

    /** Crit Chance wird auf [0%, 100%] gecappt. */
    public static final int CRIT_CHANCE_MIN_PCT = 0;
    public static final int CRIT_CHANCE_MAX_PCT = 100;

    /** Basis Crit-Schaden (zusätzlich zum normalen Schaden): +50% => Multiplikator 1.5. */
    public static final int CRIT_DAMAGE_BASE_PCT = 50;

    /** Minimal erlaubte effektive Projektil-Geschwindigkeit/Größe als Faktor (gegen starke Debuffs/Items). */
    public static final double MIN_PROJECTILE_SPEED_MUL = 0.25;
    public static final double MIN_PROJECTILE_SIZE_MUL  = 0.25;

    /** Minimal erlaubte effektive Range in Pixeln. */
    public static final int MIN_RANGE_PX = 250;

    /** Mapping von "Range-Punkten" zu Pixeln (1 Punkt = 6 px als Startwert). */
    public static final int RANGE_POINT_TO_PX = 6;

    // --- Armor / Incoming Damage --------------------------------------------

    /** Clamped Armor in Prozent. */
    public static int clampArmorPct(int armorPct) {
        return Math.max(ARMOR_MIN_PCT, Math.min(ARMOR_MAX_PCT, armorPct));
    }

    /**
     * Multiplier auf eingehenden Schaden aus Armor-Prozent (geclamped).
     * Beispiel: +20% Armor -> 0.8;  -30% Armor -> 1.3
     */
    public static double incomingDamageMultiplier(int armorPct) {
        int a = clampArmorPct(armorPct);
        return 1.0 - (a / 100.0);
    }

    // --- Dodge ---------------------------------------------------------------

    /** Clamped Dodge-Chance in Prozent [0..60]. */
    public static int clampDodgePct(int dodgePct) {
        return Math.max(0, Math.min(DODGE_MAX_PCT, dodgePct));
    }

    /** Führt einen Dodge-Roll durch (ThreadLocalRandom). */
    public static boolean rollDodge(int dodgePct) {
        int d = clampDodgePct(dodgePct);
        if (d <= 0) return false;
        return ThreadLocalRandom.current().nextInt(100) < d;
    }

    // --- Crit ----------------------------------------------------------------

    /** Clamped Crit-Chance in Prozent [0..100]. */
    public static int clampCritChancePct(int critChancePct) {
        return Math.max(CRIT_CHANCE_MIN_PCT, Math.min(CRIT_CHANCE_MAX_PCT, critChancePct));
    }

    /**
     * Crit-Schaden-Multiplikator aus zusätzlichem CritDamage% (additiv auf die Basis +50%).
     * Negatives CritDamage reduziert den Bonus, aber nie unter 1.0 (Crit macht nie weniger als Non-Crit).
     * Beispiel: extra=0 -> 1.5; extra=50 -> 2.0; extra=-30 -> 1.2 (aber min 1.0).
     */
    public static double critMultiplier(int extraCritDamagePct) {
        double bonus = (CRIT_DAMAGE_BASE_PCT + extraCritDamagePct) / 100.0;
        return Math.max(1.0, 1.0 + bonus);
    }

    /** Führt einen Crit-Roll durch und gibt true/false zurück. */
    public static boolean rollCrit(int critChancePct) {
        int c = clampCritChancePct(critChancePct);
        if (c <= 0) return false;
        return ThreadLocalRandom.current().nextInt(100) < c;
    }

    // --- HP / Regeneration ---------------------------------------------------

    /** Effektive MaxHP: nie kleiner als 1. */
    public static int effectiveMaxHp(int baseMaxHp, int bonusMaxHp) {
        return Math.max(1, baseMaxHp + bonusMaxHp);
    }

    /** Effektive HP-Regeneration pro Sekunde: negative Werte werden zu 0. */
    public static double effectiveHpRegenPerSec(int regenPs) {
        return Math.max(0.0, regenPs);
    }

    // --- Geschwindigkeiten / Multiplikatoren --------------------------------

    /** Attack Speed: negative Werte werden ignoriert (Base bleibt). */
    public static double attackSpeedMultiplier(int attackSpeedPct) {
        return 1.0 + Math.max(0, attackSpeedPct) / 100.0;
    }

    /** Movement Speed: negative Werte werden ignoriert (Base bleibt). */
    public static double moveSpeedMultiplier(int moveSpeedPct) {
        return 1.0 + Math.max(0, moveSpeedPct) / 100.0;
    }

    /** Lifesteal: negativer Wert wird zu 0, Rückgabe als [0..] Anteil. */
    public static double lifestealFraction(int lifestealPct) {
        return Math.max(0, lifestealPct) / 100.0;
    }

    /** Projektil-Geschwindigkeit: symmetrisch, aber mit Mindestfaktor. */
    public static double projectileSpeedMultiplier(int projectileSpeedPct) {
        return Math.max(MIN_PROJECTILE_SPEED_MUL, 1.0 + projectileSpeedPct / 100.0);
    }

    /** Projektil-Größe: symmetrisch, aber mit Mindestfaktor. */
    public static double projectileSizeMultiplier(int projectileSizePct) {
        return Math.max(MIN_PROJECTILE_SIZE_MUL, 1.0 + projectileSizePct / 100.0);
    }

    // --- Range / Homing / Pierce / Multishot --------------------------------

    /** Effektive Reichweite in Pixeln aus Basis und Range-Punkten. */
    public static int effectiveRangePx(int baseRangePx, int rangePoints) {
        int addPx = rangePoints * RANGE_POINT_TO_PX;
        return Math.max(MIN_RANGE_PX, baseRangePx + addPx);
    }

    /** Homing-Chance als [0..1]. */
    public static double homingChance01(int homingChancePct) {
        int c = Math.max(0, Math.min(100, homingChancePct));
        return c / 100.0;
    }

    /** Homing-Strength als Faktor (z. B. 1.0 = Base, 1.5 = +50%). Mindest-Faktor 0.2. */
    public static double homingStrengthMultiplier(int homingStrengthPct) {
        return Math.max(0.2, 1.0 + homingStrengthPct / 100.0);
    }

    /** Multishot: negative Werte werden zu 0 gekappt. */
    public static int multishotFlat(int multishot) {
        return Math.max(0, multishot);
    }

    /** Pierce: negative Werte werden zu 0 gekappt. */
    public static int pierceFlat(int pierce) {
        return Math.max(0, pierce);
    }

    // --- Luck / Harvesting / BossDamage -------------------------------------

    /** Luck darf negativ sein (schlechtere Gewichte). */
    public static int effectiveLuck(int luck) { return luck; }

    /** Harvesting darf negativ sein. */
    public static int effectiveHarvesting(int harvesting) { return harvesting; }

    /** Boss-Damage-Multiplikator (darf negativ sein). */
    public static double bossDamageMultiplier(int bossDamagePct) {
        return 1.0 + (bossDamagePct / 100.0);
    }

    // --- Utility -------------------------------------------------------------

    /** Globaler Damage-Multiplikator aus zwei Prozent-Quellen (z. B. global + typed). Mindestens 1 Schaden. */
    public static double damageMultiplierPct(int... percents) {
        double sum = 0.0;
        for (int p : percents) sum += p;
        return Math.max(0.0, 1.0 + sum / 100.0);
    }
}
