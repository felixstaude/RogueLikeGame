package de.felixstaude.roguelike.stats;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Flux Core Instability-System: Definiert Effekte für verschiedene Instability-Tiers.
 *
 * Tier 1 (0-25%):   Stabil - keine Effekte
 * Tier 2 (26-50%):  Flux Leak - 10% Chance auf Random-Bullet
 * Tier 3 (51-75%):  Core Fracture - 20% Bullet-Split, -15% Max HP
 * Tier 4 (76-99%):  Critical Mass - +50% Damage, 2 HP/s Drain
 * Tier 5 (100%):    Meltdown - Explosion-Aura (100 dmg/s), 5 HP/s Drain
 */
public final class InstabilityEffects {

    private InstabilityEffects() {}

    /**
     * Instability-Tier bestimmen (1-5).
     */
    public static int getTier(int instabilityPct) {
        if (instabilityPct >= 100) return 5; // Meltdown
        if (instabilityPct >= 76)  return 4; // Critical Mass
        if (instabilityPct >= 51)  return 3; // Core Fracture
        if (instabilityPct >= 26)  return 2; // Flux Leak
        return 1; // Stable
    }

    /**
     * Tier-Name für UI.
     */
    public static String getTierName(int tier) {
        return switch (tier) {
            case 5 -> "MELTDOWN";
            case 4 -> "Critical Mass";
            case 3 -> "Core Fracture";
            case 2 -> "Flux Leak";
            default -> "Stable";
        };
    }

    // --- Tier 2: Flux Leak (26-50%) ---

    /**
     * Flux Leak: 10% Chance auf Random-Bullet beim Schießen.
     */
    public static boolean shouldSpawnRandomBullet(int instabilityPct) {
        if (getTier(instabilityPct) < 2) return false;
        return ThreadLocalRandom.current().nextDouble() < 0.10;
    }

    // --- Tier 3: Core Fracture (51-75%) ---

    /**
     * Core Fracture: 20% Chance dass Bullets splitten.
     */
    public static boolean shouldSplitBullet(int instabilityPct) {
        if (getTier(instabilityPct) < 3) return false;
        return ThreadLocalRandom.current().nextDouble() < 0.20;
    }

    /**
     * Core Fracture: -15% Max HP Penalty.
     */
    public static double getMaxHpMultiplier(int instabilityPct) {
        if (getTier(instabilityPct) >= 3) {
            return 0.85; // -15%
        }
        return 1.0;
    }

    // --- Tier 4: Critical Mass (76-99%) ---

    /**
     * Critical Mass: +50% Damage Bonus.
     */
    public static double getDamageMultiplier(int instabilityPct) {
        if (getTier(instabilityPct) >= 4 && instabilityPct < 100) {
            return 1.50; // +50%
        }
        return 1.0;
    }

    /**
     * Critical Mass: 2 HP/s Drain.
     */
    public static double getHpDrainPerSecond(int instabilityPct) {
        int tier = getTier(instabilityPct);
        if (tier == 5) return 5.0;  // Meltdown: 5 HP/s
        if (tier == 4) return 2.0;  // Critical Mass: 2 HP/s
        return 0.0;
    }

    // --- Tier 5: Meltdown (100%) ---

    /**
     * Meltdown: Explosion-Aura aktiv?
     */
    public static boolean hasMeltdownAura(int instabilityPct) {
        return getTier(instabilityPct) == 5;
    }

    /**
     * Meltdown: Aura Damage (100 dmg/s).
     */
    public static double getMeltdownAuraDamage() {
        return 100.0;
    }

    /**
     * Meltdown: Aura Radius in Pixeln.
     */
    public static double getMeltdownAuraRadius() {
        return 150.0;
    }

    // --- Visuelle Effekte ---

    /**
     * Glitch-Intensität für Rendering (0.0 - 1.0).
     */
    public static double getGlitchIntensity(int instabilityPct) {
        int tier = getTier(instabilityPct);
        return switch (tier) {
            case 5 -> 1.0;   // Maximum Glitch
            case 4 -> 0.6;   // Starker Glitch
            case 3 -> 0.3;   // Mittlerer Glitch
            case 2 -> 0.1;   // Leichter Glitch
            default -> 0.0;  // Kein Glitch
        };
    }

    /**
     * Glow-Puls-Geschwindigkeit basierend auf Instability.
     */
    public static double getGlowPulseSpeed(int instabilityPct) {
        return 1.0 + (instabilityPct / 50.0); // 1.0x bei 0%, 3.0x bei 100%
    }

    /**
     * Energie-Ring-Rotation-Geschwindigkeit.
     */
    public static double getRingRotationSpeed(int instabilityPct) {
        return 0.5 + (instabilityPct / 100.0); // 0.5x bei 0%, 1.5x bei 100%
    }
}
