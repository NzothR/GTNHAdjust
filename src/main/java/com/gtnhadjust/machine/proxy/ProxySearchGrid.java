package com.gtnhadjust.machine.proxy;

import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widgets.layout.Grid;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;

/** Native fixed nine-column inventory; searching never disables or relocates physical slots. */
public final class ProxySearchGrid extends Grid {

    private int count;

    public ProxySearchGrid() {
        scrollable();
        alignment(Alignment.TopLeft);
    }

    public void addSlot(ItemSlot slot) {
        slot.size(18, 18);
        child(slot);
        if (++count % 9 == 0) nextRow();
    }
}
