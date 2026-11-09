package com.gtnhadjust;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

/** Loaded before mixins are selected. Disabled features never enter the mixin list. */
final class FeatureSwitches {

    private static final Logger LOG = Logger.getLogger("GTNHAdjust");
    private static final String YSMU_ARMS = "ysmuAnimatedArms";
    private static final String LOOT_SIMULATOR = "lootSimulator";
    private static final String PROXY_FACTORY = "proxyFactory";
    private static final String PROXY_TRACE = "proxyDispatchDiagnostics";
    private static final String CROP = "cropBreeder";
    private static final String GREENHOUSE = "steamGreenhouse";
    private static final String CARPENTER = "carpenterRecipes";
    private static final String TERMINAL = "quickTerminal";
    private static final String GTSR_DIAGNOSTICS = "gtsrDiagnostics";
    private static final String GTSR_RECIPE_SPEED = "gtsrRecipeSpeedCompatibility";
    private static final String PATTERN_DEDUP = "patternDeduplication";
    private static final String TERMINAL_INVENTORY_FIX = "terminalInventoryFix";
    private static final String TERMINAL_CRAFTING_REFRESH = "terminalCraftingRefreshFix";
    private static final String CRAFTING_DIAGNOSTICS = "craftingDiagnostics";
    private static final String MATRIX_PATTERN_LOAD_FIX = "matrixPatternLoadFix";
    private static final String MATRIX_PATTERN_DIAGNOSTICS = "matrixPatternDiagnostics";
    private static final String SMART_INGREDIENTS = "terminalSmartIngredients";
    private static final String NEI_ALWAYS_SHOW_PRESENCE = "terminalNeiAlwaysShowPresence";
    private static final String PATTERN_MAP_PRESERVATION = "patternRecipeMapPreservation";
    private static final String NEO_ECO_CRAFTING_FIX = "neoEcoCraftingRefreshFix";
    private static final String SOUL_VIAL = "soulVialExpandedCapture";
    private static final String STRUCTURELIB_AE_AUTOPLACE = "structureLibAeAutoPlace";

    final boolean ysmuAnimatedArms;
    final boolean lootSimulator;
    final boolean proxyFactory;
    final boolean proxyDispatchDiagnostics;
    final boolean cropBreeder;
    final boolean steamGreenhouse;
    final boolean carpenterRecipes;
    final boolean quickTerminal;
    final boolean gtsrDiagnostics;
    final boolean gtsrRecipeSpeedCompatibility;
    final boolean patternDeduplication;
    final boolean terminalInventoryFix;
    final boolean terminalCraftingRefreshFix;
    final boolean craftingDiagnostics;
    final boolean neoEcoCraftingRefreshFix;
    final boolean patternRecipeMapPreservation;
    final boolean terminalNeiAlwaysShowPresence;
    final boolean terminalSmartIngredients;
    final boolean matrixPatternDiagnostics;
    final boolean matrixPatternLoadFix;
    final boolean soulVialExpandedCapture;
    final boolean structureLibAeAutoPlace;

    private FeatureSwitches(boolean lootSimulator, boolean proxyFactory, boolean cropBreeder, boolean steamGreenhouse,
        boolean carpenterRecipes, boolean quickTerminal, boolean gtsrDiagnostics, boolean gtsrRecipeSpeedCompatibility,
        boolean patternDeduplication, boolean terminalInventoryFix, boolean terminalCraftingRefreshFix,
        boolean craftingDiagnostics, boolean neoEcoCraftingRefreshFix, boolean patternRecipeMapPreservation,
        boolean terminalNeiAlwaysShowPresence, boolean terminalSmartIngredients, boolean matrixPatternDiagnostics,
        boolean matrixPatternLoadFix, boolean soulVialExpandedCapture, boolean structureLibAeAutoPlace,
        boolean proxyDispatchDiagnostics, boolean ysmuAnimatedArms) {
        this.ysmuAnimatedArms = ysmuAnimatedArms;
        this.lootSimulator = lootSimulator;
        this.proxyFactory = proxyFactory;
        this.proxyDispatchDiagnostics = proxyDispatchDiagnostics;
        this.cropBreeder = cropBreeder;
        this.steamGreenhouse = steamGreenhouse;
        this.carpenterRecipes = carpenterRecipes;
        this.quickTerminal = quickTerminal;
        this.gtsrDiagnostics = gtsrDiagnostics;
        this.gtsrRecipeSpeedCompatibility = gtsrRecipeSpeedCompatibility;
        this.patternDeduplication = patternDeduplication;
        this.terminalInventoryFix = terminalInventoryFix;
        this.terminalCraftingRefreshFix = terminalCraftingRefreshFix;
        this.craftingDiagnostics = craftingDiagnostics;
        this.neoEcoCraftingRefreshFix = neoEcoCraftingRefreshFix;
        this.patternRecipeMapPreservation = patternRecipeMapPreservation;
        this.terminalNeiAlwaysShowPresence = terminalNeiAlwaysShowPresence;
        this.terminalSmartIngredients = terminalSmartIngredients;
        this.matrixPatternDiagnostics = matrixPatternDiagnostics;
        this.matrixPatternLoadFix = matrixPatternLoadFix;
        this.soulVialExpandedCapture = soulVialExpandedCapture;
        this.structureLibAeAutoPlace = structureLibAeAutoPlace;
    }

    static FeatureSwitches load(Path file) {
        Properties defaults = new Properties();
        defaults.setProperty(YSMU_ARMS, "true");
        defaults.setProperty(LOOT_SIMULATOR, "true");
        defaults.setProperty(PROXY_FACTORY, "true");
        defaults.setProperty(PROXY_TRACE, "false");
        defaults.setProperty(CROP, "true");
        defaults.setProperty(GREENHOUSE, "true");
        defaults.setProperty(CARPENTER, "true");
        defaults.setProperty(TERMINAL, "true");
        defaults.setProperty(GTSR_DIAGNOSTICS, "true");
        defaults.setProperty(GTSR_RECIPE_SPEED, "true");
        defaults.setProperty(PATTERN_DEDUP, "true");
        defaults.setProperty(TERMINAL_INVENTORY_FIX, "true");
        defaults.setProperty(TERMINAL_CRAFTING_REFRESH, "true");
        defaults.setProperty(CRAFTING_DIAGNOSTICS, "false");
        defaults.setProperty(NEO_ECO_CRAFTING_FIX, "false");
        defaults.setProperty(PATTERN_MAP_PRESERVATION, "true");
        defaults.setProperty(NEI_ALWAYS_SHOW_PRESENCE, "true");
        defaults.setProperty(SMART_INGREDIENTS, "true");
        defaults.setProperty(MATRIX_PATTERN_DIAGNOSTICS, "false");
        defaults.setProperty(MATRIX_PATTERN_LOAD_FIX, "true");
        defaults.setProperty(SOUL_VIAL, "true");
        defaults.setProperty(STRUCTURELIB_AE_AUTOPLACE, "true");
        try {
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) {
                try (OutputStream output = Files.newOutputStream(file, StandardOpenOption.CREATE_NEW)) {
                    defaults.store(
                        output,
                        "GTNH Adjust feature switches. Set false and restart to disable. Use matching settings on clients and servers.");
                } catch (java.nio.file.FileAlreadyExistsException ignored) {
                    // Another loader created it between the existence check and create.
                }
            }
            Properties values = new Properties();
            try (InputStream input = Files.newInputStream(file)) {
                values.load(input);
            }
            FeatureSwitches result = new FeatureSwitches(
                read(values, LOOT_SIMULATOR),
                read(values, PROXY_FACTORY),
                read(values, CROP),
                read(values, GREENHOUSE),
                read(values, CARPENTER),
                read(values, TERMINAL),
                read(values, GTSR_DIAGNOSTICS),
                read(values, GTSR_RECIPE_SPEED),
                read(values, PATTERN_DEDUP),
                read(values, TERMINAL_INVENTORY_FIX),
                read(values, TERMINAL_CRAFTING_REFRESH),
                read(values, CRAFTING_DIAGNOSTICS, false),
                read(values, NEO_ECO_CRAFTING_FIX, false),
                read(values, PATTERN_MAP_PRESERVATION),
                read(values, NEI_ALWAYS_SHOW_PRESENCE),
                read(values, SMART_INGREDIENTS),
                read(values, MATRIX_PATTERN_DIAGNOSTICS, false),
                read(values, MATRIX_PATTERN_LOAD_FIX),
                read(values, SOUL_VIAL),
                read(values, STRUCTURELIB_AE_AUTOPLACE),
                read(values, PROXY_TRACE, false),
                read(values, YSMU_ARMS));
            LOG.info(
                "Feature switches: lootSimulator=" + result.lootSimulator
                    + ", proxyFactory="
                    + result.proxyFactory
                    + ", cropBreeder="
                    + result.cropBreeder
                    + ", steamGreenhouse="
                    + result.steamGreenhouse
                    + ", carpenterRecipes="
                    + result.carpenterRecipes
                    + ", quickTerminal="
                    + result.quickTerminal
                    + ", gtsrDiagnostics="
                    + result.gtsrDiagnostics
                    + ", gtsrRecipeSpeedCompatibility="
                    + result.gtsrRecipeSpeedCompatibility
                    + ", patternDeduplication="
                    + result.patternDeduplication
                    + ", terminalInventoryFix="
                    + result.terminalInventoryFix
                    + ", terminalCraftingRefreshFix="
                    + result.terminalCraftingRefreshFix
                    + ", craftingDiagnostics="
                    + result.craftingDiagnostics
                    + ", neoEcoCraftingRefreshFix="
                    + result.neoEcoCraftingRefreshFix
                    + ", patternRecipeMapPreservation="
                    + result.patternRecipeMapPreservation
                    + ", terminalNeiAlwaysShowPresence="
                    + result.terminalNeiAlwaysShowPresence
                    + ", terminalSmartIngredients="
                    + result.terminalSmartIngredients
                    + ", matrixPatternDiagnostics="
                    + result.matrixPatternDiagnostics
                    + ", matrixPatternLoadFix="
                    + result.matrixPatternLoadFix
                    + ", soulVialExpandedCapture="
                    + result.soulVialExpandedCapture
                    + ", structureLibAeAutoPlace="
                    + result.structureLibAeAutoPlace
                    + ", proxyDispatchDiagnostics="
                    + result.proxyDispatchDiagnostics
                    + ", ysmuAnimatedArms="
                    + result.ysmuAnimatedArms);
            return result;
        } catch (IOException | RuntimeException failure) {
            LOG.warning("Cannot read feature switches from " + file + "; all features are disabled: " + failure);
            return new FeatureSwitches(
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false);
        }
    }

    private static boolean read(Properties values, String key) {
        return read(values, key, true);
    }

    private static boolean read(Properties values, String key, boolean defaultValue) {
        String value = values.getProperty(key, Boolean.toString(defaultValue))
            .trim();
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        LOG.warning("Invalid feature switch " + key + "=" + value + "; feature disabled");
        return false;
    }

    List<String> selectMixins(Set<String> loadedMods, boolean client) {
        List<String> mixins = new ArrayList<>();
        if (client && ysmuAnimatedArms && loadedMods.contains("gtnrlib") && loadedMods.contains("ysmu")) {
            mixins.add("animation.YsmuHandTransformMixin");
            mixins.add("animation.GtnrAnimatedArmMixin");
        }
        if (proxyFactory && proxyDispatchDiagnostics
            && loadedMods.contains("gtneverreset")
            && loadedMods.contains("appliedenergistics2")) {
            mixins.add("diagnostic.ProxyCpuDispatchAuditMixin");
            mixins.add("diagnostic.ProxyHatchDispatchAuditMixin");
            mixins.add("diagnostic.ProxyCpuReceiveAuditMixin");
            mixins.add("diagnostic.ProxyCircuitSlotAuditMixin");
        }
        if (structureLibAeAutoPlace && loadedMods.contains("structurelib")
            && loadedMods.contains("appliedenergistics2")) mixins.add("structurelib.StructureLibAeAutoPlaceMixin");
        if (soulVialExpandedCapture && loadedMods.contains("EnderIO")) mixins.add("fix.SoulVialCaptureMixin");
        if (cropBreeder && loadedMods.contains("gtneverreset")) {
            mixins.add("CropBreedingRulesMixin");
            mixins.add("LargeCropBreederMixin");
        }
        if (quickTerminal && loadedMods.contains("gtneverreset")) {
            mixins.add("terminal.RecipeTransferPayloadMixin");
            mixins.add("terminal.QuickTerminalContainerMixin");
            if (client) {
                mixins.add("terminal.QuickTerminalGuiMixin");
                mixins.add("terminal.BookmarkEncodeClickMixin");
                mixins.add("terminal.InterfaceTerminalSettingsAccessor");
                mixins.add("terminal.QuickTerminalSettingsMixin");
                mixins.add("terminal.QuickTerminalRecipeMixin");
                mixins.add("terminal.TerminalIngredientsMixin");
            }
        }
        if (client && quickTerminal && terminalSmartIngredients && loadedMods.contains("gtneverreset"))
            mixins.add("terminal.SmartIngredientTransferMixin");
        if (steamGreenhouse && loadedMods.contains("sciencenotleisure") && loadedMods.contains("cropsnh")) {
            mixins.add("gtnl.SteamGreenhouseParallelMixin");
            if (client) mixins.add("gtnl.SteamGreenhouseGuiMixin");
        }
        if (gtsrDiagnostics && loadedMods.contains("gtsr")) mixins.add("diagnostic.GtsrRecipeAuditMixin");
        if (gtsrRecipeSpeedCompatibility && loadedMods.contains("gtsr") && loadedMods.contains("gtneverreset")) {
            mixins.add("fix.GtsrRegistrationScopeMixin");
            mixins.add("fix.GtsrRecipeMapSpeedExclusionMixin");
        }
        if (patternDeduplication && loadedMods.contains("gtneverreset"))
            mixins.add("terminal.TerminalPatternDedupMixin");
        if (terminalCraftingRefreshFix && loadedMods.contains("gtneverreset"))
            mixins.add("fix.QuickTerminalCraftingRefreshMixin");
        if (patternRecipeMapPreservation && loadedMods.contains("gtneverreset"))
            mixins.add("fix.PatternRecipeMapPreservationMixin");
        if (neoEcoCraftingRefreshFix && loadedMods.contains("neoecoae") && loadedMods.contains("appliedenergistics2"))
            mixins.add("fix.NeoEcoCraftingRefreshMixin");
        if (matrixPatternLoadFix && loadedMods.contains("gtneverreset")) {
            mixins.add("fix.MatrixPatternDecodeMixin");
            mixins.add("fix.MatrixPatternRetryMixin");
        }
        if (matrixPatternDiagnostics && loadedMods.contains("gtneverreset")) {
            mixins.add("diagnostic.MatrixInventoryAuditMixin");
            mixins.add("diagnostic.MatrixPatternAuditMixin");
            mixins.add("diagnostic.MatrixTickAuditMixin");
        }
        if (craftingDiagnostics && loadedMods.contains("gtneverreset")) {
            mixins.add("diagnostic.FactoryProcessingAuditMixin");
            mixins.add("diagnostic.TerminalCraftingAuditMixin");
            mixins.add("diagnostic.CraftingGridAuditMixin");
            mixins.add("diagnostic.NetworkMonitorAuditMixin");
            mixins.add("diagnostic.PatternBufferAuditMixin");
            mixins.add("diagnostic.CircuitBufferAuditMixin");
            mixins.add("diagnostic.MonitorContainerAuditMixin");
            if (client) mixins.add("diagnostic.InventoryPacketAuditMixin");
        }
        if (client && terminalNeiAlwaysShowPresence
            && terminalInventoryFix
            && loadedMods.contains("gtneverreset")
            && loadedMods.contains("neenergistics")) {
            mixins.add("terminal.NeiOverlayButtonAccessor");
            mixins.add("terminal.NeiOverlayTooltipMixin");
            mixins.add("terminal.NeiAlwaysShowPresenceMixin");
        }
        if (client && terminalInventoryFix && loadedMods.contains("gtneverreset")) {
            mixins.add("fix.QuickTerminalInventoryPacketMixin");
            mixins.add("fix.QuickTerminalRecipeScreenMixin");
            if (loadedMods.contains("neenergistics")) mixins.add("fix.QuickTerminalPresenceMixin");
        }
        return mixins;
    }
}
