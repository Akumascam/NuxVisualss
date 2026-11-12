package com.nuxvisuals.friend;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.nuxvisuals.NuxVisuals;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Список друзей: ники хранятся в config/nuxvisuals/friends.json, сравнение без учёта регистра
public final class FriendManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    // ключ — ник в нижнем регистре, значение — ник как его ввели
    private static final Map<String, String> FRIENDS = new LinkedHashMap<>();
    private static boolean loaded;

    private static void ensureLoaded() {
        if (!loaded) {
            loaded = true;
            load();
        }
    }

    private static String key(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    // ник Minecraft: 3-16 символов, латиница, цифры, _
    public static boolean isValidName(String name) {
        return name != null && name.trim().matches("[A-Za-z0-9_]{3,16}");
    }

    public static boolean isFriend(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        ensureLoaded();
        return FRIENDS.containsKey(key(name));
    }

    public static boolean add(String name) {
        ensureLoaded();
        if (!isValidName(name) || FRIENDS.containsKey(key(name))) {
            return false;
        }
        FRIENDS.put(key(name), name.trim());
        save();
        return true;
    }

    public static boolean remove(String name) {
        ensureLoaded();
        boolean removed = FRIENDS.remove(key(name)) != null;
        if (removed) {
            save();
        }
        return removed;
    }

    public static void toggle(String name) {
        if (isFriend(name)) {
            remove(name);
        } else {
            add(name);
        }
    }

    public static List<String> list() {
        ensureLoaded();
        return new ArrayList<>(FRIENDS.values());
    }

    public static int size() {
        ensureLoaded();
        return FRIENDS.size();
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("nuxvisuals").resolve("friends.json");
    }

    private static void load() {
        try {
            if (Files.exists(file())) {
                JsonArray arr = JsonParser.parseString(Files.readString(file())).getAsJsonArray();
                for (JsonElement e : arr) {
                    String name = e.getAsString();
                    if (isValidName(name)) {
                        FRIENDS.put(key(name), name.trim());
                    }
                }
            }
        } catch (Exception e) {
            NuxVisuals.LOGGER.warn("Не удалось прочитать friends.json", e);
        }
    }

    private static void save() {
        JsonArray arr = new JsonArray();
        for (String name : FRIENDS.values()) {
            arr.add(name);
        }
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), GSON.toJson(arr));
        } catch (Exception e) {
            NuxVisuals.LOGGER.warn("Не удалось сохранить friends.json", e);
        }
    }

    private FriendManager() {
    }
}
