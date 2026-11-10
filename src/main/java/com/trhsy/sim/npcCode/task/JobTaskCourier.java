package com.trhsy.sim.npcCode.task;

import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.V3;
import com.trhsy.sim.npcCode.block.FarmBox;
import com.trhsy.sim.npcCode.block.MineBox;
import com.trhsy.sim.npcCode.job.Job;
import com.trhsy.sim.npcCode.job.JobCourier;
import com.trhsy.sim.npcCode.logistics.LogisticsService;
import com.trhsy.sim.npcCode.logistics.SupplyCatalog;
import com.trhsy.sim.npcCode.logistics.SupplyOrder;
import com.trhsy.sim.npcCode.store.SimStore;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemFlintAndSteel;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.InvWrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 快递员的自动物流任务（Beta12 重构）。
 *
 * <p>不再依赖任何手动路径点；携带物也不放在个人口粮背包里。具体规则：</p>
 *
 * <ul>
 *   <li>自动发现所有农场箱、矿场箱，以及各生产职业的产出点；</li>
 *   <li>一趟可以连续跑多个顺路的收取点，装满或没有顺路点后回物流中心；</li>
 *   <li>有店铺补货订单时优先领单，按订单取货并送到店铺；</li>
 *   <li>在途货物单独存在 {@code NpcData.courierCargo}，不会把 NPC 的食物也卸掉；</li>
 *   <li>农场只拿走多余种子/种植物，保留 64 个，避免把农民的种子拿光；</li>
 *   <li>物流中心/目标箱子满时原地等待，货物不会掉进虚空。</li>
 * </ul>
 */
public class JobTaskCourier extends JobTask {

    /** 快递员一趟最多携带 27 组货物。 */
    private static final int MAX_CARGO_STACKS = 27;
    /** 任务 tick 间隔，避免每 tick 都扫箱子/寻路。 */
    private static final long TICK_INTERVAL_MS = 200L;
    /** 重新向寻路器发目标的间隔。 */
    private static final long MOVE_INTERVAL_MS = 2000L;
    /** 没有货物时的重试间隔。 */
    private static final long IDLE_RETRY_MS = 5000L;
    /** 收取点缓存刷新间隔。 */
    private static final long STOP_CACHE_MS = 15000L;
    /** 一个点走不到时，多久后放弃。没有传送后必须给足步行时间。 */
    private static final long STOP_GIVE_UP_MS = 240000L;
    /** 物流中心满时，多久后重试卸货。 */
    private static final long DROPOFF_RETRY_MS = 5000L;
    /** 到达判定：水平 3 格、垂直 4 格。 */
    private static final double ARRIVE_XZ = 3.0D;
    private static final double ARRIVE_Y = 4.0D;
    /** 顺路连续收取的最大距离。 */
    private static final double CHAIN_DISTANCE = 48.0D;
    /** 农场每种种子/种植物保留的数量。 */
    private static final int SEED_RESERVE = 64;

    private static final int KIND_FARM = 1;
    private static final int KIND_MINE = 2;
    private static final int KIND_OUTPUT = 3;

    private enum State {
        IDLE,
        GO_PICKUP,
        PICKUP,
        GO_DROPOFF,
        DROPOFF,
        ORDER_GO_PICKUP,
        ORDER_PICKUP,
        ORDER_GO_DROPOFF,
        ORDER_DROPOFF
    }

    /** 旧的公开字段，保留给可能存在的反射/存档兼容代码。 */
    public String status;

    private transient State state = State.IDLE;
    private transient V3 pickup;
    private transient V3 dropoff;
    private transient String pickupLabel = "";
    private transient String currentStopKey;
    private transient final Set<String> visited = new HashSet<>();
    private transient final Map<String, Long> stopCooldown = new HashMap<>();
    private transient List<PickupStop> cachedStops = new ArrayList<>();
    private transient long lastStopBuild = 0L;
    private transient long nextScanTime = 0L;
    private transient long nextOrderScanTime = 0L;
    private transient long lastTick = 0L;
    private transient long lastMoveTime = 0L;
    private transient long travelStart = 0L;
    private transient long nextDropoffTry = 0L;

    /** 当前正在执行的供货订单；一次只跑一张订单。 */
    private transient SupplyOrder order;
    /** 订单取货阶段：0=物流中心，1=供货点。 */
    private transient int orderPickupPhase = 0;
    /** 已经去过的供货点，避免在同一张订单里来回跑。 */
    private transient final Set<String> orderVisitedSources = new HashSet<>();
    /** 订单送货重试。 */
    private transient long orderDropoffRetryAt = 0L;
    private transient int orderDropoffFailures = 0;
    private transient long orderCenterStart = 0L;
    /** 订单剩余物品确实没有供货点时，送完手里的货就取消。 */
    private transient boolean orderCancelRemainder = false;

    public JobTaskCourier(Job j, long ms, String status) {
        super(j, ms);
        this.status = status;
    }

    @Override
    public void onTaskBegin() {
        this.state = State.IDLE;
        this.dropoff = this.job.workPlace;
        this.visited.clear();
        this.cachedStops.clear();
        this.lastStopBuild = 0L;
        this.nextScanTime = 0L;
        this.nextOrderScanTime = 0L;
        this.lastTick = 0L;
        this.lastMoveTime = 0L;
        this.nextDropoffTry = 0L;
        this.clearOrder();
        this.setStatusKey("container.sim.job.courier.auto.start");
    }

    @Override
    public void onUpdate() {
        long now = System.currentTimeMillis();
        if (now - this.lastTick < TICK_INTERVAL_MS) {
            return;
        }
        this.lastTick = now;
        try {
            switch (this.state) {
                case IDLE:
                    this.tickIdle(now);
                    break;
                case GO_PICKUP:
                    this.tickGoPickup(now);
                    break;
                case PICKUP:
                    this.tickPickup(now);
                    break;
                case GO_DROPOFF:
                    this.tickGoDropoff(now);
                    break;
                case DROPOFF:
                    this.tickDropoff(now);
                    break;
                case ORDER_GO_PICKUP:
                    this.tickOrderGoPickup(now);
                    break;
                case ORDER_PICKUP:
                    this.tickOrderPickup(now);
                    break;
                case ORDER_GO_DROPOFF:
                    this.tickOrderGoDropoff(now);
                    break;
                case ORDER_DROPOFF:
                    this.tickOrderDropoff(now);
                    break;
                default:
                    this.state = State.IDLE;
                    break;
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("JobTaskCourier-onUpdate出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
            this.state = State.IDLE;
            this.nextScanTime = now + IDLE_RETRY_MS;
        }
    }

    private void tickIdle(long now) {
        if (!this.cargoEmpty()) {
            this.startDropoff();
            return;
        }
        // 有订单时优先送订单；没有订单才继续自动收货。
        if (this.job instanceof JobCourier && now >= this.nextOrderScanTime) {
            this.nextOrderScanTime = now + 2000L;
            SimStore store = SimStore.get(this.job.jobWorld);
            SupplyOrder claimed = LogisticsService.claimOrder(store, (JobCourier) this.job);
            if (claimed != null) {
                this.beginOrder(claimed, now);
                return;
            }
        }
        if (now < this.nextScanTime) {
            this.setStatusKey("container.sim.job.courier.auto.idle");
            return;
        }
        List<PickupStop> stops = this.getStops(now);
        this.visited.clear();
        PickupStop stop = this.chooseNearest(stops, null, 0.0D);
        if (stop == null) {
            this.setStatusKey("container.sim.job.courier.auto.idle");
            this.nextScanTime = now + IDLE_RETRY_MS;
            return;
        }
        this.startPickup(stop, now);
    }

    // ==================== 订单派送 ====================

    private void beginOrder(SupplyOrder claimed, long now) {
        this.order = claimed;
        this.orderPickupPhase = 0;
        this.orderVisitedSources.clear();
        this.orderDropoffFailures = 0;
        this.orderDropoffRetryAt = 0L;
        this.orderCancelRemainder = false;
        this.orderCenterStart = now;
        this.pickup = this.job.workPlace;
        this.pickupLabel = this.t("container.sim.job.courier.auto.depot");
        this.state = State.ORDER_GO_PICKUP;
        this.travelStart = now;
        this.lastMoveTime = 0L;
        LogisticsService.markPicking(SimStore.get(this.job.jobWorld), this.order);
        this.setStatus(this.t("container.sim.job.courier.order.pickup_center"));
        this.moveTo(this.pickup, now);
    }

    private void tickOrderGoPickup(long now) {
        if (this.order == null || this.pickup == null) {
            this.clearOrder();
            this.state = State.IDLE;
            this.nextScanTime = now + IDLE_RETRY_MS;
            return;
        }
        if (SupplyOrder.CANCELLED.equals(this.order.status)) {
            this.orderCancelRemainder = true;
            if (this.cargoEmpty()) {
                this.clearOrder();
                this.state = State.IDLE;
                this.nextScanTime = now + IDLE_RETRY_MS;
            } else {
                this.startOrderDropoff(now);
            }
            return;
        }
        if (this.folk.isAtDestination(this.pickup, ARRIVE_XZ, ARRIVE_Y)) {
            this.clearPath();
            this.state = State.ORDER_PICKUP;
            return;
        }
        long elapsed = now - this.travelStart;
        boolean unreachable = elapsed > 30000L && this.folk.isNavigationUnreachable(this.pickup);
        if (unreachable || elapsed > STOP_GIVE_UP_MS) {
            if (this.orderPickupPhase == 1) {
                // 供货点走不到：换下一个供货点；没有就送手里的货并取消剩余。
                this.orderVisitedSources.add(this.pickup.toString());
                this.pickup = null;
                this.orderPickupPhase = 0;
                this.continueOrderPickup(now, true);
            } else {
                // 物流中心走不到：先重试；四分钟仍到不了就释放订单，避免永久占线。
                if (now - this.orderCenterStart > 240000L) {
                    LogisticsService.releaseOrder(SimStore.get(this.job.jobWorld), this.order);
                    this.clearOrder();
                    this.state = State.IDLE;
                    this.nextScanTime = now + IDLE_RETRY_MS;
                    this.setStatusKey("container.sim.job.courier.auto.unreachable");
                    return;
                }
                this.travelStart = now;
            }
            return;
        }
        if (now - this.lastMoveTime >= MOVE_INTERVAL_MS) {
            this.lastMoveTime = now;
            this.moveTo(this.pickup, now);
        }
    }

    private void tickOrderPickup(long now) {
        if (this.order == null) {
            this.state = State.IDLE;
            return;
        }
        if (SupplyOrder.CANCELLED.equals(this.order.status)) {
            this.orderCancelRemainder = true;
            this.pickup = null;
            this.continueOrderPickup(now, false);
            return;
        }
        int radius = this.orderPickupPhase == 0 ? 8 : 6;
        List<IInventory> inventories = this.job.inventoriesFindClosest(this.pickup, radius);
        int taken = this.takeOrderItems(this.order, inventories);
        if (taken > 0) {
            this.setStatus(this.t("container.sim.job.courier.auto.picked") + " " + taken
                    + this.t("container.sim.job.courier.auto.items"));
            ModSimLoader.log.info("快递员 {} 为订单 {} 取货 {} 件", this.folk.getName(),
                    this.order.id, taken);
        }
        if (this.orderPickupPhase == 1 && this.pickup != null) {
            this.orderVisitedSources.add(this.pickup.toString());
        }
        this.pickup = null;
        this.orderPickupPhase = 0;
        this.continueOrderPickup(now, false);
    }

    /** 取货阶段结束后的决策：继续去供货点、开始送货，或取不到货时取消剩余。 */
    private void continueOrderPickup(long now, boolean forceCheck) {
        if (this.order == null) {
            this.state = State.IDLE;
            this.nextScanTime = now + IDLE_RETRY_MS;
            return;
        }
        if (SupplyOrder.CANCELLED.equals(this.order.status)) {
            this.orderCancelRemainder = true;
            if (this.cargoEmpty()) {
                this.clearOrder();
                this.state = State.IDLE;
                this.nextScanTime = now + IDLE_RETRY_MS;
            } else {
                this.startOrderDropoff(now);
            }
            return;
        }
        List<ItemStack> remaining = this.orderRemaining(this.order);
        if (remaining.isEmpty()) {
            this.startOrderDropoff(now);
            return;
        }
        if (this.cargoFull()) {
            this.startOrderDropoff(now);
            return;
        }
        V3 source = this.findNextOrderSource(remaining);
        if (source != null) {
            this.pickup = source;
            this.orderPickupPhase = 1;
            this.state = State.ORDER_GO_PICKUP;
            this.travelStart = now;
            this.lastMoveTime = 0L;
            this.pickupLabel = this.t("container.sim.job.courier.order.source");
            this.setStatus(this.t("container.sim.job.courier.order.pickup_source"));
            this.moveTo(this.pickup, now);
            return;
        }
        // 找不到任何供货点：手里的货先送，剩余部分取消，避免订单永远卡住。
        this.orderCancelRemainder = true;
        LogisticsService.cancelOrder(SimStore.get(this.job.jobWorld), this.order);
        if (this.cargoEmpty()) {
            this.setStatus(this.t("container.sim.job.courier.order.no_source"));
            this.clearOrder();
            this.state = State.IDLE;
            this.nextScanTime = now + IDLE_RETRY_MS;
        } else {
            this.startOrderDropoff(now);
        }
    }

    private List<ItemStack> orderRemaining(SupplyOrder supplyOrder) {
        List<ItemStack> remaining = new ArrayList<>();
        if (supplyOrder == null) {
            return remaining;
        }
        for (ItemStack stack : supplyOrder.items) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            int left = supplyOrder.remainingFor(stack, this.folk.courierCargo);
            if (left > 0) {
                ItemStack copy = stack.copy();
                copy.setCount(left);
                remaining.add(copy);
            }
        }
        return remaining;
    }

    private V3 findNextOrderSource(List<ItemStack> remaining) {
        if (remaining == null || remaining.isEmpty()) {
            return null;
        }
        V3 from = this.folk.getV3();
        V3 best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ItemStack need : remaining) {
            for (V3 source : SupplyCatalog.findSources(this.job, need, 2)) {
                if (source == null || source.dimension != this.job.workPlace.dimension) {
                    continue;
                }
                String key = source.toString();
                if (this.orderVisitedSources.contains(key) || source.equals(this.job.workPlace)) {
                    continue;
                }
                double distance = from == null ? 0.0D : from.distanceSq(source);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = source;
                }
            }
        }
        return best;
    }

    /** 只从箱子里取订单还需要的物品；不整箱搬走。 */
    private int takeOrderItems(SupplyOrder supplyOrder, List<IInventory> inventories) {
        if (supplyOrder == null || inventories == null || inventories.isEmpty()) {
            return 0;
        }
        int taken = 0;
        for (IInventory inventory : inventories) {
            if (this.cargoFull()) {
                break;
            }
            if (!this.validInventory(inventory)) {
                continue;
            }
            IItemHandler handler;
            try {
                handler = SupplyCatalog.handlerFor(inventory);
            } catch (Exception e) {
                continue;
            }
            if (handler == null) {
                continue;
            }
            for (int slot = 0; slot < handler.getSlots() && !this.cargoFull(); slot++) {
                ItemStack inSlot = handler.getStackInSlot(slot);
                if (inSlot.isEmpty()) {
                    continue;
                }
                int need = supplyOrder.remainingFor(inSlot, this.folk.courierCargo);
                if (need <= 0) {
                    continue;
                }
                int want = Math.min(inSlot.getCount(), need);
                ItemStack extracted = handler.extractItem(slot, want, false);
                if (extracted.isEmpty()) {
                    continue;
                }
                int accepted = this.addCargo(extracted);
                if (accepted < extracted.getCount()) {
                    ItemStack leftover = extracted.copy();
                    leftover.setCount(extracted.getCount() - accepted);
                    ItemHandlerHelper.insertItemStacked(handler, leftover, false);
                }
                taken += accepted;
            }
        }
        return taken;
    }

    private void startOrderDropoff(long now) {
        if (this.order == null || this.order.requester == null) {
            this.clearOrder();
            this.state = State.IDLE;
            this.nextScanTime = now + IDLE_RETRY_MS;
            return;
        }
        this.state = State.ORDER_GO_DROPOFF;
        this.dropoff = this.order.requester;
        this.travelStart = now;
        this.lastMoveTime = 0L;
        this.orderDropoffRetryAt = 0L;
        this.orderDropoffFailures = 0;
        if (this.order.isActive()) {
            LogisticsService.markDelivering(SimStore.get(this.job.jobWorld), this.order);
        }
        this.setStatus(this.t("container.sim.job.courier.order.deliver") + " "
                + (this.order.requesterLabel == null ? "" : this.order.requesterLabel));
        this.moveTo(this.dropoff, now);
    }

    private void tickOrderGoDropoff(long now) {
        if (this.order == null) {
            this.state = State.IDLE;
            return;
        }
        if (this.cargoEmpty()) {
            this.endOrderTrip(now, false);
            return;
        }
        if (this.dropoff == null) {
            this.dropoff = this.order.requester;
        }
        if (this.dropoff == null) {
            this.abortOrderToCenter(now);
            return;
        }
        if (this.folk.isAtDestination(this.dropoff, ARRIVE_XZ, ARRIVE_Y)) {
            this.clearPath();
            this.state = State.ORDER_DROPOFF;
            this.orderDropoffRetryAt = 0L;
            return;
        }
        long elapsed = now - this.travelStart;
        if (elapsed > 30000L && this.folk.isNavigationUnreachable(this.dropoff)) {
            this.abortOrderToCenter(now);
            return;
        }
        if (elapsed > 240000L) {
            this.abortOrderToCenter(now);
            return;
        }
        if (now - this.lastMoveTime >= MOVE_INTERVAL_MS) {
            this.lastMoveTime = now;
            this.moveTo(this.dropoff, now);
        }
    }

    private void tickOrderDropoff(long now) {
        if (this.order == null) {
            if (this.cargoEmpty()) {
                this.state = State.IDLE;
            } else {
                this.startDropoff();
            }
            return;
        }
        if (this.cargoEmpty()) {
            this.endOrderTrip(now, false);
            return;
        }
        if (now < this.orderDropoffRetryAt) {
            return;
        }
        this.orderDropoffRetryAt = now + DROPOFF_RETRY_MS;
        // 目标箱满时订单保持 DELIVERING，续租避免被其他快递员重复领取。
        this.order.updatedAt = now;
        SimStore orderStore = SimStore.get(this.job.jobWorld);
        if (orderStore != null) {
            orderStore.markDirty();
        }
        List<IInventory> chests = this.job.inventoriesFindClosest(this.order.requester, 5);
        if (chests.isEmpty()) {
            this.orderDropoffFailures++;
            this.setStatusKey("container.sim.job.courier.order.no_chest");
            if (this.orderDropoffFailures >= 6) {
                this.abortOrderToCenter(now);
            }
            return;
        }
        int deliveredThisTrip = 0;
        for (int i = 0; i < this.folk.courierCargo.size(); i++) {
            ItemStack stack = this.folk.courierCargo.get(i);
            if (stack == null || stack.isEmpty() || this.order.requestedCount(stack) <= 0) {
                continue;
            }
            for (IInventory chest : chests) {
                if (stack.isEmpty()) {
                    break;
                }
                if (!this.validInventory(chest)) {
                    continue;
                }
                int inserted = this.job.placeInJobChestPartial(chest, stack);
                if (inserted > 0) {
                    this.order.markDelivered(stack, inserted);
                    stack.shrink(inserted);
                    deliveredThisTrip += inserted;
                }
            }
        }
        this.cleanCargo();
        if (deliveredThisTrip > 0) {
            float fee = LogisticsService.payDelivery(this.job, this.job.workPlace, deliveredThisTrip);
            String feeText = fee > 0.0F ? " (" + ModSimLoader.displayMoney(fee) + ")" : "";
            this.setStatus(this.t("container.sim.job.courier.order.delivered") + " "
                    + deliveredThisTrip + this.t("container.sim.job.courier.auto.items") + feeText);
            this.orderDropoffFailures = 0;
        }
        if (this.order.isComplete()) {
            this.endOrderTrip(now, true);
            return;
        }
        if (this.cargoEmpty()) {
            // 这一趟能送的都送完了，剩余部分释放回待领取。
            if (this.orderCancelRemainder) {
                LogisticsService.finishOrder(SimStore.get(this.job.jobWorld), this.order);
            } else {
                LogisticsService.releaseOrder(SimStore.get(this.job.jobWorld), this.order);
            }
            this.clearOrder();
            this.state = State.IDLE;
            this.nextScanTime = now + 1000L;
            this.clearPath();
            this.setStatusKey("container.sim.job.courier.auto.idle");
            return;
        }
        this.setStatus(this.t("container.sim.job.courier.order.partial") + " ("
                + this.cargoItems() + this.t("container.sim.job.courier.auto.items") + ")");
    }

    /** 收货点不可达：订单释放，手里的货先运回物流中心。 */
    private void abortOrderToCenter(long now) {
        if (this.order != null) {
            if (this.orderCancelRemainder) {
                LogisticsService.finishOrder(SimStore.get(this.job.jobWorld), this.order);
            } else {
                LogisticsService.releaseOrder(SimStore.get(this.job.jobWorld), this.order);
            }
        }
        boolean hasCargo = !this.cargoEmpty();
        this.clearOrder();
        if (hasCargo) {
            this.startDropoff();
        } else {
            this.state = State.IDLE;
            this.nextScanTime = now + IDLE_RETRY_MS;
            this.clearPath();
            this.setStatusKey("container.sim.job.courier.auto.idle");
        }
    }

    /** 订单结束：完成则从存档移除，取消则只清掉记录。 */
    private void endOrderTrip(long now, boolean completed) {
        if (this.order != null) {
            SimStore store = SimStore.get(this.job.jobWorld);
            boolean done = completed || this.order.isComplete() || this.orderCancelRemainder;
            if (done) {
                LogisticsService.finishOrder(store, this.order);
            } else {
                LogisticsService.releaseOrder(store, this.order);
            }
            ModSimLoader.log.info("快递员 {} 结束订单 {}（{}）", this.folk.getName(),
                    this.order.id, done ? "完成" : "释放");
        }
        this.clearOrder();
        this.state = State.IDLE;
        this.nextScanTime = now + 1000L;
        this.clearPath();
        this.setStatusKey("container.sim.job.courier.auto.idle");
    }

    private void clearOrder() {
        this.order = null;
        this.orderPickupPhase = 0;
        this.orderVisitedSources.clear();
        this.orderDropoffFailures = 0;
        this.orderDropoffRetryAt = 0L;
        this.orderCenterStart = 0L;
        this.orderCancelRemainder = false;
        this.pickup = null;
        this.pickupLabel = "";
    }

    private void startPickup(PickupStop stop, long now) {        if (stop == null) {
            this.state = State.IDLE;
            this.nextScanTime = now + IDLE_RETRY_MS;
            return;
        }
        this.pickup = stop.loc;
        this.pickupLabel = stop.label;
        this.currentStopKey = stop.key;
        this.state = State.GO_PICKUP;
        this.travelStart = now;
        this.lastMoveTime = 0L;
        this.setStatus(this.t("container.sim.job.courier.auto.going") + " " + stop.label);
        this.moveTo(this.pickup, now);
    }

    private void tickGoPickup(long now) {
        if (this.pickup == null) {
            this.state = State.IDLE;
            return;
        }
        if (this.folk.isAtDestination(this.pickup, ARRIVE_XZ, ARRIVE_Y)) {
            this.clearPath();
            this.state = State.PICKUP;
            return;
        }
        long elapsed = now - this.travelStart;
        boolean unreachable = elapsed > 30000L && this.folk.isNavigationUnreachable(this.pickup);
        if (unreachable || elapsed > STOP_GIVE_UP_MS) {
            this.visited.add(this.currentStopKey);
            this.stopCooldown.put(this.currentStopKey, now + 60000L);
            this.setStatus(this.t("container.sim.job.courier.auto.unreachable") + " " + this.pickupLabel);
            this.state = State.IDLE;
            this.nextScanTime = now + IDLE_RETRY_MS;
            return;
        }
        if (now - this.lastMoveTime >= MOVE_INTERVAL_MS) {
            this.lastMoveTime = now;
            this.moveTo(this.pickup, now);
        }
    }

    private void tickPickup(long now) {
        PickupStop stop = this.findStop(this.currentStopKey);
        if (stop == null) {
            this.state = State.IDLE;
            this.nextScanTime = now + IDLE_RETRY_MS;
            return;
        }
        List<IInventory> inventories = this.job.inventoriesFindClosest(this.pickup, 6);
        this.removeDropoffInventories(inventories);
        int taken = this.takeFromInventories(stop, inventories);
        if (taken > 0) {
            this.setStatus(this.t("container.sim.job.courier.auto.picked") + " " + taken
                    + this.t("container.sim.job.courier.auto.items"));
            ModSimLoader.log.info("快递员 {} 在 {} 收取了 {} 件货物", this.folk.getName(), stop.label, taken);
        } else {
            this.setStatusKey("container.sim.job.courier.auto.empty");
        }
        this.visited.add(this.currentStopKey);
        this.currentStopKey = null;
        this.pickup = null;

        if (this.cargoEmpty()) {
            this.state = State.IDLE;
            this.nextScanTime = now + 1000L;
            return;
        }
        if (this.cargoFull()) {
            this.startDropoff();
            return;
        }
        PickupStop next = this.chooseNearest(this.getStops(now), this.visited, CHAIN_DISTANCE);
        if (next != null) {
            this.startPickup(next, now);
        } else {
            this.startDropoff();
        }
    }

    private void startDropoff() {
        this.state = State.GO_DROPOFF;
        this.dropoff = this.job.workPlace;
        this.travelStart = System.currentTimeMillis();
        this.lastMoveTime = 0L;
        this.nextDropoffTry = 0L;
        int items = this.cargoItems();
        String suffix = items > 0 ? " (" + items + this.t("container.sim.job.courier.auto.items") + ")" : "";
        this.setStatus(this.t("container.sim.job.courier.auto.returning") + suffix);
        this.moveTo(this.dropoff, System.currentTimeMillis());
    }

    private void tickGoDropoff(long now) {
        if (this.cargoEmpty()) {
            this.finishTrip(now);
            return;
        }
        if (this.dropoff == null) {
            this.dropoff = this.job.workPlace;
        }
        if (this.dropoff == null) {
            this.setStatusKey("container.sim.job.courier.auto.cannot_return");
            this.state = State.IDLE;
            this.nextScanTime = now + IDLE_RETRY_MS;
            return;
        }
        if (this.folk.isAtDestination(this.dropoff, ARRIVE_XZ, ARRIVE_Y)) {
            this.clearPath();
            this.state = State.DROPOFF;
            this.nextDropoffTry = 0L;
            return;
        }
        long elapsed = now - this.travelStart;
        if (elapsed > 180000L) {
            this.setStatusKey("container.sim.job.courier.auto.cannot_return");
            this.travelStart = now;
        }
        if (now - this.lastMoveTime >= MOVE_INTERVAL_MS) {
            this.lastMoveTime = now;
            this.moveTo(this.dropoff, now);
        }
    }

    private void tickDropoff(long now) {
        if (this.cargoEmpty()) {
            this.finishTrip(now);
            return;
        }
        if (now < this.nextDropoffTry) {
            return;
        }
        this.nextDropoffTry = now + DROPOFF_RETRY_MS;
        List<IInventory> chests = this.job.inventoriesFindClosest(this.job.workPlace, 8);
        if (chests.isEmpty()) {
            this.setStatusKey("container.sim.job.courier.auto.no_warehouse_chest");
            return;
        }
        int before = this.cargoItems();
        this.depositCargo(chests);
        int after = this.cargoItems();
        if (after == 0) {
            ModSimLoader.log.info("快递员 {} 完成一趟配送：{} 件货物已入库", this.folk.getName(), before);
            this.finishTrip(now);
        } else {
            this.setStatus(this.t("container.sim.job.courier.auto.warehouse_full") + " ("
                    + after + this.t("container.sim.job.courier.auto.items") + ")");
        }
    }

    private void finishTrip(long now) {
        this.visited.clear();
        this.pickup = null;
        this.currentStopKey = null;
        this.pickupLabel = "";
        this.state = State.IDLE;
        this.nextScanTime = now + 1000L;
        this.clearPath();
        this.setStatusKey("container.sim.job.courier.auto.idle");
    }

    private void moveTo(V3 target, long now) {
        if (target == null) {
            return;
        }
        this.lastMoveTime = now;
        this.folk.forceMoveToXYZ(target);
    }

    private void clearPath() {
        if (this.folk.entity != null) {
            this.folk.entity.getNavigator().clearPath();
        }
    }

    // ==================== 收取点发现 ====================

    private List<PickupStop> getStops(long now) {
        int pointCount = ModSimLoader.farms.size() + ModSimLoader.mines.size();
        if (now - this.lastStopBuild > STOP_CACHE_MS || pointCount != this.lastPointCount) {
            this.rebuildStops();
            this.lastStopBuild = now;
            this.lastPointCount = pointCount;
        }
        return this.cachedStops;
    }

    private transient int lastPointCount = -1;

    private void rebuildStops() {
        Map<String, PickupStop> byKey = new LinkedHashMap<>();
        List<IInventory> dropoffInventories = this.job.inventoriesFindClosest(this.job.workPlace, 8);
        for (FarmBox farm : ModSimLoader.farms) {
            if (farm == null || farm.loc == null || !this.sameDimension(farm.loc)) {
                continue;
            }
            String key = "farm:" + (farm.ID == null ? farm.loc.toString() : farm.ID);
            String label = this.t("container.sim.job.courier.auto.farm") + " "
                    + (int) farm.loc.x + "," + (int) farm.loc.z;
            this.addStop(byKey, key, label, farm.loc, KIND_FARM, null, dropoffInventories);
        }
        for (MineBox mine : ModSimLoader.mines) {
            if (mine == null || mine.loc == null || !this.sameDimension(mine.loc)) {
                continue;
            }
            String key = "mine:" + (mine.ID == null ? mine.loc.toString() : mine.ID);
            String label = this.t("container.sim.job.courier.auto.mine") + " "
                    + (int) mine.loc.x + "," + (int) mine.loc.z;
            this.addStop(byKey, key, label, mine.loc, KIND_MINE, null, dropoffInventories);
        }
        // 其他职业的成品收货点：只有这些职业实际往箱子里放过的物品类型才会被取走。
        SimStore store = SimStore.get(this.job.jobWorld);
        if (store != null) {
            for (SimStore.LogisticsPoint point : store.getLogisticsPoints().values()) {
                if (point == null || point.loc == null || point.items.isEmpty()
                        || !this.sameDimension(point.loc)) {
                    continue;
                }
                // 旧存档可能记录过商店/餐厅货架；只保留生产职业的产出点。
                if (!SupplyCatalog.isProductionLabel(point.label)) {
                    continue;
                }
                String key = "out:" + point.loc.toString();
                String base = point.label == null || point.label.isEmpty()
                        ? this.t("container.sim.job.courier.auto.output") : point.label;
                String label = base + " " + (int) point.loc.x + "," + (int) point.loc.z;
                this.addStop(byKey, key, label, point.loc, KIND_OUTPUT, point.items, dropoffInventories);
            }
        }
        List<PickupStop> unique = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (PickupStop stop : byKey.values()) {
            if (stop != null && seen.add(stop.key)) {
                unique.add(stop);
            }
        }
        this.cachedStops = unique;
    }

    private void addStop(Map<String, PickupStop> byKey, String key, String label, V3 loc,
                         int kind, Set<String> outputKeys, List<IInventory> dropoffInventories) {
        String locationKey = "loc:" + loc.x + "," + loc.y + "," + loc.z + "," + loc.dimension;
        if (byKey.containsKey(key) || byKey.containsKey(locationKey)) {
            return;
        }
        List<IInventory> inventories = this.job.inventoriesFindClosest(loc, 6);
        List<IInventory> filtered = new ArrayList<>();
        for (IInventory inventory : inventories) {
            if (!this.validInventory(inventory)) {
                continue;
            }
            if (this.containsIdentity(dropoffInventories, inventory)) {
                continue;
            }
            filtered.add(inventory);
        }
        if (filtered.isEmpty()) {
            return;
        }
        PickupStop stop = new PickupStop(key, label, loc, kind, outputKeys, filtered);
        byKey.put(key, stop);
        byKey.put(locationKey, stop);
    }

    private PickupStop findStop(String key) {
        if (key == null) {
            return null;
        }
        for (PickupStop stop : this.cachedStops) {
            if (stop != null && key.equals(stop.key)) {
                return stop;
            }
        }
        return null;
    }

    private PickupStop chooseNearest(List<PickupStop> stops, Set<String> exclude, double maxDistance) {
        if (stops == null || stops.isEmpty()) {
            return null;
        }
        V3 from = this.folk.getV3();
        if (from == null) {
            return null;
        }
        PickupStop best = null;
        double bestDistance = Double.MAX_VALUE;
        double maxDistanceSq = maxDistance > 0.0D ? maxDistance * maxDistance : Double.MAX_VALUE;
        for (PickupStop stop : stops) {
            if (stop == null || !this.sameDimension(stop.loc)) {
                continue;
            }
            if (exclude != null && exclude.contains(stop.key)) {
                continue;
            }
            Long cooldown = this.stopCooldown.get(stop.key);
            if (cooldown != null && cooldown > System.currentTimeMillis()) {
                continue;
            }
            double distanceSq = this.distanceSq(from, stop.loc);
            if (distanceSq > maxDistanceSq || distanceSq >= bestDistance) {
                continue;
            }
            if (!this.hasTakeableItems(stop)) {
                continue;
            }
            best = stop;
            bestDistance = distanceSq;
        }
        return best;
    }

    // ==================== 取货 ====================

    private int takeFromInventories(PickupStop stop, List<IInventory> inventories) {
        if (stop == null || inventories == null || inventories.isEmpty()) {
            return 0;
        }
        int taken = 0;
        Map<String, Integer> seedReserve = new HashMap<>();
        for (IInventory inventory : inventories) {
            if (this.cargoFull()) {
                break;
            }
            if (!this.validInventory(inventory)) {
                continue;
            }
            IItemHandler handler;
            try {
                handler = inventory instanceof IItemHandler ? (IItemHandler) inventory : new InvWrapper(inventory);
            } catch (Exception e) {
                continue;
            }
            for (int slot = 0; slot < handler.getSlots() && !this.cargoFull(); slot++) {
                ItemStack inSlot = handler.getStackInSlot(slot);
                if (inSlot.isEmpty()) {
                    continue;
                }
                int limit = this.takeLimit(stop, inSlot, seedReserve);
                if (limit <= 0) {
                    continue;
                }
                int want = Math.min(inSlot.getCount(), limit);
                ItemStack extracted = handler.extractItem(slot, want, false);
                if (extracted.isEmpty()) {
                    continue;
                }
                int accepted = this.addCargo(extracted);
                if (accepted < extracted.getCount()) {
                    ItemStack leftover = extracted.copy();
                    leftover.setCount(extracted.getCount() - accepted);
                    ItemHandlerHelper.insertItemStacked(handler, leftover, false);
                }
                taken += accepted;
            }
        }
        return taken;
    }

    private boolean hasTakeableItems(PickupStop stop) {
        Map<String, Integer> seedReserve = new HashMap<>();
        for (IInventory inventory : stop.inventories) {
            if (!this.validInventory(inventory)) {
                continue;
            }
            IItemHandler handler;
            try {
                handler = inventory instanceof IItemHandler ? (IItemHandler) inventory : new InvWrapper(inventory);
            } catch (Exception e) {
                continue;
            }
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (!stack.isEmpty() && this.takeLimit(stop, stack, seedReserve) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 计算某个槽最多能拿多少。
     *
     * <p>农场：工具不拿；种子/种植物每种保留 {@link #SEED_RESERVE} 个，多出来的拿走。
     * 矿场：工具不拿，其余全拿。产出点：只拿白名单内的成品。</p>
     */
    private int takeLimit(PickupStop stop, ItemStack stack, Map<String, Integer> seedReserve) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        if (stop.kind == KIND_OUTPUT) {
            return stop.outputKeys != null && stop.outputKeys.contains(SimStore.itemKey(stack))
                    ? stack.getCount() : 0;
        }
        if (this.isToolLike(stack)) {
            return 0;
        }
        if (stop.kind == KIND_FARM && this.isPlantingInput(stack)) {
            String key = this.itemKey(stack);
            int left = seedReserve.containsKey(key) ? seedReserve.get(key) : SEED_RESERVE;
            if (left <= 0) {
                return stack.getCount();
            }
            int keep = Math.min(left, stack.getCount());
            seedReserve.put(key, left - keep);
            return stack.getCount() - keep;
        }
        return stack.getCount();
    }

    private String itemKey(ItemStack stack) {
        String registry = stack.getItem().getRegistryName() == null
                ? stack.getItem().getUnlocalizedName() : stack.getItem().getRegistryName().toString();
        return registry + "@" + stack.getMetadata();
    }

    private boolean isToolLike(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof ItemTool || item instanceof ItemHoe || item instanceof ItemSword
                || item instanceof ItemShears || item instanceof ItemFishingRod
                || item instanceof ItemBucket || item instanceof ItemArmor
                || item instanceof ItemBow || item instanceof ItemFlintAndSteel) {
            return true;
        }
        if (item instanceof ItemBlock) {
            Block block = ((ItemBlock) item).getBlock();
            // 矿场/农场箱里常备的火把、梯子属于生产资料，不往物流中心搬。
            return block == Blocks.TORCH || block == Blocks.LADDER;
        }
        return false;
    }

    private boolean isPlantingInput(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof IPlantable) {
            return true;
        }
        String name = item.getUnlocalizedName() == null ? "" : item.getUnlocalizedName().toLowerCase();
        return name.contains("seed") || name.contains("reeds") || name.contains("cactus")
                || name.contains("dye") || name.contains("sapling") || name.contains("cocoa");
    }

    // ==================== 货物 ====================

    private int addCargo(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        int remaining = stack.getCount();
        int accepted = 0;
        for (ItemStack existing : this.folk.courierCargo) {
            if (remaining <= 0) {
                break;
            }
            if (existing == null || existing.isEmpty()) {
                continue;
            }
            if (ItemStack.areItemsEqual(existing, stack) && ItemStack.areItemStackTagsEqual(existing, stack)) {
                int space = existing.getMaxStackSize() - existing.getCount();
                if (space > 0) {
                    int move = Math.min(space, remaining);
                    existing.grow(move);
                    remaining -= move;
                    accepted += move;
                }
            }
        }
        while (remaining > 0 && this.cargoStacks() < MAX_CARGO_STACKS) {
            int move = Math.min(stack.getMaxStackSize(), remaining);
            ItemStack copy = stack.copy();
            copy.setCount(move);
            this.folk.courierCargo.add(copy);
            remaining -= move;
            accepted += move;
        }
        this.cleanCargo();
        return accepted;
    }

    private void depositCargo(List<IInventory> chests) {
        for (int i = 0; i < this.folk.courierCargo.size(); i++) {
            ItemStack stack = this.folk.courierCargo.get(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            for (IInventory chest : chests) {
                if (stack.isEmpty()) {
                    break;
                }
                if (!this.validInventory(chest)) {
                    continue;
                }
                int inserted = this.job.placeInJobChestPartial(chest, stack);
                if (inserted > 0) {
                    stack.shrink(inserted);
                }
            }
        }
        this.cleanCargo();
    }

    private int cargoStacks() {
        int stacks = 0;
        for (ItemStack stack : this.folk.courierCargo) {
            if (stack != null && !stack.isEmpty()) {
                int max = Math.max(1, stack.getMaxStackSize());
                stacks += Math.max(1, (stack.getCount() + max - 1) / max);
            }
        }
        return stacks;
    }

    private int cargoItems() {
        int items = 0;
        for (ItemStack stack : this.folk.courierCargo) {
            if (stack != null && !stack.isEmpty()) {
                items += stack.getCount();
            }
        }
        return items;
    }

    private boolean cargoEmpty() {
        for (ItemStack stack : this.folk.courierCargo) {
            if (stack != null && !stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean cargoFull() {
        return this.cargoStacks() >= MAX_CARGO_STACKS;
    }

    private void cleanCargo() {
        for (int i = this.folk.courierCargo.size() - 1; i >= 0; i--) {
            ItemStack stack = this.folk.courierCargo.get(i);
            if (stack == null || stack.isEmpty()) {
                this.folk.courierCargo.remove(i);
            }
        }
    }

    // ==================== 工具 ====================

    private boolean validInventory(IInventory inventory) {
        if (!(inventory instanceof TileEntity)) {
            return false;
        }
        TileEntity tile = (TileEntity) inventory;
        if (tile.getWorld() == null || tile.isInvalid()) {
            return false;
        }
        if (!tile.getWorld().isBlockLoaded(tile.getPos())) {
            return false;
        }
        return tile.getWorld().getTileEntity(tile.getPos()) == tile;
    }

    private boolean sameDimension(V3 location) {
        if (this.folk.entity == null || location == null) {
            return false;
        }
        return location.dimension == this.folk.entity.dimension;
    }

    private double distanceSq(V3 a, V3 b) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        double dz = a.z - b.z;
        return dx * dx + dy * dy + dz * dz;
    }

    private void removeDropoffInventories(List<IInventory> inventories) {
        if (inventories == null || inventories.isEmpty()) {
            return;
        }
        List<IInventory> dropoffInventories = this.job.inventoriesFindClosest(this.job.workPlace, 8);
        for (int i = inventories.size() - 1; i >= 0; i--) {
            if (this.containsIdentity(dropoffInventories, inventories.get(i))) {
                inventories.remove(i);
            }
        }
    }

    private boolean containsIdentity(List<IInventory> inventories, IInventory target) {
        if (inventories == null || target == null) {
            return false;
        }
        for (IInventory inventory : inventories) {
            if (inventory == target) {
                return true;
            }
        }
        return false;
    }

    // ==================== 状态文本 ====================

    private void setStatusKey(String key) {
        this.setStatus(this.t(key));
    }

    private void setStatus(String text) {
        this.status = text;
        this.job.folk.setStatus(text);
    }

    private String t(String key) {
        return new TextComponentTranslation(key, new Object[0]).getUnformattedText();
    }

    @Override
    public void onTaskComplete() {
        this.clearPath();
    }

    /** 一个收取点。 */
    private static final class PickupStop {
        final String key;
        final String label;
        final V3 loc;
        final int kind;
        final Set<String> outputKeys;
        final List<IInventory> inventories;

        PickupStop(String key, String label, V3 loc, int kind, Set<String> outputKeys, List<IInventory> inventories) {
            this.key = key;
            this.label = label;
            this.loc = loc;
            this.kind = kind;
            this.outputKeys = outputKeys;
            this.inventories = inventories;
        }
    }
}
