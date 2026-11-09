package com.trhsy.sim.npcCode.task;

import com.trhsy.sim.loader.BlockLoader;
import com.trhsy.sim.loader.ItemLoader;
import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.V3;
import com.trhsy.sim.npcCode.build.Building;
import com.trhsy.sim.npcCode.job.Job;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.List;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.npc.task
 * @ClassName: JobTaskCheesemaker
 * @Description: 制作奶酪
 * @date 2023/07/31 下午 3:59
 */
public class JobTaskCheesemaker extends JobTask{
    //步
    public int cheesemakerStage = 0;
    public String status;
    private Building theCheeseFactory = null;
    //切换位置
    private boolean tubToggle = true;
    /**
     * 当前搅拌位置
     */
    private V3 currentStirPos;
    //搅拌计数
    private int stirCount = 0;
    public JobTaskCheesemaker(Job j, long ms, String status) {
        super(j, ms);
        this.status = status;
        V3 v3=new V3(this.job.workPlace.x,this.job.workPlace.y,this.job.workPlace.z);
        // P18: 工作点就是控制箱方块（与其他职业一致）；旧实现再 -1，会落到控制箱下方，
        // 导致“未找到奶酪工厂”。控制箱由 workPlace 直接匹配。
        this.theCheeseFactory = ModSimLoader.getBuildingByV3(v3);
        // 校验奶酪工厂是否存在
        if (this.theCheeseFactory == null) {
            ModSimLoader.log.error("未找到奶酪工厂，坐标：" + v3.toString());
            this.job.folk.setStatus("找不到奶酪工厂，任务终止");
            this.completeTask(); // 终止任务
            return;
        }
        this.cheesemakerStage = 1;
    }
    @Override
    public void onTaskBegin() {
        this.job.folk.setStatus(this.status);
    }

    @Override
    public void onUpdate() {
        try {
            if (this.cheesemakerStage == 1) {
                this.cheesemakerStage = 2;
                //准备加满水槽
                this.job.folk.setStatus(new TextComponentTranslation("container.sim.job.cheese_maker.Preparing",new Object[0]).getUnformattedText());
            } else if (this.cheesemakerStage == 2) {
                if(this.theCheeseFactory==null){}
                if(!this.job.inventoriesTransferToFolk(new ItemStack(ItemLoader.itemBucketMilk))){

                    return;
                }
                //倒牛奶
                List<V3> milkblocks = this.theCheeseFactory.getSpecialBlocks(0);
                ModSimLoader.log.info("获取牛奶块（meta=0）：数量=" + milkblocks.size());
                if (milkblocks.isEmpty()) {
                    this.job.folk.setStatus("缺少牛奶槽，无法继续");
//                    ModSimLoader.sendChat("奶酪工厂缺少牛奶槽，请检查建筑配置");
                    ModSimLoader.sendChat(new TextComponentTranslation("container.sim.job.cheese_maker.constructor",new Object[0]).getUnformattedText());
                    this.completeTask();
                    return;
                }
                boolean filledOk = false;
                for (V3 milkBlock : milkblocks) {
                    //牛奶
                    BlockPos blockPos = new BlockPos(milkBlock.x, milkBlock.y, milkBlock.z);
                    Block id = this.job.jobWorld.getBlockState(blockPos).getBlock();
                    ModSimLoader.log.debug("检查牛奶块：" + blockPos + "，当前方块：" + id.getRegistryName());
                    if ( id != BlockLoader.milk&& id != BlockLoader.flowing_milk) {
                        this.job.jobWorld.setBlockState(blockPos, BlockLoader.milk.getDefaultState(), 3);
                        filledOk = true;
                        ModSimLoader.log.info("已填充牛奶到：" + blockPos);
                    }
// 清空NPC背包（保持原逻辑，但添加日志）
                    try {
                        this.job.folk.inventory.clear(); // 替换循环删除，更简洁
                        ModSimLoader.log.debug("已清空NPC背包");
                    } catch (Exception e) {
                        ModSimLoader.log.error("清空背包失败：" + e.getMessage());
                    }
                }
                if (!filledOk) {
                    ModSimLoader.log.warn("未找到可填充牛奶的方块，进入下一阶段");
                }
                // P18: 旧实现只在 filledOk=false 时推进；成功填满牛奶槽后反而永远停在阶段 2。
                this.cheesemakerStage = 3;
                //搅拌
            }else if(this.cheesemakerStage == 3){
                //检查牛奶粘度
                this.job.folk.setStatus(new TextComponentTranslation("container.sim.job.cheese_maker.viscosity",new Object[0]).getUnformattedText());
                List<V3> stirPositions = this.theCheeseFactory.getSpecialBlocks(4);
                ModSimLoader.log.info("获取搅拌位置（meta=4）：数量=" + stirPositions.size());

                if (!stirPositions.isEmpty()) {
                    // 处理搅拌位置索引，避免越界
                    int idx1 = 0;
                    int idx2 = Math.min(1, stirPositions.size() - 1); // 若只有1个位置，idx2=0
                    if (this.tubToggle) {
                        this.currentStirPos =  stirPositions.get(idx1);

                    } else {
                        this.currentStirPos =  stirPositions.get(idx2);
                    }
                    ModSimLoader.log.info("移动到搅拌位置：" + this.currentStirPos.toBlockPos() + "（索引：" + (this.tubToggle ? idx1 : idx2) + "）");

                    this.folk.forceMoveToXYZ(this.currentStirPos);
                    this.tubToggle = !this.tubToggle;
                    this.stirCount = 0;
                    this.cheesemakerStage = 4;
                } else {
                    //奶酪工厂出了问题,放下一个建筑箱,重新建造
                    ModSimLoader.sendChat(new TextComponentTranslation("container.sim.job.cheese_maker.constructor",new Object[0]).getUnformattedText());
                    this.job.folk.fire();
                    this.completeTask();
                    return;
                }
            }else if(this.cheesemakerStage == 4){
                String say = "";
                switch (this.stirCount) {
                    case 0:
                        //搅拌牛奶
                        say = new TextComponentTranslation("container.sim.job.cheese_maker.Stirring",new Object[0]).getUnformattedText();
                        this.stirCount++;
                        break;
                    case 1:
                        //加入绝密成分
                        say = new TextComponentTranslation("container.sim.job.cheese_maker.ingredient",new Object[0]).getUnformattedText();
                        this.stirCount++;
                        break;
                    case 2:
                        //添加细菌培养
                        say = new TextComponentTranslation("container.sim.job.cheese_maker.bacterial",new Object[0]).getUnformattedText();
                        this.stirCount++;
                        break;
                    case 3:
                        //除去不需要的孢子
                        say = new TextComponentTranslation("container.sim.job.cheese_maker.unwanted",new Object[0]).getUnformattedText();
                        this.stirCount++;
                        break;
                    case 4:
                        //检查发酵进度
                        say = new TextComponentTranslation("container.sim.job.cheese_maker.fermentation",new Object[0]).getUnformattedText();
                        this.stirCount++;
                        break;
                    case 5:
                        //加入凝乳酶
                        say = new TextComponentTranslation("container.sim.job.cheese_maker.Adding",new Object[0]).getUnformattedText();
                        this.stirCount++;
                        break;
                    case 6:
                        //网条状奶酪
                        say = new TextComponentTranslation("container.sim.job.cheese_maker.Reticulating",new Object[0]).getUnformattedText();
                        //开始转化奶酪
                        this.cheesemakerStage = 5;
                }
                this.job.folk.setStatus(say);


            }else if(this.cheesemakerStage == 5){
                //提取奶酪块
               String say= new TextComponentTranslation("container.sim.job.cheese_maker.Extracting",new Object[0]).getUnformattedText();

                this.job.folk.setStatus(say);
                //0是牛奶
                List<V3> milkBlocks = this.theCheeseFactory.getSpecialBlocks(0);
                //1是要放置的奶酪块
                List<V3> cheeseBlocks = this.theCheeseFactory.getSpecialBlocks(1);
                ModSimLoader.log.info("获取牛奶块（meta=0）：" + milkBlocks.size() + "个；奶酪放置位（meta=1）：" + cheeseBlocks.size() + "个");
                // 校验牛奶块和奶酪块列表
                if (milkBlocks.isEmpty()) {
                    //"缺少牛奶块，无法制作奶酪"
                    String say1= new TextComponentTranslation("container.sim.job.cheese_maker.Extracting1",new Object[0]).getUnformattedText();
                    ModSimLoader.sendChat(say1);
                    this.completeTask();
                    return;
                }
                if (cheeseBlocks.isEmpty()) {
                    //"缺少奶酪放置位，无法制作奶酪"
                    String say2= new TextComponentTranslation("container.sim.job.cheese_maker.Extracting2",new Object[0]).getUnformattedText();
                    ModSimLoader.sendChat(say2);
                    this.completeTask();
                    return;
                }
                if (!milkBlocks.isEmpty() && !cheeseBlocks.isEmpty()) {
                    boolean placedCheese = false;
                    int milkGotCount = 0;

                    Block id;
                    int dist;
                    for (int m = milkBlocks.size() - 1; m > 0; --m) {
                        V3 cheese = (V3) milkBlocks.get(m);

                        BlockPos blockPos = new BlockPos(cheese.x, cheese.y, cheese.z);
                        id = this.job.jobWorld.getBlockState(blockPos).getBlock();
                        dist = id.getMetaFromState(this.job.jobWorld.getBlockState(blockPos));
                        //设置奶酪块
                        if (id == BlockLoader.milk && dist == 0) {
                            this.job.jobWorld.setBlockState(blockPos, Blocks.AIR.getDefaultState(), 3);
                            ++milkGotCount;
                            if (milkGotCount > 1) {
                                break;
                            }
                        }
                    }

                    if (milkGotCount > 0) {
                        for (V3 cheese : cheeseBlocks) {
                            id = this.job.jobWorld.getBlockState(new BlockPos(cheese.x, cheese.y, cheese.z)).getBlock();
                            dist = cheese.getDistanceTo(currentStirPos);
                            if (id != BlockLoader.blockCheese && dist < 5) {
                                BlockPos blockPos = new BlockPos(cheese.x, cheese.y, cheese.z);
                                this.job.jobWorld.setBlockState(blockPos, BlockLoader.blockCheese.getDefaultState(), 3);
                                placedCheese = true;
                                break;
                            }
                        }
                    }

                    if (milkGotCount == 0 || !placedCheese) {
                        //this.completeTask();
                        this.cheesemakerStage = 6;
                    }

                } else {
                    //this.theFolk.selfFire();
                    //有一个与奶酪工厂问题，请尝试重新建立它 - 没有奶块
                    ModSimLoader.sendChat(new TextComponentTranslation("container.sim.job.cheese_maker.Cheese_factory",new Object[0]).getUnformattedText());
                }
            }else if(this.cheesemakerStage == 6){
                String say= new TextComponentTranslation("container.sim.job.cheese_maker.Slicing",new Object[0]).getUnformattedText();
                this.job.folk.setStatus(say);
                //1是要放置的奶酪块
                List<V3> cheeseBlocks = this.theCheeseFactory.getSpecialBlocks(1);
                ModSimLoader.log.info("处理奶酪块（meta=1）：数量=" + cheeseBlocks.size());
                if (cheeseBlocks.isEmpty()) {
                    ModSimLoader.log.warn("无奶酪块可处理，任务完成");
                    this.completeTask();
                    return;
                }
                for (V3 cheese : cheeseBlocks) {
                    BlockPos blockPos = cheese.toBlockPos();
                    Block id = this.job.jobWorld.getBlockState(blockPos).getBlock();
//                    Block id = this.job.jobWorld.getBlockState(new BlockPos(cheese.x, cheese.y, cheese.z)).getBlock();
                    ModSimLoader.log.debug("检查奶酪块：" + blockPos + "，当前方块：" + id.getRegistryName());
                    if (id.equals(BlockLoader.blockCheese)) {
                        //替换为空气
//                        BlockPos blockPos = new BlockPos(cheese.x, cheese.y, cheese.z);
                        this.job.jobWorld.setBlockState(blockPos, Blocks.AIR.getDefaultState(), 3);
                        this.job.placeInJobChest(new ItemStack(ItemLoader.itemCheese,9));
                        ModSimLoader.log.info("已提取奶酪：" + blockPos);
                    }
                }
                this.completeTask();
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("stageGoingToTank出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }
    @Override
    public void onTaskComplete() {

    }

}
