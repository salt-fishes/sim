package com.trhsy.sim.npcCode.job;

import com.trhsy.sim.loader.ItemLoader;
import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.NpcData;
import com.trhsy.sim.npcCode.task.JobTask;
import com.trhsy.sim.npcCode.task.JobTaskButcherAnimal;
import com.trhsy.sim.npcCode.task.JobTaskIdle;
import com.trhsy.sim.npcCode.task.JobTaskSpawnLivestock;
import net.minecraft.entity.passive.*;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.npc.job
 * @ClassName: JobLivestockFarmer
 * @Description:
 * @date 2023/3/21 14:04
 */
public class JobLivestockFarmer extends Job{
    /**
     * 工作名称牲畜名称
     */
    public String livestockName;
    /**
     * 牲畜等级
     */
    public Class livestockClass;
    //工作阶段（保留字段兼容存档/外部引用；新逻辑每轮重建任务，不再用它驱动状态机）
    public int livestockFarmerStage = 0;
    public JobLivestockFarmer(NpcData folk, BlockPos pos, String livestock, World world) {
        super(folk,pos,world);
        try {
//手持斧子
            folk.holding = new ItemStack(ItemLoader.tinAxe);
            this.livestockName = livestock;
            //猪
            if (this.livestockName.equals(new TextComponentTranslation("container.sim.job_Livestock_pig",new Object[0]).getUnformattedText())) {
                this.livestockClass = EntityPig.class;
                //养猪户
                this.jobName = new TextComponentTranslation("container.sim.Vocation13",new Object[0]).getUnformattedText();
                //牛
            } else if (this.livestockName.equals(new TextComponentTranslation("container.sim.job_Livestock_cow",new Object[0]).getUnformattedText())) {
                this.livestockClass = EntityCow.class;
                //养牛户
                this.jobName =new TextComponentTranslation("container.sim.Vocation12",new Object[0]).getUnformattedText();
                //羊
            } else if (this.livestockName.equals(new TextComponentTranslation("container.sim.job_Livestock_sheep",new Object[0]).getUnformattedText())) {
                this.livestockClass = EntitySheep.class;
                //养羊户（与雇佣/采集使用的 Vocation28 对齐；中文下 27/28 同名，英文下才一致）
                this.jobName = new TextComponentTranslation("container.sim.Vocation28",new Object[0]).getUnformattedText();
                //鸡
            } else if (this.livestockName.equals(new TextComponentTranslation("container.sim.job_Livestock_chicken",new Object[0]).getUnformattedText())) {
                this.livestockClass = EntityChicken.class;
                //养鸡户
                this.jobName = new TextComponentTranslation("container.sim.Vocation14",new Object[0]).getUnformattedText();
                //兔
            }else if (this.livestockName.equals(new TextComponentTranslation("container.sim.job_Livestock_rabbit",new Object[0]).getUnformattedText())) {
                this.livestockClass = EntityRabbit.class;
                //养兔户
                this.jobName =  new TextComponentTranslation("container.sim.Vocation29",new Object[0]).getUnformattedText();
            }
        }catch (Exception e){
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("JobLivestockFarmer出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    /**
     * P3: 一次构建整轮任务，然后交给任务队列顺序执行。
     * 旧实现每 tick 只推进一个阶段、只把任务塞进队列（永远不开始），
     * 导致 NPC 到岗后一直停在"去工作"。现在队列空时重建一轮：
     * 到达 → 补栏 → 照料/屠宰交替，队列跑完（stage=-1）后自动开始下一轮。
     */
    @Override
    public void onUpdate() {
        super.onUpdate();
        try {
            if (!this.atWork) {
                return;
            }
            if (this.currentTask != null || !this.jobTasks.isEmpty()) {
                return; // 这一轮还没跑完
            }
            String tending = new TextComponentTranslation("container.sim.job.crop.farmer.Tending1",new Object[0]).getUnformattedText() + " " + this.livestockName;
            //到达
            this.addJobTask(new JobTaskIdle(this, 200L, new TextComponentTranslation("container.sim.job.builder_Arrived",new Object[0]).getUnformattedText()));
            //补栏（保持 4-6 头）
            this.addJobTask(new JobTaskSpawnLivestock(this, this.livestockName, this.livestockClass, 5000L));
            //照料 / 屠宰交替（屠宰任务内部有数量下限，不会清空）
            this.addJobTask(new JobTaskIdle(this, 5000L, tending));
            this.addJobTask(new JobTaskButcherAnimal(this, 5000L));
            this.addJobTask(new JobTaskIdle(this, 5000L, tending));
            this.addJobTask(new JobTaskButcherAnimal(this, 5000L));
            this.addJobTask(new JobTaskIdle(this, 5000L, tending));
            this.addJobTask(new JobTaskButcherAnimal(this, 5000L));
            this.addJobTask(new JobTaskIdle(this, 5000L, tending));
            this.livestockFarmerStage = 0;
            this.stage = 0;
            this.currentTask = (JobTask) this.jobTasks.get(0);
            this.currentTask.begin();
        }catch (Exception e){
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("JobLivestockFarmer-onUpdate出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }
    @Override
    public String toString() {
        //农民
        return this.jobName;
    }
}
