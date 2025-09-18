package de.felixstaude.roguelike.util;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Layout helpers for UI placement.
 */
public final class Layout {
    private Layout() {
    }

    public static Rectangle center(int containerWidth, int containerHeight, int width, int height) {
        int x = (containerWidth - width) / 2;
        int y = (containerHeight - height) / 2;
        return new Rectangle(x, y, width, height);
    }

    public static Rectangle alignLeft(Rectangle container, int padding, int y, int width, int height) {
        return new Rectangle(container.x + padding, y, width, height);
    }

    public static Rectangle alignRight(Rectangle container, int padding, int y, int width, int height) {
        return new Rectangle(container.x + container.width - padding - width, y, width, height);
    }

    public static Rectangle hotbarArea(Rectangle container, int padding, int height) {
        int y = container.y + container.height - padding - height;
        return new Rectangle(container.x + padding, y, container.width - padding * 2, height);
    }

    public static List<Rectangle> distributeHorizontally(Rectangle area, int count, int gap) {
        List<Rectangle> rects = new ArrayList<>(count);
        if (count <= 0) {
            return rects;
        }
        int totalGap = gap * Math.max(0, count - 1);
        int width = (area.width - totalGap) / count;
        width = Math.max(32, width);
        int used = width * count + totalGap;
        int startX = area.x + (area.width - used) / 2;
        for (int i = 0; i < count; i++) {
            int x = startX + i * (width + gap);
            rects.add(new Rectangle(x, area.y, width, area.height));
        }
        return rects;
    }

    public static List<Rectangle> cardRow(Rectangle container, int padding, int y, int height, int count, int gap) {
        List<Rectangle> rects = new ArrayList<>(count);
        if (count <= 0) {
            return rects;
        }
        int available = Math.max(count, container.width - padding * 2);
        int totalGap = gap * Math.max(0, count - 1);
        int width = (available - totalGap) / count;
        width = Math.min(width, 280);
        width = Math.max(140, width);
        int rowWidth = width * count + gap * Math.max(0, count - 1);
        if (rowWidth > container.width) {
            width = Math.max(120, (container.width - gap * Math.max(0, count - 1)) / count);
            rowWidth = width * count + gap * Math.max(0, count - 1);
        }
        int startX = container.x + Math.max(0, (container.width - rowWidth) / 2);
        for (int i = 0; i < count; i++) {
            int x = startX + i * (width + gap);
            rects.add(new Rectangle(x, y, width, height));
        }
        return rects;
    }
}
