package com.trhsy.sim.npcCode.task;

import com.trhsy.sim.block.BlockConstructorBox;
import com.trhsy.sim.loader.*;
import com.trhsy.sim.network.client.PacketSendBuildingRequirements;
import com.trhsy.sim.npcCode.job.Job;
import com.trhsy.sim.npcCode.job.JobBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.TextComponentTranslation;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.npcCode.task
 * @ClassName: JobTaskBuild
 * @Description: 建筑工工作
 * @date 2024/3/12 13:59
 */
public class JobTaskBuilder extends JobTask {
    public JobBuilder jobBuilder;
    public String status;

    public JobTaskBuilder(Job j, long ms, String status) {
        super(j, ms);
        this.jobBuilder = (JobBuilder) j;
        this.status = status;
    }

    @Override
    public void onTaskBegin() {
        this.job.folk.setStatus(this.status);
        //设置固定不动
        this.folk.stayPut = true;
    }

    @Override
    public void onUpdate() {
        try {
            if (folk != null) {
                if (folk.entity != null) {
                    if (folk.entity.world != null) {
                        if (!folk.entity.world.isRemote) {
                            //NPC数据为空，并且没有指派元
                            if (!this.jobBuilder.hasReassignedEmployee) {
                                //建造位置为空
                                IBlockState iBlockState = folk.entity.world.getBlockState(this.jobBuilder.constructorPos);
                                if (iBlockState == null) {
                                    return;
                                }
                                Block block = iBlockState.getBlock();
                                if (block == Blocks.AIR) {
                                    return;
                                }
                                if (block == BlockLoader.blockConstructorBox) {
                                    //获取建筑箱的
                                    BlockConstructorBox cons = (BlockConstructorBox) block;
                                    //当前建筑箱的工作人员是
                                    cons.employee = folk;
                                    //已经
                                    this.jobBuilder.hasReassignedEmployee = true;
                                    //如果允许 NPC 说话
                                    if (ConfigLoader.configFolkTalking) {
                                        //World world = FMLClientHandler.instance().getServer().getEntityWorld();
                                        //播放 我准备好了
                                        SoundEvent soundEvent = null;
                                        //判断性别，发出不一样的声音
                                        if (folk.gender == 0) {
                                            soundEvent = SoundRegistry.IM_READ_M;
                                        } else {
                                            soundEvent = SoundRegistry.IM_READ_Y;
                                        }
                                        if (soundEvent == null || soundEvent.getRegistryName() == null) {
                                            ModSimLoader.log.error("播放失败：sim:IM_READ_M 声音事件未注册");
                                        } else {
                                        this.job.jobWorld.playSound(null,folk.entity.posX, folk.entity.posY, folk.entity.posZ, soundEvent, SoundCategory.BLOCKS, 1.0F, 1.0F);}
                                    }
                                }
                            }
                            //建筑蓝图为空
                            if (this.jobBuilder.blueprint == null) {
                                //等待蓝图
                                folk.setStatus(new TextComponentTranslation("container.sim.job.builder_Awaiting_blueprint", new Object[0]).getUnformattedText());
                            } else {

                                //随机
//                            new Random();
                                //当前时间
                                long now = System.currentTimeMillis();
                                //游戏模式是正常模式
                                if (ModSimLoader.gamemode != 1) {
                                    // P12(S2): 技能 10 时旧公式 1000 - 100*skill = 0，会每 tick 放一块。
                                    long placeInterval = Math.max(200L, 1000L - (long) (80L * Math.min(10.0F, folk.skillBuilding)));
                                    if ((float) (now - this.jobBuilder.timeSinceLastBlockPlace) > placeInterval) {
                                        ////上次时间为当前时间
                                        this.jobBuilder.timeSinceLastBlockPlace = now;
                                        //放置方块
                                        this.jobBuilder.placeBlock();
                                        //发送建筑蓝图
                                        if (this.jobBuilder.conBox == null) {
                                            this.jobBuilder.createConBox();
                                        }
                                        // P12(S2): 每 20 块同步一次需求，避免每块都广播整个蓝图。
                                        if (this.jobBuilder.conBox != null && this.jobBuilder.blockNumber % 20 == 0) {
                                            NetWorkLoader.net.sendToAll(new PacketSendBuildingRequirements(this.jobBuilder.blueprint, this.jobBuilder.conBox.getUniqueID()));
                                        }

                                    }
                                } else {
                                    //上次时间为当前时间
                                    this.jobBuilder.timeSinceLastBlockPlace = now;
                                    //不是客户端
                                    if (!folk.entity.world.isRemote) {
                                        //直接放置方块
                                        this.jobBuilder.placeBlock();
                                    }
                                }

                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("JobBuilder-onUpdate出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    @Override
    public void onTaskComplete() {

    }
}
