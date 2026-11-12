package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.ColorSetting;
import com.nuxvisuals.module.setting.ModeSetting;
import com.nuxvisuals.module.setting.SliderSetting;
import com.nuxvisuals.render.RenderUtil;
import com.nuxvisuals.ui.NuxFont;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Падающие снежинки / доллары / звёзды / кубики вокруг игрока + всплеск таких же частиц при ударе
public class Particles extends Module {
    private final ModeSetting mode = add(new ModeSetting("Type", "Snow", "Snow", "Dollars", "Stars", "Cubes"));
    private final ColorSetting color = add(new ColorSetting("Color", 0.58f, 0.15f));
    private final BoolSetting rainbow = add(new BoolSetting("Rainbow", false));
    private final SliderSetting count = add(new SliderSetting("Count", 120, 10, 400, 10));
    private final SliderSetting size = add(new SliderSetting("Size", 0.2, 0.05, 0.6, 0.01));
    private final SliderSetting speed = add(new SliderSetting("Speed", 1.0, 0.2, 3.0, 0.1));
    private final BoolSetting ambient = add(new BoolSetting("Ambient", true));
    private final SliderSetting radius = add(new SliderSetting("Radius", 14, 4, 32, 1)
            .visibleIf(() -> this.ambient.get()));
    // частицы при ударе по сущности
    private final BoolSetting onHit = add(new BoolSetting("On Hit", true));
    private final SliderSetting hitCount = add(new SliderSetting("Hit Count", 14, 3, 50, 1)
            .visibleIf(() -> this.onHit.get()));
    private final SliderSetting hitSpread = add(new SliderSetting("Hit Spread", 1.0, 0.3, 2.5, 0.1)
            .visibleIf(() -> this.onHit.get()));

    public static Particles INSTANCE;
    private final List<P> bursts = new ArrayList<>();

    private final List<P> particles = new ArrayList<>();
    private final Random random = new Random();

    private static final class P {
        double x, y, z, px, py, pz;
        double vx, vy, vz;
        float rot, prevRot, spin, scale, hue, phase;
        int age, life;
        boolean burst;
    }

    public Particles() {
        super("particles", "Particles", Category.Visuals, false);
        INSTANCE = this;
    }

    // вызывается при ударе (AttackEntityCallback в NuxVisualsClient)
    public static void onHit(Entity entity) {
        if (INSTANCE == null || !INSTANCE.isEnabled() || !INSTANCE.onHit.get() || entity == null) {
            return;
        }
        INSTANCE.burst(entity);
    }

    private void burst(Entity e) {
        double cx = e.getX();
        double cy = e.getY() + e.getHeight() * 0.55;
        double cz = e.getZ();
        double w = e.getWidth() * 0.5;
        double k = 0.22 * hitSpread.get();
        int n = hitCount.getInt();
        for (int i = 0; i < n && bursts.size() < 600; i++) {
            P p = new P();
            p.burst = true;
            double a = random.nextDouble() * Math.PI * 2;
            double up = random.nextDouble() * 2 - 1;
            double r = Math.sqrt(1 - up * up);
            p.x = cx + Math.cos(a) * w * random.nextDouble();
            p.y = cy + (random.nextDouble() - 0.5) * e.getHeight() * 0.5;
            p.z = cz + Math.sin(a) * w * random.nextDouble();
            p.px = p.x;
            p.py = p.y;
            p.pz = p.z;
            double sp = k * (0.5 + random.nextDouble() * 0.7);
            p.vx = Math.cos(a) * r * sp;
            p.vy = Math.abs(up) * sp * 0.9 + 0.08;
            p.vz = Math.sin(a) * r * sp;
            p.rot = p.prevRot = random.nextFloat() * 360f;
            p.spin = (random.nextFloat() - 0.5f) * 24f;
            p.scale = 0.55f + random.nextFloat() * 0.5f;
            p.hue = random.nextFloat();
            p.phase = random.nextFloat() * 100f;
            p.life = 18 + random.nextInt(16);
            bursts.add(p);
        }
    }

    @Override
    public void onDisable() {
        particles.clear();
        bursts.clear();
    }

    private void spawn(P p, ClientPlayerEntity player, boolean anywhere) {
        double r = radius.get();
        double angle = random.nextDouble() * Math.PI * 2;
        double dist = Math.sqrt(random.nextDouble()) * r;
        p.x = player.getX() + Math.cos(angle) * dist;
        p.z = player.getZ() + Math.sin(angle) * dist;
        p.y = player.getY() + (anywhere ? -3 + random.nextDouble() * 14 : 8 + random.nextDouble() * 4);
        p.px = p.x;
        p.py = p.y;
        p.pz = p.z;
        p.rot = p.prevRot = random.nextFloat() * 360f;
        p.spin = (random.nextFloat() - 0.5f) * 8f;
        p.scale = 0.7f + random.nextFloat() * 0.6f;
        p.hue = random.nextFloat();
        p.phase = random.nextFloat() * 100f;
        p.age = 0;
    }

    @Override
    public void onTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            particles.clear();
            bursts.clear();
            return;
        }
        // всплески от ударов: разлетаются, тормозятся и плавно исчезают
        for (int i = bursts.size() - 1; i >= 0; i--) {
            P p = bursts.get(i);
            p.px = p.x;
            p.py = p.y;
            p.pz = p.z;
            p.prevRot = p.rot;
            p.age++;
            p.x += p.vx;
            p.y += p.vy;
            p.z += p.vz;
            p.vx *= 0.88;
            p.vz *= 0.88;
            p.vy = p.vy * 0.88 - 0.012;
            p.rot += p.spin;
            p.spin *= 0.95f;
            if (p.age >= p.life) {
                bursts.remove(i);
            }
        }
        if (!ambient.get()) {
            particles.clear();
            return;
        }
        int target = count.getInt();
        while (particles.size() < target) {
            P p = new P();
            spawn(p, player, true);
            particles.add(p);
        }
        while (particles.size() > target) {
            particles.remove(particles.size() - 1);
        }

        double r = radius.get() + 3;
        double fall = 0.045 * speed.get() * (mode.is("Dollars") ? 1.3 : 1.0);
        for (P p : particles) {
            p.px = p.x;
            p.py = p.y;
            p.pz = p.z;
            p.prevRot = p.rot;
            p.age++;
            p.y -= fall * (0.7 + p.scale * 0.3);
            p.x += Math.sin(p.age * 0.05 + p.phase) * 0.015 * speed.get();
            p.z += Math.cos(p.age * 0.04 + p.phase) * 0.015 * speed.get();
            p.rot += p.spin * (float) speed.get();

            double dx = p.x - player.getX();
            double dz = p.z - player.getZ();
            if (p.y < player.getY() - 4 || dx * dx + dz * dz > r * r) {
                spawn(p, player, false);
            }
        }
    }

    @Override
    public void onRender(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
        if (particles.isEmpty() && bursts.isEmpty()) {
            return;
        }
        Vec3d cam = camera.getPos();
        boolean text = mode.is("Dollars");
        TextRenderer tr = mc().textRenderer;
        VertexConsumer vc = text ? null : consumers.getBuffer(RenderLayer.getDebugQuads());

        List<P> all = new ArrayList<>(particles.size() + bursts.size());
        all.addAll(particles);
        all.addAll(bursts);
        for (P p : all) {
            double x = p.px + (p.x - p.px) * tickDelta;
            double y = p.py + (p.y - p.py) * tickDelta;
            double z = p.pz + (p.z - p.pz) * tickDelta;
            float rot = p.prevRot + (p.rot - p.prevRot) * tickDelta;
            float s = size.getFloat() * p.scale;
            float fadeIn = p.burst
                    ? Math.max(0f, Math.min(1f, 1f - (p.age + tickDelta) / p.life)) * Math.min(1f, (p.age + tickDelta) / 2f)
                    : Math.min(1f, (p.age + tickDelta) / 20f);
            if (fadeIn <= 0.01f) {
                continue;
            }
            int base = rainbow.get() ? color(color, rainbow, p.hue) : color.get();
            int c = RenderUtil.withAlpha(base, 0.9f * fadeIn);

            matrices.push();
            matrices.translate(x - cam.x, y - cam.y, z - cam.z);

            if (mode.is("Cubes")) {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rot));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rot * 0.7f));
                matrices.scale(s, s, s);
                // 3D стеклянный кубик: прозрачные грани + светящиеся рёбра
                int face = RenderUtil.withAlpha(base, 0.22f * fadeIn);
                int edge = RenderUtil.withAlpha(RenderUtil.shade(base | 0xFF000000, 1.35f), 0.95f * fadeIn);
                com.nuxvisuals.render.Fx.glassCube(vc, matrices.peek().getPositionMatrix(), 0.5f, face, edge, 0.07f);
            } else {
                matrices.multiply(camera.getRotation());
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rot));
                if (text) {
                    float k = s / 8f;
                    matrices.scale(k, -k, k);
                    Text dollar = NuxFont.of("$");
                    int argb = RenderUtil.withAlpha(base, Math.max(0.1f, 0.95f * fadeIn));
                    tr.draw(dollar, -tr.getWidth(dollar) / 2f, -4f, argb, false,
                            matrices.peek().getPositionMatrix(), consumers,
                            TextRenderer.TextLayerType.NORMAL, 0, RenderUtil.FULL_LIGHT);
                } else {
                    matrices.scale(s, s, s);
                    Matrix4f m = matrices.peek().getPositionMatrix();
                    if (mode.is("Snow")) {
                        snowflake(vc, m, c);
                    } else {
                        star(vc, m, c);
                    }
                }
            }
            matrices.pop();
        }
    }

    private static void snowflake(VertexConsumer vc, Matrix4f m, int c) {
        for (int i = 0; i < 3; i++) {
            float a = (float) (i * Math.PI / 3);
            RenderUtil.rect2d(vc, m, 0, 0, a, 1f, 0.09f, c);
        }
        for (int i = 0; i < 6; i++) {
            float a = (float) (i * Math.PI / 3);
            float bx = (float) Math.cos(a) * 0.3f;
            float by = (float) Math.sin(a) * 0.3f;
            float side = 0.13f;
            RenderUtil.rect2d(vc, m,
                    bx + (float) Math.cos(a + 0.8) * side * 0.5f, by + (float) Math.sin(a + 0.8) * side * 0.5f,
                    a + 0.8f, side, 0.07f, c);
            RenderUtil.rect2d(vc, m,
                    bx + (float) Math.cos(a - 0.8) * side * 0.5f, by + (float) Math.sin(a - 0.8) * side * 0.5f,
                    a - 0.8f, side, 0.07f, c);
        }
    }

    private static void star(VertexConsumer vc, Matrix4f m, int c) {
        for (int i = 0; i < 5; i++) {
            double a = Math.PI / 2 + i * Math.PI * 2 / 5;
            double step = Math.PI / 5;
            float ox = (float) Math.cos(a) * 0.5f, oy = (float) Math.sin(a) * 0.5f;
            float l1x = (float) Math.cos(a - step) * 0.2f, l1y = (float) Math.sin(a - step) * 0.2f;
            float l2x = (float) Math.cos(a + step) * 0.2f, l2y = (float) Math.sin(a + step) * 0.2f;
            RenderUtil.quad(vc, m, 0, 0, 0, c, l1x, l1y, 0, c, ox, oy, 0, c, l2x, l2y, 0, c);
        }
    }

    private static void cube(VertexConsumer vc, Matrix4f m, int c) {
        float h = 0.5f;
        int top = c;
        int side1 = RenderUtil.shade(c, 0.8f);
        int side2 = RenderUtil.shade(c, 0.65f);
        int bottom = RenderUtil.shade(c, 0.5f);
        RenderUtil.quad(vc, m, -h, h, -h, top, -h, h, h, top, h, h, h, top, h, h, -h, top);
        RenderUtil.quad(vc, m, -h, -h, -h, bottom, h, -h, -h, bottom, h, -h, h, bottom, -h, -h, h, bottom);
        RenderUtil.quad(vc, m, -h, -h, -h, side1, -h, h, -h, side1, h, h, -h, side1, h, -h, -h, side1);
        RenderUtil.quad(vc, m, -h, -h, h, side1, h, -h, h, side1, h, h, h, side1, -h, h, h, side1);
        RenderUtil.quad(vc, m, -h, -h, -h, side2, -h, -h, h, side2, -h, h, h, side2, -h, h, -h, side2);
        RenderUtil.quad(vc, m, h, -h, -h, side2, h, h, -h, side2, h, h, h, side2, h, -h, h, side2);
    }
}
