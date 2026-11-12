package com.nuxvisuals.music;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nuxvisuals.NuxVisuals;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

// Мост к «Сейчас играет» Windows (System Media Transport Controls).
// Запускает скрытый PowerShell-скрипт, который раз в ~0.4 c присылает JSON с треком,
// а по командам из stdin ставит паузу и листает треки. Работает со Spotify, браузерами,
// Яндекс Музыкой, VK, Apple Music — всем, что показывает плашку медиа в Windows.
public final class MediaBridge {
    public static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");

    // текущее состояние (читается из потока рендера)
    public static volatile String title = "";
    public static volatile String artist = "";
    public static volatile String app = "";
    public static volatile boolean playing;
    public static volatile double position;
    public static volatile double duration;
    public static volatile long receivedAt;
    public static volatile int coverVersion;
    public static volatile long coverEpoch;
    public static volatile boolean hasCover;
    public static volatile String error = "";

    private static Process process;
    private static Path coverFile;
    private static Path commandFile;
    private static long lastStart;

    public static Path coverFile() {
        return coverFile;
    }

    public static boolean running() {
        return process != null && process.isAlive();
    }

    public static synchronized void start() {
        if (!WINDOWS || running() || System.currentTimeMillis() - lastStart < 5000) {
            return;
        }
        lastStart = System.currentTimeMillis();
        try {
            Path dir = Files.createDirectories(Path.of(System.getProperty("java.io.tmpdir"), "nuxvisuals-media-"+ProcessHandle.current().pid()));
            Path script = dir.resolve("media.ps1");
            try (InputStream in = MediaBridge.class.getResourceAsStream("/assets/nuxvisuals/scripts/media.ps1")) {
                if (in == null) {
                    error = "нет скрипта";
                    return;
                }
                Files.copy(in, script, StandardCopyOption.REPLACE_EXISTING);
            }
            hasCover=false;coverVersion=0;coverEpoch++;
            coverFile = dir.resolve("cover.png");
            commandFile = dir.resolve("command.txt");
            Files.deleteIfExists(commandFile);
            ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive",
                    "-ExecutionPolicy", "Bypass", "-WindowStyle", "Hidden",
                    "-File", script.toString(), coverFile.toString(), commandFile.toString(),
                    String.valueOf(ProcessHandle.current().pid()));
            // ошибки PowerShell пишем в лог — пригодится, если трек не читается
            pb.redirectError(dir.resolve("media.log").toFile());
            pb.redirectInput(ProcessBuilder.Redirect.PIPE);
            process = pb.start();
            error = "";
            Process p = process;
            Thread reader = new Thread(() -> read(p), "NuxVisuals-Media");
            reader.setDaemon(true);
            reader.start();
        } catch (Exception e) {
            error = "PowerShell не запустился";
            NuxVisuals.LOGGER.warn("Music HUD: не удалось запустить PowerShell", e);
        }
    }

    private static void read(Process p) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }
                if (!line.startsWith("{")) {
                    continue;
                }
                try {
                    JsonObject o = JsonParser.parseString(line).getAsJsonObject();
                    if (o.has("error")) {
                        error = "Медиа Windows недоступны";
                        continue;
                    }
                    error="";
                    title = str(o, "title");
                    artist = str(o, "artist");
                    app = str(o, "app");
                    playing = o.has("status") && o.get("status").getAsInt() == 4;
                    position = o.has("pos") ? o.get("pos").getAsDouble() : 0;
                    duration = o.has("dur") ? o.get("dur").getAsDouble() : 0;
                    coverVersion = o.has("cover") ? o.get("cover").getAsInt() : 0;
                    hasCover = o.has("hasCover") && o.get("hasCover").getAsBoolean();
                    receivedAt = System.currentTimeMillis();
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        } finally {
            synchronized (MediaBridge.class) {
                if (process == p) {
                    playing = false;
                    receivedAt = 0;
                }
            }
        }
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }

    // текущая позиция с учётом времени, прошедшего после последнего ответа
    public static double livePosition() {
        double pos = Double.isFinite(position)?position:0;
        if (playing) {
            pos += (System.currentTimeMillis() - receivedAt) / 1000.0;
        }
        return duration > 0 ? Math.max(0, Math.min(duration, pos)) : Math.max(0, pos);
    }

    public static boolean hasTrack() {
        return !title.isEmpty()&&receivedAt>0&&System.currentTimeMillis()-receivedAt<5000;
    }

    // красивое имя приложения: Spotify.exe -> Spotify, chrome -> Chrome
    public static String appName() {
        String a = app.toLowerCase(Locale.ROOT);
        if (a.contains("spotify")) return "Spotify";
        if (a.contains("chrome")) return "Chrome";
        if (a.contains("msedge") || a.contains("edge")) return "Edge";
        if (a.contains("firefox")) return "Firefox";
        if (a.contains("opera")) return "Opera";
        if (a.contains("yandex")) return "Яндекс";
        if (a.contains("browser")) return "Браузер";
        if (a.contains("vk")) return "VK";
        if (a.contains("apple") || a.contains("itunes")) return "Apple Music";
        if (a.contains("zunemusic") || a.contains("media")) return "Медиаплеер";
        if (app.isEmpty()) return "";
        String s = app;
        int bang = s.indexOf('!');
        if (bang > 0) s = s.substring(0, bang);
        if (s.endsWith(".exe")) s = s.substring(0, s.length() - 4);
        int dot = s.lastIndexOf('.');
        if (dot >= 0 && dot < s.length() - 1) s = s.substring(dot + 1);
        return s.isEmpty() ? "" : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static synchronized void send(String command) {
        if (!running() || commandFile == null) {
            return;
        }
        try {
            // команды идут через файл: stdin в Windows PowerShell читается только блокирующе
            Files.writeString(commandFile, command + "\n", StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            // мгновенный отклик в HUD, не дожидаясь ответа
            if (command.equals("toggle")) {
                position = livePosition();
                receivedAt = System.currentTimeMillis();
                playing = !playing;
            }
        } catch (Exception ignored) {
        }
    }

    public static synchronized void stop() {
        if (process != null) {
            try {
                if (commandFile != null) {
                    Files.writeString(commandFile, "quit\n", StandardCharsets.UTF_8,
                            java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
                }
            } catch (Exception ignored) {
            }
            process.destroy();
        }
        process = null;
        playing = false;
        title = artist = app = "";
        position = duration = 0;
        receivedAt = 0;hasCover=false;coverVersion=0;
    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(MediaBridge::stop));
    }

    private MediaBridge() {
    }
}
