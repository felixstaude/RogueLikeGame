package de.felixstaude.roguelike.shop;

import de.felixstaude.roguelike.entity.Player;
import de.felixstaude.roguelike.input.Input;
import de.felixstaude.roguelike.items.Item;
import de.felixstaude.roguelike.items.ItemRarity;
import de.felixstaude.roguelike.items.Mod;
import de.felixstaude.roguelike.items.PassiveItemCatalog;
import de.felixstaude.roguelike.stats.EffectiveStats;
import de.felixstaude.roguelike.stats.Stat;
import de.felixstaude.roguelike.stats.StatRules;
import de.felixstaude.roguelike.stats.Stats;
import de.felixstaude.roguelike.util.Colors;
import de.felixstaude.roguelike.util.Draw;
import de.felixstaude.roguelike.util.Fonts;
import de.felixstaude.roguelike.util.ImageCache;
import de.felixstaude.roguelike.util.Layout;
import de.felixstaude.roguelike.weapons.WeaponCatalog;
import de.felixstaude.roguelike.weapons.WeaponDef;
import de.felixstaude.roguelike.weapons.WeaponHotbar;
import de.felixstaude.roguelike.weapons.WeaponInstance;
import de.felixstaude.roguelike.weapons.WeaponTier;
import de.felixstaude.roguelike.weapons.WeaponType;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Shop UI mit Single-Row Layout, Lock-Persistenz (eine Rotation) und Stat-Preview.
 */
public class Shop {

    public enum OfferType { PASSIVE, WEAPON }

    public static final class Offer {
        public final String offerId;
        public final OfferType type;
        public final String title;
        public final ItemRarity rarity;
        public final int price;
        public final List<Mod> mods;
        public final Item item;                  // wenn PASSIVE
        public final WeaponDef weaponDef;        // wenn WEAPON
        public final WeaponTier tier;            // wenn WEAPON
        public final String iconPath;            // optional, sonst aus id/type abgeleitet

        private Offer(String offerId, OfferType type, String title, ItemRarity rarity, int price, List<Mod> mods,
                      Item item, WeaponDef weaponDef, WeaponTier tier, String iconPath) {
            this.offerId = offerId;
            this.type = type;
            this.title = title;
            this.rarity = rarity;
            this.price = price;
            this.mods = List.copyOf(mods);
            this.item = item;
            this.weaponDef = weaponDef;
            this.tier = tier;
            this.iconPath = iconPath;
        }

        public static Offer passive(Item item) {
            String icon = "/icons/items/" + item.id + ".png";
            return new Offer(UUID.randomUUID().toString(), OfferType.PASSIVE, item.name, item.rarity, item.price,
                    item.mods, item, null, null, icon);
        }

        public static Offer weapon(WeaponDef def, WeaponTier tier) {
            String title = def.name + " [" + tier.name() + "]";
            ItemRarity rarity = rarityFromTier(def, tier);
            String icon = "/icons/weapons/" + def.type.name().toLowerCase(Locale.ROOT) + ".png";
            return new Offer(UUID.randomUUID().toString(), OfferType.WEAPON, title, rarity, def.price(tier),
                    def.mods(tier), null, def, tier, icon);
        }

        private static ItemRarity rarityFromTier(WeaponDef def, WeaponTier tier) {
            return switch (tier) {
                case COMMON -> def.rarityHint;
                case UNCOMMON -> bump(def.rarityHint);
                case RARE -> bump(bump(def.rarityHint));
                case EPIC -> ItemRarity.EPIC;
            };
        }

        private static ItemRarity bump(ItemRarity rarity) {
            return switch (rarity) {
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
            this.success = success;
            this.goldSpent = goldSpent;
            this.message = message;
        }
    }

    // --- Layout-Konstanten ---
    private static final int PANEL_PADDING = 24;
    private static final int PANEL_ARC = 24;
    private static final int TOPBAR_HEIGHT = 56;

    private static final int CARD_WIDTH = 280;
    private static final int CARD_HEIGHT = 180;
    private static final int CARD_GAP = 24;
    private static final int CARD_ICON_SIZE = 80;
    private static final int CARD_ICON_RADIUS = 8;

    private static final int PRICE_WIDTH = 86;
    private static final int PRICE_HEIGHT = 28;
    private static final int BUY_WIDTH = 96;
    private static final int BUY_HEIGHT = 28;
    private static final int LOCK_SIZE = 28;

    private static final int HOTBAR_HEIGHT = 96;
    private static final int HOTBAR_SLOT_WIDTH = 240;
    private static final int HOTBAR_SLOT_HEIGHT = 58;
    private static final int HOTBAR_GAP = 14;
    private static final int SELL_WIDTH = 60;
    private static final int SELL_HEIGHT = 32;

    private static final String LOCK_TOOLTIP = "Lock für nächste Rotation";

    // --- Datenquellen / Pools ---
    private final List<Item> passivePool = PassiveItemCatalog.all();
    private final Map<WeaponType, WeaponDef> weapons = new HashMap<>(WeaponCatalog.all());

    // --- Run- / Shop-States ---
    private final Set<String> boughtUniques = new HashSet<>();
    private final Set<String> lockedOfferIds = new HashSet<>();
    private final List<Offer> carryLockedNextShop = new ArrayList<>();

    private int shopRerollDiscount = 0;
    private int shopPriceDiscountPct = 0;

    private final List<Offer> offers = new ArrayList<>();
    private final List<CardUI> cardUIs = new ArrayList<>();
    private final List<HotbarSlotUI> hotbarUIs = new ArrayList<>();

    private final Stats passiveStats = new Stats();
    private final WeaponHotbar hotbar = new WeaponHotbar();

    private EffectiveStats.Base baseStats = new EffectiveStats.Base();

    // --- Layout-Rechtecke ---
    private Rectangle panelRect = new Rectangle();
    private Rectangle topbarRect = new Rectangle();
    private Rectangle statsRect = new Rectangle();
    private Rectangle cardsRect = new Rectangle();
    private Rectangle hotbarRect = new Rectangle();
    private Rectangle rerollBtn = new Rectangle();
    private Rectangle nextBtn = new Rectangle();
    private Rectangle messageRect = new Rectangle();

    // --- Canvas/Pointer ---
    private int canvasW = 1280;
    private int canvasH = 720;
    private int pointerX = 0;
    private int pointerY = 0;

    // --- Hover/Focus ---
    private int hoverCardIndex = -1;
    private int keyboardFocusIndex = -1;

    // --- Flags ---
    private boolean layoutDirty = true;

    // --- ökonomische Regeln ---
    private int rerollCost = 6;
    private int rerollsThisPhase = 0;

    // --- UI Messages ---
    private String lastMessage = "";
    private Color messageColor = Colors.TEXT_SECONDARY;

    // ====================================================================== //
    //                             PUBLIC API                                 //
    // ====================================================================== //

    public void setCanvasSize(int width, int height) {
        if (width != this.canvasW || height != this.canvasH) {
            this.canvasW = width;
            this.canvasH = height;
            markLayoutDirty();
        }
    }

    public void updatePointer(int x, int y) {
        this.pointerX = x;
        this.pointerY = y;
        if (!layoutDirty) refreshHover();
    }

    /** Shop-Phase starten / neu befüllen (nach einer Wave). */
    public void prepareForWave(int wave, Player player) {
        updateBaseFromPlayer(player);
        rerollCost = Math.max(2, 6 - shopRerollDiscount);
        rerollsThisPhase = 0;

        // Locks aus vorherigem Shop einmalig übernehmen
        List<OfferSlot> preserved = new ArrayList<>();
        for (int i = 0; i < carryLockedNextShop.size(); i++) {
            preserved.add(new OfferSlot(i, carryLockedNextShop.get(i)));
        }
        carryLockedNextShop.clear();
        rebuildOffers(0, 0, preserved);

        lastMessage = "";
        messageColor = Colors.TEXT_SECONDARY;
        markLayoutDirty();
    }

    /** Input im Shop behandeln. true = Start nächste Wave. */
    public boolean handleInput(Input input, Player player) {
        ensureLayout();
        sanitizeFocus();

        if (input.mousePressedL) {
            // SELL in Hotbar?
            if (trySellWeapon(input, player)) return false;

            // Lock-Button auf Karten?
            if (handleLockClick(input)) return false;

            // BUY-Button auf Karten?
            if (handleBuyClick(input, player)) return false;

            // Reroll?
            if (rerollBtn.contains(input.mouseX, input.mouseY)) {
                int cost = getRerollCost();
                if (player.gold >= cost) {
                    player.gold -= cost;
                    reroll(0, 0);
                    lastMessage = "Shop rerolled (−" + cost + "G)";
                    messageColor = Colors.TEXT_SECONDARY;
                } else {
                    lastMessage = "Nicht genug Gold für Reroll (" + cost + "G)";
                    messageColor = Colors.DANGER;
                }
                return false;
            }

            // Next Wave?
            if (nextBtn.contains(input.mouseX, input.mouseY)) {
                stashLockedForNextShop();
                return true;
            }
        }

        // Shortcuts: 1..6 = Buy
        for (int i = 0; i < offers.size() && i < 6; i++) {
            if (input.wasPressed(KeyEvent.VK_1 + i)) {
                keyboardFocusIndex = i;
                attemptPurchase(i, player);
                return false;
            }
        }

        // R = Reroll
        if (input.wasPressed(KeyEvent.VK_R)) {
            int cost = getRerollCost();
            if (player.gold >= cost) {
                player.gold -= cost;
                reroll(0, 0);
                lastMessage = "Shop rerolled (−" + cost + "G)";
                messageColor = Colors.TEXT_SECONDARY;
            } else {
                lastMessage = "Nicht genug Gold für Reroll (" + cost + "G)";
                messageColor = Colors.DANGER;
            }
            return false;
        }

        // SPACE = Start next wave
        if (input.wasPressed(KeyEvent.VK_SPACE)) {
            stashLockedForNextShop();
            return true;
        }

        // L = Lock/Unlock fokussierte Karte
        if (input.wasPressed(KeyEvent.VK_L)) {
            int focus = currentFocusIndex();
            if (focus >= 0) toggleLock(focus);
        }

        return false;
    }

    /** Shop zeichnen. */
    public void render(Graphics2D g, Player player, int wave) {
        ensureLayout();

        // Stat-Snapshots: Basis & ggf. Preview (Hover/Focus)
        StatsSnapshot baseSnapshot = computeSnapshot(passiveStats, hotbar.getSlots());
        int previewIndex = previewCardIndex();
        StatsSnapshot previewSnapshot = previewIndex >= 0
                ? computeSnapshotForOffer(offers.get(previewIndex))
                : baseSnapshot;

        // Dimmer
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, canvasW, canvasH);
        Draw.globalHints(g);

        // Panel
        Draw.drawShadowedPanel(g, panelRect, PANEL_ARC, Colors.PANEL_SHADOW, 0, 10, Colors.PANEL_BG, Colors.PANEL_BORDER);

        drawTopBar(g, player, wave);
        drawStatsPanel(g, baseSnapshot, previewSnapshot, previewIndex >= 0);
        Point tooltip = drawCards(g, player, previewIndex);
        drawHotbar(g);
        drawMessage(g);
        if (tooltip != null) drawLockTooltip(g, tooltip);
    }

    // Reset für neuen Run
    public void resetRun() {
        boughtUniques.clear();
        shopRerollDiscount = 0;
        shopPriceDiscountPct = 0;
        offers.clear();
        lockedOfferIds.clear();
        carryLockedNextShop.clear();
        passiveStats.clear();
        hotbar.getSlots().clear();
        rerollCost = Math.max(2, 6 - shopRerollDiscount);
        rerollsThisPhase = 0;
        lastMessage = "";
        messageColor = Colors.TEXT_SECONDARY;
        markLayoutDirty();
    }

    public List<Offer> getOffers() { return offers; }
    public int getRerollCost() { return rerollCost; }

    public void rollOffers(int luck, int harvesting) {
        rerollCost = Math.max(2, 6 - shopRerollDiscount);
        rerollsThisPhase = 0;
        rebuildOffers(luck, harvesting, List.of());
        markLayoutDirty();
    }

    public void reroll(int luck, int harvesting) {
        rerollsThisPhase++;
        rerollCost += 4;

        // gelockte Offers erhalten
        List<OfferSlot> preserved = new ArrayList<>();
        for (int i = 0; i < offers.size(); i++) {
            Offer offer = offers.get(i);
            if (lockedOfferIds.contains(offer.offerId)) {
                preserved.add(new OfferSlot(i, offer));
            }
        }
        rebuildOffers(luck, harvesting, preserved);
    }

    public PurchaseResult buy(int index, Stats passiveStats, WeaponHotbar hotbar, int goldAvailable) {
        if (index < 0 || index >= offers.size()) {
            return new PurchaseResult(false, 0, "Ungültiger Index.");
        }
        Offer offer = offers.get(index);
        int price = applyDiscounts(offer.price);
        if (goldAvailable < price) {
            return new PurchaseResult(false, 0, "Nicht genug Gold.");
        }
        return switch (offer.type) {
            case PASSIVE -> {
                Item item = offer.item;
                if (item.unique && boughtUniques.contains(item.id)) {
                    yield new PurchaseResult(false, 0, "Unique bereits gekauft.");
                }
                item.applyTo(passiveStats);
                if (item.unique) boughtUniques.add(item.id);

                // Shop-affine Items
                if ("u_lucky_charm".equals(item.id)) shopRerollDiscount = 2;
                if ("bargain_hunter".equals(item.id)) shopPriceDiscountPct = Math.min(50, shopPriceDiscountPct + 15);

                yield new PurchaseResult(true, price, "Gekauft: " + item.name);
            }
            case WEAPON -> {
                WeaponInstance instance = new WeaponInstance(offer.weaponDef, offer.tier);
                WeaponHotbar.Result result = hotbar.tryAddOrCombine(instance);
                if (!result.success()) {
                    yield new PurchaseResult(false, 0, "Hotbar voll (kein Combine möglich).");
                }
                yield new PurchaseResult(true, price, result.added() ? "Waffe hinzugefügt" : "Waffe kombiniert");
            }
        };
    }

    // ====================================================================== //
    //                              RENDERING                                 //
    // ====================================================================== //

    private void drawTopBar(Graphics2D g, Player player, int wave) {
        g.setFont(Fonts.bold(30));
        g.setColor(Colors.TEXT_PRIMARY);
        int titleBaseline = topbarRect.y + (topbarRect.height + g.getFontMetrics().getAscent()) / 2 - 6;
        g.drawString("Shop", topbarRect.x + 24, titleBaseline);

        boolean rerollHover = rerollBtn.contains(pointerX, pointerY);
        boolean nextHover = nextBtn.contains(pointerX, pointerY);

        g.setFont(Fonts.bold(16));
        Color rerollColor = player.gold >= getRerollCost()
                ? (rerollHover ? Colors.ACCENT_HOVER : Colors.ACCENT)
                : Colors.DISABLED;
        Draw.drawButton(g, rerollBtn, "Reroll (" + getRerollCost() + "G)", rerollColor, Colors.TEXT_PRIMARY);

        Color nextColor = nextHover ? Colors.SUCCESS_HOVER : Colors.SUCCESS;
        Draw.drawButton(g, nextBtn, "Start Next Wave", nextColor, Colors.TEXT_PRIMARY);

        // Info mittig zwischen Buttons
        int infoStart = rerollBtn.x + rerollBtn.width + 16;
        int infoEnd = nextBtn.x - 16;
        if (infoEnd > infoStart) {
            g.setFont(Fonts.regular(14));
            g.setColor(Colors.TEXT_SECONDARY);
            String info = String.format(Locale.ROOT, "Gold: %dG   •   Wave %d", player.gold, wave);
            int infoWidth = g.getFontMetrics().stringWidth(info);
            int infoX = infoStart + Math.max(0, (infoEnd - infoStart - infoWidth) / 2);
            int infoY = topbarRect.y + topbarRect.height - 14;
            g.drawString(info, infoX, infoY);
        }
    }

    private void drawStatsPanel(Graphics2D g, StatsSnapshot base, StatsSnapshot preview, boolean showDelta) {
        Draw.drawPanel(g, statsRect, 20, new Color(0x161F30), Colors.PANEL_BORDER);
        g.setFont(Fonts.bold(16));
        g.setColor(Colors.TEXT_PRIMARY);
        g.drawString("Stats", statsRect.x + 14, statsRect.y + 28);

        int rowHeight = 24;
        int baseline = statsRect.y + 52;
        for (PanelStat stat : PanelStat.values()) {
            StatLine line = stat.line(base, preview, baseStats);
            String valueText = line.valueText();
            String deltaText = showDelta ? line.deltaText() : "";

            g.setFont(Fonts.regular(14));
            g.setColor(Colors.TEXT_SECONDARY);
            g.drawString(stat.label(), statsRect.x + 14, baseline);

            g.setFont(Fonts.bold(14));
            g.setColor(Colors.TEXT_PRIMARY);
            int valueWidth = g.getFontMetrics().stringWidth(valueText);
            int valueX = statsRect.x + statsRect.width - 14 - valueWidth;
            if (deltaText != null && !deltaText.isEmpty()) {
                int deltaWidth = g.getFontMetrics().stringWidth(deltaText);
                valueX -= deltaWidth + 8;
                int sign = line.deltaSign();
                Color deltaColor = sign > 0 ? Colors.MOD_POSITIVE
                        : sign < 0 ? Colors.MOD_NEGATIVE : Colors.TEXT_SECONDARY;
                g.setColor(deltaColor);
                g.drawString(deltaText, statsRect.x + statsRect.width - 14 - deltaWidth, baseline);
                g.setColor(Colors.TEXT_PRIMARY);
            }
            g.drawString(valueText, valueX, baseline);

            baseline += rowHeight;
            if (baseline > statsRect.y + statsRect.height - 4) break;
        }
    }

    private Point drawCards(Graphics2D g, Player player, int previewIndex) {
        Point tooltip = null;
        for (int i = 0; i < cardUIs.size() && i < offers.size(); i++) {
            CardUI ui = cardUIs.get(i);
            Offer offer = offers.get(i);
            boolean locked = lockedOfferIds.contains(offer.offerId);
            boolean lockHover = ui.lockToggle.contains(pointerX, pointerY);
            drawCard(g, ui, offer, player, i, previewIndex == i, locked, lockHover);
            if (lockHover) {
                tooltip = new Point(ui.lockToggle.x + ui.lockToggle.width / 2, ui.lockToggle.y + ui.lockToggle.height + 8);
            }
        }
        return tooltip;
    }

    private void drawHotbar(Graphics2D g) {
        Draw.drawPanel(g, hotbarRect, 20, new Color(0x161F30), Colors.PANEL_BORDER);
        g.setFont(Fonts.bold(16));
        g.setColor(Colors.TEXT_PRIMARY);
        g.drawString("Weapons", hotbarRect.x + 18, hotbarRect.y + 26);
        for (HotbarSlotUI slot : hotbarUIs) drawHotbarSlot(g, slot);
    }

    private void drawMessage(Graphics2D g) {
        if (lastMessage == null || lastMessage.isEmpty()) return;
        g.setFont(Fonts.regular(14));
        g.setColor(messageColor);
        Draw.drawCenteredString(g, lastMessage, messageRect);
    }

    private void drawLockTooltip(Graphics2D g, Point anchor) {
        g.setFont(Fonts.bold(12));
        int textWidth = g.getFontMetrics().stringWidth(LOCK_TOOLTIP);
        int width = textWidth + 16;
        int height = 24;
        Rectangle box = new Rectangle(anchor.x - width / 2, anchor.y, width, height);
        Draw.drawBadge(g, box, LOCK_TOOLTIP, new Color(0x1F293D), Colors.TEXT_PRIMARY);
    }

    private void drawCard(Graphics2D g, CardUI ui, Offer offer, Player player, int index,
                          boolean preview, boolean locked, boolean lockHover) {
        boolean hovered = ui.bounds.contains(pointerX, pointerY);
        Color background = preview ? new Color(0x202B3F) : (hovered ? new Color(0x1B2436) : new Color(0x161F30));
        Color border = locked ? Colors.ACCENT : Colors.PANEL_BORDER;
        Draw.drawPanel(g, ui.bounds, 18, background, border);

        // Icon
        BufferedImage icon = ImageCache.get(offer.iconPath);
        Draw.drawIcon(g, icon, ui.icon, CARD_ICON_RADIUS, Colors.PANEL_BORDER);

        // Rarity
        g.setFont(Fonts.bold(12));
        Draw.drawBadge(g, ui.rarityBadge, offer.rarity.name(), Colors.rarity(offer.rarity), Colors.BADGE_TEXT);

        // Unique-Hinweis
        if (offer.type == OfferType.PASSIVE && offer.item.unique) {
            Rectangle unique = new Rectangle(ui.bounds.x + 16, ui.rarityBadge.y + ui.rarityBadge.height + 4, 70, 18);
            Draw.drawBadge(g, unique, "UNIQUE", new Color(0x4B566F), Colors.TEXT_PRIMARY);
        }

        boolean soldOut = offer.type == OfferType.PASSIVE && offer.item.unique && boughtUniques.contains(offer.item.id);

        // Titel
        g.setFont(Fonts.bold(18));
        g.setColor(soldOut ? Colors.TEXT_MUTED : Colors.TEXT_PRIMARY);
        int titleY = ui.icon.y + ui.icon.height + 28;
        g.drawString(offer.title, ui.bounds.x + 16, titleY);

        // Mod-Liste
        List<ModLine> mods = describeMods(offer.mods);
        g.setFont(Fonts.regular(14));
        int textY = titleY + 22;
        int maxLines = Math.max(0, (ui.price.y - textY - 8) / 18);
        for (int i = 0; i < Math.min(mods.size(), maxLines); i++) {
            ModLine line = mods.get(i);
            g.setColor(line.color());
            g.drawString(line.text(), ui.bounds.x + 16, textY);
            textY += 18;
        }

        // Preis
        int price = applyDiscounts(offer.price);
        boolean canAfford = player.gold >= price;
        g.setFont(Fonts.bold(14));
        Color priceColor = soldOut ? Colors.DISABLED : (canAfford ? Colors.SUCCESS : Colors.DANGER);
        Draw.drawPill(g, ui.price, price + "G", priceColor, Colors.TEXT_PRIMARY);

        // BUY
        boolean buyHover = ui.buy.contains(pointerX, pointerY);
        Color buyColor = soldOut ? Colors.DISABLED
                : (canAfford ? (buyHover ? Colors.ACCENT_HOVER : Colors.ACCENT) : Colors.DISABLED);
        Draw.drawButton(g, ui.buy, soldOut ? "SOLD" : "BUY", buyColor, Colors.TEXT_PRIMARY);

        // Tastaturhinweis
        g.setFont(Fonts.regular(12));
        g.setColor(Colors.TEXT_MUTED);
        g.drawString("[" + (index + 1) + "]", ui.buy.x + ui.buy.width + 6, ui.buy.y + ui.buy.height - 6);

        // Lock-Schalter
        drawLockToggle(g, ui.lockToggle, locked, lockHover);
    }

    private void drawHotbarSlot(Graphics2D g, HotbarSlotUI slot) {
        Draw.drawPanel(g, slot.bounds, 16, new Color(0x1B2334), Colors.PANEL_BORDER);
        if (slot.weapon != null) {
            g.setFont(Fonts.bold(14));
            g.setColor(Colors.TEXT_PRIMARY);
            g.drawString(slot.weapon.displayName(), slot.bounds.x + 14, slot.bounds.y + 22);

            g.setFont(Fonts.regular(12));
            g.setColor(Colors.TEXT_SECONDARY);
            g.drawString(compactMods(slot.weapon.mods()), slot.bounds.x + 14, slot.bounds.y + 40);

            boolean hoverSell = slot.sellButton.contains(pointerX, pointerY);
            g.setFont(Fonts.bold(12));
            Color sellColor = hoverSell ? Colors.DANGER_HOVER : Colors.DANGER;
            Draw.drawButton(g, slot.sellButton, "SELL (" + refundValue(slot.weapon) + "G)", sellColor, Colors.TEXT_PRIMARY);
        } else {
            g.setFont(Fonts.italic(12));
            g.setColor(Colors.TEXT_MUTED);
            g.drawString("Empty", slot.bounds.x + 14, slot.bounds.y + 34);
        }
    }

    private void drawLockToggle(Graphics2D g, Rectangle area, boolean locked, boolean hover) {
        Color base = locked ? Colors.ACCENT : Colors.TEXT_SECONDARY;
        if (hover) base = locked ? Colors.ACCENT_HOVER : Colors.TEXT_PRIMARY;

        int circleDiameter = Math.max(8, area.width - 8);
        int circleX = area.x + (area.width - circleDiameter) / 2;
        int circleY = area.y + 2;
        if (locked) {
            g.setColor(base);
            g.fillOval(circleX, circleY, circleDiameter, circleDiameter);
            g.setColor(Colors.PANEL_BG);
            g.fillOval(circleX + circleDiameter / 3, circleY + circleDiameter / 3, circleDiameter / 3, circleDiameter / 3);
        } else {
            g.setColor(base);
            g.drawOval(circleX, circleY, circleDiameter, circleDiameter);
        }
        Path2D pin = new Path2D.Double();
        int tipY = area.y + area.height - 2;
        int midX = area.x + area.width / 2;
        pin.moveTo(midX, circleY + circleDiameter + 2);
        pin.lineTo(midX - circleDiameter / 2.0, tipY);
        pin.lineTo(midX + circleDiameter / 2.0, tipY);
        pin.closePath();
        g.setColor(base);
        g.fill(pin);
    }

    // ====================================================================== //
    //                               LAYOUT                                   //
    // ====================================================================== //

    private void ensureLayout() {
        if (!layoutDirty) return;
        layoutDirty = false;
        cardUIs.clear();
        hotbarUIs.clear();

        int panelWidth = Math.min(1400, Math.max(1000, (int) Math.round(canvasW * 0.88)));
        int panelHeight = Math.min(660, (int) Math.round(canvasH * 0.82));
        panelRect = Layout.center(canvasW, canvasH, panelWidth, panelHeight);

        int contentX = panelRect.x + PANEL_PADDING;
        int contentY = panelRect.y + PANEL_PADDING;
        int contentWidth = panelRect.width - PANEL_PADDING * 2;

        topbarRect = new Rectangle(contentX, contentY, contentWidth, TOPBAR_HEIGHT);
        int buttonHeight = 40;
        int buttonY = topbarRect.y + (topbarRect.height - buttonHeight) / 2;
        rerollBtn = new Rectangle(topbarRect.x + 24, buttonY, 210, buttonHeight);
        nextBtn = new Rectangle(topbarRect.x + topbarRect.width - 24 - 220, buttonY, 220, buttonHeight);

        int mainTop = topbarRect.y + topbarRect.height + 24;
        int hotbarTop = panelRect.y + panelRect.height - PANEL_PADDING - HOTBAR_HEIGHT;
        int statsHeight = Math.max(CARD_HEIGHT, hotbarTop - mainTop - 24);

        // Stats links, Karten rechts daneben in EINER Reihe
        statsRect = new Rectangle(contentX, mainTop, 280, statsHeight);
        int cardsX = statsRect.x + statsRect.width + CARD_GAP;
        int cardsWidth = Math.max(CARD_WIDTH, contentX + contentWidth - cardsX);
        cardsRect = new Rectangle(cardsX, mainTop, cardsWidth, CARD_HEIGHT);

        List<Rectangle> cardBounds = oneRowCardBounds(offers.size(), CARD_WIDTH, CARD_GAP, cardsRect);
        for (int i = 0; i < cardBounds.size(); i++) {
            Rectangle bounds = cardBounds.get(i);
            CardUI ui = new CardUI();
            ui.index = i;
            ui.bounds = bounds;
            ui.icon = new Rectangle(bounds.x + 16, bounds.y + 16, CARD_ICON_SIZE, CARD_ICON_SIZE);
            int badgeX = ui.icon.x + ui.icon.width + 8;
            int badgeWidth = Math.max(60, bounds.x + bounds.width - badgeX - 16);
            ui.rarityBadge = new Rectangle(badgeX, bounds.y + 16, badgeWidth, 22);
            ui.price = new Rectangle(bounds.x + 16, bounds.y + bounds.height - 16 - PRICE_HEIGHT, PRICE_WIDTH, PRICE_HEIGHT);
            ui.buy = new Rectangle(bounds.x + bounds.width - 16 - BUY_WIDTH, bounds.y + bounds.height - 16 - BUY_HEIGHT, BUY_WIDTH, BUY_HEIGHT);
            ui.lockToggle = new Rectangle(bounds.x + bounds.width - 16 - LOCK_SIZE, bounds.y + 12, LOCK_SIZE, LOCK_SIZE);
            cardUIs.add(ui);
        }

        // Hotbar unten
        hotbarRect = new Rectangle(contentX, hotbarTop, contentWidth, HOTBAR_HEIGHT);
        int slotCount = hotbar.capacity();
        int totalSlotWidth = slotCount * HOTBAR_SLOT_WIDTH + (slotCount - 1) * HOTBAR_GAP;
        int slotStartX = hotbarRect.x + Math.max(0, (hotbarRect.width - totalSlotWidth) / 2);
        int slotY = hotbarRect.y + hotbarRect.height - HOTBAR_SLOT_HEIGHT - 16;
        List<WeaponInstance> slots = hotbar.getSlots();
        for (int i = 0; i < slotCount; i++) {
            Rectangle slotBounds = new Rectangle(slotStartX + i * (HOTBAR_SLOT_WIDTH + HOTBAR_GAP), slotY,
                    HOTBAR_SLOT_WIDTH, HOTBAR_SLOT_HEIGHT);
            HotbarSlotUI ui = new HotbarSlotUI();
            ui.index = i;
            ui.bounds = slotBounds;
            ui.sellButton = new Rectangle(slotBounds.x + slotBounds.width - SELL_WIDTH - 12,
                    slotBounds.y + slotBounds.height - SELL_HEIGHT - 8, SELL_WIDTH, SELL_HEIGHT);
            ui.weapon = i < slots.size() ? slots.get(i) : null;
            hotbarUIs.add(ui);
        }

        messageRect = new Rectangle(contentX, hotbarRect.y - 24, contentWidth, 18);

        refreshHover();
        sanitizeFocus();
    }

    /** simple eigene Variante einer „oneRowCardBounds“ Berechnung */
    private List<Rectangle> oneRowCardBounds(int count, int cardW, int gap, Rectangle area) {
        int n = Math.max(0, Math.min(count, 6));
        List<Rectangle> out = new ArrayList<>(n);
        if (n == 0) return out;
        int totalW = n * cardW + (n - 1) * gap;
        int startX = area.x + Math.max(0, (area.width - totalW) / 2);
        int y = area.y;
        for (int i = 0; i < n; i++) {
            int x = startX + i * (cardW + gap);
            out.add(new Rectangle(x, y, cardW, CARD_HEIGHT));
        }
        return out;
    }

    private void refreshHover() {
        hoverCardIndex = -1;
        for (int i = 0; i < cardUIs.size() && i < offers.size(); i++) {
            if (cardUIs.get(i).bounds.contains(pointerX, pointerY)) {
                hoverCardIndex = i;
                break;
            }
        }
    }

    private void sanitizeFocus() {
        int size = offers.size();
        if (hoverCardIndex >= size) hoverCardIndex = -1;
        if (keyboardFocusIndex >= size) keyboardFocusIndex = size - 1;
        if (keyboardFocusIndex < 0 || size == 0) keyboardFocusIndex = -1;
    }

    private int previewCardIndex() {
        if (hoverCardIndex >= 0 && hoverCardIndex < offers.size()) return hoverCardIndex;
        if (keyboardFocusIndex >= 0 && keyboardFocusIndex < offers.size()) return keyboardFocusIndex;
        return -1;
    }

    private int currentFocusIndex() {
        int hover = previewCardIndex();
        return hover >= 0 ? hover : -1;
    }

    private boolean handleBuyClick(Input input, Player player) {
        for (int i = 0; i < cardUIs.size() && i < offers.size(); i++) {
            if (cardUIs.get(i).buy.contains(input.mouseX, input.mouseY)) {
                keyboardFocusIndex = i;
                attemptPurchase(i, player);
                return true;
            }
        }
        return false;
    }

    private boolean handleLockClick(Input input) {
        for (int i = 0; i < cardUIs.size() && i < offers.size(); i++) {
            if (cardUIs.get(i).lockToggle.contains(input.mouseX, input.mouseY)) {
                toggleLock(i);
                return true;
            }
        }
        return false;
    }

    private void attemptPurchase(int index, Player player) {
        if (index < 0 || index >= offers.size()) return;
        Offer offer = offers.get(index);
        PurchaseResult result = buy(index, passiveStats, hotbar, player.gold);
        if (result.success) {
            player.gold -= result.goldSpent;
            offers.remove(index);
            lockedOfferIds.remove(offer.offerId);
            lastMessage = result.message + " (−" + result.goldSpent + "G)";
            messageColor = Colors.SUCCESS;
            markLayoutDirty();
        } else {
            lastMessage = result.message;
            messageColor = Colors.DANGER;
        }
        sanitizeFocus();
    }

    private void toggleLock(int index) {
        if (index < 0 || index >= offers.size()) return;
        Offer offer = offers.get(index);
        if (lockedOfferIds.remove(offer.offerId)) {
            lastMessage = "Unlock: " + offer.title;
        } else {
            lockedOfferIds.add(offer.offerId);
            lastMessage = "Lock: " + offer.title;
        }
        messageColor = Colors.TEXT_SECONDARY;
    }

    private void stashLockedForNextShop() {
        if (lockedOfferIds.isEmpty()) {
            carryLockedNextShop.clear();
            return;
        }
        carryLockedNextShop.clear();
        for (Offer offer : offers) {
            if (lockedOfferIds.contains(offer.offerId)) carryLockedNextShop.add(offer);
        }
        lockedOfferIds.clear();
    }

    private boolean trySellWeapon(Input input, Player player) {
        for (HotbarSlotUI slot : hotbarUIs) {
            if (slot.weapon == null) continue;
            if (slot.sellButton.contains(input.mouseX, input.mouseY)) {
                WeaponInstance removed = hotbar.remove(slot.index);
                if (removed != null) {
                    int refund = refundValue(removed);
                    player.gold += refund;
                    lastMessage = "Verkauft: " + removed.displayName() + " (+" + refund + "G)";
                    messageColor = Colors.SUCCESS;
                    markLayoutDirty();
                }
                return true;
            }
        }
        return false;
    }

    private int refundValue(WeaponInstance weapon) {
        return Math.max(1, (int) Math.round(weapon.price() * 0.5));
    }

    private void markLayoutDirty() { layoutDirty = true; }

    // ====================================================================== //
    //                           OFFER GENERATION                             //
    // ====================================================================== //

    private void rebuildOffers(int luck, int harvesting, List<OfferSlot> preserved) {
        int slots = computeSlots(harvesting);
        Offer[] newOffers = new Offer[slots];

        // gelockte an ursprünglichen/naheliegenden Index legen
        for (OfferSlot slot : preserved) {
            int idx = Math.max(0, Math.min(slots - 1, slot.index()));
            while (idx < slots && newOffers[idx] != null) idx++;
            if (idx >= slots) break;
            newOffers[idx] = slot.offer();
        }

        // rest auffüllen
        for (int i = 0; i < slots; i++) {
            if (newOffers[i] == null) newOffers[i] = randomOffer(luck);
        }

        offers.clear();
        for (Offer offer : newOffers) if (offer != null) offers.add(offer);

        // Locks säubern (nur ids, die noch existieren)
        lockedOfferIds.retainAll(currentOfferIds());
        markLayoutDirty();
        sanitizeFocus();
    }

    private Set<String> currentOfferIds() {
        Set<String> ids = new HashSet<>();
        for (Offer offer : offers) ids.add(offer.offerId);
        return ids;
    }

    private Offer randomOffer(int luck) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        boolean weapon = rnd.nextDouble() < 0.55;
        if (weapon) {
            WeaponDef def = randomWeaponDef(luck);
            WeaponTier tier = randomWeaponTier(luck);
            return Offer.weapon(def, tier);
        }
        return Offer.passive(randomPassiveItem(luck));
    }

    private Item randomPassiveItem(int luck) {
        List<Item> candidates = new ArrayList<>();
        for (Item item : passivePool) {
            if (item.unique && boughtUniques.contains(item.id)) continue;
            candidates.add(item);
        }
        List<Item> pool = candidates.isEmpty() ? passivePool : candidates;
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    private WeaponDef randomWeaponDef(int luck) {
        List<WeaponDef> pool = new ArrayList<>(weapons.values());
        int[] weights = new int[pool.size()];
        int total = 0;
        for (int i = 0; i < pool.size(); i++) {
            WeaponDef def = pool.get(i);
            double mult = 1.0 + Math.max(0, luck) * 0.008 * def.rarityHint.rank();
            int weight = (int) Math.max(1, Math.round(def.rarityHint.weight * mult));
            weights[i] = weight; total += weight;
        }
        int pick = ThreadLocalRandom.current().nextInt(Math.max(1, total));
        for (int i = 0; i < pool.size(); i++) {
            pick -= weights[i];
            if (pick < 0) return pool.get(i);
        }
        return pool.get(0);
    }

    private WeaponTier randomWeaponTier(int luck) {
        int common = bias(70, 0, luck);
        int uncommon = bias(22, 1, luck);
        int rare = bias(7, 2, luck);
        int epic = bias(1, 3, luck);
        int total = common + uncommon + rare + epic;
        int pick = ThreadLocalRandom.current().nextInt(Math.max(1, total));
        if ((pick -= common) < 0) return WeaponTier.COMMON;
        if ((pick -= uncommon) < 0) return WeaponTier.UNCOMMON;
        if ((pick -= rare) < 0) return WeaponTier.RARE;
        return WeaponTier.EPIC;
    }

    private int bias(int base, int rank, int luck) {
        double mult = 1.0 + Math.max(0, luck) * 0.012 * rank;
        return (int) Math.max(1, Math.round(base * mult));
    }

    private int computeSlots(int harvesting) {
        int slots = 4 + Integer.compare(harvesting, 0);
        return Math.max(2, Math.min(6, slots));
    }

    private int applyDiscounts(int price) {
        int discounted = price;
        if (shopPriceDiscountPct > 0) {
            discounted = (int) Math.round(price * (1.0 - shopPriceDiscountPct / 100.0));
        }
        return Math.max(1, discounted);
    }

    // ====================================================================== //
    //                         STATS / PREVIEW-LOGIK                          //
    // ====================================================================== //

    private void updateBaseFromPlayer(Player player) {
        EffectiveStats.Base base = new EffectiveStats.Base();
        base.baseMaxHp = (int) Math.round(player.maxHp);
        base.baseFireRate = player.fireRate;
        base.baseBulletDamage = player.bulletDamage;
        base.baseRangePx = (int) Math.round(player.bulletSpeed * player.bulletLife);
        base.baseProjectileSpeedMul = 1.0;
        base.baseProjectileSizeMul = 1.0;
        base.baseMoveSpeedMul = 1.0;
        base.baseLifestealFrac = Math.max(0.0, player.lifesteal);
        base.baseMultishot = Math.max(0, player.multishot);
        base.basePierce = Math.max(0, player.pierce);
        base.baseHomingChancePct = (int) Math.round(Math.max(0.0, Math.min(1.0, player.homingChance)) * 100.0);
        base.baseHomingStrengthPct = 0;
        baseStats = base;
    }

    private StatsSnapshot computeSnapshot(Stats passives, List<WeaponInstance> weapons) {
        Stats total = new Stats(passives);
        for (WeaponInstance weapon : weapons) {
            for (Mod mod : weapon.mods()) total.add(mod.stat(), mod.amount());
        }
        EffectiveStats effective = EffectiveStats.from(baseStats, total);
        return new StatsSnapshot(total, effective);
    }

    private StatsSnapshot computeSnapshotForOffer(Offer offer) {
        Stats passives = new Stats(passiveStats);
        List<WeaponInstance> weapons = cloneHotbar();
        if (offer.type == OfferType.PASSIVE) {
            if (offer.item.unique && boughtUniques.contains(offer.item.id)) {
                return computeSnapshot(passives, weapons);
            }
            offer.item.applyTo(passives);
        } else {
            WeaponInstance incoming = new WeaponInstance(offer.weaponDef, offer.tier);
            if (!simulateAddOrCombine(weapons, incoming)) {
                return computeSnapshot(passives, weapons);
            }
        }
        return computeSnapshot(passives, weapons);
    }

    private List<WeaponInstance> cloneHotbar() {
        List<WeaponInstance> copy = new ArrayList<>();
        for (WeaponInstance weapon : hotbar.getSlots()) {
            copy.add(new WeaponInstance(weapon.def, weapon.tier));
        }
        return copy;
    }

    private boolean simulateAddOrCombine(List<WeaponInstance> slots, WeaponInstance incoming) {
        WeaponInstance candidate = new WeaponInstance(incoming.def, incoming.tier);
        while (true) {
            int idx = indexOfSameTypeAndTier(slots, candidate);
            if (idx >= 0) {
                slots.remove(idx);
                candidate = new WeaponInstance(candidate.def, candidate.tier.next());
                if (candidate.tier == WeaponTier.EPIC) break;
                continue;
            }
            break;
        }
        if (slots.size() < hotbar.capacity()) {
            slots.add(candidate);
            return true;
        }
        return false;
    }

    private int indexOfSameTypeAndTier(List<WeaponInstance> slots, WeaponInstance weapon) {
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).sameTypeAndTier(weapon)) return i;
        }
        return -1;
    }

    private static List<ModLine> describeMods(List<Mod> mods) {
        List<ModLine> lines = new ArrayList<>();
        for (Mod mod : mods) {
            int amount = mod.amount();
            String text;
            if (mod.stat() == Stat.RANGE_PX) {
                int px = amount * StatRules.RANGE_POINT_TO_PX;
                text = String.format(Locale.ROOT, "%+d px %s", px, pretty(mod.stat()));
            } else if (isPercent(mod.stat())) {
                text = String.format(Locale.ROOT, "%+d%% %s", amount, pretty(mod.stat()));
            } else {
                text = String.format(Locale.ROOT, "%+d %s", amount, pretty(mod.stat()));
            }
            lines.add(new ModLine(text, colorForAmount(amount)));
        }
        return lines;
    }

    private static Color colorForAmount(int amount) {
        if (amount > 0) return Colors.MOD_POSITIVE;
        if (amount < 0) return Colors.MOD_NEGATIVE;
        return Colors.MOD_NEUTRAL;
    }

    private static String compactMods(List<Mod> mods) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mods.size(); i++) {
            Mod mod = mods.get(i);
            int amount = mod.amount();
            String piece;
            if (mod.stat() == Stat.RANGE_PX) {
                piece = String.format(Locale.ROOT, "%+d Range", amount * StatRules.RANGE_POINT_TO_PX);
            } else if (isPercent(mod.stat())) {
                piece = String.format(Locale.ROOT, "%+d%% %s", amount, shortName(mod.stat()));
            } else {
                piece = String.format(Locale.ROOT, "%+d %s", amount, shortName(mod.stat()));
            }
            sb.append(piece);
            if (i < mods.size() - 1) sb.append("  ");
        }
        return sb.toString();
    }

    private static boolean isPercent(Stat stat) {
        return switch (stat) {
            case ARMOR_PCT, DODGE_PCT, DAMAGE_PCT, MELEE_PCT, RANGED_PCT, MAGIC_PCT,
                    CRIT_CHANCE_PCT, CRIT_DAMAGE_PCT, ATTACK_SPEED_PCT, PROJECTILE_SPEED_PCT,
                    PROJECTILE_SIZE_PCT, HOMING_CHANCE_PCT, HOMING_STRENGTH_PCT, KNOCKBACK_PCT,
                    MOVE_SPEED_PCT, LIFESTEAL_PCT, BOSS_DAMAGE_PCT -> true;
            default -> false;
        };
    }

    private static String pretty(Stat stat) {
        return switch (stat) {
            case MAX_HP -> "Max HP";
            case ARMOR_PCT -> "Armor %";
            case DODGE_PCT -> "Dodge %";
            case HP_REGEN_PS -> "HP/s";
            case DAMAGE_PCT -> "Damage %";
            case MELEE_PCT -> "Melee Damage %";
            case RANGED_PCT -> "Ranged Damage %";
            case MAGIC_PCT -> "Magic Damage %";
            case CRIT_CHANCE_PCT -> "Crit Chance %";
            case CRIT_DAMAGE_PCT -> "Crit Damage %";
            case ATTACK_SPEED_PCT -> "Attack Speed %";
            case RANGE_PX -> "Range";
            case PROJECTILE_SPEED_PCT -> "Projectile Speed %";
            case PROJECTILE_SIZE_PCT -> "Projectile Size %";
            case MULTISHOT_FLAT -> "Multishot";
            case PIERCE_FLAT -> "Pierce";
            case HOMING_CHANCE_PCT -> "Homing Chance %";
            case HOMING_STRENGTH_PCT -> "Homing Strength %";
            case KNOCKBACK_PCT -> "Knockback %";
            case MOVE_SPEED_PCT -> "Move Speed %";
            case LIFESTEAL_PCT -> "Lifesteal %";
            case LUCK_FLAT -> "Luck";
            case HARVESTING_FLAT -> "Harvesting";
            case BOSS_DAMAGE_PCT -> "Boss Damage %";
        };
    }

    private static String shortName(Stat stat) {
        return switch (stat) {
            case MAX_HP -> "HP";
            case ARMOR_PCT -> "Armor";
            case DODGE_PCT -> "Dodge";
            case HP_REGEN_PS -> "HP/s";
            case DAMAGE_PCT -> "Damage";
            case MELEE_PCT -> "Melee";
            case RANGED_PCT -> "Ranged";
            case MAGIC_PCT -> "Magic";
            case CRIT_CHANCE_PCT -> "Crit";
            case CRIT_DAMAGE_PCT -> "CritDmg";
            case ATTACK_SPEED_PCT -> "AtkSpd";
            case RANGE_PX -> "Range";
            case PROJECTILE_SPEED_PCT -> "ProjSpd";
            case PROJECTILE_SIZE_PCT -> "ProjSize";
            case MULTISHOT_FLAT -> "Multi";
            case PIERCE_FLAT -> "Pierce";
            case HOMING_CHANCE_PCT -> "Homing";
            case HOMING_STRENGTH_PCT -> "HomingStr";
            case KNOCKBACK_PCT -> "Knockback";
            case MOVE_SPEED_PCT -> "Move";
            case LIFESTEAL_PCT -> "Lifesteal";
            case LUCK_FLAT -> "Luck";
            case HARVESTING_FLAT -> "Harvest";
            case BOSS_DAMAGE_PCT -> "Boss";
        };
    }

    private static double percentFromMultiplier(double multiplier) {
        return (multiplier - 1.0) * 100.0;
    }

    private static double armorFromMultiplier(double incomingMultiplier) {
        return (1.0 - incomingMultiplier) * 100.0;
    }

    private static int withBase(StatsSnapshot snapshot, Stat stat, int baseValue) {
        return baseValue + snapshot.raw().get(stat);
    }

    private static StatLine simple(Unit unit, double baseValue, double previewValue) {
        double diff = previewValue - baseValue;
        String delta = unit.formatDelta(diff);
        int sign = unit.isZero(diff) ? 0 : (diff > 0 ? 1 : -1);
        return new StatLine(unit.formatValue(baseValue), delta, sign);
    }

    private static StatLine simple(Unit unit, int baseValue, int previewValue) {
        return simple(unit, (double) baseValue, (double) previewValue);
    }

    private static StatLine combine(Unit unitA, double baseA, double previewA, Unit unitB, double baseB, double previewB) {
        double diffA = previewA - baseA;
        double diffB = previewB - baseB;
        String value = unitA.formatValue(baseA) + " / " + unitB.formatValue(baseB);
        if (unitA.isZero(diffA) && unitB.isZero(diffB)) {
            return new StatLine(value, "", 0);
        }
        String partA = unitA.isZero(diffA) ? unitA.formatDeltaZero() : unitA.formatDeltaRaw(diffA);
        String partB = unitB.isZero(diffB) ? unitB.formatDeltaZero() : unitB.formatDeltaRaw(diffB);
        String delta = "(" + partA + " / " + partB + ")";
        boolean positive = (!unitA.isZero(diffA) && diffA > 0) || (!unitB.isZero(diffB) && diffB > 0);
        boolean negative = (!unitA.isZero(diffA) && diffA < 0) || (!unitB.isZero(diffB) && diffB < 0);
        int sign = positive && negative ? 0 : (positive ? 1 : (negative ? -1 : 0));
        return new StatLine(value, delta, sign);
    }

    // ====================================================================== //
    //                              NESTED TYPES                               //
    // ====================================================================== //

    private enum PanelStat {
        MAX_HP(pretty(Stat.MAX_HP)) {
            @Override
            StatLine line(StatsSnapshot base, StatsSnapshot preview, EffectiveStats.Base baseStats) {
                return simple(Unit.INT, base.effective().maxHp, preview.effective().maxHp);
            }
        },
        ARMOR(pretty(Stat.ARMOR_PCT)) {
            @Override
            StatLine line(StatsSnapshot base, StatsSnapshot preview, EffectiveStats.Base baseStats) {
                double baseArmor = armorFromMultiplier(base.effective().incomingDamageMul);
                double previewArmor = armorFromMultiplier(preview.effective().incomingDamageMul);
                return simple(Unit.SIGNED_PERCENT, baseArmor, previewArmor);
            }
        },
        DODGE(pretty(Stat.DODGE_PCT)) {
            @Override
            StatLine line(StatsSnapshot base, StatsSnapshot preview, EffectiveStats.Base baseStats) {
                return simple(Unit.PERCENT, base.effective().dodgePct, preview.effective().dodgePct);
            }
        },
        HP_REGEN(pretty(Stat.HP_REGEN_PS)) {
            @Override
            StatLine line(StatsSnapshot base, StatsSnapshot preview, EffectiveStats.Base baseStats) {
                return simple(Unit.FLOAT1, base.effective().hpRegenPerSec, preview.effective().hpRegenPerSec);
            }
        },
        DAMAGE(pretty(Stat.DAMAGE_PCT)) {
            @Override
            StatLine line(StatsSnapshot base, StatsSnapshot preview, EffectiveStats.Base baseStats) {
                int baseValue = withBase(base, Stat.DAMAGE_PCT, baseStats.baseDamagePct);
                int previewValue = withBase(preview, Stat.DAMAGE_PCT, baseStats.baseDamagePct);
                return simple(Unit.SIGNED_PERCENT, baseValue, previewValue);
            }
        },
        MELEE(pretty(Stat.MELEE_PCT)) {
            @Override
            StatLine line(StatsSnapshot base, StatsSnapshot preview, EffectiveStats.Base baseStats) {
                int baseValue = withBase(base, Stat.MELEE_PCT, 0);
                int previewValue = withBase(preview, Stat.MELEE_PCT, 0);
                return simple(Unit.SIGNED_PERCENT, baseValue, previewValue);
            }
        },
        RANGED(pretty(Stat.RANGED_PCT)) {
            @Override
            StatLine line(StatsSnapshot base, StatsSnapshot preview, EffectiveStats.Base baseStats) {
                int baseValue = withBase(base, Stat.RANGED_PCT, baseStats.baseRangedPct);
                int previewValue = withBase(preview, Stat.RANGED_PCT, baseStats.baseRangedPct);
                return simple(Unit.SIGNED_PERCENT, baseValue, previewValue);
            }
        },
        MAGIC(pretty(Stat.MAGIC_PCT)) {
            @Override
            StatLine line(StatsSnapshot base, StatsSnapshot preview, EffectiveStats.Base baseStats) {
                int baseValue = withBase(base, Stat.MAGIC_PCT, 0);
                int previewValue = withBase(preview, Stat.MAGIC_PCT, 0);
                return simple(Unit.SIGNED_PERCENT, baseValue, previewValue);
            }
        },
        CRIT_CHANCE(pretty(Stat.CRIT_CHANCE_PCT)) {
            @Override
            StatLine line(StatsSnapshot base, StatsSnapshot preview, EffectiveStats.Base baseStats) {
                return simple(Unit.PERCENT, base.effective().critChancePct, preview.effective().critChancePct);
            }
        },
        CRIT_DAMAGE(pretty(Stat.CRIT_DAMAGE_PCT)) {
            @Override
            StatLine line(StatsSnapshot base, StatsSnapshot preview, EffectiveStats.Base baseStats) {
                double baseMultiplier = base.effective().critMultiplier * 100.0;
                double previewMultiplier = preview.effective().critMultiplier * 100.0;
                return simple(Unit.FLOAT1_PERCENT, baseMultiplier, previewMultiplier);
            }
        },
        ATTACK_SPEED("Fire Rate (/s)") {
            @Override
            StatLine line(StatsSnapshot base, StatsSnapshot preview, EffectiveStats.Base baseStats) {
                return simple(Unit.FLOAT1, base.effective().fireRate, preview.effective().fireRate);
            }
        },
        RANGE("Range (px)") {
            @Override
            StatLine line(StatsSnapshot base, StatsSnapshot preview, EffectiveStats.Base baseStats) {
                return simple(Unit.INT, base.effective().rangePx, preview.effective().rangePx);
            }
        },
        PROJECTILE("Projectile (spd/size)") {
            @Override
            StatLine line(StatsSnapshot base, StatsSnapshot preview, EffectiveStats.Base baseStats) {
                double baseSpeed = percentFromMultiplier(base.effective().projectileSpeedMul);
                double previewSpeed = percentFromMultiplier(preview.effective().projectileSpeedMul);
                double baseSize = percentFromMultiplier(base.effective().projectileSizeMul);
                double previewSize = percentFromMultiplier(preview.effective().projectileSizeMul);
                return combine(Unit.SIGNED_PERCENT, baseSpeed, previewSpeed,
                        Unit.SIGNED_PERCENT, baseSize, previewSize);
            }
        },
        MULTISHOT(pretty(Stat.MULTISHOT_FLAT)) {
            @Override
            StatLine line(StatsSnapshot base, StatsSnapshot preview, EffectiveStats.Base baseStats) {
                return simple(Unit.INT, base.effective().multishot, preview.effective().multishot);
            }
        },
        PIERCE(pretty(Stat.PIERCE_FLAT)) {
            @Override
            StatLine line(StatsSnapshot base, StatsSnapshot preview, EffectiveStats.Base baseStats) {
                return simple(Unit.INT, base.effective().pierce, preview.effective().pierce);
            }
        },
        HOMING("Homing (chance/str)") {
            @Override
            StatLine line(StatsSnapshot base, StatsSnapshot preview, EffectiveStats.Base baseStats) {
                double baseChance = base.effective().homingChance01 * 100.0;
                double previewChance = preview.effective().homingChance01 * 100.0;
                double baseStrength = percentFromMultiplier(base.effective().homingStrengthMul);
                double previewStrength = percentFromMultiplier(preview.effective().homingStrengthMul);
                return combine(Unit.PERCENT, baseChance, previewChance,
                        Unit.SIGNED_PERCENT, baseStrength, previewStrength);
            }
        },
        KNOCKBACK(pretty(Stat.KNOCKBACK_PCT)) {
            @Override
            StatLine line(StatsSnapshot base, StatsSnapshot preview, EffectiveStats.Base baseStats) {
                int baseValue = withBase(base, Stat.KNOCKBACK_PCT, 0);
                int previewValue = withBase(preview, Stat.KNOCKBACK_PCT, 0);
                return simple(Unit.SIGNED_PERCENT, baseValue, previewValue);
            }
        },
        MOVE_SPEED(pretty(Stat.MOVE_SPEED_PCT)) {
            @Override
            StatLine line(StatsSnapshot base, StatsSnapshot preview, EffectiveStats.Base baseStats) {
                double baseMove = percentFromMultiplier(base.effective().moveSpeedMul);
                double previewMove = percentFromMultiplier(preview.effective().moveSpeedMul);
                return simple(Unit.SIGNED_PERCENT, baseMove, previewMove);
            }
        },
        LIFESTEAL(pretty(Stat.LIFESTEAL_PCT)) {
            @Override
            StatLine line(StatsSnapshot base, StatsSnapshot preview, EffectiveStats.Base baseStats) {
                double baseValue = base.effective().lifestealFrac * 100.0;
                double previewValue = preview.effective().lifestealFrac * 100.0;
                return simple(Unit.FLOAT1_PERCENT, baseValue, previewValue);
            }
        },
        LUCK(pretty(Stat.LUCK_FLAT)) {
            @Override
            StatLine line(StatsSnapshot base, StatsSnapshot preview, EffectiveStats.Base baseStats) {
                return simple(Unit.INT, base.effective().luck, preview.effective().luck);
            }
        },
        HARVESTING(pretty(Stat.HARVESTING_FLAT)) {
            @Override
            StatLine line(StatsSnapshot base, StatsSnapshot preview, EffectiveStats.Base baseStats) {
                return simple(Unit.INT, base.effective().harvesting, preview.effective().harvesting);
            }
        },
        BOSS_DAMAGE(pretty(Stat.BOSS_DAMAGE_PCT)) {
            @Override
            StatLine line(StatsSnapshot base, StatsSnapshot preview, EffectiveStats.Base baseStats) {
                double baseValue = percentFromMultiplier(base.effective().bossDamageMul);
                double previewValue = percentFromMultiplier(preview.effective().bossDamageMul);
                return simple(Unit.SIGNED_PERCENT, baseValue, previewValue);
            }
        };

        private final String label;

        PanelStat(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }

        abstract StatLine line(StatsSnapshot base, StatsSnapshot preview, EffectiveStats.Base baseStats);
    }

    private record StatsSnapshot(Stats raw, EffectiveStats effective) {}
    private record ModLine(String text, Color color) {}
    private record OfferSlot(int index, Offer offer) {}
    private record StatLine(String valueText, String deltaText, int deltaSign) {}

    private static final class CardUI {
        int index;
        Rectangle bounds;
        Rectangle icon;
        Rectangle rarityBadge;
        Rectangle price;
        Rectangle buy;
        Rectangle lockToggle;
    }

    private static final class HotbarSlotUI {
        int index;
        Rectangle bounds;
        Rectangle sellButton;
        WeaponInstance weapon;
    }

    private enum Unit {
        INT(false, 0, ""),
        PERCENT(false, 0, "%"),
        SIGNED_PERCENT(true, 0, "%"),
        FLOAT1(false, 1, ""),
        FLOAT1_PERCENT(false, 1, "%");

        private final boolean signValue;
        private final int decimals;
        private final String suffix;

        Unit(boolean signValue, int decimals, String suffix) {
            this.signValue = signValue;
            this.decimals = decimals;
            this.suffix = suffix;
        }

        String formatValue(double value) {
            return format(value, signValue);
        }

        String formatDelta(double diff) {
            if (isZero(diff)) return "";
            return "(" + format(diff, true) + ")";
        }

        String formatDeltaRaw(double diff) {
            return format(diff, true);
        }

        String formatDeltaZero() { return "-"; }

        boolean isZero(double value) {
            return Math.abs(value) < epsilon();
        }

        private String format(double value, boolean withSign) {
            if (decimals == 0) {
                long rounded = Math.round(value);
                String pattern = withSign ? "%+d" : "%d";
                return String.format(Locale.ROOT, pattern, rounded) + suffix;
            }
            double pow = Math.pow(10, decimals);
            double rounded = Math.round(value * pow) / pow;
            if (Math.abs(rounded) < epsilon()) rounded = 0.0;
            String pattern = (withSign ? "%+" : "%") + "." + decimals + "f";
            return String.format(Locale.ROOT, pattern, rounded) + suffix;
        }

        private double epsilon() {
            return decimals == 0 ? 0.5 : 0.5 / Math.pow(10, decimals);
        }
    }
}
