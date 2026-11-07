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

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.npc.job
 * @ClassName: JobMcDonald
 * @Description: 麦当劳
 * @date 2023/07/31 下午 3:10
 */
public class JobMcDonald extends Job{
    //工作阶段
    public int mcDonaldStage = 0;
    //熟猪排 熟羊肉 熟兔肉 牛排 熟鸡肉 熟鲑鱼 熟鱼
    private int porkchop, mutton, rabbit, steak,chicken,salmon,fish;
    // 上次统计物品的时间戳（单位：ticks，1 tick = 1/20秒）
    private long lastItemStatsTime = 0;
    // 统计间隔（五分钟 = 5 * 60秒 = 300秒 = 300 * 20 ticks = 6000 ticks）
    private static final long STATS_INTERVAL = 1000;
    public JobMcDonald(NpcData folk, BlockPos pos, World world) {
        super(folk, pos, world);
        folk.holding = new ItemStack(ItemLoader.tinSpade);
        //麦当劳
        this.jobName = new TextComponentTranslation("container.sim.Vocation35",new Object[0]).getUnformattedText();
        this.stage = -1;
    }
    @Override
    public void onUpdate() {
        super.onUpdate();
        try {
            if (this.atWork) {
                if (this.stage == -1) {
                    this.mcDonaldStage = 0;
                    this.stage = 0;
                }else if (this.mcDonaldStage == 0) {
                    this.mcDonaldStage = 1;
                    //去上班
                    this.addJobTask(new JobTaskIdle(this, 200L, new TextComponentTranslation("container.sim.job.builder_Arrived",new Object[0]).getUnformattedText()));
                }else if (this.mcDonaldStage == 1) {
                    //打开烘焙工具
                    this.addJobTask(new JobTaskIdle(this, 200L, new TextComponentTranslation("container.sim.job_baker1",new Object[0]).getUnformattedText()));
                    this.mcDonaldStage = 2;
                } else if (this.mcDonaldStage == 2) {
                    List<ItemStack> colItems = new ArrayList();
                    //熟猪排
                    colItems.add(new ItemStack(Items.PORKCHOP, 16));
                    //熟羊肉
                    colItems.add(new ItemStack(Items.MUTTON, 16));
                    //熟兔肉
                    colItems.add(new ItemStack(Items.RABBIT, 16));
                    //牛排
                    colItems.add(new ItemStack(Items.BEEF, 16));
                    //牛排
                    colItems.add(new ItemStack(Items.CHICKEN, 16));
                    //鲑鱼
                    colItems.add(new ItemStack(Items.FISH, 16,1));
                    //生鱼
                    colItems.add(new ItemStack(Items.FISH, 16));
                    //收集
                    this.addJobTask(new JobTaskCollectItems(this, -1L, colItems, 1));
                    this.mcDonaldStage = 3;
                } else if (this.mcDonaldStage == 3) {
                    List<ItemStack> colItems = new ArrayList();
                    //猪排
                    colItems.add(new ItemStack(Items.PORKCHOP, 16));
                    //羊肉
                    colItems.add(new ItemStack(Items.MUTTON, 16));
                    //兔肉
                    colItems.add(new ItemStack(Items.RABBIT, 16));
                    //牛排
                    colItems.add(new ItemStack(Items.BEEF, 16));
                    //鸡肉
                    colItems.add(new ItemStack(Items.CHICKEN, 16));
                    //鲑鱼
                    colItems.add(new ItemStack(Items.FISH, 16,1));
                    //生鱼
                    colItems.add(new ItemStack(Items.FISH, 16));
                    this.addJobTask(new JobTaskUnloadItems(this, -1L, colItems));
                    this.mcDonaldStage = 4;
                } else if (this.mcDonaldStage == 4) {
                    initInventory();
                    if (this.steak >= 1) {
                        //牛肉
                        //熟牛排
                        this.addJobTask(new JobTaskProduceItem(this, 60000L, Items.COOKED_BEEF, new ItemStack(Items.BEEF, 1), new TextComponentTranslation("container.sim.job.Baker_Baking",new Object[0]).getUnformattedText()));
                    }
                    if (this.porkchop >= 1) {
                        //需要的食材
                        //猪排
                        //熟猪排
                        this.addJobTask(new JobTaskProduceItem(this, 60000L, Items.COOKED_PORKCHOP, new ItemStack(Items.PORKCHOP, 1), new TextComponentTranslation("container.sim.job.Baker_Baking",new Object[0]).getUnformattedText()));
                    }
                    if (this.mutton > 0) {
                        //烘烤 羊排
                        this.addJobTask(new JobTaskProduceItem(this, 60000L, Items.COOKED_MUTTON, new ItemStack(Items.MUTTON, 1), new TextComponentTranslation("container.sim.job.Baker_Baking",new Object[0]).getUnformattedText()));
                    }

                    if (this.rabbit > 0) {
                        //烘烤 熟兔肉
                        this.addJobTask(new JobTaskProduceItem(this, 60000L, Items.COOKED_RABBIT, new ItemStack(Items.RABBIT, 1), new TextComponentTranslation("container.sim.job.Baker_Baking",new Object[0]).getUnformattedText()));
                    }

                    if (this.chicken > 0) {
                        //烘烤 鸡肉
                        this.addJobTask(new JobTaskProduceItem(this, 60000L, Items.COOKED_CHICKEN, new ItemStack(Items.CHICKEN, 1), new TextComponentTranslation("container.sim.job.Baker_Baking",new Object[0]).getUnformattedText()));
                    }

                    if (this.salmon > 0) {
                        //烘烤 鲑鱼（1 条生鲑鱼 → 1 条熟鲑鱼，meta 1）
                        this.addJobTask(new JobTaskProduceItem(this, 60000L, Items.COOKED_FISH, 1, new ItemStack(Items.FISH, 1, 1), new TextComponentTranslation("container.sim.job.Baker_Baking",new Object[0]).getUnformattedText()));
                    }

                    if (this.fish > 0) {
                        //烘烤 生鱼（1 条 → 1 条熟鱼）
                        this.addJobTask(new JobTaskProduceItem(this, 60000L, Items.COOKED_FISH, 0, new ItemStack(Items.FISH, 1), new TextComponentTranslation("container.sim.job.Baker_Baking",new Object[0]).getUnformattedText()));
                    }
                    this.mcDonaldStage = 5;
                } else if (this.mcDonaldStage == 5) {
                    //售卖/关店
                    this.addJobTask(new JobTaskShopkeep(this, -1L, new TextComponentTranslation("container.sim.job.Grocer1",new Object[0]).getUnformattedText(),true));
                    this.mcDonaldStage = 6;
                } else if (this.mcDonaldStage == 6) {
                    //在去工作途中，并且已经到了工作位置则更新状态
                    if (this.atWork && this.folk.isAtLocation(this.workPlace) && this.currentTask == null && this.jobTasks.size() > 0) {
                        if (this.jobTasks.size() > 0) {
                            this.currentTask = (JobTask) this.jobTasks.get(0);
                            this.currentTask.begin();
                        }
                    } /*else if ((this.steak > 1 ||this.porkchop > 1 ||this.mutton > 1||this.rabbit > 1||this.chicken > 1||this.salmon > 1||this.fish > 1) && this.folk.getStatusText().contains(new TextComponentTranslation("container.sim.job_task_Selling",new Object[0]).getUnformattedText())) {
                        this.mcDonaldStage = 4;
                        this.currentTask.completeTask();
                        this.jobTasks.clear();
                    }*/
                }/*else{
                    this.mcDonaldStage = 0;
                    this.jobTasks.clear();
                }
                this.steak = 0;
                this.porkchop = 0;
                this.mutton = 0;
                this.rabbit = 0;
                this.chicken = 0;
                this.salmon = 0;
                this.fish = 0;
                List<IInventory> iterator = this.findJobChests(5);
                for (IInventory inv : iterator) {
                    for (int i = 0; i < inv.getSizeInventory(); ++i) {
                        ItemStack slot = inv.getStackInSlot(i);
                        if (slot != null) {
                            if (slot.isItemEqual(new ItemStack(Items.BEEF))) {
                                this.steak += slot.getCount();
                            } else if (slot.isItemEqual(new ItemStack(Items.PORKCHOP))) {
                                this.porkchop += slot.getCount();
                            } else if (slot.isItemEqual(new ItemStack(Items.MUTTON))) {
                                this.mutton += slot.getCount();
                            } else if (slot.isItemEqual(new ItemStack(Items.RABBIT))) {
                                this.rabbit += slot.getCount();
                            }else if (slot.isItemEqual(new ItemStack(Items.CHICKEN))) {
                                this.chicken += slot.getCount();
                            }else if (slot.isItemEqual(new ItemStack(Items.COOKED_FISH,1,1))) {
                                this.salmon += slot.getCount();
                            }else if (slot.isItemEqual(new ItemStack(Items.COOKED_FISH))) {
                                this.fish += slot.getCount();
                            }
                        }
                    }
                }*/
                // ---------------------- 物品统计优化 ----------------------
                long currentTime = this.jobWorld.getWorldTime(); // 获取当前游戏时间（ticks）
                // 检查是否达到统计间隔（五分钟）
                if (currentTime - lastItemStatsTime >= STATS_INTERVAL) {
                    initInventory();
                    // 如果还有原料且处于售卖阶段，重新进入制作阶段
                    if (hasEnoughMaterials() && this.mcDonaldStage >= 5) {
                        this.mcDonaldStage = 4;
                        this.jobTasks.clear(); // 清空现有任务，重新生成制作任务
                    }
                    // 更新上次统计时间
                    this.lastItemStatsTime = currentTime;
                }
            }
        }catch (Exception e){
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("JobMcDonald-onUpdate出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }
    /**
     * 检查是否有足够原料继续制作
     */
    private boolean hasEnoughMaterials() {
        return (this.steak > 1 ||this.porkchop > 1 ||this.mutton > 1||this.rabbit > 1||this.chicken > 1||this.salmon > 1||this.fish > 1) && this.folk.getStatusText().contains(new TextComponentTranslation("container.sim.job_task_Selling",new Object[0]).getUnformattedText());
    }
    public void initInventory(){
        porkchop=0; mutton=0; rabbit=0; steak=0;chicken=0;salmon=0;fish=0;
        List<IInventory> iterator = this.findJobChests(5);
        for (IInventory inv : iterator) {
            for (int i = 0; i < inv.getSizeInventory(); ++i) {
                ItemStack slot = inv.getStackInSlot(i);
                if (slot != null) {
                    if (slot.isItemEqual(new ItemStack(Items.BEEF))) {
                        this.steak += slot.getCount();
                    } else if (slot.isItemEqual(new ItemStack(Items.PORKCHOP))) {
                        this.porkchop += slot.getCount();
                    } else if (slot.isItemEqual(new ItemStack(Items.MUTTON))) {
                        this.mutton += slot.getCount();
                    } else if (slot.isItemEqual(new ItemStack(Items.RABBIT))) {
                        this.rabbit += slot.getCount();
                    }else if (slot.isItemEqual(new ItemStack(Items.CHICKEN))) {
                        this.chicken += slot.getCount();
                    }else if (slot.isItemEqual(new ItemStack(Items.FISH,1,1))) {
                        this.salmon += slot.getCount();
                    }else if (slot.isItemEqual(new ItemStack(Items.FISH))) {
                        this.fish += slot.getCount();
                    }
                }
            }
        }
    }
    @Override
    public String toString() {
        return this.jobName;
    }
}