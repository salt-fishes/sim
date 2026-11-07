package com.trhsy.sim.npcCode.task;


import com.trhsy.sim.npcCode.NpcData;

import java.util.Random;

/**
 * @ClassName Task
 * @Description todo 任务
 * @Author TRHSY
 * @Date 2022/10/1615:47
 **/
public abstract class Task {
    /**实施任务的人**/
    public NpcData folk;
    /**最后时间**/
    public transient long deadline;
    /**上次运行时间**/
    public transient long timeSinceLastRun = 0L;
    /**已经开始**/
    public boolean hasBegun;
    /**完成**/
    public boolean completed;
    /**任务中断，睡一会**/
    public boolean interruptSleep;
    /**随机声明**/
    public Random rand = new Random();
    public String statusText;
    /**
     * @Author fan
     * @Description //TODO 初始化任务
     * @Date 9:59 2022/11/13
     * @Param [folk, ms]
     * @return
     **/
    public Task(NpcData folk, long ms) {
        this.folk = folk;
        this.deadline = ms;
    }
    /**
     * @Author fan
     * @Description //TODO 完成任务
     * @Date 9:59 2022/11/13
     * @Param []
     * @return void
     **/
    public void completeTask() {
        // P9: 幂等——重复调用（例如 onUpdate 的 catch 里再调一次）不再二次移除任务。
        if (this.completed) {
            return;
        }
        this.onTaskComplete();
        //任务完成
        this.completed = true;
        //下一个
        this.folk.nextTask();
    }
    /**
     * @Author fan
     * @Description //TODO 任务失败
     * @Date 10:01 2022/11/13
     * @Param [message]
     * @return void
     **/
    public void failTask(String message) {
        if (this.completed) {
            return;
        }
        this.onTaskComplete();
        //
        this.completed = true;
        //下一个
        this.folk.nextTask();
    }
    /**
     * @Author fan
     * @Description //TODO 任务开始
     * @Date 10:01 2022/11/13
     * @Param []
     * @return void
     **/
    public void begin() {
        this.completed = false;
        this.timeSinceLastRun = System.currentTimeMillis();
        this.onTaskBegin();
    }
    /**
     * @Author fan
     * @Description //TODO 更新任务
     * @Date 10:02 2022/11/13
     * @Param []
     * @return void
     **/
    public void update() {
        if (!this.completed) {
            //截止时间大于0并且当前时间减去上次运行大于截止时间
            if (this.deadline > 0L && System.currentTimeMillis() - this.timeSinceLastRun > this.deadline) {
                this.completeTask();
            } else {
                //更新任务吧
                this.onUpdate();
            }
        }
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }


    public abstract void onTaskBegin();
    public abstract void onUpdate();
    public abstract void onTaskComplete();
}
