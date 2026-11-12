package com.nuxvisuals.module.impl;
import net.minecraft.client.MinecraftClient;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.ColorSetting;
import com.nuxvisuals.module.setting.ModeSetting;
import com.nuxvisuals.module.setting.SliderSetting;
import com.nuxvisuals.render.HandEffectPattern;
import com.nuxvisuals.render.RenderUtil;
import com.nuxvisuals.ui.NuxShapes;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Actual animated surface overlays on first-person arm geometry, not just six tinted corners.
 * Original UV/material/light/alpha/glint are retained. Overlay generation is deferred until
 * all vanilla hand geometry is submitted, avoiding invalidating active BufferBuilders.
 */
public class HandChams extends Module {
    public static HandChams INSTANCE;
    private final com.nuxvisuals.module.setting.ModeSetting quality=add(new com.nuxvisuals.module.setting.ModeSetting("Качество эффектов","Авто","Авто","Высокое","Среднее","Низкое"));
    public static boolean firstPerson;
    private final ModeSetting style = add(new ModeSetting("Эффект", "Перламутр", "Перламутр", "Плазма", "Молнии"));
    private final ColorSetting color = add(new ColorSetting("Цвет", 0.66f, 0.5f));
    private final ColorSetting color2 = add(new ColorSetting("Второй цвет", 0.72f, 0.25f));
    private final SliderSetting fill = add(new SliderSetting("Заливка", 0.55, 0, 1, 0.05));
    private final SliderSetting intensity = add(new SliderSetting("Сила эффекта", 0.85, 0, 1, 0.05));
    private final SliderSetting speed = add(new SliderSetting("Скорость", 1, 0, 3, 0.1));
    private final BoolSetting hands = add(new BoolSetting("Руки", true));
    private final BoolSetting items = add(new BoolSetting("Предметы", true));

    private record Quad(float[] xyz, float[] uv, float[] normal, int[] alpha, int detail) {}
    private static final List<Quad> pending = new ArrayList<>();
    private static boolean capturing;
    private static float frameTime;
    private static int frameStyle, frameColor, frameSecond;
    private static float frameFill, frameIntensity;
    private static final int MAX_QUADS = 192;

    public HandChams() {
        super("hand-chams", "3D Hands", Category.Visuals, false);
        INSTANCE = this;
    }

    private static int itemCells;
    private static int detail(boolean item) {
        var s=INSTANCE;
        String quality=s.quality.get();
        int n=quality.equals("Высокое")?3:quality.equals("Низкое")?1:quality.equals("Среднее")?2:
                (MinecraftClient.getInstance().getCurrentFps()<55?1:MinecraftClient.getInstance().getCurrentFps()<100?2:3);
        return item?(n==1?3:n==2?5:8):(n==1?8:n==2?12:20);
    }
    public static void beginFrame() {
        itemCells=0;
        pending.clear(); firstPerson = false;
        HandChams s = INSTANCE;
        capturing = s != null && s.isEnabled();
        if (!capturing) return;
        frameTime = (System.nanoTime() % 3_600_000_000_000L) / 1_000_000_000f * s.speed.getFloat();
        frameStyle = s.style.index(); frameColor = s.color.get(); frameSecond = s.color2.get();
        frameFill = s.fill.getFloat(); frameIntensity = s.intensity.getFloat();
    }

    public static VertexConsumerProvider wrap(VertexConsumerProvider original) {
        return wrap(original, INSTANCE != null && INSTANCE.hands.get(), false);
    }
    public static VertexConsumerProvider wrapItems(VertexConsumerProvider original) {
        return wrap(original, INSTANCE != null && INSTANCE.items.get(), true);
    }
    private interface Wrapped extends VertexConsumerProvider {}
    private static VertexConsumerProvider wrap(VertexConsumerProvider original, boolean enabled, boolean item) {
        if (!capturing || !firstPerson || !enabled || original == null || original instanceof Wrapped) return original;
        return (Wrapped) layer -> {
            VertexConsumer base = original.getBuffer(layer);
            String material = layer.toString().toLowerCase(Locale.ROOT);
            // Don't collect glint or debug primitives; these aren't textured entity/item quads.
            if (material.contains("glint") || material.contains("lines")) return base;
            if (item) return material.contains("debug") ? new DirectTintConsumer(base)
                    : new ItemSurfaceConsumer(base, detail(true));
            if (material.contains("debug")) return base;
            return new SurfaceConsumer(base, detail(false));
        };
    }

    public static VertexConsumerProvider wrapMap(VertexConsumerProvider original) {
        if (!capturing || !firstPerson || INSTANCE == null || !INSTANCE.items.get() || original instanceof Wrapped) return original;
        return (Wrapped) layer -> new DirectTintConsumer(original.getBuffer(layer));
    }

    // Immediate fallback for maps/custom untextured geometry (which may omit normals).
    private static final class DirectTintConsumer implements VertexConsumer {
        private final VertexConsumer base;
        private float x, y, z;
        DirectTintConsumer(VertexConsumer base) { this.base = base; }
        @Override public VertexConsumer vertex(float x, float y, float z) {
            this.x = x; this.y = y; this.z = z; base.vertex(x, y, z); return this;
        }
        @Override public VertexConsumer color(int r, int g, int b, int a) {
            int c = tint((a << 24) | (r << 16) | (g << 8) | b, x * 3 + z, y * 3 + z);
            base.color(c); return this;
        }
        @Override public VertexConsumer texture(float u, float v) { base.texture(u,v); return this; }
        @Override public VertexConsumer overlay(int u, int v) { base.overlay(u,v); return this; }
        @Override public VertexConsumer light(int u, int v) { base.light(u,v); return this; }
        @Override public VertexConsumer normal(float x,float y,float z) { base.normal(x,y,z); return this; }
    }

    private static int tint(int source, float u, float v) {
        float p = frameStyle == 2 ? HandEffectPattern.lightning(u, v, frameTime)
                : frameStyle == 1 ? HandEffectPattern.plasma(u, v, frameTime)
                : HandEffectPattern.pearl(u, v, frameTime);
        int rgb = frameStyle == 2 ? NuxShapes.blend(frameColor, 0xFFFFFFFF, HandEffectPattern.clamp((p - 0.4f) * 1.8f))
                : NuxShapes.blend(frameColor, frameSecond, p);
        if (frameStyle != 2) rgb = NuxShapes.blend(rgb, 0xFFFFFFFF, p * p * frameIntensity * 0.8f);
        float k = HandEffectPattern.clamp(frameFill + frameIntensity * p * (frameStyle == 2 ? 1f : 0.3f));
        int result = NuxShapes.blend(source | 0xFF000000, rgb | 0xFF000000, k);
        return (source & 0xFF000000) | (result & 0xFFFFFF);
    }

    // Subdivide textured ITEM geometry in its OWN material, not untextured quads on top.
    // Thus atlas UVs, cutout holes, light, overlays and vertex alpha survive on every item.
    private static final class ItemSurfaceConsumer implements VertexConsumer {
        private final VertexConsumer base;
        private final int detail;
        private final float[] xyz = new float[12], uv = new float[8], normals = new float[12];
        private final int[] colors = new int[4], lights = new int[8], overlays = new int[8];
        private int index;
        ItemSurfaceConsumer(VertexConsumer base, int detail) { this.base = base; this.detail = detail; }
        @Override public VertexConsumer vertex(float x,float y,float z) {
            xyz[index*3]=x; xyz[index*3+1]=y; xyz[index*3+2]=z; colors[index]=0xFFFFFFFF; return this;
        }
        @Override public VertexConsumer color(int r,int g,int b,int a) { colors[index]=(a<<24)|(r<<16)|(g<<8)|b; return this; }
        @Override public VertexConsumer texture(float u,float v) { uv[index*2]=u; uv[index*2+1]=v; return this; }
        @Override public VertexConsumer overlay(int u,int v) { overlays[index*2]=u; overlays[index*2+1]=v; return this; }
        @Override public VertexConsumer light(int u,int v) { lights[index*2]=u; lights[index*2+1]=v; return this; }
        @Override public VertexConsumer normal(float x,float y,float z) {
            normals[index*3]=x; normals[index*3+1]=y; normals[index*3+2]=z;
            if (++index == 4) {
                int n=itemCells+detail*detail>4096?1:detail;
                itemCells+=n*n;
                float step=1f/n;
                for(int yCell=0;yCell<n;yCell++) for(int xCell=0;xCell<n;xCell++) {
                    float u=xCell*step,v=yCell*step;
                    emit(u,v);emit(u+step,v);emit(u+step,v+step);emit(u,v+step);
                }
                index=0;
            }
            return this;
        }
        private void emit(float u,float v) {
            float x=attr(xyz,3,0,u,v),y=attr(xyz,3,1,u,v),z=attr(xyz,3,2,u,v);
            int c=0;
            for(int shift=0;shift<=24;shift+=8) {
                int value=Math.round(bilerp((colors[0]>>>shift)&255,(colors[1]>>>shift)&255,
                        (colors[2]>>>shift)&255,(colors[3]>>>shift)&255,u,v));
                c |= value << shift;
            }
            // Model-space plane coordinates produce visible patterns even on tiny atlas sprites.
            c=tint(c,x*4f+z*2f,y*4f-z);
            int ou=Math.round(bilerp(overlays[0],overlays[2],overlays[4],overlays[6],u,v));
            int ov=Math.round(bilerp(overlays[1],overlays[3],overlays[5],overlays[7],u,v));
            int lu=Math.round(bilerp(lights[0],lights[2],lights[4],lights[6],u,v));
            int lv=Math.round(bilerp(lights[1],lights[3],lights[5],lights[7],u,v));
            base.vertex(x,y,z).color(c).texture(attr(uv,2,0,u,v),attr(uv,2,1,u,v))
                    .overlay(ou,ov).light(lu,lv).normal(attr(normals,3,0,u,v),attr(normals,3,1,u,v),attr(normals,3,2,u,v));
        }
    }

    private static final class SurfaceConsumer implements VertexConsumer {
        private final VertexConsumer base;
        private final int detail;
        private float[] xyz = new float[12], uv = new float[8], normal = new float[12];
        private int[] alpha = new int[4];
        private int index;
        SurfaceConsumer(VertexConsumer base, int detail) { this.base = base; this.detail = detail; }
        @Override public VertexConsumer vertex(float x, float y, float z) {
            xyz[index * 3] = x; xyz[index * 3 + 1] = y; xyz[index * 3 + 2] = z;
            alpha[index] = 255;
            base.vertex(x, y, z); return this;
        }
        @Override public VertexConsumer color(int r, int g, int b, int a) {
            alpha[index] = a; base.color(r, g, b, a); return this;
        }
        @Override public VertexConsumer texture(float u, float v) {
            uv[index * 2] = u; uv[index * 2 + 1] = v;
            base.texture(u, v); return this;
        }
        @Override public VertexConsumer overlay(int u, int v) { base.overlay(u, v); return this; }
        @Override public VertexConsumer light(int u, int v) { base.light(u, v); return this; }
        @Override public VertexConsumer normal(float x, float y, float z) {
            normal[index * 3] = x; normal[index * 3 + 1] = y; normal[index * 3 + 2] = z;
            base.normal(x, y, z);
            // Vanilla 1.21.4 entity/baked-quad vertex paths finish each vertex with normal.
            if (++index == 4) {
                if (pending.size() < MAX_QUADS) pending.add(new Quad(xyz, uv, normal, alpha, detail));
                xyz = new float[12]; uv = new float[8]; normal = new float[12]; alpha = new int[4]; index = 0;
            }
            return this;
        }
    }

    public static void flush(VertexConsumerProvider provider) {
        firstPerson = false;
        if (!capturing || pending.isEmpty()) { capturing = false; pending.clear(); return; }
        try {
            VertexConsumer vc = provider.getBuffer(RenderLayer.getDebugQuads());
            for (Quad q : pending) {
                int n = q.detail;
                for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) {
                    float u = x / (float) n, v = y / (float) n, du = 1f / n;
                    point(vc, q, u, v); point(vc, q, u + du, v);
                    point(vc, q, u + du, v + du); point(vc, q, u, v + du);
                }
            }
        } finally { pending.clear(); capturing = false; }
    }

    private static float bilerp(float a, float b, float c, float d, float u, float v) {
        return a * (1 - u) * (1 - v) + b * u * (1 - v) + c * u * v + d * (1 - u) * v;
    }
    private static float attr(float[] values, int stride, int axis, float u, float v) {
        return bilerp(values[axis], values[stride + axis], values[stride * 2 + axis], values[stride * 3 + axis], u, v);
    }
    private static void point(VertexConsumer vc, Quad q, float u, float v) {
        float x = attr(q.xyz, 3, 0, u, v), y = attr(q.xyz, 3, 1, u, v), z = attr(q.xyz, 3, 2, u, v);
        // Offset along the transformed normal: hugs the real mesh and avoids z-fighting.
        x += attr(q.normal, 3, 0, u, v) * 0.0015f;
        y += attr(q.normal, 3, 1, u, v) * 0.0015f;
        z += attr(q.normal, 3, 2, u, v) * 0.0015f;
        // Use skin/model UVs for continuity over adjoining faces and stable motion while swinging.
        float pu = attr(q.uv, 2, 0, u, v) * 8f;
        float pv = attr(q.uv, 2, 1, u, v) * 8f;
        float p = frameStyle == 2 ? HandEffectPattern.lightning(pu, pv, frameTime)
                : frameStyle == 1 ? HandEffectPattern.plasma(pu, pv, frameTime)
                : HandEffectPattern.pearl(pu, pv, frameTime);
        int rgb;
        float opacity;
        if (frameStyle == 2) {
            rgb = NuxShapes.blend(frameColor, 0xFFFFFFFF, HandEffectPattern.clamp((p - 0.45f) * 1.8f));
            opacity = HandEffectPattern.clamp(frameFill * 0.7f + p * frameIntensity);
        } else {
            rgb = NuxShapes.blend(frameColor, frameSecond, p);
            float shine = frameStyle == 0 ? p * p * 0.85f : Math.max(0, (p - 0.65f) * 2f);
            rgb = NuxShapes.blend(rgb, 0xFFFFFFFF, shine * frameIntensity);
            opacity = HandEffectPattern.clamp(frameFill + p * frameIntensity * 0.28f);
        }
        float materialAlpha = bilerp(q.alpha[0], q.alpha[1], q.alpha[2], q.alpha[3], u, v) / 255f;
        vc.vertex(x, y, z).color(RenderUtil.withAlpha(rgb, opacity * materialAlpha));
    }

    @Override public void onDisable() { pending.clear(); firstPerson = false; capturing = false; }
}
