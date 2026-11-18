# 配置说明

当前基线为 GTNH 2.9.0、GTNR 0.1.48、GTNRLib 0.1.35。功能按目标模组是否安装加载；目标版本与本基线一致。所有启动开关修改后重启，跨端玩法功能保持客户端与服务端一致。纯客户端功能仅在客户端启停。

## 功能开关

文件：`config/gtnhadjust-features.properties`。默认开启的功能见下表；关闭 Mixin 功能后不加载相应注入。机器开关控制机器及其制作配方的注册。

| 开关 | 功能 | 范围 |
| --- | --- | --- |
| `gtnrVirtualTools` | 完整 NC 元数据、虚拟工具目录与 PH 可用状态桥接 | 两端；默认开启，关闭不加载本组六个共用 Mixin 和一个客户端导入 Mixin |
| `proxyFactory` | 代理联合工厂 | 两端 |
| `threadedJointProxy` | 多线程联合代理 | 两端 |
| `substanceReshapingDevice` | EyeOfHarmonyBuffer EV 物质重塑仪、控制器制作及 16 条加工配方，ID 31996 | 两端；默认开启，GTNH 2.9.0，重启生效 |
| `godforgeFreeUpgrades` | 诸神之锻炉升级无前置、环数、碎片或材料限制 | 两端；默认开启，GregTech 5.09.54.265，关闭后不加载对应两项 Mixin，重启生效 |
| `lootSimulator` | 战利品模拟室 | 两端 |
| `cropBreeder` | GTNR 作物杂交机调整 | 两端 |
| `steamGreenhouse` | GTNL 蒸汽温室容量 | 两端 |
| `grandAssemblyAutoMaintenance` | GTNL 大型装配线自动维护 | 两端 |
| `carpenterRecipes` | 木工机与农场组装机配方 | 两端 |
| `quickTerminal` | 二合一终端编码、上传及界面改善；上传接口名称按去除颜色和样式代码后的纯文字匹配 | 两端；含扩展转移数据包，兼容 GTNR 0.1.48，重启生效 |
| `patternDeduplication` | 接口样板按产物类型及 NBT 去重 | 两端 |
| `terminalCraftingRefreshFix` | 样板修改后的合成状态刷新 | 两端 |
| `patternRecipeMapPreservation` | 修改加工样板时保留来源配方池 | 两端 |
| `terminalInventoryFix` | 终端库存同步及 NEI 状态 | 客户端 |
| `terminalNeiAlwaysShowPresence` | NEI 右上角常驻库存／缺失状态与覆盖按钮提示，可合成样板标记在左上角 | 客户端；依赖库存修复及 NEE，适配 GTNH 2.9.0、NEI 2.8.171、NEE 1.7.50，更新后重启 |
| `terminalSmartIngredients` | 收藏、电路和网络材料选择优先级 | 两端；依赖 `quickTerminal` |
| `gtnlQuickTerminalCraftFix` | GTNL NEI 请求合成的终端槽位修复 | 两端 |
| `ae2thingTerminalRecipeFix` | AE2Thing 1.3.4 和 GTNR 同装时，跳过未注册的无线二合一终端的三处配方注册 | 两端；默认开启，重启生效 |
| `ae2thingInfinityCellUsageFix` | 修复 AE2Thing 1.3.4 的 NEI 无限存储元件用途查询报错 | 客户端；默认开启，重启生效 |
| `matrixPatternLoadFix` | 装配矩阵读档样板失效修复 | 两端 |
| `bookmarkMachineGroups` | V 排序、Shift+V 按池分组 | 客户端 |
| `hideGtnlShimmerRecipes` | 隐藏 GTNL 微光嬗变 NEI 页面 | 客户端 |
| `hideGtnrDisassemblerRecipes` | 隐藏 GTNR 拆解机 NEI 页面 | 客户端 |
| `hologramImprovements` | 全息投影仪生存建造改善总开关 | 两端 |
| `planetaryEngineProjector` | 初代目行星发动机无粒子预览、整机外框、结构定义缓存和延后检查，使用 `structure_render=1` | 两端；默认开启，GTNL dev-290／StructureLib 1.4.47，重启生效 |
| `projectorBuildBudget` | 投影仪的 `placement_budget` 覆盖生存自动放置预算，最高 2,100,000,000 格 | 两端；默认开启，StructureLib 1.4.47，重启生效 |
| `structureLibAeAutoPlace` | 生存建造从 AE 取材料与流体 | 两端；受投影仪总开关控制 |
| `questAeRetrieval` | BetterQuesting 从无线 AE 检索／交付 | 两端 |
| `soulVialExpandedCapture` | Ender IO 灵魂瓶扩展捕获 | 两端 |
| `ysmuAnimatedArms` | YSMU 第一人称手臂兼容 | 客户端 |
| `gtsrRecipeSpeedCompatibility` | GTSR 配方耗时兼容 | 两端 |
| `gtsrDiagnostics` | GTSR 只读诊断注入，输出受 debug 控制 | 两端 |

以下默认关闭：`neoEcoCraftingRefreshFix`（NeoECOAE 刷新修复）、`craftingDiagnostics`、`matrixPatternDiagnostics`、`proxyDispatchDiagnostics`（专项诊断）。

## 机器与独立功能参数

文件：`config/gtnhadjust.cfg`。

| 参数 | 默认值 | 用途 |
| --- | --- | --- |
| `general.debug` | `false` | 机器、定位器与配方诊断日志 |
| `recipeAdjustments.recombinationFusion` | `true` | 用新注册的 1 号电路配方替换 GTNL 重聚变原配方，消耗 2.1G 石头粉，基础耗时 64 秒，耗能 0 EU/t；两端同步并重启 |
| `threadedJointProxy.patternSlots` | `1024` | 内部样板容量，范围 1–16384；两端同步并重启 |
| `threadedJointProxy.wirelessPower` | `true` | 多线程代理无能源仓时无线供电，无玻璃门槛 |
| `threadedJointProxy.voidUnlistedOutputs` | `true` | 多线程代理丢弃启动样板未列出的物品和流体；两端同步并重启 |
| `proxyFactory.wirelessPower` | `true` | 代理工厂 UEV+ 玻璃且无能源仓时无线供电 |
| `proxyFactory.locatorSlots` | `128` | 两种代理共用的定位器容量，范围 1–32730 |
| `proxyFactory.locatorNavigation` | `true` | 定位器空气右键高亮与潜行传送 |
| `proxyFactory.indirectLocators` | `true` | 两种代理可通过一台中转代理读取直连多方块定位器；两端同步并重启 |
| `proxyFactory.extremeCrafting` | `true` | TST 梦魇工作台转换加工样板桥接 |
| `collector.singularityResourceRecipes` | `true` | 维度／等级标记与资源配方 |
| `collector.directAeOutput` | `true` | 主机直连 AE 优先输出，节点不可用时回退输出舱室；重启生效 |
| `collector.matterCompression` | `true` | 启用物质压缩模式和空白元件终极合成；两端同步并重启 |
| `collector.baseParallel` | `64` | LV 每线程并行能力，指数曲线起点 |
| `collector.maxVoltageParallel` | `2147483647` | MAX 每线程并行能力，指数曲线终点，范围 1–2147483647 |
| `collector.wirelessPower` | `true` | 物质解压器无能源仓时无线供电 |
| `collector.blackHoleRendering` | `true` | 黑洞、约束场和向外喷流特效 |
| `collector.markerOutputMultiplier` | `true` | 同种标记提高资源产出，最多计 64 个 |
| `recipePoolDebugger.enabled` | `true` | 配方池调试主机与目录 |
| `gtnl.steamGreenhouseMaxSeeds` | `64` | 温室种子容量，范围 1–64 |

机器 ID 和物质解压器的基础并行、线程、配方时间等参数也在此文件中；取得或放置主机后保持 ID 不变。温室缩容前取出超量种子。定位器缩容会保留已有超额槽位，输入模式可取出。

两种代理任一开启时注册共用定位器及其制作配方。无线模式使用 GT 所有者／队伍账户；具体输出、停机及余额不足行为见 [机器说明](machines.md)。新线程才使用调整后的机器 OC 设置。

## 交互细则

终端 NEI 的普通 `+` 导入配方，Shift／Alt 组合进一步自动编码；终端上传、去重与接口偏好随各自开关生效。去重按产物类型集合及 NBT 比较，不按输入或产出数量区分。网络状态提示分别显示可合成、已有库存和缺失材料。

全息生存建造优先使用玩家库存，再通过有电、在范围内且有权限的无线终端从 AE 取料。显式选择等级信道后可替换能够确认属于同一等级序列的结构方块，机器需先停止；流体源每格消耗 1000 mB。

任务 AE 检索仅由按钮触发，沿用任务的消耗、矿辞、NBT 和队伍规则；普通自动检测不查询 AE。GTNR 二合一终端可作为无线入口。

## 验证范围

构建和静态检查由开发流程执行，游戏内容测试在实际 GTNH 实例中完成。无线更新重点检查无能源仓、普通／特殊能源仓、余额不足和保存恢复；提示更新检查两种机器、中英文客户端、GTNH 分段与颜色，确认未附加模块或线程仓说明。

## GTNR 不消耗物品扩展

`gtnrVirtualTools=true` 是此功能的独立总开关，旧配置缺省也开启，可手动添加后设为 `false`。两端一致并重启生效。兼容 GTNH 2.9.0、GTNR 0.1.48、PH 0.2.1p7；PH 与 TST 为可选联动，不增加它们的硬依赖。

开启终端保留不消耗物品选项后，NEI 导入保留全部零数量 NC，包括目录外工具，供编码时按真实配方核对。新编码的处理样板追加 `gtnhadjustPatternNonConsumed`，完整保存所有 NC 的物品、meta 和 NBT，继续写入原有 `gtnrPatternCircuit`、`gtnrPatternMolds`；全部 NC 从 ME 原料请求中移除，目录外工具另有需求提示。目录内工具免费内置；目录外工具从样板总成共享槽、机器实际输入或同一 AE 网络的实物库存取得存在性证明，也可用同网中对应 PH `prog_circuit` 的库存／可合成状态满足。不提取 NC，不提交 PH 合成任务。两种代理和 GTNR 通用联合工厂使用完整输入与 NC、整批产物数量校验新样板，避免相同消耗原料、不同工具的配方冲突。

旧样板没有新字段时保留旧格式和兼容匹配，不删除物品或改写原样板。重新编辑、编码后追加完整清单。关闭扩展会恢复 GTNR 原目录与处理流程；含新增目录物品或外部工具字段的新样板需要在重新开启扩展后使用，或按原 GTNR 规则重新编码。原生 AE 处理样板仍需要实际输入需求，纯 NC 配方保留既有样板形态。

`general.debug` 默认关闭，开启时可查看 `[gtnr-virtual-tools]` 目录扩展数量和样板匹配诊断。游戏内验证多个 NC 同时记录、目录外实物／PH 库存／PH 可合成状态、断网后停止启动、不产生 PH 合成任务、编辑后工具还原、仅 NC 配方的多线程代理加工，以及旧样板不重编码仍能继续运行。

## 多线程代理样板参数

`gtnrVirtualTools` 的目录包含 PH `prog_toolkit` 的 0、1、2 三种模式。仅对配方中的零数量 NC 免提供，不改变工具包作为消耗原料时的需求；此目录同时用于编码、总成虚拟供料及两种代理的工具检查。适配 PH 0.2.1p7，修改开关及更新版本后两端重启。

内部样板栏、逐槽调度及主机 AE 接入随 `threadedJointProxy` 启停。样板 NBT 中的 `gtnhadjustThread` 独立保存 target、power、parallel、oc；除首次匹配的 power 为配方 EU/t 外默认 -1。修改产量或耗能时一次计算并回填并行、OC，实际执行继续受全局参数限制。界面、单位、搜索及调度规则见 [机器说明](machines.md)。

物质解压器不再使用 `collector.voltageParallelMultiplier`。默认曲线为 `round(64 × (2147483647 / 64)^((电压等级 - 1) / 13))`，LV 与 MAX 端点精确固定；手动上限对每个线程分别取较小值。HV 为最低玻璃要求；玻璃提升线程数，每级降低 15 个百分点的耗时与耗能，最低 1%。并行曲线和 AE 输出参数两端同步并重启。
