package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.SliderSetting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.util.math.RotationAxis;

// Item Physics: выброшенные предметы падают и лежат на земле плашмя, а в воздухе кувыркаются.
// Работает через ItemEntityRendererMixin (данные предмета запоминаются прямо перед его отрисовкой).
public class ItemPhysics extends Module {
    public static ItemPhysics INSTANCE;

    private final SliderSetting spin = add(new SliderSetting("Air Spin", 1, 0, 3, 0.1));
    private final BoolSetting flatBlocks = add(new BoolSetting("Flat Blocks", false));
    private final BoolSetting noBobbing = add(new BoolSetting("No Bobbing", true));

    // данные текущего рисуемого предмета
    private static boolean ground;
    private static boolean block;
    private static boolean water;
    private static float age;
    private static float angle;

    public ItemPhysics() {
        super("item-physics", "Item Physics", Category.Visuals, false);
        INSTANCE = this;
    }

    private static boolean on() {
        return INSTANCE != null && INSTANCE.isEnabled();
    }

    public static void capture(ItemEntity entity, float tickDelta) {
        if (!on()) {
            return;
        }
        ground = entity.isOnGround();
        water = entity.isTouchingWater();
        block = entity.getStack().getItem() instanceof BlockItem;
        age = entity.getItemAge() + tickDelta;
        // у каждого предмета свой постоянный поворот на земле
        angle = (Math.floorMod(entity.getUuid().hashCode(), 360));
    }

    // вместо подпрыгивания; true — ванильный сдвиг пропускаем
    public static boolean translate(MatrixStack m) {
        if (!on()) {
            return false;
        }
        boolean flat = !block || INSTANCE.flatBlocks.get();
        if (ground) {
            m.translate(0, flat ? 0.04f : 0.125f, 0);
        } else {
            float bob = INSTANCE.noBobbing.get() || !water ? 0 : (float) Math.sin(age / 10f) * 0.05f;
            m.translate(0, 0.15f + bob, 0);
        }
        return true;
    }

    // вместо вращения вокруг оси Y
    public static boolean rotate(MatrixStack m) {
        if (!on()) {
            return false;
        }
        boolean flat = !block || INSTANCE.flatBlocks.get();
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));
        if (ground) {
            if (flat) {
                m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
            }
        } else {
            float s = INSTANCE.spin.getFloat();
            m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(age * 18f * s));
            m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(age * 7f * s));
        }
        return true;
    }
}
