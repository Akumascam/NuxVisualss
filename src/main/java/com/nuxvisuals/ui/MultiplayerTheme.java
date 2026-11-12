package com.nuxvisuals.ui;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.AddServerScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.DirectConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;

// Тема «Сетевая игра»: фон NuxVisuals (тема как в главном меню), плашка с логотипом,
// кнопка смены темы и ник игрока. Работает на списке серверов, добавлении/подключении и экране кика.
public final class MultiplayerTheme {
    private static long openedAt;

    public static boolean applies(Screen s) {
        return s instanceof MultiplayerScreen || s instanceof AddServerScreen || s instanceof DirectConnectScreen
                || s instanceof ConnectScreen || s instanceof DisconnectedScreen;
    }

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!applies(screen)) {
                return;
            }
            ScreenEvents.remove(screen).register(s -> NuxBackground.release());
            if (screen instanceof MultiplayerScreen) {
                openedAt = System.currentTimeMillis();
                String theme = "Тема: " + MenuTheme.current().title;
                int tw = NuxFont.width(client.textRenderer, theme) + 20;
                Screens.getButtons(screen).add(new NuxMenuButton(w - tw - 8, 8, tw, 16, theme, client.textRenderer, true, false, b -> {
                    MenuTheme.cycle(Screen.hasShiftDown() ? -1 : 1);
                    client.setScreen(screen);
                }));
            }
            ScreenEvents.afterRender(screen).register((s, context, mx, my, delta) -> overlay(context, s));
        });
    }

    public static void background(DrawContext context, Screen s) {
        NuxBackground.render(context, s.width, s.height);
        // лёгкое затемнение, чтобы текст серверов читался
        context.fill(0, 0, s.width, s.height, 0x40050814);
    }

    private static void overlay(DrawContext context, Screen s) {
        MinecraftClient mc = MinecraftClient.getInstance();
        MenuTheme t = MenuTheme.current();
        String brand = "NuxVisuals";
        int bw = NuxFont.width(mc.textRenderer, brand) + 26;
        NuxClickGui.roundedRect(context, 8, 8, bw, 16, 8, 0xC00B1224);
        NuxClickGui.circle(context, 17, 16, 3, t.accentB);
        NuxFont.draw(context, mc.textRenderer, brand, 24, 12, t.accentA);

        if (s instanceof MultiplayerScreen) {
            float appear = Math.min(1f, (System.currentTimeMillis() - openedAt) / 500f);
            int lineW = Math.round(90 * appear);
            int x0 = s.width / 2 - lineW / 2;
            for (int i = 0; i < lineW; i++) {
                int c = NuxClickGui.blend(t.accentA, t.accentB, i / (float) Math.max(1, lineW - 1));
                context.fill(x0 + i, 28, x0 + i + 1, 29, c);
            }
        }
        if (s.width > 460 && mc.getSession() != null) {
            NuxFont.draw(context, mc.textRenderer, "Ник: " + mc.getSession().getUsername(), 8, s.height - 14, 0xFF5B6B92);
        }
    }

    private MultiplayerTheme() {
    }
}