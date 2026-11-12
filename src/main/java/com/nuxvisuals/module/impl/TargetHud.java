package com.nuxvisuals.module.impl;

import com.nuxvisuals.hud.HudModule;
import com.nuxvisuals.hud.HudRender;
import com.nuxvisuals.module.Category;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

// Цель: голова, ник и полоска здоровья
public class TargetHud extends HudModule {
    private static LivingEntity lastHit;
    private static long hitTime;

    private LivingEntity target;
    private float shownHealth = -1;

    public TargetHud() {
        super("target-hud", "Target HUD", Category.Hud, true, 0.5, 0.6);
    }

    public static void onHit(Entity entity) {
        if (entity instanceof LivingEntity living) {
            lastHit = living;
            hitTime = System.currentTimeMillis();
        }
    }

    private LivingEntity findTarget(MinecraftClient client, boolean editing) {
        if (client.targetedEntity instanceof LivingEntity living && living.isAlive()) {
            return living;
        }
        if (lastHit != null && lastHit.isAlive() && lastHit.getWorld() == client.world
                && System.currentTimeMillis() - hitTime < 3000) {
            return lastHit;
        }
        return editing ? client.player : null;
    }

    @Override
    protected boolean shouldShow(boolean editing) {
        LivingEntity found = findTarget(mc(), editing);
        if (found != target) {
            shownHealth = -1;
        }
        target = found;
        return target != null;
    }

    @Override
    protected void measure(MinecraftClient client, boolean editing) {
        width = Math.max(96, 36 + textWidth(target.getName().getString()) + 26);
        height = 28;
    }

    @Override
    protected void draw(DrawContext context, MinecraftClient client, float tickDelta, boolean editing) {
        panel(context, 0, 0, width, height);
        // голова
        if (target instanceof AbstractClientPlayerEntity player) {
            PlayerSkinDrawer.draw(context, player.getSkinTextures(), 5, 5, 18);
        } else {
            HudRender.rect(context, 5, 5, 18, 18, 4, 0xFF2A3142);
            String letter = target.getName().getString().substring(0, 1).toUpperCase();
            text(context, letter, 14 - textWidth(letter) / 2, 10, 0xFFE8ECF5);
        }
        divider(context, 29, 7, 14);
        text(context, target.getName().getString(), 35, 10, 0xFFE8ECF5);

        // здоровье: число и вертикальная полоса справа
        float max = Math.max(1, target.getMaxHealth());
        float hp = target.getHealth() + target.getAbsorptionAmount();
        if (shownHealth < 0) {
            shownHealth = hp;
        }
        shownHealth += (hp - shownHealth) * 0.2f;
        String num = String.valueOf((int) Math.ceil(hp));
        text(context, num, width - 11 - textWidth(num), 4, accentColor(0));
        int barX = width - 8;
        int barTop = 5;
        int barH = 18;
        HudRender.rect(context, barX, barTop, 2, barH, 1, 0x30FFFFFF);
        int fill = Math.round(barH * Math.min(1f, shownHealth / max));
        if (fill > 0) {
            HudRender.rect(context, barX, barTop + barH - fill, 2, fill, 1, accentColor(0));
        }
    }
}
