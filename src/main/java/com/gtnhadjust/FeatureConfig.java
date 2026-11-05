package com.gtnhadjust;

import java.io.File;

import net.minecraft.launchwrapper.Launch;

final class FeatureConfig {

    private static FeatureSwitches switches;

    private FeatureConfig() {}

    static synchronized FeatureSwitches get() {
        if (switches == null) {
            File file = new File(new File(Launch.minecraftHome, "config"), "gtnhadjust-features.properties");
            switches = FeatureSwitches.load(file.toPath());
        }
        return switches;
    }
}
