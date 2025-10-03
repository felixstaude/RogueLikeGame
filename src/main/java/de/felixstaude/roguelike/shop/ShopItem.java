package de.felixstaude.roguelike.shop;

import de.felixstaude.roguelike.entity.Player;

import java.util.function.Consumer;

public class ShopItem {
    public final String title;
    public final String desc;
    public int cost;
    public int bought = 0;
    public final Rarity rarity;
    public final Consumer<Player> apply;

    public ShopItem(String title, String desc, int cost, Rarity rarity, Consumer<Player> apply) {
        this.title = title;
        this.desc = desc;
        this.cost = cost;
        this.rarity = rarity;
        this.apply = apply;
    }

    public boolean purchase(Player p) {
        if (!p.spendGold(cost)) return false;
        apply.accept(p);
        bought++;
        // Kosten skalieren mit Seltenheit leicht stärker
        double mul = switch (rarity){
            case COMMON -> 1.45;
            case UNCOMMON -> 1.55;
            case RARE -> 1.70;
            case EPIC -> 1.90;
        };
        cost = Math.max(cost + 1, (int)Math.round(cost * mul));
        return true;
    }
}
