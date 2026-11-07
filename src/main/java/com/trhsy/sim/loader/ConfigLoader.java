package com.trhsy.sim.loader;

import com.google.common.collect.Lists;
import com.trhsy.sim.ModSim;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.loader
 * @ClassName: ConfigLoader
 * @Description: 配置文件加载
 * @date 2023/11/09 下午 5:59
 */
public class ConfigLoader {
    /**高级配置功能，允许为配置变量提供各种类别**/
    public static ConfigLoader instance = new ConfigLoader();

    /**配置人口限制 0为不限制**/
    public static int configPopulationLimit = 0;

    /**配置木材面积**/
    public static int configLumberArea = 60;

    /**配置NPC谈话**/
    public static boolean configFolkTalking = true;

    /**配置养殖箱和建筑箱是否用钻石**/
    public static boolean configUseExpensiveRecipies = false;

    /**配置物料提醒间隔**/
    public static int configMaterialReminderInterval = 3;

    /**停止降雨**/
    public static boolean configStopRain = false;
    /**配置NPC说英文**/
    public static boolean configFolkTalkingEnglish = true;
    /**配置男性姓名**/
    public static String[] configMaleNames;
    /**配置女性名称**/
    public static String[] configFemaleNames;
    /**配置姓氏**/
    public static String[] configSurnames;

    public static Configuration configFile;
    public static ConfigCategory Gameplay;
    public static ConfigCategory Nameplay;
    private ConfigLoader() {
    }

    public static void load(FMLPreInitializationEvent event) {
        try {
            configFile = new Configuration(event.getSuggestedConfigurationFile(), "1.12.2-1.0.9 Beta6");
            syncConfig();
        } catch (Exception e) {
            StackTraceElement element=e.getStackTrace()[0];ModSimLoader.log.error("load出错了：" + e.getMessage()+"行数："+element.getLineNumber());
        }
    }

    @SubscribeEvent
    public void update(ConfigChangedEvent.OnConfigChangedEvent event) {
        try {
            if (event.getModID().equals(ModSim.MODID)) {
                syncConfig();
            }
        } catch (Exception e) {
            StackTraceElement element=e.getStackTrace()[0];ModSimLoader.log.error("update出错了：" + e.getMessage()+"行数："+element.getLineNumber());
        }
    }
    /**
     * @Author fan
     * @Description //TODO 同步配置
     * @Date 22:48 2022/5/11
     * @Param []
     * @return boolean
     **/
    /**
     * P11(S1): 配置分类使用固定英文 internal name；旧版本的分类名是本地化字符串，
     * 切换语言会生成新分类、丢失设置。这里在第一次加载时把旧分类迁移到固定分类。
     */
    private static ConfigCategory getOrMigrateCategory(String fixedName, String... probeKeys) {
        ConfigCategory fixed = configFile.getCategory(fixedName);
        if (!fixed.isEmpty()) {
            return fixed;
        }
        for (String name : configFile.getCategoryNames()) {
            if (name.equals(fixedName)) {
                continue;
            }
            ConfigCategory old = configFile.getCategory(name);
            if (old == null || old.isEmpty()) {
                continue;
            }
            for (String key : probeKeys) {
                if (old.containsKey(key)) {
                    fixed.putAll(old.getValues());
                    configFile.removeCategory(old);
                    configFile.save();
                    ModSimLoader.log.info("配置分类已迁移: {} -> {}", name, fixedName);
                    return fixed;
                }
            }
        }
        return fixed;
    }

    public static boolean syncConfig() {
        boolean changed = false;
        try {
            //模拟城市游戏配置（固定 internal name，避免语言切换生成新分类）
            String simConfig = "gameplay";
            Gameplay = getOrMigrateCategory("gameplay", "FolkTalking", "LumbermillArea");
            List<String> propOrder = Lists.newArrayList();
            //NPC是否能说话
            Property p = configFile.get(simConfig, "FolkTalking", configFolkTalking);
            //如果人们喋喋不休的谈话变得烦人，那么将其设置为false
            String s1=new TextComponentTranslation("container.sim.config_loader1",new Object[0]).getUnformattedText();
            p.setComment(s1);
            configFolkTalking = p.getBoolean();
            propOrder.add(p.getName());
            //伐木半径
            p = configFile.get(simConfig, "LumbermillArea", configLumberArea);
            //伐木工人从起点开始寻找树木的块半径，不要设置得太高，否则每次他们扫描最近的树时，MC都会减慢速度，所以30到1000应该是可以的（1000是1公里）
            String s3=new TextComponentTranslation("container.sim.config_loader3",new Object[0]).getUnformattedText();
            p.setComment(s3);
            configLumberArea = p.getInt();
            propOrder.add(p.getName());
            //人口限制
            p = configFile.get(simConfig, "PopulationLimit", configPopulationLimit);
            //如果你有一台旧电脑，限制人口增长超过这个数字
            String s4=new TextComponentTranslation("container.sim.config_loader4",new Object[0]).getUnformattedText();
            p.setComment(s4);
            configPopulationLimit = p.getInt();
            propOrder.add(p.getName());

            //
            p = configFile.get(simConfig, "UseExpensiveRecipes", configUseExpensiveRecipies);
            //如果你认为采矿/耕种的盒子太便宜/价格太高，那么将其设置为true，以使Recipes需要钻石工具而不是石头工具
            String s6=new TextComponentTranslation("container.sim.config_loader6",new Object[0]).getUnformattedText();
            p.setComment(s6);
            configUseExpensiveRecipies = p.getBoolean();
            propOrder.add(p.getName());
            //通知频率
            p = configFile.get(simConfig, "MaterialReminderInterval", configMaterialReminderInterval);
            //当建筑商用完材料时，他们会每3分钟通知你一次，设置为0表示不再提醒。
            String s7=new TextComponentTranslation("container.sim.config_loader7",new Object[0]).getUnformattedText();
            p.setComment(s7);
            configMaterialReminderInterval = p.getInt();
            // P11(S1): 0 表示不再提醒，不再强行改成 2000。
            propOrder.add(p.getName());

            //是否可以下雨
            p = configFile.get(simConfig, "StopRain", configStopRain);
            //这只是一个个人模式：-）如果你也发现你的世界里一直在下雨，这会让你感到烦恼/导致延迟，那么把这个设置为真，你只会有短暂的淋浴
            String s9=new TextComponentTranslation("container.sim.config_loader9",new Object[0]).getUnformattedText();
            p.setComment(s9);
            configStopRain = p.getBoolean();
            propOrder.add(p.getName());

            Gameplay.setPropertyOrder(propOrder);
            // 名字配置（固定 internal name）
            String npcNames = "names";
            propOrder = Lists.newArrayList();
            Nameplay = getOrMigrateCategory("names", "MaleNames", "FemaleNames");

            p = configFile.get(npcNames, "MaleNames", new TextComponentTranslation("container.sim.MaleNames",new Object[0]).getUnformattedText());
            String temp = p.getString();
            //这些是随机名字生成器使用的男性名字，请保持格式不变，否则会发生不好的事情。
            String s10=new TextComponentTranslation("container.sim.config_loader10",new Object[0]).getUnformattedText();
            p.setComment(s10);
            configMaleNames = temp.split(",");
            propOrder.add(p.getName());

            p = configFile.get(npcNames, "FemaleNames", new TextComponentTranslation("container.sim.FemaleNames",new Object[0]).getUnformattedText());
            temp = p.getString();
            //这些是随机名字生成器使用的女性名字，保持格式不变，否则会发生不好的事情。
            String s11=new TextComponentTranslation("container.sim.config_loader11",new Object[0]).getUnformattedText();
            p.setComment(s11);
            configFemaleNames = temp.split(",");
            propOrder.add(p.getName());

            p = configFile.get(npcNames, "LastNames", new TextComponentTranslation("container.sim.LastNames",new Object[0]).getUnformattedText());
            temp = p.getString();
            //这些是随机名称生成器使用的姓氏，请保持格式不变，否则会发生错误。
            String s12=new TextComponentTranslation("container.sim.config_loader12",new Object[0]).getUnformattedText();
            p.setComment(s12);
            configSurnames = temp.split(",");
            propOrder.add(p.getName());

            Nameplay.setPropertyOrder(propOrder);


            if (configFile.hasChanged()) {
                configFile.save();
                changed = true;
            }

        } catch (Exception e) {
            StackTraceElement element=e.getStackTrace()[0];ModSimLoader.log.error("syncConfig出错了：" + e.getMessage()+"行数："+element.getLineNumber());
        }
        return changed;
    }
}
