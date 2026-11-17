# 配置说明

当前基线为 GTNH 2.9.0、GTNR 0.1.47、GTNRLib 0.1.35。功能按目标模组是否安装加载；目标版本与本基线一致。所有启动开关修改后重启，跨端玩法功能保持客户端与服务端一致。纯客户端功能仅在客户端启停。

## 功能开关

文件：`config/gtnhadjust-features.properties`。默认开启的功能见下表；关闭 Mixin 功能后不加载相应注入。机器开关控制机器及其制作配方的注册。

| 开关 | 功能 | 范围 |
| --- | --- | --- |
| `proxyFactory` | 代理联合工厂 | 两端 |
| `threadedJointProxy` | 多线程联合代理 | 两端 |
| `lootSimulator` | 战利品模拟室 | 两端 |
| `cropBreeder` | GTNR 作物杂交机调整 | 两端 |
| `steamGreenhouse` | GTNL 蒸汽温室容量 | 两端 |
| `grandAssemblyAutoMaintenance` | GTNL 大型装配线自动维护 | 两端 |
| `carpenterRecipes` | 木工机与农场组装机配方 | 两端 |
| `quickTerminal` | 二合一终端编码、上传及界面改善 | 两端；含扩展转移数据包 |
| `patternDeduplication` | 接口样板按产物类型及 NBT 去重 | 两端 |
| `terminalCraftingRefreshFix` | 样板修改后的合成状态刷新 | 两端 |
| `patternRecipeMapPreservation` | 修改加工样板时保留来源配方池 | 两端 |
| `terminalInventoryFix` | 终端库存同步及 NEI 状态 | 客户端 |
| `terminalNeiAlwaysShowPresence` | NEI 网络材料状态与覆盖按钮提示 | 客户端；依赖库存修复及 NEE |
| `terminalSmartIngredients` | 收藏、电路和网络材料选择优先级 | 两端；依赖 `quickTerminal` |
| `gtnlQuickTerminalCraftFix` | GTNL NEI 请求合成的终端槽位修复 | 两端 |
| `matrixPatternLoadFix` | 装配矩阵读档样板失效修复 | 两端 |
| `bookmarkMachineGroups` | V 排序、Shift+V 按池分组 | 客户端 |
| `hideGtnlShimmerRecipes` | 隐藏 GTNL 微光嬗变 NEI 页面 | 客户端 |
| `hideGtnrDisassemblerRecipes` | 隐藏 GTNR 拆解机 NEI 页面 | 客户端 |
| `hologramImprovements` | 全息投影仪生存建造改善总开关 | 两端 |
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
| `threadedJointProxy.wirelessPower` | `true` | 多线程代理无能源仓时无线供电，无玻璃门槛 |
| `proxyFactory.wirelessPower` | `true` | 代理工厂 UEV+ 玻璃且无能源仓时无线供电 |
| `proxyFactory.locatorSlots` | `128` | 两种代理共用的定位器容量，范围 1–32730 |
| `proxyFactory.locatorNavigation` | `true` | 定位器空气右键高亮与潜行传送 |
| `proxyFactory.extremeCrafting` | `true` | TST 梦魇工作台转换加工样板桥接 |
| `collector.singularityResourceRecipes` | `true` | 维度／等级标记与资源配方 |
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
