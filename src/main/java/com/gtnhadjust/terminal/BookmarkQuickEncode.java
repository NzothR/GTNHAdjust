package com.gtnhadjust.terminal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.glodblock.github.common.item.ItemFluidDrop;
import com.glodblock.github.common.item.ItemFluidPacket;
import com.glodblock.github.util.Util;
import com.gtnhadjust.GTNHAdjust;
import com.xyp.gtneverreset.client.gui.GuiQuickEncodingTerminal;
import com.xyp.gtneverreset.common.me.PatternRecipeMap;
import com.xyp.gtneverreset.common.packet.RecipeTransferPayload;
import com.xyp.gtneverreset.common.terminal.DualTerminalGuiObject;
import com.xyp.gtneverreset.config.ClientConfig;

import appeng.api.AEApi;
import appeng.api.storage.data.AEStackTypeRegistry;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import codechicken.nei.BookmarkPanel;
import codechicken.nei.PositionedStack;
import codechicken.nei.bookmark.BookmarkGrid;
import codechicken.nei.bookmark.BookmarkItem;
import codechicken.nei.bookmark.BookmarkItem.BookmarkItemType;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.Recipe.RecipeId;
import codechicken.nei.recipe.RecipeHandlerRef;
import codechicken.nei.recipe.StackInfo;
import gregtech.common.items.ItemFluidDisplay;
import gregtech.nei.GTNEIDefaultHandler;

/** Transfers the clicked bookmark's own group directly, without the NEI smart selection path. */
public final class BookmarkQuickEncode {

    private BookmarkQuickEncode() {}

    public static void encode(GuiQuickEncodingTerminal terminal, BookmarkPanel panel, BookmarkItem clicked) {
        try {
            List<BookmarkItem> inputs = new ArrayList<>(), outputs = new ArrayList<>();
            BookmarkGrid grid = panel.getGrid();
            for (int i = 0; i < grid.size(); i++) {
                BookmarkItem item = grid.getBookmarkItem(i);
                if (!sameRecipe(clicked, item)) continue;
                if (item.type == BookmarkItemType.INGREDIENT) inputs.add(item);
                else if (item.type == BookmarkItemType.RESULT) outputs.add(item);
            }
            RecipeHandlerRef ref = RecipeHandlerRef.of(clicked.recipeId);
            IRecipeHandler handler = ref == null ? null : ref.handler;
            String identifier = handler == null ? "" : handler.getOverlayIdentifier();
            String mode = identifier == null ? "" : identifier.toLowerCase(Locale.ROOT);
            boolean crafting = "crafting".equals(mode) || "crafting2x2".equals(mode)
                || handler == null && clicked.recipeId.isShapedRecipe();
            if (crafting && ref == null) {
                ClientNotice.show("无法还原收藏配方的合成格位置，未写入样板。");
                return;
            }
            if (!crafting && (inputs.size() > DualTerminalGuiObject.PATTERN_SLOT_COUNT
                || outputs.size() > DualTerminalGuiObject.PATTERN_SLOT_COUNT)) {
                ClientNotice.show("收藏配方超过终端的样板槽位上限，未写入样板。");
                return;
            }
            IAEStack<?>[] in = new IAEStack<?>[DualTerminalGuiObject.PATTERN_SLOT_COUNT];
            IAEStack<?>[] out = new IAEStack<?>[DualTerminalGuiObject.PATTERN_SLOT_COUNT];
            boolean arcane = mode.equals("thaumcraft.arcane.shaped") || mode.equals("thaumcraft.arcane.shapeless")
                || mode.equals("thaumcraft.wands");
            NBTTagList arcaneLayout = null;
            if (crafting || arcane) {
                fillGrid(in, inputs, handler.getIngredientStacks(ref.recipeIndex), arcane);
                if (arcane) {
                    arcaneLayout = new NBTTagList();
                    for (int i = 0; i < 9; i++) {
                        ItemStack stack = in[i] instanceof IAEItemStack ? ((IAEItemStack) in[i]).getItemStack() : null;
                        arcaneLayout
                            .appendTag(stack == null ? new NBTTagCompound() : stack.writeToNBT(new NBTTagCompound()));
                    }
                    for (int i = 0; i < outputs.size(); i++) out[i] = processingStack(oneRecipe(outputs.get(i)));
                }
            } else {
                for (int i = 0; i < inputs.size(); i++) in[i] = processingStack(oneRecipe(inputs.get(i)));
                for (int i = 0; i < outputs.size(); i++) out[i] = processingStack(oneRecipe(outputs.get(i)));
            }
            String map = handler instanceof GTNEIDefaultHandler
                ? PatternRecipeMap.nameForImport(((GTNEIDefaultHandler) handler).getRecipeMap())
                : null;
            if (GTNHAdjust.proxyFactoryEnabled && map == null && "smelting".equals(mode))
                map = gregtech.api.recipe.RecipeMaps.furnaceRecipes.unlocalizedName;
            String search = null;
            if (ClientConfig.wirelessTerminal.fillSearchFromNeiRecipes && handler != null) {
                if (crafting) {
                    ItemStack assembler = AEApi.instance()
                        .definitions()
                        .blocks()
                        .molecularAssembler()
                        .maybeStack(1)
                        .orNull();
                    search = assembler == null ? handler.getRecipeName() : Platform.getItemDisplayName(assembler);
                } else search = handler.getRecipeName();
            }
            terminal.transferRecipe(
                new RecipeTransferPayload(crafting, true, 4, false, in, out, arcaneLayout, map),
                search);
        } catch (RuntimeException | LinkageError failure) {
            GTNHAdjust.LOG.warn("Quick terminal: cannot encode clicked bookmark", failure);
            ClientNotice.show("收藏配方写入失败，详情见客户端日志。");
        }
    }

    private static boolean sameRecipe(BookmarkItem clicked, BookmarkItem candidate) {
        if (candidate == null || candidate.groupId != clicked.groupId
            || candidate.recipeId == null
            || !clicked.recipeId.equals(candidate.recipeId)) return false;
        RecipeId first = clicked.recipeId, second = candidate.recipeId;
        ItemStack a = first.getResult(), b = second.getResult();
        return a == null ? b == null : b != null && StackInfo.equalItemAndNBT(a, b, true);
    }

    private static ItemStack oneRecipe(BookmarkItem item) {
        return item.getItemStack(item.getAmount(1));
    }

    private static void fillGrid(IAEStack<?>[] dest, List<BookmarkItem> inputs, List<PositionedStack> positions,
        boolean arcane) {
        long[] remaining = new long[inputs.size()];
        for (int i = 0; i < inputs.size(); i++) remaining[i] = inputs.get(i)
            .getAmount(1);
        int fallback = 0;
        for (PositionedStack position : positions) {
            if (position == null) continue;
            int x = position.relx - (arcane ? 47 : 25), y = position.rely - (arcane ? 38 : 6);
            int width = arcane ? 28 : 18, height = arcane ? 27 : 18;
            if (arcane && (x < 0 || y < 0 || x % width != 0 || y % height != 0)) continue;
            int column = x / width, row = y / height;
            int slot = column >= 0 && column < 3 && row >= 0 && row < 3 ? column + row * 3 : arcane ? 9 : fallback;
            fallback = Math.max(fallback, slot + 1);
            if (slot >= 9) continue;
            for (int i = 0; i < inputs.size(); i++) {
                if (remaining[i] <= 0 || position.items == null) continue;
                ItemStack item = oneRecipe(inputs.get(i));
                boolean matches = false;
                for (ItemStack candidate : position.items) {
                    if (candidate != null && item != null && StackInfo.equalItemAndNBT(candidate, item, true)) {
                        matches = true;
                        break;
                    }
                }
                if (!matches) continue;
                item.stackSize = 1;
                dest[slot] = AEItemStack.create(item);
                remaining[i]--;
                break;
            }
        }
    }

    private static IAEStack<?> processingStack(ItemStack item) {
        if (item == null) return null;
        if (StackInfo.isFluidDisplayItem(item) || item.getItem() instanceof ItemFluidDisplay
            || item.getItem() instanceof ItemFluidDrop
            || item.getItem() instanceof ItemFluidPacket) {
            IAEStack<?> fluid = Util.getAEFluidFromItem(item);
            if (fluid != null) {
                NBTTagCompound nbt = StackInfo.itemStackToNBT(item);
                if (StackInfo.isFluidDisplayItem(item) && nbt != null && nbt.hasKey("Count"))
                    fluid.setStackSize(nbt.getLong("Count"));
                return fluid;
            }
        }
        for (IAEStackType<?> type : AEStackTypeRegistry.getSortedTypes()) {
            if ("item".equals(type.getId()) || "fluid".equals(type.getId())) continue;
            IAEStack<?> converted = type.convertStackFromItem(item);
            if (converted != null) return converted;
        }
        return AEItemStack.create(item.copy());
    }
}
