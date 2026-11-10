package com.trhsy.sim.network.client;

import com.trhsy.sim.loader.ModSimClientLoader;
import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.FolkRelationship;
import com.trhsy.sim.npcCode.NpcData;
import com.trhsy.sim.npcCode.SimIds;
import com.trhsy.sim.npcCode.economy.Economy;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.UUID;

/**
 * @ClassName PacketOpenFolkGui
 * @Description todo 打开NPC界面
 * @Author TRHSY
 * @Date 2022/10/1813:46
 **/
public class PacketOpenFolkGui implements IMessage {
    public UUID uid;
    /**NPC名字*/
    public String folkName;
    /**年龄*/
    public int folkAge;
    /**性别*/
    public int folkGender;
    /**工作，默认失业*/
    public String jobName = new TextComponentTranslation("container.sim.folkData1",new Object[0]).getUnformattedText();
    /**住房状态，默认无家可归*/
    public String housingStatus = new TextComponentTranslation("container.sim.folkData3",new Object[0]).getUnformattedText();
    /**情感状态*/
    public String relationshipStatus;
    /**状态*/
    public String status;
    /**饥饿状态*/
    public String hungerStatus;
    /**种族，默认人族*/
    public String folkRaceName = new TextComponentTranslation("container.sim.race_Human",new Object[0]).getUnformattedText();
    /**特征*/
    public String folkTrait1;
    /**特征*/
    public String folkTrait2;
    /**特征*/
    public String folkTrait3;

    public String folkTraitDesc1;
    public String folkTraitDesc2;
    public String folkTraitDesc3;
    /**建筑等级*/
    public String building = "1";
    /**农耕等级*/
    public String farming = "1";
    /**挖矿等级*/
    public String mining = "1";

    /**关系数据*/
    public String relationshipData = "";
    /**怀孕阶段*/
    public float pregnancyStage;
    /**个人存款*/
    public float money;
    /**岗位工资（每分钟）*/
    public float wage;
    /**稳定职业 ID*/
    public String jobId = "";
    public PacketOpenFolkGui() {
    }
    public PacketOpenFolkGui(NpcData fd) {
        this.uid=fd.ID;
        this.folkName = fd.getName();
        this.folkAge = fd.age;
        this.folkRaceName = fd.race.raceName;
        this.folkGender = fd.gender;
        this.folkTrait1=fd.trait1.getTraitName();
        this.folkTrait2=fd.trait2.getTraitName();
        this.folkTrait3=fd.trait3.getTraitName();
        this.folkTraitDesc1=fd.trait1.getTraitDescription();
        this.folkTraitDesc2=fd.trait2.getTraitDescription();
        this.folkTraitDesc3=fd.trait3.getTraitDescription();
        this.status=fd.status;
        this.hungerStatus=fd.getHunger()+":["+fd.hunger+"]";
        if (fd.job != null) {
            this.jobName = fd.job.toString();
        } else {
            this.jobName = new TextComponentTranslation("container.sim.folkData1",new Object[0]).getUnformattedText();
        }

        this.housingStatus = fd.home != null ? fd.home.buildingName :  new TextComponentTranslation("container.sim.folkData3",new Object[0]).getUnformattedText();;

        for(int i = 0; i < fd.relationships.size(); ++i) {
            this.relationshipData = this.relationshipData + ((FolkRelationship)fd.relationships.get(i)).getText() + (i < fd.relationships.size() - 1 ? ";" : "");
        }

        this.building = String.valueOf(fd.skillBuilding);
        this.farming = String.valueOf(fd.skillFarming);
        this.mining = String.valueOf(fd.skillMining);
        this.pregnancyStage=fd.pregnancyStage;
        this.money = fd.money;
        this.wage = fd.job != null ? Economy.WAGE_PER_MINUTE : 0.0F;
        this.jobId = fd.job != null ? SimIds.jobIdFromDisplay(fd.job.jobName) : "";
    }
    @Override
    public void fromBytes(ByteBuf buf) {
        this.uid=UUID.fromString(ByteBufUtils.readUTF8String(buf));
        this.folkName = ByteBufUtils.readUTF8String(buf);
        this.folkAge = buf.readInt();
        this.folkRaceName = ByteBufUtils.readUTF8String(buf);
        this.folkGender = buf.readInt();
        this.jobName = ByteBufUtils.readUTF8String(buf);
        this.housingStatus = ByteBufUtils.readUTF8String(buf);
        this.relationshipData = ByteBufUtils.readUTF8String(buf);
        this.building = ByteBufUtils.readUTF8String(buf);
        this.farming=ByteBufUtils.readUTF8String(buf);
        this.mining=ByteBufUtils.readUTF8String(buf);
        this.folkTrait1 = ByteBufUtils.readUTF8String(buf);
        this.folkTrait2 = ByteBufUtils.readUTF8String(buf);
        this.folkTrait3 = ByteBufUtils.readUTF8String(buf);
        this.folkTraitDesc1=ByteBufUtils.readUTF8String(buf);
        this.folkTraitDesc2=ByteBufUtils.readUTF8String(buf);
        this.folkTraitDesc3=ByteBufUtils.readUTF8String(buf);
        this.status = ByteBufUtils.readUTF8String(buf);
        this.hungerStatus=ByteBufUtils.readUTF8String(buf);
        this.pregnancyStage=buf.readFloat();
        this.money=buf.readFloat();
        this.wage=buf.readFloat();
        this.jobId=ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.uid.toString());
        ByteBufUtils.writeUTF8String(buf, this.folkName);
        buf.writeInt(this.folkAge);
        ByteBufUtils.writeUTF8String(buf, this.folkRaceName);
        buf.writeInt(this.folkGender);
        ByteBufUtils.writeUTF8String(buf, this.jobName);
        ByteBufUtils.writeUTF8String(buf, this.housingStatus);
        ByteBufUtils.writeUTF8String(buf, this.relationshipData);
        ByteBufUtils.writeUTF8String(buf, this.building);
        ByteBufUtils.writeUTF8String(buf, this.farming);
        ByteBufUtils.writeUTF8String(buf, this.mining);
        ByteBufUtils.writeUTF8String(buf, this.folkTrait1);
        ByteBufUtils.writeUTF8String(buf, this.folkTrait2);
        ByteBufUtils.writeUTF8String(buf, this.folkTrait3);
        ByteBufUtils.writeUTF8String(buf, this.folkTraitDesc1);
        ByteBufUtils.writeUTF8String(buf, this.folkTraitDesc2);
        ByteBufUtils.writeUTF8String(buf, this.folkTraitDesc3);
        ByteBufUtils.writeUTF8String(buf, this.status);
        ByteBufUtils.writeUTF8String(buf, this.hungerStatus);
        buf.writeFloat(this.pregnancyStage);
        buf.writeFloat(this.money);
        buf.writeFloat(this.wage);
        ByteBufUtils.writeUTF8String(buf, this.jobId == null ? "" : this.jobId);
    }
    public static class Handler implements IMessageHandler<PacketOpenFolkGui, IMessage> {
        public Handler() {
        }
        @Override
        public IMessage onMessage(PacketOpenFolkGui message, MessageContext ctx) {
            try {
                FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
                    this.handle(message, ctx);
                });
            } catch (Exception var4) {
                StackTraceElement element = var4.getStackTrace()[0];
                ModSimLoader.log.error("PacketOpenFolkGui出错了：" + var4.getMessage() + "行数：" + element.getLineNumber());
            }

            return null;
        }

        private void handle(PacketOpenFolkGui message, MessageContext ctx) {
            ModSimClientLoader.openFolkGui(message);
        }
    }
}
