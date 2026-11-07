package com.trhsy.sim.npcCode.job;

import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.NpcData;
import com.trhsy.sim.npcCode.task.JobTask;
import com.trhsy.sim.npcCode.task.JobTaskIdle;
import com.trhsy.sim.npcCode.task.JobTaskPlaceInChest;
import com.trhsy.sim.npcCode.task.JobTaskSpawnLivestock;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import java.util.Random;

/**
 * @ClassName JobEggFarmer
 * @Description todo 鸡蛋农
 * @Author TRHSY
 * @Date 2023/4/89:01
 **/
public class JobEggFarmer extends Job{
    //工作阶段（保留字段兼容；新逻辑每轮重建任务）
    public int eggFarmerStage = 0;
    public JobEggFarmer(NpcData folk, BlockPos pos, World world) {
        super(folk, pos, world);
        try {
            folk.holding = new ItemStack(Items.EGG);
            //鸡蛋农
            this.jobName = new TextComponentTranslation("container.sim.Vocation3",new Object[0]).getUnformattedText();
        }catch (Exception e){
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("JobEggFarmer出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }

    }

    /**
     * P3: 一次构建整轮任务再执行（旧实现只入队不开始，NPC 到岗后停在"去工作"）。
     * 末尾照料任务原来是 -1L（永不超时），改成有限时长，保证轮次能结束。
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
            String chicken = new TextComponentTranslation("container.sim.job_Livestock_chicken",new Object[0]).getUnformattedText();
            String tending = new TextComponentTranslation("container.sim.job.crop.farmer.Tending1",new Object[0]).getUnformattedText() + chicken;
            this.addJobTask(new JobTaskIdle(this, 200L, new TextComponentTranslation("container.sim.job.builder_Arrived",new Object[0]).getUnformattedText()));
            this.addJobTask(new JobTaskSpawnLivestock(this, chicken, EntityChicken.class, 5000L));
            this.addJobTask(new JobTaskIdle(this, 180000L, tending));
            this.addJobTask(new JobTaskPlaceInChest(this, 10000L, new ItemStack(Items.EGG, new Random().nextInt(5) + 1)));
            this.addJobTask(new JobTaskIdle(this, 60000L, tending));
            this.eggFarmerStage = 0;
            this.stage = 0;
            this.currentTask = (JobTask) this.jobTasks.get(0);
            this.currentTask.begin();
        }catch (Exception e){
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("JobEggFarmer-onUpdate出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }
    @Override
    public String toString() {
        return new TextComponentTranslation("container.sim.Vocation3",new Object[0]).getUnformattedText();
    }
}
