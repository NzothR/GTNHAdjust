package com.gtnhadjust.machine.proxy;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.value.sync.DynamicSyncHandler;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.ItemSlotSH;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.EmptyWidget;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.DynamicSyncedWidget;
import com.cleanroommc.modularui.widgets.PagedWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cleanroommc.modularui.widgets.slot.SlotGroup;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.xyp.gtneverreset.common.gui.modularui.multiblock.GTNRMultiBlockBaseGui;

import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.modularui2.GTGuiThemes;
import gregtech.common.modularui2.factory.GTBaseGuiBuilder;

/** Shared two-page terminal, searchable locator slots and operating mode controls. */
public class ProxyLocatorGui<T extends MTEExtendedPowerMultiBlockBase<?> & ProxyLocatorHost>
    extends GTNRMultiBlockBaseGui<T> {

    private final StringValue search = new StringValue("");
    private PagedWidget<?> pages;

    protected int pageCount() {
        return 2;
    }

    protected IWidget createPatternPage(ModularPanel panel, PanelSyncManager manager) {
        return new EmptyWidget();
    }

    protected ProxyLocatorGui(T controller) {
        super(controller);
    }

    @Override
    protected void registerSyncValues(PanelSyncManager manager) {
        super.registerSyncValues(manager);
        manager.syncValue(
            "proxyOperationMode",
            new IntSyncValue(multiblock::getOperationMode, multiblock::setClientOperationMode));
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
    protected ParentWidget<?> createTerminalParentWidget(ModularPanel panel, PanelSyncManager manager) {
        pages = new PagedWidget<>().size(getTerminalWidgetWidth(), getTerminalWidgetHeight())
            .addPage(super.createTerminalParentWidget(panel, manager))
            .addPage(createLocatorPage(manager));
        if (pageCount() == 3) pages.addPage(createPatternPage(panel, manager));
        pages.onPageChange(index -> pages.scheduleResize());
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
            java.util.List<ProxyLocatorSlot> slots = new java.util.ArrayList<>();
            ProxySearchGrid grid = new ProxySearchGrid(
                i -> slots.get(i)
                    .matchesSearch());
            grid.size(getTerminalWidgetWidth() - 16, getTerminalWidgetHeight() - 28);
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
                ProxyLocatorSlot widget = new ProxyLocatorSlot(multiblock, slot, search);
                widget.syncHandler(slotSync);
                slots.add(widget);
                grid.addSlot(widget);
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
            mouse -> { if (mouse.mouseButton == 0) pages.setPage((pages.getCurrentPageIndex() + 1) % pageCount()); });
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
                                    : pages.getCurrentPageIndex() == 1 ? "gtnhadjust.proxy.view.inventory"
                                        : "gtnhadjust.threaded_proxy.view.patterns"))))
            .child(createBatchModeButton(manager));
    }

}
