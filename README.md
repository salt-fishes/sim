# 模拟城市 1.12.2 精选整合包 / Simulated City Community Edition

> 当前版本：**1.0.9 Beta13**  
> 游戏版本：Minecraft **1.12.2** / Forge **14.23.5.2860** / Java **8**  
> 维护仓库：<https://github.com/salt-fishes/sim>  
> 下载：<https://github.com/salt-fishes/sim/releases>

这是一个 1.12.2 城市模拟整合包：你以“市长”的身份接管一片空地，雇佣 NPC 建造住宅、商店、农场、矿场和仓库，靠租金、工资和供应链把城镇运转起来。

本仓库是 Simulated / Sim-U 的社区维护分支。我们尽量保留原版的建筑、职业、数值和节奏，主要做 bug 修复、兼容性补充、中文本地化和旧版遗留内容清理，不是重制版。

## 这个版本有什么

- **16 页市长手册**：游戏内按 `V` 打开，带目录、开局步骤、建筑列表、材料、经济、农业、采矿、职业、商店/物流、NPC 生活、第一天路线、FAQ 和发展史。
- **启动卷轴不消耗**：右键随时打开任命/模式界面；接受任命后获得建筑箱 ×1、标记棒 ×3。
- **建筑列表重做**：分类页签、搜索、表头排序、占地筛选、预算筛选、滚轮/翻页、整行点击。
- **材料兼容**：正常/创造模式下，木板、原木、木栅栏、羊毛可以交任意变种；建筑外观仍按蓝图放置。
- **蓝图包修复**：重新生成 `buildings_zh_cn.zip`，修掉旧包里的 `sim:block_composite_brick`、`sim:block_living`、`sim:block_copper` 等已删除方块，启动日志不再出现 `addBuildingBlueprint` 空指针。
- **中文/英文双语**：语言键对齐，补全了手册和新手提示。
- **PCL 绿色完整版**：游戏本体、Forge、运行库、资源、Java 8 和模组已预装，解压即玩；包内附带 Sunflawer 0.4.1 Lightshaft Update 光影（默认未启用）。

## 快速开始（PCL 完整版）

1. 从 [Releases](https://github.com/salt-fishes/sim/releases) 下载 `Simulated-City-1.12.2-1.0.9-beta13-PCL.zip`；
2. 解压到任意目录（不要在压缩包里直接运行）；
3. 双击 `启动器.bat` 打开 PCL2，确认选中的是 `Simulated-Beta13-Pack`；
4. 点击“启动游戏”。不需要下载游戏本体或 Java。

备用方式：直接双击 `启动游戏.bat`，不经过 PCL 启动预置版本（离线用户名 `Player`）。

光影：进入游戏后打开 `选项 → 视频设置 → 光影`，选择 `Sunflawer 0.4.1 Lightshaft Update`。默认没有开启，避免低配设备一进游戏就卡顿。

## 游戏内怎么上手

1. 右键「模拟城市启动卷轴」→「接受任命」；
2. 选择「创造模式」（不耗材料，推荐第一次玩）或「正常模式」（要自己收集材料）；
3. 放建筑箱 → **先雇建筑工** → 再点「选择建筑」选小住宅；
4. 正常模式把材料放进建筑箱周围 5 格内的箱子，点「需求」核对后点「建造它」；
5. 完工后右键控制箱查看住户/员工；住宅有人入住后每天收租；
6. 需要材料就做耕种箱/采矿箱；商店需要仓库 + 快递员组成物流中心。

建筑工只能从玩家 16 格内的失业 NPC 中雇佣。如果列表为空，先在附近找找，或等新居民出现。

游戏内按 `V` 可以随时打开市长手册；卷轴丢了用 `/simmode` 重新打开任命界面；测试资金用 `/credits <金额>`。

## 从源码构建

需要 **JDK 8**（Forge 1.12.2 / ForgeGradle 2.3 不支持 JDK 17+）。

```bat
set JAVA_HOME=C:\path\to\jdk8
cd 构建
gradlew.bat build
```

产物：

```text
构建\build\libs\simulated-1.12.2-1.0.9 Beta13.jar
```

构建前请确认 `JAVA_HOME` 指向 JDK 8；`build.gradle` 会使用 `JAVA_HOME` 下的 `javac`。

## 仓库结构

```text
构建/                         Forge 1.12.2 工程（源码、资源、构建脚本）
发布/                         最终 PCL 完整版 zip 和本地测试工作目录
docs/                         设计、测试、历史与问题报告
测试脚本/                      自动化测试脚本
工具/                          蓝图编辑器
Pam's-HarvestCraft-汉化/       潘马斯农场简体中文补全资源包
上游源码-原始/                 上游 Java 源码快照（用于对照）
```

## 已知问题

- 蓝图中的**门、床、告示牌、楼梯、火把、画/展示框**放置仍有已知问题：门/床可能偏移或缺少半块，告示牌文字会丢失，楼梯转角会错，画/展示框可能因墙体替换而掉落。详细报告见 [docs/blueprint-special-blocks.md](docs/blueprint-special-blocks.md)。
- **专家/硬核模式**目前没有恢复入口：GUI 没有按钮，服务端只接受 `0/1/999`，`isRequiredBlock` 也没有“全部材料”分支。正常/创造模式不受影响。
- 启动日志里仍可能出现少量旧蓝图/第三方模组提示；如果出现 `addBuildingBlueprint` 空指针，请确认使用的是 Beta13 或更新版本。

## 参与贡献

欢迎提交 Issue 和 Pull Request：

- 复现问题时请附上游戏版本、Forge 版本、日志和操作步骤；
- 修改蓝图时请同时更新 `buildings_zh_cn.zip`，否则游戏仍会读取旧包；
- 蓝图 zip 的中文文件名必须使用 **GBK** 编码，`BuildingsExtractor` 按 GBK 读取；
- 不要提交 `.gradle/`、`.shelf/`、`build/`、`run/`、日志或测试存档。

## 许可证

本仓库包含多个来源的代码和资源，**不能用一个标准许可证覆盖全部内容**：

- 上游 Simulated / Sim-U 代码与资源仍归其原作者所有，CurseForge 页面标记为 All Rights Reserved；本仓库不重新授权这些部分；
- 本仓库新增的代码、文档、构建和打包脚本，在允许的范围内按 **MIT License** 提供；
- Minecraft、Forge、PCL2、OptiFine、Pam's HarvestCraft、MoBends 等第三方组件遵循各自许可证。

完整说明见 [LICENSE](LICENSE) 和 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。公开发布或二次分发前，请自行确认上游和第三方授权。

## 鸣谢

- 最初 Sim-U-Kraft 的作者 Scott “Satscape” Hather；
- 1.7.10 时期社区重制 Sim-U-Kraft Reloaded 的 AshJack；
- 后续中文版 Sim-U 的维护者与贡献者；
- PCL2、Forge、OptiFine、Pam's HarvestCraft、MoBends、JEI、Serene Seasons 等模组与工具的作者；
- 所有提交测试报告、建筑蓝图和翻译建议的玩家。

本项目不是 Mojang、Microsoft、PCL 或任何模组作者的官方项目。

---

## English summary

**Simulated City Community Edition** is a community-maintained Minecraft 1.12.2 city-simulation modpack. It preserves the original buildings, jobs, economy and pacing while fixing bugs, adding Chinese localization, improving onboarding, and cleaning up legacy content.

- Requires Minecraft 1.12.2, Forge 14.23.5.2860, Java 8.
- Download the PCL full package from [Releases](https://github.com/salt-fishes/sim/releases); extract, run `启动器.bat`, and launch `Simulated-Beta13-Pack`.
- Press `V` in game for the 16-page mayor's handbook.
- Build from source with JDK 8: `cd 构建 && gradlew.bat build`.
- Known issue: door/bed/sign/stairs/torch/painting placement in blueprints is still being fixed; see [docs/blueprint-special-blocks.md](docs/blueprint-special-blocks.md).
- License: upstream code/assets remain under their original terms; original contributions in this repository are MIT. See [LICENSE](LICENSE).
