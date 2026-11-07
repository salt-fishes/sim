package com.trhsy.sim;

import com.trhsy.sim.config.SimConfigSync;
import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.loader.SoundRegistry;
import com.trhsy.sim.proxy.CommonProxy;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim
 * @ClassName: ModSim
 * @Description: 该类是模拟城镇模组的主类，负责模组的初始化、配置和事件处理。
 * @date 2023/10/19 下午 2:08
 */
@Mod(modid = ModSim.MODID, name = ModSim.NAME, useMetadata = true, version = ModSim.VERSION, acceptedMinecraftVersions = "1.12.2", guiFactory = "com.trhsy.sim.gui.ConfigGui$ConfigGuiFactory")
public class ModSim {
    /**
     * 模组id 指的就是该Mod的唯一标识符
     **/
    public static final String MODID = "sim";
    /**
     * 模组名称
     **/
    public static final String NAME = "Simulated town";
    /**
     * 模组版本 在Mod间的依赖关系时可能会用作识别
     **/
    public static final String VERSION = "1.12.2-1.0.9 Beta4";
    /**
     * 将生成该mod的实例注册到对应mod的id里面，也可以访问其他mod的，要注意这里的id和此mod的id相同
     */
    @Mod.Instance(ModSim.MODID)
    public static ModSim instance;
    @SidedProxy(
            clientSide = "com.trhsy.sim.proxy.ClientProxy",
            serverSide = "com.trhsy.sim.proxy.ServerProxy"
    )
    public static CommonProxy proxy;

    /**
     * 记录异常信息，包含完整的堆栈信息
     * @param methodName 发生异常的方法名
     * @param e 异常对象
     */
    private void logException(String methodName, Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String stackTrace = sw.toString();
        ModSimLoader.log.error(methodName + " 出错了：" + e.getMessage() + " 行数：" + e.getStackTrace()[0].getLineNumber() + "\n" + stackTrace);
    }
    /**
     * 创建带modid的资源位置
     */
    public static ResourceLocation getResourceLocation(String path) {
        return new ResourceLocation(MODID, path);
    }
    /**
     * 所有Mod初始化之前调用,这时候应该加载配置文件，实例化物品和方块，并注册它们。
     *
     * @param event
     */
    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        try {

            proxy.preInit(event);
            // 调用声音注册类注册所有声音
            SoundRegistry.registerSounds();
        } catch (Exception e) {
            logException("ModSim-preInit", e);
        }

    }

    /**
     * 用于该Mod的初始化,这时候应该为Mod进行设置，如注册合成表和烧炼系统，并且向其他Mod发送交互信息。
     *
     * @param event
     */
    @EventHandler
    public void init(FMLInitializationEvent event) {
        try {
            proxy.init(event);
        } catch (Exception e) {
            logException("ModSim-init", e);
        }
    }

    /**
     * 在所有Mod都初始化之后调用,这时候应该接收其他Mod发送的交互信息，并完成对Mod的设置
     * 在所有Mod都初始化之后调用,这时候应该接收其他Mod发送的交互信息，并完成对Mod的设置
     * @param event
     */
    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        try {
            MinecraftForge.EVENT_BUS.register(new SimConfigSync());
            proxy.postInit(event);
        } catch (Exception e) {
            logException("ModSim-postInit", e);
        }

    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        try {
            proxy.serverStarting(event);
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            logException("serverStarting", e);
        }
    }
}
