package com.nuxvisuals.module.impl;

import com.nuxvisuals.hud.HudClickable;
import com.nuxvisuals.hud.HudModule;
import com.nuxvisuals.hud.HudRender;
import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.KeySetting;
import com.nuxvisuals.music.MediaBridge;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

// Музыка: что сейчас играет в Spotify / браузере / любом плеере Windows.
// Обложка, название, исполнитель, прогресс. Управление — клавишами или кнопками (открой чат и нажми).
public class MusicHud extends HudModule implements HudClickable {
    private static final Identifier COVER = Identifier.of("nuxvisuals", "music_cover");
    private static final int W = 182;
    private static final int H = 48;

    private final BoolSetting showCover = add(new BoolSetting("Show Cover", true));
    private final BoolSetting showProgress = add(new BoolSetting("Show Progress", true));
    private final BoolSetting showApp = add(new BoolSetting("Show App", true));
    private final BoolSetting hideIdle = add(new BoolSetting("Hide When Idle", true));
    private final KeySetting playKey = add(new KeySetting("Play/Pause Key", -1));
    private final KeySetting nextKey = add(new KeySetting("Next Key", -1));
    private final KeySetting prevKey = add(new KeySetting("Previous Key", -1));

    private int loadedCover = -1;
    private String coverTrack="";
    private final com.nuxvisuals.music.CoverLoader coverLoader=new com.nuxvisuals.music.CoverLoader();
    private int coverWidth=64,coverHeight=64;
    private boolean coverReady;
    private long lastCoverTry;
    private long pressedAt;
    private int pressedButton = -1;

    public MusicHud() {
        super("music", "Music", Category.Hud, false, 0.01, 0.35);
    }

    @Override
    public void onTick(MinecraftClient client) {
        MediaBridge.start();
        if (playKey.pressed()) press(1, "toggle");
        if (nextKey.pressed()) press(2, "next");
        if (prevKey.pressed()) press(0, "prev");
    }

    @Override
    public void onDisable() {
        releaseCover(mc());
        com.nuxvisuals.module.Module lyrics = com.nuxvisuals.module.ModuleManager.get("world-lyrics");
        if (lyrics == null || !lyrics.isEnabled()) {
            MediaBridge.stop();
        }
    }

    private void press(int button, String command) {
        MediaBridge.send(command);
        pressedButton = button;
        pressedAt = System.currentTimeMillis();
    }

    @Override
    public boolean click(double x, double y) {
        if (!MediaBridge.WINDOWS || !MediaBridge.hasTrack() || y < 33 || y > H) {
            return false;
        }
        int c = controlsCenter();
        if (Math.abs(x - (c - 15)) <= 6) {
            press(0, "prev");
            return true;
        }
        if (Math.abs(x - c) <= 7) {
            press(1, "toggle");
            return true;
        }
        if (Math.abs(x - (c + 15)) <= 6) {
            press(2, "next");
            return true;
        }
        return false;
    }

    private int textLeft() {
        return showCover.get() ? 48 : 8;
    }

    private int controlsCenter() {
        return (textLeft() + W - 6) / 2;
    }

    @Override
    protected boolean shouldShow(boolean editing) {
        if (editing) {
            return true;
        }
        if (!MediaBridge.WINDOWS) {
            return false;
        }
        return MediaBridge.hasTrack() || !hideIdle.get();
    }

    @Override
    protected void measure(MinecraftClient client, boolean editing) {
        width = W;
        height = (!MediaBridge.WINDOWS || !MediaBridge.hasTrack()) ? 22 : H;
    }

    @Override
    protected void draw(DrawContext context, MinecraftClient client, float tickDelta, boolean editing) {
        panel(context, 0, 0, width, height);
        int accent = accentColor(0);
        if (!MediaBridge.WINDOWS) {
            note(context, 8, 7, accent);
            text(context, "Музыка: только для Windows", 20, 7, 0xFFB8C2D9);
            return;
        }
        if (!MediaBridge.hasTrack()) {
            note(context, 8, 7, accent);
            String msg = !MediaBridge.error.isEmpty() ? MediaBridge.error
                    : MediaBridge.running() ? "Ничего не играет" : "Запуск...";
            text(context, msg, 20, 7, 0xFFB8C2D9);
            return;
        }

        int left = textLeft();
        // обложка
        if (showCover.get()) {
            updateCover(client);
            if (coverReady) {
                context.drawTexture(RenderLayer::getGuiTextured, COVER, 6, 6, 0f, 0f, 36, 36, coverWidth, coverHeight, coverWidth, coverHeight);
                // скруглить углы под цвет панели
                corners(context, 6, 6, 36);
            } else {
                HudRender.rect(context, 6, 6, 36, 36, 6, 0xFF232A3B);
                note(context, 20, 20, accent);
            }
        }

        // название (бегущая строка, если не влезает) и исполнитель
        int areaW = W - 6 - left;
        marquee(context, MediaBridge.title, left, 7, areaW, 0xFFFFFFFF);
        String sub = MediaBridge.artist;
        String appName = MediaBridge.appName();
        if (showApp.get() && !appName.isEmpty()) {
            sub = sub.isEmpty() ? appName : sub + "  ·  " + appName;
        }
        marquee(context, sub, left, 18, areaW, 0xFF9AA6C2);

        // прогресс
        double dur = MediaBridge.duration;
        double pos = MediaBridge.livePosition();
        if (showProgress.get() && dur > 1) {
            int barY = 30;
            HudRender.rect(context, left, barY, areaW, 3, 1, 0x40FFFFFF);
            int filled = (int) Math.round(areaW * Math.min(1, pos / dur));
            if (filled > 0) {
                HudRender.rect(context, left, barY, Math.max(2, filled), 3, 1, accent);
            }
            text(context, time(pos), left, 36, 0xFF7F8AA6);
            String total = time(dur);
            text(context, total, left + areaW - textWidth(total), 36, 0xFF7F8AA6);
        }

        // кнопки
        int c = controlsCenter();
        int cy = 39;
        boolean flash = System.currentTimeMillis() - pressedAt < 180;
        int base = 0xFFE8ECF5;
        prev(context, c - 15, cy, flash && pressedButton == 0 ? accent : base);
        HudRender.circle(context, c, cy, 6, flash && pressedButton == 1 ? 0xFFFFFFFF : accent);
        if (MediaBridge.playing) {
            context.fill(c - 2, cy - 3, c - 1, cy + 3, 0xFF0B1020);
            context.fill(c + 1, cy - 3, c + 2, cy + 3, 0xFF0B1020);
        } else {
            triangle(context, c - 1, cy, 0xFF0B1020, true);
        }
        next(context, c + 15, cy, flash && pressedButton == 2 ? accent : base);
    }

    private void releaseCover(MinecraftClient client){
        coverLoader.clear();
        if(coverReady)client.getTextureManager().destroyTexture(COVER);
        coverReady=false;loadedCover=-1;coverTrack="";
    }
    private void updateCover(MinecraftClient client) {
        String track=MediaBridge.coverEpoch+"|"+MediaBridge.app+"|"+MediaBridge.artist+"|"+MediaBridge.title;
        if(!track.equals(coverTrack)){releaseCover(client);coverTrack=track;}
        if(!MediaBridge.hasCover){if(coverReady)releaseCover(client);return;}
        int version=MediaBridge.coverVersion;
        if(coverReady&&version==loadedCover)return;
        byte[] bytes=coverLoader.poll(track+"|"+version,MediaBridge.coverFile());
        if(bytes==null)return;
        try{
            NativeImage image=NativeImage.read(bytes);
            if(image.getWidth()>1024||image.getHeight()>1024){image.close();return;}
            coverWidth=image.getWidth();coverHeight=image.getHeight();
            client.getTextureManager().registerTexture(COVER,new NativeImageBackedTexture(image));
            loadedCover=version;coverReady=true;coverLoader.clear();
        }catch(Exception ignored){}
    }

    // маленькие «уголки» поверх квадратной обложки, чтобы она выглядела скруглённой
    private void corners(DrawContext context, int x, int y, int size) {
        if (!background.get()) {
            return;
        }
        int a = Math.round(opacity.getFloat() * 255);
        int c = (a << 24) | (panelColor.get()&0xFFFFFF);
        int[][] px = {{0, 0}, {1, 0}, {0, 1}};
        for (int[] p : px) {
            context.fill(x + p[0], y + p[1], x + p[0] + 1, y + p[1] + 1, c);
            context.fill(x + size - 1 - p[0], y + p[1], x + size - p[0], y + p[1] + 1, c);
            context.fill(x + p[0], y + size - 1 - p[1], x + p[0] + 1, y + size - p[1], c);
            context.fill(x + size - 1 - p[0], y + size - 1 - p[1], x + size - p[0], y + size - p[1], c);
        }
    }

    private void marquee(DrawContext context, String s, int x, int y, int w, int color) {
        int tw = textWidth(s);
        if (tw <= w) {
            text(context, s, x, y, color);
            return;
        }
        int gap = 30;
        int cycle = tw + gap;
        long t = System.currentTimeMillis();
        // пауза в начале, затем плавная прокрутка
        double phase = (t / 1000.0 * 25) % (cycle + 50);
        int offset = (int) Math.max(0, phase - 50);
        context.enableScissor(x, y - 1, x + w, y + 10);
        text(context, s, x - offset, y, color);
        text(context, s, x - offset + cycle, y, color);
        context.disableScissor();
    }

    private static String time(double seconds) {
        int s = (int) Math.max(0, seconds);
        return (s / 60) + ":" + String.format("%02d", s % 60);
    }

    private static void triangle(DrawContext context, int x, int cy, int color, boolean right) {
        for (int i = -3; i <= 3; i++) {
            int len = 4 - Math.abs(i);
            if (right) {
                context.fill(x, cy + i, x + len, cy + i + 1, color);
            } else {
                context.fill(x - len, cy + i, x, cy + i + 1, color);
            }
        }
    }

    private static void prev(DrawContext context, int cx, int cy, int color) {
        context.fill(cx - 4, cy - 3, cx - 3, cy + 4, color);
        triangle(context, cx + 2, cy, color, false);
        triangle(context, cx + 5, cy, color, false);
    }

    private static void next(DrawContext context, int cx, int cy, int color) {
        triangle(context, cx - 5, cy, color, true);
        triangle(context, cx - 2, cy, color, true);
        context.fill(cx + 3, cy - 3, cx + 4, cy + 4, color);
    }

    // нота ♪
    private static void note(DrawContext context, int x, int y, int color) {
        context.fill(x + 3, y - 4, x + 4, y + 3, color);
        context.fill(x + 4, y - 4, x + 6, y - 3, color);
        context.fill(x + 5, y - 3, x + 6, y - 2, color);
        context.fill(x, y + 2, x + 3, y + 4, color);
        context.fill(x + 1, y + 1, x + 3, y + 2, color);
    }
}
