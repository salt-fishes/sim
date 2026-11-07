package com.trhsy.sim.npcCode.task;

import com.trhsy.sim.entity.EntityNpc;
import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.NpcData;
import com.trhsy.sim.npcCode.V3;
import com.trhsy.sim.npcCode.build.Building;
import com.trhsy.sim.npcCode.job.Job;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityGhast;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 士兵任务（P4，参照原版铁傀儡算法重写）。
 *
 * <p>设计对照（铁傀儡 -&gt; 士兵）：
 * <ul>
 *   <li>村庄中心/半径 -&gt; 工作岗位为"家"，驻区半径 24（见 JobSoldier.setHomePosAndDistance）；</li>
 *   <li>MoveThroughVillage -&gt; 固定巡逻路点：以岗位为中心取 6 个圆周点，循环巡逻，到点停留 2-5 秒；</li>
 *   <li>DefendVillage &gt; HurtByTarget &gt; NearestAttackableTarget -&gt;
 *       保护被攻击的玩家/NPC &gt; 反击 &gt; 10 格内最近敌对生物；</li>
 *   <li>苦力怕全程排除；目标离开驻区（24 格）就放弃（对应铁傀儡离开村庄范围放弃）。</li>
 * </ul>
 * 移动全部只走路（walkTo），不传送。
 */
public class JobTaskPatrol extends JobTask {
    /** 驻区半径（与 JobSoldier 的 setHomePosAndDistance 保持一致） */
    public static final int PATROL_RADIUS = 24;
    /** 路点圈半径 */
    private static final double WAYPOINT_RADIUS = 12.0D;
    /** 路点数量 */
    private static final int WAYPOINT_COUNT = 6;

    /** 当前目标 */
    Entity currentTarget = null;
    /** 当前要去的点（路点或随机兜底点） */
    V3 patrolTo = null;
    /** 固定巡逻路点 */
    private List<V3> waypoints = new ArrayList<V3>();
    private int waypointIndex = 0;
    /** 到点停留截止时间 */
    private long dwellUntil = 0L;

    double maxX;
    double minX;
    double maxZ;
    double minZ;
    /** 攻击延迟 */
    long attackDelay;
    /** 找怪节流 */
    long targetSearchDelay;
    /** 移动节流 */
    long moveDelay;
    /** 追击开始时间（追太久到不了就放弃） */
    long chaseStart;

    public JobTaskPatrol(Job j, long ms) {
        super(j, ms);
        //工作空间
        this.maxX = this.job.workPlace.x + 20.0D;
        this.minX = this.job.workPlace.x - 20.0D;
        this.maxZ = this.job.workPlace.z + 20.0D;
        this.minZ = this.job.workPlace.z - 20.0D;
        //零攻击延迟
        this.attackDelay = 0L;

        Building b;
        for (Iterator var4 = ModSimLoader.buildings.iterator(); var4.hasNext(); this.minZ = b.controlXYZ.z < this.minZ ? b.controlXYZ.z : this.minZ) {
            b = (Building) var4.next();
            this.maxX = b.controlXYZ.x > this.maxX ? b.controlXYZ.x : this.maxX;
            this.minX = b.controlXYZ.x < this.minX ? b.controlXYZ.x : this.minX;
            this.maxZ = b.controlXYZ.z > this.maxZ ? b.controlXYZ.z : this.maxZ;
        }

        this.maxX += 20.0D;
        this.maxZ += 20.0D;
        this.minX -= 20.0D;
        this.minZ -= 20.0D;
    }

    @Override
    public void onTaskBegin() {
        buildWaypoints();
        this.patrolTo = nextPatrolPoint();
    }

    /** 以工作岗位为中心生成固定巡逻路点（圆周 6 个点，自动找可站立高度）。 */
    private void buildWaypoints() {
        this.waypoints = new ArrayList<V3>();
        this.waypointIndex = 0;
        double cx = this.job.workPlace.x + 0.5D;
        double cz = this.job.workPlace.z + 0.5D;
        double cy = this.job.workPlace.y;
        for (int i = 0; i < WAYPOINT_COUNT; i++) {
            double angle = Math.PI * 2.0D * i / WAYPOINT_COUNT;
            double wx = cx + Math.cos(angle) * WAYPOINT_RADIUS;
            double wz = cz + Math.sin(angle) * WAYPOINT_RADIUS;
            V3 wp = findStandable(wx, cy, wz);
            if (wp != null) {
                this.waypoints.add(wp);
            }
        }
        if (this.waypoints.size() < 2) {
            this.waypoints.clear();
        }
    }

    /** 在给定 x/z 附近找一个"两格空气 + 下方实体方块"的站立点。 */
    private V3 findStandable(double x, double y, double z) {
        try {
            World world = this.folk.entity.world;
            BlockPos base = new BlockPos(x, y + 2.0D, z);
            for (int dy = -4; dy <= 3; dy++) {
                BlockPos p = base.add(0, dy, 0);
                if (world.isAirBlock(p) && world.isAirBlock(p.up())
                        && world.getBlockState(p.down()).isFullBlock()) {
                    return new V3(x, p.getY(), z);
                }
            }
        } catch (Exception e) {
            ModSimLoader.log.error("JobTaskPatrol-findStandable出错了：" + e.getMessage());
        }
        return null;
    }

    @Override
    public void onUpdate() {
        try {
            if (this.folk.entity == null) {
                return;
            }
            long now = System.currentTimeMillis();

            // 目标失效清理：死亡、过远、或跑出驻区（对应铁傀儡离开村庄范围放弃）
            if (this.currentTarget != null) {
                double dx = Math.abs(this.currentTarget.posX - this.job.workPlace.x);
                double dz = Math.abs(this.currentTarget.posZ - this.job.workPlace.z);
                if (this.currentTarget.isDead || this.currentTarget.getDistance(this.folk.entity) > PATROL_RADIUS
                        || dx > PATROL_RADIUS || dz > PATROL_RADIUS) {
                    this.currentTarget = null;
                    this.chaseStart = 0L;
                }
            }

            // 索敌：铁傀儡优先级 —— 保护村民/玩家 > 反击 > 10 格内最近敌对生物
            if (this.currentTarget == null && now - this.targetSearchDelay > 1000L) {
                this.targetSearchDelay = now;
                this.currentTarget = findDefendTarget(16.0D);
                if (this.currentTarget == null) {
                    Entity revenge = this.folk.entity.getRevengeTarget();
                    if (revenge instanceof EntityMob && !(revenge instanceof EntityCreeper)
                            && !(revenge instanceof EntityGhast)) {
                        this.currentTarget = revenge;
                    }
                }
                if (this.currentTarget == null) {
                    this.currentTarget = findNearestMonster(10.0D);
                }
                if (this.currentTarget != null) {
                    this.chaseStart = now;
                }
            }

            if (this.currentTarget != null) {
                //攻击谁
                this.folk.setStatus(new TextComponentTranslation("container.sim.ATTACKING", new Object[0]).getUnformattedText() + " " + this.currentTarget.getName());
                V3 mobPos = V3.fromBlockPos(this.currentTarget.getPosition());
                // 追击移动节流；只走路，追不到就追不到，绝不传送
                if (now - this.moveDelay > 500L) {
                    this.moveDelay = now;
                    this.folk.walkTo(mobPos);
                }
                if (mobPos.getDistanceTo(this.folk.getV3()) < 3) {
                    // 攻击间隔 1 秒（对应铁傀儡 EntityAIAttackMelee 的 20 tick）
                    if (now - this.attackDelay > 1000L) {
                        this.attackDelay = now;
                        if (this.folk.entity instanceof EntityLivingBase) {
                            this.currentTarget.attackEntityFrom(
                                    DamageSource.causeMobDamage((EntityLivingBase) this.folk.entity), 8.0F);
                        }
                        if (this.folk.entity instanceof EntityNpc) {
                            ((EntityNpc) this.folk.entity).swing();
                        }
                        if (this.currentTarget.isDead) {
                            this.currentTarget = null;
                            this.chaseStart = 0L;
                        }
                    }
                } else if (now - this.chaseStart > 10000L) {
                    // 追了 10 秒还没进攻击距离（多半到不了），放弃
                    this.currentTarget = null;
                    this.chaseStart = 0L;
                }
            } else {
                // 无目标：固定路点巡逻（到点停留 2-5 秒）
                this.folk.setStatus(new TextComponentTranslation("container.sim.ONPATROL", new Object[0]).getUnformattedText());
                if (now < this.dwellUntil) {
                    return;
                }
                if (this.patrolTo == null || this.folk.isAtLocation(this.patrolTo)) {
                    if (this.patrolTo != null) {
                        this.dwellUntil = now + 2000L + this.rand.nextInt(3000);
                    }
                    this.patrolTo = nextPatrolPoint();
                    return;
                }
                if (now - this.moveDelay > 500L) {
                    this.moveDelay = now;
                    // 只走路；到不了这个路点就跳下一个，不传送
                    if (!this.folk.walkTo(this.patrolTo)) {
                        this.patrolTo = nextPatrolPoint();
                    }
                }
            }
        } catch (Exception e) {
            ModSimLoader.log.error("JobTaskPatrol-onUpdate出错了：" + e.getMessage());
        }
    }

    /** 下一个巡逻点：优先固定路点循环，路点不可用时退回随机点。 */
    private V3 nextPatrolPoint() {
        if (this.waypoints != null && !this.waypoints.isEmpty()) {
            V3 wp = this.waypoints.get(this.waypointIndex);
            this.waypointIndex = (this.waypointIndex + 1) % this.waypoints.size();
            return wp;
        }
        return this.getNewPosition();
    }

    /**
     * 保护目标（对应铁傀儡 EntityAIDefendVillage）：
     * 附近 16 格内，玩家或其他 NPC 刚被某个敌对生物攻击 → 去打那个生物。
     */
    private Entity findDefendTarget(double range) {
        try {
            World world = this.folk.entity.world;
            double x = this.folk.entity.posX;
            double y = this.folk.entity.posY;
            double z = this.folk.entity.posZ;
            AxisAlignedBB bb = new AxisAlignedBB(x - range, y - 6.0D, z - range, x + range, y + 6.0D, z + range);
            // 玩家
            for (EntityPlayer p : world.playerEntities) {
                if (p == null) {
                    continue;
                }
                EntityLivingBase rev = p.getRevengeTarget();
                if (rev != null && rev.isEntityAlive() && !(rev instanceof EntityCreeper)
                        && !(rev instanceof EntityGhast) && bb.contains(p.getPositionVector())) {
                    return rev;
                }
            }
            // 其他 NPC
            for (NpcData fd : ModSimLoader.folks) {
                if (fd == null || fd == this.folk || fd.entity == null) {
                    continue;
                }
                if (!bb.contains(fd.entity.getPositionVector())) {
                    continue;
                }
                EntityLivingBase rev = fd.entity.getRevengeTarget();
                if (rev != null && rev.isEntityAlive() && !(rev instanceof EntityCreeper)
                        && !(rev instanceof EntityGhast)) {
                    return rev;
                }
            }
        } catch (Exception e) {
            ModSimLoader.log.error("JobTaskPatrol-findDefendTarget出错了：" + e.getMessage());
        }
        return null;
    }

    /** 找最近的敌对生物（EntityMob 覆盖僵尸/骷髅/苦力怕/蜘蛛/史莱姆/恶魂）。
     *  排除苦力怕（近战贴脸会被炸死）和恶魂（飞在天上够不到）。 */
    private Entity findNearestMonster(double range) {
        double x = this.folk.entity.posX;
        double y = this.folk.entity.posY;
        double z = this.folk.entity.posZ;
        AxisAlignedBB bb = new AxisAlignedBB(x - range, y - 4.0D, z - range, x + range, y + 6.0D, z + range);
        Entity target = this.folk.entity.world.findNearestEntityWithinAABB(EntityMob.class, bb, this.folk.entity);
        if (target instanceof EntityCreeper || target instanceof EntityGhast) {
            return null;
        }
        return target;
    }

    @Override
    public void onTaskComplete() {

    }

    /**
     * 随机巡逻点（路点生成失败时的兜底）：
     * 走出巡逻边界或离岗位超过 40 格 → 朝岗位方向取点；最多重试 5 次。
     */
    V3 getNewPosition() {
        Vec3d post = new Vec3d(this.job.workPlace.x + 0.5D, this.job.workPlace.y, this.job.workPlace.z + 0.5D);
        boolean outside = this.folk.entity.posX > this.maxX || this.folk.entity.posX < this.minX
                || this.folk.entity.posZ > this.maxZ || this.folk.entity.posZ < this.minZ;
        double distToPost = Math.abs(this.folk.entity.posX - post.x) + Math.abs(this.folk.entity.posZ - post.z);
        boolean goBack = outside || distToPost > 40.0D;
        for (int i = 0; i < 5; i++) {
            Vec3d v3d;
            if (goBack || this.rand.nextInt(3) == 0) {
                v3d = RandomPositionGenerator.findRandomTargetBlockTowards(this.job.folk.entity, 10, 7, post);
            } else {
                v3d = RandomPositionGenerator.findRandomTarget(this.job.folk.entity, 10, 7);
            }
            if (v3d != null) {
                return V3.fromVec3d(v3d);
            }
        }
        return new V3(post.x, post.y, post.z);
    }
}
