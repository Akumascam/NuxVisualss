package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.ColorSetting;
import com.nuxvisuals.module.setting.SliderSetting;
import com.nuxvisuals.render.Fx;
import com.nuxvisuals.render.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

// Дистанция удара: круг радиусом 3 блока под ногами.
// Когда кто-то в зоне досягаемости — круг загорается одним цветом, иначе — другим.
public class HitDistance extends Module {
    private final SliderSetting range = add(new SliderSetting("Range", 3.0, 2.0, 6.0, 0.1, " bl"));
    private final ColorSetting inColor = add(new ColorSetting("In Range Color", 0.36f, 0.75f));
    private final ColorSetting outColor = add(new ColorSetting("Out Of Range Color", 0.0f, 0.7f));
    private final BoolSetting onlyPlayers = add(new BoolSetting("Only Players", true));
    private final BoolSetting fill = add(new BoolSetting("Fill", true));
    private final BoolSetting wall = add(new BoolSetting("Glow Wall", true));
    private final SliderSetting thickness = add(new SliderSetting("Thickness", 0.06, 0.02, 0.2, 0.01));

    private float mix; // 0 = вне досягаемости, 1 = в досягаемости (плавно)

    public HitDistance() {
        super("hit-distance", "Hit Distance", Category.Utilities, false);
    }

    private boolean targetInRange(ClientPlayerEntity player, float tickDelta) {
        double r = range.get();
        Vec3d eye = player.getCameraPosVec(tickDelta);
        Box area = player.getBoundingBox().expand(r + 1);
        for (Entity e : player.getWorld().getOtherEntities(player, area)) {
            if (!(e instanceof LivingEntity living) || !living.isAlive() || e instanceof ArmorStandEntity
                    || e.isSpectator() || e.isInvisibleTo(player)) {
                continue;
            }
            if (onlyPlayers.get() && !(e instanceof PlayerEntity)) {
                continue;
            }
            // дистанция удара считается от глаз до ближайшей точки хитбокса
            Box b = e.getBoundingBox();
            double cx = Math.max(b.minX, Math.min(eye.x, b.maxX));
            double cy = Math.max(b.minY, Math.min(eye.y, b.maxY));
            double cz = Math.max(b.minZ, Math.min(eye.z, b.maxZ));
            if (eye.squaredDistanceTo(cx, cy, cz) <= r * r) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRender(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null || player.isSpectator()) {
            return;
        }
        boolean in = targetInRange(player, tickDelta);
        mix += ((in ? 1f : 0f) - mix) * 0.25f;
        int base = com.nuxvisuals.ui.NuxShapes.blend(outColor.get(), inColor.get(), mix);

        Vec3d cam = camera.getPos();
        Vec3d feet = player.getLerpedPos(tickDelta);
        float x = (float) (feet.x - cam.x);
        float y = (float) (feet.y - cam.y) + 0.02f;
        float z = (float) (feet.z - cam.z);
        float r = range.getFloat();
        float t = thickness.getFloat();

        Matrix4f m = matrices.peek().getPositionMatrix();
        VertexConsumer vc = consumers.getBuffer(RenderLayer.getDebugQuads());
        int seg = 72;
        int ring = RenderUtil.withAlpha(base, 0.95f);
        int fillIn = RenderUtil.withAlpha(base, 0.04f + 0.1f * mix);
        int fillOut = RenderUtil.withAlpha(base, 0.16f + 0.12f * mix);
        int wallBottom = RenderUtil.withAlpha(base, 0.25f + 0.3f * mix);
        int wallTop = base & 0xFFFFFF;
        float wallH = 0.25f + 0.25f * mix;
        for (int i = 0; i < seg; i++) {
            double a1 = i * Math.PI * 2 / seg;
            double a2 = (i + 1) * Math.PI * 2 / seg;
            float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            float c2 = (float) Math.cos(a2), s2 = (float) Math.sin(a2);
            if (fill.get()) {
                // к краю заливка гуще — выглядит объёмнее
                Fx.quad(vc, m, x, y, z, fillIn, x + c1 * r, y, z + s1 * r, fillOut,
                        x + c2 * r, y, z + s2 * r, fillOut, x, y, z, fillIn);
            }
            Fx.quad(vc, m, x + c1 * r, y, z + s1 * r, ring, x + c2 * r, y, z + s2 * r, ring,
                    x + c2 * (r - t), y, z + s2 * (r - t), ring, x + c1 * (r - t), y, z + s1 * (r - t), ring);
            if (wall.get()) {
                Fx.quad(vc, m, x + c1 * r, y, z + s1 * r, wallBottom, x + c2 * r, y, z + s2 * r, wallBottom,
                        x + c2 * r, y + wallH, z + s2 * r, wallTop, x + c1 * r, y + wallH, z + s1 * r, wallTop);
            }
        }
    }
}
