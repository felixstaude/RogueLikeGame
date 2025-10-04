// src/main/java/de/felixstaude/roguelike/items/Mod.java
package de.felixstaude.roguelike.items;

import de.felixstaude.roguelike.stats.Stat;

/** Ein einzelner Stat-Modifikator eines Items/Waffen-Tiers. */
public record Mod(Stat stat, int amount) { }
