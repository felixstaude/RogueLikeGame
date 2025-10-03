package de.felixstaude.roguelike.ui;

import de.felixstaude.roguelike.entity.Player;
import de.felixstaude.roguelike.util.Colors;
import de.felixstaude.roguelike.util.Draw;
import de.felixstaude.roguelike.util.Fonts;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Locale;

public final class HUD {
    private HUD() {
    }

    public static void drawBars(Graphics2D g, Player player, Rectangle viewport) {
        int x = viewport.x + 32;
        int y = viewport.y + 32;
        int width = 280;
        int height = 18;

        // Panel hinter den Bars
        g.setColor(new Color(12, 16, 24, 160));
        g.fillRoundRect(x - 12, y - 12, width + 24, height + 60, 20, 20);
        g.setColor(new Color(40, 46, 60, 200));
        g.drawRoundRect(x - 12, y - 12, width + 24, height + 60, 20, 20);

        // HP-Bar
        g.setFont(Fonts.bold(14));
        g.setColor(new Color(34, 38, 52));
        g.fillRoundRect(x, y, width, height, 12, 12);
        int hpFill = (int) Math.round(width * (player.hp / player.maxHp));
        g.setColor(Colors.SUCCESS);
        g.fillRoundRect(x, y, hpFill, height, 12, 12);
        g.setColor(Colors.TEXT_PRIMARY);
        g.drawString(String.format("HP %.0f/%.0f", player.hp, player.maxHp), x + 12, y + height - 4);

        // XP/Geld-Zeile
        y += 28;
        g.setColor(new Color(34, 38, 52));
        g.fillRoundRect(x, y, width, height, 12, 12);
        g.setColor(Colors.ACCENT);               // statt BUTTON_PRIMARY
        g.drawRoundRect(x, y, width, height, 12, 12);
        g.setColor(Colors.TEXT_PRIMARY);
        g.setFont(Fonts.regular(14));
        g.drawString("XP " + player.xp, x + 12, y + height - 4);
        String gold = "Gold " + player.gold;
        int goldW = g.getFontMetrics().stringWidth(gold);
        g.drawString(gold, x + width - goldW - 12, y + height - 4);
    }

    public static void drawTopBanner(Graphics2D g, Rectangle viewport, String text) {
        g.setFont(Fonts.bold(18));
        int textW = g.getFontMetrics().stringWidth(text);
        int padX = 24;
        int padY = 16;
        int boxW = textW + padX * 2;
        int boxH = 36;
        int x = viewport.x + (viewport.width - boxW) / 2;
        int y = viewport.y + 16;

        g.setColor(new Color(12, 16, 24, 200));
        g.fillRoundRect(x, y, boxW, boxH, 18, 18);
        g.setColor(new Color(52, 60, 78, 220));
        g.drawRoundRect(x, y, boxW, boxH, 18, 18);

        g.setColor(Colors.TEXT_PRIMARY);
        int baseline = y + (boxH + g.getFontMetrics().getAscent()) / 2 - 4;
        g.drawString(text, x + padX, baseline);
    }

    public static void drawCrosshair(Graphics2D g, int mx, int my) {
        g.setColor(new Color(255, 255, 255, 220));
        g.setStroke(new BasicStroke(1.6f));
        int size = 8;
        g.drawLine(mx - size, my, mx + size, my);
        g.drawLine(mx, my - size, mx, my + size);
        g.drawOval(mx - size, my - size, size * 2, size * 2);
    }

    public static void drawDebug(Graphics2D g, Rectangle viewport, double fps, double ups,
                                 int bulletCount, int particleCount, Player player) {
        Draw.globalHints(g);

        int margin = 16;
        int padding = 16;

        g.setFont(Fonts.bold(14));
        String header = "Debug Info";
        int headerWidth = g.getFontMetrics().stringWidth(header);
        int headerHeight = g.getFontMetrics().getHeight();

        g.setFont(Fonts.regular(13));
        var metrics = g.getFontMetrics();
        int lineHeight = metrics.getHeight();

        String[][] rows = {
                {"FPS", String.format(Locale.ROOT, "%.1f", fps)},
                {"UPS", String.format(Locale.ROOT, "%.1f", ups)},
                {"Bullets", Integer.toString(bulletCount)},
                {"Particles", Integer.toString(particleCount)},
                {"HP", String.format(Locale.ROOT, "%.0f / %.0f", player.hp, player.maxHp)},
                {"Pos", String.format(Locale.ROOT, "%.1f, %.1f", player.pos.x, player.pos.y)},
                {"Vel", String.format(Locale.ROOT, "%.1f, %.1f", player.vel.x, player.vel.y)},
                {"XP / Gold", player.xp + " / " + player.gold}
        };

        int maxLabel = headerWidth;
        int maxValue = 0;
        for (String[] row : rows) {
            String label = row[0] + ":";
            maxLabel = Math.max(maxLabel, metrics.stringWidth(label));
            maxValue = Math.max(maxValue, metrics.stringWidth(row[1]));
        }

        int panelWidth = padding * 2 + maxLabel + 12 + maxValue;
        int panelHeight = padding * 2 + headerHeight + 8 + rows.length * lineHeight;

        int x = viewport.x + viewport.width - panelWidth - margin;
        if (x < viewport.x + margin) x = viewport.x + margin;
        int y = viewport.y + margin;

        Rectangle panel = new Rectangle(x, y, panelWidth, panelHeight);
        Draw.drawPanel(g, panel, 18, new Color(10, 14, 24, 220), Colors.PANEL_BORDER);

        int textX = x + padding;
        int headerBaseline = y + padding + g.getFontMetrics(Fonts.bold(14)).getAscent();

        g.setFont(Fonts.bold(14));
        g.setColor(Colors.TEXT_PRIMARY);
        g.drawString(header, textX, headerBaseline);

        g.setFont(Fonts.regular(13));
        g.setColor(Colors.TEXT_SECONDARY);
        int baseline = y + padding + headerHeight + 8 + metrics.getAscent();
        int valueX = textX + maxLabel + 12;
        for (String[] row : rows) {
            String label = row[0] + ":";
            g.setColor(Colors.TEXT_SECONDARY);
            g.drawString(label, textX, baseline);
            g.setColor(Colors.TEXT_PRIMARY);
            g.drawString(row[1], valueX, baseline);
            baseline += lineHeight;
        }
    }

    public static void drawGameOverOverlay(Graphics2D g, int width, int height, Rectangle restartButton) {
        Draw.globalHints(g);

        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, width, height);

        int panelWidth = Math.max(420, restartButton.width + 200);
        int panelHeight = 260;
        int bottomMargin = 32;
        int panelX = restartButton.x + restartButton.width / 2 - panelWidth / 2;
        int panelY = restartButton.y + restartButton.height + bottomMargin - panelHeight;
        panelX = Math.max(32, Math.min(width - panelWidth - 32, panelX));
        panelY = Math.max(32, Math.min(height - panelHeight - 32, panelY));

        Rectangle panel = new Rectangle(panelX, panelY, panelWidth, panelHeight);
        Draw.drawShadowedPanel(g, panel, 28, Colors.PANEL_SHADOW, 0, 10,
                new Color(12, 16, 24, 230), Colors.PANEL_BORDER);

        Rectangle headerArea = new Rectangle(panel.x, panel.y + 24, panel.width, 48);
        g.setFont(Fonts.bold(36));
        g.setColor(Colors.TEXT_PRIMARY);
        Draw.drawCenteredString(g, "Game Over", headerArea);

        Rectangle messageArea = new Rectangle(panel.x + 40, panel.y + 96, panel.width - 80, 80);
        g.setFont(Fonts.regular(16));
        g.setColor(Colors.TEXT_SECONDARY);
        Draw.drawCenteredString(g, "You were overwhelmed by the horde.", messageArea);

        Rectangle hintArea = new Rectangle(panel.x + 40, panel.y + 136, panel.width - 80, 60);
        Draw.drawCenteredString(g, "Click the button or press [R] to try again.", hintArea);

        Color buttonColor = Colors.ACCENT;
        Draw.drawButton(g, restartButton, "Restart Run", buttonColor, Colors.TEXT_PRIMARY);
    }
}
