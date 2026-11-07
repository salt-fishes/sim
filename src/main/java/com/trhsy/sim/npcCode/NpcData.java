package com.trhsy.sim.npcCode;

import com.trhsy.sim.block.BlockControlBox;
import com.trhsy.sim.entity.EntityNpc;
import com.trhsy.sim.loader.*;
import com.trhsy.sim.network.client.PacketReturnHireableFolks;
import com.trhsy.sim.network.client.PacketSendFolkSkin;
import com.trhsy.sim.network.client.PacketUpdateNPC;
import com.trhsy.sim.npcCode.block.FarmBox;
import com.trhsy.sim.npcCode.block.MineBox;
import com.trhsy.sim.npcCode.build.Building;
import com.trhsy.sim.npcCode.build.BuildingBlueprint;
import com.trhsy.sim.npcCode.build.TerrainType;
import com.trhsy.sim.npcCode.enums.EnumFamilyType;
import com.trhsy.sim.npcCode.job.*;
import com.trhsy.sim.npcCode.moodbuff.MoodBuff;
import com.trhsy.sim.npcCode.race.Race;
import com.trhsy.sim.npcCode.race.Races;
import com.trhsy.sim.npcCode.task.*;
import com.trhsy.sim.npcCode.traits.Trait;
import com.trhsy.sim.npcCode.traits.Traits;
import com.trhsy.sim.npcCode.store.SimStore;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFire;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.pathfinding.Path;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.npcCode
 * @ClassName: NpcData
 * @Description: NPC 核心数据
 * @date 2023/11/21 上午 10:42
 */
public class NpcData {
    /**
     * NPC 的唯一标识符
     */
    public UUID ID;
    /**
     * NPC 的名字
     */
    public String forename;
    /**
     * NPC 的姓氏
     */
    public String surname;
    /**
     * NPC 的年龄，0至17岁为儿童，18岁及以上为成人
     */
    public int age;
    /**
     * NPC 的性别，0 表示男性，1 表示女性
     */
    public int gender;
    /**
     * NPC 的当前状态
     */
    public String status = new TextComponentTranslation("container.sim.folk_data.Wandering", new Object[0]).getUnformattedText();
    /**
     * NPC 的饱食度
     */
    public double hunger = 10;
    /**
     * NPC 的个人存款（工作收入 / 购买食物支出，P4 经济闭环）
     */
    public float money = 20.0F;
    /**
     * NPC 所属的种族
     */
    public Race race;
    /**
     * NPC 的工作
     */
    public Job job;
    /**
     * NPC 对应的实体
     */
    public EntityNpc entity;
    /**
     * NPC 的情感关系列表
     */
    public List<FolkRelationship> relationships = new CopyOnWriteArrayList<>();
    /**
     * NPC 的情绪列表
     */
    public List<MoodBuff> buffs = new CopyOnWriteArrayList<>();
    /**
     * NPC 的物品栏
     */
    public List<ItemStack> inventory = new CopyOnWriteArrayList<>();
    /**
     * NPC 的任务列表
     */
    public List<Task> tasks = new CopyOnWriteArrayList<>();
    /**
     * NPC 的当前任务
     */
    public Task currentTask;
    /**
     * NPC 的第一个特征
     */
    public Trait trait1;
    /**
     * NPC 的第二个特征
     */
    public Trait trait2;
    /**
     * NPC 的第三个特征
     */
    public Trait trait3;
    /**
     * 表示 NPC 是否正在移动
     */
    public boolean isMoving;
    /**
     * NPC 的建筑技能等级
     */
    public float skillBuilding = 1.0F;
    /**
     * NPC 的耕种技能等级
     */
    public float skillFarming = 1.0F;
    /**
     * NPC 的采矿技能等级
     */
    public float skillMining = 1.0F;
    /**
     * NPC 的家
     */
    public Building home;
    /**
     * NPC 的位置
     */
    public V3 pos;
    /**
     * NPC 手持的物品
     */
    public ItemStack holding;
    /**
     * NPC 的皮肤编号
     */
    public int skinnumber = 0;
    /**
     * NPC 的皮肤名称
     */
    private String skinName;
    /**
     * NPC 的交配阶段
     */
    public float matingStage;
    /**
     * NPC 的怀孕阶段
     */
    public float pregnancyStage;
    /**
     * 表示 NPC 是否留在原地
     */
    public boolean stayPut;
    /**
     * 表示 NPC 是否正在睡觉
     */
    public boolean isSleeping;
    /**
     * 表示 NPC 是否已经死亡
     */
    public boolean isDead;
    /**
     * 表示 NPC 是否已经加载
     */
    public boolean isLoaded;
    /**
     * 临时雇员状态
     */
    private int tempStage;
    /**
     * 自上次状态更新以来的时间
     */
    private transient long timeSinceLastStatusUpdate;
    /**
     * 分钟更新时间
     */
    private transient long minuteUpdate;
    /**
     * 临时员工位置
     */
    public V3 tempEmployLoc;
    /**
     * 上次路径尝试的时间
     */
    Long lastPathAttempt;
    private int fs_rand;
    public World world;
    /** P1: 住宅建筑的稳定 UUID（存档只写它，读档后由 linkHome() 解析为 Building）。 */
    public String homeId;
    /** P2: 上次安全传送时间（毫秒），用于冷却。 */
    private transient long lastTeleportTime = 0L;
    /** P2: 传送冷却，避免每 tick 瞬移。 */
    private static final long TELEPORT_COOLDOWN_MS = 3000L;
    /** P3: 天黑后的自由活动时间（ticks）。先社交/闲逛，之后才强制回家睡觉。 */
    private static final long EVENING_FREE_TICKS = 1200L;
    /** P2+: 移动进度看门狗（有目标却连续 5 秒无进展 → 救援传送）。 */
    private transient V3 pendingMoveTarget;
    private transient double lastProgressX;
    private transient double lastProgressZ;
    private transient long lastProgressCheck;
    private transient int noProgressCount;
    /** P2+: 屋顶看门狗计时（高于目标/住宅 5 格以上持续 5 秒 → 救援）。 */
    private transient int roofTicks = 0;
    /** P3: 深夜未入睡计时（10 秒兜底强制睡觉）。 */
    private transient int deepNightTicks = 0;

    /**
     * P10(S0): NPC 是否处于“可模拟”状态。
     * 1.12.2 中实体只在已加载区块里 tick；实体没加载时，
     * 数据层不能继续跑任务/写方块，否则会出现隔空取货、隔空建造和区块加载尖峰。
     */
    public boolean isActive() {
        if (this.world == null || this.world.isRemote) {
            return false;
        }
        if (this.entity == null || this.entity.isDead) {
            return false;
        }
        if (this.entity.world == null) {
            return false;
        }
        if (this.entity.world != this.world && !this.entity.world.isRemote) {
            this.world = this.entity.world;
        }
        if (!this.entity.addedToChunk) {
            return false;
        }
        BlockPos p = this.entity.getPosition();
        return this.entity.world.isBlockLoaded(p);
    }

    // ========================= 常量定义（消除硬编码）=========================
    private static final String NPC_FOLDER_NAME = "npc";
    private static final String NPC_FILE_SUFFIX = ".sk2";
    private static final String TRANSLATION_PREFIX = "container.sim.";
    private static final double NPC_SPAWN_RANGE = 80.0D; // NPC重生检测范围
    private static final int MAX_PATH_ATTEMPT = 100; // 最大路径尝试次数

    private static final int PLAYER_SPAWN_RANGE = 10; // 玩家周围生成范围（10格）
    private static final Random RANDOM = new Random(); // 复用Random实例，避免重复创建

    /**
     * 构造函数，用于创建新的 NPC
     *
     * @param world       游戏世界
     * @param fromCommand 是否通过命令创建
     */
    public NpcData(World world, boolean fromCommand) {
        try {
            /*this.fs_rand=0;
            this.isDead=false;
            //手持空
//            this.holding = new ItemStack(Blocks.AIR);
            //交配阶段 没有需求
            this.matingStage = -1.0F;
            //雇佣信息无
            this.tempStage = -1;
            //更新时间0
            this.timeSinceLastStatusUpdate = 0L;
            //更新时间0
            this.minuteUpdate = 0L;
            //雇佣位置无
            this.tempEmployLoc = null;
            //路径尝试无
            this.lastPathAttempt = 0L;
            //性别随机
            this.gender = new Random().nextInt(2);
            //皮肤随机
            this.skinnumber = new Random().nextInt(64) + 1;

            //种族分配
            this.assignRace();
            //年龄
            this.age = this.race.maturity;
            //特征
            generateTraits();
            EntityNpc e = new EntityNpc(world, true);
            e.isBeingCreated = true;
            if (world.playerEntities.size() > 0) {
                EntityPlayer thePlayer = world.playerEntities.get(0);
                e.setPositionAndUpdate(thePlayer.posX, thePlayer.posY, thePlayer.posZ);
                this.pos = new V3(thePlayer.getPosition(), thePlayer.dimension);
            }
            ModSimLoader.log.info("开始生成新的NPC4");
            if (!fromCommand) {
                Vec3d newPos= RandomPositionGenerator.findRandomTarget(e, 30, 7);
                if(newPos == null){
                    newPos = RandomPositionGenerator.findRandomTarget(e, 30, 7);
                }
                BlockPos pos = new BlockPos(newPos);
                BlockPos up=pos.up();
                //
                while (pos != null && !world.isAirBlock(up)) {
                    //ModSimLoader.log.info(!world.isAirBlock(up));
                    this.fs_rand++;
                    newPos = RandomPositionGenerator.findRandomTarget(e, 30, 7);
                    if (newPos != null) {
                        pos = new BlockPos(newPos);
                    }
                    if(this.fs_rand>100){
                        if (world.playerEntities.size() > 0) {
                            EntityPlayer thePlayer = world.playerEntities.get(0);
                            newPos=new Vec3d(thePlayer.posX, thePlayer.posY, thePlayer.posZ);
                        }
                        break;
                    }
                }
                ModSimLoader.log.info("开始生成新的NPC112");
                e.setPositionAndUpdate(newPos.x, newPos.y + 1.0D, newPos.z);
                this.pos = V3.fromVec3d(newPos);
            }
            e.theData = this;
            this.entity = e;
            world.spawnEntity(e);
            this.ID = this.entity.getUniqueID().toString();
            //刚刚进入该地区
            String fs_ldzl = new TextComponentTranslation("container.sim.folk_data_just",new Object[0]).getUnformattedText();
            ModSimLoader.sendChat(this.getName() + fs_ldzl);
            //返回可雇佣的人
            NetWorkLoader.net.sendToAll(new PacketReturnHireableFolks());
            //向客户端发送皮肤地址
            this.sendSkinPathToClient();
            //保存NPC
            this.saveFolk();
            this.isLoaded = true;
            ModSimLoader.folks.add(this);
            NetWorkLoader.net.sendToAll(new PacketUpdateNPC());*/
            this.world=world;
            // 初始化通用属性
            initializeCommonAttributes();
            // 随机分配性别
            this.gender = new Random().nextInt(2);
            // 随机分配皮肤编号
            this.skinnumber = new Random().nextInt(64) + 1;
            // 分配种族
            this.assignRace();
            // 设置年龄为种族的成熟年龄
            this.age = this.race.maturity;
            // 生成特征
            this.generateTraits();

            // 创建 NPC 实体
            EntityNpc e = new EntityNpc(world, true);
            e.isBeingCreated = true;
/*
            // 如果世界中有玩家，将 NPC 初始位置设置为第一个玩家的位置
            if (world.playerEntities.size() > 0) {
                EntityPlayer thePlayer = world.playerEntities.get(0);
                e.setPositionAndUpdate(thePlayer.posX, thePlayer.posY, thePlayer.posZ);
                this.pos = new V3(thePlayer.getPosition(), thePlayer.dimension);
            }
            ModSimLoader.log.info("开始生成新的NPC4");
            // 如果不是通过命令创建，随机生成 NPC 的位置
            if (!fromCommand) {
                Vec3d newPos = getRandomSpawnPosition(e, world);
                if (newPos != null) {
                    e.setPositionAndUpdate(newPos.x, newPos.y + 1.0D, newPos.z);
                    this.pos = V3.fromVec3d(newPos);
                }
            }*/
            setInitialPosition(e, world, fromCommand);
            // 将 NPC 数据与实体关联
            e.theData = this;
            this.entity = e;
            // 在世界中生成 NPC 实体
            world.spawnEntity(e);
            // 获取 NPC 的唯一标识符
            this.ID = this.entity.getUniqueID();

            // 发送 NPC 刚刚进入该地区的消息
            String fs_ldzl = new TextComponentTranslation("container.sim.folk_data_just", new Object[0]).getUnformattedText();
            ModSimLoader.sendChat(this.getName() + fs_ldzl);

            // 向所有客户端发送可雇佣的人列表
            NetWorkLoader.net.sendToAll(new PacketReturnHireableFolks());

            // 向客户端发送 NPC 的皮肤地址
            this.sendSkinPathToClient();
            // 保存 NPC 数据
            this.saveFolk();
            // 标记 NPC 已加载
            this.isLoaded = true;
            // 将 NPC 添加到 ModSimLoader 的 NPC 列表中
            ModSimLoader.folks.add(this);
            // 向所有客户端发送 NPC 更新消息
            NetWorkLoader.net.sendToAll(new PacketUpdateNPC());
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("NpcData出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }


    /**
     * 获取随机生成的 NPC 位置
     *
     * @param e     NPC 实体
     * @param world 游戏世界
     * @return 随机生成的位置
     */
    private Vec3d getRandomSpawnPosition(EntityNpc e, World world) {
        int maxAttempts = 100;
        int attempt = 0;
        Vec3d newPos;
        BlockPos pos;
        BlockPos up;

        while (attempt < maxAttempts) {
            newPos = RandomPositionGenerator.findRandomTarget(e, 30, 7);
            if (newPos != null) {
                pos = new BlockPos(newPos);
                up = pos.up();
                if (world.isAirBlock(up)) {
                    return newPos;
                }
            }
            attempt++;
        }

        // 如果尝试次数超过上限，将位置设置为第一个玩家的位置
        if (world.playerEntities.size() > 0) {
            EntityPlayer thePlayer = world.playerEntities.get(0);
            return new Vec3d(thePlayer.posX, thePlayer.posY, thePlayer.posZ);
        }
        return null;
    }
    /**
     * 构造函数，用于首次进入世界时加载已存在的 NPC
     *
     * @param world 游戏世界
     * @param uuid  NPC 的唯一标识符
     */
    public NpcData(World world, UUID uuid) {
        try {
            //初始化手持物品
//            this.holding = new ItemStack(Blocks.AIR);
            //交配阶段
           /* this.matingStage = -1.0F;
            //临时雇员状态
            this.tempStage = -1;
            //自上次状态更新以来的时间
            this.timeSinceLastStatusUpdate = 0L;
            //分钟更新
            this.minuteUpdate = 0L;
            //tempEmployLoc
            this.tempEmployLoc = null;
            //上次路径尝试
            this.lastPathAttempt = 0L;
            //是服务器端
            if (!world.isRemote) {
                //加载NPC到世界上
                this.loadFolk(world, uuid);
            }*/
            this.world=world;
            // 初始化通用属性
            initializeCommonAttributes();
            // 如果是服务器端，加载 NPC 数据
//            if (!world.isRemote) {
                this.loadFolk(world, uuid);
//            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("NpcData1出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    /**
     * 构造函数，用于创建新生儿 NPC
     *
     * @param world  游戏世界
     * @param mother 母亲 NPC 数据
     * @param father 父亲 NPC 数据
     */
    public NpcData(World world, NpcData mother, NpcData father) {
        try {
            /*this.isDead =false;
//            this.holding = new ItemStack(Blocks.AIR);

            this.matingStage = -1.0F;
            this.tempStage = -1;
            this.timeSinceLastStatusUpdate = 0L;
            this.skinnumber = new Random().nextInt(64) + 1;
            this.minuteUpdate = 0L;
            this.tempEmployLoc = null;
            this.lastPathAttempt = 0L;
            this.gender = new Random().nextInt(2);
            this.age = 0;
            if (new Random().nextInt(2) == 0) {
                this.assignRace(mother.race.raceName, true);
            } else {
                this.assignRace(father.race.raceName, true);
            }

            this.surname = father.surname;
            generateTraits();
            EntityNpc e = new EntityNpc(world, true);
            e.isBeingCreated = true;
            e.setPositionAndUpdate(mother.entity.posX, mother.entity.posY, mother.entity.posZ);
            this.pos = mother.pos;
            e.theData = this;
            this.ID = e.getUniqueID().toString();
            this.entity = e;
            this.assignFamilyMembers(mother, father);
            this.home = mother.home;
            mother.home.occupants.add(this);
            world.spawnEntity(e);
            NetWorkLoader.net.sendToAll(new PacketReturnHireableFolks());
            this.sendSkinPathToClient();
            this.saveFolk();
            this.isLoaded = true;*/
            this.world=world;
            // 初始化通用属性
            initializeCommonAttributes();
            // 随机分配性别
            this.gender = new Random().nextInt(2);
            // 随机分配皮肤编号
            this.skinnumber = new Random().nextInt(64) + 1;
            // 设置年龄为 0
            this.age = 0;

            // 随机选择继承母亲或父亲的种族
            this.assignRace(new Random().nextBoolean() ? mother.race.raceName : father.race.raceName, true);
            /*if (new Random().nextInt(2) == 0) {
                this.assignRace(mother.race.raceName, true);
            } else {
                this.assignRace(father.race.raceName, true);
            }*/

            // 设置姓氏为父亲的姓氏
            this.surname = father.surname;
            // 生成特征
            this.generateTraits();

            // 创建 NPC 实体
            EntityNpc e = new EntityNpc(world, true);
            e.isBeingCreated = true;
            // 将 NPC 初始位置设置为母亲的位置
            e.setPositionAndUpdate(mother.entity.posX, mother.entity.posY, mother.entity.posZ);
            this.pos = mother.pos;
            // 将 NPC 数据与实体关联
            e.theData = this;
            // 获取 NPC 的唯一标识符
            this.ID = e.getUniqueID();
            this.entity = e;
            // 分配家庭成员关系
            this.assignFamilyMembers(mother, father);
            // 设置家为母亲的家
            this.home = mother.home;
            // 将新生儿添加到母亲家的居住者列表中
            mother.home.occupants.add(this);
            // 在世界中生成 NPC 实体
            world.spawnEntity(e);

            // 向所有客户端发送可雇佣的人列表
            NetWorkLoader.net.sendToAll(new PacketReturnHireableFolks());
            // 向客户端发送 NPC 的皮肤地址
            this.sendSkinPathToClient();
            // 保存 NPC 数据
            this.saveFolk();
            // 标记 NPC 已加载
            this.isLoaded = true;
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("NpcData出错了2：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    /**
     * 分配家庭成员关系
     *
     * @param mother 母亲 NPC 数据
     * @param father 父亲 NPC 数据
     */
    public void assignFamilyMembers(NpcData mother, NpcData father) {
        this.assignRelationshipsFromParent(mother);
        this.assignRelationshipsFromParent(father);
    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 从父级分配关系
     * @Date 10:37 2022/10/21
     * @Param [parent]
     **/
    public void assignRelationshipsFromParent(NpcData parent) {
        try {
            // 添加与父级的亲子关系
            this.relationships.add(new FolkRelationship(this, parent, EnumFamilyType.PARENT));
            parent.relationships.add(new FolkRelationship(parent, this, EnumFamilyType.CHILD));
            // 遍历父级的关系列表，分配其他亲属关系
            for (FolkRelationship rel : parent.relationships) {
                NpcData folk2 = rel.getOther();
                if (rel.familyType != EnumFamilyType.EXTENDED && rel.familyType != EnumFamilyType.GRANDCHILD && rel.familyType != EnumFamilyType.GRANDPARENT && rel.familyType != EnumFamilyType.PARENTSIBLING) {
                    if (folk2 == null) {
                        folk2 = rel.getOther();
                    }
                    if (rel.familyType == EnumFamilyType.CHILD && this.getRelationshipWith(folk2) == null) {
                        this.relationships.add(new FolkRelationship(this, folk2, EnumFamilyType.SIBLING));
                        folk2.relationships.add(new FolkRelationship(folk2, this, EnumFamilyType.SIBLING));
                    }

                    if (rel.familyType == EnumFamilyType.SIBLING && this.getRelationshipWith(folk2) == null) {
                        this.relationships.add(new FolkRelationship(this, folk2, EnumFamilyType.PARENTSIBLING));
                        folk2.relationships.add(new FolkRelationship(folk2, this, EnumFamilyType.SIBLINGCHILD));
                    }

                    if (rel.familyType == EnumFamilyType.SIBLINGCHILD && this.getRelationshipWith(folk2) == null) {
                        this.relationships.add(new FolkRelationship(this, folk2, EnumFamilyType.COUSIN));
                        folk2.relationships.add(new FolkRelationship(folk2, this, EnumFamilyType.COUSIN));
                    }

                    if (rel.familyType == EnumFamilyType.PARENT && this.getRelationshipWith(folk2) == null) {
                        this.relationships.add(new FolkRelationship(this, folk2, EnumFamilyType.GRANDPARENT));
                        folk2.relationships.add(new FolkRelationship(folk2, this, EnumFamilyType.GRANDCHILD));
                    }
                }
                if (this.getRelationshipWith(folk2) == null) {
                    this.relationships.add(new FolkRelationship(this, folk2, EnumFamilyType.EXTENDED));
                    folk2.relationships.add(new FolkRelationship(folk2, this, EnumFamilyType.EXTENDED));
                }
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("assignRelationshipsFromParent出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }
    /**
     * 获取与另一个 NPC 的关系
     *
     * @param other 另一个 NPC 数据
     * @return 关系对象
     */
    public FolkRelationship getRelationshipWith(NpcData other) {
        for (FolkRelationship rel : this.relationships) {
            if (rel.getOther() == other) {
                return rel;
            }
        }
        return null;
    }
    /**
     * 分配种族
     */
//    public void assignRace() {
//        Random random = new Random();
//        List<Race> races = Races.getAllRaces();
//        int index = random.nextInt(races.size());
//        this.race = races.get(index);
//        this.skinName = this.race.getRandomSkin();
//    }
    /**
     * 分配指定种族
     *
     * @param raceName 种族名称
     * @param isChild  是否为儿童
     */
//    public void assignRace(String raceName, boolean isChild) {
//        this.race = Races.getRaceByName(raceName);
//        if (this.race != null) {
//            this.skinName = this.race.getRandomSkin();
//        }
//    }
    /**
     * 生成特征
     */
//    public void generateTraits() {
//        Random random = new Random();
//        List<Trait> allTraits = Arrays.asList(Traits.traitList);
//        int size = allTraits.size();
//        int index1 = random.nextInt(size);
//        int index2;
//        int index3;
//        do {
//            index2 = random.nextInt(size);
//        } while (index2 == index1);
//        do {
//            index3 = random.nextInt(size);
//        } while (index3 == index1 || index3 == index2);
//        this.trait1 = allTraits.get(index1);
//        this.trait2 = allTraits.get(index2);
//        this.trait3 = allTraits.get(index3);
//    }
    /**
     * 加载 NPC 数据
     *
     * @param world 游戏世界
     * @param uuid  NPC 的唯一标识符
     **/
    /**
     * P1: 从 {@link SimStore} 的 NBT 记录加载 NPC（语言无关、原子存档）。
     */
    public void loadFolk(World world, UUID uuid) {
        try {
            SimStore store = SimStore.get(world);
            NBTTagCompound tag = store == null ? null : store.getNpc(uuid);
            if (tag == null) {
                this.isLoaded = false;
                ModSimLoader.log.warn("NPC存档缺失，跳过: " + uuid);
                return;
            }
            fromNBT(tag, world);
            if (this.isDead) {
                SimStore s = SimStore.get(world);
                if (s != null) {
                    s.removeNpc(this.ID);
                }
                this.isLoaded = false;
                return;
            }
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            Entity existing = server == null ? null : server.getEntityFromUuid(this.ID);
            EntityNpc e;
            if (existing instanceof EntityNpc) {
                e = (EntityNpc) existing;
            } else {
                // P10(S0): 区块未加载时先不生成实体，避免世界加载时
                // 为了所有 NPC 强制加载区块。等玩家靠近、区块加载后再由
                // trySpawnIfLoaded() 补生成。
                if (this.pos == null || !world.isBlockLoaded(this.pos.toBlockPos())) {
                    this.entity = null;
                    this.isLoaded = true;
                    ModSimLoader.log.info("NPC {} 所在区块未加载，延迟生成实体", this.ID);
                    return;
                }
                e = new EntityNpc(world, this.ID);
                e.setPosition(this.pos.x, this.pos.y, this.pos.z);
                e.setPositionAndUpdate(this.pos.x, this.pos.y, this.pos.z);
                e.noClip = false;
                world.spawnEntity(e);
            }
            e.isBeingCreated = true;
            e.theData = this;
            this.entity = e;
            this.isLoaded = true;
        } catch (Exception e) {
            this.isLoaded = false;
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("loadFolk(NBT)出错了：" + e.getMessage() + " 行数：" + element.getLineNumber());
        }
    }

    /**
     * P10(S0): 区块加载后补生成 NPC 实体。
     * 数据层在未加载区块保持“无实体”状态，不跑任务、不写方块。
     */
    public void trySpawnIfLoaded(World world) {
        if (this.entity != null || !this.isLoaded || this.isDead
                || this.pos == null || world == null || world.isRemote) {
            return;
        }
        if (this.pos.dimension != world.provider.getDimension()) {
            return;
        }
        BlockPos bp = this.pos.toBlockPos();
        if (!world.isBlockLoaded(bp)) {
            return;
        }
        try {
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            Entity existing = server == null ? null : server.getEntityFromUuid(this.ID);
            EntityNpc e;
            if (existing instanceof EntityNpc) {
                e = (EntityNpc) existing;
            } else {
                e = new EntityNpc(world, this.ID);
                e.noClip = false;
                e.setPosition(this.pos.x, this.pos.y, this.pos.z);
                e.setPositionAndUpdate(this.pos.x, this.pos.y, this.pos.z);
                world.spawnEntity(e);
            }
            e.theData = this;
            this.entity = e;
            ModSimLoader.log.info("NPC {} 区块已加载，补生成实体 @ {}", this.ID, bp);
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("trySpawnIfLoaded出错了：" + e.getMessage() + " 行数：" + element.getLineNumber());
        }
    }

    /** P1: 解析住宅 UUID（在建筑全部加载后调用）。 */
    public void linkHome() {
        if (this.homeId == null || this.homeId.isEmpty()) {
            return;
        }
        if (this.home != null && this.homeId.equals(String.valueOf(this.home.ID))) {
            return;
        }
        for (Building b : ModSimLoader.buildings) {
            if (b != null && b.ID != null && this.homeId.equals(b.ID.toString())) {
                this.home = b;
                if (!b.occupants.contains(this)) {
                    b.occupants.add(this);
                }
                return;
            }
        }
        this.home = null;
    }

    /** P1: 序列化为稳定 ID 的 NBT（存档只写这个）。 */
    public NBTTagCompound toNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("uuid", String.valueOf(this.ID));
        tag.setString("fname", this.forename == null ? "" : this.forename);
        tag.setString("sname", this.surname == null ? "" : this.surname);
        tag.setInteger("gender", this.gender);
        tag.setInteger("age", this.age);
        tag.setDouble("hunger", this.hunger);
        tag.setFloat("money", this.money);
        tag.setFloat("skillBuilding", this.skillBuilding);
        tag.setFloat("skillFarming", this.skillFarming);
        tag.setFloat("skillMining", this.skillMining);
        tag.setFloat("pregnancy", this.pregnancyStage);
        tag.setFloat("mating", this.matingStage);
        tag.setBoolean("isDead", this.isDead);
        tag.setInteger("skinnumber", this.skinnumber);
        tag.setString("skin", this.skinName == null ? "" : this.skinName);
        tag.setString("race", SimIds.raceId(this.race));
        tag.setString("trait1", SimIds.traitId(this.trait1));
        tag.setString("trait2", SimIds.traitId(this.trait2));
        tag.setString("trait3", SimIds.traitId(this.trait3));
        tag.setString("holding", SimIds.itemId(this.holding));
        // P10(S0): 手持物保存完整 ItemStack（数量/metadata/NBT）。
        // 旧字段 "holding" 只存 registry name，读档会把附魔/耐久/数量全部丢掉。
        if (this.holding != null && !this.holding.isEmpty()) {
            NBTTagCompound holdingTag = new NBTTagCompound();
            this.holding.writeToNBT(holdingTag);
            tag.setTag("holdingItem", holdingTag);
        }
        // P10(S0): 背包完整存盘。
        NBTTagList invTag = new NBTTagList();
        if (this.inventory != null) {
            for (ItemStack stack : this.inventory) {
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                NBTTagCompound itemTag = new NBTTagCompound();
                stack.writeToNBT(itemTag);
                invTag.appendTag(itemTag);
            }
        }
        tag.setTag("inventory", invTag);
        if (this.pos != null) {
            tag.setString("pos", this.pos.toString());
        }
        if (this.tempEmployLoc != null) {
            tag.setString("employedAt", this.tempEmployLoc.toString());
        }
        if (this.home != null) {
            tag.setString("home", String.valueOf(this.home.ID));
        } else if (this.homeId != null) {
            tag.setString("home", this.homeId);
        }
        NBTTagList rels = new NBTTagList();
        for (FolkRelationship rel : this.relationships) {
            if (rel == null || rel.folk2 == null) {
                continue;
            }
            NBTTagCompound r = new NBTTagCompound();
            r.setString("uuid", rel.folk2.toString());
            r.setString("family", rel.familyType == null ? "UNRELATED" : rel.familyType.name());
            r.setString("level", rel.level == null ? "AQUAINTANCE" : rel.level.name());
            r.setInteger("sub", rel.subLevel);
            rels.appendTag(r);
        }
        tag.setTag("relationships", rels);
        if (this.job != null) {
            NBTTagCompound jobTag = new NBTTagCompound();
            String jobId = SimIds.jobIdFromDisplay(this.job.jobName);
            if (jobId.isEmpty()) {
                jobId = SimIds.vocation(1);
            }
            jobTag.setString("id", jobId);
            if (this.job.workPlace != null) {
                jobTag.setString("site", this.job.workPlace.toString());
            }
            jobTag.setInteger("stage", this.job.stage);
            if (this.job instanceof JobBuilder) {
                JobBuilder jb = (JobBuilder) this.job;
                if (jb.blueprint != null) {
                    jobTag.setString("blueprint", jb.blueprint.name);
                }
                jobTag.setInteger("direction", jb.direction);
            } else if (this.job instanceof JobTerrainFormer) {
                JobTerrainFormer jt = (JobTerrainFormer) this.job;
                if (jt.terrainType != null) {
                    jobTag.setString("terrainName", jt.terrainType.terrainName);
                    jobTag.setString("terrainType", jt.terrainType.terrainType);
                }
            }
            tag.setTag("job", jobTag);
        }
        return tag;
    }

    /** P1: 从稳定 ID 的 NBT 恢复。 */
    public void fromNBT(NBTTagCompound tag, World world) {
        this.world = world;
        try {
            this.ID = UUID.fromString(tag.getString("uuid"));
        } catch (Exception e) {
            throw new IllegalStateException("非法 UUID: " + tag.getString("uuid"));
        }
        this.forename = tag.getString("fname");
        this.surname = tag.getString("sname");
        this.gender = tag.getInteger("gender");
        this.age = tag.getInteger("age");
        this.hunger = tag.hasKey("hunger") ? tag.getDouble("hunger") : 10.0D;
        this.money = tag.hasKey("money") ? tag.getFloat("money") : 20.0F;
        this.skillBuilding = tag.getFloat("skillBuilding");
        this.skillFarming = tag.getFloat("skillFarming");
        this.skillMining = tag.getFloat("skillMining");
        this.pregnancyStage = tag.getFloat("pregnancy");
        this.matingStage = tag.getFloat("mating");
        this.isDead = tag.getBoolean("isDead");
        this.skinnumber = tag.getInteger("skinnumber");
        this.skinName = tag.getString("skin");
        this.race = SimIds.raceFromId(tag.getString("race"));
        if (this.race == null) {
            this.assignRace();
        }
        this.trait1 = SimIds.traitFromId(tag.getString("trait1"));
        this.trait2 = SimIds.traitFromId(tag.getString("trait2"));
        this.trait3 = SimIds.traitFromId(tag.getString("trait3"));
        // P10(S0): 优先读完整 ItemStack；旧档只有 registry name 时再走兼容路径。
        if (tag.hasKey("holdingItem")) {
            ItemStack savedHolding = new ItemStack(tag.getCompoundTag("holdingItem"));
            this.holding = savedHolding.isEmpty() ? null : savedHolding;
        } else {
            this.holding = SimIds.itemFromId(tag.getString("holding"));
        }
        // P10(S0): 背包完整读档。
        this.inventory.clear();
        NBTTagList invTag = tag.getTagList("inventory", 10);
        for (int i = 0; i < invTag.tagCount(); i++) {
            ItemStack stack = new ItemStack(invTag.getCompoundTagAt(i));
            if (!stack.isEmpty()) {
                this.inventory.add(stack);
            }
        }
        this.pos = tag.hasKey("pos") ? V3.fromString(tag.getString("pos")) : null;
        this.tempEmployLoc = tag.hasKey("employedAt") ? V3.fromString(tag.getString("employedAt")) : null;
        this.homeId = tag.hasKey("home") ? tag.getString("home") : null;
        NBTTagList rels = tag.getTagList("relationships", 10);
        for (int i = 0; i < rels.tagCount(); i++) {
            NBTTagCompound r = rels.getCompoundTagAt(i);
            try {
                this.relationships.add(new FolkRelationship(this,
                        r.getString("uuid") + "," + r.getString("family") + "," + r.getString("level") + "," + r.getInteger("sub")));
            } catch (Exception ignored) {
            }
        }
        if (tag.hasKey("job")) {
            NBTTagCompound jt = tag.getCompoundTag("job");
            String jobId = jt.getString("id");
            V3 site = jt.hasKey("site") ? V3.fromString(jt.getString("site")) : this.tempEmployLoc;
            BuildingBlueprint bp = jt.hasKey("blueprint") ? ModSimLoader.getBlueprintsByName(jt.getString("blueprint")) : null;
            int direction = jt.getInteger("direction");
            TerrainType terrain = jt.hasKey("terrainName")
                    ? new TerrainType(jt.getString("terrainName"), jt.getString("terrainType")) : null;
            this.job = JobFactory.createJobAt(this, jobId, site, bp, direction, terrain, world);
            if (this.job != null) {
                // P10(S0): 任务队列本身没有存盘，读档后 jobTasks 是空的。
                // 不能把旧 stage 直接当下标（Job.onArrive 会 get(stage) 越界）。
                // 统一从 -1 开始，让各职业在下一 tick 重建本轮任务。
                this.job.stage = -1;
            }
        }
    }

    public void loadFolkLegacy(World world, UUID uuid) {
        try {
            File npcFolder = new File(ModSimLoader.getSavesDataFolder() + File.separator + "npc");
            if (!npcFolder.exists()) {
                npcFolder.mkdirs();
            }

            String loadID = uuid.toString();
            InputStream inputStream = new FileInputStream(new File(npcFolder.getAbsolutePath() + File.separator + loadID + ".sk2"));
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            BuildingBlueprint buildingBlueprint = null;
            String line="";
            while ((line= reader.readLine())!=null){
                int m1 = line.indexOf("|");
                String name = line.substring(0, m1).toLowerCase();
                String value = line.substring(m1 + 1).toLowerCase();
                if (line.contains("id|")) {
                    this.ID = UUID.fromString(value);
                }
                if (line.contains("isdie|")) {
                    this.isDead = Boolean.parseBoolean(value);
                    if(this.isDead){
                        Entity entity=FMLCommonHandler.instance().getMinecraftServerInstance().getEntityFromUuid(this.ID);
                        if(entity!=null){
                            entity.setDead();
                        }
                        this.isLoaded = false;
                        reader.close();
                        inputStream.close();
                        return;
                    }
                }
                if (line.contains("fname|")) {
                    this.forename = value.substring(0, 1).toUpperCase() + value.substring(1);
                } else if (line.contains("sname|")) {
                    this.surname = value.substring(0, 1).toUpperCase() + value.substring(1);
                } else if (line.contains("gender|")) {
                    this.gender = Integer.valueOf(value);
                } else if (line.contains("age|") && !line.contains("jobstage")) {
                    this.age = Integer.valueOf(value);
                } else if (line.contains("race|")) {
                    this.assignRace(value, false);
                } else if (line.contains("skin|")) {
                    this.race.skinName = value;
                    this.skinName = value;
                } else if (line.contains("pos|")) {
                    this.pos = V3.fromString(value);
                } else if (line.contains("trait1|")) {
                    this.trait1 = Trait.getTraitFromName(value);
                } else if (line.contains("trait2|")) {
                    this.trait2 = Trait.getTraitFromName(value);
                } else if (line.contains("trait3|")) {
                    this.trait3 = Trait.getTraitFromName(value);
                } else if (line.contains("pregnancy|")) {
                    this.pregnancyStage = Float.parseFloat(value);
                }else if (line.contains("hunger|")) {
                    this.hunger = Double.valueOf(value);
                } else if (line.contains("buildingskill|")) {
                    this.skillBuilding = Float.valueOf(value);
                } else if (line.contains("farmingskill|")) {
                    this.skillFarming = Float.valueOf(value);
                }else if (line.contains("miningskill|")) {
                    this.skillMining = Float.valueOf(value);
                }  else if (line.contains("holding|")) {
                    try {
                        if ("".equals(value) || value == null) {
                            this.holding = null;
                        } else {
                            Item item = Item.getByNameOrId(value);
                            if (item != null) {
                                this.holding = new ItemStack(item);
                            } else {
                                this.holding = new ItemStack(Blocks.AIR);
                            }
                        }

                    } catch (Exception var16) {
                    }
                } else if (line.contains("employedat|")) {
                    if (!"null".equals(value)) {
                        this.tempEmployLoc = V3.fromString(value);
                    } else {
                        this.tempEmployLoc = null;
                    }
                } else if (line.contains("job|")) {
                    if (!value.contentEquals("null")) {
                        BlockPos p;
                        String job = value.split(";")[0];
                        //建筑工
                        if (job.contentEquals(new TextComponentTranslation("container.sim.Vocation1",new Object[0]).getUnformattedText())) {
                            String v = value.split(";")[1];
                            //建筑/雇佣位置
                            p = V3.fromString(v).toBlockPos();
                            if(p==null){
                                break;
                            }
                                //建筑方向
                                int d2 = Integer.valueOf(value.split(";")[2]);
                                if (buildingBlueprint != null) {
                                    this.job = new JobBuilder(this, buildingBlueprint, p, d2, world);
                                } else {
                                    this.job = new JobBuilder(this, p, d2, world);
                                }


                            //面包师
                        } else if (job.contentEquals(new TextComponentTranslation("container.sim.Vocation6",new Object[0]).getUnformattedText())) {
                            if(this.tempEmployLoc==null){
                                break;
                            }
                            p = this.tempEmployLoc.toBlockPos();
                            if(p==null){
                                break;
                            }
                            this.job = new JobBaker(this, p, world);
                            //规划师
                        } else if (job.contentEquals(new TextComponentTranslation("container.sim.Vocation16",new Object[0]).getUnformattedText())) {
                            String[] v1=value.split(";");
                            if(v1.length>1){
                                String v = v1[1];
                                p = V3.fromString(v).toBlockPos();
                                if(p==null){
                                    break;
                                }
                                String terrainName = value.split(";")[2];
                                String terrainType = value.split(";")[3];
                                TerrainType terrainTypes = new TerrainType(terrainName, terrainType);
                                this.job = new JobTerrainFormer(this, terrainTypes, p, world);
                            }
                            //农民
                        } else if (job.contentEquals(new TextComponentTranslation("container.sim.Vocation5",new Object[0]).getUnformattedText())) {
                            if(this.tempEmployLoc==null){
                                break;
                            }
                            p = this.tempEmployLoc.toBlockPos();
                            if(p==null){
                                break;
                            }
                            FarmBox fb = ModSimLoader.getFarm(new V3(p));
                            this.job = new JobFarmer(this, p, world, fb);
                            //养猪户
                        } else if (job.contentEquals(new TextComponentTranslation("container.sim.Vocation13",new Object[0]).getUnformattedText())) {
                            if(this.tempEmployLoc==null){
                                break;
                            }
                            p = this.tempEmployLoc.toBlockPos();
                            if(p==null){
                                break;
                            }
                            this.job = new JobLivestockFarmer(this, p, new TextComponentTranslation("container.sim.job_Livestock_pig",new Object[0]).getUnformattedText(), world);
                            //养牛户
                        } else if (job.contentEquals(new TextComponentTranslation("container.sim.Vocation12",new Object[0]).getUnformattedText())) {
                            p = this.tempEmployLoc.toBlockPos();
                            if(p==null){
                                break;
                            }
                            this.job = new JobLivestockFarmer(this, p, new TextComponentTranslation("container.sim.job_Livestock_cow",new Object[0]).getUnformattedText(), world);
                            //养鸡户
                        } else if (job.contentEquals(new TextComponentTranslation("container.sim.Vocation14",new Object[0]).getUnformattedText())) {
                            if(this.tempEmployLoc==null){
                                break;
                            }
                            p = this.tempEmployLoc.toBlockPos();
                            if(p==null){
                                break;
                            }
                            this.job = new JobLivestockFarmer(this, p, new TextComponentTranslation("container.sim.job_Livestock_chicken",new Object[0]).getUnformattedText(), world);
                            //养羊户
                        } else if (job.contentEquals(new TextComponentTranslation("container.sim.Vocation27",new Object[0]).getUnformattedText())) {
                            if(this.tempEmployLoc==null){
                                break;
                            }
                            p = this.tempEmployLoc.toBlockPos();
                            if(p==null){
                                break;
                            }
                            this.job = new JobLivestockFarmer(this, p, new TextComponentTranslation("container.sim.job_Livestock_sheep",new Object[0]).getUnformattedText(), world);
                            //养兔户
                        } else if (job.contentEquals(new TextComponentTranslation("container.sim.Vocation29",new Object[0]).getUnformattedText())) {
                            if(this.tempEmployLoc==null){
                                break;
                            }
                            p = this.tempEmployLoc.toBlockPos();
                            if(p==null){
                                break;
                            }
                            this.job = new JobLivestockFarmer(this, p, new TextComponentTranslation("container.sim.job_Livestock_rabbit",new Object[0]).getUnformattedText(), world);
                            //牛奶农
                        } else if (job.contentEquals(new TextComponentTranslation("container.sim.Vocation20",new Object[0]).getUnformattedText())) {
                            if(this.tempEmployLoc==null){
                                break;
                            }
                            p = this.tempEmployLoc.toBlockPos();
                            if(p==null){
                                break;
                            }
                            this.job = new JobDairyFarmer(this, p, world);
                            //牧羊人
                        } else if (job.contentEquals(new TextComponentTranslation("container.sim.Vocation8",new Object[0]).getUnformattedText())) {
                            if(this.tempEmployLoc==null){
                                break;
                            }
                            p = this.tempEmployLoc.toBlockPos();
                            if(p==null){
                                break;
                            }
                            this.job = new JobShepherd(this, p, world);
                            //鸡蛋农
                        } else if (job.contentEquals(new TextComponentTranslation("container.sim.Vocation3",new Object[0]).getUnformattedText())) {
                            if(this.tempEmployLoc==null){
                                break;
                            }
                            p = this.tempEmployLoc.toBlockPos();
                            if(p==null){
                                break;
                            }
                            this.job = new JobEggFarmer(this, p, world);
                            //屠夫
                        } else if (job.contentEquals(new TextComponentTranslation("container.sim.Vocation15",new Object[0]).getUnformattedText())) {
                            if(this.tempEmployLoc==null){
                                break;
                            }
                            p = this.tempEmployLoc.toBlockPos();
                            if(p==null){
                                break;
                            }
                            this.job = new JobButcher(this, p, world);
                            //渔夫
                        } else if (job.contentEquals(new TextComponentTranslation("container.sim.Vocation18",new Object[0]).getUnformattedText())) {
                            if(this.tempEmployLoc==null){
                                break;
                            }
                            p = this.tempEmployLoc.toBlockPos();
                            if(p==null){
                                break;
                            }
                            this.job = new JobFisherman(this, p, world);
                            //食品商
                        } else if (job.contentEquals(new TextComponentTranslation("container.sim.Vocation26",new Object[0]).getUnformattedText())) {
                            if(this.tempEmployLoc==null){
                                break;
                            }
                            p = this.tempEmployLoc.toBlockPos();
                            if(p==null){
                                break;
                            }
                            this.job = new JobGrocer(this, p, world);
                            //士兵
                        } else if (job.contentEquals(new TextComponentTranslation("container.sim.Vocation7",new Object[0]).getUnformattedText())) {
                            if(this.tempEmployLoc==null){
                                break;
                            }
                            p = this.tempEmployLoc.toBlockPos();
                            if(p==null){
                                break;
                            }
                            this.job = new JobSoldier(this, p, world);
                            //伐木工
                        } else if (job.contentEquals(new TextComponentTranslation("container.sim.Vocation2",new Object[0]).getUnformattedText())) {
                            if(this.tempEmployLoc==null){
                                break;
                            }
                            p = this.tempEmployLoc.toBlockPos();
                            if(p==null){
                                break;
                            }
                            this.job = new JobLumberjack(this, p, world);
                            //制糖师
                        } else if (job.contentEquals(new TextComponentTranslation("container.sim.Vocation30",new Object[0]).getUnformattedText())) {
                            if(this.tempEmployLoc==null){
                                break;
                            }
                            p = this.tempEmployLoc.toBlockPos();
                            if(p==null){
                                break;
                            }
                            this.job = new JobSugar(this, p, world);
                            //矿工
                        } else if (job.contentEquals(new TextComponentTranslation("container.sim.Vocation4",new Object[0]).getUnformattedText())) {
                            if(this.tempEmployLoc==null){
                                break;
                            }
                            p = this.tempEmployLoc.toBlockPos();
                            if(p==null){
                                break;
                            }
                            MineBox mb = ModSimLoader.getMine(V3.fromBlockPos(p));
                            this.job = new JobMiner(this, p, world, mb);
                            //板砖工
                        } else if (job.contentEquals(new TextComponentTranslation("container.sim.Vocation25",new Object[0]).getUnformattedText())) {
                            if(this.tempEmployLoc==null){
                                break;
                            }
                            p = this.tempEmployLoc.toBlockPos();
                            if(p==null){
                                break;
                            }
                            this.job = new JobBrickMaker(this, p, world);
                            //玻璃制造商
                        } else if (job.contentEquals(new TextComponentTranslation("container.sim.Vocation17",new Object[0]).getUnformattedText())) {
                            if(this.tempEmployLoc==null){
                                break;
                            }
                            p = this.tempEmployLoc.toBlockPos();
                            if(p==null){
                                break;
                            }
                            this.job = new JobGlassMaker(this, p, world);
                            //建筑商
                        } else if (job.contentEquals(new TextComponentTranslation("container.sim.Vocation11",new Object[0]).getUnformattedText())) {
                            if(this.tempEmployLoc==null){
                                break;
                            }
                            p = this.tempEmployLoc.toBlockPos();
                            if(p==null){
                                break;
                            }
                            this.job = new JobBuildersMerchant(this, p, world);
                            //行长
                        } else if (job.contentEquals(new TextComponentTranslation("container.sim.Vocation31",new Object[0]).getUnformattedText())) {
                            if(this.tempEmployLoc==null){
                                break;
                            }
                            p = this.tempEmployLoc.toBlockPos();
                            if(p==null){
                                break;
                            }
                            this.job = new JobATM(this, p, world);
                            //杂货商
                        } else if (job.contentEquals(new TextComponentTranslation("container.sim.Vocation9",new Object[0]).getUnformattedText())) {
                            if(this.tempEmployLoc==null){
                                break;
                            }
                            p = this.tempEmployLoc.toBlockPos();
                            if(p==null){
                                break;
                            }
                            this.job = new JobMerchant(this, p, world);
                            //插花师
                        } else if (job.contentEquals(new TextComponentTranslation("container.sim.Vocation32",new Object[0]).getUnformattedText())) {
                            if(this.tempEmployLoc==null){
                                break;
                            }
                            p = this.tempEmployLoc.toBlockPos();
                            if(p==null){
                                break;
                            }
                            this.job = new JobFlower(this, p, world);
                            //赤脚大夫
                        } else if (job.contentEquals(new TextComponentTranslation("container.sim.Vocation33",new Object[0]).getUnformattedText())) {
                            if(this.tempEmployLoc==null){
                                break;
                            }
                            p = this.tempEmployLoc.toBlockPos();
                            if(p==null){
                                break;
                            }
                            this.job = new JobBarefootDoctor(this, p, world);
                            //妇产科医生
                        } else if (job.contentEquals(new TextComponentTranslation("container.sim.Vocation34",new Object[0]).getUnformattedText())) {
                            if(this.tempEmployLoc==null){
                                break;
                            }
                            p = this.tempEmployLoc.toBlockPos();
                            if(p==null){
                                break;
                            }
                            this.job = new JobAccoucheur(this, p, world);
                            //汉堡店经理
                        } else if (job.contentEquals(new TextComponentTranslation("container.sim.Vocation36",new Object[0]).getUnformattedText())) {
                            if(this.tempEmployLoc==null){
                                break;
                            }
                            p = this.tempEmployLoc.toBlockPos();
                            if(p==null){
                                break;
                            }
                            this.job = new JobBurgers(this, p, world);
                            //奶酪匠
                        } else if (job.contentEquals(new TextComponentTranslation("container.sim.Vocation21",new Object[0]).getUnformattedText())) {
                            if(this.tempEmployLoc==null){
                                break;
                            }
                            p = this.tempEmployLoc.toBlockPos();
                            if(p==null){
                                break;
                            }
                            this.job = new JobCheesemaker(this, p, world);
                            //麦当劳
                        } else if (job.contentEquals(new TextComponentTranslation("container.sim.Vocation35",new Object[0]).getUnformattedText())) {
                            if(this.tempEmployLoc==null){
                                break;
                            }
                            p = this.tempEmployLoc.toBlockPos();
                            if(p==null){
                                break;
                            }
                            this.job = new JobMcDonald(this, p, world);
                            //酒馆
                        } else if (job.contentEquals(new TextComponentTranslation("container.sim.Vocation37",new Object[0]).getUnformattedText())) {
                            if(this.tempEmployLoc==null){
                                break;
                            }
                            p = this.tempEmployLoc.toBlockPos();
                            if(p==null){
                                break;
                            }
                            this.job = new JobBartender(this, p, world);
                        }else if (job.contentEquals(new TextComponentTranslation("container.sim.Vocation10",new Object[0]).getUnformattedText())) {
                            //快递员
                            if(this.tempEmployLoc==null){
                                break;
                            }
                            p = this.tempEmployLoc.toBlockPos();
                            if(p==null){
                                break;
                            }
                            this.job = new JobCourier(this, p, world);
                        }


                    }

                    if (this.job != null) {
                        this.job.stage = 0;
                    }
                } else if (line.contains("jobstage|")) {
                    if (this.job != null) {
                        this.job.stage = 0;
                    } else {
                        this.tempStage = 0;
                    }
                } else if (line.contains("relationship|")) {
                    String[] rels = value.split(";");
                    String[] var12 = rels;
                    int var13 = rels.length;

                    for (int var14 = 0; var14 < var13; ++var14) {
                        String rel = var12[var14];
                        if (rel.length() > 0) {
                            this.relationships.add(new FolkRelationship(this, rel.toUpperCase()));
                        }
                    }
                } else if (line.contains("building|")) {
                    buildingBlueprint = ModSimLoader.getBlueprintsByName(value);
                }
            }
            MinecraftServer minecraftServer = FMLCommonHandler.instance().getMinecraftServerInstance();
            // 创建 NPC 实体
            Entity e1=minecraftServer.getEntityFromUuid(this.ID);
            EntityNpc e = new EntityNpc(world, this.ID);
            if(e1!=null){
                e= (EntityNpc) e1;
            }
            ModSimLoader.log.warn("NPC加载位置..."+this.pos.toString());
            e.setPosition(this.pos.x, this.pos.y, this.pos.z);
            e.setPositionAndUpdate(this.pos.x, this.pos.y, this.pos.z);
            e.isBeingCreated = true;


            e.theData = this;
            this.entity = e;
            world.spawnEntity(e);

            // 标记 NPC 已加载
            this.isLoaded = true;
            ModSimLoader.log.info("NPC,Uid："+this.ID+"加载完成");

        } catch (Exception e) {
            this.isLoaded = false;
//            this.onDeath(DamageSource.GENERIC);
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("loadFolk出错了,Uid："+uuid+",错误提示：【" + e.getMessage() + "】行数：" + element.getLineNumber());
        }finally {

        }
    }
    /**
     * 解析单条NPC数据行
     */
    private void parseNpcDataLine(String key, String value, BuildingBlueprint blueprint) {
        switch (key) {
            case "id":
                this.ID = UUID.fromString(value);
                break;
            case "isdie":
                handleNpcDeathStatus(Boolean.parseBoolean(value));
                break;
            case "fname":
                this.forename = capitalizeFirstLetter(value);
                break;
            case "sname":
                this.surname = capitalizeFirstLetter(value);
                break;
            case "gender":
                this.gender = Integer.parseInt(value);
                break;
            case "age":
                if (!key.contains("jobstage")) { // 避免与jobstage中的age混淆
                    this.age = Integer.parseInt(value);
                }
                break;
            case "race":
                this.assignRace(value, false);
                break;
            case "skin":
                this.race.skinName = value;
                this.skinName = value;
                break;
            case "pos":
                this.pos = V3.fromString(value);
                break;
            case "trait1":
                this.trait1 = Trait.getTraitFromName(value);
                break;
            case "trait2":
                this.trait2 = Trait.getTraitFromName(value);
                break;
            case "trait3":
                this.trait3 = Trait.getTraitFromName(value);
                break;
            case "pregnancy":
                this.pregnancyStage = Float.parseFloat(value);
                break;
            case "hunger":
                this.hunger = Double.parseDouble(value);
                break;
            case "buildingskill":
                this.skillBuilding = Float.parseFloat(value);
                break;
            case "farmingskill":
                this.skillFarming = Float.parseFloat(value);
                break;
            case "miningskill":
                this.skillMining = Float.parseFloat(value);
                break;
            case "holding":
                this.holding = parseHoldingItem(value);
                break;
            case "employedat":
                this.tempEmployLoc = "null".equals(value) ? null : V3.fromString(value);
                break;
            case "job":
                this.job = JobFactory.createJob(this, value, blueprint, this.world);
                if (this.job != null) {
                    this.job.stage = 0;
                }
                break;
            case "jobstage":
                if (this.job != null) {
                    this.job.stage = Integer.parseInt(value);
                } else {
                    this.tempStage = Integer.parseInt(value);
                }
                break;
            case "relationship":
                parseRelationships(value);
                break;
            case "building":
                blueprint = ModSimLoader.getBlueprintsByName(value);
                break;
            default:
                ModSimLoader.log.debug("NPC[{}] 未知数据键：{}={}", this.ID, key, value);
        }
    }
    /**
     * 处理NPC死亡状态（加载时）
     */
    private void handleNpcDeathStatus(boolean isDead) {
        this.isDead = isDead;
        if (this.isDead) {
            // 清理已死亡实体
            Entity entity = FMLCommonHandler.instance().getMinecraftServerInstance().getEntityFromUuid(this.ID);
            if (entity != null) {
                entity.setDead();
            }
            this.isLoaded = false;
            // 抛出异常终止后续解析（已死亡无需加载）
            throw new IllegalStateException("NPC[" + this.ID + "] 已死亡，终止加载");
        }
    }
    /**
     * 解析手持物品
     */
    private ItemStack parseHoldingItem(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        Item item = Item.getByNameOrId(value);
        return item != null ? new ItemStack(item) : new ItemStack(Blocks.AIR);
    }
    /**
     * 解析人际关系
     */
    private void parseRelationships(String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        String[] relEntries = value.split(";");
        for (String rel : relEntries) {
            if (rel.length() > 0) {
                this.relationships.add(new FolkRelationship(this, rel.toUpperCase()));
            }
        }
    }
    /**
     * 处理NPC实体生成（加载时）
     */
    private void handleNpcEntitySpawn(World world, UUID uuid) {
        // 1. 尝试获取已存在实体（避免重复生成）
        Entity entity = FMLCommonHandler.instance().getMinecraftServerInstance().getEntityFromUuid(uuid);
        EntityNpc e = (entity instanceof EntityNpc) ? (EntityNpc) entity : new EntityNpc(world, uuid);

        // 2. 设置实体属性
        e.isBeingCreated = true;
        if (this.pos != null) {
            e.setPositionAndUpdate(this.pos.x, this.pos.y, this.pos.z);
        } else {
            ModSimLoader.log.warn("NPC[{}] 位置无效，使用默认位置", uuid);
            e.setPositionAndUpdate(0, 64, 0); // 世界出生点附近
        }
        e.theData = this;
        this.entity = e;

        // 3. 检查实体是否已在世界中（避免重复spawn）
        if (!world.loadedEntityList.contains(e)) {
            world.spawnEntity(e);
        }
    }
    /**
     * 初始化通用属性
     */
    private void initializeCommonAttributes() {
        this.fs_rand = 0;
        this.isDead = false;
        this.holding = null;
        this.matingStage = -1.0F;
        this.tempStage = -1;
        this.timeSinceLastStatusUpdate = 0L;
        this.minuteUpdate = 0L;
        this.tempEmployLoc = null;
        this.lastPathAttempt = 0L;
    }
    /**
     * 设置NPC初始位置（新建时）
     */
    private void setInitialPosition(EntityNpc e, World world, boolean fromCommand) {
//        if (fromCommand && !world.playerEntities.isEmpty()) {
//            // 命令创建：生成在玩家位置
//            EntityPlayerMP player = (EntityPlayerMP) world.playerEntities.get(0);
//            e.setPositionAndUpdate(player.posX, player.posY, player.posZ);
//            this.pos = new V3(player.getPosition(), player.dimension);
//        } else {
            // 自然生成：随机位置（避免卡方块）
            Vec3d spawnPos = getSafeSpawnPosition(e, world);
            if (spawnPos == null) {
                BlockPos spawn = world.getSpawnPoint().up();
                Vec3d fallback = new Vec3d(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
                if (isPositionSafe(fallback, world, e)) {
                    spawnPos = fallback;
                }
            }
            if (spawnPos != null) {
                // P10(S0): getSafeSpawnPosition 返回的就是“脚部”位置，
                // 原实现再 +1 会让实体悬空，随后掉下来，并且数据坐标和实体不一致。
                e.setPositionAndUpdate(spawnPos.x, spawnPos.y, spawnPos.z);
                e.noClip = false;
                this.pos = new V3(spawnPos.x, spawnPos.y, spawnPos.z, world.provider.getDimension());
            } else {
                ModSimLoader.log.warn("找不到安全生成点，NPC 将延迟到区块加载后再定位: {}", this.ID);
            }
//        }
    }
    /**
     * 获取安全的随机生成位置（无方块阻挡）
     */
    private Vec3d getSafeSpawnPosition(EntityNpc e, World world) {
        // 第一步：优先在玩家10格范围内找安全位置（核心优化）
        if (!world.playerEntities.isEmpty()) {
            EntityPlayerMP player = (EntityPlayerMP) world.playerEntities.get(0);
            Vec3d playerPos = player.getPositionVector(); // 获取玩家当前位置（中心点）

            int attempt = 0;
            while (attempt < MAX_PATH_ATTEMPT) {
                // 1. 在玩家10格范围内生成随机偏移量（x/z轴：-10 ~ +10，y轴：-3 ~ +3，避免离玩家过高/过低）
                double offsetX = RANDOM.nextDouble() * 2 * PLAYER_SPAWN_RANGE - PLAYER_SPAWN_RANGE;
                double offsetZ = RANDOM.nextDouble() * 2 * PLAYER_SPAWN_RANGE - PLAYER_SPAWN_RANGE;
                double offsetY = RANDOM.nextDouble() * 6 - 3; // y轴偏移：-3 ~ +3，覆盖玩家上下区域

                // 2. 计算候选生成位置（基于玩家位置+偏移）
                double candidateX = playerPos.x + offsetX;
                double candidateY = playerPos.y + offsetY;
                double candidateZ = playerPos.z + offsetZ;
                Vec3d candidatePos = new Vec3d(candidateX, candidateY, candidateZ);

                // 3. 校验位置是否安全（核心安全规则）
                if (isPositionSafe(candidatePos, world, e)) {
                    return candidatePos; // 找到安全位置，直接返回
                }

                attempt++;
            }

            // 第二步：玩家10格内多次尝试失败→退回到玩家脚下（仍有安全校验）
            ModSimLoader.log.debug("玩家10格内未找到安全位置，尝试玩家脚下（尝试次数：{}）", MAX_PATH_ATTEMPT);
            Vec3d playerFeet = new Vec3d(player.posX, player.posY, player.posZ);
            if (isPositionSafe(playerFeet, world, e)) {
                return playerFeet;
            }
            return null;
        }

        // 第三步：无玩家在线→世界出生点（同样做安全校验）
        ModSimLoader.log.debug("无在线玩家，尝试世界出生点");
        BlockPos spawn = world.getSpawnPoint().up();
        Vec3d spawnPos = new Vec3d(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        return isPositionSafe(spawnPos, world, e) ? spawnPos : null;
    }
    /**
     * 校验位置是否安全：满足3个核心条件
     * 1. 脚下有实体方块（非空气/液体，防止掉下去）
     * 2. 自身位置和上方1格均为空气（防止卡方块）
     * 3. 周围无危险方块（岩浆、火，防止生成即死亡）
     */
    private boolean isPositionSafe(Vec3d candidatePos, World world, EntityNpc e) {
        BlockPos candidateBlockPos = new BlockPos(candidatePos); // 候选位置的方块坐标
        BlockPos feetPos = candidateBlockPos.down(); // 脚下1格（支撑点）
        BlockPos up1Pos = candidateBlockPos.up(); // 上方1格（头部位置）

        // 条件1：脚下必须是实体方块（非空气、非液体，确保能站立）
        Block feetBlock = world.getBlockState(feetPos).getBlock();
        if (world.isAirBlock(feetPos) || feetBlock instanceof BlockLiquid) {
            return false;
        }

        // 条件2：候选位置（身体）和上方1格（头部）必须是空气（防止卡进方块）
        if (!world.isAirBlock(candidateBlockPos) || !world.isAirBlock(up1Pos)) {
            return false;
        }

        // 条件3：周围3x3范围内无危险方块（岩浆、火，避免生成即受伤）
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos checkPos = feetPos.add(dx, 0, dz); // 脚下周围3x3区域
                Block checkBlock = world.getBlockState(checkPos).getBlock();
                if (checkBlock instanceof BlockFire ) {
                    return false;
                }
            }
        }

        // 额外校验：位置是否在实体碰撞范围内（避免和其他实体重叠）
        return world.getEntitiesWithinAABBExcludingEntity(e, e.getEntityBoundingBox().offset(
                candidatePos.x - e.posX, candidatePos.y - e.posY, candidatePos.z - e.posZ
        )).isEmpty();
    }
    /**
     * 首字母大写（统一姓名格式）
     */
    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    /**
     * @return void
     * @Author fan
     * @Description //TODO 解雇
     * @Date 10:37 2022/10/21
     * @Param []
     **/
    public void fire() {
        try {
            this.setStatus(new TextComponentTranslation("container.sim.folk_data.Wandering",new Object[0]).getUnformattedText());
            if (this.job != null) {
                //建筑工
                if (this.job.jobName.equals(new TextComponentTranslation("container.sim.Vocation1",new Object[0]).getUnformattedText())) {
                    JobBuilder jobBuilder = (JobBuilder) this.job;
                    if (jobBuilder != null && jobBuilder.conBox != null) {
                        jobBuilder.conBox.folk = null;
                    }
                }
                //规划师
                if (this.job.jobName.equals(new TextComponentTranslation("container.sim.Vocation16",new Object[0]).getUnformattedText())) {
                    JobTerrainFormer jobTerrainFormer = (JobTerrainFormer) this.job;
                    if (jobTerrainFormer != null && jobTerrainFormer.conBox != null) {
                        jobTerrainFormer.conBox.folk = null;
                    }
                }
                Building building = this.job.workPlace == null ? null : ModSimLoader.getBuildingByV3(this.job.workPlace);
                if (building != null) {
                    building.occupants.remove(this);
                    building.saveBuilding();
                }

            }
            this.job = null;
            // 清空手持物品，恢复自然下垂状态
            this.holding = ItemStack.EMPTY;
            if (this.entity != null) {
                this.entity.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, ItemStack.EMPTY);
            }
            this.stayPut = false;
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("fire出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 分配种族
     * @Date 15:57 2022/10/14
     * @Param []
     **/
    public void assignRace() {
        try {
            this.race = Races.raceList.get(new Random().nextInt(Races.raceList.size()));
            this.race.skinName = this.getTexture();
            this.skinName = this.race.skinName;
        } catch (Exception e) {
            ModSimLoader.log.error("NPC[{}] 分配种族失败：", this.ID, e);
            this.race = Races.raceList.get(0); //  fallback：默认第一个种族
        }
    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 根据种族名字获取种族
     * @Date 10:37 2022/10/21
     * @Param [existingRaceName, newChild]
     **/
    public void assignRace(String existingRaceName, boolean newChild) {
        try {
            Race targetRace = new Race();
            for (Race race : Races.raceList) {
                if (race.raceName.equals(existingRaceName)) {
                    targetRace = race;
                    break;
                }
            }
            this.race = targetRace != null ? targetRace : Races.raceList.get(0);
            this.race.skinName = this.getTexture();
            this.skinName = this.race.skinName;
            /*for (int i = 0; i < Races.raceList.size(); i++) {
                Race race1 = Races.raceList.get(i);
                if (existingRaceName.equals(race1.raceName)) {
                    race = race1;
                    break;
                }
            }
            this.race = race;
            this.race.skinName = this.getTexture();
            this.skinName = this.race.skinName;*/
        } catch (Exception e) {
            ModSimLoader.log.error("NPC[{}] 分配指定种族[{}]失败：", this.ID, existingRaceName, e);
            this.race = Races.raceList.get(0);
        }
    }

    /**
     * 向客户端发送皮肤地址
     */
    public void sendSkinPathToClient() {
        if (this.entity != null && this.skinName != null) {
            String skinName = this.skinName;
            NetWorkLoader.net.sendToAll(new PacketSendFolkSkin(this.entity.getUniqueID().toString(), skinName));
        }
    }

    /**
     * 保存NPC
     */
    /** P1: 保存到 SimStore（NBT，稳定 ID）。 */
    public void saveFolk() {
        try {
            if (this.ID == null) {
                return;
            }
            if (this.home != null) {
                this.homeId = String.valueOf(this.home.ID);
            }
            SimStore store = SimStore.get(this.world);
            if (store == null) {
                return;
            }
            store.putNpc(this.ID, this.toNBT());
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("saveFolk(NBT)出错了：" + e.getMessage() + " 行数：" + element.getLineNumber());
        }
    }

    public void saveFolkLegacy() {
        try {
            ModSimLoader.log.info("开始保存NPC数据，Uid："+this.ID);
            /*if (this.entity == null) {
                ModSimLoader.log.warn("NPC[{}] 实体为空，跳过保存", this.ID);
                return;
            }File npcFolder = new File(ModSimLoader.getSavesDataFolder(), NPC_FOLDER_NAME);
            if (!npcFolder.exists()) {
                npcFolder.mkdirs();
            }

            File npcFile = new File(npcFolder, this.entity.getUniqueID() + NPC_FILE_SUFFIX);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(npcFile))) {
                // 写入核心数据
                writer.write("id|" + this.ID + "\n");
                writer.write("isdie|" + this.isDead + "\n");
                writer.write("fname|" + this.forename + "\n");
                writer.write("sname|" + this.surname + "\n");
                writer.write("gender|" + this.gender + "\n");
                writer.write("age|" + this.age + "\n");
                writer.write("race|" + (this.race != null ? this.race.raceName : "") + "\n");
                writer.write("skin|" + this.skinName + "\n");
                writer.write("pos|" + (this.pos != null ? this.pos.toString() : "") + "\n");
                writer.write("trait1|" + (this.trait1 != null ? this.trait1.traitName : "") + "\n");
                writer.write("trait2|" + (this.trait2 != null ? this.trait2.traitName : "") + "\n");
                writer.write("trait3|" + (this.trait3 != null ? this.trait3.traitName : "") + "\n");
                writer.write("hunger|" + this.hunger + "\n");
                writer.write("pregnancy|" + this.pregnancyStage + "\n");
                writer.write("buildingskill|" + this.skillBuilding + "\n");
                writer.write("farmingskill|" + this.skillFarming + "\n");
                writer.write("miningskill|" + this.skillMining + "\n");

                // 写入手持物品
                String holdingStr = "";
                if (this.holding != null && this.holding.getItem() != null) {
                    holdingStr = this.holding.getItem().getRegistryName().toString();
                }
                writer.write("holding|" + holdingStr + "\n");

                // 写入职业相关
                writer.write("employedat|" + (this.tempEmployLoc != null ? this.tempEmployLoc.toString() : "null") + "\n");
                if (this.job != null) {
                    writer.write("job|" + this.job.jobName + "\n");
                    writer.write("jobstage|" + this.job.stage + "\n");
                } else {
                    writer.write("job|null\n");
                    writer.write("jobstage|-1\n");
                }

                // 写入人际关系
                writer.write("relationship|");
                for (int i = 0; i < this.relationships.size(); i++) {
                    writer.write(this.relationships.get(i).toString() + (i < this.relationships.size() - 1 ? ";" : ""));
                }
                writer.write("\n");
            }*/
            if (this.entity != null) {
                BufferedWriter writer = null;
                try {
                    File npcFolder = new File(ModSimLoader.getSavesDataFolder() + File.separator + "npc");
                    if (!npcFolder.exists()) {
                        npcFolder.mkdirs();
                    }
                    File logFile = new File(npcFolder + File.separator + this.entity.getUniqueID() + ".sk2");
                    writer = new BufferedWriter(new FileWriter(logFile));
                    writer.write("id|" + this.ID + "\n");
                    writer.write("isdie|" + this.isDead + "\n");
                    writer.write("fname|" + this.forename + "\n");
                    writer.write("sname|" + this.surname + "\n");
                    writer.write("gender|" + this.gender + "\n");
                    writer.write("age|" + String.valueOf(this.age) + "\n");
                    writer.write("race|" + this.race.raceName + "\n");
                    writer.write("skin|" + this.skinName + "\n");
                    writer.write("pos|" + this.pos.toString() + "\n");
                    ModSimLoader.log.warn("NPC--Uid："+this.ID+"--位置："+this.pos.toString());
                    writer.write("trait1|" + this.trait1.traitName + "\n");
                    writer.write("trait2|" + this.trait2.traitName + "\n");
                    writer.write("trait3|" + this.trait3.traitName + "\n");
                    writer.write("hunger|" + String.valueOf(this.hunger) + "\n");
                    writer.write("pregnancy|" + String.valueOf(this.pregnancyStage) + "\n");
                    writer.write("buildingskill|" + String.valueOf(this.skillBuilding) + "\n");
                    writer.write("farmingskill|" + String.valueOf(this.skillFarming) + "\n");
                    writer.write("miningskill|" + String.valueOf(this.skillMining) + "\n");
                    ItemStack itemStack = this.entity.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
                    String holdings = "";
                    if (itemStack != null && itemStack.getItem() != null) {
                        holdings = itemStack.getDisplayName();
                    }
                    writer.write("holding|" + holdings + "\n");
                    boolean isBuilding = false;
                    if (this.job != null) {
                        writer.write("employedat|" + this.job.workPlace.toString() + "\n");
                        //建筑师
                        if (this.job.jobName.contentEquals(new TextComponentTranslation("container.sim.Vocation1",new Object[0]).getUnformattedText())) {
                            JobBuilder jb = (JobBuilder) this.job;
                            if (jb.blueprint != null) {
                                writer.write("building|" + jb.blueprint.name + "\n");
                            }
                            writer.write("job|" + this.job.jobName + ";" + jb.workPlace.toString() + ";" + jb.direction + "\n");
                            //规划师
                        } else if (this.job.jobName.contentEquals(new TextComponentTranslation("container.sim.Vocation16",new Object[0]).getUnformattedText())) {
                            JobTerrainFormer jb = (JobTerrainFormer) this.job;
                            if (jb.terrainType != null) {
                                writer.write("job|" + this.job.jobName + ";" + jb.workPlace.toString() + ";" + jb.terrainType.terrainName + ";" + jb.terrainType.terrainType + "\n");
                            } else {
                                writer.write("job|" + this.job.jobName + ";\n");
                            }
                        } else {
                            writer.write("job|" + this.job.jobName + "\n");
                            writer.write("jobstage|" + this.job.stage + "\n");
                        }
                    } else {
                        writer.write("employedat|null\n");
                        writer.write("job|null\n");
                        writer.write("jobstage|-1\n");
                    }
                    writer.write("relationship|");
                    for (int i = 0; i < this.relationships.size(); ++i) {
                        writer.write(((FolkRelationship) this.relationships.get(i)).toString() + (i < this.relationships.size() - 1 ? ";" : ""));
                    }
                } catch (Exception e) {
                    StackTraceElement element = e.getStackTrace()[0];
                    ModSimLoader.log.error("saveFolk出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
                } finally {
                    try {
                        writer.close();
                    } catch (Exception var15) {
                        StackTraceElement element = var15.getStackTrace()[0];
                        ModSimLoader.log.error("saveFolk-writer出错了：" + var15.getMessage() + "行数：" + element.getLineNumber());
                    }

                }

            }
        } catch (Exception e) {
            ModSimLoader.log.error("NPC[{}] 保存失败：", this.ID, e);
        }
    }

    /**
     * @return java.lang.String
     * @Author fan
     * @Description //TODO 得到NPC名字
     * @Date 10:38 2022/10/21
     * @Param []
     **/
    public String getName() {
        String name = "";
        try {
            //男性
            if (this.gender == 0) {
                if (this.forename == null || this.forename == "") {
                    int i = new Random().nextInt(ConfigLoader.configMaleNames.length);
                    this.forename = ConfigLoader.configMaleNames[i].trim();
                }
            } else {
                if (this.forename == null || this.forename == "") {
                    int i = new Random().nextInt(ConfigLoader.configFemaleNames.length);
                    this.forename = ConfigLoader.configFemaleNames[i].trim();
                }

            }
            if (this.surname == null || this.surname == "") {
                int i = new Random().nextInt(ConfigLoader.configSurnames.length);
                this.surname = ConfigLoader.configSurnames[i].trim();
            }
            if (this.surname != null && this.forename != null) {
                String lang = FMLCommonHandler.instance().getCurrentLanguage();
                if ("en_US".equals(lang)) {
                    name = this.forename + " " + this.surname;
                } else {
                    name = this.surname + " " + this.forename;
                }
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("getName出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
        return name;
    }

    /**
     * @return com.trhsy.sim.entity.util.NpcIdentity
     * @Author fan
     * @Description //TODO 得到NPC身份信息
     * @Date 10:38 2022/10/21
     * @Param []
     **/
    public NpcIdentity getClientIdentity() {
        NpcIdentity npcIdentity = null;
        try {
            if (this.entity != null) {
                String skin = this.skinName;
                npcIdentity = new NpcIdentity(this.ID, this.getName(), String.valueOf(this.age), this.getStatusText(), this.getJobTitle(), this.getHousingStatus(), this.getRelationshipStatus(), this.getHunger(), String.valueOf(this.race.maturity), skin,this.isDead);
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("getClientIdentity出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
        return npcIdentity;
    }

    /**
     * @return java.lang.String
     * @Author fan
     * @Description //TODO 状态文字
     * @Date 10:38 2022/10/21
     * @Param []
     **/
    public String getStatusText() {
        return this.status;
    }

    /**
     * @return java.lang.String
     * @Author fan
     * @Description //TODO 工作名称
     * @Date 10:38 2022/10/21
     * @Param []
     **/
    public String getJobTitle() {
        /**被解雇的**/
        String s = new TextComponentTranslation("container.sim.gui_Folk_unemployed",new Object[0]).getUnformattedText();
        return this.job != null ? this.job.toString() : s;
//        return "Unemployed";
    }

    /**
     * @return java.lang.String
     * @Author fan
     * @Description //TODO 住房信息详情
     * @Date 10:39 2022/10/21
     * @Param []
     **/
    public String getHousingStatus() {
        String s = new TextComponentTranslation("container.sim.folkData3",new Object[0]).getUnformattedText();
        String s1 = new TextComponentTranslation("container.sim.folkData2",new Object[0]).getUnformattedText();
        return this.home != null ? s1 : s;
//        return "Homeowner";
    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 重生 当实体在世界中生成时调用。这包括玩家。
     * 恢复块快照时不要删除任何项目。防止重复
     * @Date 13:29 2022/10/17
     * @Param [world, bp]
     **/
    public void respawn(World world, BlockPos bp) {
        try {
            if (this.entity == null && this.isLoaded) {
                ModSimLoader.log.info("loadFolk 开始重生实体:"+this.surname+this.forename);
                //重生实体
                EntityNpc ef = new EntityNpc(world, this.ID);
                // P10(S0): 原实现先 this.entity = ef 之前就访问 this.entity.noClip，
                // 在 entity==null 时必然 NPE，重生实际上从未成功。
                ef.noClip = false;
                //坐标
                this.pos = new V3(bp, world.provider.getDimension());
                //更新坐标
                ef.setPositionAndUpdate((double) bp.getX() + 0.5D, (double) bp.getY() + 1.0D, (double) bp.getZ() + 0.5D);
                this.entity = ef;
                this.entity.theData = this;
                ModSimLoader.log.info("********************Npc:" + this.ID + "重生于x:" + this.pos.x + ",y:" + this.pos.y + ",z:" + this.pos.z);
                world.spawnEntity(ef);

            } else {
//                ModSimLoader.log.info("已重生，更新皮肤");
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("重生出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    /**
     * @return com.trhsy.sim.npc.FolkRelationship
     * @Author fan
     * @Description //TODO 与某一个NPC的关系
     * @Date 10:39 2022/10/21
     * @Param [folk2]
     **/
//    public FolkRelationship getRelationshipWith(NpcData folk2) {
//        FolkRelationship rels = null;
//        try {
//            for (FolkRelationship rel : this.relationships) {
//                NpcData npcData=rel.getOther();
//                if(npcData!=null){
//                    if (rel.getOther().ID == folk2.ID) {
//                        rels = rel;
//                        return rels;
//                    }
//                }
//
//            }
//        } catch (Exception e) {
//            StackTraceElement element = e.getStackTrace()[0];
//            ModSimLoader.log.error("getRelationshipWith出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
//        }
//        return rels;
//    }

    /**
     * @return java.lang.String
     * @Author fan
     * @Description //TODO 关系状态
     * @Date 10:39 2022/10/21
     * @Param []
     **/
    public String getRelationshipStatus() {
        String relationshipStatus = "";
        try {
            if (this.getFamily(EnumFamilyType.SPOUSE) != null) {
                //已婚
                relationshipStatus = new TextComponentTranslation("container.sim.relation_ship_Married",new Object[0]).getUnformattedText();
                return relationshipStatus;
            } else {
                //单身狗
                String single = new TextComponentTranslation("container.sim.folkData4",new Object[0]).getUnformattedText();
                //有对象
                String In_a_relationship = new TextComponentTranslation("container.sim.In_a_relationship",new Object[0]).getUnformattedText();
                relationshipStatus = this.getFamily(EnumFamilyType.PARTNER) != null ? In_a_relationship : single;
                return relationshipStatus;
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("getRelationshipStatus出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
        return relationshipStatus;
    }

    /**
     * @return java.lang.String
     * @Author fan
     * @Description //TODO 饥饿程度
     * @Date 10:40 2022/10/21
     * @Param []
     **/
    public String getHunger() {
        String hunger = "";
        try {
            if (this.hunger > 8) {
                //吃饱的
                hunger = new TextComponentTranslation("container.sim.folkData6",new Object[0]).getUnformattedText();
                return hunger;
            } else if (this.hunger > 4) {
                //有点饿
                hunger = new TextComponentTranslation("container.sim.folkData7",new Object[0]).getUnformattedText();
                return hunger;
            } else {
                //非常饿  快饿死了
                hunger = this.hunger > 1 ? new TextComponentTranslation("container.sim.folkData9",new Object[0]).getUnformattedText() : new TextComponentTranslation("container.sim.folkData8",new Object[0]).getUnformattedText();
                return hunger;
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("getHunger出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
        return hunger;
    }

    /**
     * @return com.trhsy.sim.npc.NpcData
     * @Author fan
     * @Description //TODO 家庭成员
     * @Date 10:40 2022/10/21
     * @Param [fam]
     **/
    public NpcData getFamily(EnumFamilyType fam) {
        FolkRelationship rels = null;
        try {
            for (FolkRelationship rel : this.relationships) {
                if (rel.familyType == fam) {
                    rels = rel;
                }
            }

        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("getFamily出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
        if (rels == null) {
            return null;
        } else {
            return rels.getOther();
        }
    }

    /**
     * 移除 租户
     */
    public void evict() {
        try {
            Building oldHome = this.home;
            if (oldHome != null) {
                if (oldHome.occupants != null) {
                    oldHome.occupants.remove(this);
                }
                oldHome.saveBuilding();
            }
            if (this.entity != null) {
                this.entity.detachHome();
            }
            this.home = null;
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            // P10(S0): 原 catch 里再次访问 this.home.saveBuilding()，
            // home 已经是 null 时二次 NPE。这里只记录，不再做危险操作。
            ModSimLoader.log.error("evict出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
            this.home = null;
        }
    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 设置状态
     * @Date 10:40 2022/10/21
     * @Param [sts]
     **/
    public void setStatus(String sts) {
        this.status = sts;
    }

    /**
     * 更新NPC状态
     **/
    public void onUpdate() {
        try {
            // P10(S0): 区块未加载时实体不 tick，数据层也必须暂停。
            // 只保留“已加载且实体已加入区块”的 NPC 推进任务。
            if (!isActive()) {
                this.pendingMoveTarget = null;
                this.noProgressCount = 0;
                return;
            }
            Long now = System.currentTimeMillis();
            //每秒更新
            if (now - this.timeSinceLastStatusUpdate > 1000L) {
                this.onSecond();
                this.timeSinceLastStatusUpdate = now;
            }
            //每分钟更新
            if (now - this.minuteUpdate > 60000L) {
                this.onMinute();
                this.minuteUpdate = now;
            }
            //如果当前NPC为空
//            if (this.entity == null) {
//                PlayerList players = FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList();
//                for (EntityPlayerMP player : players.getPlayers()) {
//                    //如果位置不为空并且在人员的80个内，不是服务器端
//                    if (this.pos != null && player.getDistance(this.pos.x, this.pos.y, this.pos.z) < 80.0D && !player.world.isRemote) {
//                        //设置当前NPC 已加载
//                        ModSimLoader.hasLoadedFolks = true;
//                        //重生此NPC
//                        ModSimLoader.log.info("onSecond 重生");
//                        this.respawn(player.world, this.pos.toBlockPos());
//                    }
//                }
//            }
            //当前NPC 不为空并且是客户端
            if (this.entity != null && !this.world.isRemote) {
                //更新NPC
                this.entity.onFolkUpdate();
                // 同步手持物品到实体（包括清空）
                if (this.holding != null && !this.holding.isEmpty()) {
                    this.entity.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, this.holding);
                } else {
                    this.entity.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, ItemStack.EMPTY);
                }

                //获取NPC位置
                this.pos = V3.fromVec3d(this.entity.getPositionVector());
                //获取NPC位面
                this.pos.dimension = this.entity.dimension;

                //是否应该取消重生
           /*boolean shouldDespawn = true;
            PlayerList players = FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList();
            for (EntityPlayerMP player:players.getPlayerList()){
                //如果位置不为空并且在人员的80个内
                if (player.getDistance(this.pos.x, this.pos.y, this.pos.z) < 80.0D) {
                    //设置false
                    shouldDespawn = false;
                }
            }

            if (shouldDespawn) {
                //设置NPC死亡（假死） 摧毁
                this.entity.setDead();
                this.entity.theData = null;
                this.entity = null;
            }*/
            }
            if (this.job != null && this.shouldWork()) {
                if (!this.job.atWork) {
                    //如果没有在工作的时候，并且工作是面包师，工作阶段设置为-1
                /*if (this.job.jobName.equals(new TextComponentTranslation("container.sim.Vocation6",new Object[0]).getUnformattedText())) {
                    JobBaker jobBaker = (JobBaker) this.job;
                    boolean b = jobBaker.theStage == -1;
                }*/
                }
                this.job.onUpdate();
                //有工作，不该工作的时候 实体不是空
            } else if (this.job != null && !this.shouldWork() && this.entity != null && this.job.atWork) {
                //等待
                this.setStatus(new TextComponentTranslation("container.sim.folk_data.Wandering",new Object[0]).getUnformattedText());
                // P8: 收工必须"整队重置"：stage=-1 让第二天各职业重建全新任务，
                // 并清空未执行完的任务队列/当前任务。原来只把 stage 置 0 且不清队列，
                // 第二天会把昨天没跑完的任务（如取货中途的 JobTaskCollectItems）
                // 重新 begin()，旧目的地被再次追加 → 重复坐标 → 收集死循环。
                this.job.stage = -1;
                this.job.jobTasks.clear();
                this.job.currentTask = null;
                //停止工作
                this.job.atWork = false;
                //停止工作
                this.job.onWayToWork = false;
                //自由活动
                this.stayPut = false;
            }
            //工作为空 不应该工作 实体不为空

            // 优化后的主逻辑（P3：白天/傍晚自由时间/深夜 三段）
            if ((this.job == null || !this.shouldWork()) && this.entity != null) {
                boolean isDay = ModSimLoader.isDayTime(this.world);
                if (!isDay) {
                    long nightTicks = this.world.getWorldTime() % 24000L;
                    boolean eveningFree = nightTicks < 12000L + EVENING_FREE_TICKS;
                    if (eveningFree) {
                        // 傍晚自由时间：工作被打断后先社交/闲逛一段时间，不急着回家
                        if (this.currentTask != null) {
                            this.currentTask.update();
                        } else {
                            discardNonEveningTasks();
                            if (this.tasks.isEmpty()) {
                                pickEveningTask();
                            }
                            if (!this.tasks.isEmpty()) {
                                this.currentTask = (Task) this.tasks.remove(0);
                                this.currentTask.begin();
                            }
                        }
                    } else {
                        // 深夜：清掉白天/傍晚遗留任务，强制回家睡觉
                        interruptCurrentTasks();
                        if (this.currentTask == null) {
                            discardNonNightTasks();
                            if (this.tasks.isEmpty()) {
                                handleNightLogic();
                            }
                            if (!this.tasks.isEmpty()) {
                                this.currentTask = (Task) this.tasks.remove(0);
                                this.currentTask.begin();
                            }
                        } else {
                            this.currentTask.update();
                        }
                    }
                } else {
                    // 白天：任务队列处理
                    if (this.tasks.size() > 0) {
                        if (this.currentTask == null) {
                            this.currentTask = (Task) this.tasks.remove(0);
                            this.currentTask.begin();
                        } else {
                            this.currentTask.update();
                        }
                    } else {
                        this.pickRandomTask();
                    }
                }
            }
           /* if ((this.job == null || !this.shouldWork()) && this.entity != null) {

                //任务不为空
                if (this.tasks.size()>0) {
                    //最近任务不为空
                   if (this.currentTask != null) {
                       this.currentTask.update();
                            //夜晚更新最近任务
                        if(!ModSimLoader.isDayTime(this.world)){
                            if(this.currentTask!=null){
                                //睡觉
                                String fs_n1=new TextComponentTranslation("container.sim.folk_data.Sleeping",new Object[0]).getUnformattedText();
                                String fs_n2=new TextComponentTranslation("container.sim.folk_data_Going_home",new Object[0]).getUnformattedText();
                                String fs_n3 =this.currentTask.getStatusText();
                                //不等于睡觉或者回家则直接完成
                                if(fs_n3!=null&&!fs_n3.equals(fs_n2)&&!fs_n3.equals(fs_n1)){
                                    this.currentTask.onTaskComplete();
                                    this.tasks.clear();
                                }
                                //晚上 不在家回家
                                if (this.home != null) {
                                    if(!this.isAtBuilding(this.home)){
                                        TaskGoTo taskGoTo=new TaskGoTo(this, -1L, this.home, fs_n2);
                                        this.currentTask=taskGoTo;
                                        this.currentTask.begin();
                                        //回家
//                                        this.addTask(taskGoTo);
                                    }
                                    if(fs_n3!=null&&this.isAtBuilding(this.home)&&!fs_n3.equals(fs_n1)){
                                        //睡觉
                                        this.addTask(new TaskSleep(this, -1L, new TextComponentTranslation("container.sim.folk_data.Sleeping",new Object[0]).getUnformattedText()));
                                    }
                                } else {
                                    if(fs_n3!=null&&!this.currentTask.getStatusText().equals(fs_n1)) {
                                        //睡觉 没有家直接睡觉
                                        this.addTask(new TaskSleep(this, -1L, new TextComponentTranslation("container.sim.folk_data.Sleeping",new Object[0]).getUnformattedText()));
                                    }
                                }
                            }

                        }
                    } else{
                       //重新获取任务
                       this.currentTask = (Task) this.tasks.get(0);
                       //开始任务
                       this.currentTask.begin();
                   }
                    //白天的话随机运行任务
                } else if (ModSimLoader.isDayTime(this.world)) {
                    this.pickRandomTask();
                } else if(!ModSimLoader.isDayTime(this.world)){
                    //晚上 不在家回家
                    if (this.home != null) {
                        String fs_n2=new TextComponentTranslation("container.sim.folk_data_Going_home",new Object[0]).getUnformattedText();
                        if (!this.isAtBuilding(this.home)) {
                            //回家
                            TaskGoTo taskGoTo=new TaskGoTo(this, -1L, this.home,fs_n2);
                            this.addTask(taskGoTo);
                        }else {
                            if(this.currentTask==null){
                                //回家
                                TaskGoTo taskGoTo=new TaskGoTo(this, -1L, this.home,fs_n2);
                                this.addTask(taskGoTo);
                            }
                        }
                        if(this.currentTask!=null){
                            //睡觉
                            String fs_n1=new TextComponentTranslation("container.sim.folk_data.Sleeping",new Object[0]).getUnformattedText();
                            if(this.isAtBuilding(this.home)&&!this.currentTask.getStatusText().equals(fs_n1)){
                                //睡觉
                                this.addTask(new TaskSleep(this, -1L, fs_n1));
                            }
                        }
                    } else {
                        //睡觉 没有家直接睡觉
                        this.addTask(new TaskSleep(this, -1L, new TextComponentTranslation("container.sim.folk_data.Sleeping",new Object[0]).getUnformattedText()));
                    }
                }
            }*/
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];

            ModSimLoader.log.error("NPCData-onUpdate出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
            e.printStackTrace();
        }
    }
    // 添加任务唯一性检查方法
    private boolean hasTaskOfType(Class<? extends Task> taskClass) {
        if (this.currentTask != null && taskClass.isInstance(this.currentTask)) {
            return true;
        }
        for (Object task : this.tasks) {
            if (taskClass.isInstance(task)) {
                return true;
            }
        }
        return false;
    }
    // 封装夜晚逻辑
    private void handleNightLogic() {
        // P3: 士兵 24 小时巡逻，不参与回家/睡觉
        if (isSoldier()) {
            return;
        }
        String fs_n1 = new TextComponentTranslation("container.sim.folk_data.Sleeping").getUnformattedText();
        String fs_n2 = new TextComponentTranslation("container.sim.folk_data_Going_home").getUnformattedText();

        // 优先级1：已在家 -> 睡觉
        if (this.home != null && this.isAtBuilding(this.home)) {
            if (!hasTaskOfType(TaskSleep.class)) {
                this.addTask(new TaskSleep(this, -1L, fs_n1));
            }
        }
        // 优先级2：有家但不在家 -> 走回家（走不到时由移动看门狗兜底传送）
        else if (this.home != null) {
            if (!hasTaskOfType(TaskGoTo.class)) {
                TaskGoTo task = new TaskGoTo(this, -1L, this.home, fs_n2);
                this.addTask(task);
            }
        }
        // 优先级3：无家可归 -> 直接睡觉
        else {
            if (!hasTaskOfType(TaskSleep.class)) {
                this.addTask(new TaskSleep(this, -1L, fs_n1));
            }
        }
    }

    /** P3: 傍晚自由时间：优先找人社交，找不到就闲逛；时长受剩余自由时间限制。 */
    private void pickEveningTask() {
        try {
            long nightTicks = this.world.getWorldTime() % 24000L;
            long remainTicks = (12000L + EVENING_FREE_TICKS) - nightTicks;
            long remainMs = Math.max(3000L, remainTicks * 50L);
            // P9: 傍晚自由时间先解决吃饭——背包没食物且饿了就去食品店，
            // 吃完/不饿才去社交/闲逛。
            if (this.hunger < 8.0D && !this.hasFoodInInventory()) {
                Building shop = findFoodShop();
                if (shop != null) {
                    this.addTask(new TaskBuyFood(this, Math.min(90000L, remainMs), shop));
                    return;
                }
            }
            NpcData partner = null;
            for (NpcData fd : ModSimLoader.folks) {
                if (fd == null || fd == this || fd.entity == null || fd.home == null) {
                    continue;
                }
                if (this.getV3() != null && fd.getV3() != null
                        && this.getV3().distanceSq(fd.getV3()) < 1600.0D) {
                    partner = fd;
                    break;
                }
            }
            if (partner != null && new Random().nextInt(2) == 0) {
                this.addTask(new TaskSocialise(this, Math.min(remainMs, 15000L), partner, this.home, false));
            } else {
                this.addTask(new TaskWander(this, Math.min(remainMs, 10000L)));
            }
        } catch (Exception e) {
            ModSimLoader.log.error("pickEveningTask出错了：" + e.getMessage());
        }
    }

    /** P3: 傍晚自由时间只保留社交/闲逛任务，丢掉工作/购物等白天任务。 */
    private void discardNonEveningTasks() {
        for (int i = this.tasks.size() - 1; i >= 0; i--) {
            Task t = this.tasks.get(i);
            if (!(t instanceof TaskSocialise) && !(t instanceof TaskWander)) {
                this.tasks.remove(i);
            }
        }
    }

    /** P3: 深夜只保留"回家/睡觉"任务，丢掉其余任务（原来 interrupt 不清队列，NPC 半夜还在跑白天任务）。 */
    private void discardNonNightTasks() {
        String sleeping = new TextComponentTranslation("container.sim.folk_data.Sleeping", new Object[0]).getUnformattedText();
        String goingHome = new TextComponentTranslation("container.sim.folk_data_Going_home", new Object[0]).getUnformattedText();
        for (int i = this.tasks.size() - 1; i >= 0; i--) {
            Task t = this.tasks.get(i);
            String s = t.getStatusText();
            boolean keep = t instanceof TaskSleep || t instanceof TaskGoTo
                    || (s != null && (s.equals(sleeping) || s.equals(goingHome)));
            if (!keep) {
                this.tasks.remove(i);
            }
        }
    }
    /** 强制终止当前任务（若不是回家/睡觉） */
    private void interruptCurrentTasks() {
        if (this.currentTask != null) {
            String sleeping = new TextComponentTranslation("container.sim.folk_data.Sleeping").getUnformattedText();
            String goingHome = new TextComponentTranslation("container.sim.folk_data_Going_home").getUnformattedText();
            String currentStatus = this.currentTask.getStatusText();
            if (currentStatus == null || (!currentStatus.equals(sleeping) && !currentStatus.equals(goingHome))) {
                this.currentTask.onTaskComplete(); // 终止当前任务
                this.currentTask = null; // 清空当前任务引用
            }
        }
    }
    /**
     * @return boolean
     * @Author fan
     * @Description //TODO 是否应该工作
     * @Date 10:40 2022/10/21
     * @Param []
     **/
    public boolean shouldWork() {
        try {
            if (this.entity == null) {
                return false;
            } else if (this.job == null) {
                return false;
            } else if (this.pregnancyStage > 0.0F) {
                return false;
                //士兵一直工作
            } else if (this.job != null && this.job.jobName.equals(new TextComponentTranslation("container.sim.Vocation7",new Object[0]).getUnformattedText())) {
                return true;
            } else {
                //白天
                return ModSimLoader.isDayTime(this.world);
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("shouldWork出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
        return false;
    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 每秒更新 只是更新关系/工作状态
     * @Date 10:40 2022/10/21
     * @Param []
     **/
    public void onSecond() {
        try {
            // P2+: 移动无进展看门狗
            tickMoveWatchdog();
            // P2+: 屋顶看门狗（部分情况下 NPC 会停在屋顶）
            tickRoofWatchdog();
            // P3: 深夜强制睡觉兜底（士兵除外）
            enforceNightSleep();
            // P4: 先不做定价系统——饿了先吃自己背包里的食物，保证不会被饿死
            trySelfFeed();
            if (this.entity != null) {
                //应该工作就去工作
                if (this.job != null && this.shouldWork()) {
                    this.job.onSecond();
                } else {
                    // 不工作时，确保没有手持物品
                    if (this.holding != null && !this.holding.isEmpty()) {
                        this.holding = ItemStack.EMPTY;
                    }
                }

                if (this.home != null) {
                    if (this.entity == null) {
                        return;
                    }

                    if (ModSimLoader.isDayTime(this.world) && this.stayPut && this.isSleeping && this.shouldWork()) {
                        //闲逛
                        this.setStatus(new TextComponentTranslation("container.sim.folk_data.Wandering",new Object[0]).getUnformattedText());
                        this.stayPut = false;
                        this.isSleeping = false;
                    }
                }

                // 同步手持物品到实体（包括清空）
                if (this.holding != null && !this.holding.isEmpty()) {
                    this.entity.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, this.holding);
                } else {
                    this.entity.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, ItemStack.EMPTY);
                }

                if (this.entity != null) {
                    for (NpcData fd : ModSimLoader.folks) {
                        if (fd != null && fd.ID != this.ID) {
                            //与某人的关系
                            FolkRelationship rel = this.getRelationshipWith(fd);
                            if (rel != null) {
                                if (new Random().nextInt(20) > 18) {
                                    if (new Random().nextInt(2) > 0) {
                                        rel.addLevel(1);
                                    } else {
                                        rel.addLevel(-1);
                                    }
                                }
                            } else if (new Random().nextInt(10) > 8) {
                                this.addRelationship(fd);
                            }
                        }
                    }
                }

            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("onSecond出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 每分钟更新 / 工作状态
     * @Date 10:41 2022/10/21
     * @Param []
     **/
    public void onMinute() {
        try {
            if (this.entity != null && !this.isDead) {
                //应该工作
                if (this.job != null && this.shouldWork()) {
                    this.job.onMinute();
                }
                //父亲
                NpcData father;
                //没有家
                if (this.home == null) {
                    //没有工作 或者有工作但没再工作的
                    if (this.job == null || (this.job != null && !this.shouldWork())) {
                        //未成年成年，跟随父母
                        if (this.age < this.race.maturity) {
                            father = this.getParent(0);
                            NpcData mother = this.getParent(1);
                            Building newHome = null;
                            if (father != null && father.home != null) {
                                newHome = father.home;
                            }

                            if (mother != null && mother.home != null) {
                                newHome = mother.home;
                            }

                            if (newHome != null) {
                                //设置家
                                newHome.occupants.add(this);
                                this.home = newHome;
                                // 已经搬到了
                                String s1 = new TextComponentTranslation("container.sim.npcData_onupdate1",new Object[0]).getUnformattedText();
                                // ，和TA父母一起
                                String s2 = new TextComponentTranslation("container.sim.npcData_onupdate2",new Object[0]).getUnformattedText();
                                ModSimLoader.sendChat(this.getName() + s1 + this.home.buildingName + s2);
                            }
                        } else {
                            //找到空房子
                            Building empty = ModSimLoader.getEmptyHome();
                            if (empty != null) {
                                empty.occupants.add(this);
                                if(this.entity!=null){
                                    this.entity.setHomePosAndDistance(empty.livingXYZ.toBlockPos(),1);
                                }
                                this.home = empty;
                                // 已搬入
                                String sText = this.getName() + new TextComponentTranslation("container.sim.npcData_onupdate3",new Object[0]).getUnformattedText() + empty.buildingName;
                                ModSimLoader.sendChat(sText);
                                //System.out.println("开始传送");
                                V3 v3=new V3(this.home.livingXYZ.x+0.5,this.home.livingXYZ.y,this.home.livingXYZ.z+0.5);
                                this.forceMoveToXYZ(v3);
                            }
                        }
                    }
                } else {
                    //任务回家 在家 男性 配偶不为空
                    if (this.currentTask instanceof TaskSleep && this.isAtBuilding(this.home) && this.gender == 0 && this.getSpouse() != null) {
                        //配偶
                        father = this.getSpouse();
                        //配偶在家
                        if (father.isAtBuilding(this.home) && father.pregnancyStage < 0.1F && new Random().nextInt(5) == 4) {
                            //生育任务
                            this.addTask(new TaskProcreate(this, 10000L, father));
                            //生育任务
                            father.addTask(new TaskProcreate(father, 10000L, this));
                            this.currentTask.completeTask();
                            father.currentTask.completeTask();
                        }
                    }
                    //生孩子
                    if (this.pregnancyStage >= 1.0F) {

                        //必须要有诊所/医院 检查是否有诊所/医院
                        /*container.sim.FolkData.Clinic=诊所
                        container.sim.FolkData.Hospital=医院*/
                        String fs_zshensuo=new TextComponentTranslation("container.sim.FolkData.Clinic",new Object[0]).getUnformattedText();
                        String fs_yiyuan=new TextComponentTranslation("container.sim.FolkData.Hospital",new Object[0]).getUnformattedText();
                        Building building=null;

                        building=ModSimLoader.getBuildingByName(fs_zshensuo);
                        if(building==null){
                            building=ModSimLoader.getBuildingByName(fs_yiyuan);
                        }
                        //找到医院/诊所
                        if(building!=null) {
                            //传送到医院或者诊所
                            V3 v3=building.livingXYZ;
                            this.forceMoveToXYZ(v3);

                            this.pregnancyStage = 0.0F;
                            new NpcData(this.world, this.getSpouse(), this);
                            //这里要播放那个宝宝笑的音频 找到了 音频文件 叫 birth
//                            SoundEvent soundEvent = new SoundEvent(new ResourceLocation(ModSim.MODID + ":birth"));
                            SoundEvent birth = SoundRegistry.BIRTH;
                            if (birth == null || birth.getRegistryName() == null) {
                                ModSimLoader.log.error("播放失败：sim:birth 声音事件未注册");
                            } else {
                            for (EntityPlayer entityPlayer : this.world.playerEntities) {
                                BlockPos pos=entityPlayer.getPosition();
                                ModSimLoader.log.info("播放 生孩子宝宝笑的那个声音:[x:" + pos.getX() + "],y:[" + pos.getY() + "],z:[" + pos.getZ() + "]");
                                this.world.playSound( null, pos.getX(),pos.getY(),pos.getZ(), birth, SoundCategory.BLOCKS, 1.0F, 1.0F);
                            }}
//                            for (EntityPlayer entityPlayer : world.playerEntities) {
//                                ModSimLoader.log.info("播放 生孩子宝宝笑的那个声音:[x:" + entityPlayer.posX + "],y:[" + entityPlayer.posY + "],z:[" + entityPlayer.posZ + "]");
//                                world.playSound((EntityPlayer) null, entityPlayer.posX, entityPlayer.posY, entityPlayer.posZ, soundEvent, SoundCategory.BLOCKS, 1.0F, 1.0F);
//                            }
                            //刚生了个宝宝
                            this.setStatus(new TextComponentTranslation("container.sim.folk_data_a_baby", new Object[0]).getUnformattedText());
                            String fs_ldzl =new TextComponentTranslation("container.sim.folk_data_a_baby", new Object[0]).getUnformattedText();
                            ModSimLoader.sendChat(this.getName() +"::"+  fs_ldzl);
                        }else{
                            //孕妇难产死了 谢谢点赞
                            ModSimLoader.log.info("NPC {} 因无医疗设施难产致死", this.getName());
                            // P10(S0): 统一走 onDeath 清理，避免只 setDead 留下幽灵数据。
                            this.onDeath(DamageSource.STARVE);
                            this.setStatus(new TextComponentTranslation("container.sim.folk_data_a_baby_day", new Object[0]).getUnformattedText());
                            String fs_ldzl =new TextComponentTranslation("container.sim.folk_data_a_baby_day", new Object[0]).getUnformattedText();
                            ModSimLoader.sendChat(this.getName() +"::"+  fs_ldzl);

                        }


                    }
                }

            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("onMinute出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }

    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 添加任务
     * @Date 10:41 2022/10/21
     * @Param [task]
     **/
    public void addTask(Task task) {
        if (task instanceof TaskSleep && this.currentTask != null && !(this.currentTask instanceof TaskSleep) && !this.currentTask.interruptSleep) {
            this.currentTask = null;
            this.tasks.clear();
        }

        this.tasks.add(task);
    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 下一个任务
     * @Date 10:41 2022/10/21
     * @Param []
     **/
    public void nextTask() {
        // P9: 原来无条件 this.tasks.remove(0)。当任务已完成/被 addTask(TaskSleep)
        // 清空/重复完成时，对空列表 remove(0) 会直接 ArrayIndexOutOfBoundsException
        // 崩服（TaskBuyFood 完成时踩到过）。改为安全移除。
        if (this.currentTask != null) {
            this.tasks.remove(this.currentTask);
            this.currentTask = null;
        } else if (!this.tasks.isEmpty()) {
            this.tasks.remove(0);
        }
        if (this.tasks.size() > 0) {
            this.currentTask = (Task) this.tasks.get(0);
            this.currentTask.begin();
        }

    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 选择随机任务
     * @Date 10:41 2022/10/21
     * @Param []
     **/
    public void pickRandomTask() {
        try {
            // P9: 没有职业/空闲时，吃饭是绝对第一优先级：
            // 背包有食物时 trySelfFeed 会每秒自动吃；没食物才去最近的食品店。
            if (this.hunger < 8.0D && !this.hasFoodInInventory()) {
                Building shop = findFoodShop();
                if (shop != null) {
                    this.addTask(new TaskBuyFood(this, 120000L, shop));
                    return;
                }
            }
            //有家并且随机任务是3
            if (this.home != null && new Random().nextInt(4) == 3) {
                //回家在家放松
                String s=new TextComponentTranslation("container.sim.folk_data_Relaxing_home",new Object[0]).getUnformattedText();
                addGoToTask(this.home,s, 5000, 8000);
                //this.addTask(new TaskGoTo(this, (long) (new Random().nextInt(30000) + 5000), this.home, new TextComponentTranslation("container.sim.folk_data_Relaxing_home",new Object[0]).getUnformattedText()));
            } else if (new Random().nextInt(4) == 3) {
                for (Building b : ModSimLoader.buildings) {
                    if (b.controlXYZ.getDistanceTo(this.pos) < 40 && new Random().nextInt(4) == 3) {
                        //住宅
                        String s1=new TextComponentTranslation("container.sim.sim_gui_BC_Residential",new Object[0]).getUnformattedText();
                        if (b.buildingType.contentEquals(s1)) {
                            for (NpcData fd : b.occupants) {
                                if (!(fd.currentTask instanceof TaskWander) && !(fd.currentTask instanceof TaskGoTo) && fd.currentTask != null) {
                                    if (fd.shouldWork()) {
                                        if (this.isAdult() != fd.isAdult()) {
                                            if (fd.ID == this.ID) {
                                                //社交任务
                                                // 社交任务，减少任务时长
                                                long duration = (long) (new Random().nextInt(5000) + 3000);
                                                this.addTask(new TaskSocialise(this, duration, fd, b, false));
//                                                this.addTask(new TaskSocialise(this, (long) (new Random().nextInt(15000) + 5000), fd, b, false));
                                                fd.currentTask = null;
                                                fd.tasks.clear();
                                                fd.addTask(new TaskSocialise(fd, duration, this, b, true));
//                                                fd.addTask(new TaskSocialise(fd, (long) (new Random().nextInt(15000) + 5000), this, b, true));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        //根据建筑类型添加任务，减少任务时长
                        //商业
                        if (b.buildingType.contentEquals(new TextComponentTranslation("container.sim.sim_gui_BC_Commercial",new Object[0]).getUnformattedText())) {
                            String s=new TextComponentTranslation("container.sim.folk_data_Shopping",new Object[0]).getUnformattedText() + b.buildingName;
                            int duration = new Random().nextInt(5000);
                            addGoToTask(b, s, duration, 8000);
//                            this.addTask(new TaskGoTo(this, (long) (new Random().nextInt(30000) + 5000), b, new TextComponentTranslation("container.sim.folk_data_Shopping",new Object[0]).getUnformattedText() + b.buildingName));
                            //工业
                        } else if (b.buildingType.contentEquals(new TextComponentTranslation("container.sim.sim_gui_BC_Industrial",new Object[0]).getUnformattedText())) {
                            String s=new TextComponentTranslation("container.sim.folk_data_Visiting",new Object[0]).getUnformattedText() + b.buildingName;
                            int duration = new Random().nextInt(5000);
                            addGoToTask(b, s, duration, 8000);
//                            this.addTask(new TaskGoTo(this, (long) (new Random().nextInt(30000) + 5000), b, new TextComponentTranslation("container.sim.folk_data_Visiting",new Object[0]).getUnformattedText() + b.buildingName));
                            //装饰
                        } else if (!b.buildingType.contentEquals(new TextComponentTranslation("container.sim.sim_gui_BC_Decorative",new Object[0]).getUnformattedText())) {
                            String s=new TextComponentTranslation("container.sim.folk_data_Visiting",new Object[0]).getUnformattedText() + b.buildingName;
                            int duration = new Random().nextInt(5000);
                            addGoToTask(b, s, duration, 8000);
//                            this.addTask(new TaskGoTo(this, (long) (new Random().nextInt(30000) + 5000), b, new TextComponentTranslation("container.sim.folk_data_Visiting",new Object[0]).getUnformattedText() + b.buildingName));
                            //其他
                        } else if (!b.buildingType.contentEquals(new TextComponentTranslation("container.sim.sim_gui_BC_Other",new Object[0]).getUnformattedText())) {
                            String s=new TextComponentTranslation("container.sim.folk_data_Visiting",new Object[0]).getUnformattedText() + b.buildingName;
                            int duration = new Random().nextInt(5000);
                            addGoToTask(b, s, duration, 8000);
//                            this.addTask(new TaskGoTo(this, (long) (new Random().nextInt(30000) + 5000), b, new TextComponentTranslation("container.sim.folk_data_Visiting",new Object[0]).getUnformattedText() + b.buildingName));
                        }
                        break;
                    }
                }
            } else {
                // 闲逛任务，减少任务时长

                this.addTask(new TaskWander(this, (long) (new Random().nextInt(5000) + 8000)));
//                this.addTask(new TaskWander(this, (long) (new Random().nextInt(30000) + 5000)));
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("pickRandomTask出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }
    // 提取添加 TaskGoTo 任务的方法
    private void addGoToTask(Building building, String translationKey, int minDuration, int maxDuration) {
        long duration = (long) (new Random().nextInt(maxDuration - minDuration) + minDuration);
        String status = new TextComponentTranslation(translationKey, new Object[0]).getUnformattedText();
        //商业
        String s=new TextComponentTranslation("container.sim.sim_gui_BC_Commercial", new Object[0]).getUnformattedText();
        if (building.buildingType.contentEquals(s)) {
            status += building.buildingName;
        }
        this.addTask(new TaskGoTo(this, duration, building, status));
    }
    /**
     * @return void
     * @Author fan
     * @Description //TODO 添加关系
     * @Date 10:43 2022/10/21
     * @Param [folk2]
     **/
    public void addRelationship(NpcData folk2) {
        try {
            boolean relExists = false;
            for (FolkRelationship rel : this.relationships) {
                if (rel.getOther() == folk2) {
                    relExists = true;
                }
            }

            if (!relExists) {
                this.relationships.add(new FolkRelationship(this, folk2, EnumFamilyType.UNRELATED));
                folk2.relationships.add(new FolkRelationship(folk2, this, EnumFamilyType.UNRELATED));
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("addRelationship出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }


    }

    /**
     * @return com.trhsy.sim.npc.NpcData
     * @Author fan
     * @Description //TODO 获取关系
     * @Date 11:32 2022/10/21
     * @Param [relCheck]
     **/
    public NpcData getRelation(EnumFamilyType relCheck) {
        NpcData npcData = null;
        FolkRelationship rel = null;
        try {
        /*for (FolkRelationship r : this.relationships) {
            if (r.familyType != relCheck) {
                npcData = r.getOther();
            }
        }*/
            Iterator var2 = this.relationships.iterator();
            do {
                if (!var2.hasNext()) {
                    return null;
                }

                rel = (FolkRelationship) var2.next();
            } while (rel.familyType != relCheck);
            npcData = rel.getOther();
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("getRelation出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
        return npcData;
    }

    /**
     * @return com.trhsy.sim.npc.NpcData
     * @Author fan
     * @Description //TODO 获取父亲
     * @Date 11:33 2022/10/21
     * @Param [gender]
     **/
    public NpcData getParent(int gender) {
        NpcData npcData = null;
        try {
            Iterator var2 = this.relationships.iterator();

            FolkRelationship rel;
            do {
                if (!var2.hasNext()) {
                    return null;
                }

                rel = (FolkRelationship) var2.next();
            } while (rel.familyType != EnumFamilyType.PARENT || rel.getOther().gender != gender);
            npcData = rel.getOther();
        /*for (FolkRelationship rel : this.relationships) {
            if (rel.familyType != EnumFamilyType.PARENT || rel.getOther().gender != gender) {
                npcData = rel.getOther();

            }
        }*/
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("getParent出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
        return npcData;
    }

    /**
     * @return com.trhsy.sim.npc.NpcData
     * @Author fan
     * @Description //TODO 获取配偶
     * @Date 11:33 2022/10/21
     * @Param []
     **/
    public NpcData getSpouse() {
        NpcData npcData = null;
        try {
        /*for (FolkRelationship rel : this.relationships) {
            if (rel.familyType != EnumFamilyType.SPOUSE) {
                npcData = rel.getOther();

            }
        }*/
            Iterator var1 = this.relationships.iterator();

            FolkRelationship rel;
            do {
                if (!var1.hasNext()) {
                    return null;
                }

                rel = (FolkRelationship) var1.next();
                //配偶
            } while (rel.familyType != EnumFamilyType.SPOUSE);
            npcData = rel.getOther();
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("getSpouse出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
        return npcData;
    }
    /**
     * P4: 只走路——没有路径就返回 false，**绝不传送**，也不登记看门狗目标。
     * 巡逻、追击、闲逛这类"机会性移动"用它，避免被传送系统干扰。
     */
    public boolean walkTo(V3 v3) {
        if (this.entity == null || v3 == null || !canReach(v3)) {
            return false;
        }
        try {
            V3 target = getAdjustedV3(v3);
            Path path = this.entity.getNavigator().getPathToXYZ(target.x, target.y, target.z);
            if (path == null) {
                return false;
            }
            this.entity.getNavigator().setPath(path, 1.0D);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void forceMoveToXYZs(V3 v3) {
        if (this.entity == null || v3 == null) {
            return;
        }
        if (!canReach(v3)) {
            return;
        }
        V3 target = getAdjustedV3(v3);
        // P10(S0): 先登记目标，让“无进展看门狗”有机会在路径失败时救援；
        // 但不再因为距离 >32 就立即传送。所有移动先走寻路。
        noteMoveTarget(v3);
        if (!isTargetLoaded(target)) {
            return;
        }
        try {
            Path path = this.entity.getNavigator().getPathToXYZ(target.x, target.y, target.z);
            if (path != null) {
                this.entity.getNavigator().setPath(path, 1.0D);
            }
        } catch (Exception e) {
            ModSimLoader.log.error("forceMoveToXYZs出错了：" + e.getMessage());
        }
    }
    /**
     * @return boolean
     * @Author fan
     * @Description //TODO 强制移动到
     * @Date 11:35 2022/10/21
     * @Param [v3]
     **/
    public boolean forceMoveToXYZ(V3 v3) {
        this.stayPut = false;
        if (this.entity == null || v3 == null) {
            return false;
        }
        if (!canReach(v3)) {
            return false;
        }
        V3 targetV3 = getAdjustedV3(v3);
        noteMoveTarget(v3);
        if (!isTargetLoaded(targetV3)) {
            return false;
        }
        try {
            // P10(S0): 删除了“距离 >32 格立即传送”。
            // 远距离移动必须先寻路；只有看门狗确认连续 5 秒无进展才允许救援传送。
            Path path = this.entity.getNavigator().getPathToXYZ(targetV3.x, targetV3.y, targetV3.z);
            if (path != null) {
                this.entity.getNavigator().setPath(path, 1.0D);
                return true;
            }
            return false;
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("forceMoveToXYZ出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
            return false;
        }
    }

    /**
     * P10(S0): 维度必须严格相等。
     * 原实现把 dimension==0 当通配符，导致主世界目标可以在地狱/末地执行，
     * 反之亦然；这是跨维度乱传的根源。
     */
    private boolean canReach(V3 v3) {
        return this.entity != null && v3 != null && v3.dimension == this.entity.dimension;
    }

    /** P10(S0): 目标方块所在区块必须已加载，禁止寻路/传送隐式加载区块。 */
    private boolean isTargetLoaded(V3 v3) {
        if (this.entity == null || v3 == null || this.entity.world == null) {
            return false;
        }
        if (v3.dimension != this.entity.dimension) {
            return false;
        }
        return this.entity.world.isBlockLoaded(new BlockPos(v3.x, v3.y, v3.z));
    }

    /** P10(S0): 传送落点必须是“脚下实体方块 + 两格空气”。 */
    private boolean isSafeTeleport(V3 v3) {
        if (!isTargetLoaded(v3)) {
            return false;
        }
        World w = this.entity.world;
        BlockPos feet = new BlockPos(v3.x, v3.y, v3.z);
        if (!w.isAirBlock(feet) || !w.isAirBlock(feet.up())) {
            return false;
        }
        IBlockState ground = w.getBlockState(feet.down());
        return ground.getMaterial().isSolid() && !ground.getMaterial().isLiquid();
    }

    /** P10(S0): 冷却中的安全传送；只接受 attemptTeleport 的落点检查结果。 */
    private boolean teleportWithCooldown(V3 target) {
        long now = System.currentTimeMillis();
        if (now - this.lastTeleportTime < TELEPORT_COOLDOWN_MS) {
            return false;
        }
        if (!isSafeTeleport(target)) {
            return false;
        }
        this.lastTeleportTime = now;
        boolean ok = false;
        try {
            if (this.entity instanceof net.minecraft.entity.EntityLivingBase) {
                ok = ((net.minecraft.entity.EntityLivingBase) this.entity).attemptTeleport(target.x, target.y, target.z);
            }
        } catch (Exception e) {
            ModSimLoader.log.error("teleportWithCooldown出错了：" + e.getMessage());
            return false;
        }
        if (ok) {
            this.pos = V3.fromVec3d(this.entity.getPositionVector());
            this.pos.dimension = this.entity.dimension;
            spawnPortalParticles();
        }
        return ok;
    }

    /** P2+: 记录当前移动目标，供"无进展看门狗"判断。 */
    private void noteMoveTarget(V3 v3) {
        // P9: 只有"目标变化"时才重置无进展计数。取货等任务会每秒重发
        // forceMoveToXYZ（原来甚至每 tick 重发），每次都把 noProgressCount
        // 清零，导致看门狗永远触发不了——NPC 卡在栅栏/障碍旁可以
        // "永远收集材料"。同目标重发不再重置，交给看门狗自己数秒。
        boolean sameTarget = this.pendingMoveTarget != null && this.pendingMoveTarget.equals(v3);
        this.pendingMoveTarget = new V3(v3.x, v3.y, v3.z, v3.dimension);
        if (!sameTarget) {
            if (this.entity != null) {
                this.lastProgressX = this.entity.posX;
                this.lastProgressZ = this.entity.posZ;
            }
            this.lastProgressCheck = System.currentTimeMillis();
            this.noProgressCount = 0;
        }
    }

    /**
     * P2+: 移动进度看门狗（每秒调用）。
     * 有目标、还没到，且连续 5 秒几乎没位移 → 强制救援传送到目标附近。
     * 这能兜住"卡在栅栏/门/方块缝里"这类非窒息型卡死。
     */
    private void tickMoveWatchdog() {
        if (this.entity == null || this.pendingMoveTarget == null) {
            return;
        }
        // P4: 士兵不参与"移动无进展"传送——巡逻/追击本身就是到处跑，
        // 只有窒息救援这种紧急情况才允许传送士兵。
        if (isSoldier()) {
            this.pendingMoveTarget = null;
            this.noProgressCount = 0;
            return;
        }
        // P4: 与战斗/静止状态解耦——士兵停下来打怪、商店站桩、睡觉都不算"卡死"
        if (this.stayPut || this.currentTask instanceof TaskSleep) {
            this.noProgressCount = 0;
            return;
        }
        if (this.entity instanceof net.minecraft.entity.EntityLivingBase) {
            net.minecraft.entity.EntityLivingBase lb = (net.minecraft.entity.EntityLivingBase) this.entity;
            if (lb.getRevengeTarget() != null) {
                this.noProgressCount = 0;
                this.lastProgressCheck = System.currentTimeMillis();
                this.lastProgressX = this.entity.posX;
                this.lastProgressZ = this.entity.posZ;
                return;
            }
        }
        V3 t = this.pendingMoveTarget;
        double dx = Math.abs(this.entity.posX - t.x);
        double dz = Math.abs(this.entity.posZ - t.z);
        if (dx < 2.0D && dz < 2.0D) {
            this.pendingMoveTarget = null;
            this.noProgressCount = 0;
            return;
        }
        long now = System.currentTimeMillis();
        if (now - this.lastProgressCheck < 1000L) {
            return;
        }
        double moved = Math.abs(this.entity.posX - this.lastProgressX)
                + Math.abs(this.entity.posZ - this.lastProgressZ);
        if (moved < 0.3D) {
            ++this.noProgressCount;
            if (this.noProgressCount >= 5) {
                ModSimLoader.log.warn("NPC {} 移动无进展，救援传送到 {}", this.getName(), t);
                forceRescueTeleport(t);
                this.pendingMoveTarget = null;
                this.noProgressCount = 0;
            }
        } else {
            this.noProgressCount = 0;
        }
        this.lastProgressX = this.entity.posX;
        this.lastProgressZ = this.entity.posZ;
        this.lastProgressCheck = now;
    }

    /** P2+: 传送全局限频：同一 NPC 至少间隔 2 秒才允许下一次强制传送。 */
    private static final long TELEPORT_MIN_INTERVAL_MS = 2000L;
    /** P4: 超过这个距离的工作类移动允许一次冷却传送（巡逻用 walkTo，不受影响）。 */
    private static final double LONG_DISTANCE_TELEPORT = 32.0D;

    /**
     * P2+: 无视冷却的定点传送（去工作超时、救援等硬性场景）。
     * 跨维度会被拒绝；使用原版 attemptTeleport（自带落点检查）。
     */
    public void forceTeleportTo(V3 v3) {
        if (this.entity == null || v3 == null) {
            return;
        }
        if (v3.dimension != this.entity.dimension) {
            ModSimLoader.log.warn("forceTeleportTo 跨维度被拒绝: 当前 {} 目标 {}", this.entity.dimension, v3.dimension);
            return;
        }
        // P4: 全局限频，避免窒息救援/看门狗/去工作超时在同一秒里互相抢着传送
        long now = System.currentTimeMillis();
        if (now - this.lastTeleportTime < TELEPORT_MIN_INTERVAL_MS) {
            return;
        }
        try {
            V3 adjusted = getAdjustedV3(v3);
            // P10(S0): 目标区块未加载/落点不安全就不传送。
            // 原实现失败后 setPositionAndUpdate 强塞，可能把 NPC 塞进方块或空中。
            if (!isSafeTeleport(adjusted)) {
                ModSimLoader.log.warn("forceTeleportTo 目标不安全或未加载，取消: {}", adjusted);
                return;
            }
            this.lastTeleportTime = now;
            boolean ok = this.entity instanceof net.minecraft.entity.EntityLivingBase
                    && ((net.minecraft.entity.EntityLivingBase) this.entity)
                    .attemptTeleport(adjusted.x, adjusted.y, adjusted.z);
            if (!ok) {
                ModSimLoader.log.warn("forceTeleportTo 落点被原版拒绝: {}", adjusted);
                return;
            }
            this.pos = V3.fromVec3d(this.entity.getPositionVector());
            this.pos.dimension = this.entity.dimension;
            spawnPortalParticles();
        } catch (Exception e) {
            ModSimLoader.log.error("forceTeleportTo出错了：" + e.getMessage());
        }
    }

    /** P3: 把 NPC 精确放到指定位置（睡觉用，不产生粒子）。 */
    public void snapTo(V3 v3) {
        if (this.entity == null || v3 == null) {
            return;
        }
        if (v3.dimension != this.entity.dimension) {
            return;
        }
        try {
            V3 adjusted = getAdjustedV3(v3);
            if (!isTargetLoaded(adjusted)) {
                return;
            }
            this.entity.setPositionAndUpdate(adjusted.x, adjusted.y, adjusted.z);
            this.entity.getNavigator().clearPath();
            this.pos = V3.fromVec3d(this.entity.getPositionVector());
            this.pos.dimension = this.entity.dimension;
        } catch (Exception e) {
            ModSimLoader.log.error("snapTo出错了：" + e.getMessage());
        }
    }

    /** P2+: 无视冷却的救援传送（看门狗/窒息救援使用）。 */
    private void forceRescueTeleport(V3 target) {
        forceTeleportTo(target);
    }

    /**
     * P4: 先不做定价系统——NPC 饿了先吃自己背包里的食物；
     * 背包没食物时由 pickRandomTask 去食品店免费取用（TaskBuyFood）。
     * 配合"饿到 0 也不会死"，保证 NPC 不会被饿死。
     */
    private void trySelfFeed() {
        try {
            if (this.hunger >= 8.0D || this.inventory == null || this.inventory.isEmpty()) {
                return;
            }
            for (int i = 0; i < this.inventory.size(); i++) {
                ItemStack st = this.inventory.get(i);
                if (st == null || st.isEmpty() || !(st.getItem() instanceof ItemFood)) {
                    continue;
                }
                ItemFood food = (ItemFood) st.getItem();
                int heal = Math.max(1, food.getHealAmount(st) / 2);
                this.hunger = Math.min(10.0D, this.hunger + heal);
                st.shrink(1);
                if (st.isEmpty()) {
                    this.inventory.remove(i);
                }
                ModSimLoader.log.info("{} 吃了背包里的食物，饥饿值 {}", this.getName(), this.hunger);
                return;
            }
        } catch (Exception e) {
            ModSimLoader.log.error("trySelfFeed出错了：" + e.getMessage());
        }
    }

    /**
     * P9: 背包里是否有可吃的东西。
     * 有 → trySelfFeed 每秒自动吃；没有 → 白天/傍晚的自由时间应优先去食品店。
     */
    public boolean hasFoodInInventory() {
        try {
            if (this.inventory == null) {
                return false;
            }
            for (ItemStack st : this.inventory) {
                if (st != null && !st.isEmpty() && st.getItem() instanceof ItemFood) {
                    return true;
                }
            }
        } catch (Exception e) {
            ModSimLoader.log.error("hasFoodInInventory出错了：" + e.getMessage());
        }
        return false;
    }

    /** P3: 士兵 24 小时巡逻，不受夜晚回家/睡觉规则影响。 */
    public boolean isSoldier() {
        return this.job instanceof com.trhsy.sim.npcCode.job.JobSoldier;
    }

    /** P4: 最近的食品店（面包师/杂货商/食品商/麦当劳/汉堡店），用于"饿了买吃的"。 */
    private Building findFoodShop() {
        Building best = null;
        double bestDist = Double.MAX_VALUE;
        for (Building b : ModSimLoader.buildings) {
            if (b == null || b.jobType == null || b.jobType.isEmpty()) {
                continue;
            }
            String jid = SimIds.jobIdFromDisplay(b.jobType);
            if (!(jid.equals(SimIds.vocation(6)) || jid.equals(SimIds.vocation(9))
                    || jid.equals(SimIds.vocation(26)) || jid.equals(SimIds.vocation(35))
                    || jid.equals(SimIds.vocation(36)))) {
                continue;
            }
            V3 ref = b.livingXYZ != null ? b.livingXYZ : b.controlXYZ;
            if (ref == null || this.getV3() == null) {
                continue;
            }
            double d = ref.distanceSq(this.getV3());
            if (d < bestDist) {
                bestDist = d;
                best = b;
            }
        }
        return best;
    }

    /**
     * P2+: 屋顶看门狗。NPC 高于"移动目标/住宅/工作点"5 格以上并持续 5 秒，
     * 判定为卡在屋顶（多半是被旧的向上落点搜索放上去的），救援传送到目标。
     */
    private void tickRoofWatchdog() {
        try {
            if (this.entity == null || this.world.isRemote) {
                return;
            }
            // P4: 战斗/静止时不判定屋顶（避免打怪时被"屋顶"逻辑传来传去）
            if (this.stayPut) {
                this.roofTicks = 0;
                return;
            }
            if (this.entity instanceof net.minecraft.entity.EntityLivingBase) {
                net.minecraft.entity.EntityLivingBase lb = (net.minecraft.entity.EntityLivingBase) this.entity;
                if (lb.getRevengeTarget() != null) {
                    this.roofTicks = 0;
                    return;
                }
            }
            // P4: 屋顶判定只针对"自己家"（x/z 8 格内、y 高 5 格以上）；
            // 巡逻中的士兵、在山上工作、在别人家楼顶都不算。
            if (this.home == null || this.home.livingXYZ == null || isSoldier()) {
                this.roofTicks = 0;
                return;
            }
            V3 ref = this.home.livingXYZ;
            double dx = Math.abs(this.entity.posX - ref.x);
            double dz = Math.abs(this.entity.posZ - ref.z);
            if (dx < 8.0D && dz < 8.0D && this.entity.posY - ref.y > 5.0D) {
                ++this.roofTicks;
                if (this.roofTicks >= 5) {
                    this.roofTicks = 0;
                    ModSimLoader.log.warn("NPC {} 疑似卡在屋顶，救援传送到 {}", this.getName(), ref);
                    forceRescueTeleport(ref);
                }
            } else {
                this.roofTicks = 0;
            }
        } catch (Exception e) {
            ModSimLoader.log.error("tickRoofWatchdog出错了：" + e.getMessage());
        }
    }

    /**
     * P3: 深夜强制睡觉兜底。士兵除外；深夜（自由时间结束后）10 秒仍未进入睡觉任务，
     * 就清掉任务、必要时传送回家、强制睡觉。任务系统万一没接上，这里保证不出"整夜闲逛"。
     */
    private void enforceNightSleep() {
        try {
            if (this.entity == null || this.world.isRemote || isSoldier()) {
                return;
            }
            boolean isDay = ModSimLoader.isDayTime(this.world);
            long nightTicks = this.world.getWorldTime() % 24000L;
            if (isDay || nightTicks < 12000L + EVENING_FREE_TICKS) {
                this.deepNightTicks = 0;
                return;
            }
            if (this.currentTask instanceof TaskSleep) {
                this.deepNightTicks = 0;
                return;
            }
            this.deepNightTicks += 20;
            if (this.deepNightTicks < 200) {
                return;
            }
            this.deepNightTicks = 0;
            this.tasks.clear();
            this.currentTask = null;
            if (this.home != null && this.home.livingXYZ != null && !this.isAtBuilding(this.home)) {
                this.forceTeleportTo(this.home.livingXYZ);
            }
            this.currentTask = new TaskSleep(this, -1L,
                    new TextComponentTranslation("container.sim.folk_data.Sleeping", new Object[0]).getUnformattedText());
            this.currentTask.begin();
            ModSimLoader.log.info("{} 深夜未入睡，已强制回家睡觉", this.getName());
        } catch (Exception e) {
            ModSimLoader.log.error("enforceNightSleep出错了：" + e.getMessage());
        }
    }
    /**
     * 检测当前位置前方是否有方块阻挡
     * @return 如果前方有方块阻挡返回 true，否则返回 false
     */
    private boolean isPathBlocked() {
        // 获取 NPC 的朝向
        Vec3d lookVec = this.entity.getLookVec();
        // 获取 NPC 当前位置
        BlockPos currentPos = this.entity.getPosition();
        // 计算前方位置
        BlockPos frontPos = currentPos.add(lookVec.x, lookVec.y, lookVec.z);

        // 检测前方位置是否有方块
        return!this.world.isAirBlock(frontPos);
    }
    /**
     * 检测当前路径是否被方块阻挡
     * @param checkDistance 检测距离（方块）
     * @return 如果路径被阻挡返回 true，否则返回 false
     */
    private boolean isPathBlocked(double checkDistance) {
        // 获取实体尺寸和位置信息
        double entityWidth = this.entity.width / 2.0;
        double entityHeight = this.entity.height;

        // 获取实体的朝向和当前位置
        Vec3d lookVec = this.entity.getLookVec();
        BlockPos currentPos = this.entity.getPosition();

        // 计算检测范围的起始和结束点
        Vec3d start = new Vec3d(
                currentPos.getX() + 0.5,
                currentPos.getY() + 0.1,  // 稍微高于地面，避免被地毯等方块阻挡
                currentPos.getZ() + 0.5
        );

        Vec3d end = start.add(new Vec3d(lookVec.x * checkDistance, lookVec.y * checkDistance, lookVec.z * checkDistance));

        // 创建一个表示实体大小的AABB盒子
        /*AxisAlignedBB entityAABB = new AxisAlignedBB(
                -entityWidth, 0, -entityWidth,
                entityWidth, entityHeight, entityWidth
        );*/

        // 使用光线追踪检测路径上的碰撞
        RayTraceResult result = entity.world.rayTraceBlocks(
                start,
                end,
                false,  // 是否忽略流体
                true,   // 是否检查碰撞盒
                false   // 是否忽略空气
        );

        // 如果检测到碰撞，说明路径被阻挡
        return result != null && result.typeOfHit == RayTraceResult.Type.BLOCK;
    }
    /**
     * 生成传送门粒子效果
     */
    private void spawnPortalParticles() {
        // P2: 服务端安全的粒子。原实现用 Minecraft.getMinecraft().world：
        // 专用服务器上为 null（NPE 被吞，每次传送刷错误日志），单机则可能把粒子
        // 生成在客户端世界。WorldServer.spawnParticle 会自动广播给附近玩家。
        try {
            World w = this.entity != null ? this.entity.world : this.world;
            if (!(w instanceof WorldServer) || w.isRemote) {
                return;
            }
            WorldServer ws = (WorldServer) w;
            double x = this.pos != null ? this.pos.x : this.entity.posX;
            double y = (this.pos != null ? this.pos.y : this.entity.posY) + 0.8D;
            double z = this.pos != null ? this.pos.z : this.entity.posZ;
            ws.spawnParticle(EnumParticleTypes.PORTAL, false, x, y, z, 24, 0.5D, 0.5D, 0.5D, 0.05D);
        } catch (Exception ignored) {
        }
    }
    /**
     * 获取调整后的 V3 位置
     * @param v3 原始 V3 位置
     * @return 调整后的 V3 位置
     */
    private V3 getAdjustedV3(V3 v3) {
        // P2+: 落点检查。只尝试"目标上方1格 / 目标同高 / 上方2格"，
        // **绝不向上爬到屋顶**（旧实现向上扫 4 格，室内目标被挡时会把 NPC 放到屋顶上）。
        double y = v3.y + 1.0D;
        World w = this.entity != null ? this.entity.world : this.world;
        if (w != null) {
            double[] candidates = {v3.y + 1.0D, v3.y, v3.y + 2.0D};
            for (double cy : candidates) {
                BlockPos pos = new BlockPos(v3.x, cy, v3.z);
                // P10(S0): 未加载区块不要 isAirBlock，避免隐式加载。
                if (!w.isBlockLoaded(pos)) {
                    continue;
                }
                if (!w.isAirBlock(pos) || !w.isAirBlock(pos.up())) {
                    continue;
                }
                // P10(S0): 必须有实体地面，不能传送到空中/液体上。
                IBlockState ground = w.getBlockState(pos.down());
                if (!ground.getMaterial().isSolid() || ground.getMaterial().isLiquid()) {
                    continue;
                }
                y = cy;
                break;
            }
        }
        return new V3(v3.x + 0.5D, y, v3.z + 0.5D, v3.dimension);
    }

    /**
     * 判断两条路径是否相同
     * @param path1 第一条路径
     * @param path2 第二条路径
     * @return 如果相同返回 true，否则返回 false
     */
    private boolean isSamePath(Path path1, Path path2) {
        return path1 == path2 && path1 != null && path2 != null;
    }
    /**
     * 计算弧度
     * @param degree
     * @return
     */
    private static double getRadian(double degree) {
        return degree * Math.PI / 180.0;
    }

    /**
     * 依靠xy计算两点直接距离
     * @param v_x
     * @param v_y
     * @param p_v
     * @param p_y
     * @return
     */
    public static double getDistance(double v_x, double v_y, double p_v, double p_y) {
        double radLat1 = getRadian(v_x);
        double radLat2 = getRadian(p_v);
        double a = radLat1- radLat2;// 两点纬度差
        double b = getRadian(v_y) - getRadian(p_y);// 两点的经度差
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) + Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)));
        return s ;

    }

    /**
     * @return boolean
     * @Author fan
     * @Description //TODO 在建筑内
     * @Date 11:36 2022/10/21
     * @Param [b]
     **/
    public boolean isAtBuilding(Building b) {
        return this.isAtBuilding(b, 1.5F);
    }

    /**
     * @return boolean
     * @Author fan
     * @Description //TODO 在建筑内
     * @Date 11:36 2022/10/21
     * @Param [b, maxDist]
     **/
    public boolean isAtBuilding(Building b, float maxDist) {
        if (this.entity == null) {
            return false;
            //住宅
        } else if (b.buildingType.toLowerCase().contentEquals(new TextComponentTranslation("container.sim.sim_gui_BC_Residential",new Object[0]).getUnformattedText())) {
            boolean b1=(float) b.livingXYZ.getDistanceTo(this.pos) < maxDist;
            return b1;
        } else {
            boolean b1=(float) b.controlXYZ.getDistanceTo(this.pos) < maxDist;
            return b1;
        }
    }

    /**
     * @return boolean
     * @Author fan
     * @Description //TODO 在建筑内
     * @Date 11:36 2022/10/21
     * @Param [v3]
     **/
    public boolean isAtLocation(V3 v3) {
        if (this.entity == null || v3 == null) {
            return false;
        }
        // P2: 加上维度与 y 的检查；x/z 容差保持 1.5 以兼容既有调用点。
        if (v3.dimension != 0 && v3.dimension != this.entity.dimension) {
            return false;
        }
        double fs_x = Math.abs(this.entity.posX - v3.x);
        double fs_z = Math.abs(this.entity.posZ - v3.z);
        double fs_y = Math.abs(this.entity.posY - v3.y);
        return fs_x < 1.5D && fs_z < 1.5D && fs_y < 4.0D;
    }

    /**
     * P9: "到达一个点"的判定（取货/送货用）：水平圆形半径 + 垂直容差。
     * 与 isAtLocation 的区别：水平用真实距离（圆形），更符合站在箱子附近的语义。
     */
    public boolean isAtDestination(V3 v3, double radiusXZ, double radiusY) {
        if (this.entity == null || v3 == null) {
            return false;
        }
        if (v3.dimension != 0 && v3.dimension != this.entity.dimension) {
            return false;
        }
        double dx = this.entity.posX - v3.x;
        double dz = this.entity.posZ - v3.z;
        double dy = Math.abs(this.entity.posY - v3.y);
        return dx * dx + dz * dz <= radiusXZ * radiusXZ && dy <= radiusY;
    }

    /**
     * @return boolean
     * @Author fan
     * @Description //TODO 正在建筑
     * @Date 11:36 2022/10/21
     * @Param [v3, dist]
     **/
    public boolean isAtLocation(V3 v3, int dist) {
        if (this.entity == null) {
            return false;
        } else {
            return Math.abs(this.entity.posX - v3.x) < (double) dist && Math.abs(this.entity.posZ - v3.z) < (double) dist;
        }
    }

    /**
     * @return boolean
     * @Author fan
     * @Description //TODO 正在建筑
     * @Date 11:36 2022/10/21
     * @Param [blockPos]
     **/
    public boolean isAtLocation(BlockPos blockPos) {
        if (this.entity == null) {
            return false;
        } else {
            return (new BlockPos(this.entity)).distanceSq(blockPos) < 2.0D;
        }
    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 调整关系
     * @Date 11:37 2022/10/21
     * @Param [other, amount]
     **/
    public void adjustRelationship(NpcData other, int amount) {
        FolkRelationship rel = this.getRelationshipWith(other);
        if (rel != null) {
            rel.addLevel(amount);
        } else {
            this.addRelationship(other);
        }

    }

    /**
     * @return boolean
     * @Author fan
     * @Description //TODO 是成年人
     * @Date 18:57 2022/10/16
     * @Param []
     **/
    public boolean isAdult() {
        return this.age >= this.race.maturity;
    }

    /**
     * @return java.lang.String
     * @Author fan
     * @Description //TODO 获取皮肤
     * @Date 13:17 2022/10/17
     * @Param []
     **/
    public String getTexture() {
        String texture = "";
        try {
            //System.out.println("实体人性别："+theData.gender);
            if (this.skinnumber == 0) {
                texture = this.skinName;
            } else {
                if (this.gender == 0) {
                    texture = "male" + this.skinnumber + ".png";
                } else {
                    texture = "female" + this.skinnumber + ".png";
                }
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("getTexture出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
        return texture;
    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO NPC死亡
     * @Date 15:20 2022/10/18
     * @Param [cause]
     **/
    public void onDeath(DamageSource cause) {
        try {
            // P10(S0): 幂等：重复回调（实体死亡 + 数据层清理）只执行一次。
            if (this.isDead || this.world == null || this.world.isRemote) {
                return;
            }
            String deathMessage;
            if (cause == DamageSource.STARVE) {
                //张三 饿死了。他们当时18岁。
                deathMessage = this.getName() + new TextComponentTranslation("container.sim.folk_data_death_by_They", new Object[0]).getUnformattedText() + this.age + new TextComponentTranslation("container.sim.folk_data_death_by_years", new Object[0]).getUnformattedText();
            } else if (cause == DamageSource.IN_WALL) {
                //被活埋
                deathMessage = this.getName() + new TextComponentTranslation("container.sim.folk_data_death_by_died", new Object[0]).getUnformattedText() + new TextComponentTranslation("container.sim.folk_data_death_under", new Object[0]).getUnformattedText() + this.age + new TextComponentTranslation("container.sim.folk_data_death_by_years", new Object[0]).getUnformattedText();
            } else {
                //已经死了
                deathMessage = this.getName() + new TextComponentTranslation("container.sim.folk_data_death_by_were", new Object[0]).getUnformattedText() + this.age + new TextComponentTranslation("container.sim.folk_data_death_by_years", new Object[0]).getUnformattedText();
            }
            if (this.pos != null) {
                deathMessage += ":" + this.pos.x + "," + this.pos.y + "," + this.pos.z;
            }
            this.isDead = true;

            // 1) 从住宅/所有建筑中移除
            Building oldHome = this.home;
            if (oldHome != null) {
                oldHome.occupants.remove(this);
                oldHome.saveBuilding();
            }
            for (Building building : ModSimLoader.buildings) {
                if (building != null && building.occupants != null) {
                    building.occupants.remove(this);
                }
            }

            // 2) 解雇（内部会清理控制箱/建筑雇员）
            if (this.job != null) {
                this.fire();
            }

            // 3) 清理其他人关系里的引用（快照遍历，避免 remove 时跳过）
            for (NpcData other : new ArrayList<>(ModSimLoader.folks)) {
                if (other == null || other == this) {
                    continue;
                }
                boolean changed = other.relationships.removeIf(rel ->
                        rel != null && rel.folk2 != null && this.ID != null && this.ID.equals(rel.folk2));
                if (changed) {
                    other.saveFolk();
                }
            }
            this.relationships.clear();

            // 4) 实体死亡 + 数据移除（全服只保留一份清理路径）
            if (this.entity != null) {
                this.entity.theData = null;
                this.entity.setDead();
                this.entity = null;
            }
            final UUID deadId = this.ID;
            ModSimLoader.folks.removeIf(n -> n == this || (n != null && n.ID != null && n.ID.equals(deadId)));
            SimStore store = SimStore.get(this.world);
            if (store != null) {
                store.removeNpc(deadId);
            }
            ModSimLoader.sendChat(deathMessage);
            ModSimLoader.log.info("NPC 已清理: {} ({})", this.getName(), deadId);
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("npcDeath-onDeath出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    /**
     * @return java.lang.String
     * @Author fan
     * @Description //TODO 找到保存位置
     * @Date 15:43 2022/10/18
     * @Param []
     **/
    public String getSaveFolder() {
        DimensionManager d = new DimensionManager();
        String worldPath = "";
        worldPath = DimensionManager.getCurrentSaveRootDirectory().getAbsolutePath() + File.separator + "sim";
        return worldPath;
    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 生成特征
     * @Date 17:30 2022/10/18
     * @Param []
     **/
    public void generateTraits() {
        try {
            Random rand = new Random();
            Trait[] traits1 = Traits.traitList;
            //Trait 1
            this.trait1 = Trait.getTraitFromName(traits1[rand.nextInt(traits1.length - 1)].traitName);


            //Trait 2
            this.trait2 = Trait.getTraitFromName(traits1[rand.nextInt(traits1.length - 1)].traitName);

            while (this.trait2 == this.trait1 || traitHasOpposite(this.trait2)) {
                this.trait2 = Trait.getTraitFromName(traits1[rand.nextInt(traits1.length - 1)].traitName);
            }


            //Trait 3
            this.trait3 = Trait.getTraitFromName(traits1[rand.nextInt(traits1.length - 1)].traitName);

            while (this.trait3 == this.trait2 || this.trait3 == this.trait1 || traitHasOpposite(this.trait3)) {
                this.trait3 = Trait.getTraitFromName(traits1[rand.nextInt(traits1.length - 1)].traitName);
            }


        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("generateTraits出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }

    }

    /**
     * 特质具有相反的性质
     *
     * @param trait
     * @return
     */
    public boolean traitHasOpposite(Trait trait) {
        try {
            if (Trait.getTraitFromName(trait.traitName).traitOpposite != null) {
                if (trait.traitName.contains(Trait.getTraitFromName(trait.traitName).traitOpposite.traitName)) {
                    return true;
                }
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("traitHasOpposite出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
        return false;
    }

    /**
     * 有特点
     *
     * @param trait
     * @return
     */
    public boolean hasTrait(Trait trait) {
        boolean flag = true;
        try {
            if (this.trait1.traitName.contentEquals(trait.traitName) || this.trait2.traitName.contentEquals(trait.traitName) || this.trait3.traitName.contentEquals(trait.traitName)) {
                flag = true;
            } else {
                flag = false;
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("hasTrait出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
        return flag;
    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 添加到随身物品栏
     * @Date 13:23 2022/10/21
     * @Param [is]
     **/
    public void addToInventory(ItemStack is) {
        for (int i = 0; i < this.inventory.size(); ++i) {
            ItemStack itemStack = this.inventory.get(i);
            if (itemStack.isItemEqual(is)) {
                int fsCount= itemStack.getCount() + is.getCount();
                itemStack.setCount(fsCount);
                return;
            }
        }

        this.inventory.add(is);
    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 清除状态
     * @Date 13:24 2022/10/21
     * @Param []
     **/
    public void clearStatus() {
        this.status = new TextComponentTranslation("container.sim.folk_data.Wandering",new Object[0]).getUnformattedText();
    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 雇佣地点
     * @Date 13:24 2022/10/21
     * @Param [pos, jobName, world]
     **/
    public void hireAt(V3 pos, String jobName, World world) {
        try {
            Block block = world.getBlockState(pos.toBlockPos()).getBlock();
            if (block == BlockLoader.blockControlBox) {
                BlockControlBox cont = (BlockControlBox) block;
                cont.employees.add(this);
            }
            V3 v3 = pos;
            Building building = ModSimLoader.getBuildingByV3(new V3(pos.x,pos.y-1,pos.z));
            if(building!=null){
                building.occupants.add(this);

                if (building.livingXYZ != null) {
                    v3 = new V3(building.livingXYZ.x,building.livingXYZ.y+1,building.livingXYZ.z);
                }

            }
            this.forceMoveToXYZ(v3);
            //面包师
            if (jobName.contentEquals(new TextComponentTranslation("container.sim.Vocation6",new Object[0]).getUnformattedText())) {
                this.job = new JobBaker(this, v3.toBlockPos(), world);
                //地形规划师
            } else if (jobName.contentEquals(new TextComponentTranslation("container.sim.Vocation16",new Object[0]).getUnformattedText())) {
                this.job = new JobTerrainFormer(this, v3, world);
                //建筑师
            } else if (jobName.contentEquals(new TextComponentTranslation("container.sim.Vocation1",new Object[0]).getUnformattedText())) {
                this.job = new JobBuilder(this, v3, 0, world);
                // 确保建筑师手持圆石
                this.holding = new ItemStack(Blocks.COBBLESTONE);
                if (this.entity != null) {
                    this.entity.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, this.holding);
                }
                //屠夫
            }else if (jobName.contentEquals(new TextComponentTranslation("container.sim.Vocation15",new Object[0]).getUnformattedText())) {
                this.job = new JobButcher(this, v3.toBlockPos(), world);
                //食品商
            } else if (jobName.contentEquals(new TextComponentTranslation("container.sim.Vocation26",new Object[0]).getUnformattedText())) {
                this.job = new JobGrocer(this, v3.toBlockPos(), world);
                //养牛户
            } else if (jobName.contentEquals(new TextComponentTranslation("container.sim.Vocation12",new Object[0]).getUnformattedText())) {
                this.job = new JobLivestockFarmer(this, v3.toBlockPos(), new TextComponentTranslation("container.sim.job_Livestock_cow",new Object[0]).getUnformattedText(), world);
                //养猪户
            } else if (jobName.contentEquals(new TextComponentTranslation("container.sim.Vocation13",new Object[0]).getUnformattedText())) {
                this.job = new JobLivestockFarmer(this, v3.toBlockPos(), new TextComponentTranslation("container.sim.job_Livestock_pig",new Object[0]).getUnformattedText(), world);
                //养鸡户
            } else if (jobName.contentEquals(new TextComponentTranslation("container.sim.Vocation14",new Object[0]).getUnformattedText())) {
                this.job = new JobLivestockFarmer(this, v3.toBlockPos(), new TextComponentTranslation("container.sim.job_Livestock_chicken",new Object[0]).getUnformattedText(), world);
                //养羊户
            } else if (jobName.contentEquals(new TextComponentTranslation("container.sim.Vocation28",new Object[0]).getUnformattedText())) {
                this.job = new JobLivestockFarmer(this, v3.toBlockPos(), new TextComponentTranslation("container.sim.job_Livestock_sheep",new Object[0]).getUnformattedText(), world);
                //养兔户
            } else if (jobName.contentEquals(new TextComponentTranslation("container.sim.Vocation29",new Object[0]).getUnformattedText())) {
                this.job = new JobLivestockFarmer(this, v3.toBlockPos(), new TextComponentTranslation("container.sim.job_Livestock_rabbit",new Object[0]).getUnformattedText(), world);
                //牧羊人
            } else if (jobName.contentEquals(new TextComponentTranslation("container.sim.Vocation8",new Object[0]).getUnformattedText())) {
                this.job = new JobShepherd(this, v3.toBlockPos(), world);
                //牛奶农
            } else if (jobName.contentEquals(new TextComponentTranslation("container.sim.Vocation20",new Object[0]).getUnformattedText())) {
                this.job = new JobDairyFarmer(this, v3.toBlockPos(), world);
                //伐木工
            } else if (jobName.contentEquals(new TextComponentTranslation("container.sim.Vocation2",new Object[0]).getUnformattedText())) {
                this.job = new JobLumberjack(this, v3.toBlockPos(), world);
                //士兵
            } else if (jobName.contentEquals(new TextComponentTranslation("container.sim.Vocation7",new Object[0]).getUnformattedText())) {
                this.job = new JobSoldier(this, v3.toBlockPos(), world);
                //渔夫
            } else if (jobName.contentEquals(new TextComponentTranslation("container.sim.Vocation18",new Object[0]).getUnformattedText())) {
                this.job = new JobFisherman(this, v3.toBlockPos(), world);
                //蛋农
            } else if (jobName.contentEquals(new TextComponentTranslation("container.sim.Vocation3",new Object[0]).getUnformattedText())) {
                this.job = new JobEggFarmer(this, v3.toBlockPos(), world);
                //制糖师
            } else if (jobName.contentEquals(new TextComponentTranslation("container.sim.Vocation30",new Object[0]).getUnformattedText())) {
                this.job = new JobSugar(this, v3.toBlockPos(), world);
                //板砖工匠
            } else if (jobName.contentEquals(new TextComponentTranslation("container.sim.Vocation25",new Object[0]).getUnformattedText())) {
                this.job = new JobBrickMaker(this, v3.toBlockPos(), world);
                //玻璃制造商
            } else if (jobName.contentEquals(new TextComponentTranslation("container.sim.Vocation17",new Object[0]).getUnformattedText())) {
                this.job = new JobGlassMaker(this, v3.toBlockPos(), world);
                //建筑商
            } else if (jobName.contentEquals(new TextComponentTranslation("container.sim.Vocation11",new Object[0]).getUnformattedText())) {
                this.job = new JobBuildersMerchant(this, v3.toBlockPos(), world);
                //行长
            } else if (jobName.contentEquals(new TextComponentTranslation("container.sim.Vocation31",new Object[0]).getUnformattedText())) {
                this.job = new JobATM(this, v3.toBlockPos(), world);
                //杂货商
            } else if (jobName.contentEquals(new TextComponentTranslation("container.sim.Vocation9",new Object[0]).getUnformattedText())) {
                this.job = new JobMerchant(this, v3.toBlockPos(), world);
                //插花师
            } else if (jobName.contentEquals(new TextComponentTranslation("container.sim.Vocation32",new Object[0]).getUnformattedText())) {
                this.job = new JobFlower(this, v3.toBlockPos(), world);
                //赤脚大夫
            } else if (jobName.contentEquals(new TextComponentTranslation("container.sim.Vocation33",new Object[0]).getUnformattedText())) {
                this.job = new JobBarefootDoctor(this, v3.toBlockPos(), world);
                //妇产科医生
            } else if (jobName.contentEquals(new TextComponentTranslation("container.sim.Vocation34",new Object[0]).getUnformattedText())) {
                this.job = new JobAccoucheur(this, v3.toBlockPos(), world);
                //汉堡店经理
            } else if (jobName.contentEquals(new TextComponentTranslation("container.sim.Vocation36",new Object[0]).getUnformattedText())) {
                this.job = new JobBurgers(this, v3.toBlockPos(), world);
                //奶酪匠
            } else if (jobName.contentEquals(new TextComponentTranslation("container.sim.Vocation21",new Object[0]).getUnformattedText())) {
                this.job = new JobCheesemaker(this, v3.toBlockPos(), world);
                //麦当劳
            } else if (jobName.contentEquals(new TextComponentTranslation("container.sim.Vocation35",new Object[0]).getUnformattedText())) {
                this.job = new JobMcDonald(this, v3.toBlockPos(), world);
                //酒馆
            } else if (jobName.contentEquals(new TextComponentTranslation("container.sim.Vocation37",new Object[0]).getUnformattedText())) {
                this.job = new JobBartender(this, v3.toBlockPos(), world);
                //仓库管理员
            }else if (jobName.contentEquals(new TextComponentTranslation("container.sim.Vocation10",new Object[0]).getUnformattedText())) {
                this.job = new JobCourier(this, v3.toBlockPos(), world);
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("hireAt出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }


    }

    /**
     * @return com.trhsy.sim.npc.V3
     * @Author fan
     * @Description //TODO 获得地点
     * @Date 13:25 2022/10/21
     * @Param []
     **/
    public V3 getV3() {
        return new V3(new BlockPos(this.entity), this.entity.dimension);
    }

}
