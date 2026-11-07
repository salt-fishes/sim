package com.trhsy.sim.npcCode.task;

import com.trhsy.sim.entity.EntityConBox;
import com.trhsy.sim.loader.BlockLoader;
import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.loader.NetWorkLoader;
import com.trhsy.sim.loader.SoundRegistry;
import com.trhsy.sim.network.client.PacketSendTerrainTypeRequitrements;
import com.trhsy.sim.npcCode.NpcData;
import com.trhsy.sim.npcCode.V3;
import com.trhsy.sim.npcCode.build.TerrainType;
import com.trhsy.sim.npcCode.job.Job;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.npc.task
 * @ClassName: JobTaskTerrain
 * @Description:
 * @date 2023/09/18 下午 3:50
 */
public class JobTaskTerrain extends JobTask {
    private ItemStack missingBlock = null;
    public TerrainType terrainType;
    private int totalBlockCount = 0;
    private List<V3> closestBlocks = new CopyOnWriteArrayList();
    /**
     * 自上一个块位置的时间
     **/
    private long timeSinceLastBlockPlace = 0L;
    /**
     * 建造位置
     **/
    private BlockPos constructorPos;
    /**
     * 悬浮的控制箱
     **/
    public EntityConBox conBox;
    private transient int counter = 0;
    /**
     * 上次摆动手臂的时间
     **/
    private transient long timeSwingArm = 0L;
    public JobTaskTerrain(Job j, long ms, ItemStack missingBlock, TerrainType terrainType, BlockPos constructorPos) {
        super(j, ms);
        this.missingBlock = missingBlock;
        this.terrainType = terrainType;
        this.constructorPos = constructorPos;
        this.conBox = conBox;
    }

    @Override
    public void onTaskBegin() {
        placeBlock();
    }

    @Override
    public void onUpdate() {
        placeBlock();
        //发送建筑蓝图
        NetWorkLoader.net.sendToAll(new PacketSendTerrainTypeRequitrements(this.terrainType, this.folk.ID));
    }

    @Override
    public void onTaskComplete() {

    }

    public void placeBlock() {
        try {
            //重置缺少的块
            this.missingBlock = null;
            Boolean fsMissBlock = true;
            if (ModSimLoader.money < 0.02F) {
                //没有钱付给我！
                this.folk.setStatus(new TextComponentTranslation("container.sim.JobBuilder2",new Object[0]).getUnformattedText());
                //this.completed = true;
                return;
            }
            long now1 = System.currentTimeMillis();
            if (now1 - this.timeSwingArm > 3000) {
                this.timeSwingArm = now1;
                //摇手
                this.folk.entity.swingArm(EnumHand.MAIN_HAND);

            }
            CopyOnWriteArrayList blockIDs;
            if (this.terrainType != null) {
                // P15(S1): 同一位置所在区块卸载后不要继续写方块，直接跳过。
                if (this.closestBlocks != null && !this.closestBlocks.isEmpty()) {
                    V3 first = this.closestBlocks.get(0);
                    if (!this.job.jobWorld.isBlockLoaded(new BlockPos(first.x, first.y, first.z))) {
                        this.closestBlocks.remove(0);
                        return;
                    }
                }
                switch (this.terrainType.terrainType) {
                    case "1":
                        if (this.stage == 0) {
                            //填海
                            blockIDs = new CopyOnWriteArrayList();
                            blockIDs.add(Blocks.WATER);
                            blockIDs.add(Blocks.FLOWING_WATER);
                            this.closestBlocks = null;
                            V3 v5 = new V3(constructorPos.getX(), constructorPos.getY() - 1, constructorPos.getZ());
                            this.setClosestBlocksOfType(v5.toBlockPos(), blockIDs, 30, false, true, true);
                            this.totalBlockCount = this.closestBlocks.size();
                            this.stage = 1;
                        }

                        if (this.totalBlockCount == 0) {
                            //没有什么要地球化的！
                            this.folk.setStatus(new TextComponentTranslation("container.sim.job.terra.farmer.Nothing",new Object[0]).getUnformattedText());
                            //这里没有任何东西能以这种方式被规划
                            ModSimLoader.sendChat(new TextComponentTranslation("container.sim.job.terra.farmer.terraformed",new Object[0]).getUnformattedText());
                            //解雇
                            this.folk.fire();
                            this.folk.stayPut = false;
                            if (this.conBox != null) {
                                this.conBox.folk = null;
                            }
                            this.completed = true;
                            return;
                        } else {
                            //开始地形规划
                            this.folk.setStatus(new TextComponentTranslation("container.sim.job.terra.farmer.process",new Object[0]).getUnformattedText());
                            //for (int i = 0; i < this.closestBlocks.size(); i++) {
                            //当前时间
                            Long now = System.currentTimeMillis();
                            V3 v = (V3) this.closestBlocks.get(0);
                            //计算规划的百分比
                            Double x = (double) this.totalBlockCount;
                            Double y = (double) this.closestBlocks.size();
                            Double percent = (x - y) / x;
                            percent = percent * 100;
                            //环境改造 10% 完成
                            this.folk.setStatus(new TextComponentTranslation("container.sim.job.terra.farmer.Terraforming",new Object[0]).getUnformattedText() + "- " + percent + " % " + new TextComponentTranslation("container.sim.job.terra.farmer.complete",new Object[0]).getUnformattedText());

                            //游戏模式是正常模式
                            if (ModSimLoader.gamemode != 1) {
                                if (this.placeDelayElapsed(now)) {
                                    now = System.currentTimeMillis();
                                    ////上次时间为当前时间
                                    this.timeSinceLastBlockPlace = now;
                                    //获取周围箱子
                                    List<IInventory> inventoriesFindClosest = this.job.findJobChests(5);
                                    //循环箱子
                                    for (IInventory inv : inventoriesFindClosest) {
                                        //循环箱子库存
                                        for (int ij = 0; ij < inv.getSizeInventory(); ij++) {
                                            ItemStack itemStack = inv.getStackInSlot(ij);
                                            //拿走当前需要的块 泥土
                                            if (itemStack != null && itemStack.getItem() == Item.getItemFromBlock(Blocks.DIRT)) {
                                                inv.decrStackSize(ij, 1);
                                                fsMissBlock = false;
                                                break;
                                            }
                                        }
                                    }
                                    if (fsMissBlock) {
                                        //我需要更多的泥土！
                                        this.folk.setStatus(new TextComponentTranslation("container.sim.job.terra.farmer.dirt",new Object[0]).getUnformattedText());
                                        this.missingBlock = new ItemStack(Blocks.DIRT);
                                        //this.completed = true;
                                        return;
                                    }
                                    ModSimLoader.money = (float) ((double) ModSimLoader.money - 0.002D);
                                    BlockPos blockPos2 = new BlockPos(v.x, v.y, v.z);
                                    this.job.jobWorld.setBlockState(blockPos2, Blocks.DIRT.getDefaultState(), 3);
                                } else {
                                    return;
                                }
                            } else {
                                //不是客户端
                                if (!this.job.jobWorld.isRemote) {
                                        now = System.currentTimeMillis();
                                        //上次时间为当前时间
                                        this.timeSinceLastBlockPlace = now;
                                        BlockPos blockPos2 = new BlockPos(v.x, v.y, v.z);
                                        this.job.jobWorld.setBlockState(blockPos2, Blocks.DIRT.getDefaultState(), 3);
                                }
                            }
                            //}
                        }
                        break;
                    case "2":
                        if (this.stage == 0) {
                            //绿化 泥土变草地
                            blockIDs = new CopyOnWriteArrayList();
                            //泥土
                            blockIDs.add(Blocks.DIRT);
                            //草地
                            blockIDs.add(Blocks.GRASS);
                            this.closestBlocks = null;
                            V3 v2 = new V3(constructorPos.getX(), constructorPos.getY() + 1, constructorPos.getZ());
                            this.setClosestBlocksOfType(v2.toBlockPos(), blockIDs, 30, true, true, false);
                            this.totalBlockCount = this.closestBlocks.size();
                            this.stage = 1;
                        }
                        if (this.totalBlockCount == 0) {
                            //没有什么要地球化的！
                            this.folk.status = new TextComponentTranslation("container.sim.job.terra.farmer.Nothing",new Object[0]).getUnformattedText();
                            //这里没有任何东西能以这种方式被规划
                            ModSimLoader.sendChat(new TextComponentTranslation("container.sim.job.terra.farmer.terraformed",new Object[0]).getUnformattedText());
                            //解雇
                            this.folk.fire();
                            this.folk.stayPut = false;
                            if (this.conBox != null) {
                                this.conBox.folk = null;
                            }
                            this.completed = true;
                            return;
                        } else {
                            //开始地形规划
                            this.folk.status = new TextComponentTranslation("container.sim.job.terra.farmer.process",new Object[0]).getUnformattedText();
                            Boolean hasPlacedTree = false;
                            //for (int i = 0; i < this.closestBlocks.size(); i++) {
                            //当前时间
                            Long now = System.currentTimeMillis();
                            V3 v1 = (V3) this.closestBlocks.get(0);
                            this.counter++;
                            if (this.counter % 5 == 0) {
                                hasPlacedTree = true;
                                //计算规划的百分比
                                Double x1 = (double) this.totalBlockCount;
                                Double y1 = (double) this.closestBlocks.size();
                                Double percent1 = (x1 - y1) / x1;
                                percent1 = percent1 * 100;
                                //环境改造 10% 完成
                                this.folk.status = new TextComponentTranslation("container.sim.job.terra.farmer.Terraforming",new Object[0]).getUnformattedText() + "-" + percent1 + " % " + new TextComponentTranslation("container.sim.job.terra.farmer.complete",new Object[0]).getUnformattedText();
                                //是否是创造模式
                                if (ModSimLoader.gamemode != 1) {
                                    //获取周围箱子
                                    List<IInventory> inventoriesFindClosest = this.job.findJobChests(5);
                                    //循环箱子
                                    for (IInventory inv : inventoriesFindClosest) {
                                        //循环箱子库存
                                        for (int ij = 0; ij < inv.getSizeInventory(); ij++) {
                                            ItemStack itemStack = inv.getStackInSlot(ij);
                                            //拿走当前需要的块 树苗
                                            if (itemStack != null && itemStack.getItem() == Item.getItemFromBlock(Blocks.SAPLING)) {
                                                inv.decrStackSize(ij, 1);
                                                fsMissBlock = false;
                                                break;
                                            }
                                        }
                                    }
                                    if (fsMissBlock) {
                                        this.counter = 4;
                                        //没有更多的树苗，放一些在箱子里
                                        this.folk.status = new TextComponentTranslation("container.sim.job.terra.farmer.saplings",new Object[0]).getUnformattedText();
                                        this.missingBlock = new ItemStack(Blocks.SAPLING);
                                        //this.completed = true;
                                        return;
                                    }
                                    if (this.placeDelayElapsed(now)) {
                                        now = System.currentTimeMillis();
                                        ////上次时间为当前时间
                                        this.timeSinceLastBlockPlace = now;
                                        if (hasPlacedTree) {
                                            BlockPos blockPos2_1 = new BlockPos(v1.x, v1.y + 1, v1.z);
                                            this.job.jobWorld.setBlockState(blockPos2_1, Blocks.SAPLING.getDefaultState(), 3);
                                            if (ModSimLoader.gamemode != 1) {
                                                ModSimLoader.money = (float) ((double) ModSimLoader.money - 0.002D);
                                            }
                                            int r = new Random().nextInt(10);
                                            BlockPos blockPos2_2 = new BlockPos(v1.x + 1, v1.y + 1, v1.z);
                                            if (r == 2) {
                                                this.job.jobWorld.setBlockState(blockPos2_2, Blocks.RED_FLOWER.getDefaultState(), 3);
                                            } else if (r == 5) {
                                                this.job.jobWorld.setBlockState(blockPos2_2, Blocks.YELLOW_FLOWER.getDefaultState(), 3);
                                            }
                                        }
                                    } else {
                                        return;
                                    }
                                } else {
                                        now = System.currentTimeMillis();
                                        ////上次时间为当前时间
                                        this.timeSinceLastBlockPlace = now;
                                        if (hasPlacedTree) {
                                            BlockPos blockPos2_1 = new BlockPos(v1.x, v1.y + 1, v1.z);
                                            this.job.jobWorld.setBlockState(blockPos2_1, Blocks.SAPLING.getDefaultState(), 3);
                                            if (ModSimLoader.gamemode != 1) {
                                                ModSimLoader.money = (float) ((double) ModSimLoader.money - 0.002D);
                                            }
                                            int r = new Random().nextInt(10);
                                            BlockPos blockPos2_2 = new BlockPos(v1.x + 1, v1.y + 1, v1.z);
                                            if (r == 2) {
                                                this.job.jobWorld.setBlockState(blockPos2_2, Blocks.RED_FLOWER.getDefaultState(), 3);
                                            } else if (r == 5) {
                                                this.job.jobWorld.setBlockState(blockPos2_2, Blocks.YELLOW_FLOWER.getDefaultState(), 3);
                                            }
                                        }

                                }
                            }
                            //}
                        }
                        break;
                    case "3":
                        if (this.stage == 0) {
                            //除草
                            blockIDs = new CopyOnWriteArrayList();
                            //草
                            blockIDs.add(Blocks.TALLGRASS);
                            //花
                            blockIDs.add(Blocks.RED_FLOWER);
                            blockIDs.add(Blocks.YELLOW_FLOWER);
                            this.closestBlocks = null;
                            V3 v3 = new V3(constructorPos.getX(), constructorPos.getY() + 1, constructorPos.getZ());
                            this.setClosestBlocksOfType(v3.toBlockPos(), blockIDs, 30, false, true, false);
                            this.totalBlockCount = this.closestBlocks.size();
                            this.stage = 1;
                        }
                        if (this.totalBlockCount == 0) {
                            //没有什么要地球化的！
                            this.folk.status = new TextComponentTranslation("container.sim.job.terra.farmer.Nothing",new Object[0]).getUnformattedText();
                            //这里没有任何东西能以这种方式被规划
                            ModSimLoader.sendChat(new TextComponentTranslation("container.sim.job.terra.farmer.terraformed",new Object[0]).getUnformattedText());
                            //解雇
                            this.folk.fire();
                            this.folk.stayPut = false;
                            if (this.conBox != null) {
                                this.conBox.folk = null;
                            }
                            this.completed = true;
                            return;
                        } else {
                            //开始地形规划
                            this.folk.status = new TextComponentTranslation("container.sim.job.terra.farmer.process",new Object[0]).getUnformattedText();

                            //for (int i = 0; i < this.closestBlocks.size(); i++) {
                            //计算规划的百分比
                            Double x3 = (double) this.totalBlockCount;
                            Double y3 = (double) this.closestBlocks.size();
                            Double percent3 = (x3 - y3) / x3;
                            percent3 = percent3 * 100;
                            //环境改造 10% 完成
                            this.folk.status = new TextComponentTranslation("container.sim.job.terra.farmer.Terraforming",new Object[0]).getUnformattedText() + "- " + percent3 + " % " + new TextComponentTranslation("container.sim.job.terra.farmer.complete",new Object[0]).getUnformattedText();

                            //当前时间
                            Long now = System.currentTimeMillis();
                            V3 v3 = (V3) this.closestBlocks.get(0);

                            //是否是创造模式
                            if (ModSimLoader.gamemode != 1) {

                                if (this.placeDelayElapsed(now)) {
                                    now = System.currentTimeMillis();
                                    //上次时间为当前时间
                                    this.timeSinceLastBlockPlace = now;
                                    List minedStacks = this.translateBlockWhenMined(this.job.jobWorld, v3);
                                    if (minedStacks != null) {
                                        for (int s = 0; s < minedStacks.size(); ++s) {
                                            ItemStack stack = (ItemStack) minedStacks.get(s);
                                            if (stack != null) {
                                                this.job.placeInJobChest(stack);
//                                this.inventoriesPut(this.constructorChests, stack, false);
                                            }
                                        }
                                        BlockPos blockPos3_1 = new BlockPos(v3.x, v3.y, v3.z);
                                        this.job.jobWorld.setBlockState(blockPos3_1, Blocks.AIR.getDefaultState(), 3);
                                        if (ModSimLoader.gamemode != 1) {
                                            ModSimLoader.money = (float) ((double) ModSimLoader.money - 0.002D);
                                        }
                                    }
                                } else {
                                    return;
                                }
                            } else {
                                    now = System.currentTimeMillis();
                                    ////上次时间为当前时间
                                    this.timeSinceLastBlockPlace = now;
                                    List minedStacks = this.translateBlockWhenMined(this.job.jobWorld, v3);
                                    if (minedStacks != null) {
                                        for (int s = 0; s < minedStacks.size(); ++s) {
                                            ItemStack stack = (ItemStack) minedStacks.get(s);
                                            if (stack != null) {
                                                this.job.placeInJobChest(stack);
//                                this.inventoriesPut(this.constructorChests, stack, false);
                                            }
                                        }
                                        BlockPos blockPos3_1 = new BlockPos(v3.x, v3.y, v3.z);
                                        this.job.jobWorld.setBlockState(blockPos3_1, Blocks.AIR.getDefaultState(), 3);
                                        if (ModSimLoader.gamemode != 1) {
                                            ModSimLoader.money = (float) ((double) ModSimLoader.money - 0.002D);
                                        }
                                    }

                            }
                            //}
                        }
                        break;
                    case "4":
                        if (this.stage == 0) {
                            //平整化 铺平
                            blockIDs = new CopyOnWriteArrayList();
                            //草地
                            /*blockIDs.add(Blocks.GRASS);
                            //泥土
                            blockIDs.add(Blocks.DIRT);
                            //草
                            blockIDs.add(Blocks.TALLGRASS);
                            //石头
                            blockIDs.add(Blocks.STONE);
                            //沙子
                            blockIDs.add(Blocks.SAND);
                            //圆石
                            blockIDs.add(Blocks.SANDSTONE);
                            //砂砾
                            blockIDs.add(Blocks.GRAVEL);*/
                            //空气
                            blockIDs.add(Blocks.AIR);
                            //基岩
                            blockIDs.add(Blocks.BEDROCK);
                            //建筑箱
                            blockIDs.add(BlockLoader.blockConstructorBox);
                            //箱子
                            blockIDs.add(Blocks.CHEST);
                            this.closestBlocks = null;
                            this.getBlocksNoOfType(constructorPos, blockIDs, 30, false, false, false);
                            this.totalBlockCount = this.closestBlocks.size();
                            this.stage = 1;
                        }
                        if (this.totalBlockCount == 0) {
                            //没有什么要地球化的！
                            this.folk.status = new TextComponentTranslation("container.sim.job.terra.farmer.Nothing",new Object[0]).getUnformattedText();
                            //这里没有任何东西能以这种方式被规划
                            ModSimLoader.sendChat(new TextComponentTranslation("container.sim.job.terra.farmer.terraformed",new Object[0]).getUnformattedText());
                            //解雇
                            this.folk.fire();
                            this.folk.stayPut = false;
                            if (this.conBox != null) {
                                this.conBox.folk = null;
                            }
                            this.completed = true;
                            return;
                        } else {
                            //开始地形规划
                            this.folk.status = new TextComponentTranslation("container.sim.job.terra.farmer.process",new Object[0]).getUnformattedText();

                            //for (int i = 0; i < simThread.getClosestBlocks().size(); i++) {
                            //计算规划的百分比
                            Double x4 = (double) this.totalBlockCount;
                            Double y4 = (double) this.closestBlocks.size();
                            Double percent4 = (x4 - y4) / x4;
                            percent4 = percent4 * 100;
                            //环境改造 10% 完成
                            this.folk.status = new TextComponentTranslation("container.sim.job.terra.farmer.Terraforming",new Object[0]).getUnformattedText() + "- " + percent4 + " % " + new TextComponentTranslation("container.sim.job.terra.farmer.complete",new Object[0]).getUnformattedText();

                            V3 v4 = (V3) this.closestBlocks.get(0);
//当前时间
                            Long now = System.currentTimeMillis();
                            if (ModSimLoader.gamemode != 1) {
                                if (this.placeDelayElapsed(now)) {
                                    now = System.currentTimeMillis();
                                    //上次时间为当前时间
                                    this.timeSinceLastBlockPlace = now;
                                    List minedStacks1 = this.translateBlockWhenMined(this.job.jobWorld, v4);
                                    for (int s = 0; s < minedStacks1.size(); ++s) {
                                        ItemStack stack = (ItemStack) minedStacks1.get(s);
                                        if (stack != null) {
                                            this.job.placeInJobChest(stack);
//                                this.inventoriesPut(this.constructorChests, stack, false);
                                        }
                                    }
                                    BlockPos blockPos3_1 = new BlockPos(v4.x, v4.y, v4.z);
                                    this.job.jobWorld.setBlockState(blockPos3_1, Blocks.AIR.getDefaultState(), 3);
                                    if (ModSimLoader.gamemode != 1) {
                                        ModSimLoader.money = (float) ((double) ModSimLoader.money - 0.002D);
                                    }
                                }else{
                                          return;
                                        }
                            } else {
                                    now = System.currentTimeMillis();
                                    //上次时间为当前时间
                                    this.timeSinceLastBlockPlace = now;
                                    List minedStacks1 = this.translateBlockWhenMined(this.job.jobWorld, v4);
                                    for (int s = 0; s < minedStacks1.size(); ++s) {
                                        ItemStack stack = (ItemStack) minedStacks1.get(s);
                                        if (stack != null) {
                                            this.job.placeInJobChest(stack);
//                                this.inventoriesPut(this.constructorChests, stack, false);
                                        }
                                    }
                                    BlockPos blockPos3_1 = new BlockPos(v4.x, v4.y, v4.z);
                                    this.job.jobWorld.setBlockState(blockPos3_1, Blocks.AIR.getDefaultState(), 3);


                            }
                            //}


                        }
                        break;
                    case "5":
                        if (this.stage == 0) {
                            //单层泥土
                            blockIDs = new CopyOnWriteArrayList();
                            //空气
                            blockIDs.add(Blocks.AIR);
                            //高甘草
                            blockIDs.add(Blocks.TALLGRASS);
                            //红花
                            blockIDs.add(Blocks.RED_FLOWER);
                            //黄花
                            blockIDs.add(Blocks.YELLOW_FLOWER);
                            V3 v5 = new V3(constructorPos.getX(), constructorPos.getY() - 1, constructorPos.getZ());
                            this.closestBlocks = null;
                            this.setClosestBlocksOfType(v5.toBlockPos(), blockIDs, 30, false, true, true);
                            this.totalBlockCount = this.closestBlocks.size();
                            this.stage = 1;
                        }
                        if (this.totalBlockCount == 0) {
                            //没有什么要地球化的！
                            this.folk.status = new TextComponentTranslation("container.sim.job.terra.farmer.Nothing",new Object[0]).getUnformattedText();
                            //这里没有任何东西能以这种方式被规划
                            ModSimLoader.sendChat(new TextComponentTranslation("container.sim.job.terra.farmer.terraformed",new Object[0]).getUnformattedText());
                            //解雇
                            this.folk.fire();
                            this.folk.stayPut = false;
                            if (this.conBox != null) {
                                this.conBox.folk = null;
                            }
                            this.completed = true;
                            return;
                        } else {
//开始地形规划
                            this.folk.status = new TextComponentTranslation("container.sim.job.terra.farmer.process",new Object[0]).getUnformattedText();

                            //for (int i = 0; i < this.closestBlocks.size(); i++) {
                            //计算规划的百分比
                            Double x5 = (double) this.totalBlockCount;
                            Double y5 = (double) this.closestBlocks.size();
                            Double percent5 = (x5 - y5) / x5;
                            percent5 = percent5 * 100;
                            //环境改造 10% 完成
                            this.folk.status = new TextComponentTranslation("container.sim.job.terra.farmer.Terraforming",new Object[0]).getUnformattedText() + "- " + percent5 + " % " + new TextComponentTranslation("container.sim.job.terra.farmer.complete",new Object[0]).getUnformattedText();

                            V3 v5_1 = (V3) this.closestBlocks.get(0);
                            //当前时间
                            Long now = System.currentTimeMillis();
                            if (ModSimLoader.gamemode != 1) {
                                if (this.placeDelayElapsed(now)) {
                                    now = System.currentTimeMillis();
                                    //上次时间为当前时间
                                    this.timeSinceLastBlockPlace = now;
                                    //获取周围箱子
                                    List<IInventory> inventoriesFindClosest = this.job.findJobChests(5);
                                    //循环箱子
                                    for (IInventory inv : inventoriesFindClosest) {
                                        //循环箱子库存
                                        for (int ij = 0; ij < inv.getSizeInventory(); ij++) {
                                            ItemStack itemStack = inv.getStackInSlot(ij);
                                            //拿走当前需要的块 泥土
                                            if (itemStack != null /*&& itemStack.getItem() == Item.getItemFromBlock(Blocks.DIRT)*/) {
                                                inv.decrStackSize(ij, 1);
                                                fsMissBlock = false;
                                                break;
                                            }
                                        }

                                    }
                                    if (fsMissBlock) {
                                        //我需要更多的泥土！
                                        this.folk.status = new TextComponentTranslation("container.sim.job.terra.farmer.dirt",new Object[0]).getUnformattedText();
                                        this.missingBlock = new ItemStack(Blocks.DIRT);
                                        //this.completed = true;
                                        return;
                                    }
                                    BlockPos blockPos5_1 = new BlockPos(v5_1.x, v5_1.y, v5_1.z);
                                    this.job.jobWorld.setBlockState(blockPos5_1, Blocks.DIRT.getDefaultState(), 3);
                                    if (ModSimLoader.gamemode != 1) {
                                        ModSimLoader.money = (float) ((double) ModSimLoader.money - 0.002D);
                                    }
                                } else {
                                    return;
                                }
                            } else {
                                    now = System.currentTimeMillis();
                                    //上次时间为当前时间
                                    this.timeSinceLastBlockPlace = now;
                                    BlockPos blockPos5_1 = new BlockPos(v5_1.x, v5_1.y, v5_1.z);
                                    this.job.jobWorld.setBlockState(blockPos5_1, Blocks.DIRT.getDefaultState(), 3);
                                    if (ModSimLoader.gamemode != 1) {
                                        ModSimLoader.money = (float) ((double) ModSimLoader.money - 0.002D);
                                    }

                            }
                            //}
                        }
                        break;
                    case "6":
                        if (this.stage == 0) {
                            //冰川
                            blockIDs = new CopyOnWriteArrayList();
                            //空气
                            //blockIDs.add(Blocks.AIR);
                            //高甘草
                            //blockIDs.add(Blocks.TALLGRASS);
                            //水
                            blockIDs.add(Blocks.WATER);
                            //流动的水
                            blockIDs.add(Blocks.FLOWING_WATER);
//                    v = new V3(this.theFolk.employedAt.xCoord, this.theFolk.employedAt.yCoord, this.theFolk.employedAt.zCoord, this.theFolk.employedAt.theDimension);
                            this.closestBlocks = null;
                            this.setClosestBlocksOfType(constructorPos, blockIDs, 30, true, true, false);
                            this.totalBlockCount = this.closestBlocks.size();
                            this.stage = 1;
                        }
                        if (this.totalBlockCount == 0) {
                            //没有什么要地球化的！
                            this.folk.status = new TextComponentTranslation("container.sim.job.terra.farmer.Nothing",new Object[0]).getUnformattedText();
                            //这里没有任何东西能以这种方式被规划
                            ModSimLoader.sendChat(new TextComponentTranslation("container.sim.job.terra.farmer.terraformed",new Object[0]).getUnformattedText());
                            //解雇
                            this.folk.fire();
                            this.folk.stayPut = false;
                            if (this.conBox != null) {
                                this.conBox.folk = null;
                            }
                            this.completed = true;
                            return;
                        } else {
                            //开始地形规划
                            this.folk.status = new TextComponentTranslation("container.sim.job.terra.farmer.process",new Object[0]).getUnformattedText();


                            //for (int i = 0; i < this.closestBlocks.size(); i++) {
                            //计算规划的百分比
                            Double x6 = (double) this.totalBlockCount;
                            Double y6 = (double) this.closestBlocks.size();
                            Double percent6 = (x6 - y6) / x6;
                            percent6 = percent6 * 100;
                            //环境改造 10% 完成
                            this.folk.status = new TextComponentTranslation("container.sim.job.terra.farmer.Terraforming",new Object[0]).getUnformattedText() + "- " + percent6 + " % " + new TextComponentTranslation("container.sim.job.terra.farmer.complete",new Object[0]).getUnformattedText();
                            V3 v6 = (V3) this.closestBlocks.get(0);
                            Block blockId = this.job.jobWorld.getBlockState(new BlockPos(v6.x, v6.y, v6.z)).getBlock();
                            //当前时间
                            Long now = System.currentTimeMillis();
                            if (ModSimLoader.gamemode != 1) {
                                if (this.placeDelayElapsed(now)) {
                                    now = System.currentTimeMillis();
                                    //上次时间为当前时间
                                    this.timeSinceLastBlockPlace = now;
//方块不为空 不是草
                                    if (blockId != null && blockId != Blocks.AIR && blockId != Blocks.TALLGRASS) {
                                        if (blockId == Blocks.WATER || blockId == Blocks.FLOWING_WATER) {
                                            BlockPos blockPos6_1 = new BlockPos(v6.x, v6.y, v6.z);
                                            this.job.jobWorld.setBlockState(blockPos6_1, Blocks.ICE.getDefaultState(), 3);
                                            if (ModSimLoader.gamemode != 1) {
                                                ModSimLoader.money = (float) ((double) ModSimLoader.money - 0.002D);
                                            }
//                                this.job.placeInJobChest(new ItemStack(Items.BUCKET, 1));
                                        }
                                    } else {
                                        Block idBelow = this.job.jobWorld.getBlockState(new BlockPos(v6.x, v6.y - 1, v6.z)).getBlock();
                                        //方块不是空 不是冰 不是水 不是雪
                                        if (idBelow != null && idBelow != Blocks.AIR && idBelow != Blocks.ICE && idBelow != Blocks.WATER && idBelow != Blocks.FLOWING_WATER && idBelow != Blocks.SNOW && !this.job.jobWorld.isRemote) {
                                            BlockPos blockPos6_1 = new BlockPos(v6.x, v6.y, v6.z);
                                            //雪
                                            this.job.jobWorld.setBlockState(blockPos6_1, Blocks.SNOW.getDefaultState(), 3);
                                            if (ModSimLoader.gamemode != 1) {
                                                ModSimLoader.money = (float) ((double) ModSimLoader.money - 0.002D);
                                            }
                                        }
                                    }
                                } else {
                                    return;
                                }
                            } else {
                                    now = System.currentTimeMillis();
                                    //上次时间为当前时间
                                    this.timeSinceLastBlockPlace = now;
//方块不为空 不是草
                                    if (blockId != null && blockId != Blocks.AIR && blockId != Blocks.TALLGRASS) {
                                        if (blockId == Blocks.WATER || blockId == Blocks.FLOWING_WATER) {
                                            BlockPos blockPos6_1 = new BlockPos(v6.x, v6.y, v6.z);
                                            this.job.jobWorld.setBlockState(blockPos6_1, Blocks.ICE.getDefaultState(), 3);
                                            if (ModSimLoader.gamemode != 1) {
                                                ModSimLoader.money = (float) ((double) ModSimLoader.money - 0.002D);
                                            }
//                                this.job.placeInJobChest(new ItemStack(Items.BUCKET, 1));
                                        }
                                    } else {
                                        Block idBelow = this.job.jobWorld.getBlockState(new BlockPos(v6.x, v6.y - 1, v6.z)).getBlock();
                                        //方块不是空 不是冰 不是水 不是雪
                                        if (idBelow != null && idBelow != Blocks.AIR && idBelow != Blocks.ICE && idBelow != Blocks.WATER && idBelow != Blocks.FLOWING_WATER && idBelow != Blocks.SNOW && !this.job.jobWorld.isRemote) {
                                            BlockPos blockPos6_1 = new BlockPos(v6.x, v6.y, v6.z);
                                            //雪
                                            this.job.jobWorld.setBlockState(blockPos6_1, Blocks.SNOW.getDefaultState(), 3);
                                            if (ModSimLoader.gamemode != 1) {
                                                ModSimLoader.money = (float) ((double) ModSimLoader.money - 0.002D);
                                            }
                                        }
                                    }

                            }
                            //}

                        }
                        break;
                    case "7":
                        if (this.stage == 0) {
                            //湿润
                            blockIDs = new CopyOnWriteArrayList();
                            //熔岩
                            blockIDs.add(Blocks.LAVA);
                            blockIDs.add(Blocks.FLOWING_LAVA);
//                    v = new V3(this.theFolk.employedAt.xCoord, this.theFolk.employedAt.yCoord, this.theFolk.employedAt.zCoord, this.theFolk.employedAt.theDimension);
                            this.closestBlocks = null;
                            this.setClosestBlocksOfType(constructorPos, blockIDs, 30, false, false, false);
                            this.totalBlockCount = this.closestBlocks.size();
                            this.stage = 1;
                        }
                        if (this.totalBlockCount == 0) {
                            //没有什么要地球化的！
                            this.folk.status = new TextComponentTranslation("container.sim.job.terra.farmer.Nothing",new Object[0]).getUnformattedText();
                            //这里没有任何东西能以这种方式被规划
                            ModSimLoader.sendChat(new TextComponentTranslation("container.sim.job.terra.farmer.terraformed",new Object[0]).getUnformattedText());
                            //解雇
                            this.folk.fire();
                            this.folk.stayPut = false;
                            if (this.conBox != null) {
                                this.conBox.folk = null;
                            }
                            this.completed = true;
                            return;
                        } else {
                            //开始地形规划
                            this.folk.status = new TextComponentTranslation("container.sim.job.terra.farmer.process",new Object[0]).getUnformattedText();

                            //for (int i = 0; i < this.closestBlocks.size(); i++) {
                            //计算规划的百分比
                            Double x7 = (double) this.totalBlockCount;
                            Double y7 = (double) this.closestBlocks.size();
                            Double percent7 = (x7 - y7) / x7;
                            percent7 = percent7 * 100;
                            //环境改造 10% 完成
                            this.folk.status = new TextComponentTranslation("container.sim.job.terra.farmer.Terraforming",new Object[0]).getUnformattedText() + "- " + percent7 + " % " + new TextComponentTranslation("container.sim.job.terra.farmer.complete",new Object[0]).getUnformattedText();
                            V3 v7 = (V3) this.closestBlocks.get(0);
                            // P15(S1): 扫描到的是熔岩本身，湿润应把熔岩变成黑曜石；
                            // 旧代码写到了 y-1，熔岩还在，白吞一个水桶。
                            BlockPos blockPos = new BlockPos(v7.x, v7.y, v7.z);
                            //当前时间
                            Long now = System.currentTimeMillis();
                            if (ModSimLoader.gamemode != 1) {
                                if (this.placeDelayElapsed(now)) {
                                    now = System.currentTimeMillis();
                                    //上次时间为当前时间
                                    this.timeSinceLastBlockPlace = now;
//获取周围箱子
                                    List<IInventory> inventoriesFindClosest = this.job.findJobChests(5);
                                    //循环箱子
                                    for (IInventory inv : inventoriesFindClosest) {
                                        //循环箱子库存
                                        for (int ij = 0; ij < inv.getSizeInventory(); ij++) {
                                            ItemStack itemStack = inv.getStackInSlot(ij);
                                            //拿走当前需要的块 泥土
                                            if (itemStack != null && itemStack.getItem() == Items.WATER_BUCKET) {
                                                inv.decrStackSize(ij, 1);
                                                fsMissBlock = false;
                                                break;
                                            }
                                        }
                                    }
                                    if (fsMissBlock) {
                                        //我需要更多的泥土！
                                        this.folk.status = new TextComponentTranslation("container.sim.job.terra.farmer.water",new Object[0]).getUnformattedText();
                                        this.missingBlock = new ItemStack(Items.WATER_BUCKET);
                                        //this.completed = true;
                                        return;
                                    }
                                    //黑曜石
                                    this.job.jobWorld.setBlockState(blockPos, Blocks.OBSIDIAN.getDefaultState(), 3);
//                        this.job.jobWorld.markBlockForUpdate(blockPos);
                                    if (ModSimLoader.gamemode != 1) {
                                        ModSimLoader.money = (float) ((double) ModSimLoader.money - 0.002D);
                                        // P15(S1): 用完水桶把空桶还回工作箱，不能凭空吞掉。
                                        this.job.placeInJobChest(new ItemStack(Items.BUCKET, 1));
                                    }
                                } else {
                                    return;
                                }
                            } else {
                                    now = System.currentTimeMillis();
                                    //上次时间为当前时间
                                    this.timeSinceLastBlockPlace = now;
//黑曜石
                                    this.job.jobWorld.setBlockState(blockPos, Blocks.OBSIDIAN.getDefaultState(), 3);
//                        this.job.jobWorld.markBlockForUpdate(blockPos);
                                    if (ModSimLoader.gamemode != 1) {
                                        ModSimLoader.money = (float) ((double) ModSimLoader.money - 0.002D);
                                    }

                            }
                            //}

                        }
                        break;
                    case "8":
                        if (this.stage == 0) {
                            //炎热
                            blockIDs = new CopyOnWriteArrayList();
                            blockIDs.add(Blocks.LAVA);
                            blockIDs.add(Blocks.FLOWING_LAVA);
//                    v = new V3(this.theFolk.employedAt.xCoord, this.theFolk.employedAt.yCoord, this.theFolk.employedAt.zCoord, this.theFolk.employedAt.theDimension);
                            this.closestBlocks = null;
                            this.setClosestBlocksOfType(constructorPos, blockIDs, 30, false, true, false);
                            this.totalBlockCount = this.closestBlocks.size();
                            this.stage = 1;
                        }
                        if (this.totalBlockCount == 0) {
                            //没有什么要地球化的！
                            this.folk.status = new TextComponentTranslation("container.sim.job.terra.farmer.Nothing",new Object[0]).getUnformattedText();
                            //这里没有任何东西能以这种方式被规划
                            ModSimLoader.sendChat(new TextComponentTranslation("container.sim.job.terra.farmer.terraformed",new Object[0]).getUnformattedText());
                            //解雇
                            this.folk.fire();
                            this.folk.stayPut = false;
                            if (this.conBox != null) {
                                this.conBox.folk = null;
                            }
                            this.completed = true;
                            return;
                        } else {
                            //开始地形规划
                            this.folk.status = new TextComponentTranslation("container.sim.job.terra.farmer.process",new Object[0]).getUnformattedText();
                            //for (int i = 0; i < this.closestBlocks.size(); i++) {
                            //计算规划的百分比
                            Double x8 = (double) this.totalBlockCount;
                            Double y8 = (double) this.closestBlocks.size();
                            Double percent8 = (x8 - y8) / x8;
                            percent8 = percent8 * 100;
                            //环境改造 10% 完成
                            this.folk.status = new TextComponentTranslation("container.sim.job.terra.farmer.Terraforming",new Object[0]).getUnformattedText() + "- " + percent8 + " % " + new TextComponentTranslation("container.sim.job.terra.farmer.complete",new Object[0]).getUnformattedText();
                            V3 v8 = (V3) this.closestBlocks.get(0);
                            BlockPos blockPos8_1 = new BlockPos(v8.x, v8.y, v8.z);
                            //当前时间
                            Long now = System.currentTimeMillis();
                            if (ModSimLoader.gamemode != 1) {
                                if (this.placeDelayElapsed(now)) {
                                    now = System.currentTimeMillis();
                                    //上次时间为当前时间
                                    this.timeSinceLastBlockPlace = now;
//获取周围箱子
                                    List<IInventory> inventoriesFindClosest = this.job.findJobChests(5);
                                    //循环箱子
                                    for (IInventory inv : inventoriesFindClosest) {
                                        //循环箱子库存
                                        for (int ij = 0; ij < inv.getSizeInventory(); ij++) {
                                            ItemStack itemStack = inv.getStackInSlot(ij);
                                            //拿走当前需要的块 泥土
                                            if (itemStack != null && itemStack.getItem() == Items.BUCKET) {
                                                inv.decrStackSize(ij, 1);
                                                fsMissBlock = false;
                                                break;
                                            }
                                        }
                                    }
                                    if (fsMissBlock) {
                                        //我需要一些空桶来装熔岩。
                                        this.folk.status = new TextComponentTranslation("container.sim.job.terra.farmer.buckets",new Object[0]).getUnformattedText();
                                        this.missingBlock = new ItemStack(Items.BUCKET);
                                        //this.completed = true;
                                        return;
                                    }
                                    ModSimLoader.money = (float) ((double) ModSimLoader.money - 0.002D);
                                    this.job.jobWorld.setBlockState(blockPos8_1, Blocks.AIR.getDefaultState(), 3);
                                    this.job.placeInJobChest(new ItemStack(Items.LAVA_BUCKET, 1));
                                } else {
                                    return;
                                }
                            } else {
                                    now = System.currentTimeMillis();
                                    //上次时间为当前时间
                                    this.timeSinceLastBlockPlace = now;
                                    this.job.jobWorld.setBlockState(blockPos8_1, Blocks.AIR.getDefaultState(), 3);
                                    this.job.placeInJobChest(new ItemStack(Items.LAVA_BUCKET, 1));

                            }
                            //}

                        }
                        break;
                    case "9":
                        if (this.stage == 0) {
                            //除雪
                            blockIDs = new CopyOnWriteArrayList();
                            blockIDs.add(Blocks.SNOW);
//                    v = new V3(this.theFolk.employedAt.xCoord, this.theFolk.employedAt.yCoord, this.theFolk.employedAt.zCoord, this.theFolk.employedAt.theDimension);
                            this.closestBlocks = null;
                            this.setClosestBlocksOfType(constructorPos, blockIDs, 30, false, true, false);
                            this.totalBlockCount = this.closestBlocks.size();
                            this.stage = 1;
                        }
                        if (this.totalBlockCount == 0) {
                            //没有什么要地球化的！
                            this.folk.status = new TextComponentTranslation("container.sim.job.terra.farmer.Nothing",new Object[0]).getUnformattedText();
                            //这里没有任何东西能以这种方式被规划
                            ModSimLoader.sendChat(new TextComponentTranslation("container.sim.job.terra.farmer.terraformed",new Object[0]).getUnformattedText());
                            //解雇
                            this.folk.fire();
                            this.folk.stayPut = false;
                            if (this.conBox != null) {
                                this.conBox.folk = null;
                            }
                            this.completed = true;
                            return;
                        } else {
                            //开始地形规划
                            this.folk.status = new TextComponentTranslation("container.sim.job.terra.farmer.process",new Object[0]).getUnformattedText();

                            //for (int i = 0; i < this.closestBlocks.size(); i++) {
                            //计算规划的百分比
                            Double x9 = (double) this.totalBlockCount;
                            Double y9 = (double) this.closestBlocks.size();
                            Double percent9 = (x9 - y9) / x9;
                            percent9 = percent9 * 100;
                            //环境改造 10% 完成
                            this.folk.status = new TextComponentTranslation("container.sim.job.terra.farmer.Terraforming",new Object[0]).getUnformattedText() + "- " + percent9 + " % " + new TextComponentTranslation("container.sim.job.terra.farmer.complete",new Object[0]).getUnformattedText();

                            //当前时间
                            Long now = System.currentTimeMillis();
                            V3 v9 = (V3) this.closestBlocks.get(0);
                            BlockPos blockPo9_1 = new BlockPos(v9.x, v9.y, v9.z);
                            if (ModSimLoader.gamemode != 1) {
                                if (this.placeDelayElapsed(now)) {
                                    now = System.currentTimeMillis();
                                    //上次时间为当前时间
                                    this.timeSinceLastBlockPlace = now;
                                    ModSimLoader.money = (float) ((double) ModSimLoader.money - 0.002D);
                                    this.job.jobWorld.setBlockState(blockPo9_1, Blocks.GRASS.getDefaultState(), 3);
                                    int counter1 = 0;
                                    ++counter1;
                                    if (counter1 % 4 == 0) {
                                        this.job.placeInJobChest(new ItemStack(Blocks.SNOW, 1));
                                    }
                                } else {
                                    return;
                                }
                            } else {
                                if (this.placeDelayElapsed(now)) {
                                    now = System.currentTimeMillis();
                                    //上次时间为当前时间
                                    this.timeSinceLastBlockPlace = now;
                                    this.job.jobWorld.setBlockState(blockPo9_1, Blocks.GRASS.getDefaultState(), 3);
                                    int counter1 = 0;
                                    ++counter1;
                                    if (counter1 % 4 == 0) {
                                        this.job.placeInJobChest(new ItemStack(Blocks.SNOW, 1));
                                    }
                                } else {
                                    return;
                                }
                            }
                            //}

                        }
                        break;
                }
                this.closestBlocks.remove(0);
                int b4 = (int) Math.floor(this.folk.skillBuilding);
                //建筑等级
                if (this.folk.skillBuilding < 10.0F) {
                    NpcData var10000 = this.folk;
                    var10000.skillBuilding = (float) (var10000.skillBuilding + 0.001D / b4);
                }

                int aft = (int) Math.floor(this.folk.skillBuilding);
                if (b4 != aft) {
                    ModSimLoader.sendChat(this.folk.getName() + new TextComponentTranslation("container.sim.job.builder_constructor_levelled",new Object[0]).getUnformattedText() + aft);
                }
                if (this.closestBlocks.size() == 0) {
                    ModSimLoader.sendChat(this.folk.getName() + new TextComponentTranslation("container.sim.job.terra.farmer.has_completed",new Object[0]).getUnformattedText());
                    //播放 我准备好了
//                    SoundEvent soundEvent = new SoundEvent(new ResourceLocation(ModSim.MODID + ":cash"));
                    SoundEvent cash = SoundRegistry.CASH;
                    if (cash == null || cash.getRegistryName() == null) {
                        ModSimLoader.log.error("播放失败：sim:cash 声音事件未注册");
                    } else {
                    this.job.jobWorld.playSound(this.folk.entity.posX, this.folk.entity.posY, this.folk.entity.posZ, cash, SoundCategory.BLOCKS, 1.0F, 1.0F,false);}
                    this.folk.fire();
                    this.folk.stayPut = false;
                    if (this.conBox != null) {
                        this.conBox.folk = null;
                    }
                    this.completed = true;
                }
            }

        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("placeBlock出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    private boolean placeDelayElapsed(long now) {
        long interval = Math.max(200L, 1000L - (long) (80L * Math.min(10.0F, Math.max(0.0F, this.folk.skillBuilding))));
        return now - this.timeSinceLastBlockPlace > interval;
    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO
     * @Date 19:54 2022/11/27 起始位置，方块 距离限制，向上扫描，向下扫描，仅一层
     * @Param [constructorPos, blockIDs, distanceLimit, b, b1, oneLayerOnly]
     **/
    private void setClosestBlocksOfType(BlockPos constructorPos, List<Block> blockIDs, int distanceLimit, boolean needsToSeeSky, boolean scanDownwards, boolean oneLayerOnly) {
        try {
            World world = this.job.jobWorld;
            if (world == null || constructorPos == null) {
                return;
            }
            HashMap<String, V3> found = new HashMap<>();
            int radius = Math.max(1, distanceLimit - 1);
            int layers = oneLayerOnly ? 1 : Math.max(1, distanceLimit);
            // P15(S1): 一次遍历整个方形区域，不再按 j 重复扫描内圈（旧实现约多扫 n 倍）；
            // 同时跳过未加载区块，避免地形师把周围区块全部加载。
            for (int layer = 0; layer < layers; layer++) {
                int y = scanDownwards ? constructorPos.getY() - layer : constructorPos.getY() + layer;
                for (int xo = -radius; xo <= radius; xo++) {
                    for (int zo = -radius; zo <= radius; zo++) {
                        BlockPos pos = new BlockPos(constructorPos.getX() + xo, y, constructorPos.getZ() + zo);
                        if (!world.isBlockLoaded(pos)) {
                            continue;
                        }
                        Block block = world.getBlockState(pos).getBlock();
                        if (!blockIDs.contains(block)) {
                            continue;
                        }
                        if (needsToSeeSky && !world.isAirBlock(pos.up())) {
                            continue;
                        }
                        V3 v = new V3(pos.getX(), pos.getY(), pos.getZ());
                        found.put(v.toString(), v);
                    }
                }
            }
            this.closestBlocks = new CopyOnWriteArrayList<>(found.values());
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("setClosestBlocksOfType出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    /**
     * @return java.util.List<net.minecraft.item.ItemStack>
     * @Author fan
     * @Description //TODO 开采时平移方块
     * @Date 21:40 2022/11/29
     * @Param [world, location]
     **/
    public List<ItemStack> translateBlockWhenMined(World world, V3 location) {
        List<ItemStack> itemStacks = new CopyOnWriteArrayList<ItemStack>();
        try {
            int i = (int) location.x;
            int j = (int) location.y;
            int k = (int) location.z;
            BlockPos blockPos = new BlockPos(i, j, k);
            Block block = world.getBlockState(blockPos).getBlock();
            if (block == null) {
                return null;
            }

            int ma = block.getMetaFromState(world.getBlockState(blockPos));
            itemStacks = block.getDrops(world, blockPos, block.getStateFromMeta(ma), 0);
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("开采时平移块体出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
        return itemStacks;

    }

    private void getBlocksNoOfType(BlockPos constructorPos, List<Block> blockIDs, int distanceLimit, boolean needsToSeeSky, boolean scanDownwards, boolean oneLayerOnly) {
        try {
            World world = this.job.jobWorld;
            if (world == null || constructorPos == null) {
                return;
            }
            HashMap<String, V3> found = new HashMap<>();
            int radius = Math.max(1, distanceLimit - 1);
            int layers = oneLayerOnly ? 1 : Math.max(1, distanceLimit);
            // P15(S1): 与 setClosestBlocksOfType 相同的单次方形遍历；
            // 额外跳过 tile entity（箱子/机器等），避免平整地形时误删容器。
            for (int layer = 0; layer < layers; layer++) {
                int y = scanDownwards ? constructorPos.getY() - layer : constructorPos.getY() + layer;
                for (int xo = -radius; xo <= radius; xo++) {
                    for (int zo = -radius; zo <= radius; zo++) {
                        BlockPos pos = new BlockPos(constructorPos.getX() + xo, y, constructorPos.getZ() + zo);
                        if (!world.isBlockLoaded(pos)) {
                            continue;
                        }
                        Block block = world.getBlockState(pos).getBlock();
                        if (blockIDs.contains(block)) {
                            continue;
                        }
                        if (needsToSeeSky && !world.isAirBlock(pos.up())) {
                            continue;
                        }
                        if (world.getTileEntity(pos) != null) {
                            continue;
                        }
                        V3 v = new V3(pos.getX(), pos.getY(), pos.getZ());
                        found.put(v.toString(), v);
                    }
                }
            }
            this.closestBlocks = new CopyOnWriteArrayList<>(found.values());
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("getBlocksNoOfType出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

}
