package com.gtnhadjust.mixins.terminal;

import java.io.File;
import java.nio.file.Path;
import java.util.Properties;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.inventory.Container;
import net.minecraft.launchwrapper.Launch;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnhadjust.terminal.InterfaceTerminalPreferences;

import appeng.api.config.Settings;
import appeng.api.config.StringOrder;
import appeng.api.config.TerminalStyle;
import appeng.core.AEConfig;

@Mixin(targets = "com.xyp.gtneverreset.client.gui.GuiQuickEncodingTerminal$EmbeddedInterfaceTerminal", remap = false)
public abstract class QuickTerminalSettingsMixin {

    @Shadow
    private void changeSectionOrder(StringOrder order) {}

    @Unique
    private static Path gtnhadjust$preferencesPath() {
        return new File(new File(Launch.minecraftHome, "config"), "gtnhadjust-terminal-interface.properties").toPath();
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void gtnhadjust$restore(Container container, CallbackInfo ci) {
        Properties p = InterfaceTerminalPreferences.read(gtnhadjust$preferencesPath());
        InterfaceTerminalSettingsAccessor state = (InterfaceTerminalSettingsAccessor) this;
        state.gtnhadjust$setAssemblers(InterfaceTerminalPreferences.bool(p, "assemblersOnly", false));
        state.gtnhadjust$setBroken(InterfaceTerminalPreferences.bool(p, "brokenOnly", false));
        state.gtnhadjust$setSubstitute(InterfaceTerminalPreferences.bool(p, "substituteOnly", false));
        state.gtnhadjust$setHidden(InterfaceTerminalPreferences.bool(p, "showHidden", false));
        AEConfig.instance.showOnlyInterfacesWithFreeSlotsInInterfaceTerminal = InterfaceTerminalPreferences
            .bool(p, "hideFull", AEConfig.instance.showOnlyInterfacesWithFreeSlotsInInterfaceTerminal);
        AEConfig.instance.preserveSearchBar = InterfaceTerminalPreferences
            .bool(p, "preserveSearch", AEConfig.instance.preserveSearchBar);
        try {
            StringOrder order = StringOrder.valueOf(p.getProperty("sectionOrder", "ALPHANUM"));
            AEConfig.instance.settings.putSetting(Settings.INTERFACE_TERMINAL_SECTION_ORDER, order);
            changeSectionOrder(order);
        } catch (IllegalArgumentException ignored) {}
        try {
            String style = p.getProperty("terminalStyle");
            if (style != null)
                AEConfig.instance.settings.putSetting(Settings.TERMINAL_STYLE, TerminalStyle.valueOf(style));
        } catch (IllegalArgumentException ignored) {}
    }

    @Inject(method = "perform", at = @At("RETURN"))
    private void gtnhadjust$save(GuiButton button, CallbackInfo ci) {
        InterfaceTerminalSettingsAccessor state = (InterfaceTerminalSettingsAccessor) this;
        Properties p = new Properties();
        p.setProperty("assemblersOnly", Boolean.toString(state.gtnhadjust$getAssemblers()));
        p.setProperty("brokenOnly", Boolean.toString(state.gtnhadjust$getBroken()));
        p.setProperty("substituteOnly", Boolean.toString(state.gtnhadjust$getSubstitute()));
        p.setProperty("showHidden", Boolean.toString(state.gtnhadjust$getHidden()));
        p.setProperty(
            "hideFull",
            Boolean.toString(AEConfig.instance.showOnlyInterfacesWithFreeSlotsInInterfaceTerminal));
        p.setProperty("preserveSearch", Boolean.toString(AEConfig.instance.preserveSearchBar));
        p.setProperty(
            "sectionOrder",
            AEConfig.instance.settings.getSetting(Settings.INTERFACE_TERMINAL_SECTION_ORDER)
                .name());
        p.setProperty(
            "terminalStyle",
            AEConfig.instance.settings.getSetting(Settings.TERMINAL_STYLE)
                .name());
        InterfaceTerminalPreferences.write(gtnhadjust$preferencesPath(), p);
    }

    @Redirect(
        method = "updateSearchSectionOrder",
        at = @At(value = "FIELD", target = "Lappeng/api/config/StringOrder;ALPHANUM:Lappeng/api/config/StringOrder;"))
    private StringOrder gtnhadjust$selectedOrder() {
        return (StringOrder) AEConfig.instance.settings.getSetting(Settings.INTERFACE_TERMINAL_SECTION_ORDER);
    }
}
