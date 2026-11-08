package com.gtnhadjust.machine.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class ProxyFactoryRecipeWhitelistTest {

    @Test
    public void originalAndAdditionalNamesAreUnique() {
        Set<String> names = new HashSet<>(ProxyFactoryRecipeWhitelist.ORIGINAL);
        assertEquals(24, names.size());
        assertEquals(ProxyFactoryRecipeWhitelist.ORIGINAL.size(), names.size());
        int expected = names.size() + ProxyFactoryRecipeWhitelist.ADDITIONAL.size();
        names.addAll(ProxyFactoryRecipeWhitelist.ADDITIONAL);
        assertEquals(expected, names.size());
    }

    @Test
    public void explicitInclusionsAndExclusionsAreRespected() {
        Set<String> names = new HashSet<>(ProxyFactoryRecipeWhitelist.ORIGINAL);
        names.addAll(ProxyFactoryRecipeWhitelist.ADDITIONAL);
        for (String included : Arrays.asList(
            "tst.common.recipe.VisualExtremeCraftRecipeMap.name", // 梦魇转换配方池
            "gt.recipe.multicentrifuge", // 原厂离心，优先于排除大型蒸汽离心机。
            "gg.recipe.componentassemblyline", // 用户明确要求全局保留。
            "gg.recipe.precise_assembler",
            "otht.recipe.TSSF", // 唐山炼钢厂
            "otht.recipe.EIO", // 末影接口综合体
            "otht.recipe.OTEMegaQFT", // 狄拉克潮汐
            "tst.common.recipe.ElvenWorkshopRecipeMap.name", // 精灵工坊
            "tst.common.recipe.RuneEngraverRecipeMap.name",
            "tst.common.recipe.HyperSpacetimeTransformerRecipeMap.name", // 极限时空转换仪
            "tst.common.recipe.MegaSolarPanelFactoryRecipeMap.name")) { // 巨型太阳能板工厂
            assertTrue(included, names.contains(included));
        }
        for (String excluded : Arrays.asList(
            "gt.recipe.blastfurnace", // 太阳烈焰能量锻炉与工业高炉共用。
            "gt.recipe.fakeAssemblylineProcess",
            "tst.common.recipe.AssemblyLineWithoutResearchRecipeMap.name",
            "gt.recipe.cyclotron",
            "gt.recipe.flotationcell",
            "gt.recipe.brewer",
            "gt.recipe.fermenter",
            "gt.recipe.lftr",
            "gt.recipe.lftr.sparging",
            "gtsr.recipe.critical.loom",
            "mt.recipe.nanoscale.assemblymatrix",
            "gt.recipe.upgrade_costs")) {
            assertFalse(excluded, names.contains(excluded));
        }
    }
}
