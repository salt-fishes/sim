package com.trhsy.sim.network.server;

import com.trhsy.sim.loader.ModSimLoader;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * @ClassName PacketSetupMod
 * @Description todo
 * @Author Tian
 * @Date 2022/9/2515:27
 **/
public class PacketSetupMod implements IMessage {
    public int gamemode;
    public PacketSetupMod() {
    }
    public PacketSetupMod(int gamemode) {
        this.gamemode = gamemode;
    }
    @Override
    public void fromBytes(ByteBuf buf) {
        this.gamemode = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.gamemode);
    }

    public static class Handler implements IMessageHandler<PacketSetupMod, IMessage> {
        public Handler() {
        }

        @Override
        public IMessage onMessage(PacketSetupMod message, MessageContext ctx) {
            try {
                FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
                    this.handle(message, ctx);
                });
            } catch (Exception var4) {
                StackTraceElement element = var4.getStackTrace()[0];
                ModSimLoader.log.error("PacketSetupMod出错了：" + var4.getMessage() + "行数：" + element.getLineNumber());
            }

            return null;
        }

        private void handle(PacketSetupMod message, MessageContext ctx) {
            // P11(S1): 全局模式只能由 OP 修改；单人游戏保留本地设置能力。
            EntityPlayerMP player = ctx.getServerHandler().player;
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            boolean singleplayer = server != null && server.isSinglePlayer();
            if (!singleplayer && (player == null || !player.canUseCommand(2, "sim"))) {
                ModSimLoader.log.warn("非 OP 玩家尝试修改模拟城市模式，已拒绝: {}", player == null ? "null" : player.getName());
                return;
            }
            if (message.gamemode != 0 && message.gamemode != 1 && message.gamemode != 999) {
                ModSimLoader.log.warn("非法模拟城市模式: {}", message.gamemode);
                return;
            }
            ModSimLoader.gamemode = message.gamemode;
        }
    }
}
