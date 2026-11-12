package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.ChatUtil;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.KeySetting;
import com.nuxvisuals.module.setting.SliderSetting;
import com.nuxvisuals.module.setting.StringSetting;
import net.minecraft.client.MinecraftClient;

// AutoNear: сам прописывает /near раз в N секунд (или по клавише)
public class AutoNear extends Module {
    private final BoolSetting auto = add(new BoolSetting("Auto", true));
    private final SliderSetting interval = add(new SliderSetting("Interval", 30, 5, 300, 5, " s").visibleIf(auto::get));
    private final KeySetting key = add(new KeySetting("Manual Key", -1));
    private final StringSetting command = add(new StringSetting("Command", "/near", 64));

    private int ticks;

    public AutoNear() {
        super("auto-near", "AutoNear", Category.Utilities, false);
    }

    @Override
    public void onEnable() {
        ticks = Integer.MAX_VALUE / 2; // первый раз — сразу после включения
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (client.player == null || client.getNetworkHandler() == null) {
            return;
        }
        if (key.pressed()) {
            ChatUtil.send(command.get());
            ticks = 0;
            return;
        }
        if (auto.get() && ++ticks >= interval.getInt() * 20) {
            ticks = 0;
            ChatUtil.send(command.get());
        }
    }
}
