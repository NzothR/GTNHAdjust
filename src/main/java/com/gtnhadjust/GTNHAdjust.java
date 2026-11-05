package com.gtnhadjust;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gtnhadjust.recipe.CarpenterRecipes;

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
    public static int steamGreenhouseMaxSeeds = 64;
    private static File configDirectory;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        configDirectory = event.getModConfigurationDirectory();
        Configuration config = new Configuration(event.getSuggestedConfigurationFile());
        debug = config.getBoolean("debug", Configuration.CATEGORY_GENERAL, false, "Log crop breeder adjustments.");
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
        if (Loader.isModLoaded("Forestry") && Loader.isModLoaded("gregtech"))
            CarpenterRecipes.register(configDirectory);
    }
}
