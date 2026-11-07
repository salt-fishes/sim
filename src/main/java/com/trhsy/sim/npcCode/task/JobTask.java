package com.trhsy.sim.npcCode.task;

import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.NpcData;
import com.trhsy.sim.npcCode.job.Job;

import java.util.Random;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.task
 * @ClassName: JobTask
 * @Description:
 * @date 2022/10/13 17:44
 */
public abstract class JobTask {
    public Job job;
    public NpcData folk;
    public transient long deadline;
    public transient long timeSinceLastRun = 0L;
    public int stage;
    public boolean hasBegun;
    public boolean completed;
    public Random rand = new Random();

    public JobTask(Job j, long ms) {
        this.job = j;
        this.folk = j.folk;
        this.deadline = ms;
    }

    public void completeTask() {
        this.onTaskComplete();
        this.completed = true;
    }

    public void failTask(String message) {
        // P3/P8: 原来 message 被直接丢弃，失败和成功对 update() 完全同义，
        // 玩家既看不到失败也诊断不出原因。现在真正把消息送出去（调用点都在
        // onTaskBegin，每个任务一次，不会刷屏）。
        if (message != null && !message.isEmpty()) {
            ModSimLoader.sendChat(message);
            ModSimLoader.log.warn("[任务失败] " + this.job.jobName + ": " + message);
        }
        this.onTaskComplete();
        this.completed = true;
    }

    public void begin() {
        this.completed = false;
        this.folk.hunger = this.folk.hunger - 0.02;
        this.timeSinceLastRun = System.currentTimeMillis();
        this.onTaskBegin();
    }

    public void update() {
        if (this.completed) {
            this.job.nextTask();
        } else if (this.deadline > 0L && System.currentTimeMillis() - this.timeSinceLastRun > this.deadline) {
            ModSimLoader.log.info(this.job.jobName+"运行 completeTask() 任务完成在 " + this.deadline + " ms");
            this.completeTask();
        } else {
            this.onUpdate();
        }
    }

    /**
     * 任务的每秒回调
     */
    public void onSecond() {
    }

    public abstract void onTaskBegin();

    public abstract void onUpdate();

    public abstract void onTaskComplete();
}
