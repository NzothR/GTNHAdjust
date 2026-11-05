package com.gtnhadjust;

import java.util.List;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

import cpw.mods.fml.relauncher.FMLLaunchHandler;

@LateMixin
public class LateMixinsLoader implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.gtnhadjust.late.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        return FeatureConfig.get()
            .selectMixins(
                loadedMods,
                FMLLaunchHandler.side()
                    .isClient());
    }
}
