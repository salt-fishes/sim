package com.trhsy.sim.npcCode.task;

import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.NpcData;
import com.trhsy.sim.npcCode.block.FarmBox;
import com.trhsy.sim.npcCode.enums.FarmType;
import com.trhsy.sim.npcCode.job.Job;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.IPlantable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.npc.task
 * @ClassName: JobTaskFarmer
 * @Description: 收获
 * @date 2023/08/07 上午 10:13
 */
public class JobTaskFarmerHarvest extends JobTask {
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

    public JobTaskFarmerHarvest(Job j, long ms, String status, FarmBox farm) {
        super(j, ms);
        this.status = status;
        this.noNeed = 0;
        this.farm=farm;
    }

    @Override
    public void onTaskBegin() {
        this.job.folk.setStatus(this.status);
        //设置固定不动
        this.folk.stayPut = true;
            this.harvest();
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
                boolean falg = false;
                if (ModSimLoader.gamemode == 1) {
                    falg =(System.currentTimeMillis() - this.harvestCheck) > 1000.0F - 10.0F * 9.9;
                }
                // 等级 时间计算 工作效率
                if (falg||(float) (System.currentTimeMillis() - this.harvestCheck) > 1000.0F - 100.0F * this.folk.skillFarming) {

                        this.harvest();

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
     * @Description //TODO 收获
     * @Date 10:44 2022/12/10
     * @Param []
     **/
    public void harvest() {

        try {

            if (this.farm != null) {
                this.folk.setStatus(new TextComponentTranslation("container.sim.job.crop.farmer.Harvesting",new Object[0]).getUnformattedText());
                //先获取农田箱的 类型
                FarmType farmType = this.farm.farmType;
                //胡萝卜
                if (farmType == FarmType.CARROT) {
                    harvest1();
                    //西瓜
                } else if (farmType == FarmType.MELON) {
                    //循环宽
                    for (int z = 0; z < this.farm.z; ++z) {
                        //循环长
                        for (int x = 0; x < this.farm.x; ++x) {
                            //声明作物
                            List<ItemStack> drops = new CopyOnWriteArrayList<ItemStack>();
                            //获取当前位置的物品
                            BlockPos bp = new BlockPos(this.farm.getCorner().offset(this.farm.facing, x).offset(this.farm.facing.rotateY(), z));
                            IBlockState iBlockState = this.folk.entity.world.getBlockState(bp);
                            //获得方块
                            Block b = iBlockState.getBlock();
                            //是西瓜
                            if (b == Blocks.MELON_BLOCK) {
                                //收获
                                this.folk.setStatus(new TextComponentTranslation("container.sim.job.crop.farmer.Harvesting",new Object[0]).getUnformattedText());
                                //获得方块
                                Block bCrop = this.folk.entity.world.getBlockState(bp).getBlock();
                                //摧毁方块
                                drops = bCrop.getDrops(this.folk.entity.world, bp, this.folk.entity.world.getBlockState(bp), 1);
//                                bCrop.getDrops(drops, this.folk.entity.world, bp.north(), this.folk.entity.world.getBlockState(bp.north()), 0);
                                drops.forEach((drop) -> {
                                    //放到工作箱
                                    this.job.placeInJobChest(drop);
                                });
                                //设置手持物
                                this.folk.entity.setActiveHand(EnumHand.MAIN_HAND);
                                if ((System.currentTimeMillis() - this.swingArmCheck) > 1000 * 3) {
                                    //摇摆手臂
                                    this.folk.entity.swingArm(EnumHand.MAIN_HAND);
                                }
                                this.folk.entity.world.destroyBlock(bp, false);
                                //设置为空
//                            this.folk.entity.world.setBlockToAir(bp);
                                //增加农民等级
                                this.addFarmingLevel();
                                //设置收获时间
                                this.harvestCheck = System.currentTimeMillis();
                                return;
                            }

                        }
                    }
                    this.completed = true;
//            harvest1();
                    //土豆
                } else if (farmType == FarmType.POTATO) {
                    harvest1();
                    //南瓜
                } else if (farmType == FarmType.PUMPKIN) {
//            harvest1();
                    //循环宽
                    for (int z = 0; z < this.farm.z; ++z) {
                        //循环长
                        for (int x = 0; x < this.farm.x; ++x) {
                            //声明作物
                            List<ItemStack> drops = new CopyOnWriteArrayList<ItemStack>();
                            //获取当前位置的物品
                            BlockPos bp = new BlockPos(this.farm.getCorner().offset(this.farm.facing, x).offset(this.farm.facing.rotateY(), z));
                            IBlockState iBlockState = this.folk.entity.world.getBlockState(bp);
                            //获得方块
                            Block b = iBlockState.getBlock();
                            //是南瓜
                            if (b == Blocks.PUMPKIN) {
                                //收获
                                this.folk.setStatus(new TextComponentTranslation("container.sim.job.crop.farmer.Harvesting",new Object[0]).getUnformattedText());
                                //获得方块
                                Block bCrop = this.folk.entity.world.getBlockState(bp).getBlock();
                                //摧毁方块
                                drops = bCrop.getDrops(this.folk.entity.world, bp, this.folk.entity.world.getBlockState(bp), 1);
                                drops.forEach((drop) -> {
                                    //放到工作箱
                                    this.job.placeInJobChest(drop);
                                });
                                //设置手持物
                                this.folk.entity.setActiveHand(EnumHand.MAIN_HAND);
                                if ((System.currentTimeMillis() - this.swingArmCheck) > 1000 * 3) {
                                    //摇摆手臂
                                    this.folk.entity.swingArm(EnumHand.MAIN_HAND);
                                }
                                this.folk.entity.world.destroyBlock(bp, false);
                                //设置为空
//                            this.folk.entity.world.setBlockToAir(bp);
                                //增加农民等级
                                this.addFarmingLevel();
                                //设置收获时间
                                this.harvestCheck = System.currentTimeMillis();
                                return;
                            }

                        }
                    }
                    this.completed = true;
                    //小麦
                } else if (farmType == FarmType.WHEAT) {
                    harvest1();
                    //甜菜根
                } else if (farmType == FarmType.BEETROOTS) {
                    harvest1();
                    //甘蔗
                } else if (farmType == FarmType.SUGAR) {
                    //循环农场的宽
                    harvest1();
                    //仙人掌
                } else if (farmType == FarmType.CACTUS) {
                    harvest1();
                    //未知的 可可豆
                } else if (farmType == FarmType.COCOA) {
                    for (int z = 0; z < this.farm.z; ++z) {
                        //循环长
                        for (int x = 0; x < this.farm.x; ++x) {
                            for (int y = 0; y < 2; ++y) {
                                //声明作物
                                List<ItemStack> drops = new CopyOnWriteArrayList<ItemStack>();
                                //获取当前位置的物品
                                BlockPos bp = new BlockPos(this.farm.getCorner().offset(this.farm.facing, x).offset(this.farm.facing.rotateY(), z));
                                bp = new BlockPos(bp.getX(), bp.getY() + y, bp.getZ());
                                IBlockState iBlockState = this.folk.entity.world.getBlockState(bp);
                                //获得方块
                                Block b = iBlockState.getBlock();
                                //是可可豆（P9: 成熟度在 meta 低 2 位，age==2 即成熟；
                                //原来写死 ==9 只匹配"朝北+未成熟"，永远收不到可可豆）
                                if (b == Blocks.COCOA && (b.getMetaFromState(iBlockState) & 3) == 2) {
                                    //收获
                                    this.folk.setStatus(new TextComponentTranslation("container.sim.job.crop.farmer.Harvesting",new Object[0]).getUnformattedText());
                                    //获得方块
                                    Block bCrop = this.folk.entity.world.getBlockState(bp).getBlock();
                                    //摧毁方块
                                    drops = bCrop.getDrops(this.folk.entity.world, bp, this.folk.entity.world.getBlockState(bp), 1);
                                    drops.forEach((drop) -> {
                                        //放到工作箱
                                        this.job.placeInJobChest(drop);
                                    });
                                    //设置手持物
                                    this.folk.entity.setActiveHand(EnumHand.MAIN_HAND);
                                    if ((System.currentTimeMillis() - this.swingArmCheck) > 1000 * 3) {
                                        //摇摆手臂
                                        this.folk.entity.swingArm(EnumHand.MAIN_HAND);
                                    }
                                    this.folk.entity.world.destroyBlock(bp, false);
                                    //设置为空
//                                this.folk.entity.world.setBlockToAir(bp);
                                    //增加农民等级
                                    this.addFarmingLevel();
                                    //设置收获时间
                                    this.harvestCheck = System.currentTimeMillis();
                                    return;
                                }
                            }
                        }
                    }
                    this.completed = true;
                    //未知的
                } else {
                    harvest1();
                }
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("JobFarmer-harvest出错了:" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    public void harvest1() {
        try {
            //循环宽
            for (int z = 0; z < this.farm.z; ++z) {
                //循环长
                for (int x = 0; x < this.farm.x; ++x) {
                    //声明作物
                    List<ItemStack> drops = new CopyOnWriteArrayList<ItemStack>();
                    //获取当前位置的物品
                    BlockPos bp = new BlockPos(this.farm.getCorner().offset(this.farm.facing, x).offset(this.farm.facing.rotateY(), z));
                    IBlockState iBlockState = this.folk.entity.world.getBlockState(bp);
                    //获得方块
                    Block b = iBlockState.getBlock();
                    //有根茎 不是作物 不是可种植 不是可生长
                    if (!(b instanceof BlockCrops) && !(b instanceof IPlantable) && !(b instanceof IGrowable)) {
                        //包含根茎
                        if (!(b instanceof BlockPumpkin) && !(b instanceof BlockMelon) && !(b instanceof BlockCocoa) && !(b instanceof BlockCactus) && b != Blocks.AIR &&b instanceof BlockCrops) {
                            //
                            BlockCrops crop = (BlockCrops) b;
                            if (crop.isMaxAge(iBlockState)) {
                                this.folk.setStatus(new TextComponentTranslation("container.sim.job.crop.farmer.Harvesting",new Object[0]).getUnformattedText());
                                //获得方块
                                Block bCrop = this.folk.entity.world.getBlockState(bp).getBlock();
                                //摧毁方块
                                drops = bCrop.getDrops(this.folk.entity.world, bp, this.folk.entity.world.getBlockState(bp), 1);
//                                bCrop.getDrops(drops, this.folk.entity.world, bp.north(), this.folk.entity.world.getBlockState(bp.north()), 0);
                                drops.forEach((drop) -> {
                                    //放到工作箱
                                    this.job.placeInJobChest(drop);
                                });
                                this.folk.entity.setActiveHand(EnumHand.MAIN_HAND);
                                this.folk.entity.swingArm(EnumHand.MAIN_HAND);
                                this.folk.entity.world.destroyBlock(bp, false);
//                            this.folk.entity.world.setBlockToAir(bp);
                                this.addFarmingLevel();
                                this.harvestCheck = System.currentTimeMillis();
                                return;
                            }
                        }
                        //甘蔗
                    } else if (b == Blocks.REEDS) {
                        if (this.folk.entity.world.getBlockState(bp.up()).getBlock() == b) {
                            //收获
                            this.folk.setStatus(new TextComponentTranslation("container.sim.job.crop.farmer.Harvesting",new Object[0]).getUnformattedText());
                            // 1. 定位当前列中最高的甘蔗方块
                            BlockPos currentPos = bp.up(); // 初始位置（当前方块上方）
                            BlockPos highestReedPos = currentPos; // 最高甘蔗位置，默认初始位置
                            // 向上遍历，找到最高的甘蔗
                            while (true) {
                                BlockPos nextPos = highestReedPos.up(); // 检查上一格
                                // 如果上一格还是甘蔗，更新最高位置
                                if (this.folk.entity.world.getBlockState(nextPos).getBlock() instanceof BlockReed) {
                                    highestReedPos = nextPos;
                                } else {
                                    break; // 上一格不是甘蔗，停止遍历
                                }
                            }
                            //获得方块
                            Block bCrop = this.folk.entity.world.getBlockState(highestReedPos).getBlock();
                            // 获取掉落物（甘蔗破坏后会自然掉落所有上方部分）
                           drops = bCrop.getDrops(
                                    this.folk.entity.world,
                                    highestReedPos,
                                    this.folk.entity.world.getBlockState(highestReedPos),
                                    1
                            );

                            // 3. 处理掉落物（放入工作箱）
                            drops.forEach((drop) -> {
                                this.job.placeInJobChest(drop);
                            });

                            //摧毁方块
                           /* drops = bCrop.getDrops(this.folk.entity.world, bp.up(), this.folk.entity.world.getBlockState(bp.up()), 1);
//                                bCrop.getDrops(drops, this.folk.entity.world, bp.north(), this.folk.entity.world.getBlockState(bp.north()), 0);
                            drops.forEach((drop) -> {
                                //放到工作箱
                                this.job.placeInJobChest(drop);
                            });*/
                            //设置手持物
                            this.folk.entity.setActiveHand(EnumHand.MAIN_HAND);
                            //摇摆手臂
                            this.folk.entity.swingArm(EnumHand.MAIN_HAND);
                            //设置为空
                            this.folk.entity.world.destroyBlock(highestReedPos, false);
                            //this.folk.entity.world.setBlockToAir(bp.up());
                            //增加农民等级
                            this.addFarmingLevel();
                            //设置收获时间
                            this.harvestCheck = System.currentTimeMillis();
                            return;
                        }
                    } else if (b == Blocks.CACTUS) {
                        if (this.folk.entity.world.getBlockState(bp.up()).getBlock() == b) {
                            //收获
                            this.folk.setStatus(new TextComponentTranslation("container.sim.job.crop.farmer.Harvesting",new Object[0]).getUnformattedText());
                            // 1. 定位当前列中最高的仙人掌方块
                            BlockPos currentPos = bp.up(); // 初始位置（当前方块上方）
                            BlockPos highestReedPos = currentPos; // 最高仙人掌位置，默认初始位置
                            // 向上遍历，找到最高的仙人掌
                            while (true) {
                                BlockPos nextPos = highestReedPos.up(); // 检查上一格
                                // 如果上一格还是仙人掌，更新最高位置
                                if (this.folk.entity.world.getBlockState(nextPos).getBlock() == b) {
                                    highestReedPos = nextPos;
                                } else {
                                    break; // 上一格不是仙人掌，停止遍历
                                }
                            }
                            //获得方块
                            Block bCrop = this.folk.entity.world.getBlockState(highestReedPos).getBlock();
                            // 获取掉落物（仙人掌破坏后会自然掉落所有上方部分）
                            drops = bCrop.getDrops(
                                    this.folk.entity.world,
                                    highestReedPos,
                                    this.folk.entity.world.getBlockState(highestReedPos),
                                    1
                            );

                            // 3. 处理掉落物（放入工作箱）
                            drops.forEach((drop) -> {
                                this.job.placeInJobChest(drop);
                            });
                            //获得方块
//                            Block bCrop = this.folk.entity.world.getBlockState(bp.up()).getBlock();
//                            //摧毁方块
//                            drops = bCrop.getDrops(this.folk.entity.world, bp.up(), this.folk.entity.world.getBlockState(bp.up()), 1);
////                                bCrop.getDrops(drops, this.folk.entity.world, bp.north(), this.folk.entity.world.getBlockState(bp.north()), 0);
//                            drops.forEach((drop) -> {
//                                //放到工作箱
//                                this.job.placeInJobChest(drop);
//                            });
                            //设置手持物
                            this.folk.entity.setActiveHand(EnumHand.MAIN_HAND);
                            //摇摆手臂
                            this.folk.entity.swingArm(EnumHand.MAIN_HAND);
                            this.folk.entity.world.destroyBlock(bp.up(), false);
                            //设置为空
                            //this.folk.entity.world.setBlockToAir(bp.up());
                            //增加农民等级
                            this.addFarmingLevel();
                            //设置收获时间
                            this.harvestCheck = System.currentTimeMillis();
                            return;
                        }
                    } else {
                        BlockCrops crop = (BlockCrops) b;
                        if (crop.isMaxAge(iBlockState)) {
                            this.folk.setStatus(new TextComponentTranslation("container.sim.job.crop.farmer.Harvesting",new Object[0]).getUnformattedText());
                            //获得方块
                            Block bCrop = this.folk.entity.world.getBlockState(bp).getBlock();
                            //摧毁方块
                            drops = bCrop.getDrops(this.folk.entity.world, bp, this.folk.entity.world.getBlockState(bp), 1);
                            drops.forEach((drop) -> {
                                //放到工作箱
                                this.job.placeInJobChest(drop);
                            });
                            this.folk.entity.setActiveHand(EnumHand.MAIN_HAND);
                            this.folk.entity.swingArm(EnumHand.MAIN_HAND);
                            this.folk.entity.world.destroyBlock(bp, false);
                            //this.folk.entity.world.setBlockToAir(bp);
                            this.addFarmingLevel();
                            this.harvestCheck = System.currentTimeMillis();
                            return;
                        }
                    }
                }
            }
            this.completed = true;
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("JobFarmer-harvest1出错了:" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    @Override
    public void onTaskComplete() {

    }
}
