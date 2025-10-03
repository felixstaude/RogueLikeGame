// src/main/java/de/felixstaude/roguelike/items/PassiveItemCatalog.java
package de.felixstaude.roguelike.items;

import de.felixstaude.roguelike.stats.Stat;

import java.util.ArrayList;
import java.util.List;

/** Vordefinierte passive Items für den Shop. */
public final class PassiveItemCatalog {
    private PassiveItemCatalog(){}

    public static List<Item> all() {
        List<Item> list = new ArrayList<>();

        // Unique: Glass Cannon (ohne MaxHP-Down, nur Armor-Down wie gewünscht)
        list.add(new Item("u_glass_cannon", "Glass Cannon", ItemRarity.RARE, true, 32, List.of(
                new Mod(Stat.DAMAGE_PCT, 40),
                new Mod(Stat.ARMOR_PCT, -20)
        )));

        // Unique: Quad Split
        list.add(new Item("u_quad_split", "Quad Split", ItemRarity.EPIC, true, 54, List.of(
                new Mod(Stat.MULTISHOT_FLAT, 3),
                new Mod(Stat.RANGE_PX, -30),
                new Mod(Stat.ATTACK_SPEED_PCT, -10)
        )));

        // Unique: Lucky Charm
        list.add(new Item("u_lucky_charm", "Lucky Charm", ItemRarity.EPIC, true, 42, List.of(
                new Mod(Stat.LUCK_FLAT, 25)
                // Effekt "Shop-Reroll -2 Gold (min 2)" handeln wir in ShopV2 separat über Flag
        )));

        // Homing Core
        list.add(new Item("homing_core", "Homing Core", ItemRarity.RARE, false, 26, List.of(
                new Mod(Stat.HOMING_CHANCE_PCT, 15),
                new Mod(Stat.HOMING_STRENGTH_PCT, 50),
                new Mod(Stat.PIERCE_FLAT, -1)
        )));

        // Piercing Crown
        list.add(new Item("piercing_crown", "Piercing Crown", ItemRarity.RARE, false, 24, List.of(
                new Mod(Stat.PIERCE_FLAT, 2),
                new Mod(Stat.CRIT_CHANCE_PCT, -10)
        )));

        // Bargain Hunter
        list.add(new Item("bargain_hunter", "Bargain Hunter", ItemRarity.UNCOMMON, false, 18, List.of(
                // Preisreduktion erledigt ShopV2 (Flag) – hier ein Tradeoff:
                new Mod(Stat.HARVESTING_FLAT, -10)
        )));

        // Boss Bane
        list.add(new Item("boss_bane", "Boss Bane", ItemRarity.RARE, false, 28, List.of(
                new Mod(Stat.BOSS_DAMAGE_PCT, 25)
        )));

        // Simple generische Buffs ohne Debuff
        list.add(new Item("sturdy_vest", "Sturdy Vest", ItemRarity.UNCOMMON, false, 20, List.of(
                new Mod(Stat.MAX_HP, 20),
                new Mod(Stat.ARMOR_PCT, 8)
        )));
        list.add(new Item("adrenaline", "Adrenaline", ItemRarity.RARE, false, 26, List.of(
                new Mod(Stat.HP_REGEN_PS, 2),
                new Mod(Stat.ATTACK_SPEED_PCT, 5)
        )));
        list.add(new Item("polished_barrel", "Polished Barrel", ItemRarity.COMMON, false, 12, List.of(
                new Mod(Stat.RANGE_PX, 12)
        )));
        list.add(new Item("keen_eye", "Keen Eye", ItemRarity.UNCOMMON, false, 18, List.of(
                new Mod(Stat.CRIT_CHANCE_PCT, 6),
                new Mod(Stat.CRIT_DAMAGE_PCT, 15)
        )));

        return list;
    }
}
