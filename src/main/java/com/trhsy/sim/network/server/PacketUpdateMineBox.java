package com.trhsy.sim.network.server;

import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.block.MineBox;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.Iterator;
import java.util.UUID;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.network.server
 * @ClassName: PacketUpdateMineBox
 * @Description:
 * @date 2023/5/17 9:20
 */
public class PacketUpdateMineBox implements IMessage {
    public UUID id;
    public int x;
    public int z;
    public EnumFacing facing;

    public PacketUpdateMineBox() {
    }

    public PacketUpdateMineBox(UUID id, int x, int z, EnumFacing facing) {
        this.id = id;
        this.x = x;
        this.z = z;
        this.facing = facing;
    }
    @Override
    public void fromBytes(ByteBuf buf) {
        this.id = UUID.fromString(ByteBufUtils.readUTF8String(buf));
        this.x = buf.readInt();
        this.z = buf.readInt();
        this.facing = EnumFacing.byName(ByteBufUtils.readUTF8String(buf));
    }
    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.id.toString());
        buf.writeInt(this.x);
        buf.writeInt(this.z);
        ByteBufUtils.writeUTF8String(buf, this.facing.toString());
    }

    public static class Handler implements IMessageHandler<PacketUpdateMineBox, IMessage> {
        public Handler() {
        }

        @Override
        public IMessage onMessage(PacketUpdateMineBox message, MessageContext ctx) {
            try {
                FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
                    this.handle(message, ctx);
                });
            } catch (Exception var4) {
                var4.printStackTrace();
            }

            return null;
        }

        private void handle(PacketUpdateMineBox message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            if (player == null || message.facing == null
                    || message.x < 0 || message.x > 64 || message.z < 0 || message.z > 64) {
                return;
            }
            Iterator iterator = ModSimLoader.mines.iterator();
            while (iterator.hasNext()) {
                MineBox mine = (MineBox) iterator.next();
                if (mine.ID.equals(message.id)) {
                    // P15(S1): 服务端校验维度+距离。
                    if (mine.loc == null || mine.loc.dimension != player.world.provider.getDimension()
                            || player.getDistance(mine.loc.x, mine.loc.y, mine.loc.z) > 16.0D) {
                        ModSimLoader.log.warn("玩家 {} 距离矿场过远，拒绝修改", player.getName());
                        return;
                    }
                    mine.x = message.x;
                    mine.z = message.z;
                    mine.facing = message.facing;
                    mine.saveMine();
                    break;
                }
            }
        }
    }
}
