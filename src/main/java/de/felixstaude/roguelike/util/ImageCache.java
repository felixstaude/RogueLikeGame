package de.felixstaude.roguelike.util;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple classpath image cache with optional scaling and placeholder support.
 */
public final class ImageCache {
    private ImageCache() {
    }

    private static final Map<String, BufferedImage> ORIGINALS = new ConcurrentHashMap<>();
    private static final Map<String, BufferedImage> SCALED = new ConcurrentHashMap<>();
    private static final String PLACEHOLDER = "/icons/placeholder.png";

    public static BufferedImage get(String path) {
        String normalized = normalize(path);
        return ORIGINALS.computeIfAbsent(normalized, ImageCache::loadOrPlaceholder);
    }

    public static BufferedImage get(String path, int width, int height) {
        BufferedImage original = get(path);
        if (original == null) {
            return null;
        }
        if (original.getWidth() == width && original.getHeight() == height) {
            return original;
        }
        String key = normalize(path) + "#" + width + "x" + height;
        return SCALED.computeIfAbsent(key, k -> scale(original, width, height));
    }

    private static String normalize(String path) {
        if (path == null || path.isBlank()) {
            return PLACEHOLDER;
        }
        String normalized = path.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized;
    }

    private static BufferedImage loadOrPlaceholder(String path) {
        BufferedImage img = load(path);
        if (img != null) {
            return img;
        }
        if (!PLACEHOLDER.equals(path)) {
            return loadOrPlaceholder(PLACEHOLDER);
        }
        // Fallback placeholder (16x16 checker) if even the resource is missing.
        BufferedImage fallback = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int alpha = 0xFF;
                int shade = ((x / 4) + (y / 4)) % 2 == 0 ? 0x556070 : 0x8892A6;
                fallback.setRGB(x, y, (alpha << 24) | shade);
            }
        }
        return fallback;
    }

    private static BufferedImage load(String path) {
        try (InputStream in = ImageCache.class.getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            return ImageIO.read(in);
        } catch (IOException ex) {
            return null;
        }
    }

    private static BufferedImage scale(BufferedImage source, int width, int height) {
        BufferedImage scaled = new BufferedImage(Math.max(1, width), Math.max(1, height), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(source, 0, 0, width, height, null);
        } finally {
            g.dispose();
        }
        return scaled;
    }
}
