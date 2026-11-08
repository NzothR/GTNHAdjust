package com.futureresourceprotocol;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import net.minecraft.launchwrapper.Launch;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

@LateMixin
public final class LateMixinsLoader implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.futureresourceprotocol.late.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        if (!loadedMods.contains("structurelib")) return Collections.emptyList();
        File file = new File(new File(Launch.minecraftHome, "config"), "frp-features.properties");
        Properties properties = new Properties();
        if (file.isFile()) try (FileInputStream stream = new FileInputStream(file)) {
            properties.load(stream);
        } catch (IOException ex) {
            Logger.getLogger("FRP")
                .warning("FRP feature settings could not be read; StructureLib integration disabled: " + ex);
            return Collections.emptyList();
        }
        String value = properties.getProperty("structureLibAdvance", "true")
            .trim();
        if (!"true".equalsIgnoreCase(value)) {
            if (!"false".equalsIgnoreCase(value)) Logger.getLogger("FRP")
                .warning("Invalid structureLibAdvance; disabled");
            return Collections.emptyList();
        }
        Logger.getLogger("FRP")
            .info("StructureLib advance source mixin enabled (StructureLib 1.4.47)");
        return Collections.singletonList("StructureLibAdvanceMixin");
    }
}
