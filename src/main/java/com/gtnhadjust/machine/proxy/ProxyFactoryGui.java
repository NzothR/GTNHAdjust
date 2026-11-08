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
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.value.sync.GenericListSyncHandler;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.xyp.gtneverreset.common.gui.modularui.multiblock.GTNRMultiBlockBaseGui;

import gregtech.api.modularui2.GTGuiTextures;

final class ProxyFactoryGui extends GTNRMultiBlockBaseGui<ProxyFactory> {

    private static final int PAGE_SIZE = 10;
    private final StringValue search = new StringValue("");
    private String previousSearch = "";
    private int page;

    ProxyFactoryGui(ProxyFactory controller) {
        super(controller);
        withMachineModeIcons(GTGuiTextures.OVERLAY_BUTTON_MACHINEMODE_DEFAULT);
    }

    @Override
    protected IWidget createModeSwitchButton(PanelSyncManager syncManager) {
        InteractionSyncHandler action = new InteractionSyncHandler().setOnMousePressed(mouse -> {
            if (!mouse.isClient() && (mouse.mouseButton == 0 || mouse.mouseButton == 1))
                multiblock.changeRecipeMode(mouse.mouseButton == 1);
        });
        return new ButtonWidget<>().name("proxy_recipe_mode")
            .syncHandler(action)
            .overlay(GTGuiTextures.OVERLAY_BUTTON_MACHINEMODE_DEFAULT)
            .tooltipBuilder(this::createModeSwitchTooltip);
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
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
    protected Flow createRightPanelGapRow(ModularPanel parent, PanelSyncManager syncManager) {
        InteractionSyncHandler action = new InteractionSyncHandler().setOnMousePressed(
            mouse -> { if (!mouse.isClient() && mouse.mouseButton == 0) multiblock.cycleOperationMode(); });
        ButtonWidget<?> button = new ButtonWidget<>().name("proxy_operation_mode")
            .syncHandler(action)
            .overlay(new DynamicDrawable(() -> {
                if (multiblock.getOperationMode() == 1) return GTGuiTextures.OVERLAY_BUTTON_ALLOW_INPUT;
                if (multiblock.getOperationMode() == 2) return GTGuiTextures.OVERLAY_BUTTON_ALLOW_OUTPUT;
                return GTGuiTextures.OVERLAY_BUTTON_MACHINEMODE_DEFAULT;
            }))
            .tooltipBuilder(
                t -> t.addLine(
                    IKey.dynamic(
                        () -> StatCollector
                            .translateToLocal("gtnhadjust.proxy.operation." + multiblock.getOperationMode()))));
        IPanelHandler list = syncManager
            .syncedPanel("proxyRecipeList", true, (manager, handler) -> createRecipeListPanel());
        ButtonWidget<?> listButton = new ButtonWidget<>().name("proxy_recipe_list")
            .overlay(GTGuiTextures.OVERLAY_BUTTON_WHITELIST)
            .tooltipBuilder(t -> t.addLine(IKey.lang("gtnhadjust.proxy.recipe_list")))
            .onMousePressed(mouse -> {
                if (mouse != 0) return false;
                page = 0;
                list.openPanel();
                return true;
            });
        return super.createRightPanelGapRow(parent, syncManager).child(button)
            .child(listButton);
    }

    private ModularPanel createRecipeListPanel() {
        ModularPanel panel = ModularPanel.defaultPanel("proxy_recipe_list", 220, 170);
        panel.child(
            IKey.lang("gtnhadjust.proxy.recipe_list")
                .color(0xff404040)
                .shadow(false)
                .asWidget()
                .pos(8, 8));
        panel.child(
            new TextFieldWidget().value(search)
                .setMaxLength(128)
                .autoUpdateOnChange(true)
                .hintText(StatCollector.translateToLocal("gtnhadjust.proxy.search"))
                .pos(8, 22)
                .size(202, 16));
        for (int row = 0; row < PAGE_SIZE; row++) {
            final int index = row;
            panel.child(
                IKey.dynamic(() -> recipeName(index))
                    .color(0xff404040)
                    .shadow(false)
                    .asWidget()
                    .pos(8, 41 + row * 10));
        }
        panel.child(
            IKey.dynamic(() -> (page + 1) + " / " + pageCount())
                .color(0xff404040)
                .shadow(false)
                .asWidget()
                .pos(95, 149));
        panel.child(
            new ButtonWidget<>().pos(8, 145)
                .size(55, 18)
                .overlay(IKey.lang("gtnhadjust.proxy.previous"))
                .onMousePressed(mouse -> {
                    if (mouse == 0) page = Math.max(0, page - 1);
                    return true;
                }));
        panel.child(
            new ButtonWidget<>().pos(155, 145)
                .size(55, 18)
                .overlay(IKey.lang("gtnhadjust.proxy.next"))
                .onMousePressed(mouse -> {
                    if (mouse == 0) page = Math.min(pageCount() - 1, page + 1);
                    return true;
                }));
        panel.child(
            new ButtonWidget<>().pos(195, 5)
                .size(18, 14)
                .overlay(IKey.str("X"))
                .onMousePressed(mouse -> {
                    if (mouse == 0) panel.closeIfOpen();
                    return true;
                }));
        return panel;
    }

    private List<String> filteredRecipeNames() {
        String query = search.getStringValue();
        if (!query.equals(previousSearch)) {
            previousSearch = query;
            page = 0;
        }
        List<String> result = new ArrayList<>();
        for (String name : multiblock.getUnlockedRecipeNames()) {
            if (ProxyRecipeSearch.matches(name, StatCollector.translateToLocal(name), query)) result.add(name);
        }
        return result;
    }

    private int pageCount() {
        return Math.max(1, (filteredRecipeNames().size() + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private String recipeName(int row) {
        List<String> names = filteredRecipeNames();
        page = Math.min(page, Math.max(0, (names.size() - 1) / PAGE_SIZE));
        int index = page * PAGE_SIZE + row;
        return index < names.size() ? StatCollector.translateToLocal(names.get(index)) : "";
    }
}
