// Ported from EyeOfHarmonyBuffer (GPL-3.0); see THIRD_PARTY_NOTICES.md.
package com.gtnhadjust.machine.reshaping;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.HatchElement.OutputHatch;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_DTPF_OFF;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_DTPF_ON;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FUSION1_GLOW;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnhadjust.GTNHAdjust;

import bartworks.common.loaders.ItemRegistry;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.TAE;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.OverclockCalculator;
import gtPlusPlus.core.block.ModBlocks;

/** EV structure with ten recipe passes per fixed 64-second cycle. */
public final class SubstanceReshapingDevice extends MTEExtendedPowerMultiBlockBase<SubstanceReshapingDevice>
    implements ISurvivalConstructable {

    private static final String MAIN = "main";
    private static IStructureDefinition<SubstanceReshapingDevice> structure;

    public SubstanceReshapingDevice(int id) {
        super(id, "gtnhadjust.substance_reshaping", "Substance Reshaping Device");
    }

    private SubstanceReshapingDevice(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new SubstanceReshapingDevice(mName);
    }

    @Override
    public IStructureDefinition<SubstanceReshapingDevice> getStructureDefinition() {
        if (structure == null) structure = IStructureDefinition.<SubstanceReshapingDevice>builder()
            .addShape(MAIN, transpose(ReshapingShape.shapeMain))
            .addElement('A', ofBlock(ItemRegistry.bw_realglas, 2))
            .addElement('B', ofBlock(GregTechAPI.sBlockCasings4, 0))
            .addElement(
                'C',
                buildHatchAdder(SubstanceReshapingDevice.class)
                    .atLeast(InputBus, OutputBus, InputHatch, OutputHatch, Energy.or(ExoticEnergy))
                    .casingIndex(TAE.GTPP_INDEX(11))
                    .hint(1)
                    .buildAndChain(ofBlock(ModBlocks.blockCasings2Misc, 4)))
            .build();
        return structure;
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        checkPiece(MAIN, 4, 4, 0, errors);
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece(MAIN, stack, hintsOnly, 4, 4, 0);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int budget, ISurvivalBuildEnvironment env) {
        return mMachine ? -1 : survivalBuildPiece(MAIN, stack, 4, 4, 0, budget, env, false, true);
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return ReshapingRegistration.MAP;
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new ProcessingLogic() {

            @Override
            protected OverclockCalculator createOverclockCalculator(GTRecipe recipe) {
                return OverclockCalculator.ofNoOverclock(recipe);
            }
        }.setMaxParallelSupplier(() -> 10);
    }

    /** Reserve real power per cycle and let GT calculate its parallel outputs independently. */
    @Override
    protected void setProcessingLogicPower(ProcessingLogic logic) {
        logic.setAvailableVoltage(Long.MAX_VALUE)
            .setAvailableAmperage(1)
            .setAmperageOC(false);
    }

    /** Preserve the upstream ten sequential recipe-selection passes, including native separated buses. */
    @Override
    public CheckRecipeResult checkProcessing() {
        if (getMaxInputEu() < ReshapingRegistration.PROCESSING_EUT)
            return CheckRecipeResultRegistry.insufficientPower(ReshapingRegistration.PROCESSING_EUT);
        List<ItemStack> items = new ArrayList<>();
        List<FluidStack> fluids = new ArrayList<>();
        CheckRecipeResult result = CheckRecipeResultRegistry.NO_RECIPE;
        int passes = 0;
        for (int pass = 0; pass < 10; pass++) {
            setupProcessingLogic(processingLogic);
            CheckRecipeResult current = doCheckRecipe();
            if (!current.wasSuccessful()) {
                if (items.isEmpty() && fluids.isEmpty()) result = current;
                break;
            }
            result = current;
            passes++;
            if (processingLogic.getOutputItems() != null) items.addAll(Arrays.asList(processingLogic.getOutputItems()));
            if (processingLogic.getOutputFluids() != null)
                fluids.addAll(Arrays.asList(processingLogic.getOutputFluids()));
        }
        updateSlots();
        if (GTNHAdjust.debug) GTNHAdjust.LOG.info(
            "[reshaping] passes={} itemStacks={} fluidStacks={} result={}",
            passes,
            items.size(),
            fluids.size(),
            result.getID());
        if (!result.wasSuccessful()) return result;
        mOutputItems = items.toArray(new ItemStack[0]);
        mOutputFluids = fluids.toArray(new FluidStack[0]);
        mEfficiency = 10000;
        mEfficiencyIncrease = 10000;
        mMaxProgresstime = ReshapingRegistration.PROCESSING_TICKS;
        lEUt = -ReshapingRegistration.PROCESSING_EUT;
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    @Override
    public boolean getDefaultHasMaintenanceChecks() {
        return false;
    }

    @Override
    public boolean supportsInputSeparation() {
        return true;
    }

    @Override
    public boolean supportsVoidProtection() {
        return true;
    }

    @Override
    public boolean supportsBatchMode() {
        return true;
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return true;
    }

    @Override
    public int getItemOutputLimit() {
        return 16;
    }

    @Override
    public int getFluidOutputLimit() {
        return 16;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        ITexture casing = gregtech.api.enums.Textures.BlockIcons.casingTexturePages[0][12];
        if (side != facing) return new ITexture[] { casing };
        if (!active) return new ITexture[] { casing, TextureFactory.builder()
            .addIcon(OVERLAY_DTPF_OFF)
            .extFacing()
            .build() };
        return new ITexture[] { casing, TextureFactory.builder()
            .addIcon(OVERLAY_DTPF_ON)
            .extFacing()
            .build(),
            TextureFactory.builder()
                .addIcon(OVERLAY_FUSION1_GLOW)
                .extFacing()
                .glow()
                .build() };
    }

    private static String text(String key) {
        return StatCollector.translateToLocal("gtnhadjust.reshaping." + key);
    }

    /** Retain the original tooltip text and append the requested structure attribution. */
    @Override
    public MultiblockTooltipBuilder createTooltip() {
        MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder() {

            private String[] information;
            private String[] structureInformation;

            @Override
            public MultiblockTooltipBuilder toolTipFinisher() {
                super.toolTipFinisher();
                information = withCredit(super.getInformation());
                structureInformation = withCredit(super.getStructureInformation());
                return this;
            }

            @Override
            public String[] getInformation() {
                return information;
            }

            @Override
            public String[] getStructureInformation() {
                return structureInformation;
            }
        };
        tt.addMachineType(text("Tooltip_SubstanceReshapingDevice_MachineType"))
            .addInfo(text("Tooltip_SubstanceReshapingDevice_Controller"))
            .addInfo(text("processing_power"))
            .addInfo(text("EOHB_Starry_Miracle_Project"))
            .addInfo(text("EOHB_Text_SeparatingLine"));
        for (int i = 0; i <= 10; i++) {
            if (i == 6 || i == 9) tt.addInfo(text("EOHB_Text_SeparatingLine"));
            tt.addInfo(text("Tooltip_SubstanceReshapingDevice_" + String.format("%02d", i)));
        }
        return tt.addSeparator()
            .addInfo(text("StructureTooComplex"))
            .addInfo(text("BLUE_PRINT_INFO"))
            .addInputBus(text("add_InputBus"))
            .addInputHatch(text("add_inputHatch"))
            .addOutputBus(text("add_OutputBus"))
            .addOutputHatch(text("add_outputHatch"))
            .addEnergyHatch(text("add_energyHatch"), 1)
            .toolTipFinisher();
    }

    /** Keep attribution below the native footer in both ordinary and Shift tooltips. */
    private static String[] withCredit(String[] original) {
        String[] result = Arrays.copyOf(original, original.length + 1);
        result[original.length] = text("credit");
        return result;
    }
}
