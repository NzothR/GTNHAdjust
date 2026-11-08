package com.futureresourceprotocol.device;

import static org.junit.Assert.*;

import java.math.BigInteger;

import org.junit.Test;

public class DebtDisplayTest {

    @Test
    public void showsWorldDebtWithoutOverflow() {
        assertEquals("0", DebtDisplay.format(DebtDisplay.total(0, 0)));
        assertEquals("-64", DebtDisplay.format(DebtDisplay.total(32, 32)));
        assertEquals("-10K", DebtDisplay.format(DebtDisplay.total(10000, 0)));
        assertEquals("-1.2M", DebtDisplay.format(DebtDisplay.total(1200000, 0)));
        assertEquals(new BigInteger("18446744073709551614"), DebtDisplay.total(Long.MAX_VALUE, Long.MAX_VALUE));
    }
}
