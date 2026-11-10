package com.trhsy.sim.npcCode.job;

import com.trhsy.sim.loader.ItemLoader;
import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.NpcData;
import com.trhsy.sim.npcCode.task.*;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName JobButcher
 * @Description todo 屠夫
 * @Author TRHSY
 * @Date 2023/4/916:16
 **/
public class JobButcher extends Job{
    //工作阶段
    public int butcherStage = 0;
    //要收集的物品
    public List<ItemStack> colItems = new ArrayList();
    public JobButcher(NpcData folk, BlockPos pos, World world) {
        super(folk, pos, world);
        try {
            folk.holding = new ItemStack(ItemLoader.tinAxe);
            this.jobName = new TextComponentTranslation("container.sim.Vocation15",new Object[0]).getUnformattedText();
            //牛肉
            this.colItems.add(new ItemStack(Items.BEEF, 16));
            //鸡肉
            this.colItems.add(new ItemStack(Items.CHICKEN, 16));
            //羊肉
            this.colItems.add(new ItemStack(Items.MUTTON, 16));
            //猪排
            this.colItems.add(new ItemStack(Items.PORKCHOP, 16));
            //兔肉
            this.colItems.add(new ItemStack(Items.RABBIT, 16));
        }catch (Exception e){
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("JobButcher出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }
    @Override
    public void onUpdate() {
        super.onUpdate();
        try {
            if (this.atWork) {
                if (this.stage == -1) {
                    this.butcherStage = 0;
                    this.stage = 0;
                } else if (this.butcherStage == 0) {
                    this.butcherStage = 1;
                    //去上班
                    this.addJobTask(new JobTaskIdle(this, 200L, new TextComponentTranslation("container.sim.job.builder_Arrived",new Object[0]).getUnformattedText()));
                } else if (this.butcherStage == 1) {
                    //向物流中心下单
                    this.addJobTask(new JobTaskRequestSupply(this, -1L, colItems));
                    this.butcherStage = 2;
                } else if (this.butcherStage == 2) {
                    //货由快递员直接放进店内箱子
                    this.butcherStage = 3;
                } else if (this.butcherStage == 3) {
                    /**
                     * @Author fan
                     * @Description //TODO 卖肉
                     * @Date 16:44 2023/4/9
                     * @Param [folk, pos, world]
                     * @return
                     **/
                    this.addJobTask(new JobTaskShopkeep(this, -1L, new TextComponentTranslation("container.sim.SELLINGMEAT",new Object[0]).getUnformattedText(),true));
                    this.butcherStage = 4;
                }else{
                    if (this.jobTasks.size() > 0&&this.currentTask==null) {
                        this.currentTask = (JobTask) this.jobTasks.get(0);
                        this.currentTask.begin();
                    }
                }
            }
        }catch (Exception e){
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("JobButcher-onUpdate出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }

    }
    @Override
    public String toString() {
        return new TextComponentTranslation("container.sim.Vocation15",new Object[0]).getUnformattedText();
    }
}
