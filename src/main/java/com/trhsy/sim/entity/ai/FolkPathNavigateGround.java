package com.trhsy.sim.entity.ai;

import net.minecraft.entity.EntityLiving;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.world.World;

/**
 * 受控寻路器：把单次 A* 的搜索半径限制在 48 格以内。
 *
 * <p>原版 {@link PathNavigateGround} 直接使用 FOLLOW_RANGE（本模组是 256），
 * 每次寻路都会构造半径 264 格的 ChunkCache；在真实地形上既慢又容易失败。
 * 长距离移动改由 {@code NpcNavigator} 拆成多个 24 格左右的 hop，
 * 单次寻路只需要处理当前这一段地形。</p>
 */
public class FolkPathNavigateGround extends PathNavigateGround {
    /** 单次寻路的最大搜索半径（格）。 */
    private static final float MAX_SEARCH_RANGE = 48.0F;

    public FolkPathNavigateGround(EntityLiving entityIn, World worldIn) {
        super(entityIn, worldIn);
    }

    @Override
    public float getPathSearchRange() {
        return Math.min(super.getPathSearchRange(), MAX_SEARCH_RANGE);
    }
}
