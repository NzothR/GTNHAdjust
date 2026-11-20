package com.gtnhadjust.machine.threadedproxy;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.gtnhadjust.machine.proxy.ProxyLocatorSearch;
import com.xyp.gtneverreset.common.me.CircuitPatternCodec;
import com.xyp.gtneverreset.common.me.PatternRecipeMap;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;

/** Encoded-pattern slot with cached ingredient names. */
final class ThreadPatternSlot extends ItemSlot {

    private final ThreadedJointProxy owner;
    private final int index;
    private final StringValue[] searches;
    private ItemStack cached;
    private final List<List<String>> names = new ArrayList<>();
    private String previousQuery;
    private boolean matches;

    ThreadPatternSlot(ThreadedJointProxy owner, int index, StringValue[] searches) {
        this.owner = owner;
        this.index = index;
        this.searches = searches;
        for (int i = 0; i < 3; i++) names.add(new ArrayList<>());
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

    /** Highlight matching physical patterns while keeping the scrolling slot grid stable. */
    @Override
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public com.cleanroommc.modularui.api.drawable.IDrawable getCurrentBackground(
        com.cleanroommc.modularui.theme.WidgetThemeEntry<?> theme) {
        boolean searching = false;
        for (StringValue query : searches) searching |= !query.getStringValue()
            .trim()
            .isEmpty();
        if (searching && owner.patterns()
            .getStackInSlot(index) != null && matchesSearch()) return SEARCH_MATCH;
        return super.getCurrentBackground(theme);
    }

    private static final com.cleanroommc.modularui.api.drawable.IDrawable SEARCH_MATCH = new com.cleanroommc.modularui.drawable.Rectangle()
        .color(0xFFFFFF60);

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
