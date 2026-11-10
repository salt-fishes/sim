package com.trhsy.sim.block;

import com.trhsy.sim.loader.CreativeTabsLoader;
import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.loader.NetWorkLoader;
import com.trhsy.sim.loader.SoundRegistry;
import com.trhsy.sim.network.client.PacketOpenFarmGui;
import com.trhsy.sim.npcCode.NpcData;
import com.trhsy.sim.npcCode.V3;
import com.trhsy.sim.npcCode.block.FarmBox;
import com.trhsy.sim.npcCode.enums.FarmType;
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
import java.util.List;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.block
 * @ClassName: BlockFarmingBox
 * @Description: 农田箱子
 * @date 2023/11/07 下午 4:32
 */
public class BlockFarmingBox extends Block {
    public BlockFarmingBox() {
        super(Material.WOOD);
        this.setSoundType(SoundType.WOOD);
        this.setHardness(2.0F);
        this.setResistance(1);
        this.setUnlocalizedName("farmingBox");
        this.setCreativeTab(CreativeTabsLoader.tabSimU);
    }

    /**
     * @Author fan
     * @Description //TODO 激活
     * @Date 12:26 2022/12/10
     * @Param [worldIn, pos, state, playerIn, hand, heldItem, side, hitX, hitY, hitZ]
     * @return boolean
     **/
    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        //在给定块位置的中心为播放器播放指定的声音 constructor activated 控制箱激活
//        SoundEvent soundEvent=new SoundEvent(new ResourceLocation(ModSim.MODID + ":sim_u_ddd"));
        SoundEvent computer = SoundRegistry.COMPUTER;
        if (computer == null || computer.getRegistryName() == null) {
            ModSimLoader.log.error("播放失败：sim:computer 声音事件未注册");
        } else {
            worldIn.playSound(playerIn, pos, computer, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
        if (!worldIn.isRemote) {
            V3 vPos = new V3(pos,playerIn.dimension);
            NpcData fd = null;
            for (NpcData f: ModSimLoader.folks){
                if (f.job != null && f.job.workPlace.toString().equals(new V3(pos,playerIn.dimension).toString())) {
                    fd = f;
                }
            }
            //获取当前农田箱
            FarmBox fBox = ModSimLoader.getFarm(vPos);
            if (fBox == null) {
                //这个养殖箱坏了，请把它打碎重新放好
                ModSimLoader.sendChat(new TextComponentTranslation("container.sim.gui_Farming_broken",new Object[0]).getUnformattedText());
                return false;
            }

            if (fd == null) {
                NetWorkLoader.net.sendTo(new PacketOpenFarmGui(fBox.ID, vPos, fBox.facing,fBox.farmType, fBox.x, fBox.z), (EntityPlayerMP)playerIn);
            } else {
                NetWorkLoader.net.sendTo(new PacketOpenFarmGui(fBox.ID, vPos, fBox.facing,fBox.farmType, fBox.x, fBox.z, fd.getClientIdentity()), (EntityPlayerMP)playerIn);
            }
        }
        return true;
    }
    /**
     * @Author fan
     * @Description //TODO
     * @Date 12:27 2022/12/10
     * @Param [world, pos, state, placer, stack]
     * @return void
     **/
    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        //在给定块位置的中心为播放器播放指定的声音 constructor activated 控制箱激活
//        SoundEvent soundEvent = new SoundEvent(new ResourceLocation(ModSim.MODID + ":sim_u_kraft_ddd_farming_constructor_activated"));
        SoundEvent sim_u_kraft_ddd_farming_constructor_activated = SoundRegistry.SIM_U_KRAFT_DDD_FARMING_CONSTRUCTOR_ACTIVATED;
        if (sim_u_kraft_ddd_farming_constructor_activated == null || sim_u_kraft_ddd_farming_constructor_activated.getRegistryName() == null) {
            ModSimLoader.log.error("播放失败：sim:power_down 声音事件未注册");
        } else {
            world.playSound(null, pos, sim_u_kraft_ddd_farming_constructor_activated, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
        if (!world.isRemote) {
            V3 markerPos = new V3(pos.getX(),pos.getY(),pos.getZ(),placer.dimension);
            //东
            EnumFacing facing = EnumFacing.EAST;
            int px = (int)Math.floor(placer.posX);
            Math.floor(placer.posY);
            int pz = (int)Math.floor(placer.posZ);
            if (pos.getZ() == pz) {
                if (px < pos.getX()) {
                    facing = EnumFacing.EAST;
                } else {
                    //西
                    facing = EnumFacing.WEST;
                }
            } else if (pos.getX() == px) {
                if (pz < pos.getZ()) {
                    //南
                    facing = EnumFacing.SOUTH;
                } else {
                    //北
                    facing = EnumFacing.NORTH;
                }
            }
            for (FarmBox farmBox : ModSimLoader.farms) {
                if (farmBox.loc.equals(markerPos)) {
                    farmBox.removeFarm(farmBox.ID);
                }
            }
            FarmBox fb = new FarmBox(new V3(pos,placer.dimension), (V3)markerPos);
            ModSimLoader.farms.add(fb);
            fb.facing = facing;
            fb.farmType= FarmType.WHEAT;
            fb.saveFarm();
        }

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
            worldIn.playSound(null, pos, power_down, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
        V3 markerPos = new V3(pos.getX(),pos.getY(),pos.getZ(),worldIn.provider.getDimension());
        for (FarmBox farmBox : ModSimLoader.farms) {
            if (farmBox.loc.equals(markerPos)) {
                farmBox.removeFarm(farmBox.ID);
            }
        }
        for (NpcData fd : ModSimLoader.folks) {
            if (fd.job != null && fd.job.workPlace.equals(new V3(pos, worldIn.provider.getDimension()))) {
                fd.fire();
            }
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
        tooltip.add(net.minecraft.util.text.translation.I18n.translateToLocal("block.sim.farming_box.tooltip.1"));
        tooltip.add(net.minecraft.util.text.translation.I18n.translateToLocal("block.sim.farming_box.tooltip.2"));
    }
}
