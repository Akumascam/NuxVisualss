package com.nuxvisuals.module.impl;

import com.nuxvisuals.friend.FriendManager;
import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.ChatUtil;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.SliderSetting;
import com.nuxvisuals.module.setting.StringSetting;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

// AutoAccept: сам принимает запросы на телепорт (/tpa). Отдельно для друзей и обычных игроков.
public class AutoAccept extends Module {
    public static AutoAccept INSTANCE;

    private final BoolSetting friends = add(new BoolSetting("Friends", true));
    private final SliderSetting friendDelay = add(new SliderSetting("Friends Delay", 0, 0, 5, 0.1, " s")
            .visibleIf(() -> this.friends.get()));
    private final BoolSetting players = add(new BoolSetting("Players", false));
    private final SliderSetting playerDelay = add(new SliderSetting("Players Delay", 1, 0, 5, 0.1, " s")
            .visibleIf(() -> this.players.get()));
    private final StringSetting command = add(new StringSetting("Command", "/tpaccept", 64));
    private final BoolSetting addNick = add(new BoolSetting("Add Nick", true));
    private final BoolSetting notify = add(new BoolSetting("Notify", true));

    private String pendingCommand;
    private String pendingNick;
    private int pendingTicks = -1;

    public AutoAccept() {
        super("auto-accept", "AutoAccept", Category.Utilities, false);
        INSTANCE = this;
    }

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay && INSTANCE != null && INSTANCE.isEnabled()) {
                INSTANCE.onMessage(message.getString());
            }
        });
    }

    private static boolean isRequest(String lower) {
        boolean ru = lower.contains("телепорт") && (lower.contains("прос") || lower.contains("хочет")
                || lower.contains("запрос") || lower.contains("/tpaccept"));
        boolean en = lower.contains("has requested") || lower.contains("requested to teleport")
                || lower.contains("wants to teleport") || lower.contains("/tpaccept");
        return ru || en;
    }

    private void onMessage(String raw) {
        String lower = raw.toLowerCase(Locale.ROOT);
        // свои ответы сервера ("вы приняли запрос") — не трогаем
        if (!isRequest(lower) || lower.contains("принял") || lower.contains("accepted")) {
            return;
        }
        String nick = findNick(raw);
        if (nick == null) {
            return;
        }
        boolean friend = FriendManager.isFriend(nick);
        if (friend ? !friends.get() : !players.get()) {
            return;
        }
        String base = command.get().trim();
        if (base.isEmpty()) {
            base = "/tpaccept";
        }
        pendingCommand = addNick.get() ? base + " " + nick : base;
        pendingNick = nick;
        double delay = friend ? friendDelay.get() : playerDelay.get();
        pendingTicks = (int) Math.round(delay * 20);
    }

    // ник отправителя: ищем среди игроков в табе и среди друзей
    private static String findNick(String raw) {
        MinecraftClient mc = mc();
        String self = mc.player != null ? mc.player.getGameProfile().getName() : "";
        Set<String> names = new LinkedHashSet<>(FriendManager.list());
        if (mc.getNetworkHandler() != null) {
            for (PlayerListEntry e : mc.getNetworkHandler().getPlayerList()) {
                names.add(e.getProfile().getName());
            }
        }
        String best = null;
        for (String name : names) {
            if (name == null || name.isEmpty() || name.equalsIgnoreCase(self)) {
                continue;
            }
            Pattern p = Pattern.compile("(?<![A-Za-z0-9_])" + Pattern.quote(name) + "(?![A-Za-z0-9_])", Pattern.CASE_INSENSITIVE);
            if (p.matcher(raw).find() && (best == null || name.length() > best.length())) {
                best = name;
            }
        }
        return best;
    }

    @Override
    public void onDisable() {
        pendingTicks = -1;
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (pendingTicks < 0 || client.player == null) {
            return;
        }
        if (pendingTicks-- > 0) {
            return;
        }
        pendingTicks = -1;
        if (ChatUtil.send(pendingCommand) && notify.get()) {
            ChatUtil.info("\u00a7aПринят запрос от \u00a7f" + pendingNick
                    + (FriendManager.isFriend(pendingNick) ? " \u00a7d(друг)" : ""));
        }
    }
}
