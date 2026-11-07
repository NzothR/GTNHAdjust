package com.gtnhadjust;

import net.minecraftforge.common.config.Configuration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gtnhadjust.machine.collector.SuperFragmentRecipes;
import com.gtnhadjust.machine.collector.SuperFragmentRegistration;
import com.gtnhadjust.machine.collector.TierDimensionItem;
import com.gtnhadjust.machine.quantum.SmallQuantumRecipes;
import com.gtnhadjust.machine.quantum.SmallQuantumRegistration;
import com.gtnhadjust.recipe.CarpenterRecipes;
import com.gtnhadjust.terminal.TerminalNotifications;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;

@Mod(
    modid = GTNHAdjust.MOD_ID,
    name = "GTNH Adjust",
    version = Tags.VERSION,
    acceptedMinecraftVersions = "[1.7.10]",
    dependencies = "after:Forestry;after:gregtech;after:gtneverreset")
public class GTNHAdjust {

    public static final String MOD_ID = "gtnhadjust";
    public static final Logger LOG = LogManager.getLogger(MOD_ID);
    public static TierDimensionItem tierDimension;
    public static boolean debug;
    public static boolean singularityResourceRecipes;
    public static boolean collectorBlackHole;
    public static int collectorRecipeTicks = 128;
    public static int collectorBaseParallel = 64;
    public static int collectorBaseThreads = 8;
    public static int collectorVoltageParallelMultiplier = 2;
    public static int collectorVoltageThreads = 4;
    public static boolean terminalDebug;
    public static boolean patternDeduplication;
    public static int steamGreenhouseMaxSeeds = 64;
    public static double smallQuantumRecipeTimeMultiplier = 0.1;
    private int superFragmentMteId = 31990;
    private int smallQuantumMteId = 31991;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        patternDeduplication = FeatureConfig.get().patternDeduplication;
        if (FeatureConfig.get().quickTerminal || patternDeduplication) TerminalNotifications.init();
        Configuration config = new Configuration(event.getSuggestedConfigurationFile());
        debug = config.getBoolean(
            "debug",
            Configuration.CATEGORY_GENERAL,
            false,
            "Log detailed diagnostics for supported machines and recipe registration.");
        singularityResourceRecipes = config.getBoolean(
            "singularityResourceRecipes",
            "collector",
            true,
            "Register GT-Not-Hard resource pools in the super fragment collector. Restart required; client and server should match.");
        terminalDebug = config
            .getBoolean("debug", "quickTerminal", false, "Log quick terminal recipe transfers and automatic uploads.");
        steamGreenhouseMaxSeeds = config.getInt(
            "steamGreenhouseMaxSeeds",
            "gtnl",
            64,
            1,
            64,
            "Steam greenhouse maximum seed count; server setting, restart required. Remove excess seeds before lowering.");
        collectorRecipeTicks = config.getInt(
            "recipeDurationTicks",
            "collector",
            128,
            1,
            1000000,
            "Collector recipe base duration in ticks (20 ticks = 1 second). Restart required; client and server must match.");
        collectorBaseParallel = config.getInt(
            "baseParallel",
            "collector",
            64,
            1,
            1000000,
            "Parallel limit per thread at LV voltage and HV glass. Restart required; client and server must match.");
        collectorBaseThreads = config.getInt(
            "baseThreads",
            "collector",
            8,
            1,
            256,
            "Thread limit at LV voltage and HV glass, capped at 256. Restart required; client and server must match.");
        collectorVoltageParallelMultiplier = config.getInt(
            "voltageParallelMultiplier",
            "collector",
            2,
            1,
            16,
            "Multiply parallel limit by this for every voltage tier above LV. Restart required; client and server must match.");
        collectorVoltageThreads = config.getInt(
            "voltageThreadsPerTier",
            "collector",
            4,
            0,
            256,
            "Additional threads per voltage tier above LV, capped at 256 total. Restart required; client and server must match.");
        collectorBlackHole = config.getBoolean(
            "blackHoleRendering",
            "collector",
            true,
            "Render the GregTech black hole in the formed super fragment collector. Server setting; restart required.");
        superFragmentMteId = config.getInt(
            "superFragmentMteId",
            "collector",
            31990,
            2048,
            32766,
            "Stable GregTech machine ID for the super fragment collector. Do not change after placing or obtaining it; client and server must match.");
        smallQuantumMteId = config.getInt(
            "smallQuantumMteId",
            "quantum",
            31991,
            2048,
            32766,
            "Stable machine ID for the small quantum manipulator. Do not change after obtaining or placing it; client and server must match.");
        smallQuantumRecipeTimeMultiplier = config.get(
            "quantum",
            "recipeTimeMultiplier",
            0.1,
            "Multiplier applied to reduced small quantum recipes before coil bonuses and overclocking; minimum one tick. Restart required; client and server must match.",
            0.000001,
            100.0)
            .getDouble(0.1);
        if (!Double.isFinite(smallQuantumRecipeTimeMultiplier) || smallQuantumRecipeTimeMultiplier < 0.000001
            || smallQuantumRecipeTimeMultiplier > 100.0) {
            LOG.warn("Invalid quantum recipe time multiplier {}; using 0.1", smallQuantumRecipeTimeMultiplier);
            smallQuantumRecipeTimeMultiplier = 0.1;
        }
        if (config.hasChanged()) config.save();
        tierDimension = new TierDimensionItem();
        GameRegistry.registerItem(tierDimension, "tier_dimension");
        if (event.getSide()
            .isClient()) tierDimension.registerRenderer();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (FeatureConfig.get().smallQuantumMachine && Loader.isModLoaded("gregtech")
            && Loader.isModLoaded("Avaritia")) {
            SmallQuantumRegistration.registerMachine(smallQuantumMteId);
            SmallQuantumRecipes.initialize();
        }
        if (Loader.isModLoaded("gtneverreset") && Loader.isModLoaded("gregtech")) {
            SuperFragmentRegistration.registerMachine(superFragmentMteId);
            SuperFragmentRecipes.initialize();
            SuperFragmentRegistration.registerCrafting();
        }
    }

    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        if (FeatureConfig.get().smallQuantumMachine && Loader.isModLoaded("gregtech")
            && Loader.isModLoaded("Avaritia")) {
            SmallQuantumRegistration.registerCrafting();
            SmallQuantumRecipes.register();
        }
        if (FeatureConfig.get().carpenterRecipes && Loader.isModLoaded("Forestry") && Loader.isModLoaded("gregtech"))
            CarpenterRecipes.register();
        if (Loader.isModLoaded("gtneverreset") && Loader.isModLoaded("gregtech")) {
            SuperFragmentRecipes.register();
        }
    }
}
