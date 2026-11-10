# 1.0.9 Beta13 — 新手引导、材料兼容与蓝图包修复

这是 Simulated City 社区维护版的 Beta13 测试包，基于 Minecraft 1.12.2 / Forge 14.23.5.2860。

## 本版重点

### 1. 新手引导

- 新增 **16 页市长手册**，按 `V` 打开，包含目录、开局、第一栋建筑、建筑列表、材料、经济、农业、采矿、其他职业、商店/物流、NPC 生活、第一天路线、FAQ 和发展史。
- 任命书界面重做：页面可读性、按钮居中、模式说明、玩法说明入口。
- 启动卷轴不再消耗；接受任命后发放建筑箱 ×1、标记棒 ×3。
- 第一次放置建筑箱、第一栋建筑完工、进入已开城世界时增加提示。
- HUD 未启动时改为明确提示“右键启动卷轴 / 按 V 查看手册”。

### 2. 建筑列表

- 分类页签、搜索、表头排序、占地筛选、预算筛选、重置、滚轮/翻页、整行点击。

### 3. 材料兼容

- 正常/创造模式：木板、原木、木栅栏、羊毛可以交任意变种。
- 放置仍按蓝图设计，不会因为交云杉就改变建筑外观。
- 需求列表和缺料提示会显示“任意木板/任意原木/任意木栅栏/任意羊毛”。
- 门、楼梯、半砖、火把等仍由建筑师直接放置，不要求材料。

### 4. 蓝图包修复

- 重新生成 `buildings_zh_cn.zip`，改用源码目录的 346 个蓝图。
- 修掉旧 zip 中已删除方块：
  - `sim:block_composite_brick`（制糖厂）
  - `sim:block_living`（制糖厂）
  - `sim:block_copper`（现代住宅v4）
- 启动日志不再出现 `addBuildingBlueprint出错了：null`。
- 运行时 `resources/sim/buildings_zh_cn` 会在首次启动时从新 zip 重新解压。

### 5. 整合包

- PCL 2.99.0.1 绿色完整版，内置 Minecraft 1.12.2、Forge、运行库、资源、Java 8 和 22 个模组。
- 附带 `Sunflawer 0.4.1 Lightshaft Update` 光影（默认未启用）。
- 内置 Pam's HarvestCraft 简体中文补全资源包。
- 版本目录：`Simulated-Beta13-Pack`。

### 6. 源码对照

- 以 `上游源码-原始`（1.0.9 Beta，253 个 Java 文件）为基线：94 个未改、119 个修改、24 个新增、40 个删除。
- 新增 `docs/upstream-diff.md`，按包列出新增/修改/删除文件和功能对应；README 的改动说明以这份对照为准。
- 删除的 40 个文件主要是铜/锡矿与装备、风车、路径箱、旧颗粒、旧快递任务和旧同步包；详见文档。

## 下载

- 文件：`Simulated-City-1.12.2-1.0.9-beta13-PCL.zip`
- 大小：`282,737,576` 字节（约 269.7 MB）
- SHA-256：`6EEECA60FD64F524833B0AB2F834E130A91D0F98367EFFEF19CB7E5C2921AF8B`

## 安装

1. 解压 zip 到任意目录，不要在压缩包内直接运行；
2. 双击 `启动器.bat`，确认 PCL 选中 `Simulated-Beta13-Pack`；
3. 点击“启动游戏”；
4. 光影在 `选项 → 视频设置 → 光影` 中选择；
5. 备用启动方式：直接双击 `启动游戏.bat`（离线用户名 `Player`）。

## 已知问题

- 蓝图中的门、床、告示牌、楼梯、火把、画/物品展示框放置仍有问题：门/床可能偏移或缺半块，告示牌文字会丢失，楼梯转角可能错误，画/展示框可能因墙体替换掉落。详细报告见 `docs/blueprint-special-blocks.md`。
- 专家/硬核模式暂未恢复：GUI 没有入口，服务端只接受 `0/1/999`，也没有“全部材料”规则。
- 旧 MCBBS 基础包没有随 Beta13 更新；本次发布的是 PCL 完整版。
- 启动日志可能出现第三方模组/旧存档提示；如果仍出现 `addBuildingBlueprint` 空指针，请确认使用的是本版 Beta13 包。

## 许可证与来源

- 上游 Simulated / Sim-U 代码和资源仍归原作者所有，CurseForge 标记为 All Rights Reserved；本仓库不重新授权这些部分。
- 本仓库新增的代码、文档、构建和打包脚本按 MIT 许可提供。
- 第三方组件（Minecraft、Forge、PCL2、OptiFine、Pam's HarvestCraft、MoBends、Sunflawer 等）遵循各自许可，详见 `LICENSE` 和 `THIRD_PARTY_NOTICES.md`。

## 致谢

感谢 Scott “Satscape” Hather、AshJack、历代 Sim-U 维护者、所有模组作者，以及提交测试报告、建筑蓝图和翻译建议的玩家。
