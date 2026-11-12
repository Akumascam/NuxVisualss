package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Keys;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.KeySetting;
import com.nuxvisuals.module.setting.ModeSetting;
import com.nuxvisuals.module.setting.SliderSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;

// Free Look: зажми клавишу — камера крутится вокруг тебя, а персонаж смотрит и идёт туда же, куда и раньше
public class FreeLook extends Module {
    private final KeySetting key = add(new KeySetting("Look Key", 342)); // Left Alt
    private final ModeSetting mode = add(new ModeSetting("Mode", "Hold", "Hold", "Toggle"));
    private final BoolSetting thirdPerson = add(new BoolSetting("Third Person", true));
    private final BoolSetting invertPitch = add(new BoolSetting("Invert Pitch", false));
    private final SliderSetting distanceSetting = add(new SliderSetting("Distance", 4.0, 1.5, 10.0, 0.5, " bl"));
    private final SliderSetting sensitivity = add(new SliderSetting("Sensitivity", 1.0, 0.2, 3.0, 0.1, "x"));

    private static FreeLook instance;
    private static boolean active;
    private static float cameraYaw;
    private static float cameraPitch;

    private boolean toggled;
    private boolean wasDown;
    private Perspective savedPerspective;

    public FreeLook() {
        super("free-look", "Free Look", Category.Visuals, false);
        instance = this;
    }

    public static boolean active() {
        return active && instance != null && instance.isEnabled();
    }

    public static float yaw() {
        return cameraYaw;
    }

    public static double distance() {
        return instance.distanceSetting.get();
    }

    public static float pitch() {
        return cameraPitch;
    }

    // движение мыши во время свободного обзора — крутим только камеру
    public static void look(double dx, double dy) {
        float s = instance.sensitivity.getFloat();
        float pitchDelta = (float) dy * 0.15f * s * (instance.invertPitch.get() ? -1 : 1);
        cameraYaw += (float) dx * 0.15f * s;
        cameraPitch = MathHelper.clamp(cameraPitch + pitchDelta, -90f, 90f);
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (client.player == null) {
            stop(client);
            return;
        }
        boolean down = Keys.isDown(key.get()) && client.currentScreen == null;
        boolean want;
        if (mode.is("Toggle")) {
            if (down && !wasDown) {
                toggled = !toggled;
            }
            want = toggled;
        } else {
            want = down;
        }
        wasDown = down;
        if (want && !active) {
            start(client);
        } else if (!want && active) {
            stop(client);
        }
    }

    private void start(MinecraftClient client) {
        cameraYaw = client.player.getYaw();
        cameraPitch = client.player.getPitch();
        if (thirdPerson.get()) {
            savedPerspective = client.options.getPerspective();
            if (savedPerspective.isFirstPerson()) {
                client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
            }
        }
        active = true;
    }

    private void stop(MinecraftClient client) {
        if (!active) {
            return;
        }
        active = false;
        if (savedPerspective != null) {
            client.options.setPerspective(savedPerspective);
            savedPerspective = null;
        }
    }

    @Override
    public void onDisable() {
        toggled = false;
        stop(mc());
    }
}
