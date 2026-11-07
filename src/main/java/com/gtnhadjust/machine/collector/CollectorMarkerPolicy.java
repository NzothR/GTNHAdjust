package com.gtnhadjust.machine.collector;

/** Accept only dimension markers for which the collector registers recipes. */
final class CollectorMarkerPolicy {

    private CollectorMarkerPolicy() {}

    static boolean isMarker(String modId, String name) {
        return "gtnhadjust".equals(modId) && "tier_dimension".equals(name)
            || "gtneioreplugin".equals(modId) && name.startsWith("blockDimensionDisplay_");
    }
}
