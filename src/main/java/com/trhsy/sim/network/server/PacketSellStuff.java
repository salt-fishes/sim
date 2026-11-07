package com.trhsy.sim.network.server;

import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.NpcData;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.UUID;

/**
 * @ClassName PacketSellStuff
 * @Description todo 将选定的玩家库存出售给商家
 * @Author TRHSY
 * @Date 2023/7/50:10
 **/
public class PacketSellStuff implements IMessage{
    private UUID uuid;

    public PacketSellStuff() {
    }

    public PacketSellStuff(UUID id) {
        this.uuid=id;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.uuid = UUID.fromString(ByteBufUtils.readUTF8String(buf));
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.uuid.toString());
    }
    public static class Handler implements IMessageHandler<PacketSellStuff, IMessage> {
        public Handler() {
        }
        @Override
        public IMessage onMessage(PacketSellStuff message, MessageContext ctx) {
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

        private void handle(PacketSellStuff message, MessageContext ctx) {
            NpcData fd = ModSimLoader.getFolkDataByUID(message.uuid);
            if (fd == null || fd.job == null) {
                return;
            }
            fd.job.sellStuff();
//            fd.fire();
        }
    }
}
