package com.trhsy.sim.key;

import com.trhsy.sim.ModSim;
import com.trhsy.sim.gui.GuiCityGuide;
import com.trhsy.sim.loader.ModSimLoader;
import net.minecraft.client.Minecraft;
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

    // V 键：随时打开市长手册（原来的“重新加载蓝图”从未实现，正好改成新手入口）
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
        // 按下 V：没有其他界面时打开市长手册
        if (SUMMON.isPressed()) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.currentScreen == null && mc.player != null) {
                mc.displayGuiScreen(new GuiCityGuide());
            }
        }
    }
}
