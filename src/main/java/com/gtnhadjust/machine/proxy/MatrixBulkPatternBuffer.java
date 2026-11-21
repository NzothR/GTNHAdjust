package com.gtnhadjust.machine.proxy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import com.gtnhadjust.GTNHAdjust;
import com.xyp.gtneverreset.api.me.RecipeMapPatternInventory;
import com.xyp.gtneverreset.common.machine.hatch.CircuitMEPatternBuffer;
import com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtneverreset.common.me.PatternRecipeMap;

import appeng.api.storage.data.IAEStack;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;

/** Matrix add-on using GT-Never-Reset's public hatch and protected pattern-slot extension points. */
public final class MatrixBulkPatternBuffer extends CircuitMEPatternBuffer {

    public MatrixBulkPatternBuffer(int id) {
        super(id, "gtnhadjust.matrix_bulk_pattern_buffer", "Matrix Bulk Pattern Input Assembly (ME)");
    }

    private MatrixBulkPatternBuffer(String name, int tier, String[] description, ITexture[][][] textures) {
        super(name, tier, description, textures);
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new MatrixBulkPatternBuffer(mName, mTier, mDescriptionArray, mTextures);
    }

    @Override
    protected PatternSlot<SuperMTEHatchCraftingInputME> createPatternSlot(ItemStack pattern, NBTTagCompound saved) {
        return new BulkSlot(pattern, saved, this);
    }

    static final class BulkSlot extends CircuitSlot implements RecipeMapPatternInventory {

        // Bound the GT array size independently of the total quantity in the hatch.
        private static final int VIEWS_PER_TYPE = 128;

        private BulkSlot(ItemStack pattern, NBTTagCompound saved, MatrixBulkPatternBuffer parent) {
            super(pattern, saved, parent);
        }

        @Override
        public String getRecipeMapName() {
            var details = getPatternDetails();
            return details == null ? null : PatternRecipeMap.name(details.getPattern());
        }

        void extendItems(List<ItemStack> inputs, List<Runnable> debits, ProxyFactoryProcessingLogic logic) {
            for (var stock : getStoredItems()) appendViews(stock, inputs, debits, () -> {
                updateSlotItems();
                return itemInventory;
            },
                AEItemStack::create,
                stack -> stack.stackSize,
                (stack, count) -> stack.stackSize = count,
                logic::getPlannedConsumption,
                view -> view.getItemStack());
        }

        void extendFluids(List<FluidStack> inputs, List<Runnable> debits, ProxyFactoryProcessingLogic logic) {
            for (var stock : getStoredFluids()) appendViews(stock, inputs, debits, () -> {
                updateSlotFluids();
                return fluidInventory;
            },
                AEFluidStack::create,
                stack -> stack.amount,
                (stack, count) -> stack.amount = count,
                logic::getPlannedConsumption,
                view -> view.getFluidStack());
        }

        /** Append detached views beyond the native first stack and settle their debit only after GT commits. */
        private static <S extends IAEStack<S>, V> void appendViews(S stock, List<V> inputs, List<Runnable> debits,
            Supplier<List<V>> liveViews, Function<V, S> toAe, ToIntFunction<V> count, ObjIntConsumer<V> setCount,
            ToIntFunction<V> plannedConsumption, Function<S, V> makeView) {
            long remaining = Math.min(stock.getStackSize(), (long) VIEWS_PER_TYPE * Integer.MAX_VALUE)
                - Integer.MAX_VALUE;
            if (remaining <= 0) return;
            List<V> extra = new ArrayList<>();
            while (remaining > 0) {
                int amount = (int) Math.min(remaining, Integer.MAX_VALUE);
                V view = makeView.apply(
                    stock.copy()
                        .setStackSize(amount));
                extra.add(view);
                remaining -= amount;
            }
            inputs.addAll(extra);
            debits.add(() -> {
                long consumed = 0;
                for (V view : extra) consumed += plannedConsumption.applyAsInt(view);
                if (consumed == 0) return;
                long total = consumed;
                while (consumed > 0) {
                    V matching = null;
                    for (V live : liveViews.get()) {
                        S ae = toAe.apply(live);
                        if (ae != null && stock.isSameType(ae)) {
                            matching = live;
                            break;
                        }
                    }
                    if (matching == null) throw new IllegalStateException("Matrix bulk pattern stock changed");
                    int debit = (int) Math.min(consumed, count.applyAsInt(matching));
                    setCount.accept(matching, count.applyAsInt(matching) - debit);
                    consumed -= debit;
                }
                liveViews.get();
                if (GTNHAdjust.debug)
                    GTNHAdjust.LOG.info("[proxy-bulk] debited {} additional units of {}", total, stock);
            });
        }
    }
}
