package de.felixstaude.roguelike.ui;

import de.felixstaude.roguelike.entity.Player;
import de.felixstaude.roguelike.util.Colors;
import de.felixstaude.roguelike.util.Draw;
import de.felixstaude.roguelike.util.Fonts;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public final class HUD {
    private HUD() {
    }

    public static void drawBars(Graphics2D g, Player player, Rectangle viewport) {
        int x = viewport.x + 32;
        int y = viewport.y + 32;
        int width = 280;
        int height = 18;

        g.setColor(new Color(12, 16, 24, 160));
        g.fillRoundRect(x - 12, y - 12, width + 24, height + 60, 20, 20);
        g.setColor(new Color(40, 46, 60, 200));
        g.drawRoundRect(x - 12, y - 12, width + 24, height + 60, 20, 20);

        g.setFont(Fonts.bold(14));
        g.setColor(new Color(34, 38, 52));
        g.fillRoundRect(x, y, width, height, 12, 12);
        int hpFill = (int) Math.round(width * (player.hp / player.maxHp));
        g.setColor(Colors.SUCCESS);
        g.fillRoundRect(x, y, hpFill, height, 12, 12);
        g.setColor(Colors.TEXT_PRIMARY);
        g.drawString(String.format("HP %.0f/%.0f", player.hp, player.maxHp), x + 12, y + height - 4);

        y += 28;
        g.setColor(new Color(34, 38, 52));
        g.fillRoundRect(x, y, width, height, 12, 12);
        g.setColor(Colors.BUTTON_PRIMARY);
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

    public static void drawDebug(Graphics2D g, Rectangle viewport, double fps, double ups, int bullets, int particles, Player player) {
        g.setFont(Fonts.regular(14));
        g.setColor(new Color(0, 0, 0, 140));
        int boxW = 200;
        int boxH = 120;
        int x = viewport.x + viewport.width - boxW - 32;
        int y = viewport.y + 32;
        g.fillRoundRect(x, y, boxW, boxH, 16, 16);
        g.setColor(new Color(52, 60, 78, 220));
        g.drawRoundRect(x, y, boxW, boxH, 16, 16);

        g.setColor(Colors.TEXT_SECONDARY);
        int lineY = y + 26;
        g.drawString(String.format("FPS: %.0f", fps), x + 12, lineY); lineY += 20;
        g.drawString(String.format("UPS: %.0f", ups), x + 12, lineY); lineY += 20;
        g.drawString("Bullets: " + bullets, x + 12, lineY); lineY += 20;
        g.drawString("Particles: " + particles, x + 12, lineY); lineY += 20;
        g.drawString(String.format("Player (%.0f, %.0f)", player.pos.x, player.pos.y), x + 12, lineY);
    }

    public static void drawGameOverOverlay(Graphics2D g, int canvasW, int canvasH, Rectangle restartBtn) {
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRect(0, 0, canvasW, canvasH);

        g.setFont(Fonts.bold(40));
        g.setColor(Colors.TEXT_PRIMARY);
        String title = "GAME OVER";
        int titleW = g.getFontMetrics().stringWidth(title);
        int tx = (canvasW - titleW) / 2;
        int ty = canvasH / 2 - 60;
        g.drawString(title, tx, ty);

        g.setFont(Fonts.bold(16));
        Draw.drawButton(g, restartBtn, "Restart (R)", Colors.BUTTON_PRIMARY, Colors.TEXT_PRIMARY);
    }
}
