package com.trhsy.sim.npcCode.task;

import com.trhsy.sim.loader.ItemLoader;
import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.V3;
import com.trhsy.sim.npcCode.block.FarmBox;
import com.trhsy.sim.npcCode.build.Building;
import com.trhsy.sim.npcCode.job.Job;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.ArrayList;
import java.util.List;

/**
 * 收集任务（P8/P9 重构）。
 *
 * <p>流程：<br>
 * 1. begin：清空并重建目的地（幂等，重复 begin 不会翻倍）；<br>
 * 2. 到点判定：水平 2.5 格 / 垂直 3 格；无路可走但已在 4 格内也算到达；<br>
 * 3. 到达后停 1 秒，按清单额度从目的地 5 格内的箱子里取货（副本入包，不多拿）；<br>
 * 4. 取完立即去下一个目的地；全部走完（即使没取全）立刻回岗位。
 *
 * <p>防卡死三层保护：<br>
 * - 移动指令最多每秒重发一次（原来每 tick 重发 A*，还会冲掉无进展看门狗的计时）；<br>
 * - 单个目的地最多 90 秒，超时直接跳过；<br>
 * - 整个任务最多 10 分钟，超时强制回岗。
 */
public class JobTaskCollectItems extends JobTask {
    /**
     * 要收集的物品
     **/
    public List<ItemStack> collectionItems = new ArrayList();
    /**
     * 目的地
     **/
    public List<V3> destinations = new ArrayList();
    /**
     * 当前目的地
     **/
    V3 currentDestination = null;
    /**
     * P8: 用显式下标推进目的地（不再用 indexOf，重复坐标会乒乓死循环）。
     */
    int currentIndex = -1;
    /**
     * 正在转到目的地
     */
    boolean isGoingToDestination = false;
    /**
     * 到达后的时间
     **/
    transient long timeSinceArrival;
    /**
     * P8: 单次取货的任务级超时（10 分钟），任何情况下都不会无限收集。
     */
    private transient long taskBeginTime = 0L;
    private static final long COLLECT_TASK_TIMEOUT_MS = 10 * 60 * 1000L;
    /** P9: 到达后的短暂停留，然后立即取货并离开。 */
    private static final long COLLECT_WAIT_MS = 1000L;
    /** P9: 到点判定半径（水平 / 垂直）。 */
    private static final double ARRIVE_RADIUS_XZ = 2.5D;
    private static final double ARRIVE_RADIUS_Y = 3.0D;
    /** P9: 无路可走时的"就近算到达"半径。 */
    private static final double NEAR_RADIUS_XZ = 4.0D;
    private static final double NEAR_RADIUS_Y = 4.0D;
    /** P9: 单个目的地最多耗多久，超时直接跳过去下一个。 */
    private static final long DESTINATION_TIMEOUT_MS = 90 * 1000L;
    /** P9: 移动指令重发间隔（原来每 tick 重发一次 A*）。 */
    private static final long MOVE_REISSUE_MS = 1000L;
    private transient long lastMoveReissue = 0L;
    /** P9: 当前目的地的开始时间（单点超时用）。 */
    private transient long timeSinceDestinationStart = 0L;
    /** P9: 每种物品最多去几个供货点（默认 2；材料多的店可以只去 1 个缩短路线）。 */
    private int maxSuppliersPerType = 2;

    /**
     * 要收集的物品
     *
     * @param j
     * @param ms
     * @param collectionItems
     */
    public JobTaskCollectItems(Job j, long ms, List<ItemStack> collectionItems) {
        this(j, ms, collectionItems, 2);
    }

    /**
     * @param maxSuppliersPerType 每种物品最多去的供货点数量（&gt;0）
     */
    public JobTaskCollectItems(Job j, long ms, List<ItemStack> collectionItems, int maxSuppliersPerType) {
        super(j, (long) ms);
        this.collectionItems = collectionItems;
        if (maxSuppliersPerType > 0) {
            this.maxSuppliersPerType = maxSuppliersPerType;
        }
    }

    /**
     * 开始任务，任务分配。P8: 必须幂等——重复 begin 时先清空旧目的地与进度。
     */
    @Override
    public void onTaskBegin() {
        this.destinations.clear();
        this.currentDestination = null;
        this.currentIndex = -1;
        this.timeSinceArrival = 0L;
        this.isGoingToDestination = false;
        this.taskBeginTime = System.currentTimeMillis();
        this.timeSinceDestinationStart = this.taskBeginTime;
        this.lastMoveReissue = 0L;

        //找不到任何可收集的建筑物
        if (this.collectionItems.size() < 1) {
            this.failTask(this.job.folk.getName() + " (" + this.job.jobName + ") " + new TextComponentTranslation("container.sim.job_task_could",new Object[0]).getUnformattedText());
        } else {

            for (int i = 0; i < this.collectionItems.size(); ++i) {
                Item colItem = ((ItemStack) this.collectionItems.get(i)).getItem();
                //猪排
                if (colItem == Items.PORKCHOP) {
                    //养猪场
                    this.addDestination(new TextComponentTranslation("container.sim.Vocation13",new Object[0]).getUnformattedText());
                    //兔肉
                } else if (colItem == Items.RABBIT) {
                    //养兔场
                    this.addDestination(new TextComponentTranslation("container.sim.Vocation29",new Object[0]).getUnformattedText());
                    //牛肉
                } else if (colItem == Items.BEEF) {
                    //养牛场
                    this.addDestination(new TextComponentTranslation("container.sim.Vocation12",new Object[0]).getUnformattedText());
                    //鸡肉
                } else if (colItem == Items.CHICKEN) {
                    //养鸡场
                    this.addDestination(new TextComponentTranslation("container.sim.Vocation14",new Object[0]).getUnformattedText());
                    //羊肉
                } else if (colItem == Items.MUTTON) {
                    //养羊场
                    this.addDestination(new TextComponentTranslation("container.sim.Vocation28",new Object[0]).getUnformattedText());
                    //鸡蛋
                } else if (colItem == Items.EGG) {
                    //鸡蛋农场
                    this.addDestination(new TextComponentTranslation("container.sim.Vocation3",new Object[0]).getUnformattedText());
                    //牛奶
                } else if (colItem == ItemLoader.itemBucketMilk) {
                    //奶牛场
                    this.addDestination(new TextComponentTranslation("container.sim.Vocation20",new Object[0]).getUnformattedText());
                    //鱼
                } else if (colItem == Items.FISH) {
                    //渔场
                    this.addDestination(new TextComponentTranslation("container.sim.Vocation18",new Object[0]).getUnformattedText());
                    //胡萝卜
                } else if (colItem == Items.CARROT) {
                    this.addDestination("farmer:" + new TextComponentTranslation("container.sim.FarmType1",new Object[0]).getUnformattedText());
                    //马铃薯
                } else if (colItem == Items.POTATO) {
                    this.addDestination("farmer:" + new TextComponentTranslation("container.sim.FarmType3",new Object[0]).getUnformattedText());
                    //甜菜根
                } else if (colItem == Items.BEETROOT) {
                    this.addDestination("farmer:" + new TextComponentTranslation("container.sim.FarmType6",new Object[0]).getUnformattedText());
                    //可可豆
                } else if (colItem == Items.DYE) {
                    this.addDestination("farmer:" + new TextComponentTranslation("container.sim.FarmType10",new Object[0]).getUnformattedText());
                    //西瓜
                } else if (colItem == Items.MELON) {
                    this.addDestination("farmer:" + new TextComponentTranslation("container.sim.FarmType2",new Object[0]).getUnformattedText());
                    //小麦
                } else if (colItem == Items.WHEAT) {
                    this.addDestination("farmer:" + new TextComponentTranslation("container.sim.FarmType5",new Object[0]).getUnformattedText());
                    //甘蔗
                } else if (colItem == Items.REEDS) {
                    this.addDestination("farmer:" + new TextComponentTranslation("container.sim.FarmType7",new Object[0]).getUnformattedText());
                    //南瓜
                } else if (colItem == new ItemStack(Blocks.PUMPKIN).getItem()) {
                    this.addDestination("farmer:" + new TextComponentTranslation("container.sim.FarmType4",new Object[0]).getUnformattedText());
                    //糖
                }else if (colItem == Items.SUGAR) {
                    this.addDestination( new TextComponentTranslation("container.sim.Vocation30",new Object[0]).getUnformattedText());
                    //奶酪
                }else if (colItem == ItemLoader.itemCheese) {
                    // P9: 用稳定职业 ID（奶酪工坊 Vocation21）匹配建筑，
                    // 原来用物品显示名匹配，工厂 jobType 对不上就永远收不到奶酪。
                    this.addDestination( new TextComponentTranslation("container.sim.Vocation21",new Object[0]).getUnformattedText());
                //面包
                }if (colItem == Items.BREAD) {
                    this.addDestination( new TextComponentTranslation("container.sim.Vocation6",new Object[0]).getUnformattedText());
                }
            }

            if (this.destinations.size() < 1) {
                //找不到任何可收集的建筑物
                this.failTask(this.job.folk.getName() + " (" + this.job.jobName + ") " + new TextComponentTranslation("container.sim.job_task_could",new Object[0]).getUnformattedText());
            } else {
                this.currentIndex = 0;
                this.currentDestination = this.destinations.get(0);
            }
        }
    }

    /**
     * P8: 同坐标+同维度的目的地只加一次（防止重复目的地互相"乒乓"）。
     */
    private void addDestinationV3(V3 v) {
        if (v == null) {
            return;
        }
        for (V3 d : this.destinations) {
            if (d != null && d.equals(v)) {
                return;
            }
        }
        this.destinations.add(v);
    }

    /**
     * 添加目的地
     *
     * @param jobt
     */
    private void addDestination(String jobt) {
        //如果是农场作物
        if (jobt.contains("farmer:")) {
            String fType = jobt.split(":")[1];
            // P4: 取最近的农场供货点（默认 2 个；有库存就取，不要求一定有雇员）
            List<FarmBox> farms = ModSimLoader.getClosestFarm(this.job.workPlace, fType);
            int n = Math.min(this.maxSuppliersPerType, farms.size());
            for (int i = 0; i < n; i++) {
                FarmBox f = farms.get(i);
                this.addDestinationV3(new V3(f.loc.x, f.loc.y + 1, f.loc.z));
            }
        } else {
            List<Building> buildings = ModSimLoader.getClosestBuildingByJob(jobt, this.job.workPlace);
            if (buildings != null && buildings.size() > 0) {
                // P4: 取最近的供货建筑（默认 2 个；不再要求有住户）
                int n = Math.min(this.maxSuppliersPerType, buildings.size());
                for (int i = 0; i < n; i++) {
                    Building building = buildings.get(i);
                    if (building.controlXYZ != null) {
                        this.addDestinationV3(new V3(building.controlXYZ.x, building.controlXYZ.y + 1,
                                building.controlXYZ.z, building.dimension));
                    }
                }
            }
        }
        if (this.destinations.size() == 0) {
            String s1="";
            for (int i = 0; i < collectionItems.size(); i++) {
                ItemStack itemStack = collectionItems.get(i);
                s1+=itemStack.getDisplayName()+",";
            }
            String s2=new TextComponentTranslation("container.sim.job_coll_items",new Object[0]).getUnformattedText();
            ModSimLoader.sendChat(this.job.jobName+":["+s1+"]"+s2);
        }
    }

    @Override
    public void onUpdate() {
        // P8: 任务级超时兜底——无论遇到什么异常情况，取货都不会无限进行。
        if (this.taskBeginTime > 0L && System.currentTimeMillis() - this.taskBeginTime > COLLECT_TASK_TIMEOUT_MS) {
            ModSimLoader.log.warn("{} 取货超过10分钟，强制结束回岗", this.job.jobName);
            this.folk.forceMoveToXYZ(this.job.workPlace);
            this.completeTask();
            return;
        }
        // 没有目的地：直接完成
        if (this.destinations.isEmpty()) {
            this.completeTask();
            return;
        }
        if (this.currentDestination == null) {
            this.currentIndex = 0;
            this.currentDestination = this.destinations.get(0);
            this.timeSinceDestinationStart = System.currentTimeMillis();
        }

        // P9: 单点超时——某个目的地拖太久（寻路失败/被挡住），直接跳过，
        // 避免"卡在一个点永远收集材料"。
        if (System.currentTimeMillis() - this.timeSinceDestinationStart > DESTINATION_TIMEOUT_MS) {
            ModSimLoader.log.warn("{} 在目的地 {} 超时，跳过去下一个", this.job.jobName, this.currentDestination);
            this.advanceToNextDestination();
            return;
        }

        // P9: 到点判定——水平 2.5 / 垂直 3；无路可走但已在 4 格内也算到达。
        boolean arrived = this.job.folk.isAtDestination(this.currentDestination, ARRIVE_RADIUS_XZ, ARRIVE_RADIUS_Y);
        if (!arrived && this.job.folk.entity != null
                && !this.job.folk.entity.isMoving()
                && this.job.folk.entity.getNavigator().getPath() == null
                && this.job.folk.isAtDestination(this.currentDestination, NEAR_RADIUS_XZ, NEAR_RADIUS_Y)) {
            arrived = true;
        }

        if (!arrived) {
            this.isGoingToDestination = true;
            //收集材料
            this.folk.setStatus(new TextComponentTranslation("container.sim.job_task_Collecting_materials",new Object[0]).getUnformattedText());
            // P9: 移动指令最多每秒重发一次（原来每 tick 重发 A*，还会冲掉看门狗计时）
            long now = System.currentTimeMillis();
            if (now - this.lastMoveReissue > MOVE_REISSUE_MS
                    || this.job.folk.entity == null
                    || this.job.folk.entity.getNavigator().getPath() == null) {
                this.lastMoveReissue = now;
                this.folk.forceMoveToXYZ(this.currentDestination);
            }
        } else {
            //到达后的时间
            if (this.timeSinceArrival == 0L) {
                this.timeSinceArrival = System.currentTimeMillis();
            }

            if (System.currentTimeMillis() - this.timeSinceArrival > COLLECT_WAIT_MS) {
                ModSimLoader.log.debug("开始收集");
                // P9: 按清单额度取货——每种物品最多取清单数量，多个槽位也不会超拿；
                // 放进背包的是副本，不会把任务清单里的 ItemStack 共享出去。
                List<ItemStack> quota = new ArrayList();
                for (ItemStack c : this.collectionItems) {
                    quota.add(c.copy());
                }
                List<IInventory> chests = this.job.inventoriesFindClosest(this.currentDestination, 5);
                for (IInventory inv : chests) {
                    for (int i = 0; i < inv.getSizeInventory(); i++) {
                        ItemStack invItemStack = inv.getStackInSlot(i);
                        if (invItemStack == null || invItemStack.isEmpty()) {
                            continue;
                        }
                        for (ItemStack want : quota) {
                            if (want.getCount() <= 0 || !invItemStack.isItemEqual(want)) {
                                continue;
                            }
                            int take = Math.min(want.getCount(), invItemStack.getCount());
                            ItemStack moved = invItemStack.copy();
                            moved.setCount(take);
                            this.job.folk.inventory.add(moved);
                            inv.decrStackSize(i, take);
                            want.setCount(want.getCount() - take);
                            break;
                        }
                    }
                }

                // P9: 取完立即去下一个目的地（不在原地逗留）
                this.advanceToNextDestination();
            }
        }
    }

    /** P9: 推进到下一个目的地；没有下一个就回岗并完成任务。 */
    private void advanceToNextDestination() {
        this.timeSinceArrival = 0L;
        this.currentIndex++;
        if (this.currentIndex >= 0 && this.currentIndex < this.destinations.size()) {
            ModSimLoader.log.info("找到下一个目的地");
            this.currentDestination = this.destinations.get(this.currentIndex);
            this.timeSinceDestinationStart = System.currentTimeMillis();
            this.lastMoveReissue = 0L;
            this.folk.forceMoveToXYZ(this.currentDestination);
        } else {
            ModSimLoader.log.info("返回工作岗位");
            this.folk.setStatus(new TextComponentTranslation("container.sim.job_task_Returning_to_work",new Object[0]).getUnformattedText());
            this.folk.forceMoveToXYZ(this.job.workPlace);
            this.completeTask();
        }
    }

    @Override
    public void onTaskComplete() {
        this.destinations.clear();
    }
}
