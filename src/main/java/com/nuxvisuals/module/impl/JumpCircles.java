package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.ColorSetting;
import com.nuxvisuals.module.setting.SliderSetting;
import com.nuxvisuals.render.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class JumpCircles extends Module {
    private static final int SEGMENTS = 72;

    private final ColorSetting color = add(new ColorSetting("Color", 0.6f, 0.7f));
    private final BoolSetting rainbow = add(new BoolSetting("Rainbow", false));
    private final SliderSetting size = add(new SliderSetting("Size", 1.2, 0.3, 3.0, 0.05));
    private final SliderSetting thickness = add(new SliderSetting("Thickness", 0.3, 0.05, 1.0, 0.05));
    private final SliderSetting duration = add(new SliderSetting("Duration", 0.8, 0.3, 2.5, 0.1, "s"));
    private final SliderSetting opacity = add(new SliderSetting("Opacity", 0.85, 0.1, 1.0, 0.05));

    private final List<Circle> circles = new ArrayList<>();
    private boolean wasOnGround = true;
    private double groundY;

    private static final class Circle {
        final Vec3d pos;
        int age;

        Circle(Vec3d pos) {
            this.pos = pos;
        }
    }

    public JumpCircles() {
        super("jump-circles", "Jump Circles", Category.Visuals, false);
    }

    @Override
    public void onTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            circles.clear();
            return;
        }
        boolean onGround = player.isOnGround();
        if (wasOnGround && !onGround && player.getVelocity().y > 0.05) {
            circles.add(new Circle(new Vec3d(player.getX(), groundY + 0.02, player.getZ())));
        }
        if (onGround) {
            groundY = player.getY();
        }
        wasOnGround = onGround;

        int maxAge = durationTicks();
        circles.removeIf(c -> ++c.age > maxAge);
    }

    private int durationTicks() {
        return Math.max(1, (int) Math.round(duration.get() * 20));
    }

    @Override
    public void onDisable() {
        circles.clear();
    }

    @Override
    public void onRender(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
        if (circles.isEmpty()) {
            return;
        }
        Vec3d cam = camera.getPos();
        VertexConsumer vc = consumers.getBuffer(RenderLayer.getDebugQuads());
        float maxAge = durationTicks();

        for (Circle circle : circles) {
            float progress = Math.min(1f, (circle.age + tickDelta) / maxAge);
            float eased = 1f - (float) Math.pow(1f - progress, 3);
            float radius = size.getFloat() * (0.15f + 0.85f * eased);
            float width = Math.min(radius, size.getFloat() * thickness.getFloat() * 0.5f);
            float inner = radius - width;
            float mid = radius - width * 0.5f;
            float alpha = opacity.getFloat() * (1f - progress);

            matrices.push();
            matrices.translate(circle.pos.x - cam.x, circle.pos.y - cam.y, circle.pos.z - cam.z);
            Matrix4f m = matrices.peek().getPositionMatrix();

            for (int i = 0; i < SEGMENTS; i++) {
                float a0 = (float) (i * Math.PI * 2 / SEGMENTS);
                float a1 = (float) ((i + 1) * Math.PI * 2 / SEGMENTS);
                int c0 = color(color, rainbow, i / (float) SEGMENTS);
                int c1 = color(color, rainbow, (i + 1) / (float) SEGMENTS);
                int s0 = RenderUtil.withAlpha(c0, alpha);
                int s1 = RenderUtil.withAlpha(c1, alpha);
                int t0 = RenderUtil.withAlpha(c0, 0f);
                int t1 = RenderUtil.withAlpha(c1, 0f);

                float cos0 = (float) Math.cos(a0), sin0 = (float) Math.sin(a0);
                float cos1 = (float) Math.cos(a1), sin1 = (float) Math.sin(a1);

                // внутренняя мягкая половина кольца
                RenderUtil.quad(vc, m,
                        cos0 * inner, 0, sin0 * inner, t0,
                        cos1 * inner, 0, sin1 * inner, t1,
                        cos1 * mid, 0, sin1 * mid, s1,
                        cos0 * mid, 0, sin0 * mid, s0);
                // внешняя мягкая половина кольца
                RenderUtil.quad(vc, m,
                        cos0 * mid, 0, sin0 * mid, s0,
                        cos1 * mid, 0, sin1 * mid, s1,
                        cos1 * radius, 0, sin1 * radius, t1,
                        cos0 * radius, 0, sin0 * radius, t0);
            }
            matrices.pop();
        }
    }
}
