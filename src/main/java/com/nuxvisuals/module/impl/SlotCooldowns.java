package com.nuxvisuals.module.impl;
import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.CooldownInfo;
import com.nuxvisuals.module.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.item.ItemStack;

public class SlotCooldowns extends Module {
    public static SlotCooldowns INSTANCE;
    private final ColorSetting color = add(new ColorSetting("Цвет цифр", 0.12f, 0.15f));
    private final SliderSetting size = add(new SliderSetting("Размер цифр", 0.75, 0.5, 1, 0.05));
    public SlotCooldowns() { super("slot-cooldowns", "Slot Cooldowns", Category.Hud, true); INSTANCE = this; }
    public static void draw(DrawContext context, TextRenderer tr, ItemStack stack, int x, int y) {
        SlotCooldowns self = INSTANCE;
        MinecraftClient client = MinecraftClient.getInstance();
        if (self == null || !self.isEnabled() || client.player == null || stack.isEmpty()) return;
        int left = CooldownInfo.remainingTicks(client.player.getItemCooldownManager(), stack);
        if (left <= 0) return;
        String label = CooldownInfo.format(left);
        float scale = self.size.getFloat();
        context.getMatrices().push();
        try {
            context.getMatrices().translate(x + (16 - tr.getWidth(label) * scale) / 2f, y + 1, 300);
            context.getMatrices().scale(scale, scale, 1);
            context.fill(-1, -1, tr.getWidth(label) + 1, tr.fontHeight, 0xA0000000);
            context.drawText(tr, label, 0, 0, self.color.get() | 0xFF000000, true);
        } finally { context.getMatrices().pop(); }
    }
}
