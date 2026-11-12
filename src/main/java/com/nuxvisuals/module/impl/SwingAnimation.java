package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.ModeSetting;
import com.nuxvisuals.module.setting.SliderSetting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

// Swing Animation: разные анимации взмаха рукой с предметом (только визуал от первого лица)
public class SwingAnimation extends Module {
    public static SwingAnimation INSTANCE;

    private final ModeSetting mode = add(new ModeSetting("Mode", "Smooth",
            "Vanilla", "Smooth", "Push", "Pull", "Down", "Spin", "Slide", "Swank", "Stab", "Block", "Exhibition"));
    private final SliderSetting strength = add(new SliderSetting("Strength", 1, 0.2, 2, 0.05));
    private final SliderSetting speed = add(new SliderSetting("Swing Speed", 6, 2, 20, 1, " tick"));

    public SwingAnimation() {
        super("swing-animation", "Swing Animation", Category.Visuals, false);
        INSTANCE = this;
    }

    private static boolean on() {
        return INSTANCE != null && INSTANCE.isEnabled();
    }

    // длительность взмаха (ванильно 6 тиков)
    public static int swingDuration(int vanilla) {
        if (!on() || vanilla != 6) {
            return vanilla; // спешка/усталость меняют своё — не трогаем
        }
        return INSTANCE.speed.getInt();
    }

    private static void rx(MatrixStack m, float deg) {
        m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(deg));
    }

    private static void ry(MatrixStack m, float deg) {
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(deg));
    }

    private static void rz(MatrixStack m, float deg) {
        m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(deg));
    }

    // true — анимацию нарисовали мы, ванильную пропускаем
    public static boolean apply(MatrixStack m, Arm arm, float p) {
        if (!on() || INSTANCE.mode.is("Vanilla")) {
            return false;
        }
        int i = arm == Arm.RIGHT ? 1 : -1;
        float k = INSTANCE.strength.getFloat();
        float s1 = MathHelper.sin(p * p * (float) Math.PI);
        float s2 = MathHelper.sin(MathHelper.sqrt(p) * (float) Math.PI);
        switch (INSTANCE.mode.get()) {
            case "Smooth" -> {
                ry(m, i * 45f);
                rx(m, -s2 * 60f * k);
                rz(m, i * -s2 * 12f * k);
                ry(m, i * -45f);
            }
            case "Push" -> { // от себя
                m.translate(0, 0.04f * s2 * k, -0.38f * s2 * k);
                ry(m, i * 45f);
                rx(m, -s2 * 18f * k);
                ry(m, i * -45f);
            }
            case "Pull" -> { // к себе
                m.translate(-i * 0.05f * s2 * k, 0.08f * s2 * k, 0.32f * s2 * k);
                ry(m, i * 45f);
                rx(m, s2 * 35f * k);
                ry(m, i * -45f);
            }
            case "Down" -> {
                m.translate(0, -0.25f * s2 * k, 0);
                ry(m, i * 45f);
                rx(m, -s2 * 95f * k);
                ry(m, i * -45f);
            }
            case "Spin" -> {
                ry(m, i * 45f);
                rx(m, -p * 360f * Math.max(1, Math.round(k)));
                ry(m, i * -45f);
            }
            case "Slide" -> {
                m.translate(-i * 0.3f * s2 * k, 0, -0.1f * s2 * k);
                ry(m, i * 45f);
                rz(m, i * -s2 * 30f * k);
                ry(m, i * -45f);
            }
            case "Swank" -> {
                ry(m, i * (45f - s1 * 10f));
                rz(m, i * s2 * 28f * k);
                rx(m, -s2 * 38f * k);
                ry(m, i * -45f);
            }
            case "Stab" -> {
                m.translate(0, 0.05f, -0.55f * s2 * k);
                ry(m, i * 45f);
                rx(m, -25f - s2 * 10f * k);
                ry(m, i * -45f);
            }
            case "Block" -> { // поза «блока» как в 1.7 + взмах
                m.translate(i * -0.12f, 0.18f, 0.05f);
                ry(m, i * 45f);
                rz(m, i * -s2 * 20f * k);
                rx(m, -s2 * 60f * k);
                ry(m, i * -45f);
                ry(m, i * 30f);
                rx(m, -70f);
                ry(m, i * 60f);
            }
            case "Exhibition" -> {
                m.translate(i * -0.05f, 0.12f - s2 * 0.08f * k, 0);
                ry(m, i * 45f);
                rx(m, -s2 * 45f * k);
                rz(m, i * -s2 * 25f * k);
                ry(m, i * -45f);
                ry(m, i * 20f);
                rx(m, -45f);
                ry(m, i * 40f);
            }
            default -> {
                return false;
            }
        }
        return true;
    }
}
