package com.gtnhadjust.machine.proxy;

import java.util.Locale;

final class ProxyRecipeSearch {

    private ProxyRecipeSearch() {}

    static boolean matches(String id, String localized, String search) {
        String query = normalize(search).trim();
        return query.isEmpty() || normalize(localized).contains(query) || normalize(id).contains(query);
    }

    private static String normalize(String value) {
        return value == null ? ""
            : value.replaceAll("(?i)§[0-9a-fk-or]", "")
                .toLowerCase(Locale.ROOT);
    }
}
