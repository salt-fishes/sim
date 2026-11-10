package com.trhsy.sim.npcCode.task;

import com.trhsy.sim.npcCode.V3;
import com.trhsy.sim.npcCode.job.Job;
import com.trhsy.sim.npcCode.logistics.LogisticsService;
import com.trhsy.sim.npcCode.logistics.SupplyOrder;
import com.trhsy.sim.npcCode.store.SimStore;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.List;

/**
 * 向物流中心下单并等待快递员送货（Beta12）。
 *
 * <p>替代原来老板自己跑供货点的 {@code JobTaskCollectItems + JobTaskUnloadItems}。
 * 任务只负责：计算缺多少、下单、等第一批货到箱子；货到后把后续生产/售卖任务放行。
 * 不保留任何“老板自取”兜底。</p>
 */
public class JobTaskRequestSupply extends JobTask {
    /** 每次补货的请求清单（数量为希望补到的总量）。 */
    private final List<ItemStack> wanted;
    private String orderId;
    private int baselineDelivered = 0;
    private boolean courierPresent = true;
    private transient long lastRequestTime = 0L;
    private transient long lastStatusTime = 0L;
    private static final long REQUEST_RETRY_MS = 30000L;

    public JobTaskRequestSupply(Job j, long ms, List<ItemStack> wanted) {
        super(j, ms);
        this.wanted = wanted;
    }

    @Override
    public void onTaskBegin() {
        this.folk.stayPut = true;
        this.folk.forceMoveToXYZ(this.job.workPlace);
        this.request();
    }

    private void request() {
        if (this.wanted == null || this.wanted.isEmpty()) {
            this.completeTask();
            return;
        }
        List<ItemStack> missing = this.job.missingSupplies(this.wanted);
        if (missing.isEmpty()) {
            // 箱子里已经够本轮用量，不需要再下单。
            this.completeTask();
            return;
        }
        V3 center = LogisticsService.findCenter(this.job);
        if (center == null) {
            this.setStatusKey("container.sim.job.supply.no_center");
            this.completeTask();
            return;
        }
        long now = System.currentTimeMillis();
        if (this.lastRequestTime > 0L && now - this.lastRequestTime < REQUEST_RETRY_MS) {
            return;
        }
        this.lastRequestTime = now;
        SupplyOrder order = this.job.requestSupply(missing);
        if (order == null) {
            this.setStatusKey("container.sim.job.supply.disabled");
            this.completeTask();
            return;
        }
        this.orderId = order.id;
        this.baselineDelivered = order.totalDelivered();
        this.courierPresent = LogisticsService.hasCourier(center);
        this.setStatusKey(this.courierPresent
                ? "container.sim.job.supply.waiting" : "container.sim.job.supply.no_courier");
    }

    @Override
    public void onUpdate() {
        if (this.orderId == null) {
            // 上一次订单被取消/还没创建：保持等待，按冷却重新下单，不进入会卡住的生产任务。
            this.setStatusKey("container.sim.job.supply.waiting");
            this.request();
            return;
        }
        SimStore store = SimStore.get(this.job.jobWorld);
        SupplyOrder order = LogisticsService.getOrder(store, this.orderId);
        if (order == null) {
            // 订单已被完成或移除，直接放行后续任务。
            this.completeTask();
            return;
        }
        if (SupplyOrder.DONE.equals(order.status)) {
            this.completeTask();
            return;
        }
        if (SupplyOrder.CANCELLED.equals(order.status)) {
            if (order.totalDelivered() > this.baselineDelivered) {
                this.completeTask();
                return;
            }
            // 没送到任何货：不要进入“等原料”的生产任务，等冷却后重新下单。
            this.orderId = null;
            this.lastRequestTime = System.currentTimeMillis();
            this.setStatusKey("container.sim.job.supply.waiting");
            return;
        }
        if (order.totalDelivered() > this.baselineDelivered) {
            // 本次下单后收到了新货，就可以先开工；剩余部分仍由快递员继续送。
            this.completeTask();
            return;
        }
        long now = System.currentTimeMillis();
        if (now - this.lastStatusTime >= 1000L) {
            this.lastStatusTime = now;
            this.setStatusKey(this.courierPresent
                    ? "container.sim.job.supply.waiting" : "container.sim.job.supply.no_courier");
        }
        this.request();
    }

    private void setStatusKey(String key) {
        this.folk.setStatus(new TextComponentTranslation(key, new Object[0]).getUnformattedText());
    }

    @Override
    public void onTaskComplete() {
        this.folk.stayPut = false;
    }
}
