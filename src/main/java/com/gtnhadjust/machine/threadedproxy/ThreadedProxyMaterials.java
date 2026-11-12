package com.gtnhadjust.machine.threadedproxy;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipe.RecipeItemInput;

/**
 * Equal per-type material quotas. Portions refer to real hatch stacks, so their committed deltas cannot overwrite each
 * other.
 */
final class ThreadedProxyMaterials {

    private ThreadedProxyMaterials() {}

    /** Divide only consumed resources; non-consumed catalysts remain shared and never reduce another thread's quota. */
    static void divide(List<ThreadedJointProxy.ThreadInputs> inputs) {
        Map<Key, ItemGroup> items = new LinkedHashMap<>();
        Map<Key, FluidGroup> fluids = new LinkedHashMap<>();
        List<Share> shares = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            ThreadedJointProxy.ThreadInputs input = inputs.get(i);
            Share share = new Share();
            shares.add(share);
            input.share = share;
            for (ItemStack stack : input.items) {
                if (!consumes(input.definition.execution, stack)) {
                    share.items.add(stack);
                    continue;
                }
                ItemGroup group = items.computeIfAbsent(new Key(stack), ignored -> new ItemGroup());
                group.consumers.add(i);
                if (group.seen.put(stack, Boolean.TRUE) == null) group.stacks.add(stack);
            }
            for (FluidStack stack : input.fluids) {
                if (!consumes(input.definition.execution, stack)) {
                    share.fluids.add(stack);
                    continue;
                }
                FluidGroup group = fluids.computeIfAbsent(new Key(stack), ignored -> new FluidGroup());
                group.consumers.add(i);
                if (group.seen.put(stack, Boolean.TRUE) == null) group.stacks.add(stack);
            }
        }
        for (ItemGroup group : items.values()) group.divide(shares);
        for (FluidGroup group : fluids.values()) group.divide(shares);
        for (int i = 0; i < inputs.size(); i++) {
            inputs.get(i).items = shares.get(i).items.toArray(new ItemStack[0]);
            inputs.get(i).fluids = shares.get(i).fluids.toArray(new FluidStack[0]);
        }
    }

    private static boolean consumes(GTRecipe recipe, ItemStack item) {
        for (RecipeItemInput required : recipe.getCachedCombinedItemInputs())
            if (required.inputAmount > 0 && required.matchesRecipe(GTOreDictUnificator.getAssociation(item), item))
                return true;
        return false;
    }

    private static boolean consumes(GTRecipe recipe, FluidStack fluid) {
        for (int i = 0; i < recipe.mFluidInputs.length; i++) {
            FluidStack required = recipe.mFluidInputs[i];
            if (required != null && required.amount > 0 && required.isFluidEqual(fluid)) return true;
            if (recipe.mAltFluidInputs != null && i < recipe.mAltFluidInputs.length
                && recipe.mAltFluidInputs[i] != null)
                for (FluidStack alternative : recipe.mAltFluidInputs[i])
                    if (alternative != null && alternative.amount > 0 && alternative.isFluidEqual(fluid)) return true;
        }
        return false;
    }

    static final class Share {

        private final List<ItemStack> items = new ArrayList<>();
        private final List<FluidStack> fluids = new ArrayList<>();
        private final List<Runnable> commits = new ArrayList<>();

        /**
         * Each view owns a disjoint portion of its source; only consumed deltas are subtracted after GT accepts the
         * task.
         */
        void commit() {
            for (Runnable commit : commits) commit.run();
        }
    }

    private static final class ItemGroup {

        final List<ItemStack> stacks = new ArrayList<>();
        final IdentityHashMap<ItemStack, Boolean> seen = new IdentityHashMap<>();
        final Set<Integer> consumers = new LinkedHashSet<>();

        void divide(List<Share> shares) {
            long total = 0;
            for (ItemStack stack : stacks) total += stack.stackSize;
            long base = total / consumers.size(), remainder = total % consumers.size();
            int sourceIndex = 0, available = stacks.get(0).stackSize;
            for (int consumer : consumers) {
                long quota = base + (remainder-- > 0 ? 1 : 0);
                Share share = shares.get(consumer);
                while (quota > 0) {
                    if (available == 0) available = stacks.get(++sourceIndex).stackSize;
                    ItemStack source = stacks.get(sourceIndex);
                    int amount = (int) Math.min(quota, available);
                    ItemStack view = source.copy();
                    view.stackSize = amount;
                    share.items.add(view);
                    share.commits.add(() -> source.stackSize -= amount - view.stackSize);
                    available -= amount;
                    quota -= amount;
                }
            }
        }
    }

    private static final class FluidGroup {

        final List<FluidStack> stacks = new ArrayList<>();
        final IdentityHashMap<FluidStack, Boolean> seen = new IdentityHashMap<>();
        final Set<Integer> consumers = new LinkedHashSet<>();

        void divide(List<Share> shares) {
            long total = 0;
            for (FluidStack stack : stacks) total += stack.amount;
            long base = total / consumers.size(), remainder = total % consumers.size();
            int sourceIndex = 0, available = stacks.get(0).amount;
            for (int consumer : consumers) {
                long quota = base + (remainder-- > 0 ? 1 : 0);
                Share share = shares.get(consumer);
                while (quota > 0) {
                    if (available == 0) available = stacks.get(++sourceIndex).amount;
                    FluidStack source = stacks.get(sourceIndex);
                    int amount = (int) Math.min(quota, available);
                    FluidStack view = source.copy();
                    view.amount = amount;
                    share.fluids.add(view);
                    share.commits.add(() -> source.amount -= amount - view.amount);
                    available -= amount;
                    quota -= amount;
                }
            }
        }
    }

    /** Raw item/meta/NBT or fluid/NBT identity, without merging ore-dictionary alternatives. */
    private static final class Key {

        final Object type;
        final int damage;
        final NBTTagCompound tag;

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
