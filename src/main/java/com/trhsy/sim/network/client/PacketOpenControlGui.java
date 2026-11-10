package com.trhsy.sim.network.client;

import com.trhsy.sim.loader.ModSimClientLoader;
import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.NpcData;
import com.trhsy.sim.npcCode.V3;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @ClassName PacketOpenControlGui
 * @Description todo
 * @Author TRHSY
 * @Date 2022/11/711:44
 **/
public class PacketOpenControlGui implements IMessage {
    public NBTTagCompound nbt;
    private V3 v3;
    private String buildingId;
    private String jobName;
    private String buildingName;
    private String buildingType;
    private String author;
//    private NpcIdentity folk;
    private List<NpcData> occupants;
    private boolean isResidential;
    private String describe;

    public PacketOpenControlGui() {
    }
    /**初始化**/
    public PacketOpenControlGui(V3 v3,String buildingId, String buildingName,String buildingType,String jobName,String author,String describe, List<NpcData> occupants, boolean isResidential) {
        this.occupants = occupants;
        this.v3 = v3;
        this.buildingId = buildingId;
        this.jobName = jobName;
        this.buildingName = buildingName;
        this.author=author;
        this.buildingType=buildingType;
        this.isResidential = isResidential;
        this.describe=describe;
    }
    public PacketOpenControlGui(V3 v3,String buildingId,String buildingName,String buildingType,String jobName,String author,String describe, boolean isResidential) {
        this.occupants =new CopyOnWriteArrayList<NpcData>();
        this.v3 = v3;
        this.buildingId = buildingId;
        this.jobName = jobName;
        this.buildingName = buildingName;
        this.author=author;
        this.buildingType=buildingType;
        this.isResidential = isResidential;
        this.describe=describe;
    }
    @Override
    public void fromBytes(ByteBuf buf) {
        this.v3 = V3.fromString(ByteBufUtils.readUTF8String(buf));
        this.buildingId = ByteBufUtils.readUTF8String(buf);
        this.jobName = ByteBufUtils.readUTF8String(buf);
        this.buildingName = ByteBufUtils.readUTF8String(buf);
        this.author=ByteBufUtils.readUTF8String(buf);
        this.describe=ByteBufUtils.readUTF8String(buf);
        this.buildingType=ByteBufUtils.readUTF8String(buf);
        this.isResidential = buf.readBoolean();
        this.occupants = new CopyOnWriteArrayList<NpcData>();
        try {
            String ids = ByteBufUtils.readUTF8String(buf);
            if (ids == null || ids.isEmpty()) {
                return;
            }
            String[] id = ids.split(";");
            for (String s : id) {
                if (s == null || s.isEmpty()) {
                    continue;
                }
                NpcData npcData = ModSimLoader.getFolkDataByUID(UUID.fromString(s));
                if (npcData != null) {
                    this.occupants.add(npcData);
                }
            }
        } catch (Exception var3) {
        }

    }
    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.v3.toString());
        ByteBufUtils.writeUTF8String(buf, this.buildingId);
        ByteBufUtils.writeUTF8String(buf, this.jobName);
        ByteBufUtils.writeUTF8String(buf, this.buildingName);
        ByteBufUtils.writeUTF8String(buf, this.author);
        ByteBufUtils.writeUTF8String(buf, this.describe);
        ByteBufUtils.writeUTF8String(buf, this.buildingType);
        buf.writeBoolean(this.isResidential);
        String ids = "";
        if (this.occupants != null) {
            for (int i = 0; i < this.occupants.size(); i++) {
                if (this.occupants.get(i) != null && this.occupants.get(i).ID != null) {
                    ids += this.occupants.get(i).ID + ";";
                }
            }
        }
        ByteBufUtils.writeUTF8String(buf, ids);
    }

    public static class Handler implements IMessageHandler<PacketOpenControlGui, IMessage> {
        public Handler() {
        }
        @Override
        public IMessage onMessage(PacketOpenControlGui message, MessageContext ctx) {
            try {
                FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
                    this.handle(message, ctx);
                });
            } catch (Exception var4) {
                StackTraceElement element = var4.getStackTrace()[0];
                ModSimLoader.log.error("PacketOpenControlGui出错了：" + var4.getMessage() + "行数：" + element.getLineNumber());
            }

            return null;
        }

        private void handle(PacketOpenControlGui message, MessageContext ctx) {
            if (message.occupants==null||message.occupants.size()==0) {
                ModSimClientLoader.openControlGui(message.v3, message.buildingId,message.buildingName,message.jobName,message.buildingType,message.author,message.describe);
            } else if (message.isResidential) {
                ModSimClientLoader.openControlGui(message.v3, message.buildingId, message.occupants,true,message.buildingName,message.jobName,message.buildingType,message.author,message.describe);
            } else {
                ModSimClientLoader.openControlGui(message.v3, message.buildingId,message.occupants,message.buildingName,message.jobName,message.buildingType,message.author,message.describe);
            }

        }
    }
}