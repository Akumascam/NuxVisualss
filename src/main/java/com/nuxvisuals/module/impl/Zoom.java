package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.ModuleManager;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.SliderSetting;
import net.minecraft.client.MinecraftClient;
import com.nuxvisuals.module.Keys;
import com.nuxvisuals.module.setting.KeySetting;
import org.lwjgl.glfw.GLFW;

// Приближение на клавишу (по умолчанию C)
public class Zoom extends Module {
    private final KeySetting key = add(new KeySetting("Key", GLFW.GLFW_KEY_C));
    private final BoolSetting scrollZoom = add(new BoolSetting("Scroll Adjust", true));
    private final SliderSetting factor = add(new SliderSetting("Zoom", 4, 1.5, 10, 0.5, "x"));
    private final BoolSetting smooth = add(new BoolSetting("Smooth", true));
    private final BoolSetting cinematic = add(new BoolSetting("Cinematic Camera", true));

    private float current = 1f;
    private long lastFrame;
    private boolean wasZooming;
    private boolean savedCinematic;
    private float extra = 1f;

    public static boolean isZooming() {
        Zoom zoom = (Zoom) ModuleManager.get("zoom");
        return zoom != null && zoom.isEnabled() && zoom.wasZooming;
    }

    // колесо мыши во время зума — приблизить/отдалить
    public static boolean onScroll(double amount) {
        Zoom zoom = (Zoom) ModuleManager.get("zoom");
        if (zoom == null || !zoom.wasZooming || !zoom.scrollZoom.get()) {
            return false;
        }
        zoom.extra = Math.max(0.5f, Math.min(4f, zoom.extra * (amount > 0 ? 1.15f : 1 / 1.15f)));
        return true;
    }

    public Zoom() {
        super("zoom", "Zoom", Category.Utilities, true);
    }

    public static float apply(float fov) {
        Zoom zoom = (Zoom) ModuleManager.get("zoom");
        return zoom == null ? fov : zoom.modify(fov);
    }

    private float modify(float fov) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean zooming = isEnabled() && Keys.isDown(key.get()) && client.currentScreen == null;

        if (zooming != wasZooming && cinematic.get()) {
            if (zooming) {
                savedCinematic = client.options.smoothCameraEnabled;
                client.options.smoothCameraEnabled = true;
            } else {
                client.options.smoothCameraEnabled = savedCinematic;
            }
        }
        wasZooming = zooming;

        if (!zooming) {
            extra = 1f;
        }
        float target = zooming ? factor.getFloat() * extra : 1f;
        long now = System.currentTimeMillis();
        float dt = lastFrame == 0 ? 1 : Math.min(1f, (now - lastFrame) / 1000f * 12f);
        lastFrame = now;
        current = smooth.get() ? current + (target - current) * dt : target;
        return fov / current;
    }
}
