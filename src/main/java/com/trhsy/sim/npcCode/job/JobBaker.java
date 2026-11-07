package com.trhsy.sim.npcCode.job;

import com.trhsy.sim.loader.ItemLoader;
import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.NpcData;
import com.trhsy.sim.npcCode.task.*;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @ClassName JobBaker
 * @Description todo 面包师的工作
 * @Author TRHSY
 * @Date 2022/11/1422:27
 **/
public class JobBaker extends Job {
    //小麦，鸡蛋，南瓜，牛奶,糖,可可豆
    private int wheat, egg, pumpkin, milk_bucket, sugar, dye;
    //工作阶段
    public int baker_stage = 0;
    // 上次统计物品的时间戳（单位：ticks，1 tick = 1/20秒）
    private long lastItemStatsTime = 0;
    // 统计间隔（五分钟 = 5 * 60秒 = 300秒 = 300 * 20 ticks = 6000 ticks）
    private static final long STATS_INTERVAL = 1000;
    // 单次最大制作份数（避免任务过多卡顿）
    private static final int MAX_BATCH_COUNT = 5;
    /** P9: 补货循环——任一原料低于此数量就再收一轮。 */
    private static final int RESTOCK_MIN = 16;
    /** P9: 单日最多补货轮数（供货点没货时避免整天在外转）。 */
    private static final int MAX_RESTOCK_ROUNDS = 4;
    private int restockRounds = 0;
    /** P9: 记录天数，每天重置补货轮数。 */
    private long lastBakerDay = -1L;
    public JobBaker(NpcData folk, BlockPos pos, World world) {
        super(folk, pos, world);
        folk.holding = new ItemStack(ItemLoader.tinSpade);
        //面包师
        this.jobName = new TextComponentTranslation("container.sim.Vocation6",new Object[0]).getUnformattedText();
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        try {
        if (this.atWork) {
            if (this.stage == -1) {
                // P9: 队列耗尽——先决定"补货"还是"开卖"。
                // 原料任一低于 RESTOCK_MIN 且白天还有时间：再收一轮（最多 4 轮）；
                // 否则开卖；已过打烊时段就等待下班。
                long day = this.jobWorld.getWorldTime() / 24000L;
                long wt = this.jobWorld.getWorldTime() % 24000L;
                if (day != this.lastBakerDay) {
                    this.lastBakerDay = day;
                    this.restockRounds = 0;
                }
                this.initInventory();
                if (wt < 10000L && this.restockRounds < MAX_RESTOCK_ROUNDS && this.needMoreIngredients()) {
                    this.restockRounds++;
                    ModSimLoader.log.info("面包店原料不足，开始第 {} 轮补货", this.restockRounds);
                    this.baker_stage = 1; // 1=加"到位"idle，之后 2=采货
                } else if (wt < 11600L) {
                    this.baker_stage = 5; // 5=开卖
                } else {
                    this.baker_stage = 6; // 已打烊：不接新任务，等下班
                }
                this.stage = 0;
            }else if (this.baker_stage == 0) {
                this.baker_stage = 1;
                //去上班
                this.addJobTask(new JobTaskIdle(this, 200L, new TextComponentTranslation("container.sim.job.builder_Arrived",new Object[0]).getUnformattedText()));
            }else if (this.baker_stage == 1) {
                //打开烘焙工具
                this.addJobTask(new JobTaskIdle(this, 200L, new TextComponentTranslation("container.sim.job_baker1",new Object[0]).getUnformattedText()));
                this.baker_stage = 2;
            } else if (this.baker_stage == 2) {
                List<ItemStack> colItems = new ArrayList();
                //小麦
                colItems.add(new ItemStack(Items.WHEAT, 16));
                //鸡蛋
                colItems.add(new ItemStack(Items.EGG, 16));
                //南瓜
                colItems.add(new ItemStack(Blocks.PUMPKIN, 16));
                //牛奶
                colItems.add(new ItemStack(ItemLoader.itemBucketMilk, 1));
                //糖
                colItems.add(new ItemStack(Items.SUGAR, 16));
                //可可豆
                colItems.add(new ItemStack(Items.DYE, 16, 3));
                //收集
                this.addJobTask(new JobTaskCollectItems(this, -1L, colItems));
                this.baker_stage = 3;
            } else if (this.baker_stage == 3) {
                List<ItemStack> colItems = new ArrayList();
                //小麦
                colItems.add(new ItemStack(Items.WHEAT, 24));
                //鸡蛋
                colItems.add(new ItemStack(Items.EGG, 24));
                //南瓜
                colItems.add(new ItemStack(Blocks.PUMPKIN, 24));
                //牛奶
                colItems.add(new ItemStack(ItemLoader.itemBucketMilk, 24));
                //糖
                colItems.add(new ItemStack(Items.SUGAR, 24));
                //可可豆
                colItems.add(new ItemStack(Items.DYE, 24, 3));
                this.addJobTask(new JobTaskUnloadItems(this, -1L, colItems));
                this.baker_stage = 4;
            } else if (this.baker_stage == 4) {
                    initInventory();
                // 批量制作：根据原料计算最大可制作份数，生成对应任务
//                batchProduceFood();
                    if (this.milk_bucket >= 3 && this.sugar >= 2 && this.egg >= 1 && this.wheat > 3) {
                        //蛋糕需要的食材
                        List<ItemStack> cakes = new CopyOnWriteArrayList<ItemStack>();
                        //牛奶
                        cakes.add(new ItemStack(ItemLoader.itemBucketMilk, 3));
                        //糖
                        cakes.add(new ItemStack(Items.SUGAR, 2));
                        //鸡蛋
                        cakes.add(new ItemStack(Items.EGG, 1));
                        //小麦
                        cakes.add(new ItemStack(Items.WHEAT, 3));
                        //蛋糕
                        this.addJobTask(new JobTaskProduceItem(this, 60000L, Items.CAKE, cakes, new TextComponentTranslation("container.sim.job.Baker_Baking",new Object[0]).getUnformattedText()));
                    }
                    if (this.pumpkin > 1 && this.sugar > 1 && this.egg > 1) {
                        //南瓜饼需要的食材
                        List<ItemStack> pumpkinPies = new CopyOnWriteArrayList<ItemStack>();
                        //南瓜
                        pumpkinPies.add(new ItemStack(Blocks.PUMPKIN, 1));
                        //糖
                        pumpkinPies.add(new ItemStack(Items.SUGAR, 1));
                        //鸡蛋
                        pumpkinPies.add(new ItemStack(Items.EGG, 1));
                        //南瓜派
                        this.addJobTask(new JobTaskProduceItem(this, 60000L, Items.PUMPKIN_PIE, pumpkinPies, new TextComponentTranslation("container.sim.job.Baker_Baking",new Object[0]).getUnformattedText()));
                    }
                    if (this.dye >= 1 && this.wheat >= 2) {
                        //曲奇饼需要的食材
                        List<ItemStack> cookies = new CopyOnWriteArrayList<ItemStack>();
                        //可可豆（DYE meta 3 = 可可豆，农民产出的正是这个）
                        cookies.add(new ItemStack(Items.DYE, 1, 3));
                        //小麦
                        cookies.add(new ItemStack(Items.WHEAT, 2));
                        //曲奇
                        this.addJobTask(new JobTaskProduceItem(this, 60000L, Items.COOKIE, cookies, new TextComponentTranslation("container.sim.job.Baker_Baking",new Object[0]).getUnformattedText()));
                    }
                    if (this.wheat > 3) {
                        //烘烤 面包 食材 小麦三个
                        this.addJobTask(new JobTaskProduceItem(this, 60000L, Items.BREAD, new ItemStack(Items.WHEAT, 3), new TextComponentTranslation("container.sim.job.Baker_Baking",new Object[0]).getUnformattedText()));
                    }
                    this.baker_stage = 5;
            } else if (this.baker_stage == 5) {
                //售卖/关店
                this.addJobTask(new JobTaskShopkeep(this, -1L, new TextComponentTranslation("container.sim.job.Baker_bread",new Object[0]).getUnformattedText(),true));
                this.baker_stage = 6;
            } else if (this.baker_stage == 6) {
                //在去工作途中，并且已经到了工作位置则更新状态
                if (this.atWork && this.folk.isAtLocation(this.workPlace) && this.currentTask == null && this.jobTasks.size() > 0) {
                    if (this.jobTasks.size() > 0) {
                        this.currentTask = (JobTask) this.jobTasks.get(0);
                        this.currentTask.begin();
                    }
                }
            }/*else{
                this.baker_stage = 0;
                this.jobTasks.clear();
            }*/
            // P9: 原"每 50 秒强制清队列重做"的统计块已删除——它会打断正在
            // 执行的采货/卸货/制作任务。补货现在由 stage==-1 时的循环统一决策。
        }
        }catch (Exception e){
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("jobMaker-onUpdate出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }
    public void initInventory(){
        wheat=0;egg=0; pumpkin=0; milk_bucket=0;sugar=0; dye=0;
        List<IInventory> iterator = this.findJobChests(5);
        for (IInventory inv : iterator) {
            for (int i = 0; i < inv.getSizeInventory(); ++i) {
                ItemStack slot = inv.getStackInSlot(i);
                ItemStack itemWheat = new ItemStack(Items.WHEAT);
                ItemStack itemEgg = new ItemStack(Items.EGG);
                ItemStack itemPumpkin = new ItemStack(Blocks.PUMPKIN);
                ItemStack itemBucketMilk = new ItemStack(ItemLoader.itemBucketMilk);
                ItemStack itemSugar = new ItemStack(Items.SUGAR);
                ItemStack itemDye = new ItemStack(Items.DYE);
                if (slot != null) {
                    if (slot.isItemEqual(itemWheat)) {
                        this.wheat += slot.getCount();
                    } else if (slot.isItemEqual(itemEgg)) {
                        this.egg += slot.getCount();
                    } else if (slot.isItemEqual(itemPumpkin)) {
                        this.pumpkin += slot.getCount();
                    } else if (slot.isItemEqual(itemBucketMilk)) {
                        this.milk_bucket += slot.getCount();
                    } else if (slot.isItemEqual(itemSugar)) {
                        this.sugar += slot.getCount();
                    } else if (slot.getItem().equals(itemDye.getItem())) {
                        this.dye += slot.getCount();
                    }
                }
            }
        }
    }
    /**
     * 批量制作食物：根据原料数量计算最大可制作份数，生成对应任务
     */
    private void batchProduceFood() {
        // 1. 制作面包（每份需3小麦，最多做MAX_BATCH_COUNT份）
        int breadCount = calculateMaxBatch(wheat, 3);
        for (int i = 0; i < breadCount; i++) {
            this.addJobTask(new JobTaskProduceItem(
                    this,
                    1200L, // 1分钟/份（原5分钟改为1分钟，批量制作更合理）
                    Items.BREAD,
                    new ItemStack(Items.WHEAT, 3),
                    new TextComponentTranslation("container.sim.job.Baker_Baking_bread", i + 1, breadCount).getUnformattedText()
            ));
        }

        // 2. 制作蛋糕（每份需3牛奶+2糖+1鸡蛋+3小麦）
        int cakeCount = calculateMaxBatch(
                new int[]{milk_bucket, sugar, egg, wheat},
                new int[]{3, 2, 1, 3}
        );
        for (int i = 0; i < cakeCount; i++) {
            List<ItemStack> cakeMaterials = new CopyOnWriteArrayList<>();
            cakeMaterials.add(new ItemStack(ItemLoader.itemBucketMilk, 3));
            cakeMaterials.add(new ItemStack(Items.SUGAR, 2));
            cakeMaterials.add(new ItemStack(Items.EGG, 1));
            cakeMaterials.add(new ItemStack(Items.WHEAT, 3));
            this.addJobTask(new JobTaskProduceItem(
                    this,
                    2400L, // 2分钟/份
                    Items.CAKE,
                    cakeMaterials,
                    new TextComponentTranslation("container.sim.job.Baker_Baking_cake", i + 1, cakeCount).getUnformattedText()
            ));
        }

        // 3. 制作南瓜派（每份需1南瓜+1糖+1鸡蛋）
        int pumpkinPieCount = calculateMaxBatch(
                new int[]{pumpkin, sugar, egg},
                new int[]{1, 1, 1}
        );
        for (int i = 0; i < pumpkinPieCount; i++) {
            List<ItemStack> pieMaterials = new CopyOnWriteArrayList<>();
            pieMaterials.add(new ItemStack(Blocks.PUMPKIN, 1));
            pieMaterials.add(new ItemStack(Items.SUGAR, 1));
            pieMaterials.add(new ItemStack(Items.EGG, 1));
            this.addJobTask(new JobTaskProduceItem(
                    this,
                    1800L, // 1.5分钟/份
                    Items.PUMPKIN_PIE,
                    pieMaterials,
                    new TextComponentTranslation("container.sim.job.Baker_Baking_pie", i + 1, pumpkinPieCount).getUnformattedText()
            ));
        }

        // 4. 制作曲奇（每份需1可可豆+2小麦）
        int cookieCount = calculateMaxBatch(
                new int[]{dye, wheat},
                new int[]{1, 2}
        );
        for (int i = 0; i < cookieCount; i++) {
            List<ItemStack> cookieMaterials = new CopyOnWriteArrayList<>();
            cookieMaterials.add(new ItemStack(Items.DYE, 1, 3));
            cookieMaterials.add(new ItemStack(Items.WHEAT, 2));
            this.addJobTask(new JobTaskProduceItem(
                    this,
                    600L, // 0.5分钟/份（曲奇制作较快）
                    Items.COOKIE,
                    cookieMaterials,
                    new TextComponentTranslation("container.sim.job.Baker_Baking_cookie", i + 1, cookieCount).getUnformattedText()
            ));
        }

        ModSimLoader.log.info("面包师批量制作任务生成完成：面包x" + breadCount + "，蛋糕x" + cakeCount + "，南瓜派x" + pumpkinPieCount + "，曲奇x" + cookieCount);
    }

    /**
     * 计算单个原料的最大可制作份数（如：10小麦做3份面包，每份需3小麦）
     */
    private int calculateMaxBatch(int materialCount, int perNeed) {
        if (perNeed <= 0) return 0;
        int max = materialCount / perNeed;
        return Math.min(max, MAX_BATCH_COUNT); // 不超过单次最大份数
    }

    /**
     * 计算多个原料的最大可制作份数（取原料中限制最严的那个）
     * 如：牛奶3、糖2、鸡蛋1 → 蛋糕需3牛奶+2糖+1鸡蛋 → 最多做1份
     */
    private int calculateMaxBatch(int[] materialCounts, int[] perNeeds) {
        if (materialCounts.length != perNeeds.length) return 0;
        int minBatch = Integer.MAX_VALUE;
        for (int i = 0; i < materialCounts.length; i++) {
            int batch = materialCounts[i] / perNeeds[i];
            minBatch = Math.min(minBatch, batch);
        }
        return Math.max(0, Math.min(minBatch, MAX_BATCH_COUNT));
    }

    /**
     * 检查是否有足够原料继续制作
     */
    private boolean hasEnoughMaterials() {
        return wheat >= 3 || (milk_bucket >= 3 && sugar >= 2 && egg >= 1) || (pumpkin >= 1 && sugar >= 1 && egg >= 1) || (dye >= 1 && wheat >= 2);
    }

    /** P9: 六种原料任一低于阈值就补货。 */
    private boolean needMoreIngredients() {
        return this.wheat < RESTOCK_MIN || this.egg < RESTOCK_MIN || this.pumpkin < RESTOCK_MIN
                || this.milk_bucket < RESTOCK_MIN || this.sugar < RESTOCK_MIN || this.dye < RESTOCK_MIN;
    }

    @Override
    public void onArrive() {
    }

    @Override
    public String toString() {
        return new TextComponentTranslation("container.sim.Vocation6",new Object[0]).getUnformattedText();
    }
}
