package com.trhsy.sim.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.IPlantable;

import java.util.List;

/**
 * P15(S1): Pam's HarvestCraft 作物/种子兼容。
 *
 * <p>Pam's 的种子是 {@code ItemPamSeedFood}（继承 ItemSeeds），旧代码在子类上
 * 反射 {@code getDeclaredField("crops")} 会找不到父类字段；这里改走
 * {@link IPlantable#getPlant(World, BlockPos)}。成熟度仍然优先用 BlockCrops，
 * 非 BlockCrops 的模组作物按 metadata 7 兜底。</p>
 *
 * <p>不硬依赖 Pam's：只按类名/命名空间识别，未安装时所有判断自然返回 false。</p>
 */
public final class ModCropCompat {
    private static final String PAM_PACKAGE = "com.pam.harvestcraft";

    private ModCropCompat() {
    }

    /** 是否是 Pam's HarvestCraft 的作物方块。 */
    public static boolean isModCrop(Block block) {
        return block != null && block.getClass().getName().startsWith(PAM_PACKAGE);
    }

    /**
     * 作物是否成熟。BlockCrops（含 Pam's 的 BlockPamCrop）走 isMaxAge；
     * 其他 Pam's 方块按 metadata 7 兜底。
     */
    public static boolean isMature(World world, BlockPos pos, IBlockState state) {
        if (world == null || pos == null || state == null) {
            return false;
        }
        Block block = state.getBlock();
        if (block instanceof BlockCrops) {
            return ((BlockCrops) block).isMaxAge(state);
        }
        if (isModCrop(block)) {
            return block.getMetaFromState(state) >= 7;
        }
        return false;
    }

    /**
     * 从种子/作物物品解析要放置的作物方块。
     * 优先走 {@link IPlantable#getPlant(World, BlockPos)}，这样 Pam's 等模组种子
     * 不需要反射私有字段；原版仙人掌/可可豆等 ItemBlock 再走名称兜底。
     */
    public static Block resolveCropBlock(ItemStack stack, World world, BlockPos soilPos) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        Item item = stack.getItem();
        if (item instanceof IPlantable) {
            try {
                BlockPos pos = soilPos != null ? soilPos : BlockPos.ORIGIN;
                Block planted = ((IPlantable) item).getPlant(world, pos).getBlock();
                if (planted != null && planted != Blocks.AIR) {
                    return planted;
                }
            } catch (Throwable ignored) {
                // 某些模组种子的 getPlant 需要完整世界状态，失败后走名称兜底。
            }
        }
        String name = item.getUnlocalizedName();
        String registry = item.getRegistryName() != null ? item.getRegistryName().toString() : "";
        if (name.contains("reeds") || "minecraft:reeds".equals(registry)) {
            return Blocks.REEDS;
        }
        if (name.equals("item.seeds") || "minecraft:wheat_seeds".equals(registry)) {
            return Blocks.WHEAT;
        }
        if (name.equals("item.seeds_melon") || "minecraft:melon_seeds".equals(registry)) {
            return Blocks.MELON_STEM;
        }
        if (name.equals("item.seeds_pumpkin") || "minecraft:pumpkin_seeds".equals(registry)) {
            return Blocks.PUMPKIN_STEM;
        }
        if (name.equals("item.carrots") || "minecraft:carrot".equals(registry)) {
            return Blocks.CARROTS;
        }
        if (name.equals("item.potato") || "minecraft:potato".equals(registry)) {
            return Blocks.POTATOES;
        }
        if (name.equals("item.beetroot_seeds") || "minecraft:beetroot_seeds".equals(registry)) {
            return Blocks.BEETROOTS;
        }
        if (name.contains("cactus") || "minecraft:cactus".equals(registry)) {
            return Blocks.CACTUS;
        }
        if (name.equals("item.dye") || "minecraft:dye".equals(registry)) {
            return Blocks.COCOA;
        }
        return null;
    }

    /**
     * 某些 Pam's 方块不是 BlockCrops 时，原版耕地会拒绝；给 Pam's 一个明确例外。
     * BlockPamCrop 本身继承 BlockCrops，会直接走原版 canSustainPlant。
     */
    public static boolean canPlantModCropOn(Block soil, Block crop) {
        return isModCrop(crop) && soil == Blocks.FARMLAND;
    }

    /**
     * P15(S1): 给杂货店补充少量 Pam's 食物清单。
     * 只收 harvestcraft 命名空间下的 ItemFood，跳过种子/树苗/工具/饮品，
     * 避免把不可生产的物品塞满收集清单。
     */
    public static void addPamProduce(List<ItemStack> items, int max) {
        if (items == null || max <= 0) {
            return;
        }
        int added = 0;
        for (Item item : Item.REGISTRY) {
            if (item == null || item.getRegistryName() == null) {
                continue;
            }
            if (!"harvestcraft".equals(item.getRegistryName().getResourceDomain())) {
                continue;
            }
            if (!(item instanceof ItemFood)) {
                continue;
            }
            String path = item.getRegistryName().getResourcePath();
            if (path.contains("seed") || path.contains("sapling") || path.contains("tool")
                    || path.contains("juice") || path.contains("smoothie") || path.contains("soda")) {
                continue;
            }
            items.add(new ItemStack(item, 8));
            if (++added >= max) {
                break;
            }
        }
    }
}
