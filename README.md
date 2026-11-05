# GTNH Adjust

面向 GT New Horizons 2.9.0 的个人调整模组。目前只有 Forge 模组入口，没有游戏内容或 Mixin。

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

Gradle 由 Java 25 启动；示例构建链会为 Minecraft 源码及其他辅助任务使用相应的旧版 Java 工具链，并可将其下载到上述项目内的 Gradle 缓存。当前模组使用示例的 Jabel 模式生成 Java 8 字节码。`elytra.manifest.version` 指定 2.9.0，当前空模组尚无需要从该清单解析的第三方模组依赖；添加依赖时应使用该版本清单。
