package com.gtnhadjust.machine.collector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.science.gtnl.utils.enums.ModsItemlist;

import appeng.api.AEApi;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEFluidStackType;
import appeng.util.item.AEItemStack;
import appeng.util.item.AEItemStackType;
import cpw.mods.fml.common.registry.GameRegistry;
import fox.spiteful.avaritia.crafting.ExtremeCraftingManager;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;

/** Resolves one compression recipe from the same finite recipe map used by decompression. */
public final class MatterCompression {

    static final long POWER = 27_300_000_000_000_000L;
    static final int TICKS = 128;
    private static Item blank;
    private static MatterCompressionCell<IAEItemStack> items;
    private static MatterCompressionCell<IAEFluidStack> fluids;

    private MatterCompression() {}

    public static void registerItems() {
        blank = new Item().setUnlocalizedName("gtnhadjust.blank_compression_cell")
            .setTextureName("appliedenergistics2:ItemMaterial.EmptyAdvancedStorageCell")
            .setCreativeTab(CreativeTabs.tabMisc);
        items = new MatterCompressionCell<>(
            "item_compression_cell",
            "appliedenergistics2:ItemCreativeStorageCell",
            AEItemStackType.ITEM_STACK_TYPE);
        fluids = new MatterCompressionCell<>(
            "fluid_compression_cell",
            "ae2fc:creative_fluid_storage",
            AEFluidStackType.FLUID_STACK_TYPE);
        GameRegistry.registerItem(blank, "blank_compression_cell");
        GameRegistry.registerItem(items, "item_compression_cell");
        GameRegistry.registerItem(fluids, "fluid_compression_cell");
    }

    /**
     * Use the GTNL infinite stone cell's ultimate crafting layout with decompressor controllers in the four K slots.
     */
    public static void registerCrafting() {
        ExtremeCraftingManager.getInstance()
            .addExtremeShapedOreRecipe(
                new ItemStack(blank),
                "--ABCBA--",
                "-DEFGFED-",
                "AHFFIFFHA",
                "JFFFKFFFJ",
                "CGIKLKIGC",
                "JFFFKFFFJ",
                "AHFFIFFHA",
                "-DEFGFED-",
                "--ABCBA--",
                'A',
                GTOreDictUnificator.get(OrePrefixes.block, Materials.CosmicNeutronium, 1),
                'B',
                GTOreDictUnificator.get(OrePrefixes.plateSuperdense, Materials.Naquadah, 1),
                'C',
                GTOreDictUnificator.get(OrePrefixes.plateSuperdense, Materials.Carbon, 1),
                'D',
                GTOreDictUnificator.get(OrePrefixes.plateSuperdense, Materials.Vanadium, 1),
                'E',
                GTOreDictUnificator.get(OrePrefixes.plateSuperdense, Materials.Bedrockium, 1),
                'F',
                ModsItemlist.ExtraUtilitiesCompressedCobbleSeven.get(1),
                'G',
                ItemList.Field_Generator_UV.get(1),
                'H',
                GTOreDictUnificator.get(OrePrefixes.plateSuperdense, Materials.Tungsten, 1),
                'I',
                ModsItemlist.AvaritiaInfinityCatalyst.get(1),
                'J',
                GTOreDictUnificator.get(OrePrefixes.plateSuperdense, Materials.NiobiumTitanium, 1),
                'K',
                SuperFragmentRegistration.controller(),
                'L',
                AEApi.instance()
                    .definitions()
                    .materials()
                    .emptyAdvancedStorageCell()
                    .maybeStack(1)
                    .get());
    }

    static final class Update {

        final ItemStack input;
        final ItemStack output;

        Update(ItemStack input, ItemStack output) {
            this.input = input;
            this.output = output;
        }
    }

    /** Plan without changing any inputs; physical/ME input accounting happens during GT recipe processing. */
    static List<Update> plan(List<ItemStack> inputs) {
        ItemStack marker = null;
        ItemStack[] circuits = new ItemStack[5];
        List<ItemStack> blanks = new ArrayList<>();
        ItemStack itemCell = null;
        ItemStack fluidCell = null;
        for (ItemStack stack : inputs) {
            if (stack.stackSize <= 0) continue;
            Item item = stack.getItem();
            if (item == blank) {
                blanks.add(stack);
            } else if (item == items) {
                if (itemCell == null) itemCell = stack;
            } else if (item == fluids) {
                if (fluidCell == null) fluidCell = stack;
            } else if (GTUtility.isAnyIntegratedCircuit(stack)) {
                int number = stack.getItemDamage();
                if (number >= 20 && number <= 24) circuits[number - 20] = stack;
            } else if (marker == null) {
                GameRegistry.UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(item);
                if (id != null && CollectorMarkerPolicy.isMarker(id.modId, id.name)) marker = stack;
            }
        }
        if (marker == null || (blanks.isEmpty() && itemCell == null && fluidCell == null))
            return Collections.emptyList();

        IItemList<IAEItemStack> itemResources = AEItemStackType.ITEM_STACK_TYPE.createPrimitiveList();
        IItemList<IAEFluidStack> fluidResources = AEFluidStackType.FLUID_STACK_TYPE.createPrimitiveList();
        for (ItemStack circuit : circuits) {
            if (circuit == null) continue;
            GTRecipe recipe = SuperFragmentRecipes.MAP.findRecipeQuery()
                .items(marker, circuit)
                .notUnificated(true)
                .find();
            if (recipe == null) continue;
            for (int i = 0; i < recipe.mOutputs.length; i++) {
                ItemStack output = recipe.mOutputs[i];
                if (output != null && output.stackSize > 0 && recipe.getOutputChance(i) > 0)
                    itemResources.add(AEItemStack.create(output));
            }
            for (FluidStack output : recipe.mFluidOutputs) {
                if (output != null && output.amount > 0) fluidResources.add(AEFluidStack.create(output));
            }
        }

        List<Update> updates = new ArrayList<>(2);
        ItemStack firstBlank = blanks.isEmpty() ? null : blanks.get(0);
        ItemStack itemInput = itemCell == null ? firstBlank : itemCell;
        if (itemInput != null && !itemResources.isEmpty()) {
            ItemStack output = items.append(itemCell, itemResources);
            if (output != null) updates.add(new Update(itemInput, output));
        }
        ItemStack fluidInput = fluidCell == null ? firstBlank : fluidCell;
        if (fluidCell == null && !updates.isEmpty() && itemInput == firstBlank && firstBlank.stackSize == 1)
            fluidInput = blanks.size() > 1 ? blanks.get(1) : null;
        if (fluidInput != null && !fluidResources.isEmpty()) {
            ItemStack output = fluids.append(fluidCell, fluidResources);
            if (output != null) updates.add(new Update(fluidInput, output));
        }
        return updates;
    }
}
