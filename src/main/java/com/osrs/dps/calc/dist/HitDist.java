package com.osrs.dps.calc.dist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** A probability distribution over single-hitsplat outcomes. */
public final class HitDist {

    private final List<WeightedHit> hits;

    public HitDist(List<WeightedHit> hits) {
        this.hits = hits;
    }

    public List<WeightedHit> hits() {
        return hits;
    }

    /** Uniform damage roll in [min, max] with the given accuracy; misses are inaccurate 0s. */
    public static HitDist linear(double accuracy, int min, int max) {
        List<WeightedHit> hits = new ArrayList<>(max - min + 2);
        double hitProb = accuracy / (max - min + 1);
        for (int dmg = min; dmg <= max; dmg++) {
            hits.add(new WeightedHit(hitProb, dmg, true));
        }
        if (accuracy < 1.0) {
            hits.add(WeightedHit.inaccurate(1 - accuracy));
        }
        return new HitDist(hits);
    }

    /** A single fixed accurate hit with the given accuracy; misses are inaccurate 0s. */
    public static HitDist single(double accuracy, int damage) {
        List<WeightedHit> hits = new ArrayList<>(2);
        hits.add(new WeightedHit(accuracy, damage, true));
        if (accuracy < 1.0) {
            hits.add(WeightedHit.inaccurate(1 - accuracy));
        }
        return new HitDist(hits);
    }

    /**
     * Applies a transformer to each hit, producing a new distribution.
     *
     * @param transformInaccurate if false, inaccurate hits pass through unchanged
     */
    public HitDist transform(Function<WeightedHit, HitDist> transformer, boolean transformInaccurate) {
        List<WeightedHit> out = new ArrayList<>();
        for (WeightedHit hit : hits) {
            if (!hit.accurate() && !transformInaccurate) {
                out.add(hit);
                continue;
            }
            for (WeightedHit t : transformer.apply(hit.withProbability(1.0)).hits) {
                out.add(t.withProbability(t.probability() * hit.probability()));
            }
        }
        return new HitDist(out).flatten();
    }

    public HitDist scaleProbability(double factor) {
        return new HitDist(hits.stream().map(h -> h.withProbability(h.probability() * factor)).toList());
    }

    /** Multiplies every damage value by factor/divisor, truncating. */
    public HitDist scaleDamage(int factor, int divisor) {
        return new HitDist(hits.stream()
                .map(h -> h.withDamage((int) ((long) h.damage() * factor / divisor)))
                .toList());
    }

    /** Merges outcomes with identical damage and accuracy. */
    public HitDist flatten() {
        Map<Long, WeightedHit> merged = new HashMap<>();
        for (WeightedHit hit : hits) {
            long key = ((long) hit.damage() << 1) | (hit.accurate() ? 1 : 0);
            merged.merge(key, hit, (a, b) -> a.withProbability(a.probability() + b.probability()));
        }
        return new HitDist(new ArrayList<>(merged.values()));
    }

    public double expectedDamage() {
        return hits.stream().mapToDouble(h -> h.probability() * h.damage()).sum();
    }

    public int maxDamage() {
        return hits.stream().mapToInt(WeightedHit::damage).max().orElse(0);
    }

    /** Probability mass over damage values (accuracy flag dropped). */
    public Map<Integer, Double> damageHistogram() {
        Map<Integer, Double> hist = new HashMap<>();
        for (WeightedHit hit : hits) {
            hist.merge(hit.damage(), hit.probability(), Double::sum);
        }
        return hist;
    }
}
