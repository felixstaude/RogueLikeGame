package de.felixstaude.roguelike.util;

import de.felixstaude.roguelike.items.ItemRarity;

import java.awt.Color;

/**
 * Central color palette used by UI drawing helpers and scenes.
 */
public final class Colors {
    private Colors() {
    }

    public static final Color BACKDROP = new Color(12, 15, 24);
    public static final Color FLOOR = new Color(28, 32, 44);
    public static final Color FLOOR_GRID = new Color(40, 46, 60);
    public static final Color FLOOR_BORDER = new Color(86, 148, 255);

    public static final Color PANEL_SHADOW = new Color(0, 0, 0, 110);
    public static final Color PANEL_BG = new Color(24, 28, 38);
    public static final Color PANEL_BORDER = new Color(58, 66, 84);

    public static final Color TEXT_PRIMARY = new Color(236, 242, 255);
    public static final Color TEXT_SECONDARY = new Color(184, 198, 226);
    public static final Color TEXT_MUTED = new Color(136, 150, 182);

    public static final Color SUCCESS = new Color(64, 159, 108);
    public static final Color SUCCESS_HOVER = new Color(82, 180, 128);
    public static final Color DANGER = new Color(196, 78, 78);
    public static final Color WARNING = new Color(255, 196, 120);
    public static final Color BUTTON_PRIMARY = new Color(86, 136, 255);
    public static final Color BUTTON_PRIMARY_HOVER = new Color(104, 150, 255);
    public static final Color BUTTON_DISABLED = new Color(78, 84, 102);

    public static final Color BADGE_TEXT = new Color(18, 22, 30);

    public static final Color RARITY_COMMON = new Color(130, 140, 160);
    public static final Color RARITY_UNCOMMON = new Color(90, 194, 119);
    public static final Color RARITY_RARE = new Color(85, 168, 255);
    public static final Color RARITY_EPIC = new Color(190, 98, 235);

    public static Color rarity(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> RARITY_COMMON;
            case UNCOMMON -> RARITY_UNCOMMON;
            case RARE -> RARITY_RARE;
            case EPIC -> RARITY_EPIC;
        };
    }
}
