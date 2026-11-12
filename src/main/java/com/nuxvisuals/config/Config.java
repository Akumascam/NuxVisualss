package com.nuxvisuals.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nuxvisuals.NuxVisuals;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.ModuleManager;
import com.nuxvisuals.module.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

// Текущие настройки: .minecraft/config/nuxvisuals.json (сохраняются автоматически)
// Именные конфиги:  .minecraft/config/nuxvisuals/configs/<имя>.json
public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static String active = "";
    private static List<Entry> cachedEntries = List.of();
    private static long nextListRefresh;


    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("nuxvisuals.json");
    }

    public static Path configsDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("nuxvisuals").resolve("configs");
    }

    public static String active() {
        return active;
    }

    // ---------- основной файл ----------

    public static void load() {
        Path file = file();
        if (!Files.exists(file)) {
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            apply(root, false);
            if (root.has("_activeConfig")) {
                active = root.get("_activeConfig").getAsString();
            }
        } catch (Exception e) {
            NuxVisuals.LOGGER.warn("Не удалось прочитать конфиг NuxVisuals", e);
        }
    }

    public static void save() {
        JsonObject root = snapshot();
        root.addProperty("_activeConfig", active);
        try {
            Files.writeString(file(), GSON.toJson(root));
        } catch (Exception e) {
            NuxVisuals.LOGGER.warn("Не удалось сохранить конфиг NuxVisuals", e);
        }
    }

    // ---------- именные конфиги ----------

    public record Entry(String name, long modified) {
    }

    public static List<Entry> list() {
        long now = System.nanoTime();
        if (now < nextListRefresh) return cachedEntries;
        nextListRefresh = now + 1_000_000_000L;
        List<Entry> result = new ArrayList<>();
        Path dir = configsDir();
        if (!Files.isDirectory(dir)) {
            cachedEntries = List.of();
            return cachedEntries;
        }
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(p -> p.getFileName().toString().endsWith(".json")).forEach(p -> {
                String n = p.getFileName().toString();
                long time = 0;
                try {
                    time = Files.getLastModifiedTime(p).toMillis();
                } catch (Exception ignored) {
                }
                result.add(new Entry(n.substring(0, n.length() - 5), time));
            });
        } catch (Exception e) {
            NuxVisuals.LOGGER.warn("Не удалось прочитать папку конфигов", e);
        }
        result.sort((a, b) -> Long.compare(b.modified(), a.modified()));
        cachedEntries = List.copyOf(result);
        return cachedEntries;
    }

    // имя файла: только буквы, цифры, пробел, - и _
    public static String clean(String name) {
        StringBuilder sb = new StringBuilder();
        for (char c : name.trim().toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == ' ' || c == '-' || c == '_') {
                sb.append(c);
            }
        }
        return sb.toString().trim();
    }

    public static boolean saveAs(String name) {
        name = clean(name);
        if (name.isEmpty()) {
            return false;
        }
        try {
            Files.createDirectories(configsDir());
            Files.writeString(configsDir().resolve(name + ".json"), GSON.toJson(snapshot()));
            active = name;
            nextListRefresh = 0;
            save();
            return true;
        } catch (Exception e) {
            NuxVisuals.LOGGER.warn("Не удалось сохранить конфиг " + name, e);
            return false;
        }
    }

    public static boolean loadConfig(String name) {
        Path path = configsDir().resolve(clean(name) + ".json");
        if (!Files.exists(path)) {
            return false;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            apply(root, true);
            active = clean(name);
            save();
            return true;
        } catch (Exception e) {
            NuxVisuals.LOGGER.warn("Не удалось загрузить конфиг " + name, e);
            return false;
        }
    }

    public static boolean delete(String name) {
        try {
            boolean ok = Files.deleteIfExists(configsDir().resolve(clean(name) + ".json"));
            nextListRefresh = 0;
            if (ok && clean(name).equals(active)) {
                active = "";
                save();
            }
            return ok;
        } catch (Exception e) {
            return false;
        }
    }

    public static void openFolder() {
        try {
            Files.createDirectories(configsDir());
            net.minecraft.util.Util.getOperatingSystem().open(configsDir().toFile());
        } catch (Exception e) {
            NuxVisuals.LOGGER.warn("Не удалось открыть папку конфигов", e);
        }
    }

    // ---------- общее ----------

    private static JsonObject snapshot() {
        JsonObject root = new JsonObject();
        for (Module module : ModuleManager.MODULES) {
            JsonObject data = new JsonObject();
            data.addProperty("enabled", module.rawEnabled());
            data.addProperty("key", module.getKey());
            JsonObject settings = new JsonObject();
            for (Setting setting : module.getSettings()) {
                settings.add(setting.getName(), setting.save());
            }
            data.add("settings", settings);
            root.add(module.getId(), data);
        }
        return root;
    }

    // runtime = true — конфиг загружается в игре: модули честно включаются/выключаются (без уведомлений)
    private static void apply(JsonObject root, boolean runtime) {
        for (Module module : ModuleManager.MODULES) {
            if (!root.has(module.getId())) {
                continue;
            }
            JsonObject data = root.getAsJsonObject(module.getId());
            if (data.has("key")) {
                module.setKey(data.get("key").getAsInt());
            }
            if (data.has("settings")) {
                JsonObject settings = data.getAsJsonObject("settings");
                migrateSettings(module.getId(), settings);
                for (Setting setting : module.getSettings()) {
                    if (settings.has(setting.getName())) {
                        try {
                            setting.load(settings.get(setting.getName()));
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
            if (data.has("enabled")) {
                boolean enabled = data.get("enabled").getAsBoolean();
                boolean was = module.isEnabled();
                module.loadEnabled(enabled);
                if (runtime && was != module.isEnabled()) {
                    try {
                        if (enabled) {
                            module.onEnable();
                        } else {
                            module.onDisable();
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    // Accept old English settings without silently resetting the user's profiles.
    private static void alias(JsonObject settings, String oldKey, String newKey) {
        if (!settings.has(newKey) && settings.has(oldKey)) settings.add(newKey, settings.get(oldKey));
    }
    private static void translate(JsonObject settings, String key, String... values) {
        if (!settings.has(key)) return;
        try {
            String value = settings.get(key).getAsString();
            for (int i = 0; i + 1 < values.length; i += 2) {
                if (value.equals(values[i])) { settings.addProperty(key, values[i + 1]); return; }
            }
        } catch (RuntimeException ignored) {}
    }
    private static void migrateSettings(String id, JsonObject s) {
        switch (id) {
            case "shift-tap" -> alias(s, "Release Time", "Длительность");
            case "hand-chams" -> {
                alias(s, "Style", "Эффект"); alias(s, "Color", "Цвет");
                alias(s, "Second Color", "Второй цвет"); alias(s, "Opacity", "Заливка");
                alias(s, "Speed", "Скорость"); alias(s, "Hands", "Руки"); alias(s, "Items", "Предметы");
                translate(s, "Эффект", "Gradient", "Перламутр", "Solid", "Перламутр", "Glow", "Плазма");
            }
            case "world" -> {
                alias(s, "Time", "Время"); alias(s, "Custom Time", "Своё время");
                alias(s, "Weather", "Погода"); alias(s, "Fog", "Туман"); alias(s, "Snow Amount", "Снегопад");
                translate(s, "Время", "Default", "По серверу", "Sunrise", "Рассвет", "Day", "День",
                        "Sunset", "Закат", "Night", "Ночь", "Custom", "Своё");
                translate(s, "Погода", "Default", "По серверу", "Clear", "Ясно", "Rain", "Дождь",
                        "Thunder", "Гроза", "Snow", "Зима", "Winter", "Зима");
                translate(s, "Туман", "Default", "Обычный", "Custom", "Морозный", "Off", "Выключен");
            }
            default -> {}
        }
    }

    private Config() {
    }
}
