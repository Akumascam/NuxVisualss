package com.nuxvisuals.module.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.nuxvisuals.render.RenderUtil;

// Цвет через оттенок (Hue) и насыщенность (Saturation; 0 = белый)
public class ColorSetting extends Setting {
    private float hue;
    private float saturation;
    private float brightness = 1f;

    public ColorSetting(String name, float hue, float saturation) {
        super(name);
        this.hue = hue;
        this.saturation = saturation;
    }

    public ColorSetting(String name, float hue, float saturation, float brightness) {
        this(name, hue, saturation);
        this.brightness = brightness;
    }

    public float getHue() {
        return this.hue;
    }

    public float getSaturation() {
        return this.saturation;
    }

    public void setHue(float hue) {
        this.hue = Math.max(0f, Math.min(1f, hue));
        changed();
    }

    public void setSaturation(float saturation) {
        this.saturation = Math.max(0f, Math.min(1f, saturation));
        changed();
    }

    public float getBrightness() {
        return this.brightness;
    }

    public void setBrightness(float brightness) {
        this.brightness = Math.max(0f, Math.min(1f, brightness));
        changed();
    }

    // установить цвет из 0xRRGGBB (пресеты в палитре)
    public void setRgb(int rgb) {
        float r = ((rgb >> 16) & 255) / 255f;
        float g = ((rgb >> 8) & 255) / 255f;
        float b = (rgb & 255) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float d = max - min;
        float h = 0f;
        if (d > 0) {
            if (max == r) {
                h = ((g - b) / d) % 6f;
            } else if (max == g) {
                h = (b - r) / d + 2f;
            } else {
                h = (r - g) / d + 4f;
            }
            h /= 6f;
            if (h < 0) {
                h += 1f;
            }
        }
        this.hue = h;
        this.saturation = max == 0 ? 0 : d / max;
        this.brightness = max;
        changed();
    }

    // 0xFFRRGGBB
    public int get() {
        return RenderUtil.hsv(this.hue, this.saturation, this.brightness);
    }

    @Override
    public JsonElement save() {
        JsonArray array = new JsonArray();
        array.add(this.hue);
        array.add(this.saturation);
        array.add(this.brightness);
        return array;
    }

    private static float unit(float x, float fallback) { return Float.isFinite(x) ? Math.max(0, Math.min(1, x)) : fallback; }

    @Override
    public void load(JsonElement element) {
        JsonArray array = element.getAsJsonArray();
        this.hue = unit(array.get(0).getAsFloat(), 0.62f);
        this.saturation = unit(array.get(1).getAsFloat(), 0.5f);
        this.brightness = array.size() > 2 ? unit(array.get(2).getAsFloat(), 1f) : 1f;
    }
}
