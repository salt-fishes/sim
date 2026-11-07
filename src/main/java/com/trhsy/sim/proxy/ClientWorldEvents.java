package com.trhsy.sim.proxy;

import com.trhsy.sim.loader.ModSimClientLoader;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 客户端世界事件（P0）。
 *
 * <p>原实现把"重置客户端模拟状态镜像"写在 EventLoader.worldLoad 里，而 worldLoad
 * 在服务端执行；那一句会强制服务端链接 ModSimClientLoader（import
 * net.minecraft.client.Minecraft），专用服务器上直接 NoClassDefFoundError。
 * 拆到客户端侧、由 ClientProxy 注册后，服务端代码路径完全不引用客户端类。
 */
@SideOnly(Side.CLIENT)
public class ClientWorldEvents {

    @SubscribeEvent
    public void worldLoad(WorldEvent.Load event) {
        if (!event.getWorld().isRemote) {
            return;
        }
        ModSimClientLoader.sim_is_running = false;
    }
}
