package com.gtnhadjust.machine.threadedproxy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
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
import gregtech.api.util.GTRecipe.RecipeItemInput;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.shutdown.ShutDownReasonRegistry;
import gregtech.common.tileentities.machines.IDualInputHatch;

/** One physical pattern slot defines one thread; crafting-buffer deliveries are never machine ingredients. */
public final class ThreadedJointProxy extends MTEExtendedPowerMultiBlockBase<ThreadedJointProxy>
    implements ISurvivalConstructable {

    private static final String THREADS = "gtnhadjustThreadedProxyTasks";
    private IStructureDefinition<ThreadedJointProxy> structure;
    private final List<CrossRecipeMEPatternBuffer> patternHatches = new ArrayList<>();
    private final Map<String, RecipeThreadTask> tasks = new LinkedHashMap<>();
    private List<Definition> definitions = Collections.emptyList();
    private List<PatternSlot> snapshot = Collections.emptyList();
    private ItemStack previousController;
    private boolean definitionsDirty = true;
    private String conflict = "";
    private String clientStatus = "";
    private int maxOverclocks;
    private int scheduleCursor;

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
            || meta instanceof MTEHatchOutputBus) return addToMachineList(tile, texture);
        return addEnergyInputToMachineList(tile, texture) || addExoticEnergyInputToMachineList(tile, texture);
    }

    @Override
    public void clearHatches() {
        super.clearHatches();
        patternHatches.clear();
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

    /** Existing tasks keep their inputs, progress and yields when definitions change or conflict. */
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
        if (!tasks.isEmpty() || completed) markDirty();
        if (completed || tick % 5 == 0 || tile.hasInventoryBeenModified() || tile.hasWorkJustBeenEnabled())
            startThreads();
        updateDisplay();
    }

    /** Fairly shares remaining EU/t among ready definitions, rotating priority when power cannot cover all. */
    private void startThreads() {
        refreshDefinitions();
        if (!conflict.isEmpty() || definitions.isEmpty()) return;
        startRecipeProcessing();
        try {
            List<ItemStack> ordinaryItems = getStoredInputs();
            List<FluidStack> ordinaryFluids = getStoredFluids();
            List<ThreadInputs> pending = definitions.stream()
                .filter(def -> !tasks.containsKey(def.key))
                .map(def -> selectInputs(def, ordinaryItems, ordinaryFluids))
                .filter(this::hasReadyInputs)
                .collect(Collectors.toCollection(ArrayList::new));
            if (pending.isEmpty()) return;
            Collections.rotate(pending, -(scheduleCursor % pending.size()));
            scheduleCursor = (scheduleCursor + 1) & Integer.MAX_VALUE;
            long remaining = Math.max(0, getMaxInputEu() - runningPower());
            for (int i = 0; i < pending.size(); i++) {
                ThreadInputs input = pending.get(i);
                long budget = remaining / (pending.size() - i);
                if (budget < input.definition.recipe.recipe.mEUt) budget = remaining;
                long used = startThread(input, budget);
                remaining -= used;
            }
            updateSlots();
        } finally {
            endRecipeProcessing();
        }
    }

    /** Restricts consumed inputs to this pattern's exact identities; shared slots can only provide zero-count tools. */
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
            .filter(
                fluid -> Arrays.stream(match.consumedFluids)
                    .anyMatch(fluid::isFluidEqual)
                    || Arrays.stream(match.recipe.mFluidInputs)
                        .anyMatch(required -> required != null && required.amount == 0 && required.isFluidEqual(fluid)))
            .toArray(FluidStack[]::new);
        return new ThreadInputs(definition, inputs.toArray(new ItemStack[0]), fluids);
    }

    /** Commits ordinary material consumption only after power, duration and the output ledger have been accepted. */
    private long startThread(ThreadInputs input, long budget) {
        Definition definition = input.definition;
        ProxyPatternRecipe match = definition.recipe;
        if (budget < match.recipe.mEUt) return 0;
        ThreadedProxyProcessingLogic logic = (ThreadedProxyProcessingLogic) processingLogic;
        setupProcessingLogic(logic);
        logic.configure(match.recipe, budget, getMaxInputVoltage(), maxOverclocks);
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
            if (!logic.commitInputs()) return 0;
            tasks.put(definition.key, task);
            markDirty();
            if (GTNHAdjust.debug) GTNHAdjust.LOG.info(
                "[threaded-proxy] start {}: map={}, parallel={}, duration={}, EU/t={}, OC limit={}, exact={}",
                definition.label,
                definition.map.unlocalizedName,
                task.getParallels(),
                task.getDuration(),
                eut,
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
        for (FluidStack required : input.definition.recipe.recipe.mFluidInputs)
            if (required != null && required.amount == 0
                && Arrays.stream(input.fluids)
                    .noneMatch(fluid -> fluid.amount > 0 && required.isFluidEqual(fluid)))
                return false;
        return true;
    }

    /** Excludes material-starved definitions before distributing power, including their required real tools. */
    private boolean hasReadyInputs(ThreadInputs input) {
        boolean ready = hasNonConsumedFluids(input)
            && input.definition.recipe.recipe.maxParallelCalculatedByInputs(1, input.fluids, input.items) >= 1;
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
        for (CrossRecipeMEPatternBuffer hatch : patternHatches) {
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
        boolean changed = definitionsDirty || !ItemStack.areItemStacksEqual(controller, previousController)
            || current.size() != snapshot.size();
        for (int i = 0; !changed && i < current.size(); i++) {
            PatternSlot left = current.get(i), right = snapshot.get(i);
            changed = !left.key.equals(right.key) || !ItemStack.areItemStacksEqual(left.pattern, right.pattern);
        }
        if (!changed) return;
        previousController = controller == null ? null : controller.copy();
        snapshot = current;
        definitionsDirty = false;
        List<String> allowed = ProxyFactoryControllerRegistry.maps(controller);
        List<Definition> resolved = new ArrayList<>();
        for (PatternSlot slot : current) {
            String name = PatternRecipeMap.name(slot.pattern);
            if (name == null || !allowed.contains(name)) {
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
                resolved.add(new Definition(slot.key, slot.label, map, recipe));
                if (GTNHAdjust.debug) GTNHAdjust.LOG.info(
                    "[threaded-proxy] matched {}: map={}, exact={}, recipeItems={}, recipeFluids={}, virtualTools={}",
                    slot.label,
                    name,
                    recipe.exact,
                    Arrays.toString(recipe.recipe.mInputs),
                    Arrays.toString(recipe.recipe.mFluidInputs),
                    Arrays.toString(recipe.virtualTools));
            } else if (GTNHAdjust.debug) GTNHAdjust.LOG
                .info("[threaded-proxy] ignored {}: no complete input/output match in {}", slot.label, name);
        }
        definitions = resolved;
        conflict = PatternInputConflicts.find(resolved);
        if (GTNHAdjust.debug) GTNHAdjust.LOG.info(
            "[threaded-proxy] pattern slots={}, valid={}, controller={}, allowedMaps={}, conflict={}",
            current.size(),
            resolved.size(),
            controller,
            allowed,
            conflict);
    }

    private long runningPower() {
        long power = 0;
        for (RecipeThreadTask task : tasks.values()) if (!task.isFinished()) {
            if (task.getEUt() > Long.MAX_VALUE - power) return Long.MAX_VALUE;
            power += task.getEUt();
        }
        return power;
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
            saved.appendTag(entry);
        });
        tag.setTag(THREADS, saved);
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        maxOverclocks = Math.max(0, tag.getInteger("threadedProxyMaxOverclocks"));
        tasks.clear();
        NBTTagList saved = tag.getTagList(THREADS, 10);
        for (int i = 0; i < saved.tagCount(); i++) {
            NBTTagCompound entry = saved.getCompoundTagAt(i);
            tasks.put(entry.getString("patternSlot"), RecipeThreadTask.load(entry));
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
                .setDefaultColor(0xFFFFFF));
        column.widget(new FakeSyncWidget.StringSyncer(this::status, value -> clientStatus = value));
    }

    private String status() {
        String text = StatCollector.translateToLocalFormatted(
            "gtnhadjust.threaded_proxy.status",
            tasks.size(),
            snapshot.size(),
            definitions.size(),
            maxOverclocks);
        return conflict.isEmpty() ? text
            : text + "\n" + StatCollector.translateToLocal("gtnhadjust.threaded_proxy.conflict") + " " + conflict;
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
            .addInfo("消耗输入不得重叠；冲突时只禁止启动新线程")
            .addInfo("GTNR 支持的不消耗工具虚拟提供，其他工具需放入共享槽或输入舱室")
            .addInfo("线程和单线程并行上限均为 2,147,483,647；受材料和供电限制")
            .addInfo("默认不超频，在电力设置面板调整超频次数上限；到达 1 tick 后停止超频")
            .addInfo("默认启用 GT 批处理；输出受阻时保留线程产物等待输出")
            .toolTipFinisher();
    }

    static final class Definition {

        final String key, label;
        final RecipeMap<?> map;
        final ProxyPatternRecipe recipe;

        Definition(String key, String label, RecipeMap<?> map, ProxyPatternRecipe recipe) {
            this.key = key;
            this.label = label;
            this.map = map;
            this.recipe = recipe;
        }
    }

    /** References real hatch resources plus private virtual tools; planning consumes only transactional copies. */
    private static final class ThreadInputs {

        final Definition definition;
        final ItemStack[] items;
        final FluidStack[] fluids;

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
