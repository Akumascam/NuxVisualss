package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.ModeSetting;
import com.nuxvisuals.module.setting.StringSetting;
import net.minecraft.client.MinecraftClient;

import java.util.Locale;
import java.util.Set;

/** Streamer protection: replaces the account nickname everywhere and masks auth-command passwords. */
public class Protected extends Module {
    public static Protected INSTANCE;

    private static final Set<String> AUTH_COMMANDS = Set.of(
            "l", "login", "log", "reg", "register", "changepassword", "changepass", "changepw",
            "cp", "unregister", "unreg", "premium", "2fa", "authme", "email");

    private final BoolSetting hideNick = add(new BoolSetting("Hide Nick", true));
    private final ModeSetting nickMode = add(new ModeSetting("Nick", "NuxVisual", "NuxVisual", "Custom")
            .visibleIf(() -> this.hideNick.get()));
    private final StringSetting customNick = add(new StringSetting("Custom Nick", "NuxVisual", 24)
            .visibleIf(() -> this.hideNick.get() && this.nickMode.is("Custom")));
    private final BoolSetting hidePassword = add(new BoolSetting("Hide Password", true));

    public Protected() {
        super("protected", "Protected", Category.Utilities, false);
        setDescription("Скрывает ник и пароль при входе на сервер");
        INSTANCE = this;
    }

    /** Nickname to hide, or null when nickname protection is inactive. */
    public static String hiddenName() {
        Protected p = INSTANCE;
        if (p == null || !p.isEnabled() || !p.hideNick.get()) {
            return null;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getSession() == null) {
            return null;
        }
        String name = client.getSession().getUsername();
        return name == null || name.length() < 2 ? null : name;
    }

    public static String replacement() {
        Protected p = INSTANCE;
        if (p != null && p.nickMode.is("Custom") && !p.customNick.get().isBlank()) {
            return p.customNick.get();
        }
        return "NuxVisual";
    }

    public static boolean hidePassword() {
        Protected p = INSTANCE;
        return p != null && p.isEnabled() && p.hidePassword.get();
    }

    /**
     * Returns the same-length text with every argument after an auth command replaced by '*',
     * or null if the text is not an auth command.
     */
    public static String maskAuthCommand(String text) {
        if (text == null || !text.startsWith("/")) {
            return null;
        }
        int end = text.indexOf(' ');
        if (end < 0) {
            return null;
        }
        String command = text.substring(1, end).toLowerCase(Locale.ROOT);
        int colon = command.indexOf(':');
        if (colon >= 0) {
            command = command.substring(colon + 1);
        }
        if (!AUTH_COMMANDS.contains(command)) {
            return null;
        }
        StringBuilder out = new StringBuilder(text.length());
        out.append(text, 0, end);
        for (int i = end; i < text.length(); i++) {
            char c = text.charAt(i);
            out.append(c == ' ' ? ' ' : '*');
        }
        return out.toString();
    }
}
