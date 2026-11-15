package com.gtnhadjust.terminal;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.github.vfyjxf.nee.utils.GuiUtils;
import com.github.vfyjxf.nee.utils.Ingredient;
import com.github.vfyjxf.nee.utils.IngredientTracker;
import com.xyp.gtneverreset.client.gui.QuickEncodingTerminalGui;

import appeng.api.AEApi;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import codechicken.lib.gui.GuiDraw;
import codechicken.nei.ItemStackAmount;
import codechicken.nei.ItemsTooltipLineHandler;
import codechicken.nei.guihook.GuiContainerManager;
import codechicken.nei.recipe.GuiOverlayButton;
import codechicken.nei.recipe.StackInfo;

/** Builds a fresh snapshot for each tooltip from the terminal's synchronized network inventory. */
public final class OverlayIngredientTooltip {

    private OverlayIngredientTooltip() {}

    public static void append(GuiOverlayButton button, QuickEncodingTerminalGui terminal, List<String> tooltip) {
        IngredientTracker tracker = new IngredientTracker(
            terminal,
            button.handlerRef.handler,
            button.handlerRef.recipeIndex);
        List<IAEStack<?>> network = GuiUtils.getStorageStacks(terminal, stack -> true);
        ItemStackAmount present = new ItemStackAmount();
        ItemStackAmount missing = new ItemStackAmount();
        List<ItemStack> craftable = new ArrayList<>();
        ItemStackAmount allCraftable = new ItemStackAmount();
        long currentTime = Minecraft.getSystemTime();
        for (Ingredient ingredient : tracker.getIngredients()) {
            if (ingredient.getIngredient() == null || ingredient.getIngredient().item == null) continue;
            ItemStack shown = ingredient.getIngredient().item;
            boolean hasStored = false, hasCraftable = false;
            ItemStackAmount candidates = new ItemStackAmount();
            for (IAEStack<?> available : network) {
                if (!ingredient.matches(available)) continue;
                ItemStack icon = iconFor(available, shown);
                if (icon == null) continue;
                if (available.getStackSize() > 0) {
                    hasStored = true;
                    present.put(icon, available.getStackSize());
                }
                if (available.isCraftable()) {
                    hasCraftable = true;
                    candidates.put(icon, 1);
                    allCraftable.put(icon, 1);
                }
            }
            List<ItemStack> choices = candidates.values();
            if (!choices.isEmpty())
                craftable.add(choices.get(OverlayGroupPolicy.candidateIndex(choices.size(), currentTime)));
            if (OverlayGroupPolicy.isMissing(hasCraftable, hasStored, ingredient.getMissingCount()))
                missing.add(shown, ingredient.getMissingCount());
        }
        if (!craftable.isEmpty()) {
            ItemsTooltipLineHandler handler = ItemsTooltipLineHandler
                .of(StatCollector.translateToLocal("gtnhadjust.nei.craftable"), craftable, Integer.MAX_VALUE, 0);
            handler.setLabelColor(EnumChatFormatting.BLUE);
            handler.setAmountRenderer(OverlayIngredientTooltip::drawCraftable);
            tooltip.add(GuiDraw.TOOLTIP_HANDLER + GuiDraw.getTipLineId(handler));
        }
        for (ItemStack icon : allCraftable.values()) {
            present.remove(icon);
            missing.remove(icon);
        }
        for (ItemStack icon : present.values()) missing.remove(icon);
        appendAmounts(tooltip, "gtnhadjust.nei.network_present", EnumChatFormatting.GREEN, present);
        appendAmounts(tooltip, "gtnhadjust.nei.missing", EnumChatFormatting.RED, missing);
    }

    private static ItemStack iconFor(IAEStack<?> available, ItemStack shown) {
        if (available instanceof IAEItemStack) {
            ItemStack stack = ((IAEItemStack) available).getItemStack();
            if (stack == null) return null;
            stack = stack.copy();
            stack.stackSize = 1;
            return stack;
        }
        return StackInfo.isFluidDisplayItem(shown) ? shown.copy() : null;
    }

    private static void appendAmounts(List<String> tooltip, String label, EnumChatFormatting color,
        ItemStackAmount amounts) {
        List<ItemStack> icons = new ArrayList<>();
        for (java.util.Map.Entry<net.minecraft.nbt.NBTTagCompound, Long> entry : amounts.entrySet()) {
            ItemStack icon = StackInfo.loadFromNBT(entry.getKey(), 1);
            if (icon != null) icons.add(icon);
        }
        if (icons.isEmpty()) return;
        ItemsTooltipLineHandler handler = ItemsTooltipLineHandler
            .of(StatCollector.translateToLocal(label), icons, Integer.MAX_VALUE, 0);
        handler.setLabelColor(color);
        ItemsTooltipLineHandler.TotalAmountRenderer renderer = new ItemsTooltipLineHandler.TotalAmountRenderer();
        handler.setAmountRenderer((x, y, stack, ignored) -> renderer.draw(x, y, stack, amounts.getOrDefault(stack, 0)));
        tooltip.add(GuiDraw.TOOLTIP_HANDLER + GuiDraw.getTipLineId(handler));
    }

    private static void drawCraftable(int x, int y, ItemStack stack, long ignored) {
        GuiContainerManager.drawItem(x, y, stack, true, "");
        ItemStack pattern = AEApi.instance()
            .definitions()
            .items()
            .encodedPattern()
            .maybeStack(1)
            .orNull();
        if (pattern == null) return;
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(x, y, 200.0F);
            GL11.glScalef(0.4F, 0.4F, 1.0F);
            GuiContainerManager.drawItem(0, 0, pattern);
        } finally {
            GL11.glPopMatrix();
        }
    }
}
