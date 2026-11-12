package com.nuxvisuals.hud;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.ColorSetting;
import com.nuxvisuals.module.setting.SliderSetting;
import com.nuxvisuals.render.RenderUtil;
import com.nuxvisuals.ui.NuxFont;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

// Элемент HUD: свой цвет, фон, масштаб и позиция (перетаскивается мышкой в чате)
public abstract class HudModule extends Module {
    protected final ColorSetting accent;
    protected final BoolSetting rainbow;
    protected final ColorSetting panelColor;
    protected final BoolSetting background;
    protected final SliderSetting opacity;
    protected final SliderSetting scale;
    // позиция в долях экрана (0..1); в GUI не показывается
    final SliderSetting posX;
    final SliderSetting posY;

    // размер содержимого (без масштаба), задаётся в measure()
    protected int width = 40;
    protected int height = 12;

    // итоговые координаты на экране (с масштабом)
    int screenX;
    int screenY;
    int screenW;
    int screenH;

    protected HudModule(String id, String name, Category category, boolean enabled, double x, double y) {
        super(id, name, category, enabled);
        this.accent = add(new ColorSetting("Color", 0.64f, 0.5f));
        this.rainbow = add(new BoolSetting("Rainbow", false));
        this.background = add(new BoolSetting("Background", true));
        this.panelColor = add(new ColorSetting("Цвет фона", 0.62f, 0.46f, 0.15f).visibleIf(this.background::get));
        this.opacity = add(new SliderSetting("Background Opacity", 0.82, 0.1, 1.0, 0.05))
                .visibleIf(this.background::get);
        this.scale = add(new SliderSetting("Scale", 1.0, 0.5, 2.0, 0.05, "x"));
        this.posX = add(new SliderSetting("Pos X", x, 0, 1, 0.001)).visibleIf(() -> false);
        this.posY = add(new SliderSetting("Pos Y", y, 0, 1, 0.001)).visibleIf(() -> false);
    }

    // можно ли перетаскивать (у замены хотбара/прицела позиция фиксированная)
    public boolean draggable() {
        return true;
    }

    // показывать ли элемент прямо сейчас (например, нет эффектов — нечего показывать)
    protected boolean shouldShow(boolean editing) {
        return true;
    }

    // посчитать width/height
    protected abstract void measure(MinecraftClient client, boolean editing);

    // нарисовать в точке (0, 0), уже с учётом масштаба
    protected abstract void draw(DrawContext context, MinecraftClient client, float tickDelta, boolean editing);

    void render(DrawContext context, MinecraftClient client, float tickDelta, boolean editing) {
        if (!shouldShow(editing)) {
            screenW = 0;
            screenH = 0;
            return;
        }
        measure(client, editing);
        float s = scale.getFloat();
        int sw = context.getScaledWindowWidth();
        int sh = context.getScaledWindowHeight();
        screenW = Math.round(width * s);
        screenH = Math.round(height * s);
        if (draggable()) {
            screenX = 3 + (int) Math.round(posX.get() * Math.max(0, sw - screenW - 6));
            screenY = 3 + (int) Math.round(posY.get() * Math.max(0, sh - screenH - 6));
        } else {
            int[] fixed = fixedPosition(sw, sh);
            screenX = fixed[0];
            screenY = fixed[1];
        }
        context.getMatrices().push();
        context.getMatrices().translate(screenX, screenY, 0);
        context.getMatrices().scale(s, s, 1f);
        try { draw(context, client, tickDelta, editing); }
        finally { context.getMatrices().pop(); }
    }

    protected int[] fixedPosition(int screenWidth, int screenHeight) {
        return new int[]{0, 0};
    }

    // элемент справа на экране — выравниваем содержимое по правому краю
    protected boolean rightSide() {
        return posX.get() > 0.5;
    }

    void moveTo(int x, int y, int sw, int sh) {
        int freeW = Math.max(1, sw - screenW - 6);
        int freeH = Math.max(1, sh - screenH - 6);
        posX.set(Math.max(0, Math.min(1, (x - 3) / (double) freeW)));
        posY.set(Math.max(0, Math.min(1, (y - 3) / (double) freeH)));
    }

    // ---------- помощники для рисования ----------

    protected int accentColor(float offset) {
        com.nuxvisuals.module.impl.ClickGuiModule gui = com.nuxvisuals.module.impl.ClickGuiModule.get();
        if (gui != null && gui.syncHud.get()) {
            return 0xFF000000 | gui.accentColor();
        }
        return color(accent, rainbow, offset);
    }

    protected void panel(DrawContext context, int x, int y, int w, int h) {
        if (background.get()) {
            int a = Math.round(opacity.getFloat() * 255);
            HudRender.rect(context, x, y, w, h, 6, (a << 24) | (panelColor.get() & 0xFFFFFF));
        }
    }

    protected void text(DrawContext context, String text, int x, int y, int color) {
        NuxFont.drawOn(context, mc().textRenderer, text, x, y, color, background.get()?panelColor.get():0xFF10141E);
    }

    protected static int textWidth(String text) {
        return NuxFont.width(mc().textRenderer, text);
    }

    // тонкий вертикальный разделитель
    protected static void divider(DrawContext context, int x, int y, int h) {
        context.fill(x, y, x + 1, y + h, 0x30FFFFFF);
    }

    // шапка панели: иконка + заголовок
    protected void header(DrawContext context, String[] icon, String title) {
        HudIcons.draw(context, icon, 8, 7, accentColor(0));
        text(context, title, 21, 6, 0xFFFFFFFF);
    }

    protected static int alpha(int rgb, float a) {
        return RenderUtil.withAlpha(rgb, a);
    }
}
