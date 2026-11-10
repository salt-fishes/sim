package com.trhsy.sim.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;

/**
 * 原版 GUI 的居中、行距、截断和裁剪工具。
 */
public final class GuiLayout {
    private GuiLayout() {
    }

    public static int centerX(int screenWidth, int panelWidth) {
        return (screenWidth - panelWidth) / 2;
    }

    public static int centerY(int screenHeight, int panelHeight) {
        return (screenHeight - panelHeight) / 2;
    }

    public static int rowY(int startY, int index, int rowHeight) {
        return startY + index * rowHeight;
    }

    /** 按像素宽度截断，超出部分用 ... 代替。 */
    public static String truncate(FontRenderer fr, String text, int maxWidth) {
        if (text == null) {
            return "";
        }
        if (maxWidth <= 0) {
            return "";
        }
        if (fr.getStringWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int ellipsisWidth = fr.getStringWidth(ellipsis);
        if (ellipsisWidth >= maxWidth) {
            return fr.trimStringToWidth(text, maxWidth);
        }
        return fr.trimStringToWidth(text, maxWidth - ellipsisWidth) + ellipsis;
    }

    /** 按像素宽度换行；返回空列表表示没有内容。 */
    public static List<String> wrap(FontRenderer fr, String text, int width) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        return fr.listFormattedStringToWidth(text, Math.max(10, width));
    }

    /** 用 Scissor 裁剪一段渲染，坐标是 GUI 逻辑像素。 */
    public static void scissor(int x, int y, int w, int h, Runnable render) {
        if (w <= 0 || h <= 0) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution resolution = new ScaledResolution(mc);
        int scale = resolution.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * scale, mc.displayHeight - (y + h) * scale, w * scale, h * scale);
        try {
            render.run();
        } finally {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    /** 把宽度按比例拆成若干列，返回每列的起始偏移（相对 x）。 */
    public static int[] columns(int width, float... ratios) {
        if (ratios == null || ratios.length == 0) {
            return new int[]{0};
        }
        float total = 0.0F;
        for (float ratio : ratios) {
            total += Math.max(0.0F, ratio);
        }
        if (total <= 0.0F) {
            total = 1.0F;
        }
        int[] result = new int[ratios.length + 1];
        int cursor = 0;
        result[0] = 0;
        for (int i = 0; i < ratios.length; i++) {
            cursor += Math.round(width * Math.max(0.0F, ratios[i]) / total);
            result[i + 1] = cursor;
        }
        result[ratios.length] = width;
        return result;
    }
}
