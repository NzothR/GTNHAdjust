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
import static gregtech.api.util.GTUtility.validMTEList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.gtnhadjust.GTNHAdjust;
import com.xyp.gtneverreset.common.gui.modularui.multiblock.GTNRMultiBlockBaseGui;
import com.xyp.gtneverreset.common.machine.hatch.ParameterModuleHatch;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadProcessingLogic;
import com.xyp.gtneverreset.common.machine.multiblock.UniversalJointFactory;
import com.xyp.gtneverreset.common.machine.tooltip.StructureTooltipHooks;
import com.xyp.gtneverreset.utils.structure.MultiblockStructureLoader;

import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.util.AECableType;
import appeng.api.util.DimensionalCoord;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.Textures;
import gregtech.api.enums.VoltageIndex;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTUtility;
import gregtech.api.util.ItemEjectionHelper;
import gregtech.common.tileentities.machines.IDualInputHatch;
import gregtech.common.tileentities.machines.IRecipeProcessingAwareHatch;
import gregtech.common.tileentities.machines.MTEHatchCraftingInputME;
import gregtech.common.tileentities.machines.MTEHatchInputBusME;

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
    private static final int OFFSET_X = 24, OFFSET_Y = 15, OFFSET_Z = 0;
    private static final int RUN = 0, INPUT = 1, OUTPUT = 2;
    private static final int MAX_CONTROLLERS = 256;

    public static int parallelsForTier(int tier) {
        int upgrades = (int) Math.min(30, Math.max(0L, (long) tier - VoltageIndex.LV));
        return (int) Math.min(Integer.MAX_VALUE, (long) BASE_PARALLELS << upgrades);
    }

    public static int threadsForTier(int tier) {
        long upgrades = Math.max(0L, (long) tier - VoltageIndex.LV);
        return (int) Math.min(Integer.MAX_VALUE, BASE_THREADS + upgrades * THREADS_PER_TIER);
    }

    private IStructureDefinition<ProxyFactory> definition;
    private AENetworkProxy gridProxy;
    private RecipeMap<?> processingRecipeMap;
    private final List<ItemStack> controllers = new ArrayList<>();
    private List<RecipeMap<?>> unlockedMaps;
    private List<String> clientRecipeNames;
    private String savedRecipeName;
    private int operationMode = RUN;

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
        if (!GTNHAdjust.proxyDirectAeOutput) checkHatchMin(errors, OutputBus.or(OutputHatch), 1);
        checkHatchMin(errors, Energy.or(ExoticEnergy), 1);
        checkHatchMin(errors, Maintenance, 1);
        checkHatchMax(errors, Maintenance, 1);
    }

    @Override
    protected RecipeThreadProcessingLogic createRecipeThreadProcessingLogic() {
        return new ProxyFactoryProcessingLogic(this);
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
                "[proxy-ae] formed={} enabled={} direct={} active={} threads={}",
                mMachine,
                tile.isAllowedToWork(),
                GTNHAdjust.proxyDirectAeOutput,
                getProxy() != null && getProxy().isActive(),
                getProxyThreadTasks().size());
        }
        if (!tile.isServerSide() || !mMachine || !tile.isAllowedToWork() || tick % 20 != 0) return;
        if (operationMode == INPUT) importController();
        else if (operationMode == OUTPUT) exportController();
    }

    @Override
    public void onScrewdriverRightClick(ForgeDirection side, EntityPlayer player, float x, float y, float z,
        ItemStack tool) {
        if (!player.isSneaking()) {
            super.onScrewdriverRightClick(side, player, x, y, z, tool);
            return;
        }
        if (getBaseMetaTileEntity().isServerSide()) {
            cycleOperationMode();
            GTUtility.sendChatToPlayer(
                player,
                StatCollector.translateToLocal("gtnhadjust.proxy.operation." + operationMode));
        }
    }

    int getOperationMode() {
        return operationMode;
    }

    void setClientOperationMode(int mode) {
        operationMode = Math.floorMod(mode, 3);
    }

    void cycleOperationMode() {
        operationMode = (operationMode + 1) % 3;
        getBaseMetaTileEntity().markDirty();
    }

    private void importController() {
        if (controllers.size() >= MAX_CONTROLLERS) return;
        Set<String> existing = new HashSet<>(getUnlockedRecipeNames());
        for (MTEHatchInputBus bus : validMTEList(mInputBusses)) {
            // ME virtual slots and crafting buffers require a separate transaction protocol.
            if (bus instanceof MTEHatchInputBusME || bus instanceof MTEHatchCraftingInputME
                || bus instanceof IDualInputHatch
                || bus instanceof IRecipeProcessingAwareHatch) continue;
            IGregTechTileEntity inventory = bus.getBaseMetaTileEntity();
            if (inventory == null) continue;
            for (int i = 0; i < inventory.getSizeInventory(); i++) {
                ItemStack candidate = inventory.getStackInSlot(i);
                if (candidate == null || candidate.stackSize <= 0) continue;
                List<String> maps = ProxyFactoryControllerRegistry.maps(candidate);
                if (!ProxyFactoryUnlockRules.addsNew(existing, maps)) continue;
                ItemStack removed = inventory.decrStackSize(i, 1);
                if (removed == null || removed.stackSize != 1) continue;
                controllers.add(removed.copy());
                refreshUnlockedMaps();
                if (GTNHAdjust.debug) GTNHAdjust.LOG.info("Proxy controller accepted: {}, maps={}", removed, maps);
                inventory.markDirty();
                getBaseMetaTileEntity().markDirty();
                return;
            }
        }
    }

    private void exportController() {
        if (controllers.isEmpty()) return;
        ItemStack returned = controllers.get(controllers.size() - 1)
            .copy();
        ItemEjectionHelper output = new ItemEjectionHelper(getOutputBusses(), true);
        if (output.ejectStack(returned) != 1) {
            if (GTNHAdjust.debug) GTNHAdjust.LOG.info("Proxy controller output blocked");
            return;
        }
        output.commit();
        ItemStack removed = controllers.remove(controllers.size() - 1);
        refreshUnlockedMaps();
        if (GTNHAdjust.debug) GTNHAdjust.LOG.info("Proxy controller returned: {}", removed);
        getBaseMetaTileEntity().markDirty();
    }

    private void refreshUnlockedMaps() {
        List<RecipeMap<?>> previous = unlockedMaps;
        String selected = previous != null && !previous.isEmpty()
            ? previous.get(Math.floorMod(machineMode, previous.size())).unlocalizedName
            : savedRecipeName;
        List<RecipeMap<?>> maps = new ArrayList<>(UniversalJointFactory.RECIPE_MAPS);
        if (!maps.contains(RecipeMaps.furnaceRecipes)) maps.add(RecipeMaps.furnaceRecipes);
        List<String> original = new ArrayList<>();
        for (RecipeMap<?> map : maps) original.add(map.unlocalizedName);
        List<List<String>> supplied = new ArrayList<>();
        for (ItemStack controller : controllers) supplied.add(ProxyFactoryControllerRegistry.maps(controller));
        for (String name : ProxyFactoryUnlockRules
            .orderedAdditional(original, ProxyFactoryRecipeWhitelist.ADDITIONAL, supplied)) {
            RecipeMap<?> map = RecipeMap.ALL_RECIPE_MAPS.get(name);
            if (map != null && !maps.contains(map)) maps.add(map);
        }
        unlockedMaps = Collections.unmodifiableList(maps);
        int index = 0;
        if (selected != null) {
            for (int i = 0; i < maps.size(); i++) {
                if (selected.equals(maps.get(i).unlocalizedName)) {
                    index = i;
                    break;
                }
            }
        } else if (previous == null) index = Math.floorMod(machineMode, maps.size());
        machineMode = index;
        savedRecipeName = null;
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
        writeControllers(tag);
        tag.setInteger("gtnhadjustProxyOperation", operationMode);
        RecipeMap<?> selected = getRecipeMap();
        if (selected != null) tag.setString("gtnhadjustProxySelectedMap", selected.unlocalizedName);
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        AENetworkProxy proxy = getProxy();
        if (proxy != null) proxy.readFromNBT(tag);
        controllers.clear();
        NBTTagList saved = tag.getTagList("gtnhadjustProxyControllers", 10);
        for (int i = 0; i < saved.tagCount(); i++) {
            ItemStack stack = ItemStack.loadItemStackFromNBT(saved.getCompoundTagAt(i));
            if (stack != null && stack.stackSize > 0) controllers.add(stack);
        }
        operationMode = Math.floorMod(tag.getInteger("gtnhadjustProxyOperation"), 3);
        savedRecipeName = tag.hasKey("gtnhadjustProxySelectedMap") ? tag.getString("gtnhadjustProxySelectedMap") : null;
        unlockedMaps = null;
        clientRecipeNames = null;
    }

    private void writeControllers(NBTTagCompound tag) {
        NBTTagList saved = new NBTTagList();
        for (ItemStack controller : controllers) saved.appendTag(controller.writeToNBT(new NBTTagCompound()));
        tag.setTag("gtnhadjustProxyControllers", saved);
    }

    @Override
    public void setItemNBT(NBTTagCompound tag) {
        super.setItemNBT(tag);
        if (controllers.isEmpty()) return;
        writeControllers(tag);
        RecipeMap<?> selected = getRecipeMap();
        if (selected != null) tag.setString("gtnhadjustProxySelectedMap", selected.unlocalizedName);
    }

    @Override
    public String[] getInfoData() {
        List<String> lines = new ArrayList<>();
        Collections.addAll(lines, super.getInfoData());
        lines.add(StatCollector.translateToLocal("gtnhadjust.proxy.operation." + operationMode));
        lines.add(
            StatCollector.translateToLocalFormatted(
                "gtnhadjust.proxy.count",
                controllers.size(),
                getAvailableRecipeMaps().size()));
        return lines.toArray(new String[0]);
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        List<RecipeMap<?>> maps = getAvailableRecipeMaps();
        return processingRecipeMap == null ? maps.get(Math.floorMod(machineMode, maps.size())) : processingRecipeMap;
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
            if (!maps.isEmpty()) return maps;
        }
        if (unlockedMaps == null) refreshUnlockedMaps();
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
    protected GTNRMultiBlockBaseGui<ProxyFactory> getGui() {
        return new ProxyFactoryGui(this);
    }

    @Override
    public boolean supportsMachineModeSwitch() {
        return true;
    }

    @Override
    public int nextMachineMode() {
        return (getMachineMode() + 1) % getAvailableRecipeMaps().size();
    }

    void changeRecipeMode(boolean previous) {
        int count = getAvailableRecipeMaps().size();
        setMachineMode(Math.floorMod(getMachineMode() + (previous ? -1 : 1), count));
        getBaseMetaTileEntity().markDirty();
    }

    @Override
    public void setMachineMode(int index) {
        if (index < 0) return;
        if (getBaseMetaTileEntity() != null && getBaseMetaTileEntity().isClientSide()) {
            super.setMachineMode(index);
        } else {
            super.setMachineMode(index % getAvailableRecipeMaps().size());
        }
    }

    @Override
    public String getMachineModeKey() {
        return getRecipeMap().unlocalizedName;
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
