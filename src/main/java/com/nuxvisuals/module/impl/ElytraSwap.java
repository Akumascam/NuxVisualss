package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.InvUtil;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.KeySetting;
import com.nuxvisuals.module.setting.SliderSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

// ElytraSwap: по клавише открывает инвентарь и меняет нагрудник <-> элитры
public class ElytraSwap extends Module {
    private static final int CHEST_SCREEN_SLOT = 6;
    private static final Item[] CHESTPLATES = {
            Items.NETHERITE_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.IRON_CHESTPLATE,
            Items.CHAINMAIL_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.LEATHER_CHESTPLATE
    };

    private final KeySetting key = add(new KeySetting("Swap Key", -1));
    private final BoolSetting openInventory = add(new BoolSetting("Open Inventory", true));
    private final SliderSetting delay = add(new SliderSetting("Delay", 2, 0, 10, 1, " tick"));
    private final BoolSetting notify = add(new BoolSetting("Notify", true));

    private int stage = -1;
    private int timer;
    private int slotToSwap = -1;
    private String targetName = "";

    public ElytraSwap() {
        super("elytra-swap", "ElytraSwap", Category.Utilities, false);
    }

    private static boolean isChestArmor(ItemStack s) {
        if (s.isEmpty() || s.isOf(Items.ELYTRA)) {
            return false;
        }
        EquippableComponent eq = s.get(DataComponentTypes.EQUIPPABLE);
        return eq != null && eq.slot() == EquipmentSlot.CHEST;
    }

    // лучший нагрудник: незерит > алмаз > ... > любой другой
    private static int findChestplate() {
        for (Item item : CHESTPLATES) {
            int slot = InvUtil.findInventory(s -> s.isOf(item));
            if (slot >= 0) {
                return slot;
            }
        }
        return InvUtil.findInventory(ElytraSwap::isChestArmor);
    }

    @Override
    public void onDisable() {
        stage = -1;
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null) {
            stage = -1;
            return;
        }
        if (stage < 0) {
            if (key.pressed()) {
                begin(client);
            }
            return;
        }
        if (timer-- > 0) {
            return;
        }
        if (stage == 0) {
            int sync = client.player.playerScreenHandler.syncId;
            SlotLock.runUnlocked(() -> {
                if (slotToSwap < 9) {
                    // предмет в хотбаре — обмен цифрой
                    client.interactionManager.clickSlot(sync, CHEST_SCREEN_SLOT, slotToSwap, SlotActionType.SWAP, client.player);
                } else {
                    int src = InvUtil.screenSlot(slotToSwap);
                    client.interactionManager.clickSlot(sync, src, 0, SlotActionType.PICKUP, client.player);
                    client.interactionManager.clickSlot(sync, CHEST_SCREEN_SLOT, 0, SlotActionType.PICKUP, client.player);
                    client.interactionManager.clickSlot(sync, src, 0, SlotActionType.PICKUP, client.player);
                }
            });
            stage = 1;
            timer = delay.getInt();
            return;
        }
        if (client.currentScreen instanceof InventoryScreen) {
            client.player.closeHandledScreen();
        }
        if (notify.get()) {
            InvUtil.actionBar("\u00a7bНадето: \u00a7f" + targetName);
        }
        stage = -1;
    }

    private void begin(MinecraftClient client) {
        if (client.currentScreen != null && !(client.currentScreen instanceof InventoryScreen)) {
            return;
        }
        boolean wearingElytra = client.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
        int slot = wearingElytra ? findChestplate() : InvUtil.findInventory(s -> s.isOf(Items.ELYTRA));
        targetName = wearingElytra ? "нагрудник" : "элитры";
        if (slot < 0) {
            if (notify.get()) {
                InvUtil.actionBar("\u00a7cНет в инвентаре: \u00a7f" + targetName);
            }
            return;
        }
        slotToSwap = slot;
        if (openInventory.get() && client.currentScreen == null) {
            client.setScreen(new InventoryScreen(client.player));
        }
        stage = 0;
        timer = openInventory.get() ? delay.getInt() : 0;
    }
}
