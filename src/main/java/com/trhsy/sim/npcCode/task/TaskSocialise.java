//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.trhsy.sim.npcCode.task;

import com.trhsy.sim.npcCode.NpcData;
import com.trhsy.sim.npcCode.V3;
import com.trhsy.sim.npcCode.build.Building;
import net.minecraft.util.text.TextComponentTranslation;

/**
 * @Author fan
 * @Description //TODO 社交
 * @Date 16:01 2022/10/16
 * @Param
 * @return
 **/
public class TaskSocialise extends Task {
	public NpcData other;
	public Building building;
	public boolean host;
	long socialUpdate = 0L;

	public TaskSocialise(NpcData folk, long ms, NpcData other, Building b, boolean host) {
		super(folk, ms);
		this.other = other;
		this.building = b;
		this.host = host;
	}

	@Override
    public void onTaskBegin() {
		this.folk.stayPut = false;
		// P3: 先判空再解引用（原来先访问 other.forename，other 为 null 时 NPE）
		if (this.other == null) {
			this.failTask(new TextComponentTranslation("container.sim.folk_other_was_null",new Object[0]).getUnformattedText());
			return;
		}
		this.folk.setStatus(new TextComponentTranslation("container.sim.folk_data_Hanging",new Object[0]).getUnformattedText() + this.other.forename);
	}

	@Override
	public void onUpdate() {
		if (this.building == null) {
			this.completeTask();
			return;
		}
		if (!this.folk.isAtBuilding(this.building)) {
			// P3: 去社交建筑（原来错用了 folk.home.livingXYZ，无家 NPC 直接 NPE）
			V3 v3 = this.building.livingXYZ != null ? this.building.livingXYZ : this.building.controlXYZ;
			if (v3 != null) {
				this.folk.forceMoveToXYZ(v3);
			}
		} else if (System.currentTimeMillis() - this.socialUpdate > 10000L && this.host) {
			this.socialUpdate = System.currentTimeMillis();
			this.folk.adjustRelationship(this.other, 1);
		}
	}

	@Override
	public void onTaskComplete() {
		this.folk.stayPut = false;
	}
}
