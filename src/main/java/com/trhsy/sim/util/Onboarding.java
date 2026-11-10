package com.trhsy.sim.util;

import com.trhsy.sim.loader.BlockLoader;
import com.trhsy.sim.loader.ItemLoader;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentTranslation;

/**
 * 新手引导相关的通用逻辑。
 *
 * <p>只放服务端也能安全执行的代码：发卷轴、发新手工具、一次性提示。
 * 客户端界面（GuiRunMod / GuiCityGuide）不在这里处理。
 */
public final class Onboarding {
    private static final String KIT_TAG = "simStarterKit";
    private static final String WELCOME_TAG = "simWelcomeTip";
    private static final String CONSTRUCTOR_TAG = "simConstructorTip";

    private Onboarding() {
    }

    /** 玩家背包（含副手）里是否已经有启动卷轴。 */
    public static boolean hasScroll(EntityPlayer player) {
        if (player == null || ItemLoader.itemSimULoader == null) {
            return false;
        }
        for (ItemStack stack : player.inventory.mainInventory) {
            if (stack != null && stack.getItem() == ItemLoader.itemSimULoader) {
                return true;
            }
        }
        for (ItemStack stack : player.inventory.offHandInventory) {
            if (stack != null && stack.getItem() == ItemLoader.itemSimULoader) {
                return true;
            }
        }
        return false;
    }

    /** 没有卷轴时补发一个，返回是否真的发了。 */
    public static boolean giveScroll(EntityPlayer player) {
        if (player == null || ItemLoader.itemSimULoader == null || hasScroll(player)) {
            return false;
        }
        addOrDrop(player, new ItemStack(ItemLoader.itemSimULoader));
        return true;
    }

    /**
     * 发一次新手工具：建筑箱 x1、标记棒 x3。
     *
     * @return true 表示这次真的发了；已经发过则返回 false。
     */
    public static boolean giveStarterKit(EntityPlayer player) {
        if (player == null || !markOnce(player, KIT_TAG)) {
            return false;
        }
        addOrDrop(player, new ItemStack(BlockLoader.blockConstructorBox));
        addOrDrop(player, new ItemStack(BlockLoader.blockMarker, 3));
        return true;
    }

    /** 第一次放置建筑箱时提示一次。 */
    public static boolean markConstructorTip(EntityPlayer player) {
        return markOnce(player, CONSTRUCTOR_TAG);
    }

    /** 第一次进入世界时提示一次。 */
    public static boolean markWelcomeTip(EntityPlayer player) {
        return markOnce(player, WELCOME_TAG);
    }

    /** 给玩家发一条翻译键对应的系统消息。 */
    public static void send(EntityPlayer player, String translationKey) {
        if (player != null && translationKey != null) {
            player.sendMessage(new TextComponentTranslation(translationKey));
        }
    }

    /** 放到背包，放不下就掉在脚边，避免直接消失。 */
    public static void addOrDrop(EntityPlayer player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return;
        }
        if (!player.inventory.addItemStackToInventory(stack)) {
            player.dropItem(stack, false);
        }
    }

    /**
     * 每个玩家只成功一次。标记写在 PlayerPersisted 里，死亡/重生后仍然保留。
     */
    public static boolean markOnce(EntityPlayer player, String key) {
        if (player == null || key == null) {
            return false;
        }
        NBTTagCompound data = player.getEntityData();
        NBTTagCompound persisted = data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        if (persisted.getBoolean(key)) {
            return false;
        }
        persisted.setBoolean(key, true);
        data.setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);
        return true;
    }
}
