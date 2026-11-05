package com.gtnhadjust.mixins.fix.gtnl;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.gtnewhorizon.cropsnh.api.ICropCard;
import com.gtnewhorizon.cropsnh.api.IGrowthRequirement;
import com.gtnewhorizon.cropsnh.api.ISeedData;
import com.gtnewhorizon.cropsnh.farming.requirements.SubSoilRequirement;
import com.gtnewhorizon.cropsnh.utility.CropsNHUtils;
import com.gtnhadjust.GTNHAdjust;
import com.science.gtnl.api.IGreenHouse;
import com.science.gtnl.utils.machine.greenHouseManager.GreenHouseStoredCrop;

import gregtech.api.recipe.check.CheckRecipeResult;

@Mixin(targets = "com.science.gtnl.common.machine.multiblock.module.steamElevator.SteamGreenhouseModule", remap = false)
public abstract class SteamGreenhouseSubsoilFix implements IGreenHouse {

    @Unique
    private static final Map<ICropCard, Optional<ItemStack>> gtnhadjust$vanillaSoils = new IdentityHashMap<>();

    @Override
    public int getEffectiveStoredCropCount(GreenHouseStoredCrop stored, ISeedData seed) {
        if (seed != null && gtnhadjust$vanillaSoil(seed.getCrop()) != null) return stored.getSeedCount();
        return IGreenHouse.super.getEffectiveStoredCropCount(stored, seed);
    }

    @Override
    public CheckRecipeResult validateCanGrow(ISeedData seed, GreenHouseStoredCrop stored) {
        ItemStack soil = gtnhadjust$vanillaSoil(seed.getCrop());
        if (soil == null) return IGreenHouse.super.validateCanGrow(seed, stored);
        return IGreenHouse.super.validateCanGrow(seed, new GreenHouseStoredCrop(stored.getSeedStack(), soil.copy()));
    }

    @Unique
    private static ItemStack gtnhadjust$vanillaSoil(ICropCard crop) {
        Optional<ItemStack> cached = gtnhadjust$vanillaSoils.get(crop);
        if (cached != null) return cached.orElse(null);
        List<SubSoilRequirement> requirements = new ArrayList<>();
        for (IGrowthRequirement requirement : crop.getGrowthRequirements()) {
            if (requirement instanceof SubSoilRequirement) requirements.add((SubSoilRequirement) requirement);
        }
        ItemStack result = null;
        if (!requirements.isEmpty()) {
            for (ItemStack candidate : requirements.get(0)
                .getItemsForNEI()) {
                if (candidate == null || candidate.getItem() == null || candidate.stackSize <= 0) continue;
                Block block = CropsNHUtils.getBlockFromItem(candidate);
                String name = block == null ? null : Block.blockRegistry.getNameForObject(block);
                if (name == null || !name.startsWith("minecraft:")) continue;
                boolean valid = true;
                for (SubSoilRequirement requirement : requirements) {
                    if (!requirement.isValidSubSoil(candidate)) {
                        valid = false;
                        break;
                    }
                }
                if (valid) {
                    result = candidate.copy();
                    result.stackSize = 1;
                    break;
                }
            }
        }
        gtnhadjust$vanillaSoils.put(crop, Optional.ofNullable(result));
        if (GTNHAdjust.debug && !requirements.isEmpty()) {
            GTNHAdjust.LOG.info("Steam greenhouse crop {} virtual vanilla subsoil: {}", crop.getId(), result);
        }
        return result;
    }
}
