package com.trhsy.sim.network.server;

import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.NpcData;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.UUID;

/**
 * @ClassName PacketFireFolk
 * @Description todo
 * @Author TRHSY
 * @Date 2022/10/2118:00
 **/
public class PacketFireFolk implements IMessage {
    private UUID uuid;

    public PacketFireFolk() {
    }

    public PacketFireFolk(UUID uuid) {
        this.uuid = uuid;
    }
    @Override
    public void fromBytes(ByteBuf buf) {
        this.uuid = UUID.fromString(ByteBufUtils.readUTF8String(buf));
    }
    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.uuid.toString());
    }

    public static class Handler implements IMessageHandler<PacketFireFolk, IMessage> {
        public Handler() {
        }
        @Override
        public IMessage onMessage(PacketFireFolk message, MessageContext ctx) {
            try {
                FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
                    this.handle(message, ctx);
                });
            } catch (Exception var4) {
                StackTraceElement element = var4.getStackTrace()[0];
                ModSimLoader.log.error("PacketFireFolk出错了：" + var4.getMessage() + "行数：" + element.getLineNumber());
            }

            return null;
        }

        private void handle(PacketFireFolk message, MessageContext ctx) {
            NpcData fd = ModSimLoader.getFolkDataByUID(message.uuid);
            // P12(S1): 服务端校验距离，客户端不能远程解雇。
            EntityPlayerMP player = ctx.getServerHandler().player;
            if (fd == null || fd.entity == null || player == null || player.getDistance(fd.entity) > 16.0F) {
                return;
            }
            fd.fire();
        }
    }
}