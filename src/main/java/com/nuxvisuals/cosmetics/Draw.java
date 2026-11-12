package com.nuxvisuals.cosmetics;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Vector3f;

// Мини-«движок» для 3D-косметики: кубики и объёмный пиксель-арт.
// Координаты в пикселях модели игрока: ось Y смотрит вниз, -Z — вперёд (лицо), +Z — спина.
// Цвет 0xFE?????? (альфа 0xFE) = светящийся пиксель (не темнеет в тени).
public final class Draw {
    public static final int GLOW = 0xFE000000;

    public final MatrixStack m;
    public final CosmeticData d;
    public final float t;
    private final VertexConsumer vc;   // null — режим «измерения» (для превью)
    private final float bright;
    private boolean glow;
    private final float[] bounds;
    private final Vector3f tmp = new Vector3f();
    public boolean backOccupied;       // что-то уже висит на спине
    public Rig.Anchor part;            // для моделей: какая часть тела сейчас рисуется
    public int tint = -1;              // перекраска в один цвет с сохранением объёма (-1 — родные цвета)
    public float wingScale = 1f;       // размер крыльев (для друзей)

    public Draw(MatrixStack m, VertexConsumer vc, float bright, CosmeticData d) {
        this.m = m;
        this.vc = vc;
        this.bright = bright;
        this.d = d;
        this.t = d.time;
        this.bounds = null;
    }

    // режим измерения: ничего не рисует, только считает габариты
    public Draw(MatrixStack m, CosmeticData d, float[] bounds) {
        this.m = m;
        this.vc = null;
        this.bright = 1f;
        this.d = d;
        this.t = d.time;
        this.bounds = bounds;
    }

    // ---------- матрицы ----------

    public void push() {
        m.push();
    }

    public void pop() {
        m.pop();
    }

    public void move(float x, float y, float z) {
        m.translate(x, y, z);
    }

    public void rotX(float deg) {
        if (deg != 0) {
            m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(deg));
        }
    }

    public void rotY(float deg) {
        if (deg != 0) {
            m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(deg));
        }
    }

    public void rotZ(float deg) {
        if (deg != 0) {
            m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(deg));
        }
    }

    public void scale(float s) {
        m.scale(s, s, s);
    }

    public void scale(float x, float y, float z) {
        m.scale(x, y, z);
    }

    public void glow(boolean on) {
        glow = on;
    }

    // ---------- цвета ----------

    public static int glow(int rgb) {
        return GLOW | (rgb & 0xFFFFFF);
    }

    public static int alpha(int rgb, int a) {
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    public static int mix(int a, int b, float f) {
        f = Math.max(0f, Math.min(1f, f));
        int r = (int) (((a >> 16) & 255) * (1 - f) + ((b >> 16) & 255) * f);
        int g = (int) (((a >> 8) & 255) * (1 - f) + ((b >> 8) & 255) * f);
        int bl = (int) ((a & 255) * (1 - f) + (b & 255) * f);
        return (a & 0xFF000000) | (r << 16) | (g << 8) | bl;
    }

    public static int hsv(float h, float s, float v) {
        return com.nuxvisuals.render.RenderUtil.hsv(h, s, v);
    }

    // перекраска: берём яркость пикселя и красим в цвет tint
    private int applyTint(int argb) {
        if (tint == -1) {
            return argb;
        }
        float l = (0.299f * ((argb >> 16) & 255) + 0.587f * ((argb >> 8) & 255) + 0.114f * (argb & 255)) / 255f;
        float f = 0.45f + 0.85f * l;
        int r = Math.min(255, (int) (((tint >> 16) & 255) * f));
        int g = Math.min(255, (int) (((tint >> 8) & 255) * f));
        int b = Math.min(255, (int) ((tint & 255) * f));
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    private int shade(int argb, float face) {
        argb = applyTint(argb);
        int a = argb >>> 24;
        boolean g = glow || a == 0xFE;
        if (a == 0xFE) {
            a = 255;
        }
        float f = g ? 0.82f + 0.18f * face : face * bright;
        int r = Math.min(255, (int) (((argb >> 16) & 255) * f));
        int gg = Math.min(255, (int) (((argb >> 8) & 255) * f));
        int b = Math.min(255, (int) ((argb & 255) * f));
        return (a << 24) | (r << 16) | (gg << 8) | b;
    }

    // ---------- примитивы ----------

    public void face(float x1,float y1,float z1,float x2,float y2,float z2,
                     float x3,float y3,float z3,float x4,float y4,float z4,int color) {
        float ax=x2-x1,ay=y2-y1,az=z2-z1,bx=x3-x1,by=y3-y1,bz=z3-z1;
        float nx=ay*bz-az*by,ny=az*bx-ax*bz,nz=ax*by-ay*bx;
        float len=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);
        float f=len<0.00001f?0.9f:Math.min(1,0.72f+0.22f*Math.abs(ny)/len+0.1f*Math.abs(nx)/len);
        quad(x1,y1,z1,x2,y2,z2,x3,y3,z3,x4,y4,z4,shade(color,f));
    }

    private void quad(float x1, float y1, float z1, float x2, float y2, float z2,
                      float x3, float y3, float z3, float x4, float y4, float z4, int color) {
        Matrix4f mat = m.peek().getPositionMatrix();
        if (vc == null) {
            grow(mat, x1, y1, z1);
            grow(mat, x2, y2, z2);
            grow(mat, x3, y3, z3);
            grow(mat, x4, y4, z4);
            return;
        }
        vc.vertex(mat, x1, y1, z1).color(color);
        vc.vertex(mat, x2, y2, z2).color(color);
        vc.vertex(mat, x3, y3, z3).color(color);
        vc.vertex(mat, x4, y4, z4).color(color);
    }

    private void grow(Matrix4f mat, float x, float y, float z) {
        mat.transformPosition(x, y, z, tmp);
        bounds[0] = Math.min(bounds[0], tmp.x);
        bounds[1] = Math.min(bounds[1], tmp.y);
        bounds[2] = Math.min(bounds[2], tmp.z);
        bounds[3] = Math.max(bounds[3], tmp.x);
        bounds[4] = Math.max(bounds[4], tmp.y);
        bounds[5] = Math.max(bounds[5], tmp.z);
    }

    // faces: битовая маска пропускаемых граней (1=-X 2=+X 4=-Y(верх) 8=+Y(низ) 16=-Z 32=+Z)
    public void box(float x1, float y1, float z1, float x2, float y2, float z2, int color, int skip) {
        if ((color >>> 24) == 0) {
            return;
        }
        if ((skip & 4) == 0) {
            quad(x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, shade(color, 1f));
        }
        if ((skip & 8) == 0) {
            quad(x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, shade(color, 0.72f));
        }
        int side = shade(color, 0.9f);
        if ((skip & 16) == 0) {
            quad(x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, side);
        }
        if ((skip & 32) == 0) {
            quad(x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, side);
        }
        int side2 = shade(color, 0.82f);
        if ((skip & 1) == 0) {
            quad(x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, side2);
        }
        if ((skip & 2) == 0) {
            quad(x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, side2);
        }
    }

    public void box(float x1, float y1, float z1, float x2, float y2, float z2, int color) {
        box(x1, y1, z1, x2, y2, z2, color, 0);
    }

    // кубик по центру и размерам
    public void cube(float cx, float cy, float cz, float w, float h, float dz, int color) {
        box(cx - w / 2, cy - h / 2, cz - dz / 2, cx + w / 2, cy + h / 2, cz + dz / 2, color, 0);
    }

    // объёмный пиксель-арт: пиксель (col,row) -> x=col..col+1, y=row..row+1, z=z1..z2
    public void sprite(String[] rows, int[] pal, float z1, float z2) {
        for (int y = 0; y < rows.length; y++) {
            String row = rows[y];
            for (int x = 0; x < row.length(); x++) {
                char ch = row.charAt(x);
                if (ch == '.' || ch == ' ') {
                    continue;
                }
                int color = ch < pal.length ? pal[ch] : 0xFFFF00FF;
                int skip = 0;
                if (filled(rows, x - 1, y)) skip |= 1;
                if (filled(rows, x + 1, y)) skip |= 2;
                if (filled(rows, x, y - 1)) skip |= 4;
                if (filled(rows, x, y + 1)) skip |= 8;
                box(x, y, z1, x + 1, y + 1, z2, color, skip);
            }
        }
    }

    // спрайт так, что пиксель (px,py) оказывается в начале координат
    public void spriteAt(String[] rows, int[] pal, int px, int py, float depth) {
        push();
        move(-px - 0.5f, -py - 0.5f, 0);
        sprite(rows, pal, -depth / 2, depth / 2);
        pop();
    }

    private static boolean filled(String[] rows, int x, int y) {
        if (y < 0 || y >= rows.length || x < 0 || x >= rows[y].length()) {
            return false;
        }
        char c = rows[y].charAt(x);
        return c != '.' && c != ' ';
    }

    // палитра: pal('a', 0xFF112233, 'b', ...)
    public static int[] pal(Object... kv) {
        int[] p = new int[128];
        for (int i = 0; i + 1 < kv.length; i += 2) {
            p[(Character) kv[i]] = (Integer) kv[i + 1];
        }
        return p;
    }
}
