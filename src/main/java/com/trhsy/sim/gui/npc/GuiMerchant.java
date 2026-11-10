package com.trhsy.sim.gui.npc;

import com.trhsy.sim.gui.GuiLayout;
import com.trhsy.sim.gui.GuiStyle;
import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.loader.NetWorkLoader;
import com.trhsy.sim.network.client.PacketOpenMerchantGui;
import com.trhsy.sim.network.server.PacketBuyStuff;
import com.trhsy.sim.network.server.PacketSellStuff;
import com.trhsy.sim.npcCode.V3;
import com.trhsy.sim.util.PricesForBlocks;
import net.minecraft.block.Block;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @ClassName GuiMerchant
 * @Description todo 建筑商
 * @Author TRHSY
 * @Date 2023/7/323:02
 **/
public class GuiMerchant extends GuiScreen {
    private int currentPage = 0;
    //持有购买数量
    private static List<Integer> quantities = new CopyOnWriteArrayList<>();
    //基于玩家库存的销售限制
    private static List<Integer> sellLimits = new CopyOnWriteArrayList<Integer>();
    private Float totalCost = 0.0F;
    private int mouseCount = 0;
    private UUID id;
    public GuiMerchant(PacketOpenMerchantGui message) {
        this.id=message.id;
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
        try {
            Keyboard.enableRepeatEvents(true);
            if (this.currentPage == 0) {
                this.currentPage = 1;
            }
            this.quantities.clear();
            sellLimits.clear();
            for (int i = 0; i < 9; i++) {
                this.quantities.add(0);
                sellLimits.add(0);
            }
            this.showPage();
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("GuiMerchant-initGui出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }

    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        try {
            if (this.mouseCount < 10) {
                this.mouseCount++;
                Mouse.setGrabbed(false);
            }

            this.drawDefaultBackground();
            int px = left();
            int py = top();
            int pw = panelW();
            int ph = panelH();
            GuiStyle.panel(px, py, pw, ph, new TextComponentTranslation("container.sim.gui.builder", new Object[0]).getUnformattedText(), this.fontRenderer);

            String balance = new TextComponentTranslation("container.sim.gui.money", new Object[0]).getUnformattedText() + ": " + ModSimLoader.displayMoney(ModSimLoader.money);
            GuiStyle.text(this.fontRenderer, px + pw - 12 - this.fontRenderer.getStringWidth(balance), py + 30, balance, GuiStyle.GREEN);

            int cardW = (pw - 32) / 3;
            int cardH = 42;
            int startY = py + 58;
            if (this.currentPage == 1) {
                float grandTotal = 0.0F;
                for (int b = 0; b < 9; b++) {
                    int x = px + 12 + (b % 3) * (cardW + 4);
                    int y = startY + (b / 3) * (cardH + 3);
                    Block block = this.blockAt(b);
                    float price = PricesForBlocks.getPrice(block, true);
                    int qty = this.quantities.get(b);
                    grandTotal += qty * price;
                    GuiStyle.inset(x, y, cardW, cardH);
                    GuiStyle.slot(x + 3, y + 3);
                    this.drawItem(block, x + 4, y + 4);
                    GuiStyle.text(this.fontRenderer, x + 24, y + 4, GuiLayout.truncate(this.fontRenderer, this.itemName(b), cardW - 28), GuiStyle.TEXT);
                    GuiStyle.text(this.fontRenderer, x + 4, y + 21, PricesForBlocks.formatPrice(price), GuiStyle.TEXT_MUTED);
                    GuiStyle.text(this.fontRenderer, x + cardW - 36, y + 26, String.valueOf(qty), GuiStyle.TEXT);
                }
                this.totalCost = grandTotal;
                String total = new TextComponentTranslation("container.sim.gui.total", new Object[0]).getUnformattedText() + ": " + PricesForBlocks.formatPrice(grandTotal);
                GuiStyle.text(this.fontRenderer, px + 12, py + ph - 20, total, GuiStyle.GREEN);
            } else if (this.currentPage == 2) {
                for (int b = 0; b < 9; b++) {
                    int x = px + 12 + (b % 3) * (cardW + 4);
                    int y = startY + (b / 3) * (cardH + 3);
                    Block block = this.blockAt(b);
                    float price = PricesForBlocks.getPrice(block, false);
                    GuiStyle.inset(x, y, cardW, cardH);
                    GuiStyle.slot(x + 3, y + 3);
                    this.drawItem(block, x + 4, y + 4);
                    GuiStyle.text(this.fontRenderer, x + 24, y + 4, GuiLayout.truncate(this.fontRenderer, this.itemName(b), cardW - 28), GuiStyle.TEXT);
                    GuiStyle.text(this.fontRenderer, x + 4, y + 21, new TextComponentTranslation("container.sim.gui.sell_price", new Object[0]).getUnformattedText() + " " + PricesForBlocks.formatPrice(price), GuiStyle.GREEN);
                }
                GuiStyle.text(this.fontRenderer, px + 12, py + ph - 20, GuiLayout.truncate(this.fontRenderer, new TextComponentTranslation("container.sim.gui.merchant.sell_help", new Object[0]).getUnformattedText(), pw - 24), GuiStyle.TEXT_MUTED);
            }

            super.drawScreen(mouseX, mouseY, partialTicks);
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("GuiMerchant-drawScreen出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    private void showPage() {
        try {
            if (this.currentPage == 0) {
                this.currentPage = 1;
            }
            this.buttonList.clear();
            int px = left();
            int py = top();
            int pw = panelW();
            int ph = panelH();

            //完成
            this.buttonList.add(new GuiButton(0, px + pw - 60, py + 4, 52, 18, new TextComponentTranslation("container.sim.sim_gui_BC_Done", new Object[0]).getUnformattedText()));

            GuiButton buyTab = new GuiButton(3, px + 8, py + 24, 70, 20, new TextComponentTranslation("container.sim.gui.buy", new Object[0]).getUnformattedText());
            GuiButton sellTab = new GuiButton(4, px + 82, py + 24, 70, 20, new TextComponentTranslation("container.sim.gui.sell", new Object[0]).getUnformattedText());
            buyTab.enabled = this.currentPage != 1;
            sellTab.enabled = this.currentPage != 2;
            this.buttonList.add(buyTab);
            this.buttonList.add(sellTab);

            if (this.currentPage == 1) {
                int cardW = (pw - 32) / 3;
                int cardH = 42;
                int startY = py + 58;
                for (int b = 0; b < 9; b++) {
                    int x = px + 12 + (b % 3) * (cardW + 4);
                    int y = startY + (b / 3) * (cardH + 3);
                    this.buttonList.add(new GuiButton(100 + b, x + cardW - 56, y + 20, 16, 18, "-"));
                    this.buttonList.add(new GuiButton(200 + b, x + cardW - 22, y + 20, 16, 18, "+"));
                }
                this.buttonList.add(new GuiButton(2, px + pw - 90, py + ph - 26, 82, 20, new TextComponentTranslation("container.sim.Merchant9", new Object[0]).getUnformattedText()));
            } else if (this.currentPage == 2) {
                this.buttonList.add(new GuiButton(2, px + pw - 90, py + ph - 26, 82, 20, new TextComponentTranslation("container.sim.Merchant10", new Object[0]).getUnformattedText()));
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("showPage出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    private int panelW() {
        return Math.min(380, this.width - 16);
    }

    private int panelH() {
        return Math.min(260, this.height - 16);
    }

    private int left() {
        return GuiLayout.centerX(this.width, panelW());
    }

    private int top() {
        return GuiLayout.centerY(this.height, panelH());
    }

    private Block blockAt(int index) {
        switch (index) {
            case 0:
                return Blocks.PLANKS;
            case 1:
                return Blocks.LOG;
            case 2:
                return Blocks.COBBLESTONE;
            case 3:
                return Blocks.STONE;
            case 4:
                return Blocks.GLASS;
            case 5:
                return Blocks.WOOL;
            case 6:
                return Blocks.BRICK_BLOCK;
            case 7:
                return Blocks.STONEBRICK;
            default:
                return Blocks.OAK_FENCE;
        }
    }

    private String itemName(int index) {
        String key;
        switch (index) {
            case 0:
                key = "container.sim.Merchant16";
                break;
            case 1:
                key = "container.sim.Merchant17";
                break;
            case 2:
                key = "container.sim.Merchant18";
                break;
            case 3:
                key = "container.sim.Merchant19";
                break;
            case 4:
                key = "container.sim.Merchant20";
                break;
            case 5:
                key = "container.sim.Merchant21";
                break;
            case 6:
                key = "container.sim.Merchant22";
                break;
            case 7:
                key = "container.sim.Merchant23";
                break;
            default:
                key = "container.sim.Merchant24";
                break;
        }
        return new TextComponentTranslation(key, new Object[0]).getUnformattedText();
    }

    private void drawItem(Block block, int x, int y) {
        RenderHelper.enableGUIStandardItemLighting();
        this.itemRender.renderItemIntoGUI(new ItemStack(block), x, y);
        RenderHelper.disableStandardItemLighting();
    }

    @Override
    protected void actionPerformed(GuiButton guibutton) {
        try {
            if (guibutton.enabled) {
                if (guibutton.id == 0) {
                    this.mc.currentScreen = null;
                    this.mc.setIngameFocus();
                }

                if (this.currentPage == 0) {
                    //买分页
                    if (guibutton.id == 1) {
                        this.currentPage = 1;
                    } else if (guibutton.id == 2) {
//                        this.sellStuff();
                        NetWorkLoader.net.sendToServer(new PacketSellStuff(this.id));
                        this.mc.currentScreen = null;
                        this.mc.setIngameFocus();
                    }

                    this.showPage();
                } else if (this.currentPage == 1) {
                    if (guibutton.id == 4) {
                        this.currentPage = 2;
                        this.showPage();
                        return;
                    }
                    if (guibutton.id != 1) {
                        //数量较少
                        if (guibutton.id >= 100 && guibutton.id < 200) {
                            int q = this.quantities.get(guibutton.id - 100);
                            if (q > 0) {
                                q--;
                                this.quantities.set(guibutton.id - 100, q);
                            }
                        } else if (guibutton.id >= 200) {
                            int q = this.quantities.get(guibutton.id - 200);
                            q++;
                            this.quantities.set(guibutton.id - 200, q);
                        } else if (guibutton.id == 2) {
                            if (ModSimLoader.money < this.totalCost) {
                                //抱歉，您的卡已被拒绝，您可以尝试减少购买。
                                ModSimLoader.sendChat(new TextComponentTranslation("container.sim.Merchant11",new Object[0]).getUnformattedText());
                                this.mc.currentScreen = null;
                                this.mc.setIngameFocus();
                            } else {
//                                this.buyStuff();
                                NetWorkLoader.net.sendToServer(new PacketBuyStuff(this.id,quantities));
                                this.mc.currentScreen = null;
                                this.mc.setIngameFocus();
                            }
                        }
                    }
                } else if (this.currentPage == 2) {
                    if (guibutton.id == 3) {
                        this.currentPage = 1;
                        this.showPage();
                        return;
                    }
                    if (guibutton.id >= 100 && guibutton.id < 200) {
                        int q = this.quantities.get(guibutton.id - 100);
                        if (q > 0) {
                            q--;
                            this.quantities.set(guibutton.id - 100, q);
                        }
                    } else if (guibutton.id >= 200) {
                        //阻止玩家卖出比他们拥有的更多的东西
                        int q = this.quantities.get(guibutton.id - 200);
                        if (q < sellLimits.get(guibutton.id - 200)) {
                            q++;
                            this.quantities.set(guibutton.id - 200, q);
                        }
                    } else if (guibutton.id == 2) {
//                        this.sellStuff();
                        NetWorkLoader.net.sendToServer(new PacketSellStuff(this.id));
                        this.mc.currentScreen = null;
                        this.mc.setIngameFocus();
                    }
                }
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("GUIMERCHANT-actionPerformed出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }


    }




    @Override
    public void onGuiClosed() {
        try {
            Keyboard.enableRepeatEvents(false);
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("GuiMerchant-onGuiClosed出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }

    }

    @Override
    protected void keyTyped(char c, int i) {
        try {
            if (i == 1) {
                this.mc.currentScreen = null;
                this.mc.setIngameFocus();
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("keyTyped出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    @Override
    protected void mouseClicked(int i, int j, int k) {
        try {
            super.mouseClicked(i, j, k);
        } catch (IOException e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("mouseClicked鼠标点击出错：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    /**
     * 放到箱子里
     *
     * @param chest
     * @param stack
     * @param idmeta
     * @param quantity
     * @return
     */
    public boolean placeIntoChest(IInventory chest, ItemStack stack, int idmeta, int quantity) {
        Boolean placedOK = false;
        try {
            if (stack == null) {
                placedOK = true;
                return placedOK;
            }
            for (int q = 1; q <= quantity; q++) {
                for (int g = 0; g < chest.getSizeInventory(); g++) {
                    ItemStack is = chest.getStackInSlot(g);

                    if (is == null) {
                        is = new ItemStack(stack.getItem(), 1, idmeta);
                        chest.setInventorySlotContents(g, is);
                        placedOK = true;
                        //重新进入数量循环
                        break;
                    } else if (is.getItem() == stack.getItem() && is.getMetadata() == idmeta && is.getCount() < 64) {
                        is.grow(1);;
                        chest.setInventorySlotContents(g, is);
                        placedOK = true;
                        break;
                    }
                }
            }

        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("placeIntoChest出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
        return placedOK;
    }

    public List<IInventory> inventoriesFindClosests(V3 startXYZ, int searchDistance){
        List<IInventory> ret = new CopyOnWriteArrayList();

        try {
            World world = this.mc.world;
//            IBlockState blocks =world.getBlockState(startXYZ.toBlockPos());

            TileEntity te = world.getTileEntity(startXYZ.toBlockPos());
            if (te != null && te instanceof IInventory && !(te instanceof TileEntityFurnace)) {
                ret.add((IInventory)te);
            }

            for(int d = 1; d < searchDistance; ++d) {
                for(int yo = -d; yo <= d; ++yo) {
                    for(int xo = -d; xo <= d; ++xo) {
                        for(int zo = -d; zo <= d; ++zo) {
                            int sx = (int)(Math.round(startXYZ.x) + (long)xo);
                            int sy = (int)(Math.round(startXYZ.y) + (long)yo);
                            int sz = (int)(Math.round(startXYZ.z) + (long)zo);
                            te = world.getTileEntity(new BlockPos(sx, sy, sz));
                            if (te != null && te instanceof IInventory && !this.alreadyGotChest(ret, (IInventory)te)) {
                                ret.add((IInventory)te);
                            }
                        }
                    }
                }
            }

            return ret;
        } catch (Exception var13) {
            return ret;
        }
    }
    /**
     * 已经获得箱子
     * @param chests
     * @param chest
     * @return
     */
    private boolean alreadyGotChest(List<IInventory> chests, IInventory chest) {
        boolean ret = false;
        Iterator var4 = chests.iterator();

        while(var4.hasNext()) {
            IInventory ch = (IInventory)var4.next();
            if (ch.toString().contentEquals(chest.toString())) {
                ret = true;
                break;
            }
        }
        return ret;
    }
}
