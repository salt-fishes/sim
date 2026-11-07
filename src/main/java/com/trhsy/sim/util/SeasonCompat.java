package com.trhsy.sim.util;

import net.minecraft.world.World;

import java.lang.reflect.Method;

/**
 * P13(S2): SereneSeasons 可选兼容。
 *
 * <p>核心模组不硬依赖 SereneSeasons，因此这里用反射读取季节；
 * 没有安装 SereneSeasons 时所有方法返回 false，行为与原版一致。</p>
 */
public final class SeasonCompat {
    private static boolean initialized = false;
    private static Method getSeasonState;
    private static Method getSeason;

    private SeasonCompat() {
    }

    private static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        try {
            Class<?> helper = Class.forName("sereneseasons.api.season.SeasonHelper");
            getSeasonState = helper.getMethod("getSeasonState", World.class);
            Class<?> stateApi = Class.forName("sereneseasons.api.season.ISeasonState");
            getSeason = stateApi.getMethod("getSeason");
        } catch (Throwable ignored) {
            getSeasonState = null;
            getSeason = null;
        }
    }

    /** 是否冬季；未安装 SereneSeasons 时返回 false。 */
    public static boolean isWinter(World world) {
        if (world == null || world.isRemote) {
            return false;
        }
        init();
        if (getSeasonState == null || getSeason == null) {
            return false;
        }
        try {
            Object state = getSeasonState.invoke(null, world);
            if (state == null) {
                return false;
            }
            Object season = getSeason.invoke(state);
            return season != null && "WINTER".equals(String.valueOf(season));
        } catch (Throwable ignored) {
            return false;
        }
    }
}
