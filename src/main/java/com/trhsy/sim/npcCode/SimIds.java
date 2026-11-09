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

import java.util.HashMap;
import java.util.Map;

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

    /**
     * 中文职业名 -> 稳定 ID。
     *
     * <p>专服语言固定 en_US，但蓝图/建筑里的 JOB/jobtypes 可能来自中文客户端，
     * 存的是中文显示名。只按当前语言反查会导致“中文建筑找不到英文供货建筑”，
     * 例如奶酪匠找不到“牛奶农”的奶牛场。这里补一张双语反查表。
     */
    private static final Map<String, String> ZH_JOB_IDS = new HashMap<>();

    static {
        ZH_JOB_IDS.put("建筑工", "vocation1");
        ZH_JOB_IDS.put("伐木工", "vocation2");
        ZH_JOB_IDS.put("鸡蛋农", "vocation3");
        ZH_JOB_IDS.put("矿工", "vocation4");
        ZH_JOB_IDS.put("农民", "vocation5");
        ZH_JOB_IDS.put("面包师", "vocation6");
        ZH_JOB_IDS.put("士兵", "vocation7");
        ZH_JOB_IDS.put("牧羊人", "vocation8");
        ZH_JOB_IDS.put("杂货商", "vocation9");
        ZH_JOB_IDS.put("快递员", "vocation10");
        ZH_JOB_IDS.put("建筑商", "vocation11");
        ZH_JOB_IDS.put("养牛户", "vocation12");
        ZH_JOB_IDS.put("养猪户", "vocation13");
        ZH_JOB_IDS.put("养鸡户", "vocation14");
        ZH_JOB_IDS.put("屠夫", "vocation15");
        ZH_JOB_IDS.put("规划师", "vocation16");
        ZH_JOB_IDS.put("玻璃制造商", "vocation17");
        ZH_JOB_IDS.put("渔夫", "vocation18");
        ZH_JOB_IDS.put("路径生成器", "vocation19");
        ZH_JOB_IDS.put("牛奶农", "vocation20");
        ZH_JOB_IDS.put("奶酪匠", "vocation21");
        ZH_JOB_IDS.put("汉堡店经理", "vocation22");
        ZH_JOB_IDS.put("油炸厨师", "vocation23");
        ZH_JOB_IDS.put("服务员", "vocation24");
        ZH_JOB_IDS.put("板砖工匠", "vocation25");
        ZH_JOB_IDS.put("食品商", "vocation26");
        ZH_JOB_IDS.put("养羊户", "vocation28");
        ZH_JOB_IDS.put("养兔户", "vocation29");
        ZH_JOB_IDS.put("制糖师", "vocation30");
        ZH_JOB_IDS.put("行长", "vocation31");
        ZH_JOB_IDS.put("插花师", "vocation32");
        ZH_JOB_IDS.put("赤脚大夫", "vocation33");
        ZH_JOB_IDS.put("妇产科医生", "vocation34");
        ZH_JOB_IDS.put("麦当劳老板", "vocation35");
        ZH_JOB_IDS.put("汉堡店老板", "vocation36");
        ZH_JOB_IDS.put("酒保", "vocation37");
    }

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
        // 专服 en_US 下，中文客户端/中文蓝图留下的中文 jobType
        String zh = ZH_JOB_IDS.get(localizedJobName);
        if (zh != null) {
            return zh;
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
