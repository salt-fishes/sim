package com.trhsy.sim.network.server;

import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.block.FarmBox;
import com.trhsy.sim.npcCode.enums.FarmType;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.UUID;

/**
 * @ClassName PacketUpdateFarmBox
 * @Description todo 更新服务器端的养殖箱
 * @Author TRHSY
 * @Date 2022/12/1014:55
 **/
public class PacketUpdateFarmBox implements IMessage {
    public UUID id;
    public int x;
    public int z;
    public EnumFacing facing;
    public FarmType farmType;


    public PacketUpdateFarmBox() {
    }

    public PacketUpdateFarmBox(UUID id, int x, int z, EnumFacing facing,FarmType farmType) {
        this.id = id;
        this.x = x;
        this.z = z;
        this.facing = facing;
        this.farmType = farmType;
    }
    @Override
    public void fromBytes(ByteBuf buf) {
        this.id = UUID.fromString(ByteBufUtils.readUTF8String(buf));
        this.x = buf.readInt();
        this.z = buf.readInt();
        this.facing = EnumFacing.byName(ByteBufUtils.readUTF8String(buf));
        this.farmType = FarmType.byName(ByteBufUtils.readUTF8String(buf));
    }
    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.id.toString());
        buf.writeInt(this.x);
        buf.writeInt(this.z);
        ByteBufUtils.writeUTF8String(buf, this.facing.toString());
        ByteBufUtils.writeUTF8String(buf, this.farmType.toString());
    }

    public static class Handler implements IMessageHandler<PacketUpdateFarmBox, IMessage> {
        public Handler() {
        }
        @Override
        public IMessage onMessage(PacketUpdateFarmBox message, MessageContext ctx) {
            try {
                FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
                    this.handle(message, ctx);
                });
            } catch (Exception var4) {
                StackTraceElement element = var4.getStackTrace()[0];
                ModSimLoader.log.error("PacketUpdateFarmBox出错了：" + var4.getMessage() + "行数：" + element.getLineNumber());
            }

            return null;
        }

        private void handle(PacketUpdateFarmBox message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            if (player == null || message.facing == null || message.farmType == null
                    || message.x < 0 || message.x > 64 || message.z < 0 || message.z > 64) {
                return;
            }
            for (FarmBox farm : ModSimLoader.farms) {
                if (farm.ID.equals(message.id)) {
                    // P15(S1): 服务端校验维度+距离，客户端不能远程改别人的农场。
                    if (farm.loc == null || farm.loc.dimension != player.world.provider.getDimension()
                            || player.getDistance(farm.loc.x, farm.loc.y, farm.loc.z) > 16.0D) {
                        ModSimLoader.log.warn("玩家 {} 距离农场过远，拒绝修改", player.getName());
                        return;
                    }
                    farm.x = message.x;
                    farm.z = message.z;
                    farm.facing = message.facing;
                    farm.farmType = message.farmType;
                    farm.saveFarm();
                    break;
                }
            }
        }
    }
}