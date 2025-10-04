package de.felixstaude.roguelike.stats;

/**
 * Alle numerischen Spieler-Stats als additive Integer.
 * Prozent-Stats interpretieren wir als 1 Punkt = 1%.
 * Das Mapping zu effektiven Werten (Caps, Floors, "negativ=ignorieren") macht StatRules/EffectiveStats.
 */
public enum Stat {
    // Defensiv
    MAX_HP,             // +1 = +1 HP (effektiv nie < 1)
    ARMOR_PCT,          // +1 = +1% weniger Schaden, -1 = +1% mehr Schaden (cap ±80%)
    DODGE_PCT,          // 0..60% Hardcap
    HP_REGEN_PS,        // +1 = +1 HP pro Sekunde (negativ -> 0)

    // Offensiv (global & Typen)
    DAMAGE_PCT,         // globaler Damage-Multi (mind. 1 Damage)
    MELEE_PCT,
    RANGED_PCT,
    MAGIC_PCT,

    CRIT_CHANCE_PCT,    // default 0%; negativ -> 0
    CRIT_DAMAGE_PCT,    // additiv auf 50 % Basis (0 = ×1.5, +50 = ×2.0)

    ATTACK_SPEED_PCT,   // negativ -> 0 (Base bleibt)

    // Waffenhandling
    RANGE_PX,           // +1 = +1 px (wir mappen auf +6 px pro „Range-Punkt“ extern, hier als Rohwert)
    PROJECTILE_SPEED_PCT,
    PROJECTILE_SIZE_PCT,
    MULTISHOT_FLAT,     // +1 = +1 Kugel
    PIERCE_FLAT,        // +1 = +1 Durchschlag
    HOMING_CHANCE_PCT,  // 0..100
    HOMING_STRENGTH_PCT,// skaliert Turnrate
    KNOCKBACK_PCT,      // optional

    // Movement/Ökonomie
    MOVE_SPEED_PCT,     // negativ -> 0
    LIFESTEAL_PCT,      // negativ -> 0
    LUCK_FLAT,          // Beeinflusst Gewichte
    HARVESTING_FLAT,    // Shop-Slots & End-Wave-Belohnungen

    // Boss/Elites
    BOSS_DAMAGE_PCT,    // Bonus gegen Bosse

    // Flux Core System
    FLUX_INSTABILITY_PCT,    // 0-100%, beeinflusst Chaos-Effekte und Damage-Multiplier
    FLUX_STABILITY_FLAT,     // negative Werte reduzieren Instability
    CORE_OVERCHARGE_PCT,     // zusätzlicher Damage-Multiplier basierend auf Instability
}
