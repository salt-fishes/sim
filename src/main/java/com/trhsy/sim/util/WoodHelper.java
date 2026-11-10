package com.trhsy.sim.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * 木材族的兼容判断。
 *
 * <p>1.12.2 里木板共用一个 Block（不同 meta 区分橡木/云杉/白桦/丛林/金合欢/深色橡木），
 * 原木却分成 LOG 和 LOG2 两个 Block。旧代码按 Item 精确匹配，导致：
 * <ul>
 *   <li>蓝图要橡木原木时，箱子里的云杉/白桦原木不算；</li>
 *   <li>蓝图要橡木木板时，虽然物品相同，但放置出来永远是橡木。</li>
 * </ul>
 * 这里把“木板/原木/木栅栏”当成一个材料族：交材料时可以互相替代，
 * 但放置仍使用蓝图原本的方块状态，保证建筑外观按设计走。
 */
public final class WoodHelper {
    private WoodHelper() {
    }

    public static boolean isPlanks(Block block) {
        return block == Blocks.PLANKS;
    }

    public static boolean isLog(Block block) {
        return block == Blocks.LOG || block == Blocks.LOG2;
    }

    public static boolean isWood(Block block) {
        return isPlanks(block) || isLog(block) || isWoodFence(block);
    }

    /**
     * 1.12 的六种木栅栏是六个独立 Block；下界砖栅栏不是木材。
     */
    public static boolean isWoodFence(Block block) {
        return block instanceof BlockFence && block != Blocks.NETHER_BRICK_FENCE;
    }

    /**
     * 箱子里的物品能否满足蓝图方块的材料要求。
     * 非木材仍保持原来的精确 Item 匹配，避免改变其他材料的行为。
     */
    public static boolean matches(Block required, ItemStack stack) {
        if (required == null || stack == null || stack.isEmpty()) {
            return false;
        }
        Block provided = Block.getBlockFromItem(stack.getItem());
        if (isPlanks(required)) {
            return provided == Blocks.PLANKS;
        }
        if (isLog(required)) {
            return isLog(provided);
        }
        if (isWoodFence(required)) {
            return isWoodFence(provided);
        }
        return stack.getItem() == Item.getItemFromBlock(required);
    }

}
