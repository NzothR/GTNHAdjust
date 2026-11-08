package com.gtnhadjust.machine.proxy;

import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.network.NetworkUtils;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.GenericListSyncHandler;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.xyp.gtneverreset.common.gui.modularui.multiblock.GTNRMultiBlockBaseGui;

import gregtech.api.modularui2.GTGuiTextures;

final class ProxyFactoryGui extends GTNRMultiBlockBaseGui<ProxyFactory> {

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
        return super.createRightPanelGapRow(parent, syncManager).child(button);
    }
}
