package com.futureresourceprotocol;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import net.minecraft.launchwrapper.Launch;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

import cpw.mods.fml.relauncher.FMLLaunchHandler;

@LateMixin
public final class LateMixinsLoader implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.futureresourceprotocol.late.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        Properties properties = readFeatures();
        if (properties == null) return Collections.emptyList();
        List<String> mixins = new ArrayList<>();
        if (loadedMods.contains("structurelib") && enabled(properties, "structureLibAdvance"))
            mixins.add("StructureLibAdvanceMixin");
        if (FMLLaunchHandler.side()
            .isClient() && loadedMods.contains("blockrenderer6343")
            && loadedMods.contains("NotEnoughItems")
            && enabled(properties, "structureOverlayMarks")) mixins.add("StructureOverlayMarksMixin");
        return mixins;
    }

    /** Use the same early switch for client mixin admission and server packet authorization. */
    public static boolean structureOverlayMarksEnabled() {
        Properties properties = readFeatures();
        return properties != null && enabled(properties, "structureOverlayMarks");
    }

    private static Properties readFeatures() {
        File file = new File(new File(Launch.minecraftHome, "config"), "frp-features.properties");
        Properties properties = new Properties();
        if (file.isFile()) try (FileInputStream stream = new FileInputStream(file)) {
            properties.load(stream);
        } catch (IOException ex) {
            Logger.getLogger("FRP")
                .warning("FRP feature settings could not be read; StructureLib integration disabled: " + ex);
            return null;
        }
        return properties;
    }

    private static boolean enabled(Properties properties, String key) {
        String value = properties.getProperty(key, "true")
            .trim();
        if (!"true".equalsIgnoreCase(value)) {
            if (!"false".equalsIgnoreCase(value)) Logger.getLogger("FRP")
                .warning("Invalid " + key + "; disabled");
            return false;
        }
        return true;
    }
}
