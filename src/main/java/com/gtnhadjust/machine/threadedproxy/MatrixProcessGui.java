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
        int[] graph = { 0 };
        long[] refreshAt = { Long.MIN_VALUE };
        NBTTagCompound[] lastView = { new NBTTagCompound() };
        NBTTagCompound[] lastChoices = { new NBTTagCompound() };
        int[] lastPatternRevision = { -1 };
        StringSyncValue edits = new StringSyncValue(() -> "", command -> {
            if (command.startsWith("view ")) {
                try {
                    graph[0] = Integer.parseInt(command.substring(5));
                } catch (NumberFormatException ignored) {
                    return;
                }
            } else {
                machine.processEditor.edit(command);
            }
            refreshAt[0] = Long.MIN_VALUE;
        }).allowC2S();
        var view = GenericSyncValue.builder(NBTTagCompound.class)
            .adapter(ByteBufAdapters.NBT)
            .getter(() -> {
                long tick = machine.getBaseMetaTileEntity()
                    .getTimer();
                if (tick >= refreshAt[0]) {
                    lastView[0] = machine.processEditor.snapshot(graph[0]);
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
                .size(406, 186));
        panel.child(button("close", 387, 6, 20, handler::closePanel));
        panel.child(button("previous", 9, 25, 18, () -> canvas.cycle(-1)));
        panel.child(button("next", 30, 25, 18, () -> canvas.cycle(1)));
        panel.child(button("new", 53, 25, 43, () -> send.accept("new 0")));
        panel.child(button("add", 98, 25, 66, canvas::togglePicker));
        panel.child(button("pause", 166, 25, 55, () -> canvas.graphEdit("pause")));
        panel.child(button("balance", 223, 25, 50, () -> canvas.graphEdit("balance")));
        panel.child(button("removeGraph", 275, 25, 65, () -> canvas.graphEdit("deleteGraph")));
        panel.child(button("remove", 342, 25, 62, canvas::deleteSelection));
        panel.child(field("parallel", parallel, 8, 55));
        panel.child(field("oc", oc, 68, 50));
        panel.child(field("power", power, 123, 74));
        panel.child(field("water", water, 202, 67));
        panel.child(
            button(
                "apply",
                274,
                203,
                59,
                () -> canvas.apply(
                    parallel.getStringValue(),
                    oc.getStringValue(),
                    power.getStringValue(),
                    water.getStringValue())));
        panel.child(
            IKey.lang("gtnhadjust.matrix.parameters")
                .asWidget()
                .pos(9, 193));
        panel.child(
            IKey.lang("gtnhadjust.matrix.multipliers")
                .asWidget()
                .pos(8, 226));
        panel.child(field("parallel", parallelMultiplier, 146, 52).top(222));
        panel.child(field("oc", ocMultiplier, 203, 52).top(222));
        panel.child(
            button(
                "multiply",
                274,
                222,
                110,
                () -> send.accept(
                    "multipliers 0 " + parallelMultiplier.getStringValue() + " " + ocMultiplier.getStringValue())));
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
            .pos(x, 203)
            .size(width, 16)
            .setMaxLength(19)
            .tooltipBuilder(t -> t.addLine(StatCollector.translateToLocal("gtnhadjust.matrix." + key)));
    }
}
