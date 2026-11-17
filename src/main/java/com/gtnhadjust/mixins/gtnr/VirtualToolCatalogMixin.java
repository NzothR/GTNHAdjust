package com.gtnhadjust.mixins.gtnr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnhadjust.GTNHAdjust;
import com.xyp.gtneverreset.common.me.MoldDataManager;

import cpw.mods.fml.common.Loader;
import gregtech.api.enums.ItemList;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTUtility;

/** Extend the one shared GTNR catalog without renumbering existing saved mold selections. */
@Mixin(value = MoldDataManager.class, remap = false)
public abstract class VirtualToolCatalogMixin {

    /** Append shape families and PH toolkit modes before CRIB_MOLDS and pattern caches are initialized. */
    @Inject(method = "buildMoldArray", at = @At("RETURN"), cancellable = true, remap = false)
    private static void gtnhadjust$extendCatalog(CallbackInfoReturnable<ItemStack[]> result) {
        ItemStack[] original = result.getReturnValue();
        List<ItemStack> tools = new ArrayList<>(Arrays.asList(original));
        for (ItemList item : ItemList.values()) {
            String name = item.name();
            if (!(name.startsWith("Shape_Mold_") || name.startsWith("Shape_Slicer_")
                || name.startsWith("Shape_Extruder_")
                || name.startsWith("White_Dwarf_Shape_Extruder_")) || !item.hasBeenSet()) continue;
            ItemStack tool = item.get(1);
            if (tools.stream()
                .noneMatch(existing -> GTUtility.areStacksEqual(existing, tool, false))) tools.add(tool);
        }
        // TST registers its white-dwarf ingot mold independently of the GT ItemList shape families.
        if (Loader.isModLoaded("TwistSpaceTechnology")) {
            ItemStack mold = GTModHandler.getModItem("TwistSpaceTechnology", "MetaItem01", 1, 36);
            if (mold != null && tools.stream()
                .noneMatch(existing -> GTUtility.areStacksEqual(existing, mold, false))) tools.add(mold);
        }
        // PH provider recipes accept the programming toolkit in any mode as a zero-count catalyst.
        // Keep concrete modes in the catalog so its UI never has to render a wildcard metadata item.
        if (Loader.isModLoaded("programmablehatches")) {
            for (int mode = 0; mode < 3; mode++) {
                ItemStack toolkit = GTModHandler.getModItem("programmablehatches", "prog_toolkit", 1, mode);
                if (toolkit != null && tools.stream()
                    .noneMatch(existing -> GTUtility.areStacksEqual(existing, toolkit, false))) tools.add(toolkit);
            }
        }
        result.setReturnValue(tools.toArray(new ItemStack[0]));
        if (GTNHAdjust.debug) GTNHAdjust.LOG
            .info("[gtnr-virtual-tools] catalog: original={}, extended={}", original.length, tools.size());
    }
}
