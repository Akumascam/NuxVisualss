package com.nuxvisuals.render;

/** Pure, deterministic surface patterns, independent of Minecraft/OpenGL. */
public final class HandEffectPattern {
    private HandEffectPattern() {}
    public static float clamp(float x) { return Math.max(0f, Math.min(1f, x)); }
    private static float smooth(float x) { x = clamp(x); return x * x * (3f - 2f * x); }
    private static float hash(int x, int y) {
        int h = x * 374761393 + y * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return (h >>> 8 & 0xFFFFFF) / 16777215f;
    }
    public static float noise(float x, float y) {
        int ix = (int) Math.floor(x), iy = (int) Math.floor(y);
        float u = smooth(x - ix), v = smooth(y - iy);
        float a = hash(ix, iy) * (1f - u) + hash(ix + 1, iy) * u;
        float b = hash(ix, iy + 1) * (1f - u) + hash(ix + 1, iy + 1) * u;
        return a * (1f - v) + b * v;
    }
    public static float plasma(float u, float v, float time) {
        float warp = noise(u * 3f + time * 0.12f, v * 3f - time * 0.17f);
        float a = (float) Math.sin(u * 12f + v * 7f + warp * 7f + time * 1.5f);
        float b = (float) Math.cos(v * 13f - u * 5f + time * 0.9f);
        return clamp(0.5f + a * 0.32f + b * 0.18f);
    }
    public static float pearl(float u, float v, float time) {
        return clamp(0.5f + 0.35f * (float) Math.sin(v * 5f - u * 3f + time * 0.8f)
                + 0.15f * (float) Math.sin(u * 9f + v * 4f - time * 0.6f));
    }
    public static float lightning(float u, float v, float time) {
        float n = noise(u * 4f + time * 0.3f, v * 7f - time * 0.5f);
        n += 0.35f * noise(u * 13f - time * 0.7f, v * 19f + time * 0.2f);
        float a = Math.abs((float) Math.sin((u + n * 0.42f + v * 0.13f + time * 0.02f) * 13f));
        float b = Math.abs((float) Math.sin((v - u * 0.35f + n * 0.3f - time * 0.03f) * 10f));
        float d = Math.min(a, b);
        return (float) Math.exp(-d * d * 110f);
    }
}
