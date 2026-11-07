//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.trhsy.sim.npcCode.task;

import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.NpcData;
import com.trhsy.sim.npcCode.V3;
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
        if (this.isAtHome && this.folk.home != null && this.folk.home.livingXYZ != null) {
            this.folk.snapTo(this.folk.home.livingXYZ);
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
            if (this.folk.home != null && this.folk.home.livingXYZ != null) {
                this.folk.snapTo(this.folk.home.livingXYZ);
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
        return this.folk.home != null &&
                this.folk.isAtLocation(this.folk.home.livingXYZ);
    }
    @Override
    public void onTaskComplete() {
        this.folk.stayPut = false;
        // P11(S1): 白天/任务结束必须清除睡眠状态，否则 NpcData.onSecond 会一直认为它在睡。
        this.folk.isSleeping = false;
    }
}
