package com.trhsy.sim.npcCode.job;

import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.NpcData;
import com.trhsy.sim.npcCode.SimIds;
import com.trhsy.sim.npcCode.V3;
import com.trhsy.sim.npcCode.build.BuildingBlueprint;
import com.trhsy.sim.npcCode.build.TerrainType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

/**
 * 职业工厂（P1/P3 重写）。
 *
 * <p>旧实现用"当前语言的显示名"做注册键（JobFactory:176-179），换语言后读档
 * 全部找不到职业。现在注册键是稳定 ID（{@code vocationN}），显示名只用于 GUI。
 * 存档只写稳定 ID，读档按 ID 重建。
 */
public class JobFactory {

    @FunctionalInterface
    public interface JobCreator {
        Job create(NpcData data, BlockPos pos, World world, BuildingBlueprint blueprint,
                   int direction, TerrainType terrain);
    }

    private static final Map<String, JobCreator> JOBS = new HashMap<>();

    private static void register(String id, JobCreator creator) {
        JOBS.put(id, creator);
    }

    static {
        // 建筑工
        register(SimIds.vocation(1), (d, p, w, bp, dir, tt) ->
                bp != null ? new JobBuilder(d, bp, p, dir, w) : new JobBuilder(d, p, dir, w));
        // 伐木工
        register(SimIds.vocation(2), (d, p, w, bp, dir, tt) -> new JobLumberjack(d, p, w));
        // 鸡蛋农
        register(SimIds.vocation(3), (d, p, w, bp, dir, tt) -> new JobEggFarmer(d, p, w));
        // 矿工
        register(SimIds.vocation(4), (d, p, w, bp, dir, tt) ->
                new JobMiner(d, p, w, ModSimLoader.getMine(new V3(p.getX(), p.getY(), p.getZ()))));
        // 农民
        register(SimIds.vocation(5), (d, p, w, bp, dir, tt) ->
                new JobFarmer(d, p, w, ModSimLoader.getFarm(new V3(p.getX(), p.getY(), p.getZ()))));
        // 面包师
        register(SimIds.vocation(6), (d, p, w, bp, dir, tt) -> new JobBaker(d, p, w));
        // 士兵
        register(SimIds.vocation(7), (d, p, w, bp, dir, tt) -> new JobSoldier(d, p, w));
        // 牧羊人
        register(SimIds.vocation(8), (d, p, w, bp, dir, tt) -> new JobShepherd(d, p, w));
        // 杂货商
        register(SimIds.vocation(9), (d, p, w, bp, dir, tt) -> new JobMerchant(d, p, w));
        // 快递员
        register(SimIds.vocation(10), (d, p, w, bp, dir, tt) -> new JobCourier(d, p, w));
        // 建筑商
        register(SimIds.vocation(11), (d, p, w, bp, dir, tt) -> new JobBuildersMerchant(d, p, w));
        // 养牛户
        register(SimIds.vocation(12), (d, p, w, bp, dir, tt) ->
                new JobLivestockFarmer(d, p, SimIds.translate("container.sim.job_Livestock_cow"), w));
        // 养猪户
        register(SimIds.vocation(13), (d, p, w, bp, dir, tt) ->
                new JobLivestockFarmer(d, p, SimIds.translate("container.sim.job_Livestock_pig"), w));
        // 养鸡户
        register(SimIds.vocation(14), (d, p, w, bp, dir, tt) ->
                new JobLivestockFarmer(d, p, SimIds.translate("container.sim.job_Livestock_chicken"), w));
        // 屠夫
        register(SimIds.vocation(15), (d, p, w, bp, dir, tt) -> new JobButcher(d, p, w));
        // 规划师
        register(SimIds.vocation(16), (d, p, w, bp, dir, tt) ->
                tt != null ? new JobTerrainFormer(d, tt, p, w) : null);
        // 玻璃制造商
        register(SimIds.vocation(17), (d, p, w, bp, dir, tt) -> new JobGlassMaker(d, p, w));
        // 渔夫
        register(SimIds.vocation(18), (d, p, w, bp, dir, tt) -> new JobFisherman(d, p, w));
        // 牛奶农
        register(SimIds.vocation(20), (d, p, w, bp, dir, tt) -> new JobDairyFarmer(d, p, w));
        // 奶酪匠
        register(SimIds.vocation(21), (d, p, w, bp, dir, tt) -> new JobCheesemaker(d, p, w));
        // 板砖工匠
        register(SimIds.vocation(25), (d, p, w, bp, dir, tt) -> new JobBrickMaker(d, p, w));
        // 食品商
        register(SimIds.vocation(26), (d, p, w, bp, dir, tt) -> new JobGrocer(d, p, w));
        // 养羊户（历史上有 27/28 两个键，都接受）
        JobCreator sheep = (d, p, w, bp, dir, tt) ->
                new JobLivestockFarmer(d, p, SimIds.translate("container.sim.job_Livestock_sheep"), w);
        register(SimIds.vocation(27), sheep);
        register(SimIds.vocation(28), sheep);
        // 养兔户
        register(SimIds.vocation(29), (d, p, w, bp, dir, tt) ->
                new JobLivestockFarmer(d, p, SimIds.translate("container.sim.job_Livestock_rabbit"), w));
        // 制糖师
        register(SimIds.vocation(30), (d, p, w, bp, dir, tt) -> new JobSugar(d, p, w));
        // 行长
        register(SimIds.vocation(31), (d, p, w, bp, dir, tt) -> new JobATM(d, p, w));
        // 插花师
        register(SimIds.vocation(32), (d, p, w, bp, dir, tt) -> new JobFlower(d, p, w));
        // 赤脚大夫
        register(SimIds.vocation(33), (d, p, w, bp, dir, tt) -> new JobBarefootDoctor(d, p, w));
        // 妇产科医生
        register(SimIds.vocation(34), (d, p, w, bp, dir, tt) -> new JobAccoucheur(d, p, w));
        // 麦当劳老板
        register(SimIds.vocation(35), (d, p, w, bp, dir, tt) -> new JobMcDonald(d, p, w));
        // 汉堡店老板
        register(SimIds.vocation(36), (d, p, w, bp, dir, tt) -> new JobBurgers(d, p, w));
        // 酒保
        register(SimIds.vocation(37), (d, p, w, bp, dir, tt) -> new JobBartender(d, p, w));
    }

    /**
     * 按稳定 ID 创建职业。
     *
     * @param jobId     vocationN（也兼容旧的显示名，会先转换）
     * @param site      工作地点；为 null 时退回 data.tempEmployLoc
     * @param blueprint 建筑工蓝图（可为 null）
     * @param direction 建筑工方向
     * @param terrain   规划师地形（可为 null）
     */
    public static Job createJobAt(NpcData data, String jobId, V3 site, BuildingBlueprint blueprint,
                                  int direction, TerrainType terrain, World world) {
        if (data == null || jobId == null || jobId.isEmpty()) {
            return null;
        }
        String stableId = jobId;
        JobCreator creator = JOBS.get(stableId);
        if (creator == null) {
            stableId = SimIds.jobIdFromDisplay(jobId);
            creator = JOBS.get(stableId);
        }
        if (creator == null) {
            ModSimLoader.log.warn("未知职业ID: {} (NPC {})", jobId, data.ID);
            return null;
        }
        if (site == null) {
            site = data.tempEmployLoc;
        }
        if (site == null) {
            ModSimLoader.log.warn("职业 {} 缺少工作地点，NPC {}", jobId, data.ID);
            return null;
        }
        try {
            Job job = creator.create(data, site.toBlockPos(), world, blueprint, direction, terrain);
            if (job != null && job.workPlace == null) {
                job.workPlace = site;
            }
            return job;
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("JobFactory创建职业失败 {}: {} 行数:{}", jobId, e.getMessage(), element.getLineNumber());
            return null;
        }
    }

    /** 兼容旧调用：解析 "jobId;x,y,z;..." 字符串。 */
    public static Job createJob(NpcData npcData, String jobValue, BuildingBlueprint blueprint, World world) {
        if (jobValue == null || jobValue.isEmpty()) {
            return null;
        }
        String[] parts = jobValue.split(";");
        String jobId = parts[0];
        V3 site = npcData == null ? null : npcData.tempEmployLoc;
        int direction = 0;
        TerrainType terrain = null;
        if (parts.length >= 2) {
            try {
                site = V3.fromString(parts[1]);
            } catch (Exception ignored) {
            }
        }
        if (parts.length >= 4) {
            try {
                terrain = new TerrainType(parts[2], parts[3]);
            } catch (Exception ignored) {
            }
        } else if (parts.length >= 3) {
            try {
                direction = Integer.parseInt(parts[2]);
            } catch (Exception ignored) {
            }
        }
        return createJobAt(npcData, jobId, site, blueprint, direction, terrain, world);
    }
}
