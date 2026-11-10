package com.trhsy.sim.gui.block;

import com.trhsy.sim.gui.GuiLayout;
import com.trhsy.sim.gui.GuiStyle;
import com.trhsy.sim.loader.ModSimClientLoader;
import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.loader.NetWorkLoader;
import com.trhsy.sim.network.server.PacketFireFolk;
import com.trhsy.sim.network.server.PacketGetHireableFolks;
import com.trhsy.sim.network.server.PacketHireFolk;
import com.trhsy.sim.network.server.PacketUpdateMineBox;
import com.trhsy.sim.npcCode.NpcIdentity;
import com.trhsy.sim.npcCode.V3;
import com.trhsy.sim.npcCode.economy.Economy;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.UUID;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.gui.block
 * @ClassName: GuiBlockMineBlock
 * @Description:
 * @date 2023/5/16 18:07
 */
public class GuiBlockMineBlock extends GuiScreen {
    //id
    public UUID id;
    //方向
    public EnumFacing facing;
    public int x;
    public int z;
    //地点
    public V3 loc;
    private int mouseCount;
    public boolean hasEmployee;
    public NpcIdentity employee;
    public NpcIdentity[] hireableFolkNames;
    int currentPage;
    GuiButton selectedEmployee;

    public GuiBlockMineBlock(UUID id, V3 loc, EnumFacing facing, int x, int z) {
        this.facing = EnumFacing.EAST;
        this.mouseCount = 0;
        this.hasEmployee = false;
        this.hireableFolkNames = new NpcIdentity[1000];
        this.currentPage = 0;
        this.id = id;
        this.loc = loc;
        this.facing = facing;
        this.x = x;
        this.z = z;
        this.getHireableFolkNames();
        this.setDimensions();
    }

    public GuiBlockMineBlock(UUID id, V3 loc, EnumFacing facing, int x, int z, NpcIdentity folk) {
        this.facing = EnumFacing.EAST;
        this.mouseCount = 0;
        this.hasEmployee = false;
        this.hireableFolkNames = new NpcIdentity[1000];
        this.currentPage = 0;
        this.id = id;
        this.loc = loc;
        this.facing = facing;
        this.x = x;
        this.z = z;
        this.employee = folk;
        this.getHireableFolkNames();
        this.setDimensions();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
    @Override
    public void onGuiClosed() {
        ModSimClientLoader.previewPos1 = null;
        ModSimClientLoader.previewPos2 = null;
    }
    @Override
    public void initGui() {
        this.showPage();
        super.initGui();
    }

    public void showPage() {
        this.buttonList.clear();
        int px = left();
        int py = top();
        int pw = panelW();
        int ph = panelH();
        //完成
        this.buttonList.add(new GuiButton(0, px + pw - 68, py + ph - 26, 60, 20, new TextComponentTranslation("container.sim.sim_gui_BC_Done", new Object[0]).getUnformattedText()));

        if (this.currentPage == 0) {
            if (this.employee == null) {
                this.buttonList.add(new GuiButton(1, px + 8, py + 126, 150, 20, new TextComponentTranslation("container.sim.Hire21", new Object[0]).getUnformattedText()));
            } else {
                this.buttonList.add(new GuiButton(1, px + 8, py + 126, 150, 20, new TextComponentTranslation("container.sim.Fire", new Object[0]).getUnformattedText() + " " + this.employee.name));
            }
            this.buttonList.add(new GuiButton(2, px + 46, py + 68, 124, 20, this.facingName()));
            this.buttonList.add(new GuiButton(4, px + 186, py + 68, 20, 20, "-"));
            this.buttonList.add(new GuiButton(5, px + 268, py + 68, 20, 20, "+"));
            this.buttonList.add(new GuiButton(6, px + 186, py + 96, 20, 20, "-"));
            this.buttonList.add(new GuiButton(7, px + 268, py + 96, 20, 20, "+"));
        } else if (this.currentPage == 1) {
            this.buttonList.add(new GuiButton(1000, px + 8, py + ph - 26, 90, 20, new TextComponentTranslation("container.sim.sim_gui_player_to_Cancel", new Object[0]).getUnformattedText()));
            this.buttonList.add(new GuiButton(1001, px + pw - 98, py + ph - 26, 90, 20, new TextComponentTranslation("container.sim.gui_btn_name_OK", new Object[0]).getUnformattedText()));
            int btnW = (pw - 24) / 2;
            int colGap = pw - 16 - btnW * 2;
            int startY = py + 48;
            int rowH = 22;
            int max = Math.min(ModSimClientLoader.getUnemployedFolks().size(), 12);
            for (int i = 0; i < max; i++) {
                NpcIdentity folk = ModSimClientLoader.getUnemployedFolks().get(i);
                int col = i % 2;
                int row = i / 2;
                this.buttonList.add(new GuiButton(100 + i, px + 8 + col * (btnW + colGap), startY + row * rowH, btnW, 20, folk.name));
                this.hireableFolkNames[100 + i] = folk;
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (this.mouseCount < 10) {
            ++this.mouseCount;
            Mouse.setGrabbed(false);
        }
        try {
            this.drawDefaultBackground();
            int px = left();
            int py = top();
            int pw = panelW();
            int ph = panelH();
            GuiStyle.panel(px, py, pw, ph, new TextComponentTranslation("container.sim.gui_mine2", new Object[0]).getUnformattedText(), this.fontRenderer);
            if (this.currentPage == 0) {
                int gap = 4;
                int cardW = (pw - 16 - gap * 3) / 4;
                int cardsY = py + 24;
                String[] labels = new String[]{
                        new TextComponentTranslation("container.sim.gui.farm.facing", new Object[0]).getUnformattedText(),
                        new TextComponentTranslation("container.sim.gui.farm.size", new Object[0]).getUnformattedText(),
                        new TextComponentTranslation("container.sim.gui.farm.employee", new Object[0]).getUnformattedText(),
                        new TextComponentTranslation("container.sim.sim_gui_BC6", new Object[0]).getUnformattedText()
                };
                String[] values = new String[]{
                        this.facingName(),
                        this.x + " x " + this.z,
                        this.employee == null ? new TextComponentTranslation("container.sim.gui.employees.none", new Object[0]).getUnformattedText() : this.employee.name,
                        ModSimLoader.displayMoney(this.getUpgradeCost())
                };
                for (int i = 0; i < 4; i++) {
                    int x = px + 8 + i * (cardW + gap);
                    GuiStyle.inset(x, cardsY, cardW, 34);
                    GuiStyle.text(this.fontRenderer, x + 4, cardsY + 4, labels[i], GuiStyle.TEXT_MUTED);
                    GuiStyle.text(this.fontRenderer, x + 4, cardsY + 17, GuiLayout.truncate(this.fontRenderer, values[i], cardW - 8), GuiStyle.TEXT);
                }

                GuiStyle.text(this.fontRenderer, px + 8, py + 74, new TextComponentTranslation("container.sim.gui.farm.facing", new Object[0]).getUnformattedText(), GuiStyle.TEXT_MUTED);
                GuiStyle.text(this.fontRenderer, px + 186, py + 74, new TextComponentTranslation("container.sim.job_farmer_Length", new Object[0]).getUnformattedText(), GuiStyle.TEXT_MUTED);
                GuiStyle.text(this.fontRenderer, px + 186, py + 102, new TextComponentTranslation("container.sim.job_farmer_Width", new Object[0]).getUnformattedText(), GuiStyle.TEXT_MUTED);
                GuiStyle.text(this.fontRenderer, px + 210, py + 74, String.valueOf(this.x), GuiStyle.TEXT);
                GuiStyle.text(this.fontRenderer, px + 210, py + 102, String.valueOf(this.z), GuiStyle.TEXT);

                if (this.employee != null) {
                    String line = new TextComponentTranslation("container.sim.gui.money", new Object[0]).getUnformattedText() + " "
                            + ModSimLoader.displayMoney(this.employee.money) + "  "
                            + new TextComponentTranslation("container.sim.gui.wage", new Object[0]).getUnformattedText() + " "
                            + ModSimLoader.displayMoney(Economy.WAGE_PER_MINUTE) + new TextComponentTranslation("container.sim.gui.per_minute", new Object[0]).getUnformattedText();
                    GuiStyle.text(this.fontRenderer, px + 8, py + 152, GuiLayout.truncate(this.fontRenderer, line, 160), GuiStyle.GREEN);
                }
            } else {
                GuiStyle.text(this.fontRenderer, px + 8, py + 28, new TextComponentTranslation("container.sim.sim_gui_BC_unemployed", new Object[0]).getUnformattedText(), GuiStyle.TEXT_MUTED);
                if (this.selectedEmployee != null) {
                    GuiStyle.text(this.fontRenderer, px + 8, py + ph - 42, new TextComponentTranslation("container.sim.gui.selected", new Object[0]).getUnformattedText() + " " + this.selectedEmployee.displayString, GuiStyle.GREEN);
                }
            }
            super.drawScreen(mouseX, mouseY, partialTicks);
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("矿井GUI屏幕绘制出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    private int panelW() {
        return Math.min(340, this.width - 16);
    }

    private int panelH() {
        return Math.min(230, this.height - 16);
    }

    private int left() {
        return GuiLayout.centerX(this.width, panelW());
    }

    private int top() {
        return GuiLayout.centerY(this.height, panelH());
    }

    private String facingName() {
        if (this.facing == EnumFacing.WEST) {
            return new TextComponentTranslation("container.sim.gui_west", new Object[0]).getUnformattedText();
        }
        if (this.facing == EnumFacing.EAST) {
            return new TextComponentTranslation("container.sim.gui_east", new Object[0]).getUnformattedText();
        }
        if (this.facing == EnumFacing.NORTH) {
            return new TextComponentTranslation("container.sim.gui_north", new Object[0]).getUnformattedText();
        }
        if (this.facing == EnumFacing.SOUTH) {
            return new TextComponentTranslation("container.sim.gui_south", new Object[0]).getUnformattedText();
        }
        return "-";
    }

    @Override
    public void actionPerformed(GuiButton guibutton) {
        if (guibutton.enabled) {
            if (guibutton.id == 0) {
                this.mc.currentScreen = null;
                this.mc.setIngameFocus();
                this.onGuiClosed();
            } else {
                //雇佣矿工
                if (guibutton.displayString.contentEquals(new TextComponentTranslation("container.sim.Hire21",new Object[0]).getUnformattedText())) {
                    this.currentPage = 1;
                    this.showPage();
                    //解雇
                } else if (guibutton.displayString.startsWith(new TextComponentTranslation("container.sim.Fire",new Object[0]).getUnformattedText()+" ")) {
                    NetWorkLoader.net.sendToServer(new PacketFireFolk(this.employee.id));
                    guibutton.enabled = false;
                    this.mc.currentScreen = null;
                    this.mc.setIngameFocus();
                    this.onGuiClosed();
                } else if (guibutton.id == 2) {
                    this.facing = this.facing.rotateY();
                    String fs_facing = new TextComponentTranslation("container.sim.gui_west",new Object[0]).getUnformattedText();
                    //西
                    if (this.facing.toString().equals("west")) {
                        fs_facing=new TextComponentTranslation("container.sim.gui_west",new Object[0]).getUnformattedText();
                        //东
                    } else if (this.facing.toString().equals("east")) {
                        fs_facing=new TextComponentTranslation("container.sim.gui_east",new Object[0]).getUnformattedText();
                        //北
                    } else if (this.facing.toString().equals("north")) {
                        fs_facing=new TextComponentTranslation("container.sim.gui_north",new Object[0]).getUnformattedText();
                        //南
                    } else if (this.facing.toString().equals("south")) {
                        fs_facing=new TextComponentTranslation("container.sim.gui_south",new Object[0]).getUnformattedText();
                    }
                    guibutton.displayString = fs_facing;
                    this.updateMine();
                } else if (guibutton.id == 3) {
                    float cash = 5.0F;
                    if (this.getUpgradeCost() > cash) {
                        //资金不足
                        guibutton.displayString = new TextComponentTranslation("container.sim.gui_Farming_text_NOT_ENOUGH",new Object[0]).getUnformattedText();
                        guibutton.enabled = false;
                    } else {
                        if (this.x >= 4 && this.z >= 4) {
                            this.mc.currentScreen = null;
                            this.mc.setIngameFocus();
                            this.onGuiClosed();
                            return;
                        }
                        //太小
                        guibutton.displayString = new TextComponentTranslation("container.sim.gui_mine1",new Object[0]).getUnformattedText();
                        guibutton.enabled = false;
                    }
                }

                if (this.currentPage == 0) {
                    if (guibutton.id == 4 && this.x > 6) {
                        --this.x;
                        this.showPage();
                        this.updateMine();
                    } else if (guibutton.id == 5 && this.x < 32) {
                        ++this.x;
                        this.showPage();
                        this.updateMine();
                    } else if (guibutton.id == 6 && this.z > 6) {
                        --this.z;
                        this.showPage();
                        this.updateMine();
                    } else if (guibutton.id == 7 && this.z < 32) {
                        ++this.z;
                        this.showPage();
                        this.updateMine();
                    }
                }

                if (this.currentPage == 1) {
                    if (guibutton.id == 1000) {
                        this.currentPage = 0;
                        this.showPage();
                    }

                    if (guibutton.id == 1001 && this.selectedEmployee != null) {
                        this.hasEmployee = true;
                        this.employee = this.hireableFolkNames[this.selectedEmployee.id];
                        this.currentPage = 0;
                        this.showPage();
                        //矿工
                        NetWorkLoader.net.sendToServer(new PacketHireFolk(this.employee.id, new TextComponentTranslation("container.sim.Vocation4",new Object[0]).getUnformattedText(), new V3(this.loc.x,this.loc.y+1,this.loc.z)));
                    }

                    if (guibutton.id > 99 && guibutton.id < 1000) {
                        if (this.selectedEmployee != null) {
                            this.selectedEmployee.enabled = true;
                        }

                        this.selectedEmployee = guibutton;
                        this.selectedEmployee.enabled = false;
                    }
                }

            }
        }
    }

    private void updateMine() {
        this.setDimensions();
        NetWorkLoader.net.sendToServer(new PacketUpdateMineBox(this.id, this.x, this.z, this.facing));
    }

    private Float getUpgradeCost() {
        Float ret = (float)(this.x * this.z);
        ret = ret / 15.0F;
        return ret;
    }

    public void setDimensions() {
        ModSimClientLoader.previewPos1 = new Vec3d(this.loc.toBlockPos().offset(this.facing));
        ModSimClientLoader.previewPos2 = (new Vec3d(this.loc.toBlockPos().offset(this.facing, this.x + 1).offset(this.facing.rotateY(), this.z))).add(new Vec3d(0.0D, 2.0D, 0.0D));
    }

    public void getHireableFolkNames() {
        NetWorkLoader.net.sendToServer(new PacketGetHireableFolks(true));
    }

    @Override
    public void keyTyped(char c, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.mc.currentScreen = null;
            this.mc.setIngameFocus();
        }
    }
}
