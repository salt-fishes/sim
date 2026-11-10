package com.trhsy.sim.loader;

import com.trhsy.sim.commands.CommandChangeCredits;
import com.trhsy.sim.commands.CommandGenerateFolk;
import com.trhsy.sim.commands.CommandStart;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

/**
 * 命令加载
 */
public class CommandLoader {
    public CommandLoader(FMLServerStartingEvent event) {
        try {
            //命令生成NPC
            event.registerServerCommand(new CommandGenerateFolk());
            //命令修改金额
            event.registerServerCommand(new CommandChangeCredits());
            //更改模式
            event.registerServerCommand(new CommandStart());
        } catch (Exception e) {
            StackTraceElement element=e.getStackTrace()[0];ModSimLoader.log.error("CommandLoader出错了：" + e.getMessage()+"行数："+element.getLineNumber());
        }
    }
}
