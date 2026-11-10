package com.trhsy.sim.util;

import com.trhsy.sim.loader.ModSimLoader;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Random;

/**
 * @ClassName PricesForBlocks
 * @Description todo
 * @Author TRHSY
 * @Date 2023/7/323:04
 **/
public class PricesForBlocks implements Serializable {
    private static final long serialVersionUID = -2617939458756927761L;
    //木板
    private static Float basePricePlanks = 0.0131F;
    //原木
    private static Float basePriceLogs = 0.0524F;
    //圆石
    private static Float basePriceCobblestone = 0.0032F;
    //石头
    private static Float basePriceStone = 0.0141F;
    //玻璃
    private static Float basePriceGlass = 0.0121F;
    //羊毛
    private static Float basePriceWool = 0.0115F;
    //砖块
    private static Float basePriceBrick = 0.0251F;
    //石砖
    private static Float basePriceStonebrick = 0.0261F;
    //栅栏
    private static Float basePriceFence = 0.0113F;
    //钻石
    public static Float bankPriceDiamond = 10.23F;
    //绿宝石
    public static Float bankPriceEmerald = 9.34F;
    //红石
    public static Float bankPriceRedstone = 3.75F;
    //萤石
    public static Float bankPriceGlowstone = 2.48F;
    //金
    public static Float bankPriceGold = 5.12F;
    //锡
    //铜
    //铁
    public static Float bankPriceIron = 5.12F;

    public PricesForBlocks() {
    }

    public static Float getPrice(Block block, boolean isBuying) {
        float base = 0.0F;
        try {
            if (block == Blocks.PLANKS) {
                base = basePricePlanks;
            } else if (block == Blocks.LOG) {
                base = basePriceLogs;
            } else if (block == Blocks.COBBLESTONE) {
                base = basePriceCobblestone;
            } else if (block == Blocks.STONE) {
                base = basePriceStone;
            } else if (block == Blocks.GLASS) {
                base = basePriceGlass;
            } else if (block == Blocks.WOOL) {
                base = basePriceWool;
            } else if (block == Blocks.BRICK_BLOCK) {
                base = basePriceBrick;
            } else if (block == Blocks.STONEBRICK) {
                base = basePriceStonebrick;
            } else if (block == Blocks.OAK_FENCE||block == Blocks.SPRUCE_FENCE||block == Blocks.BIRCH_FENCE||block == Blocks.JUNGLE_FENCE||block == Blocks.DARK_OAK_FENCE||block == Blocks.ACACIA_FENCE) {
                base = basePriceFence;
            }

            base *= 64.0F;
            if (isBuying) {
                base = (float)((double)base + (double)base * 1.12D);
            }
        } catch (Exception e) {
            StackTraceElement element=e.getStackTrace()[0];ModSimLoader.log.error("getPrice出错了：" + e.getMessage()+"行数："+element.getLineNumber());
        }


        return base;
    }

    public static void setPrice(Block block, float newPrice) {
        try {
            if (block == Blocks.PLANKS) {
                basePricePlanks = newPrice;
            } else if (block == Blocks.LOG) {
                basePriceLogs = newPrice;
            } else if (block == Blocks.COBBLESTONE) {
                basePriceCobblestone = newPrice;
            } else if (block == Blocks.STONE) {
                basePriceStone = newPrice;
            } else if (block == Blocks.GLASS) {
                basePriceGlass = newPrice;
            } else if (block == Blocks.WOOL) {
                basePriceWool = newPrice;
            } else if (block == Blocks.BRICK_BLOCK) {
                basePriceBrick = newPrice;
            } else if (block == Blocks.STONEBRICK) {
                basePriceStonebrick = newPrice;
            } else if (block == Blocks.OAK_FENCE||block == Blocks.SPRUCE_FENCE||block == Blocks.BIRCH_FENCE||block == Blocks.JUNGLE_FENCE||block == Blocks.DARK_OAK_FENCE||block == Blocks.ACACIA_FENCE) {
                basePriceFence = newPrice;
            }
        } catch (Exception e) {
            StackTraceElement element=e.getStackTrace()[0];ModSimLoader.log.error("setPrice出错了：" + e.getMessage()+"行数："+element.getLineNumber());
        }


    }

    public static void adjustPrice(Block block, boolean afterBuying) {
        try {
            Random r = new Random();
            float cprice;
            if (afterBuying) {
                cprice = getPrice(block, false) / 64.0F;
                cprice += r.nextFloat() / 100.0F;
                if ((double)cprice > 0.99D) {
                    cprice = 0.99F;
                }

                setPrice(block, cprice);
            } else {
                cprice = getPrice(block, false) / 64.0F;
                cprice -= r.nextFloat() / 100.0F;
                if ((double)cprice < 0.012D) {
                    cprice = 0.012F;
                }

                setPrice(block, cprice);
            }

            if (block == Blocks.PLANKS) {
                setPrice(Blocks.LOG, cprice * 4.0F);
            }

            if (block == Blocks.LOG) {
                setPrice(Blocks.PLANKS, cprice / 4.0F);
            }
        } catch (Exception e) {
            StackTraceElement element=e.getStackTrace()[0];ModSimLoader.log.error("adjustPrice出错了：" + e.getMessage()+"行数："+element.getLineNumber());
        }


    }

    public static String formatPrice(float price) {
        String formatPrice="";
        try {
            NumberFormat formatter = new DecimalFormat("#0.00");
            formatPrice=formatter.format((double)price);
        } catch (Exception e) {
            StackTraceElement element=e.getStackTrace()[0];
            ModSimLoader.log.error("formatPrice出错了：" + e.getMessage()+"行数："+element.getLineNumber());
        }
        return formatPrice;
    }
}