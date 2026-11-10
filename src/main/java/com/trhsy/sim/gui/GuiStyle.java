package com.trhsy.sim.gui;

import com.trhsy.sim.loader.ModSimLoader;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

/**
 * 原版 Minecraft 像素风的薄封装。
 *
 * <p>这里只做颜色、面板、凹槽、进度条和文本对齐，不接管按钮/列表等原版控件。
 * 正文默认无阴影，按钮仍由 {@link net.minecraft.client.gui.GuiButton} 绘制。
 */
public final class GuiStyle {
    /** 面板底色，原版 GUI 的 #C6C6C6。 */
    public static final int PANEL = 0xFFC6C6C6;
    /** 凹槽/表头底色，原版 GUI 的 #8B8B8B。 */
    public static final int PANEL_DARK = 0xFF8B8B8B;
    /** 外边框。 */
    public static final int BORDER = 0xFF000000;
    /** 左上高光。 */
    public static final int HIGHLIGHT = 0xFFFFFFFF;
    /** 右下阴影。 */
    public static final int SHADOW = 0xFF555555;
    /** 主文字。 */
    public static final int TEXT = 0xFF404040;
    /** 次要文字。 */
    public static final int TEXT_MUTED = 0xFF707070;
    /** 收入/正常。 */
    public static final int GREEN = 0xFF2E7D32;
    /** 支出/错误。 */
    public static final int RED = 0xFFB00020;
    /** 警告。 */
    public static final int GOLD = 0xFF8A6D00;
    /** 白色文字。 */
    public static final int WHITE = 0xFFFFFFFF;
    /** 隔行底色。 */
    public static final int ROW_ALT = 0xFFD0D0D0;

    private GuiStyle() {
    }

    /** 原版风格面板：黑边 + 浅灰底 + 左上高光 + 右下阴影。 */
    public static void panel(int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) {
            return;
        }
        Gui.drawRect(x - 1, y - 1, x + w + 1, y + h + 1, BORDER);
        Gui.drawRect(x, y, x + w, y + h, PANEL);
        Gui.drawRect(x, y, x + w, y + 1, HIGHLIGHT);
        Gui.drawRect(x, y, x + 1, y + h, HIGHLIGHT);
        Gui.drawRect(x, y + h - 1, x + w, y + h, SHADOW);
        Gui.drawRect(x + w - 1, y, x + w, y + h, SHADOW);
    }

    /** 带标题的面板。标题居中，下面一条 1px 分隔线。 */
    public static void panel(int x, int y, int w, int h, String title, FontRenderer fr) {
        panel(x, y, w, h);
        if (title != null && !title.isEmpty()) {
            String t = GuiLayout.truncate(fr, title, Math.max(10, w - 16));
            fr.drawString(t, x + (w - fr.getStringWidth(t)) / 2, y + 6, TEXT, false);
            divider(x + 7, y + 19, w - 14);
        }
    }

    /** 凹陷区域：用于表头、内容块、输入框底。 */
    public static void inset(int x, int y, int w, int h) {
        if (w <= 1 || h <= 1) {
            return;
        }
        Gui.drawRect(x, y, x + w, y + h, BORDER);
        Gui.drawRect(x + 1, y + 1, x + w - 1, y + h - 1, PANEL_DARK);
        Gui.drawRect(x + 1, y + 1, x + w - 1, y + 2, SHADOW);
        Gui.drawRect(x + 1, y + 1, x + 2, y + h - 1, SHADOW);
        Gui.drawRect(x + 1, y + h - 2, x + w - 1, y + h - 1, HIGHLIGHT);
        Gui.drawRect(x + w - 2, y + 1, x + w - 1, y + h - 1, HIGHLIGHT);
    }

    /** 18×18 原版物品槽。物品图标画在 (x+1, y+1)。 */
    public static void slot(int x, int y) {
        Gui.drawRect(x, y, x + 18, y + 18, BORDER);
        Gui.drawRect(x + 1, y + 1, x + 17, y + 17, PANEL_DARK);
        Gui.drawRect(x + 1, y + 1, x + 17, y + 2, SHADOW);
        Gui.drawRect(x + 1, y + 1, x + 2, y + 17, SHADOW);
        Gui.drawRect(x + 1, y + 16, x + 17, y + 17, HIGHLIGHT);
        Gui.drawRect(x + 16, y + 1, x + 17, y + 17, HIGHLIGHT);
    }

    /** 1px 分隔线。 */
    public static void divider(int x, int y, int w) {
        Gui.drawRect(x, y, x + w, y + 1, SHADOW);
    }

    /** 无阴影正文。 */
    public static void text(FontRenderer fr, int x, int y, String text, int color) {
        if (text != null) {
            fr.drawString(text, x, y, color, false);
        }
    }

    /** 无阴影右对齐正文。 */
    public static void textRight(FontRenderer fr, int right, int y, String text, int color) {
        if (text != null) {
            fr.drawString(text, right - fr.getStringWidth(text), y, color, false);
        }
    }

    /** 左标签右值，无阴影。 */
    public static void labelValue(FontRenderer fr, int labelX, int valueX, int y, String label, String value, int valueColor) {
        text(fr, labelX, y, label, TEXT_MUTED);
        text(fr, valueX, y, value, valueColor);
    }

    /** 金币文本，按正负自动着色。 */
    public static void money(FontRenderer fr, int x, int y, float amount) {
        text(fr, x, y, ModSimLoader.displayMoney(amount), amount < 0 ? RED : GREEN);
    }

    /** 金币文本右对齐。 */
    public static void moneyRight(FontRenderer fr, int right, int y, float amount) {
        textRight(fr, right, y, ModSimLoader.displayMoney(amount), amount < 0 ? RED : GREEN);
    }

    /** 小状态标签。 */
    public static void statusPill(FontRenderer fr, int x, int y, String text, int color) {
        int w = fr.getStringWidth(text) + 6;
        Gui.drawRect(x, y, x + w, y + 12, BORDER);
        Gui.drawRect(x + 1, y + 1, x + w - 1, y + 11, PANEL_DARK);
        text(fr, x + 3, y + 2, text, color);
    }

    /** 凹陷背景上的进度条。 */
    public static void progressBar(int x, int y, int w, int h, float ratio, int color) {
        inset(x, y, w, h);
        float clamped = Math.max(0.0F, Math.min(1.0F, ratio));
        int fill = (int) ((w - 4) * clamped);
        if (fill > 0 && h > 4) {
            Gui.drawRect(x + 2, y + 2, x + 2 + fill, y + h - 2, color);
        }
    }

    /** 隔行背景。 */
    public static void row(int x, int y, int w, int h, boolean alt) {
        if (alt) {
            Gui.drawRect(x, y, x + w, y + h, ROW_ALT);
        }
    }
}
