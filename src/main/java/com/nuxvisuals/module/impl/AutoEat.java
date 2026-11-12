package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.InvUtil;
import com.nuxvisuals.module.Keys;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.SliderSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;

import java.util.function.Predicate;

// AutoEat: сам ест при голоде и запивает инвиз / скорость / силу, когда эффект заканчивается
public class AutoEat extends Module {
    private final BoolSetting eat = add(new BoolSetting("Eat Food", true));
    private final SliderSetting hunger = add(new SliderSetting("Hunger", 14, 1, 19, 1).visibleIf(eat::get));
    private final BoolSetting gapples = add(new BoolSetting("Golden Apples", false).visibleIf(eat::get));
    private final BoolSetting invisibility = add(new BoolSetting("Invisibility", false));
    private final BoolSetting speed = add(new BoolSetting("Speed", false));
    private final BoolSetting strength = add(new BoolSetting("Strength", false));
    private final SliderSetting refresh = add(new SliderSetting("Refresh Before", 5, 0, 60, 1, " s"));
    private final BoolSetting splash = add(new BoolSetting("Splash Potions", true));
    private final BoolSetting fromInventory = add(new BoolSetting("From Inventory", true));
    private final SliderSetting swapSlot = add(new SliderSetting("Swap Slot", 9, 1, 9, 1).visibleIf(fromInventory::get));
    private final BoolSetting notify = add(new BoolSetting("Notify", true));

    private int usingSlot = -1;
    private int prevSlot;
    private int useTicks;
    private boolean started;
    private int cooldown;

    public AutoEat() {
        super("auto-eat", "AutoEat", Category.Utilities, false);
    }

    private static boolean hasEffect(ItemStack s, RegistryEntry<StatusEffect> effect) {
        PotionContentsComponent pc = s.get(DataComponentTypes.POTION_CONTENTS);
        if (pc == null) {
            return false;
        }
        for (StatusEffectInstance e : pc.getEffects()) {
            if (e.getEffectType().equals(effect)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBadFood(ItemStack s) {
        return s.isOf(Items.ROTTEN_FLESH) || s.isOf(Items.SPIDER_EYE) || s.isOf(Items.POISONOUS_POTATO)
                || s.isOf(Items.PUFFERFISH) || s.isOf(Items.CHORUS_FRUIT) || s.isOf(Items.SUSPICIOUS_STEW);
    }

    private boolean needs(ClientPlayerEntity p, RegistryEntry<StatusEffect> effect) {
        StatusEffectInstance cur = p.getStatusEffect(effect);
        return cur == null || (!cur.isInfinite() && cur.getDuration() <= refresh.getInt() * 20);
    }

    @Override
    public void onDisable() {
        if (usingSlot >= 0) {
            finish(mc());
        }
    }

    @Override
    public void onTick(MinecraftClient client) {
        ClientPlayerEntity p = client.player;
        if (p == null || client.interactionManager == null) {
            usingSlot = -1;
            return;
        }
        if (usingSlot >= 0) {
            p.getInventory().selectedSlot = usingSlot;
            client.options.useKey.setPressed(true);
            if (p.isUsingItem()) {
                started = true;
            }
            useTicks++;
            if ((started && !p.isUsingItem()) || useTicks > 80 || (!started && useTicks > 10)) {
                finish(client);
            }
            return;
        }
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        if (client.currentScreen != null || p.isUsingItem() || p.isCreative()) {
            return;
        }
        if (invisibility.get() && needs(p, StatusEffects.INVISIBILITY) && tryPotion(client, StatusEffects.INVISIBILITY, "инвиз")) return;
        if (speed.get() && needs(p, StatusEffects.SPEED) && tryPotion(client, StatusEffects.SPEED, "скорость")) return;
        if (strength.get() && needs(p, StatusEffects.STRENGTH) && tryPotion(client, StatusEffects.STRENGTH, "сила")) return;
        if (eat.get() && p.getHungerManager().getFoodLevel() <= hunger.getInt()) {
            Predicate<ItemStack> food = s -> s.get(DataComponentTypes.FOOD) != null && !isBadFood(s)
                    && (gapples.get() || !(s.isOf(Items.GOLDEN_APPLE) || s.isOf(Items.ENCHANTED_GOLDEN_APPLE)));
            use(client, food, "еда", false);
        }
    }

    private boolean tryPotion(MinecraftClient client, RegistryEntry<StatusEffect> effect, String label) {
        if (use(client, s -> s.isOf(Items.POTION) && hasEffect(s, effect), label, false)) {
            return true;
        }
        return splash.get() && use(client, s -> s.isOf(Items.SPLASH_POTION) && hasEffect(s, effect), label, true);
    }

    private boolean use(MinecraftClient client, Predicate<ItemStack> filter, String label, boolean throwIt) {
        ClientPlayerEntity p = client.player;
        int slot = InvUtil.findHotbar(filter);
        if (slot < 0) {
            if (!fromInventory.get()) {
                return false;
            }
            int inv = InvUtil.findInventory(filter);
            if (inv < 9) {
                return false;
            }
            int target = swapSlot.getInt() - 1;
            SlotLock.runUnlocked(() -> InvUtil.swapToHotbar(inv, target));
            cooldown = 2; // в следующем тике предмет уже в хотбаре
            return true;
        }
        if (notify.get()) {
            InvUtil.actionBar("\u00a7bAutoEat: \u00a7f" + label);
        }
        if (throwIt) {
            // взрывное зелье — бросаем под ноги
            float pitch = p.getPitch();
            p.setPitch(90f);
            InvUtil.useHotbar(slot);
            p.setPitch(pitch);
            cooldown = 10;
            return true;
        }
        prevSlot = p.getInventory().selectedSlot;
        usingSlot = slot;
        useTicks = 0;
        started = false;
        p.getInventory().selectedSlot = slot;
        client.options.useKey.setPressed(true);
        client.interactionManager.interactItem(p, Hand.MAIN_HAND);
        return true;
    }

    private void finish(MinecraftClient client) {
        client.options.useKey.setPressed(client.currentScreen == null && Keys.isBindingDown(client.options.useKey));
        if (client.player != null) {
            client.player.getInventory().selectedSlot = prevSlot;
        }
        usingSlot = -1;
        cooldown = 10;
    }
}
