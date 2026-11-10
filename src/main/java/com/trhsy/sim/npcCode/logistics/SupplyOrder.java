package com.trhsy.sim.npcCode.logistics;

import com.trhsy.sim.npcCode.V3;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 物流中心供货订单（Beta12）。
 *
 * <p>店铺缺料时只下单，不再自己出门。订单记录“谁要什么、要多少、已经送到多少”，
 * 由快递员领取并配送。订单只持久化物品类型与数量，不建立全局库存账本。</p>
 */
public class SupplyOrder {
    public static final String PENDING = "PENDING";
    public static final String ASSIGNED = "ASSIGNED";
    public static final String PICKING = "PICKING";
    public static final String DELIVERING = "DELIVERING";
    public static final String DONE = "DONE";
    public static final String CANCELLED = "CANCELLED";

    public String id;
    /** 下单职业的工作点。 */
    public V3 requester;
    public String requesterLabel = "";
    public String requesterJob = "";
    /** 请求物品（数量为请求总量）。 */
    public final List<ItemStack> items = new ArrayList<>();
    /** itemKey -> 已送达数量。 */
    public final Map<String, Integer> delivered = new LinkedHashMap<>();
    public String status = PENDING;
    /** 已领取的快递员；未领取时为 null。 */
    public UUID courier;
    public long createdAt;
    public long updatedAt;
    public int attempts;

    public SupplyOrder() {
    }

    public SupplyOrder(V3 requester, String requesterLabel, String requesterJob, List<ItemStack> items) {
        this.id = UUID.randomUUID().toString();
        this.requester = requester;
        this.requesterLabel = requesterLabel == null ? "" : requesterLabel;
        this.requesterJob = requesterJob == null ? "" : requesterJob;
        if (items != null) {
            for (ItemStack stack : items) {
                addRequested(stack);
            }
        }
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }

    /** 把新请求合并进订单：同一物品取“请求量”和“已有请求量”的较大值，避免反复补货把数量越加越大。 */
    public void addRequested(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        String key = LogisticsService.itemKey(stack);
        for (ItemStack existing : this.items) {
            if (LogisticsService.itemKey(existing).equals(key)) {
                if (stack.getCount() > existing.getCount()) {
                    existing.setCount(stack.getCount());
                }
                return;
            }
        }
        this.items.add(stack.copy());
    }

    /** 该物品的请求总量。 */
    public int requestedCount(ItemStack stack) {
        String key = LogisticsService.itemKey(stack);
        for (ItemStack existing : this.items) {
            if (LogisticsService.itemKey(existing).equals(key)) {
                return existing.getCount();
            }
        }
        return 0;
    }

    public int requestedCount(String key) {
        for (ItemStack existing : this.items) {
            if (LogisticsService.itemKey(existing).equals(key)) {
                return existing.getCount();
            }
        }
        return 0;
    }

    /** 已经送达的数量。 */
    public int deliveredCount(ItemStack stack) {
        return deliveredCount(LogisticsService.itemKey(stack));
    }

    public int deliveredCount(String key) {
        Integer value = this.delivered.get(key);
        return value == null ? 0 : value;
    }

    /**
     * 还需要装车的数量 = 请求量 - 已送达 - 当前快递员手里已经装着的同类型货物。
     */
    public int remainingFor(ItemStack stack, List<ItemStack> cargo) {
        int requested = requestedCount(stack);
        if (requested <= 0) {
            return 0;
        }
        int already = deliveredCount(stack) + cargoCount(cargo, stack);
        return Math.max(0, requested - already);
    }

    /** 订单是否全部送达。 */
    public boolean isComplete() {
        for (ItemStack stack : this.items) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            if (deliveredCount(stack) < stack.getCount()) {
                return false;
            }
        }
        return true;
    }

    /** 是否还有未送达的请求。 */
    public boolean hasRemaining() {
        return !isComplete();
    }

    public int totalRequested() {
        int total = 0;
        for (ItemStack stack : this.items) {
            if (stack != null && !stack.isEmpty()) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public int totalDelivered() {
        int total = 0;
        for (Integer value : this.delivered.values()) {
            if (value != null) {
                total += value;
            }
        }
        return total;
    }

    public boolean isActive() {
        return PENDING.equals(this.status) || ASSIGNED.equals(this.status)
                || PICKING.equals(this.status) || DELIVERING.equals(this.status);
    }

    /** 记录实际送达。 */
    public void markDelivered(ItemStack stack, int count) {
        if (stack == null || stack.isEmpty() || count <= 0) {
            return;
        }
        String key = LogisticsService.itemKey(stack);
        int already = deliveredCount(key);
        int requested = requestedCount(key);
        int accepted = Math.min(count, Math.max(0, requested - already));
        if (accepted <= 0) {
            return;
        }
        this.delivered.put(key, already + accepted);
        this.updatedAt = System.currentTimeMillis();
    }

    /** 把订单缩短到“已送达”的数量（用于确认某个物品永远没有供货点）。 */
    public void trimToDelivered() {
        for (int i = this.items.size() - 1; i >= 0; i--) {
            ItemStack stack = this.items.get(i);
            if (stack == null || stack.isEmpty()) {
                this.items.remove(i);
                continue;
            }
            int deliveredCount = deliveredCount(stack);
            if (deliveredCount <= 0) {
                this.items.remove(i);
            } else if (deliveredCount < stack.getCount()) {
                stack.setCount(deliveredCount);
            }
        }
        this.updatedAt = System.currentTimeMillis();
    }

    private static int cargoCount(List<ItemStack> cargo, ItemStack wanted) {
        if (cargo == null || wanted == null || wanted.isEmpty()) {
            return 0;
        }
        String key = LogisticsService.itemKey(wanted);
        int count = 0;
        for (ItemStack stack : cargo) {
            if (stack != null && !stack.isEmpty() && LogisticsService.itemKey(stack).equals(key)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", this.id == null ? "" : this.id);
        tag.setString("requester", this.requester == null ? "" : this.requester.toString());
        tag.setString("label", this.requesterLabel == null ? "" : this.requesterLabel);
        tag.setString("job", this.requesterJob == null ? "" : this.requesterJob);
        tag.setString("status", this.status == null ? PENDING : this.status);
        tag.setString("courier", this.courier == null ? "" : this.courier.toString());
        tag.setLong("created", this.createdAt);
        tag.setLong("updated", this.updatedAt);
        tag.setInteger("attempts", this.attempts);

        NBTTagList itemList = new NBTTagList();
        for (ItemStack stack : this.items) {
            if (stack != null && !stack.isEmpty()) {
                itemList.appendTag(stack.writeToNBT(new NBTTagCompound()));
            }
        }
        tag.setTag("items", itemList);

        NBTTagCompound deliveredTag = new NBTTagCompound();
        for (Map.Entry<String, Integer> entry : this.delivered.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                deliveredTag.setInteger(entry.getKey(), entry.getValue());
            }
        }
        tag.setTag("delivered", deliveredTag);
        return tag;
    }

    public static SupplyOrder readFromNBT(NBTTagCompound tag) {
        SupplyOrder order = new SupplyOrder();
        order.id = tag.getString("id");
        try {
            order.requester = V3.fromString(tag.getString("requester"));
        } catch (Exception ignored) {
            order.requester = null;
        }
        order.requesterLabel = tag.getString("label");
        order.requesterJob = tag.getString("job");
        order.status = tag.getString("status");
        if (order.status == null || order.status.isEmpty()) {
            order.status = PENDING;
        }
        String courierId = tag.getString("courier");
        if (courierId != null && !courierId.isEmpty()) {
            try {
                order.courier = UUID.fromString(courierId);
            } catch (Exception ignored) {
                order.courier = null;
            }
        }
        order.createdAt = tag.getLong("created");
        order.updatedAt = tag.getLong("updated");
        order.attempts = tag.getInteger("attempts");

        NBTTagList itemList = tag.getTagList("items", 10);
        for (int i = 0; i < itemList.tagCount(); i++) {
            ItemStack stack = new ItemStack(itemList.getCompoundTagAt(i));
            if (!stack.isEmpty()) {
                order.items.add(stack);
            }
        }
        NBTTagCompound deliveredTag = tag.getCompoundTag("delivered");
        for (String key : deliveredTag.getKeySet()) {
            order.delivered.put(key, deliveredTag.getInteger(key));
        }
        if (order.id == null || order.id.isEmpty()) {
            order.id = UUID.randomUUID().toString();
        }
        return order;
    }
}
