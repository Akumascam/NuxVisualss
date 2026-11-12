package com.nuxvisuals.module.impl;

import com.nuxvisuals.hud.HudIcons;
import com.nuxvisuals.hud.HudModule;
import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.ModuleManager;
import com.nuxvisuals.ui.NuxClickGui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

// Список модулей с назначенными клавишами
public class Hotkeys extends HudModule {
    private final List<Module> list = new ArrayList<>();

    public Hotkeys() {
        super("hotkeys", "Hotkeys", Category.Hud, true, 1.0, 0.3);
    }

    private void collect() {
        list.clear();
        for (Module m : ModuleManager.MODULES) {
            if (m.getKey() >= 0) {
                list.add(m);
            }
        }
    }

    @Override
    protected boolean shouldShow(boolean editing) {
        collect();
        return editing || !list.isEmpty();
    }

    @Override
    protected void measure(MinecraftClient client, boolean editing) {
        int w = 21 + textWidth("Hotkeys") + 30;
        for (Module m : list) {
            w = Math.max(w, 21 + textWidth(m.getName()) + 16 + textWidth(NuxClickGui.keyName(m.getKey())) + 8);
        }
        width = Math.max(110, w);
        height = 24 + Math.max(1, list.size()) * 12 + 3;
    }

    @Override
    protected void draw(DrawContext context, MinecraftClient client, float tickDelta, boolean editing) {
        panel(context, 0, 0, width, height);
        header(context, HudIcons.COMMAND, "Hotkeys");
        if (list.isEmpty()) {
            text(context, "назначьте бинд в меню", 8, 25, 0xFF5E6678);
            return;
        }
        int y = 25;
        for (Module m : list) {
            HudIcons.draw(context, NuxClickGui.icon(m.getCategory()), 8, y, m.isEnabled() ? accentColor(0) : 0xFF5E6678);
            text(context, m.getName(), 21, y, m.isEnabled() ? 0xFFE8ECF5 : 0xFF9AA3B5);
            String key = NuxClickGui.keyName(m.getKey());
            text(context, key, width - 8 - textWidth(key), y, 0xFF9AA3B5);
            y += 12;
        }
    }
}
