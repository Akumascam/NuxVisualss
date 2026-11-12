package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.ColorSetting;
import com.nuxvisuals.module.setting.ModeSetting;
import com.nuxvisuals.module.setting.SliderSetting;
import com.nuxvisuals.render.Fx;
import com.nuxvisuals.render.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Random;

// Квадратик на игроке, когда на него наведён прицел.
// Цель берётся из обычного ванильного прицела (та же дистанция, что и для удара),
// сквозь стены не видно, ничего не делает за игрока — только подсветка.
public class TargetESP extends Module {
    private final ModeSetting style = add(new ModeSetting("Style", "Diamond", "Diamond", "Lightning", "Square", "Corners", "Призраки", "Кристаллы"));
    private final ColorSetting color = add(new ColorSetting("Color", 0.6f, 0.75f));
    private final BoolSetting rainbow = add(new BoolSetting("Rainbow", false));
    private final SliderSetting size = add(new SliderSetting("Size", 1.0, 0.4, 2.0, 0.05));
    private final SliderSetting thickness = add(new SliderSetting("Thickness", 0.06, 0.02, 0.2, 0.01));
    private final SliderSetting spinSpeed = add(new SliderSetting("Spin Speed", 2, 0, 10, 0.5));

    private final BoolSetting glowSetting = add(new BoolSetting("Glow", true));
    private final SliderSetting effectCount = add(new SliderSetting("Количество", 14, 6, 24, 1)
            .visibleIf(() -> style.is("Кристаллы")));
    private final SliderSetting holdTime = add(new SliderSetting("Удержание цели", 1, 0, 5, 0.1, " сек"));
    private static TargetESP instance;
    private int holdTicks;
    private final Random random = new Random();
    private Entity target;
    private float visibility;
    private float prevVisibility;

    public TargetESP() {
        super("target-esp", "Target ESP", Category.Visuals, false);
        instance = this;
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (client.world == null || client.player == null || (target != null && !validTarget(client, target))) {
            clearTarget();
        }
        prevVisibility = visibility;
        Entity looked = client.targetedEntity;
        boolean show = validTarget(client, looked);
        if (show) holdTicks = Math.round(holdTime.getFloat() * 20);
        else if (holdTicks > 0 && validTarget(client, target)) { holdTicks--; show = true; }
        if (validTarget(client, looked)) {
            target = looked;
        }
        visibility = Math.max(0f, Math.min(1f, visibility + (show ? 0.34f : -0.2f)));
        if (visibility == 0f) {
            target = null;
        }
    }

    @Override
    public void onRender(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
        if (!validTarget(mc(), target)) {
            clearTarget();
            return;
        }
        float vis = prevVisibility + (visibility - prevVisibility) * tickDelta;
        if (vis <= 0.01f) {
            return;
        }
        Vec3d cam = camera.getPos();
        if (style.is("Diamond") || style.is("Lightning") || style.is("Призраки") || style.is("Кристаллы")) {
            Vec3d feet = target.getLerpedPos(tickDelta);
            float fx = (float) (feet.x - cam.x), fy = (float) (feet.y - cam.y), fz = (float) (feet.z - cam.z);
            Quaternionf q = camera.getRotation();
            Vector3f right = q.transform(new Vector3f(1, 0, 0));
            Vector3f up = q.transform(new Vector3f(0, 1, 0));
            Matrix4f m = matrices.peek().getPositionMatrix();
            VertexConsumer vc = consumers.getBuffer(RenderLayer.getDebugQuads());
            int base = color(color, rainbow, 0f);
            if (style.is("Diamond")) {
                diamond(vc, m, fx, fy, fz, vis, base, right, up);
            } else if (style.is("Призраки")) {
                ghosts(vc, m, fx, fy, fz, vis, base, right, up);
            } else if (style.is("Кристаллы")) {
                crystals(vc, m, fx, fy, fz, vis, base, right, up);
            } else {
                lightning(vc, m, fx, fy, fz, vis, base, right, up);
            }
            return;
        }
        Vec3d center = target.getLerpedPos(tickDelta).add(0, target.getHeight() / 2.0, 0);
        // чуть ближе к камере, чтобы квадрат лежал поверх модели, а не внутри неё
        Vec3d pos = center.add(cam.subtract(center).normalize().multiply(target.getWidth() * 0.6));

        float half = size.getFloat() * target.getHeight() * 0.32f * (0.85f + 0.15f * vis);
        float t = thickness.getFloat();
        float angle = (System.currentTimeMillis() % 3_600_000L) / 1000f * spinSpeed.getFloat() * 30f;

        matrices.push();
        matrices.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);
        matrices.multiply(camera.getRotation());
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
        Matrix4f m = matrices.peek().getPositionMatrix();
        VertexConsumer vc = consumers.getBuffer(RenderLayer.getDebugQuads());

        int base = color(color, rainbow, 0f);
        frame(vc, m, half + t, t * 3f, RenderUtil.withAlpha(base, 0.15f * vis));
        frame(vc, m, half, t, RenderUtil.withAlpha(base, 0.95f * vis));
        matrices.pop();
    }

    private static boolean validTarget(MinecraftClient client, Entity entity) {
        return client.world != null && client.player != null && entity instanceof PlayerEntity player
                && player != client.player && !player.isSpectator() && player.isAlive() && !player.isRemoved()
                && player.getWorld() == client.world && !player.isInvisible()
                && !player.isInvisibleTo(client.player) && client.player.canSee(player);
    }

    private void clearTarget() {
        target = null;
        holdTicks = 0;
        visibility = prevVisibility = 0f;
    }

    @Override
    public void onDisable() {
        clearTarget();
    }

    // «Ромб» (кристалл) над головой: медленно вращается и покачивается, полупрозрачные грани + светлые рёбра
    private void diamond(VertexConsumer vc, Matrix4f m, float fx, float fy, float fz, float vis, int base,
                         Vector3f right, Vector3f up) {
        float time = (System.currentTimeMillis() % 3_600_000L) / 1000f;
        float k = size.getFloat() * (0.8f + 0.2f * vis);
        float r = 0.2f * k;
        float hTop = 0.22f * k;
        float hBot = 0.34f * k;
        float cx = fx, cz = fz;
        float cy = fy + target.getHeight() + 0.45f * k + 0.06f * (float) Math.sin(time * 2.2f);
        float ang = time * spinSpeed.getFloat() * 0.6f;

        float[][] ring = new float[4][];
        for (int i = 0; i < 4; i++) {
            double a = ang + i * Math.PI / 2;
            ring[i] = new float[]{cx + (float) Math.cos(a) * r, cy, cz + (float) Math.sin(a) * r};
        }
        float[] top = {cx, cy + hTop, cz};
        float[] bot = {cx, cy - hBot, cz};

        if (glowSetting.get()) {
            Fx.glow(vc, m, cx, cy, cz, 0.6f * k, RenderUtil.withAlpha(base, 0.28f * vis),
                    right.x, right.y, right.z, up.x, up.y, up.z);
        }
        for (int i = 0; i < 4; i++) {
            float[] p1 = ring[i];
            float[] p2 = ring[(i + 1) % 4];
            float shade = i % 2 == 0 ? 1f : 0.75f;
            int cTop = RenderUtil.withAlpha(RenderUtil.shade(base | 0xFF000000, shade * 1.1f), 0.55f * vis);
            int cBot = RenderUtil.withAlpha(RenderUtil.shade(base | 0xFF000000, shade * 0.7f), 0.5f * vis);
            Fx.quad(vc, m, top[0], top[1], top[2], cTop, p1[0], p1[1], p1[2], cTop, p2[0], p2[1], p2[2], cTop,
                    top[0], top[1], top[2], cTop);
            Fx.quad(vc, m, bot[0], bot[1], bot[2], cBot, p2[0], p2[1], p2[2], cBot, p1[0], p1[1], p1[2], cBot,
                    bot[0], bot[1], bot[2], cBot);
        }
        int edge = RenderUtil.withAlpha(RenderUtil.shade(base | 0xFF000000, 1.4f), 0.95f * vis);
        float w = thickness.getFloat() * 0.35f * k;
        for (int i = 0; i < 4; i++) {
            float[] p1 = ring[i];
            float[] p2 = ring[(i + 1) % 4];
            Fx.ribbon(vc, m, p1[0], p1[1], p1[2], p2[0], p2[1], p2[2], w, edge, edge, 0, 0, 0);
            Fx.ribbon(vc, m, top[0], top[1], top[2], p1[0], p1[1], p1[2], w, edge, edge, 0, 0, 0);
            Fx.ribbon(vc, m, bot[0], bot[1], bot[2], p1[0], p1[1], p1[2], w, edge, edge, 0, 0, 0);
        }
        // кольцо под ромбом
        int n = 24;
        float rr = 0.36f * k;
        float ry = cy - hBot - 0.06f;
        int ringColor = RenderUtil.withAlpha(base, 0.7f * vis);
        for (int i = 0; i < n; i++) {
            double a1 = -ang * 1.5 + i * Math.PI * 2 / n;
            double a2 = -ang * 1.5 + (i + 0.6) * Math.PI * 2 / n;
            Fx.ribbon(vc, m, cx + (float) Math.cos(a1) * rr, ry, cz + (float) Math.sin(a1) * rr,
                    cx + (float) Math.cos(a2) * rr, ry, cz + (float) Math.sin(a2) * rr, w * 0.8f,
                    ringColor, ringColor, 0, 0, 0);
        }
    }

    // Молнии: несколько разрядов обвивают игрока снизу вверх и мигают
    private void lightning(VertexConsumer vc, Matrix4f m, float fx, float fy, float fz, float vis, int base,
                           Vector3f right, Vector3f up) {
        float time = (System.currentTimeMillis() % 3_600_000L) / 1000f;
        float k = size.getFloat();
        float h = target.getHeight();
        float r = target.getWidth() * 0.75f * k;
        random.setSeed((long) (time * 12f) * 31L + target.getId());
        int core = RenderUtil.withAlpha(0xFFFFFF, 0.95f * vis);
        int glowColor = RenderUtil.withAlpha(base, 0.35f * vis);
        float w = thickness.getFloat() * 0.4f;
        int bolts = 4;
        for (int b = 0; b < bolts; b++) {
            float a1 = time * spinSpeed.getFloat() * 0.5f + b * 6.283f / bolts;
            float a2 = a1 + 1.4f + random.nextFloat();
            float y1 = fy + random.nextFloat() * h * 0.3f;
            float y2 = fy + h * (0.7f + random.nextFloat() * 0.4f);
            float sx = fx + (float) Math.cos(a1) * r, sz = fz + (float) Math.sin(a1) * r;
            float ex = fx + (float) Math.cos(a2) * r, ez = fz + (float) Math.sin(a2) * r;
            // промежуточная точка на окружности, чтобы разряд «обвивал» игрока
            float am = (a1 + a2) / 2;
            float mx = fx + (float) Math.cos(am) * r * 1.1f, mz = fz + (float) Math.sin(am) * r * 1.1f;
            float my = (y1 + y2) / 2;
            Fx.bolt(vc, m, random, sx, y1, sz, mx, my, mz, 5, 0.12f * k, w, core, glowColor, 0, 0, 0);
            Fx.bolt(vc, m, random, mx, my, mz, ex, y2, ez, 5, 0.12f * k, w, core, glowColor, 0, 0, 0);
            if (glowSetting.get()) {
                Fx.glow(vc, m, ex, y2, ez, 0.18f * k, RenderUtil.withAlpha(base, 0.6f * vis),
                        right.x, right.y, right.z, up.x, up.y, up.z);
            }
        }
        if (glowSetting.get()) {
            Fx.glow(vc, m, fx, fy + h / 2, fz, h * 0.6f * k, RenderUtil.withAlpha(base, 0.12f * vis),
                    right.x, right.y, right.z, up.x, up.y, up.z);
        }
    }

    public static void onAttack(PlayerEntity player, Entity entity) {
        if (instance != null && instance.isEnabled() && player == mc().player && validTarget(mc(), entity)) {
            instance.target = entity;
            instance.holdTicks = Math.round(instance.holdTime.getFloat() * 20);
        }
    }

    // Three smoothly moving luminous heads with tapered tails; camera-facing soft disks.
    private void ghosts(VertexConsumer vc, Matrix4f m, float x, float y, float z, float vis, int base,
                        Vector3f right, Vector3f up) {
        float time = (System.nanoTime() % 3_600_000_000_000L) / 1_000_000_000f;
        float radius = (target.getWidth() * 0.75f + 0.18f) * size.getFloat();
        for (int band = 0; band < 3; band++) {
            for (int j = 23; j >= 0; j--) {
                float phase = time * spinSpeed.getFloat() * 0.8f - j * 0.065f + band * 2.0944f;
                float life = 1f - j / 24f;
                float gx = x + (float) Math.cos(phase) * radius;
                float gz = z + (float) Math.sin(phase) * radius;
                float gy = y + target.getHeight() * (0.18f + band * 0.3f)
                        + (float) Math.sin(phase * 1.8f + band) * target.getHeight() * 0.12f;
                float r = size.getFloat() * (0.025f + life * 0.13f);
                if (glowSetting.get()) Fx.glow(vc, m, gx, gy, gz, r * 2.6f,
                        RenderUtil.withAlpha(base, vis * life * life * 0.23f),
                        right.x, right.y, right.z, up.x, up.y, up.z);
                Fx.glow(vc, m, gx, gy, gz, r,
                        RenderUtil.withAlpha(base, vis * life * life * 0.85f),
                        right.x, right.y, right.z, up.x, up.y, up.z);
                if (j < 3) Fx.glow(vc, m, gx, gy, gz, r * 0.45f,
                        RenderUtil.withAlpha(0xFFFFFF, vis * life * 0.9f),
                        right.x, right.y, right.z, up.x, up.y, up.z);
            }
        }
    }

    private void crystals(VertexConsumer vc, Matrix4f m, float x, float y, float z, float vis, int base,
                          Vector3f right, Vector3f up) {
        float time = (System.nanoTime() % 3_600_000_000_000L) / 1_000_000_000f;
        int count = effectCount.getInt();
        for (int i = 0; i < count; i++) {
            float phase = i * 2.399963f + time * spinSpeed.getFloat() * (i % 2 == 0 ? 0.28f : -0.2f);
            float radius = (target.getWidth() * 0.8f + 0.18f) * size.getFloat()
                    * (0.9f + 0.12f * (float) Math.sin(time + i));
            float cx = x + (float) Math.cos(phase) * radius;
            float cz = z + (float) Math.sin(phase) * radius;
            float cy = y + target.getHeight() * (0.08f + 0.98f * i / Math.max(1, count - 1))
                    + (float) Math.sin(time * 1.7f + i) * 0.075f;
            float scale = size.getFloat() * (0.075f + (i % 4) * 0.018f);
            if (glowSetting.get()) Fx.glow(vc, m, cx, cy, cz, scale * 2.4f,
                    RenderUtil.withAlpha(base, 0.2f * vis), right.x, right.y, right.z, up.x, up.y, up.z);
            float[][] ring = new float[4][];
            for (int k = 0; k < 4; k++) {
                double a = phase * 1.5 + k * Math.PI / 2;
                ring[k] = new float[]{cx + (float) Math.cos(a) * scale, cy, cz + (float) Math.sin(a) * scale};
            }
            for (int k = 0; k < 4; k++) {
                float[] a = ring[k], b = ring[(k + 1) % 4];
                int upper = RenderUtil.withAlpha(RenderUtil.shade(base, k % 2 == 0 ? 1.3f : 0.9f), vis * 0.8f);
                int lower = RenderUtil.withAlpha(RenderUtil.shade(base, k % 2 == 0 ? 0.85f : 0.55f), vis * 0.75f);
                Fx.quad(vc, m, cx, cy + scale * 1.5f, cz, upper, a[0], a[1], a[2], upper,
                        b[0], b[1], b[2], upper, cx, cy + scale * 1.5f, cz, upper);
                Fx.quad(vc, m, cx, cy - scale * 1.5f, cz, lower, b[0], b[1], b[2], lower,
                        a[0], a[1], a[2], lower, cx, cy - scale * 1.5f, cz, lower);
            }
        }
    }

    private void frame(VertexConsumer vc, Matrix4f m, float half, float t, int c) {
        if (style.is("Corners")) {
            float len = half * 0.5f;
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sy = -1; sy <= 1; sy += 2) {
                    float cx = sx * half;
                    float cy = sy * half;
                    RenderUtil.box2d(vc, m, Math.min(cx, cx - sx * len), Math.min(cy, cy - sy * t),
                            Math.max(cx, cx - sx * len), Math.max(cy, cy - sy * t), c);
                    RenderUtil.box2d(vc, m, Math.min(cx, cx - sx * t), Math.min(cy, cy - sy * len),
                            Math.max(cx, cx - sx * t), Math.max(cy, cy - sy * len), c);
                }
            }
        } else {
            RenderUtil.box2d(vc, m, -half, half - t, half, half, c);
            RenderUtil.box2d(vc, m, -half, -half, half, -half + t, c);
            RenderUtil.box2d(vc, m, -half, -half + t, -half + t, half - t, c);
            RenderUtil.box2d(vc, m, half - t, -half + t, half, half - t, c);
        }
    }
}
