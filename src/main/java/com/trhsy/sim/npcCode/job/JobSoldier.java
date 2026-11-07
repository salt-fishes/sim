package com.trhsy.sim.npcCode.job;

import com.trhsy.sim.loader.ItemLoader;
import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.NpcData;
import com.trhsy.sim.npcCode.task.JobTask;
import com.trhsy.sim.npcCode.task.JobTaskIdle;
import com.trhsy.sim.npcCode.task.JobTaskPatrol;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/**
 * @ClassName JobSoldier
 * @Description todo 士兵
 * @Author TRHSY
 * @Date 2023/4/1517:34
 **/
public class JobSoldier extends Job{
    //工作阶段
    public int soldierStage = 0;
    /** P4: 装备检查/回血计时 */
    private transient long equipCheckTime = 0L;
    private transient long healTime = 0L;
    public JobSoldier(NpcData folk, BlockPos pos, World world) {
        super(folk, pos, world);
        folk.holding = new ItemStack(ItemLoader.tinSword);
        //士兵
        this.jobName = new TextComponentTranslation("container.sim.Vocation7",new Object[0]).getUnformattedText();

    }

    /**
     * P4: 士兵生存能力。
     * 原来 NPC 裸装 40 血、无护甲、不回血，近战打怪（还会引来仇恨）很容易死。
     * 现在：穿全套锡甲（缺哪件补哪件）、脱战每 3 秒回 1 点血。
     */
    private void ensureEquipment() {
        try {
            if (this.folk.entity == null || System.currentTimeMillis() - this.equipCheckTime < 5000L) {
                return;
            }
            this.equipCheckTime = System.currentTimeMillis();
            equip(net.minecraft.inventory.EntityEquipmentSlot.HEAD, ItemLoader.tinHelmet);
            equip(net.minecraft.inventory.EntityEquipmentSlot.CHEST, ItemLoader.tinChestplate);
            equip(net.minecraft.inventory.EntityEquipmentSlot.LEGS, ItemLoader.tinLeggings);
            equip(net.minecraft.inventory.EntityEquipmentSlot.FEET, ItemLoader.tinBoots);
            if (this.folk.entity.getItemStackFromSlot(net.minecraft.inventory.EntityEquipmentSlot.MAINHAND).isEmpty()) {
                this.folk.holding = new ItemStack(ItemLoader.tinSword);
            }
            // P4: 驻区（参照铁傀儡的"家"）——把工作岗位设为家的中心，半径与巡逻任务一致
            if (this.folk.entity instanceof net.minecraft.entity.EntityCreature) {
                ((net.minecraft.entity.EntityCreature) this.folk.entity)
                        .setHomePosAndDistance(this.workPlace.toBlockPos(),
                                com.trhsy.sim.npcCode.task.JobTaskPatrol.PATROL_RADIUS);
            }
        } catch (Exception e) {
            ModSimLoader.log.error("JobSoldier-ensureEquipment出错了：" + e.getMessage());
        }
    }

    private void equip(net.minecraft.inventory.EntityEquipmentSlot slot, net.minecraft.item.Item item) {
        if (this.folk.entity != null && this.folk.entity.getItemStackFromSlot(slot).isEmpty()) {
            this.folk.entity.setItemStackToSlot(slot, new ItemStack(item));
        }
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        try {
            // 装备与脱战回血（不论是否已到岗都要保证）
            ensureEquipment();
            if (this.folk.entity != null
                    && this.folk.entity.getHealth() < this.folk.entity.getMaxHealth()
                    && System.currentTimeMillis() - this.healTime > 3000L) {
                this.healTime = System.currentTimeMillis();
                this.folk.entity.heal(1.0F);
            }
            if (this.atWork) {
                if (this.stage == -1) {
                    this.stage = 0;
                    this.soldierStage=0;
                } else if (this.soldierStage == 0) {
                    this.soldierStage = 1;
                    //去上班
                    this.addJobTask(new JobTaskIdle(this, 200L, new TextComponentTranslation("container.sim.job.builder_Arrived",new Object[0]).getUnformattedText()));
                } else if (this.soldierStage == 1) {
                    //士兵
                    this.addJobTask(new JobTaskPatrol(this, -1L));
                    this.soldierStage = 2;
                }else{
                    if (this.jobTasks.size() > 0&&this.currentTask==null) {
                        this.currentTask = (JobTask) this.jobTasks.get(0);
                        this.currentTask.begin();
                    }
                }
            }
        }catch (Exception e){
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("JobSoldier-onUpdate出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }

    }
    @Override
    public String toString() {
        return this.jobName;
    }
}
