package com.gtnhadjust.machine.integrated;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.science.gtnl.utils.StructureUtils;

import bartworks.common.loaders.ItemRegistry;
import gregtech.api.casing.Casings;
import gregtech.api.enums.HatchElement;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.MultiblockTooltipBuilder;

public final class IntegratedModule extends MTEExtendedPowerMultiBlockBase<IntegratedModule>
    implements ISurvivalConstructable {

    private static final IIconContainer FACE = Textures.BlockIcons.custom("gtnhadjust", "iconsets/GODFORGE_CONTROLLER");
    private IStructureDefinition<IntegratedModule> structure;
    private boolean connected;

    public IntegratedModule(int id, String name, String display) {
        super(id, name, display);
    }

    private IntegratedModule(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new IntegratedModule(mName);
    }

    @Override
    public boolean getDefaultHasMaintenanceChecks() {
        return false;
    }

    @Override
    public IStructureDefinition<IntegratedModule> getStructureDefinition() {
        if (structure == null) structure = StructureDefinition.<IntegratedModule>builder()
            .addShape(
                "main",
                StructureUtility
                    .transpose(StructureUtils.readStructureFromFile("gtnhadjust:multiblock/integrated_line/module")))
            .addElement('A', StructureUtility.ofBlock(ItemRegistry.bw_realglas2, 1))
            .addElement('B', Casings.ZPMMachineCasing.asElement())
            .addElement('C', Casings.UEVMachineCasing.asElement())
            .addElement('D', Casings.UEVMachineCasing.asElement())
            .addElement('E', Casings.UVMachineCasing.asElement())
            .addElement('F', Casings.UVSolenoidSuperconductorCoil.asElement())
            .addElement('G', Casings.UHVSolenoidSuperconductorCoil.asElement())
            .addElement('H', GTStructureUtility.ofFrame(Materials.NaquadahAlloy))
            .addElement('I', Casings.UHVMachineCasing.asElement())
            .addElement('J', Casings.ZPMMachineCasing.asElement())
            .addElement(
                'K',
                StructureUtility.ofChain(
                    StructureUtility.isAir(),
                    GTStructureUtility.buildHatchAdder(IntegratedModule.class)
                        .anyOf(
                            HatchElement.InputBus,
                            HatchElement.OutputBus,
                            HatchElement.InputHatch,
                            HatchElement.OutputHatch)
                        .casingIndex(Casings.UHVMachineCasing.getTextureId())
                        .hint(1)
                        .buildAndChain(Casings.UHVMachineCasing.asElement())))
            .build();
        return structure;
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        checkPiece("main", 4, 3, 0, errors);
    }

    @Override
    public void construct(ItemStack stack, boolean hints) {
        buildPiece("main", stack, hints, 4, 3, 0);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int budget, ISurvivalBuildEnvironment env) {
        return mMachine ? -1 : survivalBuildPiece("main", stack, 4, 3, 0, budget, env, false, true);
    }

    @Override
    public CheckRecipeResult checkProcessing() {
        return CheckRecipeResultRegistry.NO_RECIPE;
    }

    @Override
    public int getPollutionPerSecond(ItemStack stack) {
        return 0;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        ITexture casing = Casings.UHVMachineCasing.getCasingTexture();
        return side == facing ? new ITexture[] { casing, TextureFactory.builder()
            .addIcon(FACE)
            .extFacing()
            .build() } : new ITexture[] { casing };
    }

    @Override
    public MultiblockTooltipBuilder createTooltip() {
        return new MultiblockTooltipBuilder().addMachineType("联合模块")
            .addInfo("结构测试版：无配方功能")
            .addInfo("输入及输出舱室可选；不使用能源仓和维护仓")
            .beginStructureBlock(9, 7, 7, true)
            .addController("正面")
            .toolTipFinisher();
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    @Override
    public String[] getInfoData() {
        return new String[] { "联合模块", connected ? "已连接主机" : "未连接主机", "结构测试版，无加工功能" };
    }
}
