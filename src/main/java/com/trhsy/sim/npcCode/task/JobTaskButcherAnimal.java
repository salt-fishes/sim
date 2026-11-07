package com.trhsy.sim.npcCode.task;

import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.V3;
import com.trhsy.sim.npcCode.job.Job;
import com.trhsy.sim.npcCode.job.JobLivestockFarmer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.*;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.npc.task
 * @ClassName: JobTaskButcherAnimal
 * @Description: 屠夫动物
 * @date 2023/3/21 16:22
 */
public class JobTaskButcherAnimal extends JobTask {
    /**
     * 任务
     */
    public JobLivestockFarmer farmJob = null;
    public Class livestockClass;
    /**
     * 屠戮目标
     */
    public Entity butcherTarget = null;
    public JobTaskButcherAnimal(Job j, long ms) {
        super(j, ms);
        this.farmJob = (JobLivestockFarmer)j;
    }

    @Override
    public void onTaskBegin() {
        //屠宰动物
        this.job.folk.setStatus(new TextComponentTranslation("container.sim.job_task_Butcher1",new Object[0]).getUnformattedText());
        this.selectTarget();
        //设置固定不动
        this.folk.stayPut = true;
    }

    @Override
    public void onUpdate() {
        //如果屠戮目标为空则下一个
        if (this.butcherTarget == null) {
            this.selectTarget();
            if (this.butcherTarget == null) {
                // P3: 数量已达下限（成年动物 <= 4），本轮不再屠宰，结束任务
                this.folk.setStatus(new TextComponentTranslation("container.sim.job.crop.farmer.Tending1",new Object[0]).getUnformattedText() + " " + this.farmJob.livestockName);
                this.completeTask();
            }
        } else {
            V3 v3=new V3(this.butcherTarget.getPosition(),this.farmJob.folk.entity.dimension);
            this.folk.forceMoveToXYZ(v3);
            if (new V3(this.butcherTarget.getPosition(),this.farmJob.folk.entity.dimension).getDistanceTo(this.farmJob.folk.getV3()) < 2) {
                this.killTarget();
            }

        }
    }
    public void selectTarget() {
        // 1. 确定目标牲畜类型（保持不变）
        if (this.farmJob.livestockName.equals(new TextComponentTranslation("container.sim.job_Livestock_pig",new Object[0]).getUnformattedText())) {
            this.livestockClass = EntityPig.class;
            //牛
        } else if (this.farmJob.livestockName.equals(new TextComponentTranslation("container.sim.job_Livestock_cow",new Object[0]).getUnformattedText())) {
            this.livestockClass = EntityCow.class;
            //羊
        } else if (this.farmJob.livestockName.equals(new TextComponentTranslation("container.sim.job_Livestock_sheep",new Object[0]).getUnformattedText())) {
            this.livestockClass = EntitySheep.class;
            //鸡
        } else if (this.farmJob.livestockName.equals(new TextComponentTranslation("container.sim.job_Livestock_chicken",new Object[0]).getUnformattedText())) {
            this.livestockClass = EntityChicken.class;
            //兔子
        }else if (this.farmJob.livestockName.equals(new TextComponentTranslation("container.sim.job_Livestock_rabbit",new Object[0]).getUnformattedText())) {
            this.livestockClass = EntityRabbit.class;
        }
        // 2. 获取围栏内的动物列表
        List<EntityAnimal> farmAnimals = this.getAnimalsInPen(this.farmJob.workPlace, this.livestockClass);
        List<EntityAnimal> grownAnimals = new ArrayList();
        // 3. 筛选成年动物
        for (EntityAnimal animal : farmAnimals) {
            if (animal != null && !animal.isChild()) { // 增加非空判断
                grownAnimals.add(animal);
            }
        }
        // 4. 安全选择目标（核心修复）
        // P3: 保持 4-6 头：成年动物 <= 4 时不再屠宰，避免把动物杀光
        if (grownAnimals.size() <= 4) {
            this.butcherTarget = null;
            return;
        }
        if (!grownAnimals.isEmpty()) { // 只在有成年动物时执行
            // 使用 grownAnimals 的大小作为随机范围，避免索引越界
            int randomIndex = this.rand.nextInt(grownAnimals.size());
            this.butcherTarget = grownAnimals.get(randomIndex);
        } else {
            // 没有成年动物时的处理（可选）
            this.butcherTarget = null;
            // 可添加日志：ModSimLoader.log.info("没有找到可屠宰的成年动物");
        }
        /*if (farmAnimals.size() >= 1) {
            for(int i = 0; i < farmAnimals.size(); ++i) {
                if (!((EntityAnimal)farmAnimals.get(i)).isChild()) {
                    grownAnimals.add(farmAnimals.get(i));
                }
            }

            this.butcherTarget = (Entity)grownAnimals.get(this.rand.nextInt(farmAnimals.size()-1));
        }*/
    }

    /**
     * 选择实体动物
     * @param controlBox
     * @param animal
     * @return
     */
    public List<EntityAnimal> getAnimalsInPen(V3 controlBox, Class animal) {
        List<EntityAnimal> list=new CopyOnWriteArrayList<EntityAnimal>();
        try {
            list = this.job.jobWorld.getEntitiesWithinAABB(animal, (new AxisAlignedBB(controlBox.x-4, controlBox.y, controlBox.z-4, controlBox.x+4 , controlBox.y +2, controlBox.z + 4)));
            //list = this.job.jobWorld.getEntitiesWithinAABB(animal, (new AxisAlignedBB(controlBox.x-5.0D, controlBox.y, controlBox.z-5.0D, controlBox.x + 5.0D, controlBox.y + 2.0D, controlBox.z + 5.0D)));
        }catch (Exception e){
            ModSimLoader.log.error("getAnimalsInPen出错了：" + e.getMessage() );
        }
        return list;
    }

    /**
     * 杀死目标
     */
    public void killTarget() {
        ModSimLoader.log.info("尝试杀死 " + this.butcherTarget.getName() + ",实体ID： " + this.butcherTarget.getEntityId());
        //目标体生物
        this.butcherTarget.setDropItemsWhenDead(false);
        this.butcherTarget.setDead();
        this.butcherTarget.dropItem(Items.AIR,0);

        //通用的
//        this.butcherTarget.attackEntityFrom(DamageSource.GENERIC, 500.0F);
        //牛
        if (this.butcherTarget instanceof EntityCow) {
            //牛肉
            int q=new Random().nextInt(4) + 1;
            this.farmJob.placeInJobChest(new ItemStack(Items.BEEF, q));
            ModSimLoader.addMoney(-0.02F * (float)q);
            //皮革
            if (new Random().nextInt(5) > 2) {
                this.farmJob.placeInJobChest(new ItemStack(Items.LEATHER, new Random().nextInt(2) + 1));
            }
            //鸡
        } else if (this.butcherTarget instanceof EntityChicken) {
            int q=new Random().nextInt(2) + 1;
            //鸡肉
            this.farmJob.placeInJobChest(new ItemStack(Items.CHICKEN, q));
            ModSimLoader.addMoney(-0.02F*q);
            if (new Random().nextInt(5) > 2) {
                //羽毛
                this.farmJob.placeInJobChest(new ItemStack(Items.FEATHER, q));
            }
            //猪
        } else if (this.butcherTarget instanceof EntityPig) {
            //猪排
            int q=new Random().nextInt(4) + 1;
            this.farmJob.placeInJobChest(new ItemStack(Items.PORKCHOP, q));
            ModSimLoader.addMoney(-0.02F * (float)q);
            //羊
        } else if (this.butcherTarget instanceof EntitySheep) {
            int q=new Random().nextInt(4) + 1;
            //羊肉
            this.farmJob.placeInJobChest(new ItemStack(Items.MUTTON, q));
            //羊毛
            this.farmJob.placeInJobChest(new ItemStack(Blocks.WOOL, new Random().nextInt(2) + 1));
            ModSimLoader.addMoney(-0.02F * (float)q);
            //兔子
        }else if (this.butcherTarget instanceof EntityRabbit) {
            int q=new Random().nextInt(4) + 1;
            //兔肉
            this.farmJob.placeInJobChest(new ItemStack(Items.RABBIT, q));
            //兔脚
            this.farmJob.placeInJobChest(new ItemStack(Items.RABBIT_FOOT, new Random().nextInt(2) + 1));
            //兔皮
            this.farmJob.placeInJobChest(new ItemStack(Items.RABBIT_HIDE, new Random().nextInt(2) + 1));
            ModSimLoader.addMoney(-0.02F * (float)q);
        }

        this.completeTask();
    }
    @Override
    public void onTaskComplete() {

    }
}
