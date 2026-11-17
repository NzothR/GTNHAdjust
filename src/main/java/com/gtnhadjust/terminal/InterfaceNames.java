package com.gtnhadjust.terminal;

import java.util.Locale;

import net.minecraft.util.EnumChatFormatting;

/** Shared plain-text names for client target selection and server upload matching. */
public final class InterfaceNames {

    private InterfaceNames() {}

    /** Formatting describes presentation, not interface identity. */
    public static String normalize(String name) {
        return EnumChatFormatting.getTextWithoutFormattingCodes(name)
            .trim()
            .toLowerCase(Locale.ROOT);
    }
}
