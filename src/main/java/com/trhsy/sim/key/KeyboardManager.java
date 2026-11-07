package com.trhsy.sim.key;

import com.trhsy.sim.ModSim;
import com.trhsy.sim.loader.ModSimLoader;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.key
 * @ClassName: KeyboardManager
 * @Description: 按键按下后
 * @date 2023.12.6 15:19
 */
@Mod.EventBusSubscriber(modid = ModSim.MODID, value = Side.CLIENT)
public class KeyboardManager {
    public static final List<KeyBinding> KEY_BINDINGS = new ArrayList<KeyBinding>();

    //我们所有的键位进行声明  参数最重要的是Keyboard.KEY_X 对应我们的按键，其他基本不变
    public static final KeyBinding SUMMON = new ModKeyBinding("activate_skill_simfiles_loading", KeyConflictContext.IN_GAME, KeyModifier.NONE, Keyboard.KEY_V, "sim_mod");

    /**
     * 初始化
     */
    public static void init() {
        for (KeyBinding key : KEY_BINDINGS) {
            ClientRegistry.registerKeyBinding(key);
        }
        ModSimLoader.log.info("注册了 d% 个按键",KEY_BINDINGS.size());
    }

    /**
     * 捕获当按键按下时
     * @param event
     */
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onKeyPressed(InputEvent.KeyInputEvent event) {
        //当按下EFFECTME键(V键)时
        if (SUMMON.isPressed()) {
//            ModSimLoader.loadAllBuildings();
            ModSimLoader.log.info("加载所有建筑");
        }
    }
}
