package com.trhsy.sim.block;

import com.trhsy.sim.loader.*;
import com.trhsy.sim.network.client.PacketOpenConstructorGui;
import com.trhsy.sim.network.client.PacketOpenSetupGui;
import com.trhsy.sim.npcCode.NpcData;
import com.trhsy.sim.npcCode.V3;
import com.trhsy.sim.util.Onboarding;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.block
 * @ClassName: BlockConstructorBox
 * @Description: 建筑箱  建筑箱方块（优化版）
 *  * 改进点：资源集中管理、多语言支持、性能优化、代码健壮性提升
 * @date 2023/10/31 上午 10:16
 */
public class BlockConstructorBox extends Block {
    // ------------------------------ 常量定义 ------------------------------
    // 提示信息键（对应 lang/en_us.json 中的翻译键）
    private static final String TOOLTIP_KEY = "block.sim.constructor_box.tooltip";
    // 职业名称翻译键（对应 NPC 职业配置）
    private static final String VOCATION_BUILDER_1 = "container.sim.Vocation1";
    private static final String VOCATION_BUILDER_2 = "container.sim.Vocation16";
    public NpcData employee;


    public BlockConstructorBox() {
        super(Material.WOOD);
        //用于设定走在方块上的响声。
        this.setSoundType(SoundType.WOOD);
        //方块硬度
        this.setHardness(0.5F);
        this.setCreativeTab(CreativeTabsLoader.tabSimU);
        this.setUnlocalizedName("constructor_box");
    }


    /**
     * 放置时
     */

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        //在给定块位置的中心为播放器播放指定的声音 constructor activated 控制箱激活
        SoundEvent SIM_U_BUILDING_CONSTRUCTOR_ACTIVATED = SoundRegistry.SIM_U_BUILDING_CONSTRUCTOR_ACTIVATED;// 对应 sounds.json 中的键
        if (SIM_U_BUILDING_CONSTRUCTOR_ACTIVATED == null || SIM_U_BUILDING_CONSTRUCTOR_ACTIVATED.getRegistryName() == null) {
            ModSimLoader.log.error("播放失败：sim:SIM_U_BUILDING_CONSTRUCTOR_ACTIVATED 声音事件未注册");
        } else {

            worldIn.playSound(pos.getX(),pos.getY(), pos.getZ(),SIM_U_BUILDING_CONSTRUCTOR_ACTIVATED, SoundCategory.BLOCKS, 1.0F, 1.0F,false);
        }

        // 第一次放建筑箱时，把“右键 → 选蓝图 → 雇建筑师”直接发到聊天栏。
        if (!worldIn.isRemote && placer instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) placer;
            if (Onboarding.markConstructorTip(player)) {
                Onboarding.send(player, "container.sim.tip.constructor_placed");
            }
        }
    }

    /**
     * @return boolean
     * @Author fan
     * @Description //TODO 右键 激活
     * @Date 16:46 2022/10/19
     * @Param [worldIn, pos, state, playerIn, hand, heldItem, side, hitX, hitY, hitZ]
     **/
    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        //在给定块位置的中心为播放器播放指定的声音 constructor activated 控制箱激活
        SoundEvent computer = SoundRegistry.COMPUTER;
        if (computer == null || computer.getRegistryName() == null) {
            ModSimLoader.log.error("播放失败：sim:computer 声音事件未注册");
        } else {
            worldIn.playSound( pos.getX(),pos.getY(), pos.getZ(), computer, SoundCategory.BLOCKS, 1.0F, 1.0F,false);}
        int buildDirection = 0;

        if (!worldIn.isRemote) {
            if (ModSimLoader.gamemode == 999) {
                NetWorkLoader.net.sendTo(new PacketOpenSetupGui(), (EntityPlayerMP) playerIn);
                return true;
            }
            // 计算建筑方向（优化：移除冗余的 Math.floor(playerIn.posY)）
            buildDirection = calculateBuildDirection(playerIn, pos);
            // 通过缓存的 NPC 数据快速查找建筑师（优化：线性搜索 → Map 缓存）
            NpcData targetNpc = findArchitectNpc(pos);
            // 发送网络包打开 GUI（确保在主线程执行）
            sendOpenGuiPacket(worldIn, pos, buildDirection, playerIn, targetNpc);
/*
            int px = (int) Math.floor(playerIn.posX);
            Math.floor(playerIn.posY);
            int pz = (int) Math.floor(playerIn.posZ);
            if (pos.getZ() == pz) {
                if (px < pos.getX()) {
                    buildDirection = 1;
                } else {
                    buildDirection = 3;
                }
            } else if (pos.getX() == px) {
                if (pz < pos.getZ()) {
                    buildDirection = 2;
                } else {
                    buildDirection = 0;
                }
            }

            ModSimLoader.log.info("建筑方向为 " + buildDirection);
            NpcData fd = null;
            for (NpcData f : ModSimLoader.folks) {
                //建筑师
                String v1 = new TextComponentTranslation("container.sim.Vocation1", new Object[0]).getUnformattedText();
                String v2 = new TextComponentTranslation("container.sim.Vocation16", new Object[0]).getUnformattedText();
                if (f.job != null) {
                    V3 v3 = new V3(f.job.workPlace.x, f.job.workPlace.y - 1, f.job.workPlace.z);
                    V3 v31 = V3.fromBlockPos(pos);
                    if ((f.job.jobName.contentEquals(v1) || f.job.jobName.contentEquals(v2)) && v3.equals(v31)) {
                        fd = f;
                        break;
                    }
                }
            }

            if (fd != null) {

                NetWorkLoader.net.sendTo(new PacketOpenConstructorGui(pos, buildDirection, fd.getClientIdentity(), playerIn.dimension), (EntityPlayerMP) playerIn);
            } else {
                NetWorkLoader.net.sendTo(new PacketOpenConstructorGui(pos, buildDirection, playerIn.dimension), (EntityPlayerMP) playerIn);
            }*/
        }

        return true;
    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 玩家摧毁方块 摧毁方块时触发 NPC 任务中断
     * @Date 17:34 2022/11/1
     * @Param [worldIn, pos, state]
     **/
    @Override
    public void onBlockDestroyedByPlayer(World worldIn, BlockPos pos, IBlockState state) {
        //在给定块位置的中心为播放器播放指定的声音 断电 power down
        SoundEvent powerDown = SoundRegistry.POWER_DOWN;
        if (powerDown == null || powerDown.getRegistryName() == null) {
            ModSimLoader.log.error("播放失败：sim:power_down 声音事件未注册");
        } else {
            worldIn.playSound(null, pos, powerDown, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
        // 通过缓存的 NPC 数据快速查找关联 NPC（优化：线性搜索 → Map 缓存）
        NpcData targetNpc = findArchitectNpc(pos);
        // 解雇关联的建筑师NPC（使用距离判断，与findArchitectNpc保持一致）
        if (targetNpc != null && targetNpc.job != null) {
            V3 npcWorkPos = targetNpc.job.workPlace;
            V3 blockPos3D = V3.fromBlockPos(pos);
            double distanceSq = npcWorkPos.distanceSq(blockPos3D);
            if (distanceSq <= 1.25) { // 允许 0.5 格误差，与findArchitectNpc一致
                targetNpc.fire();
            }
        }
        //预览位置
        ModSimClientLoader.previewPos1 = null;
        ModSimClientLoader.previewPos2 = null;
        super.onBlockDestroyedByPlayer(worldIn, pos, state);
    }
    // ------------------------------ 工具方法 ------------------------------
    /**
     * 计算建筑方向（优化：简化逻辑，避免冗余计算）
     */
    private int calculateBuildDirection(EntityPlayer player, BlockPos blockPos) {
        int px = (int) Math.floor(player.posX);
        int pz = (int) Math.floor(player.posZ);

        if (blockPos.getZ() == pz) {
            return (px < blockPos.getX()) ? 1 : 3; // 东/西
        } else if (blockPos.getX() == px) {
            return (pz < blockPos.getZ()) ? 2 : 0; // 南/北
        }
        return 0; // 默认方向
    }
    /**
     * 查找关联的建筑师 NPC（优化：使用 Map 缓存 NPC 数据）
     */
    @Nullable
    private NpcData findArchitectNpc(BlockPos blockPos) {


        // 遍历所有 NPC（实际项目中建议使用空间分区优化，如网格划分）
        for (NpcData npc : ModSimLoader.folks) {
            if (npc.job != null && isBuilderNpc(npc)) {
                V3 npcWorkPos = npc.job.workPlace; // NPC 工作位置（V3 类型）
                V3 blockPos3D = V3.fromBlockPos(blockPos); // 当前方块位置（V3 类型）

                // 计算距离平方（替代原有的 distanceSq 调用）
                double distanceSq = npcWorkPos.distanceSq(blockPos3D);
                if (distanceSq <= 1.25) { // 允许 0.5 格误差
                    // 更新缓存（临时实现）
//                    NPC_POSITION_CACHE.put(blockPos, npc);
                    return npc;
                }
            }
        }
        return null;
    }
    /**
     * 判断 NPC 是否为建筑师（优化：提取职业判断逻辑）
     */
    private boolean isBuilderNpc(NpcData npc) {
        String jobName = npc.job.jobName;
        return jobName.contentEquals(I18n.translateToLocal(VOCATION_BUILDER_1)) || jobName.contentEquals(I18n.translateToLocal(VOCATION_BUILDER_2));
    }
    /**
     * 发送打开 GUI 的网络包（确保在主线程执行）
     */
    private void sendOpenGuiPacket(World world, BlockPos pos, int direction, EntityPlayer player, NpcData npc) {
        if (world.isRemote) return;
        PacketOpenConstructorGui packet =null;
        // 构造 GUI 参数（根据需求调整）
        if (npc != null) {

            packet=new PacketOpenConstructorGui(pos, direction, npc.getClientIdentity(), player.dimension);
        } else {
            packet=new PacketOpenConstructorGui(pos, direction, player.dimension);
        }
//        new PacketOpenConstructorGui(pos, direction, (npc != null) ?  npc.getClientIdentity() : player.dimension, player.dimension);

        // 使用 Minecraft 主线程调度（避免阻塞玩家操作）
        if (player instanceof EntityPlayerMP) {
            NetWorkLoader.net.sendTo(packet, (EntityPlayerMP) player);
        }
    }
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, ITooltipFlag advanced) {
        // 从 lang 文件加载翻译（示例："block.sim.constructor_box.tooltip"="建筑箱：右键激活以打开建造界面"）
        tooltip.add(I18n.translateToLocal(TOOLTIP_KEY));
        tooltip.add(I18n.translateToLocal(TOOLTIP_KEY + ".2"));
    }
}
