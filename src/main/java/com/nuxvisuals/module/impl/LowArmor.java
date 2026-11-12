package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.ChatUtil;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.ModeSetting;
import com.nuxvisuals.module.setting.SliderSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

// LowArmor: предупреждает о низкой прочности брони. Порог — отдельно для каждой части.
public class LowArmor extends Module {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final String[] NAMES = {"Шлем", "Нагрудник", "Поножи", "Ботинки"};

    private final SliderSetting helmet = add(new SliderSetting("Helmet", 20, 0, 100, 1, "%"));
    private final SliderSetting chest = add(new SliderSetting("Chestplate", 20, 0, 100, 1, "%"));
    private final SliderSetting legs = add(new SliderSetting("Leggings", 20, 0, 100, 1, "%"));
    private final SliderSetting boots = add(new SliderSetting("Boots", 20, 0, 100, 1, "%"));
    private final ModeSetting where = add(new ModeSetting("Notify In", "Screen", "Screen", "Chat", "Both"));
    private final BoolSetting sound = add(new BoolSetting("Sound", true));
    private final SliderSetting repeat = add(new SliderSetting("Repeat", 0, 0, 120, 5, " s"));

    private final boolean[] warned = new boolean[4];
    private final long[] lastWarn = new long[4];

    public LowArmor() {
        super("low-armor", "LowArmor", Category.Utilities, false);
    }

    private int limit(int i) {
        return switch (i) {
            case 0 -> helmet.getInt();
            case 1 -> chest.getInt();
            case 2 -> legs.getInt();
            default -> boots.getInt();
        };
    }

    @Override
    public void onDisable() {
        java.util.Arrays.fill(warned, false);
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (client.player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        for (int i = 0; i < 4; i++) {
            ItemStack stack = client.player.getEquippedStack(SLOTS[i]);
            int limit = limit(i);
            if (stack.isEmpty() || !stack.isDamageable() || limit <= 0) {
                warned[i] = false;
                continue;
            }
            int max = stack.getMaxDamage();
            int pct = (int) Math.floor((max - stack.getDamage()) * 100.0 / max);
            if (pct > limit) {
                warned[i] = false;
                continue;
            }
            boolean again = repeat.getInt() > 0 && now - lastWarn[i] >= repeat.getInt() * 1000L;
            if (!warned[i] || again) {
                warned[i] = true;
                lastWarn[i] = now;
                warn(client, NAMES[i], pct, max - stack.getDamage());
            }
        }
    }

    private void warn(MinecraftClient client, String name, int pct, int left) {
        String msg = "\u00a7f" + name + ": \u00a7c" + pct + "% \u00a77(" + left + ")";
        if (!where.is("Chat")) {
            client.inGameHud.setTitleTicks(5, 40, 10);
            client.inGameHud.setSubtitle(Text.literal(msg));
            client.inGameHud.setTitle(Text.literal("\u00a7cНизкая прочность!"));
        }
        if (!where.is("Screen")) {
            ChatUtil.info("\u00a7cНизкая прочность \u00a77— " + msg);
        }
        if (sound.get()) {
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.2f));
        }
    }
}
