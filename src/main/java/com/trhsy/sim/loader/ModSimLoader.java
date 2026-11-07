package com.trhsy.sim.loader;

import com.trhsy.sim.entity.EntityNpc;
import com.trhsy.sim.network.client.PacketUpdateMoney;
import com.trhsy.sim.npcCode.DynamicSkin;
import com.trhsy.sim.npcCode.NpcData;
import com.trhsy.sim.npcCode.NpcSkin;
import com.trhsy.sim.npcCode.SimIds;
import com.trhsy.sim.npcCode.V3;
import com.trhsy.sim.npcCode.block.FarmBox;
import com.trhsy.sim.npcCode.block.MineBox;
import com.trhsy.sim.npcCode.build.Building;
import com.trhsy.sim.npcCode.build.BuildingBlueprint;
import com.trhsy.sim.npcCode.build.TerrainTypeRequitrements;
import com.trhsy.sim.util.Courier;
import com.trhsy.sim.util.CourierTask;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.loader
 * @ClassName: ModSimLoader
 * @Description: sim 服务器加载信息
 * @date 2023/10/19 下午 2:12
 */
public class ModSimLoader {
    /**
     * 全局日志调用
     */
    public static Logger log;
    /**
     * npc 数据
     **/
    public static Queue<NpcData> folks = new ConcurrentLinkedQueue<>();
    /**
     * 建筑
     **/
    public static List<Building> buildings = new CopyOnWriteArrayList();
    /*养殖箱*/
    public static List<FarmBox> farms = new CopyOnWriteArrayList();
    /*挖矿箱*/
    public static List<MineBox> mines = new CopyOnWriteArrayList();
    public static List<NpcSkin> folkSkins = new CopyOnWriteArrayList();
    /*
   所有快递点
    */
    public static List<Courier> theCourierPoints = new CopyOnWriteArrayList();
    public static List<CourierTask> theCourierTask = new CopyOnWriteArrayList();
    /**
     * 建筑蓝图
     */
    public static List<BuildingBlueprint> buildingBlueprints = new CopyOnWriteArrayList();

    public static List<TerrainTypeRequitrements> terrainTypeReqs = new CopyOnWriteArrayList();

    /**
     * 是否加载npc
     **/
    public static boolean hasLoadedFolks = false;
    /**
     * npc 皮肤
     **/
    public static List<DynamicSkin> skins = new CopyOnWriteArrayList();

    /**
     * 游戏模式编号 999 未开启模式   0正常模式   1创造模式
     **/
    public static int gamemode = 999;
    //金钱
    public static float money = 10.0F;
    /**
     * @Author fan
     * @Description //TODO 周的某天
     * @Date 11:15 2023/4/29
     * @Param
     * @return
     **/
    public static int dayOfWeek = 0;
    public static int gameDay = 0;
    /**模拟城市是否开始运行*/
    public static Boolean sim_is_running = false;

    /**
     * 获取模拟城市建筑文文件夹
     *
     * @return
     */
    public static String getSimFolder() {
        try {
            String strmc = (new File(".")).getAbsolutePath();
            strmc = strmc.substring(0, strmc.length() - 1);
            File checks = new File(strmc + File.separator + "resources" + File.separator + "sim");
            if (!checks.exists() && !checks.isDirectory()) {
                // P0: 只警告一次（原来每次调用都刷一条，专服启动能刷十几条）
                if (!simFolderWarned) {
                    simFolderWarned = true;
                    ModSimLoader.log.warn("SimCity: 资源目录不存在，已创建: " + checks.getAbsolutePath());
                }
                checks.mkdir();
            }
            return (checks).getAbsolutePath();
        } catch (Exception e) {
            return "";
        }
    }

    private static boolean simFolderWarned = false;

    /**
     * @return void
     * @Author fan
     * @Description //TODO 添加金额
     * @Date 14:05 2022/10/18
     * @Param [amount]
     **/
    public static void addMoney(float amount) {
        if (gamemode != 1) {
            money += amount;
            NetWorkLoader.net.sendToAll(new PacketUpdateMoney());
        }

    }

    /**
     * 显示金额格式
     **/
    public static String displayMoney(float money) {
        String output = null;
        try {
            DecimalFormat myFormatter = new DecimalFormat("#,##0.00");
            output = myFormatter.format(money);
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("displayMoney出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
        return output;
    }


    /**
     * 以字符串形式获取“.minecraft/saves/游戏世界名称/sim/”文件夹 保存数据文件夹
     *
     * @return
     */
    public static String getSavesDataFolder() {
        String ret = "";
        try {
            File worldPath = DimensionManager.getCurrentSaveRootDirectory().getAbsoluteFile();
            File mainFolder = new File(worldPath.getAbsolutePath() + File.separator + "sim" + File.separator);
            ret = mainFolder.getAbsolutePath();
            /*String strmc = (new File(".")).getAbsolutePath();
            strmc = strmc.substring(0, strmc.length() - 1);
            File test = new File(strmc + "saves");
            if (test.exists()) {
                //客户端
                ret = (new File(strmc + File.separator + "saves" + File.separator  + "sim" + File.separator)).getAbsolutePath() + File.separator;
            } else {
                //服务器端
                strmc = strmc + File.separator + "sim" + File.separator;
                ret = (new File(strmc)).getAbsolutePath();
            }
            File f = new File(ret);*/
            if (!mainFolder.exists()) {
                mainFolder.mkdirs();
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("getSavesDataFolder出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
        return ret;
    }

    /**
     * 从配置文件中读取内容
     *
     * @param fullFilename
     * @return
     */
    public static List<String> loadSK2(String fullFilename) {
        CopyOnWriteArrayList ret = new CopyOnWriteArrayList();

        try {
            InputStream inputStream =new FileInputStream(new File(fullFilename));
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            for (String line = br.readLine(); line != null; line =br.readLine()) {
                ret.add(line);
            }

            br.close();
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("loadSK2出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }

        return ret;
    }

    /**
     * @return com.trhsy.sim.npc.Building
     * @Author fan
     * @Description //TODO 找到空房子
     * @Date 14:05 2022/10/18
     * @Param []
     **/
    public static Building getEmptyHome() {
        Building empty = null;
        for (Building b : buildings) {
            //住宅的空房子 住宅
            String buildingType = new TextComponentTranslation("container.sim.sim_gui_BC_Residential",new Object[0]).getUnformattedText();
            if (b.buildingType.contentEquals(buildingType) && b.occupants.size() < 1) {
                empty = b;
                break;
            }else{
                List<NpcData> npcDatas=b.occupants;
                for (NpcData n:npcDatas){
                    if(n.entity.isDead){
                        b.occupants.clear();
                    }
                }
            }
        }
        return empty;
    }

    /**
     * 判断是否白天 当世界上是白天时返回true，忽略其他世界时间
     *
     * @return
     */
    public static boolean isDayTime(World world) {
        //if (MinecraftServer.getServer().worldServers[0].getWorldInfo().getWorldTime() % 24000 <= 11999) {
        //    return true;
        //} else {
        //    return false;
        //}
        boolean falg = false;
        try {
            falg = world.getWorldInfo().getWorldTime() % 24000L <= 11999L;
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("isDayTime出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
        return falg;

    }

    /**
     * 帮助功能，向所有世界/维度的所有玩家发送聊天信息
     *
     * @param theText
     */
    public static void sendChat(String theText) {
        try {
//            MinecraftServer.getServer().playerList.getPlayerList().sendChatMsg(new TextComponentString(theText));
            FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().sendMessage(new TextComponentString(theText));

        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("sendChat出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    /**
     * @return java.lang.String
     * @Author fan
     * @Description //TODO 下载version文件并返回内容
     * @Date 20:26 2022/10/8
     * @Param [url, localFile]
     **/
    public static String downloadFile(String url, String localFile) {
        String ret = "";
        File f = new File(localFile);
        if (f.exists()) {
            deleteFile(f);
        }
        ModSimLoader.log.info("将从此链接下载文件：\n" + url);
        try {
            URL aURL = new URL(url);
            InputStream is = aURL.openStream();
            BufferedInputStream in = new BufferedInputStream(is);
            FileOutputStream fos = new FileOutputStream(localFile);
            BufferedOutputStream bout = new BufferedOutputStream(fos, 1024);
            byte[] data = new byte[4096];
            boolean var8 = false;

            int x;
            while ((x = in.read(data, 0, 4096)) >= 0) {
                bout.write(data, 0, x);
            }

            bout.flush();
            ret = new String(data);
            bout.close();
            in.close();
        } catch (Exception e) {
            ret = "";
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("下载version文件并返回内容出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }

        return ret;
    }

    public static String downloadSimFile(String url, String localFile) {
        File f = new File(localFile);
        if (f.exists()) {
            deleteFile(f);
        }
        ModSimLoader.log.info("将从此链接下载文件：\n" + url);
        try {
            URL aURL = new URL(url);
            InputStream is = aURL.openStream();
            BufferedInputStream in = new BufferedInputStream(is);
            FileOutputStream fos = new FileOutputStream(localFile);
            BufferedOutputStream bout = new BufferedOutputStream(fos, 1024);
            byte[] data = new byte[4096];
            boolean var8 = false;

            int x;
            while ((x = in.read(data, 0, 4096)) >= 0) {
                bout.write(data, 0, x);
            }

            bout.flush();
            //ret = new String(data);
            bout.close();
            in.close();
        } catch (Exception e) {
        }

        return localFile;
    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 删除旧文件
     * @Date 20:25 2022/10/8
     * @Param [file]
     **/
    public static void deleteFile(File file) {
        ModSimLoader.log.info("开始删除文件/文件夹");
        if (file.exists()) {
            file.delete();
        }
        if (file.exists()) {
            File[] paths = file.listFiles();
            for (File str : paths) {
                deleteFile(str);
            }
            file.delete();
            paths = null;    // lets gc do its works
        }
        file = null;    // lets gc do its works
    }

    /**
     * 保存配置文件
     *
     * @param fullFilename
     * @param strings
     */
    public static void saveSK2(String fullFilename, List<String> strings) {
        try {
            File f = new File(fullFilename);
            if (!f.exists()) {
                f.createNewFile();
            }
            BufferedWriter bw = new BufferedWriter(new FileWriter(fullFilename));
            for (String line : strings) {
                bw.write(line + "\r\n");
            }
            bw.close();
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("saveSK2出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }

    }

    /**
     * @return com.trhsy.sim.npc.NpcData
     * @Author fan
     * @Description //TODO 根据uid获取NPC数据
     * @Date 14:04 2022/10/18
     * @Param [uid]
     **/
    public static NpcData getFolkDataByUID(UUID uid) {
        NpcData npcDatas = null;
        for (NpcData npcData : ModSimLoader.folks) {
            if (npcData.ID.equals(uid)) {
                npcDatas = npcData;
                return npcDatas;
            }
        }
        return npcDatas;
    }

    /**
     * P11(S1): 人口上限检查。0 或负数表示不限制。
     * 只用于自动增长/生育；命令生成不走这里（由玩家显式操作）。
     */
    public static boolean canGrowPopulation() {
        int limit = ConfigLoader.configPopulationLimit;
        return limit <= 0 || ModSimLoader.folks.size() < limit;
    }

    public static EntityNpc getEntityFolkByUID(UUID uid) {
        EntityNpc entityFolk = null;
        if (uid == null) {
            return null;
        }
        for (NpcData npcData : ModSimLoader.folks) {
            // P9: UUID 必须用 equals（原来 == 几乎恒为 false，只碰巧同对象时成立）
            if (uid.equals(npcData.ID)) {
                entityFolk = npcData.entity;
                return entityFolk;
            }
        }
        return entityFolk;
    }




    /**
     * @return java.util.List<com.trhsy.sim.npc.build.BuildingBlueprint>
     * @Author fan
     * @Description //TODO 得到建筑蓝图
     * @Date 14:41 2022/10/21
     * @Param [type, searchText]
     **/
    public static List<BuildingBlueprint> getBlueprintsByType(String type, String searchText) {
        List<BuildingBlueprint> typedBlues = new CopyOnWriteArrayList<BuildingBlueprint>();
        // P9: 类型可能是稳定 ID，也可能是显示名，统一归一化后再比较；
        // 搜索串判空用 isEmpty（原来 `searchText != ""` 是引用比较）。
        String wantId = SimIds.buildingTypeId(type);
        for (BuildingBlueprint bb : buildingBlueprints) {
            boolean typeMatch = (wantId != null && wantId.equals(bb.buildingType))
                    || (bb.buildingType != null && bb.buildingType.equals(type));
            if (!typeMatch) {
                continue;
            }
            if (searchText == null || searchText.isEmpty()
                    || (bb.name != null && bb.name.contains(searchText))) {
                typedBlues.add(bb);
            }
        }
        return typedBlues;

    }

    /**
     * @return com.trhsy.sim.npc.build.BuildingBlueprint
     * @Author fan
     * @Description //TODO 根据名字获取蓝图
     * @Date 20:04 2022/11/4
     * @Param [name]
     **/
    public static BuildingBlueprint getBlueprintsByName(String name) {
        BuildingBlueprint buildingBlueprint = null;
        for (BuildingBlueprint bb : buildingBlueprints) {
            if (name.equals(bb.name)) {
                buildingBlueprint = bb;
            }
        }
        return buildingBlueprint;
    }

    /**
     * @return boolean
     * @Author fan
     * @Description //TODO 是建筑中的块
     * @Date 14:46 2022/10/21
     * @Param [v3]
     **/
    public static boolean isBlockInBuilding(V3 v3) {
        Iterator var1 = buildings.iterator();

        while (var1.hasNext()) {
            Building b = (Building) var1.next();
            Iterator var3 = b.structure.iterator();

            while (var3.hasNext()) {
                V3 bv3 = (V3) var3.next();
                if (v3.equals(bv3)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * @return java.util.List<com.trhsy.sim.block.FarmBox>
     * @Author fan
     * @Description //TODO 获得最近的农场
     * @Date 14:46 2022/10/21
     * @Param [pos]
     **/
    public static List<FarmBox> getClosestFarm(final V3 pos, String fType) {
        List<FarmBox> fs = new CopyOnWriteArrayList<FarmBox>();
        // P4: 供货用——不再要求农场必须有雇员（有库存就该能取货）；
        // 只读内存对象，按距离排序后返回。
        for (FarmBox f : farms) {
            if (f == null || f.farmType == null) {
                continue;
            }
            if (fType == null || f.farmType.toString().equals(fType)) {
                fs.add(f);
            }
        }
        if (pos != null) {
            fs.sort((a, b) -> {
                if (a.loc == null || b.loc == null) {
                    return 0;
                }
                return Double.compare(a.loc.distanceSq(pos), b.loc.distanceSq(pos));
            });
        }
        return fs;
    }

    /**
     * @return com.trhsy.sim.block.MineBox
     * @Author fan
     * @Description //TODO 矿场
     * @Date 14:46 2022/10/21
     * @Param [pos]
     **/
    public static MineBox getMine(V3 pos) {
        Iterator var1 = mines.iterator();
        MineBox f;
        do {
            if (!var1.hasNext()) {
                return null;
            }

            f = (MineBox) var1.next();
        } while (!f.loc.equals(pos));

        return f;
    }

    /**
     * @return com.trhsy.sim.block.FarmBox
     * @Author fan
     * @Description //TODO 农场
     * @Date 14:46 2022/10/21
     * @Param [pos]
     **/
    public static FarmBox getFarm(V3 pos) {
        Iterator var1 = farms.iterator();

        FarmBox f;
        do {
            if (!var1.hasNext()) {
                return null;
            }

            f = (FarmBox) var1.next();
        } while (!f.loc.equals(pos));

        return f;
    }

    /**
     * @return java.util.List<com.trhsy.sim.npc.build.Building>
     * @Author fan
     * @Description //TODO 根据工作获取最近的建筑
     * @Date 15:53 2022/10/21
     * @Param [jobType, pos]
     **/
    public static List<Building> getClosestBuildingByJob(String jobType, final V3 pos) {
        List<Building> bs = new CopyOnWriteArrayList<Building>();
        // P4: 用稳定职业 ID 匹配（不再用本地化字符串逐字比较，换语言/翻译改动都能对上），
        // 且不再强制要求生产建筑有住户（有库存就该能取货），按距离排序。
        String wantId = SimIds.jobIdFromDisplay(jobType);
        for (Building b : buildings) {
            if (b == null || b.jobType == null || b.jobType.isEmpty()) {
                continue;
            }
            String bid = SimIds.jobIdFromDisplay(b.jobType);
            boolean match = (!wantId.isEmpty() && wantId.equals(bid))
                    || (wantId.isEmpty() && b.jobType.contentEquals(jobType));
            if (match) {
                bs.add(b);
                ModSimLoader.log.info("找到供货建筑： " + b.buildingName + " (" + b.jobType + ")");
            }
        }
        if (pos != null) {
            bs.sort((a, b2) -> {
                if (a.controlXYZ == null || b2.controlXYZ == null) {
                    return 0;
                }
                return Double.compare(a.controlXYZ.distanceSq(pos), b2.controlXYZ.distanceSq(pos));
            });
        }
        return bs;
    }

    /**
     * @return java.util.List<com.trhsy.sim.npc.build.Building>
     * @Author fan
     * @Description //TODO 获取最近的建筑
     * @Date 15:53 2022/10/21
     * @Param [buildIn, pos]
     **/
    public static List<Building> getClosestBuilding(String buildIn, final V3 pos) {
        List<Building> bs = new CopyOnWriteArrayList();
        Iterator var3 = buildings.iterator();

        while (var3.hasNext()) {
            Building b = (Building) var3.next();
            if (b.buildingName == buildIn) {
                bs.add(b);
            }
        }

        Collections.sort(bs, new Comparator<Building>() {
            @Override
            public int compare(Building b1, Building b2) {
                if (b1.controlXYZ.getDistanceTo(pos) > b2.controlXYZ.getDistanceTo(pos)) {
                    return 1;
                } else {
                    return b1.controlXYZ.getDistanceTo(pos) < b2.controlXYZ.getDistanceTo(pos) ? -1 : 0;
                }
            }
        });
        return bs;
    }

    /**
     * @return com.trhsy.sim.npc.build.Building
     * @Author fan
     * @Description //TODO 建筑位置
     * @Date 15:53 2022/10/21
     * @Param [pos]
     **/
    public static Building getBuildingByV3(V3 pos) {
        if (pos == null) {
            return null;
        }
        for (int i = 0; i < buildings.size(); ++i) {
            Building building=buildings.get(i);
            if (building == null || building.controlXYZ == null) {
                continue;
            }
            if (building.controlXYZ.toString().contentEquals(pos.toString())
                    || (building.livingXYZ != null && building.livingXYZ.toString().contentEquals(pos.toString()))) {
                return building;
            }
        }

        return null;
    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 加载建筑
     * @Date 15:53 2022/10/21
     * @Param []
     **/
    public static void loadAllBuildings() {
        ModSimLoader.buildingBlueprints.clear();
        String lang = FMLCommonHandler.instance().getCurrentLanguage();
        //建筑文件检查
        File checks = new File(ModSimLoader.getSimFolder() + File.separator + "buildings_"+lang);
        if (!checks.exists()) {
            //onUpdate();
        }
        //开始加载所有建筑
        //商业
        File[] comBuildings = (new File(getSimFolder() + File.separator + "buildings_"+lang + File.separator + "commercial")).listFiles();
        if (comBuildings != null) {
            for (int i = 0; i < comBuildings.length; i++) {
                File buildingFolder = comBuildings[i];
                BuildingBlueprint b = new BuildingBlueprint(buildingFolder);
                ModSimLoader.buildingBlueprints.add(b);
            }
        }
        File[] decBuildings = (new File(getSimFolder() + File.separator + "buildings_"+lang + File.separator + "decorative")).listFiles();
        if (decBuildings != null) {
            for (int i = 0; i < decBuildings.length; i++) {
                File buildingFolder = decBuildings[i];
                BuildingBlueprint b = new BuildingBlueprint(buildingFolder);
                ModSimLoader.buildingBlueprints.add(b);
            }
        }
        File[] indBuildings = (new File(getSimFolder() + File.separator + "buildings_"+lang + File.separator + "industrial")).listFiles();
        if (indBuildings != null) {
            for (int i = 0; i < indBuildings.length; i++) {
                File buildingFolder = indBuildings[i];
                BuildingBlueprint b = new BuildingBlueprint(buildingFolder);
                ModSimLoader.buildingBlueprints.add(b);
            }
        }
        File[] othBuildings = (new File(getSimFolder() + File.separator + "buildings_"+lang + File.separator + "other")).listFiles();
        if (othBuildings != null) {
            for (int i = 0; i < othBuildings.length; i++) {
                File buildingFolder = othBuildings[i];
                BuildingBlueprint b = new BuildingBlueprint(buildingFolder);
                ModSimLoader.buildingBlueprints.add(b);
            }
        }
        File[] resBuildings = (new File(getSimFolder() + File.separator + "buildings_"+lang + File.separator + "residential")).listFiles();
        if (resBuildings != null) {
            for (int i = 0; i < resBuildings.length; i++) {
                File buildingFolder = resBuildings[i];
                BuildingBlueprint b = new BuildingBlueprint(buildingFolder);
                ModSimLoader.buildingBlueprints.add(b);
            }
        }
        File[] speBuildings = (new File(getSimFolder() + File.separator + "buildings_"+lang + File.separator + "special")).listFiles();
        if (speBuildings != null) {
            for (int i = 0; i < speBuildings.length; i++) {
                File buildingFolder = speBuildings[i];
                BuildingBlueprint b = new BuildingBlueprint(buildingFolder);
                ModSimLoader.buildingBlueprints.add(b);
            }
        }
        Collections.sort(ModSimLoader.buildingBlueprints);
    }

    public static void onUpdate() {
        try {
//"https://www.dropbox.com/s/i51v1lsq0u89elw/";
            /*String baseURL = "https://trhsy.github.io/sim/1.9/Simukraft_zh_CN.zip";
            String lang = FMLCommonHandler.instance().getCurrentLanguage();
            if ("en_US".equals(lang)) {
                baseURL = "https://trhsy.github.io/sim/1.9/Simukraft_en_US.zip";
            }
            String unzipFilePath = ModSimLoader.getSimFolder();
            File checks = new File(unzipFilePath + File.separator);
            File[] checkss = checks.listFiles();

            for (File f : checkss) {
                ModSimLoader.deleteFile(f);
            }
            checks.mkdir();
            String simFile = unzipFilePath + File.separator + "Simukraft.zip";
            String ver = ModSimLoader.downloadSimFile(baseURL, simFile);
            if (ver != null) {
                File zipFile = new File(ver);
                //开始解压
                ModSimLoader.log.info("开始解压：", zipFile.getName());
                ZipEntry entry = null;
                String entryFilePath = null, entryDirPath = null;
                File entryFile = null, entryDir = null;
                int index = 0, count = 0;
                byte[] buffer = new byte[1024];
                BufferedInputStream bis = null;
                BufferedOutputStream bos = null;
                ZipFile zip = new ZipFile(zipFile);
                Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) zip.entries();
                //循环对压缩包里的每一个文件进行解压
                while (entries.hasMoreElements()) {

                    entry = entries.nextElement();

                    //构建压缩包中一个文件解压后保存的文件全路径
                    entryFilePath = unzipFilePath + File.separator + entry.getName();
                    //构建解压后保存的文件夹路径
                    index = entryFilePath.lastIndexOf(".txt");
                    if (index != -1) {
                        //创建解压文件
                        entryFile = new File(entryFilePath);
                        //写入文件
                        bos = new BufferedOutputStream(new FileOutputStream(entryFile));
                        bis = new BufferedInputStream(zip.getInputStream(entry));
                        while ((count = bis.read(buffer, 0, 1024)) != -1) {
                            bos.write(buffer, 0, count);
                        }
                        bos.flush();
                        bos.close();
                        //ModSimReloaded.log.info("创建解压文件：",entryFile.getName());
                    } else {
                        entryDirPath = entryFilePath.substring(0, entryFilePath.length() - 1);
                        entryDir = new File(entryDirPath);
                        //如果文件夹路径不存在，则创建文件夹
                        if (!entryDir.exists() || !entryDir.isDirectory()) {
                            entryDir.mkdirs();
                            ModSimLoader.log.info("创建解压文件夹：", entryDir.getName());
                        }
                    }

                }

            }

            new File(simFile).deleteOnExit();
*/
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("检查sim建筑包出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }


    }

    /**
     * @return com.trhsy.sim.npc.build.Building
     * @Author fan
     * @Description //TODO 根据uid获取建筑
     * @Date 15:54 2022/10/21
     * @Param [uuid]
     **/
    public static Building getBuildingByUUID(String uuid) {
        Building b1 = null;
        for (Building b : buildings) {
            if (b.ID.toString().contentEquals(uuid)) {
                b1 = b;
                return b1;
            }
        }
        return b1;
    }

    public static Building getBuildingByName(String name) {
        Building b1 = null;
        for (Building b : buildings) {
            if (b.buildingName.toString().contentEquals(name)) {
                b1 = b;
                return b1;
            }
        }
        return b1;
    }



    public static void saveStates() {
        try {
            String folder = ModSimLoader.getSavesDataFolder();
            List<String> strings = new CopyOnWriteArrayList();
            //金额
            strings.add("credits|" + money);
            //游戏状态
            strings.add("gamemode|" + gamemode);
            //星期几
            strings.add("dayofweek|" + dayOfWeek);
            //第几天
            strings.add("gameDay|" + gameDay);

            //是否运行
            strings.add("is_running|" + sim_is_running);


            ModSimLoader.saveSK2(folder + "settings.sk2", strings);
            ModSimLoader.log.info("游戏状态: saveStates() called BOTH sides, 金额存储为 " + money);
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("saveStates出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }

    }

    /**
     * 加载配置文件
     */
    public static void loadStates() {
        try {
            File f = new File(ModSimLoader.getSavesDataFolder() + "settings.sk2");
            if (!f.exists()) {
                saveStates();
            } else {
                loadStates2();
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("loadStates出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    /**
     * 从配置文件读取并写入
     */
    private static void loadStates2() {
        try {
            List<String> strings = ModSimLoader.loadSK2(ModSimLoader.getSavesDataFolder() + "settings.sk2");
            for (String line : strings) {
                if (line.contains("|")) {
                    int m1 = line.indexOf("|");
                    String name = line.substring(0, m1);
                    String value = line.substring(m1 + 1);
                    if ("credits".equals(name)) {
                        money = Float.parseFloat(value);
                    } else if ("gamemode".equals(name)) {
                        gamemode = Integer.parseInt(value);
                    } else if ("dayofweek".equals(name)) {
                        dayOfWeek = Integer.parseInt(value);
                    }else if ("gameDay".equals(name)) {
                        gameDay = Integer.parseInt(value);
                    } else if ("is_running".equals(name)) {
                        sim_is_running = Boolean.valueOf(value);
                    }
                }
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("loadStates2出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }


    }

}
