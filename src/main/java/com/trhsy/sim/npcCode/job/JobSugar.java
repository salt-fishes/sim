package com.trhsy.sim.npcCode.job;

import com.trhsy.sim.loader.ItemLoader;
import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.NpcData;
import com.trhsy.sim.npcCode.task.*;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.npc.job
 * @ClassName: JobSugar
 * @Description: 制糖师工作
 * @date 2023/5/9 11:47
 */
public class JobSugar extends Job{
    private int sugarcane;
    //工作阶段
    public int sugarStage = 0;
    public JobSugar(NpcData folk, BlockPos pos, World world) {
        super(folk, pos, world);
        folk.holding = new ItemStack(ItemLoader.tinSpade);
        //制糖师
        this.jobName = new TextComponentTranslation("container.sim.Vocation30",new Object[0]).getUnformattedText();
        this.stage = -1;
    }
    @Override
    public void onUpdate() {
        super.onUpdate();
        try {
            if (this.atWork) {
                if (this.stage == -1) {
                    this.sugarStage = 0;
                    this.stage = 0;
                }else if (this.sugarStage == 0) {
                    this.sugarStage = 1;
                }else if (this.sugarStage == 1) {
                    //打开烘焙工具
                    this.addJobTask(new JobTaskIdle(this, 200L, new TextComponentTranslation("container.sim.job_baker1",new Object[0]).getUnformattedText()));
                    this.sugarStage = 2;
                } else if (this.sugarStage == 2) {
                    List<ItemStack> colItems = new ArrayList();
                    //甘蔗
                    colItems.add(new ItemStack(Items.REEDS, 64));
                    //向物流中心下单
                    this.addJobTask(new JobTaskRequestSupply(this, -1L, colItems));
                    this.sugarStage = 3;
                } else if (this.sugarStage == 3) {
                    //货由快递员直接放进店内箱子
                    this.sugarStage = 4;
                } else if (this.sugarStage == 4) {

                    if (this.sugarcane >= 1 ) {
                        //蛋糕需要的食材
                        List<ItemStack> cakes = new CopyOnWriteArrayList<ItemStack>();
                        //甘蔗
                        cakes.add(new ItemStack(Items.REEDS, 1));
                        //糖
                        this.addJobTask(new JobTaskProduceItem(this, 60000L, Items.SUGAR, cakes, new TextComponentTranslation("container.sim.job.Baker_Baking_Sugar",new Object[0]).getUnformattedText()));
                    }
                    this.sugarStage = 5;
                } else if (this.sugarStage == 5) {
                    //售卖/关店
                    this.addJobTask(new JobTaskShopkeep(this, -1L, new TextComponentTranslation("container.sim.job.Baker_Baking_Sugar",new Object[0]).getUnformattedText(),false));
                    this.sugarStage = 6;
                } else if (this.sugarStage == 6) {
                    //在去工作途中，并且已经到了工作位置则更新状态
                    if (this.atWork && this.folk.isAtLocation(this.workPlace) && this.currentTask == null && this.jobTasks.size() > 0) {
                        if (this.jobTasks.size() > 0) {
                            this.currentTask = (JobTask) this.jobTasks.get(0);
                            this.currentTask.begin();
                        }
                    } else if ((this.sugarcane > 1)  && this.folk.getStatusText().contains(new TextComponentTranslation("container.sim.job_task_Selling",new Object[0]).getUnformattedText())) {
                        this.sugarStage = 4;
                        if(this.currentTask!=null) {
                            this.currentTask.completeTask();
                            this.jobTasks.clear();
                        }
                    }
                }else{
                    this.sugarStage = 0;
                    this.jobTasks.clear();
                }
                this.sugarcane = 0;
                List<IInventory> iterator = this.findJobChests(5);
                for (IInventory inv : iterator) {
                    for (int i = 0; i < inv.getSizeInventory(); ++i) {
                        ItemStack slot = inv.getStackInSlot(i);
                        if (slot != null) {
                            if (slot.isItemEqual(new ItemStack(Items.REEDS))) {
                                this.sugarcane += slot.getCount();
                            }
                        }
                    }
                }
            }
        }catch (Exception e){
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("JobSugar-onUpdate出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
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
