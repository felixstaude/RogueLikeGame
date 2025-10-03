package de.felixstaude.roguelike.shop;

public enum Rarity {
    COMMON(60, 0xB0C4DE),
    UNCOMMON(25, 0x8FBC8F),
    RARE(12, 0xBA55D3),
    EPIC(3, 0xFFD700);

    public final int weight;
    public final int rgb;

    Rarity(int weight, int rgb) {
        this.weight = weight;
        this.rgb = rgb;
    }
}
