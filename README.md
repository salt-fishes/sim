# 模拟城市 1.12.2 精选整合包 / Simulated City Community Edition

> 当前版本：**1.0.9 Beta13**  
> 游戏版本：Minecraft **1.12.2** / Forge **14.23.5.2860** / Java **8**  
> 维护仓库：<https://github.com/salt-fishes/sim>  
> 下载：<https://github.com/salt-fishes/sim/releases>

这是一个 1.12.2 城市模拟整合包：你以“市长”的身份接管一片空地，雇佣 NPC 建造住宅、商店、农场、矿场和仓库，靠租金、工资和供应链把城镇运转起来。

本仓库是 Simulated / Sim-U 的社区维护分支。我们保留城市建造的核心循环、现有建筑和职业框架，主要做 bug 修复、兼容性补充、中文本地化和旧版遗留内容清理，不是重制版。

## 这个版本有什么

- **16 页市长手册**（`gui/GuiCityGuide.java`）：游戏内按 `V` 打开，第 1 页是目录，其余 15 页覆盖开局、第一栋建筑、建筑列表、材料、经济、农业、采矿、职业、商店/物流、NPC 生活、第一天路线、FAQ 和发展史。
- **启动卷轴不消耗**（`item/ItemSimULoader.java`）：右键只打开任命/模式界面，不再 `splitStack(1)`；接受任命后由 `util/Onboarding.java` 发建筑箱 ×1、标记棒 ×3。
- **建筑列表重做**（`gui/block/GuiBlockConstructorBlock.java`）：分类页签、搜索、表头点击排序、占地筛选、预算筛选、重置、滚轮、整行点击。
- **材料族兼容**（`util/WoodHelper.java` + `npcCode/build/BuildingBlueprint.java`）：正常/创造模式下，木板、原木、木栅栏、羊毛可以交任意变种；放置仍使用蓝图原本的方块状态。
- **蓝图包修复**：`buildings_zh_cn.zip` 里的 346 个蓝图已重新生成，`sim:block_composite_brick`、`sim:block_living`、`sim:block_copper` 三个旧 ID 均为 0 处，旧包因这些 ID 触发的 `addBuildingBlueprint` 空指针已消除。
- **中文/英文双语**：手册与新手提示的 `container.sim.guide.*` 中英各 113 个键；物流和提示也有英文。
- **PCL 绿色完整版**：PCL 2.99.0.1、游戏本体、Forge 14.23.5.2860、Java 8 和 22 个模组已预装；包内附带 Sunflawer 0.4.1 Lightshaft Update 光影（默认未启用）。

## 相对上游源码改了什么

对照基线是上游 `1.12.2-1.0.9 Beta` 的原始 Java 源码（本地快照 `上游源码-原始`，253 个文件；公开仓库 `trhsy/sim`）。按相对路径逐文件比 SHA-256：

| 状态 | 文件数 | 说明 |
|---|---:|---|
| 上游文件总数 | 253 | `1.12.2-1.0.9 Beta` |
| 当前文件总数 | 237 | `1.12.2-1.0.9 Beta13` |
| 未改动 | 94 | 与上游逐字节相同，含 `JobLumberjack`、`JobMiner`、`JobTerrainFormer` 等原版职业 |
| 修改 | 119 | 主要在 `npcCode` 47、`gui` 14、`network` 14、`loader` 12 |
| 新增 | 24 | 存档、寻路、经济、物流、手册、兼容层 |
| 删除 | 40 | 铜/锡矿与装备、风车、路径箱、旧颗粒、旧快递任务等 |
| **有变化** | **183** | `+14,390 / -10,693` 行 |

完整文件清单和逐项对应见 [docs/upstream-diff.md](docs/upstream-diff.md)。下面只列会影响玩法的部分。

### 存档与网络

- `npcCode/store/SimStore.java`：用原版 `WorldSavedData` + NBT 保存经济、日历、NPC、物流设置和订单；不再把 `.sk2` 文本当主存档。
- `loader/SimmodeStart.java`：从 `SimStore` 读档；旧 `.sk2` 不直接删除，先移到 `legacy_backup_*`。
- `util/AsyncFileWriter.java`：建筑/农场/矿场的小文件异步、原子写入，避免在服务端 tick 里做磁盘 I/O。
- 删除 `PacketSyncBuilding`、`PacketSyncNpcData`：NPC/建筑不再整包同步。

### NPC

- `npcCode/nav/NpcNavigator.java`（1018 行）：统一移动控制器，长距离拆成约 24 格的 hop，卡住时救援。
- `entity/ai/FolkPathNavigateGround.java`：单次 A* 搜索半径限制为 48 格，避免原版 `FOLLOW_RANGE=256` 带来的大范围寻路。
- `npcCode/SimIds.java`：职业、种族、特质、建筑类型改用稳定 ID，不再拿翻译后的显示名当主键；切换语言不会让职业变 null。
- `NpcData`、`EntityNpc`：生命周期、住宅分配、人口增长、睡觉/回家、死亡清理。

### 建筑与蓝图

- `Building`、`BuildingBlueprint`：结构、材料需求、门/床坐标、施工和读档。
- `WoodHelper` 只处理木板/原木/木栅栏；羊毛在 `BuildingBlueprint` 里按 `Blocks.WOOL` 合并。两处都用 `ModSimLoader.gamemode != 2` 判断，所以正常/创造模式可以交任意变种，专家模式（2）仍精确匹配；专家模式当前没有 GUI 入口，见“已知问题”。
- `loader/SimChunkLoader.java`：只加载建筑结构覆盖的区块、农场/矿场工作点周围 1 区块，不是整张地图常加载。

### 经济与物流

- `npcCode/economy/Economy.java`：在岗工资、产出补贴、NPC 买饭、店主喂食、原料费；任何一方余额都不会被扣成负数。
- `npcCode/logistics/*`（5 个类）+ `PacketToggleLogistics` / `PacketLogisticsSnapshot`：商店下单、快递员领单/取货/送货，订单有租约和重试上限。
- `TaskBuyFood`、`JobTaskRequestSupply`：替换旧的 `JobTaskCollectItems` / `JobTaskUnloadItems`，老板不再自己跑供货点。

### 界面与引导

- `GuiCityGuide` + `KeyboardManager`：`V` 键打开 16 页手册；`Onboarding` 负责补发卷轴、新手工具和一次性提示。
- `GuiBlockConstructorBlock`：搜索、表头排序、占地/预算筛选、重置、滚轮、整行点击；点击表头名称/占地/费用/来源可切换排序方向。
- `PacketSetupMod`：只接受 `0/1/999`；非单人服务器要求 OP；首次从 `999` 进入城市时发新手工具。
- `GuiStyle` / `GuiLayout`：统一原版灰色面板、居中、换行和裁剪。

### 清理和删减

- 删除上游的铜/锡矿、工具、护甲、颗粒和世界生成（`BlockCopper*`、`BlockTin*`、`ItemCopper*`、`ItemTin*`、`WorldGenerator*`）；`ItemLoader` 保留 `tinPickaxe = Items.IRON_PICKAXE` 等铁制别名，职业代码不用全部重写。
- 删除风车、路径箱、复合砖、地毯方块、旧牛奶流体实现，以及对应的方块状态/模型/语言键；风车实体贴图仍留在 `textures/models/`，当前没有代码引用。
- 界面标题和 `mcmod.info` 里的旧 QQ 群/公众号/官网文案已移除，作者改为 `Simulated Community`，链接指向维护仓库。
- 以上删除共 40 个 Java 文件；保留的 94 个文件与上游逐字节相同，建筑、职业和数值没有被整体重做。

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
git clone https://github.com/salt-fishes/sim.git
cd sim
set JAVA_HOME=C:\path\to\jdk8
gradlew.bat build
```

产物：

```text
build\libs\simulated-1.12.2-1.0.9 Beta13.jar
```

本地工作目录里工程放在 `构建\` 下，所以产物是 `构建\build\libs\...`；GitHub 仓库根目录直接放 `build.gradle`，两者源码相同。构建前请确认 `JAVA_HOME` 指向 JDK 8；`build.gradle` 会使用 `JAVA_HOME` 下的 `javac`。

## 仓库结构

GitHub 仓库只提交可复现的源码和文档：

```text
src/                     Forge 1.12.2 源码与资源
docs/                    设计、问题报告、上游对照、发展史
build.gradle             构建脚本
README.md               项目说明
LICENSE                 许可说明
THIRD_PARTY_NOTICES.md  第三方组件说明
```

本地完整工作目录还包含以下内容（未提交到仓库）：

```text
构建/                    Forge 工程和构建产物
发布/                    PCL 完整版 zip 和打包工作目录
测试报告/ 测试脚本/       本地测试记录和脚本
工具/                    蓝图编辑器
Pam's-HarvestCraft-汉化/ 潘马斯农场简体中文补全资源包
上游源码-原始/           上游 1.0.9 Beta 源码快照（对照用）
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

**Simulated City Community Edition** is a community-maintained Minecraft 1.12.2 city-simulation modpack. It keeps the original city-building loop, existing blueprints and job values, while fixing bugs, adding Chinese localization, improving onboarding, and cleaning up legacy content.

- Requires Minecraft 1.12.2, Forge 14.23.5.2860, Java 8.
- Download the PCL full package from [Releases](https://github.com/salt-fishes/sim/releases); extract, run `启动器.bat`, and launch `Simulated-Beta13-Pack`.
- Press `V` in game for the 16-page mayor's handbook.
- Source diff against upstream 1.0.9 Beta: 94 files unchanged, 119 modified, 24 added, 40 removed; see [docs/upstream-diff.md](docs/upstream-diff.md).
- Build from source with JDK 8: `git clone https://github.com/salt-fishes/sim.git && cd sim && gradlew.bat build`.
- Known issue: door/bed/sign/stairs/torch/painting placement in blueprints is still being fixed; see [docs/blueprint-special-blocks.md](docs/blueprint-special-blocks.md).
- License: upstream code/assets remain under their original terms; original contributions in this repository are MIT. See [LICENSE](LICENSE).
