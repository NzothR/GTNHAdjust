package com.gtnhadjust.machine.debug;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;

import gregtech.api.GregTechAPI;
import gregtech.api.casing.Casings;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.MultiblockTooltipBuilder;

/** Read-only recipe-map discovery controller; its decorative structure never enables processing. */
public final class RecipePoolDebugger extends MTEExtendedPowerMultiBlockBase<RecipePoolDebugger>
    implements ISurvivalConstructable {

    private static ItemStack controller;
    private static final IStructureDefinition<RecipePoolDebugger> STRUCTURE = StructureDefinition
        .<RecipePoolDebugger>builder()
        .addShape("main", new String[][] { { "CCC", "C~C", "CCC" }, { "CCC", "C-C", "CCC" }, { "CCC", "CCC", "CCC" } })
        .addElement('C', ofBlock(GregTechAPI.sBlockCasings4, 1))
        .build();

    public RecipePoolDebugger(int id) {
        super(id, "gtnhadjust.recipe_pool_debugger", "Recipe Pool Debugger");
    }

    private RecipePoolDebugger(String name) {
        super(name);
    }

    /** Register only the creative/NEI debug item; no survival recipe is added. */
    public static void register(int id) {
        if (GregTechAPI.METATILEENTITIES[id] != null)
            throw new IllegalStateException("Recipe pool debugger MTE ID is already in use: " + id);
        controller = new RecipePoolDebugger(id).getStackForm(1);
    }

    public static ItemStack controller() {
        return controller;
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new RecipePoolDebugger(mName);
    }

    /** Enumerate the live registry instead of a fixed machine or recipe-map whitelist. */
    @Override
    public Collection<RecipeMap<?>> getAvailableRecipeMaps() {
        List<RecipeMap<?>> maps = new ArrayList<>(RecipeMap.ALL_RECIPE_MAPS.values());
        maps.sort(java.util.Comparator.comparing(map -> map.unlocalizedName));
        return maps;
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return null;
    }

    /** Keep real machine controllers ahead of this universal debug catalyst in NEI. */
    @Override
    public int getRecipeCatalystPriority() {
        return -1000;
    }

    @Override
    public CheckRecipeResult checkProcessing() {
        return CheckRecipeResultRegistry.NO_RECIPE;
    }

    @Override
    public boolean getDefaultHasMaintenanceChecks() {
        return false;
    }

    @Override
    public IStructureDefinition<RecipePoolDebugger> getStructureDefinition() {
        return STRUCTURE;
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece("main", stack, hintsOnly, 1, 1, 0);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int budget, ISurvivalBuildEnvironment env) {
        return mMachine ? -1 : survivalBuildPiece("main", stack, 1, 1, 0, budget, env, false, true);
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        checkPiece("main", 1, 1, 0, errors);
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        return new ITexture[] { Casings.CleanStainlessSteelMachineCasing.getCasingTexture() };
    }

    @Override
    public MultiblockTooltipBuilder createTooltip() {
        return new MultiblockTooltipBuilder().addMachineType(StatCollector.translateToLocal("gtnhadjust.debugger.type"))
            .addInfo(StatCollector.translateToLocal("gtnhadjust.debugger.info"))
            .addInfo(StatCollector.translateToLocal("gtnhadjust.debugger.read_only"))
            .beginStructureBlock(3, 3, 3, false)
            .addController("正面中央")
            .addCasing("25", Casings.CleanStainlessSteelMachineCasing.getLocalizedName(), false)
            .addAir("中心")
            .toolTipFinisher();
    }
}
