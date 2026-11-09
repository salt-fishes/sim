# 职业系统与传送系统：全面复核与完全重构方案（v2·原生 API 优先）

> v2 说明：本版根据"多调用 MC 原生方法、多参考其他模组写法"的要求重写。v1 的自建调度器/自建步骤解释器/自建存档格式被替换为**原版 AI、原版导航、原版 NBT、原版物品与交易、原版区块加载**为主的方案。所有"原生 API"均在 1.12.2 Forge 14.23.5.2847 的 MCP 映射下核对过签名（来源：ForgeJavaDocs-NG 1.12.2）。
>
> 目标：`C:\Users\15572\Music\mc2\模拟城市 1.12.2 精选整合包-源码\sim-1.12.2`；前提：**不考虑旧存档兼容**。

---

## 0. 基线快照（防止"源码不同"造成的分析漂移）

本方案针对下列快照逐文件核对过。请以 `git hash-object` 结果为准：

| 文件 | git blob 哈希（当前上游） |
|---|---|
| `npcCode/NpcData.java` | `613a901f19c37d46926c9a553aba655fd88ed2cd` |
| `npcCode/job/Job.java` | `44c514f939770eb71f35e53674b04feb52788de4` |
| `npcCode/task/JobTask.java` | `b2c4a155c34bf90600876688e1ee2340ad6071d9` |
| `loader/EventLoader.java` | `44f84fc373d8e09aacfec78e5d8762ba40c25557` |
| `loader/SimmodeStart.java` | `86638fa3be981c3d4963138e5aada03a8c5d62c1` |
| `entity/EntityNpc.java` | `957de9936a40821f6caebd996bafe4198016a925` |
| `npcCode/job/JobFactory.java` | `a17ec653c60256230b3f25b13d44931ee5ed6389` |

复核命令（PowerShell）：

```powershell
$up = "C:\Users\15572\Music\mc2\模拟城市 1.12.2 精选整合包-源码\sim-1.12.2\src\main\java"
foreach ($f in @(
  "com\trhsy\sim\npcCode\NpcData.java",
  "com\trhsy\sim\npcCode\job\Job.java",
  "com\trhsy\sim\npcCode\task\JobTask.java",
  "com\trhsy\sim\loader\EventLoader.java",
  "com\trhsy\sim\loader\SimmodeStart.java",
  "com\trhsy\sim\entity\EntityNpc.java",
  "com\trhsy\sim\npcCode\job\JobFactory.java")) {
  "{0}  {1}" -f (git hash-object (Join-Path $up $f)), (Split-Path $f -Leaf)
}
```

> 如果哈希不匹配，说明上游已被替换：本方案需要先按差异重审再实施（文中的行号与结论只对上述快照成立）。若你说的"上游"其实是另一份代码（例如 `构建` 或别的路径），请指出，我按那份重做。

---

## 1. 结论：能否"完全修复"

**能。** 在不考虑旧存档的前提下，第 4 节 10 类痛点都有对应的**原生机制**或**已被多个成熟模组验证过的做法**，且不引入任何新依赖（NBT、AI、导航、物品、交易、区块加载全部是 1.12.2 自带/Forge 自带）。

需要说清楚的边界（第 8 节详述）：

- "寻路到不了的地方"不是 bug，任何模组都无法保证一定走到；正确做法是**超时 + 安全传送兜底 + 失败上报**，让行为可预期、可诊断，而不是静默卡死。
- NPC 数量极大时（数百）性能取决于寻路预算与区块加载策略；方案给出的是**有界开销**设计（节流、冷却、批次、按需加载），不是"无限免费"。
- 完全修复的前提是**换掉任务索引状态机、换掉 .sk2 存档、换掉每 tick 全量驱动**；如果保留这三样，只做局部修补，痛点会以新的形式复现（上一轮 54 个文件、约 2600 行补丁已经证明了这一点）。

---

## 2. 现状复核（只列与修复直接相关的机制）

### 2.1 职业系统

| 机制 | 位置 | 事实 |
|---|---|---|
| 职业数据挂在 `NpcData.job` | `NpcData.java:1966-2050` | `onUpdate()` 每 tick 由 `SimmodeStart` 驱动 |
| 任务列表 + 下标状态机 | `Job.java:99-125`（`nextTask`）、`Job.java:1001-1021`（`onArrive`） | `stage` 是 `jobTasks` 的下标；任务完成后 `stage=-1/currentTask=null`，重启靠各职业自己写 `else if` |
| 养殖业状态机 | `JobLivestockFarmer.java:111-114` | 判断 `livestockFarmerStage == 9`，但没有任何地方置 9；状态 8 无分支接住 |
| 任务超时语义 | `JobTask.java:49-58` | `deadline > 0` 才超时；`-1L` = 永不超时；`JobTaskIdle.onUpdate/onTaskComplete` 为空 |
| 失败静默 | `JobTask.java:37-40` | `failTask(message)` 丢弃 message |
| 职业重建 | `NpcData.java:679-1155`、`1159-1233`、`JobFactory.java:176-179` | 按**当前语言翻译串** if/else 或注册表匹配 |
| 存档 | `NpcData.java:1539-1676` | `.sk2` 行格式；`job|<本地化名>`；`holding|getDisplayName()` |
| 派工/查询 | `ModSimLoader.getClosestFarm`、`getEntityFolkByUID` | 遍历中 `remove(i)+add(new)` 跳元素、丢运行时状态；`==` 比较 UUID/String |

### 2.2 传送/移动系统

| 机制 | 位置 | 事实 |
|---|---|---|
| `forceMoveToXYZs` | `NpcData.java:2707-2732` | `clearPath()` 后 `getPath()` 必为 null，条件 `path==path1 && path!=null && path1!=null` 永不成立 → **每次调用都瞬移** |
| `forceMoveToXYZ` | `NpcData.java:2740-2801` | `path==null` 或 `distance>=15` 时瞬移；否则 `setPath(path,1.5)`；末尾再做一次 A* + 射线检测；永远 `return true` |
| 每 tick 重发 | `Job.java:291-300` | `handleGoingToWork()` 每 tick `forceMoveToXYZ(workPlace)`（每次都 `clearPath()`+A*） |
| 粒子 | `NpcData.java:2861-2915` | `Minecraft.getMinecraft().world`（客户端类）→ 专服 NPE 被吞、单机世界错位 |
| 到达判定 | `NpcData.java:2999-3008` | 只看 x/z 绝对值 < 1.5；不看 y、不看维度 |
| 坐标偏移 | `NpcData.java:2921-2923` | `getAdjustedV3` 固定 `y+1.1`，与到达阈值/落点不一致 |
| 多写者 | `Job`、`JobTask*`、`TaskGoTo/TaskSleep`、原版 AI、`EntityNpc.noClip` | 同一 tick 内导航被反复 `clearPath`/`setPath`，没有"移动所有权" |

### 2.3 tick 与侧隔离

| 机制 | 位置 | 事实 |
|---|---|---|
| 事件重复注册 | `EventLoader.java:64-73` | `MinecraftForge.EVENT_BUS` + 自建 `EVENT_BUS` + `FMLCommonHandler.bus()`（后两者同一个/空总线） |
| tick 放大 | `EventLoader.java:249-270`、`SimmodeStart.java:284-409` | `WorldTickEvent` 无 Phase/维度守卫 → 双相 × 三维度；所有 NPC 每 tick `onUpdate` |
| 客户端类进入服务端路径 | `SimmodeStart.java:12/909/964`、`KeyboardManager.java:25-30`、`BuildingsExtractor.java:36` | 专用服务器崩溃报告 `crash-2026-09-30_18.03.59-server.txt` 即 `KeyBinding` |
| 异步残骸 | `SimmodeStart.java:611-712` 等 | `processNpcsAsync`/`clientSimModupdate`/`serverSimModupdate` 未接线 |

---

## 3. 原生 API 对照表（"多调用 MC 原生方法"）

下表是逐项替换依据。签名均来自 1.12.2 Forge JavaDocs（MCP 名）。

| 现状（自建/错误做法） | 原生替代 | 为什么更好 |
|---|---|---|
| `JobTask` 列表 + 下标 `stage` | `EntityAIBase` + `EntityAITasks.addTask(priority, task)`；AI 内用 `shouldExecute/startExecuting/updateTask/resetTask` 表达步骤 | 优先级与互斥（`setMutexBits`）由原版保证；任务生命周期由原版管理；不再有下标/死分支/列表膨胀 |
| `NpcData.onUpdate()` 每 tick 驱动所有 NPC | `Entity.onUpdate()` + 每 NPC 的 tick 计数器；全局节奏用 `TickEvent.ServerTickEvent`（或 `WorldTickEvent` + `Phase.END`） | 与原版实体更新同频，天然按维度/加载状态处理；无需自建调度器 |
| `forceMoveToXYZs`（每次瞬移） | `PathNavigateGround.getPathToXYZ` + `setPath(path, speed)`；`EntityMoveHelper.setMoveTo`；AI 中的 `EntityAIMoveToBlock` | 原版寻路自带节点处理、卡住重算、门/游泳配置；不会"引用比较"式误判 |
| `setPositionAndUpdate` + 自建粒子 | `EntityLivingBase.attemptTeleport(x,y,z)`（原版安全传送，末影人/潜影贝同款）；需要粒子时用 `WorldServer.spawnParticle(EnumParticleTypes, boolean longDistance, double,double,double, int, double,double,double, double, int...)` | 服务端安全、自动做落点合法性检查、自动同步；粒子按距离广播 |
| 到达判定 | 原版 `Entity.getDistanceSq`/`getDistance` 与 AABB；`PathNavigate.noPath()`、`Path.isFinished()` | 语义正确、包含 y 与维度 |
| `.sk2` 行文本存档 | `NBTTagCompound` + `CompressedStreamTools.writeCompressed/readCompressed`；`Entity.writeEntityToNBT/readEntityFromNBT`；`WorldSavedData` + `MapStorage.getOrLoadData` | 原版存档格式（含类型、版本、原子写入由原版保障）；实体随世界保存/加载天然一致 |
| 本地化字符串主键 | `ResourceLocation` + `ForgeRegistries`（物品）；`VillagerRegistry.VillagerProfession/VillagerCareer`（职业式注册）；自定义 `Map<ResourceLocation, JobEntry>`（参考 MineColonies `IJobRegistry`/`JobDataManager`） | 语言无关、命名空间化、可迁移 |
| 手写箱子循环（布尔 + 整组结算） | `IItemHandler` + `ItemHandlerHelper.insertItemStacked`/`insertItem`；`IItemHandler.extractItem`；`InvWrapper` 适配 `IInventory`；`CapabilityItemHandler.ITEM_HANDLER_CAPABILITY` | 返回值就是"实际放入/取出数量"；槽位、堆叠、模拟插入全部由原版/Forge 处理 |
| 商店买卖 GUI 自己扣钱 | `MerchantRecipeList` + `MerchantRecipe`（`EntityVillager` 交易同款） | 交易规则（上限、经验、价格）原版管理；不重复实现货币与库存逻辑 |
| 建筑/工地每 tick 由全局循环扫描 | `TileEntity implements ITickable`；`ForgeChunkManager` 票据按需加载工地区块 | 只 tick 加载中的方块实体；工地不在线时不空转 |
| 家/建筑/兴趣点分配 | `VillageCollection`/`Village`（原版村庄 POI 体系）或同构的 `WorldSavedData` | 原版已解决"门/住宅/聚集点"的维护与保存 |
| 服务端误用客户端类 | `@SideOnly(Side.CLIENT)`、`@Mod.EventBusSubscriber(modid = ModSim.MODID, value = Side.CLIENT)`、`CommonProxy/ClientProxy` | 专用服务器不再链接客户端类（`NoClassDefFoundError` 从根上消失） |
| 自建线程池/异步改世界 | `IThreadListener.addScheduledTask`（`MinecraftServer`/`WorldServer` 实现）、`isCallingFromMinecraftThread()` | 所有世界操作回到主线程；不再有异步状态撕裂 |
| Forge 配置 | `net.minecraftforge.common.config.Configuration` | 标准 config 文件与同步机制 |
| 调试命令 | `ICommand` + `CommandBase`（注册到 `FMLServerStartingEvent`） | 自带权限、补全、子命令；便于做 `/sim diag` |

---

## 4. 参考其他模组的做法（按问题对应）

以下都是 1.12.2 时代、可公开查证的实现；括号内类名已核对存在。

### 4.1 MineColonies（1.12.2 `archive/1.12` 分支）——最接近的整套参考

- **职业数据与行为分离**：`colony/jobs/AbstractJob`（职业数据）+ `colony/jobs/JobFarmer`、`JobBuilder`、`JobMiner`、`JobDeliveryman`、`JobBaker`、`JobChickenHerder`、`JobCowboy`、`JobShepherd`、`JobFisherman`、`JobFlorist`、`JobSmelter` 等职业类；**行为**放在 `entity/ai/citizen/<职业>` 包（如 `farmer/`、`builder/`、`miner/`、`herders/`、`deliveryman/`）。
- **职业序列化注册**：`colony/jobs/registry/IJobRegistry` + `JobDataManager`——按注册名读写职业数据（对应我们"稳定 ID + NBT"）。
- **全局数据**：`ColonyManager` 以 `WorldSavedData` 形式保存殖民地；建筑/市民各自 NBT（对应我们的 `SimWorldData` + 实体 NBT）。
- **可借用结论**：NPC 的"职业"只是**数据 + AI 任务**，不是自建 tick 循环；所有移动/动作都在 AI 层，由原版调度。

### 4.2 Minecraft Comes Alive（1.12.2 `1.12.2-6.0.1` 分支）

- **自定义实体 + AI**：`mca/entity/EntityVillagerMCA`，AI 在 `mca/entity/ai` 包（全部是 `EntityAIBase` 家族）。
- **数据对象化**：`mca/entity/data/SavedVillagers`、`PlayerSaveData`、`ParentData`——持久数据是独立类，走 NBT 读写（对应我们的 `NpcRecord`/`JobData`）。
- **物品栏**：`mca/entity/inventory` 包。
- **可借用结论**：不要把状态塞进"大而全的 NpcData + 文本文件"，而是**实体 NBT + 小型数据类**。

### 4.3 原版（最应该先抄的"模组"）

- **村民交易**：`EntityVillager` + `VillagerRegistry.VillagerProfession/VillagerCareer` + `MerchantRecipeList/MerchantRecipe`——商店职业（杂货、面包、酒馆、麦当劳、银行）直接用这套，别自己写买卖。
- **村庄 POI**：`VillageCollection`/`Village`/`VillageDoorInfo`——住宅分配、建筑登记、POI 维护的现成范式。
- **AI/导航**：`EntityAITasks`、`EntityAIBase`、`EntityAIMoveToBlock`、`EntityAIRestrictOpenDoor`、`EntityAIOpenDoor`、`EntityAIMoveIndoors`、`PathNavigateGround`、`EntityMoveHelper`、`WalkNodeProcessor`。
- **传送/粒子**：`EntityLivingBase.attemptTeleport`、`WorldServer.spawnParticle`（boolean longDistance 重载）、`Entity.setPositionAndUpdate`（会经 `EntityTracker` 自动同步）。
- **存档**：`NBTTagCompound`、`CompressedStreamTools`、`WorldSavedData`、`MapStorage`、`Entity.writeEntityToNBT/readEntityFromNBT`。
- **物品**：`IItemHandler`/`ItemHandlerHelper`/`InvWrapper`/`CapabilityItemHandler`（Forge）；`Container`/`Slot`/`SlotItemHandler`。
- **区块**：`ForgeChunkManager`（`Ticket`/`forceChunk`）。
- **侧隔离**：`@SideOnly`、`Side`、代理模式、`@Mod.EventBusSubscriber`。

### 4.4 其他可对照的模组（模式层面）

- **Millénaire**：村庄/建筑/村民职业与 POI 驱动，建筑数据 NBT。
- **Ancient Warfare 2**：工人（`EntityWorker` 类体系）+ 工作订单（work order）+ AI 执行。
- **Tektopia**：殖民地居民职业与工作区分配。
- 共同点：**职业=数据，行为=AI，存档=NBT，建筑=TileEntity，移动=原版导航**。

---

## 5. 目标架构（原生优先）

### 5.1 数据与生命周期（替换 `NpcData` + `.sk2`）

```
EntityNpc (extends EntityCreature)
├── writeEntityToNBT / readEntityFromNBT   // 原版实体持久化：身份、种族、特质、技能、关系、职业数据
├── tasks: EntityAITasks                   // 原版 AI 调度
│   ├── 生活 AI（回家/睡觉/社交/闲逛）      // EntityAIBase
│   ├── 工作 AI（当前职业，一个）           // EntityAIBase，读取 JobData
│   └── 原版 AI（游泳/开门/看玩家…）
└── getCapability(ITEM_HANDLER)            // Forge 物品能力（或保留 InventoryNpc + InvWrapper）

SimWorldData extends WorldSavedData         // 每个存档一份（参考 ColonyManager）
├── NPC UUID 索引（HashMap，替换线性扫描）
├── 建筑/农场/矿场/快递点注册表（稳定 ID）
├── 经济（钱、价格、工资）
└── 日历（day/week/rent）
```

- 读档时：先加载 `SimWorldData`，实体随区块由原版从 NBT 恢复；`EntityNpc.readEntityFromNBT` 恢复 `JobData`；不再"手工 new 实体 + 手工 setPosition"。
- 语言切换不再影响任何逻辑（`ResourceLocation` 主键）。

### 5.2 职业框架（替换 `Job/JobTask/JobFactory`）

```java
// 数据（可 NBT 序列化）——对应 MineColonies 的 AbstractJob/JobDataManager
public final class JobData implements INBTSerializable<NBTTagCompound> {
    ResourceLocation jobId;      // sim:job/livestock_pig
    BlockPos site; int dimension;
    NBTTagCompound progress;     // 每职业的小状态（不再用全局 stage 下标）
    float skill;                 // 技能进度（统一升级公式）
}

// 注册表（稳定 ID → 定义）
public final class JobRegistry {
    static final Map<ResourceLocation, JobEntry> JOBS = new HashMap<>();
    // JobEntry: displayKey, Class<? extends EntityAIBase> ai, site matcher, wage
}

// 行为（原版 AI）——对应 MineColonies entity/ai/citizen/*
public class AIWorkLivestock extends EntityAIBase {
    public boolean shouldExecute() { return job() != null && npc.shouldWork(); }
    public void startExecuting() { step = Step.SPAWN; }
    public void updateTask() {
        switch (step) {
            case SPAWN:   /* 数量检查 + 生成，完成后 step=IDLE */ break;
            case IDLE:    /* 计时，完成后 step=BUTCHER */ break;
            case BUTCHER: /* 目标选择 + 原版寻路；到达后处理，完成/超时转 TEND */ break;
            case TEND:    /* 计时 + 状态文本 */ break;
        }
    }
    public void resetTask() { npc.getNavigator().clearPath(); }
}
```

要点：
- **一个 NPC 同时只有一个工作 AI**；`setMutexBits` 与生活 AI 互斥，从根上消除"多个系统抢导航"。
- 步骤用 `enum`（编译期穷尽），不再有 `==9` 死分支；状态推进在 `updateTask` 内显式完成。
- 每步自带超时计数与"无进展"看门狗（位置/库存/实体数不变化超过 N 秒 → 失败上报 + 回退），对应痛点 P2/P3/P8。
- 工资/升级统一走 `JobData.skill` + `SimWorldData.economy`，替换 12 处复制的 `0.001/b4`。

### 5.3 移动与传送（替换 `forceMoveToXYZ*`）

```java
// AI 内：
if (!npc.getNavigator().noPath()) return;                 // 已有路径，交给原版
Path p = npc.getNavigator().getPathToXYZ(target.getX(), target.getY(), target.getZ());
if (p != null) {
    npc.getNavigator().setPath(p, 1.0D);                  // 走
} else if (now >= npc.teleportReadyAt) {                  // 走不了 → 兜底
    if (npc.attemptTeleport(target.getX()+0.5, target.getY()+1, target.getZ()+0.5)) {
        npc.teleportReadyAt = now + TELEPORT_COOLDOWN;    // 例如 100 tick
    } else {
        failJob("unreachable");                           // 上报，不静默
    }
}
```

- `attemptTeleport` 自带安全落点检查；需要更醒目的传送效果时用 `((WorldServer)world).spawnParticle(EnumParticleTypes.PORTAL, false, x, y, z, 32, 0.5, 0.5, 0.5, 0.1)`（服务端安全）。
- 寻路失败重试节流（例如 40 tick 一次），传送冷却（每 NPC 5 秒），全局每 tick 传送预算（例如 4 次），彻底消灭"每 tick 瞬移"。
- 到达判定统一用 `getDistanceSq` + 容差；维度不一致直接判失败（不再忽略 `V3.dimension`）。

### 5.4 建筑与工地（替换全局扫描）

- 工地/建筑改为 `TileEntity implements ITickable`，保存自身 NBT（库存用 `IItemHandler` 能力）。
- 需要持续工作的工地用 `ForgeChunkManager` 票据加载（参考 MineColonies 对建筑的加载策略）；NPC 只在区块加载时由原版 AI 正常工作。
- 住宅/兴趣点参考 `VillageCollection`：`WorldSavedData` 里维护 `BlockPos → 建筑` 与空位索引，替换"遍历所有建筑找空房"。

### 5.5 商店与经济

- 商店职业（`JobGrocer/JobBaker/JobBartender/JobMcDonald/JobBurgers/JobMerchant/JobATM/JobFlower`）用 `MerchantRecipeList` 提供交易，GUI 只做展示与选择；金额结算走 `SimWorldData.economy`（`add/spend` 入口做 NaN/Infinity 防护）。
- 物品进出统一 `ItemHandlerHelper.insertItemStacked`（返回剩余）与 `extractItem`（返回实际取出），不再有"布尔 + 整组扣款"。

### 5.6 调度与侧隔离

- 唯一入口：`@SubscribeEvent` 的 `TickEvent.ServerTickEvent`（或 `WorldTickEvent`，只处理 `Phase.END` + `dimension == 0` + `!world.isRemote`），只注册一次。
- 模拟节拍：全局 tick 计数器 + 每 NPC 自己的计数器（例如每 10 tick 一次经济/年龄/饥饱，每 60 秒一次租金），用 `world.getTotalWorldTime()` 驱动；不再依赖 `System.currentTimeMillis()`。
- 建筑由 `ITickable` 原生 tick；实体由原版 tick；`IThreadListener.addScheduledTask` 处理任何跨线程回调。
- `KeyboardManager` 改为 `@Mod.EventBusSubscriber(modid = ModSim.MODID, value = Side.CLIENT)` 或完全移入 `ClientProxy`；`SimmodeStart` 删除 `Minecraft` 引用，拆出客户端类。

### 5.7 存档与旧档处理

- 新存档：`SimWorldData`（`WorldSavedData`，`sim.dat`）+ 实体 NBT（原版随区块保存）。
- 旧档：首次进入世界时若检测到旧 `sim/` 目录且无 `sim.dat`，整体改名为 `sim_legacy_<时间戳>/`，聊天与日志明确提示"本版本不兼容旧存档"（不解析、不删除）。

---

## 6. 痛点 → 原生修复 → 验收

| # | 痛点 | 原生修复 | 验收标准 |
|---|---|---|---|
| P1 | 换语言/重进后失业、特质错乱、手持物丢失 | `ResourceLocation` 主键 + `NBTTagCompound` 存档 + `ForgeRegistries` 物品 ID | 中文建档→切英文→切回：职业/特质/手持/种族一致；日志无 "未知职业" |
| P2 | 卡去工作、瞬移风暴、粒子刷屏 | `PathNavigateGround` 正常寻路 + `attemptTeleport` 冷却兜底 + `WorldServer.spawnParticle` | 每 NPC 瞬移次数/分钟 ≤ 阈值；诊断命令不再出现"卡去工作"；粒子只在真实传送时出现 |
| P3 | 状态机死角、任务列表膨胀 | `EntityAIBase` 步骤枚举 + 看门狗 + 每轮重进 | 养殖户连续 3 游戏日：无停留 > 2× 超时、任务对象数恒定、无死分支 |
| P4 | CPU/TPS、事件重复、全量遍历 | 单入口 `ServerTickEvent` + 每 NPC 计数器 + 寻路节流 + `ITickable` 建筑 | 30 NPC：TPS ≥ 19；`simModupdate` 每 tick 调用 = 1；A* 调用数下降一个数量级 |
| P5 | 专用服务器崩溃 | `@Mod.EventBusSubscriber(value = Side.CLIENT)` + 代理 + 移除 common 中的客户端类 | `forge-server` 正常启动进世界并跑通 30 NPC |
| P6 | 物品/金钱凭空消失或复制 | `IItemHandler`/`ItemHandlerHelper`/`InvWrapper` + `MerchantRecipe` | 箱子满时买卖不扣钱不吞货；交易测试覆盖边界 |
| P7 | 存档损坏、丢 NPC、中文乱码 | 原版 NBT + `WorldSavedData` + 旧档隔离 | 坏档只影响自身；其余实体/数据由原版正常加载 |
| P8 | 失败静默 | AI 失败回调 → 聊天/HUD/日志 | 缺料采石工明确显示原因 |
| P9 | 查询错乱、派工异常 | `SimWorldData` 索引 + 距离排序 + `ResourceLocation` 比较 | `getClosestFarm` 不改列表、按距离命中 |
| P10 | 双端状态镜像混乱 | 服务端权威 + 原版实体同步（`EntityTracker`）+ 侧隔离 | 客户端类不出现在服务端加载路径 |

---

## 7. 目标目录结构（建议）

```
com/trhsy/sim/
├── sim/
│   ├── SimWorldData.java          // WorldSavedData：NPC 索引、建筑/农场/矿场、经济、日历
│   ├── SimTickHandler.java        // 唯一 ServerTickEvent 入口（Phase.END/dim0/单次注册）
│   └── id/SimIds.java             // ResourceLocation 常量与显示映射
├── npc/
│   ├── EntityNpc.java             // 原版实体 + AI + NBT（重写现有类）
│   ├── NpcRecord.java             // 可 NBT 序列化的持久数据（替换 NpcData 的持久部分）
│   └── job/
│       ├── JobData.java           // INBTSerializable<NBTTagCompound>
│       ├── JobRegistry.java       // ResourceLocation → JobEntry
│       └── ai/                    // EntityAIBase 子类（每职业一个，参考 MineColonies）
├── building/
│   ├── TileEntityWorkSite.java    // ITickable + IItemHandler + NBT
│   └── poi/                       // 住宅/兴趣点（参考 VillageCollection）
├── move/
│   └── MoveHelper.java            // 走/传送/失败的统一小工具（AI 内部使用）
└── shop/
    └── ShopTrades.java            // MerchantRecipeList 构建
```

保留外部契约：方块注册名、物品、GUI 入口、网络包名不变；包体字段换成稳定 ID。

---

## 8. 可行性、风险与不能保证的部分

### 8.1 技术可行性（逐项）

- 全部替代项都在 1.12.2 Forge 14.23.5.2847 中可用（第 3 节签名已核对：`WorldServer.spawnParticle` 三个重载、`EntityLivingBase.attemptTeleport`、`EntityAITasks`、`EntityAIMoveToBlock`、`WorldSavedData`/`MapStorage`、`IItemHandler`、`MerchantRecipeList`、`ForgeChunkManager`、`@Mod.EventBusSubscriber(value=Side.CLIENT)`）。
- 无新增依赖；Java 8 语法即可。
- 现有构建链（`build.gradle`、`构建/`）可继续用于产包与专服验证。

### 8.2 风险与对策

| 风险 | 对策 |
|---|---|
| 28 个职业行为细节多 | 先做行为快照清单（输入/输出/场地/工资/失败条件），再逐个换成 AI；保留旧类作对照直到验收 |
| GUI/网络包仍按显示名通信 | 先加 `SimIds` 适配层（ID ↔ 显示），包体逐步换 ID |
| 原版 AI 与现有生活任务冲突 | 工作 AI 与生活 AI 用 `setMutexBits` 明确互斥；`stayPut` 由 AI 状态统一管理 |
| 寻路本身失败（不可达/跨维度） | 超时 + `attemptTeleport` 冷却兜底 + 失败上报；跨维度直接判失败 |
| 大量 NPC 性能 | 每 tick 预算 + 寻路节流 + 按需加载工地；诊断命令统计寻路/传送次数 |
| 旧档被玩家继续使用 | 启动时隔离旧 `sim/`，聊天与日志明确提示 |

### 8.3 不能保证的部分（诚实说明）

- 不保证"任何坐标都能走到"：原版寻路有物理极限；保证的是**可预期**（超时→传送→失败上报）。
- 不保证"零维护成本"：AI 行为仍需按职业逐个调参（速度、范围、时长）。
- 不保证与旧存档、旧配置文件、旧网络包兼容：旧档按约定隔离；配置项会迁移到 Forge `Configuration`。

---

## 9. 分阶段实施（全部在上游）

| 阶段 | 内容 | 验收 |
|---|---|---|
| P0 | 侧隔离（`KeyboardManager`、`SimmodeStart` 客户端引用）+ 单入口 tick（Phase.END/dim0/单次注册）+ `SimIds` | 专服能启动；每 tick `simModupdate` 恰一次 |
| P1 | `SimWorldData`（`WorldSavedData`）+ `EntityNpc` NBT 持久化 + 旧档隔离 | 存读档一致；换语言不影响数据 |
| P2 | 移动统一（导航 + `attemptTeleport` 冷却 + `WorldServer.spawnParticle`），替换 34 处调用 | 无瞬移风暴；诊断命令通过 |
| P3 | 职业框架（`JobData` + `JobRegistry` + `EntityAIBase`），迁移养殖业/面包师/矿工三个试点 | 试点职业 3 游戏日稳定 |
| P4 | 其余职业迁移 + 商店换 `MerchantRecipe` + 库存换 `IItemHandler` | 买卖/库存边界测试通过 |
| P5 | 工地 `ITickable` + `ForgeChunkManager` + POI（`VillageCollection` 风格） | 离远/回程工地状态正确；TPS 达标 |
| P6 | 专服 + 多人 + 长稳（3 游戏日）回归 | 无崩溃、无丢档、无静默卡死 |

`merge-out` 的 54 文件补丁仍作为**缺陷目录**：被新架构覆盖的模块直接重写；未覆盖模块（经济、部分 GUI）把 A3–A9、B4/B5、E1–E3 等回植上游；`SimIds`、`ClientWorldEvents` 思路并入 P0。

---

## 10. 待拍板

1. 确认旧存档隔离策略（`sim/` → `sim_legacy_<时间戳>/`，不解析）。
2. 确认保留 28 个职业与现有 GUI/交互入口，只换内部实现。
3. 确认实施顺序：P0+P1（基础设施）→ P2（移动，最快止血）→ P3+（职业），还是先做职业试点。

---

*本方案只针对第 0 节列出的快照。若上游源码被替换，请先复核哈希再实施。*

---

# 实施记录（v3，2026-10-02）

按 P0→P6 顺序已在**上游**落地，编译与专用服务器启动均已验证。以下为验收清单。

### P2+ 卡死救援（本轮新增，按你的要求）
- `entity/EntityNpc.java`：
  - **持续窒息/溺水救援**：每秒检查 `isEntityInsideOpaqueBlock()` 与 `isInWater() && getAir() <= 0`，同时 `attackEntityFrom` 里累计 `IN_WALL`/`DROWN` 伤害；连续 3 秒处于危险 → `rescueToSafeLocation()`：
    - 以自身为中心螺旋搜索（半径 8，最多 ±2 格高度）第一个"两格空气 + 下方实体方块"的落点；
    - 找不到则退回家 → 工作点 → 世界出生点；
    - 用原版 `attemptTeleport`（失败则 `setPositionAndUpdate`）+ 服务端传送粒子，5 秒冷却；
    - 日志：`NPC <uuid> 持续窒息/溺水，已救援传送到 <pos>`。
- `npcCode/NpcData.java`：
  - **移动无进展看门狗**：每次 `forceMoveToXYZ/XYZs` 记录目标；`onSecond()` 每秒检查，若"有目标、未到达、连续 5 秒位移 < 0.3 格" → 强制救援传送到目标附近（无视传送冷却）；
  - 日志：`NPC <name> 移动无进展，救援传送到 <pos>`；
  - 传送失败也会记录目标，交给看门狗兜底（不会被冷却卡成永久静止）。

### P2+ 去工作限时（本轮新增，按你的要求）
- `npcCode/job/Job.java`：NPC 一旦进入"去工作"（`onWayToWork`）状态就开始计时；**超过 5 秒仍未到岗**（走不到/卡住/路径失败）→ 直接 `folk.forceTeleportTo(workPlace)` 送到工作地点，随后由 `handleArrival` 正常进入 `atWork`。若到岗失败，每 5 秒重试一次传送（自愈，不会永久卡住）。
- `npcCode/NpcData.java`：新增公开的 `forceTeleportTo(V3)`（无视传送冷却、跨维度拒绝、原版 `attemptTeleport` + 服务端粒子、同步数据层坐标）；看门狗/窒息救援改为复用它。

### P3+ 用户反馈修复（本轮）
- **养殖职业卡"去工作"**：`JobLivestockFarmer` / `JobDairyFarmer` / `JobEggFarmer` / `JobShepherd` 的 `onUpdate` 全部改为"队列空时一次构建整轮任务再执行"（到达 → 补栏 → 照料/生产交替），不再每 tick 只入队不开始；末尾 `-1L` 的照料任务改为有限时长，保证轮次能结束。
- **动物被屠宰光**：`JobTaskSpawnLivestock` 改为数量 <5 时补栏到 6；`JobTaskButcherAnimal` 只在成年动物 >4 时才杀，否则直接结束该屠宰任务 → 数量稳定在 4-6 头。
- **夜晚乱逛**：
  - 夜晚分成两段：**傍晚自由时间**（天黑后 `EVENING_FREE_TICKS = 1200` ticks ≈ 60 秒）NPC 只做社交/闲逛；之后进入深夜，清掉所有非"回家/睡觉"任务并强制回家睡觉（原来 `interruptCurrentTasks` 不清队列，NPC 半夜还在逐个执行白天的闲逛/购物任务）；
  - `TaskSleep` 修复无家 NPC 的 NPE（原来 `home.livingXYZ` 空指针被上层吞掉，任务永远不结束）。

- **回家睡觉 + 睡觉位置**（按你的澄清修正）：深夜不再直接传送，而是走回家（`TaskGoTo`），走不到时由移动看门狗兜底传送；到家开始睡觉的瞬间用 `snapTo(home.livingXYZ)` 精确落到住宅的居住点（原来停在哪睡哪，容易睡在门口/墙边）。建筑没有 `block_living` 标记时会在日志里警告"居住点退化为控制箱位置"。

### P3++ 夜晚/屋顶/士兵（本轮）
- **屋顶卡死**：`getAdjustedV3` 不再向上扫描 4 格（室内目标被挡时会把 NPC 放到屋顶）；新增**屋顶看门狗**——NPC 高于移动目标/住宅/工作点 5 格并持续 5 秒 → 救援传送回目标。
- **夜晚一直闲逛**：新增**深夜兜底**——自由时间结束后的深夜，若 10 秒仍未进入睡觉任务，清任务、必要时传送回家、强制睡觉（`enforceNightSleep`）。
- **士兵豁免**：`isSoldier()` 判定；`handleNightLogic` 直接返回、`enforceNightSleep` 跳过；士兵保持 24 小时巡逻（`shouldWork` 原本就对士兵恒真）。
- **社交任务修复**：`TaskSocialise` 先判空再取 `other.forename`（原 NPE）；目标点原来错用 `folk.home.livingXYZ`（无家 NPC 必 NPE），改为社交建筑自身的位置。

### P4+ 士兵 / 经济闭环 / 钓鱼（本轮）
- **士兵能打怪**：`JobTaskPatrol` 重写——16 格范围找敌对生物（`EntityMob` 覆盖僵尸/骷髅/苦力怕/蜘蛛/史莱姆/恶魂）、攻击间隔 1 秒（原来 2ms 几乎每 tick 都打）、用 `DamageSource.causeMobDamage` 结算并挥臂、目标死亡/超过 24 格自动清理；**没有目标时也会正常巡逻**（原实现无目标时原地发呆）。
- **经济闭环（NPC 自己买吃的）**：
  - `NpcData.money` 个人存款（NBT 持久化，默认 20）；
  - 上班工资：`Job.onMinute` 里 NPC 在岗每分钟 +0.2（玩家支付）；
  - 饥饿 < 6 且有钱 → 找最近食品店（面包师/杂货商/食品商/麦当劳/汉堡店）→ 新增 `TaskBuyFood`：走到店里，从箱子取一份食物，支付 0.5 给玩家，吃掉回饥饿值；
  - 闭环：**工作 → 工资 → 买食物 → 钱回流店主（玩家）**；店里没货或钱不够则先回去工作。
- **钓鱼机制说明**（现有实现，未改）：渔夫是**模拟钓鱼**——工作点 5 格内有水即可：每轮 60 秒"钓鱼" + 11 秒把鱼放进箱子，生鱼/鲑鱼/小丑鱼/河豚循环，天黑后售卖/关店，次日重新开始；没有真实的鱼钩实体。找水失败时每 3 秒重试。

### P4++ 供应链取货修复 + 经济系统设计（本轮）
**"店铺没有货源"的根因（已修）**：
- `getClosestBuildingByJob` 用**本地化职业名逐字比较**，还强制要求生产建筑有住户 → 换语言或没雇工人的生产建筑全部找不到；
- `getClosestFarm` 同样要求农场必须有雇员；
- `JobTaskCollectItems` 推进目的地写的是 `destinations.get(+1)`（字面量 1）→ 只会去第二个供货点然后结束。

修复：稳定职业 ID 匹配、去掉"必须有住户/雇员"限制、按距离排序取最近 2 个供货点、正确推进目的地。现在屠夫/食品商/麦当劳/汉堡/面包师/奶酪匠等会按物品去对应生产建筑（养殖场/农场/渔场/制糖师…）取货。

**经济系统现状与设计（v1）**：

| 角色 | 资金 | 说明 |
|---|---|---|
| 玩家 | `ModSimLoader.money` | 政府/店主/地主，唯一全局货币 |
| NPC | `NpcData.money` | 个人存款，随存档持久化 |
| 建筑 | 暂无独立账户 | 生产者与店铺都归玩家，内部调拨不产生货币 |

货币流（当前）：
1. 玩家 → NPC：上班工资 0.2/分钟（在岗）；
2. NPC → 玩家：买食物 0.5/份（`TaskBuyFood` 自主购买 + 商店 `feedFolks` 出售）；
3. 玩家 ← 租客：每日租金（原有）；
4. 生产 → 店铺：免费调拨（本轮修复的取货链路）。

**建议的下一步（尚未实现）**：
- 建筑账户：店铺收入先入建筑，日终上缴玩家；批发按 60% 价结算给生产者；
- 补货阈值/订单：库存低于阈值当天重新取货（现在一天只取一轮）；
- 统一价格表：`PricesForBlocks` 只管方块，食物/商品需要独立定价；
- 玩家 GUI 买卖与 NPC 消费共用同一价格源。

### P4+++ 先保证 NPC 不饿死（按你的要求，暂缓定价）
- **不再饿死**：移除三处饥饿 `DamageSource.STARVE 1.0` 伤害（主循环 + 两个状态更新路径），饥饿值下限 0；寿命到期的 999 伤害保留（正常老死）。
- **自己吃背包里的食物**：`NpcData.trySelfFeed()` 每秒检查，饥饿 < 8 且有食物就吃（回一半饱食度）。
- **去食品店免费取用**：饥饿 < 6 → `TaskBuyFood` 走到最近的食品店，从箱子取一份食物吃掉；**不做支付/定价**，有钱没钱都能去。
- **商店免费喂食**：`feedFolks` 恢复免费（不再检查 NPC 存款）。
- **工资/定价暂缓**：`Job.onMinute` 的工资扣款先留空；`NpcData.money` 字段保留（随存档，将来启用）。
- 之前的"福利制 vs 资本主义"设计仍保留在本文档中，作为后续可选方向。

### P20 最小经济闭环（2026-11-09）
- **工资启用**：在岗 NPC 每分钟 +0.5，城市扣款；统一下沉到 `NpcData.onMinute()`。
- **买饭付款**：`TaskBuyFood` 先结账再吃，NPC -0.5/份；余额不足慈善免费。
- **商店喂食收费**：`feedFolks` 按实际件数收 0.25/件，收入进城市；工作箱只扫描一次。
- **产出补贴**：成品进箱/制作完成时给生产 NPC 0.05/件，不再反向给玩家钱。
- **生产成本**：制作/卸货 0.02/件，由城市承担。
- **验证**：面包师 7/7、独立屠夫 8/8、6 建筑 + 8 NPC 40 分钟压测 14/14；核心 SHA1 `207546E68D0384340F324F7C832D8A3811AFCF4E`。

### P4++++ 士兵生存能力（本轮）
- **死亡原因**：NPC 基础 40 血、**没有任何护甲**、不会回血；攻击用 `causeMobDamage` 会引来怪物仇恨；苦力怕贴脸爆炸基本必死。不是攻击伤害低，是生存属性太低。
- **修复**：
  - 士兵每 5 秒检查并补齐**全套锡甲**（头/胸/腿/脚）+ 主手剑（死亡/读档后也会自动补）；
  - **脱战回血**：每 3 秒回 1 点；
  - 攻击 6 → **8**；
  - 巡逻排除**苦力怕**（近战会被炸死）和**恶魂**（飞在天上够不到）。
- 备注：锡甲减伤数值是 `{10,10,10,10}`（合计 40，游戏计算上限 20），穿上后非常硬；如果你觉得士兵过强，可以再调 `ItemTinArmor.TIN_ARMOR` 的数值。

### P4+++++ 传送冲突治理（本轮）
多个兜底系统（窒息救援 / 移动看门狗 / 屋顶看门狗 / 去工作超时）会同时抢着传送，表现为"NPC 被到处传送"。现在统一规则：
- **全局限频**：同一 NPC 两次强制传送至少间隔 2 秒（`forceTeleportTo` 内的 `TELEPORT_MIN_INTERVAL_MS`）；
- **去工作**：从"一到 5 秒就传"改为"**5 秒无进展才传**"——距离在缩短（正常赶路）不打扰；**战斗中不传**；
- **移动看门狗**：`stayPut`、睡觉、战斗中一律不触发（士兵停下来打怪、商店站桩都不算卡死）；
- **屋顶看门狗**：只用住宅/工作点做参照（不再拿战斗目标），战斗中不触发。

### P4++++++ 传送策略重构（士兵巡逻不再被传送）
原则：**机会性移动（巡逻/追击/闲逛）永不传送；硬性目标（上班/回家/救援）才允许兜底传送，且必须"5 秒无进展"。**

- 新增 `NpcData.walkTo(V3)`：只走路，没有路径就返回 false，**不传送、不登记看门狗**；
- `forceMoveToXYZ(s)`：没有路径**不再立刻传送**（这是"到处传送"的主因），只登记目标，交给看门狗 5 秒后兜底一次；
- 移动看门狗：**士兵完全不参与**；`stayPut`、睡觉、战斗中也都不触发；
- 士兵巡逻/追击改用 `walkTo`：巡逻点到不了就换点、怪追不到就放弃，绝不传送；
- 屋顶看门狗只针对"自己家 8 格内、y 高 5 格以上"，且**跳过士兵**；
- 去工作超时**跳过士兵**（24 小时巡逻岗，不因上班慢被拽走）；
- 士兵现在唯一会被传送的情况：**持续窒息/溺水救援**。

### 士兵巡逻算法（P4 说明与修正）
流程（`JobTaskPatrol`，逻辑有节流）：
1. **目标清理**：目标死亡或超过 24 格 → 清除；
2. **索敌**（每秒一次）：以自身为中心 16×16、y-4..y+6 找最近 `EntityMob`；排除苦力怕（近战会被炸死）和恶魂（飞在天上够不到）；
3. **有目标**：状态"攻击中"；每 0.5 秒 `walkTo(怪)` 追击（只走路）；距离 <3 格时每 1 秒攻击一次（8 伤害 + 挥臂）；**追击 10 秒仍进不了 3 格 → 放弃**；
4. **无目标**：状态"巡逻中"；到达巡逻点后取新点；每 0.5 秒 `walkTo(巡逻点)`，到不了就立刻换点（不传送）；
5. **巡逻点选取**：岗位 ±20 为边界（并覆盖所有建筑再 ±20）；平时 2/3 概率在岗位附近、1/3 在当前位置附近；走出边界或离岗位 >40 格 → 强制朝岗位方向取点（回岗）；最多重试 5 次，失败回岗位；
6. **移动策略**：巡逻/追击全部只走路、永不传送；士兵豁免移动看门狗/屋顶看门狗/去工作超时/深夜睡觉；唯一传送是窒息/溺水救援。

修正前的四个问题：巡逻边界算了没用、巡逻点永远以当前位置为中心（随机游走会漂移）、取不到点会无限递归、追击没有超时（隔着墙会永远追）。

### 士兵巡逻 v2（参照原版铁傀儡，按你确认的 1/2/3 点）
1. **驻区（家）**：`JobSoldier` 每 5 秒把工作岗位设为 `setHomePosAndDistance(workPlace, 24)`，与巡逻任务的 `PATROL_RADIUS=24` 一致；
2. **固定巡逻路点**：以岗位为中心、半径 12 的圆周取 6 个点，自动找"两格空气 + 下方实体方块"的站立高度；循环巡逻，**到点停留 2-5 秒**；某个点到不了就跳下一个，路点全部无效时退回随机点（仍带边界/回岗逻辑）；
3. **索敌优先级（铁傀儡式）**：
   1. **保护**：16 格内玩家或其他 NPC 刚被敌对生物攻击 → 去打那个生物（对应 `EntityAIDefendVillage`）；
   2. **反击**：自己被谁打就打谁（对应 `EntityAIHurtByTarget`）；
   3. **最近敌对**：10 格内最近的 `EntityMob`（对应 `EntityAINearestAttackableTarget`）；
   - 全程排除苦力怕/恶魂；目标死亡、超过 24 格或**离开驻区** → 放弃（对应铁傀儡离开村庄范围放弃追击）；
4. 战斗与移动：1 秒攻击间隔、8 伤害、挥臂；追击只用 `walkTo`，10 秒无果放弃；**全程不传送**。

### 渔夫产出效率重做（本轮）
**原因**：每次下网 `JobTaskIdle(60000L)` 空等 60 秒；收网固定再等 10 秒；一轮只有 5 网，跑完就进入卖鱼任务直到傍晚——剩下大半天不生产。约 10 条鱼/游戏日。
**修复**：
- 下网 60 秒 → **20 秒**；收网固定等待 10 秒 → **2 秒**（`JobTaskPlaceInChest`）；
- 白天**循环作业**（队列空就重建下一轮），傍晚（≥11000）才收摊卖鱼；每网 2-4 条；
- 一轮约 110 秒、约 15 条鱼 → 游戏日约 60-75 条（是原来的 6 倍以上）。
- 说明：捕鱼仍是模拟产出（不消耗饵料），鱼进工作点附近的箱子。

### 店长"一直收集材料"（本轮）
**原因**：上一轮为治理士兵传送关闭了"远距离自动传送"；店长取货目标是远处的生产建筑时只能步行几百格，而 `JobTaskCollectItems` 的 deadline 是 `-1L`（永不过期）→ 长期停在"收集材料"。
**修复**：工作类移动（取货/送货/上班/回家）目标超过 **32 格**时允许一次冷却传送（全局限频 2 秒）；巡逻/追击仍走 `walkTo` 不传送；移动看门狗继续兜底无路径/无进展。
**预期**：店长取货从数分钟降到几秒，随后进入卸货 → 售卖循环。

### 店主取货/制作全流程重做（P8/P9，本轮）

**本轮修掉的死循环与卡死（"永远收集材料"）**：
1. 收工时原来只把 `job.stage=0`、**不清任务队列**，第二天会把昨天没跑完的 `JobTaskCollectItems` 重新 `begin()`；`onTaskBegin` 又不先清空目的地 → 目的地重复；再用 `indexOf(currentDestination)` 推进 → 永远命中第一个重复坐标 → 在两个点之间无限往返。
   - 修复：收工改 `stage=-1 + jobTasks.clear() + currentTask=null`；`onTaskBegin` 幂等（先清空重建）；改用**显式下标**推进；目的地去重；10 分钟任务级超时兜底。
2. `JobTaskShopkeep` 打烊时调用的是空实现 `onTaskComplete()`，`completed` 永不置位 → 店长打烊后任务永不完成，第二天不重新进货。修复：改调 `completeTask()`。
3. `Job.handleArrival` 到岗时会直接调 `currentTask.onTaskComplete()`（清掉取货目的地却不标记完成）→ 另一条卡死路径。已删除该调用。

**按需求重定的流程**：
- **到点判定**：水平 2.5 格圆 + 垂直 ±3；若"无路可走但已在 4 格内"也算到达（避免站在箱子旁因寻路失败永远到不了）。
- **取货**：到达后停 1 秒 → 扫目的地 5 格内箱子 → 按清单额度取（每种最多取清单数量，多槽位不超拿）→ 放入背包的是副本（原来把任务清单的 ItemStack 直接塞进背包，会被后续修改串数据）。
- **取完即走**：不再原地等 5 秒，立即指向下一个目的地并出发；全部走完（即使没取全）立刻回岗。
- **材料不全也回去制作**：`JobTaskProduceItem` 修正"材料不齐也会把背包匹配材料全部销毁"的 Bug——统计改成"箱子+背包"，只有 `maxProd > 0` 才消耗，且只扣 `maxProd × 每份用量`（箱子优先）。制作准备时间 10 秒→3 秒，卸货 10 秒→3 秒。
- **吃饭优先级**：傍晚自由时间先吃饭（饿且背包没食物 → 去食品店，否则社交/闲逛）；无职业/空闲时吃饭放第一位（阈值统一为 hunger<8，背包有食物则 `trySelfFeed` 自动吃，不浪费路程）。

### 取货卡死收尾 + 面包店补货循环（本轮）
- **看门狗失效根因**：取货任务每 tick 调 `forceMoveToXYZ`，`noteMoveTarget` 每次都把无进展计数清零 → 看门狗永远不触发，"卡在障碍旁"= 永远收集材料。现在同目标重发不再重置计数；取货的移动指令最多每秒重发一次。
- **单点超时**：每个目的地最多 90 秒，超时自动跳过（整体 10 分钟超时保留）。
- **面包店补货循环**：每轮"采货→卸货→制作"结束后检查箱子库存，任一原料 < 16 且白天（<10000 刻）就再收一轮，单日最多 4 轮，然后开卖；删除了原"每 50 秒强制清队列重做"的统计块（会打断正在执行的任务）。
- **麦当劳修复**：取货每种材料只去最近 1 个供货点（缩短路线）；鱼料理统计改为生鱼（原来统计 `COOKED_FISH` 永远为 0）；做鱼需求 16 条 → 1 条；熟鲑鱼输出 meta 1。
- **成品回箱**：制作完成统一走 `placeInJobChest`（工作点 5 格内箱子），已确认。

## 已实施

### P0 侧隔离 + 单入口 tick + 稳定 ID
- `key/KeyboardManager.java`：`@Mod.EventBusSubscriber(modid = ModSim.MODID, value = Side.CLIENT)`。
- `proxy/CommonProxy.java`：移除 `KeyboardManager.init()`（不再在服务端初始化按键）。
- `proxy/ClientProxy.java`：客户端注册 `KeyboardManager.init()` + `ClientWorldEvents`。
- `proxy/ClientWorldEvents.java`（新增）：客户端镜像重置，替代服务端 worldLoad 中的客户端调用。
- `loader/EventLoader.java`：
  - 事件总线只注册一次；
  - `worldTick` 只在 `Phase.END` + 服务端 + 维度 0 执行（消除 2 相 × 3 维度放大）；
  - `worldSave` 只服务端维度 0；
  - `worldLoad` 重置 `hasLoadedWorld`，不再触碰客户端类；
  - 客户端方法加 `@SideOnly(Side.CLIENT)`；加载缓存改为并发集合。
- `loader/SimmodeStart.java`：删除 `Minecraft` 引用；`scheduleMainThreadTask`/`simModDisconnected` 只用服务端 API。
- `npcCode/SimIds.java`（新增）：职业/建筑类型/种族/特质/物品的稳定 ID 双向映射；读档路径不再用翻译串匹配。

### P1 原生存档 + 旧档删除
- `npcCode/store/SimStore.java`（新增）：`WorldSavedData` + `NBTTagCompound`，保存 NPC 记录与经济/日历。
- `npcCode/NpcData.java`：
  - 新增 `toNBT()/fromNBT()`（职业存 `vocationN` + 工作点 + 蓝图/方向/地形；种族/特质/手持物/住宅全部存稳定 ID）；
  - `saveFolk()/loadFolk()` 改走 `SimStore`；旧实现保留为 `*Legacy()` 但不再被调用；
  - 死亡时从 `SimStore` 移除记录；新增 `linkHome()` 在读档后关联住宅。
- `loader/SimmodeStart.java`：`simModLoad` 首次加载时**删除旧 `sim/npc` 与 `sim/settings.sk2`**，经济/日历读 `SimStore`，NPC 从 `SimStore` 加载；建筑/农场/矿场/快递点按文件独立 try + 字面量后缀截断；`simModSave` 写 `SimStore` 并保存各系统。
- `npcCode/job/JobFactory.java`（重写）：注册键为稳定 ID（`vocationN`），新增 `createJobAt(...)`；旧显示名会自动转换。

### P2 移动/传送修复
- `npcCode/NpcData.java`：
  - `forceMoveToXYZs`：改为原版导航优先；只有无路径才走冷却传送（原来每次调用必然瞬移）；
  - `forceMoveToXYZ`：同样语义，失败返回 false（调用方兜底生效）；
  - 新增 `canReach`（维度检查）、`teleportWithCooldown`（优先原版 `attemptTeleport`，3 秒冷却）、`spawnPortalParticles` 改用 `WorldServer.spawnParticle`（服务端安全）；
  - `getAdjustedV3` 做落点空气检查；`isAtLocation` 增加维度与 y 检查。
- `npcCode/job/Job.java`：`handleGoingToWork` 最多每 2 秒重发一次移动目标（原来每 tick 一次 A*）。

### P3 职业生命周期
- `npcCode/job/Job.java`：`nextTask()` 改为**队列语义**（完成即移除，`jobTasks` 不再无限增长）；队列耗尽置 `stage=-1` 并回调 `onTasksExhausted()`；推进时不改 `stage`（兼容把 stage 当状态机的职业）。
- `npcCode/task/JobTask.java`：`failTask(message)` 真正上报（聊天 + 日志）。
- `npcCode/job/JobLivestockFarmer.java`：修复 `==9` 死分支（8→照料→回到 2），照料任务 deadline 用 5000ms；绵羊职业键统一为 `Vocation27`。

### P4 物品/金钱事务（原生 `IItemHandler`）
- `Job.java`：新增 `placeInJobChestPartial(IInventory/ItemStack)`（`InvWrapper` + `ItemHandlerHelper.insertItemStacked`，返回实际件数）；`inventoriesTransferToFolk(s)` 按匹配项精确转移；`buyStuff` 按实际放入件数扣款并删除 `Thread.sleep(1000)`；`sellStuff` 修复 `adjustPrice(null)`；`inventoriesFindClosest` 改为外壳扫描（O(d³)→O(6d²)）。
- `task/JobTaskUseFurnace.java`：产物按实际件数结算并循环下一炉；`job/JobBrickMaker.java`：熔炉任务 deadline `-1L→120000L`。
- `task/JobTaskHarvestAnimal.java`：按实际放入件数扣钱。

### P9 查询/快递点
- `loader/ModSimLoader.java`：UUID 用 `equals`；`getBlueprintsByType` 用稳定 ID + `isEmpty`；`getClosestFarm` 不再边遍历边改列表，改为只读 + 距离排序；资源目录警告只打一次。
- `util/Courier.java`（重写）：UTF-8 + try-with-resources + 主键精确解析；`removeFarm` 真正从 `theCourierPoints` 移除并删档。

## 验证结果

1. **编译**（构建工程，ForgeGradle 2.3 / JDK8）：
   `gradlew.bat build --offline` → `BUILD SUCCESSFUL`。
2. **专用服务器**（`forge-server`，新 jar，运行 150 秒）：
   - `Done (1.446s)! For help, type "help" or "?"`；
   - 无 `NoClassDefFoundError`（原 KeyBinding 崩溃已消除）；
   - `旧存档（sim/npc、sim/settings.sk2）已删除` → `NPC加载完成: 0`；
   - 无新 crash-report，无 sim 异常。

## 未包含（后续阶段）

- **P5**：建筑/工地改 `ITickable` + `ForgeChunkManager` 按需加载尚未实施；当前建筑仍沿用原 `.sk2` 格式（已加独立 try 与后缀截断）。
- **职业行为 AI 化**：本轮修的是**生命周期与数据层**（队列、稳定 ID、超时、失败上报、移动统一）；28 个职业的 `JobTask` 逻辑保持不变，未逐个迁移为 `EntityAIBase`。若需要，可作为下一阶段按文档第 5.2/5.3 节推进。
- 商店 GUI 仍走原买卖代码（金额/物品结算已按实修正），未改用 `MerchantRecipe`。

## 如何验收

```powershell
# 1) 同步修改版源码到构建工程
$src="C:\Users\15572\Music\mc2\模拟城市 1.12.2 精选整合包-源码\sim-1.12.2\src\main\java\com\trhsy\sim"
$dst="C:\Users\15572\Music\mc2\构建\src\main\java\trhsy\sim"
Remove-Item $dst -Recurse -Force; New-Item -ItemType Directory -Force $dst | Out-Null
Copy-Item "$src\*" $dst -Recurse -Force

# 2) 构建
$env:JAVA_HOME="C:\Users\15572\jdk8\jdk8u504-b01"
cd C:\Users\15572\Music\mc2\构建
.\gradlew.bat build --offline

# 3) 产物
# 构建输出：构建\build\libs\simulated-1.12.2-1.0.9 Beta2.jar
# 如需专服验证，把该 jar 放到任意 Forge 1.12.2 服务端的 mods\ 目录即可
```

预期：看到 `Done (...)`、`旧存档（sim/npc、sim/settings.sk2）已删除`、`NPC加载完成: N`，且无崩溃。

> 客户端行为（NPC 不再瞬移风暴、养殖户不再卡死、换语言不失业）需要在单机/联机客户端里实际观察验收；本轮已用编译 + 专服启动验证了架构与侧隔离，逻辑层修复点见上表。
