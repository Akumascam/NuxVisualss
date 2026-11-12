package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.InvUtil;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.KeySetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

// SlotLock: защищает слоты/предметы от выбрасывания (Q) и, по желанию, от перекладывания
public class SlotLock extends Module {
    public static SlotLock INSTANCE;
    private static boolean bypass;

    private final BoolSetting allSlots = add(new BoolSetting("All Slots", false));
    private final BoolSetting swords = add(new BoolSetting("Swords", true));
    private final BoolSetting axes = add(new BoolSetting("Axes", false));
    private final BoolSetting totems = add(new BoolSetting("Totems", true));
    private final BoolSetting gapples = add(new BoolSetting("Golden Apples", true));
    private final BoolSetting armor = add(new BoolSetting("Armor", false));
    private final BoolSetting pearls = add(new BoolSetting("Pearls", false));
    private final BoolSetting donate = add(new BoolSetting("Donate Items", true));
    private final BoolSetting blockDrop = add(new BoolSetting("Block Drop", true));
    private final BoolSetting blockMove = add(new BoolSetting("Block Moving", false));
    private final KeySetting lockKey = add(new KeySetting("Lock Current Slot", -1));
    private final BoolSetting notify = add(new BoolSetting("Notify", true));
    private final BoolSetting[] slots = new BoolSetting[9];

    private long lastWarn;

    public SlotLock() {
        super("slot-lock", "SlotLock", Category.Utilities, false);
        INSTANCE = this;
        for (int i = 0; i < 9; i++) {
            slots[i] = add(new BoolSetting("Slot " + (i + 1), false).visibleIf(() -> !this.allSlots.get()));
        }
    }

    private static boolean on() {
        return !bypass && INSTANCE != null && INSTANCE.isEnabled();
    }

    // действия самого клиента (AutoEat, ElytraSwap...) — без блокировки
    public static void runUnlocked(Runnable action) {
        boolean prev = bypass;
        bypass = true;
        try {
            action.run();
        } finally {
            bypass = prev;
        }
    }

    // index — индекс в инвентаре игрока (0-8 хотбар, 9-35, 36-39 броня, 40 вторая рука)
    public boolean isLocked(int index, ItemStack s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        if (allSlots.get()) {
            return true;
        }
        if (index >= 0 && index < 9 && slots[index].get()) {
            return true;
        }
        Item it = s.getItem();
        if (swords.get() && (it instanceof SwordItem || s.isOf(Items.MACE))) return true;
        if (axes.get() && it instanceof AxeItem) return true;
        if (totems.get() && s.isOf(Items.TOTEM_OF_UNDYING)) return true;
        if (gapples.get() && (s.isOf(Items.GOLDEN_APPLE) || s.isOf(Items.ENCHANTED_GOLDEN_APPLE))) return true;
        if (armor.get() && (it instanceof ArmorItem || s.isOf(Items.ELYTRA))) return true;
        if (pearls.get() && s.isOf(Items.ENDER_PEARL)) return true;
        return donate.get() && s.contains(DataComponentTypes.CUSTOM_NAME);
    }

    private void warn() {
        long now = System.currentTimeMillis();
        if (notify.get() && now - lastWarn > 700) {
            lastWarn = now;
            InvUtil.actionBar("\u00a7cСлот заблокирован \u00a77(SlotLock)");
        }
    }

    // Q / Ctrl+Q в руке
    public static boolean blockDrop() {
        if (!on() || !INSTANCE.blockDrop.get()) {
            return false;
        }
        ClientPlayerEntity p = mc().player;
        if (p == null) {
            return false;
        }
        if (INSTANCE.isLocked(p.getInventory().selectedSlot, p.getMainHandStack())) {
            INSTANCE.warn();
            return true;
        }
        return false;
    }

    // клики в инвентаре
    public static boolean blockClick(int syncId, int slotId, int button, SlotActionType action) {
        if (!on()) {
            return false;
        }
        ClientPlayerEntity p = mc().player;
        if (p == null) {
            return false;
        }
        ScreenHandler h = p.currentScreenHandler;
        if (h == null || h.syncId != syncId || slotId < 0 || slotId >= h.slots.size()) {
            return false;
        }
        Slot slot = h.slots.get(slotId);
        boolean locked = slot.inventory instanceof PlayerInventory && INSTANCE.isLocked(slot.getIndex(), slot.getStack());
        if (action == SlotActionType.THROW) {
            if (locked && INSTANCE.blockDrop.get()) {
                INSTANCE.warn();
                return true;
            }
            return false;
        }
        if (!INSTANCE.blockMove.get() || action == SlotActionType.CLONE) {
            return false;
        }
        if (locked) {
            INSTANCE.warn();
            return true;
        }
        if (action == SlotActionType.SWAP && ((button >= 0 && button < 9) || button == 40)
                && INSTANCE.isLocked(button, p.getInventory().getStack(button))) {
            INSTANCE.warn();
            return true;
        }
        return false;
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (client.player == null) {
            return;
        }
        if (lockKey.pressed()) {
            int sel = client.player.getInventory().selectedSlot;
            slots[sel].toggle();
            InvUtil.actionBar(slots[sel].get()
                    ? "\u00a7aСлот " + (sel + 1) + " заблокирован"
                    : "\u00a7eСлот " + (sel + 1) + " разблокирован");
        }
    }
}
