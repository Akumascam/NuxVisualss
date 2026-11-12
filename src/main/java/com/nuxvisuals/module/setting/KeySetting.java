package com.nuxvisuals.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

// Настройка-клавиша (назначается в меню кликом)
public class KeySetting extends Setting {
    private int key;
    private int repeatTicks;

    public KeySetting(String name, int key) {
        super(name);
        this.key = key;
    }

    public int get() {
        return this.key;
    }

    private boolean wasDown;

    // true один раз в момент нажатия (не срабатывает, пока открыт любой экран)
    public boolean pressed() {
        boolean down = this.key >= 0 && com.nuxvisuals.module.Keys.isDown(this.key)
                && net.minecraft.client.MinecraftClient.getInstance().currentScreen == null;
        boolean result = down && !this.wasDown;
        this.wasDown = down;
        return result;
    }

    public boolean trigger(boolean hold, int interval) {
        var client = net.minecraft.client.MinecraftClient.getInstance();
        boolean down = key >= 0 && com.nuxvisuals.module.Keys.isDown(key);
        boolean rising = down && !wasDown;
        wasDown = down;
        if (!down) { repeatTicks = 0; return false; }
        if (client.currentScreen != null || client.player == null || !client.isWindowFocused()) {
            repeatTicks = Math.max(1, interval); return false;
        }
        if (rising) { repeatTicks = Math.max(1, interval); return true; }
        if (!hold) return false;
        if (repeatTicks > 0) { repeatTicks--; return false; }
        repeatTicks = Math.max(1, interval); return true;
    }

    public void set(int key) {
        this.key = key;
        wasDown = true; repeatTicks = 0;
        changed();
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(this.key);
    }

    @Override
    public void load(JsonElement element) {
        this.key = element.getAsInt();
    }
}
