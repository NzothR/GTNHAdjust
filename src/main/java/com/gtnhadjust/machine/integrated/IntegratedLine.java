package com.gtnhadjust.machine.integrated;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.gtnewhorizon.structurelib.structure.StructureUtility;

import bartworks.common.loaders.ItemRegistry;
import gregtech.api.casing.Casings;
import gregtech.api.enums.HatchElement;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IHatchElement;
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
import gregtech.api.util.HatchElementBuilder;
import gregtech.api.util.IGTHatchAdder;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

public final class IntegratedLine extends MTEExtendedPowerMultiBlockBase<IntegratedLine>
    implements ISurvivalConstructable {

    private static final IIconContainer FACE = Textures.BlockIcons.custom("gtnhadjust", "iconsets/GODFORGE_CONTROLLER");
    private static final String[] PIECES = { "center", "up", "down", "top", "bottom" };
    private static final int[][] OFFSETS = { { 26, 5, 1 }, { 26, 27, 0 }, { 26, -6, 0 }, { 37, 46, 11 },
        { 37, -28, 11 } };
    private final List<IntegratedModule> modules = new ArrayList<>();
    private IStructureDefinition<IntegratedLine> structure;
    private boolean voltageEnabled;

    public IntegratedLine(int id, String name, String display) {
        super(id, name, display);
    }

    private IntegratedLine(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new IntegratedLine(mName);
    }

    @Override
    public boolean getDefaultHasMaintenanceChecks() {
        return false;
    }

    @Override
    public IStructureDefinition<IntegratedLine> getStructureDefinition() {
        if (structure == null) {
            StructureDefinition.Builder<IntegratedLine> b = StructureDefinition.<IntegratedLine>builder();
            for (String piece : PIECES) b.addShape(piece, StructureUtility.transpose(IntegratedShapes.load(piece)));
            structure = b.addElement('A', Casings.UHVMachineCasing.asElement())
                .addElement('B', Casings.ZPMMachineCasing.asElement())
                .addElement('C', Casings.UEVMachineCasing.asElement())
                .addElement('D', Casings.UHVSolenoidSuperconductorCoil.asElement())
                .addElement('E', Casings.UVMachineCasing.asElement())
                .addElement('F', Casings.LuVMachineCasing.asElement())
                .addElement('G', Casings.ZPMMachineCasing.asElement())
                .addElement('H', Casings.UEVMachineCasing.asElement())
                .addElement('I', Casings.UEVSolenoidSuperconductorCoil.asElement())
                .addElement('J', Casings.UVSolenoidSuperconductorCoil.asElement())
                .addElement('K', GTStructureUtility.ofFrame(Materials.NaquadahAlloy))
                .addElement('L', StructureUtility.ofBlock(ItemRegistry.bw_realglas2, 1))
                .addElement('M', StructureUtility.ofBlock(ItemRegistry.bw_realglas2, 1))
                .addElement(
                    'N',
                    GTStructureUtility.buildHatchAdder(IntegratedLine.class)
                        .anyOf(
                            HatchElement.InputBus,
                            HatchElement.OutputBus,
                            HatchElement.InputHatch,
                            HatchElement.OutputHatch)
                        .casingIndex(Casings.ZPMMachineCasing.getTextureId())
                        .hint(1)
                        .buildAndChain(Casings.ZPMMachineCasing.asElement()))
                .addElement('O', Casings.LuVMachineCasing.asElement())
                .addElement('P', Casings.LuVMachineCasing.asElement())
                .addElement('Q', Casings.UEVMachineCasing.asElement())
                .addElement('R', Casings.UVMachineCasing.asElement())
                .addElement('S', Casings.UHVSolenoidSuperconductorCoil.asElement())
                .addElement('T', Casings.LuVMachineCasing.asElement())
                .addElement('U', Casings.UVMachineCasing.asElement())
                .addElement('V', Casings.UIVMachineCasing.asElement())
                .addElement('W', Casings.UHVMachineCasing.asElement())
                .addElement('X', Casings.ZPMMachineCasing.asElement())
                .addElement(
                    'Y',
                    HatchElementBuilder.<IntegratedLine>builder()
                        .anyOf(ModuleElement.Module)
                        .casingIndex(Casings.UHVMachineCasing.getTextureId())
                        .hint(1)
                        .buildAndChain(Casings.UHVMachineCasing.asElement()))
                .addElement('Z', Casings.UEVMachineCasing.asElement())
                .addElement('a', StructureUtility.isAir())
                .build();
        }
        return structure;
    }

    public boolean addModule(IGregTechTileEntity tile, int casing) {
        if (tile != null && tile.getMetaTileEntity() instanceof IntegratedModule module && !modules.contains(module))
            return modules.add(module);
        return false;
    }

    private void disconnect() {
        for (IntegratedModule module : modules) module.setConnected(false);
        modules.clear();
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        disconnect();
        for (int i = 0; i < PIECES.length; i++)
            if (!checkPiece(PIECES[i], OFFSETS[i][0], OFFSETS[i][1], OFFSETS[i][2], errors)) {
                disconnect();
                return;
            }
        for (IntegratedModule module : modules) module.setConnected(true);
    }

    @Override
    public void onRemoval() {
        disconnect();
        super.onRemoval();
    }

    @Override
    public void construct(ItemStack stack, boolean hints) {
        for (int i = 0; i < PIECES.length; i++)
            buildPiece(PIECES[i], stack, hints, OFFSETS[i][0], OFFSETS[i][1], OFFSETS[i][2]);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int budget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        for (int i = 0; i < PIECES.length; i++) {
            int n = survivalBuildPiece(
                PIECES[i],
                stack,
                OFFSETS[i][0],
                OFFSETS[i][1],
                OFFSETS[i][2],
                budget,
                env,
                false,
                true);
            if (n >= 0) return n;
        }
        return -1;
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
        ITexture casing = Casings.ZPMMachineCasing.getCasingTexture();
        return side == facing ? new ITexture[] { casing, TextureFactory.builder()
            .addIcon(FACE)
            .extFacing()
            .build() } : new ITexture[] { casing };
    }

    @Override
    public MultiblockTooltipBuilder createTooltip() {
        return new MultiblockTooltipBuilder().addMachineType("联合集成产线")
            .addInfo("结构测试版：无配方功能")
            .addInfo("22 个联合模块接入点；输入及输出舱室可选")
            .addInfo("不使用能源仓和维护仓")
            .beginStructureBlock(75, 96, 75, true)
            .addController("正面")
            .toolTipFinisher();
    }

    @Override
    public String[] getInfoData() {
        return new String[] { "联合集成产线", "已检测模块：" + modules.size() + "/22", "结构测试版，无加工功能" };
    }

    @Override
    protected MTEMultiBlockBaseGui<?> getGui() {
        return new IntegratedLineGui(this);
    }

    public int connectedModules() {
        return mMachine ? modules.size() : 0;
    }

    public boolean isVoltageEnabled() {
        return voltageEnabled;
    }

    public void setVoltageEnabled(boolean enabled) {
        voltageEnabled = enabled;
    }

    public void refreshModules() {
        checkStructure(true, getBaseMetaTileEntity());
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        tag.setBoolean("integratedVoltageEnabled", voltageEnabled);
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        voltageEnabled = tag.getBoolean("integratedVoltageEnabled");
    }

    enum ModuleElement implements IHatchElement<IntegratedLine> {

        Module;

        @Override
        public List<? extends Class<? extends IMetaTileEntity>> mteClasses() {
            return Collections.unmodifiableList(Arrays.asList(IntegratedModule.class));
        }

        @Override
        public IGTHatchAdder<? super IntegratedLine> adder() {
            return IntegratedLine::addModule;
        }

        @Override
        public long count(IntegratedLine machine) {
            return machine.modules.size();
        }
    }
}
