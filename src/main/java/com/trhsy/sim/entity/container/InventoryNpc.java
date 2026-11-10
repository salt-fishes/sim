package com.trhsy.sim.entity.container;

import com.trhsy.sim.entity.EntityNpc;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;

import java.util.Arrays;
import java.util.List;

/**
 * NPC 的背包（41 格：0-8 快捷栏、9-35 主背包、36-39 护甲、40 副手）。
 *
 * <p>原实现是一个所有方法都返回 null/0 的占位类，打开 NPC 背包时
 * GuiContainer 会因为 getStackInSlot 返回 null 直接 NPE。这里补成
 * InventoryPlayer 等价的最小实现。</p>
 */
public class InventoryNpc implements IInventory {

    /** 36 格主背包（含快捷栏）。 */
    public final NonNullList<ItemStack> mainInventory = NonNullList.<ItemStack>withSize(36, ItemStack.EMPTY);
    /** 4 格护甲。 */
    public final NonNullList<ItemStack> armorInventory = NonNullList.<ItemStack>withSize(4, ItemStack.EMPTY);
    /** 1 格副手。 */
    public final NonNullList<ItemStack> offHandInventory = NonNullList.<ItemStack>withSize(1, ItemStack.EMPTY);
    private final List<NonNullList<ItemStack>> allInventories;
    /** 当前选中快捷栏。 */
    public int currentItem;
    /** 背包所属 NPC。 */
    public EntityNpc player;
    private ItemStack itemStack = ItemStack.EMPTY;
    private int timesChanged;

    public InventoryNpc(EntityNpc playerIn) {
        this.allInventories = Arrays.<NonNullList<ItemStack>>asList(this.mainInventory, this.armorInventory, this.offHandInventory);
        this.player = playerIn;
    }

    @Override
    public int getSizeInventory() {
        return 41;
    }

    @Override
    public boolean isEmpty() {
        for (NonNullList<ItemStack> list : this.allInventories) {
            for (ItemStack stack : list) {
                if (stack != null && !stack.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        if (index >= 0 && index < 9) {
            return this.mainInventory.get(index);
        }
        if (index >= 9 && index < 36) {
            return this.mainInventory.get(index);
        }
        if (index >= 36 && index < 40) {
            return this.armorInventory.get(index - 36);
        }
        if (index == 40) {
            return this.offHandInventory.get(0);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        ItemStack stack = this.getStackInSlot(index);
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = stack.splitStack(count);
        if (stack.isEmpty()) {
            this.setInventorySlotContents(index, ItemStack.EMPTY);
        }
        return result;
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        ItemStack stack = this.getStackInSlot(index);
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        this.setInventorySlotContents(index, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        ItemStack value = stack == null ? ItemStack.EMPTY : stack;
        if (index >= 0 && index < 36) {
            this.mainInventory.set(index, value);
        } else if (index >= 36 && index < 40) {
            this.armorInventory.set(index - 36, value);
        } else if (index == 40) {
            this.offHandInventory.set(0, value);
        }
        this.markDirty();
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public void markDirty() {
        this.timesChanged++;
    }

    public int getTimesChanged() {
        return this.timesChanged;
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return this.player != null && this.player.isEntityAlive()
                && player.getDistanceSq(this.player) <= 64.0D;
    }

    @Override
    public void openInventory(EntityPlayer player) {
    }

    @Override
    public void closeInventory(EntityPlayer player) {
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return true;
    }

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {
    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clear() {
        for (NonNullList<ItemStack> list : this.allInventories) {
            for (int i = 0; i < list.size(); i++) {
                list.set(i, ItemStack.EMPTY);
            }
        }
    }

    public ItemStack getItemStack() {
        return this.itemStack;
    }

    public void setItemStack(ItemStack stack) {
        this.itemStack = stack == null ? ItemStack.EMPTY : stack;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new net.minecraft.util.text.TextComponentString("NPC Inventory");
    }

    @Override
    public String getName() {
        return "NPC Inventory";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }
}
