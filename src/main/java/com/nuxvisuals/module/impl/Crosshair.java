package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.ColorSetting;
import com.nuxvisuals.module.setting.ModeSetting;
import com.nuxvisuals.module.setting.SliderSetting;
import com.nuxvisuals.ui.NuxShapes;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

// Свой прицел. «Pack» — классический прицел из PvP-ресурспаков: тонкий крест с зазором и точкой.
public class Crosshair extends Module {
    public static Crosshair INSTANCE;

    private final ModeSetting mode = add(new ModeSetting("Mode", "Pack",
            "Pack", "Pack Circle", "Cross", "Dot", "Circle", "Circle Dot", "T-Shape", "X", "Arrow", "Custom"));
    private final ColorSetting color = add(new ColorSetting("Color", 0f, 0f, 1f));
    private final BoolSetting rainbow = add(new BoolSetting("Rainbow", false));
    private final BoolSetting outline = add(new BoolSetting("Outline", true));
    private final SliderSetting scale = add(new SliderSetting("Scale", 1, 0.5, 3, 0.25));
    private final BoolSetting dynamic = add(new BoolSetting("Dynamic Gap", false));
    private final BoolSetting attackBar = add(new BoolSetting("Attack Indicator", true));
    private final BoolSetting thirdPerson = add(new BoolSetting("Show In 3rd Person", false));
    // свой прицел
    private final SliderSetting length = add(new SliderSetting("Length", 5, 1, 20, 1))
            .visibleIf(() -> this.mode.is("Custom"));
    private final SliderSetting thickness = add(new SliderSetting("Thickness", 1, 1, 6, 1))
            .visibleIf(() -> this.mode.is("Custom"));
    private final SliderSetting gap = add(new SliderSetting("Gap", 3, 0, 15, 1))
            .visibleIf(() -> this.mode.is("Custom"));
    private final BoolSetting dot = add(new BoolSetting("Center Dot", true))
            .visibleIf(() -> this.mode.is("Custom"));
    private final BoolSetting tStyle = add(new BoolSetting("T-Style", false))
            .visibleIf(() -> this.mode.is("Custom"));
    private final BoolSetting ring = add(new BoolSetting("Ring", false))
            .visibleIf(() -> this.mode.is("Custom"));

    private float dynGap;
    private int pass;

    public Crosshair() {
        super("crosshair", "Crosshair", Category.Visuals, false);
        INSTANCE = this;
    }

    // вызывается вместо ванильного прицела; true — ванильный не рисуем
    public static boolean draw(DrawContext context) {
        if (INSTANCE == null || !INSTANCE.isEnabled()) {
            return false;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return true;
        }
        if (!client.options.getPerspective().isFirstPerson() && !INSTANCE.thirdPerson.get()) {
            return true;
        }
        INSTANCE.render(context, client);
        return true;
    }

    private void render(DrawContext ctx, MinecraftClient client) {
        int s = NuxShapes.guiScale();
        int w = ctx.getScaledWindowWidth() * s;
        int h = ctx.getScaledWindowHeight() * s;
        int cx = w / 2;
        int cy = h / 2;
        float sc = scale.getFloat() * Math.max(1, s / 2f);
        int c = color(color, rainbow, 0f);

        float speed = (float) client.player.getVelocity().horizontalLength();
        float targetGap = dynamic.get() ? Math.min(8f, speed * 25f + (client.player.isOnGround() ? 0 : 3f)) : 0f;
        dynGap += (targetGap - dynGap) * 0.25f;
        int dg = Math.round(dynGap * sc);

        ctx.getMatrices().push();
        ctx.getMatrices().scale(1f / s, 1f / s, 1f);
        // сначала вся обводка, потом сам прицел — чтобы обводка не перекрывала соседние части
        for (pass = outline.get() ? 0 : 1; pass < 2; pass++) {
        switch (mode.get()) {
            case "Pack" -> {
                int t = Math.max(1, Math.round(sc));
                int g = Math.round(2 * sc) + dg;
                int l = Math.round(4 * sc);
                cross(ctx, cx, cy, l, t, g, c, false);
                box(ctx, cx - t / 2, cy - t / 2, t, t, c);
            }
            case "Pack Circle" -> {
                int t = Math.max(1, Math.round(sc));
                ringShape(ctx, cx, cy, Math.round(6 * sc) + dg, t, c);
                box(ctx, cx - t / 2, cy - t / 2, t, t, c);
            }
            case "Cross" -> cross(ctx, cx, cy, Math.round(6 * sc), Math.max(1, Math.round(1.5f * sc)), dg, c, false);
            case "Dot" -> {
                int d = Math.max(2, Math.round(3 * sc));
                box(ctx, cx - d / 2, cy - d / 2, d, d, c);
            }
            case "Circle" -> ringShape(ctx, cx, cy, Math.round(5 * sc) + dg, Math.max(1, Math.round(sc)), c);
            case "Circle Dot" -> {
                ringShape(ctx, cx, cy, Math.round(6 * sc) + dg, Math.max(1, Math.round(sc)), c);
                int d = Math.max(2, Math.round(2 * sc));
                box(ctx, cx - d / 2, cy - d / 2, d, d, c);
            }
            case "T-Shape" -> cross(ctx, cx, cy, Math.round(5 * sc), Math.max(1, Math.round(sc)), Math.round(2 * sc) + dg, c, true);
            case "X" -> {
                int l = Math.round(5 * sc);
                int g = Math.round(2 * sc) + dg;
                int t = Math.max(1, Math.round(sc));
                for (int i = g; i < g + l; i++) {
                    for (int dx = -1; dx <= 1; dx += 2) {
                        for (int dy = -1; dy <= 1; dy += 2) {
                            box(ctx, cx + dx * i - t / 2, cy + dy * i - t / 2, t, t, c);
                        }
                    }
                }
            }
            case "Arrow" -> {
                int t = Math.max(1, Math.round(sc));
                int l = Math.round(5 * sc);
                for (int i = 0; i < l; i++) {
                    box(ctx, cx - i - t / 2, cy + i + dg, t, t, c);
                    box(ctx, cx + i - t / 2, cy + i + dg, t, t, c);
                }
            }
            default -> { // Custom
                int t = thickness.getInt();
                int l = Math.round(length.getInt() * sc);
                int g = Math.round(gap.getInt() * sc) + dg;
                cross(ctx, cx, cy, l, Math.max(1, Math.round(t * sc / 1.5f)), g, c, tStyle.get());
                if (dot.get()) {
                    int d = Math.max(1, Math.round(t * sc / 1.5f));
                    box(ctx, cx - d / 2, cy - d / 2, d, d, c);
                }
                if (ring.get()) {
                    ringShape(ctx, cx, cy, g + l + Math.round(2 * sc), Math.max(1, Math.round(sc)), c);
                }
            }
        }
        }
        // индикатор перезарядки удара
        if (attackBar.get()) {
            float progress = client.player.getAttackCooldownProgress(0f);
            if (progress < 1f) {
                int bw = Math.round(16 * sc);
                int bh = Math.max(2, Math.round(2 * sc));
                int by = cy + Math.round(10 * sc) + dg;
                ctx.fill(cx - bw / 2, by, cx + bw - bw / 2, by + bh, 0x80000000);
                ctx.fill(cx - bw / 2, by, cx - bw / 2 + Math.round(bw * progress), by + bh, c);
            }
        }
        ctx.getMatrices().pop();
    }

    // прямоугольник с чёрной обводкой в 1 пиксель
    private void box(DrawContext ctx, int x, int y, int w, int h, int c) {
        if (pass == 0) {
            ctx.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xC0000000);
        } else {
            ctx.fill(x, y, x + w, y + h, c);
        }
    }

    private void cross(DrawContext ctx, int cx, int cy, int len, int t, int gap, int c, boolean noTop) {
        int half = t / 2;
        if (!noTop) {
            box(ctx, cx - half, cy - gap - len, t, len, c);
        }
        box(ctx, cx - half, cy + gap + (t % 2 == 0 ? 0 : 1), t, len, c);
        box(ctx, cx - gap - len, cy - half, len, t, c);
        box(ctx, cx + gap + (t % 2 == 0 ? 0 : 1), cy - half, len, t, c);
    }

    private void ringShape(DrawContext ctx, int cx, int cy, int r, int t, int c) {
        if (pass == 0) {
            ringPixels(ctx, cx, cy, r + 1, t + 2, 0xC0000000);
        } else {
            ringPixels(ctx, cx, cy, r, t, c);
        }
    }

    private static void ringPixels(DrawContext ctx, int cx, int cy, int r, int t, int c) {
        float outer = r + 0.5f;
        float inner = r - t + 0.5f;
        for (int y = -r - 1; y <= r + 1; y++) {
            int start = Integer.MIN_VALUE;
            for (int x = -r - 1; x <= r + 2; x++) {
                float d = (float) Math.sqrt(x * x + y * y);
                boolean in = x <= r + 1 && d <= outer && d >= inner;
                if (in && start == Integer.MIN_VALUE) {
                    start = x;
                } else if (!in && start != Integer.MIN_VALUE) {
                    ctx.fill(cx + start, cy + y, cx + x, cy + y + 1, c);
                    start = Integer.MIN_VALUE;
                }
            }
        }
    }
}
