package com.trhsy.sim.npcCode.task;

import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.NpcData;
import com.trhsy.sim.npcCode.block.FarmBox;
import com.trhsy.sim.npcCode.enums.FarmType;
import com.trhsy.sim.npcCode.job.Job;
import com.trhsy.sim.util.SeasonCompat;
import com.trhsy.sim.util.ModCropCompat;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockStem;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.IPlantable;

import java.util.List;

/**
 * @ClassName JobTaskFarmerPlant
 * @Description todo
 * @Author TRHSY 种植
 * @Date 2023/8/1421:36
 **/
public class JobTaskFarmerPlant extends JobTask {
    //农田箱
    public FarmBox farm;
    String status;
    //收获检查
    long harvestCheck = 0L;
    //
    long qsCheck = 0L;
    long growCheck = 0L;
    //没有种子等待时间，
    private int noNeed;
    //摇臂和声音循环
    private long swingArmCheck = 0L;

    public JobTaskFarmerPlant(Job j, long ms, String status, FarmBox farm) {
        super(j, ms);
        this.status = status;
        this.noNeed = 0;
        this.farm = farm;
    }

    @Override
    public void onTaskBegin() {
        this.job.folk.setStatus(this.status);
        //设置固定不动
        this.folk.stayPut = true;
        //锄地
        this.plant();

    }

    @Override
    public void onUpdate() {
        try {
//ModSimLoader.log.info("农民任务检查："+System.currentTimeMillis());
            //收获检查
            //this.harvestCheck = System.currentTimeMillis();
            if (ModSimLoader.money > 0.02F) {
                //周围五格内查找箱子
                if (this.job.jobChests == null) {
                    //没有箱子
                    this.folk.setStatus(new TextComponentTranslation("container.sim.WAITINGFORCHEST",new Object[0]).getUnformattedText());
                    return;
                }
                // P11(S1): 技能 10 时旧公式会变成 0，导致每 tick 扫描/种植。
                long now = System.currentTimeMillis();
                long interval = Math.max(800L, 2500L - (long) (200L * Math.min(10.0F, this.folk.skillFarming)));
                if (ModSimLoader.gamemode == 1) {
                    interval = Math.min(interval, 200L);
                }
                if (now - this.harvestCheck >= interval) {
                    this.plant();
                }

            } else {
                //没有钱付给我
                this.folk.setStatus(new TextComponentTranslation("container.sim.JobBuilder2",new Object[0]).getUnformattedText());
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("JobFarmer-onUpdate出错了:" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 农场等级
     * @Date 22:00 2022/12/9
     * @Param []
     **/
    public void addFarmingLevel() {
        try {
            int b4 = (int) Math.floor((double) this.folk.skillFarming);
            ModSimLoader.addMoney(-0.01F);
            if (this.folk.skillFarming < 10.0F) {
                NpcData var10000 = this.folk;
                var10000.skillFarming = (float) ((double) var10000.skillFarming + 0.001D / (double) b4);
            }

            int aft = (int) Math.floor((double) this.folk.skillFarming);
            if (b4 != aft) {
                //的农民等级刚刚达到了
                ModSimLoader.sendChat(this.folk.getName() + " " + new TextComponentTranslation("container.sim.job_farmer_has",new Object[0]).getUnformattedText() + " " + aft);
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("JobFarmer-addFarmingLevel出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 种植
     * @Date 10:13 2022/12/10
     * @Param []
     **/
    public void plant() {
        try {
            // P11(S1): 每轮扫描后重新计时，避免没有种子/空地时每 tick 重复扫描。
            this.harvestCheck = System.currentTimeMillis();
            //先获取农田箱的 类型
            FarmType farmType = this.farm.farmType;
            this.folk.setStatus(new TextComponentTranslation("container.sim.job.crop.farmer.Planting",new Object[0]).getUnformattedText());
            //胡萝卜
            if (farmType == FarmType.CARROT) {
                plant1(new ItemStack(Items.CARROT));
                //西瓜
            } else if (farmType == FarmType.MELON) {
                plant1(new ItemStack(Items.MELON_SEEDS));
                //土豆
            } else if (farmType == FarmType.POTATO) {
                plant1(new ItemStack(Items.POTATO));
                //南瓜
            } else if (farmType == FarmType.PUMPKIN) {
                plant1(new ItemStack(Items.PUMPKIN_SEEDS));
                //小麦
            } else if (farmType == FarmType.WHEAT) {
                plant1(new ItemStack(Items.WHEAT_SEEDS));
                //甜菜根
            } else if (farmType == FarmType.BEETROOTS) {
                plant1(new ItemStack(Items.BEETROOT_SEEDS));
                //甘蔗
            } else if (farmType == FarmType.SUGAR) {
                //循环农场的宽
                for (int z = 0; z < this.farm.z; ++z) {
                    //循环长
                    for (int x = 0; x < this.farm.x; ++x) {
                        boolean hasBlock = false;
                        BlockPos bp = new BlockPos(this.farm.getCorner().offset(this.farm.facing, x).offset(this.farm.facing.rotateY(), z));
                        if (!this.folk.entity.world.isBlockLoaded(bp)) {
                            continue;
                        }
                        if (SeasonCompat.isWinter(this.folk.entity.world) && this.folk.entity.world.canSeeSky(bp)) {
                            continue;
                        }
                        Block b = this.folk.entity.world.getBlockState(bp).getBlock();
                        Block b1 = this.folk.entity.world.getBlockState(bp.down()).getBlock();
                        //包含灌木 不是庄家
                        if (b instanceof BlockBush && !(b instanceof BlockCrops)) {
                            this.folk.entity.world.destroyBlock(bp, false);
                            //设置为空气
//                        this.folk.entity.world.setBlockToAir(bp);
                        }
                        //如果当前为空气
                        if (this.folk.entity.world.isAirBlock(bp)) {
                            // 泥土 草地 沙子
                            if (b1 == Blocks.DIRT || b1 == Blocks.GRASS || b1 == Blocks.SAND) {
                                ItemStack seed = null;
                                List<IInventory> iterator = this.job.inventoriesFindClosest(this.job.workPlace, 5);
                                for (IInventory inv : iterator) {
                                    for (int i = 0; i < inv.getSizeInventory(); ++i) {
                                        ItemStack slot = inv.getStackInSlot(i);
                                        if (slot != null) {
                                            Item item = slot.getItem();
                                            String unlocalizedName = item.getUnlocalizedName();
                                            if (item instanceof IPlantable || unlocalizedName.contains("reeds")
                                            || unlocalizedName.contains("cactus") || unlocalizedName.contains("dye")) {
                                                seed = slot;
                                                break;
                                            }
                                        }
                                    }
                                    if (seed != null) {
                                        break;
                                    }
                                }
//可种植的种子为空
                                if (seed == null) {
                                    //没有种子
                                    this.folk.setStatus(new TextComponentTranslation("container.sim.job.crop.farmer.no_seeds",new Object[0]).getUnformattedText());
                                    this.noNeed++;
                                    if (this.noNeed / 10 == 1) {
                                        this.noNeed = 0;
                                        this.completed = true;
                                    }
                                    return;
                                }
//                            IPlantable plantable = Blocks.REEDS;
                                try {
                                    for (IInventory inv : iterator) {
                                        for (int i = 0; i < inv.getSizeInventory(); ++i) {
                                            ItemStack slot = inv.getStackInSlot(i);
                                            //可种植
                                            if (slot != null && slot.isItemEqual(seed)) {
                                                hasBlock = true;
                                                inv.decrStackSize(i, 1);
                                                break;
                                            }
                                        }
                                        if (hasBlock) {
                                            break;
                                        }
                                    }

                                    //设置状态 种植 作物
                                    this.folk.setStatus(new TextComponentTranslation("container.sim.job.crop.farmer.Planting",new Object[0]).getUnformattedText() + " " + seed.getDisplayName());
                                    //种植 作物
                                    this.folk.entity.world.setBlockState(bp, Blocks.REEDS.getDefaultState());
                                    if ((System.currentTimeMillis() - this.swingArmCheck) > 1000 * 3) {
                                        //摇摆手臂
                                        this.folk.entity.swingArm(EnumHand.MAIN_HAND);
                                    }
//                        seed.shrink(1);
                                    //增加农民等级
                                    this.addFarmingLevel();
                                    //更新收获时间
                                    this.harvestCheck = System.currentTimeMillis();
                                    return;
                                } catch (Exception e) {
                                    StackTraceElement element = e.getStackTrace()[0];
                                    ModSimLoader.log.error("种植甘蔗发生了错误：" + e.getMessage() + "行数：" + element.getLineNumber());
                                }

                            }
                        }
                    }
                }
                this.completed = true;
                //仙人掌
            } else if (farmType == FarmType.CACTUS) {
                //循环农场的宽
                for (int z = 0; z < this.farm.z; ++z) {
                    //循环长
                    for (int x = 0; x < this.farm.x; ++x) {
                        boolean hasBlock = false;
                        BlockPos bp = new BlockPos(this.farm.getCorner().offset(this.farm.facing, x).offset(this.farm.facing.rotateY(), z));
                        Block b = this.folk.entity.world.getBlockState(bp).getBlock();
                        Block b1 = this.folk.entity.world.getBlockState(bp.down()).getBlock();
                        //包含灌木 不是庄家
                        if (b != Blocks.CACTUS) {
                            this.folk.entity.world.destroyBlock(bp, false);
                            //设置为空气
//                        this.folk.entity.world.setBlockToAir(bp);
                        }
                        //如果当前为空气
                        if (this.folk.entity.world.isAirBlock(bp)) {
                            if (b1 == Blocks.SAND) {

                                ItemStack seed = null;
                                List<IInventory> iterator = this.job.inventoriesFindClosest(this.job.workPlace, 5);
                                for (IInventory inv : iterator) {
                                    for (int i = 0; i < inv.getSizeInventory(); ++i) {
                                        ItemStack slot = inv.getStackInSlot(i);
                                        if (slot != null) {
                                            Item item = slot.getItem();
                                            String unlocalizedName = item.getUnlocalizedName();
                                            if (item instanceof IPlantable || unlocalizedName.contains("cactus")) {
                                                seed = slot;
                                                break;
                                            }
                                        }
                                    }
                                    if (seed != null) {
                                        break;
                                    }
                                }
                                //可种植的种子为空
                                if (seed == null) {
                                    //没有种子
                                    this.folk.setStatus(new TextComponentTranslation("container.sim.job.crop.farmer.no_seeds",new Object[0]).getUnformattedText());
                                    this.noNeed++;
                                    if (this.noNeed / 10 == 1) {
                                        this.noNeed = 0;
                                        this.completed = true;
                                    }
                                    return;
                                }
                                try {
                                    for (IInventory inv : iterator) {
                                        for (int i = 0; i < inv.getSizeInventory(); ++i) {
                                            ItemStack slot = inv.getStackInSlot(i);
                                            //可种植
                                            if (slot != null && slot.isItemEqual(seed)) {
                                                hasBlock = true;
                                                inv.decrStackSize(i, 1);
                                                break;
                                            }
                                        }
                                        if (hasBlock) {
                                            break;
                                        }
                                    }

                                    //设置状态 种植 作物
                                    this.folk.setStatus(new TextComponentTranslation("container.sim.job.crop.farmer.Planting",new Object[0]).getUnformattedText() + " " + seed.getDisplayName());
                                    //种植 作物
                                    this.folk.entity.world.setBlockState(bp, Blocks.CACTUS.getDefaultState());
                                    if ((System.currentTimeMillis() - this.swingArmCheck) > 1000 * 3) {
                                        //摇摆手臂
                                        this.folk.entity.swingArm(EnumHand.MAIN_HAND);
                                    }
//                        seed.shrink(1);
                                    //增加农民等级
                                    this.addFarmingLevel();
                                    //更新收获时间
                                    this.harvestCheck = System.currentTimeMillis();
                                    return;
                                } catch (Exception e) {
                                    StackTraceElement element = e.getStackTrace()[0];
                                    ModSimLoader.log.error("种植仙人掌发生了错误：" + e.getMessage() + "行数：" + element.getLineNumber());
                                }

                            }
                        }
                    }
                }
                this.completed = true;
                //可可豆
            } else if (farmType == FarmType.COCOA) {
                //未知的
                for (int z = 0; z < this.farm.z; ++z) {
                    //循环长
                    for (int x = 0; x < this.farm.x; ++x) {
                        for (int y = 0; y < 2; ++y) {
                            boolean hasBlock = false;
                            BlockPos bp = new BlockPos(this.farm.getCorner().offset(this.farm.facing, x).offset(this.farm.facing.rotateY(), z));
                            bp = new BlockPos(bp.getX(), bp.getY() + y, bp.getZ());
                            //包含灌木 不是庄家
                            boolean f1 = this.folk.entity.world.isAirBlock(bp);
                            //如果当前为空气
                            if (f1) {
                                ItemStack seed = null;
                                List<IInventory> iterator = this.job.inventoriesFindClosest(this.job.workPlace, 5);
                                for (IInventory inv : iterator) {
                                    for (int i = 0; i < inv.getSizeInventory(); ++i) {
                                        ItemStack slot = inv.getStackInSlot(i);
                                        if (slot != null) {
                                            Item item = slot.getItem();
                                            String unlocalizedName = item.getUnlocalizedName();
                                            if (item instanceof IPlantable || unlocalizedName.contains("item.dyePowder")) {
                                                seed = slot;
                                                break;
                                            }
                                        }
                                    }
                                    if (seed != null) {
                                        break;
                                    }
                                }
                                //可种植的种子为空
                                if (seed == null) {
                                    //没有种子
                                    this.folk.setStatus(new TextComponentTranslation("container.sim.job.crop.farmer.no_seeds",new Object[0]).getUnformattedText());
                                    this.noNeed++;
                                    if (this.noNeed / 10 == 1) {
                                        this.noNeed = 0;
                                        this.completed = true;
                                    }
                                    return;
                                }
                                try {
                                    for (IInventory inv : iterator) {
                                        for (int i = 0; i < inv.getSizeInventory(); ++i) {
                                            ItemStack slot = inv.getStackInSlot(i);
                                            //可种植
                                            if (slot != null && slot.isItemEqual(seed)) {
                                                hasBlock = true;
                                                inv.decrStackSize(i, 1);
                                                break;
                                            }
                                        }
                                        if (hasBlock) {
                                            break;
                                        }
                                    }
                                    //设置状态 种植 作物
                                    this.folk.setStatus(new TextComponentTranslation("container.sim.job.crop.farmer.Planting",new Object[0]).getUnformattedText() + " " + seed.getDisplayName());
                                    EnumFacing facing = EnumFacing.NORTH;
                                    Block north = this.folk.entity.world.getBlockState(bp.north()).getBlock();
                                    //东
                                    Block east = this.folk.entity.world.getBlockState(bp.east()).getBlock();
                                    //南
                                    Block south = this.folk.entity.world.getBlockState(bp.south()).getBlock();
                                    //西
                                    Block west = this.folk.entity.world.getBlockState(bp.west()).getBlock();
                                    Block fs_bss = Blocks.LOG.getStateFromMeta(3).getBlock();
                                    if (north == fs_bss) {
                                        facing = EnumFacing.SOUTH;
                                    } else if (east == fs_bss) {
                                        facing = EnumFacing.WEST;
                                    } else if (south == fs_bss) {
                                        facing = EnumFacing.NORTH;
                                    } else if (west == fs_bss) {
                                        facing = EnumFacing.EAST;
                                    }
                                    //种植 作物
                                    IBlockState iblockstate1 = Blocks.COCOA.getStateForPlacement(this.folk.entity.world, bp, facing, bp.getX(), bp.getY(), bp.getZ(), 0, this.folk.entity);
                                    this.folk.entity.world.setBlockState(bp, iblockstate1, 10);
                                    if ((System.currentTimeMillis() - this.swingArmCheck) > 1000 * 3) {
                                        //摇摆手臂
                                        this.folk.entity.swingArm(EnumHand.MAIN_HAND);
                                    }
//                        seed.shrink(1);
                                    //增加农民等级
                                    this.addFarmingLevel();
                                    //更新收获时间
                                    this.harvestCheck = System.currentTimeMillis();
                                    return;
                                } catch (Exception e) {
                                    StackTraceElement element = e.getStackTrace()[0];
                                    ModSimLoader.log.error("种植仙人掌发生了错误：" + e.getMessage() + "行数：" + element.getLineNumber());
                                }
                            }
                        }
                    }
                }
                this.completed = true;
            } else {
                plant1(null);
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("JobFarmer-plant出错了:" + e.getMessage() + "行数：" + element.getLineNumber());
        }

    }

    public void plant1(ItemStack seeds) {
        try {
            //循环宽
            for (int z = 0; z < this.farm.z; ++z) {
                //循环长
                for (int x = 0; x < this.farm.x; ++x) {
                    boolean hasBlock = false;
                    //获得角落
                    BlockPos bp = new BlockPos(this.farm.getCorner().offset(this.farm.facing, x).offset(this.farm.facing.rotateY(), z));
                    if (!this.folk.entity.world.isBlockLoaded(bp)) {
                        continue;
                    }
                    // P13(S2): 冬季露天不种植；温室/室内仍可种。
                    if (SeasonCompat.isWinter(this.folk.entity.world) && this.folk.entity.world.canSeeSky(bp)) {
                        continue;
                    }
//                ItemStack seed = ItemStack.EMPTY;
                    ItemStack seed = null;
                    List<IInventory> iterator = this.job.inventoriesFindClosest(this.job.workPlace, 5);
                    for (IInventory inv : iterator) {
                        for (int i = 0; i < inv.getSizeInventory(); ++i) {
                            ItemStack slot = inv.getStackInSlot(i);
                            if (slot != null) {
                                //可种植 / 是甘蔗
                                if (seeds == null) {
                                    Item item = slot.getItem();
                                    String unlocalizedName = item.getUnlocalizedName();
                                    if (item instanceof IPlantable || unlocalizedName.contains("reeds")
                                            || unlocalizedName.contains("cactus") || unlocalizedName.contains("dye")) {
                                        seed = slot;
                                        break;
                                    }
                                } else if (slot.isItemEqual(seeds)) {
                                    seed = slot;
                                    break;
                                }
                            }

                        }
                        if (seed != null) {
                            break;
                        }
                    }
                    //可种植的种子为空
                    if (seed == null) {
                        //没有种子
                        this.folk.setStatus(new TextComponentTranslation("container.sim.job.crop.farmer.no_seeds",new Object[0]).getUnformattedText());
                        this.noNeed++;
                        if (this.noNeed / 10 == 1) {
                            this.noNeed = 0;
                            this.completed = true;
                        }
                        return;
                    }
                    try {
                        // P15(S1): 统一用 IPlantable/名称兜底解析作物方块，
                        // 不再反射 ItemSeeds 的私有 crops 字段；Pam's 等模组种子也能种。
                        Item item = seed.getItem();
                        Block crop = ModCropCompat.resolveCropBlock(seed, this.folk.entity.world, bp.down());
                        if (crop == null) {
                            continue;
                        }

//                    //获取种植下方的物品是否是 农田等可种植区域
                        if (crop == null) {
                            continue;
                        }
                        IBlockState soil = this.folk.entity.world.getBlockState(bp.down());
                        //Block crop=plantable.getPlant(this.folk.entity.world,bp.down()).getBlock();
                        //确定此块是否可以支持传入的植物，允许其种植和生长。一些例子：芦苇检查它是否是芦苇，或者它的沙子/泥土/草和水附近的仙人掌检查它是否为仙人掌，或者如果它的沙子Nether类型检查灵魂沙子作物检查耕土洞穴检查它是否坚硬地面平原检查它的草或泥土水检查它是否静止
                        // P11(S1): 不再无条件把物品强转 IPlantable。仙人掌/可可豆等 ItemBlock 不实现 IPlantable，
                        // 旧代码会在这里 ClassCastException；优先用作物方块自身做 canSustainPlant 判断。
                        IPlantable plantable = crop instanceof IPlantable
                                ? (IPlantable) crop
                                : (item instanceof IPlantable ? (IPlantable) item : null);
                        if (plantable == null) {
                            continue;
                        }
                        boolean fs_canSustainPlant = soil.getBlock().canSustainPlant(soil, this.folk.entity.world, bp.down(), EnumFacing.UP, plantable)
                                || ModCropCompat.canPlantModCropOn(soil.getBlock(), crop);
                        // P11(S1): 普通作物需要 9 级光照；甘蔗/仙人掌/可可豆不受这条限制。
                        boolean lightOk = crop == Blocks.REEDS || crop == Blocks.CACTUS || crop == Blocks.COCOA
                                || this.folk.entity.world.getLightFromNeighbors(bp) >= 9;
                        //北

                        /* Chunk chunk =this.folk.entity.world.getChunkFromChunkCoords(bp.north().getX(),bp.west().getZ());*/
                        Block north = this.folk.entity.world.getBlockState(bp.north()).getBlock();
                        boolean f1 = north instanceof BlockStem;
                        //东
                        /*Chunk chunk1 =this.folk.entity.world.getChunkFromChunkCoords(bp.east().getX(),bp.west().getZ());*/
                        Block east = this.folk.entity.world.getBlockState(bp.east()).getBlock();
                        boolean f2 = east instanceof BlockStem;
                        //南
                        //Chunk chunk2 =this.folk.entity.world.getChunkFromChunkCoords(bp.south().getX(),bp.west().getZ());
                        Block south = this.folk.entity.world.getBlockState(bp.south()).getBlock();
                        boolean f3 = south instanceof BlockStem;
                        //西
                        //Chunk chunk3 =this.folk.entity.world.getChunkFromChunkCoords(bp.west().getX(),bp.west().getZ());
                        Block west = this.folk.entity.world.getBlockState(bp.west()).getBlock();
                        boolean f4 = west instanceof BlockStem;
                        IBlockState iBlockState = this.folk.entity.world.getBlockState(bp);
                        Block block = iBlockState.getBlock();
                        boolean f7 = block instanceof BlockStem;
                        //是否是空气方块
                        boolean f5 = this.folk.entity.world.isAirBlock(bp);
                        String b_u_name = block.getUnlocalizedName();
//                    System.out.println("当前种植的作物是：" + b_u_name);
                        if (!f1 && !f2 && !f3 && fs_canSustainPlant && !f4 && f5 && lightOk) {
                            for (IInventory inv : iterator) {
                                for (int i = 0; i < inv.getSizeInventory(); ++i) {
                                    ItemStack slot = inv.getStackInSlot(i);
                                    //可种植
                                    if (slot != null && slot.isItemEqual(seed)) {
                                        hasBlock = true;
                                        inv.decrStackSize(i, 1);
                                        break;
                                    }
                                }
                                if (hasBlock) {
                                    break;
                                }
                            }
                            //设置状态 种植 作物
                            this.folk.setStatus(new TextComponentTranslation("container.sim.job.crop.farmer.Planting",new Object[0]).getUnformattedText() + " " + seed.getDisplayName());
                            //种植 作物
                            this.folk.entity.world.setBlockState(bp, crop.getDefaultState());
                            if ((System.currentTimeMillis() - this.swingArmCheck) > 1000 * 3) {
                                //摇摆手臂
                                this.folk.entity.swing();
                            }
//                        seed.shrink(1);
                            //增加农民等级
                            this.addFarmingLevel();
                            //更新收获时间
                            this.harvestCheck = System.currentTimeMillis();
                            return;
                        }
                    } catch (Exception var10) {
                        StackTraceElement element = var10.getStackTrace()[0];
                        ModSimLoader.log.error("种植发生了错误：" + var10.getMessage() + "行数：" + element.getLineNumber());
                    }
                }
            }
            this.completed = true;
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("JobFarmer-plant1出错了:" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    @Override
    public void onTaskComplete() {

    }
}
