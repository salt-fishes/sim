package com.trhsy.sim.gui.npc;

import com.trhsy.sim.gui.ATMscreen;
import com.trhsy.sim.gui.GuiLayout;
import com.trhsy.sim.gui.GuiStyle;
import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.loader.SoundRegistry;
import com.trhsy.sim.network.client.PacketOpenMerchantsGui;
import com.trhsy.sim.npcCode.V3;
import com.trhsy.sim.util.PricesForBlocks;
import com.trhsy.sim.util.items.Commodity;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.TextComponentTranslation;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.gui.npc
 * @ClassName: GuiMerchants
 * @Description: 杂货商
 * @date 2023/07/24 下午 4:02
 */
public class GuiMerchants extends GuiScreen {
    private UUID id;
    //杂货商地址
    private V3 bankLocation;
    //银屏
    private ATMscreen theScreen;
    long fuckingBodge;
    //错误文本
    private String errorText;
    //鼠标计数
    private int mouseCount = 0;
    private List<Commodity> cart;
    private List<ItemStack> depositItems = new ArrayList<ItemStack>();

    public GuiMerchants() {
    }

    public GuiMerchants(PacketOpenMerchantsGui message) {
        this.id = message.id;
        this.bankLocation = message.v3;
        this.theScreen = ATMscreen.START;
        this.fuckingBodge = 0L;
        this.errorText = "";
        this.cart = new CopyOnWriteArrayList();
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
            if (Commodity.theCommodities.size() == 0) {
                Commodity.refreshAvailableCommoditities();
            }
            if (this.theScreen == ATMscreen.START) {
                this.theScreen = ATMscreen.DEPOSIT;
            }
            this.buttonList.clear();
            int px = left();
            int py = top();
            int pw = panelW();
            int ph = panelH();

            this.buttonList.add(new GuiButton(0, px + pw - 60, py + 4, 52, 18, new TextComponentTranslation("container.sim.sim_gui_BC_Done", new Object[0]).getUnformattedText()));
            GuiButton sellTab = new GuiButton(10, px + 8, py + 24, 80, 20, new TextComponentTranslation("container.sim.gui.sell", new Object[0]).getUnformattedText());
            GuiButton buyTab = new GuiButton(11, px + 92, py + 24, 80, 20, new TextComponentTranslation("container.sim.gui.buy", new Object[0]).getUnformattedText());
            sellTab.enabled = this.theScreen != ATMscreen.DEPOSIT;
            buyTab.enabled = this.theScreen != ATMscreen.COMMODITIES;
            this.buttonList.add(sellTab);
            this.buttonList.add(buyTab);

            if (this.theScreen == ATMscreen.DEPOSIT) {
                this.buildDepositItems();
                int cardW = (pw - 32) / 3;
                int cardH = 32;
                int startY = py + 62;
                for (int i = 0; i < this.depositItems.size() && i < 15; i++) {
                    int x = px + 12 + (i % 3) * (cardW + 4);
                    int y = startY + (i / 3) * (cardH + 2);
                    this.buttonList.add(new GuiButton(100 + i, x + cardW - 56, y + 12, 24, 18, new TextComponentTranslation("container.sim.gui.sell_one", new Object[0]).getUnformattedText()));
                    this.buttonList.add(new GuiButton(500 + i, x + cardW - 30, y + 12, 28, 18, new TextComponentTranslation("container.sim.gui.sell_all", new Object[0]).getUnformattedText()));
                }
            } else if (this.theScreen == ATMscreen.COMMODITIES) {
                int cardW = (pw - 32) / 3;
                int cardH = 32;
                int startY = py + 62;
                for (int i = 0; i < Commodity.theCommodities.size() && i < 15; i++) {
                    int x = px + 12 + (i % 3) * (cardW + 4);
                    int y = startY + (i / 3) * (cardH + 2);
                    this.buttonList.add(new GuiButton(200 + i, x + cardW - 58, y + 12, 16, 18, "-"));
                    this.buttonList.add(new GuiButton(300 + i, x + cardW - 24, y + 12, 16, 18, "+"));
                }
                this.buttonList.add(new GuiButton(400, px + pw - 90, py + ph - 26, 82, 20, new TextComponentTranslation("container.sim.sim_gui_ATMs_Buy", new Object[0]).getUnformattedText()));
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("GuiMerchants-initGui出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        try {
            if (this.mouseCount < 10) {
                ++this.mouseCount;
                Mouse.setGrabbed(false);
            }
            this.drawDefaultBackground();
            int px = left();
            int py = top();
            int pw = panelW();
            int ph = panelH();
            GuiStyle.panel(px, py, pw, ph, new TextComponentTranslation("container.sim.gui.grocery", new Object[0]).getUnformattedText(), this.fontRenderer);

            String balance = new TextComponentTranslation("container.sim.gui.money", new Object[0]).getUnformattedText() + ": " + ModSimLoader.displayMoney(ModSimLoader.money);
            GuiStyle.text(this.fontRenderer, px + pw - 12 - this.fontRenderer.getStringWidth(balance), py + 30, balance, GuiStyle.GREEN);

            int cardW = (pw - 32) / 3;
            int cardH = 32;
            int startY = py + 62;
            if (this.theScreen == ATMscreen.DEPOSIT) {
                GuiStyle.text(this.fontRenderer, px + 12, py + 50, new TextComponentTranslation("container.sim.sim_gui_ATMs_Items1", new Object[0]).getUnformattedText(), GuiStyle.TEXT_MUTED);
                if (this.depositItems.isEmpty()) {
                    GuiStyle.text(this.fontRenderer, px + 12, startY + 4, new TextComponentTranslation("container.sim.sim_gui_ATMs_sorry", new Object[0]).getUnformattedText(), GuiStyle.TEXT_MUTED);
                } else {
                    for (int i = 0; i < this.depositItems.size() && i < 15; i++) {
                        ItemStack stack = this.depositItems.get(i);
                        int x = px + 12 + (i % 3) * (cardW + 4);
                        int y = startY + (i / 3) * (cardH + 2);
                        int count = this.countType(stack);
                        float price = this.depositPrice(stack);
                        GuiStyle.inset(x, y, cardW, cardH);
                        GuiStyle.slot(x + 2, y + 2);
                        this.drawItem(stack, x + 3, y + 3);
                        GuiStyle.text(this.fontRenderer, x + 22, y + 2, GuiLayout.truncate(this.fontRenderer, stack.getDisplayName(), cardW - 26), GuiStyle.TEXT);
                        GuiStyle.text(this.fontRenderer, x + 2, y + 19, count + " x " + ModSimLoader.displayMoney(price), GuiStyle.GREEN);
                    }
                }
            } else if (this.theScreen == ATMscreen.COMMODITIES) {
                GuiStyle.text(this.fontRenderer, px + 12, py + 50, new TextComponentTranslation("container.sim.sim_gui_ATMs_today", new Object[0]).getUnformattedText(), GuiStyle.TEXT_MUTED);
                if (Commodity.theCommodities.isEmpty()) {
                    GuiStyle.text(this.fontRenderer, px + 12, startY + 4, new TextComponentTranslation("container.sim.sim_gui_ATMs_later", new Object[0]).getUnformattedText(), GuiStyle.TEXT_MUTED);
                } else {
                    float total = 0.0F;
                    for (int i = 0; i < Commodity.theCommodities.size() && i < 15; i++) {
                        Commodity item = Commodity.theCommodities.get(i);
                        int x = px + 12 + (i % 3) * (cardW + 4);
                        int y = startY + (i / 3) * (cardH + 2);
                        int qty = this.cartQuantity(item);
                        total += qty * item.priceEach;
                        GuiStyle.inset(x, y, cardW, cardH);
                        GuiStyle.slot(x + 2, y + 2);
                        this.drawItem(item.theItemStack, x + 3, y + 3);
                        GuiStyle.text(this.fontRenderer, x + 22, y + 2, GuiLayout.truncate(this.fontRenderer, item.theItemStack.getDisplayName(), cardW - 26), GuiStyle.TEXT);
                        GuiStyle.text(this.fontRenderer, x + 2, y + 19, item.quantity + " x " + ModSimLoader.displayMoney(item.priceEach), GuiStyle.TEXT_MUTED);
                        GuiStyle.text(this.fontRenderer, x + cardW - 40, y + 19, String.valueOf(qty), GuiStyle.TEXT);
                    }
                    GuiStyle.text(this.fontRenderer, px + 12, py + ph - 20, new TextComponentTranslation("container.sim.gui.total", new Object[0]).getUnformattedText() + ": " + ModSimLoader.displayMoney(total), GuiStyle.GREEN);
                }
            }

            if (this.errorText != null && !this.errorText.isEmpty()) {
                GuiStyle.text(this.fontRenderer, px + 12, py + 46, GuiLayout.truncate(this.fontRenderer, this.errorText, pw - 24), GuiStyle.RED);
            }
            super.drawScreen(mouseX, mouseY, partialTicks);
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("GuiMerchants-drawScreen出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    private int panelW() {
        return Math.min(360, this.width - 16);
    }

    private int panelH() {
        return Math.min(250, this.height - 16);
    }

    private int left() {
        return GuiLayout.centerX(this.width, panelW());
    }

    private int top() {
        return GuiLayout.centerY(this.height, panelH());
    }

    private void drawItem(ItemStack stack, int x, int y) {
        RenderHelper.enableGUIStandardItemLighting();
        this.itemRender.renderItemIntoGUI(stack, x, y);
        RenderHelper.disableStandardItemLighting();
    }

    private void buildDepositItems() {
        this.depositItems.clear();
        for (int i = 0; i < this.mc.player.inventory.getSizeInventory(); i++) {
            ItemStack stack = this.mc.player.inventory.getStackInSlot(i);
            if (stack == null || stack.isEmpty() || !this.isAccepted(stack)) {
                continue;
            }
            boolean known = false;
            for (ItemStack type : this.depositItems) {
                if (this.sameType(type, stack)) {
                    known = true;
                    break;
                }
            }
            if (!known) {
                this.depositItems.add(new ItemStack(stack.getItem(), 1, stack.getMetadata()));
            }
        }
    }

    private boolean isAccepted(ItemStack stack) {
        Item item = stack.getItem();
        return item == Items.ENDER_PEARL || item == Items.BLAZE_ROD || item == Items.BONE
                || item == Items.GUNPOWDER || item == Items.SLIME_BALL || item == Items.STRING
                || item == Items.SPIDER_EYE;
    }

    private float depositPrice(ItemStack stack) {
        Item item = stack.getItem();
        if (item == Items.ENDER_PEARL) {
            return PricesForBlocks.bankPriceDiamond;
        }
        if (item == Items.BLAZE_ROD) {
            return PricesForBlocks.bankPriceEmerald;
        }
        if (item == Items.BONE) {
            return PricesForBlocks.bankPriceRedstone;
        }
        if (item == Items.GUNPOWDER) {
            return PricesForBlocks.bankPriceGlowstone;
        }
        if (item == Items.SLIME_BALL) {
            return PricesForBlocks.bankPriceGold;
        }
        if (item == Items.STRING) {
            return PricesForBlocks.bankPriceIron;
        }
        return PricesForBlocks.bankPriceIron;
    }

    private boolean sameType(ItemStack a, ItemStack b) {
        return a != null && b != null && a.getItem() == b.getItem() && a.getMetadata() == b.getMetadata();
    }

    private int countType(ItemStack type) {
        int count = 0;
        for (int i = 0; i < this.mc.player.inventory.getSizeInventory(); i++) {
            ItemStack stack = this.mc.player.inventory.getStackInSlot(i);
            if (stack != null && !stack.isEmpty() && this.sameType(type, stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private void sellDeposit(int typeIndex, boolean all) {
        if (typeIndex < 0 || typeIndex >= this.depositItems.size()) {
            return;
        }
        ItemStack type = this.depositItems.get(typeIndex);
        float unit = this.depositPrice(type);
        float total = 0.0F;
        for (int i = 0; i < this.mc.player.inventory.getSizeInventory(); i++) {
            ItemStack stack = this.mc.player.inventory.getStackInSlot(i);
            if (stack == null || stack.isEmpty() || !this.sameType(type, stack)) {
                continue;
            }
            if (all) {
                total += unit * stack.getCount();
                this.mc.player.inventory.setInventorySlotContents(i, ItemStack.EMPTY);
            } else {
                total += unit;
                stack.shrink(1);
                if (stack.getCount() <= 0) {
                    this.mc.player.inventory.setInventorySlotContents(i, ItemStack.EMPTY);
                }
                break;
            }
        }
        if (total > 0.0F) {
            ModSimLoader.money += total;
            this.playCashSound(SoundRegistry.CASHSHORT);
            this.errorText = "";
        }
        this.initGui();
    }

    private int cartQuantity(Commodity commodity) {
        int qty = 0;
        for (Commodity cartItem : this.cart) {
            if (this.sameStack(cartItem.theItemStack, commodity.theItemStack)) {
                qty += cartItem.quantity;
            }
        }
        return qty;
    }

    private boolean sameStack(ItemStack a, ItemStack b) {
        return a != null && b != null && a.getItem() == b.getItem() && a.getMetadata() == b.getMetadata();
    }

    private void changeCart(int index, int delta) {
        if (index < 0 || index >= Commodity.theCommodities.size()) {
            return;
        }
        Commodity commodity = Commodity.theCommodities.get(index);
        for (int i = 0; i < this.cart.size(); i++) {
            Commodity cartItem = this.cart.get(i);
            if (this.sameStack(cartItem.theItemStack, commodity.theItemStack)) {
                int next = cartItem.quantity + delta;
                if (next <= 0) {
                    this.cart.remove(i);
                } else {
                    cartItem.quantity = Math.min(next, commodity.quantity);
                }
                return;
            }
        }
        if (delta > 0) {
            this.cart.add(new Commodity(commodity.theItemStack, 1, commodity.priceEach));
        }
    }

    private void buyCart() {
        if (this.cart.isEmpty()) {
            this.errorText = new TextComponentTranslation("container.sim.sim_gui_ATMs_added", new Object[0]).getUnformattedText();
            return;
        }
        float cost = 0.0F;
        for (Commodity cartItem : this.cart) {
            cost += cartItem.quantity * cartItem.priceEach;
        }
        if (cost > ModSimLoader.money) {
            this.errorText = new TextComponentTranslation("container.sim.sim_gui_ATMs_cost", new Object[0]).getUnformattedText()
                    + ModSimLoader.displayMoney(cost)
                    + new TextComponentTranslation("container.sim.sim_gui_ATMs_only", new Object[0]).getUnformattedText()
                    + ModSimLoader.displayMoney(ModSimLoader.money);
            return;
        }
        for (Commodity cartItem : this.cart) {
            ItemStack bought = cartItem.theItemStack.copy();
            bought.setCount(cartItem.quantity);
            this.mc.player.inventory.addItemStackToInventory(bought);
            for (int i = 0; i < Commodity.theCommodities.size(); i++) {
                if (this.sameStack(Commodity.theCommodities.get(i).theItemStack, cartItem.theItemStack)) {
                    Commodity.theCommodities.remove(i);
                    break;
                }
            }
        }
        ModSimLoader.money -= cost;
        this.playCashSound(SoundRegistry.CASH);
        ModSimLoader.sendChat(new TextComponentTranslation("container.sim.sim_gui_ATMs_worth", new Object[0]).getUnformattedText() + ModSimLoader.displayMoney(cost));
        this.mc.currentScreen = null;
        this.mc.setIngameFocus();
    }

    private void playCashSound(SoundEvent event) {
        if (event == null || event.getRegistryName() == null) {
            return;
        }
        this.mc.world.playSound(this.mc.player.posX, this.mc.player.posY, this.mc.player.posZ, event, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        try {
            if (!button.enabled) {
                return;
            }
            if (button.id == 0) {
                this.mc.currentScreen = null;
                this.mc.setIngameFocus();
                return;
            }
            if (button.id == 10) {
                this.theScreen = ATMscreen.DEPOSIT;
                this.errorText = "";
                this.initGui();
                return;
            }
            if (button.id == 11) {
                this.theScreen = ATMscreen.COMMODITIES;
                this.errorText = "";
                this.initGui();
                return;
            }
            if (button.id >= 100 && button.id < 200) {
                this.sellDeposit(button.id - 100, false);
                return;
            }
            if (button.id >= 500 && button.id < 600) {
                this.sellDeposit(button.id - 500, true);
                return;
            }
            if (button.id >= 200 && button.id < 300) {
                this.changeCart(button.id - 200, -1);
                return;
            }
            if (button.id >= 300 && button.id < 400) {
                this.changeCart(button.id - 300, 1);
                return;
            }
            if (button.id == 400) {
                this.buyCart();
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("GuiMerchants-actionPerformed出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }
}
