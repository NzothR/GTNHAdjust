package com.gtnhadjust;

import net.minecraftforge.common.config.Configuration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gtnhadjust.collector.SuperFragmentRecipes;
import com.gtnhadjust.collector.SuperFragmentRegistration;
import com.gtnhadjust.quantum.SmallQuantumRecipes;
import com.gtnhadjust.quantum.SmallQuantumRegistration;
import com.gtnhadjust.recipe.CarpenterRecipes;
import com.gtnhadjust.terminal.TerminalNotifications;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(
    modid = GTNHAdjust.MOD_ID,
    name = "GTNH Adjust",
    version = Tags.VERSION,
    acceptedMinecraftVersions = "[1.7.10]",
    dependencies = "after:Forestry;after:gregtech;after:gtneverreset")
public class GTNHAdjust {

    public static final String MOD_ID = "gtnhadjust";
    public static final Logger LOG = LogManager.getLogger(MOD_ID);
    public static boolean debug;
    public static boolean terminalDebug;
    public static boolean patternDeduplication;
    public static int steamGreenhouseMaxSeeds = 64;
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
        terminalDebug = config
            .getBoolean("debug", "quickTerminal", false, "Log quick terminal recipe transfers and automatic uploads.");
        steamGreenhouseMaxSeeds = config.getInt(
            "steamGreenhouseMaxSeeds",
            "gtnl",
            64,
            1,
            64,
            "Steam greenhouse maximum seed count; server setting, restart required. Remove excess seeds before lowering.");
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
        if (config.hasChanged()) config.save();
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
        if (Loader.isModLoaded("gtneverreset") && Loader.isModLoaded("gregtech")) SuperFragmentRecipes.register();
    }
}
