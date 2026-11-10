package com.trhsy.sim.network.client;

import com.trhsy.sim.loader.ModSimClientLoader;
import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.NpcData;
import com.trhsy.sim.npcCode.NpcIdentity;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.network.client
 * @ClassName: PacketReturnHireableFolks
 * @Description: 可雇佣的人
 * @date 2022/10/13 15:04
 */
public class PacketReturnHireableFolks implements IMessage {
    public List<NpcIdentity> folkNames = new CopyOnWriteArrayList<NpcIdentity>();
    public PacketReturnHireableFolks() {
        this.folkNames= new CopyOnWriteArrayList<NpcIdentity>();
        //ModSimLoader.log.info("开始读取可雇佣的人");
        for (NpcData folk:ModSimLoader.folks){
            NpcIdentity identity = folk.getClientIdentity();
            if(identity!=null &&!folk.isDead){
                this.folkNames.add(identity);
            }
        }
    }
    @Override
    public void fromBytes(ByteBuf buf) {
        this.folkNames.clear();
        try {
           /* String rawFolkData = ByteBufUtils.readUTF8String(buf);
            String[] rawFolks = rawFolkData.split(";");
            String[] var4 = rawFolks;
            int var5 = rawFolks.length;

            for(int var6 = 0; var6 < var5; ++var6) {
                String cFolk = var4[var6];
                if (cFolk.length() > 0 && cFolk.contains(",_,")) {
                }
                NpcIdentity npcIdentity=new NpcIdentity(cFolk);
                this.folkNames.add(npcIdentity);
            }*/

            // 读取 NPC 数量
            int count = buf.readInt();

            // 逐个读取每个 NPC 的数据
            for (int i = 0; i < count; i++) {
                NpcIdentity identity = readNpcIdentity(buf);
                this.folkNames.add(identity);
            }
        } catch (Exception var8) {

        }

    }

    @Override
    public void toBytes(ByteBuf buf) {
       /* String s="";
        for (NpcIdentity fName :this.folkNames){
            s+=fName.id + ",_," + fName.name + ",_," + fName.age + ",_," + fName.status + ",_," + fName.job + ",_," + fName.house + ",_," + fName.relationship + ",_," + fName.hunger + ",_," + fName.maturityAge + ",_," + fName.skinPath + ",_," + fName.isDead + ";";
        }
            ByteBufUtils.writeUTF8String(buf, s);*/

        // 先写入 NPC 数量
        buf.writeInt(folkNames.size());

        // 逐个写入每个 NPC 的数据
        for (NpcIdentity identity : folkNames) {
            writeNpcIdentity(buf, identity);
        }

    }
    // 写入单个 NPC 数据
    private void writeNpcIdentity(ByteBuf buf, NpcIdentity identity) {
        ByteBufUtils.writeUTF8String(buf, identity.id.toString());
        ByteBufUtils.writeUTF8String(buf, identity.name);
        buf.writeInt(Integer.parseInt(identity.age));
        ByteBufUtils.writeUTF8String(buf, identity.status);
        ByteBufUtils.writeUTF8String(buf, identity.job);
        ByteBufUtils.writeUTF8String(buf, identity.house);
        ByteBufUtils.writeUTF8String(buf, identity.relationship);
        ByteBufUtils.writeUTF8String(buf, identity.hunger);
        buf.writeInt(Integer.parseInt(identity.maturityAge));
        ByteBufUtils.writeUTF8String(buf, identity.skinPath);
        buf.writeBoolean(identity.isDead);
        buf.writeFloat(identity.money);
        buf.writeFloat(identity.wage);
        ByteBufUtils.writeUTF8String(buf, identity.jobId == null ? "" : identity.jobId);
    }

    // 读取单个 NPC 数据
    private NpcIdentity readNpcIdentity(ByteBuf buf) {
        String id = ByteBufUtils.readUTF8String(buf);
        String name = ByteBufUtils.readUTF8String(buf);
        int age = buf.readInt();
        String status = ByteBufUtils.readUTF8String(buf);
        String job = ByteBufUtils.readUTF8String(buf);
        String house = ByteBufUtils.readUTF8String(buf);
        String relationship = ByteBufUtils.readUTF8String(buf);
        String hunger =  ByteBufUtils.readUTF8String(buf);
        int maturityAge = buf.readInt();
        String skinPath = ByteBufUtils.readUTF8String(buf);
        boolean isDead = buf.readBoolean();
        float money = buf.readFloat();
        float wage = buf.readFloat();
        String jobId = ByteBufUtils.readUTF8String(buf);

        return new NpcIdentity(UUID.fromString(id), name, String.valueOf(age), status, job, house, relationship, String.valueOf(hunger), String.valueOf(maturityAge), skinPath, isDead, money, wage, jobId);
    }
    public static class Handler implements IMessageHandler<PacketReturnHireableFolks, IMessage> {
        public Handler() {
        }

        @Override
        public IMessage onMessage(PacketReturnHireableFolks message, MessageContext ctx) {
            try {
                FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
                    this.handle(message, ctx);
                });
            } catch (Exception var4) {
                StackTraceElement element = var4.getStackTrace()[0];
                ModSimLoader.log.error("PacketReturnHireableFolks出错了：" + var4.getMessage() + "行数：" + element.getLineNumber());
            }

            return null;
        }

        private void handle(PacketReturnHireableFolks message, MessageContext ctx) {
            ModSimClientLoader.tempHireableNpcNames.clear();
            ModSimClientLoader.tempHireableNpcNames = message.folkNames;
            //ModSimLoader.log.info("获取可以受雇佣的人，客户端收到数据包，并处理");
        }
    }
}
