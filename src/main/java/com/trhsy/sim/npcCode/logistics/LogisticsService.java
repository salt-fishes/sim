package com.trhsy.sim.npcCode.logistics;

import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.NpcData;
import com.trhsy.sim.npcCode.V3;
import com.trhsy.sim.npcCode.build.Building;
import com.trhsy.sim.npcCode.economy.Economy;
import com.trhsy.sim.npcCode.job.Job;
import com.trhsy.sim.npcCode.job.JobCourier;
import com.trhsy.sim.npcCode.store.SimStore;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 物流中心调度（Beta12）。
 *
 * <p>店铺只负责下单，快递员只负责领单/取货/送货，钱和订单都走服务端。
 * 这里不建立库存账本：货物仍然实际躺在箱子里，订单只记录“请求/已送达”数量。</p>
 */
public final class LogisticsService {
    /** 订单被领走后，超过这个时间没有进展就释放回待领取。 */
    private static final long ORDER_LEASE_MS = 10 * 60 * 1000L;
    /** 同一订单最多重试次数，超过后取消，避免永远占用。 */
    private static final int MAX_ATTEMPTS = 5;
    private static long lastFundsWarn = 0L;

    private LogisticsService() {
    }

    /** 物品类型键：注册名 + metadata。 */
    public static String itemKey(ItemStack stack) {
        return SimStore.itemKey(stack);
    }

    public static String courierJobName() {
        return new TextComponentTranslation("container.sim.Vocation10", new Object[0]).getUnformattedText();
    }

    /**
     * 找店铺对应的物流中心：优先找最近的快递员工作点，其次找最近的快递员建筑。
     * 没有物流中心的旧存档返回 null，店铺会显示“没有可用的物流中心”，不会自己跑腿。
     */
    public static V3 findCenter(Job requester) {
        if (requester == null || requester.workPlace == null) {
            return null;
        }
        V3 best = null;
        double bestDistance = Double.MAX_VALUE;
        for (NpcData npc : ModSimLoader.folks) {
            if (npc == null || !(npc.job instanceof JobCourier) || npc.job.workPlace == null) {
                continue;
            }
            V3 site = npc.job.workPlace;
            if (site.dimension != requester.workPlace.dimension) {
                continue;
            }
            double distance = site.distanceSq(requester.workPlace);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = new V3(site.x, site.y, site.z, site.dimension);
            }
        }
        if (best != null) {
            return best;
        }
        String courier = courierJobName();
        List<Building> buildings = ModSimLoader.getClosestBuildingByJob(courier, requester.workPlace);
        if (buildings != null) {
            for (Building building : buildings) {
                if (building == null || building.controlXYZ == null) {
                    continue;
                }
                if (building.dimension != requester.workPlace.dimension) {
                    continue;
                }
                V3 site = new V3(building.controlXYZ.x, building.controlXYZ.y,
                        building.controlXYZ.z, building.dimension);
                double distance = site.distanceSq(requester.workPlace);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = site;
                }
            }
        }
        return best;
    }

    /** 把控制箱位置归一到快递员真实工作点（雇佣时通常记录为控制箱上方一格）。 */
    public static V3 normalizeCenter(V3 center) {
        if (center == null) {
            return null;
        }
        V3 best = center;
        double bestDistance = 36.0D;
        for (NpcData npc : ModSimLoader.folks) {
            if (npc == null || !(npc.job instanceof JobCourier) || npc.job.workPlace == null) {
                continue;
            }
            V3 site = npc.job.workPlace;
            if (site.dimension != center.dimension) {
                continue;
            }
            double distance = site.distanceSq(center);
            if (distance <= bestDistance) {
                bestDistance = distance;
                best = site;
            }
        }
        return best;
    }

    /** 物流中心附近是否有在岗快递员。 */
    public static boolean hasCourier(V3 center) {
        center = normalizeCenter(center);
        if (center == null) {
            return false;
        }
        for (NpcData npc : ModSimLoader.folks) {
            if (npc == null || !(npc.job instanceof JobCourier) || npc.job.workPlace == null) {
                continue;
            }
            if (npc.job.workPlace.dimension == center.dimension
                    && npc.job.workPlace.distanceSq(center) < 16.0D) {
                return true;
            }
        }
        return false;
    }

    /** 店铺下单。返回已有/新建的订单；没有物流中心、总开关关闭或全部物品被禁用时返回 null。 */
    public static SupplyOrder requestSupply(Job requester, List<ItemStack> items) {
        if (requester == null || requester.workPlace == null || items == null || items.isEmpty()) {
            return null;
        }
        V3 center = findCenter(requester);
        if (center == null) {
            return null;
        }
        SimStore store = SimStore.get(requester.jobWorld);
        if (store == null) {
            return null;
        }
        LogisticsSettings settings = store.getOrCreateLogisticsSettings(center);
        if (!settings.autoDispatch) {
            return null;
        }
        String requesterKey = requester.workPlace.toString();
        settings.registerRequest(requesterKey, requester.jobName, requester.jobName, items);

        List<ItemStack> enabled = new ArrayList<>();
        for (ItemStack stack : items) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            if (settings.isEnabled(requesterKey, itemKey(stack))) {
                enabled.add(stack.copy());
            }
        }
        if (enabled.isEmpty()) {
            store.markDirty();
            return null;
        }

        SupplyOrder existing = findActiveOrder(store, requester.workPlace);
        if (existing != null && existing.isComplete()) {
            store.removeSupplyOrder(existing.id);
            existing = null;
        }
        // 清理同一店铺已经取消且没有在途货物的旧订单，避免反复重试时无限堆积。
        if (existing == null) {
            for (SupplyOrder old : new ArrayList<>(store.getSupplyOrders().values())) {
                if (old != null && SupplyOrder.CANCELLED.equals(old.status)
                        && old.requester != null && old.requester.equals(requester.workPlace)
                        && old.totalDelivered() == 0) {
                    store.removeSupplyOrder(old.id);
                }
            }
        }
        if (existing != null) {
            for (ItemStack stack : enabled) {
                existing.addRequested(stack);
            }
            existing.updatedAt = System.currentTimeMillis();
            store.markDirty();
            return existing;
        }

        SupplyOrder order = new SupplyOrder(requester.workPlace, requester.jobName,
                requester.jobName, enabled);
        store.putSupplyOrder(order);
        return order;
    }

    public static SupplyOrder findActiveOrder(SimStore store, V3 requester) {
        if (store == null || requester == null) {
            return null;
        }
        for (SupplyOrder order : store.getSupplyOrders().values()) {
            if (order == null || !order.isActive() || order.requester == null) {
                continue;
            }
            if (order.requester.equals(requester)) {
                return order;
            }
        }
        return null;
    }

    public static SupplyOrder getOrder(SimStore store, String id) {
        if (store == null || id == null) {
            return null;
        }
        return store.getSupplyOrder(id);
    }

    /**
     * 快递员领单。一次只领一个：最近创建且未分配的订单优先。
     */
    public static SupplyOrder claimOrder(SimStore store, JobCourier courier) {
        if (store == null || courier == null || courier.workPlace == null) {
            return null;
        }
        LogisticsSettings settings = store.getOrCreateLogisticsSettings(courier.workPlace);
        if (!settings.autoDispatch) {
            return null;
        }
        if (ModSimLoader.gamemode != 1 && ModSimLoader.money < settings.minFeePerTrip) {
            long now = System.currentTimeMillis();
            if (now - lastFundsWarn > 60000L) {
                lastFundsWarn = now;
                ModSimLoader.log.warn("物流：城市余额不足，暂不派单");
            }
            return null;
        }
        expireStaleOrders(store, courier.workPlace);

        List<SupplyOrder> candidates = new ArrayList<>();
        for (SupplyOrder order : store.getSupplyOrders().values()) {
            if (order == null || order.requester == null || !SupplyOrder.PENDING.equals(order.status)) {
                continue;
            }
            if (order.requester.dimension != courier.workPlace.dimension) {
                continue;
            }
            if (order.attempts >= MAX_ATTEMPTS) {
                cancelOrder(store, order);
                continue;
            }
            candidates.add(order);
        }
        candidates.sort((a, b) -> {
            double da = a.requester == null ? Double.MAX_VALUE : a.requester.distanceSq(courier.workPlace);
            double db = b.requester == null ? Double.MAX_VALUE : b.requester.distanceSq(courier.workPlace);
            int distance = Double.compare(da, db);
            return distance != 0 ? distance : Long.compare(a.createdAt, b.createdAt);
        });
        for (SupplyOrder order : candidates) {
            boolean disabled = false;
            for (ItemStack stack : order.items) {
                if (stack != null && !stack.isEmpty()
                        && !settings.isEnabled(order.requester.toString(), itemKey(stack))) {
                    disabled = true;
                    break;
                }
            }
            if (disabled) {
                cancelOrder(store, order);
                continue;
            }
            order.status = SupplyOrder.ASSIGNED;
            order.courier = courier.folk == null ? null : courier.folk.ID;
            order.attempts++;
            order.updatedAt = System.currentTimeMillis();
            store.markDirty();
            return order;
        }
        return null;
    }

    /** 领到订单后，标记正在取货。 */
    public static void markPicking(SimStore store, SupplyOrder order) {
        if (store == null || order == null || !order.isActive()) {
            return;
        }
        order.status = SupplyOrder.PICKING;
        order.updatedAt = System.currentTimeMillis();
        store.markDirty();
    }

    /** 货已装车，标记正在送货。 */
    public static void markDelivering(SimStore store, SupplyOrder order) {
        if (store == null || order == null || !order.isActive()) {
            return;
        }
        order.status = SupplyOrder.DELIVERING;
        order.updatedAt = System.currentTimeMillis();
        store.markDirty();
    }

    /** 释放订单回待领取（快递员被卡住/货物先运回中心时）。 */
    public static void releaseOrder(SimStore store, SupplyOrder order) {
        if (store == null || order == null) {
            return;
        }
        if (!order.isActive()) {
            store.removeSupplyOrder(order.id);
            return;
        }
        order.status = SupplyOrder.PENDING;
        order.courier = null;
        order.updatedAt = System.currentTimeMillis();
        store.markDirty();
    }

    /** 取消订单：不再派送，但保留记录到本轮配送结束，便于结算已送达部分。 */
    public static void cancelOrder(SimStore store, SupplyOrder order) {
        if (store == null || order == null) {
            return;
        }
        order.status = SupplyOrder.CANCELLED;
        order.courier = null;
        order.updatedAt = System.currentTimeMillis();
        store.markDirty();
    }

    /** 订单完成：从存档移除。 */
    public static void finishOrder(SimStore store, SupplyOrder order) {
        if (store == null || order == null) {
            return;
        }
        store.removeSupplyOrder(order.id);
    }

    /**
     * 按实际送达件数结算配送费。
     *
     * @return 实际支付金额；没付（创造模式/余额不足）返回 0。
     */
    public static float payDelivery(Job courier, V3 center, int delivered) {
        if (courier == null || center == null || delivered <= 0) {
            return 0.0F;
        }
        SimStore store = SimStore.get(courier.jobWorld);
        if (store == null) {
            return 0.0F;
        }
        LogisticsSettings settings = store.getOrCreateLogisticsSettings(center);
        float fee = Math.max(settings.feePerItem * (float) delivered, settings.minFeePerTrip);
        if (settings.statsDay != store.gameDay) {
            settings.statsDay = store.gameDay;
            settings.todayDelivered = 0;
            settings.todayFees = 0.0F;
        }
        settings.todayDelivered += delivered;
        if (ModSimLoader.gamemode != 1) {
            boolean paid = Economy.cityPaysNpc(courier.folk, fee, "配送费 " + delivered + " 件");
            if (!paid) {
                ModSimLoader.log.warn("物流：城市余额不足，本次 {} 件配送费 {} 未支付", delivered,
                        String.format("%.2f", fee));
                store.markDirty();
                return 0.0F;
            }
            settings.todayFees += fee;
            store.markDirty();
            return fee;
        }
        store.markDirty();
        return 0.0F;
    }

    /** 快递员被解雇时，把它名下所有未完成订单释放回待领取。 */
    public static void releaseOrdersForCourier(SimStore store, UUID courierId) {
        if (store == null || courierId == null) {
            return;
        }
        for (SupplyOrder order : store.getSupplyOrders().values()) {
            if (order != null && order.isActive() && courierId.equals(order.courier)) {
                order.status = SupplyOrder.PENDING;
                order.courier = null;
                order.updatedAt = System.currentTimeMillis();
                store.markDirty();
            }
        }
    }

    /** 释放超时/快递员已不存在的订单。 */
    public static void expireStaleOrders(SimStore store, V3 center) {
        if (store == null) {
            return;
        }
        long now = System.currentTimeMillis();
        for (SupplyOrder order : new ArrayList<>(store.getSupplyOrders().values())) {
            if (order == null || !order.isActive()) {
                continue;
            }
            boolean stale = now - order.updatedAt > ORDER_LEASE_MS;
            boolean courierGone = order.courier != null && findFolk(order.courier) == null;
            if (stale || courierGone) {
                if (order.attempts >= MAX_ATTEMPTS) {
                    cancelOrder(store, order);
                } else {
                    order.status = SupplyOrder.PENDING;
                    order.courier = null;
                    order.updatedAt = now;
                    store.markDirty();
                }
            }
        }
    }

    public static NpcData findFolk(UUID id) {
        if (id == null) {
            return null;
        }
        return ModSimLoader.getFolkDataByUID(id);
    }

    /** 物流中心快照，供 GUI 展示。 */
    public static LogisticsSnapshot snapshot(SimStore store, V3 center) {
        LogisticsSnapshot snapshot = new LogisticsSnapshot();
        center = normalizeCenter(center);
        snapshot.center = center;
        if (store == null || center == null) {
            return snapshot;
        }
        LogisticsSettings settings = store.getOrCreateLogisticsSettings(center);
        snapshot.autoDispatch = settings.autoDispatch;
        snapshot.feePerItem = settings.feePerItem;
        snapshot.minFeePerTrip = settings.minFeePerTrip;
        snapshot.todayDelivered = settings.todayDelivered;
        snapshot.todayFees = settings.todayFees;
        snapshot.fundsLow = ModSimLoader.gamemode != 1 && ModSimLoader.money < settings.minFeePerTrip;
        snapshot.pendingOrders = 0;
        for (SupplyOrder order : store.getSupplyOrders().values()) {
            if (order != null && order.isActive() && order.requester != null
                    && order.requester.dimension == center.dimension) {
                snapshot.pendingOrders++;
            }
        }
        snapshot.courierCount = 0;
        for (NpcData npc : ModSimLoader.folks) {
            if (npc != null && npc.job instanceof JobCourier && npc.job.workPlace != null
                    && npc.job.workPlace.dimension == center.dimension
                    && npc.job.workPlace.distanceSq(center) < 16.0D) {
                snapshot.courierCount++;
            }
        }
        for (LogisticsSettings.RequesterSettings requester : settings.requesters.values()) {
            if (requester == null) {
                continue;
            }
            LogisticsSnapshot.RequesterView view = new LogisticsSnapshot.RequesterView();
            view.key = findRequesterKey(settings, requester);
            view.label = requester.label == null ? "" : requester.label;
            try {
                V3 site = V3.fromString(view.key);
                view.label = view.label + " " + (int) site.x + "," + (int) site.z;
            } catch (Exception ignored) {
                // 旧数据坐标损坏时只显示职业名
            }
            for (java.util.Map.Entry<String, ItemStack> entry : requester.knownItems.entrySet()) {
                if (entry.getValue() == null || entry.getValue().isEmpty()) {
                    continue;
                }
                LogisticsSnapshot.ItemView item = new LogisticsSnapshot.ItemView();
                item.key = entry.getKey();
                // 物品显示名由客户端按 itemKey 解析，避免专用服务器用英文名回传。
                item.display = "";
                item.enabled = requester.enabledItems.contains(entry.getKey());
                view.items.add(item);
            }
            snapshot.requesters.add(view);
        }
        return snapshot;
    }

    private static String findRequesterKey(LogisticsSettings settings, LogisticsSettings.RequesterSettings target) {
        for (java.util.Map.Entry<String, LogisticsSettings.RequesterSettings> entry : settings.requesters.entrySet()) {
            if (entry.getValue() == target) {
                return entry.getKey();
            }
        }
        return "";
    }

    /** 切换某个店铺的某个物品是否允许派送。 */
    public static boolean toggleItem(SimStore store, V3 center, String requesterKey, String itemKey, boolean enabled) {
        if (store == null || center == null || requesterKey == null || itemKey == null) {
            return false;
        }
        center = normalizeCenter(center);
        LogisticsSettings settings = store.getOrCreateLogisticsSettings(center);
        if (!settings.requesters.containsKey(requesterKey)) {
            return false;
        }
        settings.setEnabled(requesterKey, itemKey, enabled);
        store.markDirty();
        return true;
    }

    /** 切换物流中心总开关。 */
    public static void setAutoDispatch(SimStore store, V3 center, boolean enabled) {
        if (store == null || center == null) {
            return;
        }
        center = normalizeCenter(center);
        store.getOrCreateLogisticsSettings(center).autoDispatch = enabled;
        store.markDirty();
    }
}
