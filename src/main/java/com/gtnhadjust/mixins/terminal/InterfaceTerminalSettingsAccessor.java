package com.gtnhadjust.mixins.terminal;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import appeng.client.gui.implementations.GuiInterfaceTerminal;

@Mixin(value = GuiInterfaceTerminal.class, remap = false)
public interface InterfaceTerminalSettingsAccessor {

    @Accessor("onlyMolecularAssemblers")
    boolean gtnhadjust$getAssemblers();

    @Accessor("onlyMolecularAssemblers")
    void gtnhadjust$setAssemblers(boolean value);

    @Accessor("onlyBrokenRecipes")
    boolean gtnhadjust$getBroken();

    @Accessor("onlyBrokenRecipes")
    void gtnhadjust$setBroken(boolean value);

    @Accessor("onlySubstitute")
    boolean gtnhadjust$getSubstitute();

    @Accessor("onlySubstitute")
    void gtnhadjust$setSubstitute(boolean value);

    @Accessor("showHidden")
    boolean gtnhadjust$getHidden();

    @Accessor("showHidden")
    void gtnhadjust$setHidden(boolean value);
}
