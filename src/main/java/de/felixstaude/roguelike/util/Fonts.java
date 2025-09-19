package de.felixstaude.roguelike.util;

import java.awt.Font;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple font helper with caching and a shared font family.
 */
public final class Fonts {
    private Fonts() {}

    private static final String FAMILY = "Inter";
    private static final Map<Key, Font> CACHE = new ConcurrentHashMap<>();

    public static Font regular(int size) { return font(Font.PLAIN, size); }
    public static Font bold(int size)     { return font(Font.BOLD, size); }
    public static Font italic(int size)   { return font(Font.ITALIC, size); }

    private static Font font(int style, int size) {
        return CACHE.computeIfAbsent(new Key(style, size), key -> {
            Font f = new Font(FAMILY, key.style(), key.size());
            // Fallback, falls "Inter" nicht vorhanden ist
            if (!FAMILY.equalsIgnoreCase(f.getFamily())) {
                f = new Font("Dialog", key.style(), key.size());
            }
            return f;
        });
    }

    private record Key(int style, int size) {}
}
