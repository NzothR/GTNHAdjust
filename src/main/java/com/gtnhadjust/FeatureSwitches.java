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
    private static final String CRUSHER_FIXED_LOOTING = "crusherFixedLooting";
    private static final String CRUSHER_EQUIPMENT_VOID = "crusherEquipmentVoid";
    private static final String CROP = "cropBreeder";
    private static final String GREENHOUSE = "steamGreenhouse";
    private static final String CARPENTER = "carpenterRecipes";
    private static final String TERMINAL = "quickTerminal";
    private static final String GTSR_DIAGNOSTICS = "gtsrDiagnostics";
    private static final String GTSR_RECIPE_SPEED = "gtsrRecipeSpeedCompatibility";
    private static final String PATTERN_DEDUP = "patternDeduplication";
    private static final String TERMINAL_INVENTORY_FIX = "terminalInventoryFix";
    private static final String SMALL_QUANTUM = "smallQuantumMachine";
    private static final String TERMINAL_CRAFTING_REFRESH = "terminalCraftingRefreshFix";
    private static final String CRAFTING_DIAGNOSTICS = "craftingDiagnostics";
    private static final String MATRIX_PATTERN_LOAD_FIX = "matrixPatternLoadFix";
    private static final String MATRIX_PATTERN_DIAGNOSTICS = "matrixPatternDiagnostics";
    private static final String SMART_INGREDIENTS = "terminalSmartIngredients";
    private static final String NEI_ALWAYS_SHOW_PRESENCE = "terminalNeiAlwaysShowPresence";
    private static final String PATTERN_MAP_PRESERVATION = "patternRecipeMapPreservation";
    private static final String NEO_ECO_CRAFTING_FIX = "neoEcoCraftingRefreshFix";
    private static final String WAND_CAPTURE = "wandCaptureCompatibility";

    final boolean crusherFixedLooting;
    final boolean crusherEquipmentVoid;
    final boolean cropBreeder;
    final boolean steamGreenhouse;
    final boolean carpenterRecipes;
    final boolean quickTerminal;
    final boolean gtsrDiagnostics;
    final boolean gtsrRecipeSpeedCompatibility;
    final boolean patternDeduplication;
    final boolean terminalInventoryFix;
    final boolean smallQuantumMachine;
    final boolean terminalCraftingRefreshFix;
    final boolean craftingDiagnostics;
    final boolean neoEcoCraftingRefreshFix;
    final boolean patternRecipeMapPreservation;
    final boolean terminalNeiAlwaysShowPresence;
    final boolean terminalSmartIngredients;
    final boolean matrixPatternDiagnostics;
    final boolean matrixPatternLoadFix;
    final boolean wandCaptureCompatibility;

    private FeatureSwitches(boolean crusherFixedLooting, boolean crusherEquipmentVoid, boolean cropBreeder,
        boolean steamGreenhouse, boolean carpenterRecipes, boolean quickTerminal, boolean gtsrDiagnostics,
        boolean gtsrRecipeSpeedCompatibility, boolean patternDeduplication, boolean terminalInventoryFix,
        boolean smallQuantumMachine, boolean terminalCraftingRefreshFix, boolean craftingDiagnostics,
        boolean neoEcoCraftingRefreshFix, boolean patternRecipeMapPreservation, boolean terminalNeiAlwaysShowPresence,
        boolean terminalSmartIngredients, boolean matrixPatternDiagnostics, boolean matrixPatternLoadFix,
        boolean wandCaptureCompatibility) {
        this.crusherFixedLooting = crusherFixedLooting;
        this.crusherEquipmentVoid = crusherEquipmentVoid;
        this.cropBreeder = cropBreeder;
        this.steamGreenhouse = steamGreenhouse;
        this.carpenterRecipes = carpenterRecipes;
        this.quickTerminal = quickTerminal;
        this.gtsrDiagnostics = gtsrDiagnostics;
        this.gtsrRecipeSpeedCompatibility = gtsrRecipeSpeedCompatibility;
        this.patternDeduplication = patternDeduplication;
        this.terminalInventoryFix = terminalInventoryFix;
        this.smallQuantumMachine = smallQuantumMachine;
        this.terminalCraftingRefreshFix = terminalCraftingRefreshFix;
        this.craftingDiagnostics = craftingDiagnostics;
        this.neoEcoCraftingRefreshFix = neoEcoCraftingRefreshFix;
        this.patternRecipeMapPreservation = patternRecipeMapPreservation;
        this.terminalNeiAlwaysShowPresence = terminalNeiAlwaysShowPresence;
        this.terminalSmartIngredients = terminalSmartIngredients;
        this.matrixPatternDiagnostics = matrixPatternDiagnostics;
        this.matrixPatternLoadFix = matrixPatternLoadFix;
        this.wandCaptureCompatibility = wandCaptureCompatibility;
    }

    static FeatureSwitches load(Path file) {
        Properties defaults = new Properties();
        defaults.setProperty(CRUSHER_FIXED_LOOTING, "true");
        defaults.setProperty(CRUSHER_EQUIPMENT_VOID, "true");
        defaults.setProperty(CROP, "true");
        defaults.setProperty(GREENHOUSE, "true");
        defaults.setProperty(CARPENTER, "true");
        defaults.setProperty(TERMINAL, "true");
        defaults.setProperty(GTSR_DIAGNOSTICS, "true");
        defaults.setProperty(GTSR_RECIPE_SPEED, "true");
        defaults.setProperty(PATTERN_DEDUP, "true");
        defaults.setProperty(TERMINAL_INVENTORY_FIX, "true");
        defaults.setProperty(SMALL_QUANTUM, "true");
        defaults.setProperty(TERMINAL_CRAFTING_REFRESH, "true");
        defaults.setProperty(CRAFTING_DIAGNOSTICS, "false");
        defaults.setProperty(NEO_ECO_CRAFTING_FIX, "false");
        defaults.setProperty(PATTERN_MAP_PRESERVATION, "true");
        defaults.setProperty(NEI_ALWAYS_SHOW_PRESENCE, "true");
        defaults.setProperty(SMART_INGREDIENTS, "true");
        defaults.setProperty(MATRIX_PATTERN_DIAGNOSTICS, "false");
        defaults.setProperty(MATRIX_PATTERN_LOAD_FIX, "true");
        defaults.setProperty(WAND_CAPTURE, "true");
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
                read(values, CRUSHER_FIXED_LOOTING),
                read(values, CRUSHER_EQUIPMENT_VOID),
                read(values, CROP),
                read(values, GREENHOUSE),
                read(values, CARPENTER),
                read(values, TERMINAL),
                read(values, GTSR_DIAGNOSTICS),
                read(values, GTSR_RECIPE_SPEED),
                read(values, PATTERN_DEDUP),
                read(values, TERMINAL_INVENTORY_FIX),
                read(values, SMALL_QUANTUM),
                read(values, TERMINAL_CRAFTING_REFRESH),
                read(values, CRAFTING_DIAGNOSTICS, false),
                read(values, NEO_ECO_CRAFTING_FIX, false),
                read(values, PATTERN_MAP_PRESERVATION),
                read(values, NEI_ALWAYS_SHOW_PRESENCE),
                read(values, SMART_INGREDIENTS),
                read(values, MATRIX_PATTERN_DIAGNOSTICS, false),
                read(values, MATRIX_PATTERN_LOAD_FIX),
                read(values, WAND_CAPTURE));
            LOG.info(
                "Feature switches: crusherFixedLooting=" + result.crusherFixedLooting
                    + ", crusherEquipmentVoid="
                    + result.crusherEquipmentVoid
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
                    + ", smallQuantumMachine="
                    + result.smallQuantumMachine
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
                    + ", wandCaptureCompatibility="
                    + result.wandCaptureCompatibility);
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
        if (wandCaptureCompatibility && loadedMods.contains("gtneverreset"))
            mixins.add("fix.UniversalWandCaptureMixin");
        if (crusherFixedLooting && loadedMods.contains("kubatech") && loadedMods.contains("mobsinfo"))
            mixins.add("fix.IndustrialCrusherLootingMixin");
        if (crusherEquipmentVoid && loadedMods.contains("kubatech") && loadedMods.contains("mobsinfo"))
            mixins.add("fix.IndustrialCrusherEquipmentMixin");
        if (cropBreeder && loadedMods.contains("gtneverreset")) {
            mixins.add("CropBreedingRulesMixin");
            mixins.add("LargeCropBreederMixin");
        }
        if (quickTerminal && loadedMods.contains("gtneverreset")) {
            mixins.add("terminal.RecipeTransferPayloadMixin");
            mixins.add("terminal.QuickTerminalContainerMixin");
            if (client) {
                mixins.add("terminal.QuickTerminalGuiMixin");
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
