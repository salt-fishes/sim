package com.trhsy.sim.npcCode.task;

import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.job.Job;
import net.minecraft.util.text.TextComponentTranslation;

/**
 * @ClassName JobTaskShopkeep
 * @Description todo 售卖任务
 * @Author TRHSY
 * @Date 2022/11/1521:20
 **/
public class JobTaskShopkeep extends JobTask {
    String product;
    boolean hasFed = false;
    boolean nhasFed = false;

    public JobTaskShopkeep(Job j, long ms, String product,boolean hasFed) {
        super(j, ms);
        this.product = product;
        this.hasFed = hasFed;
        this.nhasFed = hasFed;
    }

    @Override
    public void onTaskBegin() {
        this.hasFed = this.nhasFed;
        this.folk.entity.getNavigator().clearPath();
        //设置固定不动
        this.folk.stayPut = true;
    }

    @Override
    public void onUpdate() {
        if (this.job.jobWorld.getWorldTime() % 24000L < 11600L) {
            //售卖
            this.folk.setStatus(new TextComponentTranslation("container.sim.job_task_Selling",new Object[0]).getUnformattedText() + " " + this.product);
        } else {
            //关闭店铺
            this.folk.setStatus(new TextComponentTranslation("container.sim.job.Baker_Closing",new Object[0]).getUnformattedText());
//            if(this.job.jobName.contains("")){}
            if (this.hasFed) {
                int sell = this.job.feedFolks();
                this.hasFed = false;
                if (sell > 0) {
                    //张三 （工作/面包师）今天卖了 000 个食物给人们的。
                    ModSimLoader.sendChat(this.folk.getName() + "(" + this.job.toString() + ")  " + new TextComponentTranslation("container.sim.job.has_sold",new Object[0]).getUnformattedText() + sell + new TextComponentTranslation("container.sim.job.grocer.farmer.folks",new Object[0]).getUnformattedText());
                } else {
                    //张三 今天没有产品可以卖给人们。
                    ModSimLoader.sendChat(this.folk.getName() + "(" + this.job.toString() + ")  " + new TextComponentTranslation("container.sim.job.grocer.farmer.today",new Object[0]).getUnformattedText());
                }
            }
            // P8: 原来调用的是 onTaskComplete()（空实现），completed 永不置位，
            // 打烊后任务永不完成 → 第二天不会重新进货。必须用 completeTask()。
            this.completeTask();
        }

    }

    @Override
    public void onTaskComplete() {
    }
}
