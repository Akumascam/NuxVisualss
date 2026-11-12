package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.ChatUtil;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.ModeSetting;
import com.nuxvisuals.module.setting.SliderSetting;
import com.nuxvisuals.waypoint.WaypointManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

/** Не даёт чату очищаться и схлопывает одинаковые сообщения. */
public class ChatHelper extends Module {
    public static ChatHelper INSTANCE;

    private final BoolSetting keepChat = add(new BoolSetting("Keep Chat", true));
    private final SliderSetting history = add(new SliderSetting("History Size", 500, 100, 2000, 50));
    private final BoolSetting antiSpam = add(new BoolSetting("Anti Duplicate", true));
    private final ModeSetting duplicateMode = add(new ModeSetting("Duplicates", "Counter", "Counter", "Hide")
            .visibleIf(() -> this.antiSpam.get()));
    private final SliderSetting range = add(new SliderSetting("Search Depth", 10, 1, 50, 1)
            .visibleIf(() -> this.antiSpam.get()));
    private final BoolSetting timestamps = add(new BoolSetting("Timestamps", false));
    private final BoolSetting deathCoords = add(new BoolSetting("Death Coords", true));
    private final BoolSetting deathWaypoint = add(new BoolSetting("Death Waypoint", false)
            .visibleIf(() -> this.deathCoords.get()));

    private boolean wasDead;

    public ChatHelper() {
        super("chat-helper", "ChatHelper", Category.Utilities, false);
        INSTANCE = this;
    }

    // координаты смерти — в чат (и по желанию метка Waypoint)
    @Override
    public void onTick(MinecraftClient client) {
        ClientPlayerEntity p = client.player;
        if (p == null) {
            wasDead = false;
            return;
        }
        boolean dead = p.isDead() || p.getHealth() <= 0;
        if (dead && !wasDead && deathCoords.get()) {
            int x = p.getBlockX(), y = p.getBlockY(), z = p.getBlockZ();
            String dim = WaypointManager.dimensionName(WaypointManager.dimension());
            ChatUtil.info("\u00a7cТы умер на \u00a7f" + x + " " + y + " " + z + " \u00a77(" + dim + ")");
            if (deathWaypoint.get()) {
                WaypointManager.add("Смерть", x, y, z);
            }
        }
        wasDead = dead;
    }

    private static boolean on() {
        return INSTANCE != null && INSTANCE.isEnabled();
    }

    public static boolean keepChat() {
        return on() && INSTANCE.keepChat.get();
    }

    public static int historySize(int vanilla) {
        return on() ? Math.max(vanilla, INSTANCE.history.getInt()) : vanilla;
    }

    public static boolean antiDuplicate() {
        return on() && INSTANCE.antiSpam.get();
    }

    public static boolean hideDuplicates() {
        return INSTANCE.duplicateMode.is("Hide");
    }

    public static int searchDepth() {
        return INSTANCE.range.getInt();
    }

    public static boolean timestamps() {
        return on() && INSTANCE.timestamps.get();
    }
}
