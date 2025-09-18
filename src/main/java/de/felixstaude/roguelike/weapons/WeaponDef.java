// src/main/java/de/felixstaude/roguelike/weapons/WeaponDef.java
package de.felixstaude.roguelike.weapons;

import de.felixstaude.roguelike.items.ItemRarity;
import de.felixstaude.roguelike.items.Mod;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Definition einer Waffe: Mods & Preis je Tier. */
public class WeaponDef {
    public final WeaponType type;
    public final String name;
    public final ItemRarity rarityHint; // für Shop-Gewichtung/Anzeige

    private final Map<WeaponTier, List<Mod>> modsPerTier = new EnumMap<>(WeaponTier.class);
    private final Map<WeaponTier, Integer> pricePerTier = new EnumMap<>(WeaponTier.class);

    public WeaponDef(WeaponType type, String name, ItemRarity rarityHint) {
        this.type = type;
        this.name = name;
        this.rarityHint = rarityHint;
    }

    public WeaponDef tier(WeaponTier tier, int price, List<Mod> mods) {
        modsPerTier.put(tier, List.copyOf(mods));
        pricePerTier.put(tier, price);
        return this;
    }

    public List<Mod> mods(WeaponTier tier) { return modsPerTier.get(tier); }
    public int price(WeaponTier tier) { return pricePerTier.getOrDefault(tier, 999); }
}
