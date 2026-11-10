package com.trhsy.sim.loader;

import com.trhsy.sim.ModSim;
import com.trhsy.sim.item.ItemBucketMilk;
import com.trhsy.sim.item.*;
import com.trhsy.sim.item.food.*;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.loader
 * @ClassName: ItemLoader
 * @Description: 物品加载注册类
 * @date 2023/10/31 上午 10:13
 */
@Mod.EventBusSubscriber
public class ItemLoader {
    /**
     * 桶装牛奶
     */
    public static Item itemBucketMilk = new ItemBucketMilk();
    /**
     * 啤酒瓶
     **/
    public static Item itemDrinkEmpty = new ItemDrinkEmpty();
    /**
     * 模拟城市启动卷轴
     **/
    public static Item itemSimULoader = new ItemSimULoader();

    /**
     * 兼容别名：铜/锡工具和护甲已删除，统一使用原版铁制装备。
     * 这样职业代码里的手持物/装备引用不需要全部重写，同时不会再注册任何铜/锡物品。
     */
    public static Item tinPickaxe = Items.IRON_PICKAXE;
    public static Item tinHoe = Items.IRON_HOE;
    public static Item tinAxe = Items.IRON_AXE;
    public static Item tinSpade = Items.IRON_SHOVEL;
    public static Item tinSword = Items.IRON_SWORD;
    public static Item tinHelmet = Items.IRON_HELMET;
    public static Item tinChestplate = Items.IRON_CHESTPLATE;
    public static Item tinLeggings = Items.IRON_LEGGINGS;
    public static Item tinBoots = Items.IRON_BOOTS;

    public static Item copperAxe = Items.IRON_AXE;
    public static Item copperPickaxe = Items.IRON_PICKAXE;
    public static Item copperHoe = Items.IRON_HOE;
    public static Item copperSpade = Items.IRON_SHOVEL;
    public static Item copperSword = Items.IRON_SWORD;
    public static Item copperHelmet = Items.IRON_HELMET;
    public static Item copperChestplate = Items.IRON_CHESTPLATE;
    public static Item copperLeggings = Items.IRON_LEGGINGS;
    public static Item copperBoots = Items.IRON_BOOTS;

    /**汉堡**/
    public static ItemFood itemBurger = new ItemBurger();
    /**薯条**/
    public static ItemFood itemFries = new ItemFries();
    /**奶酪**/
    public static ItemFood itemCheese = new ItemCheese();
    /**奶酪汉堡**/
    public static ItemFood itemCheeseburger = new ItemCheeseburger();

    /**
     * 啤酒
     */
    public static Item itemDrink = new ItemDrink();

    /**
     * 注册物品
     *
     * @param event
     */
    @SubscribeEvent
    public static void registerItem(RegistryEvent.Register<Item> event) {
        //桶装牛奶
        event.getRegistry().register(itemBucketMilk.setRegistryName(ModSim.MODID + ":item_bucket_milk"));
        //啤酒瓶
        event.getRegistry().register(itemDrinkEmpty.setRegistryName(ModSim.MODID + ":beer_bottle"));
        //模拟城市启动卷轴
        event.getRegistry().register(itemSimULoader.setRegistryName(ModSim.MODID + ":item_sim_u_loader"));
        //汉堡
        event.getRegistry().register(itemBurger.setRegistryName(ModSim.MODID + ":food_burger"));
        //薯条
        event.getRegistry().register(itemFries.setRegistryName(ModSim.MODID + ":food_fries"));
        //奶酪
        event.getRegistry().register(itemCheese.setRegistryName(ModSim.MODID + ":food_cheese"));
        //奶酪汉堡
        event.getRegistry().register(itemCheeseburger.setRegistryName(ModSim.MODID + ":food_cheese_burger"));
        //啤酒
        event.getRegistry().register(itemDrink.setRegistryName(ModSim.MODID + ":drinks"));
    }

    /**
     * 给物品加上模型材质
     *
     * @param event
     */
    @SubscribeEvent
    public static void registerItemModel(ModelRegistryEvent event) {
        //桶装牛奶
        ModelLoader.setCustomModelResourceLocation(itemBucketMilk, 0, new ModelResourceLocation(itemBucketMilk.getRegistryName(), "inventory"));
        //啤酒瓶
        ModelLoader.setCustomModelResourceLocation(itemDrinkEmpty, 0, new ModelResourceLocation(itemDrinkEmpty.getRegistryName(), "inventory"));
        //模拟城市启动卷轴
        ModelLoader.setCustomModelResourceLocation(itemSimULoader, 0, new ModelResourceLocation(itemSimULoader.getRegistryName(), "inventory"));
        //汉堡
        ModelLoader.setCustomModelResourceLocation(itemBurger, 0, new ModelResourceLocation(itemBurger.getRegistryName(), "inventory"));
        //薯条
        ModelLoader.setCustomModelResourceLocation(itemFries, 0, new ModelResourceLocation(itemFries.getRegistryName(), "inventory"));
        //奶酪
        ModelLoader.setCustomModelResourceLocation(itemCheese, 0, new ModelResourceLocation(itemCheese.getRegistryName(), "inventory"));
        //奶酪汉堡
        ModelLoader.setCustomModelResourceLocation(itemCheeseburger, 0, new ModelResourceLocation(itemCheeseburger.getRegistryName(), "inventory"));
        //啤酒
        ModelLoader.setCustomModelResourceLocation(itemDrink, 0, new ModelResourceLocation(itemDrink.getRegistryName(), "inventory"));
    }
}
