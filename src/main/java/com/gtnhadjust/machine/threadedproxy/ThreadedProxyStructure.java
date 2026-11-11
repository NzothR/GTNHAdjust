package com.gtnhadjust.machine.threadedproxy;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.HatchElement.OutputHatch;

import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.science.gtnl.api.casing.GTNLCasings;
import com.science.gtnl.utils.StructureUtils;
import com.xyp.gtneverreset.common.machine.hatch.CrossRecipeMEPatternBuffer;

import bartworks.common.loaders.ItemRegistry;
import gregtech.api.casing.Casings;
import gregtech.api.enums.Materials;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.util.GTStructureUtility;
import kubatech.loaders.BlockLoader;
import tectech.thing.casing.BlockGTCasingsTT;

/** GTNL dev-290 alloy mixing furnace geometry, with this controller and optional ordinary service hatches. */
final class ThreadedProxyStructure {

    static final String PIECE = "main";
    static final int X = 8, Y = 14, Z = 0;
    static final int CASING_TEXTURE = BlockGTCasingsTT.textureOffset;
    static final IHatchElement<ThreadedJointProxy> PATTERNS = InputBus
        .<ThreadedJointProxy>withMteClass(CrossRecipeMEPatternBuffer.class)
        .withName("gtnhadjust_threaded_proxy_patterns")
        .withCount(
            machine -> machine.patternHatches()
                .size());

    private ThreadedProxyStructure() {}

    static IStructureDefinition<ThreadedJointProxy> create() {
        return StructureDefinition.<ThreadedJointProxy>builder()
            .addShape(
                PIECE,
                transpose(StructureUtils.readStructureFromFile("sciencenotleisure:multiblock/smelting_mixing_furnace")))
            .addElement('A', ofBlock(ItemRegistry.bw_realglas, 6))
            .addElement('B', GTNLCasings.NeutroniumGearbox.asElement())
            .addElement('C', GTNLCasings.Antifreeze_Heatproof_Machine_Casing.asElement())
            .addElement('D', ofBlock(BlockLoader.defcCasingBlock, 7))
            .addElement('E', ofBlock(BlockLoader.defcCasingBlock, 10))
            .addElement('F', Casings.DimensionallyTranscendentCasing.asElement())
            .addElement('G', Casings.DimensionalInjectionCasing.asElement())
            .addElement('H', Casings.ActiveNeutroniumCasing.asElement())
            .addElement('I', Casings.SolidifierCasing.asElement())
            .addElement('J', Casings.AdvancedIridiumPlatedMachineCasing.asElement())
            .addElement('K', Casings.NaquadriaReinforcedWaterPlantCasing.asElement())
            .addElement('L', Casings.ContainmentFieldGenerator.asElement())
            .addElement('M', Casings.HollowCasing.asElement())
            .addElement('N', GTStructureUtility.ofFrame(Materials.Infinity))
            .addElement('O', Casings.WashPlantCasing.asElement())
            .addElement('P', Casings.MolecularContainmentCasing.asElement())
            .addElement('Q', Casings.HastelloyNReactorCasing.asElement())
            .addElement(
                'R',
                GTStructureUtility.buildHatchAdder(ThreadedJointProxy.class)
                    .anyOf(PATTERNS, InputBus, InputHatch, OutputBus, OutputHatch, Energy, ExoticEnergy)
                    .adder(ThreadedJointProxy::addProxyHatch)
                    .casingIndex(CASING_TEXTURE)
                    .hint(1)
                    .buildAndChain(Casings.HighPowerCasing.asElement()))
            .build();
    }
}
