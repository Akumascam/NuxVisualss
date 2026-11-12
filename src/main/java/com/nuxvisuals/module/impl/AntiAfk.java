package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.SliderSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

// Anti AFK: раз в N секунд прыгает (и по желанию крутит камеру, машет рукой, приседает)
public class AntiAfk extends Module {
    private final SliderSetting interval = add(new SliderSetting("Interval", 15, 2, 300, 1, "s"));
    private final BoolSetting jump = add(new BoolSetting("Jump", true));
    private final BoolSetting rotate = add(new BoolSetting("Rotate", false));
    private final SliderSetting angle = add(new SliderSetting("Rotate Angle", 30, 5, 180, 5, "°"))
            .visibleIf(this.rotate::get);
    private final BoolSetting swing = add(new BoolSetting("Swing Hand", false));
    private final BoolSetting sneak = add(new BoolSetting("Sneak", false));
    private final BoolSetting onlyAfk = add(new BoolSetting("Only When AFK", true));

    private int timer;
    private int release;
    private int idle;
    private float lastYaw;
    private float lastPitch;
    private boolean turnBack;

    public AntiAfk() {
        super("anti-afk", "Anti AFK", Category.Utilities, false);
    }

    @Override
    public void onEnable() {
        timer = 0;
        idle = 0;
    }

    @Override
    public void onDisable() {
        MinecraftClient client = mc();
        if (release > 0) {
            client.options.jumpKey.setPressed(false);
            client.options.sneakKey.setPressed(false);
            release = 0;
        }
    }

    @Override
    public void onTick(MinecraftClient client) {
        ClientPlayerEntity p = client.player;
        if (p == null) {
            return;
        }
        // отпускаем клавиши после короткого нажатия
        if (release > 0 && --release == 0) {
            client.options.jumpKey.setPressed(false);
            client.options.sneakKey.setPressed(false);
        }
        // считаем, сколько игрок ничего не делает
        boolean active = client.options.forwardKey.isPressed() || client.options.backKey.isPressed()
                || client.options.leftKey.isPressed() || client.options.rightKey.isPressed()
                || client.options.attackKey.isPressed() || client.options.useKey.isPressed()
                || Math.abs(p.getYaw() - lastYaw) > 0.01f && release == 0 && !turnBack
                || Math.abs(p.getPitch() - lastPitch) > 0.01f && release == 0;
        lastYaw = p.getYaw();
        lastPitch = p.getPitch();
        idle = active && release == 0 ? 0 : idle + 1;
        if (onlyAfk.get() && idle < interval.getInt() * 20) {
            timer = 0;
            return;
        }
        if (++timer < interval.getInt() * 20) {
            return;
        }
        timer = 0;
        if (jump.get()) {
            client.options.jumpKey.setPressed(true);
            release = 3;
        }
        if (sneak.get()) {
            client.options.sneakKey.setPressed(true);
            release = 6;
        }
        if (swing.get()) {
            p.swingHand(net.minecraft.util.Hand.MAIN_HAND);
        }
        if (rotate.get()) {
            float a = angle.getFloat() * (turnBack ? -1 : 1);
            p.setYaw(p.getYaw() + a);
            lastYaw = p.getYaw();
            turnBack = !turnBack;
        }
    }
}
