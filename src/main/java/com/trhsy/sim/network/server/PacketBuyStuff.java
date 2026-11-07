package com.trhsy.sim.network.server;

import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.NpcData;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @ClassName PacketBuyStuff
 * @Description todo
 * @Author TRHSY
 * @Date 2023/7/50:41
 **/
public class PacketBuyStuff implements IMessage {
    private UUID uuid;
    List<Integer>  quantities;
    public PacketBuyStuff() {
    }

    public PacketBuyStuff(UUID id, List<Integer>  quantities) {
        this.uuid=id;
        this.quantities=quantities;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.uuid = UUID.fromString(ByteBufUtils.readUTF8String(buf));
        String uIds=ByteBufUtils.readUTF8String(buf);
        uIds=uIds.substring(1,uIds.length()-1);
        uIds=uIds.replace(" ","");
        String[] ids=uIds.split(",");
        this.quantities= Stream.of(ids).map(Integer::parseInt).collect(Collectors.toList());
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.uuid.toString());
        ByteBufUtils.writeUTF8String(buf, this.quantities.toString());
    }
    public static class Handler implements IMessageHandler<PacketBuyStuff, IMessage> {
        public Handler() {
        }
        @Override
        public IMessage onMessage(PacketBuyStuff message, MessageContext ctx) {
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

        private void handle(PacketBuyStuff message, MessageContext ctx) {
            NpcData fd = ModSimLoader.getFolkDataByUID(message.uuid);
            // P10(S0): 空指针/无职业时直接丢弃，不让服务端线程崩。
            if (fd == null || fd.job == null) {
                return;
            }
            fd.job.buyStuff(message.quantities);
//            fd.fire();
        }
    }
}