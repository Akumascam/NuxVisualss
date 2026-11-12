package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.SliderSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

/** Briefly PRESS sneak after a critical hit; don't release the user's key. */
public class ShiftTap extends Module {
    public static ShiftTap INSTANCE;
    private final SliderSetting duration = add(new SliderSetting("Длительность", 2, 1, 10, 1, " тик"));
    private final BoolSetting onlyPlayers = add(new BoolSetting("Only Players", false));
    private final BoolSetting allHits = add(new BoolSetting("All Hits", false));
    private final SneakPulse pulse = new SneakPulse();
    private ClientWorld pulseWorld;

    public ShiftTap() {
        super("shift-tap", "ShiftTap", Category.Utilities, false);
        INSTANCE = this;
    }

    // Callback runs BEFORE cooldown reset. Sneak is sampled on the NEXT input tick,
    // not before the attack packet; this must not invalidate the current critical.
    public static void onAttack(PlayerEntity player, Entity target) {
        ShiftTap self = INSTANCE;
        MinecraftClient client = mc();
        if (self == null || !self.isEnabled() || client.world == null
                || client.currentScreen != null || player != client.player || player.isSpectator()) return;
        if (self.onlyPlayers.get() && !(target instanceof PlayerEntity)) return;
        if (!self.allHits.get() && !HitColor.isCrit(player, target)) return;
        self.pulseWorld = client.world;
        self.pulse.start(self.duration.getInt());
    }

    public static boolean sneakInput(boolean manual) {
        ShiftTap self = INSTANCE;
        MinecraftClient client = mc();
        if (self == null) return manual;
        if (!self.isEnabled() || client.world != self.pulseWorld || client.player == null
                || !client.player.isAlive() || client.currentScreen != null || !client.isWindowFocused()) {
            self.pulse.reset();
            return manual;
        }
        return self.pulse.sample(manual);
    }

    @Override public void onTick(MinecraftClient client) {
        if (client.world != pulseWorld || client.player == null || client.currentScreen != null
                || !client.isWindowFocused() || !client.player.isAlive()) pulse.reset();
    }
    @Override public void onDisable() { pulse.reset(); pulseWorld = null; }
}
