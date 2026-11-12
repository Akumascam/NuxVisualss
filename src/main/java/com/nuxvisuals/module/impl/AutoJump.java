package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import net.minecraft.client.MinecraftClient;

// Включает ванильный автопрыжок
public class AutoJump extends Module {
    public AutoJump() {
        super("auto-jump", "Auto Jump", Category.Utilities, false);
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (!client.options.getAutoJump().getValue()) {
            client.options.getAutoJump().setValue(true);
        }
    }

    @Override
    public void onDisable() {
        MinecraftClient.getInstance().options.getAutoJump().setValue(false);
    }
}
