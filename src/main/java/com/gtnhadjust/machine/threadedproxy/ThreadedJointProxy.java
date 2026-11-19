package com.gtnhadjust.machine.threadedproxy;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnhadjust.GTNHAdjust;
import com.gtnhadjust.compat.GtnrToolNetwork;
import com.gtnhadjust.machine.proxy.ProxyLocatorHost;
import com.gtnhadjust.machine.proxy.ProxyLocatorInventory;
import com.gtnhadjust.machine.proxy.ProxyLocatorPool;
import com.gtnhadjust.machine.proxy.ProxyThreadOutputs;
import com.gtnhadjust.mixins.gtnr.ThreadedProxyOutputAccessor;
import com.xyp.gtneverreset.common.machine.hatch.CrossRecipeMEPatternBuffer;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadOutputs;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadTask;
import com.xyp.gtneverreset.common.me.CircuitPatternCodec;
import com.xyp.gtneverreset.common.me.PatternRecipeMap;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.me.helpers.AENetworkProxy;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.Textures;
import gregtech.api.enums.VoidingMode;
import gregtech.api.enums.VoltageIndex;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.metatileentity.implementations.MTEHatchOutput;
import gregtech.api.metatileentity.implementations.MTEHatchOutputBus;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipe.RecipeItemInput;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.OverclockCalculator;
import gregtech.api.util.shutdown.ShutDownReason;
import gregtech.api.util.shutdown.ShutDownReasonRegistry;
import gregtech.common.misc.WirelessNetworkManager;
import gregtech.common.tileentities.machines.IDualInputHatch;
import gregtech.common.tileentities.machines.MTEHatchInputBusME;
import gregtech.common.tileentities.machines.MTEHatchInputME;
import gtPlusPlus.xmod.gregtech.api.metatileentity.implementations.MTEHatchReservoir;

/** One physical pattern slot defines one thread; crafting-buffer deliveries are never machine ingredients. */
public final class ThreadedJointProxy extends MTEExtendedPowerMultiBlockBase<ThreadedJointProxy>
    implements ISurvivalConstructable, ProxyLocatorHost, appeng.me.helpers.IGridProxyable {

    private static final String THREADS = "gtnhadjustThreadedProxyTasks";
    private IStructureDefinition<ThreadedJointProxy> structure;
    private final List<CrossRecipeMEPatternBuffer> patternHatches = new ArrayList<>();
    private final Map<String, RecipeThreadTask> tasks = new LinkedHashMap<>();
    private final Map<String, ThreadedProxyDisplay.RunInfo> runInfo = new LinkedHashMap<>();
    private final Map<String, Failure> failures = new LinkedHashMap<>();
    private final Map<String, Definition> definitionIndex = new LinkedHashMap<>();
    private List<Definition> definitions = Collections.emptyList();
    private List<PatternSlot> snapshot = Collections.emptyList();
    private final ProxyLocatorInventory locatorInventory = new ProxyLocatorInventory(this);
    private final ProxyLocatorPool locatorPool = new ProxyLocatorPool();
    private int operationMode = 1;
    private boolean definitionsDirty = true;
    private boolean refreshRequested;
    private boolean hasWaterReservoir;
    private boolean previousWaterReservoir;
    private int maxOverclocks;
    private UUID wirelessOwner;
    private appeng.me.helpers.AENetworkProxy gridProxy;
    private final ThreadPatternInventory patterns = new ThreadPatternInventory(this);

    public ThreadedJointProxy(int id, String name, String localizedName) {
        super(id, name, localizedName);
    }

    private ThreadedJointProxy(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new ThreadedJointProxy(mName);
    }

    @Override
    public Collection<RecipeMap<?>> getAvailableRecipeMaps() {
        return locatorPool.recipeMaps();
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return getAvailableRecipeMaps().stream()
            .findFirst()
            .orElse(null);
    }

    @Override
    public ProxyLocatorInventory getLocatorInventory() {
        return locatorInventory;
    }

    @Override
    public ProxyLocatorPool getLocatorPool() {
        return locatorPool;
    }

    @Override
    public boolean canEditLocators() {
        return operationMode == 1;
    }

    @Override
    public int getOperationMode() {
        return operationMode;
    }

    @Override
    public void setClientOperationMode(int mode) {
        operationMode = mode == 1 ? 1 : 0;
    }

    /** Input mode locks out new threads while committed tasks retain their progress. */
    @Override
    public void cycleOperationMode() {
        operationMode = 1 - operationMode;
        markDirty();
    }

    /** Reindex controller capabilities only when slots change; live source checks occur at thread admission. */
    @Override
    public void onLocatorInventoryChanged() {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (tile == null || tile.isClientSide()) return;
        List<ItemStack> locators = new ArrayList<>();
        for (int i = 0; i < locatorInventory.getSlots(); i++) {
            ItemStack stack = locatorInventory.getStackInSlot(i);
            if (stack != null) locators.add(stack);
        }
        locatorPool.replace(locators);
        definitionsDirty = true;
        markDirty();
    }

    List<CrossRecipeMEPatternBuffer> patternHatches() {
        return patternHatches;
    }

    /** Native bottom region accepts the defining assembly, ordinary inputs and normal/exotic energy. */
    boolean addProxyInputHatch(IGregTechTileEntity tile, int texture) {
        if (tile == null) return false;
        IMetaTileEntity meta = tile.getMetaTileEntity();
        if (meta instanceof CrossRecipeMEPatternBuffer hatch) {
            if (!addToMachineList(tile, texture)) return false;
            patternHatches.add(hatch);
            return true;
        }
        if (meta instanceof IDualInputHatch) return false;
        if (meta instanceof MTEHatchInput || meta instanceof MTEHatchInputBus) {
            boolean added = addToMachineList(tile, texture);
            if (added && meta instanceof MTEHatchReservoir) hasWaterReservoir = true;
            return added;
        }
        return addEnergyInputToMachineList(tile, texture) || addExoticEnergyInputToMachineList(tile, texture);
    }

    /** Native top region accepts ordinary output buses and fluid output hatches. */
    boolean addProxyOutputHatch(IGregTechTileEntity tile, int texture) {
        if (tile == null) return false;
        IMetaTileEntity meta = tile.getMetaTileEntity();
        return (meta instanceof MTEHatchOutput || meta instanceof MTEHatchOutputBus) && addToMachineList(tile, texture);
    }

    @Override
    public void clearHatches() {
        super.clearHatches();
        patternHatches.clear();
        hasWaterReservoir = false;
    }

    /** The native GUI button schedules a structure check for the next server tick. Extend that exact request. */
    @Override
    public void setStructureUpdateTime(int time) {
        super.setStructureUpdateTime(time);
        if (time == 1 && getBaseMetaTileEntity().isServerSide()) refreshRequested = true;
    }

    /**
     * Re-read locators and all patterns after the new hatch lists are known, even while the machine is paused.
     */
    @Override
    protected void onStructureCheckFinished(IGregTechTileEntity tile) {
        super.onStructureCheckFinished(tile);
        if (!refreshRequested) return;
        refreshRequested = false;
        onLocatorInventoryChanged();
        definitionsDirty = true;
        ((ThreadedProxyProcessingLogic) processingLogic).discardPreparedRecipe();
        refreshDefinitions();
        if (GTNHAdjust.debug) GTNHAdjust.LOG.info(
            "[threaded-proxy] manual refresh: formed={}, maps={}, installed={}, valid={}",
            mMachine,
            locatorPool.recipeMaps(),
            snapshot.size(),
            definitions.size());
    }

    @Override
    public IStructureDefinition<ThreadedJointProxy> getStructureDefinition() {
        if (structure == null) structure = ThreadedProxyStructure.create();
        return structure;
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        checkPiece(
            ThreadedProxyStructure.PIECE,
            ThreadedProxyStructure.X,
            ThreadedProxyStructure.Y,
            ThreadedProxyStructure.Z,
            errors);
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece(
            ThreadedProxyStructure.PIECE,
            stack,
            hintsOnly,
            ThreadedProxyStructure.X,
            ThreadedProxyStructure.Y,
            ThreadedProxyStructure.Z);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int budget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        return survivalBuildPiece(
            ThreadedProxyStructure.PIECE,
            stack,
            ThreadedProxyStructure.X,
            ThreadedProxyStructure.Y,
            ThreadedProxyStructure.Z,
            budget,
            env,
            false,
            true);
    }

    @Override
    public boolean shouldCheckMaintenance() {
        return false;
    }

    @Override
    public boolean supportsBatchMode() {
        return true;
    }

    @Override
    public boolean getDefaultBatchMode() {
        return true;
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return false;
    }

    @Override
    public int getMaxParallelRecipes() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean supportsVoidProtection() {
        return true;
    }

    @Override
    public VoidingMode getDefaultVoidingMode() {
        return VoidingMode.VOID_NONE;
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new ThreadedProxyProcessingLogic().noRecipeCaching();
    }

    /** With no normal or exotic energy hatch, the optional wireless supply uses the owner's GT account. */
    public boolean isWirelessMode() {
        return GTNHAdjust.threadedProxyWirelessPower && mEnergyHatches.isEmpty() && mExoticEnergyHatches.isEmpty();
    }

    @Override
    public long getMaxInputVoltage() {
        return isWirelessMode() ? GTValues.V[VoltageIndex.MAX] : super.getMaxInputVoltage();
    }

    @Override
    public long getMaxInputEu() {
        return isWirelessMode() ? Long.MAX_VALUE - 1 : super.getMaxInputEu();
    }

    /** Debit actual aggregate EU/t once per tick; insufficient credit follows the existing power-loss policy. */
    @Override
    public boolean onRunningTick(ItemStack stack) {
        if (!isWirelessMode()) return super.onRunningTick(stack);
        UUID owner = getBaseMetaTileEntity().getOwnerUuid();
        if (owner != null && !owner.equals(wirelessOwner)) {
            WirelessNetworkManager.strongCheckOrAddUser(owner);
            wirelessOwner = owner;
        }
        BigInteger cost = BigInteger.valueOf(-lEUt);
        if (owner == null || !WirelessNetworkManager.addEUToGlobalEnergyMap(owner, cost.negate())) {
            if (GTNHAdjust.debug)
                GTNHAdjust.LOG.info("[threaded-proxy] wireless power loss: owner={}, requested={} EU/t", owner, cost);
            stopMachine(ShutDownReasonRegistry.POWER_LOSS);
            return false;
        }
        return true;
    }

    @Override
    public CheckRecipeResult checkProcessing() {
        return CheckRecipeResultRegistry.NO_RECIPE;
    }

    /** Pattern configuration is available even before constructing the multiblock. */
    @Override
    public void onPostTick(IGregTechTileEntity tile, long tick) {
        if (tile.isServerSide() && tick % 5 == 0) {
            definitionsDirty |= locatorPool.refreshIndirect(this);
            refreshDefinitions();
        }
        super.onPostTick(tile, tick);
    }

    /** Existing tasks keep their inputs, progress and yields when definitions change. */
    @Override
    protected void runMachine(IGregTechTileEntity tile, long tick) {
        // Definition visibility is independent of the work switch; paused machines still show installed patterns.
        if (definitionsDirty || tile.hasInventoryBeenModified() || tile.hasWorkJustBeenEnabled()) refreshDefinitions();
        if (!tile.isAllowedToWork()) {
            updateDisplay();
            return;
        }
        long power = runningPower();
        if (power > 0) {
            mEfficiency = 10000;
            lEUt = -power;
            if (power > getMaxInputEu() || !onRunningTick(getControllerSlot())) {
                if (tile.isAllowedToWork()) stopMachine(ShutDownReasonRegistry.POWER_LOSS);
                return;
            }
        }
        for (RecipeThreadTask task : tasks.values()) if (!task.isFinished()) task.advance();
        boolean completed = false;
        for (Iterator<RecipeThreadTask> it = tasks.values()
            .iterator(); it.hasNext();) {
            RecipeThreadTask task = it.next();
            if (task.isFinished() && flushOutputs(task.getOutputs())) {
                it.remove();
                recipesDone += task.getParallels();
                outputAfterRecipe();
                completed = true;
            }
        }
        runInfo.keySet()
            .retainAll(tasks.keySet());
        if (!tasks.isEmpty() || completed) markDirty();
        if (completed || tick % 5 == 0 || tile.hasInventoryBeenModified() || tile.hasWorkJustBeenEnabled())
            startThreads();
        updateDisplay();
    }

    /** Launch in physical slot order using resources remaining after each committed thread. */
    private void startThreads() {
        refreshDefinitions();
        if (canEditLocators() || definitions.isEmpty()) return;
        startRecipeProcessing();
        try {
            long remaining = Math.max(0, getMaxInputEu() - runningPower());
            long voltage = getMaxInputVoltage();
            List<AENetworkProxy> toolNetworks = GTNHAdjust.gtnrVirtualTools ? toolNetworks() : Collections.emptyList();
            for (Definition definition : definitions) {
                if (tasks.containsKey(definition.key)) continue;
                CheckRecipeResult source = locatorPool.checkStart(this, definition.map, definition.recipe.recipe);
                if (!source.wasSuccessful()) {
                    failures.put(definition.key, new Failure(source));
                    continue;
                }
                ThreadInputs input = selectInputs(definition, getStoredInputs(), getStoredFluids(), toolNetworks);
                if (!hasReadyInputs(input)) continue;
                ThreadPatternSettings settings = ThreadPatternSettings
                    .read(patterns.getStackInSlot(Integer.parseInt(definition.key)));
                long budget = settings.power < 0 ? remaining : Math.min(remaining, settings.power);
                if (definition.execution.mEUt > budget
                    || definition.execution.mEUt > OverclockCalculator.getMaxAllowedRecipeEUt(voltage, 1)) {
                    failures.put(
                        definition.key,
                        new Failure(CheckRecipeResultRegistry.insufficientPower(definition.execution.mEUt)));
                    continue;
                }
                int oc = Math.min(maxOverclocks, settings.oc < 0 ? maxOverclocks : settings.oc);
                ThreadedProxyAllocation.Plan plan = ThreadedProxyAllocation
                    .plan(definition.execution.mEUt, definition.execution.mDuration, voltage, oc);
                while (plan.eut > budget && oc > 0) {
                    oc = Math.max(0, Math.min(oc - 1, plan.overclocks - 1));
                    plan = ThreadedProxyAllocation
                        .plan(definition.execution.mEUt, definition.execution.mDuration, voltage, oc);
                }
                int parallel = Math
                    .min(getTrueParallel(), settings.parallel < 0 ? Integer.MAX_VALUE : settings.parallel);
                if (plan.eut > 0) parallel = (int) Math.min(parallel, budget / plan.eut);
                if (parallel <= 0) continue;
                ThreadedProxyMaterials.divide(Collections.singletonList(input));
                remaining -= startThread(input, budget, parallel, plan);
            }
            updateSlots();
        } finally {
            endRecipeProcessing();
        }
    }

    /** Use the controller network for direct delivery only when the structure has no output hatches. */
    private boolean flushOutputs(RecipeThreadOutputs outputs) {
        if (!GTNHAdjust.threadedProxyAeOutput || GTUtility.validMTEList(mOutputBusses)
            .iterator()
            .hasNext()
            || GTUtility.validMTEList(mOutputHatches)
                .iterator()
                .hasNext())
            return outputs.flush(this);
        ThreadedProxyOutputAccessor ledger = (ThreadedProxyOutputAccessor) (Object) outputs;
        return ProxyThreadOutputs.flushAe(
            this,
            getProxy(),
            ledger.gtnhadjust$getItems(),
            ledger.gtnhadjust$getFluids(),
            ledger.gtnhadjust$getItemAmounts(),
            ledger.gtnhadjust$getFluidAmounts());
    }

    /** Use the controller and the same AE connections that supply the machine's inputs. */
    private List<AENetworkProxy> toolNetworks() {
        var networks = new java.util.LinkedHashSet<AENetworkProxy>();
        AENetworkProxy controller = getProxy();
        if (controller != null) networks.add(controller);
        for (CrossRecipeMEPatternBuffer hatch : GTUtility.validMTEList(patternHatches)) networks.add(hatch.getProxy());
        for (MTEHatchInputBus input : GTUtility.validMTEList(mInputBusses))
            if (input instanceof MTEHatchInputBusME bus) networks.add(bus.getProxy());
        for (MTEHatchInput input : GTUtility.validMTEList(mInputHatches))
            if (input instanceof MTEHatchInputME hatch) networks.add(hatch.getProxy());
        return new ArrayList<>(networks);
    }

    /** Select physical inputs for this pattern; reservoir water never enters allocation or consumption views. */
    private ThreadInputs selectInputs(Definition definition, List<ItemStack> ordinaryItems,
        List<FluidStack> ordinaryFluids, List<AENetworkProxy> toolNetworks) {
        ProxyPatternRecipe match = definition.recipe;
        List<ItemStack> inputs = ordinaryItems.stream()
            .filter(item -> item != null && item.stackSize > 0)
            .filter(
                item -> Arrays.stream(match.consumedItems)
                    .anyMatch(encoded -> sameItem(encoded, item)) || isRequiredTool(match, item))
            .collect(Collectors.toCollection(ArrayList::new));
        for (CrossRecipeMEPatternBuffer hatch : patternHatches) for (ItemStack item : hatch.getSharedItems()) {
            // GTNR 0.1.48 exposes physical shared catalysts as zero-count views, never consumable inventory.
            if (isRequiredTool(match, item)) inputs.add(GTUtility.copyAmount(1, item));
        }
        Collections.addAll(inputs, match.virtualTools);
        for (ItemStack tool : match.externalTools) {
            if (toolNetworks.isEmpty() || ThreadedProxyMaterials.consumes(definition.execution, tool)
                || inputs.stream()
                    .anyMatch(item -> GTUtility.areStacksEqual(item, tool, false)))
                continue;
            for (AENetworkProxy network : toolNetworks) {
                if (!GtnrToolNetwork
                    .available(tool, GTValues.emptyItemStackArray, network, getBaseMetaTileEntity().getTimer()))
                    continue;
                inputs.add(GTUtility.copyAmount(1, tool));
                break;
            }
        }
        FluidStack[] fluids = ordinaryFluids.stream()
            .filter(fluid -> fluid != null && fluid.amount > 0)
            .filter(fluid -> !hasWaterReservoir || fluid.getFluid() != FluidRegistry.WATER)
            .filter(
                fluid -> Arrays.stream(match.consumedFluids)
                    .anyMatch(fluid::isFluidEqual)
                    || Arrays.stream(definition.execution.mFluidInputs)
                        .anyMatch(required -> required != null && required.amount == 0 && required.isFluidEqual(fluid)))
            .toArray(FluidStack[]::new);
        return new ThreadInputs(definition, inputs.toArray(new ItemStack[0]), fluids);
    }

    /** Commits ordinary material consumption only after power, duration and the output ledger have been accepted. */
    private long startThread(ThreadInputs input, long budget, int parallel, ThreadedProxyAllocation.Plan plan) {
        Definition definition = input.definition;
        ProxyPatternRecipe match = definition.recipe;
        // Another admitted recipe may consume an item that this recipe uses as a non-consumed catalyst.
        if (!hasReadyInputs(input)) return 0;
        if (budget < definition.execution.mEUt) return 0;
        ThreadedProxyProcessingLogic logic = (ThreadedProxyProcessingLogic) processingLogic;
        setupProcessingLogic(logic);
        logic.configure(definition, budget, getMaxInputVoltage(), parallel, plan);
        logic.setRecipeMap(definition.map)
            .setMachine(this)
            .setRecipeLocking(this, false)
            .setVoidProtection(false, false)
            .setBatchSize(isBatchModeEnabled() ? getMaxBatchSize() : 1)
            .setInputItems(input.items)
            .setInputFluids(input.fluids);
        try {
            CheckRecipeResult result = logic.process();
            long eut = logic.getCalculatedEut();
            if (!result.wasSuccessful() || eut < 0 || eut > budget || !logic.canCommitInputs()) {
                failures.put(
                    definition.key,
                    new Failure(!result.wasSuccessful() ? result : CheckRecipeResultRegistry.INTERNAL_ERROR));
                if (GTNHAdjust.debug && mTotalRunTime % 200 < 5) GTNHAdjust.LOG.info(
                    "[threaded-proxy] cannot start {}: result={}, EU/t={}, budget={}, voltage={}",
                    definition.label,
                    result.getID(),
                    eut,
                    budget,
                    getMaxInputVoltage());
                return 0;
            }
            RecipeThreadTask task = new RecipeThreadTask(
                0,
                eut,
                Math.max(1, logic.getDuration()),
                logic.getCurrentParallels(),
                logic.pendingOutputs());
            if (!logic.commitInputs()) {
                failures.put(definition.key, new Failure("inputs_changed"));
                return 0;
            }
            input.share.commit();
            tasks.put(definition.key, task);
            runInfo.put(
                definition.key,
                ThreadedProxyDisplay.RunInfo.create(match, task, logic.concurrentParallel(), plan.overclocks));
            failures.remove(definition.key);
            markDirty();
            if (GTNHAdjust.debug) GTNHAdjust.LOG.info(
                "[threaded-proxy] start {}: map={}, operations={}, duration={}, EU/t={}, concurrent={}, OC={}/{}, exact={}",
                definition.label,
                definition.map.unlocalizedName,
                task.getParallels(),
                task.getDuration(),
                eut,
                logic.concurrentParallel(),
                plan.overclocks,
                maxOverclocks,
                match.exact);
            return eut;
        } finally {
            logic.finishRecipe();
        }
    }

    private static boolean sameItem(ItemStack left, ItemStack right) {
        return left.getItem() == right.getItem() && left.getItemDamage() == right.getItemDamage()
            && ItemStack.areItemStackTagsEqual(left, right);
    }

    private static boolean isRequiredTool(ProxyPatternRecipe match, ItemStack item) {
        for (RecipeItemInput input : match.recipe.getCachedCombinedItemInputs())
            if (input.inputAmount == 0 && input.matchesRecipe(GTOreDictUnificator.getAssociation(item), item))
                return true;
        return false;
    }

    /** GT's parallel helper skips NC fluid checks; this proxy requires real fluid for unsupported virtual inputs. */
    private static boolean hasNonConsumedFluids(ThreadInputs input) {
        for (FluidStack required : input.definition.execution.mFluidInputs) if (required != null && required.amount == 0
            && Arrays.stream(input.fluids)
                .noneMatch(fluid -> fluid.amount > 0 && required.isFluidEqual(fluid)))
            return false;
        return true;
    }

    /** Excludes material-starved definitions before distributing power, including their required real tools. */
    private boolean hasReadyInputs(ThreadInputs input) {
        boolean tools = hasNonConsumedFluids(input);
        for (RecipeItemInput required : input.definition.execution.getCachedCombinedItemInputs())
            if (required.inputAmount == 0 && Arrays.stream(input.items)
                .noneMatch(
                    item -> item.stackSize > 0
                        && required.matchesRecipe(GTOreDictUnificator.getAssociation(item), item)))
                tools = false;
        boolean ready = tools
            && input.definition.execution.maxParallelCalculatedByInputs(1, input.fluids, input.items) >= 1;
        if (!ready) failures.put(input.definition.key, new Failure(tools ? "materials" : "catalyst"));
        if (!ready && GTNHAdjust.debug && mTotalRunTime % 200 < 5) GTNHAdjust.LOG.info(
            "[threaded-proxy] waiting for materials/tools {}: items={}, fluids={}",
            input.definition.label,
            Arrays.toString(input.items),
            Arrays.toString(input.fluids));
        return ready;
    }

    /**
     * Rebuilds expensive recipe matches only after an encoded stack, its position or the locator inventory changes.
     */
    private void refreshDefinitions() {
        List<PatternSlot> current = new ArrayList<>();
        for (int slot = 0; slot < patterns.getSlots(); slot++) {
            ItemStack pattern = patterns.getStackInSlot(slot);
            if (pattern != null) current.add(new PatternSlot(Integer.toString(slot), "#" + (slot + 1), pattern));
        }
        boolean changed = definitionsDirty || previousWaterReservoir != hasWaterReservoir
            || current.size() != snapshot.size();
        for (int i = 0; !changed && i < current.size(); i++) {
            PatternSlot left = current.get(i), right = snapshot.get(i);
            changed = !left.key.equals(right.key) || !ItemStack.areItemStacksEqual(left.pattern, right.pattern);
        }
        if (!changed) return;
        previousWaterReservoir = hasWaterReservoir;
        snapshot = current;
        definitionsDirty = false;
        java.util.Set<String> allowed = locatorPool.recipeMaps()
            .stream()
            .map(map -> map.unlocalizedName)
            .collect(Collectors.toSet());
        List<Definition> resolved = new ArrayList<>();
        failures.clear();
        definitionIndex.clear();
        for (PatternSlot slot : current) {
            ICraftingPatternDetails details = CircuitPatternCodec
                .decode(slot.pattern.copy(), getBaseMetaTileEntity().getWorld());
            slot.firstProduct = ThreadedProxyDisplay.RunInfo.firstProduct(details);
            String name = PatternRecipeMap.name(slot.pattern);
            if (name == null || PatternRecipeMap.resolve(name) == null) {
                failures.put(
                    slot.key,
                    new Failure(allowed.isEmpty() ? "no_locator" : name == null ? "missing_map" : "wrong_map"));
                if (GTNHAdjust.debug) GTNHAdjust.LOG.info(
                    "[threaded-proxy] ignored {}: pattern map={} not provided by locator inventory",
                    slot.label,
                    name);
                continue;
            }
            RecipeMap<?> map = PatternRecipeMap.resolve(name);
            if (!com.gtnhadjust.machine.proxy.ProxyFactoryControllerRegistry.isExecutable(map)) {
                failures.put(slot.key, new Failure("blocked_map"));
                continue;
            }
            ProxyPatternRecipe recipe = ProxyPatternRecipe.find(map, details);
            if (recipe != null) {
                ItemStack installed = patterns.getStackInSlot(Integer.parseInt(slot.key));
                if (!installed.getTagCompound()
                    .getCompoundTag(ThreadPatternSettings.KEY)
                    .getBoolean("resolved")) {
                    ThreadPatternSettings initial = ThreadPatternSettings.read(installed);
                    initial.power = recipe.recipe.mEUt;
                    initial.write(installed);
                    installed.getTagCompound()
                        .getCompoundTag(ThreadPatternSettings.KEY)
                        .setBoolean("resolved", true);
                    markDirty();
                }
                Definition definition = new Definition(
                    slot.key,
                    slot.label,
                    map,
                    recipe,
                    hasWaterReservoir,
                    details.getCondensedAEOutputs());
                resolved.add(definition);
                definitionIndex.put(slot.key, definition);
                if (!allowed.contains(name))
                    failures.put(slot.key, new Failure(allowed.isEmpty() ? "no_locator" : "wrong_map"));
                if (tasks.containsKey(slot.key) && !runInfo.containsKey(slot.key)) {
                    runInfo.put(slot.key, ThreadedProxyDisplay.RunInfo.recover(recipe, tasks.get(slot.key)));
                    markDirty();
                }
                if (GTNHAdjust.debug) GTNHAdjust.LOG.info(
                    "[threaded-proxy] matched {}: map={}, exact={}, recipeItems={}, recipeFluids={}, virtualTools={}, retainedItemOutputs={}, retainedFluidOutputs={}",
                    slot.label,
                    name,
                    recipe.exact,
                    Arrays.toString(recipe.recipe.mInputs),
                    Arrays.toString(recipe.recipe.mFluidInputs),
                    Arrays.toString(recipe.virtualTools),
                    definition.itemOutputs,
                    definition.fluidOutputs);
            } else {
                failures.put(
                    slot.key,
                    new Failure(
                        details == null || details.isCraftable() || details.isInputOnly() ? "invalid_pattern"
                            : "no_recipe"));
                if (GTNHAdjust.debug) GTNHAdjust.LOG
                    .info("[threaded-proxy] ignored {}: no complete input/output match in {}", slot.label, name);
            }
        }
        definitions = resolved;
        if (GTNHAdjust.debug) GTNHAdjust.LOG.info(
            "[threaded-proxy] pattern slots={}, valid={}, allowedMaps={}, reservoir={}",
            current.size(),
            resolved.size(),
            allowed,
            hasWaterReservoir);
    }

    private long runningPower() {
        long power = 0;
        for (RecipeThreadTask task : tasks.values()) if (!task.isFinished()) {
            if (task.getEUt() > Long.MAX_VALUE - power) return Long.MAX_VALUE;
            power += task.getEUt();
        }
        return power;
    }

    /**
     * A native power trip discards every task, including completed tasks still waiting to output. Manual pause retains
     * them.
     */
    @Override
    public void stopMachine(ShutDownReason reason) {
        super.stopMachine(reason);
        if (!ShutDownReasonRegistry.POWER_LOSS.getID()
            .equals(reason.getID())
            || !ShutDownReasonRegistry.POWER_LOSS.getKey()
                .equals(reason.getKey()))
            return;
        int discarded = tasks.size();
        tasks.clear();
        runInfo.clear();
        failures.clear();
        ((ThreadedProxyProcessingLogic) processingLogic).discardPreparedRecipe();
        updateDisplay();
        markDirty();
        if (GTNHAdjust.debug) GTNHAdjust.LOG.info(
            "[threaded-proxy] power trip: discarded {} tasks and all their buffered products; consumed inputs are not refunded",
            discarded);
    }

    private void updateDisplay() {
        mEUt = 0;
        lEUt = 0;
        mProgresstime = 0;
        mMaxProgresstime = 0;
        if (!getBaseMetaTileEntity().isAllowedToWork()) return;
        for (RecipeThreadTask task : tasks.values()) if (!task.isFinished()) {
            mProgresstime = task.getProgress();
            mMaxProgresstime = task.getDuration();
            lEUt = -runningPower();
            break;
        }
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        if (getProxy() != null) getProxy().writeToNBT(tag);
        tag.setTag("gtnhadjustLocatorInventory", locatorInventory.serializeNBT());
        tag.setTag("gtnhadjustPatterns", patterns.serializeNBT());
        tag.setInteger("gtnhadjustProxyOperation", operationMode);
        tag.setInteger("threadedProxyMaxOverclocks", maxOverclocks);
        NBTTagList saved = new NBTTagList();
        tasks.forEach((key, task) -> {
            NBTTagCompound entry = task.save();
            entry.setString("patternSlot", key);
            if (runInfo.containsKey(key)) entry.setTag(
                "view",
                runInfo.get(key)
                    .save());
            saved.appendTag(entry);
        });
        tag.setTag(THREADS, saved);
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        if (getProxy() != null) getProxy().readFromNBT(tag);
        if (tag.hasKey("gtnhadjustPatterns", 10)) patterns.deserializeNBT(tag.getCompoundTag("gtnhadjustPatterns"));
        if (tag.hasKey("gtnhadjustLocatorInventory", 10))
            locatorInventory.deserializeNBT(tag.getCompoundTag("gtnhadjustLocatorInventory"));
        operationMode = !tag.hasKey("gtnhadjustProxyOperation") ? 1 : tag.getInteger("gtnhadjustProxyOperation");
        onLocatorInventoryChanged();
        maxOverclocks = Math.max(0, tag.getInteger("threadedProxyMaxOverclocks"));
        tasks.clear();
        runInfo.clear();
        NBTTagList saved = tag.getTagList(THREADS, 10);
        // Old versions saved tasks after power failure. Use GT's persisted shutdown state, not the work toggle alone.
        if (tag.getBoolean("mWasShutdown") && ShutDownReasonRegistry.POWER_LOSS.getID()
            .equals(tag.getString("shutDownReasonID"))
            && ShutDownReasonRegistry.POWER_LOSS.writeToNBT(new NBTTagCompound())
                .equals(tag.getCompoundTag("shutDownReason"))) {
            definitionsDirty = true;
            if (saved.tagCount() > 0) {
                markDirty();
                if (GTNHAdjust.debug) GTNHAdjust.LOG
                    .info("[threaded-proxy] discarded {} saved tasks from a previous power trip", saved.tagCount());
            }
            return;
        }
        for (int i = 0; i < saved.tagCount(); i++) {
            NBTTagCompound entry = saved.getCompoundTagAt(i);
            tasks.put(entry.getString("patternSlot"), RecipeThreadTask.load(entry));
            if (entry.hasKey("view")) runInfo
                .put(entry.getString("patternSlot"), ThreadedProxyDisplay.RunInfo.load(entry.getCompoundTag("view")));
        }
        definitionsDirty = true;
    }

    /** Keep real locator stacks and settings on the dropped controller item. */
    @Override
    public void setItemNBT(NBTTagCompound tag) {
        super.setItemNBT(tag);
        tag.setTag("gtnhadjustLocatorInventory", locatorInventory.serializeNBT());
        tag.setTag("gtnhadjustPatterns", patterns.serializeNBT());
        tag.setInteger("gtnhadjustProxyOperation", operationMode);
        tag.setInteger("threadedProxyMaxOverclocks", maxOverclocks);
    }

    @Override
    protected boolean useMui2() {
        return true;
    }

    @Override
    public boolean showRecipeTextInGUI() {
        return false;
    }

    @Override
    protected ThreadedProxyGui getGui() {
        return new ThreadedProxyGui(this);
    }

    /** PH tools always use this controller's AE network. */
    @Override
    public appeng.me.helpers.AENetworkProxy getProxy() {
        if (gridProxy == null && getBaseMetaTileEntity() instanceof appeng.me.helpers.IGridProxyable) {
            gridProxy = new appeng.me.helpers.AENetworkProxy(this, "proxy", getStackForm(1), true);
            gridProxy.setFlags(appeng.api.networking.GridFlags.REQUIRE_CHANNEL);
            gridProxy.setIdlePowerUsage(0);
            IGregTechTileEntity tile = getBaseMetaTileEntity();
            if (tile.getWorld() != null) gridProxy.setOwner(
                tile.getWorld()
                    .getPlayerEntityByName(tile.getOwnerName()));
        }
        return gridProxy;
    }

    @Override
    public appeng.api.util.DimensionalCoord getLocation() {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        return new appeng.api.util.DimensionalCoord(
            tile.getWorld(),
            tile.getXCoord(),
            tile.getYCoord(),
            tile.getZCoord());
    }

    @Override
    public appeng.api.networking.IGridNode getGridNode(ForgeDirection side) {
        return getProxy() == null ? null : getProxy().getNode();
    }

    @Override
    public appeng.api.util.AECableType getCableConnectionType(ForgeDirection side) {
        return appeng.api.util.AECableType.SMART;
    }

    @Override
    public void securityBreak() {}

    @Override
    public void onFirstTick(IGregTechTileEntity tile) {
        super.onFirstTick(tile);
        if (tile.isServerSide()) onLocatorInventoryChanged();
        if (getProxy() != null) getProxy().onReady();
    }

    ThreadPatternInventory patterns() {
        return patterns;
    }

    /** Invalidate definitions without touching committed tasks. */
    void patternsChanged() {
        definitionsDirty = true;
        if (getBaseMetaTileEntity() != null && getBaseMetaTileEntity().isServerSide()) markDirty();
    }

    ProxyPatternRecipe patternRecipe(int slot) {
        Definition definition = definitionIndex.get(Integer.toString(slot));
        return definition == null ? null : definition.recipe;
    }

    /** Only target and budget edits run the one-shot estimate calculator. */
    void editPattern(int slot, int field, String expression) {
        if (!canEditLocators() || slot < 0 || slot >= patterns.getSlots()) return;
        ItemStack stack = patterns.getStackInSlot(slot);
        if (stack == null) return;
        ThreadPatternSettings settings = ThreadPatternSettings.read(stack);
        double value = ThreadParameterExpression.parse(expression);
        if (!Double.isFinite(value) || value < -1 || (value != -1 && value <= 0 && field != 3)) return;
        if (field == 0) settings.target = value;
        if (field == 1) settings.power = value == -1 ? -1 : (long) Math.min(Long.MAX_VALUE, value);
        if (field == 2) settings.parallel = value == -1 ? -1 : (int) Math.min(Integer.MAX_VALUE, value);
        if (field == 3) settings.oc = value == -1 ? -1 : (int) Math.min(Integer.MAX_VALUE, value);
        ProxyPatternRecipe recipe = patternRecipe(slot);
        if ((field == 0 || field == 1) && recipe != null) settings.tune(recipe, getTrueParallel());
        settings.write(stack);
        // A user-entered budget takes precedence even if the locator has not resolved this recipe yet.
        if (field == 1) stack.getTagCompound()
            .getCompoundTag(ThreadPatternSettings.KEY)
            .setBoolean("resolved", true);
        patternsChanged();
        if (GTNHAdjust.debug) GTNHAdjust.LOG.info(
            "[threaded-proxy] pattern #{} settings: field={}, target={}/s, budget={} EU/t, parallel={}, OC={}",
            slot + 1,
            field,
            settings.target,
            settings.power,
            settings.parallel,
            settings.oc);
    }

    int getMaxOverclocks() {
        return maxOverclocks;
    }

    /** OC changes affect the next launch, leaving committed task parameters intact. */
    void setMaxOverclocks(int value) {
        maxOverclocks = Math.max(0, value);
        markDirty();
    }

    Collection<RecipeThreadTask> threadTasks() {
        return tasks.values();
    }

    int patternCount() {
        return snapshot.size();
    }

    int validPatternCount() {
        return definitions.size();
    }

    boolean hasWaterReservoir() {
        return hasWaterReservoir;
    }

    /**
     * Every installed slot gets a row, including invalid or idle definitions; removed but unfinished tasks follow them.
     */
    List<ThreadedProxyDisplay.Row> threadRows() {
        List<ThreadedProxyDisplay.Row> rows = new ArrayList<>();
        java.util.Set<String> shown = new java.util.HashSet<>();
        boolean paused = !getBaseMetaTileEntity().isAllowedToWork();
        for (int i = 0; i < snapshot.size(); i++) {
            PatternSlot slot = snapshot.get(i);
            shown.add(slot.key);
            RecipeThreadTask task = tasks.get(slot.key);
            if (task != null) {
                rows.add(ThreadedProxyDisplay.Row.running(i + 1, task, runInfo.get(slot.key), paused));
                continue;
            }
            Failure failure = failures.get(slot.key);
            if (definitionIndex.containsKey(slot.key)) {
                if (paused) failure = new Failure("paused");
            }
            rows.add(
                ThreadedProxyDisplay.Row.idle(
                    i + 1,
                    failure == null ? "queued" : failure.reason,
                    failure == null ? null : failure.result,
                    slot.firstProduct));
        }
        tasks.forEach((key, task) -> {
            if (!shown.contains(key)) rows.add(
                new ThreadedProxyDisplay.Row(
                    rows.size() + 1,
                    "removed",
                    null,
                    runInfo.get(key),
                    task.getProgress(),
                    task.getDuration(),
                    task.getEUt()));
        });
        return rows;
    }

    String status() {
        String text = StatCollector.translateToLocalFormatted(
            "gtnhadjust.threaded_proxy.status",
            tasks.size(),
            snapshot.size(),
            definitions.size(),
            maxOverclocks);
        return hasWaterReservoir ? text + "\n" + StatCollector.translateToLocal("gtnhadjust.threaded_proxy.reservoir")
            : text;
    }

    @Override
    public String[] getInfoData() {
        String[] base = super.getInfoData();
        String[] result = Arrays.copyOf(base, base.length + 1);
        result[base.length] = status();
        return result;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        ITexture casing = Textures.BlockIcons.getCasingTextureForId(ThreadedProxyStructure.CASING_TEXTURE);
        if (side != facing) return new ITexture[] { casing };
        return new ITexture[] { casing, TextureFactory.builder()
            .addIcon(active ? Textures.BlockIcons.OVERLAY_DTPF_ON : Textures.BlockIcons.OVERLAY_DTPF_OFF)
            .extFacing()
            .build() };
    }

    /** GTNH-style processing, power and hatch summary for this controller. */
    @Override
    public MultiblockTooltipBuilder createTooltip() {
        MultiblockTooltipBuilder tooltip = new MultiblockTooltipBuilder()
            .addMachineType(StatCollector.translateToLocal("gtnhadjust.threaded_proxy.tooltip.type"));
        tooltip.addInfo(StatCollector.translateToLocal("gtnhadjust.threaded_proxy.tooltip.0"));
        tooltip.addInfo(StatCollector.translateToLocal("gtnhadjust.threaded_proxy.tooltip.1"));
        tooltip.addInfo(StatCollector.translateToLocal("gtnhadjust.threaded_proxy.tooltip.2"));
        tooltip.addInfo(StatCollector.translateToLocal("gtnhadjust.threaded_proxy.tooltip.3"));
        tooltip.addSeparator();
        tooltip.addInfo(StatCollector.translateToLocal("gtnhadjust.threaded_proxy.tooltip.5"));
        tooltip.addInfo(StatCollector.translateToLocal("gtnhadjust.threaded_proxy.tooltip.6"));
        tooltip.addInfo(StatCollector.translateToLocal("gtnhadjust.threaded_proxy.tooltip.7"));
        tooltip.addSeparator();
        tooltip.addInfo(StatCollector.translateToLocal("gtnhadjust.threaded_proxy.tooltip.9"));
        tooltip.addInfo(StatCollector.translateToLocal("gtnhadjust.threaded_proxy.tooltip.10"));
        tooltip.addInfo(StatCollector.translateToLocal("gtnhadjust.threaded_proxy.tooltip.11"));
        tooltip.addSeparator();
        tooltip.addInfo(StatCollector.translateToLocal("gtnhadjust.threaded_proxy.tooltip.13"));
        tooltip.addInfo(StatCollector.translateToLocal("gtnhadjust.threaded_proxy.tooltip.14"));
        tooltip.addInfo(StatCollector.translateToLocal("gtnhadjust.threaded_proxy.tooltip.15"));
        tooltip.addInfo(StatCollector.translateToLocal("gtnhadjust.threaded_proxy.tooltip.16"));
        tooltip.addSeparator();
        tooltip.addInfo(StatCollector.translateToLocal("gtnhadjust.threaded_proxy.tooltip.18"));
        tooltip.addInfo(StatCollector.translateToLocal("gtnhadjust.threaded_proxy.tooltip.19"));
        tooltip.addInfo(StatCollector.translateToLocal("gtnhadjust.threaded_proxy.tooltip.20"));
        tooltip.addInfo(StatCollector.translateToLocal("gtnhadjust.threaded_proxy.tooltip.21"));
        tooltip.addInfo(StatCollector.translateToLocal("gtnhadjust.threaded_proxy.tooltip.22"));
        return tooltip.toolTipFinisher();
    }

    private static final class Failure {

        final String reason;
        final CheckRecipeResult result;

        Failure(String reason) {
            this.reason = reason;
            result = null;
        }

        Failure(CheckRecipeResult result) {
            reason = "";
            this.result = result;
        }
    }

    static final class Definition {

        final String key, label;
        final RecipeMap<?> map;
        final ProxyPatternRecipe recipe;
        final GTRecipe execution;
        final BitSet itemOutputs = new BitSet();
        final BitSet fluidOutputs = new BitSet();

        /** Keep original recipe indices so each retained output uses its own chance and quantity. */
        Definition(String key, String label, RecipeMap<?> map, ProxyPatternRecipe recipe, boolean reservoir,
            IAEStack<?>[] encodedOutputs) {
            this.key = key;
            this.label = label;
            this.map = map;
            this.recipe = recipe;
            execution = recipe.executionRecipe(reservoir);
            if (!GTNHAdjust.threadedProxyVoidUnlistedOutputs) {
                itemOutputs.set(0, execution.mOutputs.length);
                fluidOutputs.set(0, execution.mFluidOutputs.length);
                return;
            }
            for (IAEStack<?> encoded : encodedOutputs) {
                if (encoded == null || encoded.getStackSize() <= 0) continue;
                if (encoded instanceof IAEItemStack item) {
                    ItemStack stack = item.getItemStack();
                    for (int i = 0; i < execution.mOutputs.length; i++)
                        if (GTUtility.areStacksEqual(execution.mOutputs[i], stack)) itemOutputs.set(i);
                } else if (encoded instanceof IAEFluidStack fluid) {
                    FluidStack stack = fluid.getFluidStack();
                    for (int i = 0; i < execution.mFluidOutputs.length; i++)
                        if (stack.isFluidEqual(execution.mFluidOutputs[i])) fluidOutputs.set(i);
                }
            }
        }
    }

    /** References real hatch resources plus private virtual tools; planning consumes only transactional copies. */
    static final class ThreadInputs {

        final Definition definition;
        ItemStack[] items;
        FluidStack[] fluids;
        ThreadedProxyMaterials.Share share;

        ThreadInputs(Definition definition, ItemStack[] items, FluidStack[] fluids) {
            this.definition = definition;
            this.items = items;
            this.fluids = fluids;
        }
    }

    private static final class PatternSlot {

        final String key, label;
        final ItemStack pattern;
        ThreadedProxyDisplay.RunInfo firstProduct;

        PatternSlot(String key, String label, ItemStack pattern) {
            this.key = key;
            this.label = label;
            this.pattern = pattern.copy();
        }
    }
}
