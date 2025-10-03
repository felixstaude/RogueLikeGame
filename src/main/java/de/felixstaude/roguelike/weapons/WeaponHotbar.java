// src/main/java/de/felixstaude/roguelike/weapons/WeaponHotbar.java
package de.felixstaude.roguelike.weapons;

import de.felixstaude.roguelike.items.Mod;
import de.felixstaude.roguelike.stats.Stats;

import java.util.ArrayList;
import java.util.List;

/** 4-Slot-Hotbar; auto-combine bei zwei gleichen Typ+Tier. */
public class WeaponHotbar {
    private final int capacity = 4;
    private final List<WeaponInstance> slots = new ArrayList<>();

    public List<WeaponInstance> getSlots(){ return slots; }
    public int size(){ return slots.size(); }
    public int capacity(){ return capacity; }

    /** Summiert Mods aller Waffen in eine neue Stats-Instanz. */
    public Stats toStats() {
        Stats s = new Stats();
        for (WeaponInstance w : slots) {
            for (Mod m : w.mods()) s.add(m.stat(), m.amount());
        }
        return s;
    }

    /** Fügt Waffe hinzu; wenn bereits eine gleiche (Typ+Tier) existiert, kombiniere zu nächstem Tier. */
    public Result tryAddOrCombine(WeaponInstance in) {
        // 1) versuche combine chain (auch mehrfach, falls nach Upgrade erneut kombinierbar)
        while (true) {
            int idx = indexOfSameTypeAndTier(in);
            if (idx >= 0) {
                // remove bestehende, upgrade Eingabe
                slots.remove(idx);
                in = new WeaponInstance(in.def, in.tier.next());
                if (in.tier == WeaponTier.EPIC) break; // mehr geht nicht
                // loop weiter: evtl. gibt's noch eine gleiche zum Kombinieren
                continue;
            }
            break;
        }
        // 2) einfügen, falls Platz
        if (slots.size() < capacity) {
            slots.add(in);
            return new Result(true, in, true, false);
        }
        // 3) kein Platz und keine Kombi möglich
        return new Result(false, in, false, false);
    }

    private int indexOfSameTypeAndTier(WeaponInstance w) {
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).sameTypeAndTier(w)) return i;
        }
        return -1;
    }

    public WeaponInstance remove(int index) {
        if (index < 0 || index >= slots.size()) {
            return null;
        }
        return slots.remove(index);
    }

    public record Result(boolean success, WeaponInstance finalWeapon, boolean added, boolean combined) {}
}
