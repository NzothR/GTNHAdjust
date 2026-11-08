# Future Resource Protocol

FRP 是面向 GTNH 2.9.0 的独立模组，与 GTNH Adjust 分别编译、分别安装，不依赖 GTNH Adjust。本目录目前只有可加载的空模组入口，尚未实现预支、清算、配方限制或其他玩法；本轮不需要游戏内测试。

功能需求见 [FRP 功能设计](../docs/FRP.md)，实现方案及待验证问题见 [FRP 技术设计](../docs/FRP-technical-design.md)。正式功能的开关、依赖、兼容版本和重启要求将在实现时补充。

在仓库根目录使用 Java 25 构建，复用仓库 Gradle Wrapper 和项目内缓存：

```powershell
$env:JAVA_HOME = 'D:\CustomLib\zulu25.28.85-ca-jdk25.0.0-win_x64'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:GRADLE_USER_HOME = "$PWD\.gradle-user-home"
$env:TEMP = "$PWD\.tmp"
$env:TMP = $env:TEMP
.\gradlew.bat -p frp build
```

构建产物位于 `frp/build/libs/`；主项目仍使用仓库根目录的原有构建命令。FRP 是独立 Gradle 构建，不加入主项目的构建依赖图；共享 Wrapper 只是开发工具，不是模组运行时依赖。
