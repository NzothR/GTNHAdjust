package com.gtnhadjust;

import net.minecraftforge.common.config.Configuration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gtnhadjust.recipe.CarpenterRecipes;
import com.gtnhadjust.terminal.TerminalNotifications;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(
    modid = GTNHAdjust.MOD_ID,
    name = "GTNH Adjust",
    version = Tags.VERSION,
    acceptedMinecraftVersions = "[1.7.10]",
    dependencies = "after:Forestry;after:gregtech")
public class GTNHAdjust {

    public static final String MOD_ID = "gtnhadjust";
    public static final Logger LOG = LogManager.getLogger(MOD_ID);
    public static boolean debug;
    public static boolean terminalDebug;
    public static int steamGreenhouseMaxSeeds = 64;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        if (FeatureConfig.get().quickTerminal) TerminalNotifications.init();
        Configuration config = new Configuration(event.getSuggestedConfigurationFile());
        debug = config.getBoolean("debug", Configuration.CATEGORY_GENERAL, false, "Log crop breeder adjustments.");
        terminalDebug = config
            .getBoolean("debug", "quickTerminal", false, "Log quick terminal recipe transfers and automatic uploads.");
        steamGreenhouseMaxSeeds = config.getInt(
            "steamGreenhouseMaxSeeds",
            "gtnl",
            64,
            1,
            64,
            "Steam greenhouse maximum seed count; server setting, restart required. Remove excess seeds before lowering.");
        if (config.hasChanged()) config.save();
    }

    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        if (FeatureConfig.get().carpenterRecipes && Loader.isModLoaded("Forestry") && Loader.isModLoaded("gregtech"))
            CarpenterRecipes.register();
    }
}
