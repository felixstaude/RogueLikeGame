// src/main/java/de/felixstaude/roguelike/weapons/WeaponTier.java
package de.felixstaude.roguelike.weapons;

/** Tier-Leiter für Waffen; zwei gleiche Tiers -> nächste Stufe. */
public enum WeaponTier {
    COMMON, UNCOMMON, RARE, EPIC;

    public WeaponTier next() {
        return switch (this) {
            case COMMON -> UNCOMMON;
            case UNCOMMON -> RARE;
            case RARE -> EPIC;
            case EPIC -> EPIC; // schon am Cap
        };
    }
}
