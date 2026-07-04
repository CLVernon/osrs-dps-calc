package com.osrs.dps.calc.dist;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Standard hitsplat transformers, ported from the wiki DPS tool. */
public final class Transforms {

    private Transforms() {
    }

    /** Adds a flat amount to damage, flooring at the given minimum. */
    public static Function<WeightedHit, HitDist> add(int addend, int minimum) {
        return h -> new HitDist(List.of(
                new WeightedHit(1.0, Math.max(minimum, h.damage() + addend), h.accurate())));
    }

    /** Clamps damage into [minimum, maximum]. */
    public static Function<WeightedHit, HitDist> flatLimit(int maximum, int minimum) {
        return h -> new HitDist(List.of(
                new WeightedHit(1.0, Math.max(minimum, Math.min(h.damage(), maximum)), h.accurate())));
    }

    /** Rerolls damage to min(damage, uniform[offset, maximum+offset]). */
    public static Function<WeightedHit, HitDist> linearMin(int maximum, int offset) {
        return h -> {
            List<WeightedHit> hits = new ArrayList<>(maximum + 1);
            double prob = 1.0 / (maximum + 1);
            for (int i = 0; i <= maximum; i++) {
                hits.add(new WeightedHit(prob, Math.min(h.damage(), i + offset), h.accurate()));
            }
            return new HitDist(hits).flatten();
        };
    }

    /** If damage exceeds limit, reroll uniformly in [offset, rollMax+offset]. */
    public static Function<WeightedHit, HitDist> cappedReroll(int limit, int rollMax, int offset) {
        return h -> {
            if (h.damage() <= limit) {
                return new HitDist(List.of(h.withProbability(1.0)));
            }
            List<WeightedHit> hits = new ArrayList<>(rollMax + 1);
            double prob = 1.0 / (rollMax + 1);
            for (int i = 0; i <= rollMax; i++) {
                hits.add(new WeightedHit(prob, i + offset, h.accurate()));
            }
            return new HitDist(hits).flatten();
        };
    }

    /**
     * Multiplies damage by numerator/divisor with the wiki tool's minimum semantics:
     * values that started at or above the minimum can't drop below it; values that
     * started below aren't reduced further.
     */
    public static Function<WeightedHit, HitDist> multiply(int numerator, int divisor, int minimum) {
        return h -> {
            int dmg = (int) ((long) numerator * h.damage() / divisor);
            if (minimum != 0) {
                dmg = h.damage() >= minimum ? Math.max(minimum, dmg) : Math.max(h.damage(), dmg);
            }
            return new HitDist(List.of(new WeightedHit(1.0, dmg, h.accurate())));
        };
    }

    public static Function<WeightedHit, HitDist> division(int divisor, int minimum) {
        return multiply(1, divisor, minimum);
    }
}
