package com.gtnhadjust;

import net.minecraftforge.common.config.Configuration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gtnhadjust.machine.collector.MatterCompression;
import com.gtnhadjust.machine.collector.SuperFragmentRecipes;
import com.gtnhadjust.machine.collector.SuperFragmentRegistration;
import com.gtnhadjust.machine.collector.TierDimensionItem;
import com.gtnhadjust.machine.debug.RecipePoolCatalog;
import com.gtnhadjust.machine.debug.RecipePoolDebugger;
import com.gtnhadjust.machine.loot.LootSimulatorRegistration;
import com.gtnhadjust.machine.proxy.ProxyFactoryRegistration;
import com.gtnhadjust.machine.threadedproxy.ThreadedProxyRegistration;
import com.gtnhadjust.recipe.CarpenterRecipes;
import com.gtnhadjust.soul.SoulVialMultipartHandler;
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
    dependencies = "after:Forestry;after:gregtech;after:gtneverreset;after:sciencenotleisure")
public class GTNHAdjust {

    public static int threadedProxyPatternSlots = 1024;

    public static final String MOD_ID = "gtnhadjust";
    public static final Logger LOG = LogManager.getLogger(MOD_ID);
    public static TierDimensionItem tierDimension;
    public static boolean debug;
    public static boolean singularityResourceRecipes;
    public static boolean collectorBlackHole;
    public static boolean collectorWirelessPower;
    public static boolean collectorDirectAeOutput = true;
    public static boolean collectorMatterCompression;
    public static boolean proxyFactoryWirelessPower;
    public static boolean threadedProxyWirelessPower;
    public static boolean threadedProxyVoidUnlistedOutputs = true;
    public static boolean proxyIndirectLocators = true;
    public static boolean proxyExtremeCrafting;
    public static boolean locatorNavigation;
    public static int proxyFactoryLocatorSlots = 128;
    public static boolean collectorMarkerOutputMultiplier;
    public static int collectorRecipeTicks = 128;
    public static int collectorBaseParallel = 64;
    public static int collectorBaseThreads = 8;
    public static int collectorMaxVoltageParallel = Integer.MAX_VALUE;
    public static int collectorVoltageThreads = 4;
    public static boolean terminalDebug;
    public static boolean patternDeduplication;
    public static boolean gtnrVirtualTools;
    public static boolean proxyFactoryEnabled;
    public static boolean machineLocatorsEnabled;
    public static boolean hologramAeMaterials;
    public static int steamGreenhouseMaxSeeds = 64;
    private int superFragmentMteId = 31990;
    private int lootSimulatorMteId = 31992;
    private int proxyFactoryMteId = 31993;
    private int threadedProxyMteId = 31994;
    private boolean recipePoolDebuggerEnabled;
    private int recipePoolDebuggerMteId;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        if (FeatureConfig.get().soulVialExpandedCapture && Loader.isModLoaded("EnderIO"))
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new SoulVialMultipartHandler());
        patternDeduplication = FeatureConfig.get().patternDeduplication;
        gtnrVirtualTools = FeatureConfig.get().gtnrVirtualTools && Loader.isModLoaded("gtneverreset")
            && Loader.isModLoaded("appliedenergistics2");
        if (gtnrVirtualTools)
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new com.gtnhadjust.compat.GtnrToolPatterns());
        proxyFactoryEnabled = FeatureConfig.get().proxyFactory;
        machineLocatorsEnabled = proxyFactoryEnabled || FeatureConfig.get().threadedJointProxy;
        hologramAeMaterials = FeatureConfig.get().structureLibAeAutoPlace;
        if (FeatureConfig.get().questAeRetrieval && Loader.isModLoaded("betterquesting")
            && Loader.isModLoaded("appliedenergistics2")) com.gtnhadjust.quest.QuestAeNetwork.init();
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
        collectorMaxVoltageParallel = config.getInt(
            "maxVoltageParallel",
            "collector",
            Integer.MAX_VALUE,
            1,
            Integer.MAX_VALUE,
            "Per-thread parallel capability at MAX voltage. Exponential growth from baseParallel at LV; replaces voltageParallelMultiplier. Restart required; client and server must match.");
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
            "Render the custom black hole, rotating confinement sphere and outward jets in the formed matter decompressor. Server setting; restart required.");
        collectorDirectAeOutput = config.getBoolean(
            "directAeOutput",
            "collector",
            true,
            "Connect the controller to AE for direct item/fluid output; fall back to output buses/hatches when inactive. Restart required.");
        collectorMatterCompression = config.getBoolean(
            "matterCompression",
            "collector",
            true,
            "Enable the matter compression mode and blank cell recipe. Requires GTNL and Avaritia. Restart required.")
            && Loader.isModLoaded("sciencenotleisure")
            && Loader.isModLoaded("Avaritia");
        collectorWirelessPower = config.getBoolean(
            "wirelessPower",
            "collector",
            true,
            "Allow the collector to use its owner's wireless EU network when no energy hatch is installed. Restart required.");
        proxyFactoryWirelessPower = config.getBoolean(
            "wirelessPower",
            "proxyFactory",
            true,
            "Allow UEV+ glass proxy factories without energy hatches to use their owner's wireless EU network. Restart required; client and server must match.");
        threadedProxyPatternSlots = config.getInt(
            "patternSlots",
            "threadedJointProxy",
            1024,
            1,
            16384,
            "Internal single-pattern slot count. Restart required; client and server must match.");
        threadedProxyWirelessPower = config.getBoolean(
            "wirelessPower",
            "threadedJointProxy",
            true,
            "Use the owner's wireless EU network when no energy hatch is installed. Restart required; client and server must match.");
        threadedProxyVoidUnlistedOutputs = config.getBoolean(
            "voidUnlistedOutputs",
            "threadedJointProxy",
            true,
            "Discard recipe outputs absent from the thread's encoded pattern. Restart required; client and server must match.");
        proxyExtremeCrafting = config.getBoolean(
            "extremeCrafting",
            "proxyFactory",
            true,
            "Support TST extreme crafting conversion processing patterns, including patterns without recipe-map tags. Restart required; client and server must match.");
        locatorNavigation = config.getBoolean(
            "locatorNavigation",
            "proxyFactory",
            true,
            "Allow recorded locators to highlight on air right-click and safely teleport on sneaking air right-click. Restart required.");
        proxyIndirectLocators = config.getBoolean(
            "indirectLocators",
            "proxyFactory",
            true,
            "Allow both proxy controllers to read direct locators from one other proxy. Restart required; client and server must match.");
        if (machineLocatorsEnabled && locatorNavigation) com.gtnhadjust.machine.proxy.MachineLocatorNavigation.init();
        // Vanilla slot IDs are signed shorts; reserve 36 player slots and the controller slot.
        proxyFactoryLocatorSlots = config.getInt(
            "locatorSlots",
            "proxyFactory",
            128,
            1,
            32730,
            "Locator inventory capacity shared by both proxy machines. Existing overflow slots remain extractable. Restart required; client and server must match.");
        collectorMarkerOutputMultiplier = config.getBoolean(
            "markerOutputMultiplier",
            "collector",
            true,
            "Multiply collector outputs by the number of matching dimension markers in input buses, up to 64. Restart required.");
        superFragmentMteId = config.getInt(
            "superFragmentMteId",
            "collector",
            31990,
            2048,
            32766,
            "Stable GregTech machine ID for the super fragment collector. Do not change after placing or obtaining it; client and server must match.");
        lootSimulatorMteId = config.getInt(
            "mteId",
            "lootSimulator",
            31992,
            2048,
            32766,
            "Stable loot simulator controller ID. Do not change after obtaining or placing it; clients and servers must match.");
        proxyFactoryMteId = config.getInt(
            "mteId",
            "proxyFactory",
            31993,
            2048,
            32766,
            "Proxy joint factory controller ID. Restart required; clients and servers must match.");
        threadedProxyMteId = config.getInt(
            "mteId",
            "threadedJointProxy",
            31994,
            2048,
            32766,
            "Stable threaded joint proxy controller ID. Restart required; clients and servers must match.");
        recipePoolDebuggerEnabled = config.getBoolean(
            "enabled",
            "recipePoolDebugger",
            true,
            "Register the read-only recipe pool debugger and NEI directory. Restart required; client and server must match.");
        recipePoolDebuggerMteId = config.getInt(
            "mteId",
            "recipePoolDebugger",
            31995,
            2048,
            32766,
            "Recipe pool debugger controller ID. Restart required; client and server must match.");
        if (config.hasChanged()) config.save();
        tierDimension = new TierDimensionItem();
        GameRegistry.registerItem(tierDimension, "tier_dimension");
        if (event.getSide()
            .isClient()) tierDimension.registerRenderer();
        if (machineLocatorsEnabled && Loader.isModLoaded("gtneverreset") && Loader.isModLoaded("gregtech"))
            ProxyFactoryRegistration.registerItems(
                event.getSide()
                    .isClient());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (FeatureConfig.get().substanceReshapingDevice && Loader.isModLoaded("gregtech"))
            com.gtnhadjust.machine.reshaping.ReshapingRegistration.registerMachine(31996);
        if (recipePoolDebuggerEnabled && Loader.isModLoaded("gregtech"))
            RecipePoolDebugger.register(recipePoolDebuggerMteId);
        if (FeatureConfig.get().threadedJointProxy && Loader.isModLoaded("gtneverreset")
            && Loader.isModLoaded("sciencenotleisure")
            && Loader.isModLoaded("gregtech")) ThreadedProxyRegistration.registerMachine(threadedProxyMteId);
        if (FeatureConfig.get().proxyFactory && Loader.isModLoaded("gtneverreset") && Loader.isModLoaded("gregtech"))
            ProxyFactoryRegistration.registerMachine(proxyFactoryMteId);
        if (FeatureConfig.get().lootSimulator && Loader.isModLoaded("gregtech")
            && Loader.isModLoaded("kubatech")
            && Loader.isModLoaded("mobsinfo")
            && Loader.isModLoaded("EnderIO")) LootSimulatorRegistration.registerMachine(lootSimulatorMteId);
        if (Loader.isModLoaded("gtneverreset") && Loader.isModLoaded("gregtech")) {
            MatterCompression.registerItems();
            SuperFragmentRegistration.registerMachine(superFragmentMteId);
            SuperFragmentRecipes.initialize();
            SuperFragmentRegistration.registerCrafting();
        }
    }

    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        if (FeatureConfig.get().substanceReshapingDevice && Loader.isModLoaded("gregtech"))
            com.gtnhadjust.machine.reshaping.ReshapingRegistration.registerRecipes();
        if (machineLocatorsEnabled && Loader.isModLoaded("gtneverreset") && Loader.isModLoaded("gregtech"))
            ProxyFactoryRegistration.registerLocatorCrafting();
        if (recipePoolDebuggerEnabled && event.getSide()
            .isClient() && Loader.isModLoaded("NotEnoughItems")) RecipePoolCatalog.register();
        if (FeatureConfig.get().threadedJointProxy && Loader.isModLoaded("gtneverreset")
            && Loader.isModLoaded("sciencenotleisure")
            && Loader.isModLoaded("gregtech")) ThreadedProxyRegistration.registerCrafting();
        if (FeatureConfig.get().proxyFactory && Loader.isModLoaded("gtneverreset") && Loader.isModLoaded("gregtech"))
            ProxyFactoryRegistration.registerCrafting();
        if (FeatureConfig.get().lootSimulator && Loader.isModLoaded("gregtech")
            && Loader.isModLoaded("kubatech")
            && Loader.isModLoaded("mobsinfo")
            && Loader.isModLoaded("EnderIO")) LootSimulatorRegistration.registerCrafting();
        if (FeatureConfig.get().carpenterRecipes && Loader.isModLoaded("Forestry") && Loader.isModLoaded("gregtech"))
            CarpenterRecipes.register();
        if (Loader.isModLoaded("gtneverreset") && Loader.isModLoaded("gregtech")) {
            SuperFragmentRecipes.register();
            if (collectorMatterCompression) MatterCompression.registerCrafting();
        }
    }
}
