package com.nuxvisuals.module.impl;

import com.nuxvisuals.mixin.MinecraftClientInvoker;
import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.ModeSetting;
import com.nuxvisuals.module.setting.SliderSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.hit.HitResult;

import java.util.Random;

// Auto Clicker: кликает за тебя с настраиваемым CPS
public class AutoClicker extends Module {
    private final ModeSetting button = add(new ModeSetting("Button", "Left", "Left", "Right", "Both"));
    private final ModeSetting mode = add(new ModeSetting("Mode", "Hold", "Hold", "Always"));
    private final SliderSetting minCps = add(new SliderSetting("Min CPS", 9, 1, 20, 1));
    private final SliderSetting maxCps = add(new SliderSetting("Max CPS", 12, 1, 20, 1));
    private final BoolSetting cooldown = add(new BoolSetting("1.9 Cooldown", false));
    private final BoolSetting onlyWeapon = add(new BoolSetting("Only Weapon", false));
    private final BoolSetting ignoreBlocks = add(new BoolSetting("Ignore Blocks", true));

    private final Random random = new Random();
    private double leftBudget;
    private double rightBudget;
    private double leftCps;
    private double rightCps;

    public AutoClicker() {
        super("auto-clicker", "Auto Clicker", Category.Utilities, false);
    }

    private double nextCps() {
        double lo = Math.min(minCps.get(), maxCps.get());
        double hi = Math.max(minCps.get(), maxCps.get());
        return lo + random.nextDouble() * (hi - lo);
    }

    @Override
    public void onEnable() {
        leftBudget = 0;
        rightBudget = 0;
        leftCps = nextCps();
        rightCps = nextCps();
    }

    @Override
    public void onTick(MinecraftClient client) {
        ClientPlayerEntity p = client.player;
        if (p == null || client.currentScreen != null || client.interactionManager == null) {
            return;
        }
        MinecraftClientInvoker inv = (MinecraftClientInvoker) client;
        boolean always = mode.is("Always");

        if (!button.is("Right")) {
            boolean held = always || client.options.attackKey.isPressed();
            boolean blocked = ignoreBlocks.get() && client.crosshairTarget != null
                    && client.crosshairTarget.getType() == HitResult.Type.BLOCK;
            if (held && !blocked && weaponOk(p.getMainHandStack())) {
                if (cooldown.get()) {
                    if (p.getAttackCooldownProgress(0.5f) >= 0.93f) {
                        inv.nuxvisuals$doAttack();
                    }
                } else {
                    leftBudget += leftCps / 20.0;
                    while (leftBudget >= 1) {
                        leftBudget -= 1;
                        inv.nuxvisuals$doAttack();
                        leftCps = nextCps();
                    }
                }
            } else {
                leftBudget = 0;
            }
        }
        if (!button.is("Left")) {
            boolean held = always || client.options.useKey.isPressed();
            if (held && !p.isUsingItem()) {
                rightBudget += rightCps / 20.0;
                while (rightBudget >= 1) {
                    rightBudget -= 1;
                    inv.nuxvisuals$setItemUseCooldown(0);
                    inv.nuxvisuals$doItemUse();
                    rightCps = nextCps();
                }
            } else {
                rightBudget = 0;
            }
        }
    }

    private boolean weaponOk(ItemStack stack) {
        return !onlyWeapon.get() || stack.isIn(ItemTags.SWORDS) || stack.isIn(ItemTags.AXES);
    }
}
