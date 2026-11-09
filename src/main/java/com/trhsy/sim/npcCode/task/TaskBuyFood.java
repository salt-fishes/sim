package com.trhsy.sim.npcCode.task;

import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.NpcData;
import com.trhsy.sim.npcCode.V3;
import com.trhsy.sim.npcCode.build.Building;
import com.trhsy.sim.npcCode.economy.Economy;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/**
 * 去商店买食物（P4 经济闭环）。
 *
 * <p>NPC 饿了（hunger &lt; 6）且有钱时，会走到最近的食品店，从店里箱子取一份食物：
 * <pre>
 *   NPC.money -= FOOD_PRICE
 *   ModSimLoader.money += FOOD_PRICE   // 店主/玩家收钱
 *   吃掉食物，hunger 回升
 * </pre>
 * 工资来自上班（见 {@code Job.onMinute}），于是形成
 * "工作 → 工资 → 买食物 → 钱回流给店主" 的闭环。
 */
public class TaskBuyFood extends Task {
    /** 一份食物的价格（P20：统一走 Economy，真正扣 NPC 余额）。 */
    public static final float FOOD_PRICE = Economy.FOOD_PRICE;

    private final Building shop;
    private transient long actionDelay = 0L;

    public TaskBuyFood(NpcData folk, long ms, Building shop) {
        super(folk, ms);
        this.shop = shop;
        this.statusText = new TextComponentTranslation("container.sim.folk_data_Shopping", new Object[0]).getUnformattedText();
    }

    @Override
    public void onTaskBegin() {
        this.folk.stayPut = false;
        this.folk.setStatus(this.statusText);
    }

    @Override
    public void onUpdate() {
        try {
            if (this.shop == null || this.folk.entity == null) {
                this.completeTask();
                return;
            }
            V3 target = this.shop.livingXYZ != null ? this.shop.livingXYZ : this.shop.controlXYZ;
            if (target == null) {
                this.completeTask();
                return;
            }
            // 先到店
            if (!this.folk.isAtBuilding(this.shop, 3)) {
                this.folk.forceMoveToXYZ(target);
                return;
            }
            // 到店后每秒尝试一次
            if (System.currentTimeMillis() - this.actionDelay < 1000L) {
                return;
            }
            this.actionDelay = System.currentTimeMillis();

            // P4: 先不做定价系统：免费取用，只要 NPC 能自己吃到饭
            ItemStack food = takeOneFood();
            if (food == null) {
                // 店里没货，先回去（店主补货后再来）
                this.folk.setStatus(new TextComponentTranslation("container.sim.folk_data.Wandering", new Object[0]).getUnformattedText());
                this.completeTask();
                return;
            }
            // 吃掉
            int heal = 1;
            if (food.getItem() instanceof ItemFood) {
                heal = Math.max(1, ((ItemFood) food.getItem()).getHealAmount(food) / 2);
            }
            // P20: 先结账再吃；余额不足时按慈善免费，保证不会饿死。
            boolean paid = Economy.npcPaysCity(this.folk, FOOD_PRICE, "在 " + this.shop.buildingName + " 买饭");
            if (!paid) {
                ModSimLoader.log.info("{} 在 {} 慈善免费获得一份食物", this.folk.getName(), this.shop.buildingName);
            }
            this.folk.hunger = Math.min(10.0D, this.folk.hunger + heal);
            ModSimLoader.log.info("{} 在 {} 买了食物，余额 {}，饥饿值 {}",
                    this.folk.getName(), this.shop.buildingName, this.folk.money, this.folk.hunger);
            this.completeTask();
        } catch (Exception e) {
            ModSimLoader.log.error("TaskBuyFood出错了：" + e.getMessage());
            if (!this.completed) {
                this.completeTask();
            }
        }
    }

    /** 从商店周围 5 格内的箱子取一份食物（外壳扫描，避免 O(d^3)）。 */
    private ItemStack takeOneFood() {
        World world = this.folk.world;
        BlockPos center = (this.shop.livingXYZ != null ? this.shop.livingXYZ : this.shop.controlXYZ).toBlockPos();
        for (int d = 0; d < 5; d++) {
            for (int yo = -d; yo <= d; yo++) {
                for (int xo = -d; xo <= d; xo++) {
                    for (int zo = -d; zo <= d; zo++) {
                        if (Math.max(Math.abs(xo), Math.max(Math.abs(yo), Math.abs(zo))) != d) {
                            continue;
                        }
                        TileEntity te = world.getTileEntity(center.add(xo, yo, zo));
                        if (!(te instanceof IInventory)) {
                            continue;
                        }
                        IInventory inv = (IInventory) te;
                        for (int slot = 0; slot < inv.getSizeInventory(); slot++) {
                            ItemStack st = inv.getStackInSlot(slot);
                            if (st.isEmpty() || !(st.getItem() instanceof ItemFood)) {
                                continue;
                            }
                            ItemStack one = st.copy();
                            one.setCount(1);
                            inv.decrStackSize(slot, 1);
                            return one;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void onTaskComplete() {
    }
}
