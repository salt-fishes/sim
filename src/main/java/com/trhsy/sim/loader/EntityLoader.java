package com.trhsy.sim.loader;

import com.trhsy.sim.ModSim;
import com.trhsy.sim.entity.EntityConBox;
import com.trhsy.sim.entity.EntityDinkEmpty;
import com.trhsy.sim.entity.EntityNpc;
import com.trhsy.sim.loader.render.RenderConBox;
import com.trhsy.sim.loader.render.RenderEntityFolk;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.loader
 * @ClassName: EntityLoader
 * @Description:
 * @date 2023/11/20 下午 2:54
 */
public class EntityLoader {
    private static int nextID = 0;

    /**
     * 加载实体
     */
    public static void registerEntitys() {
        /**
         * entityClass–实体类
         * entityName–实体的唯一名称
         * id–实体的特定于国防部的id
         * mod–mod
         * trackingRange–MC发送跟踪更新的范围
         * updateFrequency–跟踪更新的频率
         * sendsVelocityUpdates–是否也发送速度信息包
         */
        
        EntityRegistry.registerModEntity(new ResourceLocation(ModSim.MODID + ":folk"), EntityNpc.class, "EntityFolk", nextID++, ModSim.instance, 64, 1, true);
        EntityRegistry.registerModEntity(new ResourceLocation(ModSim.MODID + ":ConBox"), EntityConBox.class, "ConBox", nextID++, ModSim.instance, 64, 3, false);
        EntityRegistry.registerModEntity(new ResourceLocation(ModSim.MODID + ":DinkEmpty"), EntityDinkEmpty.class, "DinkEmpty", nextID++, ModSim.instance, 64, 3, false);
//        DataFixer datafixer = new DataFixer(1343);
//        datafixer = new net.minecraftforge.common.util.CompoundDataFixer(datafixer);

    }

    @SideOnly(Side.CLIENT)
    public static void initModels() {
        try {
            ModSimLoader.log.info("[渲染器注册] 开始注册 EntityNpc 渲染器...");
            // 注册时获取注册结果（Forge的注册方法无返回值，需通过日志间接验证）
            RenderingRegistry.registerEntityRenderingHandler(EntityNpc.class, RenderEntityFolk.FACTORY);
            ModSimLoader.log.info("[渲染器注册] EntityNpc 渲染器注册完成（使用 RenderEntityFolk）");
//            RenderingRegistry.registerEntityRenderingHandler(EntityNpc.class, RenderEntityFolk.FACTORY);
            RenderingRegistry.registerEntityRenderingHandler(EntityConBox.class, RenderConBox.FACTORY);

//            REGISTRY.putObject(new ResourceLocation("Windmill"), TileEntityWindmill.class);
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
//            ModSimLoader.log.error("registerRenders出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
            ModSimLoader.log.error("[渲染器注册] 失败！错误：" + e.getMessage() + "，行数：" + e.getStackTrace()[0].getLineNumber());
        }
    }
}
