package com.gtnhadjust.structurelib;

/** Ensures a reserved replacement is either placed or refunded while the original is retained once. */
final class ReplacementTransaction {

    interface Steps {

        boolean remove();

        boolean isEmpty();

        boolean place();

        boolean isOriginal();

        boolean restore();

        void returnOriginal();

        void refundReplacement();

        void markChanged();
    }

    private ReplacementTransaction() {}

    static boolean execute(Steps steps) {
        boolean completed = false;
        try {
            if (!steps.remove() || !steps.isEmpty() || !steps.place()) return false;
            completed = true;
            steps.markChanged();
            steps.returnOriginal();
            return true;
        } finally {
            if (!completed) {
                boolean refund = true;
                if (steps.isEmpty()) {
                    if (!steps.restore()) {
                        steps.markChanged();
                        steps.returnOriginal();
                    }
                } else if (!steps.isOriginal()) {
                    // Preserve an unexpected replacement in the world and never also refund its item.
                    steps.markChanged();
                    steps.returnOriginal();
                    refund = false;
                }
                if (refund) steps.refundReplacement();
            }
        }
    }
}
