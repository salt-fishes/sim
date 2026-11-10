package com.trhsy.sim.loader;

import com.trhsy.sim.ModSim;
import com.trhsy.sim.npcCode.V3;
import com.trhsy.sim.npcCode.build.Building;
import com.trhsy.sim.npcCode.block.FarmBox;
import com.trhsy.sim.npcCode.block.MineBox;
import com.trhsy.sim.npcCode.store.SimStore;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 建筑地块强制加载。
 *
 * <p>建筑、农场、矿场所在区块在服务端运行期间保持加载，避免 NPC 走到目的地后
 * 因为区块卸载而停住；也让玩家离开城镇时商店/物流中心继续工作。</p>
 *
 * <p>只加载“有实际内容”的区块：建筑结构覆盖的区块 + 1 区块外扩；
 * 农场/矿场只加载工作点周围 1 区块。不是整张地图常加载。</p>
 */
public final class SimChunkLoader {
    /** 维度 -> 已申请的票据。 */
    private static final Map<Integer, List<ForgeChunkManager.Ticket>> TICKETS = new HashMap<>();
    /** NPC 移动时临时保持其周围区块加载。 */
    private static final Map<UUID, NpcTicket> NPC_TICKETS = new HashMap<>();
    /** 默认单票据最大区块数（Forge 配置读不到时使用）。 */
    private static final int FALLBACK_CHUNKS_PER_TICKET = 25;

    private static final class NpcTicket {
        final ForgeChunkManager.Ticket ticket;
        final int chunkX;
        final int chunkZ;

        NpcTicket(ForgeChunkManager.Ticket ticket, int chunkX, int chunkZ) {
            this.ticket = ticket;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }
    }

    private SimChunkLoader() {
    }

    /** 在 Mod 初始化阶段注册 Forge 区块票据回调，否则 requestTicket 会抛异常。 */
    public static void register(Object mod) {
        ForgeChunkManager.setForcedChunkLoadingCallback(mod,
                new ForgeChunkManager.LoadingCallback() {
                    @Override
                    public void ticketsLoaded(List<ForgeChunkManager.Ticket> tickets, World world) {
                        // 旧票据由 refresh() 按当前建筑列表重建，先全部释放，避免重复加载。
                        for (ForgeChunkManager.Ticket ticket : tickets) {
                            try {
                                ForgeChunkManager.releaseTicket(ticket);
                            } catch (Exception ignored) {
                            }
                        }
                    }
                });
    }

    /**
     * NPC 移动时保持其当前 3x3 区块加载；跨区块时释放旧票、申请新票。
     * 这解决“建筑地块加载了，但两个建筑之间的路没有加载，NPC 走到一半停住”的问题。
     */
    public static synchronized void keepNpcLoaded(World world, UUID npcId, V3 pos) {
        if (world == null || world.isRemote || npcId == null || pos == null) {
            return;
        }
        int cx = ((int) Math.floor(pos.x)) >> 4;
        int cz = ((int) Math.floor(pos.z)) >> 4;
        NpcTicket old = NPC_TICKETS.get(npcId);
        if (old != null && old.chunkX == cx && old.chunkZ == cz) {
            return;
        }
        ForgeChunkManager.Ticket ticket;
        if (old != null) {
            // 复用票据：先把旧 3x3 区块取消强制加载，再切到新 3x3。
            ticket = old.ticket;
            for (int ox = -1; ox <= 1; ox++) {
                for (int oz = -1; oz <= 1; oz++) {
                    try {
                        ForgeChunkManager.unforceChunk(ticket,
                                new ChunkPos(old.chunkX + ox, old.chunkZ + oz));
                    } catch (Exception ignored) {
                    }
                }
            }
        } else {
            try {
                ticket = ForgeChunkManager.requestTicket(ModSim.instance, world,
                        ForgeChunkManager.Type.NORMAL);
            } catch (Exception e) {
                return;
            }
        }
        if (ticket == null) {
            return;
        }
        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                try {
                    ForgeChunkManager.forceChunk(ticket, new ChunkPos(cx + ox, cz + oz));
                } catch (Exception ignored) {
                }
            }
        }
        NPC_TICKETS.put(npcId, new NpcTicket(ticket, cx, cz));
    }

    /** 释放某个 NPC 的移动区块票据。 */
    public static synchronized void releaseNpc(UUID npcId) {
        if (npcId == null) {
            return;
        }
        NpcTicket old = NPC_TICKETS.remove(npcId);
        if (old != null) {
            try {
                ForgeChunkManager.releaseTicket(old.ticket);
            } catch (Exception ignored) {
            }
        }
    }

    /** 释放所有 NPC 移动票据（世界卸载/停服）。 */
    public static synchronized void releaseAllNpcTickets() {
        for (UUID id : new ArrayList<>(NPC_TICKETS.keySet())) {
            releaseNpc(id);
        }
    }

    /** 重新计算并强制加载当前世界的建筑/农场/矿场区块。 */
    public static synchronized void refresh(World world) {
        if (world == null || world.isRemote || world.provider == null) {
            return;
        }
        int dimension = world.provider.getDimension();
        release(dimension);

        Set<ChunkPos> chunks = collectChunks(world, dimension);
        if (chunks.isEmpty()) {
            return;
        }

        int maxChunks = ForgeChunkManager.getMaxChunkDepthFor(ModSim.MODID);
        if (maxChunks <= 0) {
            maxChunks = FALLBACK_CHUNKS_PER_TICKET;
        }

        List<ForgeChunkManager.Ticket> tickets = new ArrayList<>();
        ForgeChunkManager.Ticket ticket = null;
        int used = 0;
        for (ChunkPos pos : chunks) {
            if (ticket == null || used >= maxChunks) {
                try {
                    ticket = ForgeChunkManager.requestTicket(ModSim.instance, world,
                            ForgeChunkManager.Type.NORMAL);
                } catch (Exception e) {
                    ModSimLoader.log.warn("建筑地块强制加载：申请票据失败 {}", e.getMessage());
                    ticket = null;
                }
                if (ticket == null) {
                    ModSimLoader.log.warn("建筑地块强制加载失败：Forge 票据已用完，已加载 {} 个区块",
                            tickets.size() * maxChunks);
                    break;
                }
                tickets.add(ticket);
                used = 0;
            }
            try {
                ForgeChunkManager.forceChunk(ticket, pos);
                ++used;
            } catch (Exception e) {
                ModSimLoader.log.warn("建筑地块强制加载区块 {} 失败：{}", pos, e.getMessage());
            }
        }
        TICKETS.put(dimension, tickets);
        ModSimLoader.log.info("已强制加载 {} 个建筑/农场/矿场区块（维度 {}，{} 张票据）",
                chunks.size(), dimension, tickets.size());
    }

    /** 释放某个维度的所有强制加载票据。 */
    public static synchronized void release(int dimension) {
        List<ForgeChunkManager.Ticket> old = TICKETS.remove(dimension);
        if (old == null) {
            return;
        }
        for (ForgeChunkManager.Ticket ticket : old) {
            try {
                ForgeChunkManager.releaseTicket(ticket);
            } catch (Exception ignored) {
            }
        }
    }

    /** 世界卸载时释放。 */
    public static synchronized void release(World world) {
        if (world != null && !world.isRemote && world.provider != null) {
            release(world.provider.getDimension());
            releaseAllNpcTickets();
        }
    }

    private static Set<ChunkPos> collectChunks(World world, int dimension) {
        Set<ChunkPos> chunks = new LinkedHashSet<>();

        // 建筑：结构范围外扩 1 区块
        for (Building building : ModSimLoader.buildings) {
            if (building == null || building.dimension != dimension) {
                continue;
            }
            int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
            if (building.structure != null && !building.structure.isEmpty()) {
                for (V3 block : building.structure) {
                    if (block == null) {
                        continue;
                    }
                    minX = Math.min(minX, (int) Math.floor(block.x));
                    minZ = Math.min(minZ, (int) Math.floor(block.z));
                    maxX = Math.max(maxX, (int) Math.floor(block.x));
                    maxZ = Math.max(maxZ, (int) Math.floor(block.z));
                }
            }
            if (minX == Integer.MAX_VALUE) {
                if (building.controlXYZ == null) {
                    continue;
                }
                minX = maxX = (int) Math.floor(building.controlXYZ.x);
                minZ = maxZ = (int) Math.floor(building.controlXYZ.z);
            }
            addRect(chunks, minX, minZ, maxX, maxZ, 1);
        }

        // 农场/矿场：工作点周围 1 区块
        for (FarmBox farm : ModSimLoader.farms) {
            if (farm != null && farm.loc != null && farm.loc.dimension == dimension) {
                addRadius(chunks, farm.loc, 1);
            }
        }
        for (MineBox mine : ModSimLoader.mines) {
            if (mine != null && mine.loc != null && mine.loc.dimension == dimension) {
                addRadius(chunks, mine.loc, 1);
            }
        }
        // 物流取货/产出点也可能在建筑外，确保快递员到达时区块是加载的。
        SimStore store = SimStore.get(world);
        if (store != null) {
            for (SimStore.LogisticsPoint point : store.getLogisticsPoints().values()) {
                if (point != null && point.loc != null && point.loc.dimension == dimension) {
                    addRadius(chunks, point.loc, 1);
                }
            }
        }
        return chunks;
    }

    private static void addRect(Set<ChunkPos> chunks, int minX, int minZ, int maxX, int maxZ, int margin) {
        int minCX = (Math.min(minX, maxX) >> 4) - margin;
        int maxCX = (Math.max(minX, maxX) >> 4) + margin;
        int minCZ = (Math.min(minZ, maxZ) >> 4) - margin;
        int maxCZ = (Math.max(minZ, maxZ) >> 4) + margin;
        for (int cx = minCX; cx <= maxCX; cx++) {
            for (int cz = minCZ; cz <= maxCZ; cz++) {
                chunks.add(new ChunkPos(cx, cz));
            }
        }
    }

    private static void addRadius(Set<ChunkPos> chunks, V3 pos, int margin) {
        int cx = (int) Math.floor(pos.x) >> 4;
        int cz = (int) Math.floor(pos.z) >> 4;
        for (int ox = -margin; ox <= margin; ox++) {
            for (int oz = -margin; oz <= margin; oz++) {
                chunks.add(new ChunkPos(cx + ox, cz + oz));
            }
        }
    }
}
