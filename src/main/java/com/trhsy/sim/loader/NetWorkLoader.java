package com.trhsy.sim.loader;

import com.trhsy.sim.ModSim;
import com.trhsy.sim.network.client.*;
import com.trhsy.sim.network.server.*;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.loader
 * @ClassName: NetWorkLoader
 * @Description: 通讯服务
 * @date 2022/9/22 0022 下午 3:12
 */
public class NetWorkLoader {
    public static SimpleNetworkWrapper net = null;
    private static int nextID = 0;
    /**
     * 初始化数据包
     */
    public NetWorkLoader(FMLPreInitializationEvent event){
        net = NetworkRegistry.INSTANCE.newSimpleChannel(ModSim.MODID);
        registerMessagesAsServer();
        registerMessagesAsClient();
    }

    /**
     * 服务端
     */
    public void registerMessagesAsServer(){
        //启动
        registerMessage(PacketSetupMod.Handler.class,PacketSetupMod.class,Side.SERVER);
        //雇佣
        registerMessage(PacketHireFolk.Handler.class,PacketHireFolk.class,Side.SERVER);
        //解雇
        registerMessage(PacketFireFolk.Handler.class,PacketFireFolk.class,Side.SERVER);
        //获得蓝图
        registerMessage(PacketSendBlueprint.Handler.class,PacketSendBlueprint.class,Side.SERVER);
        //获得规划
        registerMessage(PacketSendTerrainType.Handler.class,PacketSendTerrainType.class,Side.SERVER);
        //获取可雇佣NPC
        registerMessage(PacketGetHireableFolks.Handler.class,PacketGetHireableFolks.class,Side.SERVER);
        //拆除建筑
        registerMessage(PacketDemolishBuilding.Handler.class,PacketDemolishBuilding.class,Side.SERVER);
        //旋转楼梯
        registerMessage(PacketrotateStairs.Handler.class,PacketrotateStairs.class,Side.SERVER);
        //更新养殖箱
        registerMessage(PacketUpdateFarmBox.Handler.class,PacketUpdateFarmBox.class,Side.SERVER);
        //采矿箱更新
        registerMessage(PacketUpdateMineBox.Handler.class,PacketUpdateMineBox.class,Side.SERVER);
        //买卖
        registerMessage(PacketBuyStuff.Handler.class,PacketBuyStuff.class,Side.SERVER);
        registerMessage(PacketSellStuff.Handler.class,PacketSellStuff.class,Side.SERVER);
        //生成npc
        registerMessage(PacketNewFolk.Handler.class,PacketNewFolk.class,Side.SERVER);



    }

    /**
     * 客户端
     */
    public void registerMessagesAsClient(){
        //开始卷轴启动页
        registerMessage(PacketOpenSetupGui.Handler.class,PacketOpenSetupGui.class,Side.CLIENT);
        //左上角配置
        registerMessage(PacketOpenHudGui.Handler.class,PacketOpenHudGui.class,Side.CLIENT);
        //配置文件
        registerMessage(ConfigSyncPacket.Handler.class,ConfigSyncPacket.class,Side.CLIENT);
        //发送NPC皮肤
        registerMessage(PacketSendFolkSkin.Handler.class,PacketSendFolkSkin.class,Side.CLIENT);
        //可雇佣 的NPC
        registerMessage(PacketReturnHireableFolks.Handler.class,PacketReturnHireableFolks.class,Side.CLIENT);
        //更新资金
        registerMessage(PacketUpdateMoney.Handler.class,PacketUpdateMoney.class,Side.CLIENT);
        //更新客户端NPC
        registerMessage(PacketUpdateNPC.Handler.class,PacketUpdateNPC.class,Side.CLIENT);
        //打开NPC互动界面
        registerMessage(PacketOpenFolkGui.Handler.class,PacketOpenFolkGui.class,Side.CLIENT);
        //打开建筑箱gui
        registerMessage(PacketOpenConstructorGui.Handler.class,PacketOpenConstructorGui.class,Side.CLIENT);
        //请求蓝图
        registerMessage(PacketSendBuildingRequirements.Handler.class,PacketSendBuildingRequirements.class,Side.CLIENT);
        //请求规划
        registerMessage(PacketSendTerrainTypeRequitrements.Handler.class,PacketSendTerrainTypeRequitrements.class,Side.CLIENT);
        //打开控制箱
        registerMessage(PacketOpenControlGui.Handler.class,PacketOpenControlGui.class,Side.CLIENT);
        //打开养殖箱
        registerMessage(PacketOpenFarmGui.Handler.class,PacketOpenFarmGui.class,Side.CLIENT);
        //添加标记点
        registerMessage(PacketAddNewMarker.Handler.class,PacketAddNewMarker.class,Side.CLIENT);
        //打开标记点gui
        registerMessage(PacketOpenMarkerGui.Handler.class,PacketOpenMarkerGui.class,Side.CLIENT);
        //打开采矿箱GUI
        registerMessage(PacketOpenMineGui.Handler.class,PacketOpenMineGui.class,Side.CLIENT);
        //打开建筑商GUI
        registerMessage(PacketOpenMerchantGui.Handler.class,PacketOpenMerchantGui.class,Side.CLIENT);
        //打开银行
        registerMessage(PacketOpenBankATMGui.Handler.class,PacketOpenBankATMGui.class,Side.CLIENT);
        //打开杂货铺
        registerMessage(PacketOpenMerchantsGui.Handler.class,PacketOpenMerchantsGui.class,Side.CLIENT);
        //花店
        registerMessage(PacketOpenFlowerGui.Handler.class,PacketOpenFlowerGui.class,Side.CLIENT);
        //路径箱
        registerMessage(PacketOpenPathBoxGui.Handler.class,PacketOpenPathBoxGui.class,Side.CLIENT);
        //风车
        registerMessage(PacketOpenWindmillGui.Handler.class,PacketOpenWindmillGui.class,Side.CLIENT);



    }
    /**
     *
     * @param messageHandler
     * @param requestMessageType
     * @param side
     * @param <REQ>
     * @param <REPLY>
     */
    private static <REQ extends IMessage, REPLY extends IMessage> void registerMessage(
            Class<? extends IMessageHandler<REQ, REPLY>> messageHandler, Class<REQ> requestMessageType, Side side) {
        net.registerMessage(messageHandler, requestMessageType, nextID++, side);
    }
}
