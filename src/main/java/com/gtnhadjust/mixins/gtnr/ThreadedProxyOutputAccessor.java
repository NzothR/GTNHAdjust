package com.gtnhadjust.mixins.gtnr;

import java.math.BigInteger;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadOutputs;

/** Exposes the native persisted ledger for graph routing and the matrix's AE output fallback. */
@Mixin(value = RecipeThreadOutputs.class, remap = false)
public interface ThreadedProxyOutputAccessor {

    @Accessor("items")
    ItemStack[] gtnhadjust$getItems();

    @Accessor("fluids")
    FluidStack[] gtnhadjust$getFluids();

    @Accessor("itemAmounts")
    BigInteger[] gtnhadjust$getItemAmounts();

    @Accessor("fluidAmounts")
    BigInteger[] gtnhadjust$getFluidAmounts();
}
