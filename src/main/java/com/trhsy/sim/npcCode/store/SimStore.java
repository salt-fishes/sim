package com.trhsy.sim.npcCode.store;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

import java.util.LinkedHashMap;
import java.util.Map;
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
        return nbt;
    }
}
