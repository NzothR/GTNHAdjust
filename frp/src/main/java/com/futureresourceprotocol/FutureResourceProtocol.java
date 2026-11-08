package com.futureresourceprotocol;

import net.minecraftforge.common.config.Configuration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.futureresourceprotocol.device.PortableTerminal;
import com.futureresourceprotocol.device.TerminalGuiHandler;
import com.futureresourceprotocol.device.TerminalMessage;
import com.futureresourceprotocol.protocol.ProtocolCommand;
import com.futureresourceprotocol.recipe.RecipeIndex;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;

@Mod(
    modid = FutureResourceProtocol.MOD_ID,
    name = "Future Resource Protocol",
    version = Tags.VERSION,
    acceptedMinecraftVersions = "[1.7.10]",
    dependencies = "required-after:gregtech;required-after:appliedenergistics2;required-after:NotEnoughItems")
public class FutureResourceProtocol {

    public static final String MOD_ID = "futureresourceprotocol";
    public static final Logger LOG = LogManager.getLogger(MOD_ID);
    public static boolean debug;
    public static boolean enabled;
    public static SimpleNetworkWrapper NETWORK;
    public static PortableTerminal terminal;

    @Mod.Instance(MOD_ID)
    public static FutureResourceProtocol instance;

    public static void debug(String message, Object... args) {
        if (debug) LOG.info("[FRP debug] " + message, args);
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        Configuration config = new Configuration(event.getSuggestedConfigurationFile());
        enabled = config
            .getBoolean("enabled", "general", true, "Enable FRP. Restart required; client and server must match.");
        debug = config
            .getBoolean("debug", "general", true, "Detailed development diagnostics. May produce large logs.");
        RecipeIndex.configure(config);
        if (config.hasChanged()) config.save();
        if (!enabled) {
            LOG.warn("FRP is disabled; existing world debt is retained but protocol rules cannot be enforced.");
            return;
        }
        terminal = new PortableTerminal();
        GameRegistry.registerItem(terminal, "portable_terminal");
        NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel("frp_terminal");
        NETWORK.registerMessage(TerminalMessage.Handler.class, TerminalMessage.class, 0, Side.SERVER);
        FMLCommonHandler.instance()
            .bus()
            .register(new TerminalMessage.Handler());
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new TerminalGuiHandler());
        debug("preInit enabled={}, debug={}", enabled, debug);
    }

    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        if (enabled) RecipeIndex.rebuild();
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        if (enabled) event.registerServerCommand(new ProtocolCommand());
    }
}
