package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.ChatUtil;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.ModeSetting;
import com.nuxvisuals.module.setting.StringSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.Locale;

// ItemPickup: пишет в чат, какой донатный предмет ты подобрал
public class ItemPickup extends Module {
    public static ItemPickup INSTANCE;

    private final ModeSetting mode = add(new ModeSetting("Items", "Donate", "Donate", "All"));
    private final BoolSetting customName = add(new BoolSetting("Custom Name", true).visibleIf(() -> this.mode.is("Donate")));
    private final BoolSetting lore = add(new BoolSetting("With Lore", true).visibleIf(() -> this.mode.is("Donate")));
    private final BoolSetting enchanted = add(new BoolSetting("Enchanted", false).visibleIf(() -> this.mode.is("Donate")));
    private final StringSetting keywords = add(new StringSetting("Keywords",
            "трап,пласт,сфер,талисман,дезор,аура,смерч,пыль,исцел", 256).visibleIf(() -> this.mode.is("Donate")));
    private final BoolSetting sound = add(new BoolSetting("Sound", true));

    public ItemPickup() {
        super("item-pickup", "ItemPickup", Category.Utilities, false);
        INSTANCE = this;
    }

    private boolean isDonate(ItemStack s) {
        if (customName.get() && s.contains(DataComponentTypes.CUSTOM_NAME)) {
            return true;
        }
        if (lore.get()) {
            LoreComponent lc = s.get(DataComponentTypes.LORE);
            if (lc != null && !lc.lines().isEmpty()) {
                return true;
            }
        }
        if (enchanted.get() && s.hasEnchantments()) {
            return true;
        }
        String name = s.getName().getString().toLowerCase(Locale.ROOT);
        for (String k : keywords.get().toLowerCase(Locale.ROOT).split(",")) {
            String key = k.trim();
            if (!key.isEmpty() && name.contains(key)) {
                return true;
            }
        }
        return false;
    }

    // из ClientPlayNetworkHandlerMixin (пакет анимации подбора, до удаления предмета)
    public static void onPickup(int entityId, int collectorId, int amount) {
        if (INSTANCE == null || !INSTANCE.isEnabled()) {
            return;
        }
        MinecraftClient mc = mc();
        if (mc.player == null || mc.world == null || collectorId != mc.player.getId()) {
            return;
        }
        Entity entity = mc.world.getEntityById(entityId);
        if (!(entity instanceof ItemEntity item)) {
            return;
        }
        ItemStack s = item.getStack();
        if (s.isEmpty() || (INSTANCE.mode.is("Donate") && !INSTANCE.isDonate(s))) {
            return;
        }
        int count = amount > 0 ? amount : s.getCount();
        ChatUtil.info(Text.literal("\u00a7dПодобран: \u00a7r").append(s.getName())
                .append(Text.literal(" \u00a77x" + count)));
        if (INSTANCE.sound.get()) {
            mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.4f));
        }
    }
}
