package com.gtnhadjust.machine.proxy;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import gregtech.api.enums.HeatingCoilLevel;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.common.tileentities.machines.multi.MTEElectricBlastFurnace;
import tectech.thing.metaTileEntity.multi.godforge.MTESmeltingModule;

/** Additional admission rules run after physical source validation and before any input transaction commits. */
public final class ProxyRecipeRequirements {

    @FunctionalInterface
    public interface Requirement {

        CheckRecipeResult check(MTEExtendedPowerMultiBlockBase<?> proxy, MTEMultiBlockBase source, GTRecipe recipe);
    }

    private static final Map<RecipeMap<?>, Requirement> RULES = new HashMap<>();

    // GT, BartWorks, GTNL and GTNR store their installed coil as a HeatingCoilLevel field.
    // Resolve once per concrete class; do not reflect over every recipe attempt or read aggregate machine heat.
    private static final ClassValue<Optional<Field>> COILS = new ClassValue<>() {

        @Override
        protected Optional<Field> computeValue(Class<?> type) {
            for (Class<?> owner = type; owner != null; owner = owner.getSuperclass()) {
                for (Field field : owner.getDeclaredFields()) {
                    if (field.getType() == HeatingCoilLevel.class
                        && !java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                        field.setAccessible(true);
                        return Optional.of(field);
                    }
                }
            }
            return Optional.empty();
        }
    };

    static {
        register(RecipeMaps.blastFurnaceRecipes, ProxyRecipeRequirements::checkBlastHeat);
    }

    private ProxyRecipeRequirements() {}

    /** Register or replace the special admission rule for a recipe pool; ordinary pools need no rule. */
    public static void register(RecipeMap<?> map, Requirement requirement) {
        RULES.put(map, requirement);
    }

    public static CheckRecipeResult check(MTEExtendedPowerMultiBlockBase<?> proxy, MTEMultiBlockBase source,
        RecipeMap<?> map, GTRecipe recipe) {
        Requirement rule = RULES.get(map);
        return rule == null ? CheckRecipeResultRegistry.SUCCESSFUL : rule.check(proxy, source, recipe);
    }

    /** Godforge uses its computed process heat; coil furnaces use physical coil heat plus proxy voltage heat. */
    private static CheckRecipeResult checkBlastHeat(MTEExtendedPowerMultiBlockBase<?> proxy, MTEMultiBlockBase source,
        GTRecipe recipe) {
        if (source instanceof MTESmeltingModule module) {
            int heat = module.getHeat();
            if (com.gtnhadjust.GTNHAdjust.debug) com.gtnhadjust.GTNHAdjust.LOG
                .info("[proxy-heat] godforge effectiveHeat={} requiredHeat={}", heat, recipe.mSpecialValue);
            return heat >= recipe.mSpecialValue ? CheckRecipeResultRegistry.SUCCESSFUL
                : CheckRecipeResultRegistry.insufficientHeat(recipe.mSpecialValue);
        }
        Optional<Field> field = COILS.get(source.getClass());
        if (!field.isPresent()) return SimpleCheckRecipeResult.ofFailure("gtnhadjust_proxy_coil_unavailable");
        HeatingCoilLevel coil;
        try {
            coil = (HeatingCoilLevel) field.get()
                .get(source);
        } catch (IllegalAccessException failure) {
            throw new IllegalStateException("Cannot read source heating coil", failure);
        }
        if (coil == null || coil == HeatingCoilLevel.None)
            return CheckRecipeResultRegistry.insufficientHeat(recipe.mSpecialValue);
        long heat = coil.getHeat() + (long) MTEElectricBlastFurnace.HEAT_PER_VOLTAGE_TIER
            * (GTUtility.getTier(proxy.getMaxInputVoltage()) - MTEElectricBlastFurnace.BASE_TIER_FOR_HEAT_INCREASE);
        return heat >= recipe.mSpecialValue ? CheckRecipeResultRegistry.SUCCESSFUL
            : CheckRecipeResultRegistry.insufficientHeat(recipe.mSpecialValue);
    }
}
