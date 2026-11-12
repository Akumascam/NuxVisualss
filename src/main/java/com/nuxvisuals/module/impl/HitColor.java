package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.ModuleManager;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.ColorSetting;
import com.nuxvisuals.module.setting.ModeSetting;
import com.nuxvisuals.module.setting.SliderSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

// Hit Color: при крите (или любом ударе) цель на мгновение окрашивается в выбранный цвет
public class HitColor extends Module {
    private final ModeSetting mode = add(new ModeSetting("Mode", "Crits Only", "Crits Only", "All Hits"));
    private final ColorSetting critColor = add(new ColorSetting("Crit Color", 0.13f, 0.85f));
    private final ColorSetting hitColor = add(new ColorSetting("Hit Color", 0.0f, 0.75f))
            .visibleIf(() -> this.mode.is("All Hits"));
    private final SliderSetting duration = add(new SliderSetting("Duration", 400, 100, 1500, 50, " ms"));
    private final BoolSetting fade = add(new BoolSetting("Fade", true));
    private final BoolSetting noRed = add(new BoolSetting("Replace Red Hurt", true));
    private final SliderSetting strength = add(new SliderSetting("Strength", 0.85, 0.2, 1.0, 0.05));

    private record Mark(long time, int color) {
    }

    private static final Map<Integer, Mark> MARKS = new HashMap<>();
    private static HitColor instance;

    public HitColor() {
        super("hit-color", "Hit Color", Category.Visuals, false);
        instance = this;
    }

    // ванильная проверка крита (до удара, пока кулдаун не сброшен)
    public static boolean isCrit(PlayerEntity p, Entity target) {
        return p.getAttackCooldownProgress(0.5f) > 0.9f && p.fallDistance > 0 && !p.isOnGround()
                && !p.isClimbing() && !p.isTouchingWater() && !p.hasStatusEffect(StatusEffects.BLINDNESS)
                && !p.hasVehicle() && !p.isSprinting() && target instanceof LivingEntity;
    }

    public static void onHit(PlayerEntity player, Entity target) {
        if (instance == null || !instance.isEnabled() || !(target instanceof LivingEntity)) {
            return;
        }
        boolean crit = isCrit(player, target);
        if (!crit && !instance.mode.is("All Hits")) {
            return;
        }
        int color = crit ? instance.critColor.get() : instance.hitColor.get();
        MARKS.put(target.getId(), new Mark(System.currentTimeMillis(), color));
    }

    // множитель цвета для модели (ARGB) или -1, если подсветки нет
    public static int tintFor(LivingEntity entity) {
        if (instance == null || !instance.isEnabled() || MARKS.isEmpty()) {
            return -1;
        }
        long now = System.currentTimeMillis();
        long dur = (long) instance.duration.get();
        Iterator<Map.Entry<Integer, Mark>> it = MARKS.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue().time() > dur) {
                it.remove();
            }
        }
        Mark mark = MARKS.get(entity.getId());
        if (mark == null) {
            return -1;
        }
        float k = instance.strength.getFloat();
        if (instance.fade.get()) {
            k *= 1f - (now - mark.time()) / (float) dur;
        }
        int c = mark.color();
        int r = Math.round(255 + (((c >> 16) & 255) - 255) * k);
        int g = Math.round(255 + (((c >> 8) & 255) - 255) * k);
        int b = Math.round(255 + ((c & 255) - 255) * k);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public static boolean replaceRed() {
        return instance != null && instance.isEnabled() && instance.noRed.get();
    }

    // перемножить два ARGB-цвета
    public static int multiply(int a, int b) {
        int al = ((a >>> 24) * (b >>> 24)) / 255;
        int r = (((a >> 16) & 255) * ((b >> 16) & 255)) / 255;
        int g = (((a >> 8) & 255) * ((b >> 8) & 255)) / 255;
        int bl = ((a & 255) * (b & 255)) / 255;
        return (al << 24) | (r << 16) | (g << 8) | bl;
    }

    public static HitColor get() {
        return (HitColor) ModuleManager.get("hit-color");
    }
}
