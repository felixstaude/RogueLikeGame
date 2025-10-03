// src/main/java/de/felixstaude/roguelike/items/PassiveItemCatalog.java
package de.felixstaude.roguelike.items;

import de.felixstaude.roguelike.stats.Stat;

import java.util.ArrayList;
import java.util.List;

/**
 * Flux Core Themed Passive Items.
 *
 * Tiers:
 * - LEGENDARY: 60-80g (neu)
 * - EPIC:      35-45g
 * - RARE:      20-28g
 * - UNCOMMON:  12-16g
 * - COMMON:    8-10g
 */
public final class PassiveItemCatalog {
    private PassiveItemCatalog(){}

    public static List<Item> all() {
        List<Item> list = new ArrayList<>();

        // =====================================================================
        // TIER: LEGENDARY (60-80g) - Mächtige Items mit hohem Instability-Risk
        // =====================================================================

        list.add(new Item("u_flux_singularity", "Flux Singularity", ItemRarity.LEGENDARY, true, 75, List.of(
                new Mod(Stat.DAMAGE_PCT, 80),
                new Mod(Stat.FLUX_INSTABILITY_PCT, 60),
                new Mod(Stat.HOMING_CHANCE_PCT, 40),
                new Mod(Stat.HOMING_STRENGTH_PCT, 100)
                // TODO: Bullets werden zu schwarzen Löchern (0.5s pull) - spätere Implementation
        )));

        list.add(new Item("u_perfect_stabilizer", "Perfect Stabilizer", ItemRarity.LEGENDARY, true, 70, List.of(
                new Mod(Stat.FLUX_STABILITY_FLAT, 50),
                new Mod(Stat.ARMOR_PCT, 30),
                new Mod(Stat.HP_REGEN_PS, 3)
                // TODO: Immune zu Instability-Effekten für 3s nach Hit - spätere Implementation
        )));

        // =====================================================================
        // TIER: EPIC (35-45g) - Starke Items mit signifikantem Instability-Einfluss
        // =====================================================================

        // Overcharge Core (war Glass Cannon)
        list.add(new Item("u_overcharge_core", "Overcharge Core", ItemRarity.EPIC, true, 40, List.of(
                new Mod(Stat.DAMAGE_PCT, 50),
                new Mod(Stat.CORE_OVERCHARGE_PCT, 30),
                new Mod(Stat.FLUX_INSTABILITY_PCT, 35),
                new Mod(Stat.ARMOR_PCT, -15)
        )));

        // Fractal Splitter (war Quad Split)
        list.add(new Item("u_fractal_splitter", "Fractal Splitter", ItemRarity.EPIC, true, 45, List.of(
                new Mod(Stat.MULTISHOT_FLAT, 4),
                new Mod(Stat.FLUX_INSTABILITY_PCT, 45),
                new Mod(Stat.RANGE_PX, -20),
                new Mod(Stat.ATTACK_SPEED_PCT, -10)
        )));

        // Resonance Amplifier (war Lucky Charm)
        list.add(new Item("u_resonance_amplifier", "Resonance Amplifier", ItemRarity.EPIC, true, 42, List.of(
                new Mod(Stat.CORE_OVERCHARGE_PCT, 50),
                new Mod(Stat.LUCK_FLAT, 20),
                new Mod(Stat.FLUX_INSTABILITY_PCT, 25)
                // Damage = Base × (1 + Instability/100) wird durch CORE_OVERCHARGE_PCT + Instability erreicht
        )));

        // =====================================================================
        // TIER: RARE (20-28g) - Mittelstarke Items mit Flux-Thema
        // =====================================================================

        // Entropy Siphon (war Homing Core)
        list.add(new Item("entropy_siphon", "Entropy Siphon", ItemRarity.RARE, false, 26, List.of(
                new Mod(Stat.HOMING_CHANCE_PCT, 20),
                new Mod(Stat.HOMING_STRENGTH_PCT, 50),
                new Mod(Stat.LIFESTEAL_PCT, 6),
                new Mod(Stat.FLUX_INSTABILITY_PCT, 15)
                // TODO: Homing-Bullets stehlen 2 HP pro Hit (zusätzlich zu Lifesteal) - spätere Implementation
        )));

        // Phase Shifter (war Piercing Crown)
        list.add(new Item("phase_shifter", "Phase Shifter", ItemRarity.RARE, false, 24, List.of(
                new Mod(Stat.PIERCE_FLAT, 3),
                new Mod(Stat.FLUX_INSTABILITY_PCT, 20),
                new Mod(Stat.CRIT_CHANCE_PCT, -8)
                // TODO: Durchbohrte Gegner explodieren - spätere Implementation
        )));

        // Flux Capacitor (war Boss Bane)
        list.add(new Item("flux_capacitor", "Flux Capacitor", ItemRarity.RARE, false, 28, List.of(
                new Mod(Stat.BOSS_DAMAGE_PCT, 30),
                new Mod(Stat.FLUX_INSTABILITY_PCT, 10)
                // TODO: Speichert Instability für Burst-Release (Q-Taste) - spätere Implementation
        )));

        // =====================================================================
        // TIER: UNCOMMON (12-16g) - Kleine Boosts mit Flux-Integration
        // =====================================================================

        // Cooling Matrix (war Sturdy Vest)
        list.add(new Item("cooling_matrix", "Cooling Matrix", ItemRarity.UNCOMMON, false, 14, List.of(
                new Mod(Stat.FLUX_STABILITY_FLAT, 15),
                new Mod(Stat.HP_REGEN_PS, 2),
                new Mod(Stat.MAX_HP, 15)
        )));

        // Energy Cell (war Adrenaline)
        list.add(new Item("energy_cell", "Energy Cell", ItemRarity.UNCOMMON, false, 16, List.of(
                new Mod(Stat.ATTACK_SPEED_PCT, 10),
                new Mod(Stat.FLUX_INSTABILITY_PCT, 5)
                // TODO: +5% Instability pro Kill (decays) - spätere Implementation
        )));

        // Flux Conduit (war Bargain Hunter)
        list.add(new Item("flux_conduit", "Flux Conduit", ItemRarity.UNCOMMON, false, 12, List.of(
                new Mod(Stat.DAMAGE_PCT, 15),
                new Mod(Stat.FLUX_INSTABILITY_PCT, 10)
                // TODO: Konvertiert 50% Schaden zu Energie-DOT - spätere Implementation
                // Shop-Discount-Mechanik wird entfernt
        )));

        // =====================================================================
        // TIER: COMMON (8-10g) - Basis-Stats ohne/mit minimaler Instability
        // =====================================================================

        list.add(new Item("stable_core_fragment", "Stable Core Fragment", ItemRarity.COMMON, false, 8, List.of(
                new Mod(Stat.MAX_HP, 20),
                new Mod(Stat.ARMOR_PCT, 5)
        )));

        list.add(new Item("energy_lens", "Energy Lens", ItemRarity.COMMON, false, 10, List.of(
                new Mod(Stat.RANGE_PX, 15),
                new Mod(Stat.PROJECTILE_SPEED_PCT, 10)
        )));

        list.add(new Item("flux_focusing_crystal", "Flux Focusing Crystal", ItemRarity.COMMON, false, 9, List.of(
                new Mod(Stat.CRIT_CHANCE_PCT, 8),
                new Mod(Stat.CRIT_DAMAGE_PCT, 12)
        )));

        list.add(new Item("nano_repair_kit", "Nano Repair Kit", ItemRarity.COMMON, false, 8, List.of(
                new Mod(Stat.HP_REGEN_PS, 1),
                new Mod(Stat.DODGE_PCT, 3)
        )));

        return list;
    }
}
