package com.nuxvisuals.module;
import com.nuxvisuals.mixin.ItemCooldownManagerAccessor;
import com.nuxvisuals.mixin.CooldownEntryAccessor;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.ItemStack;
public final class CooldownInfo {
    private CooldownInfo() {}
    public static int remainingTicks(ItemCooldownManager manager, ItemStack stack) {
        if (manager == null || stack.isEmpty() || !manager.isCoolingDown(stack)) return 0;
        ItemCooldownManagerAccessor access = (ItemCooldownManagerAccessor) manager;
        Object entry = access.nux$getEntries().get(manager.getGroup(stack));
        return entry == null ? 0 : Math.max(0, ((CooldownEntryAccessor) entry).nux$getEndTick() - access.nux$getTick());
    }
    public static String format(int ticks) {
        if (ticks < 20) return String.format(java.util.Locale.ROOT, "%.1f", ticks / 20.0);
        return Integer.toString((ticks + 19) / 20);
    }
}
