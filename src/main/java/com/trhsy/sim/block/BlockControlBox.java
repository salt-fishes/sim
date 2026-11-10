package com.trhsy.sim.block;

import com.trhsy.sim.block.enums.EnumBlock;
import com.trhsy.sim.block.enums.EnumControlBox;
import com.trhsy.sim.loader.CreativeTabsLoader;
import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.loader.NetWorkLoader;
import com.trhsy.sim.loader.SoundRegistry;
import com.trhsy.sim.network.client.PacketOpenControlGui;
import com.trhsy.sim.network.client.PacketLogisticsSnapshot;
import com.trhsy.sim.npcCode.NpcData;
import com.trhsy.sim.npcCode.SimIds;
import com.trhsy.sim.npcCode.V3;
import com.trhsy.sim.npcCode.build.Building;
import com.trhsy.sim.npcCode.logistics.LogisticsService;
import com.trhsy.sim.npcCode.logistics.LogisticsSnapshot;
import com.trhsy.sim.npcCode.store.SimStore;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.block
 * @ClassName: BlockControlBox
 * @Description: 控制箱
 * @date 2023/11/06 下午 4:45
 */
public class BlockControlBox extends EnumBlock<EnumControlBox> {
    public List<NpcData> employees = new CopyOnWriteArrayList<NpcData>();
    public static final PropertyEnum<EnumControlBox> TYPE = PropertyEnum.create("type", EnumControlBox.class);
    // ------------------------------ 常量定义 ------------------------------
    // 声音事件键（对应 resources/assets/[modid]/sounds.json 中的注册名）
    private static final String SOUND_ACTIVATE = ":sim_u_ddd";
    // 提示信息键（对应 lang/en_us.json 中的翻译键）
    private static final String TOOLTIP_KEY = "block.sim.control_box.tooltip";
    // 建筑类型提示键（住宅）
    private static final String BUILDING_TYPE_RESIDENTIAL = "container.sim.sim_gui_BC_Residential";

    public BlockControlBox() {
        super(Material.WOOD,TYPE,EnumControlBox.class);
        //用于设定走在方块上的响声。
        this.setSoundType(SoundType.WOOD);
        //方块硬度
        this.setHardness(10.0F);
        //爆炸
        this.setResistance(1.0F); // 爆炸抗性调整为 1.0F（原 1 可能过低）
        this.setUnlocalizedName("controlBox");
        this.setDefaultState(this.blockState.getBaseState().withProperty(TYPE, EnumControlBox.TOP));
        this.setCreativeTab(CreativeTabsLoader.tabSimU);
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

    /**
     * @return void
     * @Author fan
     * @Description //TODO 返回具有相同ID但不同meta的块列表（例如：wood返回4个块）
     * @Date 10:03 2022/11/7
     * @Param [itemIn, tab, list]
     **/
    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> list) {
       /* try {
            for (EnumControlBox enumControlBox : EnumControlBox.values()) {
                list.add(new ItemStack(this, 1, enumControlBox.getMetadata()));
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("控制箱getSubBlocks出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }*/
        for (EnumControlBox type : EnumControlBox.values()) {
            list.add(new ItemStack(this, 1, type.getMetadata()));
        }
    }

    /**
     * @return int
     * @Author fan
     * @Description //TODO 将BlockState转换为正确的元数据值
     * @Date 10:04 2022/11/7
     * @Param [state]
     **/
    @Override
    public int getMetaFromState(IBlockState state) {
        return (state.getValue(TYPE)).getMetadata();
    }
    /**
     * @return net.minecraft.block.state.IBlockState
     * @Author fan
     * @Description //TODO 将给定的元数据转换为此块的BlockState
     * @Date 10:05 2022/11/7
     * @Param [meta]
     **/
    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(TYPE, EnumControlBox.byMetadata(meta));
    }


    /**
     * @return net.minecraft.block.state.BlockStateContainer
     * @Author fan
     * @Description //TODO
     * @Date 10:05 2022/11/7
     * @Param []
     **/
    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, new IProperty[]{TYPE});
    }


    /**
     * 销毁时要丢弃的项目数量
     *
     * @param random
     * @return
     */
    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    /**
     * @return boolean
     * @Author fan
     * @Description //TODO 右键
     * @Date 11:39 2022/11/7
     * @Param [worldIn, pos, state, playerIn, hand, heldItem, side, hitX, hitY, hitZ]
     **/
    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        //在给定块位置的中心为播放器播放指定的声音 computer 控制箱激活
        SoundEvent computer = SoundRegistry.COMPUTER;
        if (computer == null || computer.getRegistryName() == null) {
            ModSimLoader.log.error("播放失败：sim:computer 声音事件未注册");
        } else {
            worldIn.playSound(playerIn, pos, computer, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
        //客户端
       /* if (!worldIn.isRemote) {
            //获取控制箱位置
            V3 vPos = new V3(pos,playerIn.dimension);
            //获取该位置的建筑
            Building b = ModSimLoader.getBuildingByV3(vPos);
            List<NpcData> occupants=new CopyOnWriteArrayList<NpcData>();

            if(b!=null){
                if(b.controlXYZ!=null){
                    vPos=b.controlXYZ;
                }
                //存在则查询其下拥有者/员工
                occupants=b.occupants;
                if(occupants!=null&&occupants.size()>0){
                    //建筑是住宅
                    if(b.buildingType.equals(new TextComponentTranslation("container.sim.sim_gui_BC_Residential",new Object[0]).getUnformattedText())){
                        NetWorkLoader.net.sendTo(new PacketOpenControlGui(vPos,b.ID.toString(),b.buildingName,b.buildingType,b.jobType,b.author,b.desc, occupants,true), (EntityPlayerMP) playerIn);
                    }else{
                        NetWorkLoader.net.sendTo(new PacketOpenControlGui(vPos, b.ID.toString(),b.buildingName,b.buildingType,b.jobType,b.author,b.desc, occupants,false), (EntityPlayerMP) playerIn);
                    }
                }else{
                    //没有员工
                    NetWorkLoader.net.sendTo(new PacketOpenControlGui(vPos, b.ID.toString(),b.buildingName,b.buildingType,b.jobType,b.author,b.desc, occupants,false), (EntityPlayerMP) playerIn);
                }
            }else{
                //建筑等于空不存在提示错误信息
                NetWorkLoader.net.sendTo(new PacketOpenControlGui(vPos, "","","","","","", false), (EntityPlayerMP) playerIn);
            }


        }*/
        // 播放激活声音（确保声音已注册）
        playActivateSound(worldIn, pos);
        if (!worldIn.isRemote) {
            // 获取控制箱位置（带维度信息）
            V3 controlPos = new V3(pos, playerIn.dimension);

            // 查询关联建筑（带空值保护）
            Building building = ModSimLoader.getBuildingByV3(controlPos);
            List<NpcData> occupants = getBuildingOccupants(building);

            // 构建 GUI 参数（使用安全值）
            PacketOpenControlGui packet = buildControlGuiPacket(controlPos, building, occupants, playerIn);

            // 发送网络包（仅服务端）
            if (packet != null) {
                NetWorkLoader.net.sendTo(packet, (EntityPlayerMP) playerIn);
                sendLogisticsSnapshot(worldIn, controlPos, building, playerIn);
            }
        }
        return true;
    }
    /**
     * @return void
     * @Author fan
     * @Description //TODO 玩家销毁时
     * @Date 10:50 2022/11/7
     * @Param [worldIn, pos, state]
     **/
    @Override
    public void onBlockDestroyedByPlayer(World worldIn, BlockPos pos, IBlockState state) {
        //控制箱销毁，解除所有NPC
        /*for (NpcData fd : ModSimLoader.folks) {
            if (fd.job != null && fd.job.workPlace.equals(new V3(pos))) {
                fd.fire();
            }
        }*/
        // 遍历所有 NPC 并解除关联
        ModSimLoader.folks.forEach(npc -> {
            if (npc.job != null && isControlBoxWorkplace(npc.job.workPlace, pos)) {
                npc.fire();
            }
        });
    }
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, ITooltipFlag advanced) {
        // 添加多语言提示（示例："control_box.tooltip"="控制箱：右键打开管理界面"）
        tooltip.add(I18n.translateToLocal(TOOLTIP_KEY));
        tooltip.add(I18n.translateToLocal(TOOLTIP_KEY + ".2"));
    }

    /**
     * 打开快递员/物流中心控制箱时，把当前物流设置快照发给客户端。
     */
    private void sendLogisticsSnapshot(World world, V3 controlPos, @Nullable Building building, EntityPlayer player) {
        if (building == null || player == null || !(player instanceof EntityPlayerMP)) {
            return;
        }
        String jobType = building.jobType == null ? "" : building.jobType;
        if (!SimIds.vocation(10).equals(SimIds.jobIdFromDisplay(jobType))) {
            return;
        }
        SimStore store = SimStore.get(world);
        if (store == null) {
            return;
        }
        LogisticsSnapshot snapshot = LogisticsService.snapshot(store, controlPos);
        NetWorkLoader.net.sendTo(new PacketLogisticsSnapshot(snapshot.writeToNBT()), (EntityPlayerMP) player);
    }

    /**
     * 播放控制箱激活声音（确保声音已注册）
     */
    private void playActivateSound(World world, BlockPos pos) {
        SoundEvent computer = SoundRegistry.COMPUTER;
        if (computer == null || computer.getRegistryName() == null) {
            ModSimLoader.log.error("播放失败：sim:computer 声音事件未注册");
        } else {
            world.playSound(null, pos, computer, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }

    }
    /**
     * 获取建筑的关联 NPC（带空值保护）
     */
    @Nullable
    private List<NpcData> getBuildingOccupants(@Nullable Building building) {
        if (building == null || building.occupants == null) {
            return new CopyOnWriteArrayList<>(); // 返回空列表而非 null
        }
        return building.occupants;
    }
    /**
     * 构建控制箱 GUI 网络包（带参数校验）
     */
    @Nullable
    private PacketOpenControlGui buildControlGuiPacket(V3 controlPos, @Nullable Building building,
                                                       List<NpcData> occupants, EntityPlayer player) {
        if (building == null) {
            // 无建筑时发送默认包
            return new PacketOpenControlGui(
                    controlPos,
                    "", "", "", "", "", "",
                    occupants,
                    false
            );
        }

        // 建筑基础信息（带空值保护）
        String buildingId = building.ID != null ? building.ID.toString() : "";
        String buildingName = building.buildingName != null ? building.buildingName : "";
        String buildingType = building.buildingType != null ? building.buildingType : "";
        String jobType = building.jobType != null ? building.jobType : "";
        String author = building.author != null ? building.author : "";
        String desc = building.desc != null ? building.desc : "";

        // 判断是否为住宅（使用多语言翻译）
        boolean isResidential = I18n.translateToLocal(BUILDING_TYPE_RESIDENTIAL).equals(buildingType);

        // 旧存档里 building.occupants 可能为空（雇员只记录在 NPC 的 job.workPlace 上）。
        // 非住宅建筑在打开 GUI 时按工作地点补齐，避免“员工 0 / 没有员工”，
        // 点开员工列表后才看到人的问题。
        if (!isResidential) {
            mergeWorkplaceEmployees(occupants, controlPos);
        }

        return new PacketOpenControlGui(
                controlPos,
                buildingId,
                buildingName,
                buildingType,
                jobType,
                author,
                desc,
                occupants,
                isResidential
        );
    }
    /**
     * 旧存档兼容：把工作地点等于当前控制箱的 NPC 合并进雇员列表。
     * 只用于非住宅建筑；按 NPC UUID 去重。
     */
    private void mergeWorkplaceEmployees(List<NpcData> occupants, V3 controlPos) {
        if (occupants == null || controlPos == null) {
            return;
        }
        for (NpcData npc : ModSimLoader.folks) {
            if (npc == null || npc.job == null || npc.job.workPlace == null) {
                continue;
            }
            if (!npc.job.workPlace.equals(controlPos)) {
                continue;
            }
            boolean exists = false;
            for (NpcData occupant : occupants) {
                if (occupant != null && occupant.ID != null && occupant.ID.equals(npc.ID)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                occupants.add(npc);
            }
        }
    }

    /**
     * 判断 NPC 工作位置是否为当前控制箱位置
     */
    private boolean isControlBoxWorkplace(V3 workplace, BlockPos controlPos) {
        return workplace != null && workplace.toBlockPos().equals(controlPos);
    }
}
