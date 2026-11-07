package com.trhsy.sim.gui.npc;

import com.trhsy.sim.ModSim;
import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.loader.NetWorkLoader;
import com.trhsy.sim.network.client.PacketOpenFolkGui;
import com.trhsy.sim.npcCode.NpcData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.lwjgl.opengl.GL11;

import java.util.UUID;

/**
 * @ClassName GuiFolk
 * @Description todo NPC 交互界面
 * @Author TRHSY
 * @Date 2022/10/1812:55
 **/
public class GuiFolk extends GuiScreen {
    // npc 名称
    String folkName;
    //年龄
    int folkAge;
    //性别
    int folkGender;
    //工作名称
    String jobName;
    //住房状态
    String housingStatus;
    //关系状态
    String relationshipStatus;
    //当前状态
    String status;
    //饥饿状态
    String hungerStatus;
    //种族
    String folkRaceName;

    String folkTrait1;
    String folkTrait2;
    String folkTrait3;

    String folkTraitDesc1;
    String folkTraitDesc2;
    String folkTraitDesc3;
    //关系数据
    String relationshipData;
    //建筑等级
    String building;
    //农耕等级
    String farming;
    //挖矿等级
    String mining;
    float pregnancyStage;
    int page = 0;
    UUID uid;

    public GuiFolk(PacketOpenFolkGui message) {
        this.uid=message.uid;
        this.folkName = message.folkName;
        this.folkAge = message.folkAge;
        this.folkGender = message.folkGender;
        this.folkRaceName = message.folkRaceName;
        this.jobName = message.jobName;
        this.housingStatus = message.housingStatus;
        this.relationshipStatus = message.relationshipStatus;
        this.status = message.status;
        this.folkTrait1 = message.folkTrait1;
        this.folkTrait2 = message.folkTrait2;
        this.folkTrait3 = message.folkTrait3;
        this.folkTraitDesc1 = message.folkTraitDesc1;
        this.folkTraitDesc2 = message.folkTraitDesc2;
        this.folkTraitDesc3 = message.folkTraitDesc3;
        this.relationshipData = message.relationshipData;
        this.building = message.building;
        this.farming = message.farming;
        this.mining = message.mining;
        this.hungerStatus = message.hungerStatus;
        this.pregnancyStage=message.pregnancyStage ;
    }
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
    @Override
    public void initGui() {
        super.initGui();
        this.showPage();
    }

    public void showPage() {
        this.buttonList.clear();
        //完成 按鈕
        this.buttonList.add(new GuiButton(0, 5, 5, 50, 20, new TextComponentTranslation("container.sim.sim_gui_BC_Done",new Object[0]).getUnformattedText()));
        if (this.page == 0) {
            //情感
            this.buttonList.add(new GuiButton(1, this.width / 2 - 50, 130, 100, 20, new TextComponentTranslation("container.sim.gui_Folk_Relationshipss",new Object[0]).getUnformattedText()));
            //需要
            this.buttonList.add(new GuiButton(1, this.width / 2 - 50, 150, 100, 20, new TextComponentTranslation("container.sim.gui_Folk_Needss",new Object[0]).getUnformattedText()));
            //特征
            this.buttonList.add(new GuiButton(1, width / 2 - 50, 170, 100, 20, new TextComponentTranslation("container.sim.guiFolk.Traits",new Object[0]).getUnformattedText()));
            //库存
//            this.buttonList.add(new GuiButton(1, width / 2 - 50, 190, 100, 20, new TextComponentTranslation("container.sim.guiFolk.Inventory",new Object[0]).getUnformattedText()));
        }else if (this.page == 1) {
            //返回
            this.buttonList.add(new GuiButton(1, 2, this.height - 42, 50, 20, new TextComponentTranslation("container.sim.gui_Folk_Back",new Object[0]).getUnformattedText()));
//            if (this.relOffset > 0) {
//                this.buttonList.add(new GuiButton(2, this.width / 2 - 125, 8, 20, 20, "<"));
//            }
//
//            int rels = this.folksRelationships.size();
//            if (rels - this.relOffset > 18) {
//                this.buttonList.add(new GuiButton(3, this.width / 2 + 105, 8, 20, 20, ">"));
//            }
        }else if (this.page == 2) {
            //返回
            this.buttonList.add(new GuiButton(1, 2, this.height - 42, 50, 20, new TextComponentTranslation("container.sim.gui_Folk_Back",new Object[0]).getUnformattedText()));
        } else if (this.page == 3) {
            //返回
            this.buttonList.add(new GuiButton(1, 2, this.height - 42, 50, 20, new TextComponentTranslation("container.sim.gui_Folk_Back",new Object[0]).getUnformattedText()));
        }


    }
    @Override
    public void drawScreen(int i, int j, float f) {
        try {


        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.renderEngine.bindTexture(new ResourceLocation(ModSim.MODID, "textures/gui/gui_folk.png"));
        int posX = (this.width - 256) / 2;
        int labelPos = this.width / 2 - 96;
        this.drawDefaultBackground();
        this.drawTexturedModalRect(posX, 5, 0, 0, 256, 256);
        if (this.page == 0) {
            //你好！这是我的信息：
            this.fontRenderer.drawString( new TextComponentTranslation("container.sim.gui_Folk_Here_my",new Object[0]).getUnformattedText(), this.width / 2, 17, 0x000000);
            //姓名
            this.fontRenderer.drawString( new TextComponentTranslation("container.sim.gui_Folk_Name",new Object[0]).getUnformattedText()+":", labelPos, 27, 0x000000);
            this.fontRenderer.drawString( this.folkName, this.width / 2, 27, 0x000000);
            //年龄
            this.fontRenderer.drawString( new TextComponentTranslation("container.sim.gui_Folk_Age",new Object[0]).getUnformattedText()+":", labelPos, 37, 0x000000);
            this.fontRenderer.drawString( String.valueOf(this.folkAge), this.width / 2, 37, 0x000000);
            //性别
            this.fontRenderer.drawString( new TextComponentTranslation("container.sim.gui_Folk_Gender",new Object[0]).getUnformattedText()+":", labelPos, 47, 0x000000);
            this.fontRenderer.drawString( this.folkGender == 0 ? new TextComponentTranslation("container.sim.gui_Folk_Male",new Object[0]).getUnformattedText() : new TextComponentTranslation("container.sim.gui_Folk_Female",new Object[0]).getUnformattedText(), this.width / 2, 47, 0x000000);
            //种族
            this.fontRenderer.drawString( new TextComponentTranslation("container.sim.gui_Folk_Species",new Object[0]).getUnformattedText()+":", labelPos, 57, 0x000000);
            this.fontRenderer.drawString( this.folkRaceName, this.width / 2, 57, 0x000000);
            //工作
            this.fontRenderer.drawString( new TextComponentTranslation("container.sim.gui_Folk_Job",new Object[0]).getUnformattedText()+":", labelPos, 67, 0x000000);
            this.fontRenderer.drawString( this.jobName, this.width / 2, 67, 0x000000);
            //住房状态
            this.fontRenderer.drawString( new TextComponentTranslation("container.sim.gui_Folk_Housing_status",new Object[0]).getUnformattedText()+":", labelPos, 77, 0x000000);
            this.fontRenderer.drawString( this.housingStatus, this.width / 2, 77, 0x000000);
            //建筑等级
            this.fontRenderer.drawString( new TextComponentTranslation("container.sim.gui_Folk_Building_skill",new Object[0]).getUnformattedText()+":", labelPos, 87, 0x000000);

            double w = 128 * ((double) (Double.valueOf(this.building) % 1 * 1000.0F) / 1000);
            this.drawGradientRect(this.width / 2, 87, (int) w + this.width / 2, 96, -125, -115);
            this.fontRenderer.drawString( this.building, this.width / 2, 87, 0x000000);
            //农耕等级
            this.fontRenderer.drawString( new TextComponentTranslation("container.sim.gui_Folk_Farming_Skill",new Object[0]).getUnformattedText()+":", labelPos, 97, 0x000000);

            double far = 128 * ((double) (Double.valueOf(this.farming) % 1 * 1000.0F) / 1000);
            this.drawGradientRect(this.width / 2, 97, (int) far + this.width / 2, 106, -125, -115);
            this.fontRenderer.drawString(this.farming, this.width / 2, 97, 0x000000);
            //挖矿等级
            this.fontRenderer.drawString( new TextComponentTranslation("container.sim.gui_Folk_Mining_skill",new Object[0]).getUnformattedText()+":", labelPos, 107, 0x000000);

            double min = 128 * ((double) (Double.valueOf(this.mining) % 1 * 1000.0F) / 1000);
            this.drawGradientRect(this.width / 2, 107, (int) min + this.width / 2, 116, -125, -115);
            this.fontRenderer.drawString(this.mining, this.width / 2, 107, 0x000000);
            //怀孕阶段
            if (this.pregnancyStage > 0.0F) {
                //医疗状况
                this.fontRenderer.drawString(new TextComponentTranslation("container.sim.gui_Folk_Medical_status",new Object[0]).getUnformattedText(), labelPos, 117, 0x000000);
                String days = (int) (this.pregnancyStage * 9.0F) + "";
                if (days.contentEquals("0")) {
                    //孕
                    days = new TextComponentTranslation("container.sim.gui_Folk_Pregnant",new Object[0]).getUnformattedText();
                } else if (days.contentEquals("1")) {
                    //怀孕一天
                    days = new TextComponentTranslation("container.sim.gui_Folk_day_pregnant",new Object[0]).getUnformattedText();
                } else {
                    //怀孕天数
                    String lang = FMLCommonHandler.instance().getCurrentLanguage();
                    if ("en_US".equals(lang)) {
                        days = days + new TextComponentTranslation("container.sim.gui_Folk_days_pregnant",new Object[0]).getUnformattedText();
                    }else{
                        days = new TextComponentTranslation("container.sim.gui_Folk_days_pregnant",new Object[0]).getUnformattedText()+days;
                    }

                }

                this.fontRenderer.drawString(days, this.width / 2, 117, 128);
            }
        //关系
        } else if(this.page == 1) {
            //的关系
            this.fontRenderer.drawString( new TextComponentTranslation("container.sim.gui_Folk_Relationshipss",new Object[0]).getUnformattedText()+":", this.width / 2, 17, 0x000000);
            int height = 50;
            String[] var7 = this.relationshipData.split(";");
            int var8 = var7.length;
            for (int var9 = 0; var9 < var8; ++var9) {
                String relDat = var7[var9];
                if(!StringUtils.isNullOrEmpty(relDat)){
                    this.fontRenderer.drawString( relDat, labelPos, height, 0x000000);
                    height += 15;
                }
            }
        }else if(this.page == 2) {
            //XX的需要
            this.fontRenderer.drawString( this.folkName + new TextComponentTranslation("container.sim.gui_Folk_Needs",new Object[0]).getUnformattedText(), this.width / 2, 10, 0x000000);
            //当前状态
            this.fontRenderer.drawString(new TextComponentTranslation("container.sim.gui_Folk_Status",new Object[0]).getUnformattedText() + ":", this.width / 3, 20, 0);
            this.fontRenderer.drawString(this.status, this.width / 2, 20, 128);
            //饥饿状态
            this.fontRenderer.drawString(new TextComponentTranslation("container.sim.gui_Folk_Hunger",new Object[0]).getUnformattedText() + ":", this.width / 3, 30, 0);
            this.fontRenderer.drawString(this.hungerStatus, this.width / 2, 30, 128);

           /* //乐趣
            this.fontRenderer.drawString(new TextComponentTranslation("container.sim.gui_Folk_Fun",new Object[0]).getUnformattedText() + ":", this.width / 3, 30, 0);
            this.fontRenderer.drawString(this.funStatus, this.width / 2, 30, 128);
            //社交
            fontRenderer.drawString(new TextComponentTranslation("container.sim.guiFolk.Social",new Object[0]).getUnformattedText() + ":", this.width / 3, 40, 0x000000);
            fontRenderer.drawString(theFolk.socialStatus, this.width / 2, 40, 0x000080);
            //环境
            fontRenderer.drawString(new TextComponentTranslation("container.sim.guiFolk.Environment",new Object[0]).getUnformattedText() + ":", this.width / 3, 50, 0x000000);
            fontRenderer.drawString(theFolk.environmentStatus, this.width / 2, 50, 0x000080);*/
        }else if (this.page == 3) {
            int left = this.width / 2 - 120;
            //特征
            this.fontRenderer.drawString(this.folkTrait1+":", left, 30, 0x000000,false);
            int trd1 = this.folkTraitDesc1.length();
            int fs_y=40;
            if (trd1 > 28) {
                int z=0;
                for (int k = 0; k < trd1; k++) {
                    String trds1;
                    if (z+28 >= trd1) {
                        trds1=this.folkTraitDesc1.substring(z,trd1-1);
                        this.fontRenderer.drawString(trds1, left, fs_y, 1677800,false);
                        break;
                    }else{
                        trds1=this.folkTraitDesc1.substring(z,z+28);
                        this.fontRenderer.drawString(trds1, left, fs_y, 1677800,false);
                    }
                    fs_y=fs_y+8;
                    z+=28;
                }
            }else{
                String trds1=this.folkTraitDesc1;
                this.fontRenderer.drawString(trds1, left, fs_y, 1677800,false);
            }
            fs_y=fs_y+8;
            this.fontRenderer.drawString(this.folkTrait2+":", left, fs_y, 0x000000);
            fs_y=fs_y+8;
            int trd2 = this.folkTraitDesc2.length();
            if (trd2 > 28) {
                int z=0;
                for (int k = 0; k < trd2; k++) {
                    String trds1;
                    if (z+28 >= trd2) {
                        trds1=this.folkTraitDesc2.substring(z,trd2-1);
                        this.fontRenderer.drawString(trds1, left, fs_y, 1677800,false);
                        break;
                    }else{
                        trds1=this.folkTraitDesc2.substring(z,z+28);
                        this.fontRenderer.drawString(trds1, left, fs_y, 1677800,false);
                    }
                    fs_y=fs_y+8;
                    z+=28;
                }
            }else{
                String trds1=this.folkTraitDesc2;
                this.fontRenderer.drawString(trds1, left, fs_y, 1677800,false);
            }
//            this.fontRenderer.drawString(this.folkTraitDesc2, left, fs_y, 0x000000,false);
            fs_y=fs_y+8;
            this.fontRenderer.drawString(this.folkTrait3+":", left, fs_y, 0x000000);
            fs_y=fs_y+8;
            int trd3 = this.folkTraitDesc3.length();
            if (trd3 > 28) {
                int z=0;
                for (int k = 0; k < trd3; k++) {
                    String trds1;
                    if (z+28 >= trd3) {
                        trds1=this.folkTraitDesc3.substring(z,trd3-1);
                        this.fontRenderer.drawString(trds1, left, fs_y, 1677800,false);
                        break;
                    }else{
                        trds1=this.folkTraitDesc3.substring(z,z+28);
                        this.fontRenderer.drawString(trds1, left, fs_y, 1677800,false);
                    }
                    fs_y=fs_y+8;
                    z+=28;
                }
            }else{
                String trds1=this.folkTraitDesc3;
                this.fontRenderer.drawString(trds1, left, fs_y, 1677800,false);
            }
//            this.fontRenderer.drawString(this.folkTraitDesc3, left, fs_y, 0x000000,false);
        }
        }catch (Exception e){
            e.printStackTrace();
        }
        super.drawScreen(i, j, f);
    }
    @Override
    public void actionPerformed(GuiButton button) {
        if (button.enabled) {
            if (button.id == 0) {
                this.mc.currentScreen = null;
                this.mc.setIngameFocus();
            }
            if (this.page == 0) {
                //关系
                if (button.displayString.contentEquals(new TextComponentTranslation("container.sim.gui_Folk_Relationshipss",new Object[0]).getUnformattedText())) {
                    this.page = 1;
                    this.showPage();
                }
                //需要
                if (button.displayString.contentEquals(new TextComponentTranslation("container.sim.gui_Folk_Needss",new Object[0]).getUnformattedText())) {
                    this.page = 2;
                    this.showPage();
                }
                //特征
                if (button.displayString.contentEquals(new TextComponentTranslation("container.sim.guiFolk.Traits",new Object[0]).getUnformattedText())) {
                    this.page = 3;
                    this.showPage();
                }
                //库存
                if (button.displayString.contentEquals(new TextComponentTranslation("container.sim.guiFolk.Inventory",new Object[0]).getUnformattedText())) {
                    NpcData npcData = ModSimLoader.getFolkDataByUID(uid);
                    // P15(S1): 原来把“打开客户端 GUI”的包发给服务端，服务端再调用
                    // Minecraft.getMinecraft()，专服必崩。GUI 直接在客户端打开。
                    if (npcData != null) {
                        Minecraft.getMinecraft().displayGuiScreen(new GuiFolkInventory(uid));
                    }
                }
            } else if (this.page == 1) {
                //情感分页  返回
                if (button.displayString.contentEquals(new TextComponentTranslation("container.sim.gui_Folk_Back",new Object[0]).getUnformattedText())) {
                    this.page = 0;
                    this.showPage();
                }

                if (button.displayString.contentEquals("<")) {
//                    this.relOffset -= 18;
//                    if (this.relOffset < 0) {
//                        this.relOffset = 0;
//                    }

                    this.showPage();
                }

                if (button.displayString.contentEquals(">")) {
//                    this.relOffset += 18;
                    this.showPage();
                }
            } else if (this.page == 2) {
                //需求分页 返回
                if (button.displayString.contentEquals(new TextComponentTranslation("container.sim.gui_Folk_Back",new Object[0]).getUnformattedText())) {
                    this.page = 0;
                    this.showPage();
                }

            } else if (page == 3) {
                // 特征 page
                if (button.displayString.contentEquals(new TextComponentTranslation("container.sim.gui_Folk_Back",new Object[0]).getUnformattedText())) {
                    this.page = 0;
                    this.showPage();
                }
            }

        }
    }
}
