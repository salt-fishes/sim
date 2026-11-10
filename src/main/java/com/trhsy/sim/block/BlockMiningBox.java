package com.trhsy.sim.block;

import com.trhsy.sim.loader.CreativeTabsLoader;
import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.loader.NetWorkLoader;
import com.trhsy.sim.loader.SoundRegistry;
import com.trhsy.sim.network.client.PacketOpenMineGui;
import com.trhsy.sim.npcCode.NpcData;
import com.trhsy.sim.npcCode.V3;
import com.trhsy.sim.npcCode.block.MineBox;
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
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.block
 * @ClassName: BlockMiningBox
 * @Description: 采矿箱
 * @date 2023/11/08 上午 10:50
 */
public class BlockMiningBox extends Block {
    public BlockMiningBox() {
        super(Material.WOOD);
        this.setSoundType(SoundType.WOOD);
        this.setHardness(2.0F);
        this.setResistance(1);
        this.setUnlocalizedName("miningBox");
        this.setCreativeTab(CreativeTabsLoader.tabSimU);
    }
    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        //在给定块位置的中心为播放器播放指定的声音 constructor activated 控制箱激活
//        SoundEvent soundEvent = new SoundEvent(new ResourceLocation(ModSim.MODID + ":sim_u_kraft_ddd_mining_constructor_activated"));
        SoundEvent sim_u_kraft_ddd_mining_constructor_activated = SoundRegistry.SIM_U_KRAFT_DDD_MINING_CONSTRUCTOR_ACTIVATED;
        if (sim_u_kraft_ddd_mining_constructor_activated == null || sim_u_kraft_ddd_mining_constructor_activated.getRegistryName() == null) {
            ModSimLoader.log.error("播放失败：sim:sim_u_kraft_ddd_mining_constructor_activated 声音事件未注册");
        } else {
            world.playSound(null, pos, sim_u_kraft_ddd_mining_constructor_activated, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
        if (!world.isRemote) {
            V3 markerPos = null;
            EnumFacing facing = EnumFacing.EAST;
            int px = (int)Math.floor(placer.posX);
            Math.floor(placer.posY);
            int pz = (int)Math.floor(placer.posZ);
            if (pos.getZ() == pz) {
                if (px < pos.getX()) {
                    facing = EnumFacing.EAST;
                } else {
                    facing = EnumFacing.WEST;
                }
            } else if (pos.getX() == px) {
                if (pz < pos.getZ()) {
                    facing = EnumFacing.SOUTH;
                } else {
                    facing = EnumFacing.NORTH;
                }
            }

            MineBox mb = new MineBox(new V3(pos, placer.dimension), (V3)markerPos, 6, 6);
            ModSimLoader.mines.add(mb);
            mb.facing = facing;
            mb.saveMine();
        }

    }
    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand , EnumFacing side, float hitX, float hitY, float hitZ) {
        //在给定块位置的中心为播放器播放指定的声音 constructor activated 控制箱激活
//        SoundEvent soundEvent=new SoundEvent(new ResourceLocation(ModSim.MODID + ":sim_u_ddd"));
        SoundEvent computer = SoundRegistry.COMPUTER;
        if (computer == null || computer.getRegistryName() == null) {
            ModSimLoader.log.error("播放失败：sim:computer 声音事件未注册");
        } else {
            worldIn.playSound(playerIn, pos, computer, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
        if(!worldIn.isRemote){
            V3 vPos = new V3(pos, playerIn.dimension);
            NpcData fd = null;
            Iterator var12 = ModSimLoader.folks.iterator();
            while(var12.hasNext()) {
                NpcData f = (NpcData)var12.next();
                if (f.job != null && f.job.workPlace.toString().equals(vPos.toString())) {
                    fd = f;
                }
            }
            MineBox mBox = ModSimLoader.getMine(vPos);
            if (mBox == null) {
                //抱歉,此采矿箱出现问题,请重新放置
                ModSimLoader.sendChat(new TextComponentTranslation("container.sim.Mining_box_Sorry",new Object[0]).getUnformattedText());
                return false;
            }

            if (fd == null) {
                NetWorkLoader.net.sendTo(new PacketOpenMineGui(mBox.ID, vPos, mBox.facing, mBox.x, mBox.z), (EntityPlayerMP)playerIn);
            } else {
                NetWorkLoader.net.sendTo(new PacketOpenMineGui(mBox.ID, vPos, mBox.facing, mBox.x, mBox.z, fd.getClientIdentity()), (EntityPlayerMP)playerIn);
            }
        }
        return true;
    }
    /**
     * @Author fan
     * @Description //TODO 玩家摧毁方块
     * @Date 17:34 2022/11/1
     * @Param [worldIn, pos, state]
     * @return void
     **/
    @Override
    public void onBlockDestroyedByPlayer(World worldIn, BlockPos pos, IBlockState state){
        //在给定块位置的中心为播放器播放指定的声音 断电 power down
//        SoundEvent soundEvent=new SoundEvent(new ResourceLocation(ModSim.MODID + ":power_down"));
        SoundEvent power_down = SoundRegistry.POWER_DOWN;
        if (power_down == null || power_down.getRegistryName() == null) {
            ModSimLoader.log.error("播放失败：sim:power_down 声音事件未注册");
        } else {
            worldIn.playSound(null, pos, power_down, SoundCategory.RECORDS, 1.0F, 1.0F);
        }
        for (NpcData fd : ModSimLoader.folks) {
            if (fd.job != null && fd.job.workPlace.equals(new V3(pos, worldIn.provider.getDimension()))) {
                fd.fire();
            }
        }
        MineBox mineBox = ModSimLoader.getMine(new V3(pos, worldIn.provider.getDimension()));
        if (mineBox != null) {
            mineBox.removeMine(mineBox.ID);
        }
        super.onBlockDestroyedByPlayer(worldIn, pos, state);
    }


    /**
     * @return int
     * @Author fan
     * @Description //TODO 获取此块可以删除的项的元数据。当块被破坏时调用此方法。它基于块的旧元数据返回被删除项的元数据。
     * @Date 10:04 2022/11/7
     * @Param [state]
     **/
    @Override
    public int damageDropped(IBlockState state) {
        return this.getMetaFromState(state);
    }
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, ITooltipFlag advanced) {
        tooltip.add(net.minecraft.util.text.translation.I18n.translateToLocal("block.sim.mining_box.tooltip.1"));
        tooltip.add(net.minecraft.util.text.translation.I18n.translateToLocal("block.sim.mining_box.tooltip.2"));
    }
}
