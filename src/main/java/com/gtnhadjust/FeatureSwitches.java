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

    private FeatureSwitches(boolean crusherFixedLooting, boolean crusherEquipmentVoid, boolean cropBreeder, boolean steamGreenhouse,
        boolean carpenterRecipes, boolean quickTerminal, boolean gtsrDiagnostics, boolean gtsrRecipeSpeedCompatibility,
        boolean patternDeduplication, boolean terminalInventoryFix, boolean smallQuantumMachine) {
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
                read(values, SMALL_QUANTUM));
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
                    + result.smallQuantumMachine);
            return result;
        } catch (IOException | RuntimeException failure) {
            LOG.warning("Cannot read feature switches from " + file + "; all features are disabled: " + failure);
            return new FeatureSwitches(false, false, false, false, false, false, false, false, false, false, false);
        }
    }

    private static boolean read(Properties values, String key) {
        String value = values.getProperty(key, "true")
            .trim();
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        LOG.warning("Invalid feature switch " + key + "=" + value + "; feature disabled");
        return false;
    }

    List<String> selectMixins(Set<String> loadedMods, boolean client) {
        List<String> mixins = new ArrayList<>();
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
        if (client && terminalInventoryFix && loadedMods.contains("gtneverreset")) {
            mixins.add("fix.QuickTerminalInventoryPacketMixin");
            mixins.add("fix.QuickTerminalRecipeScreenMixin");
            if (loadedMods.contains("neenergistics")) mixins.add("fix.QuickTerminalPresenceMixin");
        }
        return mixins;
    }
}
