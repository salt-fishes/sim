package com.trhsy.sim.npcCode.logistics;

import com.trhsy.sim.loader.ItemLoader;
import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.V3;
import com.trhsy.sim.npcCode.block.FarmBox;
import com.trhsy.sim.npcCode.build.Building;
import com.trhsy.sim.npcCode.job.Job;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 供货目录（Beta12 物流中心）。
 *
 * <p>从旧的 {@code JobTaskCollectItems} 抽取出来的“物品 -> 供货职业/农场类型”映射。
 * 快递员和店铺共用这一份映射，避免删除老板自取任务后不知道去哪里取货。</p>
 *
 * <p>这里只负责“找得到哪些供货点、有没有库存”，不负责扣款、订单状态或运输。</p>
 */
public final class SupplyCatalog {

    private SupplyCatalog() {
    }

    /**
     * 返回物品对应的供货职业显示名；农场作物返回 {@code "farmer:<农场类型>"}。
     * 没有映射时返回 {@code null}。
     */
    public static String supplierFor(ItemStack wanted) {
        if (wanted == null || wanted.isEmpty()) {
            return null;
        }
        Item item = wanted.getItem();
        //猪排
        if (item == Items.PORKCHOP) {
            //养猪场
            return new TextComponentTranslation("container.sim.Vocation13", new Object[0]).getUnformattedText();
        }
        //兔肉
        if (item == Items.RABBIT) {
            //养兔场
            return new TextComponentTranslation("container.sim.Vocation29", new Object[0]).getUnformattedText();
        }
        //牛肉
        if (item == Items.BEEF) {
            //养牛场
            return new TextComponentTranslation("container.sim.Vocation12", new Object[0]).getUnformattedText();
        }
        //鸡肉
        if (item == Items.CHICKEN) {
            //养鸡场
            return new TextComponentTranslation("container.sim.Vocation14", new Object[0]).getUnformattedText();
        }
        //羊肉
        if (item == Items.MUTTON) {
            //养羊场
            return new TextComponentTranslation("container.sim.Vocation28", new Object[0]).getUnformattedText();
        }
        //鸡蛋
        if (item == Items.EGG) {
            //鸡蛋农场
            return new TextComponentTranslation("container.sim.Vocation3", new Object[0]).getUnformattedText();
        }
        //牛奶
        if (item == ItemLoader.itemBucketMilk) {
            //奶牛场
            return new TextComponentTranslation("container.sim.Vocation20", new Object[0]).getUnformattedText();
        }
        //鱼
        if (item == Items.FISH) {
            //渔场
            return new TextComponentTranslation("container.sim.Vocation18", new Object[0]).getUnformattedText();
        }
        //胡萝卜
        if (item == Items.CARROT) {
            return "farmer:" + new TextComponentTranslation("container.sim.FarmType1", new Object[0]).getUnformattedText();
        }
        //马铃薯
        if (item == Items.POTATO) {
            return "farmer:" + new TextComponentTranslation("container.sim.FarmType3", new Object[0]).getUnformattedText();
        }
        //甜菜根
        if (item == Items.BEETROOT) {
            return "farmer:" + new TextComponentTranslation("container.sim.FarmType6", new Object[0]).getUnformattedText();
        }
        //可可豆
        if (item == Items.DYE) {
            return "farmer:" + new TextComponentTranslation("container.sim.FarmType10", new Object[0]).getUnformattedText();
        }
        //西瓜
        if (item == Items.MELON) {
            return "farmer:" + new TextComponentTranslation("container.sim.FarmType2", new Object[0]).getUnformattedText();
        }
        //小麦
        if (item == Items.WHEAT) {
            return "farmer:" + new TextComponentTranslation("container.sim.FarmType5", new Object[0]).getUnformattedText();
        }
        //甘蔗
        if (item == Items.REEDS) {
            return "farmer:" + new TextComponentTranslation("container.sim.FarmType7", new Object[0]).getUnformattedText();
        }
        //南瓜
        if (item == new ItemStack(Blocks.PUMPKIN).getItem()) {
            return "farmer:" + new TextComponentTranslation("container.sim.FarmType4", new Object[0]).getUnformattedText();
        }
        //糖
        if (item == Items.SUGAR) {
            return new TextComponentTranslation("container.sim.Vocation30", new Object[0]).getUnformattedText();
        }
        //奶酪：用稳定职业 ID（奶酪工坊 Vocation21）匹配建筑。
        if (item == ItemLoader.itemCheese) {
            return new TextComponentTranslation("container.sim.Vocation21", new Object[0]).getUnformattedText();
        }
        //面包
        if (item == Items.BREAD) {
            return new TextComponentTranslation("container.sim.Vocation6", new Object[0]).getUnformattedText();
        }
        return null;
    }

    /** 该物品是否有自动供货映射。 */
    public static boolean isSupported(ItemStack wanted) {
        return supplierFor(wanted) != null;
    }

    /**
     * 旧存档里的 {@code LogisticsPoint} 可能记录了商店/餐厅的货架。
     * 只有这些生产职业的产出点才允许快递员自动收货。
     */
    public static boolean isProductionLabel(String label) {
        if (label == null || label.isEmpty()) {
            return false;
        }
        String[] production = {
                "container.sim.Vocation6",   // 面包师
                "container.sim.Vocation21",  // 奶酪匠
                "container.sim.Vocation30",  // 制糖师
                "container.sim.Vocation17",  // 玻璃匠
                "container.sim.Vocation25"   // 板砖匠
        };
        for (String key : production) {
            if (label.equals(new TextComponentTranslation(key, new Object[0]).getUnformattedText())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 查找有库存的供货点。
     *
     * @param context   用于扫描箱子的职业上下文（通常为快递员或店铺）
     * @param wanted    需要的物品
     * @param maxPoints 最多返回几个供货点
     */
    public static List<V3> findSources(Job context, ItemStack wanted, int maxPoints) {
        List<V3> result = new ArrayList<>();
        if (context == null || wanted == null || wanted.isEmpty()) {
            return result;
        }
        String supplier = supplierFor(wanted);
        if (supplier == null) {
            return result;
        }
        int limit = Math.max(1, maxPoints);
        if (supplier.startsWith("farmer:")) {
            String farmType = supplier.substring("farmer:".length());
            List<FarmBox> farms = ModSimLoader.getClosestFarm(context.workPlace, farmType);
            for (FarmBox farm : farms) {
                if (farm == null || farm.loc == null || result.size() >= limit) {
                    continue;
                }
                V3 dest = new V3(farm.loc.x, farm.loc.y + 1, farm.loc.z, farm.loc.dimension);
                if (hasStockAt(context, dest, wanted, 5)) {
                    addUnique(result, dest);
                }
            }
        } else {
            List<Building> buildings = ModSimLoader.getClosestBuildingByJob(supplier, context.workPlace);
            if (buildings != null) {
                for (Building building : buildings) {
                    if (building == null || building.controlXYZ == null || result.size() >= limit) {
                        continue;
                    }
                    V3 dest = new V3(building.controlXYZ.x, building.controlXYZ.y + 1,
                            building.controlXYZ.z, building.dimension);
                    if (hasStockAt(context, dest, wanted, 5)) {
                        addUnique(result, dest);
                    }
                }
            }
        }
        return result;
    }

    /** 检查目标点附近的箱子里是否有指定物品。 */
    public static boolean hasStockAt(Job context, V3 dest, ItemStack wanted, int radius) {
        if (context == null || dest == null || wanted == null || wanted.isEmpty()) {
            return false;
        }
        List<IInventory> chests = context.inventoriesFindClosest(dest, radius);
        for (IInventory inv : chests) {
            if (inv == null) {
                continue;
            }
            for (int slot = 0; slot < inv.getSizeInventory(); slot++) {
                ItemStack stack = inv.getStackInSlot(slot);
                if (stack != null && !stack.isEmpty() && stack.isItemEqual(wanted)
                        && ItemStack.areItemStackTagsEqual(stack, wanted)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 统计目标点附近箱子里的指定物品数量。 */
    public static int countStockAt(Job context, V3 dest, ItemStack wanted, int radius) {
        int count = 0;
        if (context == null || dest == null || wanted == null || wanted.isEmpty()) {
            return 0;
        }
        for (IInventory inv : context.inventoriesFindClosest(dest, radius)) {
            if (inv == null) {
                continue;
            }
            for (int slot = 0; slot < inv.getSizeInventory(); slot++) {
                ItemStack stack = inv.getStackInSlot(slot);
                if (stack != null && !stack.isEmpty() && stack.isItemEqual(wanted)
                        && ItemStack.areItemStackTagsEqual(stack, wanted)) {
                    count += stack.getCount();
                }
            }
        }
        return count;
    }

    private static void addUnique(List<V3> result, V3 value) {
        for (V3 existing : result) {
            if (existing != null && existing.equals(value)) {
                return;
            }
        }
        result.add(value);
    }

    /**
     * 按物品类型汇总某个箱子列表里的库存，键为 {@link LogisticsService#itemKey(ItemStack)}。
     */
    public static Map<String, Integer> countByKey(List<IInventory> inventories) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (inventories == null) {
            return result;
        }
        for (IInventory inv : inventories) {
            if (inv == null) {
                continue;
            }
            for (int slot = 0; slot < inv.getSizeInventory(); slot++) {
                ItemStack stack = inv.getStackInSlot(slot);
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                String key = LogisticsService.itemKey(stack);
                Integer current = result.get(key);
                result.put(key, (current == null ? 0 : current) + stack.getCount());
            }
        }
        return result;
    }

    /** 兼容旧调用：把 IInventory 转成 Forge 物品能力。 */
    public static IItemHandler handlerFor(IInventory inventory) {
        if (inventory == null) {
            return null;
        }
        if (inventory instanceof IItemHandler) {
            return (IItemHandler) inventory;
        }
        return new InvWrapper(inventory);
    }
}
