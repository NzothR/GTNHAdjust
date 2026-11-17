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

    /** Add registered PH shape families once, before CRIB_MOLDS is assigned and any pattern caches are built. */
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
        result.setReturnValue(tools.toArray(new ItemStack[0]));
        if (GTNHAdjust.debug) GTNHAdjust.LOG
            .info("[gtnr-virtual-tools] catalog: original={}, extended={}", original.length, tools.size());
    }
}
