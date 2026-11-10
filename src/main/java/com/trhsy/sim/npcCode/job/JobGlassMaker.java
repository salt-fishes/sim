package com.trhsy.sim.npcCode.job;

import com.trhsy.sim.loader.ConfigLoader;
import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.NpcData;
import com.trhsy.sim.npcCode.V3;
import com.trhsy.sim.npcCode.task.JobTask;
import com.trhsy.sim.npcCode.task.JobTaskIdle;
import com.trhsy.sim.npcCode.task.JobTaskSearchForBlock;
import com.trhsy.sim.npcCode.task.JobTaskUseFurnace;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName JobGlassMaker
 * @Description todo 玻璃制造商
 * @Author TRHSY
 * @Date 2023/6/416:29
 **/
public class JobGlassMaker extends Job{
    public V3 v3;
    //工作阶段
    public int glassMakerStage = 0;
    public JobGlassMaker(NpcData folk, BlockPos pos, World world) {
        super(folk, pos, world);
        try {
//手持玻璃
            folk.holding = new ItemStack(Blocks.SAND);
            //玻璃制造商
            this.jobName = new TextComponentTranslation("container.sim.Vocation17",new Object[0]).getUnformattedText();
            this.v3=new V3(pos.getX(),pos.getY(),pos.getZ());
        }catch (Exception e){
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("JobGlassMaker出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }

    }
    @Override
    public void onUpdate() {
        super.onUpdate();
        try {
            if (this.atWork) {
                if (this.stage == -1) {
                    this.glassMakerStage = 0;
                    this.stage = 0;
                } else if (this.glassMakerStage == 0) {
                    this.glassMakerStage = 1;
                    //去上班
                    this.addJobTask(new JobTaskIdle(this, 200L, new TextComponentTranslation("container.sim.job.builder_Arrived",new Object[0]).getUnformattedText()));
                } else if (this.glassMakerStage == 1) {
                    //要收集的物品
                    List<Block> colItems = new ArrayList();
                    //沙子
                    colItems.add(Blocks.SAND);
                    //去寻找 沙子
                    this.folk.setStatus(new TextComponentTranslation("container.sim.GOTOCLAYBLOCK",new Object[0]).getUnformattedText());
                    this.addJobTask(new JobTaskSearchForBlock(this, -1L, colItems,this.v3, ConfigLoader.configLumberArea,false));
                    this.glassMakerStage = 2;
                }else if(this.glassMakerStage == 2){
                    this.addJobTask(new JobTaskUseFurnace(this, 120000, new ItemStack(Blocks.SAND)));
                    this.glassMakerStage = 3;
                }else{
                    if (this.jobTasks.size() > 0&&this.currentTask==null) {
                        this.currentTask = (JobTask) this.jobTasks.get(0);
                        this.currentTask.begin();
                    }
                }
            }
        }catch (Exception e){
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("JobGlassMaker-onUpdate出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }

    }
    @Override
    public boolean isLogisticsSource() {
        return true;
    }

    @Override
    public String toString() {
        return new TextComponentTranslation("container.sim.Vocation17",new Object[0]).getUnformattedText();
    }
}
