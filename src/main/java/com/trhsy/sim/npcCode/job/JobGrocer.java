package com.trhsy.sim.npcCode.job;

import com.trhsy.sim.loader.ItemLoader;
import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.NpcData;
import com.trhsy.sim.npcCode.task.*;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName JobGrocer
 * @Description todo 食品店
 * @Author TRHSY
 * @Date 2023/4/921:05
 **/
public class JobGrocer extends Job{
    //工作阶段
    public int grocerStage = 0;
    public List<ItemStack> colItems = new ArrayList();
    public JobGrocer(NpcData folk, BlockPos pos, World world) {
        super(folk, pos, world);
        try {
//手持锡锹
            folk.holding = new ItemStack(ItemLoader.tinSpade);
            this.jobName = new TextComponentTranslation("container.sim.Vocation26",new Object[0]).getUnformattedText();

            //马铃薯
            this.colItems.add(new ItemStack(Items.POTATO, 16));
            //胡萝卜
            this.colItems.add(new ItemStack(Items.CARROT, 16));
            //甜菜根
            this.colItems.add(new ItemStack(Items.BEETROOT, 16));
            // P9: 苹果没有任何自动化来源（无果树职业、无采集映射），已从清单移除
            //南瓜
            this.colItems.add(new ItemStack(Blocks.PUMPKIN, 8));
            //西瓜
            this.colItems.add(new ItemStack(Items.MELON, 16));
        }catch (Exception e){
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("JobGrocer出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }
    @Override
    public void onUpdate() {
        super.onUpdate();
        try {
            if (this.atWork) {
                if (this.stage == -1) {
                    this.grocerStage = 0;
                    this.stage = 0;
                } else if (this.grocerStage == 0) {
                    this.grocerStage = 1;
                    //去上班
                    this.addJobTask(new JobTaskIdle(this, 200L, new TextComponentTranslation("container.sim.job.builder_Arrived",new Object[0]).getUnformattedText()));
                } else if (this.grocerStage == 1) {
                    //装卸货
                    this.addJobTask(new JobTaskCollectItems(this, -1L, colItems));
                    this.grocerStage = 2;
                }else if (this.grocerStage == 2) {
                    //装卸货
                    this.addJobTask(new JobTaskUnloadItems(this, -1L, colItems));
                    this.grocerStage = 3;
                }else if (this.grocerStage == 3) {
                    //售卖
                    this.addJobTask(new JobTaskShopkeep(this, -1L, new TextComponentTranslation("container.sim.job.Grocer1",new Object[0]).getUnformattedText(),true));
                    this.grocerStage = 4;
                }else{
                    if (this.jobTasks.size() > 0&&this.currentTask==null) {
                        this.currentTask = (JobTask) this.jobTasks.get(0);
                        this.currentTask.begin();
                    }
                }
            }
        }catch (Exception e){
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("JobGrocer-onUpdate出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }

    }
    @Override
    public String toString() {
        return new TextComponentTranslation("container.sim.Vocation26",new Object[0]).getUnformattedText();
    }
}
