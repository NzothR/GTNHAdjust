# 开发约定

- 以 GTNH 2.9.0 稳定版为当前开发基线；新增模组依赖时参照该版本清单及 `ref/GT-Never-Reset`，避免混用版本。
- 开发和构建使用 Java 25，本机 JDK 位于 `D:\CustomLib\zulu25.28.85-ca-jdk25.0.0-win_x64`；系统 `JAVA_HOME` 不代表开发版本。IDEA 可用，但必要时需自行查找其安装位置。
- 本机游戏实例为 `D:\Game\PrismLauncher-Windows-MinGW-w64-Portable-11.1.1\instances\GT_New_Horizons_2.9.0_Java_17-26\.minecraft`。未经用户事先同意，不修改项目目录以外的文件或文件夹；申请时说明范围和作用。
- 本机服务端实例为 `D:\Game\MinecraftServer\GT_New_Horizons_2.9.0_Server_Java_17-26`；只读检查日志及配置可以直接进行，修改仍须事先说明并获用户同意。
- 参考源码放在 `ref/`，不提交到 Git。Git 提交遵循 Conventional Commits。
- 可启动开发相关进程并关闭自己启动的进程，不得随意关闭已有进程。
- 负责构建和静态检查；游戏启动和内容测试由用户完成。核心流程、推测点及可能出错处按需加入可由配置开关控制、默认关闭的调试日志。
- 可以自主实现改进和测试思路，并在交付时说明用户需要测试的内容；涉及存档损坏或其他高风险操作时先询问用户。
- Python 由 conda 管理；如果现有环境不足，遵守项目外修改限制后再处理环境和依赖。
