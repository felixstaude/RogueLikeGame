// src/main/java/de/felixstaude/roguelike/weapons/WeaponCatalog.java
package de.felixstaude.roguelike.weapons;

import de.felixstaude.roguelike.items.ItemRarity;
import de.felixstaude.roguelike.items.Mod;
import de.felixstaude.roguelike.stats.Stat;

import java.util.EnumMap;
import java.util.Map;
import java.util.List;

/** Globale Registrierung der Waffen-Definitionen. */
public final class WeaponCatalog {
    private WeaponCatalog(){}

    private static final Map<WeaponType, WeaponDef> defs = new EnumMap<>(WeaponType.class);

    static {
        // PISTOL – Allrounder
        defs.put(WeaponType.PISTOL, new WeaponDef(WeaponType.PISTOL, "Pistol", ItemRarity.COMMON)
                .tier(WeaponTier.COMMON,   10, List.of(
                        new Mod(Stat.RANGED_PCT, 6), new Mod(Stat.ATTACK_SPEED_PCT, 4)))
                .tier(WeaponTier.UNCOMMON, 18, List.of(
                        new Mod(Stat.RANGED_PCT, 12), new Mod(Stat.ATTACK_SPEED_PCT, 8)))
                .tier(WeaponTier.RARE,     30, List.of(
                        new Mod(Stat.RANGED_PCT, 18), new Mod(Stat.ATTACK_SPEED_PCT, 12), new Mod(Stat.CRIT_CHANCE_PCT, 5)))
                .tier(WeaponTier.EPIC,     48, List.of(
                        new Mod(Stat.RANGED_PCT, 26), new Mod(Stat.ATTACK_SPEED_PCT, 16), new Mod(Stat.CRIT_CHANCE_PCT, 10)))
        );

        // SMG – sehr schnell, weniger Damage
        defs.put(WeaponType.SMG, new WeaponDef(WeaponType.SMG, "SMG", ItemRarity.UNCOMMON)
                .tier(WeaponTier.COMMON,   12, List.of(
                        new Mod(Stat.ATTACK_SPEED_PCT, 10), new Mod(Stat.DAMAGE_PCT, -5)))
                .tier(WeaponTier.UNCOMMON, 20, List.of(
                        new Mod(Stat.ATTACK_SPEED_PCT, 18), new Mod(Stat.DAMAGE_PCT, -5)))
                .tier(WeaponTier.RARE,     34, List.of(
                        new Mod(Stat.ATTACK_SPEED_PCT, 26), new Mod(Stat.DAMAGE_PCT, 5)))
                .tier(WeaponTier.EPIC,     52, List.of(
                        new Mod(Stat.ATTACK_SPEED_PCT, 36), new Mod(Stat.DAMAGE_PCT, 12)))
        );

        // SHOTGUN – Multishot, kürzere Range
        defs.put(WeaponType.SHOTGUN, new WeaponDef(WeaponType.SHOTGUN, "Shotgun", ItemRarity.RARE)
                .tier(WeaponTier.COMMON,   14, List.of(
                        new Mod(Stat.MULTISHOT_FLAT, 1), new Mod(Stat.RANGE_PX, -8)))
                .tier(WeaponTier.UNCOMMON, 22, List.of(
                        new Mod(Stat.MULTISHOT_FLAT, 2), new Mod(Stat.RANGE_PX, -12)))
                .tier(WeaponTier.RARE,     36, List.of(
                        new Mod(Stat.MULTISHOT_FLAT, 3), new Mod(Stat.RANGE_PX, -16), new Mod(Stat.DAMAGE_PCT, 8)))
                .tier(WeaponTier.EPIC,     56, List.of(
                        new Mod(Stat.MULTISHOT_FLAT, 4), new Mod(Stat.RANGE_PX, -20), new Mod(Stat.DAMAGE_PCT, 16)))
        );

        // CROSSBOW – langsamer, viel Pierce/Damage
        defs.put(WeaponType.CROSSBOW, new WeaponDef(WeaponType.CROSSBOW, "Crossbow", ItemRarity.RARE)
                .tier(WeaponTier.COMMON,   14, List.of(
                        new Mod(Stat.PIERCE_FLAT, 1), new Mod(Stat.ATTACK_SPEED_PCT, -6), new Mod(Stat.DAMAGE_PCT, 6)))
                .tier(WeaponTier.UNCOMMON, 22, List.of(
                        new Mod(Stat.PIERCE_FLAT, 2), new Mod(Stat.ATTACK_SPEED_PCT, -6), new Mod(Stat.DAMAGE_PCT, 12)))
                .tier(WeaponTier.RARE,     36, List.of(
                        new Mod(Stat.PIERCE_FLAT, 3), new Mod(Stat.ATTACK_SPEED_PCT, -8), new Mod(Stat.DAMAGE_PCT, 20)))
                .tier(WeaponTier.EPIC,     56, List.of(
                        new Mod(Stat.PIERCE_FLAT, 4), new Mod(Stat.ATTACK_SPEED_PCT, -10), new Mod(Stat.DAMAGE_PCT, 28)))
        );

        // RAILGUN – Range/Speed/Pierce, weniger AS
        defs.put(WeaponType.RAILGUN, new WeaponDef(WeaponType.RAILGUN, "Railgun", ItemRarity.EPIC)
                .tier(WeaponTier.COMMON,   16, List.of(
                        new Mod(Stat.PIERCE_FLAT, 1), new Mod(Stat.RANGE_PX, 10), new Mod(Stat.ATTACK_SPEED_PCT, -8)))
                .tier(WeaponTier.UNCOMMON, 26, List.of(
                        new Mod(Stat.PIERCE_FLAT, 2), new Mod(Stat.RANGE_PX, 16), new Mod(Stat.ATTACK_SPEED_PCT, -10)))
                .tier(WeaponTier.RARE,     40, List.of(
                        new Mod(Stat.PIERCE_FLAT, 3), new Mod(Stat.RANGE_PX, 20), new Mod(Stat.ATTACK_SPEED_PCT, -12)))
                .tier(WeaponTier.EPIC,     60, List.of(
                        new Mod(Stat.PIERCE_FLAT, 4), new Mod(Stat.RANGE_PX, 28), new Mod(Stat.ATTACK_SPEED_PCT, -14), new Mod(Stat.RANGED_PCT, 12)))
        );

        // ARC WAND – Homing/Magic
        defs.put(WeaponType.ARC_WAND, new WeaponDef(WeaponType.ARC_WAND, "Arc Wand", ItemRarity.UNCOMMON)
                .tier(WeaponTier.COMMON,   12, List.of(
                        new Mod(Stat.HOMING_CHANCE_PCT, 8), new Mod(Stat.MAGIC_PCT, 6), new Mod(Stat.PIERCE_FLAT, -1)))
                .tier(WeaponTier.UNCOMMON, 20, List.of(
                        new Mod(Stat.HOMING_CHANCE_PCT, 12), new Mod(Stat.HOMING_STRENGTH_PCT, 50), new Mod(Stat.MAGIC_PCT, 10)))
                .tier(WeaponTier.RARE,     34, List.of(
                        new Mod(Stat.HOMING_CHANCE_PCT, 18), new Mod(Stat.HOMING_STRENGTH_PCT, 70), new Mod(Stat.MAGIC_PCT, 16)))
                .tier(WeaponTier.EPIC,     52, List.of(
                        new Mod(Stat.HOMING_CHANCE_PCT, 25), new Mod(Stat.HOMING_STRENGTH_PCT, 100), new Mod(Stat.MAGIC_PCT, 24)))
        );
    }

    public static WeaponDef get(WeaponType type){ return defs.get(type); }
    public static Map<WeaponType, WeaponDef> all(){ return defs; }
}
