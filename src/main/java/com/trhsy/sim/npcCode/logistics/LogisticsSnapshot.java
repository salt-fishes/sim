package com.trhsy.sim.npcCode.logistics;

import com.trhsy.sim.npcCode.V3;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务端发给物流中心 GUI 的快照。
 *
 * <p>只包含展示和开关所需的数据：中心位置、总开关、费率、待处理订单、快递员数量、
 * 当日统计，以及每家店铺的已知物品和开关。客户端只发开关包，不直接改钱、改订单。</p>
 */
public class LogisticsSnapshot {
    public V3 center;
    public boolean autoDispatch = true;
    public float feePerItem = 0.005F;
    public float minFeePerTrip = 0.01F;
    public int pendingOrders = 0;
    public int courierCount = 0;
    public int todayDelivered = 0;
    public float todayFees = 0.0F;
    public boolean fundsLow = false;
    public final List<RequesterView> requesters = new ArrayList<>();

    public static class RequesterView {
        public String key = "";
        public String label = "";
        public final List<ItemView> items = new ArrayList<>();
    }

    public static class ItemView {
        public String key = "";
        public String display = "";
        public boolean enabled = true;
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("center", this.center == null ? "" : this.center.toString());
        tag.setBoolean("autoDispatch", this.autoDispatch);
        tag.setFloat("feePerItem", this.feePerItem);
        tag.setFloat("minFeePerTrip", this.minFeePerTrip);
        tag.setInteger("pendingOrders", this.pendingOrders);
        tag.setInteger("courierCount", this.courierCount);
        tag.setInteger("todayDelivered", this.todayDelivered);
        tag.setFloat("todayFees", this.todayFees);
        tag.setBoolean("fundsLow", this.fundsLow);

        NBTTagList requesterList = new NBTTagList();
        for (RequesterView requester : this.requesters) {
            if (requester == null) {
                continue;
            }
            NBTTagCompound requesterTag = new NBTTagCompound();
            requesterTag.setString("key", requester.key == null ? "" : requester.key);
            requesterTag.setString("label", requester.label == null ? "" : requester.label);
            NBTTagList itemList = new NBTTagList();
            for (ItemView item : requester.items) {
                if (item == null) {
                    continue;
                }
                NBTTagCompound itemTag = new NBTTagCompound();
                itemTag.setString("key", item.key == null ? "" : item.key);
                itemTag.setString("display", item.display == null ? "" : item.display);
                itemTag.setBoolean("enabled", item.enabled);
                itemList.appendTag(itemTag);
            }
            requesterTag.setTag("items", itemList);
            requesterList.appendTag(requesterTag);
        }
        tag.setTag("requesters", requesterList);
        return tag;
    }

    public static LogisticsSnapshot readFromNBT(NBTTagCompound tag) {
        LogisticsSnapshot snapshot = new LogisticsSnapshot();
        try {
            snapshot.center = V3.fromString(tag.getString("center"));
        } catch (Exception ignored) {
            snapshot.center = null;
        }
        snapshot.autoDispatch = !tag.hasKey("autoDispatch") || tag.getBoolean("autoDispatch");
        snapshot.feePerItem = tag.hasKey("feePerItem") ? tag.getFloat("feePerItem") : 0.005F;
        snapshot.minFeePerTrip = tag.hasKey("minFeePerTrip") ? tag.getFloat("minFeePerTrip") : 0.01F;
        snapshot.pendingOrders = tag.getInteger("pendingOrders");
        snapshot.courierCount = tag.getInteger("courierCount");
        snapshot.todayDelivered = tag.getInteger("todayDelivered");
        snapshot.todayFees = tag.getFloat("todayFees");
        snapshot.fundsLow = tag.getBoolean("fundsLow");

        NBTTagList requesterList = tag.getTagList("requesters", 10);
        for (int i = 0; i < requesterList.tagCount(); i++) {
            NBTTagCompound requesterTag = requesterList.getCompoundTagAt(i);
            RequesterView requester = new RequesterView();
            requester.key = requesterTag.getString("key");
            requester.label = requesterTag.getString("label");
            NBTTagList itemList = requesterTag.getTagList("items", 10);
            for (int j = 0; j < itemList.tagCount(); j++) {
                NBTTagCompound itemTag = itemList.getCompoundTagAt(j);
                ItemView item = new ItemView();
                item.key = itemTag.getString("key");
                item.display = itemTag.getString("display");
                item.enabled = !itemTag.hasKey("enabled") || itemTag.getBoolean("enabled");
                requester.items.add(item);
            }
            snapshot.requesters.add(requester);
        }
        return snapshot;
    }
}
