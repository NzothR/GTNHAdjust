package com.gtnhadjust.machine.threadedproxy;

import java.util.function.Consumer;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.Rectangle;
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
        StringValue parallel = new StringValue("1"), oc = new StringValue("0"), water = new StringValue("0");
        StringValue parallelMultiplier = new StringValue("1"), ocMultiplier = new StringValue("1");
        MatrixProcessCanvas canvas = new MatrixProcessCanvas(view::getValue, patterns::getValue, send, selected -> {
            parallel.setStringValue(Integer.toString(selected.getInteger("parallel")));
            oc.setStringValue(Integer.toString(selected.getInteger("oc")));
            water.setStringValue(Integer.toString(selected.getInteger("high")));
        });
        ModularPanel panel = new ModularPanel("matrixProcessGraph").size(414, 240);
        panel.child(
            canvas.pos(4, 4)
                .size(406, 232));
        panel.child(button("close", 387, 6, 20, handler::closePanel));
        panel.child(rail("+", "add", 34, canvas::togglePicker));
        panel.child(rail("0", "resetView", 58, canvas::resetView));
        panel.child(rail("B", "balance", 82, () -> canvas.graphEdit("balance")));
        panel.child(rail("×n", "multipliersButton", 106, () -> {
            NBTTagCompound data = view.getValue();
            if (data != null) {
                parallelMultiplier.setStringValue(Integer.toString(Math.max(1, data.getInteger("parallel"))));
                ocMultiplier.setStringValue(Integer.toString(Math.max(1, data.getInteger("oc"))));
            }
            canvas.toggleMultipliers();
        }));
        panel.child(rail("Ⅱ", "pause", 130, () -> canvas.graphEdit("pause")));
        panel.child(rail("−", "remove", 154, canvas::deleteSelection));
        panel.child(rail("×", "removeGraph", 178, () -> {
            canvas.graphEdit("deleteGraph");
            canvas.closePopup();
        }));

        panel.child(button("close", 291, 41, 16, canvas::closePopup).setEnabledIf(w -> canvas.hasPopup()));
        panel.child(label("parallelLabel", 42, 78).setEnabledIf(w -> canvas.nodePopup()));
        panel.child(field(parallel, 91, 74, 58).setEnabledIf(w -> canvas.nodePopup()));
        panel.child(label("ocLabel", 42, 104).setEnabledIf(w -> canvas.nodePopup()));
        panel.child(field(oc, 91, 100, 58).setEnabledIf(w -> canvas.nodePopup()));
        panel.child(
            styledButton(
                IKey.dynamic(
                    () -> StatCollector
                        .translateToLocal("gtnhadjust.matrix." + (canvas.selectedTarget() ? "targetOn" : "targetOff"))),
                42,
                129,
                107,
                canvas::toggleTarget).setEnabledIf(w -> canvas.nodePopup()));
        panel.child(field(water, 36, 184, 94).setEnabledIf(w -> canvas.edgePopup()));
        panel.child(button("apply", 235, 198, 65, () -> {
            canvas.apply(parallel.getStringValue(), oc.getStringValue(), water.getStringValue());
            canvas.closePopup();
        }).setEnabledIf(w -> canvas.canApply() && !canvas.multiplierPopup()));
        panel.child(label("parallelLabel", 42, 96).setEnabledIf(w -> canvas.multiplierPopup()));
        panel.child(field(parallelMultiplier, 110, 92, 70).setEnabledIf(w -> canvas.multiplierPopup()));
        panel.child(label("ocLabel", 42, 122).setEnabledIf(w -> canvas.multiplierPopup()));
        panel.child(field(ocMultiplier, 110, 118, 70).setEnabledIf(w -> canvas.multiplierPopup()));
        panel.child(button("multiply", 198, 198, 102, () -> {
            send.accept("multipliers 0 " + parallelMultiplier.getStringValue() + " " + ocMultiplier.getStringValue());
            canvas.closePopup();
        }).setEnabledIf(w -> canvas.multiplierPopup()));
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
        return styledButton(IKey.lang("gtnhadjust.matrix." + key), x, y, width, action);
    }

    private static ButtonWidget<?> rail(String glyph, String key, int y, Runnable action) {
        return styledButton(IKey.str(glyph), 377, y, 24, action).height(20)
            .tooltipBuilder(t -> t.addLine(IKey.lang("gtnhadjust.matrix." + key)));
    }

    private static ButtonWidget<?> styledButton(IKey label, int x, int y, int width, Runnable action) {
        return new ButtonWidget<>().overlay(label.color(0xffedf5ff))
            .background(
                new Rectangle().color(0xff3d526c),
                new Rectangle().color(0xff7793ad)
                    .hollow())
            .hoverBackground(
                new Rectangle().color(0xff526e88),
                new Rectangle().color(0xff89c6d8)
                    .hollow())
            .pos(x, y)
            .size(width, 17)
            .onMousePressed(mouse -> {
                if (mouse != 0) return false;
                action.run();
                return true;
            });
    }

    private static com.cleanroommc.modularui.widgets.TextWidget<?> label(String key, int x, int y) {
        return IKey.lang("gtnhadjust.matrix." + key)
            .color(0xffedf5ff)
            .asWidget()
            .pos(x, y);
    }

    private static TextFieldWidget field(StringValue value, int x, int y, int width) {
        return new TextFieldWidget().value(value)
            .background(
                new Rectangle().color(0xff0c1624),
                new Rectangle().color(0xff90aec6)
                    .hollow())
            .pos(x, y)
            .size(width, 18)
            .setMaxLength(10);
    }
}
