package com.gtnhadjust.terminal;

/** AE2 creates a new stack object on successful encoding, even for an identical recipe. */
public final class PatternEncodingResult {

    private PatternEncodingResult() {}

    public static boolean isNew(Object previous, Object current) {
        return current != null && current != previous;
    }
}
