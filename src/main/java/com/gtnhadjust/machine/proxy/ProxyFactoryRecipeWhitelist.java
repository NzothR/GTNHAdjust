package com.gtnhadjust.machine.proxy;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Registry names eligible for proxy factory unlocks. */
public final class ProxyFactoryRecipeWhitelist {

    private ProxyFactoryRecipeWhitelist() {}

    // GTNR 通用联合工厂已有的 24 个配方池，顺序与 GTNR 0.1.34 一致。
    public static final List<String> ORIGINAL = Collections.unmodifiableList(
        Arrays.asList(
            "gt.recipe.metalbender", // 卷板机
            "gt.recipe.compressor", // 压缩机
            "gt.recipe.hammer", // 锻造锤
            "gt.recipe.cuttingsaw", // 板材切割机
            "gt.recipe.extruder", // 压模机
            "gt.recipe.lathe", // 车床
            "gt.recipe.wiremill", // 线材轧机
            "gt.recipe.press", // 冲压机床
            "gt.recipe.polarizer", // 两极磁化机
            "gt.recipe.laserengraver", // 激光蚀刻机
            "gt.recipe.fluidsolidifier", // 流体固化器
            "gt.recipe.assembler", // 组装机
            "gt.recipe.circuitassembler", // 电路组装机
            "gt.recipe.multicentrifuge", // 工业离心机
            "gt.recipe.thermalcentrifuge", // 热力离心机
            "gt.recipe.multielectro", // 工业电解机
            "gt.recipe.sifter", // 筛选机
            "gt.recipe.macerator", // 研磨机
            "gt.recipe.extractor", // 提取机
            "gt.recipe.multidehydrator", // 工业脱水机
            "gt.recipe.largechemicalreactor", // 大型化学反应釜
            "gt.recipe.multimixer", // 工业搅拌机
            "gt.recipe.chemicalbath", // 化学浸洗机
            "gt.recipe.orewasher" // 洗矿机
        ));

    // 代理联合工厂新增的默认配方池，独立于 GTNR 原有 24 池。
    public static final List<String> DEFAULT_EXTRA = Collections.singletonList("gt.recipe.furnace"); // 熔炉

    // 静态核对出的扩展候选；解锁后使用原工厂通用逻辑，不模拟原机器特殊条件。
    public static final List<String> ADDITIONAL = Collections.unmodifiableList(
        Arrays.asList(
            "gg.recipe.componentassemblyline", // 部件装配线
            "gg.recipe.precise_assembler", // 精密组装机
            "gt.recipe.alloyblastsmelter", // 合金冶炼炉
            "gt.recipe.alloysmelter", // 合金炉
            "gt.recipe.arcfurnace", // 电弧炉
            "gt.recipe.autoclave", // 高压釜
            "gt.recipe.canner", // 装罐机
            "gt.recipe.cokeoven", // 焦炉
            "gt.recipe.coldtrap", // 冷阱
            "gt.recipe.distillationtower", // 蒸馏塔
            "gt.recipe.distillery", // 蒸馏室
            "gt.recipe.efrblasting", // 蒸汽壁炉配方
            "gt.recipe.efrsmelting", // 蒸汽壁炉配方
            "gt.recipe.electricimplosioncompressor", // 电动聚爆压缩机
            "gt.recipe.electromagneticseparator", // 电磁离析机
            "gt.recipe.entropic-processing", // 混沌过程
            "gt.recipe.fluidchemicaleactor", // 化工厂
            "gt.recipe.fluidextractor", // 流体提取机
            "gt.recipe.fluidheater", // 流体加热器
            "gt.recipe.fusionreactor", // 核聚变反应堆
            "gt.recipe.implosioncompressor", // 聚爆压缩机
            "gt.recipe.industrialchisel", // 工业凿子
            "gt.recipe.industrialcokeoven", // 工业焦炉
            "gt.recipe.moleculartransformer", // 分子重组仪
            "gt.recipe.neutroniumcompressor", // 中子态素压缩机
            "gt.recipe.nuclearsaltprocessingplant", // 反应堆熔盐加工厂
            "gt.recipe.oremill", // 研磨
            "gt.recipe.packager", // 打包机
            "gt.recipe.plasmaforge", // 超维度等离子锻炉
            "gt.recipe.primitiveblastfurnace", // 砖高炉
            "gt.recipe.pyro", // 热解炉
            "gt.recipe.quantumforcesmelter", // 量子操纵者
            "gt.recipe.replicator", // 复制机
            "gt.recipe.simplewasher", // 简易洗矿池
            "gt.recipe.solarfactory", // 太阳能板制造厂
            "gt.recipe.transcendentplasmamixerrecipes", // 超维度等离子搅拌机
            "gt.recipe.unpackager", // 解包机
            "gt.recipe.vacfurnace", // 真空干燥炉
            "gt.recipe.vacuumfreezer", // 真空冷冻机
            "gtnl.recipe.alchemic_chemistry_set", // 秘法炼金
            "gtnl.recipe.blood_demon_injection", // 血魔注入
            "gtnl.recipe.decay_hastener", // 衰变扭曲
            "gtnl.recipe.electric_neutron_activator", // 电力中子活化器
            "gtnl.recipe.falling_tower", // 模拟坠星标位
            "gtnl.recipe.industrial_crucible", // 工业坩埚
            "gtnl.recipe.industrial_infusion_crafting", // 工业注魔矩阵
            "gtnl.recipe.industrial_shaped_arcane_crafting", // 工业奥术装配室
            "gtnl.recipe.infernal_coke_oven", // 焦炉但是超模
            "gtnl.recipe.isa_mill", // 湿法碾磨
            "gtnl.recipe.lava_maker", // 蒸汽蒸融
            "gtnl.recipe.mana_infusion", // 魔力灌注
            "gtnl.recipe.microorganism_master", // 微生物之主
            "gtnl.recipe.molecular_transformer", // 物质重组
            "gtnl.recipe.nature_spirit_array", // 自然灵阵
            "gtnl.recipe.plasma_condensation", // 等离子冷凝
            "gtnl.recipe.portal_to_alfheim", // 精灵传送门
            "gtnl.recipe.precision_laser_engraver", // 精密激光蚀刻
            "gtnl.recipe.primitive_brick_kiln", // 原始土窑
            "gtnl.recipe.rocket_assembler", // 火箭组装厂
            "gtnl.recipe.rune_altar", // 符文祭坛
            "gtnl.recipe.shallow_chemical_coupling", // 浅层化学耦合
            "gtnl.recipe.smelting_mixing_furnace", // 合金搅拌
            "gtnl.recipe.steam_carpenter", // 蒸汽工艺
            "gtnl.recipe.steam_fusion_reactor", // 高压反应堆
            "gtnl.recipe.steam_gate_assembler", // 液压动力精密制造
            "gtnl.recipe.steam_manufacturer", // 液压制造
            "gtsr.recipe.siemens_martin_furnace", // 平炉
            "kubatech.defusioncrafter", // 龙研聚合装置
            "mt.recipe.steel_brick", // 超泥土锻炉: 冶金模式
            "ote.computer.recipe", // 异星计算机可用部件配方
            "otht.recipe.AE2", // 奇点制造机
            "otht.recipe.AbstractSanctumCraft", // 奈隆圣地 - 制作
            "otht.recipe.AbstractSanctumDuplicate", // 奈隆圣地 - 复制
            "otht.recipe.BIN", // 垃圾桶
            "otht.recipe.DeepSeikoAL", // 大肥鱼装配
            "otht.recipe.DeepSeikoBEC", // 大肥鱼凝聚
            "otht.recipe.EIO", // 末影接口综合体
            "otht.recipe.EXH", // 吊炸天热交换
            "otht.recipe.MCA", // 进阶高能电路装配线
            "otht.recipe.MISA", // 艾萨集成工厂
            "otht.recipe.MegaIsaForge", // 艾萨锻炉-老登制造者
            "otht.recipe.MegaPCB", // 巨型PCB工厂
            "otht.recipe.MegaPCB_A", // 贴片工坊
            "otht.recipe.MegaPCB_B", // NMD晶圆厂
            "otht.recipe.MegaPCB_C", // 元件聚合者
            "otht.recipe.NQF", // 压缩硅岩燃料精炼厂
            "otht.recipe.OTEMegaQFT", // 狄拉克逆变
            "otht.recipe.SINOPEC", // 中国石化
            "otht.recipe.TSSF", // 唐山炼钢厂
            "otht.recipe.WoodenFusionReactor", // 压缩原木聚变反应堆Mk 0
            "otht.recipe.nasa", // CNSA - 装配式
            "otht.recipe.steam.pump", // 蒸汽九合一-水泵
            "simplification.recipe.petrochemical_complex", // 石油化工综合体
            "tst.common.recipe.BloodyHellRecipeMap.name", // 血狱祭坛
            "tst.common.recipe.CokingFactoryRecipeMap.name", // 工业炼焦
            "tst.common.recipe.CrystallineInfinitierRecipeMap.name", // 晶胞铸造器
            "tst.common.recipe.DeployedNanoCoreRecipeMap.name", // 纳米核心
            "tst.common.recipe.ElvenWorkshopRecipeMap.name", // 魔力灌注
            "tst.common.recipe.HyperSpacetimeTransformerRecipeMap.name", // 极限时空转换仪
            "tst.common.recipe.IndustrialAlchemyTowerRecipeMap.name", // 工业炼金塔
            "tst.common.recipe.IndustrialMagicMatrixRecipeMap.name", // 工业注魔矩阵配方
            "tst.common.recipe.IntensifyChemicalDistorterRecipeMap.name", // 深度化学扭曲
            "tst.common.recipe.MassFabricatorGenesisRecipeMap.name", // 质量发生器: 创世纪
            "tst.common.recipe.MegaSolarPanelFactoryRecipeMap.name", // 巨型太阳能板工厂
            "tst.common.recipe.MegaStoneBreakerRecipeMap.name", // 巨型碎石机
            "tst.common.recipe.MicroSpaceTimeFabricatorioRecipeMap.name", // 微型时空发生器
            "tst.common.recipe.MiracleTopRecipeMap.name", // 奇迹顶点
            "tst.common.recipe.NeutronActivatorWithEURecipeMap.name", // 中子振荡器 - 电力版中子活化器
            "tst.common.recipe.PreciseHighEnergyPhotonicQuantumMasterRecipeMap.name", // 光子掌控者
            "tst.common.recipe.QuantumInversionRecipeMap.name", // 量子逆变
            "tst.common.recipe.RapidCoolingDownRecipeMap.name", // 快速冷却
            "tst.common.recipe.RapidHeatExchangeRecipeMap.name", // 快速热交换
            "tst.common.recipe.RuneEngraverRecipeMap.name", // 符文雕刻
            "tst.common.recipe.SkypiercerTowerRecipeMap.name", // 穿云尖塔
            "tst.common.recipe.VisualExtremeCraftRecipeMap.name", // 梦魇工作台配方转换
            "tst.common.recipe.StarKernelGeneratorRecipeMap.name", // 星核发生器
            "tst.common.recipe.StellarForgeAlloySmelterRecipeMap.name", // 恒星锻炉: 合金冶炼
            "tst.common.recipe.StellarForgeRecipeMap.name" // 恒星锻炉
        ));
}
