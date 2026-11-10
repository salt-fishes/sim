package com.trhsy.sim.block;

import com.trhsy.sim.loader.CreativeTabsLoader;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.block
 * @ClassName: BlockCheese
 * @Description:  优化后的奶酪方块（适配 Minecraft 1.12.2 的 Material 设计）
 * @date 2023/11/03 上午 9:25
 */
public class BlockCheese extends Block {


    // ------------------------------ 私有构造（禁止外部直接实例化） ------------------------------
    /**
     * 私有构造方法（强制通过工厂方法创建）
     */
    public BlockCheese() {
        super(Material.CAKE);  // 直接使用原生 Material.CAKE
        this.setSoundType(SoundType.WOOD);  // 木质点击音
        this.setHardness(0.1F);             // 易破坏
        this.setResistance(0.3F);           // 低抗性
        this.setCreativeTab(CreativeTabsLoader.tabSimU);  // 自定义创意标签页
        this.setUnlocalizedName("cheeseBlock");  // 关联 lang: tile.cheeseBlock.name
    }
// ------------------------------ 核心方法重写（多语言/交互逻辑） ------------------------------
    /**
     * 添加物品提示信息（多语言支持）
     */
    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(@Nonnull ItemStack stack, @Nullable World player,
                               @Nonnull List<String> tooltip, @Nonnull ITooltipFlag advanced) {
        // 从lang/en_us.json中读取提示文本（示例："block.sim.cheeseBlock.tooltip"="奶酪方块：易融化，需冷藏保存"）

        tooltip.add(I18n.translateToLocal("block.sim.cheeseBlock.tooltip"));

        // 可选：添加状态提示（如是否被融化）
       /* if (this.isMelted()) {  // 假设新增了融化状态属性
            tooltip.add(I18n.translateToLocal("block.sim.cheeseBlock.melted"));
        }*/
    }
// ------------------------------ 自定义逻辑（可选扩展） ------------------------------
    /**
     * 示例：判断方块是否处于融化状态（需配合状态管理）
     */
    private boolean isMelted() {
        // 实际逻辑：检查方块元数据或NBT标签
        return false;
    }
 /*   public BlockCheese() {
        super(Material.CAKE,"cheeseBlock");
        //用于设定走在方块上的响声。
        this.setSoundType(SoundType.WOOD);
        //设定方块的硬度，如黑曜石是50，铁块5，金块3，圆石2，石头1.5，南瓜1，泥土0.5，甘蔗0，基岩-1。
        this.setHardness(0.1F);
        //设定方块的爆炸抗性，如木头的抗性为4，石头为10，黑曜石为2000，基岩为6000000。
        this.setResistance(0.5F);
        this.setCreativeTab(CreativeTabsLoader.tabSimU);
    }
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, ITooltipFlag advanced) {
    }*/
}