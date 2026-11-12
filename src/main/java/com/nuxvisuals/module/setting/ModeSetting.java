package com.nuxvisuals.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class ModeSetting extends Setting {
    private final String[] modes;
    private int index;

    public ModeSetting(String name, String value, String... modes) {
        super(name);
        this.modes = modes;
        this.index = Math.max(0, indexOf(value));
    }

    public String get() {
        return this.modes[this.index];
    }

    public int index() {
        return this.index;
    }

    public boolean is(String mode) {
        return get().equals(mode);
    }

    public void cycle(int direction) {
        this.index = Math.floorMod(this.index + direction, this.modes.length);
        changed();
    }

    public void set(int index) {
        this.index = Math.max(0, Math.min(this.modes.length - 1, index));
        changed();
    }

    public String[] modes() {
        return this.modes;
    }

    public void setSilently(int index) {
        this.index = Math.max(0, Math.min(this.modes.length - 1, index));
    }

    private int indexOf(String value) {
        for (int i = 0; i < this.modes.length; i++) {
            if (this.modes[i].equals(value)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(get());
    }

    @Override
    public void load(JsonElement element) {
        int i = indexOf(element.getAsString());
        if (i >= 0) {
            this.index = i;
        }
    }
}
