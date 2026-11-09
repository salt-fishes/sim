package com.trhsy.sim.npcCode.task;

import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.V3;
import com.trhsy.sim.npcCode.economy.Economy;
import com.trhsy.sim.npcCode.job.Job;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.ArrayList;
import java.util.List;

/**
 * 制作任务（P9 重做结算）。
 *
 * <p>旧实现的致命问题：统计时把背包里所有匹配材料都记入 {@code indicesToRemove} 并在
 * 计算后**无条件移除**——哪怕可生产数量是 0（材料不齐），材料也会凭空消失。
 * 现在：<br>
 *   - 原料统计 = 工作点 5 格内箱子 + NPC 背包；<br>
 *   - 可生产数 = 各需求"总数 / 每份用量"的最小值；<br>
 *   - 只有 maxProd &gt; 0 才消耗，且只消耗 maxProd × 每份用量（箱子优先，其次背包）。
 */
public class JobTaskProduceItem extends JobTask {
    Item produce;
    List<ItemStack> requirements;
    transient long timeToPlace = 0L;
    String status;
    /** P9: 成品 meta（例如熟鲑鱼 = COOKED_FISH meta 1）。 */
    private int produceMeta = 0;
    /** P9: 制作准备时间（原 10 秒 → 3 秒），配合"回岗即制作"。 */
    private static final long PREPARE_MS = 3000L;

    /**
     * @Author fan
     * @Description //TODO
     * @Date 21:20 2022/11/16
     * @Param [j, ms, prod, req, stage, status]工作，耗时，成品，消耗品，状态字段
     * @return
     **/
    public JobTaskProduceItem(Job j, long ms, Item prod, List<ItemStack> req, String status) {
        super(j, ms);
        this.produce = prod;
        this.requirements = req;
        this.status = status;
    }

    public JobTaskProduceItem(Job j, long ms, Item prod, ItemStack req, String status) {
        super(j, ms);
        this.produce = prod;
        this.requirements = new ArrayList();
        this.requirements.add(req);
        this.status = status;
    }

    /** P9: 支持带 meta 的成品（例如熟鲑鱼 = COOKED_FISH meta 1）。 */
    public JobTaskProduceItem(Job j, long ms, Item prod, int meta, ItemStack req, String status) {
        super(j, ms);
        this.produce = prod;
        this.produceMeta = meta;
        this.requirements = new ArrayList();
        this.requirements.add(req);
        this.status = status;
    }

    @Override
    public void onTaskBegin() {
        //设置固定不动
        this.folk.stayPut = true;
    }

    @Override
    public void onUpdate() {
        //在工作期间
        // P20: 工作点判定放宽到水平 3 格；控制箱常嵌在家具/柜台里，
        // NPC 只能站到 1.5~2 格外的可站立面，旧判定会导致生产任务永远不执行。
        if (this.job.folk.isAtDestination(this.job.workPlace, 3.0D, 4.0D)) {
            //设置状态
            this.folk.setStatus(this.status);

            if (this.timeToPlace == 0L) {
                this.timeToPlace = System.currentTimeMillis();
                return;
            }

            if (System.currentTimeMillis() - this.timeToPlace < PREPARE_MS) {
                return;
            }

            // P9: 原料来源 = 工作点 5 格内的箱子 + NPC 背包
            List<IInventory> chests = this.job.inventoriesFindClosest(this.job.workPlace, 5);

            // 可生产数 = 各需求 (总数 / 每份用量) 的最小值
            int maxProd = -1;
            for (ItemStack requiredStack : this.requirements) {
                int avail = countInList(this.folk.inventory, requiredStack) + countInChests(chests, requiredStack);
                int div = requiredStack.getCount() > 0 ? avail / requiredStack.getCount() : 0;
                if (maxProd == -1 || div < maxProd) {
                    maxProd = div;
                }
            }
            if (maxProd < 0) {
                maxProd = 0;
            }
            ModSimLoader.log.info("制作 " + maxProd + " " + this.produce.getUnlocalizedName());

            if (maxProd > 0) {
                // P9: 只扣除"实际生产用量"（maxProd × 每份用量）；箱子优先，其次背包。
                for (ItemStack requiredStack : this.requirements) {
                    int need = maxProd * requiredStack.getCount();
                    need = consumeFromChests(chests, requiredStack, need);
                    if (need > 0) {
                        consumeFromList(this.folk.inventory, requiredStack, need);
                    }
                }
                //箱子里放入制作的物品
                this.job.placeInJobChest(new ItemStack(this.produce, maxProd, this.produceMeta));
                // P20: 原料成本照旧由城市承担，成品再按件给生产 NPC 发产出补贴。
                ModSimLoader.addMoney(-0.02F * (float) maxProd);
                Economy.payOutput(this.job.folk, maxProd, this.job.jobName);
            }
            //完成任务
            this.completeTask();
        } else {
            //返回工作岗位
            this.folk.setStatus(new TextComponentTranslation("container.sim.job_task_Returning_to_work",new Object[0]).getUnformattedText());
            if (!this.job.folk.entity.isMoving() && this.job.folk.entity.getNavigator().getPath() == null) {
                V3 v3=this.job.workPlace;

                this.folk.forceMoveToXYZ(v3);
            }
        }

    }

    /** 统计一组 ItemStack 中匹配原型的总数。 */
    private int countInList(List<ItemStack> list, ItemStack proto) {
        int total = 0;
        if (list == null) {
            return 0;
        }
        for (ItemStack st : list) {
            if (st != null && !st.isEmpty() && ItemStack.areItemsEqual(st, proto)
                    && ItemStack.areItemStackTagsEqual(st, proto)) {
                total += st.getCount();
            }
        }
        return total;
    }

    /** 统计一组箱子中匹配原型的总数。 */
    private int countInChests(List<IInventory> chests, ItemStack proto) {
        int total = 0;
        for (IInventory inv : chests) {
            for (int slot = 0; slot < inv.getSizeInventory(); slot++) {
                ItemStack st = inv.getStackInSlot(slot);
                if (st != null && !st.isEmpty() && ItemStack.areItemsEqual(st, proto)
                        && ItemStack.areItemStackTagsEqual(st, proto)) {
                    total += st.getCount();
                }
            }
        }
        return total;
    }

    /** 从箱子扣除最多 need 个匹配物品，返回还差多少。 */
    private int consumeFromChests(List<IInventory> chests, ItemStack proto, int need) {
        for (IInventory inv : chests) {
            for (int slot = 0; slot < inv.getSizeInventory() && need > 0; slot++) {
                ItemStack st = inv.getStackInSlot(slot);
                if (st == null || st.isEmpty() || !ItemStack.areItemsEqual(st, proto)
                        || !ItemStack.areItemStackTagsEqual(st, proto)) {
                    continue;
                }
                int take = Math.min(need, st.getCount());
                inv.decrStackSize(slot, take);
                need -= take;
            }
            if (need <= 0) {
                break;
            }
        }
        return need;
    }

    /** 从 NPC 背包扣除最多 need 个匹配物品，返回还差多少。 */
    private int consumeFromList(List<ItemStack> list, ItemStack proto, int need) {
        if (list == null) {
            return need;
        }
        for (int i = list.size() - 1; i >= 0 && need > 0; i--) {
            ItemStack st = list.get(i);
            if (st == null || st.isEmpty() || !ItemStack.areItemsEqual(st, proto)
                    || !ItemStack.areItemStackTagsEqual(st, proto)) {
                continue;
            }
            int take = Math.min(need, st.getCount());
            st.shrink(take);
            need -= take;
            if (st.isEmpty()) {
                list.remove(i);
            }
        }
        return need;
    }

    @Override
    public void onTaskComplete() {
    }
}
