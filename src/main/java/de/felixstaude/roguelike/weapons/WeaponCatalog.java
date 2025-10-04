// src/main/java/de/felixstaude/roguelike/weapons/WeaponCatalog.java
package de.felixstaude.roguelike.weapons;

import de.felixstaude.roguelike.items.ItemRarity;
import de.felixstaude.roguelike.items.Mod;
import de.felixstaude.roguelike.stats.Stat;

import java.util.EnumMap;
import java.util.Map;
import java.util.List;

/**
 * Flux Core Weapon Definitions.
 * All weapons add FLUX_INSTABILITY_PCT at higher tiers to reflect power corruption.
 */
public final class WeaponCatalog {
    private WeaponCatalog(){}

    private static final Map<WeaponType, WeaponDef> defs = new EnumMap<>(WeaponType.class);

    static {
        // PULSE CORE (was PISTOL) – Balanced allrounder with precision
        defs.put(WeaponType.PULSE_CORE, new WeaponDef(WeaponType.PULSE_CORE, "Pulse Core", ItemRarity.COMMON)
                .tier(WeaponTier.COMMON,   10, List.of(
                        new Mod(Stat.RANGED_PCT, 6),
                        new Mod(Stat.ATTACK_SPEED_PCT, 4)))
                .tier(WeaponTier.UNCOMMON, 18, List.of(
                        new Mod(Stat.RANGED_PCT, 12),
                        new Mod(Stat.ATTACK_SPEED_PCT, 8),
                        new Mod(Stat.FLUX_INSTABILITY_PCT, 2)))
                .tier(WeaponTier.RARE,     30, List.of(
                        new Mod(Stat.RANGED_PCT, 18),
                        new Mod(Stat.ATTACK_SPEED_PCT, 12),
                        new Mod(Stat.CRIT_CHANCE_PCT, 5),
                        new Mod(Stat.FLUX_INSTABILITY_PCT, 5)))
                .tier(WeaponTier.EPIC,     48, List.of(
                        new Mod(Stat.RANGED_PCT, 26),
                        new Mod(Stat.ATTACK_SPEED_PCT, 16),
                        new Mod(Stat.CRIT_CHANCE_PCT, 10),
                        new Mod(Stat.FLUX_INSTABILITY_PCT, 8)))
        );

        // FLUX STREAM (was SMG) – Continuous energy beam, very fast
        defs.put(WeaponType.FLUX_STREAM, new WeaponDef(WeaponType.FLUX_STREAM, "Flux Stream", ItemRarity.UNCOMMON)
                .tier(WeaponTier.COMMON,   12, List.of(
                        new Mod(Stat.ATTACK_SPEED_PCT, 10),
                        new Mod(Stat.DAMAGE_PCT, -5)))
                .tier(WeaponTier.UNCOMMON, 20, List.of(
                        new Mod(Stat.ATTACK_SPEED_PCT, 18),
                        new Mod(Stat.DAMAGE_PCT, -5),
                        new Mod(Stat.FLUX_INSTABILITY_PCT, 3)))
                .tier(WeaponTier.RARE,     34, List.of(
                        new Mod(Stat.ATTACK_SPEED_PCT, 26),
                        new Mod(Stat.DAMAGE_PCT, 5),
                        new Mod(Stat.FLUX_INSTABILITY_PCT, 6)))
                .tier(WeaponTier.EPIC,     52, List.of(
                        new Mod(Stat.ATTACK_SPEED_PCT, 36),
                        new Mod(Stat.DAMAGE_PCT, 12),
                        new Mod(Stat.FLUX_INSTABILITY_PCT, 10)))
        );

        // WAVE BURST (was SHOTGUN) – Energy wave with multishot and knockback
        defs.put(WeaponType.WAVE_BURST, new WeaponDef(WeaponType.WAVE_BURST, "Wave Burst", ItemRarity.RARE)
                .tier(WeaponTier.COMMON,   14, List.of(
                        new Mod(Stat.MULTISHOT_FLAT, 1),
                        new Mod(Stat.RANGE_PX, -8),
                        new Mod(Stat.KNOCKBACK_PCT, 10)))
                .tier(WeaponTier.UNCOMMON, 22, List.of(
                        new Mod(Stat.MULTISHOT_FLAT, 2),
                        new Mod(Stat.RANGE_PX, -12),
                        new Mod(Stat.KNOCKBACK_PCT, 15),
                        new Mod(Stat.FLUX_INSTABILITY_PCT, 4)))
                .tier(WeaponTier.RARE,     36, List.of(
                        new Mod(Stat.MULTISHOT_FLAT, 3),
                        new Mod(Stat.RANGE_PX, -16),
                        new Mod(Stat.DAMAGE_PCT, 8),
                        new Mod(Stat.KNOCKBACK_PCT, 20),
                        new Mod(Stat.FLUX_INSTABILITY_PCT, 8)))
                .tier(WeaponTier.EPIC,     56, List.of(
                        new Mod(Stat.MULTISHOT_FLAT, 4),
                        new Mod(Stat.RANGE_PX, -20),
                        new Mod(Stat.DAMAGE_PCT, 16),
                        new Mod(Stat.KNOCKBACK_PCT, 30),
                        new Mod(Stat.FLUX_INSTABILITY_PCT, 12)))
        );

        // PHASE LANCE (was CROSSBOW) – Pierces everything, leaves energy trail
        defs.put(WeaponType.PHASE_LANCE, new WeaponDef(WeaponType.PHASE_LANCE, "Phase Lance", ItemRarity.RARE)
                .tier(WeaponTier.COMMON,   14, List.of(
                        new Mod(Stat.PIERCE_FLAT, 1),
                        new Mod(Stat.ATTACK_SPEED_PCT, -6),
                        new Mod(Stat.DAMAGE_PCT, 6)))
                .tier(WeaponTier.UNCOMMON, 22, List.of(
                        new Mod(Stat.PIERCE_FLAT, 2),
                        new Mod(Stat.ATTACK_SPEED_PCT, -6),
                        new Mod(Stat.DAMAGE_PCT, 12),
                        new Mod(Stat.FLUX_INSTABILITY_PCT, 4)))
                .tier(WeaponTier.RARE,     36, List.of(
                        new Mod(Stat.PIERCE_FLAT, 3),
                        new Mod(Stat.ATTACK_SPEED_PCT, -8),
                        new Mod(Stat.DAMAGE_PCT, 20),
                        new Mod(Stat.FLUX_INSTABILITY_PCT, 7)))
                .tier(WeaponTier.EPIC,     56, List.of(
                        new Mod(Stat.PIERCE_FLAT, 4),
                        new Mod(Stat.ATTACK_SPEED_PCT, -10),
                        new Mod(Stat.DAMAGE_PCT, 28),
                        new Mod(Stat.FLUX_INSTABILITY_PCT, 11)))
        );

        // SINGULARITY CANNON (was RAILGUN) – Pulls enemies before impact
        defs.put(WeaponType.SINGULARITY_CANNON, new WeaponDef(WeaponType.SINGULARITY_CANNON, "Singularity Cannon", ItemRarity.EPIC)
                .tier(WeaponTier.COMMON,   16, List.of(
                        new Mod(Stat.PIERCE_FLAT, 1),
                        new Mod(Stat.RANGE_PX, 10),
                        new Mod(Stat.ATTACK_SPEED_PCT, -8)))
                .tier(WeaponTier.UNCOMMON, 26, List.of(
                        new Mod(Stat.PIERCE_FLAT, 2),
                        new Mod(Stat.RANGE_PX, 16),
                        new Mod(Stat.ATTACK_SPEED_PCT, -10),
                        new Mod(Stat.FLUX_INSTABILITY_PCT, 5)))
                .tier(WeaponTier.RARE,     40, List.of(
                        new Mod(Stat.PIERCE_FLAT, 3),
                        new Mod(Stat.RANGE_PX, 20),
                        new Mod(Stat.ATTACK_SPEED_PCT, -12),
                        new Mod(Stat.FLUX_INSTABILITY_PCT, 9)))
                .tier(WeaponTier.EPIC,     60, List.of(
                        new Mod(Stat.PIERCE_FLAT, 4),
                        new Mod(Stat.RANGE_PX, 28),
                        new Mod(Stat.ATTACK_SPEED_PCT, -14),
                        new Mod(Stat.RANGED_PCT, 12),
                        new Mod(Stat.FLUX_INSTABILITY_PCT, 14)))
        );

        // ENTROPY FIELD (was ARC_WAND) – Energy chains between enemies
        defs.put(WeaponType.ENTROPY_FIELD, new WeaponDef(WeaponType.ENTROPY_FIELD, "Entropy Field", ItemRarity.UNCOMMON)
                .tier(WeaponTier.COMMON,   12, List.of(
                        new Mod(Stat.HOMING_CHANCE_PCT, 8),
                        new Mod(Stat.MAGIC_PCT, 6),
                        new Mod(Stat.PIERCE_FLAT, -1)))
                .tier(WeaponTier.UNCOMMON, 20, List.of(
                        new Mod(Stat.HOMING_CHANCE_PCT, 12),
                        new Mod(Stat.HOMING_STRENGTH_PCT, 50),
                        new Mod(Stat.MAGIC_PCT, 10),
                        new Mod(Stat.FLUX_INSTABILITY_PCT, 3)))
                .tier(WeaponTier.RARE,     34, List.of(
                        new Mod(Stat.HOMING_CHANCE_PCT, 18),
                        new Mod(Stat.HOMING_STRENGTH_PCT, 70),
                        new Mod(Stat.MAGIC_PCT, 16),
                        new Mod(Stat.FLUX_INSTABILITY_PCT, 7)))
                .tier(WeaponTier.EPIC,     52, List.of(
                        new Mod(Stat.HOMING_CHANCE_PCT, 25),
                        new Mod(Stat.HOMING_STRENGTH_PCT, 100),
                        new Mod(Stat.MAGIC_PCT, 24),
                        new Mod(Stat.FLUX_INSTABILITY_PCT, 12)))
        );
    }

    public static WeaponDef get(WeaponType type){ return defs.get(type); }
    public static Map<WeaponType, WeaponDef> all(){ return defs; }
}
