package com.trhsy.sim.entity.ai;

import com.trhsy.sim.entity.EntityNpc;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.util.math.Vec3d;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.entity.ai
 * @ClassName: FolkAIWander
 * @Description: 实体NPC的Ai
 * @date 2023/11/20 下午 4:23
 */
public class FolkAIWander extends EntityAIWander {

    protected EntityNpc folk;
    protected boolean mustUpdate;
    protected int executionChance;
    protected double x;
    protected double y;
    protected double z;
    protected double speed;

    public FolkAIWander(EntityNpc entity, double speedIn) {
        super(entity, speedIn);
        this.folk = entity;
        this.speed = speedIn;
        this.executionChance = 100;
    }

    @Override
    protected Vec3d getPosition() {
//        if(!this.folk.theData.stayPut) {
            return RandomPositionGenerator.findRandomTarget(this.entity, 10, 7);
//        }else{
//            return this.folk.getPositionVector();
//        }
    }

    @Override
    public boolean shouldExecute() {
        if (this.folk.theData == null) {
            return false;
        }
        // P10(S0): 只有真正空闲的 NPC 才闲逛。
        // 上班途中/在工作/有任务/被要求站定时都不允许和职业导航抢路径。
        if (this.folk.theData.stayPut
                || this.folk.theData.currentTask != null
                || (this.folk.theData.job != null
                    && (this.folk.theData.job.onWayToWork || this.folk.theData.job.atWork))) {
            return false;
        }
        if (!this.mustUpdate) {
                //获得空闲时间
                if (this.entity.getIdleTime() >= 100) {
                    return false;
                }
                //下一个
                if (this.entity.getRNG().nextInt(this.executionChance) != 0) {
                    return false;
                }
            }

            Vec3d vec3d = this.getPosition();
            if (vec3d == null) {
                return false;
            } else {
                this.x = vec3d.x;
                this.y = vec3d.y;
                this.z = vec3d.z;
                this.mustUpdate = false;
                return true;
            }
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (this.folk.theData != null
                && (this.folk.theData.stayPut || this.folk.theData.currentTask != null)) {
            return false;
        }
        return super.shouldContinueExecuting();
    }

    @Override
    public void startExecuting()
    {
        this.entity.getNavigator().tryMoveToXYZ(this.x, this.y, this.z, this.speed);
    }
    /**
     * Makes task to bypass chance
     */
    @Override
    public void makeUpdate()
    {
        this.mustUpdate = true;
    }

    /**
     * Changes task random possibility for execution
     */
    @Override
    public void setExecutionChance(int newchance)
    {
        this.executionChance = newchance;
    }
}
