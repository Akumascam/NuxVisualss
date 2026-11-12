package com.nuxvisuals.perf;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nuxvisuals.NuxVisuals;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Автоустановка модов оптимизации (Sodium, Lithium, FerriteCore) с Modrinth.
 * Если их нет в папке mods — тихо скачивает в фоне и просит перезапустить игру.
 * Отключить: создать файл config/nuxvisuals/no-perf-mods
 */
public final class PerfMods {
    public static final String[][] MODS = {
            {"sodium", "sodium", "Sodium"},
            {"lithium", "lithium", "Lithium"},
            {"ferritecore", "ferrite-core", "FerriteCore"},
    };

    public static void checkAsync() {
        FabricLoader fl = FabricLoader.getInstance();
        if (Files.exists(fl.getConfigDir().resolve("nuxvisuals").resolve("no-perf-mods"))) {
            return;
        }
        List<String[]> missing = new ArrayList<>();
        for (String[] m : MODS) {
            if (!fl.isModLoaded(m[0])) {
                missing.add(m);
            }
        }
        if (missing.isEmpty()) {
            return;
        }
        Thread t = new Thread(() -> install(missing), "NuxVisuals-PerfMods");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    private static void install(List<String[]> missing) {
        Path mods = FabricLoader.getInstance().getGameDir().resolve("mods");
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NORMAL).build();
        List<String> done = new ArrayList<>();
        for (String[] m : missing) {
            try {
                String q = "?loaders=" + URLEncoder.encode("[\"fabric\"]", StandardCharsets.UTF_8)
                        + "&game_versions=" + URLEncoder.encode("[\"1.21.4\"]", StandardCharsets.UTF_8);
                HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create("https://api.modrinth.com/v2/project/" + m[1] + "/version" + q))
                        .header("User-Agent", "NuxVisuals/1.2.0 (t.me/NuxVisuals)").timeout(Duration.ofSeconds(15)).build(),
                        HttpResponse.BodyHandlers.ofString());
                if (r.statusCode() != 200) {
                    continue;
                }
                JsonObject file = pick(JsonParser.parseString(r.body()).getAsJsonArray());
                if (file == null) {
                    continue;
                }
                String name = file.get("filename").getAsString();
                Path dst = mods.resolve(name);
                if (Files.exists(dst)) {
                    continue;
                }
                Path tmp = mods.resolve(name + ".part");
                HttpResponse<Path> f = http.send(HttpRequest.newBuilder(URI.create(file.get("url").getAsString()))
                        .header("User-Agent", "NuxVisuals/1.2.0").timeout(Duration.ofMinutes(3)).build(), HttpResponse.BodyHandlers.ofFile(tmp));
                String sha1 = file.getAsJsonObject("hashes").get("sha1").getAsString();
                if (f.statusCode() == 200 && sha1.equalsIgnoreCase(sha1(tmp))) {
                    Files.move(tmp, dst, StandardCopyOption.REPLACE_EXISTING);
                    done.add(m[2]);
                } else {
                    Files.deleteIfExists(tmp);
                }
            } catch (Exception e) {
                NuxVisuals.LOGGER.debug("PerfMods: {} не скачался: {}", m[2], e.toString());
            }
        }
        if (!done.isEmpty()) {
            String list = String.join(", ", done);
            NuxVisuals.LOGGER.info("NuxVisuals установил моды оптимизации: {}", list);
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.execute(() -> SystemToast.add(mc.getToastManager(), SystemToast.Type.PERIODIC_NOTIFICATION,
                    Text.literal("NuxVisuals: +FPS"), Text.literal("Установлено: " + list + ". Перезапусти игру")));
        }
    }

    /** Самая свежая release-версия (или любая, если релизов нет), основной файл. */
    private static JsonObject pick(JsonArray versions) {
        JsonObject any = null;
        for (JsonElement e : versions) {
            JsonObject v = e.getAsJsonObject();
            JsonObject primary = null;
            for (JsonElement fe : v.getAsJsonArray("files")) {
                JsonObject fo = fe.getAsJsonObject();
                if (primary == null || (fo.has("primary") && fo.get("primary").getAsBoolean())) {
                    primary = fo;
                }
            }
            if (primary == null) {
                continue;
            }
            if ("release".equals(v.get("version_type").getAsString())) {
                return primary;
            }
            if (any == null) {
                any = primary;
            }
        }
        return any;
    }

    private static String sha1(Path p) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(p)));
    }

    private PerfMods() {
    }
}
