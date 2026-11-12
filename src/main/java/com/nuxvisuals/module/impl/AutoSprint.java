package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

// Автоматический бег при движении вперёд
public class AutoSprint extends Module {
    public AutoSprint() {
        super("auto-sprint", "Auto Sprint", Category.Utilities, false);
    }

    @Override
    public void onTick(MinecraftClient client) {
        ClientPlayerEntity p = client.player;
        if (p == null) {
            return;
        }
        boolean canSprint = p.getHungerManager().getFoodLevel() > 6 || p.getAbilities().allowFlying;
        if (client.options.forwardKey.isPressed() && canSprint && !p.isSneaking()
                && !p.isUsingItem() && !p.horizontalCollision) {
            p.setSprinting(true);
        }
    }
}
