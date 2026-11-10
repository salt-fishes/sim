package com.trhsy.sim.npcCode.job;

import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.loader.SoundRegistry;
import com.trhsy.sim.npcCode.NpcData;
import com.trhsy.sim.npcCode.V3;
import com.trhsy.sim.npcCode.economy.Economy;
import com.trhsy.sim.npcCode.logistics.LogisticsService;
import com.trhsy.sim.npcCode.logistics.SupplyOrder;
import com.trhsy.sim.npcCode.store.SimStore;
import com.trhsy.sim.npcCode.task.JobTask;
import com.trhsy.sim.util.PricesForBlocks;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.npc
 * @ClassName: Job
 * @Description: 工作
 * @date 2022/10/13 17:38
 */
public abstract class Job {
    //工作名称
    public String jobName = "null";
    //NPC数据
    public NpcData folk = null;
    //工作地址
    public V3 workPlace = null;
    //工作世界
    public World jobWorld = null;
    //工作阶段
    public int stage = -1;
    //物品
    public ItemStack stuckItem = null;
    //箱子是否已满
    public boolean chestsFull = false;
    //工作任务
    public List<JobTask> jobTasks = new CopyOnWriteArrayList();
    //工作箱子
    public List<IInventory> jobChests = new CopyOnWriteArrayList();
    //最近任务
    public JobTask currentTask;
    //是否在上班途中
    public boolean onWayToWork;
    //在工作
    public boolean atWork;
    //支付
    public float pay;
    //P2: 去工作目标的最近一次重发时间（原实现每 tick 重发一次寻路）
    private transient long lastWorkMoveTime = 0L;
    //去工作状态起始时间（只用于日志/重试，不会触发传送）
    private transient long workTravelStartTime = 0L;
    //连续不可达后，暂停重新规划的时间
    private transient long commuteRetryAt = 0L;
    private transient boolean commuteUnreachableLogged = false;
    /** P4: 每分钟工资（玩家支付给 NPC，用于经济闭环）。 */
    private static final float WAGE_PER_MINUTE = 0.2F;

    public Job(NpcData folk, BlockPos pos, World world) {
        this.folk = folk;
//        folk.respawn(world, pos);
        this.workPlace = new V3(pos, world.provider.getDimension());
        this.jobWorld = world;
    }

    public Job(NpcData folk, V3 pos, World world) {
        this.folk = folk;
//        folk.respawn(world, pos.toBlockPos());
        this.workPlace = new V3(pos.x, pos.y, pos.z, world.provider.getDimension());
        this.jobWorld = world;
    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 下一个任务
     * @Date 10:33 2022/10/21
     * @Param []
     **/
    public void nextTask() {
        try {
            // P3: 队列语义。完成的任务立即移出列表，jobTasks 不再无限增长；
            // stage 只在"任务耗尽"时置 -1，各职业的 stage==-1 分支会重建本轮任务。
            if (this.currentTask != null) {
                this.jobTasks.remove(this.currentTask);
                this.currentTask = null;
            }
            if (!this.jobTasks.isEmpty()) {
                // 注意：推进时**不要**改 stage —— 有些职业（如快递员）把 stage 当作
                // 自己的状态机，只有在队列耗尽时才置 -1 让它们重建。
                this.currentTask = (JobTask) this.jobTasks.get(0);
                this.currentTask.begin();
            } else {
                this.stage = -1;
                this.onTasksExhausted();
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("Job-nextTask出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    /**
     * 本轮任务耗尽（任务队列为空）。
     * 默认什么也不做：各职业在 onUpdate 的 {@code stage == -1} 分支重建下一轮任务。
     */
    protected void onTasksExhausted() {
    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 工作更新回调
     * @Date 10:42 2022/10/21
     * @Param []
     **/
    public void onUpdate() {
        try {
            // 检查 NPC 数据和实体是否为空
            if (isNpcValid()) {
                //处理工作状态
                handleWorkStatus();
                //处理当前任务
                handleCurrentTask();
                // 处理去工作的逻辑
                handleGoingToWork();
                //处理到达工作地点的逻辑
                handleArrival();
            }
        } catch (Exception e) {
            logUpdateError(e);
        }
    }

    /**
     * 检查 NPC 数据和实体是否有效
     *
     * @return 如果有效返回 true，否则返回 false
     */
    private boolean isNpcValid() {
        return this.folk != null && this.folk.entity != null;
    }

    /**
     * 处理工作状态
     */
    private void handleWorkStatus() {
        if (this.folk.shouldWork()) {//是否应该工作
//            this.folk.stayPut = true;
        } else if (this.atWork) {
            clearWorkStatus();
        }
    }

    /**
     * 清除工作状态
     */
    private void clearWorkStatus() {
        this.folk.clearStatus();
        this.folk.stayPut = false;
        this.atWork = false;
        this.onWayToWork = false;
        this.workTravelStartTime = 0L;
        this.commuteRetryAt = 0L;
        this.commuteUnreachableLogged = false;
        this.currentTask = null;
    }

    /**
     * 处理当前任务
     */
    private void handleCurrentTask() {
        if (this.currentTask != null) {
            this.currentTask.update();
        }
    }

    /**
     * 处理去工作的逻辑
     */
    private void handleGoingToWork() {
        if (this.folk.shouldWork() && !this.atWork && !this.folk.entity.world.isRemote) {
            long now = System.currentTimeMillis();
            if (!this.onWayToWork) {
                this.onWayToWork = true;
                this.workTravelStartTime = now;
                this.commuteRetryAt = 0L;
                this.commuteUnreachableLogged = false;
            }
            this.folk.stayPut = false;

            if (now < this.commuteRetryAt) {
                return;
            }

            // 统一步行控制器已经判定这个工作点不可达：保持等待，不再每 5 秒
            // 传到工作点。控制器每 30 秒会自动重试一次，这里只负责在连续
            // 失败后暂停一段时间并记录一次日志。
            if (this.folk.isNavigationUnreachable(this.workPlace)) {
                if (this.workTravelStartTime <= 0L) {
                    this.workTravelStartTime = now;
                }
                if (now - this.workTravelStartTime > 45000L) {
                    this.commuteRetryAt = now + 60000L;
                    this.workTravelStartTime = 0L;
                    if (!this.commuteUnreachableLogged) {
                        this.commuteUnreachableLogged = true;
                        ModSimLoader.log.warn("NPC {} 暂时无法步行到达工作地点 {}，将在一分钟后重试",
                                this.folk.getName(), this.workPlace);
                    }
                }
                return;
            }

            // 正常最多每 2 秒重发一次移动目标；NpcNavigator 会复用同目标的分段路径。
            if (now - this.lastWorkMoveTime < 2000L) {
                return;
            }
            this.lastWorkMoveTime = now;
            this.folk.setStatus(new TextComponentTranslation("container.sim.folk_data_Going_work", new Object[0]).getUnformattedText());
            V3 v3 = new V3(this.workPlace.x, this.workPlace.y, this.workPlace.z, this.workPlace.dimension);
            this.folk.forceMoveToXYZ(v3);
        }
    }

    /**
     * 处理到达工作地点的逻辑
     */
    private void handleArrival() {
        if (this.onWayToWork && this.folk.isAtLocation(this.workPlace)) {
            this.atWork = true;
            this.onWayToWork = false;
            this.workTravelStartTime = 0L;
            this.commuteRetryAt = 0L;
            this.commuteUnreachableLogged = false;
            this.onArrive();
            this.folk.stayPut = false;
            this.folk.stopNavigationPath();
            // P8: 删掉了这里对 currentTask.onTaskComplete() 的直接调用。它只触发
            // 任务回调却不标记完成，会把正在执行的任务破坏成"永远收集/空指针卡死"。
            // 任务的状态由各任务自己的 onUpdate/update() 推进，不在这里干预。
        }
    }

    /**
     * 记录更新方法中的错误信息
     *
     * @param e 捕获的异常
     */
    private void logUpdateError(Exception e) {
        StackTraceElement element = e.getStackTrace()[0];
        ModSimLoader.log.error("job-onUpdate出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        e.printStackTrace();
    }

    /**
     * 查找最近的箱子
     *
     * @param startXYZ
     * @param searchDistance
     * @return
     */
    public List<IInventory> inventoriesFindClosest(V3 startXYZ, int searchDistance) {
        // 指定泛型为 IInventory
        List<IInventory> ret = new CopyOnWriteArrayList();

        try {
            World world = ((net.minecraft.entity.Entity) folk.entity).world;
            // P10(S0): 起点未加载时不做任何扫描，避免 getTileEntity 隐式加载区块。
            BlockPos center = startXYZ.toBlockPos();
            if (!world.isBlockLoaded(center)) {
                return ret;
            }
            TileEntity te = world.getTileEntity(center);
            if (isAllowedJobContainer(te)) {
                ret.add((IInventory) te);
            }
            // 遍历搜索范围内的所有方块。
            // P4: 只检查"外壳"（切比雪夫距离 == d），跳过内部已检查过的位置：
            // 覆盖率不变，getTileEntity 调用次数从 O(d^3) 降到 O(6d^2)。
            for (int d = 1; d < searchDistance; ++d) {
                for (int yo = -d; yo <= d; ++yo) {
                    for (int xo = -d; xo <= d; ++xo) {
                        for (int zo = -d; zo <= d; ++zo) {
                            int chebyshev = Math.max(Math.abs(xo), Math.max(Math.abs(yo), Math.abs(zo)));
                            if (chebyshev != d) {
                                continue;
                            }
                            int sx = (int) (Math.round(startXYZ.x) + (long) xo);
                            int sy = (int) (Math.round(startXYZ.y) + (long) yo);
                            int sz = (int) (Math.round(startXYZ.z) + (long) zo);
                            BlockPos pos = new BlockPos(sx, sy, sz);
                            if (!world.isBlockLoaded(pos)) {
                                continue;
                            }
                            te = world.getTileEntity(pos);
                            // P10(S0): 只认原版箱子/陷阱箱，不再把任意 IInventory
                            // （玩家箱子、熔炉、模组机器）当成 NPC 的工作箱。
                            if (isAllowedJobContainer(te)
                                    && ((IInventory) te).getSizeInventory() > 9
                                    && !this.alreadyGotChest(ret, (IInventory) te)) {
                                ret.add((IInventory) te);
                            }
                        }
                    }
                }
            }

            return ret;
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("job-inventoriesFindClosest出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
            return ret;
        }
    }

    /** P10(S0): 工作箱白名单。原版箱子/陷阱箱都是 TileEntityChest。 */
    private boolean isAllowedJobContainer(TileEntity te) {
        return te instanceof net.minecraft.tileentity.TileEntityChest;
    }

    /** P10(S0): 缓存的箱位必须仍然存在、未被移除/替换。 */
    private boolean isInventoryValid(IInventory inv) {
        if (!(inv instanceof TileEntity)) {
            return false;
        }
        TileEntity te = (TileEntity) inv;
        World w = te.getWorld();
        return w != null && !te.isInvalid() && w.getTileEntity(te.getPos()) == te;
    }

    /**
     * 已经获得箱子
     *
     * @param chests
     * @param chest
     * @return
     */
    private boolean alreadyGotChest(List<IInventory> chests, IInventory chest) {
//        boolean ret = false;
//        Iterator var4 = chests.iterator();
//        try {
//            while (var4.hasNext()) {
//                IInventory ch = (IInventory) var4.next();
//                if (ch.toString().contentEquals(chest.toString())) {
//                    ret = true;
//                    break;
//                }
//            }
//        } catch (Exception e) {
//            StackTraceElement element = e.getStackTrace()[0];
//            ModSimLoader.log.error("job-alreadyGotChest出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
//        }
//        return ret;
        try {
            // 遍历所有已有的箱子
            for (IInventory ch : chests) {
                // 比较箱子的位置
                if (isSameInventory(ch, chest)) {
                    return true;
                }
            }
        } catch (NullPointerException e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("job-alreadyGotChest出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
        return false;
    }

    /**
     * 比较两个 IInventory 对象是否相同
     *
     * @param inv1
     * @param inv2
     * @return
     */
    private boolean isSameInventory(IInventory inv1, IInventory inv2) {
        if (inv1 instanceof TileEntity && inv2 instanceof TileEntity) {
            TileEntity te1 = (TileEntity) inv1;
            TileEntity te2 = (TileEntity) inv2;
            return te1.getPos().equals(te2.getPos());
        }
        return false;
    }

    /**
     * 每秒
     */
    public void onSecond() {
        try {
            if (this.currentTask != null) {
                this.currentTask.onSecond();
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("job-onSecond出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }


    }

    /**
     * 每分钟
     */
    public void onMinute() {
        try {
            // P4: 定价/工资系统暂缓（先保证 NPC 能自己吃到饭）。
            // 以后启用：在岗 NPC 每分钟 +WAGE_PER_MINUTE（玩家支付）。
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("job-onMinute出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    /**
     * @return java.util.List<net.minecraft.inventory.IInventory>
     * @Author fan
     * @Description //TODO 查找工作箱子
     * @Date 21:21 2022/12/8
     * @Param [radius]
     **/
    public List<IInventory> findJobChests(int radius) {
        try {
            this.jobChests.clear();
            List<IInventory> inventoriesFindClosest = this.inventoriesFindClosest(this.workPlace, radius);
            for (IInventory i : inventoriesFindClosest) {
                jobChests.add(i);
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("job-findJobChests出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
        return this.jobChests;
    }

    /**
     * 将物品放置到箱子里
     *
     * @param item
     * @return
     */
    public boolean placeInJobChest(ItemStack item) {
        try {
            if (this.jobChests.size() > 0) {
                for (IInventory chest : this.jobChests) {
                    if (this.placeInJobChest(chest, item)) {
                        return true;
                    }
                }
            } else {
                List<IInventory> inventoriesFindClosest = this.inventoriesFindClosest(this.workPlace, 5);
                for (IInventory i : inventoriesFindClosest) {
                    if (this.placeInJobChest(i, item)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("placeInJobChest出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
        return false;
    }

    /**
     * 将物品放置到箱子里
     *
     * @param chest
     * @param item
     * @return
     */
    public boolean placeInJobChest(IInventory chest, ItemStack item) {
        if (item == null || item.isEmpty()) {
            return true;
        }
        return placeInJobChestPartial(chest, item) >= item.getCount();
    }

    /** P4: 返回实际放进去的件数（调用方按实结算，不再"布尔 + 整组扣款"）。 */
    public int placeInJobChestPartial(IInventory chest, ItemStack item) {
        if (chest == null || item == null || item.isEmpty()) {
            return 0;
        }
        // P10(S0): 箱子被拆/被替换后，缓存的 IInventory 不能再写入。
        if (!isInventoryValid(chest)) {
            return 0;
        }
        try {
            IItemHandler handler = chest instanceof IItemHandler ? (IItemHandler) chest : new InvWrapper(chest);
            ItemStack remaining = ItemHandlerHelper.insertItemStacked(handler, item.copy(), false);
            int inserted = item.getCount() - (remaining == null ? 0 : remaining.getCount());
            this.stuckItem = inserted < item.getCount() ? remaining : null;
            // 记录“这个工作地点产出过什么”。只有生产职业才登记，
            // 商店/餐厅/商人不会把自己的货架当成物流产出点。
            if (inserted > 0 && this.isLogisticsSource()) {
                SimStore store = SimStore.get(this.jobWorld);
                if (store != null) {
                    store.recordLogisticsOutput(this.workPlace, this.jobName, item);
                }
            }
            return inserted;
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("job-placeInJobChestPartial出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
            return 0;
        }
    }

    /** P4: 依次尝试所有工作箱子，返回实际放入总数。 */
    public int placeInJobChestPartial(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return 0;
        }
        int total = 0;
        try {
            if (this.jobChests.size() > 0) {
                this.jobChests.removeIf(chest -> !isInventoryValid(chest));
            }
            List<IInventory> targets = this.jobChests.size() > 0
                    ? this.jobChests : this.inventoriesFindClosest(this.workPlace, 5);
            ItemStack remaining = item.copy();
            for (IInventory chest : targets) {
                if (remaining.isEmpty()) {
                    break;
                }
                int put = placeInJobChestPartial(chest, remaining);
                if (put > 0) {
                    remaining.setCount(remaining.getCount() - put);
                    total += put;
                }
            }
            if (!remaining.isEmpty()) {
                this.stuckItem = remaining;
            }
            return total;
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("job-placeInJobChestPartial(list)出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
            return total;
        }
    }

    /**
     * 将一些物品/任何物品从一组箱子中转移到npc的库存中
     *
     * @param  箱子在哪里得到它们
     * @return
     */
    public boolean inventoriesTransferToFolk(ItemStack item) {
        // P4: 只搬真正匹配的那一种，并用 Forge 物品能力的返回值判断是否搬动。
        // 原实现 `chestStack.isItemEqual(item);` 的结果被丢弃，导致整箱搬空。
        if (item == null || item.isEmpty()) {
            return false;
        }
        boolean ret = false;
        try {
            findJobChests(5);
            for (IInventory chest : this.jobChests) {
                if (!isInventoryValid(chest)) {
                    continue;
                }
                IItemHandler handler = chest instanceof IItemHandler ? (IItemHandler) chest : new InvWrapper(chest);
                for (int slot = 0; slot < handler.getSlots(); slot++) {
                    ItemStack inSlot = handler.getStackInSlot(slot);
                    if (inSlot.isEmpty() || !ItemHandlerHelper.canItemStacksStack(inSlot, item)) {
                        continue;
                    }
                    ItemStack extracted = handler.extractItem(slot, inSlot.getCount(), false);
                    if (!extracted.isEmpty()) {
                        this.folk.inventory.add(extracted);
                        ret = true;
                    }
                }
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("将一些物品/任何物品从一组箱子中转移到人们的库存中出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
        return ret;
    }
    public boolean inventoriesTransferToFolks(List<IInventory> iInventories) {
        boolean ret = false;
        if (iInventories == null) {
            return false;
        }
        try {
            for (IInventory chest : iInventories) {
                if (!isInventoryValid(chest)) {
                    continue;
                }
                IItemHandler handler = chest instanceof IItemHandler ? (IItemHandler) chest : new InvWrapper(chest);
                for (int slot = 0; slot < handler.getSlots(); slot++) {
                    ItemStack inSlot = handler.getStackInSlot(slot);
                    if (inSlot.isEmpty()) {
                        continue;
                    }
                    ItemStack extracted = handler.extractItem(slot, inSlot.getCount(), false);
                    if (!extracted.isEmpty()) {
                        this.folk.inventory.add(extracted);
                        ret = true;
                    }
                }
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("将一些物品/任何物品从一组箱子中转移到人们的库存中出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
        return ret;
    }
    /**
     * @return int
     * @Author fan
     * @Description //TODO 投喂，喂养npc
     * @Date 17:44 2022/12/10
     * @Param []
     **/
    public int feedFolks() {
        int fedFolks = 0;
        float revenue = 0.0F;
        long feedStart = System.currentTimeMillis();
        try {
            // P20: 工作箱只扫描一次，不能在每个 NPC 循环里重复扫描 9x9x9 体积。
            List<IInventory> shopChests = this.inventoriesFindClosest(this.workPlace, 5);

            for (NpcData fd : ModSimLoader.folks) {

                if (fd.hunger >= 10) {
                    ModSimLoader.log.info("NPC {} 已吃饱，跳过", fd.getName());
                    //已吃饱
                    continue;
                }

                //在修 店铺喂养npc
                //当前Npc 还要多少食物才能吃饱
                int totalNeed  = 10 - (int) Math.ceil(fd.hunger);
                // 核心限制：单次最多喂一半（向上取整，至少1点），确保喂不饱
                int maxFeedThisTime = Math.max(1, (int) Math.ceil(totalNeed / 2.0));
                int needFood = Math.min(totalNeed, maxFeedThisTime); // 本次实际可喂的上限

                if (needFood <= 0) {

                    continue;
                }
                // 遍历最近的箱子
                for (IInventory chest : shopChests) {

                    for (int i = 0; i < chest.getSizeInventory() && needFood > 0; ++i) {

                        ItemStack is = chest.getStackInSlot(i);
                        if (is == null || is.isEmpty()) {
                            continue;
                        }
                        String itemName = is.getItem().getUnlocalizedName(is);
                        // 输出当前物品信息（调试用，避免每槽都拼字符串）
                        if (ModSimLoader.log.isDebugEnabled()) {
                            ModSimLoader.log.debug("检查物品：{}，类：{}", is.getUnlocalizedName(), is.getItem().getClass().getName());
                        }
// 判断是否为食物（包括蛋糕）
                        if (is.getItem() instanceof ItemFood) {
                            ItemFood itemFood = (ItemFood) is.getItem();
                            int healAmount = itemFood.getHealAmount(is);
                            // 防御性检查：避免无效食物（如饱食度为0）
                            if (healAmount <= 0) {
                                continue;
                            }
                            // P10(S0): 恢复量按“实际消耗的件数”计算，且不超过箱子里的数量。
                            // 原实现只按 needFood 算 maxCanFeed，1 个面包也能喂出多份饱食度（食物复制）。
                            int healPerItem = Math.max(1, healAmount / 2);
                            int maxByNeed = Math.max(1, (int) Math.ceil(needFood / (double) healPerItem));
                            int maxCanFeed = Math.min(Math.min(is.getCount(), is.getMaxStackSize()), maxByNeed);
                            if (maxCanFeed <= 0) {
                                continue;
                            }
                            ItemStack removed = chest.decrStackSize(i, maxCanFeed);
                            int actual = (removed == null || removed.isEmpty()) ? 0 : removed.getCount();
                            if (actual <= 0) {
                                continue;
                            }
                            // P20: 商店喂食收费；余额不足按慈善免费。
                            fd.hunger = Math.min(10.0D, fd.hunger + (double) healPerItem * actual);
                            revenue += Economy.chargeShopMeal(fd, actual, this.jobName);
                            fedFolks++;
                            needFood = 10 - (int) Math.ceil(fd.hunger); // 重新计算剩余需求
                            ModSimLoader.log.info("喂食{}个{}给NPC{}，当前饥饿值：{}",
                                    actual, itemFood.getUnlocalizedName(), fd.getName(), fd.hunger);
                        }// 2. 单独处理蛋糕（假设蛋糕物品类为ItemCake，根据实际类名修改）
                        else if (itemName.contains("cake") || itemName.contains("block.cake")) { // 替换为你的蛋糕物品类名
                            // 假设一块蛋糕提供4点饱食度（可根据需求调整）
                            int cakeHeal = 4;
                            int maxByNeed = Math.max(1, (int) Math.ceil(needFood / (double) cakeHeal));
                            int maxCanFeed = Math.min(is.getCount(), maxByNeed);
                            ItemStack removed = chest.decrStackSize(i, maxCanFeed);
                            int actual = (removed == null || removed.isEmpty()) ? 0 : removed.getCount();
                            if (actual <= 0) {
                                continue;
                            }
                            // P20: 商店喂食收费；余额不足按慈善免费。
                            fd.hunger = Math.min(10.0D, fd.hunger + (double) cakeHeal * actual);
                            revenue += Economy.chargeShopMeal(fd, actual, this.jobName);
                            fedFolks++;
                            needFood = 10 - (int) Math.ceil(fd.hunger);
                            ModSimLoader.log.info("喂食{}个蛋糕给NPC{}，当前饥饿值：{}",
                                    actual, fd.getName(), fd.hunger);
                        }




                        //fd.hunger ++;


                    }
                    if (needFood <= 0) {
                        ModSimLoader.log.info("NPC {} 已吃饱，停止当前箱子检查", fd.getName());
                        break;
                    }

                }

            }
            if (revenue > 0.0F) {
                ModSimLoader.log.info("{} 商店喂食结算：{} 人次，收入 {}，耗时 {} ms",
                        this.jobName, fedFolks, String.format("%.2f", revenue),
                        System.currentTimeMillis() - feedStart);
            }
            return fedFolks;
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("job-feedFolks出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
            return fedFolks;
        }
    }

    public boolean isNearBlock(Block block) {
        return false;
    }

    public void addJobTask(JobTask jtask) {
        this.jobTasks.add(jtask);
    }

    /**
     * 向物流中心下单（Beta12）。订单只记录缺料，不再让老板自己跑供货点。
     *
     * @return 已有/新建的订单；没有物流中心或全部物品被禁用时返回 null。
     */
    public SupplyOrder requestSupply(List<ItemStack> items) {
        return LogisticsService.requestSupply(this, items);
    }

    /** 统计工作点 5 格内箱子里某种物品的数量。 */
    public int countSupply(ItemStack wanted) {
        if (wanted == null || wanted.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (IInventory inv : this.findJobChests(5)) {
            if (inv == null) {
                continue;
            }
            for (int slot = 0; slot < inv.getSizeInventory(); slot++) {
                ItemStack stack = inv.getStackInSlot(slot);
                if (stack != null && !stack.isEmpty() && stack.isItemEqual(wanted)
                        && ItemStack.areItemStackTagsEqual(stack, wanted)) {
                    count += stack.getCount();
                }
            }
        }
        return count;
    }

    /** 计算清单里还缺多少（箱子里已有的会扣掉）。 */
    public List<ItemStack> missingSupplies(List<ItemStack> wanted) {
        List<ItemStack> missing = new ArrayList<>();
        if (wanted == null) {
            return missing;
        }
        for (ItemStack stack : wanted) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            int have = this.countSupply(stack);
            if (have < stack.getCount()) {
                ItemStack copy = stack.copy();
                copy.setCount(stack.getCount() - have);
                missing.add(copy);
            }
        }
        return missing;
    }

    /** 箱子里是否已经有清单中的任意一种物品。 */
    public boolean hasSupplies(List<ItemStack> wanted) {
        if (wanted == null) {
            return false;
        }
        for (ItemStack stack : wanted) {
            if (stack != null && !stack.isEmpty() && this.countSupply(stack) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 这个职业的工作箱是否属于“物流产出点”。
     *
     * <p>只有生产职业返回 true；商店、餐厅、商人、快递员返回 false，
     * 避免快递员把要卖的商品或买来的建材当成品搬回物流中心。</p>
     */
    public boolean isLogisticsSource() {
        return false;
    }

    /**
     * 将选定的玩家库存出售给商家
     */
    public void sellStuff() {
        try {
            List<IInventory> chests = inventoriesFindClosest(this.workPlace, 5);
            // P10(S0): 原来的 `|` 是位或，chests 为 null 时仍会调用 size()。
            if (chests == null || chests.size() == 0) {
                ModSimLoader.sendChat(new TextComponentTranslation("container.sim.Merchant13", new Object[0]).getUnformattedText());//商人：请在这里放一个箱子,然后在里面放64个物品。
                return;
            }
            float total = 0.0F;
            IInventory iInventory = chests.get(0);
            if (!isInventoryValid(iInventory)) {
                return;
            }
            for (int g = 0; g < iInventory.getSizeInventory(); g++) {

                ItemStack is = iInventory.getStackInSlot(g);
                if (is != null && !is.isEmpty()) {
                    // P10(S0): 支持零散数量，按 64 个的单价折算；不再只收整组。
                    Block soldBlock = Block.getBlockFromItem(is.getItem());
                    float stackPrice = PricesForBlocks.getPrice(soldBlock, false);
                    if (stackPrice > 0.0F) {
                        float value = stackPrice * ((float) is.getCount() / 64.0F);
                        if (value <= 0.0F) {
                            continue;
                        }
                        ModSimLoader.money += value;
                        PricesForBlocks.adjustPrice(soldBlock, false);
                        total += value;
                        iInventory.setInventorySlotContents(g, ItemStack.EMPTY);
                    }
                }
            }
            if (total == 0.0F) {
                //箱子里没有我想从你那里买的有效堆栈？
                ModSimLoader.sendChat(new TextComponentTranslation("container.sim.Merchant14", new Object[0]).getUnformattedText());
            } else {
                SoundEvent cash = SoundRegistry.CASH;
                if (cash == null || cash.getRegistryName() == null) {
                    ModSimLoader.log.error("播放失败：sim:cash 声音事件未注册");
                } else {
                for (EntityPlayer entityPlayer : this.jobWorld.playerEntities) {
                    this.jobWorld.playSound(entityPlayer, entityPlayer.posX, entityPlayer.posY, entityPlayer.posZ, cash, SoundCategory.BLOCKS, 1.0F, 1.0F);
                }}
                ModSimLoader.sendChat(new TextComponentTranslation("container.sim.Merchant15", new Object[0]).getUnformattedText() + ModSimLoader.displayMoney(total));
            }


        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("job-sellStuff出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }

    }

    /**
     * 买东西
     * 购买当前显示在购买页面上的东西
     */
    public void buyStuff(List<Integer> quantities) {
        try {
            ModSimLoader.log.info("准备买东西");
            // P10(S0): 客户端传来的数量必须做边界检查，不能直接 get(i)。
            if (quantities == null || quantities.isEmpty()) {
                return;
            }
            //找到箱子
            List<IInventory> chests = inventoriesFindClosest(this.workPlace, 5);
            if (chests == null || chests.isEmpty()) {
                ModSimLoader.sendChat(new TextComponentTranslation("container.sim.Merchant12", new Object[0]).getUnformattedText());
                return;
            }
            if (!isInventoryValid(chests.get(0))) {
                return;
            }
            for (int i = 0; i < 9; i++) {
                Integer qObj = i < quantities.size() ? quantities.get(i) : null;
                int quant = qObj == null ? 0 : qObj;
                // P10(S0): 单次最多购买 64 组，防止恶意包导致主线程长循环。
                quant = Math.max(0, Math.min(64, quant));
                if (quant <= 0) {
                    continue;
                }
                Block block = null;
                if (i == 0) {
                    block = Blocks.PLANKS;
                } else if (i == 1) {
                    block = Blocks.LOG;
                } else if (i == 2) {
                    block = Blocks.COBBLESTONE;
                } else if (i == 3) {
                    block = Blocks.STONE;
                } else if (i == 4) {
                    block = Blocks.GLASS;
                } else if (i == 5) {
                    block = Blocks.WOOL;
                } else if (i == 6) {
                    block = Blocks.BRICK_BLOCK;
                } else if (i == 7) {
                    block = Blocks.STONEBRICK;
                } else if (i == 8) {
                    block = Blocks.OAK_FENCE;
                }
                if (block == null) {
                    continue;
                }

                // P4: 按实际放进去的件数结算。原来 placeInJobChest 的返回值被丢弃、
                // 钱无条件扣 —— 箱子满时钱货两空。
                int paid = 0;
                float unitPrice = PricesForBlocks.getPrice(block, true);
                if (unitPrice <= 0.0F) {
                    continue;
                }
                for (int c = 0; c < quant; c++) {
                    // P10(S0): 先查余额，再放货；钱不够就停，绝不出现负余额。
                    if (ModSimLoader.gamemode != 1 && ModSimLoader.money < unitPrice) {
                        break;
                    }
                    ItemStack stack = new ItemStack(block, 64);
                    int put = this.placeInJobChestPartial(chests.get(0), stack);
                    if (put <= 0) {
                        break;
                    }
                    paid += put;
                    ModSimLoader.money -= unitPrice * ((float) put / 64.0F);
                    if (ModSimLoader.money < 0.0F) {
                        ModSimLoader.money = 0.0F;
                    }
                }
                if (paid == 0) {
                    ModSimLoader.sendChat(new TextComponentTranslation("container.sim.Merchant12", new Object[0]).getUnformattedText());
                    continue;
                }
                PricesForBlocks.adjustPrice(block, true);
            }
                SoundEvent cash = SoundRegistry.CASH;
                if (cash == null || cash.getRegistryName() == null) {
                    ModSimLoader.log.error("播放失败：sim:cash 声音事件未注册");
                } else {
                for (EntityPlayer entityPlayer : this.jobWorld.playerEntities) {
                    this.jobWorld.playSound(entityPlayer, entityPlayer.posX, entityPlayer.posY, entityPlayer.posZ, cash, SoundCategory.BLOCKS, 1.0F, 1.0F);
                }}
                // P4: 删除 Thread.sleep(1000L)。buyStuff 经 addScheduledTask 在世界线程
                // 执行，睡觉 1 秒会把整个世界冻住（多人下所有玩家一起卡）。
                SoundEvent merchm = SoundRegistry.MERCHM;
                if (merchm == null || merchm.getRegistryName() == null) {
                    ModSimLoader.log.error("播放失败：sim:merchm 声音事件未注册");
                } else {
                for (EntityPlayer entityPlayer :this.jobWorld.playerEntities) {
                    this.jobWorld.playSound(entityPlayer, entityPlayer.posX, entityPlayer.posY, entityPlayer.posZ, merchm, SoundCategory.BLOCKS, 1.0F, 1.0F);
                }}
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("job-buyStuff出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }

    }

    /**
     * 到底工作地点时
     */
    public void onArrive() {
        try {
            // 到达工作点后不要再发一次寻路：控制器已经把人送到可站立落点。
            this.folk.stopNavigationPath();
            this.folk.stayPut = true;
            if (this.stage != 0) {
                this.stage = 0;
            }

            if (this.jobTasks.size() > 0) {
                // P10(S0): 读档后 jobTasks 可能是空的/旧的 stage 可能越界。
                int index = this.stage;
                if (index < 0) {
                    index = 0;
                }
                if (index >= this.jobTasks.size()) {
                    index = this.jobTasks.size() - 1;
                }
                this.stage = index;
                this.currentTask = this.jobTasks.get(index);
                this.currentTask.begin();
            } else {
                // 让职业在下一 tick 重建任务队列。
                this.stage = -1;
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("job-onArrive出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }


    }

    @Override
    public abstract String toString();


}
