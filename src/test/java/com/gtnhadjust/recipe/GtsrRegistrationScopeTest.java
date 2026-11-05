package com.gtnhadjust.recipe;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class GtsrRegistrationScopeTest {

    @Test
    public void nestedAndFailedRegistrationsRestoreScope() {
        assertFalse(GtsrRegistrationScope.active());
        GtsrRegistrationScope.run(() -> {
            assertTrue(GtsrRegistrationScope.active());
            try {
                GtsrRegistrationScope.run(() -> { throw new IllegalStateException("test"); });
                fail("expected exception");
            } catch (IllegalStateException expected) {
                assertTrue(GtsrRegistrationScope.active());
            }
        });
        assertFalse(GtsrRegistrationScope.active());
        try {
            GtsrRegistrationScope.run(() -> { throw new IllegalStateException("test"); });
            fail("expected exception");
        } catch (IllegalStateException expected) {
            assertFalse(GtsrRegistrationScope.active());
        }
    }
}
