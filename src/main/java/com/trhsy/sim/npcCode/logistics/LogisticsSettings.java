package com.trhsy.sim.npcCode.logistics;

import com.trhsy.sim.npcCode.V3;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 一个物流中心的派送设置。
 *
 * <p>玩家在物流中心 GUI 里逐物品勾选“是否允许快递员派送”。默认全部开启；
 * 关闭时只从 {@link RequesterSettings#enabledItems} 里移除，不会删除已知物品，
 * 所以重新打开时还能看到并勾回来。</p>
 */
public class LogisticsSettings {
    /** 物流中心控制箱/工作点位置。 */
    public V3 center;
    public boolean autoDispatch = true;
    public float feePerItem = 0.005F;
    public float minFeePerTrip = 0.01F;

    /** 统计：游戏日 + 当日已送件数/配送费。 */
    public int statsDay = -1;
    public int todayDelivered = 0;
    public float todayFees = 0.0F;

    /** requesterKey(V3.toString) -> 该店铺的已知物品与开关。 */
    public final Map<String, RequesterSettings> requesters = new LinkedHashMap<>();

    public LogisticsSettings() {
    }

    public LogisticsSettings(V3 center) {
        this.center = center;
    }

    public static class RequesterSettings {
        public String label = "";
        public String jobName = "";
        /** itemKey -> 展示用物品（数量固定为 1）。 */
        public final Map<String, ItemStack> knownItems = new LinkedHashMap<>();
        /** 允许派送的 itemKey。 */
        public final Set<String> enabledItems = new LinkedHashSet<>();

        public boolean isEnabled(String itemKey) {
            return itemKey != null && this.enabledItems.contains(itemKey);
        }
    }

    /** 登记的店铺；第一次下单时创建。 */
    public RequesterSettings requester(String key, String label, String jobName) {
        RequesterSettings value = this.requesters.get(key);
        if (value == null) {
            value = new RequesterSettings();
            this.requesters.put(key, value);
        }
        if (label != null && !label.isEmpty()) {
            value.label = label;
        }
        if (jobName != null && !jobName.isEmpty()) {
            value.jobName = jobName;
        }
        return value;
    }

    /** 记录店铺请求过的物品，默认开启派送。 */
    public RequesterSettings registerRequest(String key, String label, String jobName, List<ItemStack> items) {
        RequesterSettings value = requester(key, label, jobName);
        if (items != null) {
            for (ItemStack stack : items) {
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                String itemKey = LogisticsService.itemKey(stack);
                boolean isNew = !value.knownItems.containsKey(itemKey);
                if (isNew) {
                    ItemStack display = stack.copy();
                    display.setCount(1);
                    value.knownItems.put(itemKey, display);
                    // 新物品默认开启；已有物品保留玩家当前开关。
                    value.enabledItems.add(itemKey);
                }
            }
        }
        return value;
    }

    public boolean isEnabled(String requesterKey, String itemKey) {
        RequesterSettings value = this.requesters.get(requesterKey);
        return value == null || value.isEnabled(itemKey);
    }

    public void setEnabled(String requesterKey, String itemKey, boolean enabled) {
        RequesterSettings value = this.requesters.get(requesterKey);
        if (value == null || itemKey == null) {
            return;
        }
        if (enabled) {
            value.enabledItems.add(itemKey);
        } else {
            value.enabledItems.remove(itemKey);
        }
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("center", this.center == null ? "" : this.center.toString());
        tag.setBoolean("autoDispatch", this.autoDispatch);
        tag.setFloat("feePerItem", this.feePerItem);
        tag.setFloat("minFeePerTrip", this.minFeePerTrip);
        tag.setInteger("statsDay", this.statsDay);
        tag.setInteger("todayDelivered", this.todayDelivered);
        tag.setFloat("todayFees", this.todayFees);

        NBTTagList requesterList = new NBTTagList();
        for (Map.Entry<String, RequesterSettings> entry : this.requesters.entrySet()) {
            RequesterSettings value = entry.getValue();
            if (value == null) {
                continue;
            }
            NBTTagCompound requesterTag = new NBTTagCompound();
            requesterTag.setString("key", entry.getKey());
            requesterTag.setString("label", value.label == null ? "" : value.label);
            requesterTag.setString("job", value.jobName == null ? "" : value.jobName);

            NBTTagList itemList = new NBTTagList();
            for (Map.Entry<String, ItemStack> itemEntry : value.knownItems.entrySet()) {
                if (itemEntry.getValue() == null || itemEntry.getValue().isEmpty()) {
                    continue;
                }
                NBTTagCompound itemTag = new NBTTagCompound();
                itemTag.setString("key", itemEntry.getKey());
                itemTag.setTag("stack", itemEntry.getValue().writeToNBT(new NBTTagCompound()));
                itemTag.setBoolean("enabled", value.enabledItems.contains(itemEntry.getKey()));
                itemList.appendTag(itemTag);
            }
            requesterTag.setTag("items", itemList);
            requesterList.appendTag(requesterTag);
        }
        tag.setTag("requesters", requesterList);
        return tag;
    }

    public static LogisticsSettings readFromNBT(NBTTagCompound tag) {
        LogisticsSettings settings = new LogisticsSettings();
        try {
            settings.center = V3.fromString(tag.getString("center"));
        } catch (Exception ignored) {
            settings.center = null;
        }
        settings.autoDispatch = !tag.hasKey("autoDispatch") || tag.getBoolean("autoDispatch");
        settings.feePerItem = tag.hasKey("feePerItem") ? tag.getFloat("feePerItem") : 0.005F;
        settings.minFeePerTrip = tag.hasKey("minFeePerTrip") ? tag.getFloat("minFeePerTrip") : 0.01F;
        settings.statsDay = tag.getInteger("statsDay");
        settings.todayDelivered = tag.getInteger("todayDelivered");
        settings.todayFees = tag.getFloat("todayFees");

        NBTTagList requesterList = tag.getTagList("requesters", 10);
        for (int i = 0; i < requesterList.tagCount(); i++) {
            NBTTagCompound requesterTag = requesterList.getCompoundTagAt(i);
            String key = requesterTag.getString("key");
            if (key == null || key.isEmpty()) {
                continue;
            }
            RequesterSettings value = new RequesterSettings();
            value.label = requesterTag.getString("label");
            value.jobName = requesterTag.getString("job");
            NBTTagList itemList = requesterTag.getTagList("items", 10);
            for (int j = 0; j < itemList.tagCount(); j++) {
                NBTTagCompound itemTag = itemList.getCompoundTagAt(j);
                String itemKey = itemTag.getString("key");
                ItemStack stack = new ItemStack(itemTag.getCompoundTag("stack"));
                if (itemKey == null || itemKey.isEmpty() || stack.isEmpty()) {
                    continue;
                }
                value.knownItems.put(itemKey, stack);
                if (itemTag.getBoolean("enabled")) {
                    value.enabledItems.add(itemKey);
                }
            }
            settings.requesters.put(key, value);
        }
        return settings;
    }

    /** 兼容旧代码的只读列表。 */
    public List<String> requesterKeys() {
        return new ArrayList<>(this.requesters.keySet());
    }
}
