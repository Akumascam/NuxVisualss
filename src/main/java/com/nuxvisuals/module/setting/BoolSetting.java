package com.nuxvisuals.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class BoolSetting extends Setting {
    private boolean value;

    public BoolSetting(String name, boolean value) {
        super(name);
        this.value = value;
    }

    public boolean get() {
        return this.value;
    }

    public void set(boolean value) {
        this.value = value;
        changed();
    }

    public void toggle() {
        set(!this.value);
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(this.value);
    }

    @Override
    public void load(JsonElement element) {
        this.value = element.getAsBoolean();
    }
}
