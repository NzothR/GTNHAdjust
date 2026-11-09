package com.gtnhadjust;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FeatureSwitchesTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    private static final Set<String> MODS = new HashSet<>(
        Arrays.asList("gtneverreset", "sciencenotleisure", "cropsnh"));

    @Test
    public void ysmuArmsAreClientOnlyAndRequireBothMods() throws Exception {
        Path file = folder.newFile()
            .toPath();
        Set<String> mods = new HashSet<>(Arrays.asList("gtnrlib", "ysmu"));
        List<String> mixins = Arrays.asList("animation.YsmuHandTransformMixin", "animation.GtnrAnimatedArmMixin");
        FeatureSwitches enabled = FeatureSwitches.load(file);
        assertTrue(
            enabled.selectMixins(mods, true)
                .containsAll(mixins));
        assertFalse(
            enabled.selectMixins(mods, false)
                .contains(mixins.get(0)));
        assertFalse(
            enabled.selectMixins(new HashSet<>(Arrays.asList("gtnrlib")), true)
                .contains(mixins.get(0)));
        Files.write(file, Arrays.asList("ysmuAnimatedArms=false"), StandardCharsets.ISO_8859_1);
        for (String mixin : mixins) assertFalse(
            FeatureSwitches.load(file)
                .selectMixins(mods, true)
                .contains(mixin));
    }

    @Test
    public void proxyFactoryHasIndependentSwitch() throws Exception {
        Path file = folder.newFile()
            .toPath();
        assertTrue(FeatureSwitches.load(file).proxyFactory);
        Files.write(file, Arrays.asList("proxyFactory=false"), StandardCharsets.ISO_8859_1);
        assertFalse(FeatureSwitches.load(file).proxyFactory);
    }

    @Test
    public void proxyDispatchDiagnosticsIsOptInAndRequiresBothMods() throws Exception {
        Path file = folder.newFile()
            .toPath();
        Set<String> mods = new HashSet<>(Arrays.asList("gtneverreset", "appliedenergistics2"));
        String mixin = "diagnostic.ProxyCpuDispatchAuditMixin";
        String hatchMixin = "diagnostic.ProxyHatchDispatchAuditMixin";
        List<String> allProxyAudits = Arrays
            .asList(mixin, hatchMixin, "diagnostic.ProxyCircuitSlotAuditMixin", "diagnostic.ProxyCpuReceiveAuditMixin");
        assertFalse(
            FeatureSwitches.load(file)
                .selectMixins(mods, false)
                .contains(mixin));
        Files.write(file, Arrays.asList("proxyDispatchDiagnostics=true"), StandardCharsets.ISO_8859_1);
        assertTrue(
            FeatureSwitches.load(file)
                .selectMixins(mods, false)
                .containsAll(allProxyAudits));
        assertTrue(
            FeatureSwitches.load(file)
                .selectMixins(mods, false)
                .contains(mixin));
        assertTrue(
            FeatureSwitches.load(file)
                .selectMixins(mods, true)
                .contains(mixin));
        assertFalse(
            FeatureSwitches.load(file)
                .selectMixins(new HashSet<>(), false)
                .contains(mixin));
        Files.write(
            file,
            Arrays.asList("proxyDispatchDiagnostics=true", "proxyFactory=false"),
            StandardCharsets.ISO_8859_1);
        for (String audit : allProxyAudits) assertFalse(
            FeatureSwitches.load(file)
                .selectMixins(mods, false)
                .contains(audit));
        assertFalse(
            FeatureSwitches.load(file)
                .selectMixins(mods, false)
                .contains(mixin));
    }

    @Test
    public void lootSimulatorHasIndependentSwitch() throws Exception {
        Path file = folder.newFile()
            .toPath();
        assertTrue(FeatureSwitches.load(file).lootSimulator);
        Files.write(file, Arrays.asList("lootSimulator=false"), StandardCharsets.ISO_8859_1);
        assertFalse(FeatureSwitches.load(file).lootSimulator);
    }

    @Test
    public void soulVialExpansionRequiresEnderIoAndCanBeDisabledOnBothSides() throws Exception {
        Path file = folder.newFile()
            .toPath();
        Set<String> mods = new HashSet<>(Arrays.asList("EnderIO"));
        String mixin = "fix.SoulVialCaptureMixin";
        FeatureSwitches enabled = FeatureSwitches.load(file);
        assertTrue(
            enabled.selectMixins(mods, true)
                .contains(mixin));
        assertTrue(
            enabled.selectMixins(mods, false)
                .contains(mixin));
        assertFalse(
            enabled.selectMixins(new HashSet<>(), false)
                .contains(mixin));
        Files.write(file, Arrays.asList("soulVialExpandedCapture=false"), StandardCharsets.ISO_8859_1);
        FeatureSwitches disabled = FeatureSwitches.load(file);
        assertFalse(disabled.soulVialExpandedCapture);
        assertFalse(
            disabled.selectMixins(mods, true)
                .contains(mixin));
        assertFalse(
            disabled.selectMixins(mods, false)
                .contains(mixin));
    }

    @Test
    public void defaultsCreateFileAndSelectAll() throws Exception {
        Path file = folder.getRoot()
            .toPath()
            .resolve("config/features.properties");
        FeatureSwitches switches = FeatureSwitches.load(file);
        assertTrue(Files.isRegularFile(file));
        assertTrue(switches.carpenterRecipes);
        assertEquals(
            20,
            switches.selectMixins(MODS, true)
                .size());
        assertEquals(
            10,
            switches.selectMixins(MODS, false)
                .size());
    }

    @Test
    public void disabledFeaturesDoNotSelectAnyMixin() throws Exception {
        Path file = folder.newFile()
            .toPath();
        Files.write(
            file,
            Arrays.asList(
                "matrixPatternLoadFix=false",
                "patternRecipeMapPreservation=false",
                "matrixPatternLoadFix=false",
                "cropBreeder=false",
                "steamGreenhouse=false",
                "carpenterRecipes=false",
                "quickTerminal=false",
                "terminalCraftingRefreshFix=false",
                "patternDeduplication=false",
                "terminalInventoryFix=false"),
            StandardCharsets.ISO_8859_1);
        FeatureSwitches switches = FeatureSwitches.load(file);
        assertFalse(switches.carpenterRecipes);
        assertTrue(
            switches.selectMixins(MODS, true)
                .isEmpty());
        assertTrue(
            switches.selectMixins(MODS, false)
                .isEmpty());
    }

    @Test
    public void featuresCanBeDisabledIndependentlyAndInvalidValuesFailClosed() throws Exception {
        Path file = folder.newFile()
            .toPath();
        Files.write(
            file,
            Arrays.asList(
                "matrixPatternLoadFix=false",
                "patternRecipeMapPreservation=false",
                "matrixPatternLoadFix=false",
                "cropBreeder=false",
                "steamGreenhouse=unexpected",
                "carpenterRecipes=true",
                "quickTerminal=false",
                "terminalCraftingRefreshFix=false",
                "patternDeduplication=false",
                "terminalInventoryFix=false"),
            StandardCharsets.ISO_8859_1);
        FeatureSwitches switches = FeatureSwitches.load(file);
        assertTrue(switches.carpenterRecipes);
        assertTrue(
            switches.selectMixins(MODS, true)
                .isEmpty());
        Files.write(
            file,
            Arrays.asList(
                "matrixPatternLoadFix=false",
                "patternRecipeMapPreservation=false",
                "cropBreeder=true",
                "steamGreenhouse=false",
                "quickTerminal=false",
                "terminalCraftingRefreshFix=false",
                "patternDeduplication=false",
                "terminalInventoryFix=false"),
            StandardCharsets.ISO_8859_1);
        List<String> mixins = FeatureSwitches.load(file)
            .selectMixins(MODS, true);
        assertEquals(Arrays.asList("CropBreedingRulesMixin", "LargeCropBreederMixin"), mixins);
    }

    @Test
    public void unreadableConfigurationFailsClosed() throws Exception {
        Path directory = folder.newFolder()
            .toPath();
        FeatureSwitches switches = FeatureSwitches.load(directory);
        assertFalse(switches.carpenterRecipes);
        assertTrue(
            switches.selectMixins(MODS, true)
                .isEmpty());
    }

    @Test
    public void terminalMixinsAreOneSwitchAcrossBothSides() throws Exception {
        Path file = folder.newFile()
            .toPath();
        Files.write(
            file,
            Arrays.asList(
                "matrixPatternLoadFix=false",
                "patternRecipeMapPreservation=false",
                "matrixPatternLoadFix=false",
                "cropBreeder=false",
                "steamGreenhouse=false",
                "quickTerminal=true",
                "terminalCraftingRefreshFix=false",
                "patternDeduplication=false",
                "terminalInventoryFix=false"),
            StandardCharsets.ISO_8859_1);
        FeatureSwitches enabled = FeatureSwitches.load(file);
        assertEquals(
            9,
            enabled.selectMixins(MODS, true)
                .size());
        assertTrue(
            enabled.selectMixins(MODS, true)
                .contains("terminal.BookmarkEncodeClickMixin"));
        assertFalse(
            enabled.selectMixins(MODS, false)
                .contains("terminal.BookmarkEncodeClickMixin"));
        assertEquals(
            2,
            enabled.selectMixins(MODS, false)
                .size());
        assertTrue(
            enabled.selectMixins(new HashSet<>(), true)
                .isEmpty());
        Files.write(
            file,
            Arrays.asList(
                "matrixPatternLoadFix=false",
                "patternRecipeMapPreservation=false",
                "matrixPatternLoadFix=false",
                "cropBreeder=false",
                "steamGreenhouse=false",
                "quickTerminal=false",
                "terminalCraftingRefreshFix=false",
                "patternDeduplication=false",
                "terminalInventoryFix=false"),
            StandardCharsets.ISO_8859_1);
        FeatureSwitches disabled = FeatureSwitches.load(file);
        assertTrue(
            disabled.selectMixins(MODS, true)
                .isEmpty());
        assertTrue(
            disabled.selectMixins(MODS, false)
                .isEmpty());
    }

    @Test
    public void gtsrDiagnosticsCanBeDisabledWithoutDisablingOtherFeatures() throws Exception {
        Path file = folder.newFile()
            .toPath();
        Set<String> mods = new HashSet<>(Arrays.asList("gtsr"));
        assertEquals(
            Arrays.asList("diagnostic.GtsrRecipeAuditMixin"),
            FeatureSwitches.load(file)
                .selectMixins(mods, false));
        Files.write(
            file,
            Arrays.asList("terminalCraftingRefreshFix=false", "gtsrDiagnostics=false"),
            StandardCharsets.ISO_8859_1);
        assertTrue(
            FeatureSwitches.load(file)
                .selectMixins(mods, false)
                .isEmpty());
    }

    @Test
    public void gtsrSpeedFixRequiresBothModsAndCanBeDisabledIndependently() throws Exception {
        Path file = folder.newFile()
            .toPath();
        Files.write(
            file,
            Arrays.asList(
                "matrixPatternLoadFix=false",
                "patternRecipeMapPreservation=false",
                "matrixPatternLoadFix=false",
                "cropBreeder=false",
                "quickTerminal=false",
                "gtsrDiagnostics=false",
                "terminalCraftingRefreshFix=false",
                "patternDeduplication=false"),
            StandardCharsets.ISO_8859_1);
        Set<String> mods = new HashSet<>(Arrays.asList("gtsr", "gtneverreset"));
        assertEquals(
            Arrays.asList("fix.GtsrRegistrationScopeMixin", "fix.GtsrRecipeMapSpeedExclusionMixin"),
            FeatureSwitches.load(file)
                .selectMixins(mods, false));
        mods.remove("gtneverreset");
        assertTrue(
            FeatureSwitches.load(file)
                .selectMixins(mods, false)
                .isEmpty());
        mods.add("gtneverreset");
        Files.write(
            file,
            Arrays.asList(
                "matrixPatternLoadFix=false",
                "patternRecipeMapPreservation=false",
                "matrixPatternLoadFix=false",
                "cropBreeder=false",
                "quickTerminal=false",
                "gtsrDiagnostics=false",
                "gtsrRecipeSpeedCompatibility=false",
                "terminalCraftingRefreshFix=false",
                "patternDeduplication=false"),
            StandardCharsets.ISO_8859_1);
        assertTrue(
            FeatureSwitches.load(file)
                .selectMixins(mods, false)
                .isEmpty());
    }

    @Test
    public void patternDeduplicationHasIndependentSwitch() throws Exception {
        Path file = folder.newFile()
            .toPath();
        Files.write(
            file,
            Arrays.asList(
                "matrixPatternLoadFix=false",
                "patternRecipeMapPreservation=false",
                "matrixPatternLoadFix=false",
                "cropBreeder=false",
                "quickTerminal=false",
                "terminalCraftingRefreshFix=false",
                "patternDeduplication=true"),
            StandardCharsets.ISO_8859_1);
        Set<String> mods = new HashSet<>(Arrays.asList("gtneverreset"));
        assertEquals(
            Arrays.asList("terminal.TerminalPatternDedupMixin"),
            FeatureSwitches.load(file)
                .selectMixins(mods, false));
        Files.write(
            file,
            Arrays.asList(
                "matrixPatternLoadFix=false",
                "patternRecipeMapPreservation=false",
                "matrixPatternLoadFix=false",
                "cropBreeder=false",
                "quickTerminal=false",
                "terminalCraftingRefreshFix=false",
                "patternDeduplication=false"),
            StandardCharsets.ISO_8859_1);
        assertTrue(
            FeatureSwitches.load(file)
                .selectMixins(mods, false)
                .isEmpty());
    }

    @Test
    public void terminalInventoryFixIsIndependentAndClientOnly() throws Exception {
        Path file = folder.newFile()
            .toPath();
        Files.write(
            file,
            Arrays.asList(
                "matrixPatternLoadFix=false",
                "patternRecipeMapPreservation=false",
                "matrixPatternLoadFix=false",
                "cropBreeder=false",
                "quickTerminal=false",
                "terminalCraftingRefreshFix=false",
                "patternDeduplication=false"),
            StandardCharsets.ISO_8859_1);
        Set<String> mods = new HashSet<>(Arrays.asList("gtneverreset", "neenergistics"));
        FeatureSwitches enabled = FeatureSwitches.load(file);
        assertEquals(
            Arrays.asList(
                "terminal.NeiOverlayButtonAccessor",
                "terminal.NeiOverlayTooltipMixin",
                "terminal.NeiAlwaysShowPresenceMixin",
                "fix.QuickTerminalInventoryPacketMixin",
                "fix.QuickTerminalRecipeScreenMixin",
                "fix.QuickTerminalPresenceMixin"),
            enabled.selectMixins(mods, true));
        assertTrue(
            enabled.selectMixins(mods, false)
                .isEmpty());
        mods.remove("neenergistics");
        assertEquals(
            Arrays.asList("fix.QuickTerminalInventoryPacketMixin", "fix.QuickTerminalRecipeScreenMixin"),
            enabled.selectMixins(mods, true));
        Files.write(
            file,
            Arrays.asList(
                "matrixPatternLoadFix=false",
                "patternRecipeMapPreservation=false",
                "matrixPatternLoadFix=false",
                "cropBreeder=false",
                "quickTerminal=false",
                "terminalCraftingRefreshFix=false",
                "patternDeduplication=false",
                "terminalInventoryFix=false"),
            StandardCharsets.ISO_8859_1);
        assertTrue(
            FeatureSwitches.load(file)
                .selectMixins(mods, true)
                .isEmpty());
    }

    @Test
    public void terminalCraftingRefreshHasIndependentSwitch() throws Exception {
        Path file = folder.newFile()
            .toPath();
        Files.write(
            file,
            Arrays.asList(
                "matrixPatternLoadFix=false",
                "patternRecipeMapPreservation=false",
                "matrixPatternLoadFix=false",
                "cropBreeder=false",
                "quickTerminal=false",
                "patternDeduplication=false"),
            StandardCharsets.ISO_8859_1);
        Set<String> mods = new HashSet<>(Arrays.asList("gtneverreset"));
        assertEquals(
            Arrays.asList("fix.QuickTerminalCraftingRefreshMixin"),
            FeatureSwitches.load(file)
                .selectMixins(mods, false));
        Files.write(
            file,
            Arrays.asList(
                "matrixPatternLoadFix=false",
                "patternRecipeMapPreservation=false",
                "matrixPatternLoadFix=false",
                "cropBreeder=false",
                "quickTerminal=false",
                "patternDeduplication=false",
                "terminalCraftingRefreshFix=false"),
            StandardCharsets.ISO_8859_1);
        assertTrue(
            FeatureSwitches.load(file)
                .selectMixins(mods, false)
                .isEmpty());
    }

    @Test
    public void neoEcoCraftingFixRequiresBothModsAndCanBeDisabled() throws Exception {
        Path file = folder.newFile()
            .toPath();
        Set<String> mods = new HashSet<>(Arrays.asList("neoecoae", "appliedenergistics2"));
        FeatureSwitches defaults = FeatureSwitches.load(file);
        assertFalse(
            defaults.selectMixins(mods, false)
                .contains("fix.NeoEcoCraftingRefreshMixin"));
        Files.write(file, Arrays.asList("neoEcoCraftingRefreshFix=true"), StandardCharsets.ISO_8859_1);
        defaults = FeatureSwitches.load(file);
        assertTrue(
            defaults.selectMixins(mods, false)
                .contains("fix.NeoEcoCraftingRefreshMixin"));
        assertTrue(
            defaults.selectMixins(mods, true)
                .contains("fix.NeoEcoCraftingRefreshMixin"));
        assertFalse(
            defaults.selectMixins(new HashSet<>(Arrays.asList("neoecoae")), false)
                .contains("fix.NeoEcoCraftingRefreshMixin"));
        Files.write(file, Arrays.asList("neoEcoCraftingRefreshFix=false"), StandardCharsets.ISO_8859_1);
        assertFalse(
            FeatureSwitches.load(file)
                .selectMixins(mods, false)
                .contains("fix.NeoEcoCraftingRefreshMixin"));
    }

    @Test
    public void craftingDiagnosticsHasIndependentSwitch() throws Exception {
        Path file = folder.newFile()
            .toPath();
        Files.write(
            file,
            Arrays.asList(
                "patternRecipeMapPreservation=false",
                "matrixPatternLoadFix=false",
                "cropBreeder=false",
                "quickTerminal=false",
                "patternDeduplication=false",
                "terminalCraftingRefreshFix=false",
                "terminalInventoryFix=false"),
            StandardCharsets.ISO_8859_1);
        Set<String> mods = new HashSet<>(Arrays.asList("gtneverreset"));
        FeatureSwitches defaults = FeatureSwitches.load(file);
        assertFalse(defaults.craftingDiagnostics);
        assertTrue(
            defaults.selectMixins(mods, false)
                .isEmpty());
        Files.write(
            file,
            Arrays.asList(
                "patternRecipeMapPreservation=false",
                "matrixPatternLoadFix=false",
                "cropBreeder=false",
                "quickTerminal=false",
                "patternDeduplication=false",
                "terminalCraftingRefreshFix=false",
                "terminalInventoryFix=false",
                "matrixPatternLoadFix=false",
                "craftingDiagnostics=true"),
            StandardCharsets.ISO_8859_1);
        List<String> mixins = FeatureSwitches.load(file)
            .selectMixins(mods, false);
        assertTrue(mixins.contains("diagnostic.CraftingGridAuditMixin"));
        assertTrue(mixins.contains("diagnostic.FactoryProcessingAuditMixin"));
    }

    @Test
    public void patternMapPreservationIsIndependentlyControlledOnBothSides() throws Exception {
        Path file = folder.newFile()
            .toPath();
        Set<String> mods = new HashSet<>(Arrays.asList("gtneverreset"));
        FeatureSwitches enabled = FeatureSwitches.load(file);
        assertTrue(
            enabled.selectMixins(mods, true)
                .contains("fix.PatternRecipeMapPreservationMixin"));
        assertTrue(
            enabled.selectMixins(mods, false)
                .contains("fix.PatternRecipeMapPreservationMixin"));
        Files.write(file, Arrays.asList("patternRecipeMapPreservation=false"), StandardCharsets.ISO_8859_1);
        FeatureSwitches disabled = FeatureSwitches.load(file);
        assertFalse(
            disabled.selectMixins(mods, true)
                .contains("fix.PatternRecipeMapPreservationMixin"));
        assertFalse(
            disabled.selectMixins(mods, false)
                .contains("fix.PatternRecipeMapPreservationMixin"));
    }

    @Test
    public void terminalNeiPresenceCanBeDisabledAndIsClientOnly() throws Exception {
        Path file = folder.newFile()
            .toPath();
        Set<String> mods = new HashSet<>(Arrays.asList("gtneverreset", "neenergistics"));
        assertTrue(
            FeatureSwitches.load(file)
                .selectMixins(mods, true)
                .contains("terminal.NeiAlwaysShowPresenceMixin"));
        assertFalse(
            FeatureSwitches.load(file)
                .selectMixins(mods, false)
                .contains("terminal.NeiAlwaysShowPresenceMixin"));
        Files.write(file, Arrays.asList("terminalNeiAlwaysShowPresence=false"), StandardCharsets.ISO_8859_1);
        assertFalse(
            FeatureSwitches.load(file)
                .selectMixins(mods, true)
                .contains("terminal.NeiAlwaysShowPresenceMixin"));
    }

    @Test
    public void smartIngredientMixinRequiresSwitchAndClientSide() throws Exception {
        Path file = folder.newFile()
            .toPath();
        Set<String> mods = new HashSet<>(Arrays.asList("gtneverreset"));
        assertTrue(
            FeatureSwitches.load(file)
                .selectMixins(mods, true)
                .contains("terminal.SmartIngredientTransferMixin"));
        assertFalse(
            FeatureSwitches.load(file)
                .selectMixins(mods, false)
                .contains("terminal.SmartIngredientTransferMixin"));
        Files.write(file, Arrays.asList("terminalSmartIngredients=false"), StandardCharsets.ISO_8859_1);
        assertFalse(
            FeatureSwitches.load(file)
                .selectMixins(mods, true)
                .contains("terminal.SmartIngredientTransferMixin"));
    }

    @Test
    public void matrixPatternDiagnosticsDefaultOffAndSelectableOnBothSides() throws Exception {
        Path file = folder.newFile()
            .toPath();
        Set<String> mods = new HashSet<>(Arrays.asList("gtneverreset"));
        assertFalse(
            FeatureSwitches.load(file)
                .selectMixins(mods, false)
                .contains("diagnostic.MatrixPatternAuditMixin"));
        Files.write(file, Arrays.asList("matrixPatternDiagnostics=true"), StandardCharsets.ISO_8859_1);
        FeatureSwitches enabled = FeatureSwitches.load(file);
        for (String mixin : Arrays.asList(
            "diagnostic.MatrixInventoryAuditMixin",
            "diagnostic.MatrixPatternAuditMixin",
            "diagnostic.MatrixTickAuditMixin")) {
            assertTrue(
                enabled.selectMixins(mods, false)
                    .contains(mixin));
            assertTrue(
                enabled.selectMixins(mods, true)
                    .contains(mixin));
            assertFalse(
                enabled.selectMixins(new HashSet<>(), false)
                    .contains(mixin));
        }
    }

    @Test
    public void matrixLoadFixCanBeDisabledOnBothSides() throws Exception {
        Path file = folder.newFile()
            .toPath();
        Set<String> mods = new HashSet<>(Arrays.asList("gtneverreset"));
        FeatureSwitches enabled = FeatureSwitches.load(file);
        for (String name : Arrays.asList("fix.MatrixPatternDecodeMixin", "fix.MatrixPatternRetryMixin")) {
            assertTrue(
                enabled.selectMixins(mods, true)
                    .contains(name));
            assertTrue(
                enabled.selectMixins(mods, false)
                    .contains(name));
        }
        Files.write(file, Arrays.asList("matrixPatternLoadFix=false"), StandardCharsets.ISO_8859_1);
        FeatureSwitches disabled = FeatureSwitches.load(file);
        assertFalse(
            disabled.selectMixins(mods, true)
                .contains("fix.MatrixPatternDecodeMixin"));
        assertFalse(
            disabled.selectMixins(mods, false)
                .contains("fix.MatrixPatternRetryMixin"));
    }

}
