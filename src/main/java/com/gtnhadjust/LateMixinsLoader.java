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
        boolean client = FMLLaunchHandler.side()
            .isClient();
        List<String> mixins = features.selectMixins(loadedMods, client);
        if (loadedMods.contains("ae2thing") && "1.3.4".equals(
            Loader.instance()
                .getIndexedModList()
                .get("ae2thing")
                .getVersion())) {
            if (features.ae2thingTerminalRecipeFix && loadedMods.contains("gtneverreset"))
                mixins.add("fix.Ae2ThingTerminalRecipeMixin");
            if (client && features.ae2thingInfinityCellUsageFix && loadedMods.contains("NotEnoughItems"))
                mixins.add("fix.Ae2ThingInfinityCellUsageMixin");
        }
        return mixins;
    }
}
