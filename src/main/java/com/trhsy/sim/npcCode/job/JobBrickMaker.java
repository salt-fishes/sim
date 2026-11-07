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
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName JobBrickMaker
 * @Description todo 板砖工匠
 * @Author TRHSY
 * @Date 2023/6/416:29
 **/
public class JobBrickMaker extends Job{
    //工作阶段
    public int brickMakerStage = 0;
    public V3 v3;
    public JobBrickMaker(NpcData folk, BlockPos pos, World world) {
        super(folk, pos, world);
        try {
            folk.holding = new ItemStack(Blocks.CLAY);
            //板砖工匠
            this.jobName = new TextComponentTranslation("container.sim.Vocation25",new Object[0]).getUnformattedText();
            this.v3=new V3(pos.getX(),pos.getY(),pos.getZ());
        }catch (Exception e){
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("JobBrickMaker出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }

    }
    @Override
    public void onUpdate() {
        super.onUpdate();
        try {
            if (this.atWork) {
                if (this.stage == -1) {
                    this.brickMakerStage = 0;
                    this.stage = 0;
                }else if (this.brickMakerStage == 0) {
                    this.brickMakerStage = 1;
                    //去上班
                    this.addJobTask(new JobTaskIdle(this, 200L, new TextComponentTranslation("container.sim.job.builder_Arrived",new Object[0]).getUnformattedText()));
                } else if (this.brickMakerStage == 1) {
                    //要收集的物品
                    List<Block> colItems = new ArrayList();
                    //黏土块
                    colItems.add(Blocks.CLAY);
                    //寻找黏土
                    //去寻找
                    this.folk.setStatus(new TextComponentTranslation("container.sim.GOTOCLAYBLOCK",new Object[0]).getUnformattedText());
                    this.addJobTask(new JobTaskSearchForBlock(this, 5000L, colItems,this.v3, ConfigLoader.configLumberArea,false));
                    this.brickMakerStage = 2;
                }else if(this.brickMakerStage == 2){
                    //使用熔炉
                    //黏土球
                    this.addJobTask(new JobTaskUseFurnace(this, 120000L, new ItemStack(Items.CLAY_BALL)));
                    this.brickMakerStage = 3;
                }else{
                    if (this.jobTasks.size() > 0&&this.currentTask==null) {
                        this.currentTask = (JobTask) this.jobTasks.get(0);
                        this.currentTask.begin();
                    }
                }
            }
        }catch (Exception e){
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("JobBrickMaker-onUpdate出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }

    }
    @Override
    public String toString() {
        return new TextComponentTranslation("container.sim.Vocation25",new Object[0]).getUnformattedText();
    }
}
