package com.nuxvisuals.badge;

import com.nuxvisuals.NuxVisuals;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Значок «N» у ника пользователей NuxVisuals (над головой и в табе).
// Кто считается нашим:
//  1) ты сам — всегда;
//  2) ники/UUID из config/nuxvisuals/users.txt (по одному в строке);
//  3) общий список по ссылке LIST_URL (например, raw-файл на GitHub) — обновляется раз в 5 минут.
public final class NuxUsers {
    // вставь сюда ссылку на текстовый файл со списком ников (по одному в строке), например:
    // https://raw.githubusercontent.com/<ник>/<репо>/main/users.txt
    public static final String LIST_URL = "";

    private static final Identifier FONT = Identifier.of("nuxvisuals", "badge");
    private static final Set<String> USERS = ConcurrentHashMap.newKeySet();
    private static long lastFetch;
    private static boolean fetching;

    public static boolean isNux(UUID uuid, String name) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && mc.player.getUuid().equals(uuid)) {
            return true;
        }
        if (name != null && mc.getSession() != null && name.equalsIgnoreCase(mc.getSession().getUsername())) {
            return true;
        }
        refresh();
        return (name != null && USERS.contains(name.toLowerCase(Locale.ROOT)))
                || (uuid != null && USERS.contains(uuid.toString().toLowerCase(Locale.ROOT)));
    }

    // «N » перед ником (значок из своего шрифта + пробел)
    public static Text decorate(Text original) {
        if (original == null) {
            return null;
        }
        MutableText badge = Text.literal("\uE000").setStyle(Style.EMPTY.withFont(FONT));
        return Text.empty().append(badge).append(Text.literal(" ")).append(original);
    }

    private static synchronized void refresh() {
        if (fetching || System.currentTimeMillis() - lastFetch < 300_000) {
            return;
        }
        lastFetch = System.currentTimeMillis();
        fetching = true;
        Thread t = new Thread(() -> {
            try {
                Set<String> next = ConcurrentHashMap.newKeySet();
                Path local = FabricLoader.getInstance().getConfigDir().resolve("nuxvisuals").resolve("users.txt");
                if (Files.exists(local)) {
                    parse(Files.readString(local), next);
                }
                if (!LIST_URL.isEmpty()) {
                    HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
                    HttpResponse<String> r = client.send(HttpRequest.newBuilder(URI.create(LIST_URL))
                            .timeout(Duration.ofSeconds(8)).GET().build(), HttpResponse.BodyHandlers.ofString());
                    if (r.statusCode() == 200) {
                        parse(r.body(), next);
                    }
                }
                USERS.clear();
                USERS.addAll(next);
            } catch (Exception e) {
                NuxVisuals.LOGGER.debug("NuxUsers: список не загрузился", e);
            } finally {
                fetching = false;
            }
        }, "NuxVisuals-Users");
        t.setDaemon(true);
        t.start();
    }

    private static void parse(String text, Set<String> out) {
        for (String line : text.split("\\R")) {
            String s = line.trim();
            if (!s.isEmpty() && !s.startsWith("#")) {
                out.add(s.toLowerCase(Locale.ROOT));
            }
        }
    }

    private NuxUsers() {
    }
}
