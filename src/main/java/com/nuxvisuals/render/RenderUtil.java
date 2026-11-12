package com.nuxvisuals.render;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

public final class RenderUtil {
    public static final int FULL_LIGHT = 0xF000F0;

    // h, s, v в диапазоне 0..1 → 0xFFRRGGBB
    public static int hsv(float h, float s, float v) {
        h = (h % 1f + 1f) % 1f;
        float c = v * s;
        float x = c * (1 - Math.abs((h * 6f) % 2f - 1));
        float m = v - c;
        float r, g, b;
        int sector = (int) (h * 6f);
        switch (sector) {
            case 0 -> { r = c; g = x; b = 0; }
            case 1 -> { r = x; g = c; b = 0; }
            case 2 -> { r = 0; g = c; b = x; }
            case 3 -> { r = 0; g = x; b = c; }
            case 4 -> { r = x; g = 0; b = c; }
            default -> { r = c; g = 0; b = x; }
        }
        return 0xFF000000
                | (Math.round((r + m) * 255) << 16)
                | (Math.round((g + m) * 255) << 8)
                | Math.round((b + m) * 255);
    }

    // плавно бегущий оттенок для режима Rainbow
    public static float rainbowHue(float offset) {
        return (System.currentTimeMillis() % 4000L) / 4000f + offset;
    }

    public static int withAlpha(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    public static int shade(int argb, float factor) {
        int r = Math.min(255, Math.round(((argb >> 16) & 255) * factor));
        int g = Math.min(255, Math.round(((argb >> 8) & 255) * factor));
        int b = Math.min(255, Math.round((argb & 255) * factor));
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    public static void vertex(VertexConsumer vc, Matrix4f m, float x, float y, float z, int argb) {
        vc.vertex(m, x, y, z).color(argb);
    }

    public static void quad(VertexConsumer vc, Matrix4f m,
                            float x1, float y1, float z1, int c1,
                            float x2, float y2, float z2, int c2,
                            float x3, float y3, float z3, int c3,
                            float x4, float y4, float z4, int c4) {
        vertex(vc, m, x1, y1, z1, c1);
        vertex(vc, m, x2, y2, z2, c2);
        vertex(vc, m, x3, y3, z3, c3);
        vertex(vc, m, x4, y4, z4, c4);
    }

    // прямоугольник в плоскости XY (для billboard-фигур), повёрнутый на angle вокруг (cx, cy)
    public static void rect2d(VertexConsumer vc, Matrix4f m, float cx, float cy, float angle,
                              float length, float width, int argb) {
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        float hl = length / 2f;
        float hw = width / 2f;
        float ax = cos * hl, ay = sin * hl;
        float bx = -sin * hw, by = cos * hw;
        quad(vc, m,
                cx - ax - bx, cy - ay - by, 0, argb,
                cx + ax - bx, cy + ay - by, 0, argb,
                cx + ax + bx, cy + ay + by, 0, argb,
                cx - ax + bx, cy - ay + by, 0, argb);
    }

    // осевой прямоугольник в плоскости XY
    public static void box2d(VertexConsumer vc, Matrix4f m, float x1, float y1, float x2, float y2, int argb) {
        quad(vc, m, x1, y1, 0, argb, x2, y1, 0, argb, x2, y2, 0, argb, x1, y2, 0, argb);
    }

    public static void line(VertexConsumer vc, MatrixStack.Entry entry,
                            float x1, float y1, float z1, float x2, float y2, float z2, int argb) {
        float nx = x2 - x1, ny = y2 - y1, nz = z2 - z1;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-6f) {
            return;
        }
        nx /= len;
        ny /= len;
        nz /= len;
        vc.vertex(entry.getPositionMatrix(), x1, y1, z1).color(argb).normal(entry, nx, ny, nz);
        vc.vertex(entry.getPositionMatrix(), x2, y2, z2).color(argb).normal(entry, nx, ny, nz);
    }

    private RenderUtil() {
    }
}
