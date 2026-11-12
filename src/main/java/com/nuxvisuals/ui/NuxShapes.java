package com.nuxvisuals.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

// Сглаженные фигуры для всего GUI
public final class NuxShapes {
    // Фигуры рисуются в реальных пикселях экрана (а не в GUI-пикселях) со сглаживанием краёв,
    // поэтому скругления и переключатели получаются гладкими при любом GUI Scale.
    public static void roundedRect(DrawContext context, int x, int y, int width, int height, int radius, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        // small shapes (pills, buttons) stay anti-aliased: in GUI pixels they turn into jagged blobs
        // Скругления всегда сглаженные: в «быстром» режиме крупные карточки и их обводка
        // рисовались ступеньками (баг со скрина). Сглаживание затрагивает только пиксели края,
        // поэтому почти не стоит FPS.
        if (radius <= 1) {
            fastRect(context, x, y, width, height, radius, color);
            return;
        }
        int s = guiScale();
        float r = Math.min(radius, Math.min(width, height) / 2f) * s;
        context.getMatrices().push();
        context.getMatrices().scale(1f / s, 1f / s, 1f);
        smoothRect(context, x * s, y * s, width * s, height * s, r, color);
        context.getMatrices().pop();
    }

    public static void circle(DrawContext context, int centerX, int centerY, int radius, int color) {
        int size = radius * 2 + 1;
        if (com.nuxvisuals.module.impl.Optimization.fastGui() && radius >= 12) {
            fastRect(context, centerX - radius, centerY - radius, size, size, radius, color);
            return;
        }
        int s = guiScale();
        context.getMatrices().push();
        context.getMatrices().scale(1f / s, 1f / s, 1f);
        smoothRect(context, (centerX - radius) * s, (centerY - radius) * s, size * s, size * s, size * s / 2f, color);
        context.getMatrices().pop();
    }

    /**
     * Toggle switch drawn in real screen pixels: the knob is a true circle, vertically centred
     * and moves sub-GUI-pixel smoothly with the animation progress.
     */
    public static void toggle(DrawContext context, int x, int y, int w, int h, float progress, int offColor, int onColor) {
        int s = guiScale();
        float p = clamp01(progress);
        int track = blend(offColor, onColor, p);
        int W = w * s;
        int H = h * s;
        int X = x * s;
        int Y = y * s;
        int pad = Math.max(1, Math.round(1.5f * s));
        int knob = H - pad * 2;
        int travel = W - pad * 2 - knob;
        int kx = X + pad + Math.round(travel * p);
        int ky = Y + pad;
        context.getMatrices().push();
        context.getMatrices().scale(1f / s, 1f / s, 1f);
        smoothRect(context, X, Y, W, H, H / 2f, track);
        smoothRect(context, kx, ky + Math.max(1, s / 2), knob, knob, knob / 2f, 0x40000000);
        smoothRect(context, kx, ky, knob, knob, knob / 2f, blend(0xFFD4D9E4, 0xFFFFFFFF, p));
        context.getMatrices().pop();
    }

    private static void fastRect(DrawContext context, int x, int y, int w, int h, int radius, int color) {
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        if (r == 0) {
            context.fill(x, y, x + w, y + h, color);
            return;
        }
        Corner cn = corner(r);
        context.fill(x, y + r, x + w, y + h - r, color);
        for (int row = 0; row < r; row++) {
            int inset = cn.solidFrom()[row];
            context.fill(x + inset, y + row, x + w - inset, y + row + 1, color);
            context.fill(x + inset, y + h - row - 1, x + w - inset, y + h - row, color);
        }
    }

    public static int guiScale() {
        return Math.max(1, (int) Math.round(MinecraftClient.getInstance().getWindow().getScaleFactor()));
    }

    // покрытие пикселей угла кэшируется по радиусу: раньше sqrt считался для каждого пикселя каждый кадр
    private static final java.util.Map<Integer, Corner> CORNERS = new java.util.HashMap<>();

    private record Corner(int size, float[][] coverage, int[] solidFrom) {
    }

    private static Corner corner(float r) {
        int key = Math.round(r * 8);
        Corner cached = CORNERS.get(key);
        if (cached != null) {
            return cached;
        }
        float rr = key / 8f;
        int size = (int) Math.ceil(rr);
        float[][] cov = new float[size][];
        int[] solid = new int[size];
        for (int row = 0; row < size; row++) {
            float dy = rr - (row + 0.5f);
            int from = size;
            float[] line = new float[size];
            for (int col = 0; col < size; col++) {
                float dx = rr - (col + 0.5f);
                float c = clamp01(rr - (float) Math.sqrt(dx * dx + dy * dy) + 0.5f);
                if (c >= 0.999f) {
                    from = col;
                    break;
                }
                line[col] = c;
            }
            cov[row] = line;
            solid[row] = from;
        }
        Corner c = new Corner(size, cov, solid);
        if (CORNERS.size() > 512) {
            CORNERS.clear();
        }
        CORNERS.put(key, c);
        return c;
    }

    // x, y, w, h — в пикселях экрана
    private static void smoothRect(DrawContext context, int x, int y, int w, int h, float r, int color) {
        if (r <= 0.5f) {
            context.fill(x, y, x + w, y + h, color);
            return;
        }
        Corner cn = corner(r);
        int corner = cn.size();
        context.fill(x, y + corner, x + w, y + h - corner, color);
        int baseAlpha = color >>> 24;
        int rgb = color & 0xFFFFFF;

        for (int row = 0; row < corner; row++) {
            int topY = y + row;
            int bottomY = y + h - 1 - row;
            int solidFrom = cn.solidFrom()[row];
            float[] line = cn.coverage()[row];
            for (int col = 0; col < solidFrom; col++) {
                float coverage = line[col];
                if (coverage > 0.004f) {
                    int c = (Math.round(baseAlpha * coverage) << 24) | rgb;
                    int left = x + col;
                    int right = x + w - 1 - col;
                    context.fill(left, topY, left + 1, topY + 1, c);
                    context.fill(right, topY, right + 1, topY + 1, c);
                    context.fill(left, bottomY, left + 1, bottomY + 1, c);
                    context.fill(right, bottomY, right + 1, bottomY + 1, c);
                }
            }
            context.fill(x + solidFrom, topY, x + w - solidFrom, topY + 1, color);
            context.fill(x + solidFrom, bottomY, x + w - solidFrom, bottomY + 1, color);
        }
    }

    // мягкое радиальное свечение одним мешем (вместо десятков кругов попиксельно)
    public static void glow(DrawContext context, float cx, float cy, float radius, int rgb, int centerAlpha) {
        if (radius <= 0 || centerAlpha <= 0) {
            return;
        }
        org.joml.Matrix4f m = context.getMatrices().peek().getPositionMatrix();
        int center = (Math.min(255, centerAlpha) << 24) | (rgb & 0xFFFFFF);
        int mid = (Math.min(255, centerAlpha * 2 / 5) << 24) | (rgb & 0xFFFFFF);
        int edge = rgb & 0xFFFFFF;
        float r1 = radius * 0.45f;
        int seg = 40;
        context.draw(consumers -> {
            net.minecraft.client.render.VertexConsumer vc = consumers.getBuffer(net.minecraft.client.render.RenderLayer.getGui());
            for (int i = 0; i < seg; i++) {
                double a1 = i * Math.PI * 2 / seg;
                double a2 = (i + 1) * Math.PI * 2 / seg;
                float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
                float c2 = (float) Math.cos(a2), s2 = (float) Math.sin(a2);
                // центр -> середина
                vc.vertex(m, cx, cy, 0).color(center);
                vc.vertex(m, cx + c2 * r1, cy + s2 * r1, 0).color(mid);
                vc.vertex(m, cx + c1 * r1, cy + s1 * r1, 0).color(mid);
                vc.vertex(m, cx, cy, 0).color(center);
                // середина -> край
                vc.vertex(m, cx + c1 * r1, cy + s1 * r1, 0).color(mid);
                vc.vertex(m, cx + c2 * r1, cy + s2 * r1, 0).color(mid);
                vc.vertex(m, cx + c2 * radius, cy + s2 * radius, 0).color(edge);
                vc.vertex(m, cx + c1 * radius, cy + s1 * radius, 0).color(edge);
            }
        });
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : Math.min(1f, v);
    }

    public static int blend(int from, int to, float t) {
        int alpha = (int) (((from >>> 24) & 255) * (1 - t) + ((to >>> 24) & 255) * t);
        int red = (int) (((from >> 16) & 255) * (1 - t) + ((to >> 16) & 255) * t);
        int green = (int) (((from >> 8) & 255) * (1 - t) + ((to >> 8) & 255) * t);
        int blue = (int) ((from & 255) * (1 - t) + (to & 255) * t);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }


    // градиентный квадрат насыщенность/яркость для палитры (рисуется по реальным пикселям)
    public static void svSquare(DrawContext context, int x, int y, int w, int h, float hue) {
        int s = guiScale();
        context.getMatrices().push();
        context.getMatrices().scale(1f / s, 1f / s, 1f);
        int pw = w * s;
        for (int i = 0; i < pw; i++) {
            float sat = i / (float) Math.max(1, pw - 1);
            int top = com.nuxvisuals.render.RenderUtil.hsv(hue, sat, 1f);
            context.fillGradient(x * s + i, y * s, x * s + i + 1, (y + h) * s, top, 0xFF000000);
        }
        context.getMatrices().pop();
    }

    // радужная полоса выбора оттенка
    public static void hueBar(DrawContext context, int x, int y, int w, int h) {
        int s = guiScale();
        context.getMatrices().push();
        context.getMatrices().scale(1f / s, 1f / s, 1f);
        int pw = w * s;
        for (int i = 0; i < pw; i++) {
            int c = com.nuxvisuals.render.RenderUtil.hsv(i / (float) Math.max(1, pw - 1), 1f, 1f);
            context.fill(x * s + i, y * s, x * s + i + 1, (y + h) * s, c);
        }
        context.getMatrices().pop();
    }

    private NuxShapes() {
    }
}
