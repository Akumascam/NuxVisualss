package com.nuxvisuals.cosmetics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nuxvisuals.NuxVisuals;
import com.nuxvisuals.cosmetics.Rig.Anchor;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// 3D-косметика: плащи, крылья, вещи на спину, питомцы, шапки и мечи.
// Всё рисуется кодом (объёмные кубики), видишь её только ты — на своём персонаже.
public final class Cosmetics {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<Cosmetic> BUILTINS = AtelierCosmetics.extend(ModernCosmetics.upgrade(CosmeticModels.all()));
    private static final List<Cosmetic> ITEMS = new java.util.ArrayList<>(BUILTINS);
    private static int catalogVersion;
    private static final Map<String, Cosmetic> BY_ID = new HashMap<>();
    private static final Map<Category, Cosmetic> EQUIPPED = new EnumMap<>(Category.class);
    private static final Map<String, float[]> BOUNDS = new HashMap<>();
    private static final Map<String, float[]> SWORD_BOUNDS = new HashMap<>();
    private static boolean loaded;

    static {
        for (Cosmetic c : ITEMS) {
            BY_ID.put(c.id(), c);
        }
    }

    public static List<Cosmetic> items() {
        ensureLoaded();
        return ITEMS;
    }

    private static void ensureLoaded() {
        if (!loaded) {
            loaded = true;
            reloadCustom();
            load();
        }
    }

    public static int catalogVersion() {ensureLoaded();return catalogVersion;}
    public static void reloadCustom() {
        Map<Category,String> selected=new EnumMap<>(Category.class);
        EQUIPPED.forEach((category,c)->selected.put(category,c.id()));
        ITEMS.clear();ITEMS.addAll(BUILTINS);ITEMS.addAll(CustomCosmetics.readAll());
        BY_ID.clear();for(Cosmetic c:ITEMS)BY_ID.put(c.id(),c);
        EQUIPPED.clear();selected.forEach((category,id)->{Cosmetic c=BY_ID.get(id);if(c!=null)EQUIPPED.put(category,c);});
        BOUNDS.clear();SWORD_BOUNDS.clear();catalogVersion++;
    }

    // ---------- надетое ----------

    public static Cosmetic equipped(Category category) {
        ensureLoaded();
        return EQUIPPED.get(category);
    }

    public static boolean isEquipped(Cosmetic c) {
        return equipped(c.category()) == c;
    }

    public static void toggle(Cosmetic c) {
        ensureLoaded();
        if (EQUIPPED.get(c.category()) == c) {
            EQUIPPED.remove(c.category());
        } else {
            EQUIPPED.put(c.category(), c);
        }
    }

    public static void unequip(Category category) {
        ensureLoaded();
        EQUIPPED.remove(category);
    }

    public static void clear() {
        ensureLoaded();
        EQUIPPED.clear();
    }

    public static int count() {
        ensureLoaded();
        return EQUIPPED.size();
    }

    // ---------- на какой меч надевается скин меча ----------

    private static final String[][] SWORD_TYPES = {
            {"any", "Любой меч"}, {"netherite", "Незеритовый"}, {"diamond", "Алмазный"}, {"iron", "Железный"},
            {"golden", "Золотой"}, {"stone", "Каменный"}, {"wooden", "Деревянный"}};
    private static int swordType;

    public static String swordTargetLabel() {
        return SWORD_TYPES[swordType][1];
    }

    public static void cycleSwordTarget(int dir) {
        swordType = Math.floorMod(swordType + dir, SWORD_TYPES.length);
    }

    // подходит ли предмет под выбранный меч
    public static boolean swordMatches(net.minecraft.item.ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return switch (SWORD_TYPES[swordType][0]) {
            case "netherite" -> stack.isOf(Items.NETHERITE_SWORD);
            case "diamond" -> stack.isOf(Items.DIAMOND_SWORD);
            case "iron" -> stack.isOf(Items.IRON_SWORD);
            case "golden" -> stack.isOf(Items.GOLDEN_SWORD);
            case "stone" -> stack.isOf(Items.STONE_SWORD);
            case "wooden" -> stack.isOf(Items.WOODEN_SWORD);
            default -> stack.isIn(net.minecraft.registry.tag.ItemTags.SWORDS);
        };
    }

    // надет скин меча и в руке подходящий меч
    public static boolean swordActive(net.minecraft.item.ItemStack stack) {
        return equipped(Category.SWORD) != null && swordMatches(stack);
    }

    // скин меча от первого лица: ставим туда же, где ванильный меч (по его трансформам из модели предмета)
    private static float[] swordBounds(Cosmetic c){
        return SWORD_BOUNDS.computeIfAbsent(c.id(),id->{
            float[] b={Float.MAX_VALUE,Float.MAX_VALUE,Float.MAX_VALUE,-Float.MAX_VALUE,-Float.MAX_VALUE,-Float.MAX_VALUE};
            CosmeticData d=new CosmeticData();d.me=true;d.swordHeld=true;c.shape().draw(new Draw(new MatrixStack(),d,b));
            return b[0]>b[3]?new float[]{-4,-20,-1,4,4,1}:b;
        });
    }
    public static int max() {
        return Category.values().length;
    }

    // ---------- данные игрока (из миксина) ----------

    public static void fill(CosmeticData d, AbstractClientPlayerEntity player, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        d.me = player == client.player
                || (client.player != null && player.getUuid().equals(client.player.getUuid()));
        d.friend = !d.me && com.nuxvisuals.module.impl.Friends.wingsFor(player);
        if (!d.me && !d.friend) {
            return;
        }
        d.invisible = player.isInvisible();
        d.emptyHand = player.getMainHandStack().isEmpty();
        d.swordHeld = swordActive(player.getMainHandStack());
        d.leftHanded = player.getMainArm() == Arm.LEFT;
        d.elytra = player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
        d.sneaking = player.isInSneakingPose();
        d.flying = player.getAbilities().flying;
        d.time = player.age + tickDelta;
        d.speed = (float) player.getVelocity().horizontalLength();
    }

    // яркость от освещения (0.4..1)
    public static float brightness(int light) {
        int block = LightmapTextureManager.getBlockLightCoordinates(light);
        int sky = LightmapTextureManager.getSkyLightCoordinates(light);
        return 0.55f + 0.45f * Math.max(block, sky) / 15f;
    }

    // ---------- рисование ----------

    // всё надетое
    public static void renderAll(Draw r, Rig rig) {
        ensureLoaded();
        r.backOccupied = EQUIPPED.containsKey(Category.BACK);
        for (Category category : Category.values()) {
            Cosmetic c = EQUIPPED.get(category);
            if (c != null) {
                renderOne(r, rig, c);
            }
        }
    }

    // крылья на друге (тип/цвет/свечение/размер — из модуля Friends)
    public static void renderFriend(Draw r, Rig rig) {
        com.nuxvisuals.module.impl.Friends f = com.nuxvisuals.module.impl.Friends.get();
        if (f == null) {
            return;
        }
        Cosmetic c = BY_ID.get(f.wingsId());
        if (c == null) {
            return;
        }
        r.backOccupied = false;
        r.wingScale = f.size.getFloat();
        r.tint = f.customColor.get() ? (f.color.get() & 0xFFFFFF) : -1;
        r.glow(f.glow.get());
        try {
            renderOne(r, rig, c);
        } finally {
            r.glow(false);
            r.tint = -1;
            r.wingScale = 1f;
        }
    }

    // один предмет — прикрепляем к нужной части тела
    public static void renderOne(Draw r, Rig rig, Cosmetic c) {
        CosmeticData d = r.d;
        float t = r.t;
        r.push();
        try {
            switch (c.category()) {
                case MODEL -> {
                    // модель целиком: каждая часть крепится к своей части тела
                    for (Anchor anchor : Anchor.values()) {
                        r.push();
                        rig.apply(r.m, anchor);
                        r.part = anchor;
                        c.shape().draw(r);
                        r.pop();
                    }
                    r.part = null;
                }
                case CAPE -> {
                    if (d.elytra) {
                        return;
                    }
                    rig.apply(r.m, Anchor.BODY);
                    r.move(0, 0, 2);
                    c.shape().draw(r);
                }
                case WINGS -> {
                    if (d.elytra) {
                        return;
                    }
                    rig.apply(r.m, Anchor.BODY);
                    r.move(0, 1.5f, 2.6f + (r.backOccupied ? 5f : 0f));
                    if (r.wingScale != 1f) {
                        r.scale(r.wingScale);
                    }
                    float flap = d.flying ? (float) Math.sin(t * 0.45f) * 28f : (float) Math.sin(t * 0.11f) * 8f;
                    float open = (d.flying ? 12f : 26f) + Math.min(1f, d.speed * 4f) * 14f;
                    for (int side = -1; side <= 1; side += 2) {
                        r.push();
                        r.scale(side, 1, 1);
                        r.move(1, 0, 0);
                        r.rotY(-(open + flap));
                        r.rotZ(-12);
                        c.shape().draw(r);
                        r.pop();
                    }
                }
                case BACK -> {
                    rig.apply(r.m, Anchor.BODY);
                    r.move(0, 0, 2);
                    c.shape().draw(r);
                }
                case PET -> {
                    rig.apply(r.m, Anchor.BODY);
                    r.move(11, -7 + (float) Math.sin(t * 0.08f) * 1.2f, 2);
                    r.rotY((float) Math.sin(t * 0.03f) * 20f);
                    r.scale(0.9f);
                    c.shape().draw(r);
                }
                case HAT -> {
                    rig.apply(r.m, Anchor.HEAD);
                    r.move(0, -8, 0);
                    c.shape().draw(r);
                }
                case SWORD -> {
                    if(rig!=Rig.MANNEQUIN)return; // held items now use vanilla baked item models
                    // скин меча виден только когда в руке выбранный меч (в меню — всегда)
                    if (!d.swordHeld && rig != Rig.MANNEQUIN) {
                        return;
                    }
                    boolean left = d.leftHanded;
                    rig.apply(r.m, left ? Anchor.LEFT_ARM : Anchor.RIGHT_ARM);
                    r.move(left ? 1 : -1, 10, -0.5f);
                    r.rotX(90);
                    r.rotY(left ? -90 : 90);
                    r.scale(SwordFit.scale(swordBounds(c),16f));
                    c.shape().draw(r);
                }
            }
        } finally {
            r.pop();
        }
    }

    // габариты предмета (для превью в меню) — считаются один раз
    public static float[] bounds(Cosmetic c) {
        return BOUNDS.computeIfAbsent(c.id(), id -> {
            float[] b = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE,
                    -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
            // Sample the new animated collection once, rather than fitting only its rest pose.
            int samples=c.id().startsWith("atelier:")?12:1;
            for(int frame=0;frame<samples;frame++) {
                CosmeticData data=new CosmeticData();data.time=frame*12.5f;
                Draw r = new Draw(new MatrixStack(), data, b);
                renderOne(r, Rig.MANNEQUIN, c);
            }
            if (b[0] > b[3]) {
                return new float[]{-4, -4, -4, 4, 4, 4};
            }
            return b;
        });
    }

    public static void mannequin(Draw r) {
        if (!EQUIPPED.containsKey(Category.MODEL)) {
            CosmeticModels.mannequin(r);
        }
    }

    // надета модель — обычное тело игрока прячется
    public static boolean hidesBody() {
        ensureLoaded();
        return EQUIPPED.containsKey(Category.MODEL);
    }

    // ---------- сохранение ----------

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("nuxvisuals").resolve("cosmetics3d.json");
    }

    private static void load() {
        try {
            if (Files.exists(file())) {
                JsonObject root = JsonParser.parseString(Files.readString(file())).getAsJsonObject();
                if (root.has("sword_for")) {
                    String want = root.get("sword_for").getAsString();
                    for (int i = 0; i < SWORD_TYPES.length; i++) {
                        if (SWORD_TYPES[i][0].equals(want)) {
                            swordType = i;
                        }
                    }
                }
                for (Category category : Category.values()) {
                    String key = category.name().toLowerCase(Locale.ROOT);
                    if (root.has(key)) {
                        Cosmetic c = BY_ID.get(root.get(key).getAsString());
                        if (c != null && c.category() == category) {
                            EQUIPPED.put(category, c);
                        }
                    }
                }
            }
        } catch (Exception e) {
            NuxVisuals.LOGGER.warn("Не удалось прочитать cosmetics3d.json", e);
        }
    }

    public static boolean save() {
        ensureLoaded();
        JsonObject root = new JsonObject();
        for (Map.Entry<Category, Cosmetic> e : EQUIPPED.entrySet()) {
            root.addProperty(e.getKey().name().toLowerCase(Locale.ROOT), e.getValue().id());
        }
        root.addProperty("sword_for", SWORD_TYPES[swordType][0]);
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), GSON.toJson(root));
            return true;
        } catch (Exception e) {
            NuxVisuals.LOGGER.warn("Не удалось сохранить cosmetics3d.json", e);
            return false;
        }
    }

    private Cosmetics() {
    }
}
