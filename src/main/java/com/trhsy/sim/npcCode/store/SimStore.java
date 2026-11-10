package com.trhsy.sim.npcCode.store;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import com.trhsy.sim.npcCode.V3;
import com.trhsy.sim.npcCode.logistics.LogisticsSettings;
import com.trhsy.sim.npcCode.logistics.SupplyOrder;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 模拟世界数据（P1）。
 *
 * <p>用原版 {@link WorldSavedData} + {@link NBTTagCompound} 取代 {@code .sk2} 行文本：
 * 存档路径、原子写入、类型信息由原版负责；NPC 记录以 UUID 为键，读写都不再经过任何
 * 本地化字符串。旧 {@code sim/npc/*.sk2} 与 {@code sim/settings.sk2} 由
 * {@code SimmodeStart} 在加载时删除（本版本不兼容旧档）。
 */
public class SimStore extends WorldSavedData {
    public static final String DATA_NAME = "sim_store";

    /** UUID -> NPC 的 NBT 记录。 */
    private final Map<UUID, NBTTagCompound> npcs = new LinkedHashMap<>();

    /**
     * 物流收货点：工作地点 -> 该地点产生过的成品/产物类型。
     *
     * <p>只记“产物类型”，不记数量；快递员按这个白名单从该地点附近的箱子取货，
     * 所以不会把面包师箱子里的小麦、玻璃匠箱子里的沙子一起搬走。</p>
     */
    private final Map<String, LogisticsPoint> logistics = new LinkedHashMap<>();

    /** 一个物流收货点。 */
    public static class LogisticsPoint {
        public final V3 loc;
        public String label = "";
        public final Set<String> items = new HashSet<>();

        public LogisticsPoint(V3 loc) {
            this.loc = loc;
        }
    }

    /** 物流中心设置：centerKey -> 设置。 */
    private final Map<String, LogisticsSettings> logisticsSettings = new LinkedHashMap<>();

    /** 供货订单：orderId -> 订单。 */
    private final Map<String, SupplyOrder> supplyOrders = new LinkedHashMap<>();

    /** 经济与日历。 */
    public float money = 10.0F;
    public int dayOfWeek = 0;
    public int gameDay = 0;
    public int gamemode = 999;

    /** MapStorage 反射构造需要 String 构造器。 */
    public SimStore(String name) {
        super(name);
    }

    public SimStore() {
        super(DATA_NAME);
    }

    /** 取（或创建）服务端主世界的数据。客户端/非主世界返回 null。 */
    public static SimStore get(World world) {
        if (world == null || world.isRemote || world.provider.getDimension() != 0) {
            return null;
        }
        MapStorage storage = world.getPerWorldStorage();
        SimStore data = (SimStore) storage.getOrLoadData(SimStore.class, DATA_NAME);
        if (data == null) {
            data = new SimStore();
            storage.setData(DATA_NAME, data);
            data.markDirty();
        }
        return data;
    }

    public Map<UUID, NBTTagCompound> getNpcs() {
        return npcs;
    }

    public NBTTagCompound getNpc(UUID id) {
        return id == null ? null : npcs.get(id);
    }

    public void putNpc(UUID id, NBTTagCompound tag) {
        if (id == null || tag == null) {
            return;
        }
        npcs.put(id, tag);
        markDirty();
    }

    public void removeNpc(UUID id) {
        if (id != null && npcs.remove(id) != null) {
            markDirty();
        }
    }

    /** 所有物流收货点（只读遍历用）。 */
    public Map<String, LogisticsPoint> getLogisticsPoints() {
        return logistics;
    }

    private String centerKey(V3 center) {
        return center == null ? "" : center.toString();
    }

    /** 取物流中心设置；没有时返回 null。 */
    public LogisticsSettings getLogisticsSettings(V3 center) {
        return center == null ? null : this.logisticsSettings.get(centerKey(center));
    }

    /** 取（或创建）物流中心设置。 */
    public LogisticsSettings getOrCreateLogisticsSettings(V3 center) {
        if (center == null) {
            return null;
        }
        String key = centerKey(center);
        LogisticsSettings settings = this.logisticsSettings.get(key);
        if (settings == null) {
            settings = new LogisticsSettings(center);
            this.logisticsSettings.put(key, settings);
            markDirty();
        }
        return settings;
    }

    /** 所有供货订单（只读遍历用）。 */
    public Map<String, SupplyOrder> getSupplyOrders() {
        return supplyOrders;
    }

    public SupplyOrder getSupplyOrder(String id) {
        return id == null ? null : this.supplyOrders.get(id);
    }

    public void putSupplyOrder(SupplyOrder order) {
        if (order == null || order.id == null || order.id.isEmpty()) {
            return;
        }
        this.supplyOrders.put(order.id, order);
        markDirty();
    }

    public void removeSupplyOrder(String id) {
        if (id != null && this.supplyOrders.remove(id) != null) {
            markDirty();
        }
    }

    /**
     * 记录某工作地点产出过的一种物品。快递员只从该地点附近箱子拿这些物品。
     */
    public void recordLogisticsOutput(V3 workplace, String label, ItemStack stack) {
        if (workplace == null || stack == null || stack.isEmpty()) {
            return;
        }
        String key = workplace.toString();
        LogisticsPoint point = this.logistics.get(key);
        if (point == null) {
            point = new LogisticsPoint(workplace);
            this.logistics.put(key, point);
        }
        if (label != null && !label.isEmpty() && !label.equals(point.label)) {
            point.label = label;
            markDirty();
        }
        if (point.items.add(itemKey(stack))) {
            markDirty();
        }
    }

    /** 物品类型键：注册名 + metadata，足以区分绝大多数原版/模组产物。 */
    public static String itemKey(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        String registry = stack.getItem().getRegistryName() == null
                ? stack.getItem().getUnlocalizedName() : stack.getItem().getRegistryName().toString();
        return registry + "@" + stack.getMetadata();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        npcs.clear();
        money = nbt.hasKey("money") ? nbt.getFloat("money") : 10.0F;
        dayOfWeek = nbt.getInteger("dayOfWeek");
        gameDay = nbt.getInteger("gameDay");
        gamemode = nbt.hasKey("gamemode") ? nbt.getInteger("gamemode") : 999;
        NBTTagList list = nbt.getTagList("npcs", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            if (!tag.hasKey("uuid")) {
                continue;
            }
            try {
                UUID id = UUID.fromString(tag.getString("uuid"));
                npcs.put(id, tag);
            } catch (Exception ignored) {
                // 单个坏记录不影响其余记录
            }
        }
        // 物流收货点
        this.logistics.clear();
        NBTTagList logisticsList = nbt.getTagList("logistics", 10);
        for (int i = 0; i < logisticsList.tagCount(); i++) {
            NBTTagCompound tag = logisticsList.getCompoundTagAt(i);
            try {
                V3 loc = V3.fromString(tag.getString("site"));
                LogisticsPoint point = new LogisticsPoint(loc);
                point.label = tag.getString("label");
                NBTTagList items = tag.getTagList("items", 8);
                for (int j = 0; j < items.tagCount(); j++) {
                    point.items.add(items.getStringTagAt(j));
                }
                if (!point.items.isEmpty()) {
                    this.logistics.put(loc.toString(), point);
                }
            } catch (Exception ignored) {
                // 单个坏物流点不影响其余记录
            }
        }
        // 物流中心设置
        this.logisticsSettings.clear();
        NBTTagList settingsList = nbt.getTagList("logisticsSettings", 10);
        for (int i = 0; i < settingsList.tagCount(); i++) {
            try {
                LogisticsSettings settings = LogisticsSettings.readFromNBT(settingsList.getCompoundTagAt(i));
                if (settings.center != null) {
                    this.logisticsSettings.put(centerKey(settings.center), settings);
                }
            } catch (Exception ignored) {
                // 单个坏设置不影响其余记录
            }
        }
        // 供货订单
        this.supplyOrders.clear();
        NBTTagList orderList = nbt.getTagList("supplyOrders", 10);
        for (int i = 0; i < orderList.tagCount(); i++) {
            try {
                SupplyOrder order = SupplyOrder.readFromNBT(orderList.getCompoundTagAt(i));
                if (order.id != null && !order.id.isEmpty()) {
                    // 读档后任务对象不会恢复，所有“已领取/配送中”的订单都回到待领取，
                    // 避免快递员读档后手里没有订单、订单却永久占线。
                    if (SupplyOrder.ASSIGNED.equals(order.status)
                            || SupplyOrder.PICKING.equals(order.status)
                            || SupplyOrder.DELIVERING.equals(order.status)) {
                        order.status = SupplyOrder.PENDING;
                        order.courier = null;
                        order.updatedAt = System.currentTimeMillis();
                    }
                    this.supplyOrders.put(order.id, order);
                }
            } catch (Exception ignored) {
                // 单个坏订单不影响其余记录
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setFloat("money", money);
        nbt.setInteger("dayOfWeek", dayOfWeek);
        nbt.setInteger("gameDay", gameDay);
        nbt.setInteger("gamemode", gamemode);
        NBTTagList list = new NBTTagList();
        for (Map.Entry<UUID, NBTTagCompound> e : npcs.entrySet()) {
            NBTTagCompound tag = e.getValue();
            if (tag == null) {
                continue;
            }
            tag.setString("uuid", e.getKey().toString());
            list.appendTag(tag);
        }
        nbt.setTag("npcs", list);
        // 物流收货点
        NBTTagList logisticsList = new NBTTagList();
        for (LogisticsPoint point : this.logistics.values()) {
            if (point == null || point.loc == null || point.items.isEmpty()) {
                continue;
            }
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("site", point.loc.toString());
            tag.setString("label", point.label == null ? "" : point.label);
            NBTTagList items = new NBTTagList();
            for (String item : point.items) {
                items.appendTag(new NBTTagString(item));
            }
            tag.setTag("items", items);
            logisticsList.appendTag(tag);
        }
        nbt.setTag("logistics", logisticsList);
        // 物流中心设置
        NBTTagList settingsList = new NBTTagList();
        for (LogisticsSettings settings : this.logisticsSettings.values()) {
            if (settings == null || settings.center == null) {
                continue;
            }
            settingsList.appendTag(settings.writeToNBT());
        }
        nbt.setTag("logisticsSettings", settingsList);
        // 供货订单
        NBTTagList orderList = new NBTTagList();
        for (SupplyOrder order : this.supplyOrders.values()) {
            if (order != null) {
                orderList.appendTag(order.writeToNBT());
            }
        }
        nbt.setTag("supplyOrders", orderList);
        return nbt;
    }
}
