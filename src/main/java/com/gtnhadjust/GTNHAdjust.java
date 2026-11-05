package com.gtnhadjust;

import net.minecraftforge.common.config.Configuration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = GTNHAdjust.MOD_ID, name = "GTNH Adjust", version = Tags.VERSION, acceptedMinecraftVersions = "[1.7.10]")
public class GTNHAdjust {

    public static final String MOD_ID = "gtnhadjust";
    public static final Logger LOG = LogManager.getLogger(MOD_ID);
    public static boolean debug;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        Configuration config = new Configuration(event.getSuggestedConfigurationFile());
        debug = config.getBoolean("debug", Configuration.CATEGORY_GENERAL, false, "Log crop breeder adjustments.");
        if (config.hasChanged()) config.save();
    }
}
