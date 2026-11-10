package com.trhsy.sim.loader;

import com.trhsy.sim.ModSim;
import com.trhsy.sim.block.*;
import com.trhsy.sim.block.enums.EnumControlBox;
import com.trhsy.sim.block.enums.EnumLightColour;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemMultiTexture;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fluids.BlockFluidClassic;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.loader
 * @ClassName: BlockLoader
 * @Description: 方块注册加载类
 * @date 2023/10/31 上午 10:13
 */
@Mod.EventBusSubscriber
public class BlockLoader {

    private static final Logger LOGGER = LogManager.getLogger(BlockLoader.class);

    /**
     * 建筑箱
     */
    public static Block blockConstructorBox=new BlockConstructorBox();
    /**农田箱**/
    public static BlockFarmingBox blockFarmingBox = new BlockFarmingBox();
    /**挖矿箱**/
    public static BlockMiningBox blockMiningBox = new BlockMiningBox();
    /**
     * 奶酪块
     */
    public static BlockCheese blockCheese=new BlockCheese();

    /**控制箱***/
    public static BlockControlBox blockControlBox=new BlockControlBox();

    /**灯箱**/
    public static BlockLightBox blockLightBox=new BlockLightBox();
    /**标记棒**/
    public static BlockMarker blockMarker=new BlockMarker();

    /**
     * 静态牛奶块
     */
    public static BlockFluidClassic milk=new BlockMilk();
    /**特除方块**/
    public static BlockSpecial blockSpecial=new BlockSpecial();
    /**元数据**/
    private static final Map<Block, Integer> BLOCK_METADATA_MAP = new HashMap<>();
    //模型后缀
    private static final Map<Block, String[]> BLOCK_MODEL_SUFFIX_MAP = new HashMap<>();

    static {
        BLOCK_METADATA_MAP.put(blockControlBox, 3);
        BLOCK_MODEL_SUFFIX_MAP.put(blockControlBox, new String[]{"", "_atm", "_other"});
        BLOCK_METADATA_MAP.put(blockLightBox, 8);
    }
    /**
     * 注册方块
     * @param event
     */
    @SubscribeEvent
    public static void registerBlock(RegistryEvent.Register<Block> event){
        //注册方块
        /*
        event.getRegistry().register(blockConstructorBox.setRegistryName(ModSim.MODID+":block_constructor_box"));

        event.getRegistry().register(blockFarmingBox.setRegistryName(ModSim.MODID+":block_farming_box"));

        event.getRegistry().register(blockMiningBox.setRegistryName(ModSim.MODID+":block_mining_box"));

        event.getRegistry().register(blockMarker.setRegistryName(ModSim.MODID+":block_marker"));

        event.getRegistry().register(blockWindmill.setRegistryName(ModSim.MODID+":block_windmill").setUnlocalizedName("windmill").setCreativeTab(CreativeTabsLoader.tabSimU));
        event.getRegistry().register(litBlockWindmill.setRegistryName(ModSim.MODID+":lit_block_windmill").setUnlocalizedName("windmill"));

        event.getRegistry().register(milk.setRegistryName(ModSim.MODID+":milk"));

        event.getRegistry().register(blockCompositeBrick.setRegistryName(ModSim.MODID+":block_composite_brick"));

        event.getRegistry().register(blockCheese.setRegistryName(ModSim.MODID+":block_cheese"));

        event.getRegistry().register(blockControlBox.setRegistryName(ModSim.MODID+":block_control_box"));

        event.getRegistry().register(blockCopper.setRegistryName(ModSim.MODID+":block_copper"));

        event.getRegistry().register(blockCopperOre.setRegistryName(ModSim.MODID+":block_copper_ore"));

        event.getRegistry().register(blockTin.setRegistryName(ModSim.MODID+":block_tin"));

        event.getRegistry().register(blockTinOre.setRegistryName(ModSim.MODID+":block_tin_ore"));

        event.getRegistry().register(blockLightBox.setRegistryName(ModSim.MODID+":block_light_box"));

        event.getRegistry().register(blockLiving.setRegistryName(ModSim.MODID+":block_living"));

        event.getRegistry().register(blockSpecial.setRegistryName(ModSim.MODID+":block_special"));
*/
        //建筑箱
        registerBlock(event, blockConstructorBox, "block_constructor_box");
        //农田箱
        registerBlock(event, blockFarmingBox, "block_farming_box");
        //采矿箱
        registerBlock(event, blockMiningBox, "block_mining_box");
        //标记棒
        registerBlock(event, blockMarker, "block_marker");
        //牛奶
        registerBlock(event, milk, "milk");
        //奶酪块
        registerBlock(event, blockCheese, "block_cheese");
        //控制箱
        registerBlock(event, blockControlBox, "block_control_box");
        //灯箱
        registerBlock(event, blockLightBox, "block_light_box");
        //特除方块
        registerBlock(event, blockSpecial, "block_special");

    }
    private static void registerBlock(RegistryEvent.Register<Block> event, Block block, String registryName) {
        try {
            Block registeredBlock = block.setRegistryName(ModSim.MODID + ":" + registryName);
            registeredBlock.setCreativeTab((net.minecraft.creativetab.CreativeTabs) CreativeTabsLoader.tabSimU);
            event.getRegistry().register(registeredBlock);
        } catch (Exception e) {
            LOGGER.error("注册方块失败 {}: {}", registryName, e.getMessage());
        }
    }

    private static void registerBlock(RegistryEvent.Register<Block> event, Block block, String registryName, String unlocalizedName, Object creativeTab) {
        try {
            Block registeredBlock = block.setRegistryName(ModSim.MODID + ":" + registryName);
            if (unlocalizedName != null) {
                registeredBlock.setUnlocalizedName(unlocalizedName);
            }
            if (creativeTab != null) {
                registeredBlock.setCreativeTab((net.minecraft.creativetab.CreativeTabs) creativeTab);
            }
            event.getRegistry().register(registeredBlock);
        } catch (Exception e) {
            LOGGER.error("注册方块失败 {}: {}", registryName, e.getMessage());
        }
    }
    /**
     * 同时注册为物品
     * @param event
     */
    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event){
        //注册为物品
        /*
        event.getRegistry().register(new ItemBlock(blockConstructorBox).setRegistryName(ModSim.MODID+":block_constructor_box"));

        event.getRegistry().register(new ItemBlock(blockFarmingBox).setRegistryName(ModSim.MODID+":block_farming_box"));


        event.getRegistry().register(new ItemBlock(blockMiningBox).setRegistryName(ModSim.MODID+":block_mining_box"));

        Item blockControlBoxItem=new ItemMultiTexture(blockControlBox,blockControlBox,new ItemMultiTexture.Mapper(){
            @Override
            public String apply(ItemStack var1) {
                return EnumControlBox.byMetadata(var1.getMetadata()).getUnlocalizedName();
            }
        });
        event.getRegistry().register(blockControlBoxItem.setRegistryName(ModSim.MODID+":block_control_box"));

        event.getRegistry().register(new ItemBlock(blockMarker).setRegistryName(ModSim.MODID+":block_marker"));

        event.getRegistry().register(new ItemBlock(blockWindmill).setRegistryName(ModSim.MODID+":block_windmill"));
        event.getRegistry().register(new ItemBlock(litBlockWindmill).setRegistryName(ModSim.MODID+":lit_block_windmill"));

        event.getRegistry().register(new ItemBlock(blockCompositeBrick).setRegistryName(ModSim.MODID+":block_composite_brick"));

        event.getRegistry().register(new ItemBlock(milk).setRegistryName(ModSim.MODID+":milk"));

        event.getRegistry().register(new ItemBlock(blockCheese).setRegistryName(ModSim.MODID+":block_cheese"));


        event.getRegistry().register(new ItemBlock(blockCopper).setRegistryName(ModSim.MODID+":block_copper"));

        event.getRegistry().register(new ItemBlock(blockCopperOre).setRegistryName(ModSim.MODID+":block_copper_ore"));


        event.getRegistry().register(new ItemBlock(blockTin).setRegistryName(ModSim.MODID+":block_tin"));

        event.getRegistry().register(new ItemBlock(blockTinOre).setRegistryName(ModSim.MODID+":block_tin_ore"));


        Item blockLightBoxItem=new ItemMultiTexture(blockLightBox,blockLightBox,new ItemMultiTexture.Mapper(){
            @Override
            public String apply(ItemStack var1) {
                return EnumLightColour.byMetadata(var1.getMetadata()).getUnlocalizedName();
            }
        });
        event.getRegistry().register(blockLightBoxItem.setRegistryName(ModSim.MODID+":block_light_box"));

        Item blockLivingItem=new ItemMultiTexture(blockLiving,blockLiving,new ItemMultiTexture.Mapper(){
            @Override
            public String apply(ItemStack var1) {
                return EnumBlockLiving.byMetadata(var1.getMetadata()).getUnlocalizedName();
            }
        });
        event.getRegistry().register(blockLivingItem.setRegistryName(ModSim.MODID+":block_living"));

        event.getRegistry().register(new ItemBlock(blockSpecial).setRegistryName(ModSim.MODID+":block_special"));*/

//建筑箱
        registerItem(event, blockConstructorBox, "block_constructor_box");
        //农田箱
        registerItem(event, blockFarmingBox, "block_farming_box");
        //采矿箱
        registerItem(event, blockMiningBox, "block_mining_box");
        //标记棒
        registerItem(event, blockMarker, "block_marker");
        //牛奶
        registerItem(event, milk, "milk");
        //奶酪块
        registerItem(event, blockCheese, "block_cheese");
        //特制方块空气
        registerItem(event, blockSpecial, "block_special");
//控制箱
        registerMultiTextureItem(event, blockControlBox, "block_control_box", new ItemMultiTexture.Mapper() {
            @Override
            public String apply(ItemStack var1) {
                return EnumControlBox.byMetadata(var1.getMetadata()).getUnlocalizedName();
            }
        });
        //灯箱
        registerMultiTextureItem(event, blockLightBox, "block_light_box", new ItemMultiTexture.Mapper() {
            @Override
            public String apply(ItemStack var1) {
                return EnumLightColour.byMetadata(var1.getMetadata()).getUnlocalizedName();
            }
        });
    }
    private static void registerItem(RegistryEvent.Register<Item> event, Block block, String registryName) {
        try {
            event.getRegistry().register(new ItemBlock(block).setRegistryName(ModSim.MODID + ":" + registryName));
        } catch (Exception e) {
            LOGGER.error("注册物品失败 {}: {}", registryName, e.getMessage());
        }
    }

    private static void registerMultiTextureItem(RegistryEvent.Register<Item> event, Block block, String registryName, ItemMultiTexture.Mapper mapper) {
        try {
            Item item = new ItemMultiTexture(block, block, mapper);
            event.getRegistry().register(item.setRegistryName(ModSim.MODID + ":" + registryName));
        } catch (Exception e) {
            LOGGER.error("注册物品失败 {}: {}", registryName, e.getMessage());
        }
    }
    /**
     * 注册材质
     * @param event
     */
    @SubscribeEvent
    public static void registerItemBlockModel(ModelRegistryEvent event){
        //注册材质
//建筑箱
        registerModel(Item.getItemFromBlock(blockConstructorBox), "block_constructor_box");
        //农田箱
        registerModel(Item.getItemFromBlock(blockFarmingBox), "block_farming_box");
        //采矿箱
        registerModel(Item.getItemFromBlock(blockMiningBox), "block_mining_box");
        //标记棒
        registerModel(Item.getItemFromBlock(blockMarker), "block_marker");
        //牛奶
        registerModel(Item.getItemFromBlock(milk), "milk");
        //奶酪块
        registerModel(Item.getItemFromBlock(blockCheese), "block_cheese");
        //特除方块
        registerModel(Item.getItemFromBlock(blockSpecial), "block_special");
//控制箱 三个元素
        registerMultiModel(Item.getItemFromBlock(blockControlBox), "block_control_box", BLOCK_MODEL_SUFFIX_MAP.get(blockControlBox));
        //灯箱
        registerMultiModel(Item.getItemFromBlock(blockLightBox), "block_light_box", BLOCK_METADATA_MAP.get(blockLightBox));

        /*
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(blockConstructorBox),0,new ModelResourceLocation(blockConstructorBox.getRegistryName(),"inventory"));

        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(blockFarmingBox),0,new ModelResourceLocation(blockFarmingBox.getRegistryName(),"inventory"));

        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(blockMiningBox),0,new ModelResourceLocation(blockMiningBox.getRegistryName(),"inventory"));

        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(blockMarker),0,new ModelResourceLocation(blockMarker.getRegistryName(),"inventory"));

        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(milk),0,new ModelResourceLocation(milk.getRegistryName(),"inventory"));

        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(blockCompositeBrick),0,new ModelResourceLocation(blockCompositeBrick.getRegistryName(),"inventory"));

        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(blockCheese),0,new ModelResourceLocation(blockCheese.getRegistryName(),"inventory"));

        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(blockControlBox),0,new ModelResourceLocation(blockControlBox.getRegistryName(),"inventory"));
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(blockControlBox),1,new ModelResourceLocation(blockControlBox.getRegistryName()+"_atm","inventory"));
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(blockControlBox),2,new ModelResourceLocation(blockControlBox.getRegistryName()+"_other","inventory"));

        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(blockCopper),0,new ModelResourceLocation(blockCopper.getRegistryName(),"inventory"));

        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(blockCopperOre),0,new ModelResourceLocation(blockCopperOre.getRegistryName(),"inventory"));

        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(blockTin),0,new ModelResourceLocation(blockTin.getRegistryName(),"inventory"));

        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(blockTinOre),0,new ModelResourceLocation(blockTinOre.getRegistryName(),"inventory"));

        for (int i = 0; i < 8; i++) {
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(blockLightBox),i,new ModelResourceLocation(blockLightBox.getRegistryName()+""+i,"inventory"));
        }


        for (int i = 0; i < 16; i++) {
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(blockLiving),i,new ModelResourceLocation(blockLiving.getRegistryName()+""+i,"inventory"));
        }

        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(blockSpecial),0,new ModelResourceLocation(blockSpecial.getRegistryName(),"inventory"));

        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(blockWindmill),0,new ModelResourceLocation(blockWindmill.getRegistryName(),"inventory"));
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(litBlockWindmill),0,new ModelResourceLocation(litBlockWindmill.getRegistryName(),"inventory"));*/

    }

    private static void registerModel(Item item, String registryName) {
        try {
            ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(ModSim.MODID + ":" + registryName, "inventory"));
        } catch (Exception e) {
            LOGGER.error("未能注册物品的模型 {}: {}", registryName, e.getMessage());
        }
    }

    private static void registerMultiModel(Item item, String registryName, String[] suffixes) {
        try {
            for (int i = 0; i < suffixes.length; i++) {
                ModelLoader.setCustomModelResourceLocation(item, i, new ModelResourceLocation(ModSim.MODID + ":" + registryName + suffixes[i], "inventory"));
            }
        } catch (Exception e) {
            LOGGER.error("无法为物品注册多个模型 {}: {}", registryName, e.getMessage());
        }
    }

    private static void registerMultiModel(Item item, String registryName, int metadataCount) {
        try {
            for (int i = 0; i < metadataCount; i++) {
                ModelLoader.setCustomModelResourceLocation(item, i, new ModelResourceLocation(ModSim.MODID + ":" + registryName + i, "inventory"));
            }
        } catch (Exception e) {
            LOGGER.error("无法为物品注册多个模型 {}: {}", registryName, e.getMessage());
        }
    }
}
