package com.nuxvisuals.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class NuxMenuScreen extends Screen {
    private static final String[] MODULES = {
            "ESP",
            "Tracers",
            "HUD",
            "FullBright",
            "TargetESP"
    };

    private static final String[][] MODULE_ICONS = {
            NuxIcons.EYE,
            NuxIcons.RAYS,
            NuxIcons.HUD,
            NuxIcons.BULB,
            NuxIcons.TARGET
    };

    private static final int ON_COLOR = 0xFF55FF55;
    private static final int OFF_COLOR = 0xFF6E6E78;

    private final boolean[] enabled = new boolean[MODULES.length];

    public NuxMenuScreen() {
        super(Text.literal("NuxVisuals"));
    }

    @Override
    protected void init() {
        int buttonWidth = 320;
        int buttonHeight = 32;
        int gap = 6;
        int stride = buttonHeight + gap;
        int x = this.width / 2 - buttonWidth / 2;
        int startY = this.height / 2 - 102;

        for (int i = 0; i < MODULES.length; i++) {
            final int index = i;

            addDrawableChild(new NuxButton(x, startY + i * stride, buttonWidth, buttonHeight,
                    Text.literal(MODULES[i]), this.textRenderer, button -> {
                        enabled[index] = !enabled[index];
                        boolean on = enabled[index];
                        ((NuxButton) button).setIconColor(on ? ON_COLOR : OFF_COLOR);

                        var player = MinecraftClient.getInstance().player;
                        if (player != null) {
                            player.sendMessage(Text.literal(
                                    MODULES[index] + (on ? " §aвключён" : " §cвыключен")
                            ), true);
                        }
                    }, MODULE_ICONS[i], OFF_COLOR));
        }

        int smallWidth = (buttonWidth - gap) / 2;
        int bottomY = startY + MODULES.length * stride + gap;

        addDrawableChild(new NuxButton(x, bottomY, smallWidth, buttonHeight,
                Text.literal("Настройки"), this.textRenderer,
                button -> {
                    var player = MinecraftClient.getInstance().player;
                    if (player != null) {
                        player.sendMessage(Text.literal("§7Настройки скоро появятся"), true);
                    }
                }, NuxIcons.GEAR));

        addDrawableChild(new NuxButton(x + smallWidth + gap, bottomY, smallWidth, buttonHeight,
                Text.literal("Закрыть"), this.textRenderer,
                button -> this.close(), NuxIcons.CROSS));
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        context.fill(0, 0, this.width, this.height, 0xB3000000);

        int logoY = this.height / 2 - 128;
        if (logoY >= 6) {
            NuxIcons.drawGradientText(context, this.textRenderer, "NuxVisuals",
                    this.width / 2, logoY, 0xA7, 0x8B, 0xFA, 0x22, 0xD3, 0xEE);
            NuxFont.drawCentered(context, this.textRenderer,
                    Text.literal("Build: #1.0"),
                    this.width / 2, logoY + 14, 0xFF8E8E99);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
