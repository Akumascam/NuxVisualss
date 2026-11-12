package com.nuxvisuals.ui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class NuxButton extends ButtonWidget {
    private final TextRenderer textRenderer;
    private String[] icon;
    private int iconColor = 0xFFFFFFFF;

    public NuxButton(int x, int y, int width, int height, Text message, TextRenderer textRenderer, ButtonWidget.PressAction onPress) {
        this(x, y, width, height, message, textRenderer, onPress, null, 0xFFFFFFFF);
    }

    public NuxButton(int x, int y, int width, int height, Text message, TextRenderer textRenderer, ButtonWidget.PressAction onPress, String[] icon) {
        this(x, y, width, height, message, textRenderer, onPress, icon, 0xFFFFFFFF);
    }

    public NuxButton(int x, int y, int width, int height, Text message, TextRenderer textRenderer, ButtonWidget.PressAction onPress, String[] icon, int iconColor) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
        this.textRenderer = textRenderer;
        this.icon = icon;
        this.iconColor = iconColor;
    }

    public void setIconColor(int iconColor) {
        this.iconColor = iconColor;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean hovered = this.isMouseOver(mouseX, mouseY);

        // полупрозрачный тёмный фон без рамки, при наведении чуть плотнее
        context.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height,
                hovered ? 0xB3000000 : 0x8C000000);

        if (icon == null) {
            NuxFont.drawCentered(context, this.textRenderer, this.getMessage(),
                    this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, 0xFFFFFFFF);
        } else {
            // текст слева
            NuxFont.draw(context, this.textRenderer, this.getMessage(),
                    this.getX() + 20, this.getY() + (this.height - 8) / 2, 0xFFFFFFFF);

            // иконка справа, квадратики 2x2 пикселя
            int scale = 2;
            int iconW = icon[0].length() * scale;
            int iconH = icon.length * scale;
            NuxIcons.draw(context, icon,
                    this.getX() + this.width - 20 - iconW,
                    this.getY() + (this.height - iconH) / 2,
                    scale, iconColor);
        }
    }
}
