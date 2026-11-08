package com.futureresourceprotocol.device;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/** NEI 2.8.171 reloads factor and multiplier as signed ints. */
public final class BookmarkAmounts {

    public static final int MAX_ENTRIES = 10000;
    private static final long MAX = Integer.MAX_VALUE;

    private BookmarkAmounts() {}

    public static List<long[]> split(BigInteger count, int fluidCellAmount) {
        if (count.signum() < 0 || fluidCellAmount <= 0) throw new IllegalArgumentException("Invalid bookmark amount");
        List<long[]> chunks = new ArrayList<>();
        if (count.signum() == 0) {
            chunks.add(new long[] { 1, 0 });
            return chunks;
        }
        // NEI rounds up with amount + fluidCellAmount - 1 before division.
        long maxNumerator = Long.MAX_VALUE - (fluidCellAmount - 1L);
        long maxFactor = Math.min(MAX, (maxNumerator / fluidCellAmount) / MAX);
        if (maxFactor < 1) maxFactor = 1;
        BigInteger factor = BigInteger.valueOf(maxFactor);
        BigInteger full = factor.multiply(BigInteger.valueOf(MAX));
        BigInteger[] parts = count.divideAndRemainder(full);
        if (parts[0].compareTo(BigInteger.valueOf(MAX_ENTRIES - 2L)) > 0)
            throw new IllegalArgumentException("Bookmark count too large for NEI");
        for (int i = 0; i < parts[0].intValue(); i++) chunks.add(new long[] { maxFactor, MAX });
        BigInteger[] remainder = parts[1].divideAndRemainder(factor);
        if (remainder[0].signum() > 0) chunks.add(new long[] { maxFactor, remainder[0].longValueExact() });
        if (remainder[1].signum() > 0) chunks.add(new long[] { 1, remainder[1].longValueExact() });
        return chunks;
    }
}
