package com.trhsy.sim.network.server;

import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.loader.NetWorkLoader;
import com.trhsy.sim.network.client.PacketLogisticsSnapshot;
import com.trhsy.sim.npcCode.V3;
import com.trhsy.sim.npcCode.logistics.LogisticsService;
import com.trhsy.sim.npcCode.logistics.LogisticsSnapshot;
import com.trhsy.sim.npcCode.store.SimStore;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 客户端 -> 服务端：切换物流中心总开关或单个物品的派送开关。
 *
 * <p>服务端校验玩家与物流中心的距离后改 {@link SimStore}，再把新快照发回客户端。
 * 客户端不直接改钱、库存或订单。</p>
 */
public class PacketToggleLogistics implements IMessage {
    private V3 center;
    private String requesterKey = "";
    private String itemKey = "";
    private boolean enabled;
    /** 0=物品开关，1=总开关。 */
    private int kind;

    public PacketToggleLogistics() {
    }

    public PacketToggleLogistics(V3 center, String requesterKey, String itemKey, boolean enabled) {
        this.center = center;
        this.requesterKey = requesterKey == null ? "" : requesterKey;
        this.itemKey = itemKey == null ? "" : itemKey;
        this.enabled = enabled;
        this.kind = 0;
    }

    public static PacketToggleLogistics autoDispatch(V3 center, boolean enabled) {
        PacketToggleLogistics packet = new PacketToggleLogistics();
        packet.center = center;
        packet.enabled = enabled;
        packet.kind = 1;
        return packet;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        String centerText = ByteBufUtils.readUTF8String(buf);
        try {
            this.center = V3.fromString(centerText);
        } catch (Exception e) {
            this.center = null;
        }
        this.kind = buf.readInt();
        this.requesterKey = ByteBufUtils.readUTF8String(buf);
        this.itemKey = ByteBufUtils.readUTF8String(buf);
        this.enabled = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.center == null ? "" : this.center.toString());
        buf.writeInt(this.kind);
        ByteBufUtils.writeUTF8String(buf, this.requesterKey == null ? "" : this.requesterKey);
        ByteBufUtils.writeUTF8String(buf, this.itemKey == null ? "" : this.itemKey);
        buf.writeBoolean(this.enabled);
    }

    public static class Handler implements IMessageHandler<PacketToggleLogistics, IMessage> {
        @Override
        public IMessage onMessage(PacketToggleLogistics message, MessageContext ctx) {
            try {
                FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
                    this.handle(message, ctx);
                });
            } catch (Exception e) {
                StackTraceElement element = e.getStackTrace()[0];
                ModSimLoader.log.error("PacketToggleLogistics出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
            }
            return null;
        }

        private void handle(PacketToggleLogistics message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            if (player == null || message.center == null) {
                return;
            }
            if (player.dimension != message.center.dimension
                    || player.getDistance(message.center.x, message.center.y, message.center.z) > 16.0F) {
                return;
            }
            SimStore store = SimStore.get(player.world);
            if (store == null) {
                return;
            }
            if (message.kind == 1) {
                LogisticsService.setAutoDispatch(store, message.center, message.enabled);
            } else if (!LogisticsService.toggleItem(store, message.center, message.requesterKey,
                    message.itemKey, message.enabled)) {
                return;
            }
            LogisticsSnapshot snapshot = LogisticsService.snapshot(store, message.center);
            NetWorkLoader.net.sendTo(new PacketLogisticsSnapshot(snapshot.writeToNBT()), player);
        }
    }
}
