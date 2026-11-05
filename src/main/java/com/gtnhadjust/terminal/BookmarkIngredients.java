package com.gtnhadjust.terminal;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.gtnhadjust.GTNHAdjust;

import codechicken.nei.ItemPanels;
import codechicken.nei.PositionedStack;
import codechicken.nei.bookmark.BookmarkItem;
import codechicken.nei.bookmark.BookmarkItem.BookmarkItemType;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.Recipe.RecipeId;
import codechicken.nei.recipe.StackInfo;

/** Select bookmarked variants only when the complete NEI recipe identity matches. */
public final class BookmarkIngredients {

    private static final ThreadLocal<Boolean> ACTIVE = new ThreadLocal<>();

    private BookmarkIngredients() {}

    public static void begin(boolean active) {
        ACTIVE.set(active);
    }

    public static void end() {
        ACTIVE.remove();
    }

    public static List<PositionedStack> get(IRecipeHandler handler, int index) {
        List<PositionedStack> original = handler.getIngredientStacks(index);
        if (!Boolean.TRUE.equals(ACTIVE.get()) || original == null) return original;
        try {
            RecipeId id = RecipeId.of(handler, index, original);
            List<BookmarkItem> choices = bookmarks(id, BookmarkItemType.INGREDIENT);
            if (choices == null) return original;
            List<PositionedStack> result = new ArrayList<>(original.size());
            boolean[] used = new boolean[choices.size()];
            int selected = 0;
            for (PositionedStack position : original) {
                if (position == null || position.items == null) {
                    result.add(position);
                    continue;
                }
                PositionedStack copy = position.copy();
                boolean matched = false;
                for (int pass = 0; pass < 2 && !matched; pass++) {
                    for (int i = 0; i < choices.size() && !matched; i++) {
                        if (pass == 0 && used[i]) continue;
                        BookmarkItem choice = choices.get(i);
                        for (ItemStack candidate : position.items) {
                            if (candidate != null && choice.itemStack != null
                                && StackInfo.equalItemAndNBT(candidate, choice.itemStack, true)) {
                                copy.item = candidate.copy();
                                used[i] = true;
                                selected++;
                                matched = true;
                                break;
                            }
                        }
                    }
                }
                result.add(copy);
            }
            if (GTNHAdjust.terminalDebug)
                GTNHAdjust.LOG.info("Quick terminal: selected {} bookmarked ingredient slots", selected);
            return result;
        } catch (ReflectiveOperationException | RuntimeException failure) {
            if (GTNHAdjust.terminalDebug)
                GTNHAdjust.LOG.info("Quick terminal: bookmark lookup failed; using displayed ingredients", failure);
            return original;
        }
    }

    public static List<?> outputs(IRecipeHandler handler, int index, List<?> original) {
        if (!Boolean.TRUE.equals(ACTIVE.get()) || original == null) return original;
        try {
            RecipeId id = RecipeId.of(handler, index);
            List<BookmarkItem> selected = bookmarks(id, BookmarkItemType.RESULT);
            if (selected == null || selected.isEmpty()) return original;
            List<Object> result = new ArrayList<>();
            for (Object ingredient : original) {
                if (ingredient == null) continue;
                Object stack = Reflection.field(ingredient, "stack");
                for (BookmarkItem choice : selected) {
                    if (matchesOutput(stack, choice.itemStack)) {
                        result.add(ingredient);
                        break;
                    }
                }
            }
            if (result.isEmpty()) {
                if (GTNHAdjust.terminalDebug)
                    GTNHAdjust.LOG.info("Quick terminal: bookmarked outputs could not be matched; using NEI outputs");
                return original;
            }
            if (GTNHAdjust.terminalDebug) GTNHAdjust.LOG
                .info("Quick terminal: kept {} bookmarked output slots of {}", result.size(), original.size());
            return result;
        } catch (ReflectiveOperationException | RuntimeException failure) {
            if (GTNHAdjust.terminalDebug)
                GTNHAdjust.LOG.info("Quick terminal: bookmark output lookup failed; using NEI outputs", failure);
            return original;
        }
    }

    private static boolean matchesOutput(Object stack, ItemStack bookmarked) {
        if (bookmarked == null) return false;
        if (stack instanceof ItemStack) return StackInfo.equalItemAndNBT((ItemStack) stack, bookmarked, true);
        if (stack instanceof FluidStack) {
            FluidStack fluid = StackInfo.getFluid(bookmarked);
            return fluid != null && ((FluidStack) stack).isFluidEqual(fluid);
        }
        return false;
    }

    private static List<BookmarkItem> bookmarks(RecipeId id, BookmarkItemType type)
        throws ReflectiveOperationException {
        List<?> items = (List<?>) Reflection.field(ItemPanels.bookmarkPanel.getGrid(), "bookmarkItems");
        Integer group = null;
        for (Object value : items) {
            BookmarkItem item = (BookmarkItem) value;
            if (item.type != BookmarkItemType.RESULT || item.recipeId == null || !id.equals(item.recipeId)) continue;
            ItemStack expected = id.getResult();
            ItemStack actual = item.recipeId.getResult();
            if (expected == null || actual == null || !StackInfo.equalItemAndNBT(expected, actual, true)) continue;
            group = item.groupId;
            break;
        }
        if (group == null) return null;
        List<BookmarkItem> selected = new ArrayList<>();
        for (Object value : items) {
            BookmarkItem item = (BookmarkItem) value;
            if (item.groupId == group && item.type == type && id.equals(item.recipeId)) selected.add(item);
        }
        return selected;
    }
}
