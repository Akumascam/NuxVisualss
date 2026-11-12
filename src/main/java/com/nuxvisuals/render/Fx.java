package com.nuxvisuals.render;

import net.minecraft.client.render.VertexConsumer;
import org.joml.Matrix4f;

// Общие 3D-примитивы для эффектов: мягкое свечение (диск с затуханием к краю),
// «лента» между двумя точками (для молний и дуг) и полупрозрачный куб с рёбрами.
// Координаты — относительно камеры (камера в точке camX, camY, camZ локальной системы).
public final class Fx {
    private static final int SEGMENTS = 12;
    private static final float[] COS = new float[SEGMENTS + 1];
    private static final float[] SIN = new float[SEGMENTS + 1];

    static {
        for (int i = 0; i <= SEGMENTS; i++) {
            double a = i * Math.PI * 2 / SEGMENTS;
            COS[i] = (float) Math.cos(a);
            SIN[i] = (float) Math.sin(a);
        }
    }

    // если doubleSided — квадрат выводится в обе стороны (для слоёв с отсечением граней)
    public static boolean doubleSided = false;

    private static void v(VertexConsumer vc, Matrix4f m, float x, float y, float z, int c) {
        vc.vertex(m, x, y, z).color(c);
    }

    public static void quad(VertexConsumer vc, Matrix4f m,
                            float x1, float y1, float z1, int c1, float x2, float y2, float z2, int c2,
                            float x3, float y3, float z3, int c3, float x4, float y4, float z4, int c4) {
        v(vc, m, x1, y1, z1, c1);
        v(vc, m, x2, y2, z2, c2);
        v(vc, m, x3, y3, z3, c3);
        v(vc, m, x4, y4, z4, c4);
        if (doubleSided) {
            v(vc, m, x4, y4, z4, c4);
            v(vc, m, x3, y3, z3, c3);
            v(vc, m, x2, y2, z2, c2);
            v(vc, m, x1, y1, z1, c1);
        }
    }

    // круглое свечение, повёрнутое к камере. rx..rz — вектор «вправо», ux..uz — «вверх» (единичные)
    public static void glow(VertexConsumer vc, Matrix4f m, float x, float y, float z, float r, int center,
                            float rx, float ry, float rz, float ux, float uy, float uz) {
        int edge = center & 0xFFFFFF;
        for (int i = 0; i < SEGMENTS; i++) {
            float ax = (COS[i] * rx + SIN[i] * ux) * r, ay = (COS[i] * ry + SIN[i] * uy) * r, az = (COS[i] * rz + SIN[i] * uz) * r;
            float bx = (COS[i + 1] * rx + SIN[i + 1] * ux) * r, by = (COS[i + 1] * ry + SIN[i + 1] * uy) * r,
                    bz = (COS[i + 1] * rz + SIN[i + 1] * uz) * r;
            quad(vc, m, x, y, z, center, x + ax, y + ay, z + az, edge, x + bx, y + by, z + bz, edge, x, y, z, center);
        }
    }

    // сплошной круг (без затухания)
    public static void disc(VertexConsumer vc, Matrix4f m, float x, float y, float z, float r, int color,
                            float rx, float ry, float rz, float ux, float uy, float uz) {
        for (int i = 0; i < SEGMENTS; i += 2) {
            float ax = (COS[i] * rx + SIN[i] * ux) * r, ay = (COS[i] * ry + SIN[i] * uy) * r, az = (COS[i] * rz + SIN[i] * uz) * r;
            float bx = (COS[i + 1] * rx + SIN[i + 1] * ux) * r, by = (COS[i + 1] * ry + SIN[i + 1] * uy) * r,
                    bz = (COS[i + 1] * rz + SIN[i + 1] * uz) * r;
            float cx = (COS[i + 2] * rx + SIN[i + 2] * ux) * r, cy = (COS[i + 2] * ry + SIN[i + 2] * uy) * r,
                    cz = (COS[i + 2] * rz + SIN[i + 2] * uz) * r;
            quad(vc, m, x, y, z, color, x + ax, y + ay, z + az, color, x + bx, y + by, z + bz, color,
                    x + cx, y + cy, z + cz, color);
        }
    }

    // лента от A до B шириной w, развёрнутая к камере
    public static void ribbon(VertexConsumer vc, Matrix4f m, float ax, float ay, float az, float bx, float by, float bz,
                              float w, int ca, int cb, float camX, float camY, float camZ) {
        float dx = bx - ax, dy = by - ay, dz = bz - az;
        float vx = camX - (ax + bx) * 0.5f, vy = camY - (ay + by) * 0.5f, vz = camZ - (az + bz) * 0.5f;
        float px = dy * vz - dz * vy, py = dz * vx - dx * vz, pz = dx * vy - dy * vx;
        float len = (float) Math.sqrt(px * px + py * py + pz * pz);
        if (len < 1e-6f) {
            return;
        }
        float k = w * 0.5f / len;
        px *= k;
        py *= k;
        pz *= k;
        quad(vc, m, ax - px, ay - py, az - pz, ca, bx - px, by - py, bz - pz, cb,
                bx + px, by + py, bz + pz, cb, ax + px, ay + py, az + pz, ca);
    }

    // молния из A в B: ломаная из segments кусков со случайным смещением jitter
    public static void bolt(VertexConsumer vc, Matrix4f m, java.util.Random random,
                            float ax, float ay, float az, float bx, float by, float bz,
                            int segments, float jitter, float width, int core, int glowColor,
                            float camX, float camY, float camZ) {
        float px = ax, py = ay, pz = az;
        for (int i = 1; i <= segments; i++) {
            float t = i / (float) segments;
            float nx = ax + (bx - ax) * t, ny = ay + (by - ay) * t, nz = az + (bz - az) * t;
            if (i < segments) {
                float j = jitter * (float) Math.sin(t * Math.PI);
                nx += (random.nextFloat() - 0.5f) * 2 * j;
                ny += (random.nextFloat() - 0.5f) * 2 * j;
                nz += (random.nextFloat() - 0.5f) * 2 * j;
            }
            ribbon(vc, m, px, py, pz, nx, ny, nz, width * 3.5f, glowColor, glowColor, camX, camY, camZ);
            ribbon(vc, m, px, py, pz, nx, ny, nz, width, core, core, camX, camY, camZ);
            px = nx;
            py = ny;
            pz = nz;
        }
    }

    // полупрозрачный куб: грани face (с лёгким затенением) + светлые рёбра edge толщиной t. Куб от -h до h.
    public static void glassCube(VertexConsumer vc, Matrix4f m, float h, int face, int edge, float t) {
        int top = RenderUtil.shade(face, 1.15f);
        int side1 = face;
        int side2 = RenderUtil.shade(face, 0.8f);
        int bottom = RenderUtil.shade(face, 0.65f);
        quad(vc, m, -h, h, -h, top, -h, h, h, top, h, h, h, top, h, h, -h, top);
        quad(vc, m, -h, -h, -h, bottom, h, -h, -h, bottom, h, -h, h, bottom, -h, -h, h, bottom);
        quad(vc, m, -h, -h, -h, side1, -h, h, -h, side1, h, h, -h, side1, h, -h, -h, side1);
        quad(vc, m, -h, -h, h, side1, h, -h, h, side1, h, h, h, side1, -h, h, h, side1);
        quad(vc, m, -h, -h, -h, side2, -h, -h, h, side2, -h, h, h, side2, -h, h, -h, side2);
        quad(vc, m, h, -h, -h, side2, h, h, -h, side2, h, h, h, side2, h, -h, h, side2);
        // 12 рёбер — тонкие брусочки
        float a = h + t * 0.5f;
        for (int s1 = -1; s1 <= 1; s1 += 2) {
            for (int s2 = -1; s2 <= 1; s2 += 2) {
                box(vc, m, -a, s1 * h - t / 2, s2 * h - t / 2, a, s1 * h + t / 2, s2 * h + t / 2, edge);
                box(vc, m, s1 * h - t / 2, -a, s2 * h - t / 2, s1 * h + t / 2, a, s2 * h + t / 2, edge);
                box(vc, m, s1 * h - t / 2, s2 * h - t / 2, -a, s1 * h + t / 2, s2 * h + t / 2, a, edge);
            }
        }
    }

    public static void box(VertexConsumer vc, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, int c) {
        quad(vc, m, x1, y2, z1, c, x1, y2, z2, c, x2, y2, z2, c, x2, y2, z1, c);
        quad(vc, m, x1, y1, z1, c, x2, y1, z1, c, x2, y1, z2, c, x1, y1, z2, c);
        quad(vc, m, x1, y1, z1, c, x1, y2, z1, c, x2, y2, z1, c, x2, y1, z1, c);
        quad(vc, m, x1, y1, z2, c, x2, y1, z2, c, x2, y2, z2, c, x1, y2, z2, c);
        quad(vc, m, x1, y1, z1, c, x1, y1, z2, c, x1, y2, z2, c, x1, y2, z1, c);
        quad(vc, m, x2, y1, z1, c, x2, y2, z1, c, x2, y2, z2, c, x2, y1, z2, c);
    }

    private Fx() {
    }
}
