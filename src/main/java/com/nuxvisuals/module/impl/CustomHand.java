package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.SliderSetting;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;

// Положение рук от первого лица. Применяется в HeldItemRendererMixin.
public class CustomHand extends Module {
    public static CustomHand INSTANCE;

    private final SliderSetting x = add(new SliderSetting("X", 0, -1, 1, 0.01));
    private final SliderSetting y = add(new SliderSetting("Y", 0, -1, 1, 0.01));
    private final SliderSetting z = add(new SliderSetting("Z", 0, -1, 1, 0.01));
    private final SliderSetting scale = add(new SliderSetting("Scale", 1, 0.2, 2, 0.05));
    private final SliderSetting rotX = add(new SliderSetting("Rotate X", 0, -180, 180, 1, "°"));
    private final SliderSetting rotY = add(new SliderSetting("Rotate Y", 0, -180, 180, 1, "°"));
    private final SliderSetting rotZ = add(new SliderSetting("Rotate Z", 0, -180, 180, 1, "°"));

    public CustomHand() {
        super("custom-hand", "Custom Hand", Category.Visuals, false);
        INSTANCE = this;
    }

    public static void apply(MatrixStack matrices, AbstractClientPlayerEntity player, Hand hand) {
        if (INSTANCE == null || !INSTANCE.isEnabled()) {
            return;
        }
        Arm arm = hand == Hand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        float side = arm == Arm.RIGHT ? 1f : -1f;
        matrices.translate(INSTANCE.x.getFloat() * side, INSTANCE.y.getFloat(), INSTANCE.z.getFloat());
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(INSTANCE.rotX.getFloat()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(INSTANCE.rotY.getFloat() * side));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(INSTANCE.rotZ.getFloat() * side));
        float s = INSTANCE.scale.getFloat();
        matrices.scale(s, s, s);
    }
}
