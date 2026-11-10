package com.trhsy.sim.gui.npc;

import com.trhsy.sim.gui.GuiLayout;
import com.trhsy.sim.gui.GuiStyle;
import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.network.client.PacketOpenFolkGui;
import com.trhsy.sim.npcCode.NpcData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.TextComponentTranslation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * @ClassName GuiFolk
 * @Description NPC 资料界面（原版像素风）
 * @Author TRHSY
 * @Date 2022/10/18 12:55
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
    float money;
    float wage;
    String jobId = "";
    int page = 0;
    int relOffset = 0;
    int traitScroll = 0;
    UUID uid;

    public GuiFolk(PacketOpenFolkGui message) {
        this.uid = message.uid;
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
        this.pregnancyStage = message.pregnancyStage;
        this.money = message.money;
        this.wage = message.wage;
        this.jobId = message.jobId == null ? "" : message.jobId;
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

    private int panelW() {
        return Math.min(340, this.width - 16);
    }

    private int panelH() {
        return Math.min(250, this.height - 16);
    }

    private int left() {
        return GuiLayout.centerX(this.width, panelW());
    }

    private int top() {
        return GuiLayout.centerY(this.height, panelH());
    }

    public void showPage() {
        this.buttonList.clear();
        int px = left();
        int py = top();
        int pw = panelW();
        int ph = panelH();
        //完成
        this.buttonList.add(new GuiButton(0, px + pw - 58, py + 4, 50, 18, new TextComponentTranslation("container.sim.sim_gui_BC_Done", new Object[0]).getUnformattedText()));

        String[] tabs = new String[]{
                new TextComponentTranslation("container.sim.gui.folk.tab.overview", new Object[0]).getUnformattedText(),
                new TextComponentTranslation("container.sim.gui_Folk_Relationshipss", new Object[0]).getUnformattedText(),
                new TextComponentTranslation("container.sim.gui_Folk_Needss", new Object[0]).getUnformattedText(),
                new TextComponentTranslation("container.sim.guiFolk.Traits", new Object[0]).getUnformattedText(),
                new TextComponentTranslation("container.sim.guiFolk.Inventory", new Object[0]).getUnformattedText()
        };
        int tabGap = 4;
        int tabW = (pw - 16 - tabGap * 4) / 5;
        int tabY = py + ph - 26;
        for (int i = 0; i < tabs.length; i++) {
            GuiButton button = new GuiButton(10 + i, px + 8 + i * (tabW + tabGap), tabY, tabW, 20, tabs[i]);
            if (i == this.page) {
                button.enabled = false;
            }
            this.buttonList.add(button);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        int px = left();
        int py = top();
        int pw = panelW();
        int ph = panelH();
        String title = new TextComponentTranslation("container.sim.gui.folk.title", new Object[0]).getUnformattedText() + " · " + this.folkName;
        GuiStyle.panel(px, py, pw, ph, GuiLayout.truncate(this.fontRenderer, title, pw - 110), this.fontRenderer);

        if (this.page == 0) {
            this.drawOverview(px, py, pw, ph);
        } else if (this.page == 1) {
            this.drawRelationships(px, py, pw, ph);
        } else if (this.page == 2) {
            this.drawNeeds(px, py, pw, ph);
        } else if (this.page == 3) {
            this.drawTraits(px, py, pw, ph);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawOverview(int px, int py, int pw, int ph) {
        int cardsY = py + 26;
        int gap = 4;
        int cardW = (pw - 16 - gap * 3) / 4;
        String[] labels = new String[]{
                new TextComponentTranslation("container.sim.gui_Folk_Job", new Object[0]).getUnformattedText(),
                new TextComponentTranslation("container.sim.gui_Folk_Status", new Object[0]).getUnformattedText(),
                new TextComponentTranslation("container.sim.gui.money", new Object[0]).getUnformattedText(),
                new TextComponentTranslation("container.sim.gui.wage", new Object[0]).getUnformattedText()
        };
        String[] values = new String[]{
                this.jobName,
                this.status,
                ModSimLoader.displayMoney(this.money),
                formatMoney(this.wage) + new TextComponentTranslation("container.sim.gui.per_minute", new Object[0]).getUnformattedText()
        };
        int[] colors = new int[]{GuiStyle.TEXT, GuiStyle.GREEN, this.money < 0 ? GuiStyle.RED : GuiStyle.GREEN, GuiStyle.TEXT};
        for (int i = 0; i < 4; i++) {
            int x = px + 8 + i * (cardW + gap);
            GuiStyle.inset(x, cardsY, cardW, 34);
            GuiStyle.text(this.fontRenderer, x + 4, cardsY + 4, labels[i], GuiStyle.TEXT_MUTED);
            GuiStyle.text(this.fontRenderer, x + 4, cardsY + 17, GuiLayout.truncate(this.fontRenderer, values[i], cardW - 8), colors[i]);
        }

        int leftX = px + 8;
        int valueX = px + 52;
        int detailsY = py + 68;
        GuiStyle.labelValue(this.fontRenderer, leftX, valueX, detailsY, new TextComponentTranslation("container.sim.gui_Folk_Age", new Object[0]).getUnformattedText(), String.valueOf(this.folkAge), GuiStyle.TEXT);
        GuiStyle.labelValue(this.fontRenderer, leftX, valueX, detailsY + 14, new TextComponentTranslation("container.sim.gui_Folk_Gender", new Object[0]).getUnformattedText(), this.folkGender == 0 ? new TextComponentTranslation("container.sim.gui_Folk_Male", new Object[0]).getUnformattedText() : new TextComponentTranslation("container.sim.gui_Folk_Female", new Object[0]).getUnformattedText(), GuiStyle.TEXT);
        GuiStyle.labelValue(this.fontRenderer, leftX, valueX, detailsY + 28, new TextComponentTranslation("container.sim.gui_Folk_Species", new Object[0]).getUnformattedText(), this.folkRaceName, GuiStyle.TEXT);
        GuiStyle.labelValue(this.fontRenderer, leftX, valueX, detailsY + 42, new TextComponentTranslation("container.sim.gui_Folk_Housing_status", new Object[0]).getUnformattedText(), this.housingStatus, GuiStyle.TEXT);
        GuiStyle.labelValue(this.fontRenderer, leftX, valueX, detailsY + 56, new TextComponentTranslation("container.sim.gui_Folk_Hunger", new Object[0]).getUnformattedText(), this.hungerStatus, GuiStyle.TEXT);

        this.drawSkill(leftX, py + 140, 0, new TextComponentTranslation("container.sim.gui_Folk_Building_skill", new Object[0]).getUnformattedText(), this.building);
        this.drawSkill(leftX, py + 154, 1, new TextComponentTranslation("container.sim.gui_Folk_Farming_Skill", new Object[0]).getUnformattedText(), this.farming);
        this.drawSkill(leftX, py + 168, 2, new TextComponentTranslation("container.sim.gui_Folk_Mining_skill", new Object[0]).getUnformattedText(), this.mining);

        int leftW = Math.max(150, (pw - 16) * 55 / 100);
        int rx = px + 8 + leftW + 6;
        int rw = px + pw - 8 - rx;
        GuiStyle.inset(rx, py + 68, rw, 118);
        GuiStyle.text(this.fontRenderer, rx + 5, py + 73, new TextComponentTranslation("container.sim.gui.current_job", new Object[0]).getUnformattedText(), GuiStyle.TEXT_MUTED);
        GuiStyle.text(this.fontRenderer, rx + 5, py + 85, GuiLayout.truncate(this.fontRenderer, this.jobName, rw - 10), GuiStyle.TEXT);
        GuiStyle.divider(rx + 5, py + 100, rw - 10);
        GuiStyle.text(this.fontRenderer, rx + 5, py + 105, new TextComponentTranslation("container.sim.gui.wage", new Object[0]).getUnformattedText(), GuiStyle.TEXT_MUTED);
        GuiStyle.text(this.fontRenderer, rx + 5, py + 117, formatMoney(this.wage) + new TextComponentTranslation("container.sim.gui.per_minute", new Object[0]).getUnformattedText(), this.wage > 0 ? GuiStyle.GREEN : GuiStyle.TEXT_MUTED);
        GuiStyle.text(this.fontRenderer, rx + 5, py + 133, new TextComponentTranslation("container.sim.gui.money", new Object[0]).getUnformattedText(), GuiStyle.TEXT_MUTED);
        GuiStyle.money(this.fontRenderer, rx + 5, py + 145, this.money);
        GuiStyle.text(this.fontRenderer, rx + 5, py + 161, new TextComponentTranslation("container.sim.gui_Folk_Status", new Object[0]).getUnformattedText(), GuiStyle.TEXT_MUTED);
        GuiStyle.text(this.fontRenderer, rx + 5, py + 173, GuiLayout.truncate(this.fontRenderer, this.status, rw - 10), GuiStyle.GREEN);
    }

    private void drawSkill(int x, int y, int index, String label, String rawValue) {
        float skill = parseSkill(rawValue);
        float ratio = Math.max(0.0F, Math.min(1.0F, skill / 10.0F));
        GuiStyle.text(this.fontRenderer, x, y, label, GuiStyle.TEXT_MUTED);
        int barX = x + 48;
        GuiStyle.progressBar(barX, y + 1, 96, 7, ratio, GuiStyle.GREEN);
        GuiStyle.text(this.fontRenderer, barX + 102, y, String.format(Locale.ROOT, "%.1f", skill), GuiStyle.TEXT);
    }

    private void drawRelationships(int px, int py, int pw, int ph) {
        String heading = this.folkName + new TextComponentTranslation("container.sim.gui_Folk_Relationshipss", new Object[0]).getUnformattedText();
        GuiStyle.text(this.fontRenderer, px + 8, py + 28, heading, GuiStyle.TEXT);
        List<String> lines = new ArrayList<String>();
        if (this.relationshipData != null) {
            for (String line : this.relationshipData.split(";")) {
                if (line != null && !line.trim().isEmpty()) {
                    lines.add(line.trim());
                }
            }
        }
        int contentTop = py + 44;
        int contentBottom = py + ph - 32;
        int rowH = 13;
        int maxRows = Math.max(1, (contentBottom - contentTop) / rowH);
        if (this.relOffset > Math.max(0, lines.size() - maxRows)) {
            this.relOffset = Math.max(0, lines.size() - maxRows);
        }
        if (lines.isEmpty()) {
            GuiStyle.text(this.fontRenderer, px + 12, contentTop + 4, new TextComponentTranslation("container.sim.gui.folk.no_relationships", new Object[0]).getUnformattedText(), GuiStyle.TEXT_MUTED);
            return;
        }
        GuiLayout.scissor(px + 8, contentTop, pw - 16, contentBottom - contentTop, () -> {
            for (int i = this.relOffset; i < lines.size() && i < this.relOffset + maxRows; i++) {
                int y = contentTop + (i - this.relOffset) * rowH;
                GuiStyle.row(px + 8, y - 2, pw - 16, rowH, (i - this.relOffset) % 2 == 1);
                GuiStyle.text(this.fontRenderer, px + 12, y, GuiLayout.truncate(this.fontRenderer, lines.get(i), pw - 28), GuiStyle.TEXT);
            }
        });
        this.drawScrollbar(px + pw - 9, contentTop, contentBottom - contentTop, lines.size(), maxRows, this.relOffset);
    }

    private void drawNeeds(int px, int py, int pw, int ph) {
        String heading = this.folkName + new TextComponentTranslation("container.sim.gui_Folk_Needs", new Object[0]).getUnformattedText();
        GuiStyle.text(this.fontRenderer, px + 8, py + 28, heading, GuiStyle.TEXT);
        int labelX = px + 12;
        int valueX = px + 88;
        int y = py + 50;
        GuiStyle.labelValue(this.fontRenderer, labelX, valueX, y, new TextComponentTranslation("container.sim.gui_Folk_Status", new Object[0]).getUnformattedText(), this.status, GuiStyle.GREEN);
        y += 18;
        GuiStyle.labelValue(this.fontRenderer, labelX, valueX, y, new TextComponentTranslation("container.sim.gui_Folk_Hunger", new Object[0]).getUnformattedText(), this.hungerStatus, GuiStyle.TEXT);
        y += 18;
        GuiStyle.labelValue(this.fontRenderer, labelX, valueX, y, new TextComponentTranslation("container.sim.gui_Folk_Job", new Object[0]).getUnformattedText(), this.jobName, GuiStyle.TEXT);
        y += 18;
        GuiStyle.labelValue(this.fontRenderer, labelX, valueX, y, new TextComponentTranslation("container.sim.gui.money", new Object[0]).getUnformattedText(), ModSimLoader.displayMoney(this.money), this.money < 0 ? GuiStyle.RED : GuiStyle.GREEN);
        y += 18;
        GuiStyle.labelValue(this.fontRenderer, labelX, valueX, y, new TextComponentTranslation("container.sim.gui.wage", new Object[0]).getUnformattedText(), formatMoney(this.wage) + new TextComponentTranslation("container.sim.gui.per_minute", new Object[0]).getUnformattedText(), this.wage > 0 ? GuiStyle.GREEN : GuiStyle.TEXT_MUTED);
        y += 18;
        GuiStyle.labelValue(this.fontRenderer, labelX, valueX, y, new TextComponentTranslation("container.sim.gui_Folk_Housing_status", new Object[0]).getUnformattedText(), GuiLayout.truncate(this.fontRenderer, this.housingStatus, pw - 110), GuiStyle.TEXT);
    }

    private void drawTraits(int px, int py, int pw, int ph) {
        GuiStyle.text(this.fontRenderer, px + 8, py + 28, new TextComponentTranslation("container.sim.guiFolk.Traits", new Object[0]).getUnformattedText(), GuiStyle.TEXT);
        int contentTop = py + 44;
        int contentBottom = py + ph - 32;
        int contentW = pw - 24;
        final int[] cursor = new int[]{contentTop - this.traitScroll};
        GuiLayout.scissor(px + 8, contentTop, pw - 16, contentBottom - contentTop, () -> {
            drawTraitBlock(px + 12, contentW, cursor, this.folkTrait1, this.folkTraitDesc1);
            drawTraitBlock(px + 12, contentW, cursor, this.folkTrait2, this.folkTraitDesc2);
            drawTraitBlock(px + 12, contentW, cursor, this.folkTrait3, this.folkTraitDesc3);
        });
        int total = this.traitTotalHeight(contentW) - (contentBottom - contentTop);
        if (total > 0) {
            this.drawScrollbarPixels(px + pw - 9, contentTop, contentBottom - contentTop, this.traitTotalHeight(contentW), contentBottom - contentTop, this.traitScroll);
            this.traitScroll = Math.max(0, Math.min(this.traitScroll, total));
        } else {
            this.traitScroll = 0;
        }
    }

    private void drawTraitBlock(int x, int width, int[] cursor, String name, String desc) {
        if (name == null) {
            return;
        }
        int y = cursor[0];
        GuiStyle.text(this.fontRenderer, x, y, name, GuiStyle.TEXT);
        cursor[0] += 13;
        if (desc != null) {
            for (String line : GuiLayout.wrap(this.fontRenderer, desc, width)) {
                GuiStyle.text(this.fontRenderer, x, cursor[0], line, GuiStyle.TEXT_MUTED);
                cursor[0] += 11;
            }
        }
        cursor[0] += 7;
    }

    private int traitTotalHeight(int width) {
        int total = 0;
        total += traitBlockHeight(width, this.folkTraitDesc1);
        total += traitBlockHeight(width, this.folkTraitDesc2);
        total += traitBlockHeight(width, this.folkTraitDesc3);
        return total;
    }

    private int traitBlockHeight(int width, String desc) {
        int height = 13;
        if (desc != null) {
            height += GuiLayout.wrap(this.fontRenderer, desc, width).size() * 11;
        }
        return height + 7;
    }

    private void drawScrollbar(int x, int y, int height, int total, int visible, int offset) {
        if (total <= visible) {
            return;
        }
        GuiStyle.inset(x, y, 6, height);
        int thumb = Math.max(10, height * visible / total);
        int maxOffset = Math.max(1, total - visible);
        int thumbY = y + 2 + (height - thumb - 4) * offset / maxOffset;
        GuiStyle.panel(x + 1, thumbY, 4, thumb);
    }

    private void drawScrollbarPixels(int x, int y, int height, int total, int visible, int offset) {
        if (total <= visible) {
            return;
        }
        GuiStyle.inset(x, y, 6, height);
        int thumb = Math.max(10, height * visible / total);
        int maxOffset = Math.max(1, total - visible);
        int thumbY = y + 2 + (height - thumb - 4) * offset / maxOffset;
        GuiStyle.panel(x + 1, thumbY, 4, thumb);
    }

    private float parseSkill(String raw) {
        try {
            return Float.parseFloat(raw);
        } catch (Exception ignored) {
            return 0.0F;
        }
    }

    private String formatMoney(float value) {
        String formatted = ModSimLoader.displayMoney(value);
        return formatted == null ? "0.00" : formatted;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }
        int step = wheel > 0 ? -1 : 1;
        if (this.page == 1) {
            this.relOffset += step;
            if (this.relOffset < 0) {
                this.relOffset = 0;
            }
            List<String> lines = new ArrayList<String>();
            if (this.relationshipData != null) {
                for (String line : this.relationshipData.split(";")) {
                    if (line != null && !line.trim().isEmpty()) {
                        lines.add(line.trim());
                    }
                }
            }
            int maxRows = Math.max(1, (top() + panelH() - 32 - (top() + 44)) / 13);
            int max = Math.max(0, lines.size() - maxRows);
            if (this.relOffset > max) {
                this.relOffset = max;
            }
        } else if (this.page == 3) {
            this.traitScroll += step * 16;
            if (this.traitScroll < 0) {
                this.traitScroll = 0;
            }
            int max = Math.max(0, this.traitTotalHeight(panelW() - 24) - (panelH() - 76));
            if (this.traitScroll > max) {
                this.traitScroll = max;
            }
        }
    }

    @Override
    public void actionPerformed(GuiButton button) {
        if (!button.enabled) {
            return;
        }
        if (button.id == 0) {
            this.mc.currentScreen = null;
            this.mc.setIngameFocus();
            return;
        }
        if (button.id >= 10 && button.id <= 13) {
            this.page = button.id - 10;
            this.relOffset = 0;
            this.traitScroll = 0;
            this.showPage();
            return;
        }
        if (button.id == 14) {
            // 即使客户端没有 NpcData 镜像，GuiFolkInventory 也会从已加载实体/玩家容器兜底。
            Minecraft.getMinecraft().displayGuiScreen(new GuiFolkInventory(uid));
        }
    }

    @Override
    public void keyTyped(char c, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.mc.displayGuiScreen((GuiScreen) null);
            this.mc.setIngameFocus();
        }
    }
}


