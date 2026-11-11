# 代理联合工厂配方池范围

本表基于当前服务端模组文件及参考源码整理，代码中的注册名与逐项中文注释见 `ProxyFactoryRecipeWhitelist.java`。配方池以注册名识别，运行时不以机器物品或机器类作为排除依据。清单包含 GTNR 原有 24 个池、默认熔炉池及扩展候选；扩展池须输入提供该配方池的 GT 控制器后才会解锁。候选名称的静态核对不等于确认它们会在当前配置中实际注册。

原厂 24 池具有最高优先级：被排除的机器即使使用这些池，也不影响原有支持。例如大型蒸汽离心机使用的多方块离心配方池仍保留。用户另外明确要求全局保留部件装配线和精密组装机配方池；即使全能原初装配矩阵使用部件装配线配方池，该池仍保留。

明确不纳入普通装配线展示池、巨型装配线池和回旋加速器池；纳米铸造厂及各模块、净化水线、发电燃料、诸神锻炉和用户指定排除的其他机器所使用的非例外池均从候选中排除。太阳烈焰能量锻炉使用的工业高炉配方池也按用户确认排除。拟态织机的建设展示池及气涤塔、液态氟钍反应堆池均不纳入。

Simplification 的现代产业园（`simplification.recipe.modern_industrial_park`）可通过其控制器解锁。Simplification 的石油化工综合体（`simplification.recipe.petrochemical_complex`）与被排除的 GTNL 石化工厂是不同配方池，前者允许解锁。特别指定的唐山炼钢厂、末影接口综合体、狄拉克潮汐、精灵工坊（两个配方池）、极限时空转换仪及巨型太阳能板工厂均已列入。机器直接采用 GTNR 的通用执行逻辑，不检查原机器专属结构等级或环境，也不模拟原机器的特殊加工；动态后端、自定义查找与特殊输入可能影响实际匹配，需要游戏内验证。

GTNL 燃料精炼复合体配方池 `gtnl.recipe.fuel_refining_complex` 已纳入扩展候选。燃料精炼复合体与原子能激发厂共用此池，任一控制器都可提供同一解锁；按代理工厂现有规则，仅接收能新增解锁的控制器，退回后根据剩余控制器重新计算。代理工厂不检查原机器的线圈等级、加热温度或其他专属条件，实际配方和样板需游戏内验证。

输入、运行和输出三种模式的操作、保存和退回规则见 README。输入模式仅接收普通实体输入总线中的控制器，客户端和服务端需使用相同配置及模组版本并重启。

## 已定位的排除池

以下是从被排除机器的源码直接定位到的注册名，以及单独确认的排除项。这里只记录本轮静态核对到的池；动态引用、源码版本差异或服务端配置仍需在后续运行时核查。原有 24 池和用户特别保留的部件装配线、精密组装机不在此表中。

| 配方池注册名 | 排除依据 |
| --- | --- |
| `bw.fuels.acidgens` | 大型中和引擎 |
| `bw.recipe.BacteriaVat` | 生物圈III号 |
| `bw.recipe.cal` | 进阶电路装配线 |
| `gg.recipe.naquadah_reactor` | 大型硅岩湮灭场发电机 |
| `gt.recipe.algae_pond` | 藻类农场 |
| `gt.recipe.beamcrafter` | 束流合成器 |
| `gt.recipe.blastfurnace` | 太阳烈焰能量锻炉 |
| `gt.recipe.brewer` | 生物圈III号 |
| `gt.recipe.centrifuge` | 大型蒸汽离心机 |
| `gt.recipe.craker` | 巨型石油裂化机 |
| `gt.recipe.cyclotron` | 粒子加速器 |
| `gt.recipe.dieselgeneratorfuel` | 通用发电机 |
| `gt.recipe.extremedieselgeneratorfuel` | 极限内燃引擎 |
| `gt.recipe.eyeofharmony` | 永恒格雷工坊 - 鸿蒙之眼模块 |
| `gt.recipe.fakeAssemblylineProcess` | 大型装配线 |
| `gt.recipe.fermenter` | 生物圈III号 |
| `gt.recipe.fishpond` | 珠海渔场 |
| `gt.recipe.fissionfuel` | 反应堆燃料加工厂 |
| `gt.recipe.flotationcell` | 工业浮选机 |
| `gt.recipe.fog_exotic` | 太阳聚变异化器 |
| `gt.recipe.fog_molten` | 太阳射流融化核心 |
| `gt.recipe.fog_plasma` | 太阳热能等离子制造机 |
| `gt.recipe.foundry_modules` | 异星铸造塔模块成本 |
| `gt.recipe.furnace` | 太阳烈焰能量锻炉 |
| `gt.recipe.gasturbinefuel` | 通用发电机 |
| `gt.recipe.isotope-decay` | 放射物衰变库 |
| `gt.recipe.large_hadron_collider` | 大型强子对撞机 |
| `gt.recipe.lftr` | 液态氟钍反应堆 |
| `gt.recipe.lftr.sparging` | 气涤塔 |
| `gt.recipe.matterfab2` | 物质发生器CPU |
| `gt.recipe.multiblockrockbreaker` | 工业碎石机 |
| `gt.recipe.nanochip.assemblymatrix` | 纳米芯片组装矩阵 |
| `gt.recipe.nanochip.biologicalcoordinator` | 加速生物调谐器 |
| `gt.recipe.nanochip.boardprocessor` | 全板浸没设备 |
| `gt.recipe.nanochip.conversion` | 纳米芯片组装复合体 |
| `gt.recipe.nanochip.cuttingchamber` | 纳米精度切割仓 |
| `gt.recipe.nanochip.encasementwrapper` | 纳米封装卷绕器 |
| `gt.recipe.nanochip.etchingarray` | 超高能蚀刻阵列 |
| `gt.recipe.nanochip.opticalorganizer` | 光学优化组织器 |
| `gt.recipe.nanochip.partprocessor` | 元件预处理装置 |
| `gt.recipe.nanochip.superconductorsplitter` | 超导线束分离器 |
| `gt.recipe.nanochip.wiretracer` | 纳米精度导线追踪器 |
| `gt.recipe.nanoforge` | 纳米锻炉 |
| `gt.recipe.pcbfactory` | PCB工厂 |
| `gt.recipe.plasmageneratorfuels` | 大型等离子涡轮 |
| `gt.recipe.purificationplantclarifier` | 澄清净化单元 |
| `gt.recipe.purificationplantdegasifier` | 残留净化剂除气机净化单元 |
| `gt.recipe.purificationplantflocculation` | 絮凝净化单元 |
| `gt.recipe.purificationplantozonation` | 臭氧净化单元 |
| `gt.recipe.purificationplantphadjustment` | pH中和净化单元 |
| `gt.recipe.purificationplantquarkextractor` | 绝对重子完美净化单元 |
| `gt.recipe.purificationplantuvtreatment` | 高能激光净化单元 |
| `gt.recipe.quantumcomputer` | 运算中心 |
| `gt.recipe.researchStation` | 纳米计算中心 |
| `gt.recipe.rocketenginefuel` | 火箭引擎 F-1A |
| `gt.recipe.scanner` | 纳米计算中心 |
| `gt.recipe.semifluidgeneratorfuels` | 大型半流质发电机 |
| `gt.recipe.solartower` | 太阳能塔 |
| `gt.recipe.spaceAssembler` | 太空组装机 |
| `gt.recipe.thermalboiler` | 地热锅炉 |
| `gt.recipe.treefarm` | 原木拟生场 |
| `gt.recipe.upgrade_costs` | 诸神锻炉升级成本 |
| `gtnhlanth.recipe.digester` | 溶解核心 |
| `gtnhlanth.recipe.disstank` | 溶解核心 |
| `gtnl.recipe.cactus_wonder_fake` | 仙人掌奇迹 |
| `gtnl.recipe.cell_regulator` | 工业浮选机 |
| `gtnl.recipe.cheat_ore_processing` | 作弊者的究极集成矿处 |
| `gtnl.recipe.desulfurizer` | 脱硫机 |
| `gtnl.recipe.electrocell_generator` | 电化学电池发电机 |
| `gtnl.recipe.eternal_greg_tech_workshop_upgrade` | 永恒格雷工坊 |
| `gtnl.recipe.extreme_extreme_entity_crusher` | 蒸汽屠宰场模块 |
| `gtnl.recipe.fishing_ground` | 渔场 |
| `gtnl.recipe.gas_collector` | 大型集气室 |
| `gtnl.recipe.grand_assembly_line_special` | 大型装配线 |
| `gtnl.recipe.industrial_rock_crusher` | 地层撕裂者 |
| `gtnl.recipe.lapotron_chip` | 兰波顿工厂 |
| `gtnl.recipe.large_bio_lab` | 大型生物实验室 |
| `gtnl.recipe.matter_fabricator` | 物质生成机 |
| `gtnl.recipe.nanites_integrated_processing` | 纳米集成加工中心 |
| `gtnl.recipe.petrochemical_plant` | 石化工厂 |
| `gtnl.recipe.platinum_based_treatment` | 铂族工业统合枢纽 |
| `gtnl.recipe.rare_earth_centrifugal` | 稀土离心机 |
| `gtnl.recipe.real_artificial_star.generating` | 真·湮灭发电机 |
| `gtnl.recipe.recombination_fusion_reactor` | 初代目行星发动机 |
| `gtnl.recipe.rock_breaker` | 蒸汽造石机 |
| `gtnl.recipe.solar_muon_catalyst` | 太阳μ子催化器 |
| `gtnl.recipe.space_drill` | 太空电梯资源采集模块 |
| `gtnl.recipe.space_miner` | 太空电梯资源采集模块 |
| `gtnl.recipe.steam_cracker` | 蒸汽裂化机 |
| `gtnl.recipe.steam_extractinator` | 蒸汽精萃机 |
| `gtnl.recipe.steam_weather_module` | 蒸汽天气更替模块 |
| `gtnl.recipe.the_twilight_forest` | 废墟图书馆 |
| `gtnl.recipe.tree_diagram` | 树状图设计者 |
| `gtnl.recipe.wood_distillation` | 木化工厂 |
| `gtnl.recipe.woodcutter` | 蒸汽树场 |
| `gtsr.recipe.air_compressor` | 空气压缩机 |
| `gtsr.recipe.ammonia_plant` | 制氨工厂 |
| `gtsr.recipe.atmospheric_centrifuge` | 大气离心机 |
| `gtsr.recipe.critical.loom` | 拟态织机建设展示池 |
| `gtsr.recipe.critical_singularity_compressor` | 临界纠缠奇点稳定装置 |
| `gtsr.recipe.dense_state_manipulator` | 致密态蒸汽操控装置 |
| `gtsr.recipe.gear_steam_compressor` | 自驱式机械蒸汽压缩机 |
| `gtsr.recipe.geothermal_steam_boiler` | 大型地热蒸汽锅炉 |
| `gtsr.recipe.steam_fluid_drill` | 蒸汽流体钻井 |
| `gtsr.recipe.steam_singularity_entangler` | 蒸汽奇点纠缠装置 |
| `ic.recipe.recycler` | 物质发生器CPU |
| `mt.recipe.assfactory.assemblyline` | 全能原初装配矩阵 |
| `mt.recipe.boses_crafting_array` | 玻色-爱因斯坦凝聚态物质聚合阵列 |
| `mt.recipe.chemicaltwister` | 大型化学扭曲者 |
| `mt.recipe.comb_processing` | 大型化学扭曲者 |
| `mt.recipe.computingcenter` | 纳米计算中心 |
| `mt.recipe.leg` | 大型源质发电机 |
| `mt.recipe.nanoscale.assemblymatrix` | 纳米铸造厂 |
| `mt.recipe.nanoscale.biologicalcoordinator` | 纳米铸造厂 |
| `mt.recipe.nanoscale.boardprocessor` | 纳米铸造厂 |
| `mt.recipe.nanoscale.conversion` | 纳米铸造厂 |
| `mt.recipe.nanoscale.cuttingchamber` | 纳米铸造厂 |
| `mt.recipe.nanoscale.encasementwrapper` | 纳米铸造厂 |
| `mt.recipe.nanoscale.etchingarray` | 纳米铸造厂 |
| `mt.recipe.nanoscale.onestepcircuitpool` | 纳米铸造厂 |
| `mt.recipe.nanoscale.opticalorganizer` | 纳米铸造厂 |
| `mt.recipe.nanoscale.smdprocessor` | 纳米铸造厂 |
| `mt.recipe.nanoscale.superconductorsplitter` | 纳米铸造厂 |
| `mt.recipe.nanoscale.wiretracer` | 纳米铸造厂 |
| `mt.recipe.qft_probability_destroyer` | 大型化学扭曲者 |
| `recipe.gtneverreset.ore_processing` | 大型矿石处理机 |
| `tst.common.recipe.AdvCircuitAssemblyLineRecipeMap.name` | 进阶电路装配线 |
| `tst.common.recipe.AssemblyLineWithoutResearchRecipeMap.name` | 巨型装配线 |
