package com.nuxvisuals.module.impl;

import com.nuxvisuals.hud.HudIcons;
import com.nuxvisuals.hud.HudModule;
import com.nuxvisuals.hud.HudRender;
import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.setting.BoolSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;

import java.util.ArrayList;
import java.util.List;

// Плашка: NuxVisuals | ник | FPS | пинг
public class Watermark extends HudModule {
    private final BoolSetting showName = add(new BoolSetting("Show Nick", true));
    private final BoolSetting showFps = add(new BoolSetting("Show FPS", true));
    private final BoolSetting showPing = add(new BoolSetting("Show Ping", true));

    private record Part(String[] icon, String text, boolean accentText) {
    }

    private final List<Part> parts = new ArrayList<>();

    public Watermark() {
        super("watermark", "Watermark", Category.Hud, true, 0.5, 0.0);
    }

    @Override
    protected void measure(MinecraftClient client, boolean editing) {
        parts.clear();
        parts.add(new Part(HudIcons.LOGO, "NuxVisuals", false));
        if (showName.get()) {
            parts.add(new Part(HudIcons.USER, client.getSession().getUsername(), false));
        }
        if (showFps.get()) {
            parts.add(new Part(HudIcons.MONITOR, client.getCurrentFps() + "fps", false));
        }
        if (showPing.get()) {
            int ping = 0;
            if (client.getNetworkHandler() != null && client.player != null) {
                PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
                if (entry != null) {
                    ping = entry.getLatency();
                }
            }
            parts.add(new Part(HudIcons.PING, ping + "ms", false));
        }
        int w = 7;
        for (int i = 0; i < parts.size(); i++) {
            w += 13 + textWidth(parts.get(i).text());
            if (i < parts.size() - 1) {
                w += 13;
            }
        }
        width = w + 7;
        height = 17;
    }

    @Override
    protected void draw(DrawContext context, MinecraftClient client, float tickDelta, boolean editing) {
        panel(context, 0, 0, width, height);
        int x = 7;
        for (int i = 0; i < parts.size(); i++) {
            Part p = parts.get(i);
            if (p.icon() == HudIcons.LOGO) {
                com.nuxvisuals.ui.NuxIcons.logo(context, x - 1, 3, 11, accentColor(0));
            } else {
                HudIcons.draw(context, p.icon(), x, 5, accentColor(i * 0.1f));
            }
            x += 13;
            text(context, p.text(), x, 5, 0xFFE8ECF5);
            x += textWidth(p.text());
            if (i < parts.size() - 1) {
                divider(context, x + 6, 5, 8);
                x += 13;
            }
        }
    }
}
