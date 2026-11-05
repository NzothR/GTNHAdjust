package com.gtnhadjust.terminal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.gtnhadjust.GTNHAdjust;

/** Read the synchronized list directly, without running the GUI's draw or search code. */
public final class ClientTargets {

    private ClientTargets() {}

    public static long[] find(Object interfaceTerminal, String search) {
        if (interfaceTerminal == null || search == null
            || search.trim()
                .isEmpty())
            return new long[0];
        String query = search.trim()
            .toLowerCase(Locale.ROOT);
        try {
            Object master = Reflection.field(interfaceTerminal, "masterList");
            Map<?, ?> entries = (Map<?, ?>) Reflection.field(master, "list");
            List<Target> targets = new ArrayList<>();
            for (Map.Entry<?, ?> entry : entries.entrySet()) {
                Object value = entry.getValue();
                String name = String.valueOf(Reflection.field(value, "dispName"))
                    .toLowerCase(Locale.ROOT);
                if (!name.contains(query)) continue;
                int rank = name.equals(query) ? 0 : name.startsWith(query) ? 1 : 2;
                targets.add(new Target(((Number) entry.getKey()).longValue(), rank, name));
            }
            targets.sort(
                Comparator.comparingInt((Target t) -> t.rank)
                    .thenComparing(t -> t.name)
                    .thenComparingLong(t -> t.id));
            long[] result = new long[Math.min(targets.size(), 1024)];
            for (int i = 0; i < result.length; i++) result[i] = targets.get(i).id;
            return result;
        } catch (ReflectiveOperationException | RuntimeException failure) {
            if (GTNHAdjust.terminalDebug)
                GTNHAdjust.LOG.info("Quick terminal: cannot inspect synchronized interfaces", failure);
            return new long[0];
        }
    }

    private static final class Target {

        final long id;
        final int rank;
        final String name;

        Target(long id, int rank, String name) {
            this.id = id;
            this.rank = rank;
            this.name = name;
        }
    }
}
