// src/main/java/de/felixstaude/roguelike/weapons/WeaponInstance.java
package de.felixstaude.roguelike.weapons;

import de.felixstaude.roguelike.items.Mod;

import java.util.List;

/** Konkrete Waffe im Inventar (Typ + Tier). */
public class WeaponInstance {
    public final WeaponDef def;
    public WeaponTier tier;

    public WeaponInstance(WeaponDef def, WeaponTier tier) {
        this.def = def;
        this.tier = tier;
    }

    public List<Mod> mods() { return def.mods(tier); }
    public int price() { return def.price(tier); }

    public String displayName() { return def.name + " [" + tier.name() + "]"; }

    public boolean sameTypeAndTier(WeaponInstance other){
        return other != null && other.def.type == def.type && other.tier == tier;
    }
}
