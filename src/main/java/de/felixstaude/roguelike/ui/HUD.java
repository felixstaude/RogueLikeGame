package de.felixstaude.roguelike.ui;

import de.felixstaude.roguelike.core.Engine;
import de.felixstaude.roguelike.entity.Player;

import java.awt.*;

public class HUD {

    public static void drawBars(Graphics2D g, Player p){
        int x=20,y=20,w=260,h=16;

        // HP
        g.setColor(new Color(30,34,44)); g.fillRoundRect(x-2,y-2,w+4,h+4,8,8);
        g.setColor(new Color(60,66,80)); g.fillRoundRect(x,y,w,h,8,8);
        double ratio = p.hp / p.maxHp; int fill=(int)(w*ratio);
        g.setColor(new Color(90,230,120)); g.fillRoundRect(x,y,fill,h,8,8);
        g.setColor(Color.WHITE); g.setFont(new Font("Consolas", Font.PLAIN, 12));
        g.drawString(String.format("HP %.0f/%.0f", p.hp, p.maxHp), x+6, y+h-4);

        // XP
        y+=26;
        g.setColor(new Color(60,66,80)); g.fillRoundRect(x,y,w,h,8,8);
        g.setColor(Color.WHITE);
        g.drawString("XP " + p.xp, x + 6, y + h - 4);

        // Gold (Text daneben)
        g.drawString("Gold " + p.gold, x + 120, y + h - 4);
    }

    public static void drawTopBanner(Graphics2D g, String text) {
        g.setFont(new Font("Consolas", Font.BOLD, 18));
        int tw = g.getFontMetrics().stringWidth(text);
        int x = (Engine.WIDTH - tw) / 2;
        int y = 28;
        g.setColor(new Color(0,0,0,120));
        g.fillRoundRect(x-10, y-18, tw+20, 26, 10, 10);
        g.setColor(Color.WHITE);
        g.drawString(text, x, y);
    }

    public static void drawCrosshair(Graphics2D g, int mx,int my){
        g.setColor(new Color(255,255,255,200));
        g.setStroke(new BasicStroke(1.5f));
        int s=8; g.drawLine(mx-s,my,mx+s,my); g.drawLine(mx,my-s,mx,my+s); g.drawOval(mx-s,my-s,s*2,s*2);
    }

    public static void drawDebug(Graphics2D g, double fps,double ups,int bullets,int particles, Player p){
        g.setColor(new Color(255,255,255,200));
        g.setFont(new Font("Consolas", Font.PLAIN, 14));
        int y=20; int x= Engine.WIDTH-240;
        g.drawString(String.format("FPS: %.0f", fps), x, y); y+=18;
        g.drawString(String.format("UPS: %.0f", ups), x, y); y+=18;
        g.drawString("Bullets: "+bullets, x, y); y+=18;
        g.drawString("Particles: "+particles, x, y); y+=18;
        g.drawString(String.format("Player (%.0f, %.0f)", p.pos.x, p.pos.y), x, y);
    }

    public static void drawGameOverOverlay(Graphics2D g, Rectangle restartBtn) {
        g.setColor(new Color(0,0,0,150));
        g.fillRect(0,0,Engine.WIDTH, Engine.HEIGHT);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Consolas", Font.BOLD, 36));
        String title = "GAME OVER";
        int tw = g.getFontMetrics().stringWidth(title);
        g.drawString(title, (Engine.WIDTH - tw) / 2, Engine.HEIGHT/2 - 40);

        // Button
        g.setFont(new Font("Consolas", Font.PLAIN, 20));
        g.setColor(new Color(30,34,44));
        g.fillRoundRect(restartBtn.x, restartBtn.y, restartBtn.width, restartBtn.height, 12, 12);
        g.setColor(new Color(200, 230, 255));
        String btnText = "Restart (R)";
        int bw = g.getFontMetrics().stringWidth(btnText);
        int bx = restartBtn.x + (restartBtn.width - bw)/2;
        int by = restartBtn.y + (restartBtn.height + g.getFontMetrics().getAscent())/2 - 6;
        g.drawString(btnText, bx, by);
    }
}
