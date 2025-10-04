package de.felixstaude.roguelike.util;

/**
 * Small time utilities.
 */
public final class Time {
    private Time() {
    }

    public static long nowMillis() {
        return System.currentTimeMillis();
    }

    public static long nowNanos() {
        return System.nanoTime();
    }

    public static double deltaSeconds(long previousNanos, long currentNanos) {
        return (currentNanos - previousNanos) / 1_000_000_000.0;
    }
}
