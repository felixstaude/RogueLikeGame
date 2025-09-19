package de.felixstaude.roguelike.util;

import de.felixstaude.roguelike.items.ItemRarity;

import java.awt.Color;

/**
 * Central color palette used by UI drawing helpers and scenes.
 */
public final class Colors {
    private Colors() {
    }

    public static final Color BACKDROP = new Color(0x0E1117);
    public static final Color FLOOR = new Color(0x141824);
    public static final Color FLOOR_GRID = new Color(0x1F2536);
    public static final Color FLOOR_BORDER = new Color(0x2C3650);

    public static final Color PANEL_SHADOW = new Color(0, 0, 0, 140);
    public static final Color PANEL_BG = new Color(0x141824);
    public static final Color PANEL_BORDER = new Color(0x2C3650);

    public static final Color TEXT_PRIMARY = new Color(0xE6ECFF);
    public static final Color TEXT_SECONDARY = new Color(0xBFD1E6);
    public static final Color TEXT_MUTED = new Color(0x94A3C0);

    public static final Color ACCENT = new Color(0x5DA1F0);
    public static final Color ACCENT_HOVER = new Color(0x74B1F4);
    public static final Color SUCCESS = new Color(0x409F6C);
    public static final Color SUCCESS_HOVER = new Color(0x4FB67E);
    public static final Color DANGER = new Color(0xC65C5C);
    public static final Color DANGER_HOVER = new Color(0xD87474);
    public static final Color WARNING = new Color(0xF0A85D);
    public static final Color DISABLED = new Color(0x2F3A52);

    public static final Color BADGE_TEXT = new Color(0x0E1117);

    public static final Color RARITY_COMMON = new Color(0x8892A6);
    public static final Color RARITY_UNCOMMON = new Color(0x4CAF50);
    public static final Color RARITY_RARE = new Color(0x42A5F5);
    public static final Color RARITY_EPIC = new Color(0xAB47BC);

    public static final Color MOD_POSITIVE = new Color(0x90EE90);
    public static final Color MOD_NEGATIVE = new Color(0xFFA0A0);
    public static final Color MOD_NEUTRAL = new Color(0xC8D2E6);

    public static Color rarity(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> RARITY_COMMON;
            case UNCOMMON -> RARITY_UNCOMMON;
            case RARE -> RARITY_RARE;
            case EPIC -> RARITY_EPIC;
        };
    }
}
