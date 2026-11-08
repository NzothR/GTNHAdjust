package com.gtnhadjust.machine.proxy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Pure unlock rules, shared by controller intake and reconstruction after loading or output. */
final class ProxyFactoryUnlockRules {

    private ProxyFactoryUnlockRules() {}

    static boolean addsNew(Collection<String> unlocked, Collection<String> offered) {
        for (String name : offered) if (!unlocked.contains(name)) return true;
        return false;
    }

    static List<String> orderedAdditional(Collection<String> original, Collection<String> allowedOrder,
        Collection<? extends Collection<String>> controllers) {
        Set<String> offered = new HashSet<>();
        for (Collection<String> maps : controllers) offered.addAll(maps);
        offered.removeAll(original);
        List<String> result = new ArrayList<>();
        for (String name : allowedOrder) if (offered.contains(name) && !result.contains(name)) result.add(name);
        return result;
    }
}
