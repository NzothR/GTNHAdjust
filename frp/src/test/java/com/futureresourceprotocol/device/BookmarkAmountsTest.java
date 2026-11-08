package com.futureresourceprotocol.device;

import static org.junit.Assert.*;

import java.math.BigInteger;
import java.util.List;

import org.junit.Test;

public class BookmarkAmountsTest {

    @Test
    public void preservesLargeAndZeroAmountsWithinNeiPersistenceLimits() {
        check(BigInteger.ZERO, 1);
        check(BigInteger.valueOf(64), 1);
        check(
            BigInteger.valueOf(Integer.MAX_VALUE)
                .add(BigInteger.valueOf(7)),
            1);
        check(
            BigInteger.valueOf(Long.MAX_VALUE)
                .multiply(BigInteger.valueOf(2)),
            1);
        check(BigInteger.valueOf(Long.MAX_VALUE), 1000);
    }

    private void check(BigInteger expected, int fluid) {
        List<long[]> chunks = BookmarkAmounts.split(expected, fluid);
        BigInteger actual = BigInteger.ZERO;
        for (long[] chunk : chunks) {
            assertTrue(chunk[0] > 0 && chunk[0] <= Integer.MAX_VALUE);
            assertTrue(chunk[1] >= 0 && chunk[1] <= Integer.MAX_VALUE);
            BigInteger amount = BigInteger.valueOf(chunk[0])
                .multiply(BigInteger.valueOf(chunk[1]));
            assertTrue(
                amount.multiply(BigInteger.valueOf(fluid))
                    .add(BigInteger.valueOf(fluid - 1L))
                    .compareTo(BigInteger.valueOf(Long.MAX_VALUE)) <= 0);
            actual = actual.add(amount);
        }
        assertEquals(expected, actual);
    }
}
