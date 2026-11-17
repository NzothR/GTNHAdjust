package com.gtnhadjust.compat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import com.gtnhadjust.GTNHAdjust;
import com.gtnhadjust.machine.threadedproxy.ProxyPatternRecipe;
import com.xyp.gtneverreset.common.me.CircuitPatternCodec;
import com.xyp.gtneverreset.common.me.MoldDataManager;
import com.xyp.gtneverreset.common.me.PatternRecipeMap;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;

/** Additional NC metadata is additive; legacy GTNR circuit/mold tags retain their original representation. */
public final class GtnrToolPatterns {

    public static final String TOOLS = "gtnhadjustPatternNonConsumed";
    private static final Map<ICraftingPatternDetails, GTRecipe> RECIPES = new WeakHashMap<>();

    public static boolean hasRecord(ItemStack pattern) {
        return GTNHAdjust.gtnrVirtualTools && pattern != null
            && pattern.hasTagCompound()
            && pattern.getTagCompound()
                .hasKey(TOOLS, 9);
    }

    public static ItemStack[] recorded(ItemStack pattern) {
        if (!hasRecord(pattern)) return new ItemStack[0];
        NBTTagList list = pattern.getTagCompound()
            .getTagList(TOOLS, 10);
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < list.tagCount(); i++) {
            ItemStack item = ItemStack.loadItemStackFromNBT(list.getCompoundTagAt(i));
            if (item != null) items.add(GTUtility.copyAmount(0, item));
        }
        return items.toArray(new ItemStack[0]);
    }

    public static boolean builtIn(ItemStack item) {
        if (GTUtility.isAnyIntegratedCircuit(item)) return true;
        for (ItemStack candidate : MoldDataManager.getMolds())
            if (GTUtility.areStacksEqual(candidate, item, false)) return true;
        return false;
    }

    /** Recipe matching uses the encoded map, all advertised products and the original complete ingredient list. */
    public static synchronized GTRecipe recipe(ICraftingPatternDetails details) {
        if (details == null || details.isCraftable() || details.isInputOnly()) return null;
        if (RECIPES.containsKey(details)) return RECIPES.get(details);
        String name = PatternRecipeMap.name(details.getPattern());
        RecipeMap<?> selected = PatternRecipeMap.resolve(name);
        GTRecipe found = null;
        if (selected != null) found = ProxyPatternRecipe.identify(selected, details);
        else if (name == null) {
            for (RecipeMap<?> map : RecipeMap.ALL_RECIPE_MAPS.values()) {
                if (map.getBackend()
                    .doesOverwriteFindRecipe()) continue;
                found = ProxyPatternRecipe.identify(map, details);
                if (found != null) break;
            }
        }
        if (GregTechAPI.sFullLoadFinished) RECIPES.put(details, found);
        return found;
    }

    /**
     * Retain the encoded NC choice (including NBT/ore alternatives), using recipe representatives for legacy omissions.
     */
    public static ItemStack[] tools(GTRecipe recipe, ICraftingPatternDetails details) {
        List<ItemStack> supplied = new ArrayList<>();
        for (IAEStack<?> stack : details.getAEInputs())
            if (stack instanceof IAEItemStack item) supplied.add(item.getItemStack());
        java.util.Collections.addAll(supplied, recorded(details.getPattern()));
        java.util.Collections.addAll(supplied, CircuitPatternCodec.storedMolds(details.getPattern()));
        int circuit = CircuitPatternCodec.storedCircuit(details.getPattern());
        if (circuit >= 0) supplied.add(GTUtility.getIntegratedCircuit(circuit));
        List<ItemStack> tools = new ArrayList<>();
        for (GTRecipe.RecipeItemInput input : recipe.getCachedCombinedItemInputs()) {
            if (input.inputAmount != 0) continue;
            ItemStack selected = input.unifiedStack;
            for (ItemStack item : supplied) {
                if (input.matchesRecipe(gregtech.api.util.GTOreDictUnificator.getAssociation(item), item)) {
                    selected = item;
                    break;
                }
            }
            tools.add(GTUtility.copyAmount(0, selected));
        }
        return tools.toArray(new ItemStack[0]);
    }

    /** Only catalog tools are free virtual supplies; external NCs are separately gated by physical/PH availability. */
    public static ItemStack[] catalogTools(GTRecipe recipe, ICraftingPatternDetails details) {
        return Arrays.stream(tools(recipe, details))
            .filter(item -> builtIn(item) && !GTUtility.isAnyIntegratedCircuit(item))
            .toArray(ItemStack[]::new);
    }

    /** Recorded patterns require both complete recipe identity and live evidence for every NC item. */
    public static boolean supplied(GTRecipe recipe, ICraftingPatternDetails details, ItemStack[] inputs) {
        if (!hasRecord(details.getPattern())) return true;
        if (!ProxyPatternRecipe.matchesRecordedPattern(recipe, details)) return false;
        for (ItemStack tool : recorded(details.getPattern())) if (inputs == null || Arrays.stream(inputs)
            .noneMatch(item -> GTUtility.areStacksEqual(tool, item, false))) return false;
        return true;
    }

    /** Strip NCs from AE demands on a copy and retain their full identities for runtime checks and editing. */
    public static ICraftingPatternDetails encode(ItemStack original, World world, ICraftingPatternDetails details,
        ICraftingPatternDetails nativeResult) {
        GTRecipe recipe = recipe(details);
        if (recipe == null || nativeResult == null) return nativeResult;
        ItemStack runtime = nativeResult.getPattern()
            .copy();
        NBTTagCompound tag = runtime.getTagCompound();
        ItemStack[] tools = tools(recipe, details);
        NBTTagList consumed = new NBTTagList(), encoded = new NBTTagList();
        boolean fluids = Arrays.stream(details.getAEInputs())
            .anyMatch(input -> input instanceof IAEFluidStack)
            || Arrays.stream(details.getAEOutputs())
                .anyMatch(output -> output instanceof IAEFluidStack);
        // Rebuild from the complete bill, so a catalog item actually consumed by this recipe stays a real demand.
        for (IAEStack<?> stack : details.getAEInputs()) {
            if (stack instanceof IAEItemStack item && Arrays.stream(tools)
                .anyMatch(tool -> GTUtility.areStacksEqual(tool, item.getItemStack(), false))) continue;
            NBTTagCompound entry = new NBTTagCompound();
            if (fluids) stack.writeToNBTGeneric(entry);
            else stack.writeToNBT(entry);
            consumed.appendTag(entry);
        }
        if (Arrays.stream(tools)
            .noneMatch(GTUtility::isAnyIntegratedCircuit)) tag.removeTag(CircuitPatternCodec.CIRCUIT);
        // Native AE processing patterns require at least one real demand; retain the legacy form for empty demands.
        if (consumed.tagCount() == 0) return nativeResult;
        for (ItemStack tool : tools) encoded.appendTag(tool.writeToNBT(new NBTTagCompound()));
        tag.setTag("in", consumed);
        tag.setTag(TOOLS, encoded);
        if (GTNHAdjust.debug) GTNHAdjust.LOG
            .info("[gtnr-virtual-tools] pattern map={} NC={}", PatternRecipeMap.name(runtime), Arrays.toString(tools));
        return CircuitPatternCodec.decode(runtime, world);
    }

    /** Re-opening either format restores all tools as editable inputs without mutating the stored pattern. */
    public static ItemStack editable(ItemStack original, ItemStack nativeResult) {
        if (!hasRecord(original)) return nativeResult;
        ItemStack edited = nativeResult.copy();
        NBTTagCompound tag = edited.getTagCompound();
        NBTTagList inputs = tag.getTagList("in", 10);
        for (ItemStack tool : recorded(original)) {
            boolean exists = false;
            for (int i = 0; i < inputs.tagCount(); i++) {
                IAEStack<?> entry = Platform.readStackNBT(inputs.getCompoundTagAt(i), true);
                if (entry instanceof IAEItemStack item && GTUtility.areStacksEqual(tool, item.getItemStack(), false))
                    exists = true;
            }
            if (!exists) {
                NBTTagCompound entry = new NBTTagCompound();
                AEItemStack.create(GTUtility.copyAmount(1, tool))
                    .writeToNBTGeneric(entry);
                inputs.appendTag(entry);
            }
        }
        tag.removeTag(TOOLS);
        return edited;
    }

    @SubscribeEvent
    public void tooltip(ItemTooltipEvent event) {
        for (ItemStack tool : recorded(event.itemStack)) if (!builtIn(tool)) event.toolTip
            .add(StatCollector.translateToLocalFormatted("gtnhadjust.pattern.external_tool", tool.getDisplayName()));
    }
}
