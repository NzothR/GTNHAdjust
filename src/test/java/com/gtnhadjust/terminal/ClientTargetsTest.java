package com.gtnhadjust.terminal;

import static org.junit.Assert.assertArrayEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class ClientTargetsTest {

    private static final class Entry {

        @SuppressWarnings("unused")
        private final String dispName;

        Entry(String name) {
            dispName = name;
        }
    }

    private static final class Master {

        @SuppressWarnings("unused")
        private final Map<Long, Entry> list = new HashMap<>();
    }

    private static final class Terminal {

        @SuppressWarnings("unused")
        private final Master masterList = new Master();
    }

    @Test
    public void usesSynchronizedNamesWithoutDependingOnVisibleRows() {
        Terminal terminal = new Terminal();
        terminal.masterList.list.put(9L, new Entry("Advanced Assembler"));
        terminal.masterList.list.put(2L, new Entry("Assembler 2"));
        terminal.masterList.list.put(3L, new Entry("Assembler"));
        terminal.masterList.list.put(4L, new Entry("Other"));
        assertArrayEquals(new long[] { 3, 2, 9 }, ClientTargets.find(terminal, "assembler"));
        assertArrayEquals(new long[0], ClientTargets.find(terminal, ""));
    }
}
