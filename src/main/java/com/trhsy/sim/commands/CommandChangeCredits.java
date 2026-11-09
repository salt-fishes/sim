package com.trhsy.sim.commands;

import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.loader.NetWorkLoader;
import com.trhsy.sim.network.client.PacketUpdateMoney;
import com.trhsy.sim.npcCode.store.SimStore;
import net.minecraft.client.resources.I18n;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.commands
 * @ClassName: CommandChangeCredits
 * @Description: 命令更改金钱
 * @date 2023/11/20 下午 3:33
 */
public class CommandChangeCredits implements ICommand {

    private final List aliases;
    /**
     * 命令更改金币
     */
    public CommandChangeCredits() {
        aliases = new CopyOnWriteArrayList();
        aliases.add("credits");
    }
    @Override
    public String getName() {
        return "credits";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "credits <amount>";
    }

    @Override
    public List<String> getAliases() {
        return this.aliases;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] argString) throws CommandException {
        try {
            if(argString.length>0){
                float amount = Float.parseFloat(argString[0]);
                ModSimLoader.money = amount;
                // P17(S1): 直接写 SimStore。旧实现只写 settings.sk2，而
                // simModLoad 从 sim_store.dat 读资金；开服早期执行 /credits 时
                // 运行时值会在下一次 simModLoad 被旧 store 覆盖。
                World world = server != null ? server.getWorld(0) : sender.getEntityWorld();
                SimStore store = SimStore.get(world);
                if (store != null) {
                    store.money = amount;
                    store.markDirty();
                } else {
                    // 非主世界/客户端兜底：保留旧路径，至少不丢运行时值。
                    ModSimLoader.saveStates();
                }
                NetWorkLoader.net.sendToAll(new PacketUpdateMoney());
            }else{
                //无效的参数，应该是：/credits <amount>
                ModSimLoader.sendChat(new TextComponentTranslation("container.sim.commands1",new Object[0]).getUnformattedText());
                return;
            }
        } catch (Exception e) {
            //金额必须是数字！
            ModSimLoader.sendChat(new TextComponentTranslation("container.sim.commands2",new Object[0]).getUnformattedText());
            //StackTraceElement element=e.getStackTrace()[0];ModSimReloaded.log.error("初始化对齐梁出错了：" + e.getMessage()+"行数："+element.getLineNumber());
            return;
        }
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
        return null;
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }

    @Override
    public int compareTo(ICommand o) {
        return 0;
    }
}
