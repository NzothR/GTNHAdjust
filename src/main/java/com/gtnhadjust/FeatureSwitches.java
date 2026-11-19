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
    private static final String HOLOGRAM = "hologramImprovements";
    private static final String GTNL_CRAFT_FIX = "gtnlQuickTerminalCraftFix";
    private static final String GRAND_MAINTENANCE = "grandAssemblyAutoMaintenance";
    private static final String QUEST_AE = "questAeRetrieval";
    private static final String YSMU_ARMS = "ysmuAnimatedArms";
    private static final String LOOT_SIMULATOR = "lootSimulator";
    private static final String PROXY_FACTORY = "proxyFactory";
    private static final String THREADED_PROXY_AE_OUTPUT = "threadedProxyAeOutput";
    private static final String THREADED_PROXY = "threadedJointProxy";
    private static final String HIDE_SHIMMER = "hideGtnlShimmerRecipes";
    private static final String HIDE_DISASSEMBLER = "hideGtnrDisassemblerRecipes";
    private static final String PROXY_TRACE = "proxyDispatchDiagnostics";
    private static final String CROP = "cropBreeder";
    private static final String GREENHOUSE = "steamGreenhouse";
    private static final String OFFHAND_PICKUP = "offhandPickupFix";
    private static final String MANA_CONVERSION = "manaConversionRecipes";
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
    private static final String AE2THING_TERMINAL_RECIPE_FIX = "ae2thingTerminalRecipeFix";
    private static final String AE2THING_INFINITY_CELL_USAGE_FIX = "ae2thingInfinityCellUsageFix";
    private static final String PLANETARY_ENGINE_PROJECTOR = "planetaryEngineProjector";
    private static final String ANTIMATTER_CYCLE_INTERVAL = "antimatterCycleInterval";
    private static final String ANTIMATTER_WIRELESS = "antimatterWireless";
    private static final String COMPUTER_CRYOTHEUM = "othComputerCryotheum";
    private static final String SOUL_PRISON_BUS_MARKER = "soulPrisonBusMarker";
    private static final String PROJECTOR_DISMANTLE = "projectorDismantle";

    boolean substanceReshapingDevice;
    boolean godforgeFreeUpgrades;
    boolean bookmarkMachineGroups;
    boolean gtnrVirtualTools;
    final boolean hologramImprovements;
    final boolean gtnlQuickTerminalCraftFix;
    final boolean grandAssemblyAutoMaintenance;
    final boolean questAeRetrieval;
    final boolean ysmuAnimatedArms;
    final boolean lootSimulator;
    final boolean proxyFactory;
    final boolean threadedJointProxy;
    final boolean threadedProxyAeOutput;
    final boolean hideGtnlShimmerRecipes;
    final boolean hideGtnrDisassemblerRecipes;
    final boolean proxyDispatchDiagnostics;
    final boolean cropBreeder;
    final boolean steamGreenhouse;
    final boolean carpenterRecipes;
    final boolean manaConversionRecipes;
    final boolean offhandPickupFix;
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
    final boolean ae2thingTerminalRecipeFix;
    final boolean ae2thingInfinityCellUsageFix;
    final boolean planetaryEngineProjector;
    final boolean projectorDismantle;
    final boolean soulPrisonBusMarker;
    final boolean othComputerCryotheum;
    final boolean antimatterWireless;
    final boolean antimatterCycleInterval;

    private FeatureSwitches(boolean lootSimulator, boolean proxyFactory, boolean cropBreeder, boolean steamGreenhouse,
        boolean carpenterRecipes, boolean quickTerminal, boolean gtsrDiagnostics, boolean gtsrRecipeSpeedCompatibility,
        boolean patternDeduplication, boolean terminalInventoryFix, boolean terminalCraftingRefreshFix,
        boolean craftingDiagnostics, boolean neoEcoCraftingRefreshFix, boolean patternRecipeMapPreservation,
        boolean terminalNeiAlwaysShowPresence, boolean terminalSmartIngredients, boolean matrixPatternDiagnostics,
        boolean matrixPatternLoadFix, boolean soulVialExpandedCapture, boolean structureLibAeAutoPlace,
        boolean proxyDispatchDiagnostics, boolean ysmuAnimatedArms, boolean hologramImprovements,
        boolean questAeRetrieval, boolean gtnlQuickTerminalCraftFix, boolean grandAssemblyAutoMaintenance,
        boolean threadedJointProxy, boolean hideGtnlShimmerRecipes, boolean hideGtnrDisassemblerRecipes,
        boolean ae2thingTerminalRecipeFix, boolean planetaryEngineProjector, boolean projectorDismantle,
        boolean ae2thingInfinityCellUsageFix, boolean antimatterWireless, boolean antimatterCycleInterval,
        boolean soulPrisonBusMarker, boolean manaConversionRecipes, boolean offhandPickupFix,
        boolean threadedProxyAeOutput, boolean othComputerCryotheum) {
        this.ae2thingInfinityCellUsageFix = ae2thingInfinityCellUsageFix;
        this.projectorDismantle = projectorDismantle;
        this.soulPrisonBusMarker = soulPrisonBusMarker;
        this.othComputerCryotheum = othComputerCryotheum;
        this.antimatterWireless = antimatterWireless;
        this.antimatterCycleInterval = antimatterCycleInterval;
        this.planetaryEngineProjector = planetaryEngineProjector;
        this.ae2thingTerminalRecipeFix = ae2thingTerminalRecipeFix;
        this.hideGtnlShimmerRecipes = hideGtnlShimmerRecipes;
        this.hideGtnrDisassemblerRecipes = hideGtnrDisassemblerRecipes;
        this.threadedJointProxy = threadedJointProxy;
        this.threadedProxyAeOutput = threadedProxyAeOutput;
        this.grandAssemblyAutoMaintenance = grandAssemblyAutoMaintenance;
        this.gtnlQuickTerminalCraftFix = gtnlQuickTerminalCraftFix;
        this.questAeRetrieval = questAeRetrieval;
        this.hologramImprovements = hologramImprovements;
        this.ysmuAnimatedArms = ysmuAnimatedArms;
        this.lootSimulator = lootSimulator;
        this.proxyFactory = proxyFactory;
        this.proxyDispatchDiagnostics = proxyDispatchDiagnostics;
        this.cropBreeder = cropBreeder;
        this.steamGreenhouse = steamGreenhouse;
        this.carpenterRecipes = carpenterRecipes;
        this.manaConversionRecipes = manaConversionRecipes;
        this.offhandPickupFix = offhandPickupFix;
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
        defaults.setProperty("substanceReshapingDevice", "true");
        defaults.setProperty("godforgeFreeUpgrades", "true");
        defaults.setProperty("bookmarkMachineGroups", "true");
        defaults.setProperty("gtnrVirtualTools", "true");
        defaults.setProperty(HOLOGRAM, "true");
        defaults.setProperty(YSMU_ARMS, "true");
        defaults.setProperty(LOOT_SIMULATOR, "true");
        defaults.setProperty(PROXY_FACTORY, "true");
        defaults.setProperty(THREADED_PROXY_AE_OUTPUT, "true");
        defaults.setProperty(THREADED_PROXY, "true");
        defaults.setProperty(HIDE_SHIMMER, "true");
        defaults.setProperty(HIDE_DISASSEMBLER, "true");
        defaults.setProperty(PROXY_TRACE, "false");
        defaults.setProperty(CROP, "true");
        defaults.setProperty(QUEST_AE, "true");
        defaults.setProperty(GRAND_MAINTENANCE, "true");
        defaults.setProperty(GTNL_CRAFT_FIX, "true");
        defaults.setProperty(GREENHOUSE, "true");
        defaults.setProperty(OFFHAND_PICKUP, "true");
        defaults.setProperty(MANA_CONVERSION, "true");
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
        defaults.setProperty(AE2THING_TERMINAL_RECIPE_FIX, "true");
        defaults.setProperty(AE2THING_INFINITY_CELL_USAGE_FIX, "true");
        defaults.setProperty(PLANETARY_ENGINE_PROJECTOR, "true");
        defaults.setProperty(ANTIMATTER_CYCLE_INTERVAL, "true");
        defaults.setProperty(ANTIMATTER_WIRELESS, "true");
        defaults.setProperty(COMPUTER_CRYOTHEUM, "true");
        defaults.setProperty(SOUL_PRISON_BUS_MARKER, "true");
        defaults.setProperty(PROJECTOR_DISMANTLE, "true");
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
                read(values, YSMU_ARMS),
                read(values, HOLOGRAM),
                read(values, QUEST_AE),
                read(values, GTNL_CRAFT_FIX),
                read(values, GRAND_MAINTENANCE),
                read(values, THREADED_PROXY),
                read(values, HIDE_SHIMMER),
                read(values, HIDE_DISASSEMBLER),
                read(values, AE2THING_TERMINAL_RECIPE_FIX),
                read(values, PLANETARY_ENGINE_PROJECTOR),
                read(values, PROJECTOR_DISMANTLE),
                read(values, AE2THING_INFINITY_CELL_USAGE_FIX),
                read(values, ANTIMATTER_WIRELESS),
                read(values, ANTIMATTER_CYCLE_INTERVAL),
                read(values, SOUL_PRISON_BUS_MARKER),
                read(values, MANA_CONVERSION),
                read(values, OFFHAND_PICKUP),
                read(values, THREADED_PROXY_AE_OUTPUT),
                read(values, COMPUTER_CRYOTHEUM));
            result.substanceReshapingDevice = read(values, "substanceReshapingDevice");
            result.godforgeFreeUpgrades = read(values, "godforgeFreeUpgrades");
            result.bookmarkMachineGroups = read(values, "bookmarkMachineGroups");
            result.gtnrVirtualTools = read(values, "gtnrVirtualTools");
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
                    + result.ysmuAnimatedArms
                    + ", hologramImprovements="
                    + result.hologramImprovements
                    + ", gtnlQuickTerminalCraftFix="
                    + result.gtnlQuickTerminalCraftFix
                    + ", threadedJointProxy="
                    + result.threadedJointProxy
                    + ", hideGtnlShimmerRecipes="
                    + result.hideGtnlShimmerRecipes
                    + ", hideGtnrDisassemblerRecipes="
                    + result.hideGtnrDisassemblerRecipes
                    + ", ae2thingTerminalRecipeFix="
                    + result.ae2thingTerminalRecipeFix
                    + ", planetaryEngineProjector="
                    + result.planetaryEngineProjector
                    + ", projectorDismantle="
                    + result.projectorDismantle
                    + ", ae2thingInfinityCellUsageFix="
                    + result.ae2thingInfinityCellUsageFix);
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
        if (threadedJointProxy && threadedProxyAeOutput
            && loadedMods.contains("gtneverreset")
            && loadedMods.contains("gregtech")
            && loadedMods.contains("appliedenergistics2")) {
            mixins.add("gtnr.ThreadedProxyOutputAccessor");
        }
        if (antimatterCycleInterval && loadedMods.contains("GoodGenerator")) {
            mixins.add("goodgenerator.AntimatterCycleMixin");
        }
        if (antimatterWireless && loadedMods.contains("GoodGenerator")) {
            mixins.add("goodgenerator.AntimatterWirelessMixin");
        }
        if (loadedMods.contains("gregtech")
            && (loadedMods.contains("123technology") || loadedMods.contains("123Technology"))) {
            if (soulPrisonBusMarker) mixins.add("fix.SoulPrisonBusMarkerMixin");
            if (othComputerCryotheum) mixins.add("fix.OthComputerCryotheumMixin");
        }
        if (projectorDismantle && loadedMods.contains("structurelib")) {
            mixins.add("structurelib.ProjectorDismantleMixin");
            mixins.add("structurelib.ProjectorDismantleWalkerMixin");
            if (loadedMods.contains("gregtech")) mixins.add("structurelib.ProjectorHatchDropsMixin");
        }
        if (planetaryEngineProjector && loadedMods.contains("sciencenotleisure")
            && loadedMods.contains("structurelib")
            && loadedMods.contains("gregtech")) {
            mixins.add("gtnl.PlanetaryEngineStructureMixin");
            mixins.add("gtnl.PlanetaryEngineProjectorMixin");
            if (client) mixins.add("gtnl.PlanetaryEnginePreviewMixin");
        }
        if (gtnrVirtualTools && loadedMods.contains("gtneverreset")
            && loadedMods.contains("gregtech")
            && loadedMods.contains("appliedenergistics2")) {
            mixins.add("gtnr.VirtualToolCatalogMixin");
            mixins.add("gtnr.PatternToolResolverMixin");
            mixins.add("gtnr.PatternToolsMixin");
            mixins.add("gtnr.PatternHostAccessor");
            mixins.add("gtnr.RequiredToolsMixin");
            mixins.add("gtnr.JointFactoryToolsMixin");
            if (client) mixins.add("gtnr.NonConsumedImportMixin");
        }
        if (client && bookmarkMachineGroups
            && loadedMods.contains("NotEnoughItems")
            && loadedMods.contains("gregtech")) {
            mixins.add("terminal.BookmarkGridAccessor");
            mixins.add("terminal.BookmarkMachineKeysMixin");
            mixins.add("terminal.BookmarkStructureCaptureMixin");
        }
        if (client && loadedMods.contains("gregtech")) {
            if (hideGtnlShimmerRecipes && loadedMods.contains("sciencenotleisure"))
                mixins.add("nei.HideGtnlShimmerRecipesMixin");
            if (hideGtnrDisassemblerRecipes && loadedMods.contains("gtneverreset"))
                mixins.add("nei.HideGtnrDisassemblerRecipesMixin");
        }
        if (godforgeFreeUpgrades && loadedMods.contains("gregtech")) {
            mixins.add("godforge.FreeUpgradesMixin");
            mixins.add("godforge.FreeUpgradeMaterialsMixin");
        }
        if (grandAssemblyAutoMaintenance && loadedMods.contains("sciencenotleisure"))
            mixins.add("gtnl.GrandAssemblyMaintenanceMixin");
        if (gtnlQuickTerminalCraftFix && loadedMods.contains("sciencenotleisure")
            && loadedMods.contains("gtneverreset")
            && loadedMods.contains("appliedenergistics2")) {
            mixins.add("gtnl.GtnlQuickTerminalCraftMixin");
        }
        if (questAeRetrieval && loadedMods.contains("betterquesting") && loadedMods.contains("appliedenergistics2")) {
            mixins.add("quest.TaskRetrievalMixin");
            if (client) mixins.add("quest.QuestScreenMixin");
        }
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
        if (hologramImprovements && loadedMods.contains("structurelib") && loadedMods.contains("gregtech")) {
            mixins.add("structurelib.HologramConstructMixin");
            mixins.add("structurelib.HologramWalkerMixin");
        }
        if (hologramImprovements && structureLibAeAutoPlace
            && loadedMods.contains("structurelib")
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
