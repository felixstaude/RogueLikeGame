// src/main/java/de/felixstaude/roguelike/items/ItemRarity.java
package de.felixstaude.roguelike.items;

/** Rarity für Items/Waffen-Angebote mit Basisgewichten (für Shop-Rolls). */
public enum ItemRarity {
    COMMON   (60),
    UNCOMMON (25),
    RARE     (12),
    EPIC     (3),
    LEGENDARY(1);  // Flux Core: Sehr selten, mächtig

    /** Basisgewicht für zufällige Auswahl (höher = häufiger). */
    public final int weight;

    ItemRarity(int weight) { this.weight = weight; }

    /** 0..4 zur einfachen Luck-Skalierung (COMMON=0 ... LEGENDARY=4). */
    public int rank() {
        return switch (this) {
            case COMMON -> 0;
            case UNCOMMON -> 1;
            case RARE -> 2;
            case EPIC -> 3;
            case LEGENDARY -> 4;
        };
    }
}
