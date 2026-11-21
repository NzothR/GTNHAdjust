# 工厂规划管理（Factory Planning Manager）

这是面向 GT New Horizons 2.9.0 的独立 Forge 模组。模组 ID 为 `factoryplanningmanager`，初始版本为 `0.1.0`。当前版本提供模组入口和构建骨架，具体玩法待讨论。

## 开发与构建

以 Minecraft 1.7.10 和 Forge 10.13.4.1614 为基线。子项目自带 Gradle Wrapper，可单独检出源码目录后构建。构建使用 Java 25，生成 Java 8 字节码。

在本目录执行以下 PowerShell 命令。按本机安装位置设置 `JAVA_HOME`。

```powershell
$env:JAVA_HOME = 'D:\CustomLib\zulu25.28.85-ca-jdk25.0.0-win_x64'
$env:GRADLE_USER_HOME = "$PWD\.gradle-user-home"
New-Item -ItemType Directory -Force .tmp | Out-Null
$env:TEMP = "$PWD\.tmp"
$env:TMP = $env:TEMP
.\gradlew.bat build -x test
```

正式 JAR 位于 `build/libs/factoryplanningmanager-0.1.0.jar`。维护分支为 `codex/factory-planning-manager`。
