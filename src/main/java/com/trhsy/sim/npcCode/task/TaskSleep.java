//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.trhsy.sim.npcCode.task;

import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.NpcData;
import com.trhsy.sim.npcCode.V3;
import com.trhsy.sim.npcCode.build.Building;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;

/**
 * @Author fan
 * @Description //TODO 回家
 * @Date 16:01 2022/10/16
 * @Param
 * @return
 **/
public class TaskSleep extends Task {
    long fs_t;
    private boolean isAtHome = false;
    private boolean wasAtHome = false;
    public TaskSleep(NpcData folk, long ms, String statusText) {
        super(folk, ms);
        this.statusText = statusText;
        this.fs_t=0l;
    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 任务开始
     * @Date 10:11 2022/11/13
     * @Param []
     **/
    @Override
    public void onTaskBegin() {
        this.folk.stayPut = true;
        this.folk.holding = ItemStack.EMPTY;
        this.folk.setStatus(this.statusText);
        this.fs_t=0l;
        this.isAtHome = checkIfAtHome();
        // P11(S1): 只有真正到家才进入“睡眠”状态；在回家路上不标记为 sleeping。
        this.folk.isSleeping = this.isAtHome;
        // P3: 睡在住宅的居住点上（原来停在哪睡哪，容易睡在门口/墙边）
        if (this.isAtHome && this.folk.home != null) {
            this.folk.snapTo(findSleepSpot());
            this.folk.entity.getNavigator().clearPath();
        }
    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 更新任务
     * @Date 10:16 2022/11/13
     * @Param []
     **/
    @Override
    public void onUpdate() {
        this.wasAtHome = this.isAtHome;
        this.isAtHome = checkIfAtHome();
        // 状态变化时处理
        if (!this.wasAtHome && this.isAtHome) {
            this.folk.stayPut = true;
            this.folk.isSleeping = true;
            this.fs_t=System.currentTimeMillis();
            this.folk.setStatus(this.statusText);
            // P3: 到家的瞬间精确落到居住点再睡
            if (this.folk.home != null) {
                this.folk.snapTo(findSleepSpot());
                this.folk.entity.getNavigator().clearPath();
            }
            ModSimLoader.log.info("{} 已到家，开始睡觉", folk.getName());
        } else if ( !this.isAtHome) {
            this.folk.isSleeping = false;
            if (this.folk.home == null) {
                // P3: 无家可归时原地睡觉（原来直接 this.folk.home.livingXYZ 会 NPE，
                // 异常被上层吞掉后任务永远结束不了，NPC 半夜乱走）
                this.folk.stayPut = true;
                return;
            }
            ModSimLoader.log.warn("{} 不在家回家", folk.getName());
            String fs_n2 = new TextComponentTranslation("container.sim.folk_data_Going_home").getUnformattedText();
            this.folk.setStatus(fs_n2);
            if(!this.folk.forceMoveToXYZ(this.folk.home.livingXYZ)){
                V3 v3=new V3(this.folk.home.livingXYZ.x,this.folk.home.livingXYZ.y,this.folk.home.livingXYZ.z);
                this.folk.forceMoveToXYZs(v3);
            }
        }
        // 白天则完成任务
        if (ModSimLoader.isDayTime(this.folk.entity.world)) {
            this.completeTask();
        }
        /*
        if (this.folk.home != null&&!this.folk.isAtLocation(this.folk.home.livingXYZ)) {
            ModSimLoader.log.info(this.folk.getName()+"，有家位置["+this.folk.home.livingXYZ.toString()+"]");
            this.folk.stayPut = false;
//            if(!this.folk.forceMoveToXYZ(this.folk.home.livingXYZ)){
            V3 v3=new V3(this.folk.home.livingXYZ.x,this.folk.home.livingXYZ.y,this.folk.home.livingXYZ.z);
            this.folk.forceMoveToXYZs(v3);
//            }
            this.fs_t=System.currentTimeMillis();
        }else{
            this.folk.stayPut = true;
            this.folk.setStatus(this.statusText);
        }
        //白天则完成任务
        if (ModSimLoader.isDayTime(this.folk.entity.world)) {
            this.completeTask();
        }
*/
    }
    private boolean checkIfAtHome() {
        return this.folk.home != null && this.folk.home.livingXYZ != null &&
                this.folk.isAtLocation(this.folk.home.livingXYZ);
    }

    /**
     * P14(S1): 给同一住宅的住户分配不同的睡眠点。
     * 有床时优先用床；床被其他 sleeping NPC 占用时，在居住点周围找空位。
     * 注意：NPC 仍不会像玩家一样真正“躺”在床上，只是位置/状态分离。
     */
    private V3 findSleepSpot() {
        Building home = this.folk.home;
        if (home == null) {
            return this.folk.pos;
        }
        V3 base = home.livingXYZ;
        if (home.bed != null) {
            int dim = base != null ? base.dimension : this.folk.pos.dimension;
            V3 bedSpot = new V3(home.bed.getX() + 0.5D, home.bed.getY() + 0.55D, home.bed.getZ() + 0.5D, dim);
            if (!isSleepSpotOccupied(bedSpot, 1.5D)) {
                return bedSpot;
            }
        }
        if (base == null) {
            return this.folk.pos;
        }
        if (!isSleepSpotOccupied(base, 1.0D)) {
            return base;
        }
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4.0D;
            V3 candidate = new V3(base.x + Math.cos(angle) * 0.9D, base.y, base.z + Math.sin(angle) * 0.9D,
                    base.dimension);
            if (!isSleepSpotOccupied(candidate, 0.9D)) {
                return candidate;
            }
        }
        return base;
    }

    private boolean isSleepSpotOccupied(V3 spot, double radius) {
        if (spot == null || this.folk.home == null) {
            return false;
        }
        for (NpcData other : this.folk.home.occupants) {
            if (other == null || other == this.folk || !other.isSleeping || other.pos == null) {
                continue;
            }
            if (other.pos.getDistanceTo(spot) <= radius) {
                return true;
            }
        }
        return false;
    }
    @Override
    public void onTaskComplete() {
        this.folk.stayPut = false;
        // P11(S1): 白天/任务结束必须清除睡眠状态，否则 NpcData.onSecond 会一直认为它在睡。
        this.folk.isSleeping = false;
    }
}
