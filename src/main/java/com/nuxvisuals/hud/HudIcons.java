package com.nuxvisuals.hud;

import com.nuxvisuals.ui.NuxIcons;
import net.minecraft.client.gui.DrawContext;

// Маленькие иконки 7x7 для HUD
public final class HudIcons {
    public static final String[] LOGO = {"X...X", "XX..X", "X.X.X", "X..XX", "X...X"};
    public static final String[] USER = {"..XXX..", "..XXX..", "..XXX..", ".......", ".XXXXX.", "XXXXXXX", "XXXXXXX"};
    public static final String[] MONITOR = {"XXXXXXX", "X.....X", "X.....X", "XXXXXXX", "...X...", "..XXX.."};
    public static final String[] PING = {".X..X..", "XXX.X..", ".X..X..", ".X..X..", ".X.XXX.", ".X..X.."};
    public static final String[] FLASK = {"..XXX..", "...X...", "...X...", "..X.X..", ".X...X.", "X.XXX.X", "XXXXXXX"};
    public static final String[] COMMAND = {"XX...XX", "X.X.X.X", ".XXXXX.", "..X.X..", ".XXXXX.", "X.X.X.X", "XX...XX"};
    public static final String[] CLOCK = {".XXXXX.", "X..X..X", "X..X..X", "X..XX.X", "X.....X", "X.....X", ".XXXXX."};
    public static final String[] SHIELD = {"XXXXXXX", "XXXXXXX", "XXXXXXX", ".XXXXX.", ".XXXXX.", "..XXX..", "...X..."};

    static {
        NuxIcons.register(USER, "user");
        NuxIcons.register(MONITOR, "monitor");
        NuxIcons.register(PING, "ping");
        NuxIcons.register(FLASK, "flask");
        NuxIcons.register(COMMAND, "keyboard");
        NuxIcons.register(CLOCK, "clock");
        NuxIcons.register(SHIELD, "shield");
    }

    public static void draw(DrawContext context, String[] icon, int x, int y, int color) {
        NuxIcons.draw(context, icon, x, y, 1, color);
    }

    private HudIcons() {
    }
}
