package com.gtnhadjust.machine.proxy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import com.gtnhadjust.GTNHAdjust;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.util.GTRecipe;

/** Ordered locator storage and first-source index. Rebuilding this index never accesses or loads a world. */
public final class ProxyLocatorPool {

    private final List<MachineLocatorRecord> records = new ArrayList<>();
    private final Map<RecipeMap<?>, MachineLocatorRecord> sources = new LinkedHashMap<>();

    /** Backend entry point for the next input/UI stage. Duplicate records deliberately retain their order. */
    public void replace(List<ItemStack> locators) {
        List<MachineLocatorRecord> replacement = new ArrayList<>(locators.size());
        for (ItemStack locator : locators) {
            MachineLocatorRecord record = MachineLocatorItem.read(locator);
            if (record == null) throw new IllegalArgumentException("Expected a recorded multiblock machine locator");
            replacement.add(record);
        }
        records.clear();
        records.addAll(replacement);
        rebuild();
    }

    private void rebuild() {
        sources.clear();
        for (MachineLocatorRecord record : records) {
            for (RecipeMap<?> map : ProxyFactoryControllerRegistry.unrestrictedMaps(record.controller()))
                sources.putIfAbsent(map, record);
        }
    }

    public List<RecipeMap<?>> recipeMaps() {
        return java.util.Collections.unmodifiableList(new ArrayList<>(sources.keySet()));
    }

    /** No chunk tickets, dimension initialization, background checks or fallback to later same-pool records. */
    public CheckRecipeResult checkStart(ProxyFactory proxy, RecipeMap<?> map, GTRecipe recipe) {
        MachineLocatorRecord record = sources.get(map);
        if (record == null) return failure("missing_locator");
        if (GTNHAdjust.debug) GTNHAdjust.LOG.info(
            "[proxy-locator] checking map={} source={} dim={} pos=({}, {}, {})",
            map.unlocalizedName,
            record.controller(),
            record.dimension,
            record.x,
            record.y,
            record.z);
        World world = DimensionManager.getWorld(record.dimension);
        if (world == null || !world.getChunkProvider()
            .chunkExists(record.x >> 4, record.z >> 4)) return failure("unloaded");
        TileEntity tile = world.getTileEntity(record.x, record.y, record.z);
        if (!(tile instanceof IGregTechTileEntity gt) || gt.getMetaTileEntity() == null)
            return failure("missing_controller");
        if (!record.matches(gt.getMetaTileEntity())) return failure("wrong_controller");
        if (!(gt.getMetaTileEntity() instanceof MTEMultiBlockBase source)) return failure("wrong_controller");
        if (!source.mMachine) return failure("unformed");
        CheckRecipeResult result = ProxyRecipeRequirements.check(proxy, source, map, recipe);
        if (GTNHAdjust.debug) GTNHAdjust.LOG.info(
            "[proxy-locator] map={} source={} dim={} pos=({}, {}, {}) result={}",
            map.unlocalizedName,
            record.controller(),
            record.dimension,
            record.x,
            record.y,
            record.z,
            result);
        return result;
    }

    private static CheckRecipeResult failure(String reason) {
        if (GTNHAdjust.debug) GTNHAdjust.LOG.info("[proxy-locator] admission rejected: {}", reason);
        return SimpleCheckRecipeResult.ofFailure("gtnhadjust_proxy_" + reason);
    }

    public NBTTagList save() {
        NBTTagList list = new NBTTagList();
        for (MachineLocatorRecord record : records) list.appendTag(record.save());
        return list;
    }

    public void load(NBTTagList list) {
        records.clear();
        for (int i = 0; i < list.tagCount(); i++) {
            MachineLocatorRecord record = MachineLocatorRecord.load(list.getCompoundTagAt(i));
            if (record != null) records.add(record);
        }
        rebuild();
    }
}
