package com.nuxvisuals.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.text.Text;

// Отправка сообщений/команд на сервер и локальные сообщения в чат
public final class ChatUtil {
    public static final String PREFIX = "\u00a7b[Nux] \u00a7r";

    // текст с "/" отправляется как команда, иначе — как сообщение в чат
    public static boolean send(String text) {
        if (text == null) {
            return false;
        }
        String t = text.trim();
        ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
        if (t.isEmpty() || handler == null) {
            return false;
        }
        if (t.startsWith("/")) {
            String cmd = t.substring(1).trim();
            if (cmd.isEmpty()) {
                return false;
            }
            handler.sendChatCommand(cmd);
        } else {
            handler.sendChatMessage(t);
        }
        return true;
    }

    // сообщение видно только тебе
    public static void info(String text) {
        info(Text.literal(text));
    }

    public static void info(Text text) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal(PREFIX).append(text), false);
        }
    }

    private ChatUtil() {
    }
}
