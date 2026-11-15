package com.futureresourceprotocol.device;

import java.math.BigInteger;

public final class DebtDisplay {

    private static final BigInteger THOUSAND = BigInteger.valueOf(1000);
    private static final String[] UNITS = { "", "K", "M", "G", "T", "P", "E" };

    private DebtDisplay() {}

    public static BigInteger total(long uhv, long uxv) {
        if (uhv < 0 || uxv < 0) throw new IllegalArgumentException("Negative debt");
        return BigInteger.valueOf(uhv)
            .add(BigInteger.valueOf(uxv));
    }

    public static String format(BigInteger amount) {
        if (amount.signum() == 0) return "0";
        if (amount.compareTo(BigInteger.valueOf(10000)) < 0) return "-" + amount;
        BigInteger scale = BigInteger.ONE;
        int unit = 0;
        while (unit < UNITS.length - 1 && amount.compareTo(scale.multiply(THOUSAND)) >= 0) {
            scale = scale.multiply(THOUSAND);
            unit++;
        }
        BigInteger tenths = amount.multiply(BigInteger.TEN)
            .divide(scale);
        BigInteger[] parts = tenths.divideAndRemainder(BigInteger.TEN);
        return "-" + parts[0] + (parts[1].signum() == 0 ? "" : "." + parts[1]) + UNITS[unit];
    }
}
