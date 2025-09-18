package de.felixstaude.roguelike.shop;

import de.felixstaude.roguelike.entity.Player;
import de.felixstaude.roguelike.input.Input;
import de.felixstaude.roguelike.items.Item;
import de.felixstaude.roguelike.items.ItemRarity;
import de.felixstaude.roguelike.items.Mod;
import de.felixstaude.roguelike.items.PassiveItemCatalog;
import de.felixstaude.roguelike.stats.Stat;
import de.felixstaude.roguelike.stats.Stats;
import de.felixstaude.roguelike.util.Colors;
import de.felixstaude.roguelike.util.Draw;
import de.felixstaude.roguelike.util.Fonts;
import de.felixstaude.roguelike.util.Layout;
import de.felixstaude.roguelike.weapons.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Shop UI with single-row layout, rarity badges, top buttons and hotbar SELL support.
 */
public class Shop {

    public enum OfferType { PASSIVE, WEAPON }

    public static final class Offer {
        public final OfferType type;
        public final String title;
        public final ItemRarity rarity;
        public final int price;
        public final List<Mod> mods;
        public final Item item;
        public final WeaponDef weaponDef;
        public final WeaponTier tier;

        private Offer(OfferType type, String title, ItemRarity rarity, int price, List<Mod> mods,
                      Item item, WeaponDef weaponDef, WeaponTier tier) {
            this.type = type;
            this.title = title;
            this.rarity = rarity;
            this.price = price;
            this.mods = mods;
            this.item = item;
            this.weaponDef = weaponDef;
            this.tier = tier;
        }

        public static Offer passive(Item item) {
            return new Offer(OfferType.PASSIVE, item.name, item.rarity, item.price, item.mods, item, null, null);
        }

        public static Offer weapon(WeaponDef def, WeaponTier tier) {
            String title = def.name + " [" + tier.name() + "]";
            return new Offer(OfferType.WEAPON, title, rarityFromTier(def, tier), def.price(tier), def.mods(tier), null, def, tier);
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

    private final List<Item> passivePool = PassiveItemCatalog.all();
    private final Map<WeaponType, WeaponDef> weapons = new HashMap<>(WeaponCatalog.all());
    private final Set<String> boughtUniques = new HashSet<>();

    private int shopRerollDiscount = 0;
    private int shopPriceDiscountPct = 0;

    private final List<Offer> offers = new ArrayList<>();
    private int rerollCost = 6;
    private int rerollsThisPhase = 0;

    private final Stats passiveStats = new Stats();
    private final WeaponHotbar hotbar = new WeaponHotbar();

    private String lastMessage = "";
    private final List<CardUI> cardUIs = new ArrayList<>();
    private final List<HotbarSlotUI> hotbarUIs = new ArrayList<>();

    private Rectangle panelRect = new Rectangle();
    private Rectangle rerollBtn = new Rectangle();
    private Rectangle nextBtn = new Rectangle();
    private Rectangle hotbarRect = new Rectangle();
    private Rectangle messageRect = new Rectangle();

    private int canvasW = 1280;
    private int canvasH = 720;
    private int pointerX = 0;
    private int pointerY = 0;
    private boolean layoutDirty = true;

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
    }

    public void prepareForWave(int wave, Player player) {
        rollOffers(0, 0);
    }

    public boolean handleInput(Input input, Player player) {
        ensureLayout();

        if (input.mousePressedL) {
            if (trySellWeapon(input, player)) {
                return false;
            }

            for (int i = 0; i < cardUIs.size(); i++) {
                CardUI card = cardUIs.get(i);
                if (card.buyBtn.contains(input.mouseX, input.mouseY)) {
                    PurchaseResult result = buy(i, passiveStats, hotbar, player.gold);
                    if (result.success) {
                        player.gold -= result.goldSpent;
                        offers.remove(i);
                        lastMessage = result.message + " (−" + result.goldSpent + "G)";
                        markLayoutDirty();
                    } else {
                        lastMessage = result.message;
                    }
                    return false;
                }
            }

            if (rerollBtn.contains(input.mouseX, input.mouseY)) {
                int cost = getRerollCost();
                if (player.gold >= cost) {
                    player.gold -= cost;
                    reroll(0, 0);
                    lastMessage = "Shop rerolled (−" + cost + "G)";
                } else {
                    lastMessage = "Nicht genug Gold für Reroll (" + cost + "G)";
                }
                return false;
            }

            if (nextBtn.contains(input.mouseX, input.mouseY)) {
                return true;
            }
        }

        for (int i = 0; i < offers.size(); i++) {
            if (input.wasPressed(KeyEvent.VK_1 + i)) {
                PurchaseResult result = buy(i, passiveStats, hotbar, player.gold);
                if (result.success) {
                    player.gold -= result.goldSpent;
                    offers.remove(i);
                    lastMessage = result.message + " (−" + result.goldSpent + "G)";
                    markLayoutDirty();
                } else {
                    lastMessage = result.message;
                }
                return false;
            }
        }

        if (input.wasPressed(KeyEvent.VK_R)) {
            int cost = getRerollCost();
            if (player.gold >= cost) {
                player.gold -= cost;
                reroll(0, 0);
                lastMessage = "Shop rerolled (−" + cost + "G)";
            } else {
                lastMessage = "Nicht genug Gold für Reroll (" + cost + "G)";
            }
        }

        if (input.wasPressed(KeyEvent.VK_SPACE)) {
            return true;
        }

        return false;
    }

    public void render(Graphics2D g, Player player, int wave) {
        ensureLayout();

        g.setColor(new Color(0, 0, 0, 160));
        g.fillRect(0, 0, canvasW, canvasH);

        Draw.drawShadowedPanel(g, panelRect, 22, Colors.PANEL_SHADOW, 6, 8, Colors.PANEL_BG, Colors.PANEL_BORDER);

        g.setFont(Fonts.bold(22));
        g.setColor(Colors.TEXT_PRIMARY);
        Rectangle titleRect = new Rectangle(panelRect.x, panelRect.y + 26, panelRect.width, 36);
        Draw.drawCenteredString(g, "SHOP – Wave " + wave + " beendet", titleRect);

        g.setFont(Fonts.regular(15));
        g.setColor(Colors.TEXT_SECONDARY);
        String hint = "Gold: " + player.gold + "   •   Reroll: " + getRerollCost() + "G   •   Klick BUY oder 1.." + Math.max(1, offers.size()) + "   •   SPACE: Start";
        Rectangle hintRect = new Rectangle(panelRect.x, titleRect.y + titleRect.height + 4, panelRect.width, 24);
        Draw.drawCenteredString(g, hint, hintRect);

        drawButtons(g, player);

        g.setFont(Fonts.regular(14));
        for (int i = 0; i < cardUIs.size(); i++) {
            drawCard(g, cardUIs.get(i), offers.get(i), player);
        }

        drawHotbar(g);

        if (!lastMessage.isEmpty()) {
            g.setFont(Fonts.regular(15));
            g.setColor(Colors.WARNING);
            Draw.drawCenteredString(g, lastMessage, messageRect);
        }
    }

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
        markLayoutDirty();
    }

    public List<Offer> getOffers() { return offers; }
    public int getRerollCost() { return rerollCost; }

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
        markLayoutDirty();
    }

    public void reroll(int luck, int harvesting) {
        rerollsThisPhase++;
        rerollCost += 4;
        rollOffers(luck, harvesting);
    }

    public PurchaseResult buy(int index, Stats passiveStats, WeaponHotbar hotbar, int goldAvailable) {
        if (index < 0 || index >= offers.size()) return new PurchaseResult(false, 0, "Ungültiger Index.");
        Offer offer = offers.get(index);

        int price = applyDiscounts(offer.price);
        if (goldAvailable < price) return new PurchaseResult(false, 0, "Nicht genug Gold.");

        switch (offer.type) {
            case PASSIVE -> {
                Item item = offer.item;
                if (item.unique && boughtUniques.contains(item.id)) {
                    return new PurchaseResult(false, 0, "Unique bereits gekauft.");
                }
                item.applyTo(passiveStats);
                if (item.unique) boughtUniques.add(item.id);
                if ("u_lucky_charm".equals(item.id)) shopRerollDiscount = 2;
                if ("bargain_hunter".equals(item.id)) shopPriceDiscountPct = Math.min(50, shopPriceDiscountPct + 15);
                return new PurchaseResult(true, price, "Gekauft: " + item.name);
            }
            case WEAPON -> {
                WeaponInstance instance = new WeaponInstance(offer.weaponDef, offer.tier);
                WeaponHotbar.Result res = hotbar.tryAddOrCombine(instance);
                if (!res.success()) return new PurchaseResult(false, 0, "Hotbar voll (kein Combine möglich).");
                return new PurchaseResult(true, price, res.added() ? "Waffe hinzugefügt" : "Waffe kombiniert");
            }
        }
        return new PurchaseResult(false, 0, "Unbekannter Typ.");
    }
    private void ensureLayout() {
        if (!layoutDirty) {
            return;
        }
        layoutDirty = false;

        cardUIs.clear();
        hotbarUIs.clear();

        int margin = Math.max(48, Math.min(canvasW, canvasH) / 12);
        int panelW = Math.min(1180, canvasW - margin * 2);
        panelW = Math.max(760, panelW);
        int basePanelH = 560;
        int panelH = Math.max(basePanelH, Math.min(canvasH - margin * 2, 700));
        panelRect = Layout.center(canvasW, canvasH, panelW, panelH);

        int padding = 32;
        int titleHeight = 48;
        int buttonHeight = 44;
        int buttonY = panelRect.y + padding + titleHeight;

        rerollBtn = Layout.alignLeft(panelRect, padding, buttonY, 200, buttonHeight);
        nextBtn = Layout.alignRight(panelRect, padding, buttonY, 220, buttonHeight);

        int cardsTop = buttonY + buttonHeight + 28;
        int cardHeight = 220;
        List<Rectangle> cardRects = Layout.cardRow(panelRect, padding, cardsTop, cardHeight, offers.size(), 24);
        for (Rectangle rect : cardRects) {
            CardUI ui = new CardUI();
            ui.bounds = rect;
            int pillWidth = Math.min(120, rect.width / 3);
            int pillHeight = 32;
            ui.priceTag = new Rectangle(rect.x + 16, rect.y + rect.height - pillHeight - 16, pillWidth, pillHeight);
            int buttonWidth = Math.min(140, rect.width / 3 + 40);
            ui.buyBtn = new Rectangle(rect.x + rect.width - buttonWidth - 16, rect.y + rect.height - pillHeight - 16, buttonWidth, pillHeight);
            int badgeWidth = Math.min(140, rect.width / 2);
            ui.rarityTag = new Rectangle(rect.x + 16, rect.y + 16, badgeWidth, 28);
            cardUIs.add(ui);
        }

        int hotbarHeight = 140;
        hotbarRect = Layout.hotbarArea(panelRect, padding, hotbarHeight);
        Rectangle slotsArea = new Rectangle(hotbarRect.x, hotbarRect.y + 48, hotbarRect.width, hotbarHeight - 56);
        List<Rectangle> slotRects = Layout.distributeHorizontally(new Rectangle(slotsArea.x, slotsArea.y, slotsArea.width, 72), hotbar.capacity(), 18);
        List<WeaponInstance> slots = hotbar.getSlots();
        for (int i = 0; i < slotRects.size(); i++) {
            HotbarSlotUI ui = new HotbarSlotUI();
            ui.index = i;
            ui.bounds = slotRects.get(i);
            ui.weapon = i < slots.size() ? slots.get(i) : null;
            ui.sellBtn = new Rectangle(ui.bounds.x + ui.bounds.width - 78, ui.bounds.y + ui.bounds.height - 34, 68, 28);
            hotbarUIs.add(ui);
        }

        messageRect = new Rectangle(panelRect.x + padding, hotbarRect.y - 28, panelRect.width - padding * 2, 24);
    }

    private void drawButtons(Graphics2D g, Player player) {
        int cost = getRerollCost();
        boolean canReroll = player.gold >= cost;
        Color rerollColor = canReroll ? (isHover(rerollBtn) ? Colors.BUTTON_PRIMARY_HOVER : Colors.BUTTON_PRIMARY) : Colors.BUTTON_DISABLED;
        g.setFont(Fonts.bold(15));
        Draw.drawButton(g, rerollBtn, "Reroll (" + cost + "G)", rerollColor, Colors.TEXT_PRIMARY);

        Color startColor = isHover(nextBtn) ? Colors.SUCCESS_HOVER : Colors.SUCCESS;
        Draw.drawButton(g, nextBtn, "Start Next Wave", startColor, Colors.TEXT_PRIMARY);
    }

    private void drawCard(Graphics2D g, CardUI ui, Offer offer, Player player) {
        boolean hover = ui.bounds.contains(pointerX, pointerY);
        g.setColor(hover ? new Color(38, 42, 58) : new Color(30, 34, 46));
        g.fillRoundRect(ui.bounds.x, ui.bounds.y, ui.bounds.width, ui.bounds.height, 20, 20);
        g.setColor(new Color(62, 72, 94));
        g.drawRoundRect(ui.bounds.x, ui.bounds.y, ui.bounds.width, ui.bounds.height, 20, 20);

        g.setFont(Fonts.bold(12));
        Draw.drawBadge(g, ui.rarityTag, offer.rarity.name(), Colors.rarity(offer.rarity), Colors.BADGE_TEXT);

        g.setFont(Fonts.bold(18));
        g.setColor(Colors.TEXT_PRIMARY);
        g.drawString(offer.title, ui.bounds.x + 20, ui.bounds.y + 58);

        g.setFont(Fonts.regular(14));
        int textY = ui.bounds.y + 88;
        for (String line : formatMods(offer.mods)) {
            g.setColor(colorForLine(line));
            g.drawString("• " + line, ui.bounds.x + 22, textY);
            textY += 20;
            if (textY > ui.bounds.y + ui.bounds.height - 76) break;
        }

        int price = applyDiscounts(offer.price);
        boolean affordable = player.gold >= price;
        Color pillColor = affordable ? Colors.SUCCESS : Colors.DANGER;
        g.setFont(Fonts.bold(14));
        Draw.drawPill(g, ui.priceTag, price + "G", pillColor, Colors.TEXT_PRIMARY);

        Color buttonColor = affordable ? (isHover(ui.buyBtn) ? Colors.BUTTON_PRIMARY_HOVER : Colors.BUTTON_PRIMARY) : Colors.BUTTON_DISABLED;
        Draw.drawButton(g, ui.buyBtn, "BUY", buttonColor, Colors.TEXT_PRIMARY);
    }

    private void drawHotbar(Graphics2D g) {
        g.setColor(new Color(12, 16, 24, 160));
        g.fillRoundRect(hotbarRect.x, hotbarRect.y, hotbarRect.width, hotbarRect.height, 20, 20);
        g.setColor(new Color(52, 60, 78, 200));
        g.drawRoundRect(hotbarRect.x, hotbarRect.y, hotbarRect.width, hotbarRect.height, 20, 20);

        g.setFont(Fonts.bold(16));
        g.setColor(Colors.TEXT_PRIMARY);
        g.drawString("Weapons", hotbarRect.x + 18, hotbarRect.y + 30);

        for (HotbarSlotUI slot : hotbarUIs) {
            drawHotbarSlot(g, slot);
        }
    }

    private void drawHotbarSlot(Graphics2D g, HotbarSlotUI slot) {
        g.setColor(new Color(30, 34, 46));
        g.fillRoundRect(slot.bounds.x, slot.bounds.y, slot.bounds.width, slot.bounds.height, 16, 16);
        g.setColor(new Color(62, 72, 94));
        g.drawRoundRect(slot.bounds.x, slot.bounds.y, slot.bounds.width, slot.bounds.height, 16, 16);

        if (slot.weapon != null) {
            g.setFont(Fonts.bold(14));
            g.setColor(Colors.TEXT_PRIMARY);
            g.drawString(slot.weapon.displayName(), slot.bounds.x + 16, slot.bounds.y + 24);

            g.setFont(Fonts.regular(12));
            g.setColor(Colors.TEXT_SECONDARY);
            g.drawString(compactMods(slot.weapon.mods()), slot.bounds.x + 16, slot.bounds.y + 44);

            int refund = refundValue(slot.weapon);
            g.setFont(Fonts.bold(13));
            Color sellColor = isHover(slot.sellBtn) ? Colors.DANGER.brighter() : Colors.DANGER;
            Draw.drawButton(g, slot.sellBtn, "SELL (" + refund + "G)", sellColor, Colors.TEXT_PRIMARY);
        } else {
            g.setFont(Fonts.italic(13));
            g.setColor(Colors.TEXT_MUTED);
            g.drawString("Empty", slot.bounds.x + 16, slot.bounds.y + 36);
        }
    }

    private boolean trySellWeapon(Input input, Player player) {
        for (HotbarSlotUI slot : hotbarUIs) {
            if (slot.weapon == null) continue;
            if (slot.sellBtn.contains(input.mouseX, input.mouseY)) {
                WeaponInstance removed = hotbar.remove(slot.index);
                if (removed != null) {
                    int refund = refundValue(removed);
                    player.gold += refund;
                    lastMessage = "Verkauft: " + removed.displayName() + " (+" + refund + "G)";
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

    private boolean isHover(Rectangle rect) {
        return rect.contains(pointerX, pointerY);
    }

    private void markLayoutDirty() {
        layoutDirty = true;
    }

    private static final class CardUI {
        Rectangle bounds;
        Rectangle buyBtn;
        Rectangle priceTag;
        Rectangle rarityTag;
    }

    private static final class HotbarSlotUI {
        int index;
        Rectangle bounds;
        Rectangle sellBtn;
        WeaponInstance weapon;
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

    private Item randomPassiveItem(int luck) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int pick = rnd.nextInt(passivePool.size());
        return passivePool.get(pick);
    }

    private WeaponDef randomWeaponDef(int luck) {
        List<WeaponDef> pool = new ArrayList<>(weapons.values());
        int[] weights = new int[pool.size()];
        int total = 0;
        for (int i = 0; i < pool.size(); i++) {
            WeaponDef def = pool.get(i);
            double mult = 1.0 + Math.max(0, luck) * 0.008 * def.rarityHint.rank();
            int weight = (int) Math.max(1, Math.round(def.rarityHint.weight * mult));
            weights[i] = weight;
            total += weight;
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
        int pick = ThreadLocalRandom.current().nextInt(total);
        if ((pick -= common) < 0) return WeaponTier.COMMON;
        if ((pick -= uncommon) < 0) return WeaponTier.UNCOMMON;
        if ((pick -= rare) < 0) return WeaponTier.RARE;
        return WeaponTier.EPIC;
    }

    private int bias(int base, int rank, int luck) {
        double mult = 1.0 + Math.max(0, luck) * 0.012 * rank;
        return (int) Math.max(1, Math.round(base * mult));
    }

    private static List<String> formatMods(List<Mod> mods) {
        List<String> out = new ArrayList<>();
        for (Mod mod : mods) {
            String name = pretty(mod.stat());
            int amount = mod.amount();
            String sign = amount >= 0 ? "+" : "";
            String suffix = isPercent(mod.stat()) ? "%" : (mod.stat() == Stat.RANGE_PX ? "" : "");
            if (mod.stat() == Stat.RANGE_PX) {
                out.add(sign + (amount * 6) + " " + name);
            } else {
                out.add(sign + amount + suffix + " " + name);
            }
        }
        return out;
    }

    private static String compactMods(List<Mod> mods) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mods.size(); i++) {
            Mod mod = mods.get(i);
            int amount = mod.amount();
            String sign = amount >= 0 ? "+" : "";
            if (mod.stat() == Stat.RANGE_PX) sb.append(sign).append(amount * 6).append(" Range");
            else if (isPercent(mod.stat())) sb.append(sign).append(amount).append("% ").append(shortName(mod.stat()));
            else sb.append(sign).append(amount).append(" ").append(shortName(mod.stat()));
            if (i < mods.size() - 1) sb.append("  ");
        }
        return sb.toString();
    }

    private static Color colorForLine(String line) {
        if (line.startsWith("+")) return new Color(144, 238, 144);
        if (line.startsWith("-")) return new Color(255, 160, 160);
        return Colors.TEXT_SECONDARY;
    }

    private static boolean isPercent(Stat stat) {
        return switch (stat) {
            case ARMOR_PCT, DODGE_PCT, DAMAGE_PCT, MELEE_PCT, RANGED_PCT, MAGIC_PCT,
                    CRIT_CHANCE_PCT, CRIT_DAMAGE_PCT, ATTACK_SPEED_PCT, PROJECTILE_SPEED_PCT,
                    PROJECTILE_SIZE_PCT, HOMING_CHANCE_PCT, HOMING_STRENGTH_PCT,
                    MOVE_SPEED_PCT, LIFESTEAL_PCT, BOSS_DAMAGE_PCT -> true;
            default -> false;
        };
    }

    private static String pretty(Stat stat) {
        return switch (stat) {
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

    private static String shortName(Stat stat) {
        return switch (stat) {
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
