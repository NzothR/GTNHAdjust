package com.gtnhadjust.machine.threadedproxy;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.widget.Interactable.Result;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.gtnhadjust.machine.proxy.ProxyLocatorSearch;
import com.xyp.gtneverreset.common.me.CircuitPatternCodec;
import com.xyp.gtneverreset.common.me.PatternRecipeMap;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;

/** Encoded-pattern slot with cached ingredient names and an independent middle-click editor action. */
final class ThreadPatternSlot extends ItemSlot {

    private final ThreadedJointProxy owner;
    private final int index;
    private final StringValue[] searches;
    private final InteractionSyncHandler edit;
    private final Runnable beforeEdit;
    private ItemStack cached;
    private final List<List<String>> names = new ArrayList<>();
    private String previousQuery;
    private boolean matches;

    ThreadPatternSlot(ThreadedJointProxy owner, int index, StringValue[] searches, InteractionSyncHandler edit,
        Runnable beforeEdit) {
        this.owner = owner;
        this.index = index;
        this.searches = searches;
        this.edit = edit;
        this.beforeEdit = beforeEdit;
        for (int i = 0; i < 3; i++) names.add(new ArrayList<>());
    }

    @Override
    public Result onMousePressed(int button) {
        if (button == 2) {
            // Commit the old field before the selection action is sent to the server.
            beforeEdit.run();
            edit.onMousePressed(button);
            return Result.SUCCESS;
        }
        return super.onMousePressed(button);
    }

    @Override
    public void buildTooltip(ItemStack stack, com.cleanroommc.modularui.screen.RichTooltip tooltip) {
        super.buildTooltip(stack, tooltip);
        tooltip.addLine(StatCollector.translateToLocalFormatted("gtnhadjust.threaded_proxy.pattern_slot", index + 1));
    }

    boolean matchesSearch() {
        ItemStack stack = owner.patterns()
            .getStackInSlot(index);
        if (!ItemStack.areItemStacksEqual(cached, stack)) {
            cached = stack == null ? null : stack.copy();
            for (List<String> group : names) group.clear();
            if (stack != null) {
                ICraftingPatternDetails details = CircuitPatternCodec.decode(
                    stack.copy(),
                    owner.getBaseMetaTileEntity()
                        .getWorld());
                if (details != null) {
                    append(names.get(0), details.getAEInputs());
                    append(names.get(1), details.getAEOutputs());
                }
                String map = PatternRecipeMap.name(stack);
                if (map != null) {
                    names.get(2)
                        .add(map);
                    names.get(2)
                        .add(StatCollector.translateToLocal(map));
                }
            }
            previousQuery = null;
        }
        String query = searches[0].getStringValue() + '\n'
            + searches[1].getStringValue()
            + '\n'
            + searches[2].getStringValue();
        if (!query.equals(previousQuery)) {
            previousQuery = query;
            matches = true;
            for (int i = 0; i < 3; i++) if (!searches[i].getStringValue()
                .trim()
                .isEmpty() && !ProxyLocatorSearch.matches(names.get(i), searches[i].getStringValue())) matches = false;
        }
        return matches;
    }

    private static void append(List<String> names, IAEStack<?>[] stacks) {
        for (IAEStack<?> stack : stacks) {
            if (stack instanceof IAEItemStack item) names.add(
                item.getItemStack()
                    .getDisplayName());
            if (stack instanceof IAEFluidStack fluid) names.add(
                fluid.getFluidStack()
                    .getLocalizedName());
        }
    }
}
