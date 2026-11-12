package com.nuxvisuals.cosmetics;

import java.util.List;

import static com.nuxvisuals.cosmetics.Draw.glow;
import static com.nuxvisuals.cosmetics.Draw.mix;
import static com.nuxvisuals.cosmetics.Draw.pal;

// Хеллоуин, зима и дополнительные мечи
final class HolidayModels {
    private static final String H = "Хеллоуин";
    private static final String W = "Зима";

    static void addAll(List<Cosmetic> list) {
        // ---------- Хеллоуин ----------
        list.add(new Cosmetic("cape:halloween", "Хеллоуин", Category.CAPE,
                r -> CosmeticModels.cape(r, HolidayModels::capeHalloween), H));
        list.add(new Cosmetic("cape:vampire", "Вампир", Category.CAPE,
                r -> CosmeticModels.cape(r, HolidayModels::capeVampire), H));
        list.add(new Cosmetic("wings:bat", "Летучая мышь", Category.WINGS,
                r -> CosmeticModels.wing(r, Sprites.DEMON, Sprites.DEMON_GRIP, BAT_WING, 0.95f), H));
        list.add(new Cosmetic("back:broom", "Метла ведьмы", Category.BACK, HolidayModels::broom, H));
        list.add(new Cosmetic("pet:jack", "Тыковка", Category.PET, HolidayModels::jackPet, H));
        list.add(new Cosmetic("pet:bat", "Летучая мышь", Category.PET, HolidayModels::batPet, H));
        list.add(new Cosmetic("pet:spider", "Паучок", Category.PET, HolidayModels::spider, H));
        list.add(new Cosmetic("hat:pumpkin", "Тыква", Category.HAT, r -> CharacterModels.pumpkinHead(r, 8.2f), H));
        list.add(new Cosmetic("hat:witch", "Шляпа ведьмы", Category.HAT, HolidayModels::witchHat, H));
        list.add(new Cosmetic("sword:reaper", "Reaper", Category.SWORD,
                r -> r.spriteAt(Sprites.REAPER, REAPER, Sprites.REAPER_GRIP[0], Sprites.REAPER_GRIP[1], 1.2f), H));
        list.add(new Cosmetic("sword:bloodmoon", "Blood Moon", Category.SWORD,
                r -> r.spriteAt(Sprites.BLOODMOON, BLOODMOON, Sprites.BLOODMOON_GRIP[0], Sprites.BLOODMOON_GRIP[1], 1.2f), H));
        // ---------- Зима ----------
        list.add(new Cosmetic("cape:newyear", "Новогодний", Category.CAPE,
                r -> CosmeticModels.cape(r, HolidayModels::capeNewYear), W));
        list.add(new Cosmetic("wings:frost", "Морозные", Category.WINGS,
                r -> CosmeticModels.wing(r, Sprites.ANGEL, Sprites.ANGEL_GRIP, FROST_WING, 0.95f), W));
        list.add(new Cosmetic("back:gift", "Подарок", Category.BACK, HolidayModels::gift, W));
        list.add(new Cosmetic("back:tree", "Ёлочка", Category.BACK, HolidayModels::tree, W));
        list.add(new Cosmetic("pet:snowman", "Снеговичок", Category.PET, HolidayModels::snowPet, W));
        list.add(new Cosmetic("pet:penguin", "Пингвин", Category.PET, HolidayModels::penguin, W));
        list.add(new Cosmetic("hat:santa", "Шапка Санты", Category.HAT, HolidayModels::santaHat, W));
        list.add(new Cosmetic("hat:antlers", "Рога оленя", Category.HAT, HolidayModels::antlers, W));
        list.add(new Cosmetic("hat:earflaps", "Ушанка", Category.HAT, HolidayModels::ushanka, W));
        list.add(new Cosmetic("sword:candycane", "Candy Cane", Category.SWORD,
                r -> r.spriteAt(Sprites.CANDYCANE, CANDY, Sprites.CANDYCANE_GRIP[0], Sprites.CANDYCANE_GRIP[1], 1.4f), W));
        list.add(new Cosmetic("sword:icefang", "Ice Fang", Category.SWORD,
                r -> r.spriteAt(Sprites.ICEFANG, ICEFANG, Sprites.ICEFANG_GRIP[0], Sprites.ICEFANG_GRIP[1], 1.2f), W));
        // ---------- ещё мечи ----------
        list.add(new Cosmetic("sword:emerald", "Emerald Blade", Category.SWORD,
                r -> r.spriteAt(Sprites.EMERALD, EMERALD, Sprites.EMERALD_GRIP[0], Sprites.EMERALD_GRIP[1], 1.2f)));
        list.add(new Cosmetic("sword:thunder", "Thunder", Category.SWORD,
                r -> r.spriteAt(Sprites.THUNDER, THUNDER, Sprites.THUNDER_GRIP[0], Sprites.THUNDER_GRIP[1], 1.2f)));
        list.add(new Cosmetic("sword:dragonslayer", "Dragon Slayer", Category.SWORD,
                r -> r.spriteAt(Sprites.DRAGONSLAYER, DRAGON, Sprites.DRAGONSLAYER_GRIP[0], Sprites.DRAGONSLAYER_GRIP[1], 1.6f)));
        list.add(new Cosmetic("sword:celestial", "Celestial", Category.SWORD,
                r -> r.spriteAt(Sprites.CELESTIAL, CELESTIAL, Sprites.CELESTIAL_GRIP[0], Sprites.CELESTIAL_GRIP[1], 1.2f)));
    }

    // ==================== палитры ====================

    private static final int[] BAT_WING = pal('m', 0xE83A1E4A, 'b', 0xFF140A1C, 'r', glow(0xFF8A1F));
    private static final int[] FROST_WING = pal('w', 0xFFD8F0FF, 'l', 0xFF8CC8F0, 'W', glow(0xF4FBFF));
    private static final int[] REAPER = pal('O', glow(0xFF8A1F), 'o', 0xFF96400A, 'Y', glow(0xFFD23A),
            'k', 0xFF141014, 'h', 0xFF2A2230, 'p', 0xFFE8761C);
    private static final int[] BLOODMOON = pal('R', 0xFFC81E1E, 'r', 0xFF6E0A0A, 'P', glow(0xFF5050),
            'k', 0xFF141014, 'b', 0xFF2A0A0A);
    private static final int[] CANDY = pal('R', 0xFFE02828, 'W', 0xFFF8F8F8);
    private static final int[] ICEFANG = pal('I', 0xC896DCFF, 'i', 0xFF4696DC, 'W', glow(0xF0FAFF),
            'C', glow(0x5AC8FF), 'n', 0xFF1E325A);
    private static final int[] EMERALD = pal('E', 0xFF3CE678, 'e', 0xFF147A3C, 'W', glow(0xC8FFDC),
            'y', 0xFFDCAA28, 'b', 0xFF3A2414);
    private static final int[] THUNDER = pal('Y', glow(0xFFE04A), 'y', 0xFFC89A10, 'B', glow(0x78C8FF),
            'k', 0xFF1A1A24);
    private static final int[] DRAGON = pal('S', 0xFFB4BAC4, 's', 0xFF6A707C, 'W', 0xFFE8ECF2,
            'd', 0xFF8C5A1E, 'R', glow(0xFF3020), 'b', 0xFF3A2414);
    private static final int[] CELESTIAL = pal('W', 0xFFF6F8FF, 'l', 0xFFB8C0D8, 'Y', glow(0xFFE680),
            'G', 0xFFF0BE3C, 'B', glow(0x78C8FF), 'w', 0xFFE8E8F0);

    private static int hash(int x, int y) {
        int h = x * 374761393 + y * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return (h ^ (h >>> 16)) & 0x7FFFFFFF;
    }

    // ==================== плащи ====================

    private static final String[] PUMPKIN = {
            "..ooo..",
            ".ooooo.",
            "oyoooyo",
            "ooooooo",
            "oyyyyyo",
            ".ooooo.",
    };

    private static int capeHalloween(int x, int y, float t) {
        int lx = x - 1;
        int ly = y - 5;
        if (lx >= 0 && lx < 7 && ly >= 0 && ly < 6) {
            char c = PUMPKIN[ly].charAt(lx);
            if (c == 'o') return 0xFFE8761C;
            if (c == 'y') return glow(0xFFD23A);
        }
        if (x == 0 || x == 9 || y == 15) {
            return 0xFFE8761C;
        }
        return ((x + y) / 2) % 2 == 0 ? 0xFF1A1220 : 0xFF2A1A36;
    }

    private static int capeVampire(int x, int y, float t) {
        if (y <= 1) {
            return 0xFF8E0E1C;
        }
        if (x == 0 || x == 9) {
            return 0xFF8E0E1C;
        }
        int fy = Math.floorMod((int) (y + t * 0.04f), 16);
        if (hash(x, fy) % 23 == 0) {
            return 0xFF3A0A12;
        }
        return mix(0xFF16121A, 0xFF060408, y / 15f);
    }

    private static int capeNewYear(int x, int y, float t) {
        if (y >= 14 || x == 0 || x == 9) {
            return 0xFFF6F6F6;
        }
        int fy = Math.floorMod((int) (y - t * 0.06f), 16);
        if (hash(x, fy) % 7 == 0) {
            return glow(0xFFFFFF);
        }
        return mix(0xFFD32F2F, 0xFF8E1414, y / 15f);
    }

    // ==================== на спину ====================

    static void broom(Draw r) {
        r.push();
        r.move(0, 6, 1.2f);
        r.rotZ(55);
        r.box(-0.6f, -11, -0.6f, 0.6f, 6, 0.6f, 0xFF6B4423);
        r.box(-0.8f, 5, -0.8f, 0.8f, 6.2f, 0.8f, 0xFF8E2A20);
        r.box(-2, 6.2f, -1.6f, 2, 11, 1.6f, 0xFFC8A050);
        r.box(-2.6f, 8.5f, -2, 2.6f, 12.5f, 2, 0xFFB08838);
        r.pop();
    }

    static void gift(Draw r) {
        int box = 0xFF2E7D32;
        int ribbon = 0xFFD32F2F;
        r.box(-4, 1.5f, 0, 4, 10.5f, 6, box);
        r.box(-0.9f, 1.4f, -0.1f, 0.9f, 10.6f, 6.1f, ribbon);
        r.box(-4.1f, 5.1f, -0.1f, 4.1f, 6.9f, 6.1f, ribbon);
        r.box(-3, 0, 2, -0.5f, 1.5f, 4, ribbon);                    // бантик
        r.box(0.5f, 0, 2, 3, 1.5f, 4, ribbon);
        r.box(-0.8f, 0.2f, 2.3f, 0.8f, 1.5f, 3.7f, 0xFFB71C1C);
    }

    static void tree(Draw r) {
        r.push();
        r.move(0, 0, 2.6f);
        int g1 = 0xFF2E7D32;
        int g2 = 0xFF1B5E20;
        float[][] layers = {{9, 9, 12}, {7.5f, 5.5f, 9}, {5.5f, 2.5f, 5.5f}, {3.5f, -0.5f, 2.5f}};
        for (int i = 0; i < layers.length; i++) {
            float w = layers[i][0];
            r.box(-w / 2, layers[i][1] - 3, -w / 4, w / 2, layers[i][1], w / 4, i % 2 == 0 ? g1 : g2);
        }
        r.box(-1, 9, -1, 1, 11, 1, 0xFF5D4037);
        int[] lights = {glow(0xFF4040), glow(0xFFE04A), glow(0x40A0FF), glow(0xFF60D0)};
        for (int i = 0; i < 7; i++) {
            float y = 7.5f - i * 1.3f;
            float x = ((i * 37) % 7 - 3) * 0.8f * (1 - i / 9f);
            boolean on = ((int) (r.t / 8) + i) % 3 != 0;
            int c = on ? lights[i % 4] : 0xFF3A3A3A;
            r.box(x - 0.45f, y - 0.45f, -2.6f, x + 0.45f, y + 0.45f, -2.0f, c);
        }
        r.cube(0, -4.6f, 0, 1.8f, 1.8f, 1.8f, glow(0xFFE04A));    // звезда
        r.pop();
    }

    // ==================== питомцы ====================

    private static final String[] JACK_FACE = {
            "y....y",
            "yy..yy",
            "......",
            "yyyyyy",
            ".y..y.",
    };
    private static final int[] JACK_PAL = pal('y', glow(0xFFD23A));

    static void jackPet(Draw r) {
        r.box(-3.5f, -3, -3.5f, 3.5f, 3, 3.5f, 0xFFE8761C);
        r.box(-1.5f, -3.1f, -3.6f, -1, 3.1f, 3.6f, 0xFFB8530E);
        r.box(1, -3.1f, -3.6f, 1.5f, 3.1f, 3.6f, 0xFFB8530E);
        r.box(-0.5f, -4.6f, -0.5f, 0.5f, -3, 0.5f, 0xFF3F7A2A);
        r.push();
        r.move(-3, -2, -3.75f);
        r.sprite(JACK_FACE, JACK_PAL, 0, 0.3f);
        r.pop();
    }

    static void batPet(Draw r) {
        int fur = 0xFF2A1E2E;
        r.box(-1.6f, -1.6f, -1.6f, 1.6f, 1.8f, 1.6f, fur);
        r.box(-1.4f, -2.8f, -1.2f, -0.6f, -1.6f, -0.4f, fur);
        r.box(0.6f, -2.8f, -1.2f, 1.4f, -1.6f, -0.4f, fur);
        r.box(-1.1f, -0.9f, -1.7f, -0.4f, -0.3f, -1.6f, glow(0xFF3030));
        r.box(0.4f, -0.9f, -1.7f, 1.1f, -0.3f, -1.6f, glow(0xFF3030));
        float flap = (float) Math.sin(r.t * 1.2f) * 40;
        for (int side = -1; side <= 1; side += 2) {
            r.push();
            r.move(side * 1.6f, -0.6f, 0);
            r.rotZ(side * flap);
            r.box(side < 0 ? -5 : 0, -0.15f, -1.4f, side < 0 ? 0 : 5, 0.15f, 1.6f, 0xE83A1E4A);
            r.box(side < 0 ? -5 : 4.2f, -0.2f, -1.6f, side < 0 ? -4.2f : 5, 0.2f, 1.8f, 0xFF140A1C);
            r.pop();
        }
    }

    static void spider(Draw r) {
        int body = 0xFF2A2420;
        r.box(-2.2f, -1.5f, -0.5f, 2.2f, 1.5f, 4, body);
        r.box(-1.6f, -1.2f, -3.2f, 1.6f, 1.2f, -0.5f, 0xFF3A322C);
        r.box(-1.2f, -0.6f, -3.3f, -0.4f, 0, -3.2f, glow(0xFF2020));
        r.box(0.4f, -0.6f, -3.3f, 1.2f, 0, -3.2f, glow(0xFF2020));
        for (int i = 0; i < 4; i++) {
            float z = -0.2f + i * 1.1f;
            float wig = (float) Math.sin(r.t * 0.5f + i) * 8;
            for (int side = -1; side <= 1; side += 2) {
                r.push();
                r.move(side * 2.2f, 0, z);
                r.rotZ(side * (-25 + wig));
                r.box(side < 0 ? -4 : 0, -0.25f, -0.25f, side < 0 ? 0 : 4, 0.25f, 0.25f, body);
                r.pop();
            }
        }
    }

    static void snowPet(Draw r) {
        r.cube(0, 2, 0, 5, 5, 5, 0xFFF4F8FF);
        r.cube(0, -2, 0, 3.8f, 3.6f, 3.8f, 0xFFF4F8FF);
        r.box(-1.2f, -2.8f, -2.0f, -0.6f, -2.2f, -1.9f, 0xFF1A1A1A);
        r.box(0.6f, -2.8f, -2.0f, 1.2f, -2.2f, -1.9f, 0xFF1A1A1A);
        r.box(-0.3f, -2.1f, -3.4f, 0.3f, -1.5f, -1.9f, 0xFFF08A24);
        r.box(-2.2f, -4, -2.2f, 2.2f, -3.6f, 2.2f, 0xFF1A1A1F);
        r.box(-1.4f, -6, -1.4f, 1.4f, -4, 1.4f, 0xFF1A1A1F);
        r.box(-2.0f, -0.5f, -2.0f, 2.0f, 0.3f, 2.0f, 0xFFC62828);
    }

    static void penguin(Draw r) {
        int black = 0xFF1C1E26;
        r.box(-2.5f, -3, -2, 2.5f, 3.5f, 2.5f, black);
        r.box(-1.8f, -1.5f, -2.1f, 1.8f, 3.3f, -1.9f, 0xFFF4F4F4);
        r.box(-2, -6, -2, 2, -3, 2, black);
        r.box(-1.5f, -5.5f, -2.1f, 1.5f, -3.2f, -1.9f, 0xFFF4F4F4);
        r.box(-1.2f, -5, -2.2f, -0.5f, -4.3f, -2.1f, 0xFF111111);
        r.box(0.5f, -5, -2.2f, 1.2f, -4.3f, -2.1f, 0xFF111111);
        r.box(-0.6f, -4.2f, -3.2f, 0.6f, -3.6f, -2, 0xFFF2A12A);
        float flap = (float) Math.sin(r.t * 0.3f) * 15;
        for (int side = -1; side <= 1; side += 2) {
            r.push();
            r.move(side * 2.5f, -2.5f, 0);
            r.rotZ(side * (10 + flap));
            r.box(side < 0 ? -0.8f : 0, 0, -1.2f, side < 0 ? 0 : 0.8f, 4.5f, 1.2f, black);
            r.pop();
        }
        r.box(-2, 3.5f, -2.6f, -0.4f, 4.1f, 0.4f, 0xFFF2A12A);
        r.box(0.4f, 3.5f, -2.6f, 2, 4.1f, 0.4f, 0xFFF2A12A);
        r.box(-2.6f, -3.4f, -2.1f, 2.6f, -2.6f, 2.6f, 0xFFC62828);   // шарфик
    }

    // ==================== шапки ====================

    static void witchHat(Draw r) {
        int black = 0xFF1A1420;
        r.box(-6.5f, -0.6f, -6.5f, 6.5f, 0.4f, 6.5f, black);
        r.box(-4.3f, -2, -4.3f, 4.3f, -0.6f, 4.3f, 0xFFE8761C);
        r.box(-1.2f, -1.9f, -4.5f, 1.2f, -0.7f, -4.3f, 0xFFE2B33C);
        float z = 0;
        float y = -2;
        for (int i = 0; i < 6; i++) {
            float w = 8.2f - i * 1.3f;
            z += i * 0.35f;
            r.box(-w / 2, y - 2, z - w / 2, w / 2, y, z + w / 2, i % 2 == 0 ? black : 0xFF241C2C);
            y -= 2;
        }
        r.box(-0.5f, y - 1, z + 1, 0.5f, y, z + 2.6f, black);
    }

    static void santaHat(Draw r) {
        int red = 0xFFD32F2F;
        r.box(-4.6f, -1.6f, -4.6f, 4.6f, 0.6f, 4.6f, 0xFFF6F6F6);
        r.box(-4, -4, -4, 4, -1.6f, 4, red);
        r.box(-3, -6, -2.5f, 3, -4, 3.5f, red);
        r.box(-2, -7.4f, 0, 2, -6, 4.2f, red);
        r.box(-1.2f, -7.6f, 3, 1.2f, -5.4f, 6, red);
        r.cube(0, -5, 6.6f, 2.2f, 2.2f, 2.2f, 0xFFF6F6F6);
    }

    static void antlers(Draw r) {
        int brown = 0xFF8D6E4A;
        for (int side = -1; side <= 1; side += 2) {
            r.push();
            r.move(side * 2.8f, 0, -0.5f);
            r.rotZ(side * 18);
            r.box(-0.5f, -6, -0.5f, 0.5f, 0, 0.5f, brown);
            r.box(side < 0 ? -2.5f : 0, -6, -0.45f, side < 0 ? 0 : 2.5f, -5.1f, 0.45f, brown);
            r.box(side < 0 ? -2.5f : 1.6f, -8, -0.45f, side < 0 ? -1.6f : 2.5f, -6, 0.45f, brown);
            r.box(-0.45f, -9, -0.45f, 0.45f, -6, 0.45f, brown);
            r.box(side < 0 ? -1.8f : 0, -3.4f, -0.45f, side < 0 ? 0 : 1.8f, -2.6f, 0.45f, brown);
            r.box(side < 0 ? -1.8f : 1, -4.6f, -0.45f, side < 0 ? -1 : 1.8f, -3.4f, 0.45f, brown);
            r.pop();
        }
        r.cube(0, 4.6f, -4.6f, 1.6f, 1.6f, 1.2f, glow(0xFF3030));     // красный нос Рудольфа
    }

    static void ushanka(Draw r) {
        int fur = 0xFF6D5340;
        int furLight = 0xFFA08468;
        r.box(-4.6f, -3.2f, -4.6f, 4.6f, 0.6f, 4.6f, fur);
        r.box(-4.9f, -0.6f, -4.9f, 4.9f, 0.9f, -3.9f, furLight);
        r.box(-0.9f, -2.4f, -5.0f, 0.9f, -0.8f, -4.9f, glow(0xE53935));
        for (int side = -1; side <= 1; side += 2) {
            float x1 = side < 0 ? -5.1f : 4.1f;
            r.box(x1, 0.6f, -2.5f, x1 + 1, 6, 2.5f, furLight);
        }
        r.box(-4.6f, 0.6f, 3.6f, 4.6f, 3, 4.8f, furLight);
    }

    private HolidayModels() {
    }
}
