package com.trhsy.sim.network.client;

import com.google.common.collect.Lists;
import com.trhsy.sim.config.SimConfigSync;
import com.trhsy.sim.loader.ModSimLoader;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.List;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.network.client
 * @ClassName: ConfigSyncPacket
 * @Description: 配置同步
 * @date 2022/10/11 11:08
 */
public class ConfigSyncPacket implements IMessage {
    public List<ConfigCategory> categories = Lists.newLinkedList();
    @Override
    public void fromBytes(ByteBuf buf) {
        short categoryCount = buf.readShort();

        for (short i = 0; i < categoryCount; i++) {
            int propCount = buf.readInt();
            String categoryName = ByteBufUtils.readUTF8String(buf);
            ConfigCategory category = new ConfigCategory(categoryName);
            this.categories.add(category);

            for (int j = 0; j < propCount; ++j) {
                String name = ByteBufUtils.readUTF8String(buf);
                char type = buf.readChar();
                String value = ByteBufUtils.readUTF8String(buf);
                category.put(name, new Property(name, value, Property.Type.tryParse(type)));
            }
        }
        // P16(S1): 不要在这里 release。FMLIndexedMessageToMessageCodec 在
        // decode 结束后会统一 release payload；这里再 release 会导致
        // IllegalReferenceCountException: refCnt: 0，玩家一进服就断线。
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeShort(this.categories.size());
        for (ConfigCategory category : this.categories) {
            buf.writeInt(category.values().size());
            ByteBufUtils.writeUTF8String(buf, category.getName());
            for (Property prop : category.values()) {
                ByteBufUtils.writeUTF8String(buf, prop.getName());
                buf.writeChar(prop.getType().getID());
                ByteBufUtils.writeUTF8String(buf, prop.getString());
            }
        }
    }
    public static class Handler implements IMessageHandler<ConfigSyncPacket, IMessage> {
        public Handler() {
        }
        @Override
        public IMessage onMessage(ConfigSyncPacket message, MessageContext ctx) {
            try {
                FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
                    this.handle(message, ctx);
                });
            } catch (Exception var4) {
                StackTraceElement element = var4.getStackTrace()[0];
                ModSimLoader.log.error("ConfigSyncPacket出错了：" + var4.getMessage() + "行数：" + element.getLineNumber());
            }

            return null;
        }
        private void handle(ConfigSyncPacket message, MessageContext ctx) {
            SimConfigSync.syncConfig(message.categories);
        }
    }
}
