package com.gtnhadjust.terminal;

import java.util.Locale;
import java.util.regex.Pattern;

/** Shared plain-text names for client target selection and server upload matching. */
public final class InterfaceNames {

    private static final Pattern FORMATTING_CODES = Pattern.compile("(?i)\u00a7[0-9A-FK-OR]");

    private InterfaceNames() {}

    /** Strip vanilla formatting without calling EnumChatFormatting's client-only method on a dedicated server. */
    public static String normalize(String name) {
        return FORMATTING_CODES.matcher(name)
            .replaceAll("")
            .trim()
            .toLowerCase(Locale.ROOT);
    }
}
