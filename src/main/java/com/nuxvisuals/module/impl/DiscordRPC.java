package com.nuxvisuals.module.impl;

import com.nuxvisuals.discord.DiscordIpc;
import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.StringSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

// Discord Rich Presence: в профиле Discord видно «Играет в NuxVisuals» + где ты сейчас.
// Application ID — из https://discord.com/developers/applications (приложение с названием NuxVisuals,
// в Rich Presence → Art Assets загрузить картинку с ключом "logo").
public class DiscordRPC extends Module {
    /** Вставь сюда ID своего Discord-приложения, чтобы он был у всех по умолчанию. */
    public static final String DEFAULT_APP_ID = "1559197715815071804";
    public static final String VERSION = "1.2.0";

    private final StringSetting appId = add(new StringSetting("Application ID", DEFAULT_APP_ID, 24));
    private final BoolSetting showServer = add(new BoolSetting("Show Server", true));
    private final BoolSetting showNick = add(new BoolSetting("Show Nick", false));
    private final BoolSetting button = add(new BoolSetting("Telegram Button", true));

    private final long started = System.currentTimeMillis() / 1000;
    private int timer;

    public DiscordRPC() {
        super("discord-rpc", "Discord RPC", Category.Utilities, true);
    }

    @Override
    public void onDisable() {
        DiscordIpc.stop();
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (timer-- > 0) {
            return;
        }
        timer = 20;
        String id = appId.get().trim();
        if (!id.matches("\\d{15,22}")) {
            id = DEFAULT_APP_ID;
        }
        if (!id.matches("\\d{15,22}")) {
            setDescription("Покажи NuxVisuals в профиле Discord. Впиши Application ID в настройках модуля");
            return;
        }
        setDescription(DiscordIpc.connected() ? "Статус в Discord: подключено" : "Статус в Discord: " + DiscordIpc.lastError());
        DiscordIpc.start(id);

        String details;
        if (client.world == null) {
            details = "В главном меню";
        } else if (client.isInSingleplayer()) {
            details = "Одиночная игра";
        } else {
            ServerInfo s = client.getCurrentServerEntry();
            details = showServer.get() && s != null ? "Играет на " + s.address : "Сетевая игра";
        }
        String state = showNick.get() && client.getSession() != null
                ? "Ник: " + client.getSession().getUsername() : "Minecraft 1.21.4 · Fabric";
        DiscordIpc.update(new DiscordIpc.Activity(details, state, started, "logo", "NuxVisuals " + VERSION,
                "", "", "NuxVisuals в Telegram", button.get() ? "https://t.me/NuxVisuals" : null));
    }
}
