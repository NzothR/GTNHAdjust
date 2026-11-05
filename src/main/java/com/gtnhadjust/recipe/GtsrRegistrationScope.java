package com.gtnhadjust.recipe;

/** Scoped to the thread executing GTSR's recipe-registration callbacks. */
public final class GtsrRegistrationScope {

    private static final ThreadLocal<Integer> DEPTH = new ThreadLocal<>();

    private GtsrRegistrationScope() {}

    public static boolean active() {
        Integer depth = DEPTH.get();
        return depth != null && depth > 0;
    }

    public static void run(Runnable action) {
        Integer previous = DEPTH.get();
        DEPTH.set(previous == null ? 1 : previous + 1);
        try {
            action.run();
        } finally {
            if (previous == null) DEPTH.remove();
            else DEPTH.set(previous);
        }
    }
}
