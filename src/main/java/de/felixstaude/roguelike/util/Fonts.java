package de.felixstaude.roguelike.util;

import java.awt.Font;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple font helper with caching and a shared font family.
 */
public final class Fonts {
    private Fonts() {
    }

    private static final String FAMILY = "Inter";
    private static final Map<Key, Font> CACHE = new ConcurrentHashMap<>();

    public static Font regular(int size) {
        return font(Font.PLAIN, size);
    }

    public static Font bold(int size) {
        return font(Font.BOLD, size);
    }

    public static Font italic(int size) {
        return font(Font.ITALIC, size);
    }

    private static Font font(int style, int size) {
        return CACHE.computeIfAbsent(new Key(style, size), key -> {
            Font font = new Font(FAMILY, key.style(), key.size());
            if (!FAMILY.equalsIgnoreCase(font.getFamily())) {
                font = new Font("Dialog", key.style(), key.size());
            }
            return font;
        });
    }

    private record Key(int style, int size) {
    }
}
