package com.trhsy.sim.network.server;

import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.build.Building;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * @ClassName PacketDemolishBuilding
 * @Description todo 拆除建筑
 * @Author TRHSY
 * @Date 2022/11/711:53
 **/
public class PacketDemolishBuilding implements IMessage {
    String uuid;

    public PacketDemolishBuilding() {
    }

    public PacketDemolishBuilding(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.uuid = ByteBufUtils.readUTF8String(buf);
    }
    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.uuid);
    }

    public static class Handler implements IMessageHandler<PacketDemolishBuilding, IMessage> {
        public Handler() {
        }
        @Override
        public IMessage onMessage(PacketDemolishBuilding message, MessageContext ctx) {
            try {
                FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
                    this.handle(message, ctx);
                });
            } catch (Exception var4) {
                StackTraceElement element = var4.getStackTrace()[0];
                ModSimLoader.log.error("PacketDemolishBuilding出错了：" + var4.getMessage() + "行数：" + element.getLineNumber());
            }

            return null;
        }

        private void handle(PacketDemolishBuilding message, MessageContext ctx) {
            Building b = ModSimLoader.getBuildingByUUID(message.uuid);
            EntityPlayerMP player = ctx.getServerHandler().player;
            if (b != null && b.controlXYZ != null && player != null) {
                // P12(S1): 服务端校验玩家与建筑控制箱的距离，防止远程拆楼。
                if (player.getDistance(b.controlXYZ.x, b.controlXYZ.y, b.controlXYZ.z) > 16.0D) {
                    ModSimLoader.log.warn("玩家 {} 距离建筑过远，拒绝拆除 {}", player.getName(), b.buildingName);
                    return;
                }
                ModSimLoader.log.info("拆除建筑："+b.buildingName);
                b.demolish(player.world, true);
            }

        }
    }
}
