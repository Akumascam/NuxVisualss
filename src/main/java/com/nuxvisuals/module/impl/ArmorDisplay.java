package com.nuxvisuals.module.impl;

import com.nuxvisuals.hud.HudIcons;
import com.nuxvisuals.hud.HudModule;
import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.ui.NuxShapes;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

// Компактная плашка с бронёй и полосками прочности
public class ArmorDisplay extends HudModule {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private final BoolSetting durabilityBar = add(new BoolSetting("Durability", true));

    public ArmorDisplay() {
        super("armor", "Armor", Category.Hud, true, 0.5, 0.78);
    }

    @Override
    protected void measure(MinecraftClient client, boolean editing) {
        width = 7 + 7 + 7 + SLOTS.length * 15 + 3;
        height = 17;
    }

    @Override
    protected void draw(DrawContext context, MinecraftClient client, float tickDelta, boolean editing) {
        panel(context, 0, 0, width, height);
        HudIcons.draw(context, HudIcons.SHIELD, 7, 5, accentColor(0));
        int x = 21;
        for (EquipmentSlot slot : SLOTS) {
            divider(context, x - 1, 5, 7);
            ItemStack stack = client.player.getEquippedStack(slot);
            if (!stack.isEmpty()) {
                context.getMatrices().push();
                context.getMatrices().translate(x + 3, 3, 0);
                context.getMatrices().scale(0.625f, 0.625f, 1f);
                context.drawItem(stack, 0, 0);
                context.getMatrices().pop();
                if (durabilityBar.get() && stack.isDamageable()) {
                    float k = (stack.getMaxDamage() - stack.getDamage()) / (float) stack.getMaxDamage();
                    int color = k > 0.5f ? NuxShapes.blend(0xFFFACC15, 0xFF4ADE80, (k - 0.5f) * 2f)
                            : NuxShapes.blend(0xFFEF4444, 0xFFFACC15, k * 2f);
                    context.fill(x + 3, 14, x + 3 + Math.round(10 * k), 15, color);
                }
            } else {
                context.fill(x + 7, 8, x + 9, 10, 0x30FFFFFF);
            }
            x += 15;
        }
    }
}
