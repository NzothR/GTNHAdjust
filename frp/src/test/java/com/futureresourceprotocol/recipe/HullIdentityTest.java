package com.futureresourceprotocol.recipe;

import static org.junit.Assert.*;

import org.junit.Test;

public class HullIdentityTest {

    @Test
    public void matchesOnlyTheSameItemAndExactMetadata() {
        Object block = new Object();
        assertTrue(HullIdentity.matches(block, 19, block, 19));
        assertFalse(HullIdentity.matches(block, 20, block, 19));
        assertFalse(HullIdentity.matches(block, 32767, block, 19));
        assertFalse(HullIdentity.matches(new Object(), 19, block, 19));
        assertFalse(HullIdentity.matches(null, 19, null, 19));
    }
}
