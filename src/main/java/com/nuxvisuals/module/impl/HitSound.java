package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.ModuleManager;
import com.nuxvisuals.module.setting.ModeSetting;
import com.nuxvisuals.module.setting.SliderSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

// Звук при ударе по сущности
public class HitSound extends Module {
    private final ModeSetting sound = add(new ModeSetting("Sound", "Orb", "Orb", "Crit", "Amethyst", "Arrow", "Bell"));
    private final SliderSetting volume = add(new SliderSetting("Volume", 0.8, 0.1, 1.0, 0.05));
    private final SliderSetting pitch = add(new SliderSetting("Pitch", 1.0, 0.5, 2.0, 0.05));

    public HitSound() {
        super("hit-sound", "Hit Sound", Category.Utilities, false);
    }

    public static void onHit() {
        Module m = ModuleManager.get("hit-sound");
        if (m instanceof HitSound hs && hs.isEnabled()) {
            hs.play();
        }
    }

    private void play() {
        SoundEvent event = switch (sound.get()) {
            case "Crit" -> SoundEvents.ENTITY_PLAYER_ATTACK_CRIT;
            case "Amethyst" -> SoundEvents.BLOCK_AMETHYST_BLOCK_HIT;
            case "Arrow" -> SoundEvents.ENTITY_ARROW_HIT_PLAYER;
            case "Bell" -> SoundEvents.BLOCK_BELL_USE;
            default -> SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
        };
        MinecraftClient.getInstance().getSoundManager()
                .play(PositionedSoundInstance.master(event, pitch.getFloat(), volume.getFloat()));
    }
}
