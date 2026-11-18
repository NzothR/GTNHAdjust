package com.gtnhadjust.machine.proxy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import com.gtnhadjust.GTNHAdjust;

import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.util.GTRecipe;

/** Ordered locator storage with a separate direct index that other proxies can read without recursion. */
public final class ProxyLocatorPool {

    private final List<MachineLocatorRecord> records = new ArrayList<>();
    private final Map<RecipeMap<?>, MachineLocatorRecord> directSources = new LinkedHashMap<>();
    // Only intermediate proxy records have an empty map list here.
    private final Map<MachineLocatorRecord, List<RecipeMap<?>>> indexedRecords = new LinkedHashMap<>();
    private Map<RecipeMap<?>, MachineLocatorRecord> sources = directSources;
    private Map<RecipeMap<?>, MachineLocatorRecord> intermediates = Collections.emptyMap();
    private List<RecipeMap<?>> availableMaps = Collections.emptyList();
    private boolean hasIndirect;

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
        directSources.clear();
        indexedRecords.clear();
        hasIndirect = false;
        for (MachineLocatorRecord record : records) {
            ItemStack controller = record.controller();
            if (GTNHAdjust.proxyIndirectLocators && ProxyFactoryControllerRegistry.isProxyController(controller)) {
                hasIndirect = true;
                indexedRecords.put(record, Collections.emptyList());
            } else {
                List<RecipeMap<?>> maps = ProxyFactoryControllerRegistry.unrestrictedMaps(controller);
                if (!maps.isEmpty()) indexedRecords.put(record, maps);
                for (RecipeMap<?> map : maps) directSources.putIfAbsent(map, record);
            }
        }
        sources = directSources;
        intermediates = Collections.emptyMap();
        availableMaps = Collections.unmodifiableList(new ArrayList<>(directSources.keySet()));
    }

    /** Refresh the live one-hop routes in the outer machine's slot order, without requesting chunks. */
    public boolean refreshIndirect(ProxyLocatorHost owner) {
        if (!hasIndirect) return false;
        Map<RecipeMap<?>, MachineLocatorRecord> resolved = new LinkedHashMap<>();
        Map<RecipeMap<?>, MachineLocatorRecord> via = new LinkedHashMap<>();
        for (Map.Entry<MachineLocatorRecord, List<RecipeMap<?>>> entry : indexedRecords.entrySet()) {
            MachineLocatorRecord record = entry.getKey();
            if (!entry.getValue()
                .isEmpty()) {
                for (RecipeMap<?> map : entry.getValue()) resolved.putIfAbsent(map, record);
                continue;
            }
            World world = loadedWorld(record);
            if (world == null) continue;
            IMetaTileEntity target = controllerAt(world, record);
            if (!(target instanceof ProxyLocatorHost host) || host == owner || !record.matches(target)) continue;
            host.getLocatorPool().directSources
                .forEach((map, source) -> { if (resolved.putIfAbsent(map, source) == null) via.put(map, record); });
        }
        List<RecipeMap<?>> maps = new ArrayList<>(resolved.keySet());
        if (sources.equals(resolved) && intermediates.equals(via) && availableMaps.equals(maps)) return false;
        sources = resolved;
        intermediates = via;
        availableMaps = Collections.unmodifiableList(maps);
        return true;
    }

    public List<RecipeMap<?>> recipeMaps() {
        return availableMaps;
    }

    /** Display the same first locator source used for admission, without querying its world. */
    ItemStack sourceController(RecipeMap<?> map) {
        MachineLocatorRecord source = sources.get(map);
        return source == null ? null : source.controller();
    }

    /** Revalidate both recorded controllers and the intermediate's current direct locator before admission. */
    public CheckRecipeResult checkStart(MTEExtendedPowerMultiBlockBase<?> proxy, RecipeMap<?> map, GTRecipe recipe) {
        MachineLocatorRecord record = sources.get(map);
        if (record == null) return failure("missing_locator");
        MachineLocatorRecord via = intermediates.get(map);
        if (GTNHAdjust.debug) GTNHAdjust.LOG.info(
            "[proxy-locator] checking map={} indirect={} source={} dim={} pos=({}, {}, {})",
            map.unlocalizedName,
            via != null,
            record.controller(),
            record.dimension,
            record.x,
            record.y,
            record.z);
        MTEMultiBlockBase source;
        MachineLocatorRecord step = via == null ? record : via;
        while (true) {
            World world = loadedWorld(step);
            if (world == null) return failure("unloaded");
            IMetaTileEntity controller = controllerAt(world, step);
            if (controller == null) return failure("missing_controller");
            if (!(controller instanceof MTEMultiBlockBase machine) || !step.matches(controller))
                return failure("wrong_controller");
            if (!machine.mMachine) return failure("unformed");
            if (step == record) {
                source = machine;
                break;
            }
            if (!(machine instanceof ProxyLocatorHost host) || machine == proxy) return failure("wrong_controller");
            MachineLocatorRecord current = host.getLocatorPool().directSources.get(map);
            if (current == null || !current.sameRecord(record)) return failure("missing_locator");
            step = record;
        }
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

    private static World loadedWorld(MachineLocatorRecord record) {
        World world = DimensionManager.getWorld(record.dimension);
        return world != null && world.getChunkProvider()
            .chunkExists(record.x >> 4, record.z >> 4) ? world : null;
    }

    private static IMetaTileEntity controllerAt(World world, MachineLocatorRecord record) {
        TileEntity tile = world.getTileEntity(record.x, record.y, record.z);
        return tile instanceof IGregTechTileEntity gt ? gt.getMetaTileEntity() : null;
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
