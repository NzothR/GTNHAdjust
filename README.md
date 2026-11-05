# GTNH Adjust

面向 GT New Horizons 2.9.0 的个人调整模组。

当前适配 GT-Never-Reset 0.1.29：大型作物杂交机的五级电压调整为 ULV 至 EV，杂交基础耗电为 8 EU/t；新入档及新产出的种子属性为 31/31/31。已有档案不主动修改，洗种功耗保持 512 EU/t。其他版本的目标字节码可能不同，升级后需要重新核对注入点。

针对 GTNL `dev-290` 的蒸汽温室，种子总容量默认为 64，最多仍为 4 个品种；机器按有效种子数计算产出，底土规则沿用 GTNL 原逻辑。界面可设置 0–4 次蒸汽超频，每次将耗能乘 4、运行时间减半。升级 GTNL 后应重新核对注入点。

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

`gtnl.steamGreenhouseMaxSeeds` 是服务端配置，范围 1–64，默认 64，重启后生效。界面的容量使用服务端同步值。降低上限前请先取出超出新上限的种子；否则 GTNL 会阻止温室运行，直到取出多余种子。
