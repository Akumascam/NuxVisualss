package com.nuxvisuals.hud;

import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.ModuleManager;
import com.nuxvisuals.ui.NuxFont;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// Всплывающие уведомления справа снизу («China Hat включён»)
public final class Notifications {
    private static final long LIFE = 2600;
    private static final List<Toast> TOASTS = new ArrayList<>();

    private record Toast(String title, boolean on, long created, com.nuxvisuals.module.Category category) {
    }

    public static void moduleToggled(Module module) {
        Module alerts = ModuleManager.get("alerts");
        if (alerts == null || !alerts.isEnabled() || module == alerts) {
            return;
        }
        TOASTS.add(new Toast(module.getName(), module.isEnabled(), System.currentTimeMillis(), module.getCategory()));
        if (TOASTS.size() > 5) {
            TOASTS.remove(0);
        }
    }

    static void render(DrawContext context, int sw, int sh) {
        if (TOASTS.isEmpty()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        long now = System.currentTimeMillis();
        Iterator<Toast> it = TOASTS.iterator();
        List<Toast> alive = new ArrayList<>();
        while (it.hasNext()) {
            Toast t = it.next();
            if (now - t.created() > LIFE) {
                it.remove();
            } else {
                alive.add(t);
            }
        }
        int y = 24;
        for (Toast t : alive) {
            long age = now - t.created();
            float in = Math.min(1f, age / 200f);
            float out = Math.min(1f, (LIFE - age) / 250f);
            float k = Math.min(in, out);
            k = 1 - (1 - k) * (1 - k);
            int alpha = Math.max(4, Math.round(k * 255));

            String text = "Модуль " + t.title() + (t.on() ? " включён" : " выключен");
            int w = NuxFont.width(client.textRenderer, text) + 52;
            int h = 17;
            int x = sw / 2 - w / 2;
            int yy = y - Math.round((1 - k) * 8);
            HudRender.rect(context, x, yy, w, h, 6, (Math.round(0.85f * alpha) << 24) | 0x161B27);
            int accent = 0x7C8BFF;
            com.nuxvisuals.ui.NuxIcons.draw(context, com.nuxvisuals.ui.NuxClickGui.icon(t.category()), x + 7, yy + 5, 1, (alpha << 24) | accent);
            NuxFont.draw(context, client.textRenderer, text, x + 20, yy + 5, (alpha << 24) | 0xE8ECF5);
            // переключатель справа
            int tx = x + w - 25;
            int bg = t.on() ? accent : 0x3A4255;
            HudRender.rect(context, tx, yy + 4, 18, 9, 4, (alpha << 24) | bg);
            HudRender.circle(context, t.on() ? tx + 13 : tx + 4, yy + 8, 3, (alpha << 24) | 0xFFFFFF);
            y += Math.round((h + 4) * k);
        }
    }

    private Notifications() {
    }
}
