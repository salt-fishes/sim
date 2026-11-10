package com.trhsy.sim.gui.npc;

import com.trhsy.sim.ModSim;
import com.trhsy.sim.entity.EntityNpc;
import com.trhsy.sim.entity.container.ContainerNpc;
import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.NpcData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiButtonImage;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.recipebook.GuiRecipeBook;
import net.minecraft.client.gui.recipebook.IRecipeShownListener;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.gui.npc
 * @ClassName: GuiFolkInventory
 * @Description: npc 库存
 * @date 2024/3/19 14:29
 */
@SideOnly(Side.CLIENT)
public class GuiFolkInventory extends InventoryEffectRenderer implements IRecipeShownListener {
    public static final ResourceLocation NPC_INVENTORY_BACKGROUND = new ResourceLocation(ModSim.MODID ,"textures/gui/npc_inventory.png");
    private GuiTextField searchField;
    private NpcData npcData;
    private final java.util.UUID uid;
    private boolean widthTooNarrow;

    private final GuiRecipeBook recipeBookGui = new GuiRecipeBook();

    private GuiButtonImage recipeButton;

    /**
     * 鼠标指针的旧x位置
     */
    private float oldMouseX;
    /**
     * 鼠标指针的旧y位置
     */
    private float oldMouseY;
    private boolean buttonClicked;

    public GuiFolkInventory(java.util.UUID uid) {
        super(containerFor(uid));
        this.uid = uid;
        this.npcData = ModSimLoader.getFolkDataByUID(uid);
    }

    /**
     * P15(S1): 客户端不一定有 ModSimLoader.folks（多人游戏时那是服务端数据）。
     * 优先找已加载的 EntityNpc；找不到再退回玩家容器，至少不会 NPE。
     */
    private static Container containerFor(java.util.UUID uid) {
        EntityNpc npc = ModSimLoader.getEntityFolkByUID(uid);
        if (npc == null) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.world != null) {
                for (Entity entity : mc.world.loadedEntityList) {
                    if (entity instanceof EntityNpc && uid.equals(entity.getUniqueID())) {
                        npc = (EntityNpc) entity;
                        break;
                    }
                }
            }
        }
        if (npc != null && npc.inventoryContainer != null) {
            return npc.inventoryContainer;
        }
        EntityPlayer player = Minecraft.getMinecraft().player;
        return new ContainerPlayer(player != null ? player.inventory : new net.minecraft.entity.player.InventoryPlayer(null), false, player);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void updateScreen() {
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        super.initGui();
        this.widthTooNarrow = this.width < 379;
    }
    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = new net.minecraft.util.text.TextComponentTranslation("container.sim.gui.npc_inventory", new Object[0]).getUnformattedText();
        this.fontRenderer.drawString(title, this.xSize - 8 - this.fontRenderer.getStringWidth(title), 6, 4210752);
    }
    /**
     * 所有组件
     *
     * @param mouseX
     * @param mouseY
     * @param partialTicks
     */
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
        this.oldMouseX = (float) mouseX;
        this.oldMouseY = (float) mouseY;
    }


    /**
     * 绘制GuiContainer的前景层（项目前面的所有内容）
     *
     * @param partialTicks
     * @param mouseX
     * @param mouseY
     */
    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(NPC_INVENTORY_BACKGROUND);
        int i = this.guiLeft;
        int j = this.guiTop;
        this.drawTexturedModalRect(i, j, 0, 0, this.xSize, this.ySize);
        EntityLivingBase model = this.mc.player;
        if (this.npcData != null && this.npcData.entity != null) {
            model = this.npcData.entity;
        }
        if (this.mc.world != null) {
            for (Entity entity : this.mc.world.loadedEntityList) {
                if (entity instanceof EntityNpc && this.uid.equals(entity.getUniqueID())) {
                    model = (EntityLivingBase) entity;
                    break;
                }
            }
        }
        if (model != null) {
            drawEntityOnScreen(i + 51, j + 70, 28, (float) (i + 51) - this.oldMouseX, (float) (j + 20) - this.oldMouseY, model);
        }
    }

    /**
     * 在屏幕上朝向光标绘制一个实体。
     */
    public static void drawEntityOnScreen(int posX, int posY, int scale, float mouseX, float mouseY, EntityLivingBase ent) {
        GlStateManager.enableColorMaterial();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) posX, (float) posY, 50.0F);
        GlStateManager.scale((float) (-scale), (float) scale, (float) scale);
        GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
        float f = ent.renderYawOffset;
        float f1 = ent.rotationYaw;
        float f2 = ent.rotationPitch;
        float f3 = ent.prevRotationYawHead;
        float f4 = ent.rotationYawHead;
        GlStateManager.rotate(135.0F, 0.0F, 1.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.rotate(-135.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-((float) Math.atan((double) (mouseY / 40.0F))) * 20.0F, 1.0F, 0.0F, 0.0F);
        ent.renderYawOffset = (float) Math.atan((double) (mouseX / 40.0F)) * 20.0F;
        ent.rotationYaw = (float) Math.atan((double) (mouseX / 40.0F)) * 40.0F;
        ent.rotationPitch = -((float) Math.atan((double) (mouseY / 40.0F))) * 20.0F;
        ent.rotationYawHead = ent.rotationYaw;
        ent.prevRotationYawHead = ent.rotationYaw;
        GlStateManager.translate(0.0F, 0.0F, 0.0F);
        RenderManager rendermanager = Minecraft.getMinecraft().getRenderManager();
        rendermanager.setPlayerViewY(180.0F);
        rendermanager.setRenderShadow(false);
        rendermanager.renderEntity(ent, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, false);
        rendermanager.setRenderShadow(true);
        ent.renderYawOffset = f;
        ent.rotationYaw = f1;
        ent.rotationPitch = f2;
        ent.prevRotationYawHead = f3;
        ent.rotationYawHead = f4;
        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    /**
     * Test if the 2D point is in a rectangle (relative to the GUI). Args : rectX, rectY, rectWidth, rectHeight, pointX,
     * pointY
     */
    @Override
    protected boolean isPointInRegion(int rectX, int rectY, int rectWidth, int rectHeight, int pointX, int pointY) {
        return super.isPointInRegion(rectX, rectY, rectWidth, rectHeight, pointX, pointY);
    }

    /**
     * 单击鼠标时调用。Args:mouseX，mouseY，点击按钮
     */
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    /**
     * Called when a mouse button is released.
     */
    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (this.buttonClicked) {
            this.buttonClicked = false;
        } else {
            super.mouseReleased(mouseX, mouseY, state);
        }
    }

    @Override
    protected boolean hasClickedOutside(int p_193983_1_, int p_193983_2_, int p_193983_3_, int p_193983_4_) {
        boolean flag = p_193983_1_ < p_193983_3_ || p_193983_2_ < p_193983_4_ || p_193983_1_ >= p_193983_3_ + this.xSize || p_193983_2_ >= p_193983_4_ + this.ySize;
        return flag;
    }

    /**
     * Called by the controls from the buttonList when activated. (Mouse pressed for buttons)
     */
    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
    }

    /**
     * Fired when a key is typed (except F11 which toggles full screen). This is the equivalent of
     * KeyListener.keyTyped(KeyEvent e). Args : character (character on the key), keyCode (lwjgl Keyboard key code)
     */
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
    }

    /**
     * 当鼠标在插槽上或gui外部单击时调用。
     */
    @Override
    protected void handleMouseClick(Slot slotIn, int slotId, int mouseButton, ClickType type) {
        super.handleMouseClick(slotIn, slotId, mouseButton, type);
    }

    @Override
    public void recipesUpdated() {
    }
    /**
     * 在卸载屏幕时调用。用于禁用键盘重复事件
     */
    @Override
    public void onGuiClosed()
    {
        super.onGuiClosed();
    }

    @Override
    public GuiRecipeBook func_194310_f() {
        return this.recipeBookGui;
    }
}
