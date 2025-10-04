// src/main/java/de/felixstaude/roguelike/stats/Stats.java
package de.felixstaude.roguelike.stats;

import java.util.Arrays;

/**
 * Einfacher, schneller Container für alle additive Spieler-Stats.
 * - Hält pro {@link Stat} einen Integer-Wert (darf negativ sein, außer wo Regeln später greifen).
 * - Keine Caps/Mapping hier! -> Das macht {@link StatRules} / EffectiveStats.
 */
public final class Stats {

    private final int[] values;

    /** Erzeugt leere Stats (alle 0). */
    public Stats() {
        this.values = new int[Stat.values().length];
    }

    /** Kopier-Konstruktor (tief). */
    public Stats(Stats other) {
        this.values = Arrays.copyOf(other.values, other.values.length);
    }

    /** Fabrik: neue Instanz mit einer einzigen Initial-Änderung. */
    public static Stats of(Stat stat, int amount) {
        Stats s = new Stats();
        s.values[stat.ordinal()] = amount;
        return s;
    }

    /** Liefert den rohen (nicht gemappten) Wert eines Stats. */
    public int get(Stat stat) {
        return values[stat.ordinal()];
    }

    /** Setzt den rohen Wert. (Achtung: keine Caps hier!) */
    public void set(Stat stat, int value) {
        values[stat.ordinal()] = value;
    }

    /** Addiert delta auf einen Stat (negativ erlaubt). */
    public void add(Stat stat, int delta) {
        values[stat.ordinal()] += delta;
    }

    /** Addiert alle Werte aus other auf diese Instanz. */
    public void addAll(Stats other) {
        int len = values.length;
        for (int i = 0; i < len; i++) values[i] += other.values[i];
    }

    /** Subtrahiert alle Werte aus other von dieser Instanz. */
    public void subAll(Stats other) {
        int len = values.length;
        for (int i = 0; i < len; i++) values[i] -= other.values[i];
    }

    /** Multipliziert ALLE Werte (inkl. negative) und rundet. Nützlich für globale Skalierung. */
    public void scaleAll(double factor) {
        int len = values.length;
        for (int i = 0; i < len; i++) values[i] = (int) Math.round(values[i] * factor);
    }

    /** Setzt alle Werte auf 0. */
    public void clear() {
        Arrays.fill(values, 0);
    }

    /** Tiefenkopie. */
    public Stats copy() {
        return new Stats(this);
    }

    /** Summiert beliebig viele Stats in eine neue Instanz. */
    public static Stats sum(Stats... many) {
        Stats out = new Stats();
        for (Stats s : many) out.addAll(s);
        return out;
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder("Stats{");
        Stat[] all = Stat.values();
        boolean first = true;
        for (int i = 0; i < all.length; i++) {
            int v = values[i];
            if (v == 0) continue;
            if (!first) sb.append(", ");
            sb.append(all[i].name()).append("=").append(v);
            first = false;
        }
        if (first) sb.append("empty");
        sb.append("}");
        return sb.toString();
    }
}
