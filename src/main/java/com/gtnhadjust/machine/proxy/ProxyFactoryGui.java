package com.gtnhadjust.machine.proxy;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.network.NetworkUtils;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.DynamicSyncHandler;
import com.cleanroommc.modularui.value.sync.GenericListSyncHandler;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.ItemSlotSH;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.EmptyWidget;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.DynamicSyncedWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.PagedWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.layout.Grid;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cleanroommc.modularui.widgets.slot.SlotGroup;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.xyp.gtneverreset.common.gui.modularui.multiblock.GTNRMultiBlockBaseGui;

import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.modularui2.GTGuiThemes;
import gregtech.common.modularui2.factory.GTBaseGuiBuilder;

/** Status/inventory pages share the same real container and server-side locator admission rules. */
final class ProxyFactoryGui extends GTNRMultiBlockBaseGui<ProxyFactory> {

    private final StringValue search = new StringValue("");
    private PagedWidget<?> pages;

    ProxyFactoryGui(ProxyFactory controller) {
        super(controller);
    }

    @Override
    protected ModularPanel getBasePanel(PosGuiData data, PanelSyncManager manager, UISettings settings) {
        settings.useTheme(GTGuiThemes.STANDARD.getId());
        return new GTBaseGuiBuilder(multiblock, data, manager, settings).setWidth(getBasePanelWidth())
            .setHeight(getBasePanelHeight())
            .doesAddGregTechLogo(false)
            .doesBindPlayerInventory(false)
            .build();
    }

    @Override
    protected int getTerminalRowHeight() {
        return 128;
    }

    @Override
    protected int getBasePanelHeight() {
        return super.getBasePanelHeight() + 34;
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        syncManager.syncValue("proxyWirelessMode", new BooleanSyncValue(multiblock::isWirelessMode));
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
            "proxyOperationMode",
            new IntSyncValue(multiblock::getOperationMode, multiblock::setClientOperationMode));
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

    @Override
    protected ListWidget<IWidget, ?> createTerminalTextWidget(PanelSyncManager manager, ModularPanel parent) {
        BooleanSyncValue wireless = manager.findSyncHandler("proxyWirelessMode", BooleanSyncValue.class);
        return super.createTerminalTextWidget(manager, parent)
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
                    .fullWidth());
    }

    @Override
    protected ParentWidget<?> createTerminalParentWidget(ModularPanel panel, PanelSyncManager manager) {
        pages = new PagedWidget<>().size(getTerminalWidgetWidth(), getTerminalWidgetHeight())
            .addPage(super.createTerminalParentWidget(panel, manager))
            .addPage(createLocatorPage(manager));
        return new ParentWidget<>().size(getTerminalWidgetWidth(), getTerminalWidgetHeight())
            .child(pages);
    }

    /** Server sends the saved slot count so a reduced-capacity client's grid also includes preserved overflow. */
    private IWidget createLocatorPage(PanelSyncManager manager) {
        manager.registerSlotGroup(
            new SlotGroup("proxyLocators", 9, SlotGroup.STORAGE_SLOT_PRIO, true).setAllowSorting(false));
        DynamicSyncHandler inventory = new DynamicSyncHandler().widgetProvider((childManager, packet) -> {
            if (packet == null) return new EmptyWidget();
            int count = packet.readInt();
            if (childManager.isClient()) multiblock.getLocatorInventory()
                .setSize(count);
            Grid grid = new Grid().scrollable()
                .size(getTerminalWidgetWidth() - 16, getTerminalWidgetHeight() - 28);
            for (int i = 0; i < count; i++) {
                final int slot = i;
                ModularSlot modularSlot = new ModularSlot(multiblock.getLocatorInventory(), i) {

                    @Override
                    public boolean canTakeStack(EntityPlayer player) {
                        return multiblock.canEditLocators() && super.canTakeStack(player);
                    }

                    @Override
                    public boolean canDragIntoSlot() {
                        return multiblock.canEditLocators() && super.canDragIntoSlot();
                    }
                }.slotGroup("proxyLocators");
                ItemSlotSH slotSync = childManager
                    .getOrCreateSyncHandler("proxyLocatorSlot", i, ItemSlotSH.class, () -> new ItemSlotSH(modularSlot));
                grid.child(new ProxyLocatorSlot(multiblock, slot, search).syncHandler(slotSync));
                if ((i + 1) % 9 == 0 && i + 1 < count) grid.nextRow();
            }
            return grid;
        });
        manager.addOpenListener(player -> {
            if (!manager.isClient()) {
                inventory.notifyUpdate(
                    packet -> packet.writeInt(
                        multiblock.getLocatorInventory()
                            .getSlots()));
                // Register the client grid before sending its initial saved contents.
                for (int i = 0; i < multiblock.getLocatorInventory()
                    .getSlots(); i++)
                    manager.findSyncHandler("proxyLocatorSlot", i, ItemSlotSH.class)
                        .forceSyncItem();
            }
        });
        return new ParentWidget<>().size(getTerminalWidgetWidth(), getTerminalWidgetHeight())
            .child(
                new TextFieldWidget().value(search)
                    .setMaxLength(128)
                    .autoUpdateOnChange(true)
                    .hintText(StatCollector.translateToLocal("gtnhadjust.proxy.locator_search"))
                    .pos(4, 4)
                    .size(getTerminalWidgetWidth() - 8, 16))
            .child(
                new DynamicSyncedWidget<>().syncHandler(inventory)
                    .pos(8, 24)
                    .size(getTerminalWidgetWidth() - 16, getTerminalWidgetHeight() - 28));
    }

    @Override
    protected Flow createLeftPanelGapRow(ModularPanel parent, PanelSyncManager manager) {
        InteractionSyncHandler mode = new InteractionSyncHandler().setOnMousePressed(
            mouse -> { if (!mouse.isClient() && mouse.mouseButton == 0) multiblock.cycleOperationMode(); });
        InteractionSyncHandler view = new InteractionSyncHandler().setOnMousePressed(
            mouse -> { if (mouse.mouseButton == 0) pages.setPage(1 - pages.getCurrentPageIndex()); });
        return Flow.row()
            .coverChildrenWidth()
            .fullHeight()
            .child(
                new ButtonWidget<>().syncHandler(mode)
                    .overlay(
                        new DynamicDrawable(
                            () -> multiblock.canEditLocators() ? GTGuiTextures.OVERLAY_BUTTON_ALLOW_INPUT
                                : GTGuiTextures.OVERLAY_BUTTON_MACHINEMODE_DEFAULT))
                    .tooltipBuilder(
                        t -> t.addLine(
                            IKey.dynamic(
                                () -> StatCollector
                                    .translateToLocal("gtnhadjust.proxy.operation." + multiblock.getOperationMode())))))
            .child(
                new ButtonWidget<>().syncHandler(view)
                    .width(56)
                    .overlay(
                        IKey.dynamic(
                            () -> StatCollector.translateToLocal(
                                pages.getCurrentPageIndex() == 0 ? "gtnhadjust.proxy.view.status"
                                    : "gtnhadjust.proxy.view.inventory"))))
            .child(createBatchModeButton(manager));
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
