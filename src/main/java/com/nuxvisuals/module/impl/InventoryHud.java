package com.nuxvisuals.module.impl;

import com.nuxvisuals.hud.HudModule;
import com.nuxvisuals.hud.HudRender;
import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.ui.NuxClickGui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

// Инвентарь на экране: 27 слотов основного инвентаря (без хотбара)
public class InventoryHud extends HudModule {
    private static final int SLOT = 17;

    private final BoolSetting title = add(new BoolSetting("Title", true));
    private final BoolSetting slotBg = add(new BoolSetting("Slot Background", true));

    public InventoryHud() {
        super("inventory", "Inventory", Category.Hud, false, 0.99, 0.5);
    }

    private int top() {
        return title.get() ? 20 : 6;
    }

    @Override
    protected void measure(MinecraftClient client, boolean editing) {
        width = 6 * 2 + 9 * SLOT - 1;
        height = top() + 3 * SLOT - 1 + 6;
    }

    @Override
    protected void draw(DrawContext context, MinecraftClient client, float tickDelta, boolean editing) {
        panel(context, 0, 0, width, height);
        if (title.get()) {
            header(context, NuxClickGui.icon(Category.Hud), "Инвентарь");
        }
        if (client.player == null) {
            return;
        }
        int y0 = top();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int x = 6 + col * SLOT;
                int y = y0 + row * SLOT;
                if (slotBg.get()) {
                    HudRender.rect(context, x, y, 16, 16, 3, 0x1CFFFFFF);
                }
                ItemStack stack = client.player.getInventory().getStack(9 + row * 9 + col);
                if (!stack.isEmpty()) {
                    context.drawItem(stack, x, y);
                    context.drawStackOverlay(client.textRenderer, stack, x, y);
                }
            }
        }
    }
}
