package com.nuxvisuals.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Short single-line text value edited directly in the ClickGUI. */
public class StringSetting extends Setting {
    private final int maxLength;
    private String value;

    public StringSetting(String name, String value, int maxLength) {
        super(name);
        this.maxLength = maxLength;
        this.value = clean(value);
    }

    public String get() {
        return this.value;
    }

    public int maxLength() {
        return this.maxLength;
    }

    public void set(String value) {
        String next = clean(value);
        if (!next.equals(this.value)) {
            this.value = next;
            changed();
        }
    }

    private String clean(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < raw.length() && out.length() < this.maxLength; i++) {
            char c = raw.charAt(i);
            if (c >= 32 && c != 127 && c != '\u00A7') {
                out.append(c);
            }
        }
        return out.toString();
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(this.value);
    }

    @Override
    public void load(JsonElement element) {
        this.value = clean(element.getAsString());
    }
}
