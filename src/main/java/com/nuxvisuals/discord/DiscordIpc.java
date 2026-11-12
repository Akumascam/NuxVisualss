package com.nuxvisuals.discord;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nuxvisuals.NuxVisuals;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Discord Rich Presence без библиотек: локальный IPC Discord
 * (Windows — named pipe \\.\pipe\discord-ipc-N, macOS/Linux — unix-сокет).
 * Весь обмен идёт в отдельном потоке, игру не тормозит.
 */
public final class DiscordIpc {
    public record Activity(String details, String state, long startEpochSec, String largeImage, String largeText,
                           String smallImage, String smallText, String buttonLabel, String buttonUrl) {
    }

    private interface Pipe {
        void write(byte[] data) throws IOException;
        byte[] read() throws IOException;
        void close();
    }

    private static Thread thread;
    private static volatile boolean running;
    private static volatile String clientId = "";
    private static volatile Activity wanted;
    private static volatile boolean connected;
    private static volatile String lastError = "";

    public static boolean connected() {
        return connected;
    }

    public static String lastError() {
        return lastError;
    }

    public static synchronized void start(String appId) {
        if (appId == null || appId.isBlank()) {
            return;
        }
        if (running && appId.equals(clientId)) {
            return;
        }
        stop();
        clientId = appId.trim();
        running = true;
        thread = new Thread(DiscordIpc::loop, "NuxVisuals-Discord");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    public static synchronized void stop() {
        running = false;
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
        connected = false;
    }

    public static void update(Activity activity) {
        wanted = activity;
    }

    private static void loop() {
        while (running) {
            Pipe pipe = null;
            try {
                pipe = open();
                if (pipe == null) {
                    lastError = "Discord не запущен";
                    sleep(15000);
                    continue;
                }
                JsonObject hello = new JsonObject();
                hello.addProperty("v", 1);
                hello.addProperty("client_id", clientId);
                send(pipe, 0, hello);
                JsonObject ready = JsonParser.parseString(new String(pipe.read(), StandardCharsets.UTF_8)).getAsJsonObject();
                if (ready.has("code") && !ready.has("evt")) {
                    lastError = "Discord отклонил Application ID";
                    sleep(60000);
                    continue;
                }
                connected = true;
                lastError = "";
                Activity sent = null;
                long lastSend = 0;
                while (running) {
                    Activity a = wanted;
                    long now = System.currentTimeMillis();
                    // Discord ограничивает частоту: не чаще раза в 15 с, при изменениях — сразу (но не чаще раза в 5 с)
                    if (a != null && (!a.equals(sent) && now - lastSend > 5000 || now - lastSend > 60000)) {
                        send(pipe, 1, activityPayload(a));
                        pipe.read();
                        sent = a;
                        lastSend = now;
                    }
                    sleep(1000);
                }
                send(pipe, 1, activityPayload(null)); // убрать статус при выключении
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                lastError = "нет связи с Discord";
                NuxVisuals.LOGGER.debug("Discord RPC: {}", e.toString());
                connected = false;
                try {
                    sleep(10000);
                } catch (InterruptedException ie) {
                    break;
                }
            } finally {
                connected = false;
                if (pipe != null) {
                    pipe.close();
                }
            }
        }
    }

    private static void sleep(long ms) throws InterruptedException {
        Thread.sleep(ms);
    }

    private static JsonObject activityPayload(Activity a) {
        JsonObject args = new JsonObject();
        args.addProperty("pid", ProcessHandle.current().pid());
        if (a != null) {
            JsonObject act = new JsonObject();
            if (!a.details().isEmpty()) act.addProperty("details", cut(a.details()));
            if (!a.state().isEmpty()) act.addProperty("state", cut(a.state()));
            JsonObject ts = new JsonObject();
            ts.addProperty("start", a.startEpochSec());
            act.add("timestamps", ts);
            JsonObject assets = new JsonObject();
            if (!a.largeImage().isEmpty()) assets.addProperty("large_image", a.largeImage());
            if (!a.largeText().isEmpty()) assets.addProperty("large_text", cut(a.largeText()));
            if (!a.smallImage().isEmpty()) assets.addProperty("small_image", a.smallImage());
            if (!a.smallText().isEmpty()) assets.addProperty("small_text", cut(a.smallText()));
            act.add("assets", assets);
            if (a.buttonUrl() != null && a.buttonUrl().startsWith("http")) {
                JsonArray buttons = new JsonArray();
                JsonObject b = new JsonObject();
                b.addProperty("label", cut(a.buttonLabel()).substring(0, Math.min(31, cut(a.buttonLabel()).length())));
                b.addProperty("url", a.buttonUrl());
                buttons.add(b);
                act.add("buttons", buttons);
            }
            args.add("activity", act);
        }
        JsonObject root = new JsonObject();
        root.addProperty("cmd", "SET_ACTIVITY");
        root.add("args", args);
        root.addProperty("nonce", UUID.randomUUID().toString());
        return root;
    }

    private static String cut(String s) {
        if (s == null) return "";
        return s.length() > 120 ? s.substring(0, 120) : (s.length() < 2 ? s + "  " : s);
    }

    private static void send(Pipe pipe, int op, JsonObject json) throws IOException {
        byte[] body = json.toString().getBytes(StandardCharsets.UTF_8);
        ByteBuffer buf = ByteBuffer.allocate(8 + body.length).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(op).putInt(body.length).put(body);
        pipe.write(buf.array());
    }

    // ---------- подключение ----------
    private static Pipe open() {
        boolean win = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
        for (int i = 0; i < 10; i++) {
            try {
                if (win) {
                    RandomAccessFile f = new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw");
                    return new Pipe() {
                        public void write(byte[] d) throws IOException { f.write(d); }
                        public byte[] read() throws IOException {
                            byte[] h = new byte[8]; f.readFully(h);
                            int len = ByteBuffer.wrap(h).order(ByteOrder.LITTLE_ENDIAN).getInt(4);
                            byte[] b = new byte[len]; f.readFully(b); return b;
                        }
                        public void close() { try { f.close(); } catch (IOException ignored) { } }
                    };
                }
                for (Path dir : unixDirs()) {
                    Path sock = dir.resolve("discord-ipc-" + i);
                    if (!Files.exists(sock)) continue;
                    SocketChannel ch = SocketChannel.open(StandardProtocolFamily.UNIX);
                    ch.connect(UnixDomainSocketAddress.of(sock));
                    return new Pipe() {
                        public void write(byte[] d) throws IOException { ByteBuffer b = ByteBuffer.wrap(d); while (b.hasRemaining()) ch.write(b); }
                        public byte[] read() throws IOException {
                            ByteBuffer h = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN); fill(h);
                            ByteBuffer b = ByteBuffer.allocate(h.getInt(4)); fill(b); return b.array();
                        }
                        private void fill(ByteBuffer b) throws IOException { while (b.hasRemaining()) if (ch.read(b) < 0) throw new IOException("closed"); b.flip(); }
                        public void close() { try { ch.close(); } catch (IOException ignored) { } }
                    };
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static List<Path> unixDirs() {
        List<Path> base = new ArrayList<>();
        for (String env : new String[]{"XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP"}) {
            String v = System.getenv(env);
            if (v != null && !v.isBlank()) base.add(Path.of(v));
        }
        base.add(Path.of("/tmp"));
        List<Path> out = new ArrayList<>();
        for (Path b : base) {
            out.add(b);
            out.add(b.resolve("app/com.discordapp.Discord")); // Flatpak
            out.add(b.resolve("snap.discord"));               // Snap
            out.add(b.resolve(".flatpak/dev.vencord.Vesktop/xdg-run")); // Vesktop
        }
        return out;
    }

    private DiscordIpc() {
    }
}
