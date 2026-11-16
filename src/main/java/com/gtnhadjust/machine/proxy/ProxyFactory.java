package com.gtnhadjust.machine.proxy;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.HatchElement.OutputHatch;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTStructureUtility.chainAllGlasses;
import static gregtech.api.util.GTStructureUtility.ofHatchAdder;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.gtnhadjust.GTNHAdjust;
import com.xyp.gtneverreset.common.gui.modularui.multiblock.GTNRMultiBlockBaseGui;
import com.xyp.gtneverreset.common.machine.hatch.ParameterModuleHatch;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadProcessingLogic;
import com.xyp.gtneverreset.common.machine.tooltip.StructureTooltipHooks;
import com.xyp.gtneverreset.utils.structure.MultiblockStructureLoader;

import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.util.AECableType;
import appeng.api.util.DimensionalCoord;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.Textures;
import gregtech.api.enums.VoltageIndex;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTUtility;
import gregtech.api.util.shutdown.ShutDownReasonRegistry;
import gregtech.common.misc.WirelessNetworkManager;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;

@IMetaTileEntity.SkipGenerateDescription
public final class ProxyFactory extends ProxyThreadedMachine<ProxyFactory>
    implements ISurvivalConstructable, IGridProxyable {

    private static final IIconContainer MARK = Textures.BlockIcons.custom(GTNHAdjust.MOD_ID, "proxy/overlay_mark");
    private static final int BASE_PARALLELS = 64;
    private static final int BASE_THREADS = 32;
    private static final int THREADS_PER_TIER = 8;
    private static final int BASE_GLASS_TIER = VoltageIndex.HV;
    private static final double SPEED_PER_GLASS_TIER = 0.2;
    private static final double EU_MULTIPLIER_PER_GLASS_TIER = 0.9;
    private static final int CASING_TEXTURE_ID = 16;
    private static final String STRUCTURE_PIECE = "main";
    private static final String AE_CONNECTED_NBT = "gtnhadjustProxyAeConnected";
    private static final int OFFSET_X = 24, OFFSET_Y = 15, OFFSET_Z = 0;
    private static final int RUN = 0, INPUT = 1;

    /** Each voltage tier above LV quadruples the parallel limit, saturating at the GT integer limit. */
    public static int parallelsForTier(int tier) {
        int upgrades = (int) Math.min(13, Math.max(0L, (long) tier - VoltageIndex.LV));
        return (int) Math.min(Integer.MAX_VALUE, (long) BASE_PARALLELS << (2 * upgrades));
    }

    public static int threadsForTier(int tier) {
        long upgrades = Math.max(0L, (long) tier - VoltageIndex.LV);
        return (int) Math.min(Integer.MAX_VALUE, BASE_THREADS + upgrades * THREADS_PER_TIER);
    }

    private IStructureDefinition<ProxyFactory> definition;
    private AENetworkProxy gridProxy;
    private RecipeMap<?> processingRecipeMap;
    private final ProxyLocatorInventory locatorInventory = new ProxyLocatorInventory(this);
    private List<RecipeMap<?>> unlockedMaps;
    private List<String> clientRecipeNames;
    private int operationMode = INPUT;
    private final ProxyLocatorPool locatorPool = new ProxyLocatorPool();
    private final ProxyPatternIssues patternIssues = new ProxyPatternIssues();
    private int wirelessMaxOverclocks;
    private UUID wirelessOwner;

    public ProxyFactory(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    public ProxyFactory(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new ProxyFactory(mName);
    }

    @Override
    public AENetworkProxy getProxy() {
        if (gridProxy == null && getBaseMetaTileEntity() instanceof IGridProxyable) {
            gridProxy = new AENetworkProxy(this, "proxy", getStackForm(1), true);
            gridProxy.setFlags(GridFlags.REQUIRE_CHANNEL);
            gridProxy.setIdlePowerUsage(0);
            IGregTechTileEntity tile = getBaseMetaTileEntity();
            if (tile.getWorld() != null) gridProxy.setOwner(
                tile.getWorld()
                    .getPlayerEntityByName(tile.getOwnerName()));
        }
        return gridProxy;
    }

    /** Report the same powered, channel-ready AE node state required by output delivery. */
    public boolean isAeNetworkConnected() {
        return gridProxy != null && gridProxy.isActive();
    }

    /** Localize and color the shared GUI/Waila status without recoloring subsequent tooltip lines. */
    static String aeNetworkStatusText(boolean connected) {
        return (connected ? EnumChatFormatting.GREEN : EnumChatFormatting.RED)
            + StatCollector.translateToLocal("gtnhadjust.proxy.ae." + (connected ? "connected" : "disconnected"))
            + EnumChatFormatting.RESET;
    }

    /** Synchronize the server's AE availability with the existing multiblock Waila payload. */
    @Override
    public void getWailaNBTData(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag, World world, int x, int y,
        int z) {
        super.getWailaNBTData(player, tile, tag, world, x, y, z);
        tag.setBoolean(AE_CONNECTED_NBT, isAeNetworkConnected());
    }

    /** Add connection status to the controller's block tooltip in both operation modes. */
    @Override
    public void getWailaBody(ItemStack stack, List<String> currentTip, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        super.getWailaBody(stack, currentTip, accessor, config);
        currentTip.add(
            aeNetworkStatusText(
                accessor.getNBTData()
                    .getBoolean(AE_CONNECTED_NBT)));
    }

    @Override
    public DimensionalCoord getLocation() {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        return new DimensionalCoord(tile.getWorld(), tile.getXCoord(), tile.getYCoord(), tile.getZCoord());
    }

    @Override
    public IGridNode getGridNode(ForgeDirection side) {
        AENetworkProxy proxy = getProxy();
        return proxy == null ? null : proxy.getNode();
    }

    @Override
    public AECableType getCableConnectionType(ForgeDirection side) {
        return AECableType.SMART;
    }

    @Override
    public void securityBreak() {}

    @Override
    public void onFirstTick(IGregTechTileEntity tile) {
        super.onFirstTick(tile);
        if (tile.isServerSide()) onLocatorInventoryChanged();
        AENetworkProxy proxy = getProxy();
        if (proxy != null) proxy.onReady();
    }

    @Override
    public IStructureDefinition<ProxyFactory> getStructureDefinition() {
        if (definition == null) {
            var ports = ofChain(
                StructureTooltipHooks.describeAsHatch(
                    ofHatchAdder(ProxyFactory::addParameterModuleToMachineList, CASING_TEXTURE_ID, 1),
                    ParameterModuleHatch.NAME_KEY),
                buildHatchAdder(ProxyFactory.class)
                    .atLeast(InputBus, InputHatch, OutputBus, OutputHatch, Energy.or(ExoticEnergy), Maintenance)
                    .casingIndex(CASING_TEXTURE_ID)
                    .hint(1)
                    .buildAndChain(ofBlock(GregTechAPI.sBlockCasings2, 0)));
            definition = StructureDefinition.<ProxyFactory>builder()
                .addShape(STRUCTURE_PIECE, transpose(MultiblockStructureLoader.load("universal_joint_factory")))
                .addElement('X', ports)
                .addElement('Q', ports)
                .addElement('A', ofBlock(GregTechAPI.sBlockCasings2, 0))
                .addElement('B', ofBlock(GregTechAPI.sBlockCasings1, 2))
                .addElement('D', ofBlock(GregTechAPI.sBlockCasings2, 0))
                .addElement('E', ofBlock(GregTechAPI.sBlockCasings2, 3))
                .addElement('F', ofBlock(GregTechAPI.sBlockCasings2, 13))
                .addElement('G', ofBlock(GregTechAPI.sBlockCasings5, 0))
                .addElement(
                    'I',
                    chainAllGlasses(-1, (machine, tier) -> machine.mGlassTier = tier, machine -> machine.mGlassTier))
                .addElement('J', ofBlock(Blocks.glowstone, 0))
                .build();
        }
        return definition;
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece(STRUCTURE_PIECE, stack, hintsOnly, OFFSET_X, OFFSET_Y, OFFSET_Z);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int elementBudget, ISurvivalBuildEnvironment environment) {
        if (mMachine) return -1;
        return survivalBuildPiece(
            STRUCTURE_PIECE,
            stack,
            OFFSET_X,
            OFFSET_Y,
            OFFSET_Z,
            elementBudget,
            environment,
            false,
            true);
    }

    @Override
    protected void checkMachineStructure(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        checkPiece(STRUCTURE_PIECE, OFFSET_X, OFFSET_Y, OFFSET_Z, errors);
    }

    @Override
    protected void checkMachineHatches(List<StructureError> errors) {
        checkHatchMin(errors, InputBus.or(InputHatch), 1);
        if (!isWirelessMode()) checkHatchMin(errors, Energy.or(ExoticEnergy), 1);
        checkHatchMin(errors, Maintenance, 1);
        checkHatchMax(errors, Maintenance, 1);
    }

    @Override
    protected RecipeThreadProcessingLogic createRecipeThreadProcessingLogic() {
        return new ProxyFactoryProcessingLogic(this);
    }

    @Override
    protected ProxyThreadOutputs createThreadOutputs(RecipeThreadProcessingLogic logic) {
        return ((ProxyFactoryProcessingLogic) logic).getPreparedOutputs();
    }

    /** High-tier glass unlocks wireless power only while every energy hatch slot is absent. */
    public boolean isWirelessMode() {
        return !isTooltipProbeActive() && GTNHAdjust.proxyFactoryWirelessPower
            && mGlassTier > VoltageIndex.UHV
            && mEnergyHatches.isEmpty()
            && mExoticEnergyHatches.isEmpty();
    }

    @Override
    public long getMaxInputVoltage() {
        return isWirelessMode() ? GTValues.V[VoltageIndex.MAX] : super.getMaxInputVoltage();
    }

    @Override
    public long getMaxInputEu() {
        return isWirelessMode() ? Long.MAX_VALUE - 1 : super.getMaxInputEu();
    }

    /** Recipe recognition sees MAX voltage independently of the actual per-thread EU budget. */
    @Override
    protected void setupProcessingLogic(ProcessingLogic logic) {
        super.setupProcessingLogic(logic);
        if (isWirelessMode()) logic.setAvailableVoltage(GTValues.V[VoltageIndex.MAX])
            .setAvailableAmperage(1);
    }

    /** Debit the native owner/team wireless account once per running tick at the actual machine load. */
    @Override
    public boolean onRunningTick(ItemStack stack) {
        if (!isWirelessMode()) return super.onRunningTick(stack);
        UUID owner = getBaseMetaTileEntity().getOwnerUuid();
        if (owner != null && !owner.equals(wirelessOwner)) {
            WirelessNetworkManager.strongCheckOrAddUser(owner);
            wirelessOwner = owner;
        }
        BigInteger cost = BigInteger.valueOf(-lEUt)
            .multiply(BigInteger.valueOf(10000))
            .divide(BigInteger.valueOf(Math.max(1000, mEfficiency)));
        if (owner == null || !WirelessNetworkManager.addEUToGlobalEnergyMap(owner, cost.negate())) {
            if (GTNHAdjust.debug)
                GTNHAdjust.LOG.info("[proxy-wireless] insufficient energy owner={} requested={} EU/t", owner, cost);
            stopMachine(ShutDownReasonRegistry.POWER_LOSS);
            return false;
        }
        return true;
    }

    public int getWirelessMaxOverclocks() {
        return wirelessMaxOverclocks;
    }

    /** Changes apply to newly started wireless threads; running tasks retain their committed costs. */
    public void setWirelessMaxOverclocks(int value) {
        wirelessMaxOverclocks = Math.max(0, value);
        markDirty();
    }

    @Override
    protected CheckRecipeResult doCheckRecipe() {
        return operationMode == RUN ? super.doCheckRecipe() : CheckRecipeResultRegistry.NO_RECIPE;
    }

    @Override
    public void onPostTick(IGregTechTileEntity tile, long tick) {
        super.onPostTick(tile, tick);
        if (tile.isServerSide() && GTNHAdjust.debug && tick % 200 == 0) {
            GTNHAdjust.LOG.info(
                "[proxy-ae] formed={} enabled={} active={} threads={}",
                mMachine,
                tile.isAllowedToWork(),
                getProxy() != null && getProxy().isActive(),
                getProxyThreadTasks().size());
        }
    }

    @Override
    public void onScrewdriverRightClick(ForgeDirection side, EntityPlayer player, float x, float y, float z,
        ItemStack tool) {
        // Recipe-pool selection and recipe locking are intentionally unavailable.
    }

    int getOperationMode() {
        return operationMode;
    }

    void setClientOperationMode(int mode) {
        operationMode = mode == INPUT ? INPUT : RUN;
    }

    void cycleOperationMode() {
        operationMode = operationMode == INPUT ? RUN : INPUT;
        patternIssues.clear();
        scheduleRecipeCheckImmediate();
        markDirty();
    }

    public boolean canEditLocators() {
        return operationMode == INPUT;
    }

    public ProxyLocatorInventory getLocatorInventory() {
        return locatorInventory;
    }

    /** Rebuild only when actual locator slots change; running tasks keep their committed recipe and outputs. */
    void onLocatorInventoryChanged() {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (tile == null || tile.isClientSide()) return;
        patternIssues.clear();
        List<ItemStack> locators = new ArrayList<>();
        for (int i = 0; i < locatorInventory.getSlots(); i++) {
            ItemStack stack = locatorInventory.getStackInSlot(i);
            if (stack != null) locators.add(stack);
        }
        locatorPool.replace(locators);
        unlockedMaps = locatorPool.recipeMaps();
        scheduleRecipeCheckImmediate();
        markDirty();
    }

    /** Admission is checked only for a prospective new thread, never for a running task. */
    CheckRecipeResult checkLocatorRequirements(RecipeMap<?> map, gregtech.api.util.GTRecipe recipe) {
        return locatorPool.checkStart(this, map, recipe);
    }

    void recordPatternResult(gregtech.common.tileentities.machines.IDualInputInventoryWithPattern inventory,
        appeng.api.networking.crafting.ICraftingPatternDetails pattern, RecipeMap<?> map, CheckRecipeResult result,
        boolean sourceFailure) {
        patternIssues.record(inventory, pattern, sourceFailure ? locatorPool.sourceController(map) : null, result);
    }

    List<ProxyFactoryStatus.Issue> getPatternIssues() {
        return patternIssues.snapshot();
    }

    List<String> getUnlockedRecipeNames() {
        List<String> result = new ArrayList<>();
        for (RecipeMap<?> map : getAvailableRecipeMaps()) result.add(map.unlocalizedName);
        return result;
    }

    void setClientRecipeNames(List<String> names) {
        clientRecipeNames = new ArrayList<>(names);
    }

    boolean hasCreativeEssentiaCell() {
        ItemStack slot = getControllerSlot();
        ItemStack cell = GTModHandler.getModItem("thaumicenergistics", "storage.essentia", 1, 4);
        return slot != null && cell != null && slot.stackSize > 0 && slot.isItemEqual(cell);
    }

    @Override
    protected boolean canUseControllerSlotForRecipe() {
        return !hasCreativeEssentiaCell() && super.canUseControllerSlotForRecipe();
    }

    boolean isRecipeMapUnlocked(RecipeMap<?> map) {
        return map != null && getAvailableRecipeMaps().contains(map);
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        AENetworkProxy proxy = getProxy();
        if (proxy != null) proxy.writeToNBT(tag);
        tag.setTag("gtnhadjustLocatorInventory", locatorInventory.serializeNBT());
        tag.setInteger("gtnhadjustProxyOperation", operationMode);
        tag.setInteger("gtnhadjustProxyWirelessOC", wirelessMaxOverclocks);
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        AENetworkProxy proxy = getProxy();
        if (proxy != null) proxy.readFromNBT(tag);
        if (tag.hasKey("gtnhadjustLocatorInventory", 10))
            locatorInventory.deserializeNBT(tag.getCompoundTag("gtnhadjustLocatorInventory"));
        operationMode = !tag.hasKey("gtnhadjustProxyOperation") || tag.getInteger("gtnhadjustProxyOperation") == INPUT
            ? INPUT
            : RUN;
        inputSeparation = false;
        mLockedToSingleRecipe = false;
        setSingleRecipeCheck(null);
        voidingMode = gregtech.api.enums.VoidingMode.VOID_NONE;
        onLocatorInventoryChanged();
        wirelessMaxOverclocks = Math.max(0, tag.getInteger("gtnhadjustProxyWirelessOC"));
        unlockedMaps = null;
        clientRecipeNames = null;
    }

    @Override
    public void setItemNBT(NBTTagCompound tag) {
        super.setItemNBT(tag);
        tag.setTag("gtnhadjustLocatorInventory", locatorInventory.serializeNBT());
        tag.setInteger("gtnhadjustProxyOperation", operationMode);
        tag.setInteger("gtnhadjustProxyWirelessOC", wirelessMaxOverclocks);
    }

    @Override
    public String[] getInfoData() {
        List<String> lines = new ArrayList<>();
        Collections.addAll(lines, super.getInfoData());
        lines
            .add(StatCollector.translateToLocal("gtnhadjust.proxy.power." + (isWirelessMode() ? "wireless" : "wired")));
        lines.add(StatCollector.translateToLocal("gtnhadjust.proxy.operation." + operationMode));
        lines.add(
            StatCollector.translateToLocalFormatted(
                "gtnhadjust.proxy.count",
                getRecordedLocatorCount(),
                getAvailableRecipeMaps().size()));
        return lines.toArray(new String[0]);
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return processingRecipeMap;
    }

    void setProcessingRecipeMap(RecipeMap<?> map) {
        processingRecipeMap = map;
    }

    @Override
    public List<RecipeMap<?>> getAvailableRecipeMaps() {
        if (getBaseMetaTileEntity() != null && getBaseMetaTileEntity().isClientSide() && clientRecipeNames != null) {
            List<RecipeMap<?>> maps = new ArrayList<>();
            for (String name : clientRecipeNames) {
                RecipeMap<?> map = RecipeMap.ALL_RECIPE_MAPS.get(name);
                if (map != null && !maps.contains(map)) maps.add(map);
            }
            return maps;
        }
        if (unlockedMaps == null) unlockedMaps = locatorPool.recipeMaps();
        return unlockedMaps;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        ITexture casing = Textures.BlockIcons.getCasingTextureForId(CASING_TEXTURE_ID);
        if (side != facing) return new ITexture[] { casing };
        return new ITexture[] { casing, TextureFactory.builder()
            .addIcon(
                active ? Textures.BlockIcons.OVERLAY_FRONT_ASSEMBLY_LINE_ACTIVE
                    : Textures.BlockIcons.OVERLAY_FRONT_ASSEMBLY_LINE)
            .extFacing()
            .build(),
            TextureFactory.builder()
                .addIcon(MARK)
                .extFacing()
                .build() };
    }

    @Override
    public String getTooltipPath() {
        return "universal-joint-factory";
    }

    @Override
    public String getTooltipMachineType() {
        // #tr gtnr.machine.universal_joint_factory.type
        // # Universal processing / cross-recipe threads
        // # zh_CN 通用加工 / 跨配方线程
        return StatCollector.translateToLocal("gtnr.machine.universal_joint_factory.type");
    }

    @Override
    public List<String> getTooltipFeatureLines() {
        List<String> lines = super.getTooltipFeatureLines();
        if (GTNHAdjust.proxyFactoryWirelessPower)
            lines.add(StatCollector.translateToLocal("gtnhadjust.proxy.wireless.tooltip"));
        return lines;
    }

    @Override
    protected GTNRMultiBlockBaseGui<ProxyFactory> getGui() {
        return new ProxyFactoryGui(this);
    }

    @Override
    public boolean supportsMachineModeSwitch() {
        return false;
    }

    @Override
    public boolean supportsVoidProtection() {
        return false;
    }

    @Override
    public boolean supportsInputSeparation() {
        return false;
    }

    @Override
    public boolean isInputSeparationEnabled() {
        return false;
    }

    @Override
    public boolean getDefaultInputSeparationMode() {
        return false;
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return false;
    }

    @Override
    public boolean isRecipeLockingEnabled() {
        return false;
    }

    @Override
    public gregtech.api.enums.VoidingMode getVoidingMode() {
        return gregtech.api.enums.VoidingMode.VOID_NONE;
    }

    private int getRecordedLocatorCount() {
        int count = 0;
        for (int i = 0; i < locatorInventory.getSlots(); i++) if (locatorInventory.getStackInSlot(i) != null) count++;
        return count;
    }

    @Override
    public int getMaxRecipeThreads() {
        return threadsForTier(GTUtility.getTier(getMaxInputVoltage()));
    }

    @Override
    protected int getBaseParallelRecipes() {
        return parallelsForTier(GTUtility.getTier(getMaxInputVoltage()));
    }

    @Override
    protected double getBaseSpeedMultiplier() {
        return 1 + SPEED_PER_GLASS_TIER * getGlassUpgrades();
    }

    @Override
    protected double getBaseEUtMultiplier() {
        return Math.pow(EU_MULTIPLIER_PER_GLASS_TIER, getGlassUpgrades());
    }

    int getRecipeGlassTier() {
        return Math.max(1, mGlassTier);
    }

    private int getGlassUpgrades() {
        return Math.max(0, mGlassTier - BASE_GLASS_TIER);
    }
}
