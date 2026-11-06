# GTNH Adjust

面向 GT New Horizons 2.9.0 的个人调整模组。

当前适配 GT-Never-Reset 0.1.29：大型作物杂交机的五级电压调整为 ULV 至 EV，杂交基础耗电为 8 EU/t；新入档及新产出的种子属性为 31/31/31。已有档案不主动修改，洗种功耗保持 512 EU/t。其他版本的目标字节码可能不同，升级后需要重新核对注入点。

针对 GTNL `dev-290` 的蒸汽温室，种子总容量默认为 64，最多仍为 4 个品种；机器按有效种子数计算产出，底土规则沿用 GTNL 原逻辑。升级 GTNL 后应重新核对注入点。

Forestry 木工机配方固定注册到 GT 普通组装机配方池。数据由本机 GTNH 2.9.0 服务端的 373 条不同配方快照，以及 GTNH Core 的 44 条农场部件配方组成；农场方块输入严格匹配 `FarmBlock` NBT。配方保留流体、包装箱、替代材料和服务端快照中的时间及 EU/t。静态检查未发现这批木工机配方之间的无序输入冲突，因此无需添加电路。原有组装机配方不做改动；如果它们与新增配方重叠，原有查找顺序可能影响实际结果。

配方数据随模组发布，不再读取或写入旧版本的 `GTNHAdjust-carpenter-recipes.dat` 和 `GTNHAdjust-carpenter-circuits.cfg`。新增其他模组不会自动追加木工机配方；若要更新，应重新采集并审核静态数据。输入物品或流体不可用时，启动日志会报告跳过的配方。可在本机 `general_env` 环境运行 `python tools/audit_carpenter_data.py` 检查快照内部的无序输入冲突。

## 超级碎片世界采集器配方

采集器配方采用 GT-Not-Hard 源码提交 `e8b2133fe08e7182bfa0316bff43379c132218f7` 的维度资源列表，标记和电路均不消耗；基础耗时为 128 tick（6.4 秒，默认值），基础耗电为 T0 8 EU/t，维度每升一级耗电乘以 4，仅包含 T0–T9；T10 配方不再注册。20–24 号电路分别选择矿石、宝石、粉末、锭和流体；列表中相同产物去重后每种确定产出 1 个或 1000 L。NEI 按五种资源类型分标签，并以每页最多 90 种产物的展示配方分页；实际是一条配方并同时输出全部产物。同一维度的粉末、锭和流体会按已注册的材料对应关系补齐，缺少对应形式时跳过；矿石和宝石列表不参与补齐。旧版超级碎片世界采集配方不再注册。机器支持不同配方同时运行，同一配方最多占用一个线程。LV 起每个线程基础并行为 64，输入电压每升一级翻倍；输入电压按能源仓电压汇总，最高限制为最高等级能源仓的上一电压等级；支持多个能源仓和多安能源仓，总供电能力仍按实际能源仓计算。LV 电压与 HV 玻璃提供 8 个线程，电压和玻璃每升一级各增加 4 个线程；玻璃每升一级使基础耗能和基础耗时各减少 10 个百分点，最低均为 10%；低于 LV 不运行。线程总数上限为 256。配方时间、基础并行、基础线程、电压每级并行倍率和电压每级线程增加量可在 `config/gtnhadjust.cfg` 的 `collector` 项中配置，修改后重启并保持客户端与服务端一致。机器优先启动更多不同配方，按各配方的最低耗电及可用功率尽量平均分配线程预算；线程内部优先并行，再按 GT 规则进行无损超频。批处理开关对所有线程生效，已运行线程将在下一周期采用新设置。正在运行的线程完成当前周期后才会按新的线程数量重新分配功率。线程分别计算并行、无损超频、耗时和耗电，再汇总耗电。供电不足会停机并清空所有线程进度及缓存产物。

配置 `config/gtnhadjust.cfg` 的 `collector.singularityResourceRecipes` 默认为 `true`，修改后重启，客户端与服务端应一致。来源适配 GTNH 2.8.0，当前以 GTNH 2.9.0 为基准，缺失的维度标记会跳过相应配方，缺失的物品或流体会跳过该产物，启动日志包含各配方的缺失数。开启 `general.debug` 可记录缺失项明细以及采集器线程的启动、耗电、进度和停机诊断；存在缺失项时还会导出 `config/gtnhadjust/singularity-registries.json`，包含已注册流体、物品及粉末、锭、宝石和矿石相关矿辞物品与 meta。注册名存在不代表所有 meta 的语义都已核实，需游戏内检查。

每个等级另有一个金色 T0–T9 维度标记，由该等级的全部维度方块在组装机合成（无序输入，128 tick，512 EU/t）。对应采集配方按资源类型合并该等级的单维度产物，先去除同等级重复，再排除所有低等级已出现的同类型产物；物品按注册名和 meta、流体按注册名去重，不累加产量。没有新产物的类型不注册配方。若缺少维度标记，对应等级的合成配方不会注册；缺失的维度及产物不会参与整合和跨等级排重，详见启动日志。该功能随 `collector.singularityResourceRecipes` 一同启停，重启生效，客户端与服务端配置须一致。

### 采集器结构

采集器使用交错星门结构，尺寸为 28×28×25（宽×深×高）。控制器嵌入正面控制台；洁净不锈钢机械方块位置可替换为输入总线、输出总线、输出仓、能源仓或维护仓。等级玻璃必须同等级，中心 5×5×5 保持空气。成型后客户端显示 GT 黑洞动画，不替换或删除世界方块。`collector.blackHoleRendering` 默认开启，由服务端控制，重启生效；开启 `general.debug` 可记录动画显示切换。旧的三种结构测试控制器已移除。

## 代码分类

- `com.gtnhadjust.mixins`：针对其他模组的 Mixin 注入，按目标功能和修复分类。
- `com.gtnhadjust.machine`：本模组新增的机器；`collector` 和 `quantum` 分别包含对应机器、结构及配方。
- `com.gtnhadjust.recipe`：独立于新增机器的配方调整与配方兼容逻辑。
- `com.gtnhadjust.terminal`：二合一终端 Mixin 使用的辅助逻辑。
- 根包负责模组入口、配置及 Mixin 加载；`tools` 存放开发用的数据生成和校验脚本。

项目仍构建为单个模组，机器注册名、数值 ID、配方池名称及资源路径保持原有值。

## 构建

使用 Java 25。PowerShell 示例（将 Gradle 缓存和临时文件保留在项目目录）：

```powershell
$env:JAVA_HOME = 'D:\CustomLib\zulu25.28.85-ca-jdk25.0.0-win_x64'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:GRADLE_USER_HOME = "$PWD\.gradle-user-home"
New-Item -ItemType Directory -Force .tmp | Out-Null
$env:TEMP = "$PWD\.tmp"
$env:TMP = $env:TEMP
.\gradlew.bat build
```

构建产物位于 `build/libs/`。开发约定和本机环境见 [AGENTS.md](AGENTS.md)；参考仓库位于不提交的 `ref/`。

Gradle 由 Java 25 启动；示例构建链会为 Minecraft 源码及其他辅助任务使用相应的旧版 Java 工具链，并可将其下载到上述项目内的 Gradle 缓存。当前模组使用示例的 Jabel 模式生成 Java 8 字节码。CropsNH 和 GregTech 依赖通过 GTNH 2.9.0 清单解析。构建还需要将实例中的 `gtneverreset-0.1.29.jar` 复制到不提交的 `ref/libs/`；已验证文件的 SHA-256 为 `bbbf4e04a0dad9bd9a4639c7029e305b38788f13daaf9bb11325e6454821d213`。

同样将实例中的 `sciencenotleisure-dev-290-fde37b9871fd-f93edbe8c425.jar` 复制为 `ref/libs/gtnl-instance.jar`；已验证文件的 SHA-256 为 `977335c74149afdbf280f15506fb59bd1b97e126acb3807be770d0e0b3374840`。

将实例中的 `modularui2-2.3.94-1.7.10.jar` 复制为 `ref/libs/modularui2-instance.jar`；已验证文件的 SHA-256 为 `5fee68e74b5d20eb5b2e8a5de668c74a1975bd0f8a6f463ed2be98844803dfdc`。

游戏首次加载后可在 `config/gtnhadjust.cfg` 的 `general.debug` 开启调试日志，默认关闭。

### 功能开关

首次启动时会生成 `config/gtnhadjust-features.properties`。修改后重启游戏或服务端；客户端和服务端应保持一致：

```properties
cropBreeder=true
steamGreenhouse=true
carpenterRecipes=true
quickTerminal=true
terminalInventoryFix=true
smallQuantumMachine=true
gtsrDiagnostics=true
gtsrRecipeSpeedCompatibility=true
patternDeduplication=true
```

分别控制大型作物杂交机调整、蒸汽温室调整、木工机静态组装机配方、GT-Never-Reset 二合一无线终端调整。设为 `false` 时，对应 Mixin 不会被加入加载列表，配方功能则不会注册任何配方。配置文件无法读取时所有功能均关闭；个别值不是 `true` 或 `false` 时对应功能关闭，并在日志记录原因。此前写入的世界数据和物品不会自动迁移，关闭温室调整前应取出超过原容量的种子。

`gtsrDiagnostics` 控制 GTSteamReborn 2.1.9 的只读配方核查诊断注入；开启 `general.debug` 后记录 WS-1 的实际配方和不一致的物品、流体数组，供排查启动崩溃，不改变配方及校验结果。升级 GTSteamReborn 后如注入不兼容，可将该项设为 `false` 并重启。

`gtsrRecipeSpeedCompatibility` 修复 GTSteamReborn 2.1.9 的配方自检与 GT-Never-Reset 全局配方加速之间的冲突：GTSR 自己的配方注册回调中创建的 GT 配方，以及 `gtsr.recipe.` 命名空间的独有配方池，都保留注册时的原始耗时；回调结束后其他模组在通用配方池中注册的配方仍使用全局加速。自检保持不变，客户端和服务端应使用一致开关。

`patternDeduplication` 单独控制 GT-Never-Reset 二合一无线终端的接口去重：手动放入或自动上传时，如果目标接口已有产物种类集合相同的样板，则拒绝写入。产物的物品或流体类型及 NBT 参与比较；数量、顺序和输入材料不参与。已有样板不会被删除，其他终端和总成本身的插入规则不变。自动上传的成功或阻断原因会写入 INFO 日志、玩家聊天栏及屏幕下方提示，不依赖调试日志开关。服务端与客户端需要安装同版本并保持终端与去重开关一致。

终端调整以 GT-Never-Reset 0.1.29、NEI 2.8.171、AE2 rv3-beta-1095 为基准。客户端与服务端必须安装相同版本的本模组，并将 `quickTerminal` 设置为相同值：该功能扩展了终端配方转移的数据包。旧配置文件没有该项时默认为 `true`；要关闭时手动添加 `quickTerminal=false` 并重启两端。普通 `+` 传入配方，Shift/Alt + `+` 进一步自动编码；匹配到收藏配方时优先使用收藏的材料和产物，未匹配的槽位保留 NEI 当前材料。编码后只有一个产出槽位的样板才会尝试自动上传；失败或多产出时保留在终端编码槽。自动上传使用已同步的接口名称来确定候选，由服务端验证接口和空位并写入；如果客户端尚未收到接口列表，可能找不到本地化名称的目标。接口搜索仍遵循 GT-Never-Reset 的 `fillSearchFromNeiRecipes` 设置。终端的详细调试日志可在 `config/gtnhadjust.cfg` 中的 `quickTerminal.debug` 开启，默认关闭。

`terminalInventoryFix` 独立控制二合一终端的客户端库存同步及 NEI 状态修复，基于 AE2 rv3-beta-1095、NEI 2.8.171 和 NotEnoughEnergistics 1.7.50。旧配置缺省启用，添加 `terminalInventoryFix=false` 并重启客户端可彻底停用相关 Mixin；服务端无需此修复。库存状态指示复用 NotEnoughEnergistics，未安装该模组时只启用库存数据包转发。

`smallQuantumMachine` 控制小依的小型量子操纵者及其配方，客户端和服务端需一致，修改后重启。该机器按 GT5 5.09.54.265 原版 15×21×15 结构建造，使用坚固钢制机械方块、格栅机械方块、洁净不锈钢机械方块、等级线圈和等级玻璃；顶部或底部可放舱室。必须安装维护仓、能源仓、消声仓和输出总线，流体输出与输入舱室按配方需要选装；可使用多个普通或异能能源仓。原始污染 102400/秒，由消声仓减排。

支持批处理和无损超频；硼玻璃基础并行为 32，玻璃每升一级增加 32 基础并行，LV 起每升一级电压并行翻倍。白铜线圈使用 100% 配方耗时，线圈每升一级减少 10 个百分点，最低为 10%。

该机器配方是基于 GT5、Crops NH、GTNL、GTRH、Twist Space Technology 和 123Technology 的固定快照，不在游戏启动时扫描其他配方池；不包含原始输入或产物已有 1 份的配方。27 条快照中，GTRH 的两条水处理配方分别使用不消耗的 1 号和 2 号电路。快照以来源模组的原始配方耗时为起点，每次降压耗时乘以 4；再乘以 `config/gtnhadjust.cfg` 中 `quantum.recipeTimeMultiplier`，默认为 0.1，修改后重启并保持客户端和服务端一致。结果向上取整且至少为 1 tick，之后再计算线圈和超频；GT-Never-Reset 的全局加速不会重复作用。每份物品或流体产物单独按 `10000 / 产物种数` 概率判定，精度为万分之一。控制器的组装机配方指定 1024 秒，不受该倍率影响。

机器使用 `config/gtnhadjust.cfg` 的 `quantum.smallQuantumMteId`，默认 `31991`。客户端和服务端必须一致，取得或放置机器之后不要改动；关闭功能前应先移除现有机器。开启 `general.debug` 可记录配方处理等诊断信息。

构建还需要将实例的 `NotEnoughItems-2.8.171-GTNH.jar` 和 `appliedenergistics2-rv3-beta-1095-GTNH.jar` 分别复制为 `ref/libs/nei-instance.jar` 和 `ref/libs/ae2-instance.jar`。

GTSteamReborn 诊断注入的编译还需要将 `gtsr-v2.1.9.jar` 复制为 `ref/libs/gtsr-instance.jar`。

`gtnl.steamGreenhouseMaxSeeds` 是服务端配置，范围 1–64，默认 64，重启后生效。界面的容量使用服务端同步值。降低上限前请先取出超出新上限的种子；否则 GTNL 会阻止温室运行，直到取出多余种子。
