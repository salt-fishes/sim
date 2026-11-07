package com.trhsy.sim.proxy;

import com.trhsy.sim.loader.*;
import com.trhsy.sim.npcCode.race.Race;
import com.trhsy.sim.npcCode.traits.Traits;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.proxy
 * @ClassName: CommonProxy
 * @Description: 通用代理类，包含客户端和服务器端都需要的初始化逻辑
 * @date 2023/10/19 下午 2:09
 */
public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        ModSimLoader.log = event.getModLog();
        /**配置**/
        ConfigLoader.load(event);
        /**启动通讯**/
        new NetWorkLoader(event);
        /**矿物生成**/
        new WorldGeneratorLoader();
        /**实体渲染**/
        EntityLoader.registerEntitys();
        /**事件加载**/
        new EventLoader();
        /**特征**/
        Traits.loadTraits();
        /**种族*/
        Race.loadRaces();
    }

    public void init(FMLInitializationEvent event) {
        /**     合成表**/
        new CraftingLoader();
    }

    public void postInit(FMLPostInitializationEvent event) {
        /**加载所以建筑蓝图**/
        ModSimLoader.loadAllBuildings();
    }

    public void renderTick(TickEvent.RenderTickEvent e) {
        // 通用的渲染逻辑，如果有的话可以在这里实现
    }

    public void serverStarting(FMLServerStartingEvent event) {
        try {
            new CommandLoader(event);
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("serverStarting出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }
}
