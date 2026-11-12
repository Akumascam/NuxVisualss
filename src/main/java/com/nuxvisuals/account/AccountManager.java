package com.nuxvisuals.account;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.nuxvisuals.NuxVisuals;
import com.nuxvisuals.mixin.MinecraftClientAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;
import net.minecraft.util.Uuids;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

// Оффлайн-аккаунты (по нику) + возврат к основному аккаунту лаунчера.
// Список хранится в .minecraft/config/nuxvisuals-accounts.json
public final class AccountManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Pattern NICK = Pattern.compile("^[A-Za-z0-9_]{3,16}$");

    private static final List<String> OFFLINE = new ArrayList<>();
    private static Session original;
    private static ProfileKeys originalKeys;
    private static CompletableFuture<ProfileResult> originalProfile;
    private static boolean loaded;

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("nuxvisuals-accounts.json");
    }

    // запоминаем аккаунт, с которым запущена игра
    private static void init() {
        if (original == null) {
            MinecraftClient client = MinecraftClient.getInstance();
            original = client.getSession();
            originalKeys = client.getProfileKeys();
            originalProfile = ((MinecraftClientAccessor) client).nuxvisuals$getGameProfileFuture();
        }
        if (!loaded) {
            loaded = true;
            load();
        }
    }

    public static Session original() {
        init();
        return original;
    }

    public static boolean isOriginalOffline() {
        return original().getAccountType() != Session.AccountType.MSA;
    }

    public static List<String> offline() {
        init();
        return OFFLINE;
    }

    public static boolean isValidNick(String nick) {
        return NICK.matcher(nick).matches();
    }

    public static String current() {
        return MinecraftClient.getInstance().getSession().getUsername();
    }

    // null — всё ок, иначе текст ошибки
    public static String add(String nick) {
        init();
        nick = nick.trim();
        if (!isValidNick(nick)) {
            return "Ник: 3–16 символов, только A–Z, 0–9 и _";
        }
        for (String existing : OFFLINE) {
            if (existing.equalsIgnoreCase(nick)) {
                return "Такой аккаунт уже есть";
            }
        }
        OFFLINE.add(nick);
        save();
        return null;
    }

    public static void remove(String nick) {
        init();
        OFFLINE.remove(nick);
        save();
    }

    public static void loginOffline(String nick) {
        init();
        Session session = new Session(nick, Uuids.getOfflinePlayerUuid(nick), "0",
                Optional.empty(), Optional.empty(), Session.AccountType.LEGACY);
        MinecraftClientAccessor client = (MinecraftClientAccessor) MinecraftClient.getInstance();
        client.nuxvisuals$setSession(session);
        // ключи чата старого аккаунта к новому нику не подходят
        client.nuxvisuals$setProfileKeys(ProfileKeys.MISSING);
        // null -> игра возьмёт ник и UUID из новой сессии (одиночная игра, LAN)
        client.nuxvisuals$setGameProfileFuture(CompletableFuture.completedFuture(null));
        NuxVisuals.LOGGER.info("NuxVisuals: вход как {}", nick);
    }

    public static void loginOriginal() {
        init();
        MinecraftClientAccessor client = (MinecraftClientAccessor) MinecraftClient.getInstance();
        client.nuxvisuals$setSession(original);
        client.nuxvisuals$setProfileKeys(originalKeys);
        client.nuxvisuals$setGameProfileFuture(originalProfile);
    }

    private static void load() {
        try {
            Path file = file();
            if (!Files.exists(file)) {
                return;
            }
            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            if (root.has("offline")) {
                for (JsonElement element : root.getAsJsonArray("offline")) {
                    String nick = element.getAsString();
                    if (isValidNick(nick) && !OFFLINE.contains(nick)) {
                        OFFLINE.add(nick);
                    }
                }
            }
        } catch (Exception e) {
            NuxVisuals.LOGGER.warn("Не удалось прочитать аккаунты NuxVisuals", e);
        }
    }

    private static void save() {
        try {
            JsonObject root = new JsonObject();
            JsonArray array = new JsonArray();
            OFFLINE.forEach(array::add);
            root.add("offline", array);
            Files.writeString(file(), GSON.toJson(root));
        } catch (Exception e) {
            NuxVisuals.LOGGER.warn("Не удалось сохранить аккаунты NuxVisuals", e);
        }
    }

    private AccountManager() {
    }
}
