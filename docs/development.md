# 开发与构建

基线：GTNH 2.9.0、GTNR 0.1.47、GTNRLib 0.1.35；开发使用 Java 25。

## 项目结构

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
.\gradlew.bat build -x test
```

构建产物位于 `build/libs/`。NEI 提示及收藏配方编译还需将客户端的 `CodeChickenCore-1.4.22.jar` 和 `ae2fc-1.5.116-gtnh.jar` 分别保存为 `ref/libs/codechickencore-instance.jar` 和 `ref/libs/ae2fc-instance.jar`。YSMU 兼容编译还需将客户端的 `gtnrlib-0.1.35.jar` 和 `ysmu-1.9.jar` 复制到 `ref/libs/` 对应文件名；它们仅供编译，不打包进模组。开发约定和本机环境见 [AGENTS.md](../AGENTS.md)；参考仓库位于不提交的 `ref/`。

Gradle 由 Java 25 启动；示例构建链会为 Minecraft 源码及其他辅助任务使用相应的旧版 Java 工具链，并可将其下载到上述项目内的 Gradle 缓存。当前模组使用示例的 Jabel 模式生成 Java 8 字节码。CropsNH、GregTech 和 Waila 编译依赖通过 GTNH 2.9.0 清单解析。构建还需要将 GT-Never-Reset 0.1.47 正式包保存为不提交的 `ref/libs/gtneverreset-0.1.47.jar`。

同样将实例中的 `sciencenotleisure-dev-290-fde37b9871fd-f93edbe8c425.jar` 复制为 `ref/libs/gtnl-instance.jar`；已验证文件的 SHA-256 为 `977335c74149afdbf280f15506fb59bd1b97e126acb3807be770d0e0b3374840`。

将实例中的 `modularui2-2.3.94-1.7.10.jar` 复制为 `ref/libs/modularui2-instance.jar`；已验证文件的 SHA-256 为 `5fee68e74b5d20eb5b2e8a5de668c74a1975bd0f8a6f463ed2be98844803dfdc`。

游戏首次加载后可在 `config/gtnhadjust.cfg` 的 `general.debug` 开启调试日志，默认关闭。


## 补充本地编译依赖

`dependencies.gradle` 中的 `ref/libs/` 依赖需从对应游戏实例准备，仅供编译，不打包进本模组。包括 NEI 2.8.171、AE2 rv3-beta-1095、NotEnoughEnergistics 1.7.50、GTSteamReborn 2.1.9、NECH 等，完整文件名以该 Gradle 文件为准。参考源码保存在不提交的 `ref/`。

新增玩法功能及配置时同步维护 README 或对应使用说明；历史变化记录到 [history.md](history.md)。运行时验证由游戏端完成，构建和静态检查不代表游戏测试通过。
