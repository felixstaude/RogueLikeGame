// src/main/java/de/felixstaude/roguelike/items/Item.java
package de.felixstaude.roguelike.items;

import de.felixstaude.roguelike.stats.Stats;

import java.util.List;

/** Passives Shop-Item (kein Slot, keine Kombi). Unique = nur 1x pro Run. */
public class Item {
    public final String id;
    public final String name;
    public final ItemRarity rarity;
    public final boolean unique;
    public final int price;         // fixer Preis
    public final List<Mod> mods;    // Buffs/Debuffs

    public Item(String id, String name, ItemRarity rarity, boolean unique, int price, List<Mod> mods) {
        this.id = id;
        this.name = name;
        this.rarity = rarity;
        this.unique = unique;
        this.price = price;
        this.mods = List.copyOf(mods);
    }

    /** Wendet alle Mods auf die Ziel-Stats an. */
    public void applyTo(Stats stats) {
        for (Mod m : mods) stats.add(m.stat(), m.amount());
    }
}
