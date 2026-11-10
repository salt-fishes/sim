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
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 酒保（P9 修复）。
 *
 * <p>原实现的问题：
 * <ul>
 *   <li>配方列表 {@code cakes} 是空的，材料却 `colItems.add(...)` 加进了采集清单，
 *       每天还重复添加 → 清单无限膨胀；</li>
 *   <li>产物写成不存在的"啤酒"（ItemLoader 里没有该物品），实际什么都做不出来。</li>
 * </ul>
 * 现在：采购小麦 → 卸货 → 烤面包（3 小麦/个）→ 售卖；不再采购玻璃（无用途、无映射）。
 */
public class JobBartender extends Job{
    //工作阶段
    public int bartenderStage = 0;
    //要收集的物品
    public List<ItemStack> colItems = new ArrayList();
    public JobBartender(NpcData folk, BlockPos pos, World world) {
        super(folk, pos, world);
        try {
            folk.holding = new ItemStack(ItemLoader.tinAxe);
            this.jobName = new TextComponentTranslation("container.sim.Vocation37",new Object[0]).getUnformattedText();
            //小麦
            this.colItems.add(new ItemStack(Items.WHEAT, 16));
        }catch (Exception e){
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("JobBartender出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }
    @Override
    public void onUpdate() {
        super.onUpdate();
        try {
            if (this.atWork) {
                if (this.stage == -1) {
                    this.bartenderStage = 0;
                    this.stage = 0;
                }  else if (this.bartenderStage == 0) {
                    this.bartenderStage = 1;
                    //去上班
                    this.addJobTask(new JobTaskIdle(this, 200L, new TextComponentTranslation("container.sim.job.builder_Arrived",new Object[0]).getUnformattedText()));
                } else if (this.bartenderStage == 1) {
                    //向物流中心下单
                    this.addJobTask(new JobTaskRequestSupply(this, -1L, colItems));
                    this.bartenderStage = 2;
                } else if (this.bartenderStage == 2) {
                    //货由快递员直接放进店内箱子
                    this.bartenderStage = 3;
                } else if (this.bartenderStage == 3) {
                    // P9: 烤面包（3 小麦/个）。需求必须放进独立的列表，不能再往 colItems 里塞。
                    List<ItemStack> breadReq = new CopyOnWriteArrayList<ItemStack>();
                    breadReq.add(new ItemStack(Items.WHEAT, 3));
                    this.addJobTask(new JobTaskProduceItem(this, 60000L, Items.BREAD, breadReq, new TextComponentTranslation("container.sim.job.Baker_Baking",new Object[0]).getUnformattedText()));
                    this.bartenderStage = 4;
                }else if(this.bartenderStage == 4){
                    //售卖/关店（下酒面包）
                    this.addJobTask(new JobTaskShopkeep(this, -1L, new TextComponentTranslation("container.sim.job.Baker_bread",new Object[0]).getUnformattedText(),true));
                    this.bartenderStage = 5;
                }else{
                    if (this.jobTasks.size() > 0&&this.currentTask==null) {
                        this.currentTask = (JobTask) this.jobTasks.get(0);
                        this.currentTask.begin();
                    }
                }
            }
        }catch (Exception e){
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("JobBartender-onUpdate出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }
    @Override
    public String toString() {
        return this.jobName;
    }
}
