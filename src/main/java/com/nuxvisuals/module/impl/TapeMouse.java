package com.nuxvisuals.module.impl;

import com.nuxvisuals.mixin.MinecraftClientInvoker;
import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.ModeSetting;
import com.nuxvisuals.module.setting.SliderSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

// Tape Mouse: «приклеенная» кнопка мыши — держит или кликает сама (фермы, рыбалка, копание)
public class TapeMouse extends Module {
    private final ModeSetting button = add(new ModeSetting("Button", "Left", "Left", "Right"));
    private final ModeSetting mode = add(new ModeSetting("Mode", "Hold", "Hold", "Click"));
    private final SliderSetting delay = add(new SliderSetting("Delay", 20, 1, 400, 1, " tick"))
            .visibleIf(() -> this.mode.is("Click"));
    private final BoolSetting noPause = add(new BoolSetting("Work In Background", true));

    private int timer;
    private boolean held;
    private Boolean oldPause;

    public TapeMouse() {
        super("tape-mouse", "Tape Mouse", Category.Utilities, false);
    }

    private KeyBinding key(MinecraftClient client) {
        return button.is("Left") ? client.options.attackKey : client.options.useKey;
    }

    @Override
    public void onEnable() {
        timer = 0;
    }

    @Override
    public void onDisable() {
        MinecraftClient client = mc();
        if (held) {
            client.options.attackKey.setPressed(false);
            client.options.useKey.setPressed(false);
            held = false;
        }
        if (oldPause != null) {
            client.options.pauseOnLostFocus = oldPause;
            oldPause = null;
        }
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null) {
            return;
        }
        if (noPause.get() && oldPause == null) {
            oldPause = client.options.pauseOnLostFocus;
            client.options.pauseOnLostFocus = false;
        }
        if (client.currentScreen != null) {
            return;
        }
        if (mode.is("Hold")) {
            KeyBinding k = key(client);
            k.setPressed(true);
            held = true;
            return;
        }
        if (held) {
            client.options.attackKey.setPressed(false);
            client.options.useKey.setPressed(false);
            held = false;
        }
        if (++timer >= delay.getInt()) {
            timer = 0;
            MinecraftClientInvoker inv = (MinecraftClientInvoker) client;
            if (button.is("Left")) {
                inv.nuxvisuals$doAttack();
            } else {
                inv.nuxvisuals$setItemUseCooldown(0);
                inv.nuxvisuals$doItemUse();
            }
        }
    }
}
