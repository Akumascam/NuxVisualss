package com.nuxvisuals.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

import java.util.function.Predicate;

// Помощники для работы с инвентарём игрока.
// Индексы инвентаря: 0-8 хотбар, 9-35 основной, 40 — вторая рука.
// Слоты экрана инвентаря (syncId 0): 36-44 хотбар, 9-35 основной, 45 — вторая рука.
public final class InvUtil {
    public static int findHotbar(Predicate<ItemStack> filter) {
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p == null) return -1;
        for (int i = 0; i < 9; i++) {
            if (filter.test(p.getInventory().getStack(i))) return i;
        }
        return -1;
    }

    // сначала хотбар, потом основной инвентарь
    public static int findInventory(Predicate<ItemStack> filter) {
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p == null) return -1;
        for (int i = 0; i < 36; i++) {
            if (filter.test(p.getInventory().getStack(i))) return i;
        }
        return -1;
    }

    public static int screenSlot(int inventoryIndex) {
        return inventoryIndex < 9 ? 36 + inventoryIndex : inventoryIndex;
    }

    // применить предмет из слота хотбара и вернуть руку на прежний слот
    public static boolean useHotbar(int slot) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.interactionManager == null || slot < 0 || slot > 8) return false;
        int prev = p.getInventory().selectedSlot;
        p.getInventory().selectedSlot = slot;
        // interactItem сам отправляет серверу смену слота перед использованием
        ActionResult result = mc.interactionManager.interactItem(p, Hand.MAIN_HAND);
        if (result.isAccepted()) {
            p.swingHand(Hand.MAIN_HAND);
        }
        p.getInventory().selectedSlot = prev;
        return result.isAccepted();
    }

    // переложить предмет из основного инвентаря в слот хотбара (без открытия экрана)
    public static void swapToHotbar(int inventoryIndex, int hotbarSlot) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) return;
        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, screenSlot(inventoryIndex),
                hotbarSlot, SlotActionType.SWAP, mc.player);
    }

    public static void actionBar(String text) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal(text), true);
        }
    }

    private InvUtil() {
    }
}
