package com.gtnhadjust.terminal;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.gtnhadjust.GTNHAdjust;
import com.xyp.gtneverreset.client.gui.GuiQuickEncodingTerminal;

import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import codechicken.nei.ItemPanels;
import codechicken.nei.PositionedStack;
import codechicken.nei.Widget;
import codechicken.nei.bookmark.BookmarkItem;
import codechicken.nei.bookmark.BookmarkItem.BookmarkItemType;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.Recipe.RecipeId;
import codechicken.nei.recipe.StackInfo;
import codechicken.nei.recipe.widget.RecipeGroup;
import codechicken.nei.recipe.widget.RecipeWidget;
import codechicken.nei.scroll.ScrollContainer;
import cpw.mods.fml.common.registry.GameRegistry;

/** Chooses only actual NEI alternatives; grouped recipes are selected as a whole. */
public final class SmartIngredients {

    private static final ThreadLocal<Context> ACTIVE = new ThreadLocal<>();
    private static final String[] TIERS = { "ULV", "LV", "MV", "HV", "EV", "IV", "LuV", "ZPM", "UV", "UHV", "UEV",
        "UIV", "UMV", "UXV", "MAX" };

    private SmartIngredients() {}

    public static int begin(GuiContainer gui, IRecipeHandler handler, int index) {
        ACTIVE.remove();
        if (!(gui instanceof GuiQuickEncodingTerminal)) return index;
        try {
            Context context = new Context((GuiQuickEncodingTerminal) gui);
            if (hasFullRecipe(handler, index, context.bookmarks)) return index;
            ACTIVE.set(context);
            int chosen = chooseRecipe(handler, index, context);
            if (GTNHAdjust.terminalDebug && chosen != index)
                GTNHAdjust.LOG.info("Quick terminal: selected grouped recipe {} instead of {}", chosen, index);
            return chosen;
        } catch (ReflectiveOperationException | RuntimeException failure) {
            if (GTNHAdjust.terminalDebug)
                GTNHAdjust.LOG.info("Quick terminal: smart ingredient selection unavailable", failure);
            ACTIVE.remove();
            return index;
        }
    }

    public static void end() {
        ACTIVE.remove();
    }

    public static List<PositionedStack> select(List<PositionedStack> original) {
        Context context = ACTIVE.get();
        if (context == null || original == null) return original;
        List<PositionedStack> result = new ArrayList<>(original.size());
        for (PositionedStack slot : original) {
            if (slot == null || slot.items == null || slot.items.length < 2) {
                result.add(slot);
                continue;
            }
            Choice choice = choose(slot, context);
            if (choice.rank == 0) {
                result.add(slot);
                continue;
            }
            PositionedStack copy = slot.copy();
            copy.item = choice.item.copy();
            result.add(copy);
        }
        return result;
    }

    private static boolean hasFullRecipe(IRecipeHandler handler, int index, List<BookmarkItem> bookmarks) {
        RecipeId id = RecipeId.of(handler, index);
        for (BookmarkItem item : bookmarks)
            if (item.type == BookmarkItemType.RESULT && id.equals(item.recipeId)) for (BookmarkItem input : bookmarks)
                if (input.type == BookmarkItemType.INGREDIENT && input.groupId == item.groupId
                    && id.equals(input.recipeId)) return true;
        return false;
    }

    private static int chooseRecipe(IRecipeHandler handler, int index, Context context)
        throws ReflectiveOperationException {
        if (!(Minecraft.getMinecraft().currentScreen instanceof GuiRecipe<?>screen)) return index;
        ScrollContainer container = (ScrollContainer) Reflection.field(screen, "container");
        for (Widget widget : container.getWidgets()) {
            if (!(widget instanceof RecipeWidget)) continue;
            RecipeGroup group = (RecipeGroup) Reflection.field(widget, "group");
            if (group.getHandler() != handler || !group.contains(index) || group.size() < 2) continue;
            for (BookmarkItem bookmark : context.bookmarks) {
                if (bookmark.type != BookmarkItemType.RESULT || bookmark.recipeId == null) continue;
                for (int member = 0; member < group.size(); member++) {
                    int recipeIndex = group.getRecipeIndex(member);
                    if (bookmark.recipeId.equals(RecipeId.of(handler, recipeIndex))
                        && hasFullRecipe(handler, recipeIndex, context.bookmarks)) {
                        ACTIVE.remove();
                        return recipeIndex;
                    }
                }
            }
            int selected = index;
            Score best = score(group, group.indexOf(index), context);
            for (int member = 0; member < group.size(); member++) {
                Score candidate = score(group, member, context);
                if (candidate.betterThan(best) || candidate.sameAs(best) && candidate.hasPreference()) {
                    selected = group.getRecipeIndex(member);
                    best = candidate;
                }
            }
            return selected;
        }
        return index;
    }

    private static Score score(RecipeGroup group, int member, Context context) {
        Score score = new Score();
        for (PositionedStack slot : group.getInputs(member)) {
            if (slot == null || slot.items == null
                || slot.items.length == 0
                || !differsBetweenMembers(group, member, slot, context)) continue;
            Choice c = choose(slot, context);
            if (c.rank > 0) score.counts[c.rank]++;
            if (c.rank == 4) score.bookmarkOrder += c.order;
        }
        return score;
    }

    private static boolean differsBetweenMembers(RecipeGroup group, int member, PositionedStack slot, Context context) {
        Choice selected = choose(slot, context);
        java.awt.Point key = group.getSlotKey(slot);
        for (int other = 0; other < group.size(); other++) {
            if (other == member) continue;
            PositionedStack alternative = group.getSlot(other, key);
            if (alternative == null || alternative.items == null || alternative.items.length == 0) return true;
            Choice choice = choose(alternative, context);
            if (selected.rank != choice.rank || !same(selected.item, choice.item)) return true;
        }
        return false;
    }

    private static Choice choose(PositionedStack slot, Context context) {
        int n = slot.items.length;
        int[] favorites = new int[n];
        java.util.Arrays.fill(favorites, -1);
        boolean[] circuits = new boolean[n], craftable = new boolean[n], stored = new boolean[n];
        for (int i = 0; i < n; i++) {
            ItemStack candidate = slot.items[i];
            if (candidate == null) continue;
            for (int j = 0; j < context.bookmarks.size(); j++) {
                BookmarkItem favorite = context.bookmarks.get(j);
                if (favorite.type != BookmarkItemType.INGREDIENT && same(candidate, favorite.itemStack)) {
                    favorites[i] = j;
                    break;
                }
            }
            IAEStack<?> found = context.find(candidate);
            if (found != null) {
                craftable[i] = found.isCraftable();
                stored[i] = found.getStackSize() > 0;
            }
        }
        // Ore dictionary circuit slots may accept several tiers; use the lowest allowed tier.
        for (String tier : TIERS) {
            Item any = GameRegistry.findItem("dreamcraft", "Circuit" + tier);
            if (any == null) continue;
            boolean found = false;
            for (int i = 0; i < n; i++) if (slot.items[i] != null && slot.items[i].getItem() == any) {
                circuits[i] = true;
                found = true;
            }
            if (found) break;
        }
        int index = IngredientPriority.select(favorites, circuits, craftable, stored);
        if (index < 0) return new Choice(slot.item, 0, 0);
        int rank = favorites[index] >= 0 ? 4 : circuits[index] ? 3 : craftable[index] ? 2 : 1;
        return new Choice(slot.items[index], rank, Math.max(0, favorites[index]));
    }

    private static boolean same(ItemStack candidate, ItemStack favorite) {
        if (candidate == null || favorite == null) return false;
        FluidStack a = StackInfo.isFluidDisplayItem(candidate) ? StackInfo.getFluid(candidate) : null;
        FluidStack b = StackInfo.isFluidDisplayItem(favorite) ? StackInfo.getFluid(favorite) : null;
        return a != null || b != null ? a != null && b != null && a.isFluidEqual(b)
            : StackInfo.equalItemAndNBT(candidate, favorite, true);
    }

    private static final class Choice {

        final ItemStack item;
        final int rank;
        final int order;

        Choice(ItemStack item, int rank, int order) {
            this.item = item;
            this.rank = rank;
            this.order = order;
        }
    }

    private static final class Score {

        final int[] counts = new int[5];
        int bookmarkOrder;

        boolean betterThan(Score other) {
            for (int i = 4; i >= 1; i--) if (counts[i] != other.counts[i]) return counts[i] > other.counts[i];
            return bookmarkOrder < other.bookmarkOrder;
        }

        boolean sameAs(Score other) {
            return java.util.Arrays.equals(counts, other.counts) && bookmarkOrder == other.bookmarkOrder;
        }

        boolean hasPreference() {
            for (int i = 1; i < counts.length; i++) if (counts[i] > 0) return true;
            return false;
        }
    }

    private static final class Context {

        final List<BookmarkItem> bookmarks;
        final IItemList<IAEStack<?>> network;

        @SuppressWarnings("unchecked")
        Context(GuiQuickEncodingTerminal terminal) throws ReflectiveOperationException {
            bookmarks = new ArrayList<>(
                (List<BookmarkItem>) Reflection.field(ItemPanels.bookmarkPanel.getGrid(), "bookmarkItems"));
            Object repo = Reflection.field(terminal, "repo");
            network = repo == null ? null : (IItemList<IAEStack<?>>) Reflection.field(repo, "list");
        }

        IAEStack<?> find(ItemStack item) {
            if (item == null || network == null) return null;
            FluidStack fluid = StackInfo.isFluidDisplayItem(item) ? StackInfo.getFluid(item) : null;
            IAEStack<?> query = fluid == null ? AEItemStack.create(item) : AEFluidStack.create(fluid);
            return query == null ? null : network.findPrecise(query);
        }
    }
}
