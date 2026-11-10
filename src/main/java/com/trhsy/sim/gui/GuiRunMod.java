package com.trhsy.sim.gui;

import com.trhsy.sim.loader.ModSimClientLoader;
import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.loader.NetWorkLoader;
import com.trhsy.sim.network.server.PacketSetupMod;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.TextComponentTranslation;
import org.lwjgl.input.Mouse;

import java.util.List;

/**
 * 模拟城市启动界面。
 *
 * <p>第一页是任命书和“玩法说明”入口，第二页才是模式选择。模式按推荐顺序排列：
 * 创造模式（新手）→ 正常模式 → 不启动，避免新玩家一上来就选到要收集材料的模式。
 */
public class GuiRunMod extends GuiScreen {
    //运行
    public boolean running = true;
    //鼠标计数
    private int mouseCount = 0;
    //当前页
    public int page = 0;
    //玩家从模式页主动点“返回”时，允许重新查看任命书
    private boolean allowAppointmentPage = false;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int contentX;
    private int contentW;

    public GuiRunMod() {
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }

    /**
     * 初始化
     */
    @Override
    public void initGui() {
        try {
            //已经开过城的玩家，再打开卷轴时直接进入模式页，不用重新看任命书。
            if (this.page == 0 && !this.allowAppointmentPage && ModSimClientLoader.gamemode != 999) {
                this.page = 1;
            }

            this.panelW = Math.min(440, Math.max(260, this.width - 30));
            this.panelH = this.page == 0
                    ? Math.min(230, Math.max(180, this.height - 30))
                    : Math.min(260, Math.max(210, this.height - 30));
            this.panelX = (this.width - this.panelW) / 2;
            this.panelY = (this.height - this.panelH) / 2;
            this.contentX = this.panelX + 14;
            this.contentW = this.panelW - 28;

            this.buttonList.clear();
            if (this.page == 0) {
                this.initAppointmentButtons();
            } else {
                this.initModeButtons();
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("GuiRunMod-initGui出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    private void initAppointmentButtons() {
        int buttonY = this.panelY + this.panelH - 27;
        int acceptW = 104;
        int guideW = 92;
        int declineW = 92;
        int gap = 8;
        int totalW = acceptW + guideW + declineW + gap * 2;
        int x = this.panelX + (this.panelW - totalW) / 2;

        this.buttonList.add(new GuiButton(4, x, buttonY, acceptW, 20, this.t("container.sim.accept")));
        this.buttonList.add(new GuiButton(6, x + acceptW + gap, buttonY, guideW, 20,
                this.t("container.sim.guide_button")));
        this.buttonList.add(new GuiButton(5, x + acceptW + guideW + gap * 2, buttonY, declineW, 20,
                this.t("container.sim.not_accept")));
    }

    private void initModeButtons() {
        int rowY = this.panelY + 36;
        int rowH = 44;
        int buttonW = 138;

        // 推荐新手先玩创造模式，所以放在第一项。
        this.buttonList.add(new GuiButton(2, this.contentX, rowY, buttonW, 20,
                this.t("container.sim.mode.creative")));
        this.buttonList.add(new GuiButton(1, this.contentX, rowY + rowH, buttonW, 20,
                this.t("container.sim.mode.normal")));
        this.buttonList.add(new GuiButton(0, this.contentX, rowY + rowH * 2, buttonW, 20,
                this.t("container.sim.mode.off")));

        int footerY = this.panelY + this.panelH - 25;
        int guideW = 84;
        int backW = 64;
        int gap = 8;
        int x = this.panelX + (this.panelW - guideW - backW - gap) / 2;
        this.buttonList.add(new GuiButton(6, x, footerY, guideW, 20, this.t("container.sim.guide_button")));
        this.buttonList.add(new GuiButton(7, x + guideW + gap, footerY, backW, 20,
                this.t("container.sim.mode.back")));
    }

    /**
     * 绘制屏幕
     */
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        try {
            if (this.mouseCount < 10) {
                ++this.mouseCount;
                Mouse.setGrabbed(false);
            }
            this.drawDefaultBackground();
            GuiStyle.panel(this.panelX, this.panelY, this.panelW, this.panelH);

            if (this.page == 0) {
                this.drawAppointmentPage();
            } else {
                this.drawModePage();
            }

            super.drawScreen(mouseX, mouseY, partialTicks);
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.warn("GuiRunMod-drawScreen出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    private void drawAppointmentPage() {
        this.drawCentered(this.t("container.sim.sim_gui_rms"), this.panelY + 8, GuiStyle.TEXT);

        int y = this.panelY + 30;
        String[] introKeys = {
                "container.sim.sim_gui_zw0",
                "container.sim.sim_gui_zw1",
                "container.sim.sim_gui_zw2",
                "container.sim.sim_gui_zw3"
        };
        for (String key : introKeys) {
            List<String> lines = GuiLayout.wrap(this.fontRenderer, this.t(key), this.contentW);
            for (String line : lines) {
                if (y > this.panelY + this.panelH - 50) {
                    break;
                }
                this.drawCentered(line, y, GuiStyle.TEXT);
                y += 10;
            }
            y += 4;
        }

        this.drawCentered(this.t("container.sim.mode.kit_hint"), this.panelY + this.panelH - 43,
                GuiStyle.TEXT_MUTED);
    }

    private void drawModePage() {
        this.drawCentered(this.t("container.sim.mode.title"), this.panelY + 8, GuiStyle.TEXT);

        int rowY = this.panelY + 36;
        int rowH = 44;
        String[] descKeys = {
                "container.sim.mode.creative.desc",
                "container.sim.mode.normal.desc",
                "container.sim.mode.off.desc"
        };
        int descX = this.contentX + 150;
        int descW = this.contentW - 150;
        for (int i = 0; i < descKeys.length; i++) {
            List<String> lines = GuiLayout.wrap(this.fontRenderer, this.t(descKeys[i]), descW);
            int lineY = rowY + i * rowH + 1;
            for (int j = 0; j < lines.size() && j < 2; j++) {
                GuiStyle.text(this.fontRenderer, descX, lineY + j * 10, lines.get(j), GuiStyle.TEXT_MUTED);
            }
        }

        this.drawCentered(this.t("container.sim.mode.hint"), this.panelY + this.panelH - 41,
                GuiStyle.TEXT_MUTED);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        try {
            switch (button.id) {
                case 0:
                    // 不运行模拟城市
                    NetWorkLoader.net.sendToServer(new PacketSetupMod(999));
                    ModSimClientLoader.gamemode = 999;
                    ModSimClientLoader.sim_is_running = false;
                    this.closeToGame();
                    break;
                case 1:
                    // 正常模式：需要材料，有资金
                    NetWorkLoader.net.sendToServer(new PacketSetupMod(0));
                    ModSimClientLoader.gamemode = 0;
                    ModSimClientLoader.sim_is_running = true;
                    this.closeToGame();
                    break;
                case 2:
                    // 创造模式：不消耗材料
                    NetWorkLoader.net.sendToServer(new PacketSetupMod(1));
                    ModSimClientLoader.gamemode = 1;
                    ModSimClientLoader.sim_is_running = true;
                    this.closeToGame();
                    break;
                case 4:
                    // 接受任命书，进入模式选择
                    this.page = 1;
                    this.initGui();
                    break;
                case 5:
                    // 暂不接受：只关界面，不消耗卷轴
                    this.closeToGame();
                    break;
                case 6:
                    this.mc.displayGuiScreen(new GuiCityGuide(this));
                    break;
                case 7:
                    this.allowAppointmentPage = true;
                    this.page = 0;
                    this.initGui();
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("GuiRunMod-actionPerformed出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    private void closeToGame() {
        this.mc.displayGuiScreen(null);
        this.mc.setIngameFocus();
    }

    private void drawCentered(String text, int y, int color) {
        GuiStyle.text(this.fontRenderer, (this.width - this.fontRenderer.getStringWidth(text)) / 2, y, text, color);
    }

    private String t(String key) {
        return new TextComponentTranslation(key).getUnformattedText();
    }
}
