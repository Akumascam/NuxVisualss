package com.nuxvisuals.waypoint;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nuxvisuals.NuxVisuals;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Точки хранятся отдельно для каждого сервера / мира: config/nuxvisuals-waypoints.json
public final class WaypointManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, List<Waypoint>> ALL = new HashMap<>();
    private static boolean loaded;

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("nuxvisuals-waypoints.json");
    }

    // ключ текущего мира: адрес сервера или имя одиночного мира
    public static String worldKey() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.isInSingleplayer() && client.getServer() != null) {
            return "sp:" + client.getServer().getSaveProperties().getLevelName();
        }
        ServerInfo server = client.getCurrentServerEntry();
        return server != null ? "mp:" + server.address : "unknown";
    }

    public static String dimension() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.world == null ? "" : client.world.getRegistryKey().getValue().toString();
    }

    public static String dimensionName(String dimension) {
        return switch (dimension) {
            case "minecraft:overworld" -> "Мир";
            case "minecraft:the_nether" -> "Ад";
            case "minecraft:the_end" -> "Энд";
            default -> dimension;
        };
    }

    public static List<Waypoint> current() {
        load();
        if (MinecraftClient.getInstance().world == null) {
            return new ArrayList<>();
        }
        return ALL.computeIfAbsent(worldKey(), k -> new ArrayList<>());
    }

    public static void addHere() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }
        add("", client.player.getBlockX(), client.player.getBlockY(), client.player.getBlockZ());
    }

    // точка по введённым координатам (пустое имя -> «Точка N»)
    public static void add(String name, int x, int y, int z) {
        if (MinecraftClient.getInstance().world == null) {
            return;
        }
        List<Waypoint> list = current();
        int n = list.size() + 1;
        float hue = (n * 0.17f + 0.6f) % 1f;
        String title = name == null || name.isBlank() ? "Точка " + n : name.trim();
        list.add(new Waypoint(title, x, y, z, dimension(), hue));
        save();
    }

    public static void remove(Waypoint waypoint) {
        current().remove(waypoint);
        save();
    }

    private static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        try {
            Path file = file();
            if (!Files.exists(file)) {
                return;
            }
            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            for (String key : root.keySet()) {
                List<Waypoint> list = new ArrayList<>();
                for (JsonElement e : root.getAsJsonArray(key)) {
                    JsonObject o = e.getAsJsonObject();
                    Waypoint wp = new Waypoint(o.get("name").getAsString(), o.get("x").getAsInt(),
                            o.get("y").getAsInt(), o.get("z").getAsInt(), o.get("dimension").getAsString(),
                            o.get("hue").getAsFloat());
                    wp.visible = !o.has("visible") || o.get("visible").getAsBoolean();
                    if (o.has("color")) {
                        wp.color = o.get("color").getAsInt();
                    }
                    list.add(wp);
                }
                ALL.put(key, list);
            }
        } catch (Exception e) {
            NuxVisuals.LOGGER.warn("Не удалось прочитать вейпоинты NuxVisuals", e);
        }
    }

    public static void save() {
        try {
            JsonObject root = new JsonObject();
            for (Map.Entry<String, List<Waypoint>> entry : ALL.entrySet()) {
                JsonArray array = new JsonArray();
                for (Waypoint wp : entry.getValue()) {
                    JsonObject o = new JsonObject();
                    o.addProperty("name", wp.name);
                    o.addProperty("x", wp.x);
                    o.addProperty("y", wp.y);
                    o.addProperty("z", wp.z);
                    o.addProperty("dimension", wp.dimension);
                    o.addProperty("hue", wp.hue);
                    o.addProperty("visible", wp.visible);
                    o.addProperty("color", wp.color);
                    array.add(o);
                }
                root.add(entry.getKey(), array);
            }
            Files.writeString(file(), GSON.toJson(root));
        } catch (Exception e) {
            NuxVisuals.LOGGER.warn("Не удалось сохранить вейпоинты NuxVisuals", e);
        }
    }

    private WaypointManager() {
    }
}
