package com.trhsy.sim.npcCode;

import com.trhsy.sim.npcCode.race.Race;
import com.trhsy.sim.npcCode.race.Races;
import com.trhsy.sim.npcCode.traits.Trait;
import com.trhsy.sim.npcCode.traits.Traits;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * 稳定 ID 注册表（P0/P1）。
 *
 * <p>原实现把"翻译后的显示字符串"当主键：职业、种族、建筑类型、特质、手持物全部
 * 按当前语言比较。玩家切换语言后，读档时这些类型全部对不上 —— 职业变 null（全员失业）、
 * 建筑不再被识别为住宅（人口停止增长）、特质错乱（hasTrait 因 NPE 吞异常恒真）。
 *
 * <p>本类提供"显示串 &lt;-&gt; 稳定 ID"的双向映射。规则：
 * <ul>
 *   <li>职业：lang 键后缀，如 container.sim.Vocation13 -&gt; vocation13；</li>
 *   <li>建筑类型：蓝图目录名（residential/commercial/...）；</li>
 *   <li>种族/特质：类名去掉 Race/Trait 前缀后小写；</li>
 *   <li>物品：registry name（sim:item_copper_ingot）。</li>
 * </ul>
 * 读档路径不允许用 getUnformattedText() 做匹配；显示串只在 GUI 出现。
 */
public final class SimIds {
    private SimIds() {
    }

    // ------------------------------------------------------------------
    // 建筑类型
    // ------------------------------------------------------------------

    public static final String TYPE_RESIDENTIAL = "residential";
    public static final String TYPE_COMMERCIAL = "commercial";
    public static final String TYPE_INDUSTRIAL = "industrial";
    public static final String TYPE_DECORATIVE = "decorative";
    public static final String TYPE_OTHER = "other";
    public static final String TYPE_SPECIAL = "special";

    private static final String[] BUILDING_TYPE_KEYS = {
            "container.sim.sim_gui_BC_Residential",
            "container.sim.sim_gui_BC_Commercial",
            "container.sim.sim_gui_BC_Industrial",
            "container.sim.sim_gui_BC_Decorative",
            "container.sim.sim_gui_BC_Other",
            "container.sim.sim_gui_BC_special"
    };
    private static final String[] BUILDING_TYPE_IDS = {
            TYPE_RESIDENTIAL, TYPE_COMMERCIAL, TYPE_INDUSTRIAL,
            TYPE_DECORATIVE, TYPE_OTHER, TYPE_SPECIAL
    };

    /** 显示串 -> 稳定 ID；未知返回 null（调用方必须显式处理）。 */
    public static String buildingTypeId(String localizedType) {
        if (localizedType == null || localizedType.isEmpty()) {
            return null;
        }
        if (isKnownBuildingTypeId(localizedType)) {
            return localizedType;
        }
        for (int i = 0; i < BUILDING_TYPE_KEYS.length; i++) {
            if (localizedType.equalsIgnoreCase(translate(BUILDING_TYPE_KEYS[i]))) {
                return BUILDING_TYPE_IDS[i];
            }
        }
        return null;
    }

    public static boolean isKnownBuildingTypeId(String id) {
        for (String known : BUILDING_TYPE_IDS) {
            if (known.equals(id)) {
                return true;
            }
        }
        return false;
    }

    /** 稳定 ID -> 当前语言显示串；未知返回 id 本身。 */
    public static String buildingTypeDisplay(String id) {
        for (int i = 0; i < BUILDING_TYPE_IDS.length; i++) {
            if (BUILDING_TYPE_IDS[i].equals(id)) {
                return translate(BUILDING_TYPE_KEYS[i]);
            }
        }
        return id == null ? "" : id;
    }

    // ------------------------------------------------------------------
    // 职业（Vocation1..64）
    // ------------------------------------------------------------------

    private static final int[] KNOWN_VOCATIONS = {
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
            20, 21, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37
    };

    /** 显示串 -> 稳定 ID（vocationN）。找不到返回 ""。 */
    public static String jobIdFromDisplay(String localizedJobName) {
        if (localizedJobName == null || localizedJobName.isEmpty()) {
            return "";
        }
        if (localizedJobName.startsWith("vocation")) {
            return localizedJobName;
        }
        for (int n : KNOWN_VOCATIONS) {
            if (localizedJobName.equals(translate("container.sim.Vocation" + n))) {
                return "vocation" + n;
            }
        }
        // 兜底：1..64 全扫一遍（应对 lang 新增/改名）
        for (int n = 1; n <= 64; n++) {
            if (localizedJobName.equals(translate("container.sim.Vocation" + n))) {
                return "vocation" + n;
            }
        }
        return "";
    }

    /** 稳定 ID -> 当前语言显示串。 */
    public static String jobDisplay(String jobId) {
        if (jobId == null) {
            return "";
        }
        if (!jobId.startsWith("vocation")) {
            return jobId;
        }
        try {
            int n = Integer.parseInt(jobId.substring("vocation".length()));
            return translate("container.sim.Vocation" + n);
        } catch (NumberFormatException e) {
            return jobId;
        }
    }

    public static String vocation(int n) {
        return "vocation" + n;
    }

    // ------------------------------------------------------------------
    // 种族
    // ------------------------------------------------------------------

    public static String raceId(Race race) {
        if (race == null) {
            return "";
        }
        String simple = race.getClass().getSimpleName();
        if (simple.startsWith("Race")) {
            simple = simple.substring("Race".length());
        }
        return simple.toLowerCase();
    }

    public static String raceIdFromDisplay(String localizedRace) {
        if (localizedRace == null) {
            return "";
        }
        for (Race r : Races.raceList) {
            if (r != null && localizedRace.equals(r.raceName)) {
                return raceId(r);
            }
        }
        return "";
    }

    public static Race raceFromId(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        for (Race r : Races.raceList) {
            if (r == null) {
                continue;
            }
            String simple = r.getClass().getSimpleName();
            if (simple.startsWith("Race")) {
                simple = simple.substring("Race".length());
            }
            if (id.equalsIgnoreCase(simple)) {
                return r;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // 特质
    // ------------------------------------------------------------------

    public static String traitId(Trait trait) {
        if (trait == null) {
            return "";
        }
        String simple = trait.getClass().getSimpleName();
        if (simple.startsWith("Trait")) {
            simple = simple.substring("Trait".length());
        }
        return simple.toLowerCase();
    }

    public static Trait traitFromId(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        Trait[] list = Traits.traitList;
        if (list == null) {
            return null;
        }
        for (Trait t : list) {
            if (t == null) {
                continue;
            }
            String simple = t.getClass().getSimpleName();
            if (simple.startsWith("Trait")) {
                simple = simple.substring("Trait".length());
            }
            if (id.equalsIgnoreCase(simple)) {
                return t;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // 物品
    // ------------------------------------------------------------------

    /** 手持物/库存 -> registry name；空返回 ""。 */
    public static String itemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        Item item = stack.getItem();
        if (item == null || item == Items.AIR) {
            return "";
        }
        ResourceLocation name = item.getRegistryName();
        return name == null ? "" : name.toString();
    }

    public static ItemStack itemFromId(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        try {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
            if (item != null && item != Items.AIR) {
                return new ItemStack(item);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    // ------------------------------------------------------------------
    // 工具
    // ------------------------------------------------------------------

    public static String translate(String key) {
        return new TextComponentTranslation(key, new Object[0]).getUnformattedText();
    }
}
