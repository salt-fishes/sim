package com.trhsy.sim.npcCode.job;

import com.trhsy.sim.block.BlockConstructorBox;
import com.trhsy.sim.entity.EntityConBox;
import com.trhsy.sim.loader.BlockLoader;
import com.trhsy.sim.loader.ConfigLoader;
import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.loader.SoundRegistry;
import com.trhsy.sim.npcCode.NpcData;
import com.trhsy.sim.npcCode.V3;
import com.trhsy.sim.npcCode.build.Building;
import com.trhsy.sim.npcCode.build.BuildingBlueprint;
import com.trhsy.sim.npcCode.build.Structure;
import com.trhsy.sim.npcCode.task.JobTask;
import com.trhsy.sim.npcCode.task.JobTaskBuilder;
import com.trhsy.sim.npcCode.task.JobTaskIdle;
import net.minecraft.block.*;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDoor;
import net.minecraft.item.ItemStack;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @ClassName JobBuilder
 * @Description todo 建筑工作
 * @Author TRHSY
 * @Date 2022/10/2014:25
 **/
public class JobBuilder extends Job {
    /**
     * 建筑蓝图
     **/
    public BuildingBlueprint blueprint;
    /**
     * 悬浮的控制箱
     **/
    public EntityConBox conBox;
    /**
     * 放置的块
     **/
    public CopyOnWriteArrayList<V3> placedBlocks;
    /**
     * 缺失的方块
     **/
    public Block missingBlock = null;
    /**
     * 开始位置
     **/
    public BlockPos startPos;
    /**
     * 建造位置
     **/
    public BlockPos constructorPos;
    /**
     * 控制箱位置
     **/
    public BlockPos controllerPos;
    /**
     * 活动区域
     **/
    public BlockPos livingPos;
    /**
     * 建筑箱
     **/
    public BlockConstructorBox constructorBlock;
    /**
     * 方向
     */
    public int direction;
    int x = 0;
    int y = 0;
    int z = 0;
    /**
     * 块的编号
     **/
    public int blockNumber = 0;
    /**
     * 自上一个块位置的时间
     **/
    public long timeSinceLastBlockPlace = 0L;
    /**
     * 上次摆动手臂的时间
     **/
    private transient long timeSwingArm = 0L;
    private transient long timeTx = 0L;
    /**
     * 已重新指派员工
     **/
    public boolean hasReassignedEmployee;
    /**
     * 缺失检查
     **/
    int missingCheck = 0;
    //特除的空气方块
    public List<V3> blockSpecial = new CopyOnWriteArrayList();
    private Boolean isOk= false;
    //工作阶段
    public int builder_stage = 0;

    /**
     * @return npc 蓝图 块
     * @Author fan
     * @Description //TODO 初始化工作 从文件来若有蓝图
     * @Date 22:12 2022/10/30
     * @Param [folk, bp, pos, direction, world]
     **/
    public JobBuilder(NpcData folk, BuildingBlueprint bp, BlockPos pos, int direction, World world) {
        super(folk, pos, world);
        try {
            folk.holding = new ItemStack(Blocks.COBBLESTONE);
            if (folk.entity != null) {
                folk.entity.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, folk.holding);
            }
            //建筑工
            this.jobName = new TextComponentTranslation("container.sim.Vocation1", new Object[0]).getUnformattedText();
            V3 v3=new V3(V3.fromBlockPos(pos).x+0.5,V3.fromBlockPos(pos).y,V3.fromBlockPos(pos).z+0.5);
            this.startPos = v3.toBlockPos();
            this.constructorPos = v3.toBlockPos();
            this.direction = direction;
            this.blueprint = bp;
            int i = 0;
            boolean hasControlBox = false;
            //建筑蓝图的所有方块
            Structure[] var8 = this.blueprint.structure;
            for (Structure st : var8) {
                if (st != null) {
                    Block block = st.getiBlockState().getBlock();
                    if (block == BlockLoader.blockControlBox) {
                        hasControlBox = true;
                        break;
                    }
                    ++i;
                }
            }

            if (!hasControlBox) {
                //如果没有找到控制箱，则第一个方块就是建筑箱
                Structure structure = new Structure();
                structure.setMeta(0);
                structure.setiBlockState(BlockLoader.blockControlBox.getDefaultState());
                this.blueprint.structure[0] = structure;
            }
            constructorBlock = null;
            IBlockState s = BlockLoader.blockControlBox.getDefaultState();
            if (s != null) {
                Block block = s.getBlock();
                if (block != null && block == BlockLoader.blockConstructorBox) {
                    //建筑箱
                    this.constructorBlock = (BlockConstructorBox) block;
                    this.constructorBlock.employee = folk;
                }
            }

            //建筑方向
            if (direction == 0) {
                this.startPos = this.startPos.add(0, 0, -1);
            } else if (direction == 1) {
                this.startPos = this.startPos.add(1, 0, 0);
            } else if (direction == 2) {
                this.startPos = this.startPos.add(0, 0, 1);
            } else {
                this.startPos = this.startPos.add(-1, 0, 0);
            }

//        this.createConBox();
            placedBlocks = new CopyOnWriteArrayList<V3>();
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("JobBuilder1出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }

    }

    /**
     * @return
     * @Author fan
     * @Description //TODO 工作的npc 位置，方向，世界 从文件中加载来的
     * @Date 10:08 2022/11/4
     * @Param [folk, pos, direction, world]
     **/
    public JobBuilder(NpcData folk, BlockPos pos, int direction, World world) {
        super(folk, pos, world);
        try {
            folk.holding = new ItemStack(Blocks.COBBLESTONE);
            if (folk.entity != null) {
                folk.entity.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, folk.holding);
            }
            this.jobName = new TextComponentTranslation("container.sim.Vocation1", new Object[0]).getUnformattedText();
            this.constructorPos = pos;
            this.startPos = pos;
            this.direction = direction;
            if (direction == 0) {
                this.startPos = this.startPos.add(0, 0, -1);
            } else if (direction == 1) {
                this.startPos = this.startPos.add(1, 0, 0);
            } else if (direction == 2) {
                this.startPos = this.startPos.add(0, 0, 1);
            } else {
                this.startPos = this.startPos.add(-1, 0, 0);
            }

            this.createConBox();
            placedBlocks = new CopyOnWriteArrayList<V3>();
            constructorBlock = null;
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("JobBuilder2出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }

    }

    /**
     * NPC雇佣后服务端反馈的信息
     * @param folk
     * @param pos
     * @param direction
     * @param world
     */
    public JobBuilder(NpcData folk, V3 pos, int direction, World world) {
        super(folk, pos, world);
        try {
            folk.holding = new ItemStack(Blocks.COBBLESTONE);
            if (folk.entity != null) {
                folk.entity.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, folk.holding);
            }
            this.jobName = new TextComponentTranslation("container.sim.Vocation1", new Object[0]).getUnformattedText();
            this.constructorPos = pos.toBlockPos();
            this.startPos = pos.toBlockPos();
            this.direction = direction;
            Block block = this.folk.entity.world.getBlockState(pos.toBlockPos()).getBlock();
            //建筑箱
            if (block == BlockLoader.blockConstructorBox) {
                BlockConstructorBox cons = (BlockConstructorBox) block;
                cons.employee = folk;
            }
            if (direction == 0) {
                this.startPos = this.startPos.add(0, 0, -1);
            } else if (direction == 1) {
                this.startPos = this.startPos.add(1, 0, 0);
            } else if (direction == 2) {
                this.startPos = this.startPos.add(0, 0, 1);
            } else {
                this.startPos = this.startPos.add(-1, 0, 0);
            }

            this.createConBox();
            placedBlocks = new CopyOnWriteArrayList<V3>();
            constructorBlock = null;
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("JobBuilder3出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }

    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 更新
     * @Date 10:13 2022/11/4
     * @Param []
     **/
    @Override
    public void onUpdate() {
        super.onUpdate();
        // 确保建造师手持圆石
        // 注意：如果folk.job不再是当前JobBuilder（被解雇了），则不再设置手持物品
        if (this.folk != null && this.folk.entity != null && this.folk.job == this) {
            // 确保holding是圆石
            if (this.folk.holding == null || this.folk.holding.isEmpty() || this.folk.holding == ItemStack.EMPTY) {
                this.folk.holding = new ItemStack(Blocks.COBBLESTONE);
            }
            // 设置到实体主手
            ItemStack mainHandItem = this.folk.entity.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
            if (mainHandItem == null || mainHandItem.isEmpty() || !mainHandItem.isItemEqual(this.folk.holding)) {
                this.folk.entity.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, this.folk.holding);
            }
        }
        if (this.atWork) {
            if (this.stage == -1) {
                this.builder_stage = 0;
                this.stage = 0;
            }else if (this.builder_stage == 0) {
                this.builder_stage = 1;
                //去上班
                this.addJobTask(new JobTaskIdle(this, 200L, new TextComponentTranslation("container.sim.job.builder_Arrived",new Object[0]).getUnformattedText()));
            }else if(this.builder_stage == 1){
                //等待蓝图
                this.addJobTask(new JobTaskIdle(this, 200L, new TextComponentTranslation("container.sim.job.builder_Awaiting_blueprint",new Object[0]).getUnformattedText()));
                this.builder_stage = 2;
            }else if(this.builder_stage == 2){
               //建造，用异步启动
                this.addJobTask(new JobTaskBuilder(this, -1L,new TextComponentTranslation("container.sim.FolkAction3",new Object[0]).getUnformattedText()));
                this.builder_stage = 3;
            }else{
                if (this.jobTasks.size() > 0&&this.currentTask==null) {
                    this.currentTask = (JobTask) this.jobTasks.get(0);
                    this.currentTask.begin();
                }
            }
        }
        /*
        try {
            if (this.folk != null) {
                if (this.folk.entity != null) {
                    if (this.folk.entity.world != null) {
                        if (!this.folk.entity.world.isRemote) {
                            //NPC数据为空，并且没有指派元
                            if (!this.hasReassignedEmployee) {
                                //建造位置为空
                                IBlockState iBlockState = this.folk.entity.world.getBlockState(this.constructorPos);
                                if (iBlockState == null) {
                                    return;
                                }
                                Block block = iBlockState.getBlock();
                                if (block == Blocks.AIR) {
                                    return;
                                }
                                if (block == BlockLoader.blockConstructorBox) {
                                    //获取建筑箱的
                                    BlockConstructorBox cons = (BlockConstructorBox) block;
                                    //当前建筑箱的工作人员是
                                    cons.employee = this.folk;
                                    //已经
                                    this.hasReassignedEmployee = true;
                                    //如果允许 NPC 说话
                                    if (ConfigLoader.configFolkTalking) {
                                        //World world = FMLClientHandler.instance().getServer().getEntityWorld();
                                        //播放 我准备好了
                                        SoundEvent soundEvent = null;
                                        //判断性别，发出不一样的声音
                                        if (this.folk.gender == 0) {
                                            soundEvent = new SoundEvent(new ResourceLocation(ModSim.MODID + ":im_read_m"));
                                        } else {
                                            soundEvent = new SoundEvent(new ResourceLocation(ModSim.MODID + ":im_read_y"));
                                        }
                                        Minecraft mc = Minecraft.getMinecraft();
                                        for (EntityPlayer entityPlayer : mc.world.playerEntities) {
                                            mc.world.playSound(entityPlayer, entityPlayer.posX, entityPlayer.posY, entityPlayer.posZ, soundEvent, SoundCategory.BLOCKS, 1.0F, 1.0F);
                                        }

                                    }
                                }
                            }
                            //建筑蓝图为空
                            if (this.blueprint == null) {
                                //等待蓝图
                                this.folk.setStatus(new TextComponentTranslation("container.sim.job.builder_Awaiting_blueprint", new Object[0]).getUnformattedText());
                            } else {

                                //随机
//                            new Random();
                                //当前时间
                                long now = System.currentTimeMillis();
                                //游戏模式是正常模式
                                if (ModSimLoader.gamemode != 1) {
                                    if ((float) (now - this.timeSinceLastBlockPlace) > 1000.0F - 100.0F * this.folk.skillBuilding) {
                                        ////上次时间为当前时间
                                        this.timeSinceLastBlockPlace = now;
                                        //放置方块
                                        this.placeBlock();
                                        //发送建筑蓝图
                                        if (this.conBox == null) {
                                            this.createConBox();
                                        }
                                        NetWorkLoader.net.sendToAll(new PacketSendBuildingRequirements(this.blueprint, this.conBox.getUniqueID()));

                                    }
                                } else {
                                    //上次时间为当前时间
                                    this.timeSinceLastBlockPlace = now;
                                    //不是客户端
                                    if (!this.folk.entity.world.isRemote) {
                                        //直接放置方块
                                        this.placeBlock();
                                    }
                                }

                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("JobBuilder-onUpdate出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
*/
    }
    @Override
    public void onMinute() {
        try {
            if (this.missingCheck < 3) {
                ++this.missingCheck;
            } else {
                //谁在建造
                String s1 = new TextComponentTranslation("container.sim.job.builder_constructor_started_who's", new Object[0]).getUnformattedText();
                /*if (this.missingBlock != null && this.missingBlock != Blocks.AIR) {

                    String s2 = new TextComponentTranslation("container.sim.job.builder_constructor_started_more",new Object[0]).getUnformattedText();
                    //谁在建“”需要更多的“”
                    ModSimLoader.sendChat(this.folk.getName() + s1 + "(" + this.blueprint.name + ") " + s2 + this.missingBlock.getLocalizedName());
                }*/

                if (ModSimLoader.money < 0.02F) {
                    //没有足够的资金支付给
                    ModSimLoader.sendChat(new TextComponentTranslation("container.sim.JobBuilder1", new Object[0]).getUnformattedText() + this.folk.getName() + s1 + "( " + this.blueprint.name + ")!");
                }

                this.missingCheck = 0;
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("JobBuilder-onMinute出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }

    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 放置方块
     * @Date 22:07 2022/10/30
     * @Param []
     **/
    public void placeBlock() {
        ModSimLoader.log.debug("放置方块:{}", System.currentTimeMillis() / 1000);
        try {
            // 先尝试放置方块，成功后再播放音效和动画
            //正常的块
            boolean normalBlock = true;
            //已放置
            boolean hasPlaced = false;
            //方块编号大于
            if (this.blockNumber >= this.blueprint.structure.length) {
                this.folk.fire();
                if (this.constructorBlock != null) {
                    this.constructorBlock.employee = null;
                }
                this.createBuilding();
                this.isOk=true;
                ModSimLoader.log.info("检查是否建造完成");
                return;
            }
            if (ModSimLoader.money < 0.02F && ModSimLoader.gamemode != 1) {
                //没有钱付给我！
                this.folk.setStatus(new TextComponentTranslation("container.sim.JobBuilder2", new Object[0]).getUnformattedText());
                return;
            }
            BlockPos newBP = null;
            IBlockState st = null;
            Structure fs_structure = this.blueprint.structure[this.blockNumber];

            //方向
            if (this.direction == 0) {
                newBP = new BlockPos(this.startPos.getX() + this.x, this.startPos.getY() + this.y, this.startPos.getZ() - this.z);
                st = fs_structure.getiBlockState();
            } else if (this.direction == 1) {
                newBP = new BlockPos(this.startPos.getX() + this.z, this.startPos.getY() + this.y, this.startPos.getZ() + this.x);
                st = fs_structure.getiBlockState();
            } else if (this.direction == 2) {
                newBP = new BlockPos(this.startPos.getX() - this.x, this.startPos.getY() + this.y, this.startPos.getZ() + this.z);
                st = fs_structure.getiBlockState();
            } else {
                newBP = new BlockPos(this.startPos.getX() - this.z, this.startPos.getY() + this.y, this.startPos.getZ() - this.x);
                st = fs_structure.getiBlockState();
            }
            V3 buildV3 = new V3(0, 0, 0, 0);
            if (this.placedBlocks.size() > 0 && this.placedBlocks.size() > this.blockNumber) {
                buildV3 = this.placedBlocks.get(this.blockNumber);
            }
            if (buildV3 != null || !buildV3.equals(new V3(newBP))) {
                World buildWorld = this.folk.entity.world;
                // P10(S0): 未加载区块不施工，避免 setBlockState 隐式加载区块。
                if (!buildWorld.isBlockLoaded(newBP)) {
                    this.folk.setStatus(new TextComponentTranslation("container.sim.JobBuilder4", new Object[0]).getUnformattedText());
                    return;
                }
                Block fs_block = buildWorld.getBlockState(newBP).getBlock();
                Block fs_st_block = st.getBlock();
                // P10(S0): 蓝图里的命令方块/结构方块/屏障/TNT/岩浆/火一律跳过。
                boolean skipPlacement = isDangerousBlueprintBlock(fs_st_block);
                if (skipPlacement) {
                    ModSimLoader.log.warn("蓝图包含危险方块，已跳过: {} @ {}", fs_st_block.getRegistryName(), newBP);
                }
                //控制箱
                if (fs_st_block == BlockLoader.blockControlBox) {
                    this.controllerPos = newBP;
                    fs_st_block = BlockLoader.blockControlBox.getStateFromMeta(fs_structure.getMeta()).getBlock();
//                    this.createBuilding();
                    this.isOk=true;
                    //生活方块地毯
                } else if (fs_st_block == BlockLoader.blockLiving) {
                    this.livingPos = new BlockPos(newBP.getX(), newBP.getY() - 0.5, newBP.getZ());
                    fs_st_block = BlockLoader.blockLiving.getStateFromMeta(fs_structure.getMeta()).getBlock();
                }
                //特除方块
                if (!skipPlacement && fs_st_block == BlockLoader.blockSpecial) {
                    int  meta=fs_structure.getMeta();
                    // 1. 校验meta有效性（示例：假设有效范围是0~5）
                    if (meta < 0 || meta > 8) {
                        ModSimLoader.log.warn("特殊方块meta值无效：" + meta + "，坐标：" + newBP + "，已修正为0");
                        meta = 0;
                    }
                    // 2. 创建V3对象并添加到blockSpecial列表，添加日志
                    V3 v3 = new V3(newBP.getX(), newBP.getY(), newBP.getZ(), fs_st_block,meta);
                    this.blockSpecial.add(v3);
                    ModSimLoader.log.info("添加特殊方块：坐标=(" + newBP + ")，meta=" + meta);
                    // 3. 确保特殊方块以正确状态（含meta）放置到世界中
                    IBlockState specialState = BlockLoader.blockSpecial.getStateFromMeta(meta);
                    if (specialState == null) {
                        ModSimLoader.log.error("特殊方块状态获取失败，meta=" + meta);
                    } else {
                        // 放置方块时使用带meta的状态
                        this.folk.entity.world.setBlockState(newBP, specialState);
                        ModSimLoader.log.debug("已放置特殊方块到世界，状态：" + specialState);
                    }
                }
                if (!skipPlacement && fs_block != fs_st_block && buildWorld.getTileEntity(newBP) == null) {
                    if (fs_block != Blocks.AIR) {
                        // P10(S0): 用 Block.getDrops 保留 metadata/NBT；
                        // 箱子/熔炉/模组机器等 tile entity 已在条件里跳过，不会吞库存。
                        IBlockState oldState = buildWorld.getBlockState(newBP);
                        List<ItemStack> drops = fs_block.getDrops(buildWorld, newBP, oldState, 0);
                        buildWorld.setBlockState(newBP, Blocks.AIR.getDefaultState(), 3);
                        for (ItemStack drop : drops) {
                            if (drop != null && !drop.isEmpty()) {
                                this.placeInJobChest(drop);
                            }
                        }
                    }
                    Block newBlock = this.folk.entity.world.getBlockState(newBP.down()).getBlock();
                    //门
                    if (fs_st_block instanceof BlockDoor && newBlock != fs_st_block) {
                        normalBlock = false;
                        //放置门
                        ItemDoor.placeDoor(this.folk.entity.world, newBP, rotateFacing(EnumFacing.NORTH), fs_st_block, false);
                    }
                    //床
                    if (fs_st_block instanceof BlockBed) {
                        EnumFacing bedFacing = rotateFacing(EnumFacing.WEST);
                        BlockPos headPos = newBP.offset(bedFacing);
                        normalBlock = false;//床不是普通的块
                        IBlockState iblockstate1 = Blocks.BED.getDefaultState().withProperty(BlockBed.OCCUPIED, false).withProperty(BlockBed.FACING, bedFacing).withProperty(BlockBed.PART, BlockBed.EnumPartType.FOOT);
                        if (this.folk.entity.world.setBlockState(newBP, iblockstate1, 11)) {
                            IBlockState iblockstate2 = iblockstate1.withProperty(BlockBed.PART, BlockBed.EnumPartType.HEAD);
                            if (this.folk.entity.world.isBlockLoaded(headPos)) {
                                this.folk.entity.world.setBlockState(headPos, iblockstate2, 11);
                            }
                        }
                    }

                    //是否是普通的块
                    if (normalBlock) {
                        //模组模式是正常模式
                        if (ModSimLoader.gamemode != 1) {
                            //是不是蓝图的块
                            boolean hasBlock = false;
                            //是不是必须的方块
                            if (!this.blueprint.isRequiredBlock(fs_st_block)) {
                                //放置方块
                                this.folk.setStatus(new TextComponentTranslation("container.sim.JobBuilder3", new Object[0]).getUnformattedText());
                                //已放置
                                hasBlock = true;
                            } else {
                                newBlock = this.folk.entity.world.getBlockState(newBP).getBlock();
                                //是否是同一个块，是否已放置
                                if (this.isSameBlock(newBlock, fs_st_block)) {
                                    hasBlock = true;
                                    hasPlaced = true;
                                } else {
                                    //获取周围箱子
                                    List<IInventory> inventoriesFindClosest = this.inventoriesFindClosest(this.workPlace, 5);
                                    //循环箱子
                                    for (IInventory inv : inventoriesFindClosest) {
                                        //循环箱子库存
                                        for (int i = 0; i < inv.getSizeInventory(); i++) {
                                            ItemStack itemStack = inv.getStackInSlot(i);
                                            //拿走当前需要的块
                                            if (itemStack != null && itemStack.getItem() == Item.getItemFromBlock(fs_st_block)) {
                                                hasBlock = true;
                                                inv.decrStackSize(i, 1);
                                                break;
                                            }
                                        }
                                        if (hasBlock) {
                                            break;
                                        }
                                    }

                                }
                            }

                            if (!hasBlock) {
                                //等待材料
                                this.folk.setStatus(new TextComponentTranslation("container.sim.JobBuilder4", new Object[0]).getUnformattedText() + fs_st_block.getLocalizedName());
                                this.missingBlock = fs_st_block;
                                long now = System.currentTimeMillis();
                                // P11(S1): 使用配置的材料提醒间隔（分钟）；0 表示不再提醒。
                                int reminderMinutes = ConfigLoader.configMaterialReminderInterval;
                                if (reminderMinutes > 0 && now - this.timeTx > reminderMinutes * 60000L) {
                                    this.timeTx = now;
                                    //谁在建造
                                    String s1 = new TextComponentTranslation("container.sim.job.builder_constructor_started_who's", new Object[0]).getUnformattedText();
                                    if (this.missingBlock != null && this.missingBlock != Blocks.AIR) {

                                        String s2 = new TextComponentTranslation("container.sim.job.builder_constructor_started_more", new Object[0]).getUnformattedText();
                                        //谁在建“”需要更多的“”
                                        ModSimLoader.sendChat(this.folk.getName() + s1 + "(" + this.blueprint.name + ") " + s2 + this.missingBlock.getLocalizedName());
                                    }
                                }
                                return;
                            }
                            //重置缺少的块
                            this.missingBlock = null;
                            long now = System.currentTimeMillis();
                            if (now - this.timeSwingArm > 2000) {
                                this.timeSwingArm = now;

//                                this.folk.entity.swingArm(EnumHand.MAIN_HAND);
                            this.folk.entity.swing();
                            }
                            //放置方块
                            this.folk.setStatus(new TextComponentTranslation("container.sim.JobBuilder3", new Object[0]).getUnformattedText());
                        }
                        if (!hasPlaced) {
                            if (st.getProperties().size() == 0) {
                                //放置方块
                                this.folk.setStatus(new TextComponentTranslation("container.sim.JobBuilder3", new Object[0]).getUnformattedText());
                                this.folk.entity.world.setBlockState(newBP, st);
                                //栅栏
                            } else if (fs_st_block instanceof BlockFence) {
                                //放置方块
                                this.folk.setStatus(new TextComponentTranslation("container.sim.JobBuilder3", new Object[0]).getUnformattedText());
                                this.folk.entity.world.setBlockState(newBP, st);
                                //拉杆
                            } else if (fs_st_block instanceof BlockLever) {
                                //放置方块
                                this.folk.setStatus(new TextComponentTranslation("container.sim.JobBuilder3", new Object[0]).getUnformattedText());
                                this.folk.entity.world.setBlockState(newBP, st);
                            } else {
                                EnumFacing facing = null;
                                IProperty prop = null;
                                Collection validFacings = null;
                                do {
                                    for (IProperty p : st.getProperties().keySet()) {
                                        prop = p;
                                        if (prop.getName().equals("facing")) {
                                            //放置方块
                                            this.folk.setStatus(new TextComponentTranslation("container.sim.JobBuilder3", new Object[0]).getUnformattedText());
                                            ModSimLoader.log.info("================================================================");
                                            ModSimLoader.log.info(this.direction + " : " + this.blueprint.direction);
                                            ModSimLoader.log.info("得到方块" + fs_st_block.getUnlocalizedName() + "面向");
                                            facing = (EnumFacing) st.getValue(prop);
                                            validFacings = prop.getAllowedValues();
                                            ModSimLoader.log.info("块当前面向" + facing.toString());
                                            break;
                                        }
                                        //放置方块
                                        this.folk.setStatus(new TextComponentTranslation("container.sim.JobBuilder3", new Object[0]).getUnformattedText());
                                        this.folk.entity.world.setBlockState(newBP, st);
                                    }
                                } while (validFacings != null && validFacings.size() < 4);
                                //不朝上 不朝下
                                if (facing != null && facing != EnumFacing.UP && facing != EnumFacing.DOWN) {
                                    IBlockState newState = st.withProperty(prop, facing.rotateY());
                                    if (this.direction == 0 && this.blueprint.direction == 3 || this.direction == 1 && this.blueprint.direction == 0 || this.direction == 2 && this.blueprint.direction == 1 || this.direction == 3 && this.blueprint.direction == 2) {
                                        newState = st.withProperty(prop, facing.rotateY());
                                        st = st.withRotation(Rotation.CLOCKWISE_90);
                                        if (!this.isSameBlock(this.folk.entity.world.getBlockState(newBP).getBlock(), fs_st_block)) {
                                            this.folk.entity.world.setBlockState(newBP, st);
                                        }
                                        ModSimLoader.log.info("方块应该面向 " + facing.rotateY().toString() + " (顺时针方向的)");
                                        ModSimLoader.log.info("方块实际面向 " + ((EnumFacing) newState.getValue(prop)).toString());
                                    } else if ((this.direction != 0 || this.blueprint.direction != 2) && (this.direction != 1 || this.blueprint.direction != 3) && (this.direction != 2 || this.blueprint.direction != 0) && (this.direction != 3 || this.blueprint.direction != 1)) {
                                        if (this.direction == 0 && this.blueprint.direction == 1 || this.direction == 1 && this.blueprint.direction == 2 || this.direction == 2 && this.blueprint.direction == 3 || this.direction == 3 && this.blueprint.direction == 0) {
                                            newState = st.withProperty(prop, facing.rotateYCCW());
                                            st = st.withRotation(Rotation.COUNTERCLOCKWISE_90);
                                            if (!this.isSameBlock(this.folk.entity.world.getBlockState(newBP).getBlock(), fs_st_block)) {
                                                this.folk.entity.world.setBlockState(newBP, st);
                                            }
                                            ModSimLoader.log.info("方块应该面向 " + facing.rotateYCCW().toString() + " (逆时针方向)");
                                            ModSimLoader.log.info("方块实际面向 " + ((EnumFacing) newState.getValue(prop)).toString());
                                        } else if (this.direction == this.blueprint.direction) {
                                            if (!this.isSameBlock(this.folk.entity.world.getBlockState(newBP).getBlock(), fs_st_block)) {
                                                this.folk.entity.world.setBlockState(newBP, st);
                                            }
                                            ModSimLoader.log.info("方块应该面向 " + facing.toString() + " (无旋转)");
                                        } else {
                                            ModSimLoader.log.info("出了问题(无旋转)");
                                            ModSimLoader.log.error(this.direction + ":" + this.blueprint.direction);
                                        }
                                    } else {
                                        st = st.withRotation(Rotation.CLOCKWISE_180);
                                        if (!this.isSameBlock(this.folk.entity.world.getBlockState(newBP).getBlock(), fs_st_block)) {
                                            this.folk.entity.world.setBlockState(newBP, st);

                                        }
                                        ModSimLoader.log.info("方块应该面向 " + facing.rotateY().rotateY().toString() + " (快速翻转)");
                                        ModSimLoader.log.info("方块实际面向 " + ((EnumFacing) st.getValue(prop)).toString());
                                    }
                                } else {
                                    this.folk.entity.world.setBlockState(newBP, st);
                                }

                            }
                        }
                    }

                    if (ModSimLoader.gamemode != 1) {
                        ModSimLoader.addMoney(-0.02F);
                    }

                }
                this.placedBlocks.add(new V3(newBP));
            }
            //放置方块
            this.folk.setStatus(new TextComponentTranslation("container.sim.JobBuilder3", new Object[0]).getUnformattedText());

            //在客户端生成粒子
            for (int i = 0; i < 16; ++i) {
                double d0 = (double) ((float) newBP.getX() + (5.0F + new Random().nextFloat() * 6.0F) / 16.0F);
                double d1 = (double) ((float) newBP.getY() + 0.8125F);
                double d2 = (double) ((float) newBP.getZ() + (5.0F + new Random().nextFloat() * 6.0F) / 16.0F);
                double d3 = 0.0D;
                double d4 = 0.0D;
                double d5 = 0.0D;
                this.jobWorld.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, d0, d1, d2, 0.0D, 0.0D, 0.0D, new int[0]);
            }

            // 方块放置成功后，播放音效和动画
            if (this.folk != null && this.folk.entity != null) {
                //摇手（使用EntityNpc的swing方法，发送动画包到客户端）
                this.folk.entity.swing();
                //播放放置方块音效（每1.5秒播放一次，避免重叠）
                long now = System.currentTimeMillis();
                if (now - this.timeSwingArm > 1500) {
                    this.timeSwingArm = now;
                    // 播放方块放置音效
                    SoundEvent placeSound = SoundEvents.BLOCK_STONE_PLACE;
                    if (placeSound != null && placeSound.getRegistryName() != null) {
                        this.jobWorld.playSound(null, this.folk.entity.posX, this.folk.entity.posY, this.folk.entity.posZ, placeSound, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    }
                }
            }

            int b4 = (int) Math.floor(this.folk.skillBuilding);
            //建筑等级
            if (this.folk.skillBuilding < 10.0F) {
                NpcData var10000 = this.folk;
                var10000.skillBuilding = (float) (var10000.skillBuilding + 0.001D / b4);
            }

            int aft = (int) Math.floor(this.folk.skillBuilding);
            if (b4 != aft) {
                ModSimLoader.sendChat(this.folk.getName() + new TextComponentTranslation("container.sim.job.builder_constructor_levelled", new Object[0]).getUnformattedText() + aft);
            }

            ++this.x;
            if (this.x > this.blueprint.length - 1) {
                this.x = 0;
                ++this.z;
            }

            if (this.z > this.blueprint.width - 1) {
                this.z = 0;
                ++this.y;
            }
            ++this.blockNumber;

            if (this.blockNumber >= this.blueprint.structure.length) {
                this.folk.fire();
                if (this.constructorBlock != null) {
                    this.constructorBlock.employee = null;
                }
                ModSimLoader.log.info("从中间功能检查中激发");
                this.isOk=true;
                this.createBuilding();


                return;
            }
            try {
                BlockPos tempBP;
                Block f_block = this.blueprint.structure[this.blockNumber].getiBlockState().getBlock();
                Structure var20;
                if (this.direction == 0) {
                    tempBP = new BlockPos(this.startPos.getX() + this.x, this.startPos.getY() + this.y, this.startPos.getZ() - this.z);
                } else if (this.direction == 1) {
                    tempBP = new BlockPos(this.startPos.getX() + this.z, this.startPos.getY() + this.y, this.startPos.getZ() + this.x);
                } else if (this.direction == 2) {
                    tempBP = new BlockPos(this.startPos.getX() - this.x, this.startPos.getY() + this.y, this.startPos.getZ() + this.z);
                } else {
                    tempBP = new BlockPos(this.startPos.getX() - this.z, this.startPos.getY() + this.y, this.startPos.getZ() - this.x);
                }
                Block n_block = this.folk.entity.world.getBlockState(tempBP).getBlock();
                while(n_block == f_block){
                    ++this.x;
                    if (this.x > this.blueprint.length - 1) {
                        this.x = 0;
                        ++this.z;
                    }

                    if (this.z > this.blueprint.width - 1) {
                        this.z = 0;
                        ++this.y;
                    }
                    this.placedBlocks.add(new V3(tempBP));
                    ++this.blockNumber;
                    if (this.blockNumber >= this.blueprint.structure.length) {
                        this.folk.fire();
                        if (this.constructorBlock != null) {
                            this.constructorBlock.employee = null;
                        }
                        ModSimLoader.log.info("从中间功能检查中激发");
                        this.isOk=true;
                        this.createBuilding();

                        return;
                    }
                    if (this.direction == 0) {
                        tempBP = new BlockPos(this.startPos.getX() + this.x, this.startPos.getY() + this.y, this.startPos.getZ() - this.z);
                    } else if (this.direction == 1) {
                        tempBP = new BlockPos(this.startPos.getX() + this.z, this.startPos.getY() + this.y, this.startPos.getZ() + this.x);
                    } else if (this.direction == 2) {
                        tempBP = new BlockPos(this.startPos.getX() - this.x, this.startPos.getY() + this.y, this.startPos.getZ() + this.z);
                    } else {
                        tempBP = new BlockPos(this.startPos.getX() - this.z, this.startPos.getY() + this.y, this.startPos.getZ() - this.x);
                    }
                    f_block = this.blueprint.structure[this.blockNumber].getiBlockState().getBlock();
                    n_block = this.folk.entity.world.getBlockState(tempBP).getBlock();

                    //控制箱
                    if (f_block == BlockLoader.blockControlBox) {
                        this.controllerPos = tempBP;
                        //生活方块地毯
                    } else if (f_block == BlockLoader.blockLiving) {
                        this.livingPos = new BlockPos(newBP.getX(), newBP.getY() - 0.5, newBP.getZ());
                    }
                    //特除方块
                    if (f_block == BlockLoader.blockSpecial) {
                        int  meta=fs_structure.getMeta();
                        V3 v3 = new V3(tempBP.getX(), tempBP.getY(), tempBP.getZ(), f_block, meta);
                        this.blockSpecial.add(v3);
                    }
                    //在客户端生成粒子
                    for (int i = 0; i < 16; ++i) {
                        double d0 = (double) ((float) tempBP.getX() + (5.0F + new Random().nextFloat() * 6.0F) / 16.0F);
                        double d1 = (double) ((float) tempBP.getY() + 0.8125F);
                        double d2 = (double) ((float) tempBP.getZ() + (5.0F + new Random().nextFloat() * 6.0F) / 16.0F);
                        this.jobWorld.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, d0, d1, d2, 0.0D, 0.0D, 0.0D, new int[0]);
                    }
                }
                /*while (f_block==Blocks.AIR) {
                    if (n_block != f_block) {
                        this.folk.entity.world.setBlockState(newBP, Blocks.AIR.getDefaultState());
                        this.placeInJobChest(new ItemStack(n_block));
                    }
                    ++this.x;
                    if (this.x > this.blueprint.length - 1) {
                        this.x = 0;
                        ++this.z;
                    }

                    if (this.z > this.blueprint.width - 1) {
                        this.z = 0;
                        ++this.y;
                    }
                    this.placedBlocks.add(new V3(tempBP));
                    ++this.blockNumber;
                    if (this.direction == 0) {
                        tempBP = new BlockPos(this.startPos.getX() + this.x, this.startPos.getY() + this.y, this.startPos.getZ() - this.z);
                    } else if (this.direction == 1) {
                        tempBP = new BlockPos(this.startPos.getX() + this.z, this.startPos.getY() + this.y, this.startPos.getZ() + this.x);
                    } else if (this.direction == 2) {
                        tempBP = new BlockPos(this.startPos.getX() - this.x, this.startPos.getY() + this.y, this.startPos.getZ() + this.z);
                    } else {
                        tempBP = new BlockPos(this.startPos.getX() - this.z, this.startPos.getY() + this.y, this.startPos.getZ() - this.x);
                    }
                    f_block = this.blueprint.structure[this.blockNumber].getiBlockState().getBlock();
                    n_block = this.folk.entity.world.getBlockState(tempBP).getBlock();
                    //在客户端生成粒子
                    for (int i = 0; i < 16; ++i) {
                        double d0 = (double) ((float) newBP.getX() + (5.0F + new Random().nextFloat() * 6.0F) / 16.0F);
                        double d1 = (double) ((float) newBP.getY() + 0.8125F);
                        double d2 = (double) ((float) newBP.getZ() + (5.0F + new Random().nextFloat() * 6.0F) / 16.0F);
                        mc.world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, d0, d1, d2, 0.0D, 0.0D, 0.0D, new int[0]);
                    }
                }*/
            } catch (Exception var11) {
                StackTraceElement element = var11.getStackTrace()[0];
                ModSimLoader.log.error("因错误而解雇的员工1：" + var11.getMessage() + "行数：" + element.getLineNumber());
                this.folk.fire();
                if(this.constructorBlock!=null){
                    this.constructorBlock.employee = null;
                }
                this.createBuilding();
                this.isOk=true;
                return;
            }
        } catch (Exception var12) {
            StackTraceElement element = var12.getStackTrace()[0];
            ModSimLoader.log.error("因错误而解雇的员工2：" + var12.getMessage() + "行数：" + element.getLineNumber());
            this.folk.fire();
            if (this.constructorBlock != null) {
                this.constructorBlock.employee = null;
            }
            this.createBuilding();
            this.isOk=true;
        }

    }

    /**
     * @return boolean
     * @Author fan
     * @Description //TODO 是否是同一个块
     * @Date 16:37 2022/11/1
     * @Param [b1, b2]
     **/
    public boolean isSameBlock(Block b1, Block b2) {
        if (b1 == b2) {
            return true;
        } else {
            //泥土 玻璃
            return b1 == Blocks.DIRT && b2 == Blocks.GRASS || b1 == Blocks.GRASS && b2 == Blocks.DIRT;
        }
    }

    /**
     * P10(S0): 蓝图安全白名单。
     * 这些方块要么能绕过玩家权限（命令方块/结构方块），
     * 要么会在城镇里制造不可控破坏（TNT/岩浆/火）。
     */
    private boolean isDangerousBlueprintBlock(Block block) {
        return block == Blocks.COMMAND_BLOCK
                || block == Blocks.CHAIN_COMMAND_BLOCK
                || block == Blocks.REPEATING_COMMAND_BLOCK
                || block == Blocks.STRUCTURE_BLOCK
                || block == Blocks.BARRIER
                || block == Blocks.TNT
                || block == Blocks.LAVA
                || block == Blocks.FLOWING_LAVA
                || block == Blocks.FIRE;
    }

    /**
     * P14(S1): 按实际放置方向旋转门/床的朝向，避免所有建筑的门都朝北、床都朝西。
     */
    private EnumFacing rotateFacing(EnumFacing base) {
        int steps = (this.direction - this.blueprint.direction + 4) % 4;
        EnumFacing facing = base;
        for (int i = 0; i < steps; i++) {
            facing = facing.rotateY();
        }
        return facing;
    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 当建筑完成时创建建筑物
     * @Date 16:31 2022/11/1
     * @Param []
     **/
    public void createBuilding() {
        try {
            if (this.livingPos == null) {
                this.livingPos = this.controllerPos;
                ModSimLoader.log.warn("建筑 {} 没有居住标记(block_living)，居住点退化为控制箱位置，NPC 睡觉位置可能异常", this.blueprint.name);
            }
            V3 v3 = new V3(this.controllerPos);
            Building build = null;
            for (Building building : ModSimLoader.buildings) {
                if (building.controlXYZ.equals(v3)) {
                    build = building;
                    break;
                }
            }
            //创建建筑物
            if (build == null) {
                build = new Building(this.blueprint.name, 10.0F, new V3(this.controllerPos), new V3(this.livingPos));
            }
            build.buildingName=this.blueprint.name;
            build.blockSpecial = this.blockSpecial;
            //建筑物类型
            build.buildingType = this.blueprint.buildingType;
            //建筑物的结构
            build.structure = new CopyOnWriteArrayList<V3>(this.placedBlocks);
            //工作类型
            build.jobType = this.blueprint.jobType;
            //建筑作者
            build.author = this.blueprint.author;
            //租金
            build.rent = (float) this.blueprint.structure.length * 0.01F;
            //描述
            build.desc = this.blueprint.desc;
            ModSimLoader.buildings.add(build);
            //保存建筑
            build.saveBuilding();
            if(this.isOk){
                if (this.conBox != null) {
                    this.conBox.folk = null;
                    this.conBox.setDead();
                }
                //已完成建设 【】
                String text = this.folk.getName() + new TextComponentTranslation("container.sim.job.builder_constructor_completed", new Object[0]).getUnformattedText() + this.blueprint.name;
                ModSimLoader.sendChat(text);
                //播放声音
                SoundEvent cashshort = SoundRegistry.CONSTRUCTION;
                if (cashshort == null || cashshort.getRegistryName() == null) {
                    ModSimLoader.log.error("播放失败：sim:cashshort 声音事件未注册");
                } else {
                    this.jobWorld.playSound((EntityPlayer) null, build.livingXYZ.x, build.livingXYZ.y, build.livingXYZ.z, cashshort, SoundCategory.BLOCKS, 1.0F, 1.0F);
                }
                this.conBox.setDead();
            }

        } catch (Exception e) {
            if(this.conBox!=null){
                this.conBox.setDead();
            }
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("createBuilding出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }

    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 创建悬浮的控制箱
     * @Date 16:25 2022/11/1
     * @Param []
     **/
    public void createConBox() {
        try {
            EntityConBox entityConBox = new EntityConBox(this.folk.entity.world, this);
            if (entityConBox != null) {
                this.conBox = entityConBox;
                this.conBox.folk = this.folk;
                this.conBox.builderJob = this;
                this.conBox.setLocationAndAngles(this.workPlace.x + 2.0D, this.workPlace.y, this.workPlace.z, 0.0F, 0.0F);
                if (!this.folk.entity.world.isRemote) {
                    this.folk.entity.world.spawnEntity(this.conBox);
                }
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("createConBox出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 抵达
     * @Date 16:31 2022/11/1
     * @Param []
     **/
    @Override
    public void onArrive() {
    }

    @Override
    public String toString() {
        return new TextComponentTranslation("container.sim.Vocation1", new Object[0]).getUnformattedText();
    }
}
