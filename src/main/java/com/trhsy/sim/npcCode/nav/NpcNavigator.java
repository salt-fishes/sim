package com.trhsy.sim.npcCode.nav;

import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.loader.SimChunkLoader;
import com.trhsy.sim.npcCode.NpcData;
import com.trhsy.sim.npcCode.V3;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockWall;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.List;

/**
 * NPC 的统一移动控制器。
 *
 * <p>这个类只做一件事：把“去某个坐标”变成一次可恢复的步行计划。
 * 它**不会在移动过程中传送 NPC**。真实地形上的移动分成两种：</p>
 *
 * <ul>
 *   <li>距离 ≤ 40 格：直接寻路到目标旁边的可站立点；</li>
 *   <li>距离 &gt; 40 格：每次朝目标走 24 格左右的 hop，到了再计算下一段。</li>
 * </ul>
 *
 * <p>寻路失败时先换目标点周围的备用落点，仍然失败就标记为“不可达”，
 * 由任务决定跳过/取消/稍后重试；不会出现每 5 秒传送一次的循环。</p>
 */
public final class NpcNavigator {
    /** 超过这个距离就拆成多个 hop，避免一次 A* 搜索整张地图。 */
    private static final double DIRECT_RANGE = 40.0D;
    /** 每个 hop 的直线距离。 */
    private static final double HOP_DISTANCE = 24.0D;
    /** 到达中间 hop 的水平/垂直容差。 */
    private static final double WAYPOINT_XZ = 2.5D;
    private static final double WAYPOINT_Y = 3.0D;
    /** 到达最终目标的水平/垂直容差。 */
    private static final double ARRIVE_XZ = 1.8D;
    private static final double ARRIVE_Y = 2.5D;
    /** 两次重新寻路之间的最小间隔。 */
    private static final long PATH_RETRY_MS = 1200L;
    /** 连续多少秒几乎没有位移算一次“卡住”。 */
    private static final long PROGRESS_WINDOW_MS = 1200L;
    private static final int STUCK_TRIGGER = 3;
    /** 同一个目标累计卡住/寻路失败多少次后判定不可达。 */
    private static final int MAX_FAILURES = 10;
    /** 不可达后多久允许自动重试一次。 */
    private static final long UNREACHABLE_RETRY_MS = 60000L;
    /** 一次规划最多尝试多少个候选落点。 */
    private static final int MAX_PATH_CANDIDATES = 10;
    /** 超过这个距离开始加速；进入这个距离恢复原速。 */
    private static final double FAR_DISTANCE = 32.0D;
    /** 远距离步行速度倍率（1.0 是原速）。 */
    private static final double FAR_SPEED = 2.5D;
    /** 目标就在附近但脚下没有完整路径时，允许“在门口工作/交互”。 */
    private static final double CLOSE_ARRIVAL_DISTANCE = 6.0D;
    private static final double CLOSE_ARRIVAL_Y = 4.0D;
    /** 超过这个距离且寻路失败时，允许一次末影人式闪现。 */
    private static final double FAR_BLINK_DISTANCE = 64.0D;
    /** 闪现冷却。 */
    private static final long FAR_BLINK_COOLDOWN_MS = 15000L;

    private final NpcData npc;

    private V3 requested;
    private V3 anchor;
    private V3 waypoint;

    private boolean arrived;
    private long arrivedAt;
    private boolean unreachable;
    private long unreachableAt;
    private boolean waitingForChunk;

    private long lastPlanAt;
    private long lastProgressCheck;
    private double lastX;
    private double lastZ;
    private double lastAnchorDistance = Double.MAX_VALUE;
    private int idleChecks;
    private int failures;
    private int stuckAttempts;
    /** 已经对哪个目标闪现过：同一目标最多一次，避免反复传送。 */
    private V3 blinkedTarget;
    private long lastBlinkAt;
    /** 防止 NPC 被挤进栅栏/方块后永久卡住。 */
    private long lastUnstuckAt;
    private V3 lastUnstuckTarget;

    public NpcNavigator(NpcData npc) {
        this.npc = npc;
    }

    // ==================================================================
    // 对外 API
    // ==================================================================

    /**
     * 设置/更新移动目标。重复调用同一个目标是无副作用的。
     *
     * @param target 原始目标（可以是箱子、建筑控制块、树木等方块坐标）
     * @param soft   true 表示不强制清除 stayPut（TaskGoTo/TaskSleep 使用）
     * @return false 仅表示维度非法或当前已经确定不可达
     */
    public boolean moveTo(V3 target, boolean soft) {
        if (this.npc == null || this.npc.entity == null || target == null) {
            return false;
        }
        if (target.dimension != this.npc.entity.dimension) {
            return false;
        }
        if (!soft) {
            this.npc.stayPut = false;
        }

        long now = System.currentTimeMillis();
        if (sameTarget(this.requested, target)) {
            if (this.unreachable && now - this.unreachableAt >= UNREACHABLE_RETRY_MS) {
                reset(target);
            } else {
                return !this.unreachable;
            }
        } else if (this.requested != null && sameDimension(this.requested, target)
                && distanceXZ(this.requested, target) <= 2.5D) {
            // 目标只是小幅移动（动物、掉落物等）：不要每 tick 清路径重算。
            this.requested = target;
            if (this.anchor != null && distanceXZ(this.anchor, target) > 3.0D) {
                this.anchor = null;
            }
            if (this.arrived) {
                this.arrived = false;
            }
            return !this.unreachable;
        } else {
            reset(target);
        }

        if (within(target, ARRIVE_XZ, ARRIVE_Y)) {
            finishArrival(now);
            return true;
        }

        if (this.anchor == null) {
            this.anchor = resolveAnchor(target);
            if (this.anchor == null) {
                if (!isColumnLoaded(target)) {
                    this.waitingForChunk = true;
                    return true;
                }
                markUnreachable("找不到可站立落点");
                return false;
            }
        }

        // 远距离（>64 格）且目标区块已加载：直接做一次末影人闪现。
        // 同一目标只闪一次；近距离仍然步行，32~64 格用 2.5 倍速。
        if (horizontalDistance(target) > FAR_BLINK_DISTANCE && tryFarBlink(now)) {
            return true;
        }

        // 有锚点后立即尝试规划；失败也会在 tick 里按节流重试。
        plan(now);
        return !this.unreachable;
    }

    /** 一次性寻路（士兵巡逻/追击），不接管后续移动，也不会传送。 */
    public boolean walkOneShot(V3 target) {
        if (this.npc == null || this.npc.entity == null || target == null
                || target.dimension != this.npc.entity.dimension) {
            return false;
        }
        V3 stand = getAdjustedStandPoint(target);
        if (stand == null) {
            stand = resolveAnchor(target);
        }
        if (stand == null) {
            return false;
        }
        Path path = pathTo(stand);
        if (path == null) {
            return false;
        }
        this.npc.entity.getNavigator().setPath(path, 1.0D);
        return true;
    }

    /** 每个服务器 tick 调用（只在实体所在区块已加载时）。 */
    public void tick() {
        if (this.requested == null || this.npc == null || this.npc.entity == null
                || this.npc.entity.isDead || !this.npc.isActive()) {
            return;
        }

        long now = System.currentTimeMillis();

        // 移动 NPC 周围 3x3 区块保持加载，避免走到两个建筑之间时区块卸载而停住。
        SimChunkLoader.keepNpcLoaded(this.npc.entity.world, this.npc.ID,
                V3.fromVec3d(this.npc.entity.getPositionVector()));

        // 被其他实体/栅栏挤进方块时，先做一次小范围脱困；这不是常规移动传送。
        if (tryUnstuck(now)) {
            return;
        }

        // 睡觉/站定类任务不允许移动；只有 forceMoveToXYZ 会清掉 stayPut。
        if (this.npc.stayPut && !this.arrived) {
            clearPath();
            return;
        }

        if (this.arrived) {
            // 被推离/被其他 AI 带走后，重新接管。
            if (now - this.arrivedAt > 3000L && !within(this.anchor, 4.0D, 4.0D)) {
                this.arrived = false;
                plan(now);
            }
            return;
        }

        if (this.unreachable) {
            if (now - this.unreachableAt >= UNREACHABLE_RETRY_MS) {
                this.unreachable = false;
                this.failures = 0;
                this.stuckAttempts = 0;
                this.anchor = resolveAnchor(this.requested);
                plan(now);
            }
            return;
        }

        if (this.anchor == null) {
            this.anchor = resolveAnchor(this.requested);
            if (this.anchor == null) {
                if (!isColumnLoaded(this.requested)) {
                    this.waitingForChunk = true;
                } else {
                    markUnreachable("找不到可站立落点");
                }
                return;
            }
        }
        this.waitingForChunk = false;

        if (within(this.anchor, ARRIVE_XZ, ARRIVE_Y)) {
            finishArrival(now);
            return;
        }

        PathNavigate navigator = this.npc.entity.getNavigator();
        if (navigator == null) {
            return;
        }
        // 远距离加速、近距离恢复原速；每 tick 更新，进入最后 32 格会立刻降速。
        navigator.setSpeed(desiredSpeed());

        // 没有路/已经走完当前路：规划下一段。
        if (this.waypoint == null || navigator.noPath() || navigator.getPath() == null) {
            if (now - this.lastPlanAt >= PATH_RETRY_MS) {
                plan(now);
            }
            return;
        }

        // 到达当前 hop，继续下一段。
        if (within(this.waypoint, WAYPOINT_XZ, WAYPOINT_Y)) {
            this.waypoint = null;
            this.lastPlanAt = 0L;
            plan(now);
            return;
        }

        if (now - this.lastProgressCheck < PROGRESS_WINDOW_MS) {
            return;
        }

        double moved = Math.abs(this.npc.entity.posX - this.lastX)
                + Math.abs(this.npc.entity.posZ - this.lastZ);
        double dist = horizontalDistance(this.anchor);
        if (dist < this.lastAnchorDistance - 0.75D) {
            // 确实在靠近目标：清除所有失败计数。
            this.lastAnchorDistance = dist;
            this.stuckAttempts = 0;
            this.failures = 0;
            this.idleChecks = 0;
        } else if (moved < 0.30D) {
            ++this.idleChecks;
            if (this.idleChecks >= STUCK_TRIGGER) {
                this.idleChecks = 0;
                ++this.stuckAttempts;
                clearPath();
                this.waypoint = null;
                this.lastPlanAt = 0L;
                if (this.stuckAttempts >= MAX_FAILURES) {
                    markUnreachable("连续卡住");
                } else {
                    plan(now);
                }
            }
        } else {
            this.idleChecks = 0;
        }
        this.lastX = this.npc.entity.posX;
        this.lastZ = this.npc.entity.posZ;
        this.lastProgressCheck = now;
    }

    public boolean hasArrivedAt(V3 target) {
        return this.arrived && sameTarget(this.requested, target);
    }

    public boolean isUnreachable(V3 target) {
        if (this.requested == null) {
            return false;
        }
        if (target != null && !sameTarget(this.requested, target)) {
            return false;
        }
        return this.unreachable;
    }

    public boolean isNavigating() {
        return this.requested != null && !this.arrived && !this.unreachable;
    }

    public V3 getRequested() {
        return this.requested;
    }

    /** 只清路径，保留“已到达”状态，供 Job.onArrive 使用。 */
    public void stopPath() {
        clearPath();
        this.waypoint = null;
    }

    /** 完全取消移动意图。 */
    public void clear() {
        clearPath();
        if (this.npc != null) {
            SimChunkLoader.releaseNpc(this.npc.ID);
        }
        this.requested = null;
        this.anchor = null;
        this.waypoint = null;
        this.arrived = false;
        this.unreachable = false;
        this.waitingForChunk = false;
        this.failures = 0;
        this.stuckAttempts = 0;
        this.idleChecks = 0;
        this.lastAnchorDistance = Double.MAX_VALUE;
        this.blinkedTarget = null;
        this.lastBlinkAt = 0L;
    }

    // ==================================================================
    // 规划
    // ==================================================================

    private void reset(V3 target) {
        clearPath();
        this.requested = target;
        this.anchor = null;
        this.waypoint = null;
        this.arrived = false;
        this.unreachable = false;
        this.waitingForChunk = false;
        this.failures = 0;
        this.stuckAttempts = 0;
        this.idleChecks = 0;
        this.lastAnchorDistance = Double.MAX_VALUE;
        this.lastX = this.npc.entity.posX;
        this.lastZ = this.npc.entity.posZ;
        this.lastProgressCheck = System.currentTimeMillis();
        this.lastPlanAt = 0L;
        this.blinkedTarget = null;
        this.lastBlinkAt = 0L;
    }

    private void plan(long now) {
        if (this.anchor == null || this.npc == null || this.npc.entity == null) {
            return;
        }
        if (now - this.lastPlanAt < PATH_RETRY_MS) {
            return;
        }
        this.lastPlanAt = now;

        double dist = horizontalDistance(this.anchor);
        List<V3> primary = new ArrayList<>();
        if (dist <= DIRECT_RANGE) {
            primary.add(this.anchor);
        } else {
            addHopPoints(primary, this.anchor, dist);
            if (primary.isEmpty()) {
                primary.add(this.anchor);
            }
        }

        int[] tried = {0};
        if (tryCandidates(primary, tried, now)) {
            return;
        }
        // 精确落点/主 hop 都不通，再尝试目标周围的备用落点。
        if (dist <= DIRECT_RANGE) {
            List<V3> approach = new ArrayList<>();
            addApproachPoints(approach, this.anchor);
            tried[0] = 0;
            if (tryCandidates(approach, tried, now)) {
                return;
            }
        }
        // 最后一层兜底：先走出当前的小坑/死角，再下一轮重新规划。
        if (this.failures > 0 || this.stuckAttempts > 0) {
            List<V3> escape = new ArrayList<>();
            addNearEscapePoints(escape);
            tried[0] = 0;
            if (tryCandidates(escape, tried, now)) {
                return;
            }
        }
        // 路真的走不通，但目标很远：允许一次末影人式闪现直接到目标附近。
        // 同一目标只闪一次，之后无论如何都只能步行或等待，不会反复传送。
        if (tryFarBlink(now)) {
            return;
        }
        failPlan();
    }

    /** 依次尝试候选落点，成功设置路径后返回 true。 */
    private boolean tryCandidates(List<V3> candidates, int[] tried, long now) {
        for (V3 candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            if (++tried[0] > MAX_PATH_CANDIDATES) {
                return false;
            }
            Path path = pathTo(candidate);
            if (path != null) {
                this.waypoint = candidate;
                this.npc.entity.getNavigator().setPath(path, desiredSpeed());
                this.lastX = this.npc.entity.posX;
                this.lastZ = this.npc.entity.posZ;
                this.lastProgressCheck = now;
                this.idleChecks = 0;
                return true;
            }
        }
        return false;
    }

    /**
     * 实体被挤进栅栏/方块时（碰撞箱与方块重叠），找最近安全落点做一次小范围脱困。
     * 只有真正嵌入方块才触发，5 秒冷却；不属于常规寻路传送。
     */
    private boolean tryUnstuck(long now) {
        if (this.npc == null || this.npc.entity == null) {
            return false;
        }
        if (now - this.lastUnstuckAt < 5000L) {
            return false;
        }
        AxisAlignedBB box = this.npc.entity.getEntityBoundingBox().grow(-0.15D);
        BlockPos feetPos = this.npc.entity.getPosition();
        AxisAlignedBB feetBox = this.npc.entity.world.getBlockState(feetPos)
                .getCollisionBoundingBox(this.npc.entity.world, feetPos);
        // 只算“脚部真正嵌进方块”：站在台阶/半砖顶部（posY == 碰撞箱顶）不算。
        boolean blockCollision = feetBox != null
                && this.npc.entity.posY < feetBox.maxY - 0.05D;
        boolean collides = blockCollision
                || !this.npc.entity.world.getCollisionBoxes(this.npc.entity, box).isEmpty();
        if (!collides) {
            return false;
        }
        this.lastUnstuckAt = now;
        double baseY = this.npc.entity.posY;
        V3 best = null;
        double bestDist = Double.MAX_VALUE;
        for (int r = 2; r <= 5 && best == null; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    V3 stand = standableColumn(this.npc.entity.posX + dx,
                            this.npc.entity.posZ + dz, baseY);
                    if (stand == null || Math.abs(stand.y - baseY) > 3.0D) {
                        continue;
                    }
                    double dist = distanceXZ(stand, V3.fromVec3d(this.npc.entity.getPositionVector()));
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = stand;
                    }
                }
            }
        }
        if (best == null) {
            return false;
        }
        if (this.lastUnstuckTarget != null && sameTarget(this.lastUnstuckTarget, best)
                && now - this.lastUnstuckAt < 60000L) {
            return false;
        }
        this.lastUnstuckTarget = best;
        // 脱困目标是经过 isSafeStand 检查的，直接精确落位，避免 attemptTeleport
        // 因为邻近栅栏/实体拒绝而原地打转。
        this.npc.entity.setPositionAndUpdate(best.x, best.y, best.z);
        this.npc.pos = V3.fromVec3d(this.npc.entity.getPositionVector());
        this.npc.pos.dimension = this.npc.entity.dimension;
        spawnBlinkParticles();
        clearPath();
        this.waypoint = null;
        this.arrived = false;
        this.unreachable = false;
        this.failures = 0;
        this.stuckAttempts = 0;
        this.lastPlanAt = 0L;
        ModSimLoader.log.warn("NPC {} 卡在方块内，已移出到 {}", this.npc.getName(), best);
        return true;
    }

    /**
     * 远距离提速：距离目标越远，寻路速度倍率越高；进入 32 格恢复原速。
     * 速度倍率作用在 PathNavigate 上，不修改实体基础移动属性。
     */
    private double desiredSpeed() {
        double dist = horizontalDistance(this.requested);
        if (dist > FAR_DISTANCE) {
            return FAR_SPEED;
        }
        return 1.0D;
    }

    /**
     * 末影人式远距离闪现：只在“距离 >64 格且多次寻路失败”时触发，
     * 同一目标最多一次，避免回到以前反复传送的老路。
     */
    private boolean tryFarBlink(long now) {
        if (this.requested == null || this.npc == null || this.npc.entity == null) {
            return false;
        }
        if (this.blinkedTarget != null && sameTarget(this.blinkedTarget, this.requested)) {
            return false;
        }
        if (now - this.lastBlinkAt < FAR_BLINK_COOLDOWN_MS) {
            return false;
        }
        if (horizontalDistance(this.requested) < FAR_BLINK_DISTANCE) {
            return false;
        }
        if (!(this.npc.entity instanceof EntityLivingBase)) {
            return false;
        }
        V3 stand = resolveAnchor(this.requested);
        if (stand == null) {
            stand = getAdjustedStandPoint(this.requested);
        }
        if (stand == null || !this.npc.entity.world.isBlockLoaded(stand.toBlockPos())) {
            return false;
        }
        EntityLivingBase living = (EntityLivingBase) this.npc.entity;
        boolean ok = living.attemptTeleport(stand.x, stand.y, stand.z);
        if (!ok) {
            return false;
        }
        this.lastBlinkAt = now;
        this.blinkedTarget = this.requested;
        this.npc.pos = V3.fromVec3d(this.npc.entity.getPositionVector());
        this.npc.pos.dimension = this.npc.entity.dimension;
        spawnBlinkParticles();
        clearPath();
        this.waypoint = null;
        this.arrived = false;
        this.unreachable = false;
        this.failures = 0;
        this.stuckAttempts = 0;
        this.lastPlanAt = 0L;
        this.lastAnchorDistance = Double.MAX_VALUE;
        if (within(this.requested, ARRIVE_XZ, ARRIVE_Y)) {
            finishArrival(now);
        } else {
            this.anchor = resolveAnchor(this.requested);
        }
        return true;
    }

    private void spawnBlinkParticles() {
        try {
            World world = this.npc.entity.world;
            if (!(world instanceof WorldServer)) {
                return;
            }
            WorldServer server = (WorldServer) world;
            server.spawnParticle(EnumParticleTypes.PORTAL, false,
                    this.npc.entity.posX, this.npc.entity.posY + 0.8D, this.npc.entity.posZ,
                    32, 0.5D, 0.5D, 0.5D, 0.05D);
            world.playSound(null, this.npc.entity.posX, this.npc.entity.posY, this.npc.entity.posZ,
                    net.minecraft.init.SoundEvents.ENTITY_ENDERMEN_TELEPORT,
                    net.minecraft.util.SoundCategory.PLAYERS, 1.0F, 1.0F);
        } catch (Exception ignored) {
        }
    }

    private void failPlan() {
        clearPath();
        this.waypoint = null;
        ++this.failures;
        if (this.failures >= MAX_FAILURES) {
            markUnreachable("连续寻路失败");
        }
    }

    private void markUnreachable(String reason) {
        if (this.unreachable) {
            return;
        }
        // 已经走到目标附近（例如在门口、墙外 5 格），只是精确锚点被墙/玻璃挡住：
        // 视为“就地工作/交互”，不要把 NPC 永远卡在不可达状态。
        // 住宅目标例外：睡觉需要真正到家，不能用“门口 5 格”代替。
        boolean homeTarget = this.npc.home != null && this.npc.home.livingXYZ != null
                && sameTarget(this.requested, this.npc.home.livingXYZ);
        if (!homeTarget && within(this.requested, CLOSE_ARRIVAL_DISTANCE, CLOSE_ARRIVAL_Y)) {
            this.anchor = new V3(this.npc.entity.posX, this.npc.entity.posY,
                    this.npc.entity.posZ, this.npc.entity.dimension);
            finishArrival(System.currentTimeMillis());
            return;
        }
        this.unreachable = true;
        this.unreachableAt = System.currentTimeMillis();
        clearPath();
        this.waypoint = null;
        if (this.npc != null && this.npc.entity != null) {
            ModSimLoader.log.warn("NPC {} 无法步行到达 {}（{}），已停止移动；稍后重试。当前位置 {}，落点 {}，距离 {}，stayPut={}，active={}",
                    this.npc.getName(), this.requested, reason,
                    this.npc.entity.getPosition(), this.anchor,
                    String.format("%.1f", horizontalDistance(this.anchor)),
                    this.npc.stayPut, this.npc.isActive());
        }
    }

    private void finishArrival(long now) {
        clearPath();
        if (this.anchor == null) {
            this.anchor = this.requested;
        }
        this.waypoint = null;
        this.arrived = true;
        this.arrivedAt = now;
        this.unreachable = false;
        this.failures = 0;
        this.stuckAttempts = 0;
        this.idleChecks = 0;
    }

    /** 当前点周围 4/8/12 格的可站立点，用于从坑里/门口/死角先挪出来。 */
    private void addNearEscapePoints(List<V3> list) {
        if (this.npc == null || this.npc.entity == null) {
            return;
        }
        double baseY = this.npc.entity.posY;
        for (double radius : new double[]{4.0D, 8.0D, 12.0D}) {
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4.0D;
                double x = this.npc.entity.posX + Math.cos(angle) * radius;
                double z = this.npc.entity.posZ + Math.sin(angle) * radius;
                addUnique(list, standableColumn(x, z, baseY));
            }
        }
    }

    /**
     * 目标周围的备用落点。用于目标被箱子/栅栏/墙挡住，或精确点无法站立的情况。
     */
    private void addApproachPoints(List<V3> list, V3 target) {
        double[] radii = {1.5D, 2.5D, 3.5D, 5.0D};
        for (double r : radii) {
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4.0D;
                double x = target.x + Math.cos(angle) * r;
                double z = target.z + Math.sin(angle) * r;
                V3 stand = standableColumn(x, z, target.y);
                addUnique(list, stand);
            }
        }
    }

    /**
     * 长距离 hop：朝目标走 24 格，左右偏移几个角度找可站立的地面。
     * 如果直线方向被山/河挡住，局部寻路仍会在这一段内绕行。
     */
    private void addHopPoints(List<V3> list, V3 target, double dist) {
        double dx = target.x - this.npc.entity.posX;
        double dz = target.z - this.npc.entity.posZ;
        double baseAngle = Math.atan2(dz, dx);
        double baseY = this.npc.entity.posY + (target.y - this.npc.entity.posY) * (HOP_DISTANCE / dist);
        double[] angles = {0.0D, 0.35D, -0.35D, 0.70D, -0.70D, 1.05D, -1.05D};
        double[] scales = {1.0D, 0.65D, 0.35D};
        for (double scale : scales) {
            double step = Math.min(HOP_DISTANCE * scale, Math.max(4.0D, dist - 2.0D));
            for (double offset : angles) {
                double angle = baseAngle + offset;
                double x = this.npc.entity.posX + Math.cos(angle) * step;
                double z = this.npc.entity.posZ + Math.sin(angle) * step;
                V3 stand = standableColumn(x, z, baseY);
                addUnique(list, stand);
            }
        }
    }

    private void addUnique(List<V3> list, V3 value) {
        if (value == null) {
            return;
        }
        for (V3 existing : list) {
            if (existing != null && sameTarget(existing, value)) {
                return;
            }
        }
        list.add(value);
    }

    // ==================================================================
    // 落点解析
    // ==================================================================

    /**
     * 把“方块坐标”解析成真正能站的脚部坐标。
     * 例如箱子/控制块本身是实体方块，NPC 应该站在它旁边，而不是站到箱子顶上。
     */
    public V3 resolveAnchor(V3 target) {
        if (target == null || this.npc == null || this.npc.entity == null) {
            return null;
        }
        World world = this.npc.entity.world;
        int tx = floor(target.x);
        int ty = floor(target.y);
        int tz = floor(target.z);
        if (!world.isBlockLoaded(new BlockPos(tx, 0, tz))) {
            return null;
        }

        boolean targetSolid = hasCollision(world, tx, ty, tz);
        V3 best = null;
        double bestScore = Double.MAX_VALUE;
        int minDy = targetSolid ? 0 : -2;

        // 先在目标附近 3 格内找；找不到再扩大到 6 格。
        for (int radius = 0; radius <= 6; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    for (int dy = minDy; dy <= 3; dy++) {
                        int x = tx + dx;
                        int y = ty + dy;
                        int z = tz + dz;
                        BlockPos feet = new BlockPos(x, y, z);
                        if (!isSafeStand(world, feet)) {
                            continue;
                        }
                        double score = dx * dx + dz * dz + dy * dy * 1.5D;
                        // 目标本身是实体方块时，站到它头顶的优先级降低（绕到旁边更好）。
                        if (targetSolid && dx == 0 && dz == 0 && dy > 0) {
                            score += 4.0D;
                        }
                        if (score < bestScore) {
                            bestScore = score;
                            best = new V3(x + 0.5D, y, z + 0.5D, target.dimension);
                        }
                    }
                }
            }
            if (best != null && bestScore <= 2.0D) {
                break;
            }
        }
        return best;
    }

    /** 在指定 x/z 列上找离 preferredY 最近的可站立脚部位置。 */
    private V3 standableColumn(double x, double z, double preferredY) {
        if (this.npc == null || this.npc.entity == null) {
            return null;
        }
        World world = this.npc.entity.world;
        int bx = floor(x);
        int bz = floor(z);
        if (!world.isBlockLoaded(new BlockPos(bx, 0, bz))) {
            return null;
        }
        int center = floor(preferredY);
        int min = Math.max(2, center - 14);
        int max = Math.min(254, center + 14);
        // 真实地形起伏可能超过 ±14；把高度图附近也纳入候选。
        try {
            int height = world.getHeight(bx, bz);
            min = Math.min(min, Math.max(2, height - 14));
            max = Math.max(max, Math.min(254, height + 4));
        } catch (Exception ignored) {
        }

        V3 best = null;
        double bestScore = Double.MAX_VALUE;
        for (int y = min; y <= max; y++) {
            BlockPos feet = new BlockPos(bx, y, bz);
            if (!isSafeStand(world, feet)) {
                continue;
            }
            double score = Math.abs(y - preferredY);
            if (score < bestScore) {
                bestScore = score;
                best = new V3(bx + 0.5D, y, bz + 0.5D, this.npc.entity.dimension);
            }
        }
        return best;
    }

    private V3 getAdjustedStandPoint(V3 target) {
        if (target == null || this.npc == null || this.npc.entity == null) {
            return null;
        }
        World world = this.npc.entity.world;
        int bx = floor(target.x);
        int bz = floor(target.z);
        int by = floor(target.y);
        int[] offsets = {0, 1, -1, 2};
        for (int offset : offsets) {
            BlockPos feet = new BlockPos(bx, by + offset, bz);
            if (isSafeStand(world, feet)) {
                return new V3(bx + 0.5D, by + offset, bz + 0.5D, target.dimension);
            }
        }
        return null;
    }

    private boolean isSafeStand(World world, BlockPos feet) {
        if (feet.getY() <= 0 || feet.getY() >= 255) {
            return false;
        }
        if (!world.isBlockLoaded(feet)) {
            return false;
        }
        IBlockState at = world.getBlockState(feet);
        IBlockState head = world.getBlockState(feet.up());
        if (at.getMaterial().isLiquid() || head.getMaterial().isLiquid()) {
            return false;
        }
        if (at.getCollisionBoundingBox(world, feet) != null) {
            return false;
        }
        if (head.getCollisionBoundingBox(world, feet.up()) != null) {
            return false;
        }

        BlockPos groundPos = feet.down();
        IBlockState ground = world.getBlockState(groundPos);
        if (ground.getMaterial().isLiquid() || !ground.getMaterial().isSolid()) {
            return false;
        }
        if (ground.getCollisionBoundingBox(world, groundPos) == null) {
            return false;
        }
        Block block = ground.getBlock();
        if (block == Blocks.LEAVES || block == Blocks.LEAVES2 || block == Blocks.CACTUS
                || block == Blocks.FIRE || block == Blocks.LAVA || block == Blocks.FLOWING_LAVA) {
            return false;
        }
        if (block instanceof BlockFence || block instanceof BlockWall || block instanceof BlockFenceGate) {
            return false;
        }
        // 不站到箱子/熔炉等容器顶上。
        if (world.getTileEntity(groundPos) instanceof IInventory) {
            return false;
        }
        return true;
    }

    private boolean hasCollision(World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        if (!world.isBlockLoaded(pos)) {
            return false;
        }
        return world.getBlockState(pos).getCollisionBoundingBox(world, pos) != null;
    }

    // ==================================================================
    // 寻路工具
    // ==================================================================

    private Path pathTo(V3 aim) {
        if (aim == null || this.npc == null || this.npc.entity == null) {
            return null;
        }
        if (aim.dimension != this.npc.entity.dimension) {
            return null;
        }
        if (!this.npc.entity.world.isBlockLoaded(aim.toBlockPos())) {
            return null;
        }
        PathNavigate navigator = this.npc.entity.getNavigator();
        if (navigator == null) {
            return null;
        }
        try {
            navigator.clearPath();
            Path path = navigator.getPathToXYZ(aim.x, aim.y, aim.z);
            // 原版 A* 返回 null 表示完全无路。返回非 null 也可能是“只走到半路”的
            // 路径；只有终点确实比当前位置更接近 aim 时才接受，避免在墙边原地
            // 反复得到一条原地路径、看起来像抖动/卡死。
            PathPoint end = path == null ? null : path.getFinalPathPoint();
            if (path != null && end != null) {
                double curDist = distanceTo(this.npc.entity.posX, this.npc.entity.posY,
                        this.npc.entity.posZ, aim);
                double endDist = distanceTo(end.x, end.y, end.z, aim);
                if (endDist > curDist - 0.75D) {
                    return null;
                }
            }
            // 原版 A* 返回 null 表示完全无路；返回非 null 时可能是“走到半路”的路径，
            // 接受它先让 NPC 靠近，走完后再从新位置继续规划。不要因为终点距离
            // 还差几格就丢弃整条路径，否则室内目标/复杂地形会被误判为不可达。
            return path;
        } catch (Exception e) {
            ModSimLoader.log.error("NpcNavigator.pathTo 出错: {}", e.getMessage());
            return null;
        }
    }

    private void clearPath() {
        if (this.npc != null && this.npc.entity != null && this.npc.entity.getNavigator() != null) {
            this.npc.entity.getNavigator().clearPath();
        }
    }

    // ==================================================================
    // 几何工具
    // ==================================================================

    private boolean within(V3 target, double radiusXZ, double radiusY) {
        if (target == null || this.npc == null || this.npc.entity == null) {
            return false;
        }
        if (target.dimension != this.npc.entity.dimension) {
            return false;
        }
        double dx = this.npc.entity.posX - target.x;
        double dz = this.npc.entity.posZ - target.z;
        double dy = Math.abs(this.npc.entity.posY - target.y);
        return dx * dx + dz * dz <= radiusXZ * radiusXZ && dy <= radiusY;
    }

    private double horizontalDistance(V3 target) {
        if (target == null || this.npc == null || this.npc.entity == null) {
            return Double.MAX_VALUE;
        }
        double dx = this.npc.entity.posX - target.x;
        double dz = this.npc.entity.posZ - target.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private boolean isColumnLoaded(V3 target) {
        if (target == null || this.npc == null || this.npc.entity == null) {
            return false;
        }
        return this.npc.entity.world.isBlockLoaded(new BlockPos(target.x, 0, target.z));
    }

    private static boolean sameTarget(V3 a, V3 b) {
        if (a == null || b == null) {
            return false;
        }
        return a.dimension == b.dimension
                && floor(a.x) == floor(b.x)
                && floor(a.y) == floor(b.y)
                && floor(a.z) == floor(b.z);
    }

    private static boolean sameDimension(V3 a, V3 b) {
        return a != null && b != null && a.dimension == b.dimension;
    }

    private static double distanceXZ(V3 a, V3 b) {
        if (a == null || b == null) {
            return Double.MAX_VALUE;
        }
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static double distanceTo(double x, double y, double z, V3 target) {
        if (target == null) {
            return Double.MAX_VALUE;
        }
        double dx = x - target.x;
        double dy = y - target.y;
        double dz = z - target.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }
}
