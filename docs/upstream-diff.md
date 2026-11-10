# 上游源码对照：1.0.9 Beta → 1.0.9 Beta13

这份文档记录 Beta13 相对**上游原始 Java 源码**的实际改动，供审查、二次开发和发布说明使用。README 里的功能描述以这里列出的文件和行为为准。

## 1. 对照口径

- **基线（上游）**：`上游源码-原始/com/trhsy/sim`，`ModSim.VERSION = "1.12.2-1.0.9 Beta"`，253 个 `.java` 文件；公开上游为 <https://github.com/trhsy/sim>（分支 `1.12.2`）。
- **当前（Beta13）**：`src/main/java/com/trhsy/sim`（本地工程为 `构建/src/main/java/com/trhsy/sim`），`ModSim.VERSION = "1.12.2-1.0.9 Beta13"`，237 个 `.java` 文件。
- **比对方法**：按相对路径逐文件计算 SHA-256；哈希相同记为“未改”，不同记为“修改”；只在一侧存在记为“新增/删除”。
- **统计范围**：只统计 Java 源码，不含 `src/main/resources` 下的语言、模型、蓝图和贴图，也不含 `build.gradle`、`mcmod.info` 和打包脚本。
- **注意**：这是**文件级**差异。119 个“修改”文件里有格式化、注释、异常日志和小修，不等于每个文件都重写；具体行为见第 6 节。

## 2. 总览

| 状态 | 文件数 | 说明 |
|---|---:|---|
| 上游文件总数 | 253 | `1.12.2-1.0.9 Beta` |
| 当前文件总数 | 237 | `1.12.2-1.0.9 Beta13` |
| 未改动 | 94 | 与上游逐字节相同，包含部分核心职业与任务 |
| 修改 | 119 | 主要分布在 `npcCode`（47）、`gui`（14）、`network`（14）、`loader`（12） |
| 新增 | 24 | 存档、寻路、经济、物流、手册、兼容层 |
| 删除 | 40 | 铜/锡矿与装备、风车、路径箱、旧颗粒、旧快递任务等 |
| **有变化** | **183** | `+14,390 / -10,693` 行（Java 源码） |

按包分布：

| 包 | 新增 | 修改 | 删除 | 未改 | 合计 |
|---|---:|---:|---:|---:|---:|
| `npcCode` | 11 | 47 | 3 | 49 | 110 |
| `network` | 2 | 14 | 5 | 20 | 41 |
| `block` | 0 | 11 | 10 | 1 | 22 |
| `item` | 0 | 8 | 13 | 1 | 22 |
| `gui` | 3 | 14 | 2 | 2 | 21 |
| `loader` | 1 | 12 | 1 | 7 | 21 |
| `entity` | 1 | 4 | 2 | 5 | 12 |
| `util` | 5 | 2 | 2 | 3 | 12 |
| `proxy` | 1 | 2 | 0 | 1 | 4 |
| 其他 | 0 | 5 | 2 | 5 | 12 |

## 3. 新增的 24 个文件

| 文件 | 行数 | 作用 |
|---|---:|---|
| `npcCode/store/SimStore.java` | 324 | 用 `WorldSavedData` + NBT 保存经济、日历、NPC、物流设置和订单；替代 NPC/设置用的 `.sk2` 行文本 |
| `npcCode/nav/NpcNavigator.java` | 1018 | NPC 统一移动控制器：分段寻路、卡住检测、救援传送 |
| `npcCode/SimIds.java` | 322 | 职业/种族/特质/建筑类型的稳定 ID，不再拿翻译后的显示名当主键 |
| `npcCode/economy/Economy.java` | 97 | 工资、产出补贴、买饭、店主喂食、原料费；余额不为负 |
| `npcCode/logistics/LogisticsService.java` | 514 | 物流中心调度：下单、领单、租约、重试 |
| `npcCode/logistics/LogisticsSettings.java` | 197 | 物流中心开关与单物品派送设置 |
| `npcCode/logistics/LogisticsSnapshot.java` | 113 | 服务端 → 客户端的物流状态快照 |
| `npcCode/logistics/SupplyCatalog.java` | 293 | 可供应物品与供货点匹配 |
| `npcCode/logistics/SupplyOrder.java` | 283 | 供应订单与 NBT 读写 |
| `npcCode/task/TaskBuyFood.java` | 136 | NPC 饿了去商店买饭，钱从 NPC 流向店主/城市 |
| `npcCode/task/JobTaskRequestSupply.java` | 129 | 商店向物流中心下单，替代老板自己跑供货点 |
| `entity/ai/FolkPathNavigateGround.java` | 27 | 单次 A* 搜索半径限制为 48 格 |
| `loader/SimChunkLoader.java` | 287 | 只加载建筑/农场/矿场所在区块 |
| `proxy/ClientWorldEvents.java` | 27 | 把客户端世界状态重置从服务端事件里拆出来 |
| `util/AsyncFileWriter.java` | 96 | 建筑/农场/矿场小文件异步、原子写入 |
| `util/ModCropCompat.java` | 147 | Pam's HarvestCraft 作物/种子兼容 |
| `util/SeasonCompat.java` | 57 | Serene Seasons 反射兼容，未安装时行为不变 |
| `util/WoodHelper.java` | 65 | 木板/原木/木栅栏材料族判断 |
| `util/Onboarding.java` | 108 | 补发卷轴、新手工具、一次性提示 |
| `gui/GuiCityGuide.java` | 288 | 16 页市长手册（目录 + 15 个主题） |
| `gui/GuiStyle.java` | 147 | 原版灰色面板、凹槽、进度条、文本对齐 |
| `gui/GuiLayout.java` | 96 | 居中、行距、截断、裁剪 |
| `network/client/PacketLogisticsSnapshot.java` | 52 | 物流快照下发 |
| `network/server/PacketToggleLogistics.java` | 111 | 客户端切换物流总开关/单物品开关，服务端校验距离后写 `SimStore` |

## 4. 删除的 40 个文件

删除不是“换皮”，而是把上游里独立于城市循环、且本版不再维护的并行系统移出编译和注册。

| 组 | 文件 | 说明 |
|---|---|---|
| 铜/锡矿与装备 | `BlockCopper`、`BlockCopperOre`、`BlockTin`、`BlockTinOre`、`ItemCopperArmor`、`ItemTinArmor`、`ItemCopperTool`、`ItemTinTool`、`ItemCopperIngot`、`ItemTinIngot`、`ItemGranulesCopper`、`ItemGranulesGold`、`ItemGranulesIron`、`ItemGranulesTin`、`WorldGeneratorCopperOre`、`WorldGeneratorTinOre`、`WorldGeneratorLoader` | 装备与铁制重复；`ItemLoader` 保留 `tinPickaxe = Items.IRON_PICKAXE` 等兼容别名，职业代码不需要全部重写 |
| 风车 | `BlockWindmill`、`TileEntityWindmill`、`ContainerWindmill`、`GuiWindmill`、`ItemWindmillBase`、`ItemWindmillSails`、`ItemWindmillVane`、`PacketOpenWindmillGui` | 独立系统，未接入城市循环；方块状态与模型已删，实体贴图仍留在 `textures/models/`（未引用） |
| 路径箱 | `BlockPathBox`、`GuiPathBox`、`PacketOpenPathBoxGui` | 旧路径工具，已被 `NpcNavigator` 的分段寻路取代 |
| 复合砖/地毯方块 | `BlockCompositeBrick`、`BlockLiving`、`EnumBlockLiving` | 未注册/不可获得；旧蓝图引用它们会导致 `addBuildingBlueprint` 空指针 |
| 牛奶流体重复实现 | `BlockFlowingMilk` | 保留 `BlockMilk` / `FluidMilk` |
| 旧快递任务 | `JobTaskCollectItems`、`JobTaskUnloadItems`、`JobTaskCourierDroppingOff`、`Courier`、`CourierTask` | 被 `LogisticsService` + `JobTaskRequestSupply` 取代 |
| 旧同步包 | `PacketSyncBuilding`、`PacketSyncNpcData`、`PacketOpenFolkInventoryGui` | NPC 记录改走 `SimStore` 的 NBT；建筑仍写 `buildings/<uuid>.sk2`，但改由 `AsyncFileWriter` 异步原子写入；旧整包同步不再需要 |

## 5. 修改的 119 个文件（按包）

只列主要文件和行为；完整文件列表见本节末尾。

### `npcCode`（47）

- `NpcData.java`：NPC 生命周期、住宅分配、人口增长、睡觉/回家、死亡清理、经济记账；diff 约 `+1015 / -406`。
- `Job.java`、`JobBuilder.java`、`JobCourier.java`、`JobTaskCourier.java`：职业任务队列、施工、快递改走物流订单。
- `Building.java`、`BuildingBlueprint.java`：建筑结构、材料需求、门/床坐标、蓝图读取；`BuildingBlueprint` 负责材料族合并和“任意木板/原木/栅栏/羊毛”显示。
- `block/FarmBox.java`、`block/MineBox.java`：农场/矿场工作点与存档。
- `task/JobTask*.java`：采集、种植、收获、生产、巡逻、装盒等任务适配新寻路和经济。

### `gui`（14）

- `GuiBlockConstructorBlock.java`：建筑列表搜索、表头排序、占地筛选、预算筛选、重置、滚轮、整行点击；diff 约 `+946 / -610`。
- `GuiBlockControllerBlock.java`、`GuiBlockFarmBlock.java`、`GuiBlockMineBlock.java`：控制箱、农场、矿场界面。
- `GuiHud.java`、`GuiRunMod.java`：HUD 与任命书；未启动时提示右键卷轴/按 V。
- `GuiFolk.java`、`GuiEmployees.java`、`GuiBankATM.java`、`GuiMerchant*.java`：NPC、员工、银行、商店界面。

### `network`（14）

- `PacketSetupMod.java`：只接受 `0/1/999`；非单人服务器要求 OP；首次从 `999` 进入城市时发新手工具和提示。
- `PacketHireFolk.java`、`PacketFireFolk.java`、`PacketNewFolk.java`：雇佣/解雇/新生。
- `PacketBuyStuff.java`、`PacketSellStuff.java`：商店买卖。
- `PacketUpdateFarmBox.java`、`PacketUpdateMineBox.java`：农场/矿场设置。
- `PacketrotateStairs.java`：楼梯旋转。

### `loader`（12）

- `SimmodeStart.java`：从 `SimStore` 读经济/日历/NPC；旧 `.sk2` 移到 `legacy_backup_*`。
- `EventLoader.java`：登录欢迎、补发卷轴、新手工具、一次性提示。
- `ModSimLoader.java`：资金、聊天、NPC/建筑列表、存档协调。
- `BlockLoader.java`、`ItemLoader.java`、`CraftingLoader.java`：删除铜/锡/风车/路径箱/复合砖/地毯的注册与配方，保留铁制兼容别名。
- `ConfigLoader.java`、`NetWorkLoader.java`、`CommandLoader.java`、`EntityLoader.java`、`FluidLoader.java`：配置、网络、命令、实体、流体注册适配。

### `block` / `item` / `entity`（23）

- `BlockConstructorBox.java`：第一次放置建筑箱提示。
- `BlockControlBox.java`、`BlockFarmingBox.java`、`BlockMiningBox.java`、`BlockMarker.java`、`BlockLightBox.java`：控制箱/农场/矿场/标记/灯箱。
- `ItemSimULoader.java`：右键不再 `splitStack(1)`，卷轴不消耗。
- `EntityNpc.java`、`InventoryNpc.java`、`ContainerNpc.java`：NPC 实体、背包、容器。
- `ItemBase.java`、食物类：注册与工具提示。

### `util` / `proxy` / 其他（9）

- `BuildingsExtractor.java`（未改）：仍按 GBK 读取蓝图 zip；本次只换 zip 内容。
- `PricesForBlocks.java`、`CommodityAtm.java`：价格与 ATM 商品。
- `ClientProxy.java`、`CommonProxy.java`：客户端/服务端代理拆分。
- `KeyboardManager.java`：`V` 键打开 `GuiCityGuide`。
- `CommandChangeCredits.java`：`/credits`。
- `ModSim.java`：版本号、异步写盘 flush、服务器停止事件。

<details>
<summary>完整修改文件列表（119）</summary>

```text
ModSim.java
block/BlockCheese.java
block/BlockConstructorBox.java
block/BlockControlBox.java
block/BlockFarmingBox.java
block/BlockLightBox.java
block/BlockMarker.java
block/BlockMilk.java
block/BlockMiningBox.java
block/BlockSpecial.java
block/enums/EnumControlBox.java
block/enums/EnumLightColour.java
commands/CommandChangeCredits.java
creative/CreativeTabsFMLTutor.java
entity/EntityNpc.java
entity/ai/FolkAIWander.java
entity/container/ContainerNpc.java
entity/container/InventoryNpc.java
fluid/FluidMilk.java
gui/GuiHud.java
gui/GuiRunMod.java
gui/block/GuiBlockConstructorBlock.java
gui/block/GuiBlockControllerBlock.java
gui/block/GuiBlockFarmBlock.java
gui/block/GuiBlockMarker.java
gui/block/GuiBlockMineBlock.java
gui/npc/GuiBankATM.java
gui/npc/GuiEmployees.java
gui/npc/GuiFlower.java
gui/npc/GuiFolk.java
gui/npc/GuiFolkInventory.java
gui/npc/GuiMerchant.java
gui/npc/GuiMerchants.java
item/ItemBase.java
item/ItemBucketMilk.java
item/ItemSimULoader.java
item/food/ItemBurger.java
item/food/ItemCheese.java
item/food/ItemCheeseburger.java
item/food/ItemDrink.java
item/food/ItemFries.java
key/KeyboardManager.java
loader/BlockLoader.java
loader/CommandLoader.java
loader/ConfigLoader.java
loader/CraftingLoader.java
loader/EntityLoader.java
loader/EventLoader.java
loader/FluidLoader.java
loader/ItemLoader.java
loader/ModSimClientLoader.java
loader/ModSimLoader.java
loader/NetWorkLoader.java
loader/SimmodeStart.java
network/client/ConfigSyncPacket.java
network/client/PacketOpenControlGui.java
network/client/PacketOpenFolkGui.java
network/client/PacketReturnHireableFolks.java
network/server/PacketBuyStuff.java
network/server/PacketDemolishBuilding.java
network/server/PacketFireFolk.java
network/server/PacketHireFolk.java
network/server/PacketNewFolk.java
network/server/PacketSellStuff.java
network/server/PacketSetupMod.java
network/server/PacketUpdateFarmBox.java
network/server/PacketUpdateMineBox.java
network/server/PacketrotateStairs.java
npcCode/NpcData.java
npcCode/NpcIdentity.java
npcCode/V3.java
npcCode/block/FarmBox.java
npcCode/block/MineBox.java
npcCode/build/Building.java
npcCode/build/BuildingBlueprint.java
npcCode/job/Job.java
npcCode/job/JobBaker.java
npcCode/job/JobBartender.java
npcCode/job/JobBrickMaker.java
npcCode/job/JobBuilder.java
npcCode/job/JobBurgers.java
npcCode/job/JobButcher.java
npcCode/job/JobCheesemaker.java
npcCode/job/JobCourier.java
npcCode/job/JobDairyFarmer.java
npcCode/job/JobEggFarmer.java
npcCode/job/JobFactory.java
npcCode/job/JobFarmer.java
npcCode/job/JobFisherman.java
npcCode/job/JobGlassMaker.java
npcCode/job/JobGrocer.java
npcCode/job/JobLivestockFarmer.java
npcCode/job/JobMcDonald.java
npcCode/job/JobShepherd.java
npcCode/job/JobSoldier.java
npcCode/job/JobSugar.java
npcCode/task/JobTask.java
npcCode/task/JobTaskBuilder.java
npcCode/task/JobTaskButcherAnimal.java
npcCode/task/JobTaskCheesemaker.java
npcCode/task/JobTaskCourier.java
npcCode/task/JobTaskFarmerHarvest.java
npcCode/task/JobTaskFarmerPlant.java
npcCode/task/JobTaskHarvestAnimal.java
npcCode/task/JobTaskPatrol.java
npcCode/task/JobTaskPlaceInChest.java
npcCode/task/JobTaskProduceItem.java
npcCode/task/JobTaskSearchForBlock.java
npcCode/task/JobTaskShopkeep.java
npcCode/task/JobTaskSpawnLivestock.java
npcCode/task/JobTaskTerrain.java
npcCode/task/JobTaskUseFurnace.java
npcCode/task/Task.java
npcCode/task/TaskSleep.java
npcCode/task/TaskSocialise.java
proxy/ClientProxy.java
proxy/CommonProxy.java
util/PricesForBlocks.java
util/items/CommodityAtm.java
```

</details>

## 6. 功能与代码对应

| README 里的说法 | 对应实现 | 实际行为 |
|---|---|---|
| 16 页市长手册，按 V 打开 | `gui/GuiCityGuide.java`（`PAGES` 16 组）、`key/KeyboardManager.java` | 第 1 页是目录，其余 15 页为开局、第一栋建筑、建筑列表、材料、经济、农业、采矿、职业、商店/物流、NPC 生活、第一天路线、FAQ、发展史 |
| 启动卷轴不消耗 | `item/ItemSimULoader.java` | 右键只发 `PacketOpenSetupGui`，没有 `splitStack/shrink`；接受任命后由 `Onboarding.giveStarterKit` 发建筑箱 ×1、标记棒 ×3 |
| 新手提示 | `util/Onboarding.java`、`loader/EventLoader.java`、`block/BlockConstructorBox.java`、`npcCode/job/JobBuilder.java` | 登录/首次放置建筑箱/第一栋建筑完工各提示一次；`container.sim.tip.first_building` 在 `JobBuilder.createBuilding` 里发送 |
| 建筑列表搜索/排序/筛选 | `gui/block/GuiBlockConstructorBlock.java` | 搜索框、表头点击排序（名称/占地/费用/来源）、占地筛选、预算筛选、重置、滚轮、整行点击进入详情 |
| 材料族兼容 | `util/WoodHelper.java`、`npcCode/build/BuildingBlueprint.java` | `flexibleMaterials = ModSimLoader.gamemode != 2`；木板/原木/木栅栏按族合并，羊毛按 `Blocks.WOOL` 合并；专家模式（2）保持精确匹配 |
| 放置仍按蓝图 | `BuildingBlueprint`、`JobBuilder.placeBlock` | 材料统计可以合并，但放置使用蓝图原本的 `IBlockState`，不会因为交云杉就改成云杉外观 |
| 蓝图包 346 个蓝图 | `assets/sim/buildings/buildings_zh_cn.zip` | 346 个 `.txt`；`sim:block_composite_brick`、`sim:block_living`、`sim:block_copper` 均为 0 处 |
| 中文/英文 | `assets/sim/lang/zh_cn.lang`、`en_us.lang` | `container.sim.guide.*` 中英各 113 个键；手册与新手提示都有英文 |
| 22 个模组 | `mods/` | 22 个 jar（含本模组）；Forge 日志显示加载 24 个 mod 条目（含 Minecraft/Forge/FML） |

## 7. 未改动的 94 个文件

这些文件与上游逐字节相同，说明本版没有重写全部职业和任务逻辑。例子：

- 职业：`JobLumberjack`、`JobMiner`、`JobTerrainFormer`、`JobMerchant`、`JobATM`、`JobAccoucheur`、`JobBarefootDoctor`、`JobBuildersMerchant`、`JobFlower`
- 任务：`JobTaskChopTrees`、`JobTaskMineBlocks`、`JobTaskFarmer`、`JobTaskFarmerGrow`、`JobTaskIdle`、`TaskGoTo`、`TaskWander`、`TaskProcreate`
- 建筑数据：`BlueprintRequirements`、`BuildingSymbol`、`Structure`、`TerrainType`、`TerrainTypeRequitrements`
- 网络：`PacketOpenSetupGui`、`PacketOpenConstructorGui`、`PacketSendBlueprint`、`PacketUpdateMoney` 等
- 其他：`BuildingsExtractor`、`ServerProxy`、`SQLiteJDBC`、`ConfigGui`、`ATMscreen`

<details>
<summary>完整未改动文件列表（94）</summary>

```text
block/enums/EnumBlock.java
commands/CommandGenerateFolk.java
commands/CommandStart.java
config/SimConfigSync.java
entity/EntityConBox.java
entity/EntityDinkEmpty.java
entity/ai/FolkAIFenceGateInteract.java
entity/ai/FolkAIOpenFenceGate.java
entity/container/SlotNpcCrafting.java
gui/ATMscreen.java
gui/ConfigGui.java
item/ItemDrinkEmpty.java
key/ModKeyBinding.java
loader/CreativeTabsLoader.java
loader/SoundRegistry.java
loader/render/ItemRenderLoader.java
loader/render/ModelConBox.java
loader/render/RenderConBox.java
loader/render/RenderDinkEmpty.java
loader/render/RenderEntityFolk.java
network/client/PacketAddNewMarker.java
network/client/PacketEntityAnimation.java
network/client/PacketOpenBankATMGui.java
network/client/PacketOpenConstructorGui.java
network/client/PacketOpenFarmGui.java
network/client/PacketOpenFlowerGui.java
network/client/PacketOpenHudGui.java
network/client/PacketOpenMarkerGui.java
network/client/PacketOpenMerchantGui.java
network/client/PacketOpenMerchantsGui.java
network/client/PacketOpenMineGui.java
network/client/PacketOpenSetupGui.java
network/client/PacketSendBuildingRequirements.java
network/client/PacketSendFolkSkin.java
network/client/PacketSendTerrainTypeRequitrements.java
network/client/PacketUpdateMoney.java
network/client/PacketUpdateNPC.java
network/server/PacketGetHireableFolks.java
network/server/PacketSendBlueprint.java
network/server/PacketSendTerrainType.java
npcCode/DynamicSkin.java
npcCode/FolkRelationship.java
npcCode/NpcSkin.java
npcCode/block/Marker.java
npcCode/build/BlueprintRequirements.java
npcCode/build/BuildingSymbol.java
npcCode/build/Structure.java
npcCode/build/TerrainType.java
npcCode/build/TerrainTypeRequitrements.java
npcCode/enums/EnumBlock.java
npcCode/enums/EnumFamilyType.java
npcCode/enums/EnumLevel.java
npcCode/enums/FarmType.java
npcCode/job/JobATM.java
npcCode/job/JobAccoucheur.java
npcCode/job/JobBarefootDoctor.java
npcCode/job/JobBuildersMerchant.java
npcCode/job/JobFlower.java
npcCode/job/JobLumberjack.java
npcCode/job/JobMerchant.java
npcCode/job/JobMiner.java
npcCode/job/JobTerrainFormer.java
npcCode/moodbuff/MoodBuff.java
npcCode/race/Race.java
npcCode/race/RaceDarkElf.java
npcCode/race/RaceElf.java
npcCode/race/RaceHuman.java
npcCode/race/RaceOrc.java
npcCode/race/Races.java
npcCode/task/JobTaskChopTrees.java
npcCode/task/JobTaskFarmer.java
npcCode/task/JobTaskFarmerGrow.java
npcCode/task/JobTaskIdle.java
npcCode/task/JobTaskMineBlocks.java
npcCode/task/TaskGoTo.java
npcCode/task/TaskProcreate.java
npcCode/task/TaskWander.java
npcCode/traits/Trait.java
npcCode/traits/TraitBrave.java
npcCode/traits/TraitDwarvenHeritage.java
npcCode/traits/TraitFriendly.java
npcCode/traits/TraitGreenThumb.java
npcCode/traits/TraitHatesOutdoors.java
npcCode/traits/TraitLovesOutdoors.java
npcCode/traits/TraitNightOwl.java
npcCode/traits/TraitReligious.java
npcCode/traits/TraitStrong.java
npcCode/traits/TraitWorkaholic.java
npcCode/traits/Traits.java
proxy/ServerProxy.java
sqlite/SQLiteJDBC.java
util/BuildingsExtractor.java
util/items/Commodity.java
util/items/CommodityFlower.java
```

</details>

## 8. 资源与打包层

这些不在 Java 文件统计里，但会直接影响游戏：

- `buildings_zh_cn.zip`：346 个蓝图，352 个 zip 条目（含目录）；三个旧方块 ID 已清零。
- `lang/zh_cn.lang`、`lang/en_us.lang`：手册、物流、新手提示的键已补齐；`container.sim.guide.*` 中英各 113 个。
- `mcmod.info`：作者改为 `Simulated Community`，链接改为维护仓库，版本号 `1.12.2-1.0.9 Beta13`。
- 旧方块状态/模型/语言键：铜、锡、风车、路径箱、复合砖、地毯方块对应的 `blockstates`、`models`、`lang` 键已删除；风车实体贴图仍留在 `textures/models/`，当前没有代码引用。
- PCL 包：`Simulated-Beta13-Pack`，22 个模组 jar、Java 8 runtime、PCL 2.99.0.1、Sunflawer 光影（默认未启用）。

## 9. 这份对照不能证明什么

- 不能证明运行时没有 bug；它只说明代码相对上游改了什么。实际测试记录在 `测试报告/`。
- 不能证明第三方模组、光影、PCL 的兼容性；这些需要单独测试。
- 不能证明建筑外观完全正确；蓝图中的门、床、告示牌、楼梯、火把、画/展示框仍有已知问题，见 `docs/blueprint-special-blocks.md`。
- 不能证明许可证状态；上游代码和资源仍按上游条款，新增贡献按仓库 `LICENSE`。
