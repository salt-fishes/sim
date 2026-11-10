package com.trhsy.sim.network.client;

import com.trhsy.sim.loader.ModSimClientLoader;
import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.logistics.LogisticsSnapshot;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 服务端 -> 客户端：物流中心设置快照。
 */
public class PacketLogisticsSnapshot implements IMessage {
    private NBTTagCompound tag;

    public PacketLogisticsSnapshot() {
    }

    public PacketLogisticsSnapshot(NBTTagCompound tag) {
        this.tag = tag;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.tag = ByteBufUtils.readTag(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeTag(buf, this.tag == null ? new NBTTagCompound() : this.tag);
    }

    public static class Handler implements IMessageHandler<PacketLogisticsSnapshot, IMessage> {
        @Override
        public IMessage onMessage(PacketLogisticsSnapshot message, MessageContext ctx) {
            try {
                FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
                    NBTTagCompound tag = message.tag == null ? new NBTTagCompound() : message.tag;
                    ModSimClientLoader.applyLogisticsSnapshot(LogisticsSnapshot.readFromNBT(tag));
                });
            } catch (Exception e) {
                StackTraceElement element = e.getStackTrace()[0];
                ModSimLoader.log.error("PacketLogisticsSnapshot出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
            }
            return null;
        }
    }
}
