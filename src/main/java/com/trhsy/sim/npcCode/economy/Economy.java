package com.trhsy.sim.npcCode.economy;

import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.NpcData;

/**
 * 最小经济闭环（P20）。
 *
 * <p>城市/玩家是中央财政，负责发工资、收原料费、付产出补贴；
 * NPC 通过工作获得收入，再在商店买饭把一部分钱回流给城市。
 * 所有金额变动都带日志，便于长时间压测时审计资金流。
 *
 * <p>余额规则：任何一方都不会被扣成负数。城市余额不足时工资/补贴暂停，
 * NPC 余额不足时买饭降级为慈善免费（只记录，不阻断生存）。
 */
public final class Economy {
    /** 在岗 NPC 每分钟工资。 */
    public static final float WAGE_PER_MINUTE = 0.5F;
    /** 成品放入工作箱时，城市付给生产者的产出补贴（每件）。 */
    public static final float OUTPUT_SUBSIDY = 0.05F;
    /** NPC 在商店买一份食物的价格。 */
    public static final float FOOD_PRICE = 0.5F;
    /** 店主批量喂食时，每件食物向被喂 NPC 收取的价格。 */
    public static final float SHOP_MEAL_PRICE = 0.25F;

    private Economy() {
    }

    /**
     * 城市向 NPC 付款（工资 / 产出补贴）。
     *
     * @return true 表示付款成功；城市余额不足时返回 false，不产生负数。
     */
    public static boolean cityPaysNpc(NpcData npc, float amount, String reason) {
        if (npc == null || amount <= 0.0F) {
            return false;
        }
        if (ModSimLoader.money < amount) {
            ModSimLoader.log.warn("经济：城市余额不足，暂停向 {} 支付 {}（{}），当前城市余额 {}",
                    npc.getName(), String.format("%.2f", amount), reason, String.format("%.2f", ModSimLoader.money));
            return false;
        }
        ModSimLoader.addMoney(-amount);
        npc.money += amount;
        ModSimLoader.log.info("经济：城市 -> {} {}（{}），NPC 余额 {}，城市余额 {}",
                npc.getName(), String.format("%.2f", amount), reason,
                String.format("%.2f", npc.money), String.format("%.2f", ModSimLoader.money));
        return true;
    }

    /**
     * NPC 向城市付款（买饭）。余额不足返回 false，由调用方决定是否慈善免费。
     */
    public static boolean npcPaysCity(NpcData npc, float amount, String reason) {
        if (npc == null || amount <= 0.0F) {
            return false;
        }
        if (npc.money < amount) {
            ModSimLoader.log.info("经济：{} 余额不足（{} < {}），本次{}按慈善免费处理",
                    npc.getName(), String.format("%.2f", npc.money),
                    String.format("%.2f", amount), reason);
            return false;
        }
        npc.money -= amount;
        ModSimLoader.addMoney(amount);
        ModSimLoader.log.info("经济：{} -> 城市 {}（{}），NPC 余额 {}，城市余额 {}",
                npc.getName(), String.format("%.2f", amount), reason,
                String.format("%.2f", npc.money), String.format("%.2f", ModSimLoader.money));
        return true;
    }

    /**
     * 产出结算：城市按件数给生产 NPC 发补贴。
     */
    public static void payOutput(NpcData npc, int count, String jobName) {
        if (count <= 0) {
            return;
        }
        cityPaysNpc(npc, OUTPUT_SUBSIDY * (float) count, "产出补贴 " + count + " 件 " + jobName);
    }

    /**
     * 商店喂食结算：按实际喂出的件数向被喂 NPC 收费。
     *
     * @return 实际收到的金额（收不到的部分按慈善免费）
     */
    public static float chargeShopMeal(NpcData customer, int count, String shopName) {
        if (customer == null || count <= 0) {
            return 0.0F;
        }
        float amount = SHOP_MEAL_PRICE * (float) count;
        if (npcPaysCity(customer, amount, "购买 " + shopName + " 食物 " + count + " 件")) {
            return amount;
        }
        return 0.0F;
    }
}
