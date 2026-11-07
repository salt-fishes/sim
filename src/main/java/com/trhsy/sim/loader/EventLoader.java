package com.trhsy.sim.loader;

import com.trhsy.sim.ModSim;
import com.trhsy.sim.entity.EntityNpc;
import com.trhsy.sim.network.client.PacketUpdateMoney;
import com.trhsy.sim.npcCode.NpcData;
import com.trhsy.sim.util.AsyncFileWriter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @ClassName EventLoader
 * @Description todo 事件交互  * 事件处理器（优化版）
 *  * 修复线程安全、客户端/服务端逻辑混淆等问题，提升可维护性
 * @Author Tian
 * @Date 2022/9/2921:05
 **/
public class EventLoader {
    // 用于NPC加载的临时缓存（避免重复查询）
    // P0: 主线程 add()、NPC-Load-Thread 里 contains()/remove()，HashSet 并发扩容
    // 可能把结构损坏成死循环；换成并发集合。
    private static final Set<UUID> LOADING_NPC_UUIDS = java.util.concurrent.ConcurrentHashMap.newKeySet();
    // 单线程池用于NPC加载任务（替代直接创建线程）
    private static final ExecutorService NPC_LOAD_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "NPC-Load-Thread");
        thread.setDaemon(true); // 设为守护线程，随主线程退出
        return thread;
    });



    /**
     * 自定义的事件在这里被注册
     **/
    public static final EventBus EVENT_BUS = new EventBus();

    public EventLoader() {
        try {
            // P0: 只注册一次。FMLCommonHandler.instance().bus() 与 MinecraftForge.EVENT_BUS
            // 在 1.12.2 是同一个对象，自建 EVENT_BUS 从无 post()，重复注册会让每个事件
            // 回调执行多次（C3）。单入口是后续所有节拍/性能修复的前提。
            MinecraftForge.EVENT_BUS.register(this);
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("EventLoader出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 当玩家加入的时候
     * @Date 19:19 2022/10/7
     * @Param [event]
     **/
    @SubscribeEvent
    public void playerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        EntityPlayer player = event.player;

        if (player == null){
            return;
        }

        // 发送欢迎消息（主线程安全）
        String welcomeMsg = String.format("[%s] %s %s",
                player.getName(),
                new TextComponentTranslation("container.sim.welcome").getUnformattedText(),
                ModSim.VERSION
        );
        TextComponentTranslation textcomponenttranslation = new TextComponentTranslation(welcomeMsg, new Object[0]);
        player.sendMessage(textcomponenttranslation);
        // 加载NPC关联实体（通过服务端线程池异步处理，但实体操作必须在主线程）
        if (ModSimLoader.gamemode != 999 ) {
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            for (NpcData fd : ModSimLoader.folks) {
                // 避免重复加载
                if (LOADING_NPC_UUIDS.contains(fd.ID)){
                    continue;
                }
                LOADING_NPC_UUIDS.add(fd.ID);

                // 提交任务到服务端线程池（异步查询NPC实体）
                NPC_LOAD_EXECUTOR.submit(() -> {
                    try {
                        // 在服务端主线程执行实体操作（通过MinecraftServer的addScheduledTask）
                        server.addScheduledTask(() -> {
                            Entity entity = server.getEntityFromUuid(fd.ID);
                            if (entity instanceof EntityNpc) {
                                ModSimLoader.log.info("NPC-id: {} 已存在，直接关联", fd.ID);
                                fd.entity = (EntityNpc) entity;
                                fd.entity.setPositionAndUpdate(fd.pos.x, fd.pos.y, fd.pos.z);
                            } else {
                                ModSimLoader.log.warn("NPC-id: {} 未找到实体，可能已被移除", fd.ID);
                                // 若需创建新NPC，可在此处添加逻辑（需主线程）
                            }
                        });
                    } catch (Exception e) {
                        ModSimLoader.log.error("玩家加入时加载NPC失败，ID: {}, 错误: {}", fd.ID, e.getMessage(), e);
                    } finally {
                        LOADING_NPC_UUIDS.remove(fd.ID); // 清理缓存
                    }
                });
            }
        }

        // 给予初始物品（主线程安全）
        if (shouldGiveStarterItem()) {
            ItemStack starter = new ItemStack(ItemLoader.itemSimULoader);
            if (!player.inventory.addItemStackToInventory(starter)) {
                spawnItemDrop(player, starter);
            }
        }
        ModSimLoader.log.info("-----------------------------玩家加入再次重制时间，开始计时，准备加载模拟大都市------------------------");
        SimmodeStart.timeSinceLastClientUpdates=System.currentTimeMillis();
        /*
        Thread skinThread = new Thread(() -> {
            try {
                if (event.player != null) {
                    ModSimLoader.log.info("*********************玩家加入*开始重载NPC实体*****************");

                    for (NpcData fd : ModSimLoader.folks) {
                        while (fd.entity == null && FMLCommonHandler.instance() != null) {
                            try {
                                Entity  entity=FMLCommonHandler.instance().getMinecraftServerInstance().getEntityFromUuid(fd.ID);
                                ModSimLoader.log.info("找到NPC-id："+fd.ID);
                                if(!fd.isDead){
                                    entity.setDead();
                                    ModSimLoader.log.info("NPC-id："+fd.ID+"死了");
                                }else{
                                    fd.entity = (EntityNpc) entity;
                                    ModSimLoader.log.info("NPC-id："+fd.ID+",,赋予实体");
                                }
                            } catch (Exception var4) {
                                ModSimLoader.log.error("玩家加入加载数据失败了" + var4.getMessage());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.getMessage();
            }


        });
        skinThread.start();*/
    }
    /**
     * 判断是否应给予初始物品
     */
    private static boolean shouldGiveStarterItem() {
        boolean shouldGive = ItemLoader.itemSimULoader != null && ModSimLoader.gamemode == 999;
        // 替换魔数为枚举或常量（示例）
        return shouldGive;
    }
    /**
     * 生成物品掉落（主线程安全）
     */
    private static void spawnItemDrop(EntityPlayer player, ItemStack stack) {
        World world = player.world;
        if (world.isRemote){
            return;
        }

        double x = player.posX + (world.rand.nextDouble() - 0.5) * 0.7;
        double y = player.posY + world.rand.nextDouble() * 0.5 + 0.5; // 避免卡在地底
        double z = player.posZ + (world.rand.nextDouble() - 0.5) * 0.7;
        EntityItem itemEntity = new EntityItem(world, x, y, z, stack);
        itemEntity.setDefaultPickupDelay();
        world.spawnEntity(itemEntity);
    }
    /**
     * 世界保存
     *
     * @param event
     */
    @SubscribeEvent
    public void worldSave(WorldEvent.Save event) {
        World world = event.getWorld();
        // P0: 只在服务端主世界保存一次。原来客户端/地狱/末地也会触发，单机下
        // 两份静态字段指向同一份数据，会出现"客户端覆盖服务端"的存档事故（C2）。
        if (world == null || world.isRemote || world.provider.getDimension() != 0) {
            return;
        }
        SimmodeStart.simModSave(world);
    }

    /** P15(S1): 服务器关闭前等待异步保存队列落盘。 */
    @SubscribeEvent
    public void serverStopping(FMLServerStoppingEvent event) {
        AsyncFileWriter.flush(10000L);
    }

    /**
     * 世界加载
     *
     * @param event
     */
    @SubscribeEvent
    public void worldLoad(WorldEvent.Load event) {
        World world=event.getWorld();
        // 仅关注“服务端的主世界”（维度0，非客户端，非其他维度如地狱/末地）
        if (world.isRemote || world.provider.getDimension() != 0) {
            return;
        }
        ModSimLoader.log.info( "清除旧的世界数据");
        ModSimLoader.folks.clear();
        ModSimLoader.farms.clear();
        ModSimLoader.mines.clear();
        ModSimLoader.buildings.clear();
        ModSimLoader.dayOfWeek = 0;
        ModSimLoader.gameDay = 0;
        ModSimLoader.gamemode = 999;
        ModSimLoader.money = 10.0F;
        ModSimLoader.sim_is_running = false;
        // P0: 允许同一进程内重进世界时重新加载（原来只清数据不重置加载标志）
        SimmodeStart.hasLoadedWorld = false;
        // P0: 不再在这里碰 ModSimClientLoader（客户端类）。客户端镜像的重置移到
        // ClientWorldEvents，由 ClientProxy 注册（D13）。
        SimmodeStart.timeSinceLastClientUpdates=System.currentTimeMillis();
        NetWorkLoader.net.sendToAll(new PacketUpdateMoney());
        ModSimLoader.log.info("-----------------------------重制时间，开始计时，准备加载模拟大都市------------------------");
//        SimmodeStart.simModLoad(world);
//        if (!world.isRemote && world instanceof WorldServer) {
            // 只在服务端运行

//        }
    }

    /**
     * 钩子
     *
     * @param event
     */
    @SubscribeEvent
    public void worldTick(WorldTickEvent event) {
        World world = event.world;
        if (world == null) {
            return;
        }
        // P0: 唯一模拟入口。
        //  - 只在 END 相位执行（START/END 各触发一次，否则所有 NPC 跑两遍）；
        //  - 只处理服务端维度 0（地狱/末地各自触发一次，且没有模拟意义）；
        //  - 客户端不跑模拟（客户端只接收同步包）。
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (world.isRemote || world.provider.getDimension() != 0) {
            return;
        }
        SimmodeStart.simModupdate(world);
    }

    /**
     * 实时更新人的状态
     * @param event
     */
    public void updateNpcStatus(WorldTickEvent event){


    }
    /**
     * 渲染钩子
     *
     * @param e
     */
    @SubscribeEvent
    public void renderTick(RenderTickEvent e) {
        ModSim.proxy.renderTick(e);
    }

    /**
     * 当实体加入世界赋予玩家手里第一个物品栏里一个模拟城市任命卷轴
     *
     * @param event
     */
   /* @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {

        Entity entity = event.getEntity();
        World worldObj = event.getWorld();
        SimmodeStart.hasLoadedWorld=true;

        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            //欢迎来到模拟城镇,由TRHSY重制，更多资讯请关注公众号: dasha5000
            String welcome = "【" + player.getName() + "】" + new TextComponentTranslation("container.sim.welcome", new Object[0]).getUnformattedText();
            String welcomes = new TextComponentTranslation("container.sim.welcomes", new Object[0]).getUnformattedText();
            ModSimLoader.sendChat(welcome + ModSim.VERSION + welcomes);
            boolean shouldGive = ItemLoader.itemSimULoader != null && ModSimLoader.gamemode == 999;
            if (shouldGive) {
                ItemStack starter = new ItemStack(ItemLoader.itemSimULoader);
                if (!player.inventory.addItemStackToInventory(starter)) {
                    float f = 0.7F;
                    float d0 = worldObj.rand.nextFloat() * f + (1.0F - f) * 0.5F;
                    float d1 = worldObj.rand.nextFloat() * f + (1.0F - f) * 0.5F;
                    float d2 = worldObj.rand.nextFloat() * f + (1.0F - f) * 0.5F;
                    EntityItem entityitem = new EntityItem(worldObj, player.posX + (double) d0, player.posY + (double) d1, player.posZ + (double) d2, new ItemStack(ItemLoader.itemSimULoader));
                    entityitem.setDefaultPickupDelay();
                    worldObj.spawnEntity(entityitem);
                }
            }
        }
    }*/

    /**
     * 客户端断开连接
     *
     * @param event
     */
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void clientDisconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
//        dedWorld = false;

        SimmodeStart.simModDisconnected();
        // 清理NPC加载缓存
        LOADING_NPC_UUIDS.clear();
    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 世界渲染
     * @Date 17:12 2022/10/31
     * @Param [event]
     **/
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onWorldRenderLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        World world = mc.world;
        if (world == null || ModSimClientLoader.previewPos1 == null || ModSimClientLoader.previewPos2 == null) {
            return;
        }
        // 获取玩家当前视角的偏移量（考虑相机移动）
        double partialTicks = event.getPartialTicks();
        double offsetX = mc.player.prevPosX + (mc.player.posX - mc.player.prevPosX) * partialTicks;
        double offsetY = mc.player.prevPosY + (mc.player.posY - mc.player.prevPosY) * partialTicks;
        double offsetZ = mc.player.prevPosZ + (mc.player.posZ - mc.player.prevPosZ) * partialTicks;
        // 绘制包围盒（客户端主线程安全）
        drawBoundingBox(mc.player, ModSimClientLoader.previewPos1, ModSimClientLoader.previewPos2, 5.0F, event, offsetX, offsetY, offsetZ);

            /*for (EntityPlayer player : world.playerEntities) {
                if (ModSimClientLoader.previewPos1 != null && ModSimClientLoader.previewPos2 != null) {
                    //预览
                    drawBoundingBox(player, ModSimClientLoader.previewPos1, ModSimClientLoader.previewPos2, true, 5.0F, event);

                }
        }*/


    }
    /**
     * 绘制包围盒（优化版）
     */
    @SideOnly(Side.CLIENT)
    private static void drawBoundingBox(EntityPlayer player, Vec3d posA, Vec3d posB, float width,
                                        RenderWorldLastEvent event, double offsetX, double offsetY, double offsetZ) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS); // 保存所有GL状态
        try {
            // 禁用光照、纹理，启用混合
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            // 设置线宽（兼容不同驱动）
            GL11.glLineWidth(Math.max(1.0F, width));

            // 调整坐标到玩家视角
            GL11.glTranslated(-offsetX, -offsetY, -offsetZ);

            // 设置半透明红色
            GL11.glColor4f(1.0F, 0.0F, 0.0F, 0.3F);

            // 构建包围盒顶点（基于两个对角点）
            Vec3d min = new Vec3d(
                    Math.min(posA.x, posB.x),
                    Math.min(posA.y, posB.y),
                    Math.min(posA.z, posB.z)
            );
            Vec3d max = new Vec3d(
                    Math.max(posA.x, posB.x),
                    Math.max(posA.y, posB.y),
                    Math.max(posA.z, posB.z)
            );

            // 绘制立方体（简化版，使用12条边）
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

            // 前后面
            addEdge(buffer, min, new Vec3d(max.x, min.y, min.z), color(1.0F, 0.0F, 0.0F, 0.3F));
            addEdge(buffer, new Vec3d(min.x, max.y, min.z), max, color(1.0F, 0.0F, 0.0F, 0.3F));
            addEdge(buffer, new Vec3d(min.x, min.y, max.z), new Vec3d(max.x, max.y, max.z), color(1.0F, 0.0F, 0.0F, 0.3F));
            addEdge(buffer, new Vec3d(min.x, max.y, max.z), new Vec3d(max.x, min.y, max.z), color(1.0F, 0.0F, 0.0F, 0.3F));

            // 连接前后
            addEdge(buffer, new Vec3d(min.x, min.y, min.z), new Vec3d(min.x, min.y, max.z), color(1.0F, 0.0F, 0.0F, 0.3F));
            addEdge(buffer, new Vec3d(max.x, min.y, min.z), new Vec3d(max.x, min.y, max.z), color(1.0F, 0.0F, 0.0F, 0.3F));
            addEdge(buffer, new Vec3d(min.x, max.y, min.z), new Vec3d(min.x, max.y, max.z), color(1.0F, 0.0F, 0.0F, 0.3F));
            addEdge(buffer, new Vec3d(max.x, max.y, min.z), new Vec3d(max.x, max.y, max.z), color(1.0F, 0.0F, 0.0F, 0.3F));

            tessellator.draw();
        } finally {
            GL11.glPopAttrib(); // 恢复所有GL状态
        }
    }
    /**
     * 辅助方法：添加边
     */
    @SideOnly(Side.CLIENT)
    private static void addEdge(BufferBuilder buffer, Vec3d start, Vec3d end, Color color) {
        buffer.pos(start.x, start.y, start.z)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                .endVertex();
        buffer.pos(end.x, end.y, end.z)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                .endVertex();
    }

    /**
     * 辅助方法：生成颜色（可扩展为可配置）
     */
    @SideOnly(Side.CLIENT)
    private static Color color(float r, float g, float b, float a) {
        return new Color(r, g, b, a);
    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 预览程序
     * @Date 17:19 2022/10/31
     * @Param [player, posA, posB, smooth, width, event]
     **/
    @SideOnly(Side.CLIENT)
    public static void drawBoundingBox(EntityPlayer player, Vec3d posA, Vec3d posB, boolean smooth, float width, RenderWorldLastEvent event) {
        GL11.glPushAttrib(8192);
        GL11.glDisable(2884);
        GL11.glDisable(2896);
        GL11.glDisable(3553);
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        // 禁用光照、纹理，启用混合
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        // 设置线宽（兼容不同驱动）
        GL11.glLineWidth(Math.max(1.0F, width));

        // 调整坐标到玩家视角
//        GL11.glTranslated(-offsetX, -offsetY, -offsetZ);

        // 设置半透明红色
        GL11.glColor4f(1.0F, 0.0F, 0.0F, 0.3F);
        //获取位置
        double d0 = player.prevPosX + (player.posX - player.prevPosX) * (double) event.getPartialTicks();
        double d1 = player.prevPosY + (player.posY - player.prevPosY) * (double) event.getPartialTicks();
        double d2 = player.prevPosZ + (player.posZ - player.prevPosZ) * (double) event.getPartialTicks();

        Vec3d pos = new Vec3d(d0, d1, d2);
        //转换
        GL11.glTranslated(-pos.x, -pos.y, -pos.z);
        //rgba  红色
        Color c = new Color(255, 0, 0, 150);
        GL11.glColor4d((double) c.getRed(), (double) c.getGreen(), (double) c.getBlue(), (double) c.getAlpha());
        //设置宽
        GL11.glLineWidth(width);
        //深度 蒙版
        GL11.glDepthMask(true);
        //镶嵌单元
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        //设置位置颜色
        bufferBuilder.begin(1, DefaultVertexFormats.POSITION_COLOR);

        double dx = posA.x - posB.x > 0.0D ? -Math.abs(posA.x - posB.x) : Math.abs(posA.x - posB.x);
        double dy = Math.abs(posA.y - posB.y);
        double dz = posA.z - posB.z > 0.0D ? -Math.abs(posA.z - posB.z) : Math.abs(posA.z - posB.z);
        double xOf = dx > 0.0D ? 0.0D : 1.0D;
        double zOf = dz > 0.0D ? 0.0D : 1.0D;
        posA = posA.addVector(xOf, 0.0D, zOf);

        bufferBuilder.pos(posA.x, posA.y, posA.z).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        bufferBuilder.pos(posA.x, posA.y, posA.z + dz).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        bufferBuilder.pos(posA.x, posA.y, posA.z + dz).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();

        bufferBuilder.pos(posA.x + dx, posA.y, posA.z + dz).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        bufferBuilder.pos(posA.x + dx, posA.y, posA.z + dz).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        bufferBuilder.pos(posA.x + dx, posA.y, posA.z).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        bufferBuilder.pos(posA.x + dx, posA.y, posA.z).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();

        bufferBuilder.pos(posA.x, posA.y, posA.z).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        bufferBuilder.pos(posA.x, posA.y + dy, posA.z).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        bufferBuilder.pos(posA.x, posA.y + dy, posA.z + dz).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        bufferBuilder.pos(posA.x, posA.y + dy, posA.z + dz).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();

        bufferBuilder.pos(posA.x + dx, posA.y + dy, posA.z + dz).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        bufferBuilder.pos(posA.x + dx, posA.y + dy, posA.z + dz).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        bufferBuilder.pos(posA.x + dx, posA.y + dy, posA.z).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        bufferBuilder.pos(posA.x + dx, posA.y + dy, posA.z).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();

        bufferBuilder.pos(posA.x, posA.y + dy, posA.z).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        bufferBuilder.pos(posA.x, posA.y, posA.z).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        bufferBuilder.pos(posA.x, posA.y + dy, posA.z).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        bufferBuilder.pos(posA.x, posA.y, posA.z + dz).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        bufferBuilder.pos(posA.x, posA.y + dy, posA.z + dz).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();

        bufferBuilder.pos(posA.x + dx, posA.y, posA.z + dz).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        bufferBuilder.pos(posA.x + dx, posA.y + dy, posA.z + dz).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        bufferBuilder.pos(posA.x + dx, posA.y, posA.z).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        bufferBuilder.pos(posA.x + dx, posA.y + dy, posA.z).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();

        tessellator.draw();
        GL11.glDepthMask(true);
        GL11.glPopAttrib();
    }
}
