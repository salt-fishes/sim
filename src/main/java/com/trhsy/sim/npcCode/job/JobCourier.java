package com.trhsy.sim.npcCode.job;

import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.NpcData;
import com.trhsy.sim.npcCode.task.JobTaskCourier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/**
 * 物流中心快递员。
 *
 * <p>Beta12 重构后不再需要路径箱。快递员到岗后领取店铺的补货订单，
 * 先到物流中心取货，缺货时按供货目录去农场/矿场/生产点取，
 * 再把货送到下单店铺附近的箱子；没有订单时自动把农场/矿场/生产点的
 * 产出运回物流中心。</p>
 */
public class JobCourier extends Job {
    /** 本次入职后是否已经提示过自动物流。 */
    private transient boolean announced = false;

    public JobCourier(NpcData folk, BlockPos pos, World world) {
        super(folk, pos, world);
        try {
            this.jobName = new TextComponentTranslation("container.sim.Vocation10", new Object[0]).getUnformattedText();
            this.stage = -1;
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("JobCourier出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        try {
            if (!this.atWork) {
                return;
            }
            if (!this.announced) {
                this.announced = true;
                ModSimLoader.sendChat(new TextComponentTranslation(
                        "container.sim.job.courier.auto.hired", new Object[0]).getUnformattedText());
            }
            // 快递员只需要一个长期存在的物流任务；任务内部自己循环，不会完成。
            if (this.currentTask == null) {
                if (this.jobTasks.isEmpty()) {
                    this.addJobTask(new JobTaskCourier(this, -1L, this.jobName));
                }
                if (!this.jobTasks.isEmpty()) {
                    this.currentTask = this.jobTasks.get(0);
                    this.currentTask.begin();
                }
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("JobCourier-onUpdate出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    @Override
    public String toString() {
        return this.jobName;
    }
}
