package com.trhsy.sim.npcCode.job;

import com.trhsy.sim.loader.ItemLoader;
import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.NpcData;
import com.trhsy.sim.npcCode.task.*;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.npc.job
 * @ClassName: JobCheesemaker
 * @Description: 奶酪制造商
 * @date 2023/07/31 下午 3:45
 */
public class JobCheesemaker extends Job{
    //工作阶段
    public int cheesemakerStage = 0;
    public JobCheesemaker(NpcData folk, BlockPos pos, World world) {
        super(folk, pos, world);
        folk.holding = new ItemStack(ItemLoader.itemCheese);
        //奶酪酱
        this.jobName = new TextComponentTranslation("container.sim.Vocation21",new Object[0]).getUnformattedText();
        this.stage = -1;
    }
    @Override
    public void onUpdate() {
        super.onUpdate();
        try {
            if (this.atWork) {
                if (this.stage == -1) {
                    this.cheesemakerStage = 0;
                    this.stage = 0;
                } else if (this.cheesemakerStage == 0) {
                    this.cheesemakerStage = 1;
                    //去上班
                    this.addJobTask(new JobTaskIdle(this, 200L, new TextComponentTranslation("container.sim.job.builder_Arrived",new Object[0]).getUnformattedText()));
                }else if (this.cheesemakerStage == 1) {
                    List<ItemStack> colItems = new ArrayList();
                    //牛奶
                    colItems.add(new ItemStack(ItemLoader.itemBucketMilk, 16));
                    //向物流中心下单
                    this.addJobTask(new JobTaskRequestSupply(this, -1L, colItems));
                    this.cheesemakerStage = 2;
                } else if (this.cheesemakerStage == 2) {
                    //货由快递员直接放进店内箱子
                    this.cheesemakerStage = 3;
                }else if (this.cheesemakerStage == 3) {
                    //制作奶酪
                    this.addJobTask(new JobTaskCheesemaker(this, -1L, new TextComponentTranslation("container.sim.JobTaskCheesemaker1",new Object[0]).getUnformattedText()));
                    this.cheesemakerStage = 4;
                }else{
                    if (this.jobTasks.size() > 0&&this.currentTask==null) {
                        this.currentTask = (JobTask) this.jobTasks.get(0);
                        this.currentTask.begin();
                    }
                }
            }
            }catch (Exception e){
                StackTraceElement element = e.getStackTrace()[0];
                ModSimLoader.log.error("JobCheesemaker-onUpdate出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
            }
        }
    @Override
    public boolean isLogisticsSource() {
        return true;
    }

    @Override
    public String toString() {
        return this.jobName;
    }
}
