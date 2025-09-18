// src/main/java/de/felixstaude/roguelike/shop/Shop.java
package de.felixstaude.roguelike.shop;

import de.felixstaude.roguelike.entity.Player;
import de.felixstaude.roguelike.input.Input;
import de.felixstaude.roguelike.items.Item;
import de.felixstaude.roguelike.items.ItemRarity;
import de.felixstaude.roguelike.items.Mod;
import de.felixstaude.roguelike.items.PassiveItemCatalog;
import de.felixstaude.roguelike.stats.Stat;
import de.felixstaude.roguelike.stats.Stats;
import de.felixstaude.roguelike.weapons.*;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Schöner Shop:
 * - Grid-Karten mit Rarity-Badge, Mod-List, BUY-Button, Hover/Click
 * - Reroll-Button (Start 6G, +4 je Reroll in Phase, Reset pro Phase)
 * - Next Wave Button
 * - Fixed Prices + Discounts (Bargain Hunter), Unique nur 1x pro Run
 * - Duplicates in derselben Rotation erlaubt
 *
 * Kompatible Engine-API:
 *  - prepareForWave(int wave, Player p)
 *  - handleInput(Input input, Player p) -> boolean (true = nächste Wave starten)
 *  - render(Graphics2D g, Player p, int wave)
 */
public class Shop {

    // ------------------- Offer & Purchase ------------------------------------
    public enum OfferType { PASSIVE, WEAPON }

    public static final class Offer {
        public final OfferType type;
        public final String title;
        public final ItemRarity rarity;
        public final int price;
        public final List<Mod> mods;
        public final Item item;            // PASSIVE
        public final WeaponDef weaponDef;  // WEAPON
        public final WeaponTier tier;      // WEAPON

        private Offer(OfferType type, String title, ItemRarity rarity, int price, List<Mod> mods,
                      Item item, WeaponDef weaponDef, WeaponTier tier) {
            this.type = type; this.title = title; this.rarity = rarity; this.price = price; this.mods = mods;
            this.item = item; this.weaponDef = weaponDef; this.tier = tier;
        }

        public static Offer passive(Item it) {
            return new Offer(OfferType.PASSIVE, it.name, it.rarity, it.price, it.mods, it, null, null);
        }
        public static Offer weapon(WeaponDef def, WeaponTier tier) {
            String t = def.name + " ["+tier.name()+"]";
            return new Offer(OfferType.WEAPON, t, rarityFromTier(def, tier), def.price(tier), def.mods(tier), null, def, tier);
        }
        private static ItemRarity rarityFromTier(WeaponDef def, WeaponTier tier){
            return switch (tier) {
                case COMMON -> def.rarityHint;
                case UNCOMMON -> bump(def.rarityHint);
                case RARE -> bump(bump(def.rarityHint));
                case EPIC -> ItemRarity.EPIC;
            };
        }
        private static ItemRarity bump(ItemRarity r){
            return switch (r) {
                case COMMON -> ItemRarity.UNCOMMON;
                case UNCOMMON -> ItemRarity.RARE;
                case RARE, EPIC -> ItemRarity.EPIC;
            };
        }
    }

    public static final class PurchaseResult {
        public final boolean success;
        public final int goldSpent;
        public final String message;
        public PurchaseResult(boolean success, int goldSpent, String message) {
            this.success = success; this.goldSpent = goldSpent; this.message = message;
        }
    }

    // ------------------- Persistenter Run-Zustand -----------------------------
    private final List<Item> passivePool = PassiveItemCatalog.all();
    private final Map<WeaponType, WeaponDef> weapons = WeaponCatalog.all();
    private final Set<String> boughtUniques = new HashSet<>();

    // Shop-Flags (aus Items)
    private int shopRerollDiscount = 0;   // Lucky Charm: -2 (min 2)
    private int shopPriceDiscountPct = 0; // Bargain Hunter: -15% (stack bis -50%)

    // Rotation
    private final List<Offer> offers = new ArrayList<>();
    private int rerollCost = 6;
    private int rerollsThisPhase = 0;

    // Interne Stats (passive + Waffen)
    private final Stats passiveStats = new Stats();
    private final WeaponHotbar hotbar = new WeaponHotbar();

    // UI-State
    private String lastMessage = "";
    private final List<CardUI> cardUIs = new ArrayList<>();
    private Rectangle rerollBtn = new Rectangle();
    private Rectangle nextBtn = new Rectangle();

    // Layout-Konstanten
    private static final int SCREEN_W = 1280, SCREEN_H = 720;
    private static final int PANEL_W = 1100, PANEL_H = 560;
    private static final int CARD_W = 330, CARD_H = 150;
    private static final int GRID_COLS = 3;
    private static final int GRID_GAP_X = 36, GRID_GAP_Y = 26;

    // ------------------- Engine-kompatible API --------------------------------

    public void prepareForWave(int wave, Player p) {
        rollOffers(0, 0);
    }

    public boolean handleInput(Input input, Player p) {
        computeLayout(); // damit Klickflächen existieren

        // Maus: BUY-Buttons
        if (input.mousePressedL) {
            for (int i = 0; i < cardUIs.size(); i++) {
                CardUI c = cardUIs.get(i);
                if (c.buyBtn.contains(input.mouseX, input.mouseY)) {
                    // Kauf
                    PurchaseResult res = buy(i, passiveStats, hotbar, p.gold);
                    if (res.success) {
                        p.gold -= res.goldSpent;
                        // Karte einmalig kaufbar in dieser Rotation -> entferne Offer+UI
                        offers.remove(i);
                        cardUIs.remove(i);
                        relayoutAfterRemoval();
                    }
                    lastMessage = res.message + (res.success ? " (−" + res.goldSpent + "G)" : "");
                    return false;
                }
            }
            // Reroll-Button
            if (rerollBtn.contains(input.mouseX, input.mouseY)) {
                int cost = getRerollCost();
                if (p.gold >= cost) {
                    p.gold -= cost;
                    reroll(0, 0);
                    lastMessage = "Shop rerolled (−" + cost + "G)";
                } else {
                    lastMessage = "Nicht genug Gold für Reroll ("+cost+"G)";
                }
                return false;
            }
            // Next-Wave
            if (nextBtn.contains(input.mouseX, input.mouseY)) return true;
        }

        // Tastatur-Shortcuts
        int n = offers.size();
        for (int i = 0; i < n; i++) {
            if (input.wasPressed(KeyEvent.VK_1 + i)) {
                PurchaseResult res = buy(i, passiveStats, hotbar, p.gold);
                if (res.success) {
                    p.gold -= res.goldSpent;
                    offers.remove(i);
                    if (i < cardUIs.size()) cardUIs.remove(i);
                    relayoutAfterRemoval();
                }
                lastMessage = res.message + (res.success ? " (−" + res.goldSpent + "G)" : "");
                return false;
            }
        }
        if (input.wasPressed(KeyEvent.VK_R)) {
            int cost = getRerollCost();
            if (p.gold >= cost) {
                p.gold -= cost;
                reroll(0, 0);
                lastMessage = "Shop rerolled (−" + cost + "G)";
            } else lastMessage = "Nicht genug Gold für Reroll ("+cost+"G)";
        }
        if (input.wasPressed(KeyEvent.VK_SPACE)) return true;

        return false;
    }

    public void render(Graphics2D g, Player p, int wave) {
        // AA & UI-Qualität
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Dim-Overlay
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRect(0, 0, SCREEN_W, SCREEN_H);

        // Panel
        int panelX = (SCREEN_W - PANEL_W) / 2;
        int panelY = (SCREEN_H - PANEL_H) / 2;
        drawPanel(g, panelX, panelY, PANEL_W, PANEL_H);

        // Header
        int y = panelY + 40;
        g.setColor(new Color(230, 236, 255));
        g.setFont(new Font("Inter", Font.BOLD, 28));
        drawCentered(g, "SHOP – Wave " + wave + " beendet", SCREEN_W, y);

        y += 28;
        g.setFont(new Font("Inter", Font.PLAIN, 16));
        String hint = "Gold: " + p.gold + "   •   Reroll: " + getRerollCost() + "G   •   Klick auf BUY oder drücke 1.."
                + Math.max(offers.size(),1) + "  •  SPACE: Nächste Wave";
        g.setColor(new Color(190, 205, 230));
        drawCentered(g, hint, SCREEN_W, y);

        // Grid der Cards
        computeLayout(); // erzeugt cardUIs, rerollBtn, nextBtn

        // Karten
        for (int i = 0; i < cardUIs.size(); i++) {
            drawCard(g, cardUIs.get(i), offers.get(i), p);
        }

        // Footer-Buttons
        drawPrimaryButton(g, rerollBtn, "Reroll (" + getRerollCost() + "G)",
                p.gold >= getRerollCost(), isHover(rerollBtn, g));

        drawSuccessButton(g, nextBtn, "Start Next Wave", true, isHover(nextBtn, g));

        // Hotbar-Preview (unten)
        drawHotbar(g, panelX + 24, panelY + PANEL_H - 84);

        // Status-Msg
        if (!lastMessage.isEmpty()) {
            g.setFont(new Font("Inter", Font.PLAIN, 16));
            g.setColor(new Color(255, 230, 180));
            drawCentered(g, lastMessage, SCREEN_W, panelY + PANEL_H - 20);
        }
    }

    // ------------------- Neue Logik (Rotation/Reroll/Buy) --------------------

    public void resetRun() {
        boughtUniques.clear();
        shopRerollDiscount = 0;
        shopPriceDiscountPct = 0;
        offers.clear();
        rerollCost = Math.max(2, 6 - shopRerollDiscount);
        rerollsThisPhase = 0;
        lastMessage = "";
        passiveStats.clear();
        hotbar.getSlots().clear();
        cardUIs.clear();
    }

    public List<Offer> getOffers(){ return offers; }
    public int getRerollCost(){ return rerollCost; }

    public void rollOffers(int luck, int harvesting) {
        offers.clear();
        rerollCost = Math.max(2, 6 - shopRerollDiscount);
        rerollsThisPhase = 0;

        int slots = computeSlots(harvesting);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        for (int i = 0; i < slots; i++) {
            boolean pickWeapon = rnd.nextDouble() < 0.55;
            if (pickWeapon) {
                WeaponDef def = randomWeaponDef(luck);
                WeaponTier tier = randomWeaponTier(luck);
                offers.add(Offer.weapon(def, tier));
            } else {
                Item it = randomPassiveItem(luck);
                offers.add(Offer.passive(it));
            }
        }
        cardUIs.clear();
    }

    public void reroll(int luck, int harvesting) {
        rerollsThisPhase++;
        rerollCost += 4;
        rollOffers(luck, harvesting);
    }

    public PurchaseResult buy(int index, Stats passiveStats, WeaponHotbar hotbar, int goldAvailable) {
        if (index < 0 || index >= offers.size()) return new PurchaseResult(false, 0, "Ungültiger Index.");
        Offer o = offers.get(index);

        int price = applyDiscounts(o.price);
        if (goldAvailable < price) return new PurchaseResult(false, 0, "Nicht genug Gold.");

        switch (o.type) {
            case PASSIVE -> {
                Item it = o.item;
                if (it.unique && boughtUniques.contains(it.id)) {
                    return new PurchaseResult(false, 0, "Unique bereits gekauft.");
                }
                it.applyTo(passiveStats);
                if (it.unique) boughtUniques.add(it.id);
                if ("u_lucky_charm".equals(it.id)) shopRerollDiscount = 2;
                if ("bargain_hunter".equals(it.id)) shopPriceDiscountPct = Math.min(50, shopPriceDiscountPct + 15);
                return new PurchaseResult(true, price, "Gekauft: " + it.name);
            }
            case WEAPON -> {
                WeaponInstance wi = new WeaponInstance(o.weaponDef, o.tier);
                WeaponHotbar.Result res = hotbar.tryAddOrCombine(wi);
                if (!res.success()) return new PurchaseResult(false, 0, "Hotbar voll (kein Combine möglich).");
                return new PurchaseResult(true, price, res.added() ? "Waffe hinzugefügt" : "Waffe kombiniert");
            }
        }
        return new PurchaseResult(false, 0, "Unbekannter Typ.");
    }

    // ------------------- UI / Layout -----------------------------------------

    private static final class CardUI {
        Rectangle bounds;
        Rectangle buyBtn;
        Rectangle priceTag;
        Rectangle rarityTag;
    }

    private void computeLayout() {
        cardUIs.clear();
        int startX = (SCREEN_W - PANEL_W) / 2 + 28;
        int startY = (SCREEN_H - PANEL_H) / 2 + 96;

        int col = 0, row = 0;
        for (int i = 0; i < offers.size(); i++) {
            int x = startX + col * (CARD_W + GRID_GAP_X);
            int y = startY + row * (CARD_H + GRID_GAP_Y);

            CardUI ui = new CardUI();
            ui.bounds = new Rectangle(x, y, CARD_W, CARD_H);
            ui.buyBtn = new Rectangle(x + CARD_W - 110, y + CARD_H - 40, 96, 28);
            ui.priceTag = new Rectangle(x + 12, y + CARD_H - 40, 86, 28);
            ui.rarityTag = new Rectangle(x + 12, y + 10, 84, 22);
            cardUIs.add(ui);

            col++;
            if (col >= GRID_COLS) { col = 0; row++; }
        }

        // Footer-Buttons zentriert unter Grid
        int panelX = (SCREEN_W - PANEL_W) / 2;
        int panelY = (SCREEN_H - PANEL_H) / 2;
        rerollBtn = new Rectangle(panelX + 28, panelY + PANEL_H - 64, 180, 36);
        nextBtn   = new Rectangle(panelX + PANEL_W - 208, panelY + PANEL_H - 64, 180, 36);
    }

    private void relayoutAfterRemoval() {
        // Einfach neu berechnen
        computeLayout();
    }

    private void drawPanel(Graphics2D g, int x, int y, int w, int h) {
        // Schatten
        g.setColor(new Color(0, 0, 0, 80));
        g.fillRoundRect(x+4, y+6, w, h, 18, 18);
        // Panel
        g.setColor(new Color(24, 27, 36));
        g.fillRoundRect(x, y, w, h, 18, 18);
        // Border
        g.setColor(new Color(60, 70, 90));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(x, y, w, h, 18, 18);
    }

    private void drawCard(Graphics2D g, CardUI ui, Offer o, Player p) {
        boolean affordable = p.gold >= applyDiscounts(o.price);
        boolean hover = isHover(ui.bounds, g);

        // Karte
        g.setColor(hover ? new Color(36, 40, 54) : new Color(30, 34, 46));
        g.fill(new RoundRectangle2D.Float(ui.bounds.x, ui.bounds.y, ui.bounds.width, ui.bounds.height, 14, 14));
        g.setColor(new Color(65, 75, 96));
        g.setStroke(new BasicStroke(1.6f));
        g.draw(new RoundRectangle2D.Float(ui.bounds.x, ui.bounds.y, ui.bounds.width, ui.bounds.height, 14, 14));

        // Rarity-Badge
        drawBadge(g, ui.rarityTag, o.rarity.name(), rarityColor(o.rarity));

        // Title
        g.setColor(new Color(220, 230, 255));
        g.setFont(new Font("Inter", Font.BOLD, 18));
        g.drawString(o.title, ui.bounds.x + 12, ui.bounds.y + 50);

        // Mods
        g.setFont(new Font("Inter", Font.PLAIN, 14));
        int y = ui.bounds.y + 74;
        for (String line : formatMods(o.mods)) {
            g.setColor(colorForLine(line));
            g.drawString("• " + line, ui.bounds.x + 14, y);
            y += 18;
            if (y > ui.bounds.y + ui.bounds.height - 50) break; // nicht überlaufen
        }

        // Price Tag (links unten)
        int price = applyDiscounts(o.price);
        drawPill(g, ui.priceTag, price + "G", affordable ? new Color(64, 159, 108) : new Color(180, 80, 80));

        // BUY Button (rechts unten)
        drawButton(g, ui.buyBtn, "BUY", affordable, isHover(ui.buyBtn, g));
    }

    private void drawHotbar(Graphics2D g, int x, int y) {
        g.setFont(new Font("Inter", Font.PLAIN, 13));
        g.setColor(new Color(170, 185, 210));
        g.drawString("Weapons:", x, y - 6);

        int slotW = 200, slotH = 52, gap = 14;
        List<WeaponInstance> slots = hotbar.getSlots();

        for (int i = 0; i < hotbar.capacity(); i++) {
            int sx = x + i * (slotW + gap);
            int sy = y;
            boolean filled = i < slots.size();
            // Slot
            g.setColor(new Color(28, 31, 42));
            g.fillRoundRect(sx, sy, slotW, slotH, 10, 10);
            g.setColor(new Color(60, 70, 90));
            g.drawRoundRect(sx, sy, slotW, slotH, 10, 10);

            if (filled) {
                WeaponInstance w = slots.get(i);
                g.setColor(new Color(220, 230, 255));
                g.setFont(new Font("Inter", Font.BOLD, 14));
                g.drawString(w.displayName(), sx + 10, sy + 20);

                g.setFont(new Font("Inter", Font.PLAIN, 12));
                String mods = compactMods(w.mods());
                g.setColor(new Color(180, 195, 220));
                g.drawString(mods, sx + 10, sy + 38);
            } else {
                g.setColor(new Color(120, 135, 160));
                g.setFont(new Font("Inter", Font.ITALIC, 13));
                g.drawString("Empty", sx + 10, sy + 30);
            }
        }
    }

    private void drawButton(Graphics2D g, Rectangle r, String label, boolean enabled, boolean hover) {
        Color base = enabled ? new Color(70, 120, 240) : new Color(80, 80, 100);
        if (hover && enabled) base = new Color(86, 136, 255);
        g.setColor(base);
        g.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
        g.setColor(new Color(240, 245, 255));
        g.setFont(new Font("Inter", Font.BOLD, 14));
        drawCenteredIn(g, label, r);
    }
    private void drawPrimaryButton(Graphics2D g, Rectangle r, String label, boolean enabled, boolean hover) {
        drawButton(g, r, label, enabled, hover);
    }
    private void drawSuccessButton(Graphics2D g, Rectangle r, String label, boolean enabled, boolean hover) {
        Color base = enabled ? new Color(64, 159, 108) : new Color(80, 100, 90);
        if (hover && enabled) base = new Color(82, 180, 128);
        g.setColor(base);
        g.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
        g.setColor(new Color(240, 245, 255));
        g.setFont(new Font("Inter", Font.BOLD, 14));
        drawCenteredIn(g, label, r);
    }

    private void drawPill(Graphics2D g, Rectangle r, String label, Color c) {
        g.setColor(c);
        g.fillRoundRect(r.x, r.y, r.width, r.height, 12, 12);
        g.setColor(new Color(250, 255, 255));
        g.setFont(new Font("Inter", Font.BOLD, 14));
        drawCenteredIn(g, label, r);
    }

    private void drawBadge(Graphics2D g, Rectangle r, String label, Color c) {
        g.setColor(c);
        g.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Inter", Font.BOLD, 12));
        drawCenteredIn(g, label, r);
    }

    private boolean isHover(Rectangle r, Graphics2D g) {
        // Wir haben keine direkte Maus-API hier, aber Input prüft beim Klick. Hover nutzen wir nur optisch:
        // Wenn du echtes Hover willst, gib der Shop.render Input oder speichere Mauspos global.
        return false;
    }

    private static void drawCentered(Graphics2D g, String text, int width, int y){
        int w = g.getFontMetrics().stringWidth(text);
        g.drawString(text, (width - w) / 2, y);
    }
    private static void drawCenteredIn(Graphics2D g, String text, Rectangle r) {
        int tw = g.getFontMetrics().stringWidth(text);
        int th = g.getFontMetrics().getAscent();
        int x = r.x + (r.width - tw) / 2;
        int y = r.y + (r.height + th) / 2 - 3;
        g.drawString(text, x, y);
    }

    private static Color rarityColor(ItemRarity r) {
        return switch (r) {
            case COMMON -> new Color(130, 140, 160);
            case UNCOMMON -> new Color(76, 175, 80);
            case RARE -> new Color(66, 165, 245);
            case EPIC -> new Color(171, 71, 188);
        };
    }

    private static Color colorForLine(String line) {
        if (line.startsWith("+")) return new Color(144, 238, 144);
        if (line.startsWith("-")) return new Color(255, 160, 160);
        return new Color(200, 210, 230);
    }

    // ------------------- Zufallsauswahl / Preise ------------------------------

    private int computeSlots(int harvesting) {
        int slots = 4 + Integer.compare(harvesting, 0);
        return Math.max(2, Math.min(6, slots));
    }

    private int applyDiscounts(int price) {
        int p = price;
        if (shopPriceDiscountPct != 0) {
            p = (int)Math.round(p * (1.0 - shopPriceDiscountPct/100.0));
        }
        return Math.max(1, p);
    }

    private Item randomPassiveItem(int luck) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        List<Item> pool = new ArrayList<>();
        for (Item it : passivePool) {
            if (it.unique && boughtUniques.contains(it.id)) continue;
            pool.add(it);
        }
        int total = 0;
        int[] weights = new int[pool.size()];
        for (int i = 0; i < pool.size(); i++) {
            Item it = pool.get(i);
            double mult = 1.0 + Math.max(0, luck) * 0.01 * it.rarity.rank();
            int w = (int)Math.max(1, Math.round(it.rarity.weight * mult));
            weights[i] = w; total += w;
        }
        int pick = rnd.nextInt(Math.max(1, total));
        for (int i = 0; i < pool.size(); i++) if ((pick -= weights[i]) < 0) return pool.get(i);
        return pool.get(pool.size()-1);
    }

    private WeaponDef randomWeaponDef(int luck) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        List<WeaponDef> pool = new ArrayList<>(weapons.values());
        int total = 0;
        int[] weights = new int[pool.size()];
        for (int i = 0; i < pool.size(); i++) {
            WeaponDef def = pool.get(i);
            double mult = 1.0 + Math.max(0, luck) * 0.008 * def.rarityHint.rank();
            int w = (int)Math.max(1, Math.round(def.rarityHint.weight * mult));
            weights[i] = w; total += w;
        }
        int pick = rnd.nextInt(Math.max(1, total));
        for (int i = 0; i < pool.size(); i++) if ((pick -= weights[i]) < 0) return pool.get(i);
        return pool.get(0);
    }

    private WeaponTier randomWeaponTier(int luck) {
        int c = bias(70, 0, luck);
        int u = bias(22, 1, luck);
        int r = bias(7,  2, luck);
        int e = bias(1,  3, luck);
        int total = c+u+r+e;
        int pick = ThreadLocalRandom.current().nextInt(total);
        if ((pick -= c) < 0) return WeaponTier.COMMON;
        if ((pick -= u) < 0) return WeaponTier.UNCOMMON;
        if ((pick -= r) < 0) return WeaponTier.RARE;
        return WeaponTier.EPIC;
    }
    private int bias(int base, int rank, int luck) {
        double mult = 1.0 + Math.max(0, luck) * 0.012 * rank;
        return (int)Math.max(1, Math.round(base * mult));
    }

    // ------------------- Formatierung -----------------------------------------

    private static List<String> formatMods(List<Mod> mods){
        List<String> out = new ArrayList<>();
        for (Mod m : mods) {
            String name = pretty(m.stat());
            int a = m.amount();
            String sign = a >= 0 ? "+" : "";
            String suffix = isPercent(m.stat()) ? "%" : (m.stat()== Stat.RANGE_PX ? "" : "");
            if (m.stat()==Stat.RANGE_PX) {
                out.add(sign + (a*6) + " " + name); // 1P ~ 6px
            } else {
                out.add(sign + a + suffix + " " + name);
            }
        }
        return out;
    }
    private static String compactMods(List<Mod> mods){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mods.size(); i++) {
            Mod m = mods.get(i);
            int a = m.amount(); String sign = a>=0?"+":"";
            if (m.stat()==Stat.RANGE_PX) sb.append(sign).append(a*6).append(" Range");
            else if (isPercent(m.stat())) sb.append(sign).append(a).append("% ").append(shortName(m.stat()));
            else sb.append(sign).append(a).append(" ").append(shortName(m.stat()));
            if (i < mods.size()-1) sb.append("  ");
        }
        return sb.toString();
    }

    private static boolean isPercent(Stat s){
        return switch (s) {
            case ARMOR_PCT, DODGE_PCT, DAMAGE_PCT, MELEE_PCT, RANGED_PCT, MAGIC_PCT,
                 CRIT_CHANCE_PCT, CRIT_DAMAGE_PCT, ATTACK_SPEED_PCT, PROJECTILE_SPEED_PCT,
                 PROJECTILE_SIZE_PCT, HOMING_CHANCE_PCT, HOMING_STRENGTH_PCT,
                 MOVE_SPEED_PCT, LIFESTEAL_PCT, BOSS_DAMAGE_PCT -> true;
            default -> false;
        };
    }
    private static String pretty(Stat s){
        return switch (s) {
            case MAX_HP -> "Max HP";
            case ARMOR_PCT -> "Armor";
            case DODGE_PCT -> "Dodge";
            case HP_REGEN_PS -> "HP/s";
            case DAMAGE_PCT -> "Damage";
            case MELEE_PCT -> "Melee Damage";
            case RANGED_PCT -> "Ranged Damage";
            case MAGIC_PCT -> "Magic Damage";
            case CRIT_CHANCE_PCT -> "Crit Chance";
            case CRIT_DAMAGE_PCT -> "Crit Damage";
            case ATTACK_SPEED_PCT -> "Attack Speed";
            case RANGE_PX -> "Range";
            case PROJECTILE_SPEED_PCT -> "Proj Speed";
            case PROJECTILE_SIZE_PCT -> "Proj Size";
            case MULTISHOT_FLAT -> "Multishot";
            case PIERCE_FLAT -> "Pierce";
            case HOMING_CHANCE_PCT -> "Homing Chance";
            case HOMING_STRENGTH_PCT -> "Homing Strength";
            case KNOCKBACK_PCT -> "Knockback";
            case MOVE_SPEED_PCT -> "Move Speed";
            case LIFESTEAL_PCT -> "Lifesteal";
            case LUCK_FLAT -> "Luck";
            case HARVESTING_FLAT -> "Harvesting";
            case BOSS_DAMAGE_PCT -> "Boss Damage";
        };
    }
    private static String shortName(Stat s){
        return switch (s) {
            case MAX_HP -> "HP";
            case ARMOR_PCT -> "Armor";
            case DODGE_PCT -> "Dodge";
            case HP_REGEN_PS -> "HP/s";
            case DAMAGE_PCT -> "Dmg";
            case MELEE_PCT -> "Melee";
            case RANGED_PCT -> "Ranged";
            case MAGIC_PCT -> "Magic";
            case CRIT_CHANCE_PCT -> "Crit";
            case CRIT_DAMAGE_PCT -> "CritDmg";
            case ATTACK_SPEED_PCT -> "AS";
            case RANGE_PX -> "Range";
            case PROJECTILE_SPEED_PCT -> "ProjSpd";
            case PROJECTILE_SIZE_PCT -> "ProjSize";
            case MULTISHOT_FLAT -> "Multi";
            case PIERCE_FLAT -> "Pierce";
            case HOMING_CHANCE_PCT -> "Homing";
            case HOMING_STRENGTH_PCT -> "HomingStr";
            case KNOCKBACK_PCT -> "KB";
            case MOVE_SPEED_PCT -> "Move";
            case LIFESTEAL_PCT -> "LS";
            case LUCK_FLAT -> "Luck";
            case HARVESTING_FLAT -> "Harv";
            case BOSS_DAMAGE_PCT -> "Boss";
        };
    }
}
