package com.trhsy.sim.npcCode.task;

import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.V3;
import com.trhsy.sim.npcCode.job.Job;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.passive.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.npc.task
 * @ClassName: JobTaskSpawnLivestock
 * @Description: 生成畜牧
 * @date 2023/3/21 15:57
 */
public class JobTaskSpawnLivestock extends JobTask {

    public String livestockName;
    public  Class livestockClass;

    public JobTaskSpawnLivestock(Job job, String livestockName, Class livestockClass, long ms) {
        super(job, ms);
        this.livestockName = livestockName;
        this.livestockClass = livestockClass;
    }

    /**
     * 开始任务
     */
    @Override
    public void onTaskBegin() {
        this.folk.setStatus(new TextComponentTranslation("container.sim.job_task_Spawn1",new Object[0]).getUnformattedText());
        //设置固定不动
        this.folk.stayPut = true;
        ModSimLoader.log.info("开始繁殖牲畜");
        //开始检测当前地方的实体动物
        int animalCount = this.getAnimalsInPen(this.job.workPlace, this.livestockClass).size();
        ModSimLoader.log.info("检测到 " + animalCount + " 只" + this.livestockName);
        // P3: 保持 4-6 头：少于 5 才补栏到 6；配合屠宰下限（>4 才杀），数量稳定在 4-6。
        if (animalCount < 5) {
            int spawnCount = 6 - animalCount;
            ModSimLoader.log.info("生成 " + spawnCount + " 只" + this.livestockName);
            this.spawnAnimals(this.job.workPlace, this.livestockName, spawnCount);
        }
    }

    /**
     * 实体动物
     * @param controlBox
     * @param animal
     * @return
     */
    public List<EntityAnimal> getAnimalsInPen(V3 controlBox, Class animal) {
        List<EntityAnimal> list=new CopyOnWriteArrayList<EntityAnimal>();
        try {
            list = this.job.jobWorld.getEntitiesWithinAABB(animal, (new AxisAlignedBB(controlBox.x-4, controlBox.y, controlBox.z-4, controlBox.x +4, controlBox.y + 2.0D, controlBox.z + 4)));
//            list = this.job.jobWorld.getEntitiesWithinAABB(animal, new AxisAlignedBB(controlBox.x-5, controlBox.y+1, controlBox.z-5, controlBox.x + 5.0D, controlBox.y + 1.0D, controlBox.z + 5.0D));
        }catch (Exception e){
            ModSimLoader.log.error("getAnimalsInPen出错了：" + e.getMessage() );
        }
        return list;
    }

    private void spawnAnimals(V3 controlBox, String animal, int count) {
        for (int c = 1; c <= count; ++c) {
            EntityAnimal newAnimal = null;
            if (animal.contentEquals(new TextComponentTranslation("container.sim.job_Livestock_pig", new Object[0]).getUnformattedText())) {
                newAnimal = new EntityPig(this.job.jobWorld);
            } else if (animal.contentEquals(new TextComponentTranslation("container.sim.job_Livestock_cow", new Object[0]).getUnformattedText())) {
                newAnimal = new EntityCow(this.job.jobWorld);
            } else if (animal.contentEquals(new TextComponentTranslation("container.sim.job_Livestock_chicken", new Object[0]).getUnformattedText())) {
                newAnimal = new EntityChicken(this.job.jobWorld);
            } else if (animal.contentEquals(new TextComponentTranslation("container.sim.job_Livestock_sheep", new Object[0]).getUnformattedText())) {
                newAnimal = new EntitySheep(this.job.jobWorld);
            } else if (animal.contentEquals(new TextComponentTranslation("container.sim.job_Livestock_rabbit", new Object[0]).getUnformattedText())) {
                newAnimal = new EntityRabbit(this.job.jobWorld);
            }

            if (newAnimal == null) {
                ModSimLoader.log.warn("未知牲畜类型，停止生成: {}", animal);
                return;
            }

            // P11(S1): 不再把所有动物堆在控制箱同一格。
            // 在围栏范围内随机找“两格空气 + 脚下实体方块”的位置，并检查原版生成条件/碰撞/间距。
            boolean spawned = false;
            for (int attempt = 0; attempt < 24; attempt++) {
                int dx = this.job.jobWorld.rand.nextInt(7) - 3;
                int dz = this.job.jobWorld.rand.nextInt(7) - 3;
                int dy = this.job.jobWorld.rand.nextInt(3);
                BlockPos pos = new BlockPos(controlBox.x + dx, controlBox.y + dy, controlBox.z + dz);
                if (!this.job.jobWorld.isBlockLoaded(pos)) {
                    continue;
                }
                if (!this.job.jobWorld.isAirBlock(pos) || !this.job.jobWorld.isAirBlock(pos.up())) {
                    continue;
                }
                IBlockState ground = this.job.jobWorld.getBlockState(pos.down());
                if (!ground.getMaterial().isSolid() || ground.getMaterial().isLiquid()) {
                    continue;
                }
                newAnimal.setLocationAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                        this.job.jobWorld.rand.nextFloat() * 360.0F, 0.0F);
                if (!newAnimal.getCanSpawnHere() || !newAnimal.isNotColliding()) {
                    continue;
                }
                // 至少留 1 格间距，避免 1.12.2 的 maxEntityCramming 把动物挤伤。
                List<EntityAnimal> nearby = this.job.jobWorld.getEntitiesWithinAABB(
                        newAnimal.getClass(), newAnimal.getEntityBoundingBox().grow(0.8D));
                if (!nearby.isEmpty()) {
                    continue;
                }
                if (!this.job.jobWorld.isRemote) {
                    this.job.jobWorld.spawnEntity(newAnimal);
                }
                spawned = true;
                break;
            }
            if (!spawned) {
                ModSimLoader.log.warn("围栏内没有安全生成点，跳过 1 只{}", animal);
            }
        }
    }

    /**
     * 更新任务
     */
    @Override
    public void onUpdate() {

    }

    /**
     * 完成任务
     */
    @Override
    public void onTaskComplete() {

    }
}
