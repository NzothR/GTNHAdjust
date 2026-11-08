# GTNH Adjust

面向 GT New Horizons 2.9.0 的个人调整模组。

仓库另有独立的 [Future Resource Protocol 子项目](frp/README.md)，使用单独的模组和构建，不依赖 GTNH Adjust；目前提供首轮测试版便携终端及世界账本，使用范围和限制见子项目文档。

## 代理联合工厂（基础版）

代理联合工厂可由一个 GTNR 通用联合工厂控制器在工作台无序合成。结构、仓室要求、参数模块、线程、供电、超频以及样板配方池和产物匹配沿用 GT-Never-Reset 0.1.34。控制器正面纹理右下角显示“依”。机器界面可打开已解锁配方池分页列表，名称随客户端语言显示，并随控制器的输入和退回更新。

初始可用 GTNR 原有 24 个配方池。界面右侧新增操作模式按钮，悬停可查看模式；也可潜行时用螺丝刀右键控制器，按运行、输入、输出的顺序切换。机器必须成型且已启用工作，输入模式每秒检查普通实体输入总线：只在控制器能增加尚未解锁的白名单配方池时收取 1 个，并保存其物品和 NBT；已全部解锁的控制器留在总线中。每台代理联合工厂最多保存 256 个控制器。ME 输入总线和 ME 合成输入总线不参与控制器输入，物品配方输入仍按 GTNR 原逻辑处理。

输出模式每秒从最后保存的控制器开始向输出总线退回一个，只有完整插入成功才移除并重新计算解锁；输出空间不足时保留控制器，强制保护控制器不受虚空模式影响。退回后只有该控制器提供的额外配方池会锁回，其他保存的控制器仍提供的池及原有 24 池保持可用。破坏代理联合工厂时，内部保存的控制器作为物品掉落；区块卸载和重新读档保留记录。输入、输出模式不启动新配方，已运行线程仍按 GTNR 规则完成。配方池通过原有模式按钮选择；跨配方总成可以按样板记录的已解锁配方池执行。

白名单以配方池注册名筛选，包含原有 24 个池和扩展候选，详见 [配方池范围](docs/proxy-factory-recipe-scope.md)。控制器只是解锁凭据，实际仍由代理联合工厂运行，不复现原机器的结构等级、温度、特殊世界环境或专属加工逻辑；动态后端、特殊物品和实际运行结果需要游戏内验证。GT 自身配方匹配、特殊槽、禁用或展示配方过滤及 GTNR 的样板产物与配方锁校验仍生效。

`config/gtnhadjust-features.properties` 的 `proxyFactory` 默认开启，关闭时不注册机器和合成配方。`config/gtnhadjust.cfg` 的 `proxyFactory.mteId` 默认 31993。适配 GTNH 2.9.0 和 GTNR 0.1.34，开关或 ID 修改后须重启，客户端与服务端须安装相同版本并保持配置一致。

以下作物调整最初基于 GT-Never-Reset 0.1.32，当前编译参考为 0.1.34，游戏内兼容性仍需验证：大型作物杂交机的五级电压调整为 ULV 至 EV，杂交基础耗电为 8 EU/t；新入档及新产出的种子属性为 31/31/31。已有档案不主动修改，洗种功耗保持 512 EU/t。其他版本的目标字节码可能不同，升级后需要重新核对注入点。

针对 GTNL `dev-290` 的蒸汽温室，种子总容量默认为 64，最多仍为 4 个品种；机器按有效种子数计算产出，底土规则沿用 GTNL 原逻辑。升级 GTNL 后应重新核对注入点。

Forestry 木工机配方固定注册到 GT 普通组装机配方池。数据由本机 GTNH 2.9.0 服务端的 373 条不同配方快照，以及 GTNH Core 的 44 条农场部件配方组成；农场方块输入严格匹配 `FarmBlock` NBT。配方保留流体、包装箱、替代材料和服务端快照中的时间及 EU/t。静态检查未发现这批木工机配方之间的无序输入冲突，因此无需添加电路。原有组装机配方不做改动；如果它们与新增配方重叠，原有查找顺序可能影响实际结果。

配方数据随模组发布，不再读取或写入旧版本的 `GTNHAdjust-carpenter-recipes.dat` 和 `GTNHAdjust-carpenter-circuits.cfg`。新增其他模组不会自动追加木工机配方；若要更新，应重新采集并审核静态数据。输入物品或流体不可用时，启动日志会报告跳过的配方。可在本机 `general_env` 环境运行 `python tools/audit_carpenter_data.py` 检查快照内部的无序输入冲突。

## 战利品模拟室

战利品模拟室使用 3×3×3 实心结构：控制器在正面中央，正中心为一块加热线圈，其余为洁净不锈钢机械方块，可替换为输入总线、输出总线、维护仓和输出仓。至少需要一个输入总线、一个输出总线和一个维护仓；输出仓可选，不使用能源仓，也不耗电。工业屠宰场控制器可在工作台无序合成为模拟室控制器。

运行时每 128 tick（6.4 秒）扫描普通及 ME 输入总线，不消耗输入；ME 合成输入总线不参与扫描。支持 Ender IO 2.10.51 的有生物灵魂瓶、破损刷怪笼、电动刷怪笼以及原版注册的刷怪蛋。以实体注册名合并不同输入，同一实体每周期最多计算 64 个，超出的不计入掉落和经验；不同实体分别计数。模拟室使用 GTNH 2.9.0、MobsInfo 0.6.2 中处理过配置与额外掉落的生物战利品表，不受工业屠宰场配方表的灵魂瓶限制影响，可读取其中的凋灵等 Boss 掉落；不使用时运，也不生成实际生物。概率计算沿用工业屠宰场和 123Technology 噬魂监狱对玩家专属掉落的倍率（KubaTech `PlayerOnlyDropsModifier`，默认 0.1），因此这些掉落可能比 Mob Info 界面显示的基础概率少。随机耐久、随机附魔及有耐久的物品、原版武器、工具和护甲会被过滤。没有耐久且不属于上述类别的特殊装备无法仅凭类型识别。每个计入的生物提供 120 L 液态经验（xpjuice），白铜线圈产物为 1 倍，此后线圈每升一级，物品与经验均翻倍。掉落按输入数量独立随机判定，然后乘线圈倍率。产物在周期结束时输出，放不下的部分销毁；没有输出仓时经验销毁。没有 MobsInfo 战利品数据但有有效实体注册名的输入仅产生经验；无法识别的输入会跳过。模拟室不实现屠宰场的精英怪和仪式模式，第三方专用刷怪蛋格式不保证支持。

`config/gtnhadjust-features.properties` 的 `lootSimulator` 默认开启；`config/gtnhadjust.cfg` 的 `lootSimulator.mteId` 默认 31992，取得或放置控制器后不要更改。修改开关或 ID 后重启，客户端和服务端应保持一致；关闭前应先移除现有控制器。`general.debug` 可记录周期目标数和缺少 MobsInfo 数据的实体。工业屠宰场的装备销毁和固定时运 Mixin 已移除，旧配置中的 `crusherEquipmentVoid`、`crusherFixedLooting` 不再生效，原机器恢复自身逻辑。

## 超级碎片世界采集器配方

采集器配方采用 GT-Not-Hard 源码提交 `e8b2133fe08e7182bfa0316bff43379c132218f7` 的维度资源列表，标记和电路均不消耗；基础耗时为 128 tick（6.4 秒，默认值），基础耗电为 T0 8 EU/t，维度每升一级耗电乘以 4，仅包含 T0–T9；T10 配方不再注册。20–24 号电路分别选择矿石、宝石、粉末、锭和流体；列表中相同产物去重后每种确定产出 1 个或 1000 L。NEI 按五种资源类型分标签，并以每页最多 90 种产物的展示配方分页；实际是一条配方并同时输出全部产物。同一维度的粉末、锭和流体会按已注册的材料对应关系补齐，缺少对应形式时跳过；矿石和宝石列表不参与补齐。旧版超级碎片世界采集配方不再注册。机器支持不同配方同时运行，同一配方最多占用一个线程。LV 起每个线程基础并行为 64，输入电压每升一级翻倍；输入电压按能源仓电压汇总，最高限制为最高等级能源仓的上一电压等级；支持多个能源仓和多安能源仓，总供电能力仍按实际能源仓计算。LV 电压与 HV 玻璃提供 8 个线程，电压和玻璃每升一级各增加 4 个线程；玻璃每升一级使基础耗能和基础耗时各减少 10 个百分点，最低均为 10%；低于 LV 不运行。线程总数上限为 256。配方时间、基础并行、基础线程、电压每级并行倍率和电压每级线程增加量可在 `config/gtnhadjust.cfg` 的 `collector` 项中配置，修改后重启并保持客户端与服务端一致。机器优先启动更多不同配方，按各配方的最低耗电及可用功率尽量平均分配线程预算；线程内部优先并行，再按 GT 规则进行无损超频。批处理开关对所有线程生效，已运行线程将在下一周期采用新设置。正在运行的线程完成当前周期后才会按新的线程数量重新分配功率。线程分别计算并行、无损超频、耗时和耗电，再汇总耗电。供电不足会停机并清空所有线程进度及缓存产物。

配置 `config/gtnhadjust.cfg` 的 `collector.singularityResourceRecipes` 默认为 `true`，修改后重启，客户端与服务端应一致。来源适配 GTNH 2.8.0，当前以 GTNH 2.9.0 为基准，缺失的维度标记会跳过相应配方，缺失的物品或流体会跳过该产物，启动日志包含各配方的缺失数。开启 `general.debug` 可记录缺失项明细以及采集器线程的启动、耗电、进度、启动失败和停机诊断；存在缺失项时还会导出 `config/gtnhadjust/singularity-registries.json`，包含已注册流体、物品及粉末、锭、宝石和矿石相关矿辞物品与 meta。注册名存在不代表所有 meta 的语义都已核实，需游戏内检查。

每个等级另有一个金色 T0–T9 维度标记，由该等级的全部维度方块在组装机合成（无序输入，128 tick，512 EU/t）。对应采集配方按资源类型合并该等级的单维度产物，先去除同等级重复，再排除所有低等级已出现的同类型产物；物品按注册名和 meta、流体按注册名去重，不累加产量。没有新产物的类型不注册配方。若缺少维度标记，对应等级的合成配方不会注册；缺失的维度及产物不会参与整合和跨等级排重，详见启动日志。该功能随 `collector.singularityResourceRecipes` 一同启停，重启生效，客户端与服务端配置须一致。

每种已注册的 T0–T9 单维度方块还可使用 9 个同种方块，在压缩机配方池中压缩为 1 个对应等级标记。例如 9 个主世界方块或 9 个下界方块均产出 T0 标记。配方使用 GTNH 2.9.0 的稳定黑洞条件，只能在具有活跃黑洞的黑洞压缩机中运行；基础耗时 128 tick（6.4 秒），基础耗电 512 EU/t，不受 GTNR 全局配方加速影响，实际加工仍受机器加速、耗电和并行规则影响。缺少的维度方块会跳过对应配方并在启动日志报告。该功能随 `collector.singularityResourceRecipes` 一同启停，修改后重启，客户端与服务端配置须一致；既有组装机配方保留。

采集器识别本模组的等级维度标记；标记使用对应维度的外观渲染。该行为适配 GTNH 2.9.0，随上述配方功能使用，更新模组后需重启客户端和服务端。

### 采集器结构

采集器使用交错星门结构，尺寸为 28×28×25（宽×深×高）。控制器嵌入正面控制台；洁净不锈钢机械方块位置可替换为输入总线、输出总线、输出仓、能源仓或维护仓。等级玻璃必须同等级，中心 5×5×5 保持空气。成型后客户端显示 GT 黑洞动画，不替换或删除世界方块。`collector.blackHoleRendering` 默认开启，由服务端控制，重启生效；开启 `general.debug` 可记录动画显示切换。旧的三种结构测试控制器已移除。

## 文档维护

新增或扩展影响玩法的 Mixin、独立功能、机器、结构、配方和配置时，应在同一次修改中更新本 README，说明功能行为、开关默认值、兼容版本、重启要求以及必要的客户端和服务端一致性要求。删除或更改功能时同步修正对应说明。

## 代码分类

- `com.gtnhadjust.mixins`：针对其他模组的 Mixin 注入，按目标功能和修复分类。
- `com.gtnhadjust.machine`：本模组新增的机器；`collector` 和 `loot` 分别包含对应机器、结构及配方。
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

Gradle 由 Java 25 启动；示例构建链会为 Minecraft 源码及其他辅助任务使用相应的旧版 Java 工具链，并可将其下载到上述项目内的 Gradle 缓存。当前模组使用示例的 Jabel 模式生成 Java 8 字节码。CropsNH 和 GregTech 依赖通过 GTNH 2.9.0 清单解析。构建还需要将 服务端使用的 GT-Never-Reset 0.1.34 包保存为不提交的 `ref/libs/gtneverreset-0.1.34.jar`；该文件的 SHA-256 为 `56b44a22b7acb767f74a04a5441aa6ac3a50afd57547688adb365526805e8d75`。

同样将实例中的 `sciencenotleisure-dev-290-fde37b9871fd-f93edbe8c425.jar` 复制为 `ref/libs/gtnl-instance.jar`；已验证文件的 SHA-256 为 `977335c74149afdbf280f15506fb59bd1b97e126acb3807be770d0e0b3374840`。

将实例中的 `modularui2-2.3.94-1.7.10.jar` 复制为 `ref/libs/modularui2-instance.jar`；已验证文件的 SHA-256 为 `5fee68e74b5d20eb5b2e8a5de668c74a1975bd0f8a6f463ed2be98844803dfdc`。

游戏首次加载后可在 `config/gtnhadjust.cfg` 的 `general.debug` 开启调试日志，默认关闭。

### 功能开关

首次启动时会生成 `config/gtnhadjust-features.properties`。修改后重启游戏或服务端；客户端和服务端应保持一致：

```properties
proxyFactory=true
cropBreeder=true
steamGreenhouse=true
carpenterRecipes=true
quickTerminal=true
terminalInventoryFix=true
gtsrDiagnostics=true
gtsrRecipeSpeedCompatibility=true
patternDeduplication=true
terminalCraftingRefreshFix=true
patternRecipeMapPreservation=true
terminalNeiAlwaysShowPresence=true
terminalSmartIngredients=true
neoEcoCraftingRefreshFix=false
craftingDiagnostics=false
matrixPatternDiagnostics=false
matrixPatternLoadFix=true
soulVialExpandedCapture=true
lootSimulator=true
```

`soulVialExpandedCapture` 扩展 GTNH 2.9.0 的 Ender IO 2.10.51 灵魂瓶：允许捕获原本因 Boss 判定或 Ender IO 灵魂瓶黑名单而被拒绝的生物，并支持右键末影龙等龙型多部位实体的部位。沿用灵魂瓶原有的 NBT 保存和释放方式；玩家、死亡实体以及缺少可还原注册名的实体不支持，其他模组仍可能阻止交互或移除。默认开启；设为 `false` 后不加载对应 Mixin，也不转发多部位交互。客户端和服务端应安装同版本并保持开关一致，修改后重启；开启 `general.debug` 可记录多部位捕获。收容或释放 Boss 可能影响其维度、战斗和世界事件，建议先在测试存档验证。

`craftingDiagnostics` 默认关闭，设为 `true` 并重启后记录工厂输入与匹配、总成更新、AE 合成缓存事件、存储通知及客户端数据包；日志可能很多，排查完成后应关闭并重启。

`matrixPatternLoadFix` 适配 GT-Never-Reset 0.1.32 装配矩阵：解析 AE2 编码样板时使用副本，避免读档初期暂时无法匹配合成配方时将原样板永久标记为无效，并在机器运行第 20 tick 重建样板缓存。第 20 tick 时也会在副本上重新验证已失效的普通 AE2 合成样板：仅当配方恢复且实际产物与样板保存的物品、NBT 和数量完全一致时，才移除原样板的无效标记；其余样板保持不变。默认开启；修改后重启客户端和服务端并保持配置一致，关闭时不加载该功能的 Mixin。

`matrixPatternDiagnostics` 默认关闭，仅用于排查 GT-Never-Reset 0.1.32 装配矩阵在读档后出现失效合成样板的问题；开启后在服务端日志输出 `[matrix-audit]`，记录读档阶段无效标记变化和第 20 tick 时的样板副本重试结果。诊断不清除原样板标记、不修复存档；修改开关后重启，客户端与服务端保持一致。问题复现并收集日志后请关闭此开关。

`terminalCraftingRefreshFix` 尝试修复 GT-Never-Reset 0.1.32 二合一终端操作样板后合成状态延迟更新：样板实际变化后，在后续容器更新时再次通知目标接口的合成网络并同步相关产物。该开关独立于 `quickTerminal`，客户端和服务端保持一致，修改后重启；关闭后不会加载对应 Mixin。详细诊断可使用 `quickTerminal.debug`。

`neoEcoCraftingRefreshFix` 针对 NeoECOAE 7.3.7 的 AE 存储列表增量更新：合成状态变化的数量为零时回退到 AE 的完整缓存刷新，避免可合成标志滞后。默认关闭；需要使用时设为 `true`，客户端和服务端使用一致配置，修改后重启。关闭时不加载该 Mixin。

分别控制大型作物杂交机调整、蒸汽温室调整、木工机静态组装机配方、GT-Never-Reset 二合一无线终端调整。设为 `false` 时，对应 Mixin 不会被加入加载列表，配方功能则不会注册任何配方。配置文件无法读取时所有功能均关闭；个别值不是 `true` 或 `false` 时对应功能关闭，并在日志记录原因。此前写入的世界数据和物品不会自动迁移，关闭温室调整前应取出超过原容量的种子。

`gtsrDiagnostics` 控制 GTSteamReborn 2.1.9 的只读配方核查诊断注入；开启 `general.debug` 后记录 WS-1 的实际配方和不一致的物品、流体数组，供排查启动崩溃，不改变配方及校验结果。升级 GTSteamReborn 后如注入不兼容，可将该项设为 `false` 并重启。

`gtsrRecipeSpeedCompatibility` 修复 GTSteamReborn 2.1.9 的配方自检与 GT-Never-Reset 全局配方加速之间的冲突：GTSR 自己的配方注册回调中创建的 GT 配方，以及 `gtsr.recipe.` 命名空间的独有配方池，都保留注册时的原始耗时；回调结束后其他模组在通用配方池中注册的配方仍使用全局加速。自检保持不变，客户端和服务端应使用一致开关。

`patternDeduplication` 单独控制 GT-Never-Reset 二合一无线终端的接口去重：手动放入或自动上传时，如果目标接口已有产物种类集合相同的样板，则拒绝写入。产物的物品或流体类型及 NBT 参与比较；数量、顺序和输入材料不参与。已有样板不会被删除，其他终端和总成本身的插入规则不变。自动上传的成功或阻断原因会写入 INFO 日志和玩家聊天栏，并在二合一终端或其 NEI 界面的下方前景提示 5 秒；成功提示显示接口列表中的本地化名称及样板槽；成功的“样板终端”前缀为绿色，失败或阻断为红色，正文为白色，不依赖调试日志开关。服务端与客户端需要安装同版本并保持终端与去重开关一致。

GT-Never-Reset 0.1.32 已在 NEI 导入时将切割机展示配方池映射到真实配方池，本模组不再注入旧的 `factoryRecipeMapFix`。此前已编码且记录了展示配方池的旧样板不会自动迁移，遇到通用联合工厂无法匹配时需重新从 NEI 导入并编码；其他配方池不受该撤除影响。

终端调整以 GT-Never-Reset 0.1.32、NEI 2.8.171、AE2 rv3-beta-1095 为基准。客户端与服务端必须安装相同版本的本模组，并将 `quickTerminal` 设置为相同值：该功能扩展了终端配方转移的数据包。旧配置文件没有该项时默认为 `true`；要关闭时手动添加 `quickTerminal=false` 并重启两端。普通 `+` 传入配方，Shift/Alt + `+` 进一步自动编码；匹配到完整收藏配方时优先使用收藏的材料和产物；其他多候选输入按下述 `terminalSmartIngredients` 规则选择，关闭该功能时保留既有选择行为。编码后只有一个产出槽位的样板才会尝试自动上传；失败或多产出时保留在终端编码槽。自动上传使用已同步的接口名称来确定候选，由服务端验证接口和空位并写入；如果客户端尚未收到接口列表，可能找不到本地化名称的目标。接口搜索仍遵循 GT-Never-Reset 的 `fillSearchFromNeiRecipes` 设置。终端的详细调试日志可在 `config/gtnhadjust.cfg` 中的 `quickTerminal.debug` 开启，默认关闭。

`terminalInventoryFix` 独立控制二合一终端的客户端库存同步及 NEI 状态修复，基于 AE2 rv3-beta-1095、NEI 2.8.171 和 NotEnoughEnergistics 1.7.50。旧配置缺省启用，添加 `terminalInventoryFix=false` 并重启客户端可彻底停用相关 Mixin；服务端无需此修复。库存状态指示复用 NotEnoughEnergistics，未安装该模组时只启用库存数据包转发。

GTNR 快速编码终端配方池保留修复：修改并重新编码已有加工样板时，在原样板与终端记录的来源一致时保留配方池；NEI 导入仍以新配方为准。`config/gtnhadjust-features.properties` 中 `patternRecipeMapPreservation` 默认开启，关闭后不加载此 Mixin；修改须重启并保持客户端与服务端一致。适配 GTNH 2.9.0 与 GT-Never-Reset 0.1.32；`general.debug` 开启时记录保留操作。

二合一无线终端打开的 NEI 配方界面常显材料的网络状态图标，并在可合成的输入左上角显示 AE2 样板标记。`config/gtnhadjust-features.properties` 中 `terminalNeiAlwaysShowPresence` 默认开启，关闭后不加载对应 Mixin；依赖 `terminalInventoryFix`、NEI 2.8.171、NotEnoughEnergistics 和 GT-Never-Reset 0.1.32，仅客户端显示，配置变更后重启。

二合一终端从 NEI 导入时，优先使用匹配的完整收藏配方；否则对可选输入依次优先选择收藏的普通物品或收藏配方产物、对应等级的任意电路板、网络可合成项、网络已有项，同优先级从候选末尾向前选；无匹配时保留 NEI 当前项。分组配方会选择完整成员以保留对应的流体用量。`terminalSmartIngredients` 默认开启，依赖 `quickTerminal`，关闭后不加载对应 Mixin，重启生效；适配 GTNH 2.9.0、GTNR 0.1.32 和 NEI 2.8.171。


小型量子操纵者及其专属配方已移除。旧的 `smallQuantumMachine` 和 `quantum` 配置不再生效；客户端与服务端需同时更新并重启。

构建还需要将实例的 `NotEnoughItems-2.8.171-GTNH.jar`、`appliedenergistics2-rv3-beta-1095-GTNH.jar` 和 NotEnoughEnergistics 1.7.50 分别复制为 `ref/libs/nei-instance.jar`、`ref/libs/ae2-instance.jar` 和 `ref/libs/nee-instance.jar`。

GTSteamReborn 诊断注入的编译还需要将 `gtsr-v2.1.9.jar` 复制为 `ref/libs/gtsr-instance.jar`。

`gtnl.steamGreenhouseMaxSeeds` 是服务端配置，范围 1–64，默认 64，重启后生效。界面的容量使用服务端同步值。降低上限前请先取出超出新上限的种子；否则 GTNL 会阻止温室运行，直到取出多余种子。
