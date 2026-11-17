# EyeOfHarmonyBuffer 移植来源

物质重塑仪的结构、加工配方、NEI 布局、提示文本和标志纹理移植自
[SafariXiu/EyeOfHarmonyBuffer](https://github.com/SafariXiu/EyeOfHarmonyBuffer)，
源提交 `927487325d41a7b6b083158c7b5747d8b292bb4a`，原项目采用 GNU GPL v3。
原许可全文随发行包保存在 `licenses/EyeOfHarmonyBuffer-GPL-3.0.txt`。

对应代码位于 `com.gtnhadjust.machine.reshaping`，标志纹理位于
`assets/gtnhadjust/textures/gui/reshaping.png`。稳定环纹理位于
`assets/gtnhadjust/textures/blocks/reshaping/rings/`，直接复制原项目的 PhotonControllerUpgrades 纹理。
保留完整结构及可选 EV 简化模式，将材料名称、舱室结构 API、并行输出计算和生命周期适配至 GTNH 2.9.0。
鸿蒙材料链只使用粉、炽热锭、锭三种形式，由本模组独立物品保存，沿用原项目的 SHINY 纹理与颜色。
原项目的诸神之锻炉自由解锁行为在 `com.gtnhadjust.mixins.godforge` 中按 2.9.0 接口实现。

PTMEG 弹性体在 GTNH 2.9.0 中已移除。为保留原配方产物，本项目恢复其熔融流体，
沿用 GTNH 2.8.4 的颜色 `(248, 248, 255)`、温度 `600 K` 和通用熔融纹理。
未恢复已移除材料的旧物品 ID。
