package com.gtnhadjust.terminal;

/** Metadata carried with the original GTNR transfer action, on both sides. */
public interface TransferMetadata {

    void gtnhadjust$setTargets(long[] ids, String search);

    long[] gtnhadjust$getTargets();

    String gtnhadjust$getSearch();
}
