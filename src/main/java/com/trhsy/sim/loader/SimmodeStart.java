package com.trhsy.sim.loader;

import com.trhsy.sim.network.client.PacketReturnHireableFolks;
import com.trhsy.sim.network.client.PacketUpdateMoney;
import com.trhsy.sim.network.client.PacketUpdateNPC;
import com.trhsy.sim.network.server.PacketSyncNpcData;
import com.trhsy.sim.npcCode.NpcData;
import com.trhsy.sim.npcCode.store.SimStore;
import com.trhsy.sim.npcCode.block.FarmBox;
import com.trhsy.sim.npcCode.block.MineBox;
import com.trhsy.sim.npcCode.build.Building;
import com.trhsy.sim.util.Courier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.loader
 * @ClassName: SimmodeStart
 * @Description: 启动
 * @date 2024/10/25 14:20
 */
public class SimmodeStart {
    /**
     * 已加载世界
     **/
    public static boolean hasLoadedWorld = false;
    //新的一天
    public static boolean newDay = true;
    //新的一天是否收租
    public static boolean newDayRentals = true;
    /**
     * 上次可以户连接的时间
     **/
    public static long timeSinceLastClientUpdate = 0L;
    public static long timeSinceLastClientUpdates = 0L;

    /**
     * 分钟计时器
     **/
    public static long minuteTimer = System.currentTimeMillis();
    // P12(S1): 用世界绝对天数检测跨天，兼容 /time set；不再只依赖 newDay 布尔边沿。
    private static long lastWorldDay = -1L;
    //收租计时
    public static long rentalsTimer = System.currentTimeMillis();


    private static ExecutorService executorService = Executors.newCachedThreadPool();


    // 性能优化参数
    private static final int RENT_COLLECTION_DELAY = 60;         // 收租延迟 (3秒)
    private static final int NPCS_PER_UPDATE = 5;                // 每更新批次处理的NPC数量
    private static final int MONEY_UPDATE_INTERVAL = 5000;       // 资金同步间隔(毫秒)
    private static final int HIREABLE_FOLKS_UPDATE_INTERVAL = 5000; // 可雇佣NPC同步间隔(毫秒)

    // 计时器和状态变量
    private static long lastUpdateTime = 0;
    private static long lastMoneyUpdateTime = 0;
    private static long lastHireableUpdateTime = 0;
    private static int currentNpcBatch = 0;
    private static long currentNpcIndex = 0; // 当前处理的NPC批次索引
    private static float cachedTotalRent = 0.0F;
    private static boolean rentCacheDirty = true;
    // 新增：NPC住宅检查与新NPC生成的计时器
    private static long npcSpawnCheckTimer = 0;
    private static final long NPC_SPAWN_CHECK_INTERVAL = 60000L; // 60秒检查一次

    // 性能优化参数
    private static final int SERVER_UPDATE_INTERVAL = 100;         // 服务器更新间隔 (5秒，20ticks/秒)
    private static final int CLIENT_UPDATE_INTERVAL = 20;          // 客户端更新间隔 (1秒)

    // 计时器和状态变量
    private static long lastServerUpdateTime = 0;
    private static long lastClientUpdateTime = 0;

    // 客户端NPC缓存 (线程安全)
    private static Map<String, NpcData> clientNpcCache = new ConcurrentHashMap<>();
    // 任务调度配置
    private static final int MAX_CONCURRENT_TASKS = 4; // 最大并发任务数
    private static final int TASK_QUEUE_CAPACITY = 100; // 任务队列容量
    private static final int UPDATE_INTERVAL_TICKS = 5; // 主更新间隔(5 ticks = 0.25秒)
    // 任务队列和线程池
//    private static final BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>(TASK_QUEUE_CAPACITY);
    // 计时器和状态变量
    private static int tickCounter = 0;
    private static boolean isProcessing = false;
    private static final Map<Integer, NpcData> pendingNpcUpdates = new ConcurrentHashMap<>();
    private static final Map<String, Building> pendingBuildingUpdates = new ConcurrentHashMap<>();

    /*private static final ExecutorService asyncExecutor = Executors.newFixedThreadPool(
            MAX_CONCURRENT_TASKS,
            new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "SimMod-Task-" + threadNumber.getAndIncrement());
                }
            }
    );*/
    /**
     * 加载模组
     */
    public static void simModLoad(World world) {
        ModSimLoader.log.info("检查是否应该加载人员");
        if (world == null || world.isRemote || world.provider.getDimension() != 0) {
            return;
        }
        if (hasLoadedWorld) {
            ModSimLoader.log.info("sim世界已加载");
            return;
        }
        ModSimLoader.log.info("世界尚未加载，开始加载");
        newDay = true;
        timeSinceLastClientUpdate = 0L;

        // P1: 旧存档直接删除（本版本不兼容旧档）。只删旧 NPC 与旧 settings，
        // 其他目录（建筑/农场/矿场/快递点）保留并继续用各自格式。
        deleteLegacySaves();

        // P1: 经济与日历来自 SimStore（NBT，不再用 settings.sk2）
        SimStore store = SimStore.get(world);
        if (store != null) {
            ModSimLoader.money = store.money;
            ModSimLoader.dayOfWeek = store.dayOfWeek;
            ModSimLoader.gameDay = store.gameDay;
            ModSimLoader.gamemode = store.gamemode;
        }

        // 农场 / 矿场 / 建筑 / 快递点：每个文件独立 try，字面量后缀截断
        loadFarmFiles();
        loadMineFiles();
        loadBuildingFiles(world);
        loadCourierFiles();

        // P1: NPC 从 SimStore 加载
        if (store != null) {
            int loaded = 0;
            for (UUID uid : new ArrayList<>(store.getNpcs().keySet())) {
                try {
                    NpcData npcData = new NpcData(world, uid);
                    // P10(S0): 区块未加载的 NPC 也保留在数据列表里（实体延迟生成），
                    // 不能因为 entity==null 就把存档里的 NPC 丢掉。
                    if (npcData.isLoaded && !npcData.isDead) {
                        ModSimLoader.folks.add(npcData);
                        loaded++;
                    }
                } catch (Exception e) {
                    ModSimLoader.log.error("NPC加载失败 " + uid + ": " + e.getMessage());
                }
            }
            ModSimLoader.log.info("NPC加载完成: " + loaded);
            // 建筑加载完成后关联住宅
            for (NpcData npc : ModSimLoader.folks) {
                npc.linkHome();
            }
        }
        NetWorkLoader.net.sendToAll(new PacketUpdateNPC());
        NetWorkLoader.net.sendToAll(new PacketUpdateMoney());
        hasLoadedWorld = true;
    }

    /** P1: 删除旧 NPC 与旧 settings 存档（不解析、不迁移）。 */
    private static void deleteLegacySaves() {
        try {
            File sim = new File(ModSimLoader.getSavesDataFolder());
            deleteRecursively(new File(sim, "npc"));
            File settings = new File(sim, "settings.sk2");
            if (settings.exists() && !settings.delete()) {
                ModSimLoader.log.warn("旧 settings.sk2 删除失败: " + settings.getAbsolutePath());
            }
            ModSimLoader.log.info("旧存档（sim/npc、sim/settings.sk2）已删除");
        } catch (Exception e) {
            ModSimLoader.log.error("删除旧存档失败: " + e.getMessage());
        }
    }

    private static void deleteRecursively(File f) {
        if (f == null || !f.exists()) {
            return;
        }
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children) {
                    deleteRecursively(c);
                }
            }
        }
        if (!f.delete()) {
            ModSimLoader.log.warn("无法删除: " + f.getAbsolutePath());
        }
    }

    /** 只接受 "<UUID>.sk2" 且按字面量截断（原实现 split(".sk2") 是正则）。 */
    private static UUID uidOf(File f) {
        if (f == null || !f.isFile()) {
            return null;
        }
        String name = f.getName();
        if (!name.endsWith(".sk2")) {
            return null;
        }
        try {
            return UUID.fromString(name.substring(0, name.length() - 4));
        } catch (Exception e) {
            return null;
        }
    }

    private static void loadFarmFiles() {
        File folder = new File(ModSimLoader.getSavesDataFolder(), "farms");
        if (!folder.exists() && !folder.mkdirs()) {
            return;
        }
        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            UUID uid = uidOf(f);
            if (uid == null) {
                continue;
            }
            try {
                ModSimLoader.farms.add(new FarmBox(uid));
            } catch (Exception e) {
                ModSimLoader.log.error("农场加载失败 " + f.getName() + ": " + e.getMessage());
            }
        }
    }

    private static void loadMineFiles() {
        File folder = new File(ModSimLoader.getSavesDataFolder(), "mines");
        if (!folder.exists() && !folder.mkdirs()) {
            return;
        }
        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            UUID uid = uidOf(f);
            if (uid == null) {
                continue;
            }
            try {
                ModSimLoader.mines.add(new MineBox(uid));
            } catch (Exception e) {
                ModSimLoader.log.error("矿场加载失败 " + f.getName() + ": " + e.getMessage());
            }
        }
    }

    private static void loadBuildingFiles(World world) {
        File folder = new File(ModSimLoader.getSavesDataFolder(), "buildings");
        if (!folder.exists() && !folder.mkdirs()) {
            return;
        }
        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            UUID uid = uidOf(f);
            if (uid == null) {
                continue;
            }
            try {
                ModSimLoader.buildings.add(new Building(world, uid));
            } catch (Exception e) {
                ModSimLoader.log.error("建筑加载失败 " + f.getName() + ": " + e.getMessage());
            }
        }
    }

    private static void loadCourierFiles() {
        File folder = new File(ModSimLoader.getSavesDataFolder(), "CourierPoints");
        if (!folder.exists() && !folder.mkdirs()) {
            return;
        }
        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            String name = f.getName();
            if (!f.isFile() || !name.endsWith(".sk2")) {
                continue;
            }
            try {
                ModSimLoader.theCourierPoints.add(new Courier(name.substring(0, name.length() - 4)));
            } catch (Exception e) {
                ModSimLoader.log.error("快递点加载失败 " + name + ": " + e.getMessage());
            }
        }
    }

    /**
     * 保存
     */
    public static void simModSave(World world) {
        if (world == null || world.isRemote || world.provider.getDimension() != 0) {
            return;
        }
        if (!hasLoadedWorld || !ModSimLoader.sim_is_running) {
            return;
        }
        // P1: 经济/日历写入 SimStore（NBT）
        SimStore store = SimStore.get(world);
        if (store != null) {
            store.money = ModSimLoader.money;
            store.dayOfWeek = ModSimLoader.dayOfWeek;
            store.gameDay = ModSimLoader.gameDay;
            store.gamemode = ModSimLoader.gamemode;
            store.markDirty();
        }
        // 农场/矿场/NPC/建筑保存（NPC 现在是 NBT）
        for (FarmBox farmBox : ModSimLoader.farms) {
            farmBox.saveFarm();
        }
        for (MineBox mineBox : ModSimLoader.mines) {
            mineBox.saveMine();
        }
        for (NpcData folks : ModSimLoader.folks) {
            folks.saveFolk();
        }
        for (Building b : ModSimLoader.buildings) {
            b.saveBuilding();
        }
    }

    /**
     * 更新
     */
    public static void simModupdate(World world) {
//        World worlds =  Minecraft.getMinecraft().world;
        //第三版
/*
        // 频率控制
        tickCounter++;
        if (tickCounter % UPDATE_INTERVAL_TICKS != 0) {
            return;
        }
        // 检查是否已有任务在处理中
        if (isProcessing) {
            return;
        }

        // 当前世界有玩家才执行
        if (world.playerEntities.isEmpty()) {
            return;
        }

        // 标记开始处理
        isProcessing = true;

        // 1. 异步处理NPC状态更新
        processNpcsAsync(world);

        // 2. 异步处理建筑和租金
        processBuildingsAsync(world);

        // 3. 处理昼夜循环和环境控制
        processEnvironmentAsync(world);

        // 4. 同步网络数据
        scheduleNetworkSync(world);
*/
        //第二版
        /*// 检查是否需要整体更新
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime < UPDATE_INTERVAL_TICKS * 50) { // 50ms = 1tick
            return;
        }

        lastUpdateTime = currentTime;

        // 确保世界和玩家存在
        if (world == null || world.playerEntities.isEmpty()) {
            return;
        }

        ModSimLoader.sim_is_running = true;

        // =============== 分批处理NPC更新 ===============
        List<NpcData> folks = new ArrayList<>(ModSimLoader.folks);
        int totalBatches = Math.max(1, (int) Math.ceil(folks.size() / (double) NPCS_PER_UPDATE));
        // 计算当前批次的起止索引
        int startIndex = currentNpcBatch * NPCS_PER_UPDATE;
        int endIndex = Math.min(startIndex + NPCS_PER_UPDATE, folks.size());

        for (int i = (int)startIndex; i < endIndex; i++) {
            NpcData f = folks.get(i);
            if (f != null && f.entity != null && !f.entity.isDead) {
                f.onUpdate();
            }
        }
        // 更新批次索引
        currentNpcBatch = (currentNpcBatch + 1) % totalBatches;

        // =============== 定期同步网络数据 ===============
        //
        if (currentTime - lastMoneyUpdateTime > MONEY_UPDATE_INTERVAL) {
            lastMoneyUpdateTime = currentTime;
            NetWorkLoader.net.sendToAll(new PacketUpdateMoney());
        }

        if (currentTime - lastHireableUpdateTime > HIREABLE_FOLKS_UPDATE_INTERVAL) {
            lastHireableUpdateTime = currentTime;
            NetWorkLoader.net.sendToAll(new PacketReturnHireableFolks());
        }
        // =============== 白天逻辑处理 ===============
        if (ModSimLoader.isDayTime(world)) {
            // 新的一天初始化
            if (!newDay) {
                newDay = true;
                handleNewDay(world);
            }
            if(System.currentTimeMillis() - npcSpawnCheckTimer > NPC_SPAWN_CHECK_INTERVAL){
                checkAndSpawnNewNpc(world);
                npcSpawnCheckTimer = System.currentTimeMillis(); // 重置计时器
            }
            // 收租逻辑 (使用缓存值)
            if (currentTime - rentalsTimer > RENT_COLLECTION_DELAY * 50 && newDayRentals) {
                newDayRentals = false;
                collectRent(world);
            }

            // 检查饥饿和年龄 (分批处理)
            checkNpcStatus(world, startIndex, endIndex);
        }
        // 黑夜逻辑
        else if (newDay) {
            newDay = false;
        }
        // =============== 环境控制 ===============
        if (ConfigLoader.configStopRain && world.isRaining() && world.getWorldInfo().getRainTime() > 1) {
            world.getWorldInfo().setRaining(false);
            ModSimLoader.log.info("停止下雨");
            ModSimLoader.sendChat(new TextComponentTranslation("chat.sim.xiayu", new Object[0]).getUnformattedText());
        }*/


        //第一版

        long startTime = System.currentTimeMillis();
        if (world != null) {
            if (startTime - timeSinceLastClientUpdates > 15000L ) {
                if(!hasLoadedWorld&&!ModSimLoader.sim_is_running){
                    simModLoad(world);
                    ModSimLoader.sim_is_running=true;

                }
                //当前世界有玩家
                if (world.playerEntities.size() > 0&&ModSimLoader.sim_is_running) {

                    //实时更新人的状态
                    for (NpcData f : ModSimLoader.folks) {
                        // P10(S0): 玩家靠近、区块加载后才补生成实体；未加载则不推进任务。
                        f.trySpawnIfLoaded(world);
                        f.onUpdate();
                    }
                    //每1秒更新一次检查
                    if (System.currentTimeMillis() - timeSinceLastClientUpdate > 1000L) {
                        timeSinceLastClientUpdate = System.currentTimeMillis();
                        //可雇佣的人
                        NetWorkLoader.net.sendToAll(new PacketReturnHireableFolks());
                        //更新资金
                        NetWorkLoader.net.sendToAll(new PacketUpdateMoney());

                        if (ModSimLoader.gamemode != 999) {
                            // P12(S1): 绝对天数跨天检测。既处理正常日出，也处理 /time set。
                            long worldDay = world.getWorldTime() / 24000L;
                            if (lastWorldDay < 0L) {
                                lastWorldDay = worldDay;
                            }
                            if (worldDay > lastWorldDay) {
                                long delta = Math.min(7L, worldDay - lastWorldDay);
                                for (long d = 0; d < delta; d++) {
                                    handleNewDay(world);
                                }
                                lastWorldDay = worldDay;
                                newDay = true;
                            } else if (worldDay < lastWorldDay) {
                                // /time set 回退：只重新同步，不补算年龄/租金。
                                lastWorldDay = worldDay;
                            }
                            //是白天
                            if (ModSimLoader.isDayTime(world)) {
                                //60秒循环
                                if (System.currentTimeMillis() - minuteTimer > 60000L) {
                                    //已重生
                                    boolean spawnNew = true;
                                    for (NpcData starve : ModSimLoader.folks) {
                                        //判断是否已死亡
                                        // P10(S0): 不能把“区块未加载导致 entity==null”当成死亡，
                                        // 否则玩家一离开城镇，NPC 的住宅关系就被清空。
                                        if (starve.isDead) {
                                            //若有房子异常房子
                                            if (starve.home != null) {
                                                starve.home.occupants.remove(starve);
                                                starve.home = null;
                                            }
                                        } else if (starve.home == null) {
                                            spawnNew = false;
                                            //未成年不算
                                            if (starve.home != null && starve.race != null && starve.age > starve.race.maturity) {
                                                //只要有一个人没有住到房子里就不生成新的人
                                                spawnNew = true;
                                            }
                                        }
                                    }
                                    if (spawnNew && ModSimLoader.canGrowPopulation()) {
                                        ModSimLoader.log.info("所有人都有住宅，开始生成新的NPC");
                                        new NpcData(world, false);
                                    } else if (spawnNew) {
                                        ModSimLoader.log.info("已达到人口上限 {}，不生成新NPC", ConfigLoader.configPopulationLimit);
                                    }
                                    minuteTimer = System.currentTimeMillis();
                                }
                                if (System.currentTimeMillis() - rentalsTimer > 3000L && newDayRentals) {
                                    newDayRentals = false;
                                    ModSimLoader.log.info("收租了");
                                    if (ModSimLoader.gamemode != 999) {
                                        float rent = 0.0F;
                                        for (Building b : ModSimLoader.buildings) {
                                            if (b.occupants.size() > 0) {
                                                rent += b.rent;
                                            }
                                        }

                                        ModSimLoader.addMoney(rent);
                                        float rents = rent;
                                        //播放钱到账
//                                        SoundEvent cashSound = new SoundEvent(new ResourceLocation(ModSim.MODID + ":cash"));
                                        /*for (EntityPlayer entityPlayer : world.playerEntities) {
                                            BlockPos pos=entityPlayer.getPosition();
                                            ModSimLoader.log.info("播放钱到账:[x:" + pos.getX() + "],y:[" + pos.getY() + "],z:[" + pos.getZ() + "]");
//                                            world.playSound(null, pos, soundEvent, SoundCategory.BLOCKS, 1.0F, 1.0F);
                                            world.playSound( pos.getX(), pos.getY(), pos.getZ(), soundEvent, SoundCategory.PLAYERS, 1.0F, 1.0F,true);
                                        }*/
                                        SoundEvent cash = SoundRegistry.CASH;
                                        if (cash == null || cash.getRegistryName() == null) {
                                            ModSimLoader.log.error("播放失败：sim:cash 声音事件未注册");
                                        } else {
                                        for (EntityPlayer entityPlayer : world.playerEntities) {
                                            double x = entityPlayer.posX;
                                            double y = entityPlayer.posY;
                                            double z = entityPlayer.posZ;
                                            world.playSound(null,
                                                    x, y, z,
                                                    cash,
                                                    SoundCategory.PLAYERS,  // 与玩家交互相关的音效
                                                    1.0F,
                                                    1.0F
                                            );
                                        }}
                                        //你今天收了 今天的租金。
                                        ModSimLoader.sendChat(new TextComponentTranslation("container.sim.main_Collected", new Object[0]).getUnformattedText() + ModSimLoader.displayMoney(rents) + new TextComponentTranslation("container.sim.main_rent_today", new Object[0]).getUnformattedText());
                                        NetWorkLoader.net.sendToAll(new PacketUpdateMoney());
                                    }
                                    String hungerName = "";
                                    for (NpcData f : ModSimLoader.folks) {
                                        // P10(S0): 未加载区块的 NPC 暂停饥饿/年龄结算，
                                        // 与“实体不 tick 就不模拟”保持一致。
                                        if (!f.isActive()) {
                                            continue;
                                        }
                                        if (f.entity != null) {
                                            if (f.hunger > 0) {
                                                --f.hunger;
                                            } else {
                                                // P4: 先不做定价/饿死——饿到 0 也不死，只记录挨饿的人
                                                f.hunger = 0;
                                                hungerName += f.getName() + ",";
                                            }

                                        }
                                    }

                                    if (hungerName != "") {
                                        //快饿死了！你应该建立一个农场，杂货店，面包店或向他们扔一些食物。
                                        String starving = new TextComponentTranslation("container.sim.main_is_VERY", new Object[0]).getUnformattedText();
                                        //其他人正在挨饿！你应该建立一个农场，杂货店，面包店或向他们扔一些食物。
                                        String others_starving = new TextComponentTranslation("container.sim.others_starving", new Object[0]).getUnformattedText();
                                        String message = hungerName + starving;
                                        ModSimLoader.sendChat(message);
                                    }


                                }

                            } else if (newDay) {
                                newDay = false;
                            }
                        }

                        //停止下雨MOD-在我的世界里一直下雨的时候实现了这个！
                        if (world != null) {
                            if (world.isRaining() && world.getWorldInfo().getRainTime() > 1 && ConfigLoader.configStopRain) {
                                world.getWorldInfo().setRaining(false);
                                ModSimLoader.log.info("我讨厌下雨-停了吧");
                                ModSimLoader.sendChat(new TextComponentTranslation("chat.sim.xiayu", new Object[0]).getUnformattedText());
                            }
                        }
                    }
                }

            }
        }


    }
    // 异步处理NPC状态
    private static void processNpcsAsync(World world) {
//        asyncExecutor.submit(() -> {
            try {
                Map<Integer, NpcData> updatedNpcs = new HashMap<>();
                /*List<NpcData> folks = new ArrayList<>(ModSimLoader.folks);


                // 分批处理NPC (每批10个)
                final int batchSize = 10;
                final int totalBatches = (int) Math.ceil(folks.size() / (double) batchSize);
//                System.out.println("异步处理NPC状态-开始");
                for (int batch = 0; batch < totalBatches; batch++) {
                    final int startIdx = batch * batchSize;
                    final int endIdx = Math.min(startIdx + batchSize, folks.size());

                    // 提交每个批次作为独立任务
//                    taskQueue.put(() -> {
                        for (int i = startIdx; i < endIdx; i++) {
                            NpcData npc = folks.get(i);
                            if (npc != null && npc.entity != null && !npc.entity.isDead) {
                                // 处理NPC状态
                                processSingleNpc(npc);
                                updatedNpcs.put(npc.entity.getEntityId(), npc);
                            }
                        }
//                    });
                }*/

                for (NpcData npc:ModSimLoader.folks){
                    if (npc != null && npc.entity != null && !npc.entity.isDead) {
                        // 处理NPC状态
                        processSingleNpc(npc);
                        updatedNpcs.put(npc.entity.getEntityId(), npc);
                    }
                }


                // 将更新后的NPC数据提交到主线程应用
//                scheduleMainThreadTask(() -> {
                    pendingNpcUpdates.putAll(updatedNpcs);
                    // 清理无效NPC
                    cleanupInvalidNpcs(world);
//                });

            } catch (Exception e) {
                ModSimLoader.log.error("Error processing NPCs asynchronously", e);
            } finally {
                // 标记处理完成
                isProcessing = false;
            }
//        });
    }
    // 处理单个NPC
    private static void processSingleNpc(NpcData npc) {
        // 处理饥饿度（P4：先不做定价/饿死；饿到 0 也不会死）
        if (npc.hunger > 0) {
            --npc.hunger;
        } else {
            npc.hunger = 0;
        }

        // 繁殖相关
        npc.matingStage = -1.0F;

        // 妊娠期
        if (npc.pregnancyStage > 0.0F) {
            npc.pregnancyStage += 0.1F;
        }

        // 年龄增长
        int currentAge = npc.age;
        boolean ageIncreased = false;

        if (npc.age >= npc.race.maturity) {
            if (ModSimLoader.dayOfWeek == 6) {
                ++npc.age;
                ageIncreased = true;
            }
        } else if (ModSimLoader.dayOfWeek == 3 || ModSimLoader.dayOfWeek == 6) {
            ++npc.age;
            ageIncreased = true;
        }

        // 成年处理
        if (ageIncreased && currentAge < npc.race.maturity && npc.age >= npc.race.maturity) {
            npc.evict();
            String message = new TextComponentTranslation("container.sim.main_is_now", new Object[0]).getUnformattedText();
            scheduleMainThreadTask(() -> ModSimLoader.sendChat(npc.getName() + message));
        }

        // 寿命处理
        if (npc.age >= npc.race.lifespan && new Random().nextInt(10) == 5) {
            String message = new TextComponentTranslation("container.sim.main_is_old", new Object[0]).getUnformattedText();
            scheduleMainThreadTask(() -> {
                ModSimLoader.sendChat(npc.getName() + message);
                npc.entity.attackEntityFrom(DamageSource.STARVE, 999.0F);
            });
        }
    }
    // 异步处理建筑和租金
    private static void processBuildingsAsync(World world) {
//        asyncExecutor.submit(() -> {
            try {
                List<Building> buildings = new ArrayList<>(ModSimLoader.buildings);
                Map<String, Building> updatedBuildings = new HashMap<>();
                float totalRent = 0.0F;

                // 分批处理建筑
                for (Building building : buildings) {
                    if (building != null && building.occupants.size() > 0) {
                        // 计算租金
                        totalRent += building.rent;
                        updatedBuildings.put(building.ID.toString(), building);
                    }
                }

                // 将结果提交到主线程
                final float finalRent = totalRent;
//                scheduleMainThreadTask(() -> {
                    pendingBuildingUpdates.putAll(updatedBuildings);

                    // 处理租金收集逻辑
                    if (ModSimLoader.isDayTime(world) &&
                            System.currentTimeMillis() - rentalsTimer > 3000L &&
                            newDayRentals) {
                        collectRent(world, finalRent);
                    }
//                });

            } catch (Exception e) {
                ModSimLoader.log.error("Error processing buildings asynchronously", e);
            } finally {
                // 标记处理完成
                isProcessing = false;
            }
//        });
    }

    // 收租逻辑（在主线程执行）
    private static void collectRent(World world, float rent) {
        ModSimLoader.log.info("收租了: " + rent);

        if (ModSimLoader.gamemode != 0) {
            NetWorkLoader.net.sendToAll(new PacketUpdateMoney());
        } else {
            if (world instanceof WorldServer) {
                ((WorldServer) world).addScheduledTask(() -> {
                    if (!world.playerEntities.isEmpty()) {
                        EntityPlayer player = world.playerEntities.get(0);
                        SoundEvent cash = SoundRegistry.CASH;
                        if (cash == null || cash.getRegistryName() == null) {
                            ModSimLoader.log.error("播放失败：sim:cash 声音事件未注册");
                        } else {
                            world.playSound(null, player.posX, player.posY, player.posZ,
                                    cash, SoundCategory.BLOCKS, 1.0F, 1.0F);
                        }
                        ModSimLoader.sendChat(new TextComponentTranslation("container.sim.main_Collected", new Object[0]).getUnformattedText() +
                                ModSimLoader.displayMoney(rent) +
                                new TextComponentTranslation("container.sim.main_rent_today", new Object[0]).getUnformattedText());
                    }
                });
            }
        }

        // 更新租金状态
        newDayRentals = false;
        rentalsTimer = System.currentTimeMillis();
    }
    // 异步处理环境控制
    private static void processEnvironmentAsync(World world) {
//        asyncExecutor.submit(() -> {
            try {
                // 检查天气
                if (ConfigLoader.configStopRain && world.isRaining() && world.getWorldInfo().getRainTime() > 1) {
                    // 在主线程执行天气修改
//                    scheduleMainThreadTask(() -> {
                        world.getWorldInfo().setRaining(false);
                        ModSimLoader.log.info("停止下雨");
                        ModSimLoader.sendChat(new TextComponentTranslation("chat.sim.xiayu", new Object[0]).getUnformattedText());
//                    });
                }

                // 处理昼夜循环
                handleDayNightCycle(world);

            } catch (Exception e) {
                ModSimLoader.log.error("Error processing environment asynchronously", e);
            } finally {
                // 标记处理完成
                isProcessing = false;
            }
//        });
    }

    // 处理昼夜循环
    private static void handleDayNightCycle(World world) {
        if (ModSimLoader.isDayTime(world)) {
            // 新的一天初始化
            if (!newDay) {
//                scheduleMainThreadTask(() -> {
                    newDay = true;
                    handleNewDay(world);
//                });
            }
        } else if (newDay) {
            newDay = false;
        }
    }

    // 处理新的一天开始
    private static void handleNewDay(World world) {
        ModSimLoader.log.info("天亮了");

        // 播放鸡叫音效
        if (world instanceof WorldServer) {
            ((WorldServer) world).addScheduledTask(() -> {
                SoundEvent rooster = SoundRegistry.ROOSTER;
                        if (rooster == null || rooster.getRegistryName() == null) {
                            ModSimLoader.log.error("播放失败：sim:rooster 声音事件未注册");
                        } else {
                for (EntityPlayer player : world.playerEntities) {
                    world.playSound(null, player.posX, player.posY, player.posZ,
                            rooster, SoundCategory.BLOCKS, 1.0F, 1.0F);
                }}
            });
        }

        // 更新星期和游戏天数
        if (ModSimLoader.dayOfWeek >= 6) {
            ModSimLoader.dayOfWeek = 0;
        } else {
            ++ModSimLoader.dayOfWeek;
        }
        ++ModSimLoader.gameDay;

        // P12(S1): 每个游戏日推进一次 NPC 年龄/怀孕/交配欲望。
        for (NpcData f : new ArrayList<>(ModSimLoader.folks)) {
            if (f != null) {
                f.onNewDay();
            }
        }

        // 重置收租标记
        newDayRentals = true;
        rentalsTimer = System.currentTimeMillis();
    }

    // 清理无效NPC
    private static void cleanupInvalidNpcs(World world) {
        Iterator<NpcData> iterator = ModSimLoader.folks.iterator();
        while (iterator.hasNext()) {
            NpcData npc = iterator.next();
            // P10(S0): entity==null 可能是“区块未加载、实体延迟生成”，不是死亡。
            if (npc.isDead) {
                // 清理住宅关联
                if (npc.home != null) {
                    npc.home.occupants.remove(npc);
                }
                iterator.remove();
            }
        }
    }

    // 调度网络同步任务
    private static void scheduleNetworkSync(World world) {
        if (world instanceof WorldServer) {
//            ((WorldServer) world).addScheduledTask(() -> {
                try {
                    // 发送NPC更新
                    for (NpcData npc : pendingNpcUpdates.values()) {
                        NetWorkLoader.net.sendToAll(new PacketSyncNpcData(npc));
                    }
                    pendingNpcUpdates.clear();

                    // 发送建筑更新
                    for (Building building : pendingBuildingUpdates.values()) {
//                        NetWorkLoader.net.sendToAll(new PacketSyncBuilding(building));
                    }
                    pendingBuildingUpdates.clear();

                    // 每1秒更新一次可雇佣的人和资金
                    if (tickCounter % 20 == 0) {
                        NetWorkLoader.net.sendToAll(new PacketReturnHireableFolks());
                        NetWorkLoader.net.sendToAll(new PacketUpdateMoney());
                    }

                } catch (Exception e) {
                    ModSimLoader.log.error("同步网络数据时出错", e);
                }
//            });
        }
    }

    // 安全调度主线程任务（P0：不再引用客户端 Minecraft）
    private static void scheduleMainThreadTask(Runnable task) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server != null && !server.isCallingFromMinecraftThread()) {
            server.addScheduledTask(task);
        } else {
            task.run(); // 已经在主线程（或没有服务器实例时直接执行）
        }
    }


    // 优雅关闭线程池
    /*public static void shutdown() {
        asyncExecutor.shutdown();
        try {
            if (!asyncExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            asyncExecutor.shutdownNow();
        }
    }*/



    // 补充：检查是否可以生成新NPC（核心逻辑）
    private static void checkAndSpawnNewNpc(World world) {
        boolean spawnNew = true; // 默认允许生成新NPC

        // 遍历所有NPC，检查是否有成年NPC无住宅
        List<NpcData> folks = new ArrayList<>(ModSimLoader.folks);
        for (NpcData npc : folks) {
            // 处理已死亡的NPC（清理住宅关联，避免内存泄漏）
            // P10(S0): entity==null 可能是区块未加载，不能当死亡。
            if (npc.isDead) {
                if (npc.home != null) {
                    npc.home.occupants.remove(npc);
                    npc.home = null;
                }
                continue;
            }

            // 核心判断：成年NPC无住宅 → 禁止生成新NPC
            if (npc.home == null) { // 无住宅
                // 过滤未成年NPC（未成年无住宅不影响）
                if (npc.race != null && npc.age >= npc.race.maturity) { // 已成年
                    spawnNew = false; // 发现成年NPC无住宅，标记为不可生成
                    ModSimLoader.log.info("NPC " + npc.getName() + " 已成年但无住宅，停止生成新NPC");
                    break; // 找到一个即可，无需继续遍历
                }
            }
        }

        // 只有所有成年NPC都有住宅时，才生成新NPC
        if (spawnNew && ModSimLoader.canGrowPopulation()) {
            ModSimLoader.log.info("所有成年NPC均有住宅，生成新NPC");
            new NpcData(world, false); // 生成新NPC
        } else if (spawnNew) {
            ModSimLoader.log.info("已达到人口上限 {}，不生成新NPC", ConfigLoader.configPopulationLimit);
        }
    }

    public static void simModDisconnected() {
        // P0: 断开连接可能发生在客户端。用服务端实例取主世界；纯客户端（无集成服务端）时
        // world 为 null，simModSave 会直接返回，不会再引用 net.minecraft.client.Minecraft。
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        World world = server != null ? server.getEntityWorld() : null;
        //保存一下数据
        simModSave(world);
        hasLoadedWorld = false;
    }

    /**
     * 客户端专属更新（渲染、动画、本地状态）
     * @param world
     */
    public static void clientSimModupdate(World world) {
        // 客户端必需的逻辑：
        // - NPC渲染器初始化
        // - 接收服务器同步的NPC数据并更新本地缓存
        // - 动画播放（如挥动手臂）、HUD显示等
        // 1. 检查是否需要整体更新（客户端控制更新频率）
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClientUpdateTime < CLIENT_UPDATE_INTERVAL) {
            return;
        }
        lastClientUpdateTime = currentTime;

        // 2. 客户端渲染与状态更新
        updateLocalNpcRendering(world);  // 更新本地NPC渲染（位置、动画）
        displayClientEffects(world);     // 显示客户端特效（粒子、音效）
        updateHudInfo(world);            // 更新HUD信息（NPC状态显示）
    }

    /**
     *  服务器端专属更新（实体创建、数据同步、AI）
     * @param world
     */
    public static void serverSimModupdate(World world) {
        // 原simModupdate中的服务器逻辑：
        // - NPC生成、状态更新（年龄、饥饿等）
        // - 网络包发送（向客户端同步NPC数据）
        // - 经济系统、收租等核心逻辑

        // 1. 检查是否需要整体更新（服务器端控制更新频率）
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastServerUpdateTime < SERVER_UPDATE_INTERVAL) {
            return;
        }
        lastServerUpdateTime = currentTime;

        // 2. 确保世界和玩家存在
        if (world == null || world.playerEntities.isEmpty()) {
            return;
        }

        // 3. 服务器端核心逻辑
        // 服务器端核心逻辑
        manageNpcEntities(world);        // 管理NPC实体（生成、删除）
        updateNpcStatusInBatches(world); // 分批更新NPC状态
        handleDayNightCycle(world);      // 处理昼夜循环
//        manageEconomySystem(world);      // 经济系统（收租等）
        handleEnvironmentControl(world); // 环境控制（停止下雨）

    }

    // 服务器端：管理NPC实体（生成、删除）
    private static void manageNpcEntities(World world) {
        // 1. 检查并清理无效NPC
        cleanupInvalidNpcs(world);

        // 2. 检查是否需要生成新NPC（原逻辑）
        if (System.currentTimeMillis() - npcSpawnCheckTimer > NPC_SPAWN_CHECK_INTERVAL) {
            checkAndSpawnNewNpc(world);
            npcSpawnCheckTimer = System.currentTimeMillis();
        }

        // 3. 同步NPC数据到客户端（通过网络包）
        sendNpcDataToClients(world);
    }

    // 服务器端：分批更新NPC状态
    private static void updateNpcStatusInBatches(World world) {
        List<NpcData> folks = new ArrayList<>(ModSimLoader.folks);
        int totalBatches = Math.max(1, (int) Math.ceil(folks.size() / (double) NPCS_PER_UPDATE));

        // 计算当前批次的起止索引
        int startIndex = currentNpcBatch * NPCS_PER_UPDATE;
        int endIndex = Math.min(startIndex + NPCS_PER_UPDATE, folks.size());

        // 处理当前批次的NPC状态
        for (int i = startIndex; i < endIndex; i++) {
            NpcData f = folks.get(i);
            if (f != null && f.entity != null && !f.entity.isDead) {
                updateNpcStatus(f, world); // 更新NPC饥饿、年龄等状态
            }
        }

        // 更新批次索引
        currentNpcBatch = (currentNpcBatch + 1) % totalBatches;
    }

    // 服务器端：更新单个NPC状态
    private static void updateNpcStatus(NpcData npc, World world) {
        // 处理饥饿度、年龄、繁殖等核心逻辑
        // （与原代码逻辑一致，略）
        // 处理饥饿度（P4：先不做定价/饿死；饿到 0 也不会死）
        if (npc.hunger > 0) {
            --npc.hunger;
        } else {
            npc.hunger = 0;
        }

        // 繁殖相关
        npc.matingStage = -1.0F;

        // 妊娠期
        if (npc.pregnancyStage > 0.0F) {
            npc.pregnancyStage += 0.1F;
        }

        // 年龄增长
        int currentAge = npc.age;
        boolean ageIncreased = false;

        if (npc.age >= npc.race.maturity) {
            if (ModSimLoader.dayOfWeek == 6) {
                ++npc.age;
                ageIncreased = true;
            }
        } else if (ModSimLoader.dayOfWeek == 3 || ModSimLoader.dayOfWeek == 6) {
            ++npc.age;
            ageIncreased = true;
        }

        // 成年处理
        if (ageIncreased && currentAge < npc.race.maturity && npc.age >= npc.race.maturity) {
            npc.evict();
            String message = new TextComponentTranslation("container.sim.main_is_now", new Object[0]).getUnformattedText();
            ModSimLoader.sendChat(npc.getName() + message);
        }

        // 寿命处理
        if (npc.age >= npc.race.lifespan && new Random().nextInt(10) == 5) {
            String message = new TextComponentTranslation("container.sim.main_is_old", new Object[0]).getUnformattedText();
            ModSimLoader.sendChat(npc.getName() + message);
            npc.entity.attackEntityFrom(DamageSource.STARVE, 999.0F);
        }
    }


    // 服务器端：发送NPC数据到客户端
    private static void sendNpcDataToClients(World world) {
        // 1. 筛选需要同步的NPC（如在玩家视野内）
        // 筛选需要同步的NPC（在玩家视野内）
        /*List<NpcData> npcsToSync = new ArrayList<>();
        double syncRange = 64.0; // 同步范围
        for (EntityPlayer player : world.playerEntities) {
            for (NpcData npc : ModSimLoader.folks) {
                if (npc.entity != null && npc.entity.getDistance(player) <= syncRange) {
                    npcsToSync.add(npc);
                }
            }
        }
        // 2. 通过网络包发送NPC数据到客户端
        for (NpcData npc : npcsToSync) {
            NetWorkLoader.net.sendToAll(new PacketSyncNpcData(npc));
        }*/

        if (world instanceof WorldServer && !((WorldServer) world).isCallingFromMinecraftThread()) {
            ((WorldServer) world).addScheduledTask(() -> {
                // 限流：只发送可见范围内的NPC
                List<NpcData> visibleNpcs = getVisibleNpcs(world);
                for (NpcData npc : visibleNpcs) {
                    NetWorkLoader.net.sendToAll(new PacketSyncNpcData(npc));
                }
            });
        } else {
            // 直接执行（如果已经在主线程）
            List<NpcData> visibleNpcs = getVisibleNpcs(world);
            for (NpcData npc : visibleNpcs) {
                NetWorkLoader.net.sendToAll(new PacketSyncNpcData(npc));
            }
        }
    }
    // 获取可见范围内的NPC（优化性能）
    private static List<NpcData> getVisibleNpcs(World world) {
        List<NpcData> visibleNpcs = new ArrayList<>();
        double renderDistance = world.provider.getDimension() == 0 ? 32.0 : 16.0; // 根据维度调整

        for (EntityPlayer player : world.playerEntities) {
            for (NpcData npc : ModSimLoader.folks) {
                if (npc.entity != null && npc.entity.getDistance(player) <= renderDistance) {
                    visibleNpcs.add(npc);
                }
            }
        }

        return visibleNpcs;
    }
    // 服务器端：环境控制（停止下雨）
    private static void handleEnvironmentControl(World world) {
        if (ConfigLoader.configStopRain && world.isRaining() && world.getWorldInfo().getRainTime() > 1) {
            world.getWorldInfo().setRaining(false);
            ModSimLoader.log.info("停止下雨");
            ModSimLoader.sendChat(new TextComponentTranslation("chat.sim.xiayu", new Object[0]).getUnformattedText());
        }
    }
    // 客户端：更新本地NPC渲染
    private static void updateLocalNpcRendering(World world) {
        // 获取本地缓存的NPC数据
        List<NpcData> localNpcs = new ArrayList<>(clientNpcCache.values());

        // 更新每个NPC的渲染状态
        for (NpcData npc : localNpcs) {
            if (npc != null && npc.entity != null && npc.entity.isEntityAlive()) {
                updateNpcRenderProperties(npc);
            }
        }
    }
    // 客户端：更新NPC渲染属性
    private static void updateNpcRenderProperties(NpcData npc) {
        // 更新NPC位置和姿态
        updateNpcPosition(npc);

        // 更新NPC动画（如挥动手臂）
        updateNpcAnimation(npc);

        // 更新NPC显示名称和状态
        updateNpcDisplayName(npc);
    }
    // 客户端：更新NPC位置
    private static void updateNpcPosition(NpcData npc) {
        // 从网络包数据更新NPC位置
        // 示例：entity.setPosition(npc.posX, npc.posY, npc.posZ);
    }

    // 客户端：更新NPC动画
    private static void updateNpcAnimation(NpcData npc) {
        // 示例：根据NPC状态设置动画
//        if (npc.isSwingingArm) {
            // 设置挥动手臂动画
//        }
    }

    // 客户端：更新NPC显示名称
    private static void updateNpcDisplayName(NpcData npc) {
        // 示例：entity.setCustomNameTag(npc.getName() + " - " + npc.getStatus());
    }
    // 客户端：显示特效（粒子、音效）
    private static void displayClientEffects(World world) {
        // 显示NPC相关粒子效果
        spawnNpcParticles(world);

        // 播放NPC相关音效
        playNpcSounds(world);
    }

    // 客户端：生成NPC粒子效果
    private static void spawnNpcParticles(World world) {
        // 示例：在NPC周围生成粒子
        // world.spawnParticle(EnumParticleTypes.HEART, x, y, z, 0, 0, 0);
    }

    // 客户端：播放NPC音效
    private static void playNpcSounds(World world) {
        // 示例：播放NPC语音
        // world.playSound(x, y, z, soundEvent, category, volume, pitch, false);
    }
    // 客户端：更新HUD信息
    private static void updateHudInfo(World world) {
        // 更新NPC状态显示（如饥饿度、年龄）
        updateNpcStatusHud(world);

        // 更新NPC相关提示信息
        updateNpcTips(world);
    }

    // 客户端：更新NPC状态HUD
    private static void updateNpcStatusHud(World world) {
        // 示例：在屏幕上显示NPC状态
        // 这通常在渲染HUD时处理
    }

    // 客户端：更新NPC提示信息
    private static void updateNpcTips(World world) {
        // 示例：显示NPC相关提示
        // 如"点击NPC交谈"、"NPC需要食物"等
    }

    // 客户端：更新本地NPC缓存
    public static void updateClientNpcCache(String entityId, NpcData npcData) {
        clientNpcCache.put(entityId, npcData);
    }
}
