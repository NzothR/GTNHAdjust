package com.gtnhadjust.machine.collector;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Test;

public class TierResourceDeduplicationTest {

    @Test
    public void removesDuplicatesWithinAndAcrossAllEarlierTiers() {
        Set<String> seen = new LinkedHashSet<>();
        assertEquals(
            Arrays.asList("gold", "iron"),
            TierResourceDeduplication.takeNew(Arrays.asList("gold", "gold", "iron"), seen, value -> value));
        assertEquals(
            Arrays.asList("copper"),
            TierResourceDeduplication.takeNew(Arrays.asList("gold", "copper", "copper"), seen, value -> value));
        assertEquals(
            Arrays.asList("tin"),
            TierResourceDeduplication.takeNew(Arrays.asList("iron", "copper", "tin"), seen, value -> value));
    }
}
