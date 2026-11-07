package com.trhsy.sim.entity;

import com.trhsy.sim.entity.ai.FolkAIOpenFenceGate;
import com.trhsy.sim.entity.ai.FolkAIWander;
import com.trhsy.sim.entity.container.ContainerNpc;
import com.trhsy.sim.entity.container.InventoryNpc;
import com.trhsy.sim.loader.ModSimClientLoader;
import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.loader.NetWorkLoader;
import com.trhsy.sim.loader.SoundRegistry;
import com.trhsy.sim.network.client.*;
import com.trhsy.sim.npcCode.NpcData;
import com.trhsy.sim.npcCode.NpcIdentity;
import com.trhsy.sim.npcCode.build.Building;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketAnimation;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.List;
import java.util.UUID;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.entity
 * @ClassName: EntityFolk
 * @Description: 实体npc
 * @date 2023/11/20 下午 3:57
 */
public class EntityNpc extends EntityCreature implements INpc {
    /**
     * 上次更新状态时间
     **/
    private transient long timeSinceLastStatusUpdate = 0L;
    /**
     * 更新状态分钟
     **/
    private transient long timeSinceLastMinute = 0L;
    /**
     * NPC数据
     **/
    public NpcData theData;
    long secondTimer = 0L;
    /**
     * 是否为已创建
     **/
    public boolean isBeingCreated = false;
    /**
     * 物品栏
     */
    public Container inventoryContainer;

    public InventoryNpc inventory = new InventoryNpc(this);

    private boolean aiInitialized =false;//新增：AI初始化标志

    // P2+: 窒息/溺水救援。NPC 卡在方块里会持续受到 IN_WALL 伤害，溺水会受 DROWN 伤害；
    // 连续 3 秒处于危险状态就找最近的安全落点做一次救援传送（5 秒冷却）。
    private int dangerTicks = 0;
    private long lastRescueTime = 0L;
    private static final int RESCUE_TRIGGER_TICKS = 60;
    private static final long RESCUE_COOLDOWN_MS = 5000L;

    public EntityNpc(World worldIn) {
        super(worldIn);
        this.inventoryContainer = new ContainerNpc(this.inventory, !worldIn.isRemote, this);
        try {

            //会捡起地上的东西
            this.setCanPickUpLoot(false);
            //会进门
            ((PathNavigateGround) this.getNavigator()).setEnterDoors(true);
            //破门而入
//            ((PathNavigateGround) this.getNavigator()).setBreakDoors(true);
            //会游泳
            ((PathNavigateGround) this.getNavigator()).setCanSwim(true);
            this.setSize(0.4F, 1.8F);
            this.enablePersistence();
            // P10(S0): 区块重载时 Forge 用 EntityNpc(World) 构造器重建实体。
            // 这里必须是 false，否则 NPC 每次读档都会变成穿墙状态。
            this.noClip = false;
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("EntityFolk1出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    /**
     * 已创建
     *
     * @param world
     * @param isCreating
     */
    public EntityNpc(World world, boolean isCreating) {
        super(world);
        this.inventoryContainer = new ContainerNpc(this.inventory, !world.isRemote, this);
        try {
            //会捡起地上的东西
            this.setCanPickUpLoot(false);
            //会进门
            ((PathNavigateGround) this.getNavigator()).setEnterDoors(true);
            //破门而入
//            ((PathNavigateGround) this.getNavigator()).setBreakDoors(true);
            //会游泳
            ((PathNavigateGround) this.getNavigator()).setCanSwim(true);
            this.isBeingCreated = isCreating;
            this.setSize(0.4F, 1.8F);
            this.enablePersistence();
            this.noClip = false;
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("EntityFolk2出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    /**
     * 根据Uid加载
     *
     * @param world
     * @param id
     */
    public EntityNpc(World world, UUID id) {
        super(world);
        this.inventoryContainer = new ContainerNpc(this.inventory, !world.isRemote, this);
        try {
            this.setUniqueId(id);
            // 2. 显式禁用穿墙模式，确保碰撞检测生效
            this.noClip = false;
            //会捡起地上的东西
            this.setCanPickUpLoot(false);
            //会进门
            ((PathNavigateGround) this.getNavigator()).setEnterDoors(true);
            //破门而入
//            ((PathNavigateGround) this.getNavigator()).setBreakDoors(true);
            //会游泳
            ((PathNavigateGround) this.getNavigator()).setCanSwim(true);
            this.isBeingCreated = false;
            this.setSize(0.4F, 1.8F);
            this.enablePersistence();
//            this.hasNoGravity = false;
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("EntityFolk3出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    /***
     * 初始化 实体 ai
     */
    @Override
    public void initEntityAI() {
        try {
            if (theData == null) {
            return;
            }
                //智能游泳（保留：防止 NPC 在水中淹死）
                this.tasks.addTask(0, new EntityAISwimming(this));

                //开门
                this.tasks.addTask(1, new EntityAIOpenDoor(this, true));
                //限制打开门
                this.tasks.addTask(2, new EntityAIRestrictOpenDoor(this));
                //打开栅栏门
                this.tasks.addTask(3, new FolkAIOpenFenceGate(this, true));

                //自由闲逛（只在真正空闲时启用，见 FolkAIWander）
                this.tasks.addTask(6, new FolkAIWander(this, 1.0D));
                //最近观看
//            this.tasks.addTask(7, new EntityAIWatchClosest(this, EntityPlayer.class, 1.0F));
                this.tasks.addTask(7, new EntityAIWatchClosest2(this, EntityPlayer.class, 1.0F, 1));
                //看起来很空闲
                this.tasks.addTask(8, new EntityAILookIdle(this));

                // P10(S0): 不再注册 EntityAIMoveIndoors / EntityAIPanic。
                // 这两个 AI 会和职业任务的 navigator.setPath 抢控制权，
                // 表现为“上班路上突然跑进屋/逃跑/原地转圈”。
                // 受击逃跑/进屋由职业任务和 stayPut 统一管理。
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("initEntityAI出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }

    }

    /**
     * 应用实体属性
     */
    @Override
    protected void applyEntityAttributes() {
        try {
            super.applyEntityAttributes();
            //最大生命 40
            this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(40.0D);
            //移动_速度
            this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.25D);
            //跟随范围
            this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(256.0D);
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("applyEntityAttributes出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    /**
     * @return boolean
     * @Author fan
     * @Description //TODO 能否重生
     * @Date 15:18 2022/10/18
     * @Param []
     **/
    @Override
    protected boolean canDespawn() {
        // P10(S0): NPC 是存档数据的一部分，不能被原版自然清除。
        return false;
    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 实体更新
     * @Date 15:18 2022/10/18
     * @Param []
     **/
    @Override
    public void onUpdate() {
        super.onUpdate();
        try {
            if (System.currentTimeMillis() - this.secondTimer > 1000L) {
                this.secondTimer = System.currentTimeMillis();
                // P2+: 每秒检查一次是否处于"窒息/溺水"危险状态，连续 3 秒则救援传送。
                if (!this.world.isRemote) {
                    boolean suffocating = this.isEntityInsideOpaqueBlock();
                    boolean drowning = this.isInWater() && this.getAir() <= 0;
                    if (suffocating || drowning) {
                        this.dangerTicks += 20;
                        if (this.dangerTicks >= RESCUE_TRIGGER_TICKS) {
                            this.rescueToSafeLocation();
                        }
                    } else if (this.dangerTicks > 0) {
                        this.dangerTicks = Math.max(0, this.dangerTicks - 20);
                    }
                }
                //获取NPC旁边的所有实体
                List<Entity> list1 = this.world.getEntitiesWithinAABBExcludingEntity(this, (new AxisAlignedBB(this.posX, this.posY, this.posZ, this.posX + 1.0D, this.posY + 1.0D, this.posZ + 1.0D)).expand(2.0D, 4.0D, 2.0D));
                for (Entity entity1 : list1) {
                    if (entity1 instanceof EntityItem) {
                        EntityItem entityitem = (EntityItem) entity1;
                        ItemStack is = entityitem.getItem();

                        try {
                            Item item = is.getItem();
//                            ModSimLoader.log.info("NPC手持物品：" + item.getUnlocalizedName());
                            if (item instanceof ItemFood) {
                                // P10(S0): 只吃“无主、可拾取、确实饿了”的掉落食物。
                                // 原实现会抢玩家刚扔出的物品，也会在 hunger=9 时继续吃。
                                if (this.theData != null && this.theData.hunger < 8.0D
                                        && !entityitem.cannotPickup()
                                        && (entityitem.getOwner() == null || entityitem.getOwner().isEmpty())) {
                                    ModSimLoader.log.info("NPC拾取食物：{}，当前饥饿值：{}", item.getUnlocalizedName(), this.theData.hunger);
                                    ItemFood itemFood = (ItemFood) item;
                                    int healAmount = itemFood.getHealAmount(is);
                                    this.theData.hunger = Math.min(10.0D, this.theData.hunger + healAmount);
                                    entityitem.setDead();
                                    ModSimLoader.log.info("NPC吃东西了，饥饿值：{}", this.theData.hunger);
                                }
                            }
                        } catch (Exception var8) {
                            ModSimLoader.log.error("Npc 吃东西出错了：" + var8.getMessage());
                        }
                    } /*else if (entity1 instanceof EntityNpc && (int) this.posX == (int) entity1.posX && (int) this.posZ == (int) entity1.posZ) {
                        this.motionX += 0.10000000149011612D;

                        try {
                            this.theData.stayPut = false;
                        } catch (Exception var7) {
                        }
                    }*/
                }
                if (this.theData != null) {
                    if(!aiInitialized){
                        initEntityAI();
                        aiInitialized = true;
                    }
                    // P10(S0): 删除了“手持食物自动吃”的逻辑。
                    // 职业手持物是工作道具（奶酪、工具等），不是口粮；
                    // NPC 的口粮统一由 NpcData.trySelfFeed()/商店喂食处理。
                }
            }

            if (this.theData != null && !this.world.isRemote && !ModSimLoader.folks.contains(this.theData)) {
                ModSimLoader.folks.add(this.theData);
                NetWorkLoader.net.sendToAll(new PacketUpdateNPC());
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("onUpdate实体更新出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 当NPC更新时
     * @Date 10:40 2022/10/16
     * @Param []
     **/
    public void onFolkUpdate() {
        try {
            if (!this.world.isRemote) {

                if (this.theData != null) {
                    if (this.theData.entity != null && !this.theData.entity.equals(this)) {
                        ModSimLoader.log.info("获得重复的民间实体");
                        this.setDead();
                    }

                }
            }

            if (this.theData == null && !this.isBeingCreated && !this.world.isRemote) {
                this.theData = ModSimLoader.getFolkDataByUID(this.getUniqueID());
                this.theData.entity = this;
                this.theData.sendSkinPathToClient();
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("onFolkUpdate出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    /**
     * P2+: 受到窒息/溺水伤害时累计危险时间（与 onUpdate 的每秒检查互为补充，
     * 因为伤害事件本身也能证明"持续窒息"确实在发生）。
     */
    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        boolean result = super.attackEntityFrom(source, amount);
        if (!this.world.isRemote && source != null
                && (source == DamageSource.IN_WALL || source == DamageSource.DROWN)) {
            this.dangerTicks += 20;
            if (this.dangerTicks >= RESCUE_TRIGGER_TICKS) {
                this.rescueToSafeLocation();
            }
        }
        return result;
    }

    /** P2+: 找一个安全落点并传送过去；找不到就返回（不硬传送进未知位置）。 */
    private void rescueToSafeLocation() {
        long now = System.currentTimeMillis();
        if (now - this.lastRescueTime < RESCUE_COOLDOWN_MS) {
            return;
        }
        this.lastRescueTime = now;
        this.dangerTicks = 0;
        BlockPos safe = this.findSafePosition();
        if (safe == null) {
            ModSimLoader.log.warn("NPC {} 处于危险位置但找不到安全落点", this.getUniqueID());
            return;
        }
        double x = safe.getX() + 0.5D;
        double y = safe.getY();
        double z = safe.getZ() + 0.5D;
        boolean ok = this.attemptTeleport(x, y, z);
        if (!ok) {
            this.setPositionAndUpdate(x, y, z);
        }
        if (this.world instanceof WorldServer) {
            ((WorldServer) this.world).spawnParticle(EnumParticleTypes.PORTAL, false,
                    this.posX, this.posY + 0.8D, this.posZ, 24, 0.5D, 0.5D, 0.5D, 0.05D);
        }
        if (this.theData != null) {
            // 用实体实际落点同步数据层坐标（attemptTeleport 会在请求点附近找安全落点）
            this.theData.pos = com.trhsy.sim.npcCode.V3.fromVec3d(this.getPositionVector());
        }
        ModSimLoader.log.warn("NPC {} 持续窒息/溺水，已救援传送到 {}", this.getUniqueID(), safe);
    }

    /** P2+: 螺旋搜索最近的可站立位置；找不到再退回家/工作点/世界出生点。 */
    private BlockPos findSafePosition() {
        BlockPos origin = this.getPosition();
        for (int r = 1; r <= 8; r++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz))) != r) {
                            continue;
                        }
                        BlockPos p = origin.add(dx, dy, dz);
                        if (this.isSafeStanding(p)) {
                            return p;
                        }
                    }
                }
            }
        }
        if (this.theData != null) {
            if (this.theData.home != null && this.theData.home.livingXYZ != null) {
                BlockPos home = this.theData.home.livingXYZ.toBlockPos().up();
                if (this.isSafeStanding(home)) {
                    return home;
                }
            }
            if (this.theData.job != null && this.theData.job.workPlace != null) {
                BlockPos work = this.theData.job.workPlace.toBlockPos().up();
                if (this.isSafeStanding(work)) {
                    return work;
                }
            }
        }
        BlockPos spawn = this.world.getSpawnPoint().up();
        return this.isSafeStanding(spawn) ? spawn : null;
    }

    private boolean isSafeStanding(BlockPos p) {
        if (p.getY() <= 0 || p.getY() >= 255) {
            return false;
        }
        IBlockState ground = this.world.getBlockState(p.down());
        return this.world.isAirBlock(p) && this.world.isAirBlock(p.up())
                && ground.isFullBlock() && !(ground.getBlock() instanceof BlockLiquid);
    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 死亡
     * @Date 15:22 2022/10/18
     * @Param [cause]
     **/
    @Override
    public void onDeath(DamageSource cause) {
        try {
            if (this.theData != null) {
                this.theData.onDeath(cause);
            } else {
                // 2. 高效查找匹配的 NpcData（建议用 Map 存储优化查找）
                NpcData matchedData = null;
                for (NpcData npcData : ModSimLoader.folks) {
                    if (npcData.ID != null && npcData.ID.equals(this.getUniqueID())) {
                        matchedData = npcData;
                        break;
                    }
                }
                if (matchedData != null) {
                    // P10(S0): 只调用一次（原实现循环内调用一次、循环外又调用一次）。
                    matchedData.onDeath(cause);
                } else {
                    for (Building building:ModSimLoader.buildings){
                        building.occupants.removeIf(npcData -> npcData.ID.equals(this.getUniqueID()));
                    }
                    // 3. 无匹配数据时，直接从列表中清理当前实体（替代不合理的 onKillEntity）
                    ModSimLoader.log.info("NPC 已死亡位置: " + this.getPosition().toString());
                    ModSimLoader.folks.removeIf(data -> data.ID.equals(this.getUniqueID()));
                    ModSimLoader.log.info("NPC 已死亡且无匹配数据，已从列表中清理: " + this.getUniqueID());
                }
            }

        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("EtityNPC-onDeath出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }finally {
            // 5. 确保父类死亡逻辑执行，强制标记死亡状态（无论是否发生异常）
            super.onDeath(cause);
            this.setHealth(0);
            this.setDead();// 强制标记实体为死亡，避免残留
        }
    }

    /**
     * @return boolean
     * @Author fan
     * @Description //TODO 交互
     * @Date 15:01 2022/10/18
     * @Param [player, hand]
     **/
    @Override
    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        try {
            if (!player.world.isRemote) {
                if (this.theData == null) {
                    this.theData = ModSimLoader.getFolkDataByUID(this.getUniqueID());
                }
                if (this.theData != null) {
                    if (this.theData.job != null) {
                        String jobName = this.theData.job.jobName;
                        //工作是建筑商
                        if (jobName.equals(new TextComponentTranslation("container.sim.Vocation11", new Object[0]).getUnformattedText()) && this.theData.isAtLocation(this.theData.job.workPlace, 5)) {
                            //打开建筑商gui
                            NetWorkLoader.net.sendTo(new PacketOpenMerchantGui(this.theData), (EntityPlayerMP) player);
                            //行长
                        } else if (jobName.equals(new TextComponentTranslation("container.sim.Vocation31", new Object[0]).getUnformattedText()) && this.theData.isAtLocation(this.theData.job.workPlace, 5)) {
                            //打开建银行gui
                            NetWorkLoader.net.sendTo(new PacketOpenBankATMGui(this.theData), (EntityPlayerMP) player);
                        } else if (jobName.equals(new TextComponentTranslation("container.sim.Vocation9", new Object[0]).getUnformattedText()) && this.theData.isAtLocation(this.theData.job.workPlace, 5)) {
                            //打开建杂货商gui
                            NetWorkLoader.net.sendTo(new PacketOpenMerchantsGui(this.theData), (EntityPlayerMP) player);
                        } else if (jobName.equals(new TextComponentTranslation("container.sim.Vocation32", new Object[0]).getUnformattedText()) && this.theData.isAtLocation(this.theData.job.workPlace)) {
                            //打开建插花师gui
                            NetWorkLoader.net.sendTo(new PacketOpenFlowerGui(this.theData), (EntityPlayerMP) player);
                        } else {
                            NetWorkLoader.net.sendTo(new PacketOpenFolkGui(this.theData), (EntityPlayerMP) player);
                        }
                    } else {
                        NetWorkLoader.net.sendTo(new PacketOpenFolkGui(this.theData), (EntityPlayerMP) player);
                    }
                    //互动的时候说哈喽
                    if (this.theData.age < 18) {
//                        SoundEvent soundEvent = new SoundEvent(new ResourceLocation(ModSim.MODID + ":helloc"));
                        SoundEvent helloc = SoundRegistry.HELLOC;
                        if (helloc == null || helloc.getRegistryName() == null) {
                            ModSimLoader.log.error("播放失败：sim:windmill 声音事件未注册");
                        } else {
                        player.world.playSound(null, player.posX, player.posY, player.posZ, helloc, SoundCategory.PLAYERS, 1.0F, 1.0F);}
                    } else if (this.theData.gender == 0) {
                        //女声
//                        SoundEvent soundEvent = new SoundEvent(new ResourceLocation(ModSim.MODID + ":hellom"));
                        SoundEvent hellom = SoundRegistry.HELLOM;
                        if (hellom == null || hellom.getRegistryName() == null) {
                            ModSimLoader.log.error("播放失败：sim:hellom 声音事件未注册");
                        } else {
                        player.world.playSound(null, player.posX, player.posY, player.posZ, hellom, SoundCategory.PLAYERS, 1.0F, 1.0F);}
                    } else {
//                        SoundEvent soundEvent = new SoundEvent(new ResourceLocation(ModSim.MODID + ":hellof"));
                        SoundEvent hellof = SoundRegistry.HELLOF;
                            if (hellof == null || hellof.getRegistryName() == null) {
                                ModSimLoader.log.error("播放失败：sim:hellof 声音事件未注册");
                            } else {
                        player.world.playSound(null, player.posX, player.posY, player.posZ, hellof, SoundCategory.PLAYERS, 1.0F, 1.0F);}
                    }
                } else {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("processInteract出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
            return false;
        }
    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 摆动手臂
     * @Date 15:44 2022/10/18
     * @Param []
     **/
    // 手臂摆动动画计时器
    private int swingAnimTick = 0;
    
    public void swing() {
        try {
            // 设置手臂摆动动画计时器（1.12.2使用类似1.20.1的方式）
            // 在1.12.2中，我们通过设置swingProgress来实现手臂摆动
            this.swingProgressInt = 6; // 设置摆动持续6个tick
            this.swingProgress = 1.0f; // 设置摆动进度为最大值
            
            // 在服务器端，发送动画包到所有客户端
            if (!this.world.isRemote && this.world instanceof WorldServer) {
                WorldServer worldServer = (WorldServer) this.world;
                // 使用Minecraft原生的方式发送手臂挥动动画
                // 动画类型 0 = 主手挥动
                worldServer.getEntityTracker().sendToTracking(this, new SPacketAnimation(this, 0));
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("swing出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }
    
    @Override
    public float getSwingProgress(float partialTickTime) {
        // 重写手臂摆动进度计算，使用类似1.20.1的方式
        if (this.swingProgressInt <= 0) {
            return 0.0f;
        }
        // 计算摆动进度，使用sin函数模拟自然的摆动曲线
        float progress = (this.swingProgressInt - partialTickTime) / 6.0f;
        return MathHelper.sin((1.0f - progress) * (float)Math.PI) * 0.8f;
    }
    /**
     * 让实体挥动手臂，处理客户端和服务器端同步
     * @param entity 要挥动手臂的实体
     * @param hand 要挥动的手（主手或副手）
     * @param force 是否强制挥动（忽略冷却）
     */
    public static void swingEntityArm(EntityLivingBase entity, EnumHand hand, boolean force) {
        // 仅在服务器端处理（客户端会通过网络包同步）
        if (entity.world.isRemote) {
            entity.swingArm(hand);
            return;
        }
        // 挥动手臂
        entity.swingArm(hand);

        // 如果是主手挥动，并且需要同步到客户端
        if (hand == EnumHand.MAIN_HAND) {
            WorldServer worldServer = (WorldServer) entity.world;
            SPacketAnimation swingPacket = new SPacketAnimation(entity, 0);
            worldServer.getEntityTracker().sendToTracking(entity, swingPacket);
//            ((WorldServer)entity.world).getEntityTracker().sendToTracking(entity, new SPacketAnimation(entity, 0));
        }
    }

    /**
     * @return boolean
     * @Author fan
     * @Description //TODO 移动
     * @Date 15:45 2022/10/18
     * @Param []
     **/
    public boolean isMoving() {
        // P12(S2): 旧实现只看正方向 motion，向西/向北会被判“没动”。
        // 优先看导航器是否有未完成路径；否则看水平速度绝对值。
        if (this.getNavigator() != null && !this.getNavigator().noPath()) {
            return true;
        }
        return Math.abs(this.motionX) + Math.abs(this.motionZ) > 1.0E-4D;
    }

    /**
     * @return boolean
     * @Author fan
     * @Description //TODO 是儿童
     * @Date 15:46 2022/10/18
     * @Param []
     **/
    @Override
    public boolean isChild() {
        try {
            // P12(S2): 服务端以 NpcData 的年龄为准；客户端缓存只作兜底。
            if (this.theData != null && this.theData.race != null) {
                return this.theData.age < this.theData.race.maturity;
            }
            if (!this.world.isRemote) {
                NpcData data = ModSimLoader.getFolkDataByUID(this.getUniqueID());
                if (data != null && data.race != null) {
                    return data.age < data.race.maturity;
                }
                return false;
            }
            NpcIdentity cfi = ModSimClientLoader.getFolkByUUID(this.getUniqueID());
            if (cfi != null) {
                return Integer.parseInt(cfi.age) < Integer.parseInt(cfi.maturityAge);
            }
            return false;
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("isChild出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
            return false;
        }
    }
}
