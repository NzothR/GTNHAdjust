package com.futureresourceprotocol;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.config.Configuration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.futureresourceprotocol.device.BookmarkExportMessage;
import com.futureresourceprotocol.device.BookmarkImportMessage;
import com.futureresourceprotocol.device.DebtSyncMessage;
import com.futureresourceprotocol.device.MarkSyncMessage;
import com.futureresourceprotocol.device.PortableTerminal;
import com.futureresourceprotocol.device.TerminalControlMessage;
import com.futureresourceprotocol.device.TerminalGuiHandler;
import com.futureresourceprotocol.device.TerminalMessage;
import com.futureresourceprotocol.device.TerminalModeMessage;
import com.futureresourceprotocol.protocol.ProtocolCommand;
import com.futureresourceprotocol.recipe.ClientHullLocks;
import com.futureresourceprotocol.recipe.HullLockEvents;
import com.futureresourceprotocol.recipe.HullLockMessage;
import com.futureresourceprotocol.recipe.HullLockToken;
import com.futureresourceprotocol.recipe.HullRecipeLocks;
import com.futureresourceprotocol.recipe.RecipeIndex;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
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
    public static boolean portableRepaymentEnabled;
    public static boolean aeRepaymentEnabled;
    public static boolean neiBookmarkExportEnabled;
    public static boolean neiBookmarkImportEnabled;
    public static boolean hullRecipeLocksEnabled;
    public static SimpleNetworkWrapper NETWORK;
    public static PortableTerminal terminal;
    public static HullLockToken hullLockToken;

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
        portableRepaymentEnabled = config.getBoolean(
            "portableRepayment",
            "features",
            true,
            "Enable repayment in the portable terminal. Restart required; client and server must match.");
        aeRepaymentEnabled = config.getBoolean(
            "aeRepayment",
            "features",
            true,
            "Allow wireless AE debt repayment from the terminal. Restart required; client and server must match.");
        neiBookmarkExportEnabled = config.getBoolean(
            "neiBookmarkExport",
            "features",
            true,
            "Allow exporting world marks to NEI bookmarks. Restart required; client and server must match.");
        neiBookmarkImportEnabled = config.getBoolean(
            "neiBookmarkImport",
            "features",
            true,
            "Allow importing item bookmarks from the current NEI page. Restart required; client and server must match.");
        hullRecipeLocksEnabled = config.getBoolean(
            "hullRecipeLocks",
            "features",
            true,
            "Update hull recipe restrictions on world start and when debt or final milestone permissions change. Restart to change this option.");
        RecipeIndex.configure(config);
        if (config.hasChanged()) config.save();
        if (!enabled) {
            LOG.warn("FRP is disabled; existing world debt is retained but protocol rules cannot be enforced.");
            return;
        }
        terminal = new PortableTerminal();
        GameRegistry.registerItem(terminal, "portable_terminal");
        if (hullRecipeLocksEnabled) {
            hullLockToken = new HullLockToken();
            GameRegistry.registerItem(hullLockToken, "hull_lock_token");
        }
        NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel("frp_terminal");
        NETWORK.registerMessage(TerminalMessage.Handler.class, TerminalMessage.class, 0, Side.SERVER);
        NETWORK.registerMessage(DebtSyncMessage.Handler.class, DebtSyncMessage.class, 1, Side.CLIENT);
        NETWORK.registerMessage(MarkSyncMessage.Handler.class, MarkSyncMessage.class, 2, Side.CLIENT);
        NETWORK.registerMessage(TerminalControlMessage.Handler.class, TerminalControlMessage.class, 3, Side.SERVER);
        NETWORK.registerMessage(BookmarkExportMessage.Handler.class, BookmarkExportMessage.class, 4, Side.CLIENT);
        NETWORK.registerMessage(TerminalModeMessage.Handler.class, TerminalModeMessage.class, 5, Side.CLIENT);
        NETWORK.registerMessage(HullLockMessage.Handler.class, HullLockMessage.class, 6, Side.CLIENT);
        NETWORK.registerMessage(BookmarkImportMessage.Handler.class, BookmarkImportMessage.class, 7, Side.SERVER);
        FMLCommonHandler.instance()
            .bus()
            .register(new HullLockEvents());
        if (FMLCommonHandler.instance()
            .getSide() == Side.CLIENT) ClientHullLocks.register();
        FMLCommonHandler.instance()
            .bus()
            .register(new TerminalMessage.Handler());
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new TerminalGuiHandler());
        debug("preInit enabled={}, debug={}", enabled, debug);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (!enabled) return;
        GameRegistry.addShapelessRecipe(new ItemStack(terminal), Blocks.stone, Blocks.dirt, Items.stick);
        debug("registered portable terminal shapeless recipe: stone, dirt, stick");
    }

    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        // Other mods can register GT recipes during their own LoadComplete and ServerStarting events.
        if (enabled) RecipeIndex.rebuild();
        if (enabled && hullRecipeLocksEnabled)
            HullRecipeLocks.apply(com.futureresourceprotocol.protocol.WorldProtocol.get());
    }

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        if (enabled && hullRecipeLocksEnabled) HullRecipeLocks.stop();
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        if (enabled) event.registerServerCommand(new ProtocolCommand());
    }
}
