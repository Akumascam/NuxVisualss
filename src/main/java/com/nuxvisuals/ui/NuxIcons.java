package com.nuxvisuals.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

import java.util.IdentityHashMap;
import java.util.Map;

// Простые белые иконки, нарисованные квадратиками ('.' = пусто, 'X' = закрашено)
public final class NuxIcons {

    public static final String[] PERSON = {
            "...XXXX...",
            "..XXXXXX..",
            "..XXXXXX..",
            "...XXXX...",
            "....XX....",
            "...XXXX...",
            "..XXXXXX..",
            ".XXXXXXXX.",
            ".XXXXXXXX.",
            ".XX....XX.",
    };

    public static final String[] NETWORK = {
            ".XXXX..XXXX.",
            ".XXXX..XXXX.",
            "...XX...XX..",
            "..XXX..XXX..",
            ".XXXX.XXXX..",
            "XXXXX.XXXXX.",
            "XXXX..XXXX..",
    };

    public static final String[] PERSON_PLUS = {
            ".XXXX.......",
            "XXXXXX......",
            "XXXXXX......",
            ".XXXX...X...",
            "..XX....X...",
            ".XXXX..XXX..",
            "XXXXXX..X...",
            "XXXXXX......",
    };

    public static final String[] GEAR = {
            "..X....X..",
            ".X.X..X.X.",
            ".X.XXXX.X.",
            "..XXXXXX..",
            "XXX.XX.XXX",
            "XXX.XX.XXX",
            "..XXXXXX..",
            ".X.XXXX.X.",
            "..X....X..",
    };

    public static final String[] POWER = {
            "...XX...",
            "...XX...",
            "XX.XX.XX",
            "XX....XX",
            "XX....XX",
            "XX....XX",
            ".XX..XX.",
            "..XXXX..",
    };

    public static final String[] EYE = {
            "..XXXXXX..",
            ".XXXXXXXX.",
            "XXX.XX.XXX",
            ".XXXXXXXX.",
            "..XXXXXX..",
    };

    public static final String[] RAYS = {
            "X...X...X",
            "...X.X...",
            "....X....",
            "...X.X...",
            "X...X...X",
    };

    public static final String[] HUD = {
            "XXXXXXXXXXXX",
            "X..........X",
            "X.XXX......X",
            "X..........X",
            "X......XXX.X",
            "X..........X",
            "X..........X",
            "XXXXXXXXXXXX",
    };

    public static final String[] BULB = {
            "...XXX...",
            "..XXXXX..",
            ".XXXXXXX.",
            ".XX.X.XX.",
            ".XXXXXXX.",
            "..XXXXX..",
            "...XXX...",
            "....X....",
            "...XXX...",
            "..XXXXX..",
    };

    public static final String[] MAGNIFIER = {
            ".XXXXX....",
            "XX...XX...",
            "X.....X...",
            "X.....X...",
            "XX...XX...",
            ".XXXXX....",
            "....XX....",
            ".....XX...",
            "......XX..",
            ".......XX.",
    };

    public static final String[] DOT = {
            "XX",
            "XX",
    };

    public static final String[] TARGET = {
            ".XXXXX.",
            "X.....X",
            "X..X..X",
            "X.....X",
            ".XXXXX.",
    };

    public static final String[] CROSS = {
            "XX...XX",
            ".X.X.X.",
            "..X.X..",
            "...X...",
            "..X.X..",
            ".X.X.X.",
            "XX...XX",
    };

    public static final String[] MOON = {
            "...XXXX...",
            "..XXXXX...",
            ".XXXX.....",
            ".XXX......",
            "XXXX......",
            "XXXX......",
            ".XXX......",
            ".XXXX.....",
            "..XXXXX...",
            "...XXXX...",
    };

    // Гладкие картинки-иконки (assets/nuxvisuals/textures/icons/*.png).
    // Каждому старому «пиксельному» спрайту сопоставлена PNG-иконка; если её нет — рисуются квадратики.
    private static final Map<String[], Identifier> TEXTURES = new IdentityHashMap<>();

    public static void register(String[] sprite, String name) {
        TEXTURES.put(sprite, Identifier.of("nuxvisuals", "textures/icons/" + name + ".png"));
    }

    static {
        register(PERSON, "user");
        register(NETWORK, "network");
        register(PERSON_PLUS, "user_plus");
        register(GEAR, "gear");
        register(POWER, "power");
        register(EYE, "eye");
        register(RAYS, "sun");
        register(HUD, "hud");
        register(BULB, "bulb");
        register(MAGNIFIER, "search");
        register(DOT, "dot");
        register(TARGET, "target");
        register(CROSS, "cross");
        register(MOON, "moon");
    }

    private static final Identifier LOGO_AVATAR = Identifier.of("nuxvisuals", "textures/icons/nux_letter_clean.png");

    // Shared by Watermark HUD and Click GUI. The supplied artwork keeps its own colours.
    public static void logo(DrawContext context,int x,int y,int size,int accent) {
        int bg=accent|0xFF000000;
        NuxShapes.roundedRect(context,x,y,size,size,Math.max(2,size/4),bg);
        context.drawTexture(RenderLayer::getGuiTextured,LOGO_AVATAR,x,y,0f,0f,size,size,256,256,256,256,ThemeContrast.ink(bg));
    }

    private static final Identifier STAR=Identifier.of("nuxvisuals","textures/icons/favorite_smooth.png");
    public static void star(DrawContext context,int x,int y,int size,int color){
        context.drawTexture(RenderLayer::getGuiTextured,STAR,x,y,0f,0f,size,size,32,32,32,32,color);
    }

    public static void draw(DrawContext context, String[] sprite, int x, int y, int scale, int color) {
        Identifier texture = TEXTURES.get(sprite);
        if (texture != null) {
            int cols = 0;
            for (String line : sprite) {
                cols = Math.max(cols, line.length());
            }
            int rows = sprite.length;
            int size = (Math.max(cols, rows) + 1) * scale;
            int dx = Math.floorDiv(2 * x + cols * scale - size + 1, 2);
            int dy = Math.floorDiv(2 * y + rows * scale - size + 1, 2);
            context.drawTexture(RenderLayer::getGuiTextured, texture, dx, dy, 0f, 0f,
                    size, size, 32, 32, 32, 32, color);
            return;
        }
        for (int row = 0; row < sprite.length; row++) {
            String line = sprite[row];
            for (int col = 0; col < line.length(); col++) {
                if (line.charAt(col) != '.') {
                    context.fill(x + col * scale, y + row * scale,
                            x + (col + 1) * scale, y + (row + 1) * scale, color);
                }
            }
        }
    }

    // текст с градиентом по буквам, центрированный
    public static void drawGradientText(DrawContext context, net.minecraft.client.font.TextRenderer renderer,
                                        String text, int centerX, int y,
                                        int r1, int g1, int b1, int r2, int g2, int b2) {
        int x = centerX - NuxFont.width(renderer, text) / 2;
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            float t = (float) i / (text.length() - 1);
            int color = 0xFF000000
                    | (lerp(r1, r2, t) << 16)
                    | (lerp(g1, g2, t) << 8)
                    | lerp(b1, b2, t);
            NuxFont.draw(context, renderer, ch, x, y, color);
            x += NuxFont.width(renderer, ch);
        }
    }

    private static int lerp(int a, int b, float t) {
        return Math.round(a + (b - a) * t);
    }

    private NuxIcons() {
    }
}
