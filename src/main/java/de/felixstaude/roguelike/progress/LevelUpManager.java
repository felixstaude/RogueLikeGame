// src/main/java/de/felixstaude/roguelike/progress/LevelUpManager.java
package de.felixstaude.roguelike.progress;

import de.felixstaude.roguelike.stats.Stat;
import de.felixstaude.roguelike.stats.Stats;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Verwaltet XP -> Level und das Rollen eines zufälligen positiven Stat-Uplifts.
 * - Luck beeinflusst die Gewichte (Engine gibt Luck rein, wenn gerollt wird).
 * - Rerolls für das aktuelle Level kosten Gold (Kosten-Logik liefert diese Klasse; Gold-Abzug macht die Engine).
 * - Keine negativen Stats hier.
 */
public class LevelUpManager {

    private int level = 1;
    private int xp = 0;
    private int xpNeeded = xpForLevel(1);

    // Level-Reroll-Kosten (pro Level separat aufaddiert)
    private int rerollCost = 4;       // Startkosten für den ersten Reroll
    private final int rerollStep = 2; // +2 je weiterem Reroll in DIESEM Level
    private int rerollsThisLevel = 0;

    // zuletzt gerollter Vorschlag (noch nicht angewendet)
    private StatRoll pending;

    /** Ergebnis eines Stat-Rolls. */
    public static final class StatRoll {
        public final Stat stat;
        public final int amount; // immer positiv
        public final String label;

        public StatRoll(Stat stat, int amount, String label) {
            this.stat = stat;
            this.amount = amount;
            this.label = label;
        }
    }

    /** fügt XP hinzu; gibt true zurück, wenn mindestens ein Level-Up stattgefunden hat. */
    public boolean addXp(int amount, int luckForWeights) {
        xp += Math.max(0, amount);
        boolean leveled = false;
        while (xp >= xpNeeded) {
            xp -= xpNeeded;
            level++;
            xpNeeded = xpForLevel(level);
            rerollCost = 4; rerollsThisLevel = 0;
            pending = rollStat(luckForWeights);
            leveled = true;
        }
        return leveled;
    }

    /** Aktuelles Level (mindestens 1). */
    public int getLevel() { return level; }

    /** Aktuelle XP (für UI). */
    public int getXp() { return xp; }

    /** XP bis zum nächsten Level. */
    public int getXpNeeded() { return xpNeeded; }

    /** Letzter gerollter Stat (noch nicht angewendet). */
    public StatRoll getPending() { return pending; }

    /** Startkosten für einen Reroll dieses Levels. */
    public int getRerollCost() { return rerollCost; }

    /** Führe einen Reroll aus (Engine prüft vorher, ob genug Gold vorhanden ist). */
    public StatRoll reroll(int luckForWeights) {
        pending = rollStat(luckForWeights);
        rerollsThisLevel++;
        rerollCost += rerollStep;
        return pending;
    }

    /** Wendet den pending Roll auf die Stats an und leert pending. */
    public void acceptPending(Stats stats) {
        if (pending == null) return;
        stats.add(pending.stat, pending.amount);
        pending = null;
    }

    /** XP-Formel: 20 * 1.22^(level-1) (aufgerundet). */
    public static int xpForLevel(int lvl) {
        double v = 20.0 * Math.pow(1.22, Math.max(0, lvl - 1));
        return (int)Math.ceil(v);
    }

    // ------------------------------------------------------------------------

    private StatRoll rollStat(int luck) {
        // Gewichte (können wir später tunen). Luck verschiebt gute Stats nach oben, „dead“ Stats niedriger.
        List<Entry> bag = new ArrayList<>();
        add(bag, Stat.MAX_HP,           14, () -> rollInt(8, 14),    a -> "+"+a+" Max HP");
        add(bag, Stat.ARMOR_PCT,        10, () -> rollInt(2, 4),     a -> "+"+a+"% Armor");
        add(bag, Stat.DODGE_PCT,        8,  () -> rollInt(1, 3),     a -> "+"+a+"% Dodge");
        add(bag, Stat.HP_REGEN_PS,      6,  () -> rollInt(1, 2),     a -> "+"+a+" HP/s");

        add(bag, Stat.DAMAGE_PCT,       12, () -> rollInt(3, 6),     a -> "+"+a+"% Damage");
        add(bag, Stat.RANGED_PCT,       10, () -> rollInt(3, 6),     a -> "+"+a+"% Ranged Damage");
        add(bag, Stat.MELEE_PCT,        4,  () -> rollInt(3, 5),     a -> "+"+a+"% Melee Damage");
        add(bag, Stat.MAGIC_PCT,        4,  () -> rollInt(3, 5),     a -> "+"+a+"% Magic Damage");

        add(bag, Stat.CRIT_CHANCE_PCT,  7,  () -> rollInt(3, 6),     a -> "+"+a+"% Crit Chance");
        add(bag, Stat.CRIT_DAMAGE_PCT,  7,  () -> rollInt(10, 20),   a -> "+"+a+"% Crit Damage");

        add(bag, Stat.ATTACK_SPEED_PCT, 10, () -> rollInt(4, 7),     a -> "+"+a+"% Attack Speed");
        add(bag, Stat.RANGE_PX,         8,  () -> rollInt(8, 14),    a -> "+"+(a*6)+" Range"); // 1P ~ 6px
        add(bag, Stat.MOVE_SPEED_PCT,   7,  () -> rollInt(3, 5),     a -> "+"+a+"% Move Speed");
        add(bag, Stat.LIFESTEAL_PCT,    5,  () -> rollInt(1, 2),     a -> "+"+a+"% Lifesteal");

        // Luck-Einfluss
        double bias = clamp(1.0 + (luck * 0.005), 0.2, 3.0); // +0.5% Gewicht pro Luck
        for (Entry e : bag) {
            if (isGoodForRanged(e.stat)) e.weight = (int)Math.max(1, Math.round(e.weight * bias));
            if (isLessRelevantEarly(e.stat)) e.weight = (int)Math.max(1, Math.round(e.weight * clamp(1.0 + (luck * 0.0025), 0.33, 1.5)));
        }

        int total = bag.stream().mapToInt(en -> en.weight).sum();
        int pick = ThreadLocalRandom.current().nextInt(total);
        for (Entry e : bag) {
            if ((pick -= e.weight) < 0) {
                int amount = e.amountSupplier.get();
                return new StatRoll(e.stat, amount, e.labelMaker.make(amount));
            }
        }
        // Fallback
        Entry e = bag.get(0);
        int amount = e.amountSupplier.get();
        return new StatRoll(e.stat, amount, e.labelMaker.make(amount));
    }

    // Helpers
    private static int rollInt(int lo, int hi) { // inklusiv
        return ThreadLocalRandom.current().nextInt(lo, hi + 1);
    }
    private static double clamp(double v, double lo, double hi){ return Math.max(lo, Math.min(hi, v)); }

    private static boolean isGoodForRanged(Stat s){
        return switch (s) {
            case DAMAGE_PCT, RANGED_PCT, ATTACK_SPEED_PCT, CRIT_CHANCE_PCT, CRIT_DAMAGE_PCT, RANGE_PX, MOVE_SPEED_PCT, MAX_HP, ARMOR_PCT, DODGE_PCT -> true;
            default -> false;
        };
    }
    private static boolean isLessRelevantEarly(Stat s){
        return switch (s) {
            case MELEE_PCT, MAGIC_PCT -> true;
            default -> false;
        };
    }

    // Small internal entry
    private static final class Entry {
        final Stat stat;
        int weight;
        final IntSupplier amountSupplier;
        final LabelMaker labelMaker;
        Entry(Stat stat, int weight, IntSupplier sup, LabelMaker lab){ this.stat=stat; this.weight=weight; this.amountSupplier=sup; this.labelMaker=lab; }
    }
    @FunctionalInterface private interface IntSupplier { int get(); }
    @FunctionalInterface private interface LabelMaker { String make(int amount); }

    // <<< FEHLENDER HELFER (jetzt vorhanden)
    private static void add(List<Entry> bag, Stat stat, int weight, IntSupplier amountSupplier, LabelMaker labelMaker) {
        bag.add(new Entry(stat, Math.max(1, weight), amountSupplier, labelMaker));
    }
}
