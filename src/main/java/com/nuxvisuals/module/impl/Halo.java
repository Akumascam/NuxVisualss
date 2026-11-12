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
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

// Светящийся нимб над головой
public class Halo extends Module {
    private static final int SEGMENTS = 48;

    private final ColorSetting color = add(new ColorSetting("Color", 0.13f, 0.55f));
    private final BoolSetting rainbow = add(new BoolSetting("Rainbow", false));
    private final SliderSetting radius = add(new SliderSetting("Radius", 0.35, 0.15, 0.8, 0.05));
    private final SliderSetting thickness = add(new SliderSetting("Thickness", 0.06, 0.02, 0.2, 0.01));
    private final SliderSetting offset = add(new SliderSetting("Y Offset", 0.25, 0.0, 0.8, 0.01));
    private final SliderSetting opacity = add(new SliderSetting("Opacity", 0.85, 0.1, 1.0, 0.05));
    private final BoolSetting everyone = add(new BoolSetting("All Players", false));

    public Halo() {
        super("halo", "Halo", Category.Visuals, false);
    }

    @Override
    public void onRender(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
        MinecraftClient client = mc();
        if (client.world == null || client.player == null) {
            return;
        }
        VertexConsumer vc = consumers.getBuffer(RenderLayer.getDebugQuads());
        Vec3d cam = camera.getPos();
        for (PlayerEntity player : client.world.getPlayers()) {
            boolean self = player == client.player;
            if ((!self && !everyone.get()) || player.isInvisible()) {
                continue;
            }
            if (player == camera.getFocusedEntity() && !camera.isThirdPerson()) {
                continue;
            }
            Vec3d pos = player.getLerpedPos(tickDelta);
            float bob = (float) Math.sin(System.currentTimeMillis() / 600.0) * 0.03f;
            matrices.push();
            matrices.translate(pos.x - cam.x, pos.y - cam.y + player.getHeight() + offset.get() + bob, pos.z - cam.z);
            Matrix4f m = matrices.peek().getPositionMatrix();
            float r = radius.getFloat();
            float t = thickness.getFloat();
            float a = opacity.getFloat();
            for (int i = 0; i < SEGMENTS; i++) {
                float t0 = i / (float) SEGMENTS;
                float t1 = (i + 1) / (float) SEGMENTS;
                double a0 = t0 * Math.PI * 2;
                double a1 = t1 * Math.PI * 2;
                float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0);
                float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
                int col0 = color(color, rainbow, t0);
                int col1 = color(color, rainbow, t1);
                // яркое кольцо
                RenderUtil.quad(vc, m,
                        c0 * (r - t), 0, s0 * (r - t), RenderUtil.withAlpha(col0, a),
                        c1 * (r - t), 0, s1 * (r - t), RenderUtil.withAlpha(col1, a),
                        c1 * (r + t), 0, s1 * (r + t), RenderUtil.withAlpha(col1, a),
                        c0 * (r + t), 0, s0 * (r + t), RenderUtil.withAlpha(col0, a));
                // мягкое свечение снаружи
                RenderUtil.quad(vc, m,
                        c0 * (r + t), 0, s0 * (r + t), RenderUtil.withAlpha(col0, a * 0.4f),
                        c1 * (r + t), 0, s1 * (r + t), RenderUtil.withAlpha(col1, a * 0.4f),
                        c1 * (r + t * 3), 0, s1 * (r + t * 3), RenderUtil.withAlpha(col1, 0f),
                        c0 * (r + t * 3), 0, s0 * (r + t * 3), RenderUtil.withAlpha(col0, 0f));
            }
            matrices.pop();
        }
    }
}
