package com.gtnhadjust.machine.proxy;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ProxyThreadAccelerationTest {

    @Test
    public void accelerationStopsAtCompletionAndDoesNotOverflow() {
        assertEquals(3, ProxyThreadProgress.advance(0, 10, 3));
        assertEquals(3, ProxyThreadProgress.advance(3, 10, -1));
        assertEquals(10, ProxyThreadProgress.advance(3, 10, Integer.MAX_VALUE));
        assertEquals(10, ProxyThreadProgress.advance(10, 10, 1));
    }
}
