package com.gtnhadjust.machine.proxy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.item.ItemStack;

import com.gtnhadjust.GTNHAdjust;

import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.RecipeMapWorkable;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.common.blocks.ItemMachines;

/** Provides unrestricted locator pool lookup while retaining the legacy controller whitelist for existing callers. */
public final class ProxyFactoryControllerRegistry {

    private static final Set<String> ALLOWED = new HashSet<>();
    static {
        ALLOWED.addAll(ProxyFactoryRecipeWhitelist.ORIGINAL);
        ALLOWED.addAll(ProxyFactoryRecipeWhitelist.DEFAULT_EXTRA);
        ALLOWED.addAll(ProxyFactoryRecipeWhitelist.ADDITIONAL);
    }

    private ProxyFactoryControllerRegistry() {}

    /** Version 0.2 locator sources expose every registered processing map; the legacy whitelist stays separate. */
    public static List<RecipeMap<?>> unrestrictedMaps(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemMachines)) return Collections.emptyList();
        IMetaTileEntity target = ItemMachines.getMetaTileEntity(stack);
        if (!(target instanceof MTEMultiBlockBase)) return Collections.emptyList();
        Collection<RecipeMap<?>> exposed = ((RecipeMapWorkable) target).getAvailableRecipeMaps();
        List<RecipeMap<?>> result = new ArrayList<>();
        for (RecipeMap<?> map : exposed) {
            if (isExecutable(map) && RecipeMap.ALL_RECIPE_MAPS.get(map.unlocalizedName) == map && !result.contains(map))
                result.add(map);
        }
        return result;
    }

    /** Eye of Harmony requires its own simulation and is never executable by a proxy. */
    public static boolean isExecutable(RecipeMap<?> map) {
        return map != null && !"gt.recipe.eyeofharmony".equals(map.unlocalizedName);
    }

    public static List<String> maps(ItemStack stack) {
        if (stack == null || stack.stackSize <= 0 || !(stack.getItem() instanceof ItemMachines))
            return Collections.emptyList();
        IMetaTileEntity target = ItemMachines.getMetaTileEntity(stack);
        if (!(target instanceof RecipeMapWorkable) || target instanceof ProxyFactory) return Collections.emptyList();
        try {
            Collection<RecipeMap<?>> exposed = ((RecipeMapWorkable) target).getAvailableRecipeMaps();
            if (exposed == null) return Collections.emptyList();
            List<String> result = new ArrayList<>();
            for (RecipeMap<?> map : exposed) {
                if (!isExecutable(map) || !ALLOWED.contains(map.unlocalizedName)
                    || RecipeMap.ALL_RECIPE_MAPS.get(map.unlocalizedName) != map
                    || result.contains(map.unlocalizedName)) continue;
                result.add(map.unlocalizedName);
            }
            return result;
        } catch (RuntimeException | LinkageError failure) {
            if (GTNHAdjust.debug) GTNHAdjust.LOG.warn("Cannot inspect proxy controller {}", stack, failure);
            return Collections.emptyList();
        }
    }
}
