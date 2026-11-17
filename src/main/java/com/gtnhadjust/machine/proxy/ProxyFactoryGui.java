package com.gtnhadjust.machine.proxy;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.network.NetworkUtils;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.DynamicSyncHandler;
import com.cleanroommc.modularui.value.sync.GenericListSyncHandler;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.EmptyWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.DynamicSyncedWidget;
import com.cleanroommc.modularui.widgets.FluidDisplayWidget;
import com.cleanroommc.modularui.widgets.ItemDisplayWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;

/** Status/inventory pages share the same real container and server-side locator admission rules. */
final class ProxyFactoryGui extends ProxyLocatorGui<ProxyFactory> {

    private List<ProxyFactoryStatus.Output> clientOutputs = new ArrayList<>();
    private List<ProxyFactoryStatus.Issue> clientIssues = new ArrayList<>();
    private List<ProxyFactoryStatus.Output> outputSnapshot = new ArrayList<>();
    private List<ProxyFactoryStatus.Output> outputShape = new ArrayList<>();
    private long outputSnapshotTick = Long.MIN_VALUE;

    /** Match the displayed tenth-second precision without rebuilding output widgets for progress-only updates. */
    private List<ProxyFactoryStatus.Output> outputSnapshot() {
        long tick = baseMetaTileEntity.getTimer() / 2;
        if (tick != outputSnapshotTick) {
            outputSnapshotTick = tick;
            outputSnapshot = ProxyFactoryStatus.outputs(multiblock);
        }
        return outputSnapshot;
    }

    ProxyFactoryGui(ProxyFactory controller) {
        super(controller);
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        syncManager.syncValue("proxyWirelessMode", new BooleanSyncValue(multiblock::isWirelessMode));
        syncManager.syncValue("proxyAeConnected", new BooleanSyncValue(multiblock::isAeNetworkConnected));
        syncManager.syncValue("itemOutput", new GenericListSyncHandler<>(() -> {
            List<ItemStack> values = new ArrayList<>();
            for (ProxyThreadTask task : multiblock.getProxyThreadTasks()) task.getOutputs()
                .appendDisplayItems(values);
            return values;
        },
            values -> multiblock.mOutputItems = values.toArray(new ItemStack[0]),
            NetworkUtils::readItemStack,
            NetworkUtils::writeItemStack,
            ItemStack::areItemStacksEqual,
            ItemStack::copy));
        syncManager.syncValue("fluidOutput", new GenericListSyncHandler<>(() -> {
            List<FluidStack> values = new ArrayList<>();
            for (ProxyThreadTask task : multiblock.getProxyThreadTasks()) task.getOutputs()
                .appendDisplayFluids(values);
            return values;
        },
            values -> multiblock.mOutputFluids = values.toArray(new FluidStack[0]),
            NetworkUtils::readFluidStack,
            NetworkUtils::writeFluidStack,
            (left, right) -> left.isFluidEqual(right) && left.amount == right.amount,
            FluidStack::copy));
        syncManager.syncValue(
            "proxyRecipeMaps",
            new GenericListSyncHandler<>(
                multiblock::getUnlockedRecipeNames,
                multiblock::setClientRecipeNames,
                NetworkUtils::readStringSafe,
                NetworkUtils::writeStringSafe,
                String::equals,
                value -> value));
    }

    /** Native aggregate recipe text mixes thread timing and the last idle lookup; display only actual failures here. */
    @Override
    protected ListWidget<IWidget, ?> createTerminalTextWidget(PanelSyncManager manager, ModularPanel parent) {
        BooleanSyncValue wireless = manager.findSyncHandler("proxyWirelessMode", BooleanSyncValue.class);
        BooleanSyncValue aeConnected = manager.findSyncHandler("proxyAeConnected", BooleanSyncValue.class);
        GenericListSyncHandler<ProxyFactoryStatus.Output> outputs = new GenericListSyncHandler<>(
            this::outputSnapshot,
            value -> clientOutputs = value,
            ProxyFactoryStatus.Output::read,
            ProxyFactoryStatus.Output::write,
            ProxyFactoryStatus.Output::same,
            value -> value);
        GenericListSyncHandler<ProxyFactoryStatus.Issue> issues = new GenericListSyncHandler<>(
            multiblock::getPatternIssues,
            value -> clientIssues = value,
            ProxyFactoryStatus.Issue::read,
            ProxyFactoryStatus.Issue::write,
            ProxyFactoryStatus.Issue::same,
            value -> value);
        manager.syncValue("proxyThreadOutputs", outputs);
        manager.syncValue("proxyPatternIssues", issues);
        return new ListWidget<>().fullWidth()
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .child(createShutdownDurationWidget(manager))
            .child(
                IKey.dynamic(
                    () -> baseMetaTileEntity.getLastShutDownReason()
                        .getDisplayString())
                    .color(0xffff5555)
                    .asWidget()
                    .fullWidth()
                    .setEnabledIf(
                        widget -> shouldShutdownReasonBeDisplayed(
                            baseMetaTileEntity.getLastShutDownReason()
                                .getDisplayString())))
            .child(createStructureErrorWidget(manager))
            .child(
                IKey.dynamic(
                    () -> multiblock.getCheckRecipeResult()
                        .getDisplayString())
                    .color(0xffff5555)
                    .asWidget()
                    .fullWidth()
                    .setEnabledIf(widget -> showGlobalFailure()))
            .child(createPatternIssues(manager, issues))
            .child(createThreadOutputs(manager, outputs))
            .child(
                IKey.dynamic(
                    () -> StatCollector.translateToLocal("gtnhadjust.proxy.operation." + multiblock.getOperationMode()))
                    .asWidget()
                    .fullWidth())
            .child(
                IKey.dynamic(
                    () -> StatCollector
                        .translateToLocal("gtnhadjust.proxy.power." + (wireless.getValue() ? "wireless" : "wired")))
                    .asWidget()
                    .fullWidth())
            .child(
                IKey.dynamic(() -> ProxyFactory.aeNetworkStatusText(aeConnected.getValue()))
                    .asWidget()
                    .fullWidth());
    }

    private boolean showGlobalFailure() {
        CheckRecipeResult result = multiblock.getCheckRecipeResult();
        if (multiblock.canEditLocators() || result.wasSuccessful()
            || result.equals(CheckRecipeResultRegistry.NO_RECIPE)
            || result.equals(CheckRecipeResultRegistry.NONE)) return false;
        for (ProxyFactoryStatus.Issue issue : clientIssues) if (issue.result.equals(result)) return false;
        return true;
    }

    private IWidget createPatternIssues(PanelSyncManager manager,
        GenericListSyncHandler<ProxyFactoryStatus.Issue> issues) {
        DynamicSyncHandler display = new DynamicSyncHandler().widgetProvider((sync, packet) -> {
            if (packet == null) return new EmptyWidget();
            Flow column = Flow.column()
                .coverChildrenHeight(0)
                .fullWidth()
                .crossAxisAlignment(Alignment.CrossAxis.START);
            int count = packet.readInt();
            for (int i = 0; i < count; i++) {
                final int index = i;
                column.child(
                    IKey.dynamic(
                        () -> index < clientIssues.size() ? clientIssues.get(index)
                            .text() : "")
                        .color(0xffff5555)
                        .asWidget()
                        .fullWidth()
                        .marginBottom(2));
            }
            return column;
        });
        if (!manager.isClient()) issues.setChangeListener(
            () -> display.notifyUpdate(
                packet -> packet.writeInt(
                    issues.getValue()
                        .size())));
        return new DynamicSyncedWidget<>().syncHandler(display)
            .fullWidth()
            .coverChildrenHeight(0);
    }

    private IWidget createThreadOutputs(PanelSyncManager manager,
        GenericListSyncHandler<ProxyFactoryStatus.Output> outputs) {
        DynamicSyncHandler display = new DynamicSyncHandler().widgetProvider((sync, packet) -> {
            if (packet == null) return new EmptyWidget();
            Flow column = Flow.column()
                .coverChildrenHeight(0)
                .fullWidth()
                .crossAxisAlignment(Alignment.CrossAxis.START);
            int count = packet.readInt();
            for (int i = 0; i < count; i++) {
                final int index = i;
                ProxyFactoryStatus.Output row = ProxyFactoryStatus.Output.read(packet);
                IWidget icon = row.item != null ? new ItemDisplayWidget().item(row.item)
                    .displayAmount(false)
                    .disableThemeBackground(true)
                    .disableHoverThemeBackground(true)
                    .size(14)
                    .marginRight(2)
                    : new FluidDisplayWidget().value(row.fluid)
                        .displayAmount(false)
                        .disableThemeBackground(true)
                        .disableHoverThemeBackground(true)
                        .size(14)
                        .marginRight(2);
                column.child(
                    Flow.row()
                        .fullWidth()
                        .height(15)
                        .child(icon)
                        .child(
                            Flow.column()
                                .coverChildrenHeight(0)
                                .width(getTerminalWidgetWidth() - 24)
                                .crossAxisAlignment(Alignment.CrossAxis.START)
                                .child(
                                    new TextWidget<>(
                                        IKey.dynamic(
                                            () -> index < clientOutputs.size() ? clientOutputs.get(index)
                                                .name() : "")).color(0xff55ffff)
                                                    .height(8)
                                                    .scale(0.75f))
                                .child(
                                    new TextWidget<>(
                                        IKey.dynamic(
                                            () -> index < clientOutputs.size() ? clientOutputs.get(index)
                                                .amountAndProgress() : "")).height(6)
                                                    .scale(0.6f))));
            }
            return column;
        });
        if (!manager.isClient()) {
            outputs.setChangeListener(() -> {
                List<ProxyFactoryStatus.Output> current = outputs.getValue();
                boolean changed = current.size() != outputShape.size();
                for (int i = 0; !changed && i < current.size(); i++) changed = !current.get(i)
                    .sameShape(outputShape.get(i));
                if (!changed) return;
                outputShape = new ArrayList<>(current);
                display.notifyUpdate(packet -> {
                    packet.writeInt(current.size());
                    for (ProxyFactoryStatus.Output row : current) ProxyFactoryStatus.Output.write(packet, row);
                });
            });
        }
        return new DynamicSyncedWidget<>().syncHandler(display)
            .fullWidth()
            .coverChildrenHeight(0);
    }

    @Override
    protected Flow createRightPanelGapRow(ModularPanel parent, PanelSyncManager manager) {
        IPanelHandler overclock = manager
            .syncedPanel("proxyOverclock", true, (childManager, handler) -> createOverclockPanel(childManager, parent));
        return Flow.row()
            .coverChildrenWidth()
            .fullHeight()
            .rightRel(0)
            .verticalCenter()
            .child(
                new ButtonWidget<>().overlay(IKey.str("OC"))
                    .tooltipBuilder(t -> t.addLine(IKey.lang("gtnhadjust.proxy.wireless.oc")))
                    .onMousePressed(mouse -> {
                        if (mouse != 0) return false;
                        if (overclock.isPanelOpen()) overclock.closePanel();
                        else {
                            manager.findPanelHandlerNullable("powerPanel")
                                .closePanel();
                            overclock.openPanel();
                        }
                        return true;
                    }))
            .child(createPowerPanelButton(manager, parent).marginLeft(0));
    }

    /** Overclock settings keep their existing wireless-only semantics and open beside the upper terminal. */
    private ModularPanel createOverclockPanel(PanelSyncManager manager, ModularPanel parent) {
        IntSyncValue value = new IntSyncValue(
            multiblock::getWirelessMaxOverclocks,
            multiblock::setWirelessMaxOverclocks).allowC2S();
        return new ModularPanel("proxyOverclock").relative(parent)
            .leftRel(1)
            .topRel(0)
            .size(140, 64)
            .child(
                IKey.lang("gtnhadjust.proxy.wireless.oc")
                    .asWidget()
                    .pos(6, 8))
            .child(
                new TextFieldWidget().value(value)
                    .numbersInt(0, Integer.MAX_VALUE)
                    .setTextAlignment(Alignment.Center)
                    .pos(6, 30)
                    .size(128, 18)
                    .tooltipBuilder(t -> t.addLine(IKey.lang("gtnhadjust.proxy.wireless.oc.tip"))));
    }

    @Override
    protected ButtonWidget<?> createPowerPanelButton(PanelSyncManager manager, ModularPanel parent) {
        IPanelHandler settings = manager
            .syncedPanel("powerPanel", true, (childManager, handler) -> createPowerSettingsPanel(childManager, parent));
        return new ButtonWidget<>().overlay(GTGuiTextures.TT_OVERLAY_BUTTON_POWER_PANEL)
            .tooltipBuilder(t -> t.addLine(IKey.lang("GT5U.gui.button.power_panel")))
            .onMousePressed(mouse -> {
                if (settings.isPanelOpen()) settings.closePanel();
                else {
                    manager.findPanelHandlerNullable("proxyOverclock")
                        .closePanel();
                    settings.openPanel();
                }
                return true;
            });
    }

    private ModularPanel createPowerSettingsPanel(PanelSyncManager manager, ModularPanel parent) {
        IntSyncValue maximum = new IntSyncValue(() -> Math.max(1, multiblock.getMaxParallelRecipes()));
        BooleanSyncValue alwaysMax = new BooleanSyncValue(
            multiblock::isAlwaysMaxParallel,
            multiblock::setAlwaysMaxParallel).allowC2S();
        IntSyncValue selected = new IntSyncValue(multiblock::getTrueParallel, multiblock::setPowerPanelMaxParallel)
            .allowC2S();
        BooleanSyncValue powerfail = new BooleanSyncValue(
            multiblock::makesPowerfailEvents,
            multiblock::setPowerfailEventCreationStatus).allowC2S();
        manager.syncValue("maxParallel", maximum);
        manager.syncValue("alwaysMaxParallel", alwaysMax);
        return new ModularPanel("powerPanel").relative(parent)
            .leftRel(1)
            .topRel(0)
            .size(140, 112)
            .child(
                Flow.column()
                    .full()
                    .padding(3)
                    .child(makeTitleTextWidget())
                    .child(
                        IKey.lang("GTPP.CC.parallel")
                            .asWidget()
                            .marginBottom(4))
                    .child(
                        Flow.row()
                            .fullWidth()
                            .height(22)
                            .child(makeParallelConfiguratorTextFieldWidget(maximum, alwaysMax, selected))
                            .child(
                                new ButtonWidget<>()
                                    .overlay(
                                        new DynamicDrawable(
                                            () -> alwaysMax.getValue() ? GTGuiTextures.OVERLAY_BUTTON_CHECKMARK
                                                : GTGuiTextures.OVERLAY_BUTTON_CROSS))
                                    .tooltipBuilder(t -> t.addLine(IKey.lang("GT5U.gui.button.max_parallel")))
                                    .onMousePressed(mouse -> {
                                        alwaysMax.setValue(!alwaysMax.getValue());
                                        selected.setValue(maximum.getValue());
                                        return true;
                                    })))
                    .child(
                        Flow.row()
                            .fullWidth()
                            .height(18)
                            .child(
                                IKey.lang("GT5U.gui.text.powerfail_events")
                                    .asWidget()
                                    .marginRight(2))
                            .child(
                                new ToggleButton().value(powerfail)
                                    .overlay(true, GTGuiTextures.OVERLAY_BUTTON_CHECKMARK)
                                    .overlay(false, GTGuiTextures.OVERLAY_BUTTON_CROSS))));
    }
}
