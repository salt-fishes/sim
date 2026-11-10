package com.trhsy.sim.gui.npc;

import com.trhsy.sim.gui.GuiLayout;
import com.trhsy.sim.gui.GuiStyle;
import com.trhsy.sim.loader.ModSimClientLoader;
import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.loader.NetWorkLoader;
import com.trhsy.sim.network.server.PacketFireFolk;
import com.trhsy.sim.npcCode.NpcIdentity;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.TextComponentTranslation;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @ClassName GuiEmployees
 * @Description 员工列表（原版像素风）
 * @Author TRHSY
 * @Date 2022/10/20 13:44
 **/
public class GuiEmployees extends GuiScreen {
    private static final int ROWS_PER_PAGE = 7;
    List<NpcIdentity> folks = new ArrayList<NpcIdentity>();
    private int folkOffset = 0;

    public GuiEmployees() {
    }

    @Override
    public void initGui() {
        super.initGui();
        this.folks = new ArrayList<NpcIdentity>();
        for (NpcIdentity identity : ModSimClientLoader.tempHireableNpcNames) {
            if (identity != null && !isUnemployed(identity)) {
                this.folks.add(identity);
            }
        }
        this.showPage();
    }

    private boolean isUnemployed(NpcIdentity identity) {
        if (identity.job == null) {
            return true;
        }
        String unemployed = new TextComponentTranslation("container.sim.gui_Folk_unemployed", new Object[0]).getUnformattedText();
        String unemployed2 = new TextComponentTranslation("container.sim.folkData1", new Object[0]).getUnformattedText();
        return identity.job.isEmpty() || identity.job.equals(unemployed) || identity.job.equals(unemployed2);
    }

    private int panelW() {
        return Math.min(400, this.width - 16);
    }

    private int panelH() {
        return Math.min(240, this.height - 16);
    }

    private int left() {
        return GuiLayout.centerX(this.width, panelW());
    }

    private int top() {
        return GuiLayout.centerY(this.height, panelH());
    }

    private int innerX() {
        return left() + 8;
    }

    private int innerW() {
        return panelW() - 16;
    }

    private int actionX() {
        return left() + panelW() - 60;
    }

    private int columnArea() {
        return innerW() - 60;
    }

    private void showPage() {
        this.buttonList.clear();
        int px = left();
        int py = top();
        int pw = panelW();
        int ph = panelH();
        if (this.folkOffset < 0) {
            this.folkOffset = 0;
        }
        int maxOffset = Math.max(0, this.folks.size() - ROWS_PER_PAGE);
        if (this.folkOffset > maxOffset) {
            this.folkOffset = maxOffset;
        }

        //完成
        this.buttonList.add(new GuiButton(0, px + pw - 60, py + ph - 26, 52, 20, new TextComponentTranslation("container.sim.sim_gui_BC_Done", new Object[0]).getUnformattedText()));

        if (this.folkOffset > 0) {
            this.buttonList.add(new GuiButton(1000, px + pw - 60 - 96, py + ph - 26, 44, 20, "<"));
        }
        if (this.folkOffset + ROWS_PER_PAGE < this.folks.size()) {
            this.buttonList.add(new GuiButton(1001, px + pw - 60 - 48, py + ph - 26, 44, 20, ">"));
        }

        int startY = py + 46;
        for (int i = 0; i < ROWS_PER_PAGE; i++) {
            int index = this.folkOffset + i;
            if (index >= this.folks.size()) {
                break;
            }
            NpcIdentity folk = this.folks.get(index);
            this.buttonList.add(new GuiButton(100 + i, this.actionX(), startY + i * 20 - 1, 52, 18, new TextComponentTranslation("container.sim.Fire", new Object[0]).getUnformattedText()));
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        int px = left();
        int py = top();
        int pw = panelW();
        int ph = panelH();
        String title = new TextComponentTranslation("container.sim.employees", new Object[0]).getUnformattedText() + " (" + this.folks.size() + ")";
        GuiStyle.panel(px, py, pw, ph, title, this.fontRenderer);

        int area = this.columnArea();
        int nameW = Math.round(area * 0.21F);
        int jobW = Math.round(area * 0.24F);
        int statusW = Math.round(area * 0.21F);
        int moneyW = Math.round(area * 0.18F);
        int x0 = this.innerX();
        int x1 = x0 + nameW;
        int x2 = x1 + jobW;
        int x3 = x2 + statusW;
        int x4 = x3 + moneyW;
        int x5 = x4 + (area - nameW - jobW - statusW - moneyW);

        GuiStyle.inset(x0, py + 24, this.innerW(), 18);
        GuiStyle.text(this.fontRenderer, x0 + 4, py + 29, new TextComponentTranslation("container.sim.gui.name", new Object[0]).getUnformattedText(), GuiStyle.TEXT_MUTED);
        GuiStyle.text(this.fontRenderer, x1 + 4, py + 29, new TextComponentTranslation("container.sim.gui_Folk_Job", new Object[0]).getUnformattedText(), GuiStyle.TEXT_MUTED);
        GuiStyle.text(this.fontRenderer, x2 + 4, py + 29, new TextComponentTranslation("container.sim.gui_Folk_Status", new Object[0]).getUnformattedText(), GuiStyle.TEXT_MUTED);
        GuiStyle.textRight(this.fontRenderer, x4 - 4, py + 29, new TextComponentTranslation("container.sim.gui.money", new Object[0]).getUnformattedText(), GuiStyle.TEXT_MUTED);
        GuiStyle.textRight(this.fontRenderer, x5 - 4, py + 29, new TextComponentTranslation("container.sim.gui.wage", new Object[0]).getUnformattedText(), GuiStyle.TEXT_MUTED);
        GuiStyle.text(this.fontRenderer, this.actionX() + 4, py + 29, new TextComponentTranslation("container.sim.gui.action", new Object[0]).getUnformattedText(), GuiStyle.TEXT_MUTED);

        if (this.folks.isEmpty()) {
            GuiStyle.text(this.fontRenderer, px + pw / 2 - this.fontRenderer.getStringWidth(new TextComponentTranslation("container.sim.gui.employees.none", new Object[0]).getUnformattedText()) / 2, py + 94, new TextComponentTranslation("container.sim.gui.employees.none", new Object[0]).getUnformattedText(), GuiStyle.TEXT_MUTED);
        } else {
            int startY = py + 46;
            GuiLayout.scissor(x0, startY - 2, this.innerW(), ROWS_PER_PAGE * 20 + 2, () -> {
                for (int i = 0; i < ROWS_PER_PAGE; i++) {
                    int index = this.folkOffset + i;
                    if (index >= this.folks.size()) {
                        break;
                    }
                    NpcIdentity folk = this.folks.get(index);
                    int y = startY + i * 20;
                    GuiStyle.row(x0, y - 2, this.innerW(), 20, i % 2 == 1);
                    GuiStyle.text(this.fontRenderer, x0 + 4, y + 2, GuiLayout.truncate(this.fontRenderer, folk.name, nameW - 8), GuiStyle.TEXT);
                    GuiStyle.text(this.fontRenderer, x1 + 4, y + 2, GuiLayout.truncate(this.fontRenderer, folk.job, jobW - 8), GuiStyle.TEXT);
                    GuiStyle.text(this.fontRenderer, x2 + 4, y + 2, GuiLayout.truncate(this.fontRenderer, folk.status, statusW - 8), this.statusColor(folk.status));
                    GuiStyle.moneyRight(this.fontRenderer, x4 - 4, y + 2, folk.money);
                    GuiStyle.textRight(this.fontRenderer, x5 - 4, y + 2, formatMoney(folk.wage), folk.wage > 0 ? GuiStyle.TEXT : GuiStyle.TEXT_MUTED);
                }
            });
        }

        int totalWage = 0;
        float total = 0.0F;
        for (NpcIdentity folk : this.folks) {
            total += folk.wage;
        }
        String footer = this.folks.size() + " " + new TextComponentTranslation("container.sim.gui.employees.count", new Object[0]).getUnformattedText()
                + " · " + new TextComponentTranslation("container.sim.gui.wage_total", new Object[0]).getUnformattedText()
                + " " + formatMoney(total) + new TextComponentTranslation("container.sim.gui.per_minute", new Object[0]).getUnformattedText();
        GuiStyle.text(this.fontRenderer, this.innerX(), py + ph - 18, GuiLayout.truncate(this.fontRenderer, footer, this.innerW() - 168), GuiStyle.TEXT_MUTED);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private int statusColor(String status) {
        if (status == null) {
            return GuiStyle.TEXT_MUTED;
        }
        if (status.contains("休息") || status.contains("Relax") || status.contains("Idle") || status.contains("闲置")) {
            return GuiStyle.GOLD;
        }
        return GuiStyle.GREEN;
    }

    private String formatMoney(float value) {
        String formatted = ModSimLoader.displayMoney(value);
        return formatted == null ? "0.00" : formatted;
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
        if (button.id == 1000) {
            this.folkOffset -= ROWS_PER_PAGE;
            this.showPage();
            return;
        }
        if (button.id == 1001) {
            this.folkOffset += ROWS_PER_PAGE;
            this.showPage();
            return;
        }
        if (button.id >= 100 && button.id < 100 + ROWS_PER_PAGE) {
            int index = this.folkOffset + (button.id - 100);
            if (index < 0 || index >= this.folks.size()) {
                return;
            }
            NpcIdentity folk = this.folks.get(index);
            NetWorkLoader.net.sendToServer(new PacketFireFolk(folk.id));
            this.folks.remove(index);
            this.showPage();
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void keyTyped(char c, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.mc.displayGuiScreen((GuiScreen) null);
            this.mc.setIngameFocus();
        }
    }
}

