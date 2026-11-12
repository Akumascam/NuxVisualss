package com.nuxvisuals.module.impl;

import com.nuxvisuals.mixin.SimpleOptionAccessor;
import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.ModeSetting;
import com.nuxvisuals.module.setting.SliderSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class FullBright extends Module {
    private final ModeSetting mode = add(new ModeSetting("Mode", "Gamma", "Gamma", "Night Vision"));
    private final SliderSetting brightness = add(new SliderSetting("Brightness", 10, 1, 16, 0.5)
            .visibleIf(() -> this.mode.is("Gamma")));

    private Double savedGamma;
    private boolean addedEffect;

    public FullBright() {
        super("full-bright", "Full Bright", Category.Visuals, false);
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (mode.is("Gamma")) {
            removeEffect(client);
            if (savedGamma == null) {
                savedGamma = client.options.getGamma().getValue();
            }
            setGamma(client, brightness.get());
        } else {
            restoreGamma(client);
            if (client.player != null && !client.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                client.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION,
                        StatusEffectInstance.INFINITE, 0, false, false, false));
                addedEffect = true;
            }
        }
    }

    @Override
    public void onDisable() {
        restoreGamma(mc());
        removeEffect(mc());
    }

    private static void setGamma(MinecraftClient client, double value) {
        // обычный setValue не пускает выше 1.0 — пишем напрямую
        ((SimpleOptionAccessor) (Object) client.options.getGamma()).nuxvisuals$setValue(value);
    }

    private void restoreGamma(MinecraftClient client) {
        if (savedGamma != null) {
            setGamma(client, savedGamma);
            savedGamma = null;
        }
    }

    private void removeEffect(MinecraftClient client) {
        if (addedEffect && client.player != null) {
            client.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
        addedEffect = false;
    }
}
