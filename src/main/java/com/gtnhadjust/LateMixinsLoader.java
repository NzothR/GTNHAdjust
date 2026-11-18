package com.gtnhadjust;

import java.util.List;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.FMLLaunchHandler;

@LateMixin
public class LateMixinsLoader implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.gtnhadjust.late.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        FeatureSwitches features = FeatureConfig.get();
        List<String> mixins = features.selectMixins(
            loadedMods,
            FMLLaunchHandler.side()
                .isClient());
        if (features.ae2thingTerminalRecipeFix && loadedMods.contains("ae2thing")
            && loadedMods.contains("gtneverreset")
            && "1.3.4".equals(
                Loader.instance()
                    .getIndexedModList()
                    .get("ae2thing")
                    .getVersion())) {
            mixins.add("fix.Ae2ThingTerminalRecipeMixin");
        }
        return mixins;
    }
}
