package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.ColorSetting;
import com.nuxvisuals.module.setting.SliderSetting;
import com.nuxvisuals.render.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

// Полупрозрачная «китайская шляпа» (конус) над головой
public class ChinaHat extends Module {
    private static final int SEGMENTS = 48;

    private final ColorSetting color = add(new ColorSetting("Color", 0.6f, 0.7f));
    private final BoolSetting rainbow = add(new BoolSetting("Rainbow", false));
    private final SliderSetting radius = add(new SliderSetting("Radius", 0.65, 0.3, 1.5, 0.05));
    private final SliderSetting height = add(new SliderSetting("Height", 0.3, 0.05, 0.8, 0.05));
    private final SliderSetting offset = add(new SliderSetting("Y Offset", 0.05, -0.3, 0.5, 0.01));
    private final SliderSetting opacity = add(new SliderSetting("Opacity", 0.45, 0.1, 1.0, 0.05));
    private final BoolSetting outline = add(new BoolSetting("Outline", true));
    private final BoolSetting spin = add(new BoolSetting("Spin", true));
    private final BoolSetting everyone = add(new BoolSetting("All Players", false));

    public ChinaHat() {
        super("china-hat", "China Hat", Category.Visuals, true);
    }

    @Override
    public void onRender(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
        MinecraftClient client = mc();
        if (client.world == null || client.player == null) {
            return;
        }
        VertexConsumer vc = consumers.getBuffer(RenderLayer.getDebugQuads());
        for (PlayerEntity player : client.world.getPlayers()) {
            boolean self = player == client.player;
            if (!self && !everyone.get()) {
                continue;
            }
            if (player.isInvisible()) {
                continue;
            }
            // от первого лица свою шляпу не рисуем (раньше была видна, если смотреть вверх)
            if (self && !camera.isThirdPerson()) {
                continue;
            }
            if (player == camera.getFocusedEntity() && !camera.isThirdPerson()) {
                continue;
            }
            renderHat(matrices, vc, camera, player, tickDelta);
        }
    }

    private void renderHat(MatrixStack matrices, VertexConsumer vc, Camera camera, PlayerEntity player, float tickDelta) {
        Vec3d cam = camera.getPos();
        Vec3d pos = player.getLerpedPos(tickDelta);
        float yaw = player.getYaw(tickDelta);
        float spinAngle = spin.get() ? (System.currentTimeMillis() % 6000L) / 6000f * 360f : 0f;

        matrices.push();
        matrices.translate(pos.x - cam.x, pos.y - cam.y + player.getHeight() + offset.get(), pos.z - cam.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw + spinAngle));
        Matrix4f m = matrices.peek().getPositionMatrix();

        float r = radius.getFloat();
        float h = height.getFloat();
        float a = opacity.getFloat();
        for (int i = 0; i < SEGMENTS; i++) {
            float t0 = i / (float) SEGMENTS;
            float t1 = (i + 1) / (float) SEGMENTS;
            double a0 = t0 * Math.PI * 2;
            double a1 = t1 * Math.PI * 2;
            float x0 = (float) Math.cos(a0) * r, z0 = (float) Math.sin(a0) * r;
            float x1 = (float) Math.cos(a1) * r, z1 = (float) Math.sin(a1) * r;
            int c0 = color(color, rainbow, t0);
            int c1 = color(color, rainbow, t1);
            int top = color(color, rainbow, (t0 + t1) / 2f);

            // поверхность конуса (треугольник = четырёхугольник с повтором вершины)
            RenderUtil.quad(vc, m,
                    0, h, 0, RenderUtil.withAlpha(top, a * 0.6f),
                    0, h, 0, RenderUtil.withAlpha(top, a * 0.6f),
                    x1, 0, z1, RenderUtil.withAlpha(c1, a),
                    x0, 0, z0, RenderUtil.withAlpha(c0, a));

            if (outline.get()) {
                // яркий ободок по краю
                float ri = r - 0.035f;
                float x0i = (float) Math.cos(a0) * ri, z0i = (float) Math.sin(a0) * ri;
                float x1i = (float) Math.cos(a1) * ri, z1i = (float) Math.sin(a1) * ri;
                float dy = h * 0.035f / r;
                RenderUtil.quad(vc, m,
                        x0i, dy, z0i, RenderUtil.withAlpha(c0, 1f),
                        x1i, dy, z1i, RenderUtil.withAlpha(c1, 1f),
                        x1, 0, z1, RenderUtil.withAlpha(c1, 1f),
                        x0, 0, z0, RenderUtil.withAlpha(c0, 1f));
            }
        }
        matrices.pop();
    }
}
