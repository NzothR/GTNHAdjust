package com.futureresourceprotocol.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Keeps original indices so temporary removals can be reversed in order. */
public final class RecipeListSnapshot<T> {

    private final List<Entry<T>> removed = new ArrayList<>();

    public int remove(List<T> recipes, Predicate<T> shouldRemove) {
        if (!removed.isEmpty()) throw new IllegalStateException("Snapshot already in use");
        for (int i = 0; i < recipes.size(); i++) {
            T value = recipes.get(i);
            if (shouldRemove.test(value)) removed.add(new Entry<>(i, value));
        }
        for (int i = removed.size() - 1; i >= 0; i--) recipes.remove(removed.get(i).index);
        return removed.size();
    }

    public void restore(List<T> recipes) {
        for (Entry<T> entry : removed) recipes.add(Math.min(entry.index, recipes.size()), entry.value);
        removed.clear();
    }

    private static final class Entry<T> {

        final int index;
        final T value;

        Entry(int index, T value) {
            this.index = index;
            this.value = value;
        }
    }
}
