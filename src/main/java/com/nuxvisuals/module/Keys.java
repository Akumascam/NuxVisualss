package com.nuxvisuals.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

// Клавиши для биндов: обычные коды GLFW, а 1000+N — кнопки мыши (MOUSE3, MOUSE4...)
public final class Keys {
    public static final int MOUSE_OFFSET = 1000;

    public static boolean isDown(int key) {
        if (key < 0) {
            return false;
        }
        long handle = MinecraftClient.getInstance().getWindow().getHandle();
        if (key >= MOUSE_OFFSET) {
            return GLFW.glfwGetMouseButton(handle, key - MOUSE_OFFSET) == GLFW.GLFW_PRESS;
        }
        return InputUtil.isKeyPressed(handle, key);
    }

    // физически ли зажата клавиша, назначенная на KeyBinding (нужно чтобы вернуть состояние после авто-нажатий)
    public static boolean isBindingDown(net.minecraft.client.option.KeyBinding binding) {
        InputUtil.Key key = net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.getBoundKeyOf(binding);
        if (key == null || key.getCode() < 0) {
            return false;
        }
        if (key.getCategory() == InputUtil.Type.MOUSE) {
            return isDown(MOUSE_OFFSET + key.getCode());
        }
        return isDown(key.getCode());
    }

    private Keys() {
    }
}
