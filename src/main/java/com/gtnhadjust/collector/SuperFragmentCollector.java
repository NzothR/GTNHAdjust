package com.gtnhadjust.collector;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.HatchElement.OutputHatch;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtneverreset.utils.enums.BlockIcons;

import gregtech.api.GregTechAPI;
import gregtech.api.casing.Casings;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;

public final class SuperFragmentCollector extends MTEExtendedPowerMultiBlockBase<SuperFragmentCollector>
    implements ISurvivalConstructable {

    private static final String PIECE = "main";
    private static final short[] GOLD = { 255, 205, 75, 255 };
    private IStructureDefinition<SuperFragmentCollector> structure;

    public SuperFragmentCollector(int id, String name, String localizedName) {
        super(id, name, localizedName);
    }

    private SuperFragmentCollector(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new SuperFragmentCollector(mName);
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return SuperFragmentRecipes.MAP;
    }

    @Override
    public boolean supportsBatchMode() {
        return true;
    }

    @Override
    public boolean supportsVoidProtection() {
        return true;
    }

    public int maxParallel() {
        int tier = Math.max(0, GTUtility.getTier(getMaxInputVoltage()));
        return (int) Math.min(Integer.MAX_VALUE, 4L << Math.min(tier, 29));
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new ProcessingLogic().setMaxParallelSupplier(this::maxParallel)
            .enablePerfectOverclock();
    }

    @Override
    public IStructureDefinition<SuperFragmentCollector> getStructureDefinition() {
        if (structure == null) structure = StructureDefinition.<SuperFragmentCollector>builder()
            .addShape(
                PIECE,
                transpose(new String[][] { { "CCC", "CCC", "CCC" }, { "C~C", "C-C", "CCC" }, { "CCC", "CCC", "CCC" } }))
            .addElement(
                'C',
                buildHatchAdder(SuperFragmentCollector.class)
                    .atLeast(InputBus, OutputBus, InputHatch, OutputHatch, Energy.or(ExoticEnergy), Maintenance)
                    .casingIndex(Casings.CleanStainlessSteelMachineCasing.getTextureId())
                    .hint(1)
                    .buildAndChain(ofBlock(GregTechAPI.sBlockCasings4, 1)))
            .build();
        return structure;
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece(PIECE, stack, hintsOnly, 1, 1, 0);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int budget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        return survivalBuildPiece(PIECE, stack, 1, 1, 0, budget, env, false, true);
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        if (checkPiece(PIECE, 1, 1, 0, errors)) collectTooltipStructureRequirements(errors);
    }

    public void collectTooltipStructureRequirements(List<StructureError> errors) {
        checkHatchMin(errors, InputBus, 1);
        checkHatchMin(errors, OutputBus, 1);
        checkHatchMin(errors, OutputHatch, 1);
        if (mEnergyHatches.isEmpty() && mExoticEnergyHatches.isEmpty()) checkHatchMin(errors, Energy, 1);
        if (shouldCheckMaintenance()) checkHatchMin(errors, Maintenance, 1);
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        IIconContainer icon = side == facing
            ? (active ? BlockIcons.FragmentCollectorFrontActive : BlockIcons.FragmentCollectorFront)
            : side == ForgeDirection.UP ? BlockIcons.FragmentCollectorTop
                : side == ForgeDirection.DOWN ? BlockIcons.FragmentCollectorBottom
                    : side == facing.getOpposite() ? BlockIcons.FragmentCollectorBack
                        : BlockIcons.FragmentCollectorSide;
        return new ITexture[] {
            TextureFactory.of(
                Textures.BlockIcons.MACHINECASINGS_BOTTOM[0],
                Textures.BlockIcons.MACHINECASINGS_TOP[0],
                Textures.BlockIcons.MACHINECASINGS_SIDE[0],
                GOLD),
            TextureFactory.builder()
                .addIcon(icon)
                .setRGBA(GOLD)
                .build() };
    }

    @Override
    public MultiblockTooltipBuilder createTooltip() {
        return new MultiblockTooltipBuilder().addMachineType("超级碎片世界采集")
            .addInfo("合并同一维度标记的采集配方；电路 1")
            .addInfo("流体采集按钻头种类区分；支持无损超频与批处理")
            .addInfo("ULV 4 并行，每升一级电压并行翻倍")
            .beginStructureBlock(3, 3, 3, true)
            .addController("侧面正中")
            .addCasing("其余外壳", "洁净不锈钢机械方块", false)
            .addInputBus("1+", "任意外壳", 1)
            .addOutputBus("1+", "任意外壳", 1)
            .addInputHatch("0+", "任意外壳；暮色森林特殊配方需要", 1)
            .addOutputHatch("1+", "任意外壳", 1)
            .addEnergyHatch("1+", "任意外壳", 1)
            .addMaintenanceHatch("1+", "任意外壳", 1)
            .addAir("中心空心")
            .toolTipFinisher();
    }
}
