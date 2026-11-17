package com.gtnhadjust.machine.proxy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;

import com.cleanroommc.modularui.widgets.layout.Grid;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;

/** Keeps slot identities stable while compacting client search results into rows. */
public final class ProxySearchGrid extends Grid {

    private final List<ItemSlot> slots = new ArrayList<>();
    private final IntPredicate matches;

    public ProxySearchGrid(IntPredicate matches) {
        this.matches = matches;
        scrollable();
    }

    public void addSlot(ItemSlot slot) {
        slots.add(slot);
        child(slot);
        if (slots.size() % 9 == 0) nextRow();
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        boolean changed = false;
        for (int i = 0; i < slots.size(); i++) {
            boolean show = matches.test(i);
            changed |= slots.get(i)
                .isEnabled() != show;
            slots.get(i)
                .setEnabled(show);
        }
        if (changed) scheduleResize();
    }

    /** Finish native slot sizing before compacting results; hidden slots retain their original sync handlers. */
    @Override
    public boolean layoutWidgets() {
        // Native Grid waits for child dimensions and prepares the scroll viewport. Reporting success before
        // that pass completes leaves dynamically installed ItemSlots with no drawable area.
        if (!super.layoutWidgets()) return false;
        int index = 0;
        for (ItemSlot slot : slots) {
            slot.getArea().rx = slot.isEnabled() ? index % 9 * 18 : 0;
            slot.getArea().ry = slot.isEnabled() ? index / 9 * 18 : 0;
            slot.resizer()
                .setPosResized(true, true);
            if (slot.isEnabled()) index++;
        }
        getScrollArea().getScrollY()
            .setScrollSize((index + 8) / 9 * 18);
        return true;
    }
}
