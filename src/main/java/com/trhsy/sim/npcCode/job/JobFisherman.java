package com.trhsy.sim.npcCode.job;

import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.NpcData;
import com.trhsy.sim.npcCode.task.JobTask;
import com.trhsy.sim.npcCode.task.JobTaskIdle;
import com.trhsy.sim.npcCode.task.JobTaskPlaceInChest;
import com.trhsy.sim.npcCode.task.JobTaskShopkeep;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import java.util.Iterator;
import java.util.Random;

/**
 * 渔夫（P4 效率重做）。
 *
 * <p>原实现的问题：
 * <ul>
 *   <li>每次"下网"是 60 秒空等；收网还要再等 10 秒；</li>
 *   <li>一轮只有 5 次下网，跑完就进入"卖鱼"任务直到傍晚，剩下大半天不生产；</li>
 *   <li>结果约 10 条鱼 / 游戏日。</li>
 * </ul>
 * 现在：白天不断循环作业（下网 20 秒 → 收网 2 秒，五种鱼各一网，每网 2-4 条），
 * 傍晚（≥11000）才收摊卖鱼；队列空时重建下一轮，产出约 60-75 条 / 游戏日。
 */
public class JobFisherman extends Job {
    /**
     * @Author fan
     * @Description //TODO 是否靠近水
     * @Date 20:51 2023/4/9
     * @Param
     * @return
     **/
    boolean nearWater;
    public BlockPos pos;
    public World world;
    //工作阶段
    public int fishermanStage = 0;
    /**
     * 自上一个块位置的时间
     **/
    private transient long timeSinceLastBlockPlace = 0L;
    public JobFisherman(NpcData folk, BlockPos pos, World world) {
        super(folk, pos, world);
        try {
//手持鱼竿
            folk.holding = new ItemStack(Items.FISHING_ROD);
            //渔夫
            this.jobName = new TextComponentTranslation("container.sim.Vocation18",new Object[0]).getUnformattedText();
            this.pos=pos;
            this.world=world;
            findWater();
        }catch (Exception e){
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("JobFisherman出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }

    }
    @Override
    public void onArrive() {

    }
    public void findWater(){
        this.nearWater = false;
        try {
            //寻找附近水
            // P16: 旧实现只扫描工作点下方第 3 格的一层，工业图纸“渔场”的水面在下方第 2 格，
            // 会导致 nearWater 永远为 false、渔夫到岗后不作业。改为向下 3 格的体积扫描。
            BlockPos corner1 = this.pos.north(5).east(5).down(3);
            BlockPos corner2 =  this.pos.south(5).west(5);

            Iterator var6 = BlockPos.getAllInBox(corner1, corner2).iterator();
            //找到水
            while (var6.hasNext()) {
                BlockPos p = (BlockPos) var6.next();
                if(this.folk!=null&&this.folk.entity!=null){
                    Block b= this.folk.entity.world.getBlockState(p).getBlock();
                    if ( b== Blocks.WATER ||  this.folk.entity.world.getBlockState(p).getBlock() == Blocks.FLOWING_WATER) {
                        this.nearWater = true;
                        break;
                    }
                }
            }
        }catch (Exception e){
            ModSimLoader.log.error("JobFisherman-findWater寻找水出错了",e.getMessage());
        }
    }

    /**
     * P4：队列空时构建下一轮；白天持续作业，傍晚卖鱼。
     */
    @Override
    public void onUpdate() {
        super.onUpdate();
        try {
            if (!this.atWork) {
                return;
            }
            // 有任务在跑/排队时不推进（否则任务会每 tick 堆积）
            if (this.currentTask != null || !this.jobTasks.isEmpty()) {
                return;
            }
            if (!this.nearWater) {
                long now = System.currentTimeMillis();
                if (now - this.timeSinceLastBlockPlace > 3000L) {
                    this.timeSinceLastBlockPlace = now;
                    findWater();
                }
                return;
            }
            String fishing = new TextComponentTranslation("container.sim.FISHING",new Object[0]).getUnformattedText();
            long worldTime = this.jobWorld.getWorldTime() % 24000L;
            if (worldTime >= 11000L) {
                // 傍晚：售卖/关店（有限时长，跑完进入第二天）
                this.addJobTask(new JobTaskShopkeep(this, 30000L,
                        new TextComponentTranslation("container.sim.job.fisherman.farmer.fish",new Object[0]).getUnformattedText(), true));
            } else {
                // 白天：连续作业，一轮 = 五种鱼各一网
                this.folk.stayPut = true;
                this.addJobTask(new JobTaskIdle(this, 200L, new TextComponentTranslation("container.sim.job.builder_Arrived",new Object[0]).getUnformattedText()));
                this.addJobTask(new JobTaskIdle(this, 20000L, fishing));
                this.addJobTask(new JobTaskPlaceInChest(this, 4000L, new ItemStack(Items.FISH, new Random().nextInt(3) + 2, 0)));
                this.addJobTask(new JobTaskIdle(this, 20000L, fishing));
                this.addJobTask(new JobTaskPlaceInChest(this, 4000L, new ItemStack(Items.FISH, new Random().nextInt(3) + 2, 1)));
                this.addJobTask(new JobTaskIdle(this, 20000L, fishing));
                this.addJobTask(new JobTaskPlaceInChest(this, 4000L, new ItemStack(Items.FISH, new Random().nextInt(3) + 2, 2)));
                this.addJobTask(new JobTaskIdle(this, 20000L, fishing));
                this.addJobTask(new JobTaskPlaceInChest(this, 4000L, new ItemStack(Items.FISH, new Random().nextInt(3) + 2, 3)));
                this.addJobTask(new JobTaskIdle(this, 20000L, fishing));
                this.addJobTask(new JobTaskPlaceInChest(this, 4000L, new ItemStack(Items.FISH, new Random().nextInt(3) + 2)));
                this.fishermanStage = 0;
                this.stage = 0;
            }
            if (!this.jobTasks.isEmpty()) {
                this.currentTask = (JobTask) this.jobTasks.get(0);
                this.currentTask.begin();
            }
        }catch (Exception e){
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("JobFisherman-onUpdate出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    @Override
    public String toString() {
        return new TextComponentTranslation("container.sim.Vocation18",new Object[0]).getUnformattedText();
    }
}
