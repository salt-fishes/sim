package com.trhsy.sim.npcCode.task;

import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.V3;
import com.trhsy.sim.npcCode.economy.Economy;
import com.trhsy.sim.npcCode.job.Job;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;

/**
 * @ClassName JobTaskPlaceInChest
 * @Description todo 往箱子里放东西
 * @Author TRHSY
 * @Date 2023/4/89:26
 **/
public class JobTaskPlaceInChest extends JobTask {
    /**
     * @Author fan
     * @Description //TODO 放箱子里的物品
     * @Date 9:27 2023/4/8
     * @Param 
     * @return 
     **/
    ItemStack toPlace;
    /*
     * @Author fan
     * @Description //TODO 放入时间
     * @Date 9:27 2023/4/8
     * @Param 
     * @return 
     **/
    transient long timeToPlace = 0L;
    public JobTaskPlaceInChest(Job j, long ms, ItemStack is) {
        super(j, ms);
        this.toPlace = is;
    }

    @Override
    public void onTaskBegin() {
        this.folk.setStatus(new TextComponentTranslation("container.sim.job_task_PlaceInChest1",new Object[0]).getUnformattedText());
    }

    @Override
    public void onUpdate() {
        if (this.job.folk.isAtDestination(this.job.workPlace, 3.0D, 4.0D)) {
            if (this.timeToPlace == 0L) {
                this.timeToPlace = System.currentTimeMillis();
                return;
            }

            if (System.currentTimeMillis() - this.timeToPlace < 2000L) {
                return;
            }

            this.job.placeInJobChest(this.toPlace);
            // P20: 成品产出收入归生产 NPC，不再反向给玩家发钱。
            Economy.payOutput(this.job.folk, this.toPlace.getCount(), this.job.jobName);
            this.completeTask();
        } else if (!this.job.folk.entity.isMoving() && this.job.folk.entity.getNavigator().getPath() == null) {
            V3 v3=this.job.workPlace;
            this.folk.forceMoveToXYZ(v3);
        }
    }

    @Override
    public void onTaskComplete() {

    }
}
