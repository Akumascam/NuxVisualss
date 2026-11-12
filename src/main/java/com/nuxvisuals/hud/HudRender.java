package com.nuxvisuals.hud;

import net.minecraft.client.gui.DrawContext;

// Доступ к сглаженным фигурам из NuxClickGui
public final class HudRender {
    public static void rect(DrawContext context, int x, int y, int w, int h, int radius, int color) {
        com.nuxvisuals.ui.NuxShapes.roundedRect(context, x, y, w, h, radius, color);
    }

    public static void circle(DrawContext context, int cx, int cy, int r, int color) {
        com.nuxvisuals.ui.NuxShapes.circle(context, cx, cy, r, color);
    }

    private HudRender() {
    }
}
