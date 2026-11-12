package com.nuxvisuals.module.impl;

import com.nuxvisuals.mixin.MinecraftClientInvoker;
import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.SliderSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

// Fast Exp: пузырьки опыта (и по желанию другие броски) летят быстрее
public class FastExp extends Module {
    private final SliderSetting delay = add(new SliderSetting("Delay", 0, 0, 4, 1, " tick"));
    private final BoolSetting allThrowables = add(new BoolSetting("All Throwables", false));

    public FastExp() {
        super("fast-exp", "Fast Exp", Category.Utilities, false);
    }

    private boolean fits(ItemStack stack) {
        if (stack.isOf(Items.EXPERIENCE_BOTTLE)) {
            return true;
        }
        return allThrowables.get() && (stack.isOf(Items.SNOWBALL) || stack.isOf(Items.EGG)
                || stack.isOf(Items.SPLASH_POTION) || stack.isOf(Items.LINGERING_POTION));
    }

    @Override
    public void onTick(MinecraftClient client) {
        ClientPlayerEntity p = client.player;
        if (p == null || client.currentScreen != null || !client.options.useKey.isPressed()) {
            return;
        }
        if (!fits(p.getMainHandStack()) && !fits(p.getOffHandStack())) {
            return;
        }
        MinecraftClientInvoker inv = (MinecraftClientInvoker) client;
        int d = delay.getInt();
        if (d == 0) {
            // 0 = дополнительный бросок каждый тик (примерно 40 в секунду)
            if (!p.isUsingItem()) {
                inv.nuxvisuals$setItemUseCooldown(0);
                inv.nuxvisuals$doItemUse();
            }
            inv.nuxvisuals$setItemUseCooldown(0);
        } else if (inv.nuxvisuals$getItemUseCooldown() > d) {
            inv.nuxvisuals$setItemUseCooldown(d);
        }
    }
}
