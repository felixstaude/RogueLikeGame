package de.felixstaude.roguelike.util;

import de.felixstaude.roguelike.items.ItemRarity;

import java.awt.Color;

/**
 * Flux Core Color Palette - Energy and Corruption themed.
 */
public final class Colors {
    private Colors() {}

    // ========================================================================
    // FLUX CORE THEME COLORS
    // ========================================================================

    // Flux Core States (Player/Energy)
    public static final Color FLUX_STABLE    = new Color(0x00FFFF); // Cyan - Stable energy
    public static final Color FLUX_UNSTABLE  = new Color(0xFF00FF); // Magenta - Unstable energy
    public static final Color FLUX_CRITICAL  = new Color(0xFF6400); // Orange-Red - Critical state
    public static final Color CORE_GLOW      = new Color(0x96C8FF); // Soft Blue - Core glow effect
    public static final Color CORRUPTION     = new Color(0x800080); // Purple - Corruption/Enemy theme

    // Energy Grid & Environment
    public static final Color ENERGY_GRID    = new Color(0x006496, true); // Transparent grid (alpha ~50)
    public static final Color ENERGY_PULSE   = new Color(0x0096C8); // Pulsing energy lines
    public static final Color RIFT_GLOW      = new Color(0x4B00C8); // Rift zone glow

    // Scene / Floor (darker theme for energy contrast)
    public static final Color BACKDROP     = new Color(0x0A0E14); // Darker backdrop
    public static final Color FLOOR        = new Color(0x0E1117); // Dark floor
    public static final Color FLOOR_GRID   = new Color(0x1F2536); // Keep existing grid
    public static final Color FLOOR_BORDER = new Color(0x2C3650); // Keep existing border

    // Panels
    public static final Color PANEL_SHADOW = new Color(0, 0, 0, 140);
    public static final Color PANEL_BG     = new Color(0x141824);
    public static final Color PANEL_BORDER = new Color(0x2C3650);

    // Text
    public static final Color TEXT_PRIMARY   = new Color(0xE6ECFF);
    public static final Color TEXT_SECONDARY = new Color(0xBFD1E6);
    public static final Color TEXT_MUTED     = new Color(0x94A3C0);

    // Accents / Buttons / States
    public static final Color ACCENT        = new Color(0x5DA1F0);
    public static final Color ACCENT_HOVER  = new Color(0x74B1F4);
    public static final Color SUCCESS       = new Color(0x409F6C);
    public static final Color SUCCESS_HOVER = new Color(0x4FB67E);
    public static final Color DANGER        = new Color(0xC65C5C);
    public static final Color DANGER_HOVER  = new Color(0xD87474);
    public static final Color WARNING       = new Color(0xF0A85D);
    public static final Color DISABLED      = new Color(0x2F3A52);

    // Badges
    public static final Color BADGE_TEXT = new Color(0x0E1117);

    // Rarity
    public static final Color RARITY_COMMON    = new Color(0x8892A6);
    public static final Color RARITY_UNCOMMON  = new Color(0x4CAF50);
    public static final Color RARITY_RARE      = new Color(0x42A5F5);
    public static final Color RARITY_EPIC      = new Color(0xAB47BC);
    public static final Color RARITY_LEGENDARY = new Color(0xFF6B00); // Flux Core: Orange-Red

    // Mod coloring
    public static final Color MOD_POSITIVE = new Color(0x90EE90);
    public static final Color MOD_NEGATIVE = new Color(0xFFA0A0);
    public static final Color MOD_NEUTRAL  = new Color(0xC8D2E6);

    public static Color rarity(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON    -> RARITY_COMMON;
            case UNCOMMON  -> RARITY_UNCOMMON;
            case RARE      -> RARITY_RARE;
            case EPIC      -> RARITY_EPIC;
            case LEGENDARY -> RARITY_LEGENDARY;
        };
    }

    // ========================================================================
    // FLUX CORE COLOR HELPERS
    // ========================================================================

    /**
     * Returns player/core color based on instability percentage (0-100).
     * 0-25%: Cyan (Stable)
     * 26-50%: Cyan → Magenta blend
     * 51-75%: Magenta → Orange blend
     * 76-100%: Orange → Red blend
     */
    public static Color fluxCoreColor(int instabilityPct) {
        instabilityPct = Math.max(0, Math.min(100, instabilityPct));

        if (instabilityPct <= 25) {
            // Stable: Pure Cyan
            return FLUX_STABLE;
        } else if (instabilityPct <= 50) {
            // Flux Leak: Cyan → Magenta
            float t = (instabilityPct - 25) / 25.0f;
            return blend(FLUX_STABLE, FLUX_UNSTABLE, t);
        } else if (instabilityPct <= 75) {
            // Core Fracture: Magenta → Orange
            float t = (instabilityPct - 50) / 25.0f;
            return blend(FLUX_UNSTABLE, FLUX_CRITICAL, t);
        } else {
            // Critical Mass / Meltdown: Orange → Bright Red
            float t = (instabilityPct - 75) / 25.0f;
            Color brightRed = new Color(0xFF0000);
            return blend(FLUX_CRITICAL, brightRed, t);
        }
    }

    /**
     * Returns bullet/projectile color based on instability.
     */
    public static Color bulletColor(int instabilityPct) {
        return fluxCoreColor(instabilityPct);
    }

    /**
     * Linearly interpolates between two colors.
     */
    public static Color blend(Color c1, Color c2, float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        int r = (int) (c1.getRed()   + (c2.getRed()   - c1.getRed())   * t);
        int g = (int) (c1.getGreen() + (c2.getGreen() - c1.getGreen()) * t);
        int b = (int) (c1.getBlue()  + (c2.getBlue()  - c1.getBlue())  * t);
        int a = (int) (c1.getAlpha() + (c2.getAlpha() - c1.getAlpha()) * t);
        return new Color(r, g, b, a);
    }

    /**
     * Returns color with alpha channel modified.
     */
    public static Color withAlpha(Color c, int alpha) {
        alpha = Math.max(0, Math.min(255, alpha));
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }
}
