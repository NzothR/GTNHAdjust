package com.gtnhadjust.machine.proxy;

final class ProxyThreadProgress {

    private ProxyThreadProgress() {}

    static int advance(int progress, int duration, int ticks) {
        return ticks > 0 ? progress + Math.min(ticks, duration - progress) : progress;
    }
}
