package com.gtnhadjust.machine.integrated;

import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment.MainAxis;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;

import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

/** Structure testing interface using the Forge of Gods button style. */
public final class IntegratedLineGui extends MTEMultiBlockBaseGui<IntegratedLine> {

    public IntegratedLineGui(IntegratedLine machine) {
        super(machine);
    }

    @Override
    protected void registerSyncValues(PanelSyncManager sync) {
        super.registerSyncValues(sync);
        sync.syncValue("integratedModules", new IntSyncValue(multiblock::connectedModules));
        sync.syncValue(
            "integratedRefresh",
            new BooleanSyncValue(() -> false, ignored -> multiblock.refreshModules()).allowC2S());
        sync.syncValue(
            "integratedVoltage",
            new BooleanSyncValue(multiblock::isVoltageEnabled, multiblock::setVoltageEnabled).allowC2S());
    }

    @Override
    protected Flow createPanelGap(ModularPanel panel, PanelSyncManager sync) {
        BooleanSyncValue refresh = sync.findSyncHandler("integratedRefresh", BooleanSyncValue.class);
        BooleanSyncValue voltage = sync.findSyncHandler("integratedVoltage", BooleanSyncValue.class);
        return Flow.row()
            .fullWidth()
            .paddingLeft(5)
            .childPadding(2)
            .height(20)
            .mainAxisAlignment(MainAxis.START)
            .child(
                new ButtonWidget<>().size(16)
                    .overlay(GTGuiTextures.TT_OVERLAY_CYCLIC_BLUE)
                    .background(GTGuiTextures.TT_BUTTON_CELESTIAL_32x32)
                    .disableHoverBackground()
                    .onMousePressed(d -> {
                        refresh.setBoolValue(!refresh.getBoolValue());
                        return true;
                    })
                    .tooltip(t -> t.addLine(StatCollector.translateToLocal("gtnhadjust.integrated.refresh"))))
            .child(
                new ToggleButton().size(16)
                    .value(voltage)
                    .overlay(true, GTGuiTextures.TT_OVERLAY_BUTTON_POWER_SWITCH_ON)
                    .overlay(false, GTGuiTextures.TT_OVERLAY_BUTTON_POWER_SWITCH_OFF)
                    .background(GTGuiTextures.TT_BUTTON_CELESTIAL_32x32)
                    .addTooltipLine(StatCollector.translateToLocal("gtnhadjust.integrated.voltage")));
    }

    @Override
    protected int getTextBoxToInventoryGap() {
        return 20;
    }

    @Override
    protected ListWidget<IWidget, ?> createTerminalTextWidget(PanelSyncManager sync, ModularPanel panel) {
        IntSyncValue count = sync.findSyncHandler("integratedModules", IntSyncValue.class);
        return super.createTerminalTextWidget(sync, panel).child(
            new TextWidget<>(
                IKey.dynamic(
                    () -> StatCollector
                        .translateToLocalFormatted("gtnhadjust.integrated.modules", count.getIntValue()))));
    }
}
