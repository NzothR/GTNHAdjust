package com.gtnhadjust.machine.threadedproxy;

import java.util.function.Consumer;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.serialization.ByteBufAdapters;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.value.sync.GenericSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

/** ModularUI owns the window and synchronization; the canvas owns local selection and camera state. */
final class MatrixProcessGui {

    private MatrixProcessGui() {}

    static ModularPanel create(ThreadedJointProxy machine, PanelSyncManager manager, IPanelHandler handler) {
        long[] refreshAt = { Long.MIN_VALUE };
        NBTTagCompound[] lastView = { new NBTTagCompound() };
        NBTTagCompound[] lastChoices = { new NBTTagCompound() };
        int[] lastPatternRevision = { -1 };
        StringSyncValue edits = new StringSyncValue(() -> "", command -> {
            machine.processEditor.edit(command);
            refreshAt[0] = Long.MIN_VALUE;
        }).allowC2S();
        var view = GenericSyncValue.builder(NBTTagCompound.class)
            .adapter(ByteBufAdapters.NBT)
            .getter(() -> {
                long tick = machine.getBaseMetaTileEntity()
                    .getTimer();
                if (tick >= refreshAt[0]) {
                    lastView[0] = machine.processEditor.snapshot();
                    refreshAt[0] = tick + 10;
                }
                return lastView[0];
            })
            .build();
        var patterns = GenericSyncValue.builder(NBTTagCompound.class)
            .adapter(ByteBufAdapters.NBT)
            .getter(() -> {
                if (lastPatternRevision[0] != machine.patternRevision) {
                    lastChoices[0] = patternChoices(machine);
                    lastPatternRevision[0] = machine.patternRevision;
                }
                return lastChoices[0];
            })
            .build();
        manager.syncValue("matrixEdits", edits);
        manager.syncValue("matrixView", view);
        manager.syncValue("matrixPatterns", patterns);
        Consumer<String> send = command -> edits.setStringValue(command, false, true);
        StringValue parallel = new StringValue("1"), oc = new StringValue("0"), power = new StringValue("-1"),
            water = new StringValue("0");
        StringValue parallelMultiplier = new StringValue("1"), ocMultiplier = new StringValue("1");
        MatrixProcessCanvas canvas = new MatrixProcessCanvas(view::getValue, patterns::getValue, send, selected -> {
            parallel.setStringValue(Integer.toString(selected.getInteger("parallel")));
            oc.setStringValue(Integer.toString(selected.getInteger("oc")));
            power.setStringValue(Long.toString(selected.getLong("power")));
            water.setStringValue(Integer.toString(selected.getInteger("high")));
        });
        ModularPanel panel = new ModularPanel("matrixProcessGraph").size(414, 240);
        panel.child(
            canvas.pos(4, 4)
                .size(406, 232));
        panel.child(button("close", 387, 6, 20, handler::closePanel));
        panel.child(button("add", 338, 34, 65, canvas::togglePicker));
        panel.child(button("pause", 338, 55, 65, () -> canvas.graphEdit("pause")));
        panel.child(button("balance", 338, 76, 65, () -> canvas.graphEdit("balance")));
        panel.child(button("multipliersButton", 338, 97, 65, canvas::toggleMultipliers));
        panel.child(button("remove", 338, 118, 65, canvas::deleteSelection));
        panel.child(button("removeGraph", 338, 139, 65, () -> {
            canvas.graphEdit("deleteGraph");
            canvas.closePopup();
        }));
        panel.child(button("close", 289, 43, 16, canvas::closePopup).setEnabledIf(w -> canvas.hasPopup()));
        panel.child(
            IKey.lang("gtnhadjust.matrix.nodeParameters")
                .asWidget()
                .pos(33, 192)
                .setEnabledIf(w -> canvas.nodePopup()));
        panel.child(field("parallel", parallel, 33, 49).setEnabledIf(w -> canvas.nodePopup()));
        panel.child(field("oc", oc, 86, 44).setEnabledIf(w -> canvas.nodePopup()));
        panel.child(field("power", power, 134, 94).setEnabledIf(w -> canvas.nodePopup()));
        panel.child(field("water", water, 33, 94).setEnabledIf(w -> canvas.edgePopup()));
        panel.child(
            button(
                "apply",
                235,
                204,
                65,
                () -> canvas.apply(
                    parallel.getStringValue(),
                    oc.getStringValue(),
                    power.getStringValue(),
                    water.getStringValue())).setEnabledIf(w -> canvas.canApply() && !canvas.multiplierPopup()));
        panel.child(
            field("parallel", parallelMultiplier, 33, 54).top(85)
                .setEnabledIf(w -> canvas.multiplierPopup()));
        panel.child(
            field("oc", ocMultiplier, 92, 54).top(85)
                .setEnabledIf(w -> canvas.multiplierPopup()));
        panel.child(
            button(
                "multiply",
                156,
                85,
                108,
                () -> send.accept(
                    "multipliers 0 " + parallelMultiplier.getStringValue() + " " + ocMultiplier.getStringValue()))
                        .setEnabledIf(w -> canvas.multiplierPopup()));
        return panel;
    }

    private static NBTTagCompound patternChoices(ThreadedJointProxy machine) {
        NBTTagCompound result = new NBTTagCompound();
        NBTTagList slots = new NBTTagList();
        for (int i = 0; i < machine.patterns()
            .getSlots(); i++) {
            var stack = machine.patterns()
                .getStackInSlot(i);
            if (stack == null || machine.patternRecipe(i) == null) continue;
            NBTTagCompound slot = new NBTTagCompound();
            slot.setInteger("slot", i);
            var recipe = machine.patternRecipe(i);
            String name = stack.getDisplayName();
            if (recipe != null) {
                for (var product : recipe.recipe.mOutputs) if (product != null) {
                    name = product.getDisplayName();
                    break;
                }
                if (recipe.recipe.mOutputs.length == 0)
                    for (var product : recipe.recipe.mFluidOutputs) if (product != null) {
                        name = product.getLocalizedName();
                        break;
                    }
            }
            slot.setString("name", name.substring(0, Math.min(name.length(), 80)));
            slots.appendTag(slot);
        }
        result.setTag("slots", slots);
        return result;
    }

    private static ButtonWidget<?> button(String key, int x, int y, int width, Runnable action) {
        return new ButtonWidget<>().overlay(IKey.lang("gtnhadjust.matrix." + key))
            .pos(x, y)
            .size(width, 16)
            .onMousePressed(mouse -> {
                if (mouse != 0) return false;
                action.run();
                return true;
            });
    }

    private static TextFieldWidget field(String key, StringValue value, int x, int width) {
        return new TextFieldWidget().value(value)
            .pos(x, 204)
            .size(width, 16)
            .setMaxLength(19)
            .tooltipBuilder(t -> t.addLine(StatCollector.translateToLocal("gtnhadjust.matrix." + key)));
    }
}
