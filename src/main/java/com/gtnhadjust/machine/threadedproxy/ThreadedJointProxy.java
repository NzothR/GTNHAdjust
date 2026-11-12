package com.gtnhadjust.machine.threadedproxy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.minecraft.entity.player.EntityPlayer;
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
import com.gtnewhorizons.modularui.api.math.Alignment;
import com.gtnewhorizons.modularui.api.math.CrossAxisAlignment;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.widget.IWidgetBuilder;
import com.gtnewhorizons.modularui.api.widget.Widget;
import com.gtnewhorizons.modularui.common.widget.ButtonWidget;
import com.gtnewhorizons.modularui.common.widget.ChangeableWidget;
import com.gtnewhorizons.modularui.common.widget.DynamicPositionedColumn;
import com.gtnewhorizons.modularui.common.widget.FakeSyncWidget;
import com.gtnewhorizons.modularui.common.widget.SlotWidget;
import com.gtnewhorizons.modularui.common.widget.TextWidget;
import com.gtnewhorizons.modularui.common.widget.textfield.NumericWidget;
import com.gtnhadjust.GTNHAdjust;
import com.gtnhadjust.machine.proxy.ProxyFactoryControllerRegistry;
import com.xyp.gtneverreset.common.machine.hatch.CrossRecipeMEPatternBuffer;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadTask;
import com.xyp.gtneverreset.common.me.CircuitPatternCodec;
import com.xyp.gtneverreset.common.me.PatternRecipeMap;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import gregtech.api.enums.Textures;
import gregtech.api.enums.VoidingMode;
import gregtech.api.gui.modularui.GTUITextures;
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
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipe.RecipeItemInput;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.OverclockCalculator;
import gregtech.api.util.shutdown.ShutDownReason;
import gregtech.api.util.shutdown.ShutDownReasonRegistry;
import gregtech.common.tileentities.machines.IDualInputHatch;
import gtPlusPlus.xmod.gregtech.api.metatileentity.implementations.MTEHatchReservoir;

/** One physical pattern slot defines one thread; crafting-buffer deliveries are never machine ingredients. */
public final class ThreadedJointProxy extends MTEExtendedPowerMultiBlockBase<ThreadedJointProxy>
    implements ISurvivalConstructable {

    private static final String THREADS = "gtnhadjustThreadedProxyTasks";
    private IStructureDefinition<ThreadedJointProxy> structure;
    private final List<CrossRecipeMEPatternBuffer> patternHatches = new ArrayList<>();
    private final Map<String, RecipeThreadTask> tasks = new LinkedHashMap<>();
    private final Map<String, ThreadedProxyDisplay.RunInfo> runInfo = new LinkedHashMap<>();
    private final Map<String, Failure> failures = new LinkedHashMap<>();
    private final Map<String, Definition> definitionIndex = new LinkedHashMap<>();
    private List<ThreadedProxyDisplay.Row> clientRows = Collections.emptyList();
    private List<Definition> definitions = Collections.emptyList();
    private List<PatternSlot> snapshot = Collections.emptyList();
    private ItemStack previousController;
    private boolean definitionsDirty = true;
    private boolean refreshRequested;
    private boolean hasWaterReservoir;
    private boolean previousWaterReservoir;
    private String clientStatus = "";
    private int maxOverclocks;

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
        return ProxyFactoryControllerRegistry.maps(getControllerSlot())
            .stream()
            .map(PatternRecipeMap::resolve)
            .collect(Collectors.toList());
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return getAvailableRecipeMaps().stream()
            .findFirst()
            .orElse(null);
    }

    List<CrossRecipeMEPatternBuffer> patternHatches() {
        return patternHatches;
    }

    /** Accepts exactly the defining assembly, ordinary IO and normal/exotic energy; no other pattern buffers. */
    boolean addProxyHatch(IGregTechTileEntity tile, int texture) {
        if (tile == null) return false;
        IMetaTileEntity meta = tile.getMetaTileEntity();
        if (meta instanceof CrossRecipeMEPatternBuffer hatch) {
            if (!addToMachineList(tile, texture)) return false;
            patternHatches.add(hatch);
            return true;
        }
        if (meta instanceof IDualInputHatch) return false;
        if (meta instanceof MTEHatchInput || meta instanceof MTEHatchInputBus
            || meta instanceof MTEHatchOutput
            || meta instanceof MTEHatchOutputBus) {
            boolean added = addToMachineList(tile, texture);
            if (added && meta instanceof MTEHatchReservoir) hasWaterReservoir = true;
            return added;
        }
        return addEnergyInputToMachineList(tile, texture) || addExoticEnergyInputToMachineList(tile, texture);
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
     * Re-read the controller and all patterns after the new hatch lists are known, even while the machine is paused.
     */
    @Override
    protected void onStructureCheckFinished(IGregTechTileEntity tile) {
        super.onStructureCheckFinished(tile);
        if (!refreshRequested) return;
        refreshRequested = false;
        definitionsDirty = true;
        ((ThreadedProxyProcessingLogic) processingLogic).discardPreparedRecipe();
        refreshDefinitions();
        if (GTNHAdjust.debug) GTNHAdjust.LOG.info(
            "[threaded-proxy] manual refresh: formed={}, controller={}, installed={}, valid={}",
            mMachine,
            getControllerSlot(),
            snapshot.size(),
            definitions.size());
    }

    @Override
    public ButtonWidget createStructureUpdateButton(IWidgetBuilder<?> builder) {
        ButtonWidget button = super.createStructureUpdateButton(builder);
        button.addTooltip(StatCollector.translateToLocal("gtnhadjust.threaded_proxy.refresh"));
        return button;
    }

    @Override
    public IStructureDefinition<ThreadedJointProxy> getStructureDefinition() {
        if (structure == null) structure = ThreadedProxyStructure.create();
        return structure;
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        if (checkPiece(
            ThreadedProxyStructure.PIECE,
            ThreadedProxyStructure.X,
            ThreadedProxyStructure.Y,
            ThreadedProxyStructure.Z,
            errors) && patternHatches.isEmpty())
            errors.add(StructureErrors.of("gtnhadjust.threaded_proxy.missing_assembly"));
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

    @Override
    public CheckRecipeResult checkProcessing() {
        return CheckRecipeResultRegistry.NO_RECIPE;
    }

    /** Existing tasks keep their inputs, progress and yields when definitions change. */
    @Override
    protected void runMachine(IGregTechTileEntity tile, long tick) {
        // Definition visibility is independent of the work switch; paused machines still show installed patterns.
        if (tick % 5 == 0 || tile.hasInventoryBeenModified() || tile.hasWorkJustBeenEnabled()) refreshDefinitions();
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
            if (task.isFinished() && task.getOutputs()
                .flush(this)) {
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

    /** Admit more ready threads, apply their OC limits, then give the admitted group a common concurrent parallel. */
    private void startThreads() {
        refreshDefinitions();
        if (definitions.isEmpty()) return;
        startRecipeProcessing();
        try {
            List<ItemStack> ordinaryItems = getStoredInputs();
            List<FluidStack> ordinaryFluids = getStoredFluids();
            List<ThreadInputs> pending = new ArrayList<>();
            long voltage = getMaxInputVoltage();
            for (Definition definition : definitions) {
                if (tasks.containsKey(definition.key)) continue;
                ThreadInputs input = selectInputs(definition, ordinaryItems, ordinaryFluids);
                if (!hasReadyInputs(input)) continue;
                if (definition.execution.mEUt > OverclockCalculator.getMaxAllowedRecipeEUt(voltage, 1)) {
                    failures.put(
                        definition.key,
                        new Failure(CheckRecipeResultRegistry.insufficientVoltage(definition.execution.mEUt)));
                    continue;
                }
                pending.add(input);
            }
            if (pending.isEmpty()) return;
            long remaining = Math.max(0, getMaxInputEu() - runningPower());
            // Threads rejected for lack of power must not reserve a share of another thread's materials.
            ThreadedProxyAllocation admission = allocateInputs(pending, remaining, voltage, 0);
            List<ThreadInputs> admitted = new ArrayList<>();
            for (int i = 0; i < pending.size(); i++) if (admission.plans[i] != null) admitted.add(pending.get(i));
            else failures.put(
                pending.get(i).definition.key,
                new Failure(CheckRecipeResultRegistry.insufficientPower(pending.get(i).definition.execution.mEUt)));
            ThreadedProxyMaterials.divide(admitted);
            pending = new ArrayList<>();
            for (ThreadInputs input : admitted) {
                if (GTNHAdjust.debug && mTotalRunTime % 200 < 5) GTNHAdjust.LOG.info(
                    "[threaded-proxy] material quota {}: items={}, fluids={}, reservoir={}",
                    input.definition.label,
                    Arrays.toString(input.items),
                    Arrays.toString(input.fluids),
                    hasWaterReservoir);
                if (hasReadyInputs(input)) pending.add(input);
                else failures.put(input.definition.key, new Failure("shared_materials"));
            }
            if (pending.isEmpty()) return;
            ThreadedProxyAllocation allocation = allocateInputs(pending, remaining, voltage, maxOverclocks);
            boolean started = false;
            for (int i = 0; i < pending.size(); i++) {
                ThreadInputs input = pending.get(i);
                ThreadedProxyAllocation.Plan plan = allocation.plans[i];
                if (plan == null) {
                    failures.put(
                        input.definition.key,
                        new Failure(CheckRecipeResultRegistry.insufficientPower(input.definition.execution.mEUt)));
                    continue;
                }
                startThread(input, plan.eut * allocation.parallel, allocation.parallel, plan);
                started |= tasks.containsKey(input.definition.key);
            }
            if (GTNHAdjust.debug && (started || mTotalRunTime % 200 < 5)) GTNHAdjust.LOG.info(
                "[threaded-proxy] allocation: ready={}, common parallel={}, free EU/t={}, OC limit={}",
                pending.size(),
                allocation.parallel,
                remaining,
                maxOverclocks);
            updateSlots();
        } finally {
            endRecipeProcessing();
        }
    }

    /** Both admission and final OC/parallel planning see the exact inputs available to that planning stage. */
    private static ThreadedProxyAllocation allocateInputs(List<ThreadInputs> pending, long power, long voltage,
        int ocLimit) {
        long[] eut = new long[pending.size()];
        int[] durations = new int[pending.size()], limits = new int[pending.size()];
        for (int i = 0; i < pending.size(); i++) {
            ThreadInputs input = pending.get(i);
            eut[i] = input.definition.execution.mEUt;
            durations[i] = input.definition.execution.mDuration;
            limits[i] = (int) input.definition.execution
                .maxParallelCalculatedByInputs(Integer.MAX_VALUE, input.fluids, input.items);
        }
        return ThreadedProxyAllocation.allocate(power, voltage, eut, durations, limits, ocLimit);
    }

    /** Select physical inputs for this pattern; reservoir water never enters allocation or consumption views. */
    private ThreadInputs selectInputs(Definition definition, List<ItemStack> ordinaryItems,
        List<FluidStack> ordinaryFluids) {
        ProxyPatternRecipe match = definition.recipe;
        List<ItemStack> inputs = ordinaryItems.stream()
            .filter(item -> item != null && item.stackSize > 0)
            .filter(
                item -> Arrays.stream(match.consumedItems)
                    .anyMatch(encoded -> sameItem(encoded, item)) || isRequiredTool(match, item))
            .collect(Collectors.toCollection(ArrayList::new));
        for (CrossRecipeMEPatternBuffer hatch : patternHatches) for (ItemStack item : hatch.getSharedItems()) {
            if (item.stackSize > 0 && isRequiredTool(match, item)) inputs.add(item);
        }
        Collections.addAll(inputs, match.virtualTools);
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
        logic.configure(definition.execution, budget, getMaxInputVoltage(), parallel, plan);
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
     * Rebuilds expensive recipe matches only after an encoded stack, its position or the inserted controller changes.
     */
    private void refreshDefinitions() {
        List<PatternSlot> current = new ArrayList<>();
        List<CrossRecipeMEPatternBuffer> ordered = new ArrayList<>(patternHatches);
        ordered.sort(
            Comparator.comparingInt(
                (CrossRecipeMEPatternBuffer hatch) -> hatch.getBaseMetaTileEntity()
                    .getWorld().provider.dimensionId)
                .thenComparingInt(
                    hatch -> hatch.getBaseMetaTileEntity()
                        .getXCoord())
                .thenComparingInt(
                    hatch -> hatch.getBaseMetaTileEntity()
                        .getYCoord())
                .thenComparingInt(
                    hatch -> hatch.getBaseMetaTileEntity()
                        .getZCoord()));
        for (CrossRecipeMEPatternBuffer hatch : ordered) {
            IGregTechTileEntity tile = hatch.getBaseMetaTileEntity();
            String location = tile.getWorld().provider.dimensionId + ":"
                + tile.getXCoord()
                + ":"
                + tile.getYCoord()
                + ":"
                + tile.getZCoord();
            for (int slot = 0; slot < hatch.getPatterns()
                .getSizeInventory(); slot++) {
                ItemStack pattern = hatch.getPatterns()
                    .getStackInSlot(slot);
                if (pattern != null) current.add(
                    new PatternSlot(
                        location + ":" + slot,
                        tile.getXCoord() + "," + tile.getYCoord() + "," + tile.getZCoord() + " #" + (slot + 1),
                        pattern));
            }
        }
        ItemStack controller = getControllerSlot();
        boolean changed = definitionsDirty || previousWaterReservoir != hasWaterReservoir
            || !ItemStack.areItemStacksEqual(controller, previousController)
            || current.size() != snapshot.size();
        for (int i = 0; !changed && i < current.size(); i++) {
            PatternSlot left = current.get(i), right = snapshot.get(i);
            changed = !left.key.equals(right.key) || !ItemStack.areItemStacksEqual(left.pattern, right.pattern);
        }
        if (!changed) return;
        previousController = controller == null ? null : controller.copy();
        previousWaterReservoir = hasWaterReservoir;
        snapshot = current;
        definitionsDirty = false;
        List<String> allowed = ProxyFactoryControllerRegistry.maps(controller);
        List<Definition> resolved = new ArrayList<>();
        failures.clear();
        definitionIndex.clear();
        for (PatternSlot slot : current) {
            String name = PatternRecipeMap.name(slot.pattern);
            if (name == null || !allowed.contains(name)) {
                failures.put(
                    slot.key,
                    new Failure(
                        controller == null ? "no_controller"
                            : allowed.isEmpty() ? "unsupported_controller"
                                : name == null ? "missing_map" : "wrong_map"));
                if (GTNHAdjust.debug) GTNHAdjust.LOG.info(
                    "[threaded-proxy] ignored {}: pattern map={} not provided by inserted controller",
                    slot.label,
                    name);
                continue;
            }
            RecipeMap<?> map = PatternRecipeMap.resolve(name);
            ICraftingPatternDetails details = CircuitPatternCodec
                .decode(slot.pattern.copy(), getBaseMetaTileEntity().getWorld());
            ProxyPatternRecipe recipe = ProxyPatternRecipe.find(map, details);
            if (recipe != null) {
                Definition definition = new Definition(slot.key, slot.label, map, recipe, hasWaterReservoir);
                resolved.add(definition);
                definitionIndex.put(slot.key, definition);
                if (tasks.containsKey(slot.key) && !runInfo.containsKey(slot.key)) {
                    runInfo.put(slot.key, ThreadedProxyDisplay.RunInfo.recover(recipe, tasks.get(slot.key)));
                    markDirty();
                }
                if (GTNHAdjust.debug) GTNHAdjust.LOG.info(
                    "[threaded-proxy] matched {}: map={}, exact={}, recipeItems={}, recipeFluids={}, virtualTools={}",
                    slot.label,
                    name,
                    recipe.exact,
                    Arrays.toString(recipe.recipe.mInputs),
                    Arrays.toString(recipe.recipe.mFluidInputs),
                    Arrays.toString(recipe.virtualTools));
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
            "[threaded-proxy] pattern slots={}, valid={}, controller={}, allowedMaps={}, reservoir={}",
            current.size(),
            resolved.size(),
            controller,
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
        if (reason != ShutDownReasonRegistry.POWER_LOSS) return;
        int discarded = tasks.size();
        tasks.clear();
        runInfo.clear();
        failures.clear();
        ((ThreadedProxyProcessingLogic) processingLogic).discardPreparedRecipe();
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

    @Override
    protected boolean useMui2() {
        return false;
    }

    @Override
    public boolean showRecipeTextInGUI() {
        return false;
    }

    @Override
    public ModularWindow createPowerPanel(EntityPlayer player) {
        return ModularWindow.builder(160, 80)
            .setBackground(GTUITextures.BACKGROUND_SINGLEBLOCK_DEFAULT)
            .setDraggable(true)
            .widget(
                TextWidget.localised("gtnhadjust.threaded_proxy.oc_limit")
                    .setPos(8, 8)
                    .setSize(144, 16))
            .widget(
                new NumericWidget().setGetter(() -> maxOverclocks)
                    .setSetter(value -> {
                        maxOverclocks = (int) value;
                        markDirty();
                    })
                    .setBounds(0, Integer.MAX_VALUE)
                    .setScrollValues(1, 4, 16)
                    .setBackground(GTUITextures.BACKGROUND_TEXT_FIELD)
                    .setPos(8, 30)
                    .setSize(144, 18))
            .widget(
                TextWidget.localised("gtnhadjust.threaded_proxy.oc_default")
                    .setPos(8, 53)
                    .setSize(144, 18))
            .build();
    }

    @Override
    protected void drawTexts(DynamicPositionedColumn column, SlotWidget slot) {
        super.drawTexts(column, slot);
        column.widget(
            TextWidget.dynamicString(() -> clientStatus)
                .setSynced(false)
                .setTextAlignment(Alignment.CenterLeft)
                .setDefaultColor(0xFFFFFF));
        column.widget(new FakeSyncWidget.StringSyncer(this::status, value -> clientStatus = value));
        ChangeableWidget rows = new ChangeableWidget(this::createThreadRows);
        column.widget(new FakeSyncWidget.ListSyncer<>(this::threadRows, values -> {
            boolean sizeChanged = clientRows.size() != values.size();
            clientRows = values;
            if (sizeChanged) rows.notifyChangeNoSync();
        }, ThreadedProxyDisplay.Row::write, ThreadedProxyDisplay.Row::read));
        column.widget(rows);
    }

    /** Rebuild widget structure only when row count changes; progress updates use existing dynamic text widgets. */
    private Widget createThreadRows() {
        DynamicPositionedColumn rows = new DynamicPositionedColumn();
        rows.setSynced(false)
            .setSpace(0);
        rows.setAlignment(CrossAxisAlignment.START);
        for (int i = 0; i < clientRows.size(); i++) {
            final int index = i;
            rows.widget(
                TextWidget.dynamicString(
                    () -> clientRows.get(index)
                        .text())
                    .setSynced(false)
                    .setDefaultColor(0xFFFFFF)
                    .setTextAlignment(Alignment.CenterLeft)
                    .setScale(0.75f)
                    .setMaxWidth(182));
        }
        return rows;
    }

    /**
     * Every installed slot gets a row, including invalid or idle definitions; removed but unfinished tasks follow them.
     */
    private List<ThreadedProxyDisplay.Row> threadRows() {
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
                ThreadedProxyDisplay.Row
                    .idle(i + 1, failure == null ? "queued" : failure.reason, failure == null ? null : failure.result));
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

    private String status() {
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

    @Override
    public MultiblockTooltipBuilder createTooltip() {
        return new MultiblockTooltipBuilder().addMachineType("多线程联合代理")
            .addInfo("主机槽放入被代理控制器，按样板记录的白名单配方池工作")
            .addInfo("一个样板一个线程，从输入总线和输入仓取料，产物送入输出舱室")
            .addInfo("允许共享输入，同种原料按待启动线程平均分配")
            .addInfo("安装蓄水仓后，配方用水免消耗；其他流体照常消耗")
            .addInfo("GTNR 支持的不消耗工具虚拟提供，其他工具需放入共享槽或输入舱室")
            .addInfo("线程和单线程并行上限均为 2,147,483,647；受材料和供电限制")
            .addInfo("默认不超频，在电力设置面板调整超频次数上限；到达 1 tick 后停止超频")
            .addInfo("默认启用 GT 批处理；输出受阻时保留线程产物等待输出")
            .addInfo("调度顺序：更多线程 → 超频 → 共同并行 → 批处理")
            .toolTipFinisher();
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

        Definition(String key, String label, RecipeMap<?> map, ProxyPatternRecipe recipe, boolean reservoir) {
            this.key = key;
            this.label = label;
            this.map = map;
            this.recipe = recipe;
            execution = recipe.executionRecipe(reservoir);
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

        PatternSlot(String key, String label, ItemStack pattern) {
            this.key = key;
            this.label = label;
            this.pattern = pattern.copy();
        }
    }
}
