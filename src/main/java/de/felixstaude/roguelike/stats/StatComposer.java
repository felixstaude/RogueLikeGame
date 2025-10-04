// src/main/java/de/felixstaude/roguelike/stats/StatComposer.java
package de.felixstaude.roguelike.stats;

import de.felixstaude.roguelike.weapons.WeaponHotbar;

/**
 * Kleiner Aggregator: Base-Werte + passive Stats + Waffen-Hotbar
 * -> liefert die EffectiveStats für Player/Engine.
 */
public class StatComposer {
    public final EffectiveStats.Base base = new EffectiveStats.Base();
    public final Stats passives = new Stats();
    public final WeaponHotbar hotbar = new WeaponHotbar();

    /** Kombiniert Passives + Waffen-Mods und mappt mit den Base-Werten. */
    public EffectiveStats compute() {
        Stats total = Stats.sum(passives, hotbar.toStats());
        return EffectiveStats.from(base, total);
    }
}
