package com.gtnhadjust.machine.collector;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/** Selects each key once across tiers, in ascending tier order. */
final class TierResourceDeduplication {

    private TierResourceDeduplication() {}

    static <T> List<T> takeNew(Iterable<T> values, Set<String> seen, Function<T, String> key) {
        List<T> result = new ArrayList<>();
        for (T value : values) if (seen.add(key.apply(value))) result.add(value);
        return result;
    }
}
