package com.gtnhadjust.machine.threadedproxy;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.HatchElement.OutputHatch;

import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtneverreset.common.machine.hatch.CrossRecipeMEPatternBuffer;

import gregtech.api.enums.TAE;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.util.GTStructureUtility;
import gtPlusPlus.core.block.ModBlocks;

/** GTNH 2.9.0 quantum force transformer geometry, with fixed tier-one cores and native hatch regions. */
final class ThreadedProxyStructure {

    static final String PIECE = "main";
    static final int X = 7, Y = 20, Z = 4;
    static final int CASING_TEXTURE = TAE.getIndexFromPage(0, 10);
    static final IHatchElement<ThreadedJointProxy> PATTERNS = InputBus
        .<ThreadedJointProxy>withMteClass(CrossRecipeMEPatternBuffer.class)
        .withName("gtnhadjust_threaded_proxy_patterns")
        .withCount(
            machine -> machine.patternHatches()
                .size());

    private ThreadedProxyStructure() {}

    /** Preserve the native geometry and H/T regions, replacing tier channels with their tier-one blocks. */
    static IStructureDefinition<ThreadedJointProxy> create() {
        return StructureDefinition.<ThreadedJointProxy>builder()
            .addShape(
                PIECE,
                new String[][] {
                    // spotless:off
                { "               ", "               ", "               ", "               ", "               ", "               ", "               ", "               ", "               ", "               ", "               ", "               ", "               ", "      BAB      ", "   BBBBABBBB   ", "   BAAAAAAAB   ", "   BABBABBAB   ", "   BA     AB   ", "    A     A    ", "    A     A    ", "    A     A    " },
                { "               ", "               ", "               ", "               ", "               ", "               ", "               ", "               ", "               ", "               ", "               ", "               ", "      BAB      ", "   AAABBBAAA   ", "  BAAAAAAAAAB  ", "  B         B  ", "  A         A  ", "  A         A  ", "               ", "               ", "               " },
                { "               ", "               ", "               ", "               ", "               ", "               ", "               ", "               ", "               ", "               ", "               ", "      BAB      ", "    AA   AA    ", "  AA       AA  ", " BAA       AAB ", " B           B ", " A           A ", " A           A ", "               ", "               ", "               " },
                { "               ", "               ", "               ", "               ", "               ", "               ", "               ", "               ", "               ", "               ", "               ", "     BAAAB     ", "   AA     AA   ", " AA         AA ", "BAA         AAB", "B             B", "A             A", "A             A", "A             A", "A             A", "A             A" },
                { "      TTT      ", "      EEE      ", "      EEE      ", "      EEE      ", "      DDD      ", "      EEE      ", "      DDD      ", "      EEE      ", "      EEE      ", "      EEE      ", "      DDD      ", "    BAEEEAB    ", "  AA  EEE  AA  ", " A    EEE    A ", "BA    DDD    AB", "B     EEE     B", "B     DDD     B", "      EEE      ", "      EEE      ", "      EEE      ", "      H~H      " },
                { "     TTTTT     ", "     ECCCE     ", "     ECCCE     ", "     ECCCE     ", "     D   D     ", "     ECCCE     ", "     D   D     ", "     ECCCE     ", "     ECCCE     ", "     ECCCE     ", "     D   D     ", "   BAECCCEAB   ", "  A  ECCCE  A  ", " A   ECCCE   A ", "BA   D   D   AB", "B    ECCCE    B", "B    D   D    B", "B    ECCCE    B", "     ECCCE     ", "     ECCCE     ", "     HHHHH     " },
                { "    TTTTTTT    ", "    ECCCCCE    ", "    EC   CE    ", "    EC   CE    ", "    D     D    ", "    EC   CE    ", "    D     D    ", "    EC   CE    ", "    EC   CE    ", "    EC   CE    ", "    D     D    ", "  BAEC   CEAB  ", " B  EC   CE  B ", "BB  EC   CE  BB", "BA  D     D  AB", "A   EC   CE   A", "A   D     D   A", "A   EC   CE   A", "    EC   CE    ", "    EC   CE    ", "    HHHHHHH    " },
                { "    TTTTTTT    ", "    ECCCCCE    ", "    EC   CE    ", "    EC   CE    ", "    D     D    ", "    EC   CE    ", "    D     D    ", "    EC   CE    ", "    EC   CE    ", "    EC   CE    ", "    D     D    ", "  AAEC   CEAA  ", " A  EC   CE  A ", "AB  EC   CE  BA", "AA  D     D  AA", "A   EC   CE   A", "A   D     D   A", "    EC   CE    ", "    EC   CE    ", "    EC   CE    ", "    HHHHHHH    " },
                { "    TTTTTTT    ", "    ECCCCCE    ", "    EC   CE    ", "    EC   CE    ", "    D     D    ", "    EC   CE    ", "    D     D    ", "    EC   CE    ", "    EC   CE    ", "    EC   CE    ", "    D     D    ", "  BAEC   CEAB  ", " B  EC   CE  B ", "BB  EC   CE  BB", "BA  D     D  AB", "A   EC   CE   A", "A   D     D   A", "A   EC   CE   A", "    EC   CE    ", "    EC   CE    ", "    HHHHHHH    " },
                { "     TTTTT     ", "     ECCCE     ", "     ECCCE     ", "     ECCCE     ", "     D   D     ", "     ECCCE     ", "     D   D     ", "     ECCCE     ", "     ECCCE     ", "     ECCCE     ", "     D   D     ", "   BAECCCEAB   ", "  A  ECCCE  A  ", " A   ECCCE   A ", "BA   D   D   AB", "B    ECCCE    B", "B    D   D    B", "B    ECCCE    B", "     ECCCE     ", "     ECCCE     ", "     HHHHH     " },
                { "      TTT      ", "      EEE      ", "      EEE      ", "      EEE      ", "      DDD      ", "      EEE      ", "      DDD      ", "      EEE      ", "      EEE      ", "      EEE      ", "      DDD      ", "    BAEEEAB    ", "  AA  EEE  AA  ", " A    EEE    A ", "BA    DDD    AB", "B     EEE     B", "B     DDD     B", "      EEE      ", "      EEE      ", "      EEE      ", "      HHH      " },
                { "               ", "               ", "               ", "               ", "               ", "               ", "               ", "               ", "               ", "               ", "               ", "     BAAAB     ", "   AA     AA   ", " AA         AA ", "BAA         AAB", "B             B", "A             A", "A             A", "A             A", "A             A", "A             A" },
                { "               ", "               ", "               ", "               ", "               ", "               ", "               ", "               ", "               ", "               ", "               ", "      BAB      ", "    AA   AA    ", "  AA       AA  ", " BAA       AAB ", " B           B ", " A           A ", " A           A ", "               ", "               ", "               " },
                { "               ", "               ", "               ", "               ", "               ", "               ", "               ", "               ", "               ", "               ", "               ", "               ", "      BAB      ", "   AAABBBAAA   ", "  BAAAAAAAAAB  ", "  B         B  ", "  A         A  ", "  A         A  ", "               ", "               ", "               " },
                { "               ", "               ", "               ", "               ", "               ", "               ", "               ", "               ", "               ", "               ", "               ", "               ", "               ", "      BAB      ", "   BBBBABBBB   ", "   BBBAAABBB   ", "   ABBAAABBA   ", "   A BA AB A   ", "      A A      ", "      A A      ", "      A A      " }, })
                // spotless:on
            .addElement('A', ofBlock(ModBlocks.blockCasings5Misc, 7))
            .addElement('B', ofBlock(ModBlocks.blockCasings5Misc, 11))
            .addElement('C', ofBlock(ModBlocks.blockCasings4Misc, 4))
            .addElement('D', ofBlock(ModBlocks.blockCasings2Misc, 12))
            .addElement('E', ofBlock(ModBlocks.blockCasings5Misc, 15))
            .addElement(
                'H',
                GTStructureUtility.buildHatchAdder(ThreadedJointProxy.class)
                    .anyOf(PATTERNS, InputBus, InputHatch, Energy, ExoticEnergy)
                    .adder(ThreadedJointProxy::addProxyInputHatch)
                    .casingIndex(CASING_TEXTURE)
                    .hint(1)
                    .buildAndChain(ofBlock(ModBlocks.blockCasings2Misc, 12)))
            .addElement(
                'T',
                GTStructureUtility.buildHatchAdder(ThreadedJointProxy.class)
                    .anyOf(OutputBus, OutputHatch)
                    .adder(ThreadedJointProxy::addProxyOutputHatch)
                    .casingIndex(CASING_TEXTURE)
                    .hint(2)
                    .buildAndChain(ofBlock(ModBlocks.blockCasings2Misc, 12)))
            .build();
    }
}
