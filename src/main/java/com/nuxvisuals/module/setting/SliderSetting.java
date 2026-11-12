package com.nuxvisuals.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Locale;

public class SliderSetting extends Setting {
    private final double min;
    private final double max;
    private final double step;
    private final String suffix;
    private double value;

    public SliderSetting(String name, double value, double min, double max, double step) {
        this(name, value, min, max, step, "");
    }

    public SliderSetting(String name, double value, double min, double max, double step, String suffix) {
        super(name);
        this.min = min;
        this.max = max;
        this.step = step;
        this.suffix = suffix;
        this.value = clampRound(value);
    }

    public double get() {
        return this.value;
    }

    public float getFloat() {
        return (float) this.value;
    }

    public int getInt() {
        return (int) Math.round(this.value);
    }

    public double getMin() {
        return this.min;
    }

    public double getMax() {
        return this.max;
    }

    public void set(double value) {
        double rounded = clampRound(value);
        if (rounded != this.value) {
            this.value = rounded;
            changed();
        }
    }

    // без вызова слушателя (для синхронизации пресетов)
    public void setSilently(double value) {
        this.value = clampRound(value);
    }

    public void setFromProgress(double progress) {
        set(this.min + Math.max(0, Math.min(1, progress)) * (this.max - this.min));
    }

    public double progress() {
        return (this.value - this.min) / (this.max - this.min);
    }

    public String display() {
        int decimals = 0;
        double s = this.step;
        while (decimals < 4 && Math.abs(s - Math.round(s)) > 1e-9) {
            s *= 10;
            decimals++;
        }
        return String.format(Locale.US, "%." + decimals + "f", this.value) + this.suffix;
    }

    private double clampRound(double v) {
        double clamped = Math.max(this.min, Math.min(this.max, v));
        double steps = Math.round((clamped - this.min) / this.step);
        return Math.max(this.min, Math.min(this.max, this.min + steps * this.step));
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(this.value);
    }

    @Override
    public void load(JsonElement element) {
        this.value = clampRound(element.getAsDouble());
    }
}
