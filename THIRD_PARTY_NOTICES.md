# EyeOfHarmonyBuffer 移植来源

物质重塑仪的结构、加工配方、NEI 布局、提示文本和标志纹理移植自
[SafariXiu/EyeOfHarmonyBuffer](https://github.com/SafariXiu/EyeOfHarmonyBuffer)，
源提交 `927487325d41a7b6b083158c7b5747d8b292bb4a`，原项目采用 GNU GPL v3。
原许可全文随发行包保存在 `licenses/EyeOfHarmonyBuffer-GPL-3.0.txt`。

对应代码位于 `com.gtnhadjust.machine.reshaping`，标志纹理位于
`assets/gtnhadjust/textures/gui/reshaping.png`。仅保留 EV 简化结构、控制器制作和
17 条加工配方，移除原项目产出星门及升级组件的配方。材料名称、舱室 API、
并行输出和生命周期已适配至 GTNH 2.9.0；机器提示追加结构来源。
原项目的诸神之锻炉自由解锁行为在 `com.gtnhadjust.mixins.godforge` 中按 2.9.0 接口实现。

PTMEG 弹性体在 GTNH 2.9.0 中已移除。为保留原配方产物，本项目恢复其熔融流体，
沿用 GTNH 2.8.4 的颜色 `(248, 248, 255)`、温度 `600 K` 和通用熔融纹理。
未恢复已移除材料的旧物品 ID。

## GT-Never-Reset 附属总成

矩阵大批量跨配方样板输入总成基于 [GT-Never-Reset 0.1.51](https://github.com/ikkkkkkkuuu/GT-Never-Reset/tree/0.1.51) 的样板仓扩展接口运行。样板编解码、通配样板、物料缓存和 AE 下单缩放由已安装的 GTNR 提供，GTNHAdjust 随包分发自己的总成和矩阵扣料实现。权利人如需更正归属或移除相关内容，请通过本仓库联系维护者。
