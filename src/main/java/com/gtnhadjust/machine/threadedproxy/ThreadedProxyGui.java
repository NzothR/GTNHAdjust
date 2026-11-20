package com.gtnhadjust.machine.threadedproxy;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
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
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.gtnhadjust.machine.proxy.ProxyLocatorGui;

/** The shared proxy terminal presents physical-pattern threads without changing their execution or IO. */
final class ThreadedProxyGui extends ProxyLocatorGui<ThreadedJointProxy> {

    private final com.cleanroommc.modularui.value.StringValue[] patternSearch = {
        new com.cleanroommc.modularui.value.StringValue(""), new com.cleanroommc.modularui.value.StringValue(""),
        new com.cleanroommc.modularui.value.StringValue("") };

    @Override
    protected int pageCount() {
        return 3;
    }

    /** Register the fixed physical slots before opening the panel; search only highlights matches. */
    @Override
    protected IWidget createPatternPage(ModularPanel parent, PanelSyncManager manager) {
        manager.registerSlotGroup(
            new com.cleanroommc.modularui.widgets.slot.SlotGroup(
                "threadPatterns",
                9,
                com.cleanroommc.modularui.widgets.slot.SlotGroup.STORAGE_SLOT_PRIO,
                true).setAllowSorting(false));
        int count = multiblock.patterns()
            .getSlots();
        com.gtnhadjust.machine.proxy.ProxySearchGrid grid = new com.gtnhadjust.machine.proxy.ProxySearchGrid();
        grid.size(getTerminalWidgetWidth() - 16, getTerminalWidgetHeight() - 46);
        for (int i = 0; i < count; i++) {
            final int index = i;
            var slot = new com.cleanroommc.modularui.widgets.slot.ModularSlot(multiblock.patterns(), i) {

                @Override
                public boolean canTakeStack(net.minecraft.entity.player.EntityPlayer player) {
                    return multiblock.canEditLocators() && super.canTakeStack(player);
                }

                @Override
                public boolean canDragIntoSlot() {
                    return multiblock.canEditLocators() && super.canDragIntoSlot();
                }
            }.slotGroup("threadPatterns");
            var sync = manager.getOrCreateSyncHandler(
                "threadPatternSlot",
                i,
                com.cleanroommc.modularui.value.sync.ItemSlotSH.class,
                () -> new com.cleanroommc.modularui.value.sync.ItemSlotSH(slot));
            ThreadPatternSlot widget = new ThreadPatternSlot(multiblock, i, patternSearch);
            widget.syncHandler(sync);
            grid.addSlot(widget);
        }
        var page = new com.cleanroommc.modularui.widget.ParentWidget<>()
            .size(getTerminalWidgetWidth(), getTerminalWidgetHeight());
        String[] hints = { "input", "output", "map" };
        for (int i = 0; i < 3; i++) page.child(
            new TextFieldWidget().value(patternSearch[i])
                .setMaxLength(128)
                .autoUpdateOnChange(true)
                .hintText(StatCollector.translateToLocal("gtnhadjust.threaded_proxy.search." + hints[i]))
                .pos(i == 2 ? getTerminalWidgetWidth() / 2 + 2 : 4, i == 0 ? 2 : 20)
                .size(getTerminalWidgetWidth() / 2 - 6, 16));
        if (com.gtnhadjust.GTNHAdjust.threadedProxyExportIngredients) {
            var export = new com.cleanroommc.modularui.value.sync.InteractionSyncHandler().setOnMousePressed(
                mouse -> { if (mouse.mouseButton == 0) ThreadedProxyIngredientExport.append(multiblock); });
            manager.syncValue("exportPatternIngredients", export);
            page.child(
                new ButtonWidget<>().overlay(IKey.lang("gtnhadjust.threaded_proxy.export"))
                    .tooltipBuilder(t -> t.addLine(IKey.lang("gtnhadjust.threaded_proxy.export.tooltip")))
                    .onMousePressed(mouse -> {
                        if (mouse != 0) return false;
                        export.onMousePressed(mouse);
                        return true;
                    })
                    .pos(getTerminalWidgetWidth() / 2 + 2, 2)
                    .size(getTerminalWidgetWidth() / 2 - 6, 16));
        }
        return page.child(grid.pos(8, 40));
    }

    private List<ThreadedProxyDisplay.Row> clientRows = new ArrayList<>();
    private List<ThreadedProxyDisplay.Row> rowShape = new ArrayList<>();

    ThreadedProxyGui(ThreadedJointProxy machine) {
        super(machine);
    }

    @Override
    protected void registerSyncValues(PanelSyncManager manager) {
        super.registerSyncValues(manager);
        manager.syncValue("threadedWireless", new BooleanSyncValue(multiblock::isWirelessMode));
        manager.syncValue(
            "threadedTaskCount",
            new IntSyncValue(
                () -> multiblock.threadTasks()
                    .size()));
        manager.syncValue("threadedPatternCount", new IntSyncValue(multiblock::patternCount));
        manager.syncValue("threadedValidCount", new IntSyncValue(multiblock::validPatternCount));
        manager.syncValue("threadedOcMultiplier", new IntSyncValue(multiblock::overclockMultiplier));
        manager.syncValue("threadedReservoir", new BooleanSyncValue(multiblock::hasWaterReservoir));
        manager.syncValue("itemOutput", new GenericListSyncHandler<>(() -> {
            List<ItemStack> values = new ArrayList<>();
            multiblock.threadTasks()
                .forEach(
                    task -> task.getOutputs()
                        .appendDisplayItems(values));
            return values;
        },
            values -> multiblock.mOutputItems = values.toArray(new ItemStack[0]),
            NetworkUtils::readItemStack,
            NetworkUtils::writeItemStack,
            ItemStack::areItemStacksEqual,
            ItemStack::copy));
        manager.syncValue("fluidOutput", new GenericListSyncHandler<>(() -> {
            List<FluidStack> values = new ArrayList<>();
            multiblock.threadTasks()
                .forEach(
                    task -> task.getOutputs()
                        .appendDisplayFluids(values));
            return values;
        },
            values -> multiblock.mOutputFluids = values.toArray(new FluidStack[0]),
            NetworkUtils::readFluidStack,
            NetworkUtils::writeFluidStack,
            (left, right) -> left.isFluidEqual(right) && left.amount == right.amount,
            FluidStack::copy));
    }

    /** Update row text in place; rebuilding is needed only when the number of installed or retained tasks changes. */
    @Override
    protected ListWidget<IWidget, ?> createTerminalTextWidget(PanelSyncManager manager, ModularPanel parent) {
        BooleanSyncValue wireless = manager.findSyncHandler("threadedWireless", BooleanSyncValue.class);
        IntSyncValue taskCount = manager.findSyncHandler("threadedTaskCount", IntSyncValue.class);
        IntSyncValue patternCount = manager.findSyncHandler("threadedPatternCount", IntSyncValue.class);
        IntSyncValue validCount = manager.findSyncHandler("threadedValidCount", IntSyncValue.class);
        IntSyncValue ocMultiplier = manager.findSyncHandler("threadedOcMultiplier", IntSyncValue.class);
        BooleanSyncValue reservoir = manager.findSyncHandler("threadedReservoir", BooleanSyncValue.class);
        GenericListSyncHandler<ThreadedProxyDisplay.Row> rows = new GenericListSyncHandler<>(
            multiblock::threadRows,
            values -> clientRows = values,
            ThreadedProxyDisplay.Row::read,
            ThreadedProxyDisplay.Row::write,
            ThreadedProxyDisplay.Row::equals,
            value -> value);
        manager.syncValue("threadedRows", rows);
        DynamicSyncHandler display = new DynamicSyncHandler().widgetProvider((sync, packet) -> {
            if (packet == null) return new EmptyWidget();
            Flow column = Flow.column()
                .fullWidth()
                .coverChildrenHeight(0);
            int count = packet.readInt();
            for (int i = 0; i < count; i++) {
                final int index = i;
                ThreadedProxyDisplay.Row row = ThreadedProxyDisplay.Row.read(packet);
                var details = IKey
                    .dynamic(
                        () -> index < clientRows.size()
                            ? (clientRows.get(index).duration == 0 && !"queued".equals(clientRows.get(index).reason)
                                && !"paused".equals(clientRows.get(index).reason) ? "§c" : "") + clientRows.get(index)
                                    .details()
                            : "")
                    .asWidget()
                    .fullWidth()
                    .textAlign(Alignment.CenterLeft)
                    .scale(0.6f);
                if (row.duration == 0) {
                    column.child(details.marginBottom(2));
                    continue;
                }
                IWidget icon = row.info == null ? new com.cleanroommc.modularui.widget.ParentWidget<>().size(14)
                    : row.info.item != null ? new ItemDisplayWidget().item(row.info.item)
                        .displayAmount(false)
                        .disableThemeBackground(true)
                        .disableHoverThemeBackground(true)
                        .size(14)
                        : new FluidDisplayWidget().value(row.info.fluid)
                            .displayAmount(false)
                            .disableThemeBackground(true)
                            .disableHoverThemeBackground(true)
                            .size(14);
                column.child(
                    Flow.row()
                        .fullWidth()
                        .height(16)
                        .child(icon)
                        .child(
                            Flow.column()
                                .width(getTerminalWidgetWidth() - 24)
                                .coverChildrenHeight(0)
                                .child(
                                    IKey.dynamic(
                                        () -> index < clientRows.size() ? clientRows.get(index)
                                            .name() : "")
                                        .color(0xff55ffff)
                                        .asWidget()
                                        .fullWidth()
                                        .height(8)
                                        .textAlign(Alignment.CenterLeft)
                                        .scale(0.75f))
                                .child(details.height(7))));
            }
            return column;
        });
        if (!manager.isClient()) rows.setChangeListener(() -> {
            List<ThreadedProxyDisplay.Row> current = rows.getValue();
            boolean changed = current.size() != rowShape.size();
            for (int i = 0; !changed && i < current.size(); i++) changed = !current.get(i)
                .sameShape(rowShape.get(i));
            if (!changed) return;
            rowShape = new ArrayList<>(current);
            display.notifyUpdate(packet -> {
                packet.writeInt(current.size());
                for (ThreadedProxyDisplay.Row row : current) ThreadedProxyDisplay.Row.write(packet, row);
            });
        });
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
                new DynamicSyncedWidget<>().syncHandler(display)
                    .fullWidth()
                    .coverChildrenHeight(0))
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
                IKey.dynamic(
                    () -> StatCollector.translateToLocalFormatted(
                        "gtnhadjust.threaded_proxy.status",
                        taskCount.getValue(),
                        patternCount.getValue(),
                        validCount.getValue(),
                        ocMultiplier.getValue()))
                    .asWidget()
                    .fullWidth())
            .child(
                IKey.lang("gtnhadjust.threaded_proxy.reservoir")
                    .asWidget()
                    .fullWidth()
                    .setEnabledIf(widget -> reservoir.getValue()));
    }

    @Override
    protected IWidget createStructureUpdateButton(PanelSyncManager manager) {
        return ((com.cleanroommc.modularui.widgets.ToggleButton) super.createStructureUpdateButton(manager))
            .tooltipBuilder(tooltip -> tooltip.addLine(IKey.lang("gtnhadjust.threaded_proxy.refresh")));
    }

    @Override
    protected Flow createRightPanelGapRow(ModularPanel parent, PanelSyncManager manager) {
        IPanelHandler editor = manager.syncedPanel(
            "matrixProcessGraph",
            true,
            (childManager, handler) -> MatrixProcessGui.create(multiblock, childManager, handler));
        return Flow.row()
            .coverChildrenWidth()
            .fullHeight()
            .rightRel(0)
            .verticalCenter()
            .child(
                new ButtonWidget<>().overlay(IKey.lang("gtnhadjust.matrix.open"))
                    .tooltipBuilder(t -> t.addLine(IKey.lang("gtnhadjust.matrix.title")))
                    .onMousePressed(mouse -> {
                        if (mouse != 0) return false;
                        if (editor.isPanelOpen()) editor.closePanel();
                        else editor.openPanel();
                        return true;
                    }));
    }
}
