package com.trhsy.sim.loader;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.GameRegistry;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.loader
 * @ClassName: CraftingLoader
 * @Description: 合成表
 * @date 2023/11/10 下午 5:00
 */
public class CraftingLoader {

    public CraftingLoader() {
        try {
            //合成表
            //registerRecipe();
            //熔炉
            registerSmelting();
            //燃料
            registerFuel();
        } catch (Exception e) {
            StackTraceElement element=e.getStackTrace()[0];ModSimLoader.log.error("CraftingLoader出错了：" + e.getMessage()+"行数："+element.getLineNumber());
        }
    }
    /***
     * @Author fan
     * @Description //TODO 注册菜谱
     * @Date 22:17 2022/5/11
     * @Param []
     * @return void
     **/
    private static void registerRecipe() {
       try {
            /*建筑箱
             *木板 木板 木板
             *圆石 工作台 圆石
             *圆石 圆石 圆石
             * */
            //GameRegistry.addShapedRecipe(new ItemStack(BlockLoader.blockConstructorBox, 1), new Object[]{"PPP", "CWC", "CCC", 'C', Blocks.COBBLESTONE, 'P', Blocks.PLANKS, 'W', Blocks.CRAFTING_TABLE});
            /* 标记棒
             * 黄色燃料
             * 木棍
             * 木棍
             * *//*
            GameRegistry.addRecipe(new ItemStack(BlockLoader.blockMarker, 3), new Object[]{"G", "S", 'S', Items.STICK, 'G', new ItemStack(Items.DYE, 1, 11)});
            //采矿箱和养殖箱是否弃用最贵的钻石镐合成
            if (ConfigLoader.configUseExpensiveRecipies) {
                *//** 采矿箱 养殖箱
                 * 木板 木板 木板
                 * 圆石 镐子/锄头 圆石
                 * 圆石 圆石 圆石
                 *//*
                GameRegistry.addRecipe(new ItemStack(BlockLoader.blockMiningBox, 1), new Object[]{"PPP", "CWC", "CCC", 'C', Blocks.COBBLESTONE, 'P', Blocks.PLANKS, 'W', Items.DIAMOND_PICKAXE});
                GameRegistry.addRecipe(new ItemStack(BlockLoader.blockFarmingBox, 1), new Object[]{"PPP", "CWC", "CCC", 'C', Blocks.COBBLESTONE, 'P', Blocks.PLANKS, 'W', Items.DIAMOND_HOE});
            } else {
                GameRegistry.addRecipe(new ItemStack(BlockLoader.blockMiningBox, 1), new Object[]{"PPP", "CWC", "CCC", 'C', Blocks.COBBLESTONE, 'P', Blocks.PLANKS, 'W', Items.STONE_PICKAXE});
                GameRegistry.addRecipe(new ItemStack(BlockLoader.blockFarmingBox, 1), new Object[]{"PPP", "CWC", "CCC", 'C', Blocks.COBBLESTONE, 'P', Blocks.PLANKS, 'W', Items.STONE_HOE});
            }
            //四个火把合成灯箱
            GameRegistry.addRecipe(new ItemStack(BlockLoader.blockLightBox, 2), new Object[]{"LL", "LL", 'L', Blocks.TORCH});
            GameRegistry.addShapelessRecipe(new ItemStack(BlockLoader.blockLightBox, 1, 1), new Object[]{BlockLoader.blockLightBox, new ItemStack(Items.DYE, 1, 1)});
            GameRegistry.addShapelessRecipe(new ItemStack(BlockLoader.blockLightBox, 1, 2), new Object[]{BlockLoader.blockLightBox, new ItemStack(Items.DYE, 1, 14)});
            GameRegistry.addShapelessRecipe(new ItemStack(BlockLoader.blockLightBox, 1, 3), new Object[]{BlockLoader.blockLightBox, new ItemStack(Items.DYE, 1, 11)});
            GameRegistry.addShapelessRecipe(new ItemStack(BlockLoader.blockLightBox, 1, 4), new Object[]{BlockLoader.blockLightBox, new ItemStack(Items.DYE, 1, 10)});
            GameRegistry.addShapelessRecipe(new ItemStack(BlockLoader.blockLightBox, 1, 5), new Object[]{BlockLoader.blockLightBox, new ItemStack(Items.DYE, 1, 4)});
            GameRegistry.addShapelessRecipe(new ItemStack(BlockLoader.blockLightBox, 1, 6), new Object[]{BlockLoader.blockLightBox, new ItemStack(Items.DYE, 1, 5)});
            GameRegistry.addShapelessRecipe(new ItemStack(BlockLoader.blockLightBox, 1, 7), new Object[]{BlockLoader.blockLightBox, new ItemStack(Items.DYE, 1, 1), new ItemStack(Items.DYE, 1, 14), new ItemStack(Items.DYE, 1, 11), new ItemStack(Items.DYE, 1, 10), new ItemStack(Items.DYE, 1, 4), new ItemStack(Items.DYE, 1, 5)});


            //九个奶酪片合成奶酪块
            GameRegistry.addRecipe(new ItemStack(BlockLoader.blockCheese, 1), new Object[]{"CCC", "CCC", "CCC", 'C', new ItemStack(ItemLoader.itemCheese, 1, 0)});
            //一个奶酪块分解九奶酪片
            GameRegistry.addShapelessRecipe(new ItemStack(ItemLoader.itemCheese, 9), BlockLoader.blockCheese);
            *//* 复合砖
             * 硬化黏土 石头 硬化黏土
             * 石头 栅栏 石头
             * 硬化黏土 石头 硬化黏土
             * *//*
            GameRegistry.addRecipe(new ItemStack(BlockLoader.blockCompositeBrick, 1), new Object[]{"CSC", "SIS", "CSC", 'C', Blocks.HARDENED_CLAY, 'S', Blocks.STONE, 'I', Blocks.OAK_FENCE});
            *//* 风车底座
             *       复合砖
             * 复合砖 复合砖 复合砖
             * 复合砖 复合砖 复合砖
             *//*
            GameRegistry.addRecipe(new ItemStack(ItemLoader.itemWindmillBase), new Object[]{" C ", "CCC", "CCC", 'C', BlockLoader.blockCompositeBrick});
            *//*控制箱
             *木板 木板 木板
             *圆石 建筑箱 圆石
             *圆石 圆石 圆石
             *//*
            GameRegistry.addRecipe(new ItemStack(BlockLoader.blockControlBox, 1), new Object[]{
                    "PPP", "CWC", "CCC",
                    'C', Blocks.COBBLESTONE,
                    'P', Blocks.PLANKS,
                    'W', BlockLoader.blockConstructorBox
            });
            //地毯
            for (int i = 0; i < 16; i++) {
                GameRegistry.addRecipe(new ItemStack(BlockLoader.blockLiving, 3,i), new Object[]{"SSS", 'S', Items.STICK});
            }

            for (int c = 0; c < 16; ++c) {
                *//** 风车叶片
                 *木棍 木棍 木棍
                 *羊毛 羊毛 羊毛
                 *//*
                GameRegistry.addRecipe(new ItemStack(ItemLoader.itemWindmillVane, 1, c), new Object[]{"WWW", "SSS", 'S', Items.STICK, 'W', new ItemStack(Blocks.WOOL, 1, c)});
            }

            for (int c = 0; c < 16; ++c) {
                *//** 风车帆
                 *      叶片
                 * 叶片 木板 叶片
                 *     叶片
                 *//*
                GameRegistry.addRecipe(new ItemStack(ItemLoader.itemWindmillSails, 1, c), new Object[]{" V ", "VPV", " V ", 'V', new ItemStack(ItemLoader.itemWindmillVane, 1, c), 'P', Blocks.PLANKS});
            }

            for (int c = 0; c < 16; ++c) {
                *//** 风车
                 * 风车帆
                 * 风车底座
                 *
                 *//*
                GameRegistry.addRecipe(new ItemStack(BlockLoader.blockWindmill, 1, c), new Object[]{"S", "B", 'S', new ItemStack(ItemLoader.itemWindmillSails, 1, c), 'B', ItemLoader.itemWindmillBase});

            }
            *//**铜块**//*
            GameRegistry.addShapedRecipe(new ItemStack(BlockLoader.blockCopper), new Object[]
                    {
                            "###", "###", "###", '#', ItemLoader.itemCopperIngot
                    });
            *//**一个铜块等于九个铜锭**//*
            GameRegistry.addShapelessRecipe(new ItemStack(ItemLoader.itemCopperIngot, 9), BlockLoader.blockCopper);
            *//**铜镐
             * 铜锭 铜锭 铜锭
             *     木棍
             *     木棍
             * **//*
            GameRegistry.addShapedRecipe(new ItemStack(ItemLoader.copperPickaxe), new Object[]
                    {
                            "###", " * ", " * ", '#', ItemLoader.itemCopperIngot, '*', Items.STICK
                    });
            *//**铜锄头**//*
            GameRegistry.addShapedRecipe(new ItemStack(ItemLoader.copperHoe), new Object[]
                    {
                            "## ", " * ", " * ", '#', ItemLoader.itemCopperIngot, '*', Items.STICK
                    });
            *//**铜斧子**//*
            GameRegistry.addShapedRecipe(new ItemStack(ItemLoader.copperAxe), new Object[]
                    {
                            "## ", "#* ", " * ", '#', ItemLoader.itemCopperIngot, '*', Items.STICK
                    });
            *//**铜锹**//*
            GameRegistry.addShapedRecipe(new ItemStack(ItemLoader.copperSpade), new Object[]
                    {
                            " # ", " * ", " * ", '#', ItemLoader.itemCopperIngot, '*', Items.STICK
                    });
            *//**铜剑**//*
            GameRegistry.addShapedRecipe(new ItemStack(ItemLoader.copperSword), new Object[]
                    {
                            " # ", " # "," * ", '#', ItemLoader.itemCopperIngot, '*', Items.STICK
                    });
            *//**铜头盔**//*
            GameRegistry.addShapedRecipe(new ItemStack(ItemLoader.copperHelmet), new Object[]
                    {
                            "###", "# #", '#', ItemLoader.itemCopperIngot
                    });
            *//**铜甲**//*
            GameRegistry.addShapedRecipe(new ItemStack(ItemLoader.copperChestplate), new Object[]
                    {
                            "# #", "###", "###", '#', ItemLoader.itemCopperIngot
                    });
            *//**铜护腿**//*
            GameRegistry.addShapedRecipe(new ItemStack(ItemLoader.copperLeggings), new Object[]
                    {
                            "###", "# #", "# #", '#', ItemLoader.itemCopperIngot
                    });
            *//**铜鞋**//*
            GameRegistry.addShapedRecipe(new ItemStack(ItemLoader.copperBoots), new Object[]
                    {
                            "# #", "# #", '#', ItemLoader.itemCopperIngot
                    });

            *//**锡块**//*
            GameRegistry.addShapedRecipe(new ItemStack(BlockLoader.blockTin), new Object[]
                    {
                            "###", "###", "###", '#', ItemLoader.itemTinIngot
                    });
            *//**一个铜块等于九个铜锭**//*
            GameRegistry.addShapelessRecipe(new ItemStack(ItemLoader.itemTinIngot, 9), BlockLoader.blockTin);
            *//**锡镐
             * 锡锭 锡锭 锡锭
             *     木棍
             *     木棍
             * **//*
            GameRegistry.addShapedRecipe(new ItemStack(ItemLoader.tinPickaxe), new Object[]
                    {
                            "###", " * ", " * ", '#', ItemLoader.itemTinIngot, '*', Items.STICK
                    });
            *//**锡锄头**//*
            GameRegistry.addShapedRecipe(new ItemStack(ItemLoader.tinHoe), new Object[]
                    {
                            "## ", " * ", " * ", '#', ItemLoader.itemTinIngot, '*', Items.STICK
                    });
            *//**锡斧子**//*
            GameRegistry.addShapedRecipe(new ItemStack(ItemLoader.tinAxe), new Object[]
                    {
                            "## ", "#* ", " * ", '#', ItemLoader.itemTinIngot, '*', Items.STICK
                    });
            *//**锡锹**//*
            GameRegistry.addShapedRecipe(new ItemStack(ItemLoader.tinSpade), new Object[]
                    {
                            " # ", " * ", " * ", '#', ItemLoader.itemTinIngot, '*', Items.STICK
                    });
            *//**锡剑**//*
            GameRegistry.addShapedRecipe(new ItemStack(ItemLoader.tinSword), new Object[]
                    {
                            " # ", " # "," * ", '#', ItemLoader.itemTinIngot, '*', Items.STICK
                    });
            *//**锡头盔**//*
            GameRegistry.addShapedRecipe(new ItemStack(ItemLoader.tinHelmet), new Object[]
                    {
                            "###", "# #", '#', ItemLoader.itemTinIngot
                    });
            *//**锡甲**//*
            GameRegistry.addShapedRecipe(new ItemStack(ItemLoader.tinChestplate), new Object[]
                    {
                            "# #", "###", "###", '#', ItemLoader.itemTinIngot
                    });
            *//**锡护腿**//*
            GameRegistry.addShapedRecipe(new ItemStack(ItemLoader.tinLeggings), new Object[]
                    {
                            "###", "# #", "# #", '#', ItemLoader.itemTinIngot
                    });
            *//**锡鞋**//*
            GameRegistry.addShapedRecipe(new ItemStack(ItemLoader.tinBoots), new Object[]
                    {
                            "# #", "# #", '#', ItemLoader.itemTinIngot
                    });

            //CraftingManager.getInstance().addRecipe();
            */
        } catch (Exception e) {
            StackTraceElement element=e.getStackTrace()[0];ModSimLoader.log.error("registerRecipe出错了：" + e.getMessage()+"行数："+element.getLineNumber());
        }




    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO  注册熔炼
     * @Date 22:17 2022/5/11
     * @Param []
     **/
    private static void registerSmelting() {
        // 铜/锡矿与颗粒已移除，相关熔炼配方不再注册。
    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 注册燃料
     * @Date 22:17 2022/5/11
     * @Param []
     **/
    private static void registerFuel() {
        try {
        } catch (Exception e) {
            StackTraceElement element=e.getStackTrace()[0];ModSimLoader.log.error("registerFuel出错了：" + e.getMessage()+"行数："+element.getLineNumber());
        }
    }
}
