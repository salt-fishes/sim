package com.trhsy.sim.npcCode.job;

import com.trhsy.sim.loader.ItemLoader;
import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.NpcData;
import com.trhsy.sim.npcCode.block.FarmBox;
import com.trhsy.sim.npcCode.task.*;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/**
 * @ClassName JobFarmer
 * @Description todo 农民
 * @Author TRHSY
 * @Date 2022/12/622:48
 **/
public class JobFarmer extends Job {
    //农田箱
    public FarmBox farm;
    //工作阶段
    public int farmer_stage = 0;
    public JobFarmer(NpcData folk, BlockPos pos, World world, FarmBox fb) {
        super(folk, pos, world);
        try {
            folk.holding = new ItemStack(ItemLoader.tinHoe);
            this.jobName = new TextComponentTranslation("container.sim.Vocation5",new Object[0]).getUnformattedText();
            this.farm = fb;
            if(this.farm!=null){
                this.farm.employee = folk;
            }
           /* //去上班
            this.addJobTask(new JobTaskIdle(this, 200L, new TextComponentTranslation("container.sim.job.builder_Arrived",new Object[0]).getUnformattedText()));
            //开始在农场工作
            this.addJobTask(new JobTaskFarmer(this, -1L, new TextComponentTranslation("container.sim.job.livestock.farmer.Starting",new Object[0]).getUnformattedText(),this.farm));*/
        }catch (Exception e){
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("JobFarmer出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }

    }
    @Override
    public void onUpdate() {
        super.onUpdate();
        try {
            if (!this.atWork) {
                return;
            }
            // P17(S1): 一次把整轮任务排好。
            // 旧实现每次 onUpdate 只推进一步 farmer_stage，依赖“每 tick 都会调用”；
            // 实际服务端在区块/批次/季节切换时可能几十 tick 才进来一次，idle 先完成，
            // nextTask 把 stage 置 -1，farmer_stage 又被重置为 0，农民永远停在 idle。
            if (this.farm == null) {
                this.farm = ModSimLoader.getFarm(this.workPlace);
            }
            if (this.farm == null) {
                this.folk.setStatus(new TextComponentTranslation("container.sim.job.crop.farmer.no_seeds", new Object[0]).getUnformattedText());
                return;
            }
            if (this.currentTask == null) {
                if (this.jobTasks.isEmpty()) {
                    this.farmer_stage = 0;
                    this.stage = 0;
                    // 去上班
                    this.addJobTask(new JobTaskIdle(this, 200L, new TextComponentTranslation("container.sim.job.builder_Arrived", new Object[0]).getUnformattedText()));
                    // 锄地
                    this.addJobTask(new JobTaskFarmer(this, -1L, new TextComponentTranslation("container.sim.job.livestock.farmer.Starting", new Object[0]).getUnformattedText(), this.farm));
                    // 种植
                    this.addJobTask(new JobTaskFarmerPlant(this, -1L, new TextComponentTranslation("container.sim.job.livestock.farmer.Starting", new Object[0]).getUnformattedText(), this.farm));
                    // 等待/催熟
                    this.addJobTask(new JobTaskFarmerGrow(this, -1L, new TextComponentTranslation("container.sim.job.livestock.farmer.Starting", new Object[0]).getUnformattedText(), this.farm));
                    // 收获
                    this.addJobTask(new JobTaskFarmerHarvest(this, -1L, new TextComponentTranslation("container.sim.job.livestock.farmer.Starting", new Object[0]).getUnformattedText(), this.farm));
                    this.farmer_stage = 5;
                    this.stage = 5;
                }
                if (!this.jobTasks.isEmpty()) {
                    this.currentTask = this.jobTasks.get(0);
                    this.currentTask.begin();
                }
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("JobFarmer-onUpdate出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }
    @Override
    public String toString() {
        //农民
        return new TextComponentTranslation("container.sim.Vocation5",new Object[0]).getUnformattedText();
    }
}
