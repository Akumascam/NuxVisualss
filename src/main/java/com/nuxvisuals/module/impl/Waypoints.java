package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.SliderSetting;
import com.nuxvisuals.render.RenderUtil;
import com.nuxvisuals.waypoint.Waypoint;
import com.nuxvisuals.waypoint.WaypointManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

// Рисует точки в мире: луч и подпись с расстоянием (видно сквозь стены)
public class Waypoints extends Module {
    private final BoolSetting beam = add(new BoolSetting("Beam", true));
    private final BoolSetting labels = add(new BoolSetting("Labels", true));
    private final BoolSetting distance = add(new BoolSetting("Show Distance", true));
    private final SliderSetting scale = add(new SliderSetting("Scale", 1.0, 0.5, 2.0, 0.1, "x"));

    public Waypoints() {
        super("waypoints", "Waypoints", Category.Waypoint, true);
    }

    private static void beamPart(VertexConsumer vc, Matrix4f m, float w, float top, int color) {
        int topC = color & 0x00FFFFFF;
        RenderUtil.quad(vc, m, -w, 0, -w, color, w, 0, -w, color, w, top, -w, topC, -w, top, -w, topC);
        RenderUtil.quad(vc, m, w, 0, w, color, -w, 0, w, color, -w, top, w, topC, w, top, w, topC);
        RenderUtil.quad(vc, m, -w, 0, w, color, -w, 0, -w, color, -w, top, -w, topC, -w, top, w, topC);
        RenderUtil.quad(vc, m, w, 0, -w, color, w, 0, w, color, w, top, w, topC, w, top, -w, topC);
    }

    @Override
    public void onRender(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
        MinecraftClient client = mc();
        if (client.world == null) {
            return;
        }
        String dim = WaypointManager.dimension();
        Vec3d cam = camera.getPos();
        for (Waypoint wp : WaypointManager.current()) {
            if (!wp.visible || !wp.dimension.equals(dim)) {
                continue;
            }
            int color = wp.color;
            double dx = wp.x + 0.5 - cam.x;
            double dy = wp.y - cam.y;
            double dz = wp.z + 0.5 - cam.z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (beam.get() && dist < 512) {
                VertexConsumer vc = consumers.getBuffer(RenderLayer.getDebugQuads());
                matrices.push();
                matrices.translate(dx, dy, dz);
                Matrix4f m = matrices.peek().getPositionMatrix();
                float top = 200f;
                // яркая середина + мягкое свечение
                beamPart(vc, m, 0.05f, top, RenderUtil.withAlpha(color, 0.8f));
                beamPart(vc, m, 0.22f, top, RenderUtil.withAlpha(color, 0.18f));
                // кольцо на земле
                int ring = RenderUtil.withAlpha(color, 0.5f);
                for (int i = 0; i < 32; i++) {
                    double a0 = i * Math.PI * 2 / 32;
                    double a1 = (i + 1) * Math.PI * 2 / 32;
                    float r1 = 0.45f;
                    float r2 = 0.55f;
                    RenderUtil.quad(vc, m,
                            (float) Math.cos(a0) * r1, 0.02f, (float) Math.sin(a0) * r1, ring,
                            (float) Math.cos(a1) * r1, 0.02f, (float) Math.sin(a1) * r1, ring,
                            (float) Math.cos(a1) * r2, 0.02f, (float) Math.sin(a1) * r2, ring,
                            (float) Math.cos(a0) * r2, 0.02f, (float) Math.sin(a0) * r2, ring);
                }
                matrices.pop();
            }

            if (labels.get()) {
                // подпись ставим не дальше 48 блоков, чтобы её было видно
                double k = dist > 48 ? 48 / dist : 1;
                double lx = dx * k;
                double ly = (dy + 1.8) * k;
                double lz = dz * k;
                double labelDist = dist * k;
                float s = (float) (0.02 * Math.max(1, labelDist / 6) * scale.get());

                TextRenderer tr = client.textRenderer;
                matrices.push();
                matrices.translate(lx, ly, lz);
                matrices.multiply(camera.getRotation());
                matrices.scale(s, -s, s);
                Matrix4f m = matrices.peek().getPositionMatrix();
                String icon = "◆";
                tr.draw(icon, -tr.getWidth(icon) / 2f, -12, 0xFF000000 | color, false, m, consumers,
                        TextRenderer.TextLayerType.SEE_THROUGH, 0, RenderUtil.FULL_LIGHT);
                String name = " " + wp.name + " ";
                tr.draw(name, -tr.getWidth(name) / 2f, 0, 0xFFFFFFFF, false, m, consumers,
                        TextRenderer.TextLayerType.SEE_THROUGH, 0xA0101420, RenderUtil.FULL_LIGHT);
                if (distance.get()) {
                    String d = " " + Math.round(dist) + " м ";
                    tr.draw(d, -tr.getWidth(d) / 2f, 10, 0xFF000000 | RenderUtil.shade(0xFF000000 | color, 0.9f), false, m, consumers,
                            TextRenderer.TextLayerType.SEE_THROUGH, 0x80101420, RenderUtil.FULL_LIGHT);
                }
                matrices.pop();
            }
        }
    }
}
