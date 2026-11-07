package com.trhsy.sim.npcCode.task;

import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.V3;
import com.trhsy.sim.npcCode.job.Job;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @ClassName JobTaskHarvestAnimal
 * @Description todo 收获任务
 * @Author TRHSY
 * @Date 2023/4/518:35
 **/
public class JobTaskHarvestAnimal extends JobTask {
    /**
     * @Author fan
     * @Description //TODO 牲畜名称
     * @Date 18:46 2023/4/5
     * @Param 
     * @return 
     **/
    public String livestockName;
    /**
     * @Author fan
     * @Description //TODO 牲畜
     * @Date 18:46 2023/4/5
     * @Param 
     * @return 
     **/
    public List<EntityAnimal> livestock = new ArrayList();
    /**
     * @Author fan
     * @Description //TODO 生成的物品
     * @Date 18:46 2023/4/5
     * @Param 
     * @return 
     **/
    public ItemStack produce;
    /**
     * @Author fan
     * @Description //TODO 随机数
     * @Date 18:47 2023/4/5
     * @Param 
     * @return 
     **/
    public boolean randAmount;
    /**
     * @Author fan
     * @Description //TODO 当前有几个牲畜
     * @Date 18:47 2023/4/5
     * @Param 
     * @return 
     **/
    int currentLivestock = 0;
    /**
     * @Author fan
     * @Description //TODO 状态
     * @Date 18:47 2023/4/5
     * @Param 
     * @return 
     **/
    String status;
    public Class livestockClass;
    /**
     * @Author fan
     * @Description //TODO 
     * @Date 18:39 2023/4/5 工作，时间，牲畜名字，物品，随机数量，状态
     * @Param [j, ms, livestockName, produce, randomAmount, status]
     * @return 
     **/
    public JobTaskHarvestAnimal(Job j, long ms, String livestockName, ItemStack produce, boolean randomAmount, String status) {
        super(j, ms);
        this.livestockName = livestockName;
        this.produce = produce;
        this.randAmount = randomAmount;
        this.status = status;
    }

    @Override
    public void onTaskBegin() {
        this.job.folk.setStatus(this.status);
        //设置固定不动
        this.folk.stayPut = true;
        this.livestock = new ArrayList();
        this.currentLivestock = 0;
        this.findTargets();
    }

    @Override
    public void onUpdate() {
        if (this.livestock.size() < 1) {
            //找到羊
            this.findTargets();
        } else if (this.currentLivestock >= this.livestock.size()) {
            this.completeTask();
        } else {
           V3 v3= new V3((this.livestock.get(this.currentLivestock)).getPosition(),this.job.folk.entity.dimension);
            this.folk.forceMoveToXYZ(v3);
            if (new V3((this.livestock.get(this.currentLivestock)).getPosition(),this.job.folk.entity.dimension).getDistanceTo(this.job.folk.getV3()) < 2) {
                this.harvestTarget((EntityAnimal)this.livestock.get(this.currentLivestock));
                ++this.currentLivestock;
            }

        }
    }

    @Override
    public void onTaskComplete() {

    }
    public void findTargets() {

        if (this.livestockName.equals(new TextComponentTranslation("container.sim.job_Livestock_cow",new Object[0]).getUnformattedText())) {
            this.livestockClass = EntityCow.class;
            //羊
        } else if (this.livestockName.equals(new TextComponentTranslation("container.sim.job_Livestock_sheep",new Object[0]).getUnformattedText())) {
            this.livestockClass = EntitySheep.class;
            //鸡
        }
        this.livestock = this.getAnimalsInPen(this.job.workPlace, this.livestockClass);
        List<EntityAnimal> grownAnimals = new ArrayList();
        if (this.livestock.size() >= 1) {
            for(int i = 0; i < this.livestock.size(); ++i) {
                if (!((EntityAnimal)this.livestock.get(i)).isChild()) {
                    grownAnimals.add(this.livestock.get(i));
                }
            }

            this.livestock = grownAnimals;
        }
    }

    public void harvestTarget(EntityAnimal entity) {
        if (this.randAmount) {
            this.produce.setCount((new Random()).nextInt(this.produce.getCount() + 1));
            if (entity instanceof EntitySheep) {
                EntitySheep sheep = (EntitySheep)entity;
                //剪刀剪过的样子
                sheep.setSheared(true);
            }
        }

        // P4: 按实际放进去的件数扣钱（箱子满时不吞货也不扣钱）。
        int put = this.job.placeInJobChestPartial(this.produce);
        if (put > 0 && ModSimLoader.gamemode != 1) {
            ModSimLoader.addMoney(-0.02F * (float) put);
        }
    }
    public List<EntityAnimal> getAnimalsInPen(V3 controlBox, Class animal) {
        List<EntityAnimal> list = this.folk.entity.world.getEntitiesWithinAABB(animal, new AxisAlignedBB(controlBox.x - 5.0D, controlBox.y, controlBox.z - 5.0D, controlBox.x + 5.0D, controlBox.y + 2.0D, controlBox.z + 5.0D));
        return list;
    }
}
