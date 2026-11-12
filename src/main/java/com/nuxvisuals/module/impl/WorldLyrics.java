package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.ModuleManager;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.ColorSetting;
import com.nuxvisuals.module.setting.ModeSetting;
import com.nuxvisuals.module.setting.SliderSetting;
import com.nuxvisuals.music.LrcLyrics;
import com.nuxvisuals.music.LyricsService;
import com.nuxvisuals.music.MediaBridge;
import com.nuxvisuals.render.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** Timed lyric lines floating in the world, like the supplied reference screenshot. */
public class WorldLyrics extends Module {
    private final BoolSetting online = add(new BoolSetting("Online Lyrics", true));
    private final BoolSetting trackFallback = add(new BoolSetting("Track Name Fallback", true));
    private final SliderSetting radius = add(new SliderSetting("Radius", 3, 1.5, 8, 0.25, " m"));
    private final SliderSetting height = add(new SliderSetting("Height", 1.8, 0.5, 5, 0.1, " m"));
    private final SliderSetting scale = add(new SliderSetting("Scale", 1, 0.5, 2, 0.05, "x"));
    private final SliderSetting opacity = add(new SliderSetting("Opacity", 0.95, 0.2, 1, 0.05));
    private final SliderSetting offset = add(new SliderSetting("Sync Offset", 0, -10, 10, 0.1, " s"));
    private final SliderSetting copies = add(new SliderSetting("Copies", 4, 1, 6, 1));
    private final ModeSetting layout = add(new ModeSetting("Расположение", "По сторонам", "По сторонам", "Волна", "Орбита"));
    private float sceneYaw;
    private final ModeSetting presentation=add(new ModeSetting("Подача текста","Строки","Строки","Слова"));
    private final BoolSetting speak=add(new BoolSetting("Озвучивать строки",false));
    private String spoken="";
    private final BoolSetting neighbours = add(new BoolSetting("Previous/Next Lines", true));
    private final BoolSetting shadow = add(new BoolSetting("Text Shadow", true));
    private final ColorSetting color = add(new ColorSetting("Color", 0.6f, 0.05f));
    private final BoolSetting rainbow = add(new BoolSetting("Rainbow", false));
    private final ModeSetting animation = add(new ModeSetting("Animation", "Words", "Words", "Letters", "Instant"));
    private final SliderSetting revealSpeed = add(new SliderSetting("Reveal Speed", 1, 0.3, 3, 0.1, "x")
            .visibleIf(() -> !this.animation.is("Instant")));
    private LyricsService.Track track;
    private LrcLyrics lyrics = LrcLyrics.EMPTY;
    private int line = -1;
    private String previous = "", current = "", next = "";
    private double lineStart;
    private double lineDuration = 4;
    // строка, которая уезжает вверх при смене (плавный переход вместо резкой замены)
    private String outgoing = "";
    private long changedAt;
    private long fallbackStart;
    private boolean active;

    public WorldLyrics() {
        super("world-lyrics", "WorldLyrics", Category.Visuals, false);
    }

    @Override public void onTick(MinecraftClient client) {
        MediaBridge.start();
        active = client.world != null && client.player != null && MediaBridge.WINDOWS
                && MediaBridge.playing && MediaBridge.hasTrack()
                && System.currentTimeMillis() - MediaBridge.receivedAt < 5000;
        if (!active){if(!spoken.isEmpty())com.nuxvisuals.music.LyricsSpeech.stop();spoken="";return;}
        if(!speak.get()&&!spoken.isEmpty()){com.nuxvisuals.music.LyricsSpeech.stop();spoken="";}
        LyricsService.Track playing = new LyricsService.Track(MediaBridge.artist, MediaBridge.title,
                (int) Math.round(MediaBridge.duration));
        if (!playing.equals(track)) {
            track = playing;
            lyrics = LrcLyrics.EMPTY;
            line = -1;
            previous = current = next = outgoing = "";
            com.nuxvisuals.music.LyricsSpeech.stop();spoken="";
        }
        LyricsService.request(track, online.get());
        LyricsService.Result result = LyricsService.current();
        if (track.equals(result.track())) lyrics = result.lyrics();
        double pos = MediaBridge.livePosition() + offset.get();
        int index = lyrics.indexAt(pos);
        if (index != line || (index >= 0 && !lyrics.lines().get(index).text().equals(current))) {
            if (!current.isBlank()) {
                outgoing = current;
            }
            changedAt = System.nanoTime();
            sceneYaw = client.gameRenderer.getCamera().getYaw();
            line = index;
            previous = index > 0 ? lyrics.lines().get(index - 1).text() : "";
            current = index >= 0 ? lyrics.lines().get(index).text() : "";
            next = index >= 0 && index + 1 < lyrics.lines().size() ? lyrics.lines().get(index + 1).text() : "";
            lineStart = index >= 0 ? lyrics.lines().get(index).seconds() : 0;
            lineDuration = index >= 0 && index + 1 < lyrics.lines().size()
                    ? Math.max(0.4, lyrics.lines().get(index + 1).seconds() - lineStart) : 4;
        }
        if (lyrics.lines().isEmpty()) {
            previous = next = "";
            String fallback = trackFallback.get() ? LrcLyrics.clean(track.artist().isBlank()?track.title():track.artist()+" — "+track.title()) : "";
            if (!fallback.equals(current)) {
                fallbackStart = System.nanoTime();
                changedAt = fallbackStart;
                sceneYaw = client.gameRenderer.getCamera().getYaw();
                outgoing = "";
            }
            current = fallback;
            lineDuration = 4;
        }
        if(speak.get()&&line>=0&&!current.isBlank()){
            String key=track.artist()+"|"+track.title()+"|"+line;
            if(!key.equals(spoken)){com.nuxvisuals.music.LyricsSpeech.speak(current);spoken=key;}
        }else if(!spoken.isEmpty()){com.nuxvisuals.music.LyricsSpeech.stop();spoken="";}
    }

    @Override public void onRender(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
        MinecraftClient client = mc();
        if (!active || client.player == null || client.world == null || !MediaBridge.playing
                || System.currentTimeMillis() - MediaBridge.receivedAt >= 5000 || current.isBlank()
                || client.options.hudHidden) return;
        Vec3d player = client.player.getLerpedPos(tickDelta);
        Vec3d cam = camera.getPos();
        if (player.squaredDistanceTo(cam) > 48 * 48) return;
        boolean synced = !lyrics.lines().isEmpty();
        double elapsed = Math.max(0, synced ? MediaBridge.livePosition() + offset.get() - lineStart
                : (System.nanoTime() - fallbackStart) / 1e9);
        if(presentation.is("Строки")){
            double transition=Math.max(0,(System.nanoTime()-changedAt)/1e9);
            float enter=ease((float)Math.min(1,transition/.35));
            int anchor=Math.max(0,line)%Math.max(1,copies.getInt());
            double angle=Math.toRadians(sceneYaw+LyricPlacement.angle(anchor,copies.getInt())*.65);
            if(layout.is("Орбита"))angle+=Math.sin((System.nanoTime()%60_000_000_000L)/1e9*.3)*.08;
            double x=player.x-Math.sin(angle)*radius.get()-cam.x;
            double z=player.z+Math.cos(angle)*radius.get()-cam.z;
            double y=player.y+height.get()-cam.y+(1-enter)*-.12;
            if(layout.is("Волна"))y+=Math.sin((System.nanoTime()%60_000_000_000L)/1e9*.9)*.08;
            var lines=com.nuxvisuals.music.LyricPresentation.wrap(current,36,3);
            for(int i=0;i<lines.size();i++){
                String text=lines.get(i);
                drawWord(matrices,consumers,camera,x,y-i*.25,z,text,color(color,rainbow,i*.03f),opacity.getFloat()*enter,enter);
            }
            if(neighbours.get()&&!outgoing.isBlank()&&transition<.7)
                drawWord(matrices,consumers,camera,x,y+.4+transition*.12,z,outgoing,color(color,rainbow,0),opacity.getFloat()*.22f*(1-(float)(transition/.7)),.8f);
            if(lyrics.estimated())drawWord(matrices,consumers,camera,x,y-lines.size()*.25-.1,z,"~ примерная синхронизация",0xB7C1D8,opacity.getFloat()*.4f,.7f);
            return;
        }
        String[] words = current.strip().split("\\s+");
        int count = Math.min(64, words.length);
        double window = Math.max(0.1, Math.min(lineDuration * 0.65, count * 0.42)) / revealSpeed.get();
        double step = count > 1 ? window / (count - 1) : 0.1;
        int latest = animation.is("Instant") ? count - 1 : Math.min(count - 1, (int) (elapsed / step));
        int amount = copies.getInt();
        int first = Math.max(0, latest - amount + 1);
        double time = (System.nanoTime() % 3_600_000_000_000L) / 1e9;
        for (int i = first; i <= latest; i++) {
            double age = animation.is("Instant") ? elapsed : Math.max(0, elapsed - i * step);
            float enter = ease((float) Math.min(1, age / 0.28));
            float fade = i < count - amount ? 1f - (float) Math.max(0,
                    Math.min(1, (age - step * (amount - 1)) / Math.max(0.2, step))) : 1f;
            float a = opacity.getFloat() * enter * fade;
            if (a < 0.02f) continue;
            int slot = i % Math.max(1, amount);
            double angleOffset = LyricPlacement.angle(slot, amount);
            if (layout.is("Орбита")) angleOffset = (slot - (amount - 1) * 0.5) * 30 + Math.sin(time * 0.3) * 8;
            double angle = Math.toRadians(sceneYaw + angleOffset);
            double yOffset = LyricPlacement.height(slot);
            if (layout.is("Волна")) yOffset += Math.sin(time * 1.2 + i * 0.7) * 0.2;
            double x = player.x - Math.sin(angle) * radius.get();
            double z = player.z + Math.cos(angle) * radius.get();
            double y = player.y + height.get() + yOffset + Math.sin(time * 1.1 + i) * 0.035
                    - (1 - enter) * 0.16;
            String word = words[i];
            if (animation.is("Letters")) {
                int length = word.codePointCount(0, word.length());
                int visible = Math.min(length, (int) (age * revealSpeed.get() * 22) + 1);
                word = word.substring(0, word.offsetByCodePoints(0, visible));
            }
            drawWord(matrices, consumers, camera, x - cam.x, y - cam.y, z - cam.z,
                    word, color(color, rainbow, i * 0.07f), a, enter);
        }
        // Subtle context above/below, not several repeated full lyric lines at eye level.
        if (neighbours.get()) {
            double angle = Math.toRadians(sceneYaw);
            double x = player.x - Math.sin(angle) * radius.get() - cam.x;
            double z = player.z + Math.cos(angle) * radius.get() - cam.z;
            drawWord(matrices, consumers, camera, x, player.y + height.get() + 0.9 - cam.y, z,
                    previous, color(color, rainbow, 0), opacity.getFloat() * 0.16f, 0.75f);
            drawWord(matrices, consumers, camera, x, player.y + height.get() - 0.85 - cam.y, z,
                    next, color(color, rainbow, 0.1f), opacity.getFloat() * 0.2f, 0.75f);
        }
    }

    private void drawWord(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera,
            double x, double y, double z, String word, int rgb, float alpha, float appear) {
        if (word.isBlank()) return;
        TextRenderer renderer = mc().textRenderer;
        matrices.push();
        try {
            matrices.translate(x, y, z);
            matrices.multiply(camera.getRotation());
            float fit = Math.min(1f, 95f / Math.max(1, renderer.getWidth(word)));
            float size = 0.025f * scale.getFloat() * fit * (0.85f + 0.15f * appear);
            matrices.scale(size, -size, size);
            renderer.draw(word, -renderer.getWidth(word) * 0.5f, 0, RenderUtil.withAlpha(rgb, alpha),
                    shadow.get(), matrices.peek().getPositionMatrix(), consumers,
                    TextRenderer.TextLayerType.NORMAL, presentation.is("Строки")?RenderUtil.withAlpha(0x0B101C,alpha*.45f):0, RenderUtil.FULL_LIGHT);
        } finally { matrices.pop(); }
    }

    private static float ease(float t) {
        float k = 1 - Math.max(0, Math.min(1, t));
        return 1 - k * k * k;
    }

    /** Слова (или буквы) появляются по очереди: каждое всплывает снизу и проявляется. */
    private void drawReveal(TextRenderer renderer, VertexConsumerProvider consumers, Matrix4f matrix,
                            String text, int rgb, float alpha, double elapsed) {
        if (text.isBlank()) return;
        if (animation.is("Instant")) {
            drawLine(renderer, consumers, matrix, text, 0, rgb, alpha * (float) Math.min(1, elapsed / 0.25));
            return;
        }
        boolean letters = animation.is("Letters");
        String[] parts = letters ? text.split("") : text.split(" ");
        int visibleParts = 0;
        for (String p : parts) if (!p.isBlank()) visibleParts++;
        if (visibleParts == 0) return;
        // вся строка проявляется за ~55% своей длительности, но не медленнее 0.45с на слово
        double perPart = letters ? 0.045 : 0.45;
        double window = Math.min(lineDuration * 0.55, visibleParts * perPart) / revealSpeed.get();
        double step = visibleParts > 1 ? window / (visibleParts - 1) : 0;
        double fadeTime = (letters ? 0.18 : 0.32) / revealSpeed.get();
        float space = letters ? 0 : renderer.getWidth(" ");
        float total = renderer.getWidth(text);
        float cx = -total / 2f;
        int order = 0;
        for (String part : parts) {
            float w = renderer.getWidth(part);
            if (!part.isBlank()) {
                float k = (float) Math.max(0, Math.min(1, (elapsed - order * step) / fadeTime));
                order++;
                if (k > 0.01f) {
                    float e = ease(k);
                    renderer.draw(part, cx, 5 * (1 - e), RenderUtil.withAlpha(rgb, alpha * e),
                            shadow.get(), matrix, consumers, TextRenderer.TextLayerType.NORMAL, 0, RenderUtil.FULL_LIGHT);
                }
            }
            cx += w + space;
        }
    }

    private void drawLine(TextRenderer renderer, VertexConsumerProvider consumers, Matrix4f matrix,
                          String text, float y, int rgb, float alpha) {
        if (text.isBlank() || alpha < 0.02f) return;
        // NORMAL keeps lyrics occluded by blocks: no through-wall text.
        renderer.draw(text, -renderer.getWidth(text) / 2f, y, RenderUtil.withAlpha(rgb, alpha),
                shadow.get(), matrix, consumers, TextRenderer.TextLayerType.NORMAL, 0, RenderUtil.FULL_LIGHT);
    }

    @Override public void onEnable() { clear(); LyricsService.reset(); }
    @Override public void onDisable() {
        clear();
        LyricsService.reset();
        Module music = ModuleManager.get("music");
        if (music == null || !music.isEnabled()) MediaBridge.stop();
    }
    private void clear() {
        if(!spoken.isEmpty())com.nuxvisuals.music.LyricsSpeech.stop();spoken="";
        active = false; track = null; lyrics = LrcLyrics.EMPTY; line = -1;
        previous = current = next = outgoing = "";
    }
}
