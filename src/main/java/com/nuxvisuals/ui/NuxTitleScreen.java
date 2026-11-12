package com.nuxvisuals.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;

// Главное меню NuxVisuals
public class NuxTitleScreen extends Screen {
    private static final int BUTTON_WIDTH = 180;
    private static final int BUTTON_HEIGHT = 22;
    private static final int GAP = 6;


    private final long openedAt = System.currentTimeMillis();

    public NuxTitleScreen() {
        super(Text.literal("NuxVisuals"));
    }

    private int columnTop() {
        return this.height / 2 - 56;
    }

    @Override
    protected void init() {
        int x = this.width / 2 - BUTTON_WIDTH / 2;
        int y = columnTop();
        int step = BUTTON_HEIGHT + GAP;

        addDrawableChild(new NuxMenuButton(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, "Одиночная игра",
                this.textRenderer, false, false, b -> this.client.setScreen(new SelectWorldScreen(this))));
        addDrawableChild(new NuxMenuButton(x, y + step, BUTTON_WIDTH, BUTTON_HEIGHT, "Сетевая игра",
                this.textRenderer, false, false, b -> this.client.setScreen(new MultiplayerScreen(this))));
        addDrawableChild(new NuxMenuButton(x, y + step * 2, BUTTON_WIDTH, BUTTON_HEIGHT, "Модули NuxVisuals",
                this.textRenderer, false, false, b -> this.client.setScreen(new NuxClickGui())));
        addDrawableChild(new NuxMenuButton(x, y + step * 3, BUTTON_WIDTH, BUTTON_HEIGHT, "Смена аккаунта",
                this.textRenderer, false, false, b -> this.client.setScreen(new NuxAccountScreen(this))));

        int half = (BUTTON_WIDTH - GAP) / 2;
        int rowY = y + step * 4 + 4;
        addDrawableChild(new NuxMenuButton(x, rowY, half, BUTTON_HEIGHT, "Настройки",
                this.textRenderer, true, false,
                b -> this.client.setScreen(new OptionsScreen(this, this.client.options))));
        addDrawableChild(new NuxMenuButton(x, rowY + step, BUTTON_WIDTH, BUTTON_HEIGHT, "Косметика",
                this.textRenderer, true, false, b -> this.client.setScreen(new NuxCosmeticsScreen(this))));
        addDrawableChild(new NuxMenuButton(x + half + GAP, rowY, half, BUTTON_HEIGHT, "Выход",
                this.textRenderer, true, true, b -> this.client.scheduleStop()));

        // переключатель темы фона (слева сверху)
        String theme = "Тема: " + MenuTheme.current().title;
        int tw = NuxFont.width(this.textRenderer, theme) + 20;
        addDrawableChild(new NuxMenuButton(8, 8, tw, 16, theme, this.textRenderer, true, false, b -> {
            this.client.setScreen(new NuxWallpaperScreen(this));
        }));
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        NuxBackground.render(context, this.width, this.height);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        drawLogo(context);
        drawCorners(context);
    }

    private void drawLogo(DrawContext context) {
        String text = "NuxVisuals";
        int logoY = columnTop() - 62;

        // плавное появление
        float appear = Math.min(1f, (System.currentTimeMillis() - openedAt) / 600f);
        int alpha = Math.max(8, Math.round(255 * appear));

        int width = this.textRenderer.getWidth(NuxFont.logo(text));
        int x = this.width / 2 - width / 2;
        int y = logoY + Math.round((1 - appear) * 6);
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            int color = NuxClickGui.blend(MenuTheme.current().accentA, MenuTheme.current().accentB, i / (float) (text.length() - 1));
            context.drawText(this.textRenderer, NuxFont.logo(ch), x, y, (alpha << 24) | (color & 0xFFFFFF), false);
            x += this.textRenderer.getWidth(NuxFont.logo(ch));
        }

        // тонкая градиентная линия под логотипом
        int lineW = 60;
        int lineX = this.width / 2 - lineW / 2;
        int lineY = logoY + 31;
        for (int i = 0; i < lineW; i++) {
            int c = NuxClickGui.blend(MenuTheme.current().accentA, MenuTheme.current().accentB, i / (float) (lineW - 1));
            context.fill(lineX + i, lineY, lineX + i + 1, lineY + 1, c);
        }

        String subtitle = "visual client  ·  fabric 1.21.4";
        NuxFont.draw(context, this.textRenderer, subtitle,
                this.width / 2 - NuxFont.width(this.textRenderer, subtitle) / 2, lineY + 7, 0xFF8A9BC4);
    }

    private void drawCorners(DrawContext context) {
        // плашка с ником игрока (справа сверху)
        String name = this.client.getSession().getUsername();
        int nameW = NuxFont.width(this.textRenderer, name);
        int pillW = nameW + 24;
        int pillX = this.width - pillW - 8;
        NuxClickGui.roundedRect(context, pillX, 8, pillW, 16, 8, 0xB80B1224);
        NuxClickGui.circle(context, pillX + 9, 16, 3, 0xFF60A5FA);
        NuxFont.draw(context, this.textRenderer, name, pillX + 16, 12, 0xFFE8EEFF);

        NuxFont.draw(context, this.textRenderer, "NuxVisuals 1.2", 8, this.height - 14, 0xFF5B6B92);
        String right = "Minecraft 1.21.4";
        NuxFont.draw(context, this.textRenderer, right,
                this.width - NuxFont.width(this.textRenderer, right) - 8, this.height - 14, 0xFF5B6B92);
    }

    @Override
    public void removed() {
        NuxBackground.release();
        super.removed();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
