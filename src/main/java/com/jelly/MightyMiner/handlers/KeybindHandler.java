package com.jelly.MightyMiner.handlers;

import com.jelly.MightyMiner.MightyMiner;
import com.jelly.MightyMiner.config.Config;
import com.jelly.MightyMiner.utils.BlockUtils;
import com.jelly.MightyMiner.utils.ReflectionUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Keyboard;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class KeybindHandler {
    static Minecraft mc = Minecraft.getMinecraft();
    static int setmode = 0;
    public static KeyBinding keybindA = mc.gameSettings.keyBindLeft;
    public static KeyBinding keybindD =  mc.gameSettings.keyBindRight;
    public static KeyBinding keybindW = mc.gameSettings.keyBindForward;
    public static KeyBinding keybindS = mc.gameSettings.keyBindBack;
    public static KeyBinding keybindAttack =  mc.gameSettings.keyBindAttack;
    public static KeyBinding keybindUseItem = mc.gameSettings.keyBindUseItem;
    public static KeyBinding keyBindShift = mc.gameSettings.keyBindSneak;
    public static KeyBinding keyBindJump = mc.gameSettings.keyBindJump;

    private static Field mcLeftClickCounter;

    static {
        mcLeftClickCounter = ReflectionHelper.findField(Minecraft.class, "field_71429_W", "leftClickCounter");
        if (mcLeftClickCounter != null)
            mcLeftClickCounter.setAccessible(true);

    }

    public static void rightClick() {
        if (!ReflectionUtils.invoke(mc, "func_147121_ag")) {
            ReflectionUtils.invoke(mc, "rightClickMouse");
        }
    }

    public static void leftClick() {
        if (!ReflectionUtils.invoke(mc, "func_147116_af")) {
            ReflectionUtils.invoke(mc, "clickMouse");
        }
    }

    public static void middleClick() {
        if (!ReflectionUtils.invoke(mc, "func_147112_ai")) {
            ReflectionUtils.invoke(mc, "middleClickMouse");
        }
    }



    static KeyBinding[] macroKeybinds = new KeyBinding[4];

    public static void initializeCustomKeybindings() {
        macroKeybinds[0] = new KeyBinding("Start macro", Keyboard.KEY_F, "MightyMiner");
        macroKeybinds[1] = new KeyBinding("Disable macro", Keyboard.KEY_Z, "MightyMiner");
        macroKeybinds[2] = new KeyBinding("Debug", Keyboard.KEY_H, "MightyMiner");
        macroKeybinds[3] = new KeyBinding("Open GUI", Keyboard.KEY_RSHIFT, "MightyMiner");
        for (KeyBinding customKeyBind : macroKeybinds) {
            ClientRegistry.registerKeyBinding(customKeyBind);
        }
    }

    @SubscribeEvent
    public void onKeyPress(InputEvent.KeyInputEvent event) {

        if(macroKeybinds[0].isKeyDown()){
            MacroHandler.startScript(MightyMiner.config.macroType);
        }
        if(macroKeybinds[1].isKeyDown()){
            MacroHandler.disableScript();
        }
        if(macroKeybinds[2].isKeyDown()){

        }
        if(macroKeybinds[3].isKeyDown()){
            mc.displayGuiScreen(MightyMiner.config.gui());
        }

    }

    @SubscribeEvent
    public void tickEvent(TickEvent.PlayerTickEvent event){
        if(mcLeftClickCounter != null) {
            if (mc.inGameHasFocus) {
                try {
                    mcLeftClickCounter.set(mc, 0);
                } catch (IllegalAccessException | IndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void setKeyBindState(KeyBinding key, boolean pressed) {
        if (pressed) {
            if (mc.currentScreen != null) {
                realSetKeyBindState(key, false);
                return;
            }
        }
        realSetKeyBindState(key, pressed);
    }

    public static void onTick(KeyBinding key) {
        if (mc.currentScreen == null) {
            KeyBinding.onTick(key.getKeyCode());
        }
    }

    public static void updateKeys(boolean wBool, boolean sBool, boolean aBool, boolean dBool, boolean atkBool, boolean useBool, boolean shiftBool) {
        if (mc.currentScreen != null) {
            resetKeybindState();
            return;
        }
        realSetKeyBindState(keybindW, wBool);
        realSetKeyBindState(keybindS, sBool);
        realSetKeyBindState(keybindA, aBool);
        realSetKeyBindState(keybindD, dBool);
        realSetKeyBindState(keybindAttack, atkBool);
        realSetKeyBindState(keybindUseItem, useBool);
        realSetKeyBindState(keyBindShift, shiftBool);
    }

    public static void updateKeys(boolean wBool, boolean sBool, boolean aBool, boolean dBool, boolean atkBool, boolean useBool, boolean shiftBool, boolean jumpBool) {
        if (mc.currentScreen != null) {
            resetKeybindState();
            return;
        }
        realSetKeyBindState(keybindW, wBool);
        realSetKeyBindState(keybindS, sBool);
        realSetKeyBindState(keybindA, aBool);
        realSetKeyBindState(keybindD, dBool);
        realSetKeyBindState(keybindAttack, atkBool);
        realSetKeyBindState(keybindUseItem, useBool);
        realSetKeyBindState(keyBindShift, shiftBool);
        realSetKeyBindState(keyBindJump, jumpBool);
    }

    public static void updateKeys(boolean wBool, boolean sBool, boolean aBool, boolean dBool, boolean atkBool) {
        if (mc.currentScreen != null) {
            resetKeybindState();
            return;
        }
        realSetKeyBindState(keybindW, wBool);
        realSetKeyBindState(keybindS, sBool);
        realSetKeyBindState(keybindA, aBool);
        realSetKeyBindState(keybindD, dBool);
        realSetKeyBindState(keybindAttack, atkBool);
    }

    public static void resetKeybindState() {
        realSetKeyBindState(keybindA, false);
        realSetKeyBindState(keybindS, false);
        realSetKeyBindState(keybindW, false);
        realSetKeyBindState(keybindD, false);
        realSetKeyBindState(keyBindShift, false);
        realSetKeyBindState(keyBindJump, false);
        realSetKeyBindState(keybindAttack, false);
        realSetKeyBindState(keybindUseItem, false);
    }

    private static void realSetKeyBindState(KeyBinding key, boolean pressed){
        if(pressed){
            if(!key.isKeyDown()){
                KeyBinding.onTick(key.getKeyCode());
            }
            KeyBinding.setKeyBindState(key.getKeyCode(), true);

        } else {
            KeyBinding.setKeyBindState(key.getKeyCode(), false);
        }

    }
}
