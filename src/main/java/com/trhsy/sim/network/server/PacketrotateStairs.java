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
 * @ClassName PacketrotateStairs
 * @Description todo 旋转楼梯
 * @Author TRHSY
 * @Date 2023/3/2121:15
 **/
public class PacketrotateStairs implements IMessage {
    String uuid;

    public PacketrotateStairs() {
    }

    public PacketrotateStairs(String uuid) {
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

    public static class Handler implements IMessageHandler<PacketrotateStairs, IMessage> {
        public Handler() {
        }
        @Override
        public IMessage onMessage(PacketrotateStairs message, MessageContext ctx) {
            try {
                FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
                    this.handle(message, ctx);
                });
            } catch (Exception var4) {
                StackTraceElement element = var4.getStackTrace()[0];
                ModSimLoader.log.error("PacketrotateStairs出错了：" + var4.getMessage() + "行数：" + element.getLineNumber());
            }

            return null;
        }

        private void handle(PacketrotateStairs message, MessageContext ctx) {
            Building b = ModSimLoader.getBuildingByUUID(message.uuid);
            EntityPlayerMP player = ctx.getServerHandler().player;
            if (b != null && b.controlXYZ != null && player != null) {
                // P15(S1): 与服务端拆楼一致，旋转楼梯也必须靠近控制箱。
                if (b.dimension != player.world.provider.getDimension()
                        || player.getDistance(b.controlXYZ.x, b.controlXYZ.y, b.controlXYZ.z) > 16.0D) {
                    ModSimLoader.log.warn("玩家 {} 距离建筑过远，拒绝旋转楼梯 {}", player.getName(), b.buildingName);
                    return;
                }
                ModSimLoader.log.info("旋转建筑：" + b.buildingName);
                b.rotateStairs(player.world);
            }
        }
    }
}