package com.nuxvisuals.module;

import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.ColorSetting;
import com.nuxvisuals.module.setting.Setting;
import com.nuxvisuals.render.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

import java.util.ArrayList;
import java.util.List;

public class Module {
    private final String id;
    private final String name;
    private final Category category;
    private final List<Setting> settings = new ArrayList<>();
    private boolean enabled;
    private String description = "";
    private int key = -1;
    private boolean keyWasDown;
    private boolean holdActive, holdPrevious;
    private final com.nuxvisuals.module.setting.ModeSetting bindMode = add(
            new com.nuxvisuals.module.setting.ModeSetting("Режим бинда", "Нажатие", "Нажатие", "Удержание")
                    .visibleIf(() -> getKey() >= 0));

    public Module(String id, String name, Category category, boolean enabled) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.enabled = enabled;
    }

    protected <T extends Setting> T add(T setting) {
        this.settings.add(setting);
        return setting;
    }

    protected static MinecraftClient mc() {
        return MinecraftClient.getInstance();
    }

    // цвет с учётом режима Rainbow
    protected static int color(ColorSetting color, BoolSetting rainbow, float offset) {
        if (rainbow != null && rainbow.get()) {
            return RenderUtil.hsv(RenderUtil.rainbowHue(offset), Math.max(0.6f, color.getSaturation()), 1f);
        }
        return color.get();
    }

    public String getId() {
        return this.id;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // клавиша быстрого включения (-1 = нет)
    public int getKey() {
        return this.key;
    }

    public void setKey(int key) {
        releaseHold();
        this.key = key;
        this.keyWasDown = false;
    }

    public boolean pollKey(boolean down) {
        boolean pressed = down && !this.keyWasDown;
        this.keyWasDown = down;
        return pressed;
    }

    private void releaseHold() {
        if (holdActive) { holdActive = false; setEnabled(holdPrevious); }
    }
    public void updateBind(boolean physicallyDown, boolean allowed) {
        boolean rising = physicallyDown && !keyWasDown;
        keyWasDown = physicallyDown;
        if (!bindMode.is("Удержание")) {
            releaseHold();
            if (rising && allowed) toggle();
            return;
        }
        if (holdActive && (!physicallyDown || !allowed)) releaseHold();
        // Don't auto-enable after closing a menu while still holding the key.
        if (!holdActive && rising && allowed) {
            holdPrevious = isEnabled(); holdActive = true; setEnabled(true);
        }
    }

    public String getName() {
        return this.name;
    }

    public Category getCategory() {
        return this.category;
    }

    public List<Setting> getSettings() {
        return this.settings;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
        com.nuxvisuals.hud.Notifications.moduleToggled(this);
    }

    // реальное состояние (без проверки ключа) — для сохранения конфига
    public boolean rawEnabled() {
        return holdActive ? holdPrevious : this.enabled;
    }

    // при загрузке конфига, без вызова onEnable/onDisable
    public void loadEnabled(boolean enabled) {
        holdActive = false;
        this.enabled = enabled;
    }

    public void toggle() {
        setEnabled(!this.enabled);
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public void onTick(MinecraftClient client) {
    }

    public void onRender(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
    }
}
