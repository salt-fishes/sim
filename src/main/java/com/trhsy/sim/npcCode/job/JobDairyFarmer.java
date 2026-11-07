package com.trhsy.sim.npcCode.job;

import com.trhsy.sim.loader.ItemLoader;
import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.NpcData;
import com.trhsy.sim.npcCode.task.JobTask;
import com.trhsy.sim.npcCode.task.JobTaskHarvestAnimal;
import com.trhsy.sim.npcCode.task.JobTaskIdle;
import com.trhsy.sim.npcCode.task.JobTaskSpawnLivestock;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import java.util.Random;

/**
 * @ClassName JobDairyFarmer
 * @Description todo 牛奶农的工作
 * @Author TRHSY
 * @Date 2023/4/517:40
 **/
public class JobDairyFarmer extends Job{
    //工作阶段（保留字段兼容；新逻辑每轮重建任务）
    public int dairyFarmerStage = 0;
    public JobDairyFarmer(NpcData folk, BlockPos pos, World world) {
        super(folk, pos, world);
        try {
            folk.holding = new ItemStack(Items.MILK_BUCKET);
            //牛奶农
            this.jobName = new TextComponentTranslation("container.sim.Vocation20",new Object[0]).getUnformattedText();
        }catch (Exception e){
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("JobDairyFarmer出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }

    }

    /**
     * P3: 一次构建整轮任务再执行（旧实现只入队不开始，NPC 到岗后停在"去工作"）。
     * 注意末尾的照料任务原来用 -1L（永不超时），会让整轮永远结束不了，改成有限时长。
     */
    @Override
    public void onUpdate() {
        super.onUpdate();
        try {
            if (!this.atWork) {
                return;
            }
            if (this.currentTask != null || !this.jobTasks.isEmpty()) {
                return;
            }
            String cow = new TextComponentTranslation("container.sim.job_Livestock_cow",new Object[0]).getUnformattedText();
            String tending = new TextComponentTranslation("container.sim.job.crop.farmer.Tending1",new Object[0]).getUnformattedText() + cow;
            this.addJobTask(new JobTaskIdle(this, 200L, new TextComponentTranslation("container.sim.job.builder_Arrived",new Object[0]).getUnformattedText()));
            this.addJobTask(new JobTaskSpawnLivestock(this, cow, EntityCow.class, 5000L));
            this.addJobTask(new JobTaskIdle(this, 180000L, tending));
            this.addJobTask(new JobTaskHarvestAnimal(this, 10000L, cow,
                    new ItemStack(ItemLoader.itemBucketMilk, new Random().nextInt(5) + 1), false,
                    new TextComponentTranslation("container.sim.MILKING",new Object[0]).getUnformattedText()));
            this.addJobTask(new JobTaskIdle(this, 60000L, tending));
            this.dairyFarmerStage = 0;
            this.stage = 0;
            this.currentTask = (JobTask) this.jobTasks.get(0);
            this.currentTask.begin();
        }catch (Exception e){
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("JobDairyFarmer-onUpdate出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }

    }
    @Override
    public String toString() {
        //牛奶农
        return this.jobName;
    }
}
