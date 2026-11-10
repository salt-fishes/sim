package com.trhsy.sim.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextComponentTranslation;

import java.io.IOException;
import java.util.List;

/**
 * 市长手册：给第一次接触模拟城市的玩家一个随时可查的玩法入口。
 *
 * <p>内容只讲“下一步做什么”，不堆设定。页面用原版灰色面板，正文自动换行，
 * 小窗口下也不会把字画出屏幕。
 */
public class GuiCityGuide extends GuiScreen {
    private static final String[][] PAGES = new String[][]{
            {
                    "container.sim.guide.page.0.title",
                    "container.sim.guide.page.0.0",
                    "container.sim.guide.page.0.1",
                    "container.sim.guide.page.0.2",
                    "container.sim.guide.page.0.3"
            },
            {
                    "container.sim.guide.toc.title",
                    "container.sim.guide.toc.0",
                    "container.sim.guide.toc.1",
                    "container.sim.guide.toc.2",
                    "container.sim.guide.toc.3",
                    "container.sim.guide.toc.4",
                    "container.sim.guide.toc.5",
                    "container.sim.guide.toc.6",
                    "container.sim.guide.toc.7"
            },
            {
                    "container.sim.guide.page.1.title",
                    "container.sim.guide.page.1.0",
                    "container.sim.guide.page.1.1",
                    "container.sim.guide.page.1.2",
                    "container.sim.guide.page.1.3",
                    "container.sim.guide.page.1.4"
            },
            {
                    "container.sim.guide.page.2.title",
                    "container.sim.guide.page.2.0",
                    "container.sim.guide.page.2.1",
                    "container.sim.guide.page.2.2",
                    "container.sim.guide.page.2.3",
                    "container.sim.guide.page.2.4",
                    "container.sim.guide.page.2.5"
            },
            {
                    "container.sim.guide.page.8.title",
                    "container.sim.guide.page.8.0",
                    "container.sim.guide.page.8.1",
                    "container.sim.guide.page.8.2",
                    "container.sim.guide.page.8.3",
                    "container.sim.guide.page.8.4"
            },
            {
                    "container.sim.guide.materials.title",
                    "container.sim.guide.materials.0",
                    "container.sim.guide.materials.1",
                    "container.sim.guide.materials.2",
                    "container.sim.guide.materials.3",
                    "container.sim.guide.materials.4",
                    "container.sim.guide.materials.5"
            },
            {
                    "container.sim.guide.page.3.title",
                    "container.sim.guide.page.3.0",
                    "container.sim.guide.page.3.1",
                    "container.sim.guide.page.3.2",
                    "container.sim.guide.page.3.3",
                    "container.sim.guide.page.3.4"
            },
            {
                    "container.sim.guide.page.5.title",
                    "container.sim.guide.page.5.0",
                    "container.sim.guide.page.5.1",
                    "container.sim.guide.page.5.2",
                    "container.sim.guide.page.5.3",
                    "container.sim.guide.page.5.4"
            },
            {
                    "container.sim.guide.page.4.title",
                    "container.sim.guide.page.4.0",
                    "container.sim.guide.page.4.1",
                    "container.sim.guide.page.4.2",
                    "container.sim.guide.page.4.3",
                    "container.sim.guide.page.4.4"
            },
            {
                    "container.sim.guide.mining.title",
                    "container.sim.guide.mining.0",
                    "container.sim.guide.mining.1",
                    "container.sim.guide.mining.2",
                    "container.sim.guide.mining.3",
                    "container.sim.guide.mining.4"
            },
            {
                    "container.sim.guide.page.9.title",
                    "container.sim.guide.page.9.0",
                    "container.sim.guide.page.9.1",
                    "container.sim.guide.page.9.2",
                    "container.sim.guide.page.9.3",
                    "container.sim.guide.page.9.4"
            },
            {
                    "container.sim.guide.shops.title",
                    "container.sim.guide.shops.0",
                    "container.sim.guide.shops.1",
                    "container.sim.guide.shops.2",
                    "container.sim.guide.shops.3",
                    "container.sim.guide.shops.4",
                    "container.sim.guide.shops.5",
                    "container.sim.guide.shops.6"
            },
            {
                    "container.sim.guide.npc.title",
                    "container.sim.guide.npc.0",
                    "container.sim.guide.npc.1",
                    "container.sim.guide.npc.2",
                    "container.sim.guide.npc.3",
                    "container.sim.guide.npc.4",
                    "container.sim.guide.npc.5"
            },
            {
                    "container.sim.guide.page.10.title",
                    "container.sim.guide.page.10.0",
                    "container.sim.guide.page.10.1",
                    "container.sim.guide.page.10.2",
                    "container.sim.guide.page.10.3",
                    "container.sim.guide.page.10.4"
            },
            {
                    "container.sim.guide.page.6.title",
                    "container.sim.guide.page.6.0",
                    "container.sim.guide.page.6.1",
                    "container.sim.guide.page.6.2",
                    "container.sim.guide.page.6.3",
                    "container.sim.guide.page.6.4",
                    "container.sim.guide.page.6.5",
                    "container.sim.guide.page.6.6"
            },
            {
                    "container.sim.guide.page.7.title",
                    "container.sim.guide.page.7.0",
                    "container.sim.guide.page.7.1",
                    "container.sim.guide.page.7.2",
                    "container.sim.guide.page.7.3",
                    "container.sim.guide.page.7.4",
                    "container.sim.guide.page.7.5"
            }
    };

    private final GuiScreen parent;
    private int page = 0;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int contentX;
    private int contentW;
    private int contentBottom;

    public GuiCityGuide() {
        this(null);
    }

    public GuiCityGuide(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();

        this.panelW = Math.min(440, Math.max(260, this.width - 30));
        this.panelH = Math.min(260, Math.max(190, this.height - 30));
        this.panelX = (this.width - this.panelW) / 2;
        this.panelY = (this.height - this.panelH) / 2;
        this.contentX = this.panelX + 14;
        this.contentW = this.panelW - 28;
        this.contentBottom = this.panelY + this.panelH - 48;

        int buttonW = 56;
        int gap = 6;
        int tocW = 52;
        int closeW = 52;
        int totalW = buttonW * 2 + tocW + closeW + gap * 3;
        int startX = this.panelX + (this.panelW - totalW) / 2;
        int buttonY = this.panelY + this.panelH - 25;

        GuiButton prev = new GuiButton(0, startX, buttonY, buttonW, 20,
                I18n.format("container.sim.guide.prev"));
        prev.enabled = this.page > 0;
        this.buttonList.add(prev);

        GuiButton next = new GuiButton(1, startX + buttonW + gap, buttonY, buttonW, 20,
                I18n.format("container.sim.guide.next"));
        next.enabled = this.page < PAGES.length - 1;
        this.buttonList.add(next);

        this.buttonList.add(new GuiButton(3, startX + buttonW * 2 + gap * 2, buttonY, tocW, 20,
                I18n.format("container.sim.guide.toc")));

        this.buttonList.add(new GuiButton(2, startX + buttonW * 2 + tocW + gap * 3, buttonY, closeW, 20,
                I18n.format("container.sim.guide.close")));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        GuiStyle.panel(this.panelX, this.panelY, this.panelW, this.panelH);

        String title = this.t(PAGES[this.page][0]);
        GuiStyle.text(this.fontRenderer, this.panelX + 14, this.panelY + 8, title, GuiStyle.TEXT);
        GuiStyle.textRight(this.fontRenderer, this.panelX + this.panelW - 14, this.panelY + 8,
                I18n.format("container.sim.guide.page", this.page + 1, PAGES.length), GuiStyle.TEXT_MUTED);
        GuiStyle.divider(this.panelX + 8, this.panelY + 20, this.panelW - 16);

        int y = this.panelY + 28;
        boolean overflow = false;
        for (int i = 1; i < PAGES[this.page].length && !overflow; i++) {
            List<String> lines = GuiLayout.wrap(this.fontRenderer, this.t(PAGES[this.page][i]), this.contentW);
            for (String line : lines) {
                if (y > this.contentBottom) {
                    overflow = true;
                    break;
                }
                GuiStyle.text(this.fontRenderer, this.contentX, y, line, GuiStyle.TEXT);
                y += 10;
            }
            y += 3;
        }

        GuiStyle.text(this.fontRenderer, this.panelX + 14, this.panelY + this.panelH - 39,
                I18n.format("container.sim.guide.hint"), GuiStyle.TEXT_MUTED);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0 && this.page > 0) {
            this.page--;
            this.initGui();
        } else if (button.id == 1 && this.page < PAGES.length - 1) {
            this.page++;
            this.initGui();
        } else if (button.id == 3 && this.page != 1) {
            this.page = 1;
            this.initGui();
        } else if (button.id == 2) {
            this.closeGuide();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            this.closeGuide();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private void closeGuide() {
        if (this.parent != null) {
            this.mc.displayGuiScreen(this.parent);
        } else {
            this.mc.displayGuiScreen(null);
            this.mc.setIngameFocus();
        }
    }

    private String t(String key) {
        return new TextComponentTranslation(key).getUnformattedText();
    }
}
