package com.trhsy.sim.block;

import com.trhsy.sim.loader.*;
import com.trhsy.sim.network.client.PacketAddNewMarker;
import com.trhsy.sim.network.client.PacketOpenMarkerGui;
import com.trhsy.sim.npcCode.V3;
import com.trhsy.sim.npcCode.block.Marker;
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
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.block
 * @ClassName: BlockMarker
 * @Description: 标记棒 IExtendedEntityProperties
 * @date 2023/11/08 上午 10:34
 */
public class BlockMarker extends Block {
    //this.setBlockBounds(0.4F, 0.0F, 0.4F, 0.6F, 0.9F, 0.6F);
    // 调整碰撞盒：更细长的立柱（X/Z方向0.15格宽，Y方向1.0格高，更贴近“标记棒”的视觉效果）
    // 复用老版本的尺寸：X(0.4~0.6)、Y(0.0~0.9)、Z(0.4~0.6)
    protected static final AxisAlignedBB MARKER_AABB = new AxisAlignedBB(
            0.47F, 0.1F, 0.47F,  // 最小坐标（X1, Y1, Z1）
            0.53F, 0.9F, 0.53F   // 最大坐标（X2, Y2, Z2）
    );
    public BlockMarker() {
        super(Material.WOOD);
        this.setSoundType(SoundType.WOOD);
        this.setHardness(2.0F);
        this.setResistance(1);
//        this.setBlockBounds(0.4F, 0.0F, 0.4F, 0.6F, 0.9F, 0.6F);
        this.setUnlocalizedName("markerBar");
//        this.setLightLevel(0.1F);
        this.setCreativeTab(CreativeTabsLoader.tabSimU);

    }
    // 渲染用的碰撞盒（决定方块在世界中占据的空间）
    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos)
    {
        return MARKER_AABB;
    }
    // 实体碰撞盒（决定玩家/生物是否会与方块碰撞）
    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn, BlockPos pos) {
        return MARKER_AABB;
    }
    // 选择盒（玩家鼠标选中时的高亮范围）
    @Override
    public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World worldIn, BlockPos pos) {
        // 移除手动offset，使用默认方法（自动将相对坐标转换为世界坐标）
        return MARKER_AABB.offset(pos);
    }
    /**
     * 用于在重建块以进行渲染时确定环境光遮挡和剔除
     * 非透明方块（影响光影计算，根据模型是否透明调整）
     */
    @Override
    public boolean isOpaqueCube(IBlockState state)
    {
        return false;
    }

    /**
     * // 非完整方块（影响相邻方块的渲染和碰撞）
     * @param state
     * @return
     */
    @Override
    public boolean isFullCube(IBlockState state)
    {
        // 保持false，符合细长模型的特性
        return false;
    }

    /**
     * 方块被放置
     * @param world
     * @param pos
     * @param state
     * @param placer
     * @param stack
     */
    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        if (!world.isRemote) {
            NetWorkLoader.net.sendTo(new PacketAddNewMarker(pos), (EntityPlayerMP)placer);
        }
    }
    /**
     * @return boolean
     * @Author fan
     * @Description //TODO 右键
     * @Date 11:39 2022/11/7
     * @Param [worldIn, pos, state, playerIn, hand, heldItem, side, hitX, hitY, hitZ]
     **/
    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand , EnumFacing side, float hitX, float hitY, float hitZ) {
//        SoundEvent soundEvent=new SoundEvent(new ResourceLocation(ModSim.MODID + ":computer"));
        SoundEvent computer = SoundRegistry.COMPUTER;
        if (computer == null || computer.getRegistryName() == null) {
            ModSimLoader.log.error("播放失败：sim:power_down 声音事件未注册");
        } else {
            worldIn.playSound(playerIn, pos, computer, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
        if (!worldIn.isRemote) {
            V3 vPos = new V3(pos, playerIn.dimension);
            NetWorkLoader.net.sendTo(new PacketOpenMarkerGui(vPos,playerIn.dimension), (EntityPlayerMP) playerIn);
        }
        return true;
    }

    /**
     * 销毁时
     * @param worldIn
     * @param pos
     * @param state
     */
    @Override
    public void onBlockDestroyedByPlayer(World worldIn, BlockPos pos, IBlockState state){
        for (Marker marker : ModSimClientLoader.markers) {
            V3 vPos = new V3(pos);
            if(vPos.equals(marker.loc)){
                ModSimClientLoader.markers.remove(marker);
            }
        }
    }
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, ITooltipFlag advanced) {
        tooltip.add(net.minecraft.util.text.translation.I18n.translateToLocal("block.sim.marker.tooltip.1"));
        tooltip.add(net.minecraft.util.text.translation.I18n.translateToLocal("block.sim.marker.tooltip.2"));
    }
}
