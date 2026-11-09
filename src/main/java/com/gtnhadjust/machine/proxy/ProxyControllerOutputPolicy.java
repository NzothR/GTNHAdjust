package com.gtnhadjust.machine.proxy;

final class ProxyControllerOutputPolicy {

    private ProxyControllerOutputPolicy() {}

    static boolean accepts(String type) {
        return "StandardFiltered".equals(type) || "StandardUnfiltered".equals(type)
            || "CompressedFiltered".equals(type)
            || "CompressedUnfiltered".equals(type);
    }
}
