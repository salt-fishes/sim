package com.trhsy.sim.entity.container;

import com.trhsy.sim.entity.EntityNpc;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.entity.container
 * @ClassName: ContainerNpc
 * @Description:
 * @date 2024/3/21 14:41
 */
public class ContainerNpc extends Container {
    /**
     * 有效的装备口
     * 头，胸，腿，脚
     **/
    private static final EntityEquipmentSlot[] VALID_EQUIPMENT_SLOTS = new EntityEquipmentSlot[]{EntityEquipmentSlot.HEAD, EntityEquipmentSlot.CHEST, EntityEquipmentSlot.LEGS, EntityEquipmentSlot.FEET};
    /**
     * 工艺矩阵
     */
    public InventoryCrafting craftMatrix = new InventoryCrafting(this, 2, 2);
    /**
     * 合成结果
     */
    public InventoryCraftResult craftResult = new InventoryCraftResult();
    /**
     * 确定是否应处理库存操作。
     */
    public boolean isLocalWorld;

    private final EntityNpc npc;

    public ContainerNpc(InventoryNpc npcInventory, boolean localWorld, EntityNpc npc) {
        this.isLocalWorld = localWorld;
        this.npc = npc;
        //添加插槽到容器
        this.addSlotToContainer(new SlotNpcCrafting(npcInventory.player, this.craftResult, 0, 154, 28));

        //添加合成矩阵的插槽
        for (int i = 0; i < 2; ++i) {
            for (int j = 0; j < 2; ++j) {
//                this.addSlotToContainer(new Slot(this.craftMatrix, j + i * 2, 30 + j * 18, 17 + i * 18));
            }
        }
        for (int k = 0; k < 4; ++k) {
            //装备插槽
            final EntityEquipmentSlot entityequipmentslot = VALID_EQUIPMENT_SLOTS[k];
            this.addSlotToContainer(new Slot(npcInventory, 36 + (3 - k), 8, 8 + k * 18) {
                /**
                 *返回给定插槽的最大堆栈大小（通常与getInventoryStackLimit（）相同，但对于装甲插槽为1）
                 */
                @Override
                public int getSlotStackLimit() {
                    return 1;
                }

                /**
                 * 检查烟囱是否允许放置在该槽中，用于铠装槽以及熔炉燃料。
                 */
                @Override
                public boolean isItemValid(ItemStack stack) {
                    return stack.getItem().isValidArmor(stack, entityequipmentslot, npc);
                }

                /**
                 * 返回是否可以从此插槽中获取此插槽的堆栈。
                 */
                @Override
                public boolean canTakeStack(EntityPlayer playerIn) {
                    ItemStack itemstack = this.getStack();
                    return !itemstack.isEmpty() && !playerIn.isCreative() && EnchantmentHelper.hasBindingCurse(itemstack) ? false : super.canTakeStack(playerIn);
                }

                @Override
                @Nullable
                @SideOnly(Side.CLIENT)
                public String getSlotTexture() {
                    return ItemArmor.EMPTY_SLOT_NAMES[entityequipmentslot.getIndex()];
                }
            });
        }
        for (int l = 0; l < 3; ++l) {
            for (int j1 = 0; j1 < 9; ++j1) {
                this.addSlotToContainer(new Slot(npcInventory, j1 + (l + 1) * 9, 8 + j1 * 18, 84 + l * 18));
            }
        }

        for (int i1 = 0; i1 < 9; ++i1) {
            this.addSlotToContainer(new Slot(npcInventory, i1, 8 + i1 * 18, 142));
        }

        this.addSlotToContainer(new Slot(npcInventory, 40, 77, 62) {
            //贴图添加
            @Override
            @Nullable
            @SideOnly(Side.CLIENT)
            public String getSlotTexture() {
                return "minecraft:items/empty_armor_slot_shield";
            }
        });
    }

    /**
     * 当工艺矩阵改变时的回调。
     */
    @Override
    public void onCraftMatrixChanged(IInventory inventoryIn) {
//        this.slotChangedCraftingGrid(this.npc.world, this.npc, this.craftMatrix, this.craftResult);
    }

    /**
     * 当容器关闭时调用。
     */
    @Override
    public void onContainerClosed(EntityPlayer playerIn) {
        super.onContainerClosed(playerIn);
        this.craftResult.clear();

        if (!playerIn.world.isRemote) {
            this.clearContainer(playerIn, playerIn.world, this.craftMatrix);
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return this.npc != null && this.npc.isEntityAlive()
                && playerIn.getDistanceSq(this.npc) <= 64.0D;
    }

    /**
     * 移位单击插槽｛@code index｝中的堆栈时的句柄。通常情况下，这会在玩家库存和其他库存之间移动堆栈。
     */
    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            EntityEquipmentSlot entityequipmentslot = EntityLiving.getSlotForItemStack(itemstack);

            if (index == 0) {
                if (!this.mergeItemStack(itemstack1, 9, 45, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onSlotChange(itemstack1, itemstack);
            } else if (index >= 1 && index < 5) {
                if (!this.mergeItemStack(itemstack1, 9, 45, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 5 && index < 9) {
                if (!this.mergeItemStack(itemstack1, 9, 45, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (entityequipmentslot.getSlotType() == EntityEquipmentSlot.Type.ARMOR && !((Slot) this.inventorySlots.get(8 - entityequipmentslot.getIndex())).getHasStack()) {
                int i = 8 - entityequipmentslot.getIndex();

                if (!this.mergeItemStack(itemstack1, i, i + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (entityequipmentslot == EntityEquipmentSlot.OFFHAND && !((Slot) this.inventorySlots.get(45)).getHasStack()) {
                if (!this.mergeItemStack(itemstack1, 45, 46, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 9 && index < 36) {
                if (!this.mergeItemStack(itemstack1, 36, 45, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 36 && index < 45) {
                if (!this.mergeItemStack(itemstack1, 9, 36, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.mergeItemStack(itemstack1, 9, 45, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            ItemStack itemstack2 = slot.onTake(playerIn, itemstack1);

            if (index == 0) {
                playerIn.dropItem(itemstack2, false);
            }
        }

        return itemstack;
    }

    /**
     * 调用以确定当前槽对于堆栈合并（双击）代码是否有效。对于双击的初始插槽，传入的堆栈为null。
     */
    @Override
    public boolean canMergeSlot(ItemStack stack, Slot slotIn) {
        return slotIn.inventory != this.craftResult && super.canMergeSlot(stack, slotIn);
    }
}
