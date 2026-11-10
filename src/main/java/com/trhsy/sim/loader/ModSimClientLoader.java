package com.trhsy.sim.loader;


import com.trhsy.sim.gui.GuiHud;
import com.trhsy.sim.gui.GuiRunMod;
import com.trhsy.sim.gui.block.*;
import com.trhsy.sim.gui.npc.*;
import com.trhsy.sim.network.client.*;
import com.trhsy.sim.npcCode.NpcData;
import com.trhsy.sim.npcCode.NpcIdentity;
import com.trhsy.sim.npcCode.NpcSkin;
import com.trhsy.sim.npcCode.V3;
import com.trhsy.sim.npcCode.block.Marker;
import com.trhsy.sim.npcCode.build.BlueprintRequirements;
import com.trhsy.sim.npcCode.build.BuildingBlueprint;
import com.trhsy.sim.npcCode.enums.FarmType;
import com.trhsy.sim.npcCode.logistics.LogisticsSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @ClassName ModSimClientLoader
 * @Description todo
 * @Author TRHSY
 * @Date 2023/4/2910:57
 **/
public class ModSimClientLoader {
    //客户端皮肤
    public static List<NpcSkin> folkSkins = new CopyOnWriteArrayList();

    /** 物流中心 GUI 最近一次服务端快照。 */
    public static LogisticsSnapshot logisticsSnapshot;

    /** 收到物流快照后更新缓存；如果正停在物流页则重绘。 */
    public static void applyLogisticsSnapshot(LogisticsSnapshot snapshot) {
        logisticsSnapshot = snapshot;
        if (Minecraft.getMinecraft().currentScreen instanceof GuiBlockControllerBlock) {
            ((GuiBlockControllerBlock) Minecraft.getMinecraft().currentScreen).refreshLogisticsPage();
        }
    }

    public static List<NpcData> folks = new CopyOnWriteArrayList();
    /**
     * 临时可雇佣Npc姓名
     **/
    public static List<NpcIdentity> tempHireableNpcNames = new CopyOnWriteArrayList();
    /**
     * @Author fan
     * @Description //TODO 蓝图需求
     * @Date 11:00 2023/4/29
     * @Param 
     * @return 
     **/
    public static List<BlueprintRequirements> blueprintReqs = new CopyOnWriteArrayList();
    /**
     * @Author fan
     * @Description //TODO 标记
     * @Date 11:05 2023/4/29
     * @Param 
     * @return 
     **/
    public static List<Marker> markers = new CopyOnWriteArrayList();

    /**
     * 构建上一页
     **/
    public static int constructorPreviousPage;

    /**
     * 蓝图
     */
    public static BuildingBlueprint savedBlueprint;

    /**
     * 要构建的
     **/
    public static BlockPos previewConstructor;

    /**
     * 预览位置
     */
    public static Vec3d previewPos1;
    public static Vec3d previewPos2;
    public static int gamemode;
    //周几
    public static int dayOfWeek;
    public static int gameDay;
    public static float money;

    /**模拟城市是否开始运行*/
    public static Boolean sim_is_running=false;
    public ModSimClientLoader() {
    }
    /**
     * @return java.util.List<com.trhsy.sim.entity.util.NpcIdentity>
     * @Author fan
     * @Description //TODO 失业人员 获得失业人员
     * @Date 17:59 2022/10/19
     * @Param []
     **/
    public static List<NpcIdentity> getUnemployedFolks() {
        List<NpcIdentity> hireables = new CopyOnWriteArrayList<NpcIdentity>();
        for (NpcIdentity cfi : tempHireableNpcNames) {

            if (cfi.job.contentEquals(new TextComponentTranslation("container.sim.folkData1",new Object[0]).getUnformattedText()) && Integer.parseInt(cfi.age) >= Integer.parseInt(cfi.maturityAge)) {
                hireables.add(cfi);
            }
        }

        return hireables;
    }
    /**
     * @Author fan
     * @Description //TODO 从UUID获取皮肤路径
     * @Date 11:09 2023/4/29
     * @Param [UUID]
     * @return java.lang.String
     **/
    public static String getPathFromUUID(UUID UUID) {

        for(int i = 0; i < folkSkins.size(); ++i) {
            NpcSkin npcSkin=folkSkins.get(i);
            if (npcSkin!=null&&npcSkin.UUID.equals(UUID.toString())) {
                return (folkSkins.get(i)).skinPath;
            }
        }
        return "male0.png";
    }

    /**
     * @return com.trhsy.sim.entity.util.NpcIdentity
     * @Author fan
     * @Description //TODO 根据uid 获取NPC信息
     * @Date 14:05 2022/10/18
     * @Param [uuid]
     **/
    public static NpcIdentity getFolkByUUID(UUID uuid) {
        NpcIdentity npcIdentity = null;
        for (NpcIdentity npcIdentity1 : tempHireableNpcNames) {
            if (npcIdentity1.id.equals(uuid)) {
                return npcIdentity1;
            }
        }
        return npcIdentity;
    }

    /**根据蓝图id找到蓝图*/
    public static BlueprintRequirements getRequirementsByUUID(UUID uuid) {
        BlueprintRequirements cbr=null;
        for (BlueprintRequirements cbr1:blueprintReqs){
            if(!cbr1.entityId.equals(uuid)){
                cbr=cbr1;
                return cbr;
            }
        }
        return cbr;
    }

    /**
     * 打开控制箱
     */
    public static void openControlGui(V3 v3, String buildingId, String buildingName, String jobName, String bType, String author, String desc) {
        Minecraft.getMinecraft().displayGuiScreen(new GuiBlockControllerBlock(v3, buildingId, buildingName, jobName, bType, author,desc));
    }

    /**
     * 打开控制箱
     */
    public static void openControlGui(V3 v3, String buildingId, List<NpcData> occupants, String buildingName, String jobName, String bType, String author, String desc) {
        Minecraft.getMinecraft().displayGuiScreen(new GuiBlockControllerBlock(v3, occupants, buildingId, buildingName, jobName, bType, author,desc));
    }

    /**
     * 打开控制箱
     */
    public static void openControlGui(V3 v3, String buildingId, List<NpcData> occupants, boolean isResidential, String buildingName, String jobName, String bType, String author, String desc) {
        Minecraft.getMinecraft().displayGuiScreen(new GuiBlockControllerBlock(v3, occupants, buildingId, isResidential, buildingName, jobName, bType, author,desc));
    }

    public static void openFarmGui(UUID id, V3 loc, EnumFacing facing, FarmType farmType, int x, int z) {
        Minecraft.getMinecraft().displayGuiScreen(new GuiBlockFarmBlock(id, loc, facing, farmType, x, z));
    }

    public static void openFarmGui(UUID id, V3 loc, EnumFacing facing, FarmType farmType, int x, int z, NpcIdentity folk) {
        Minecraft.getMinecraft().displayGuiScreen(new GuiBlockFarmBlock(id, loc, facing, farmType, x, z, folk));
    }


    public static void openMineGui(UUID id, V3 loc, EnumFacing facing, int x, int z) {
        Minecraft.getMinecraft().displayGuiScreen(new GuiBlockMineBlock(id, loc, facing, x, z));
    }

    public static void openMineGui(UUID id, V3 loc, EnumFacing facing, int x, int z, NpcIdentity folk) {
        Minecraft.getMinecraft().displayGuiScreen(new GuiBlockMineBlock(id, loc, facing, x, z, folk));
    }
    /**
     * @Author fan
     * @Description //TODO 打开标记棒gui
     * @Date 21:19 2023/7/4
     * @Param [v3, dimension]
     * @return void
     **/
    public static void openMarkerGui(V3 v3, int dimension) {
        Minecraft.getMinecraft().displayGuiScreen(new GuiBlockMarker(v3, dimension));
    }
    /**
     * @Author fan
     * @Description //TODO 打开建筑商
     * @Date 23:03 2023/7/12
     * @Param [message]
     * @return void
     **/
    public static void openMerchantGui(PacketOpenMerchantGui message) {
        Minecraft.getMinecraft().displayGuiScreen(new GuiMerchant(message));
    }
    /**
     * @Author fan
     * @Description //TODO 打开银行
     * @Date 23:03 2023/7/12
     * @Param [message]
     * @return void
     **/
    public static void openBankATMGui(PacketOpenBankATMGui message) {
        Minecraft.getMinecraft().displayGuiScreen(new GuiBankATM(message));
    }

    /**
     * 杂货商
     * @param message
     */
    public static void openMerchants(PacketOpenMerchantsGui message) {
        Minecraft.getMinecraft().displayGuiScreen(new GuiMerchants(message));
    }

    /**
     *
     * @param message
     */
    public static void openFlowerGui(PacketOpenFlowerGui message) {
        Minecraft.getMinecraft().displayGuiScreen(new GuiFlower(message));
    }
    /**
     * 运行模组
     */
    public static void openSetupGui() {
        Minecraft.getMinecraft().displayGuiScreen(new GuiRunMod());
    }

    /**
     * 绘制头部信息
     */
    public static void openHudGui() {
        Minecraft.getMinecraft().displayGuiScreen(new GuiHud());
    }
    /**
     * @return void
     * @Author fan
     * @Description //TODO 打开npcUI
     * @Date 14:03 2022/10/18
     * @Param [message]
     **/
    public static void openFolkGui(PacketOpenFolkGui message) {
        Minecraft.getMinecraft().displayGuiScreen(new GuiFolk(message));
    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO d打开建筑箱的gui
     * @Date 16:23 2022/10/19
     * @Param [pos, bDir]
     **/
    public static void openConstructorGui(BlockPos pos, int bDir, int dimension) {
        Minecraft.getMinecraft().displayGuiScreen(new GuiBlockConstructorBlock(pos, bDir, dimension));
    }

    public static void openConstructorGui(BlockPos pos, int bDir, NpcIdentity folk, int dimension) {
        Minecraft.getMinecraft().displayGuiScreen(new GuiBlockConstructorBlock(pos, bDir, folk, dimension));
    }
}
