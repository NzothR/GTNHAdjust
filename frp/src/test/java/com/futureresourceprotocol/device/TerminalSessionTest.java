package com.futureresourceprotocol.device;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TerminalSessionTest {

    @Test
    public void sessionSurvivesStackCopiesButRejectsReplacements() {
        assertTrue(TerminalContainer.sameSession("session-1", new String("session-1")));
        assertFalse(TerminalContainer.sameSession("session-1", "session-2"));
        assertFalse(TerminalContainer.sameSession("", ""));
        assertFalse(TerminalContainer.sameSession(null, "session-1"));
    }
}
