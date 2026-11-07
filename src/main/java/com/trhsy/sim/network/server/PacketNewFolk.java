package com.trhsy.sim.network.server;

import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.NpcData;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * @ClassName PacketNewFolk
 * @Description todo
 * @Author TRHSY
 * @Date 2023/8/1316:55
 **/
public class PacketNewFolk implements IMessage {
    private boolean fromCommand;

    public PacketNewFolk() {
    }

    public PacketNewFolk(boolean fromCommand) {
        this.fromCommand = fromCommand;
    }
    @Override
    public void fromBytes(ByteBuf buf) {
        this.fromCommand = Boolean.parseBoolean(ByteBufUtils.readUTF8String(buf));
    }
    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, String.valueOf(this.fromCommand));
    }

    public static class Handler implements IMessageHandler<PacketNewFolk, IMessage> {
        public Handler() {
        }
        @Override
        public IMessage onMessage(PacketNewFolk message, MessageContext ctx) {
            try {
                FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
                    this.handle(message, ctx);
                });
            } catch (Exception var4) {
                StackTraceElement element = var4.getStackTrace()[0];
                ModSimLoader.log.error("PacketNewFolk出错了：" + var4.getMessage() + "行数：" + element.getLineNumber());
            }

            return null;
        }

        private void handle(PacketNewFolk message, MessageContext ctx) {
            // 获取服务器玩家（触发雇佣操作的玩家）
            EntityPlayerMP player = ctx.getServerHandler().player;
            if (player == null) {
                ModSimLoader.log.error("生成新的NPC失败：玩家对象为空");
                return;
            }

            // 通过玩家获取其所在的世界（服务器端世界）
            World world = player.world;
            if (world == null) {
                ModSimLoader.log.error("生成新的NPC失败：玩家所在世界为空");
                return;
            }
//            Minecraft mc = Minecraft.getMinecraft();
//            World world = mc.world;
            // P15(S1): 旧代码只在 world.isRemote 时生成，服务端永远不执行（逻辑反了）。
            // 该包目前没有调用方，但保留时必须是服务端生成 + 权限校验。
            if (world.isRemote) {
                return;
            }
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            boolean singleplayer = server != null && server.isSinglePlayer();
            if (!singleplayer && !player.canUseCommand(2, "sim")) {
                ModSimLoader.log.warn("非 OP 玩家尝试生成 NPC，已拒绝: {}", player.getName());
                return;
            }
            NpcData fd = new NpcData(world, message.fromCommand);
        }
    }
}