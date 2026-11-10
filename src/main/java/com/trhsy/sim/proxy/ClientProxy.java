package com.trhsy.sim.proxy;

import com.trhsy.sim.gui.GuiHud;
import com.trhsy.sim.key.KeyboardManager;
import com.trhsy.sim.loader.*;
import com.trhsy.sim.loader.render.ItemRenderLoader;
import com.trhsy.sim.util.BuildingsExtractor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.Display;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.proxy
 * @ClassName: ClientProxy
 * @Description: 客户端 客户端代理类，包含客户端特定的初始化和渲染逻辑
 * @date 2023/10/19 下午 2:42
 */
public class ClientProxy extends CommonProxy {


    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        /**P0: 按键注册只在客户端执行（原来在 CommonProxy，专用服务器会加载 KeyBinding 而崩溃）*/
        KeyboardManager.init();
        /**P0: 客户端世界事件（重置模拟状态镜像），不再由服务端事件触碰客户端类*/
        MinecraftForge.EVENT_BUS.register(new ClientWorldEvents());
        /**解压建筑蓝图*/
        new BuildingsExtractor();
        /**创造模式物品栏**/
        new CreativeTabsLoader(event);
        /**流体加载注册**/
        new FluidLoader();

        /**方块对应物品的渲染**/
        new ItemRenderLoader();
        //加载实体
        EntityLoader.initModels();


        /**配置**/
//        ConfigLoader.load(event);

        /**物品加载注册**/
//        new ItemLoader(event);
        /**方块加载注册**/
//        new BlockLoader(event);

    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        // 设置游戏窗口标题
        String title = new TextComponentTranslation("container.sim.title", new Object[0]).getUnformattedText();
        Display.setTitle(Display.getTitle() + title);
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
    }

    @Override
    public void renderTick(TickEvent.RenderTickEvent renderTickEvent) {
        Minecraft mc=Minecraft.getMinecraft();
        try {
            if (mc!= null && mc.world!=null&&mc.world.isRemote) {
                String worldname = "unknown";
                try {
                    if (ModSimClientLoader.gamemode== 999) {
                        return;
                    }
                    worldname = mc.getIntegratedServer().getFolderName();
                } catch (Exception var7) {
                    worldname = "Server";
                }
                try {
                    if(ModSimClientLoader.sim_is_running){
                        if (ModSimClientLoader.gamemode != 999) {
                            if (!Minecraft.getMinecraft().gameSettings.showDebugInfo) {
                                int HUDoffset = 0;
                                if (mc.player!=null&&mc.player.dimension == 1) {
                                    HUDoffset = 20;
                                }
                                String fs_day_of_week="";
                                switch (ModSimClientLoader.dayOfWeek){
                                    case 0:
                                        fs_day_of_week=new TextComponentTranslation("container.sim.simSun",new Object[0]).getUnformattedText();
                                        break;
                                    case 1:
                                        fs_day_of_week=new TextComponentTranslation("container.sim.simMon",new Object[0]).getUnformattedText();
                                        break;
                                    case 2:
                                        fs_day_of_week=new TextComponentTranslation("container.sim.simTue",new Object[0]).getUnformattedText();
                                        break;
                                    case 3:
                                        fs_day_of_week=new TextComponentTranslation("container.sim.simWed",new Object[0]).getUnformattedText();
                                        break;
                                    case 4:
                                        fs_day_of_week=new TextComponentTranslation("container.sim.simThu",new Object[0]).getUnformattedText();
                                        break;
                                    case 5:
                                        fs_day_of_week=new TextComponentTranslation("container.sim.simFri",new Object[0]).getUnformattedText();
                                        break;
                                    case 6:
                                        fs_day_of_week=new TextComponentTranslation("container.sim.simSat",new Object[0]).getUnformattedText();
                                        break;
                                }
                                if (ModSimClientLoader.gamemode == 1) {

                                    //世界名 人口
                                    drawHudText(mc, worldname+" "+new TextComponentTranslation("container.sim.title_days",new Object[]{ModSimClientLoader.gameDay}).getUnformattedText() +" (" + fs_day_of_week + ") - "+ new TextComponentTranslation("container.sim.trhsy3",new Object[0]).getUnformattedText() +": " + ModSimClientLoader.tempHireableNpcNames.size(), HUDoffset);
                                } else {
                                    //世界名  人口  资金
                                    drawHudText(mc, worldname+" "+new TextComponentTranslation("container.sim.title_days",new Object[]{ModSimClientLoader.gameDay}).getUnformattedText()+" (" +fs_day_of_week + ") - "+new TextComponentTranslation("container.sim.trhsy3",new Object[0]).getUnformattedText() +": " + ModSimClientLoader.tempHireableNpcNames.size() + "   "+ new TextComponentTranslation("container.sim.trhsy4",new Object[0]).getUnformattedText() +": " + ModSimLoader.displayMoney(ModSimClientLoader.money), HUDoffset);
                                }
                            }
                        } else if (!Minecraft.getMinecraft().gameSettings.showDebugInfo) {
                            drawHudText(mc, idleHudText(), 0);
                        }
                    }else{
                        drawHudText(mc, idleHudText(), 0);
                    }

                } catch (Exception e) {
                    StackTraceElement element = e.getStackTrace()[0];
                    ModSimLoader.log.error("renderTick出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
                }
            }else{
                return;
            }


        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("GuiHud-initGui出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    /** HUD 使用 ScaledResolution 的真实宽度居中，不再依赖未初始化的 GuiScreen.width。 */
    /**
     * 未启动和“已启动但还在加载”是两种状态，提示不能混在一起。
     */
    private String idleHudText() {
        String key = ModSimClientLoader.gamemode == 999
                ? "container.sim.hud.not_started"
                : "container.sim.trhsy5";
        return new TextComponentTranslation(key, new Object[0]).getUnformattedText();
    }

    private void drawHudText(Minecraft mc, String text, int offset) {
        int width = new ScaledResolution(mc).getScaledWidth();
        int x = (width - mc.fontRenderer.getStringWidth(text)) / 2;
        mc.fontRenderer.drawString(text, x, 2 + offset, 16777215, true);
    }
}
