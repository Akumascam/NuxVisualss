package com.nuxvisuals.ui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

// Кнопка главного меню: тёмная скруглённая плашка, при наведении — светлее,
// слева появляется градиентная полоска, справа стрелка.
public class NuxMenuButton extends ButtonWidget {
    private final TextRenderer textRenderer;
    private final boolean centered;
    private final boolean danger;
    private float hover;

    public NuxMenuButton(int x, int y, int width, int height, String label, TextRenderer textRenderer,
                         boolean centered, boolean danger, PressAction onPress) {
        super(x, y, width, height, Text.literal(label), onPress, DEFAULT_NARRATION_SUPPLIER);
        this.textRenderer = textRenderer;
        this.centered = centered;
        this.danger = danger;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean hovered = this.isHovered();
        hover += ((hovered ? 1f : 0f) - hover) * Math.min(1f, delta * 0.6f);

        int x = this.getX();
        int y = this.getY();
        int w = this.width;
        int h = this.height;

        int bg = NuxClickGui.blend(0xB80B1224, 0xE01A2A4E, hover);
        NuxClickGui.roundedRect(context, x, y, w, h, 6, bg);

        int text = NuxClickGui.blend(0xFFD6E2FF, 0xFFFFFFFF, hover);
        if (danger) {
            text = NuxClickGui.blend(0xFFD6E2FF, 0xFFFF6B6B, hover);
        }
        int textY = y + (h - 8) / 2;

        if (centered) {
            NuxFont.drawCentered(context, this.textRenderer, this.getMessage(), x + w / 2, textY, text);
            return;
        }

        // градиентная полоска слева (голубой → синий)
        if (hover > 0.02f) {
            int barH = Math.round((h - 10) * hover);
            int top = y + (h - barH) / 2;
            for (int i = 0; i < barH; i++) {
                int c = NuxClickGui.blend(0xFF7DD3FC, 0xFF3B82F6, i / (float) Math.max(1, barH - 1));
                context.fill(x + 6, top + i, x + 8, top + i + 1, c);
            }
        }
        int shift = Math.round(3 * hover);
        NuxFont.draw(context, this.textRenderer, this.getMessage(), x + 14 + shift, textY, text);
        NuxFont.draw(context, this.textRenderer, "›", x + w - 14 + shift,
                textY, NuxClickGui.blend(0xFF5B6B92, 0xFFFFFFFF, hover));
    }
}
