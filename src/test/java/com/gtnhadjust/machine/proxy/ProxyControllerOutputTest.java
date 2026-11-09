package com.gtnhadjust.machine.proxy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ProxyControllerOutputTest {

    @Test
    public void controllerReturnExcludesNetworkAndVoidBuses() {
        assertTrue(ProxyControllerOutputPolicy.accepts("StandardFiltered"));
        assertTrue(ProxyControllerOutputPolicy.accepts("StandardUnfiltered"));
        assertTrue(ProxyControllerOutputPolicy.accepts("CompressedFiltered"));
        assertTrue(ProxyControllerOutputPolicy.accepts("CompressedUnfiltered"));
        assertFalse(ProxyControllerOutputPolicy.accepts("MEFiltered"));
        assertFalse(ProxyControllerOutputPolicy.accepts("MEUnfiltered"));
        assertFalse(ProxyControllerOutputPolicy.accepts("MECacheFiltered"));
        assertFalse(ProxyControllerOutputPolicy.accepts("MECacheUnfiltered"));
        assertFalse(ProxyControllerOutputPolicy.accepts("Void"));
    }
}
