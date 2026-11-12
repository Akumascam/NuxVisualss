package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.SliderSetting;
import com.nuxvisuals.render.MotionBlurRenderer;
import net.minecraft.client.MinecraftClient;

/** Temporal accumulation at the end of world rendering, before HUD/GUI. */
public class MotionBlur extends Module {
    public static MotionBlur INSTANCE;
    private final SliderSetting strength = add(new SliderSetting("Strength", 0.45, 0, 0.9, 0.05));
    private final SliderSetting resetDistance = add(new SliderSetting("Reset Distance", 6, 1, 32, 1, " m"));

    public MotionBlur() {
        super("motion-blur", "MotionBlur", Category.Utilities, false);
        INSTANCE = this;
    }

    public static void render() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (INSTANCE == null || !INSTANCE.isEnabled() || INSTANCE.strength.get() <= 0
                || client.world == null || client.player == null || client.currentScreen != null) {
            MotionBlurRenderer.reset();
            return;
        }
        MotionBlurRenderer.render(client, INSTANCE.strength.getFloat(), INSTANCE.resetDistance.get());
    }

    @Override public void onTick(MinecraftClient client) {
        if (client.world == null || client.currentScreen != null) MotionBlurRenderer.reset();
    }
    @Override public void onEnable() { MotionBlurRenderer.reset(); }
    @Override public void onDisable() { MotionBlurRenderer.close(); }
}
