package com.gtnhadjust.machine.collector;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.HatchElement.OutputHatch;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTStructureUtility.chainAllGlasses;
import static gregtech.api.util.GTStructureUtility.ofFrame;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.google.common.io.ByteArrayDataInput;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.gtnewhorizons.modularui.api.math.Alignment;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.common.widget.DynamicPositionedColumn;
import com.gtnewhorizons.modularui.common.widget.FakeSyncWidget;
import com.gtnewhorizons.modularui.common.widget.SlotWidget;
import com.gtnewhorizons.modularui.common.widget.TextWidget;
import com.gtnewhorizons.modularui.common.widget.textfield.NumericWidget;
import com.gtnhadjust.GTNHAdjust;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadOutputs;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadTask;
import com.xyp.gtneverreset.utils.enums.BlockIcons;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.GregTechAPI;
import gregtech.api.casing.Casings;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.gui.modularui.GTUITextures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.OverclockCalculator;
import gregtech.api.util.ParallelHelper;
import gregtech.api.util.shutdown.ShutDownReason;
import gregtech.api.util.shutdown.ShutDownReasonRegistry;
import gregtech.common.misc.WirelessNetworkManager;
import gregtech.common.render.IMTERenderer;
import io.netty.buffer.ByteBuf;

public final class SuperFragmentCollector extends MTEExtendedPowerMultiBlockBase<SuperFragmentCollector>
    implements ISurvivalConstructable, IMTERenderer {

    private static final String PIECE = "main";
    private static final short[] FIELD_BLUE = { 125, 225, 255, 255 };
    private static final String THREADS_NBT = "gtnhadjustCollectorThreads";
    private final CollectorThreadLedger threads = new CollectorThreadLedger();
    private int wirelessOverclockLimit;
    private int parallelLimit;
    private UUID wirelessOwner;
    private IStructureDefinition<SuperFragmentCollector> structure;
    private int glass = -1;
    private final String[] displayedThreads = new String[256];
    private boolean clientBlackHole;
    private boolean clientWirelessMode;
    // Actual aggregate EU/t after maintenance efficiency, synchronized for the green GUI power line.
    private long clientTotalPower;
    private boolean lastBlackHoleSent;
    @SideOnly(Side.CLIENT)
    private CollectorBlackHoleRenderer blackHoleRenderer;

    public SuperFragmentCollector(int id, String name, String localizedName) {
        super(id, name, localizedName);
    }

    private SuperFragmentCollector(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new SuperFragmentCollector(mName);
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return SuperFragmentRecipes.MAP;
    }

    @Override
    public boolean supportsBatchMode() {
        return true;
    }

    @Override
    public boolean supportsVoidProtection() {
        return true;
    }

    @Override
    public long getMaxInputVoltage() {
        if (wirelessMode()) return GTValues.V[14];
        long sum = 0;
        long highest = 0;
        for (gregtech.api.metatileentity.implementations.MTEHatch hatch : getExoticAndNormalEnergyHatchList()) {
            IGregTechTileEntity tile = hatch.getBaseMetaTileEntity();
            if (tile == null) continue;
            long voltage = Math.max(0, tile.getInputVoltage());
            highest = Math.max(highest, voltage);
            sum = CollectorThreadPolicy.addPower(sum, voltage);
        }
        return CollectorThreadPolicy.cappedVoltage(sum, highest);
    }

    public int maxParallel() {
        int limit = CollectorThreadPolicy.parallel(
            GTUtility.getTier(getMaxInputVoltage()),
            glass,
            GTNHAdjust.collectorBaseParallel,
            GTNHAdjust.collectorVoltageParallelMultiplier);
        return parallelLimit == 0 ? limit : Math.min(limit, parallelLimit);
    }

    /** The wireless grid supplies the controller only when no energy hatch is installed. */
    private boolean wirelessMode() {
        return GTNHAdjust.collectorWirelessPower && mEnergyHatches.isEmpty() && mExoticEnergyHatches.isEmpty();
    }

    /** Wireless scheduling uses the representable per-tick power limit without reading grid balance. */
    private long availablePower() {
        return wirelessMode() ? Long.MAX_VALUE - 1 : getMaxInputEu();
    }

    @Override
    public boolean onRunningTick(ItemStack stack) {
        if (!wirelessMode()) return super.onRunningTick(stack);
        UUID owner = getBaseMetaTileEntity().getOwnerUuid();
        if (owner != null && !owner.equals(wirelessOwner)) {
            WirelessNetworkManager.strongCheckOrAddUser(owner);
            wirelessOwner = owner;
        }
        if (owner == null || !WirelessNetworkManager.addEUToGlobalEnergyMap(owner, -getActualEnergyUsage())) {
            if (GTNHAdjust.debug) GTNHAdjust.LOG.info(
                "Collector wireless power request failed: owner={}, requested={} EU/t",
                owner,
                getActualEnergyUsage());
            stopMachine(ShutDownReasonRegistry.POWER_LOSS);
            return false;
        }
        return true;
    }

    public int maxThreads() {
        return CollectorThreadPolicy.threads(
            GTUtility.getTier(getMaxInputVoltage()),
            glass,
            GTNHAdjust.collectorBaseThreads,
            GTNHAdjust.collectorVoltageThreads);
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new CollectorProcessingLogic().setOverclock(2.0, 2.0)
            .setAmperageOC(false)
            .noRecipeCaching();
    }

    private static final class CollectorProcessingLogic extends ProcessingLogic {

        private int maxOverclocks = Integer.MAX_VALUE;
        private boolean wireless;
        private long wirelessBudget;

        void setMaxOverclocks(int maxOverclocks) {
            this.maxOverclocks = Math.max(0, maxOverclocks);
        }

        void setWireless(boolean wireless, long budget) {
            this.wireless = wireless;
            this.wirelessBudget = budget;
        }

        @Override
        protected ParallelHelper createParallelHelper(GTRecipe recipe) {
            ParallelHelper helper = super.createParallelHelper(recipe);
            return wireless ? helper.setAvailableEUt(wirelessBudget) : helper;
        }

        @Override
        protected OverclockCalculator createOverclockCalculator(GTRecipe recipe) {
            if (!wireless) return super.createOverclockCalculator(recipe).setMaxOverclocks(maxOverclocks);
            int duration = Math.max(1, (int) (recipe.mDuration * speedBoost / Math.pow(2, maxOverclocks)));
            return super.createOverclockCalculator(recipe).setDuration(duration)
                .setDurationModifier(1.0)
                .setNoOverclock(true);
        }
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return false;
    }

    /** Thread scheduling is handled by runMachine rather than the single-recipe GT entry point. */
    @Override
    public CheckRecipeResult checkProcessing() {
        return CheckRecipeResultRegistry.NO_RECIPE;
    }

    @Override
    protected void runMachine(IGregTechTileEntity tile, long tick) {
        if (!tile.isAllowedToWork()) {
            updateThreadDisplay();
            return;
        }
        if (maxParallel() == 0 || maxThreads() == 0) {
            if (!threads.isEmpty()) stopMachine(ShutDownReasonRegistry.POWER_LOSS);
            return;
        }
        long power = runningPower();
        if (GTNHAdjust.debug && tick % 200 == 0 && !threads.isEmpty()) {
            for (Map.Entry<String, RecipeThreadTask> entry : threads.entries()) {
                RecipeThreadTask task = entry.getValue();
                GTNHAdjust.LOG.info(
                    "Collector thread {}: slot={}, progress={}/{}, parallel={}, EU/t={}, finished={}, batch={}, voltage={}, capacity={}, used={}, glass={}",
                    entry.getKey(),
                    task.getThread(),
                    task.getProgress(),
                    task.getDuration(),
                    task.getParallels(),
                    task.getEUt(),
                    task.isFinished(),
                    isBatchModeEnabled(),
                    getMaxInputVoltage(),
                    getMaxInputEu(),
                    power,
                    glass);
            }
        }
        boolean running = threads.values()
            .stream()
            .anyMatch(this::isRunnableThread);
        if (running) {
            if (!wirelessMode() && power > Math.max(0, getMaxInputEu())) {
                stopMachine(ShutDownReasonRegistry.POWER_LOSS);
                return;
            }
            mEfficiency = Math.max(1000, 10000 - (getIdealStatus() - getRepairStatus()) * 1000);
            lEUt = -power;
            if (!doRandomMaintenanceDamage() || !onRunningTick(getControllerSlot())) return;
            if (!polluteEnvironment(getPollutionPerTick(getControllerSlot()))) {
                stopMachine(ShutDownReasonRegistry.POLLUTION_FAIL);
                return;
            }
            for (RecipeThreadTask task : threads.values()) if (isRunnableThread(task)) task.advance();
            markDirty();
        }
        boolean completed = false;
        for (Iterator<Map.Entry<String, RecipeThreadTask>> it = threads.entries()
            .iterator(); it.hasNext();) {
            RecipeThreadTask task = it.next()
                .getValue();
            if (task.isFinished() && task.getOutputs()
                .flush(this)) {
                it.remove();
                recipesDone += task.getParallels();
                outputAfterRecipe();
                completed = true;
                markDirty();
            }
        }
        if (completed || tick % 5 == 0 || tile.hasInventoryBeenModified() || tile.hasWorkJustBeenEnabled())
            startEligibleThreads();
        updateThreadDisplay();
    }

    private void startEligibleThreads() {
        if (!threads.isEmpty()) return;
        startRecipeProcessing();
        try {
            List<ItemStack> markers = new ArrayList<>();
            List<ItemStack> circuits = new ArrayList<>();
            Map<String, Integer> markerCounts = new java.util.HashMap<>();
            ItemStack controllerSlot = getStackInSlot(1);
            for (ItemStack item : getStoredInputs()) {
                if (item == null || item.stackSize <= 0 || item == controllerSlot) continue;
                GameRegistry.UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(item.getItem());
                if (id != null && CollectorMarkerPolicy.isMarker(id.modId, id.name)) {
                    markers.add(item);
                    String markerKey = id + ":" + item.getItemDamage();
                    markerCounts.merge(markerKey, Math.min(64, item.stackSize), (a, b) -> Math.min(64, a + b));
                } else if (GTUtility.isAnyIntegratedCircuit(item)) circuits.add(item);
            }
            List<PendingRecipe> pending = new ArrayList<>();
            java.util.Set<String> seen = new java.util.HashSet<>(threads.keys());
            for (ItemStack marker : markers) for (ItemStack circuit : circuits) {
                String key = recipeKey(marker, circuit);
                if (!seen.add(key)) continue;
                GTRecipe recipe = getRecipeMap().findRecipeQuery()
                    .items(marker, circuit)
                    .notUnificated(true)
                    .find();
                if (recipe == null || recipe.mFluidInputs.length != 0 || recipe.mEUt <= 0) continue;
                if (recipe.mEUt > OverclockCalculator.getMaxAllowedRecipeEUt(getMaxInputVoltage(), 1)) continue;
                boolean consumes = false;
                for (ItemStack input : recipe.mInputs) if (input != null && input.stackSize != 0) consumes = true;
                if (!consumes) {
                    GameRegistry.UniqueIdentifier markerId = GameRegistry.findUniqueIdentifierFor(marker.getItem());
                    int count = GTNHAdjust.collectorMarkerOutputMultiplier
                        ? markerCounts.get(markerId + ":" + marker.getItemDamage())
                        : 1;
                    pending.add(new PendingRecipe(marker, circuit, key, recipe, count));
                }
            }
            int percent = CollectorThreadPolicy.glassEnergyPercent(glass);
            long[] recipeEUts = new long[pending.size()];
            int[] durations = new int[pending.size()];
            for (int i = 0; i < pending.size(); i++) {
                GTRecipe recipe = pending.get(i).recipe;
                recipeEUts[i] = recipe.mEUt;
                durations[i] = recipe.mDuration;
            }
            long available = CollectorThreadPolicy.remainingPower(availablePower(), runningPower());
            CollectorThreadPolicy.BalancedAllocation allocation = CollectorThreadPolicy.allocateBalanced(
                available,
                getMaxInputVoltage(),
                recipeEUts,
                durations,
                maxThreads() - threads.size(),
                maxParallel(),
                percent,
                wirelessMode(),
                wirelessOverclockLimit);
            for (int i = 0; i < pending.size(); i++) {
                if (allocation.budgets[i] > 0)
                    startThread(pending.get(i), allocation.budgets[i], allocation.parallel, allocation.overclocks[i]);
            }
        } finally {
            endRecipeProcessing();
        }
    }

    private static final class PendingRecipe {

        final ItemStack marker;
        final ItemStack circuit;
        final String key;
        final GTRecipe recipe;
        final int markerCount;

        PendingRecipe(ItemStack marker, ItemStack circuit, String key, GTRecipe recipe, int markerCount) {
            this.marker = marker;
            this.circuit = circuit;
            this.key = key;
            this.recipe = recipe;
            this.markerCount = markerCount;
        }
    }

    private boolean startThread(PendingRecipe pending, long budget, int selectedParallel, int selectedOverclocks) {
        boolean wireless = wirelessMode();
        long remaining = Math.min(budget, CollectorThreadPolicy.remainingPower(availablePower(), runningPower()));
        if (remaining == 0) return false;
        GTRecipe candidate = pending.recipe;
        ItemStack marker = pending.marker;
        ItemStack circuit = pending.circuit;
        String key = pending.key;
        if (!(processingLogic instanceof CollectorProcessingLogic)) return false;
        CollectorProcessingLogic logic = (CollectorProcessingLogic) processingLogic;
        setupProcessingLogic(logic);
        logic.setWireless(wireless, remaining);
        long voltage = getMaxInputVoltage();
        if (voltage <= 0) return false;
        logic.setEuModifier(
            CollectorThreadPolicy.glassEnergyPercent(glass) / 100.0 * (wireless ? Math.pow(2, selectedOverclocks) : 1))
            .setSpeedBonus(CollectorThreadPolicy.glassEnergyPercent(glass) / 100.0)
            .setRecipeLocking(this, false)
            .setAvailableVoltage(voltage)
            .setAvailableAmperage(wireless ? 1 : Math.max(1, selectedParallel))
            .setAmperageOC(false)
            .setMaxParallel(selectedParallel)
            .setInputItems(marker.copy(), circuit.copy())
            .setInputFluids();
        logic.setMaxOverclocks(selectedOverclocks);
        CheckRecipeResult result = logic.process();
        if (!result.wasSuccessful()) {
            if (GTNHAdjust.debug && mTotalRunTime % 200 < 5) GTNHAdjust.LOG.info(
                "Collector cannot start {}: result={}, recipeEUt={}, inputVoltage={}, remainingPower={}, selectedParallel={}, batch={}",
                key,
                result.getID(),
                candidate.mEUt,
                voltage,
                remaining,
                selectedParallel,
                isBatchModeEnabled());
            return false;
        }
        long eut = logic.getCalculatedEut();
        if (eut < 0 || eut > remaining) return false;
        int slot = freeSlot();
        if (slot < 0) return false;
        RecipeThreadTask task = new RecipeThreadTask(
            slot,
            eut,
            Math.max(1, logic.getDuration()),
            Math.max(1, logic.getCurrentParallels()),
            scaledOutputs(logic.getOutputItems(), logic.getOutputFluids(), pending.markerCount));
        if (!threads.add(key, task, maxThreads())) return false;
        mEfficiency = Math.max(1000, 10000 - (getIdealStatus() - getRepairStatus()) * 1000);
        if (GTNHAdjust.debug) GTNHAdjust.LOG.info(
            "Collector started thread {}: {} parallels, {} ticks, {} EU/t; shared parallel {}, overclocks {}, power budget {} EU/t, voltage {}, batch {}, marker multiplier {}",
            key,
            task.getParallels(),
            task.getDuration(),
            eut,
            selectedParallel,
            selectedOverclocks,
            remaining,
            voltage,
            isBatchModeEnabled(),
            pending.markerCount);
        markDirty();
        return true;
    }

    /** Store multiplied output quantities in the existing arbitrary-precision thread ledger. */
    private static RecipeThreadOutputs scaledOutputs(ItemStack[] items, net.minecraftforge.fluids.FluidStack[] fluids,
        int multiplier) {
        RecipeThreadOutputs outputs = new RecipeThreadOutputs(items, fluids);
        if (multiplier == 1) return outputs;
        NBTTagCompound saved = outputs.save();
        BigInteger factor = BigInteger.valueOf(multiplier);
        for (String[] kind : new String[][] { { "items", "amount" }, { "fluids", "bigAmount" } }) {
            NBTTagList entries = saved.getTagList(kind[0], 10);
            for (int i = 0; i < entries.tagCount(); i++) {
                NBTTagCompound entry = entries.getCompoundTagAt(i);
                entry.setString(
                    kind[1],
                    new BigInteger(entry.getString(kind[1])).multiply(factor)
                        .toString());
            }
        }
        return RecipeThreadOutputs.load(saved);
    }

    private static String recipeKey(ItemStack marker, ItemStack circuit) {
        GameRegistry.UniqueIdentifier markerId = GameRegistry.findUniqueIdentifierFor(marker.getItem());
        GameRegistry.UniqueIdentifier circuitId = GameRegistry.findUniqueIdentifierFor(circuit.getItem());
        return markerId + ":" + marker.getItemDamage() + "|" + circuitId + ":" + circuit.getItemDamage();
    }

    private int freeSlot() {
        for (int i = 0; i < maxThreads(); i++) {
            boolean used = false;
            for (RecipeThreadTask task : threads.values()) if (task.getThread() == i) {
                used = true;
                break;
            }
            if (!used) return i;
        }
        return -1;
    }

    private long runningPower() {
        long power = 0;
        for (RecipeThreadTask task : threads.values())
            if (isRunnableThread(task)) power = CollectorThreadPolicy.addPower(power, task.getEUt());
        return power;
    }

    private boolean isRunnableThread(RecipeThreadTask task) {
        return task.getThread() < maxThreads() && !task.isFinished();
    }

    private void updateThreadDisplay() {
        mOutputItems = null;
        mOutputFluids = null;
        mEUt = 0;
        mProgresstime = 0;
        mMaxProgresstime = 0;
        lEUt = 0;
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (tile == null || !tile.isAllowedToWork()) return;
        for (RecipeThreadTask task : threads.values()) if (isRunnableThread(task)) {
            mProgresstime = task.getProgress();
            mMaxProgresstime = task.getDuration();
            break;
        }
        if (mMaxProgresstime > 0) lEUt = -runningPower();
    }

    @Override
    public void stopMachine(ShutDownReason reason) {
        int cleared = threads.size();
        super.stopMachine(reason);
        threads.clear();
        if (cleared > 0 && GTNHAdjust.debug) GTNHAdjust.LOG.info(
            "Collector stopped ({}); discarded {} thread tasks and their buffered outputs",
            reason.getID(),
            cleared);
        if (cleared > 0) markDirty();
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        tag.setTag(THREADS_NBT, threads.save());
        tag.setInteger("collectorWirelessOverclockLimit", wirelessOverclockLimit);
        tag.setInteger("collectorParallelLimit", parallelLimit);
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        wirelessOverclockLimit = Math.max(0, tag.getInteger("collectorWirelessOverclockLimit"));
        parallelLimit = Math.max(0, tag.getInteger("collectorParallelLimit"));
        if (tag.hasKey(THREADS_NBT, 9)) threads.load(tag.getTagList(THREADS_NBT, 10));
        else threads.clear();
        updateThreadDisplay();
    }

    @Override
    protected boolean useMui2() {
        return false;
    }

    @Override
    public boolean showRecipeTextInGUI() {
        return false;
    }

    /** Replace the native running label with total power while retaining idle and failure messages below. */
    @Override
    public boolean showMachineStatusInGUI() {
        return false;
    }

    @Override
    public ModularWindow createPowerPanel(EntityPlayer player) {
        return ModularWindow.builder(160, 110)
            .setBackground(GTUITextures.BACKGROUND_SINGLEBLOCK_DEFAULT)
            .setDraggable(true)
            .widget(
                TextWidget.localised("gtnhadjust.collector.parallel_limit")
                    .setPos(8, 6)
                    .setSize(144, 16))
            .widget(
                new NumericWidget().setGetter(() -> parallelLimit == 0 ? maxParallel() : parallelLimit)
                    .setSetter(value -> {
                        parallelLimit = (int) value;
                        markDirty();
                    })
                    .setBounds(0, Integer.MAX_VALUE)
                    .setScrollValues(1, 4, 64)
                    .setBackground(GTUITextures.BACKGROUND_TEXT_FIELD)
                    .setPos(8, 24)
                    .setSize(144, 18))
            .widget(
                TextWidget.localised("gtnhadjust.collector.wireless_oc")
                    .setPos(8, 52)
                    .setSize(144, 16))
            .widget(
                new NumericWidget().setGetter(() -> wirelessOverclockLimit)
                    .setSetter(value -> {
                        wirelessOverclockLimit = (int) value;
                        markDirty();
                    })
                    .setBounds(0, Integer.MAX_VALUE)
                    .setScrollValues(1, 4, 16)
                    .setBackground(GTUITextures.BACKGROUND_TEXT_FIELD)
                    .setPos(8, 70)
                    .setSize(144, 18))
            .build();
    }

    /** Display aggregate power in place of the native running label, followed by the existing thread rows. */
    @Override
    protected void drawTexts(DynamicPositionedColumn screenElements, SlotWidget inventorySlot) {
        screenElements.widget(
            TextWidget.dynamicString(() -> StatCollector.translateToLocal("gtnhadjust.collector.wireless_mode"))
                .setSynced(false)
                .setEnabled(widget -> clientWirelessMode));
        screenElements
            .widget(new FakeSyncWidget.BooleanSyncer(this::wirelessMode, value -> clientWirelessMode = value));
        super.drawTexts(screenElements, inventorySlot);
        screenElements.widget(new FakeSyncWidget.IntegerSyncer(this::getErrorDisplayID, this::setErrorDisplayID))
            .widget(
                new FakeSyncWidget.BooleanSyncer(
                    () -> getBaseMetaTileEntity().isActive(),
                    value -> getBaseMetaTileEntity().setActive(value)))
            .widget(new FakeSyncWidget.LongSyncer(this::getActualEnergyUsage, value -> clientTotalPower = value));
        screenElements.widget(
            TextWidget.dynamicString(
                () -> StatCollector
                    .translateToLocalFormatted("gtnhadjust.collector.total_power", formatTotalPower(clientTotalPower)))
                .setSynced(false)
                .setTextAlignment(Alignment.CenterLeft)
                .setDefaultColor(0x55FF55)
                .setEnabled(widget -> getErrorDisplayID() == 0 && getBaseMetaTileEntity().isActive()));
        for (int i = 1; i <= 3; i++) {
            screenElements.widget(
                TextWidget.localised("gt.interact.desc.mb.idle." + i)
                    .setDefaultColor(0xFFFFFF)
                    .setEnabled(widget -> getErrorDisplayID() == 0 && !getBaseMetaTileEntity().isActive()));
        }
        for (int i = 0; i < displayedThreads.length; i++) {
            final int row = i;
            screenElements.widget(
                TextWidget.dynamicString(() -> threadDisplayText(displayedThreadData(row)))
                    .setSynced(false)
                    .setDefaultColor(0xFFFFFF)
                    .setEnabled(widget -> !displayedThreadData(row).isEmpty()));
            screenElements.widget(
                new FakeSyncWidget.StringSyncer(() -> threadDisplayData(row), value -> displayedThreads[row] = value));
        }
    }

    /** Promote only after a full amp of the next GT voltage tier; truncate fractional amps to three decimals. */
    private static String formatTotalPower(long power) {
        int tier = 0;
        // V[15] is GT's error tier, so values beyond MAX remain expressed as amps of MAX.
        while (tier < 14 && power >= GTValues.V[tier + 1]) tier++;
        String amps = BigDecimal.valueOf(power)
            .divide(BigDecimal.valueOf(GTValues.V[tier]), 3, RoundingMode.DOWN)
            .stripTrailingZeros()
            .toPlainString();
        return amps + "A " + GTValues.VN[tier];
    }

    private String displayedThreadData(int row) {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (tile != null && tile.isServerSide()) return threadDisplayData(row);
        return displayedThreads[row] == null ? "" : displayedThreads[row];
    }

    private String threadDisplayData(int row) {
        for (Map.Entry<String, RecipeThreadTask> entry : threads.entries()) {
            RecipeThreadTask task = entry.getValue();
            if (task.getThread() == row) {
                return entry.getKey() + ";" + task.getProgress() + ";" + task.getDuration() + ";" + task.getParallels();
            }
        }
        return "";
    }

    private static String threadDisplayText(String data) {
        if (data == null || data.isEmpty()) return "";
        try {
            String[] fields = data.split(";");
            String[] inputs = fields[0].split("\\|");
            String[] marker = inputs[0].split(":");
            String[] circuit = inputs[1].split(":");
            net.minecraft.item.Item item = GameRegistry.findItem(marker[0], marker[1]);
            String dimension = item == null ? marker[1]
                : new ItemStack(item, 1, Integer.parseInt(marker[2])).getDisplayName();
            int mode = Integer.parseInt(circuit[circuit.length - 1]);
            String type = StatCollector.translateToLocal("gtnhadjust.collector.mode." + mode);
            return String.format(
                java.util.Locale.ROOT,
                "%s %s %.2fs / %.2fs ×%d",
                dimension,
                type,
                Integer.parseInt(fields[1]) / 20.0,
                Integer.parseInt(fields[2]) / 20.0,
                Integer.parseInt(fields[3]));
        } catch (RuntimeException error) {
            return data;
        }
    }

    @Override
    public String[] getInfoData() {
        String[] base = super.getInfoData();
        String[] result = java.util.Arrays.copyOf(base, base.length + 2);
        long running = threads.values()
            .stream()
            .filter(this::isRunnableThread)
            .count();
        result[base.length] = "运行线程: " + running + " / " + maxThreads();
        result[base.length + 1] = "每线程最大并行: " + maxParallel();
        return result;
    }

    @Override
    public IStructureDefinition<SuperFragmentCollector> getStructureDefinition() {
        if (structure == null) structure = StructureDefinition.<SuperFragmentCollector>builder()
            .addShape(PIECE, CollectorStructureShape.create())
            .addElement('B', ofBlock(GregTechAPI.sBlockCasings10, 12))
            .addElement('D', ofFrame(Materials.NaquadahAlloy))
            .addElement(
                'A',
                chainAllGlasses(-1, (SuperFragmentCollector t, Integer tier) -> t.glass = tier, t -> t.glass))
            .addElement(
                'C',
                buildHatchAdder(SuperFragmentCollector.class)
                    .atLeast(InputBus, OutputBus, InputHatch, OutputHatch, Energy.or(ExoticEnergy), Maintenance)
                    .casingIndex(Casings.ExtremeDensitySpaceBendingCasing.getTextureId())
                    .hint(1)
                    .buildAndChain(ofBlock(GregTechAPI.sBlockCasings10, 11)))
            .build();
        return structure;
    }

    private static int verticalOffset() {
        return CollectorStructureShape.HEIGHT - CollectorStructureShape.CONTROLLER_Y - 1;
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece(
            PIECE,
            stack,
            hintsOnly,
            CollectorStructureShape.CONTROLLER_X,
            verticalOffset(),
            CollectorStructureShape.CONTROLLER_Z);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int budget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        return survivalBuildPiece(
            PIECE,
            stack,
            CollectorStructureShape.CONTROLLER_X,
            verticalOffset(),
            CollectorStructureShape.CONTROLLER_Z,
            budget,
            env,
            false,
            true);
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        glass = -1;
        if (checkPiece(
            PIECE,
            CollectorStructureShape.CONTROLLER_X,
            verticalOffset(),
            CollectorStructureShape.CONTROLLER_Z,
            errors)) collectTooltipStructureRequirements(errors);
    }

    @Override
    public void onPostTick(IGregTechTileEntity tile, long tick) {
        super.onPostTick(tile, tick);
        if (!tile.isServerSide()) return;
        boolean visible = mMachine && GTNHAdjust.collectorBlackHole;
        if (visible != lastBlackHoleSent || tick % 100 == 0) {
            if (visible != lastBlackHoleSent && GTNHAdjust.debug)
                GTNHAdjust.LOG.info("Collector black hole visibility: {}", visible);
            lastBlackHoleSent = visible;
            sendRenderDataToClient(this);
        }
    }

    @Override
    public void encodeRenderData(ByteBuf buffer) {
        buffer.writeBoolean(mMachine && GTNHAdjust.collectorBlackHole);
    }

    @Override
    public void decodeRenderData(ByteArrayDataInput buffer) {
        clientBlackHole = buffer.readBoolean();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderTESR(double x, double y, double z, float partialTick) {
        if (!clientBlackHole) {
            blackHoleRenderer = null;
            return;
        }
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (tile == null || tile.getWorld() == null) return;
        int[] offset = new int[3];
        getExtendedFacing().getWorldOffset(
            new int[] { CollectorStructureShape.BLACK_HOLE_X - CollectorStructureShape.CONTROLLER_X,
                CollectorStructureShape.CONTROLLER_Y - CollectorStructureShape.BLACK_HOLE_Y,
                CollectorStructureShape.BLACK_HOLE_Z - CollectorStructureShape.CONTROLLER_Z },
            offset);
        if (blackHoleRenderer == null) blackHoleRenderer = new CollectorBlackHoleRenderer(tile.getWorld());
        blackHoleRenderer.render(x + offset[0], y + offset[1], z + offset[2], partialTick, getExtendedFacing());
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox(int x, int y, int z) {
        return IMTERenderer.super.getRenderBoundingBox(x, y, z)
            .expand(CollectorStructureShape.WIDTH, CollectorStructureShape.HEIGHT, CollectorStructureShape.DEPTH);
    }

    @Override
    public double getMaxRenderDistanceSquared() {
        return 16384.0D;
    }

    public void collectTooltipStructureRequirements(List<StructureError> errors) {
        checkHatchMin(errors, InputBus, 1);
        checkHatchMin(errors, OutputBus, 1);
        checkHatchMin(errors, OutputHatch, 1);
        if (!GTNHAdjust.collectorWirelessPower && mEnergyHatches.isEmpty() && mExoticEnergyHatches.isEmpty())
            checkHatchMin(errors, Energy, 1);
        if (shouldCheckMaintenance()) checkHatchMin(errors, Maintenance, 1);
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        ITexture casing = Casings.ExtremeDensitySpaceBendingCasing.getCasingTexture();
        if (side != facing) return new ITexture[] { casing };
        IIconContainer icon = active ? BlockIcons.FragmentCollectorFrontActive : BlockIcons.FragmentCollectorFront;
        return new ITexture[] { casing, TextureFactory.builder()
            .addIcon(icon)
            .setRGBA(FIELD_BLUE)
            .build() };
    }

    @Override
    public MultiblockTooltipBuilder createTooltip() {
        return new MultiblockTooltipBuilder().addMachineType("物质解压器")
            .addInfo("输入维度标记与 20–24 号电路采集对应维度资源")
            .addInfo("20 矿石、21 宝石、22 粉末、23 锭、24 流体")
            .addInfo("标记和电路不消耗；不同配方可同时运行")
            .addInfo(GTNHAdjust.collectorMarkerOutputMultiplier ? "对应维度标记数量放大产出，最多 64 倍" : "维度标记产出倍率已关闭")
            .addInfo("基础耗时 " + GTNHAdjust.collectorRecipeTicks + " tick，支持无损超频与批处理")
            .addInfo(
                "LV " + GTNHAdjust.collectorBaseParallel
                    + " 并行，电压每升一级并行 ×"
                    + GTNHAdjust.collectorVoltageParallelMultiplier)
            .addInfo("多个能源仓升压最多达到最高仓的上一电压等级")
            .addInfo("LV 电压与 HV 玻璃提供 " + GTNHAdjust.collectorBaseThreads + " 线程")
            .addInfo("电压每升一级增加 " + GTNHAdjust.collectorVoltageThreads + " 线程，玻璃每升一级增加 4 线程")
            .addInfo("HV 玻璃耗能及耗时为 100%，每升一级减少 15%，最低 1%")
            .beginStructureBlock(
                CollectorStructureShape.WIDTH,
                CollectorStructureShape.HEIGHT,
                CollectorStructureShape.DEPTH,
                true)
            .addController("与原版亚稳态黑洞遏制场主机位置相同，底部第六层")
            .addCasing("最多 3671", Casings.ExtremeDensitySpaceBendingCasing.getLocalizedName(), false)
            .addCasing("988", Casings.BackgroundRadiationAbsorbentCasing.getLocalizedName(), false)
            .addCasing("144", "硅岩合金框架", false)
            .addCasing("64", "同一等级的等级玻璃，替换原霍金辐射重组焦点", false)
            .addInputBus("1+", "任意超高密度弯曲机械方块", 1)
            .addOutputBus("1+", "任意超高密度弯曲机械方块", 1)
            .addInputHatch("0+", "任意超高密度弯曲机械方块", 1)
            .addOutputHatch("1+", "任意超高密度弯曲机械方块", 1)
            .addEnergyHatch("0+", "任意超高密度弯曲机械方块；无能源仓时使用无线电网", 1)
            .addMaintenanceHatch("1+", "任意超高密度弯曲机械方块", 1)
            .addAir("黑洞核心空间")
            .toolTipFinisher();
    }
}
