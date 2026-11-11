package com.gtnhadjust.machine.threadedproxy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

/** Exact identities only: count is ignored, while item, damage and NBT are retained without ore unification. */
final class PatternInputConflicts {

    private PatternInputConflicts() {}

    /** Finds a shared consumed ingredient in expected O(total input entries) time and space. */
    static String find(List<ThreadedJointProxy.Definition> definitions) {
        Map<Key, Integer> owners = new HashMap<>();
        for (int i = 0; i < definitions.size(); i++) {
            ProxyPatternRecipe recipe = definitions.get(i).recipe;
            for (ItemStack item : recipe.consumedItems) {
                Integer previous = owners.putIfAbsent(new Key(item), i);
                if (previous != null && previous != i) return describe(definitions, previous, i, item.getDisplayName());
            }
            for (FluidStack fluid : recipe.consumedFluids) {
                Integer previous = owners.putIfAbsent(new Key(fluid), i);
                if (previous != null && previous != i)
                    return describe(definitions, previous, i, fluid.getLocalizedName());
            }
        }
        return "";
    }

    private static String describe(List<ThreadedJointProxy.Definition> definitions, int first, int second,
        String input) {
        return definitions.get(first).label + " / " + definitions.get(second).label + ": " + input;
    }

    private static final class Key {

        private final Object type;
        private final int damage;
        private final NBTTagCompound tag;

        Key(ItemStack stack) {
            type = stack.getItem();
            damage = stack.getItemDamage();
            tag = stack.getTagCompound();
        }

        Key(FluidStack stack) {
            type = stack.getFluid();
            damage = 0;
            tag = stack.tag;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Key key && type == key.type && damage == key.damage && Objects.equals(tag, key.tag);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, damage, tag);
        }
    }
}
