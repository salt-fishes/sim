package com.trhsy.sim.util.items;

import com.trhsy.sim.loader.ItemLoader;
import com.trhsy.sim.loader.ModSimLoader;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @ClassName CommodityAtm
 * @Description todo
 * @Author TRHSY
 * @Date 2023/7/2320:15
 **/
public class CommodityAtm extends Commodity{
    /**商品**/
    public ItemStack theItemStack;
    /**数量**/
    public int quantity = 0;
    /**单价**/
    public float priceEach = 0.0F;
    /**可用商品**/
    private static List<ItemStack> availableItems = new CopyOnWriteArrayList();
    /*
    银行目前正在销售的商品列表，每天早上都会更新新商品
     */
    public static List<CommodityAtm> theCommodities = new CopyOnWriteArrayList();
    public CommodityAtm(ItemStack is, int qty, float price) {
        super(is, qty, price);
        try{
            this.theItemStack = is;
            this.quantity = qty;
            this.priceEach = price;
        } catch (Exception e) {
            StackTraceElement element=e.getStackTrace()[0];
            ModSimLoader.log.error("CommodityAtm出错了：" + e.getMessage()+"行数："+element.getLineNumber());
        }

    }
    /**
     * @Author fan
     * @Description //TODO 刷新可用商品
     * @Date 23:19 2023/7/12
     * @Param []
     * @return void
     **/
    public static void refreshAvailableCommoditities() {
        try {
            if (availableItems.size() == 0) {
                setupAvailableItems();
            }

            Random rand = new Random();
            theCommodities.clear();
            int count = rand.nextInt(7) + 2;

            for(int it = 0; it < count; it++) {
                int index = rand.nextInt(availableItems.size() - 1);
                int qty = rand.nextInt(10) + 1;
                float price = 300.0F + (float) rand.nextInt(300) + rand.nextFloat() * 100.0F;
                boolean gotIt = false;

                for (int shit = 0; shit < theCommodities.size(); ++shit) {
                    Commodity cshit = theCommodities.get(shit);
                    if (cshit.theItemStack.getDisplayName().contentEquals(((ItemStack) availableItems.get(index)).getDisplayName())) {
                        gotIt = true;
                        break;
                    }
                }

                if (!gotIt) {
                    theCommodities.add(new CommodityAtm((ItemStack) availableItems.get(index), qty, price));
                }
            }
        } catch (Exception e) {
            StackTraceElement element=e.getStackTrace()[0];ModSimLoader.log.error("refreshAvailableCommoditities出错了：" + e.getMessage()+"行数："+element.getLineNumber());
        }
    }
    private static void setupAvailableItems() {
        try {
            availableItems.clear();
            //钻石
            availableItems.add(new ItemStack(Items.DIAMOND));
            //红石
            availableItems.add(new ItemStack(Items.REDSTONE));
            //绿宝石
            availableItems.add(new ItemStack(Items.EMERALD));
            //青金石
            availableItems.add(new ItemStack(Items.DYE));
            //铁
            availableItems.add(new ItemStack(Items.IRON_INGOT));
            //金
            availableItems.add(new ItemStack(Items.GOLD_INGOT));
            //萤石
            availableItems.add(new ItemStack(Items.GLOWSTONE_DUST));
        } catch (Exception e) {
            StackTraceElement element=e.getStackTrace()[0];ModSimLoader.log.error("setupAvailableItems出错了：" + e.getMessage()+"行数："+element.getLineNumber());
        }

    }
}
