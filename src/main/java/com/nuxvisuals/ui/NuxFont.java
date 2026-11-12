package com.nuxvisuals.ui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

// Fixed smooth Inter font, adaptive ink, single pass without shadow or outline.
public final class NuxFont {
    public static final Identifier ID = Identifier.of("nuxvisuals", "gui");
    private static final Style STYLE = Style.EMPTY.withFont(ID);
    // крупный жирный шрифт для логотипа: assets/nuxvisuals/font/logo.json
    public static final Identifier LOGO_ID = Identifier.of("nuxvisuals", "logo");
    private static final Style LOGO_STYLE = Style.EMPTY.withFont(LOGO_ID);

    public static Text logo(String text) {
        return Text.literal(text).setStyle(LOGO_STYLE);
    }

    private static final java.util.regex.Pattern FORMAT=java.util.regex.Pattern.compile("§[0-9a-fk-or]",java.util.regex.Pattern.CASE_INSENSITIVE);
    public static Text of(String text) {
        String clean=FORMAT.matcher(text).replaceAll("");
        return Text.literal(clean).setStyle(STYLE);
    }

    public static Text of(Text text) {
        return of(text.getString());
    }

    public static int width(TextRenderer renderer, String text) {
        return renderer.getWidth(of(text));
    }

    public static int width(TextRenderer renderer, Text text) {
        return renderer.getWidth(of(text));
    }

    private static int background() {
        var gui = com.nuxvisuals.module.impl.ClickGuiModule.get();
        return gui == null ? 0xFF10141E : gui.panelColor();
    }
    public static void drawOn(DrawContext context,TextRenderer renderer,String text,int x,int y,int color,int bg) {
        drawInk(context,renderer,of(text),x,y,ThemeContrast.readable(color,bg));
    }
    public static void draw(DrawContext context,TextRenderer renderer,String text,int x,int y,int color) {
        drawOn(context,renderer,text,x,y,color,background());
    }
    public static void draw(DrawContext context,TextRenderer renderer,Text text,int x,int y,int color) {
        drawInk(context,renderer,of(text),x,y,ThemeContrast.readable(color,background()));
    }
    public static void drawCentered(DrawContext context,TextRenderer renderer,Text text,int cx,int y,int color) {
        Text styled=of(text);
        drawInk(context,renderer,styled,cx-renderer.getWidth(styled)/2,y,ThemeContrast.readable(color,background()));
    }
    private static void drawInk(DrawContext context,TextRenderer renderer,Text text,int x,int y,int color) {
        context.drawText(renderer,text,x,y,color,false);
    }
    private NuxFont() {}
}
