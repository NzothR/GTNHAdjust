package com.gtnhadjust.quantum;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static gregtech.api.enums.HatchElement.*;
import static gregtech.api.util.GTStructureUtility.*;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;

import gregtech.api.GregTechAPI;
import gregtech.api.casing.Casings;
import gregtech.api.enums.HeatingCoilLevel;
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
import gtPlusPlus.xmod.gregtech.common.blocks.textures.TexturesGtBlock;

public final class SmallQuantumMachine extends MTEExtendedPowerMultiBlockBase<SmallQuantumMachine>
    implements ISurvivalConstructable {

    private static final String PIECE = "main";
    private static final String[][] SHAPE = new String[][] { // A - 142, B - 234, C - 177, D - 96, E - 224, H - 36, M -
                                                             // 21
        // spotless:off
        { "               ", "               ", "               ", "               ", "               ",
            "               ", "               ", "               ", "               ", "               ",
            "               ", "               ", "               ", "      BAB      ", "   BBBBABBBB   ",
            "   BAAAAAAAB   ", "   BABBABBAB   ", "   BA     AB   ", "    A     A    ", "    A     A    ",
            "    A     A    " },
        { "               ", "               ", "               ", "               ", "               ",
            "               ", "               ", "               ", "               ", "               ",
            "               ", "               ", "      BAB      ", "   AAABBBAAA   ", "  BAAAAAAAAAB  ",
            "  B         B  ", "  A         A  ", "  A         A  ", "               ", "               ",
            "               " },
        { "               ", "               ", "               ", "               ", "               ",
            "               ", "               ", "               ", "               ", "               ",
            "               ", "      BAB      ", "    AA   AA    ", "  AA       AA  ", " BAA       AAB ",
            " B           B ", " A           A ", " A           A ", "               ", "               ",
            "               " },
        { "               ", "               ", "               ", "               ", "               ",
            "               ", "               ", "               ", "               ", "               ",
            "               ", "     BAAAB     ", "   AA     AA   ", " AA         AA ", "BAA         AAB",
            "B             B", "A             A", "A             A", "A             A", "A             A",
            "A             A" },
        { "      TTT      ", "      EEE      ", "      EEE      ", "      EEE      ", "      DDD      ",
            "      EEE      ", "      DDD      ", "      EEE      ", "      EEE      ", "      EEE      ",
            "      DDD      ", "    BAEEEAB    ", "  AA  EEE  AA  ", " A    EEE    A ", "BA    DDD    AB",
            "B     EEE     B", "B     DDD     B", "      EEE      ", "      EEE      ", "      EEE      ",
            "      H~H      " },
        { "     TTTTT     ", "     ECCCE     ", "     ECCCE     ", "     ECCCE     ", "     D   D     ",
            "     ECCCE     ", "     D   D     ", "     ECCCE     ", "     ECCCE     ", "     ECCCE     ",
            "     D   D     ", "   BAECCCEAB   ", "  A  ECCCE  A  ", " A   ECCCE   A ", "BA   D   D   AB",
            "B    ECCCE    B", "B    D   D    B", "B    ECCCE    B", "     ECCCE     ", "     ECCCE     ",
            "     HHHHH     " },
        { "    TTTTTTT    ", "    ECCCCCE    ", "    EC   CE    ", "    EC   CE    ", "    D     D    ",
            "    EC   CE    ", "    D     D    ", "    EC   CE    ", "    EC   CE    ", "    EC   CE    ",
            "    D     D    ", "  BAEC   CEAB  ", " B  EC   CE  B ", "BB  EC   CE  BB", "BA  D     D  AB",
            "A   EC   CE   A", "A   D     D   A", "A   EC   CE   A", "    EC   CE    ", "    EC   CE    ",
            "    HHHHHHH    " },
        { "    TTTTTTT    ", "    ECCCCCE    ", "    EC   CE    ", "    EC   CE    ", "    D     D    ",
            "    EC   CE    ", "    D     D    ", "    EC   CE    ", "    EC   CE    ", "    EC   CE    ",
            "    D     D    ", "  AAEC   CEAA  ", " A  EC   CE  A ", "AB  EC   CE  BA", "AA  D     D  AA",
            "A   EC   CE   A", "A   D     D   A", "    EC   CE    ", "    EC   CE    ", "    EC   CE    ",
            "    HHHHHHH    " },
        { "    TTTTTTT    ", "    ECCCCCE    ", "    EC   CE    ", "    EC   CE    ", "    D     D    ",
            "    EC   CE    ", "    D     D    ", "    EC   CE    ", "    EC   CE    ", "    EC   CE    ",
            "    D     D    ", "  BAEC   CEAB  ", " B  EC   CE  B ", "BB  EC   CE  BB", "BA  D     D  AB",
            "A   EC   CE   A", "A   D     D   A", "A   EC   CE   A", "    EC   CE    ", "    EC   CE    ",
            "    HHHHHHH    " },
        { "     TTTTT     ", "     ECCCE     ", "     ECCCE     ", "     ECCCE     ", "     D   D     ",
            "     ECCCE     ", "     D   D     ", "     ECCCE     ", "     ECCCE     ", "     ECCCE     ",
            "     D   D     ", "   BAECCCEAB   ", "  A  ECCCE  A  ", " A   ECCCE   A ", "BA   D   D   AB",
            "B    ECCCE    B", "B    D   D    B", "B    ECCCE    B", "     ECCCE     ", "     ECCCE     ",
            "     HHHHH     " },
        { "      TTT      ", "      EEE      ", "      EEE      ", "      EEE      ", "      DDD      ",
            "      EEE      ", "      DDD      ", "      EEE      ", "      EEE      ", "      EEE      ",
            "      DDD      ", "    BAEEEAB    ", "  AA  EEE  AA  ", " A    EEE    A ", "BA    DDD    AB",
            "B     EEE     B", "B     DDD     B", "      EEE      ", "      EEE      ", "      EEE      ",
            "      HHH      " },
        { "               ", "               ", "               ", "               ", "               ",
            "               ", "               ", "               ", "               ", "               ",
            "               ", "     BAAAB     ", "   AA     AA   ", " AA         AA ", "BAA         AAB",
            "B             B", "A             A", "A             A", "A             A", "A             A",
            "A             A" },
        { "               ", "               ", "               ", "               ", "               ",
            "               ", "               ", "               ", "               ", "               ",
            "               ", "      BAB      ", "    AA   AA    ", "  AA       AA  ", " BAA       AAB ",
            " B           B ", " A           A ", " A           A ", "               ", "               ",
            "               " },
        { "               ", "               ", "               ", "               ", "               ",
            "               ", "               ", "               ", "               ", "               ",
            "               ", "               ", "      BAB      ", "   AAABBBAAA   ", "  BAAAAAAAAAB  ",
            "  B         B  ", "  A         A  ", "  A         A  ", "               ", "               ",
            "               " },
        { "               ", "               ", "               ", "               ", "               ",
            "               ", "               ", "               ", "               ", "               ",
            "               ", "               ", "               ", "      BAB      ", "   BBBBABBBB   ",
            "   BBBAAABBB   ", "   ABBAAABBA   ", "   A BA AB A   ", "      A A      ", "      A A      ",
            "      A A      " }, };
    private IStructureDefinition<SmallQuantumMachine> structure;
    private HeatingCoilLevel coil = HeatingCoilLevel.None;
    private int glass = -1;

    public SmallQuantumMachine(int id, String name, String display) {
        super(id, name, display);
    }

    private SmallQuantumMachine(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new SmallQuantumMachine(mName);
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return SmallQuantumRecipes.MAP;
    }

    @Override
    public boolean supportsBatchMode() {
        return true;
    }

    @Override
    public boolean supportsVoidProtection() {
        return true;
    }

    @Override
    public int getPollutionPerSecond(ItemStack stack) {
        return 102400;
    }

    int glassLevel() {
        return Math.max(1, glass - 2);
    }

    int coilLevel() {
        return Math.max(1, coil.getTier() + 1);
    }

    int parallel() {
        return SmallQuantumRules.parallel(glassLevel(), GTUtility.getTier(getMaxInputVoltage()));
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new ProcessingLogic().setMaxParallelSupplier(this::parallel)
            .setSpeedBonusSupplier(() -> SmallQuantumRules.durationMultiplier(coilLevel()))
            .setEuModifierSupplier(() -> SmallQuantumRules.energyMultiplier(glassLevel(), coilLevel()));
    }

    @Override
    public IStructureDefinition<SmallQuantumMachine> getStructureDefinition() {
        if (structure == null) structure = StructureDefinition.<SmallQuantumMachine>builder()
            .addShape(PIECE, SHAPE)
            .addElement('A', ofBlock(GregTechAPI.sBlockCasings2, 0))
            .addElement('B', ofBlock(GregTechAPI.sBlockCasings3, 10))
            .addElement(
                'C',
                ofCoil((SmallQuantumMachine t, HeatingCoilLevel level) -> { t.coil = level; }, t -> t.coil))
            .addElement('D', ofBlock(GregTechAPI.sBlockCasings4, 1))
            .addElement('E', chainAllGlasses(-1, (SmallQuantumMachine t, Integer tier) -> t.glass = tier, t -> t.glass))
            .addElement(
                'H',
                buildHatchAdder(SmallQuantumMachine.class)
                    .atLeast(
                        InputBus,
                        InputHatch,
                        OutputBus,
                        OutputHatch,
                        Energy.or(ExoticEnergy),
                        Maintenance,
                        Muffler)
                    .casingIndex(Casings.CleanStainlessSteelMachineCasing.getTextureId())
                    .hint(1)
                    .buildAndChain(ofBlock(GregTechAPI.sBlockCasings4, 1)))
            .addElement(
                'T',
                buildHatchAdder(SmallQuantumMachine.class)
                    .atLeast(
                        InputBus,
                        InputHatch,
                        OutputBus,
                        OutputHatch,
                        Energy.or(ExoticEnergy),
                        Maintenance,
                        Muffler)
                    .casingIndex(Casings.CleanStainlessSteelMachineCasing.getTextureId())
                    .hint(1)
                    .buildAndChain(ofBlock(GregTechAPI.sBlockCasings4, 1)))
            .build();
        return structure;
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece(PIECE, stack, hintsOnly, 7, 20, 4);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int budget, ISurvivalBuildEnvironment env) {
        return mMachine ? -1 : survivalBuildPiece(PIECE, stack, 7, 20, 4, budget, env, false, true);
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        coil = HeatingCoilLevel.None;
        glass = -1;
        if (!checkPiece(PIECE, 7, 20, 4, errors)) return;
        checkHatchMin(errors, OutputBus, 1);
        checkHatchMin(errors, Muffler, 1);
        checkHatchMin(errors, Maintenance, 1);
        if (mEnergyHatches.isEmpty() && mExoticEnergyHatches.isEmpty()) checkHatchMin(errors, Energy, 1);
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        ITexture casing = Casings.CleanStainlessSteelMachineCasing.getCasingTexture();
        if (side != facing) return new ITexture[] { casing };
        return new ITexture[] { casing, TextureFactory.builder()
            .addIcon(active ? TexturesGtBlock.oMCAQFTActive : TexturesGtBlock.oMCAQFT)
            .setRGBA(new short[] { 180, 180, 180, 255 })
            .build() };
    }

    @Override
    public MultiblockTooltipBuilder createTooltip() {
        return new MultiblockTooltipBuilder().addMachineType("小型量子操纵者")
            .addInfo("以低压、低产量和较长时间运行量子操纵者配方")
            .addInfo(EnumChatFormatting.AQUA + "无需催化剂，支持批处理")
            .addSeparator()
            .addInfo(EnumChatFormatting.YELLOW + "LV 1 并行" + EnumChatFormatting.GRAY + "，每升一级电压并行翻倍")
            .addInfo(EnumChatFormatting.AQUA + "硼玻璃" + EnumChatFormatting.GRAY + "基础并行 1，每升一级玻璃增加 4 基础并行")
            .addInfo(EnumChatFormatting.GOLD + "白铜线圈" + EnumChatFormatting.GRAY + "耗时 120%，每升一级减少 10%，最低 10%")
            .addInfo(
                "1 级玻璃和线圈耗能 " + EnumChatFormatting.RED + "150%" + EnumChatFormatting.GRAY + "；取较低等级，每升一级减少 10%，最低 10%")
            .addPollutionAmount(getPollutionPerSecond(null))
            .beginStructureBlock(15, 21, 15, true)
            .addController("中央立柱正面底部")
            .addCasing("236", "坚固钢制机械方块", false)
            .addCasing("142", "格栅机械方块", false)
            .addCasing("177", "同等级加热线圈", true)
            .addCasing("224", "同等级硼玻璃或更高级玻璃", true)
            .addCasing("其余", "洁净不锈钢机械方块", false)
            .addEnergyHatch("1+", "顶部或底部洁净不锈钢机械方块", 1)
            .addMaintenanceHatch("1+", "顶部或底部洁净不锈钢机械方块", 1)
            .addMufflerHatch("1+", "顶部或底部洁净不锈钢机械方块", 1)
            .addOutputBus("1+", "顶部或底部洁净不锈钢机械方块", 1)
            .addInputBus("可选", "顶部或底部洁净不锈钢机械方块", 1)
            .addInputHatch("可选", "顶部或底部洁净不锈钢机械方块", 1)
            .addOutputHatch("可选", "顶部或底部洁净不锈钢机械方块", 1)
            .toolTipFinisher();
    }
}
