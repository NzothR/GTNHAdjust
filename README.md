# GTNH Adjust

面向 GT New Horizons 2.9.0 的个人调整模组。

当前适配 GT-Never-Reset 0.1.29：大型作物杂交机的五级电压调整为 ULV 至 EV，杂交基础耗电为 8 EU/t；新入档及新产出的种子属性为 31/31/31。已有档案不主动修改，洗种功耗保持 512 EU/t。其他版本的目标字节码可能不同，升级后需要重新核对注入点。

针对 GTNL `dev-290` 的蒸汽温室，种子总容量默认为 64，最多仍为 4 个品种；机器按有效种子数计算产出，底土规则沿用 GTNL 原逻辑。升级 GTNL 后应重新核对注入点。

Forestry 木工机配方固定注册到 GT 普通组装机配方池。数据由本机 GTNH 2.9.0 服务端的 373 条不同配方快照，以及 GTNH Core 的 44 条农场部件配方组成；农场方块输入严格匹配 `FarmBlock` NBT。配方保留流体、包装箱、替代材料和服务端快照中的时间及 EU/t。静态检查未发现这批木工机配方之间的无序输入冲突，因此无需添加电路。原有组装机配方不做改动；如果它们与新增配方重叠，原有查找顺序可能影响实际结果。

配方数据随模组发布，不再读取或写入旧版本的 `GTNHAdjust-carpenter-recipes.dat` 和 `GTNHAdjust-carpenter-circuits.cfg`。新增其他模组不会自动追加木工机配方；若要更新，应重新采集并审核静态数据。输入物品或流体不可用时，启动日志会报告跳过的配方。可在本机 `general_env` 环境运行 `python tools/audit_carpenter_data.py` 检查快照内部的无序输入冲突。

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
gtsrDiagnostics=true
```

分别控制大型作物杂交机调整、蒸汽温室调整、木工机静态组装机配方、GT-Never-Reset 二合一无线终端调整。设为 `false` 时，对应 Mixin 不会被加入加载列表，配方功能则不会注册任何配方。配置文件无法读取时所有功能均关闭；个别值不是 `true` 或 `false` 时对应功能关闭，并在日志记录原因。此前写入的世界数据和物品不会自动迁移，关闭温室调整前应取出超过原容量的种子。

`gtsrDiagnostics` 控制 GTSteamReborn 2.1.9 的只读配方核查诊断注入；开启 `general.debug` 后记录 WS-1 的实际配方和不一致的物品、流体数组，供排查启动崩溃，不改变配方及校验结果。升级 GTSteamReborn 后如注入不兼容，可将该项设为 `false` 并重启。

终端调整以 GT-Never-Reset 0.1.29、NEI 2.8.171、AE2 rv3-beta-1095 为基准。客户端与服务端必须安装相同版本的本模组，并将 `quickTerminal` 设置为相同值：该功能扩展了终端配方转移的数据包。旧配置文件没有该项时默认为 `true`；要关闭时手动添加 `quickTerminal=false` 并重启两端。普通 `+` 传入配方，Shift/Alt + `+` 进一步自动编码；匹配到收藏配方时优先使用收藏的材料和产物，未匹配的槽位保留 NEI 当前材料。编码后只有一个产出槽位的样板才会尝试自动上传；失败或多产出时保留在终端编码槽。自动上传使用已同步的接口名称来确定候选，由服务端验证接口和空位并写入；如果客户端尚未收到接口列表，可能找不到本地化名称的目标。接口搜索仍遵循 GT-Never-Reset 的 `fillSearchFromNeiRecipes` 设置。终端的详细调试日志可在 `config/gtnhadjust.cfg` 中的 `quickTerminal.debug` 开启，默认关闭。

构建还需要将实例的 `NotEnoughItems-2.8.171-GTNH.jar` 和 `appliedenergistics2-rv3-beta-1095-GTNH.jar` 分别复制为 `ref/libs/nei-instance.jar` 和 `ref/libs/ae2-instance.jar`。

GTSteamReborn 诊断注入的编译还需要将 `gtsr-v2.1.9.jar` 复制为 `ref/libs/gtsr-instance.jar`。

`gtnl.steamGreenhouseMaxSeeds` 是服务端配置，范围 1–64，默认 64，重启后生效。界面的容量使用服务端同步值。降低上限前请先取出超出新上限的种子；否则 GTNL 会阻止温室运行，直到取出多余种子。
