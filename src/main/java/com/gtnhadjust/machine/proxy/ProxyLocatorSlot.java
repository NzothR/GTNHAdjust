package com.gtnhadjust.machine.proxy;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.DrawableStack;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.gtnhadjust.GTNHAdjust;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.recipe.RecipeMap;

/** The actual stack remains a locator; only the slot's presentation uses its recorded controller. */
final class ProxyLocatorSlot extends ItemSlot {

    private final ProxyLocatorHost owner;
    private final int index;
    private final StringValue search;
    private ItemStack cachedLocator;
    private MachineLocatorRecord record;
    private final List<String> names = new ArrayList<>();
    private String previousQuery;
    private boolean matches;

    ProxyLocatorSlot(ProxyLocatorHost owner, int index, StringValue search) {
        this.owner = owner;
        this.index = index;
        this.search = search;
    }

    /** Recompute searchable names only when the recorded item changes, never on every rendered frame. */
    @SideOnly(Side.CLIENT)
    private void updateRecord(ItemStack stack) {
        if (ItemStack.areItemStacksEqual(cachedLocator, stack)) return;
        cachedLocator = stack == null ? null : stack.copy();
        record = MachineLocatorItem.read(stack);
        names.clear();
        if (record != null) {
            names.add(
                record.controller()
                    .getDisplayName());
            for (RecipeMap<?> map : ProxyFactoryControllerRegistry.unrestrictedMaps(record.controller()))
                names.add(StatCollector.translateToLocal(map.unlocalizedName));
        }
        previousQuery = null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected ItemStack getItemStackForRendering(ItemStack stack, boolean phantom) {
        updateRecord(stack);
        return record == null ? stack : record.controller();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void buildTooltip(ItemStack stack, RichTooltip tooltip) {
        updateRecord(stack);
        if (!names.isEmpty()) tooltip.addLine(names.get(0));
        for (int i = 1; i < names.size(); i++)
            tooltip.addLine(StatCollector.translateToLocalFormatted("gtnhadjust.proxy.locator_pool", names.get(i)));
        if (record != null) tooltip.addLine(
            StatCollector.translateToLocalFormatted(
                "gtnhadjust.locator.position",
                record.dimension,
                record.x,
                record.y,
                record.z));
        if (!owner.canEditLocators())
            tooltip.addLine(StatCollector.translateToLocal("gtnhadjust.proxy.locator_locked"));
        if (index >= GTNHAdjust.proxyFactoryLocatorSlots)
            tooltip.addLine(StatCollector.translateToLocal("gtnhadjust.proxy.locator_overflow"));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IDrawable getCurrentBackground(WidgetThemeEntry<?> theme) {
        updateRecord(
            owner.getLocatorInventory()
                .getStackInSlot(index));
        String query = search.getStringValue();
        if (!query.equals(previousQuery)) {
            previousQuery = query;
            matches = !query.trim()
                .isEmpty() && ProxyLocatorSearch.matches(names, query);
        }
        IDrawable background = super.getCurrentBackground(theme);
        return matches ? new DrawableStack(
            background,
            new Rectangle().color(0xffe6c849)
                .hollow(1))
            : background;
    }
}
