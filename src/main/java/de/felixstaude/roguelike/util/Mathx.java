package de.felixstaude.roguelike.util;

import java.util.List;
import java.util.Random;
import java.util.function.ToDoubleFunction;

/**
 * Small math helper collection.
 */
public final class Mathx {
    private Mathx() {
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    public static <T> T weightedPick(List<T> values, ToDoubleFunction<T> weightFn, Random random) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("values must not be empty");
        }
        double total = 0.0;
        double[] weights = new double[values.size()];
        for (int i = 0; i < values.size(); i++) {
            double w = Math.max(0.0, weightFn.applyAsDouble(values.get(i)));
            weights[i] = w;
            total += w;
        }
        if (total <= 0.0) {
            return values.get(random.nextInt(values.size()));
        }
        double pick = random.nextDouble() * total;
        for (int i = 0; i < values.size(); i++) {
            pick -= weights[i];
            if (pick <= 0.0) {
                return values.get(i);
            }
        }
        return values.get(values.size() - 1);
    }
}
