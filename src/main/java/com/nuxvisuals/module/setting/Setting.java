package com.nuxvisuals.module.setting;

import com.google.gson.JsonElement;

import java.util.function.BooleanSupplier;

public abstract class Setting {
    private final String name;
    private BooleanSupplier visible = () -> true;
    private Runnable onChange = () -> {
    };

    protected Setting(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public boolean isVisible() {
        return this.visible.getAsBoolean();
    }

    @SuppressWarnings("unchecked")
    public <T extends Setting> T visibleIf(BooleanSupplier condition) {
        this.visible = condition;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public <T extends Setting> T onChange(Runnable listener) {
        this.onChange = listener;
        return (T) this;
    }

    protected void changed() {
        this.onChange.run();
    }

    public abstract JsonElement save();

    public abstract void load(JsonElement element);
}
