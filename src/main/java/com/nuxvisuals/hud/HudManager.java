package com.nuxvisuals.hud;

import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.ModuleManager;
import com.nuxvisuals.ui.NuxFont;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;

// Рисует все HUD-элементы и даёт перетаскивать их мышкой, пока открыт чат
public final class HudManager {
    private static HudModule dragging;
    private static int dragOffsetX;
    private static int dragOffsetY;

    public static void init() {
        HudRenderCallback.EVENT.register((context, tickCounter) ->
                render(context, tickCounter.getTickDelta(false)));

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof ChatScreen)) {
                return;
            }
            ScreenMouseEvents.allowMouseClick(screen).register((s, mouseX, mouseY, button) -> {
                HudModule hit = find(mouseX, mouseY);
                if (hit != null && button == 1) {
                    // ПКМ — открыть настройки элемента
                    com.nuxvisuals.ui.NuxClickGui.openFor(hit);
                    return false;
                }
                if (button != 0) {
                    return true;
                }
                // кнопки внутри элемента (например, плеер музыки)
                if (hit instanceof HudClickable clickable) {
                    float sc = Math.max(0.01f, hit.scale.getFloat());
                    if (clickable.click((mouseX - hit.screenX) / sc, (mouseY - hit.screenY) / sc)) {
                        return false;
                    }
                }
                if (hit != null) {
                    dragging = hit;
                    dragOffsetX = (int) mouseX - hit.screenX;
                    dragOffsetY = (int) mouseY - hit.screenY;
                    return false; // клик забрал HUD — чат его не получает
                }
                return true;
            });
            ScreenMouseEvents.afterMouseRelease(screen).register((s, mouseX, mouseY, button) -> {
                if (button == 0) {
                    dragging = null;
                }
            });
            ScreenEvents.remove(screen).register(s -> dragging = null);
        });
    }

    public static boolean editing() {
        return MinecraftClient.getInstance().currentScreen instanceof ChatScreen;
    }

    private static HudModule find(double mouseX, double mouseY) {
        HudModule found = null;
        for (Module module : ModuleManager.MODULES) {
            if (module instanceof HudModule hud && hud.isEnabled() && hud.draggable() && hud.screenW > 0
                    && mouseX >= hud.screenX && mouseX < hud.screenX + hud.screenW
                    && mouseY >= hud.screenY && mouseY < hud.screenY + hud.screenH) {
                found = hud; // последний = нарисован сверху
            }
        }
        return found;
    }

    private static void render(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) {
            return;
        }
        boolean editing = editing();
        int sw = context.getScaledWindowWidth();
        int sh = context.getScaledWindowHeight();

        if (editing && dragging != null) {
            double scale = client.getWindow().getScaleFactor();
            int mx = (int) (client.mouse.getX() / scale);
            int my = (int) (client.mouse.getY() / scale);
            dragging.moveTo(mx - dragOffsetX, my - dragOffsetY, sw, sh);
        }

        for (Module module : ModuleManager.MODULES) {
            if (module instanceof HudModule hud && hud.isEnabled()) {
                hud.render(context, client, tickDelta, editing);
                if (editing && hud.draggable() && hud.screenW > 0) {
                    outline(context, hud.screenX - 1, hud.screenY - 1, hud.screenW + 2, hud.screenH + 2,
                            hud == dragging ? 0xFF60A5FA : 0x803B82F6);
                }
            }
        }

        if (editing) {
            String hint = "ЛКМ — перетащить   ПКМ — открыть настройки";
            int w = NuxFont.width(client.textRenderer, hint);
            HudRender.rect(context, sw / 2 - w / 2 - 8, 6, w + 16, 15, 7, 0xC00B1224);
            NuxFont.draw(context, client.textRenderer, hint, sw / 2 - w / 2, 10, 0xFFBFD4FF);
        }

        Notifications.render(context, sw, sh);
    }

    private static void outline(DrawContext context, int x, int y, int w, int h, int color) {
        context.fill(x, y, x + w, y + 1, color);
        context.fill(x, y + h - 1, x + w, y + h, color);
        context.fill(x, y, x + 1, y + h, color);
        context.fill(x + w - 1, y, x + w, y + h, color);
    }

    private HudManager() {
    }
}
