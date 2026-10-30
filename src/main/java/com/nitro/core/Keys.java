package com.nitro.core;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/** Raw keyboard/mouse polling (independent of Screen input-event signatures). */
public final class Keys {
    private Keys() {}
    private static long win() { return MinecraftClient.getInstance().getWindow().getHandle(); }
    public static boolean down(int key) { return key > 0 && GLFW.glfwGetKey(win(), key) == GLFW.GLFW_PRESS; }
    public static boolean mouse(int btn) { return GLFW.glfwGetMouseButton(win(), btn) == GLFW.GLFW_PRESS; }

    public static boolean isDown(KeyBinding kb) {
        InputUtil.Key k = KeyBindingHelper.getBoundKeyOf(kb);
        if (k.getCategory() == InputUtil.Type.MOUSE) return mouse(k.getCode());
        if (k.getCategory() == InputUtil.Type.KEYSYM) return down(k.getCode());
        return false;
    }

    public static String name(int k) {
        switch (k) {
            case 0: return "-";
            case 32: return "Space"; case 258: return "Tab";
            case 340: return "LShift"; case 344: return "RShift";
            case 341: return "LCtrl"; case 345: return "RCtrl";
            case 342: return "LAlt"; case 346: return "RAlt";
            case 262: return "Right"; case 263: return "Left"; case 264: return "Down"; case 265: return "Up";
            default:
        }
        if (k >= 290 && k <= 301) return "F" + (k - 289);
        String s = GLFW.glfwGetKeyName(k, 0);
        return s != null ? s.toUpperCase() : "K" + k;
    }
}
