package com.nuxvisuals.cosmetics;

import com.nuxvisuals.module.impl.ClickGuiModule;

import java.util.ArrayList;
import java.util.List;

import static com.nuxvisuals.cosmetics.Draw.alpha;
import static com.nuxvisuals.cosmetics.Draw.glow;
import static com.nuxvisuals.cosmetics.Draw.mix;
import static com.nuxvisuals.cosmetics.Draw.pal;

// Все 3D-модели косметики. Каждая модель рисуется в своей локальной системе координат,
// а куда её прикрепить на игроке — решает Cosmetics.renderOne (по разделу).
final class CosmeticModels {

    static List<Cosmetic> all() {
        List<Cosmetic> list = new ArrayList<>();
        // ---------- плащи ----------
        list.add(new Cosmetic("cape:nux", "Nux", Category.CAPE, r -> cape(r, CosmeticModels::capeNux)));
        list.add(new Cosmetic("cape:galaxy", "Галактика", Category.CAPE, r -> cape(r, CosmeticModels::capeGalaxy)));
        list.add(new Cosmetic("cape:flame", "Пламя", Category.CAPE, r -> cape(r, CosmeticModels::capeFlame)));
        list.add(new Cosmetic("cape:sakura", "Сакура", Category.CAPE, r -> cape(r, CosmeticModels::capeSakura)));
        list.add(new Cosmetic("cape:frost", "Мороз", Category.CAPE, r -> cape(r, CosmeticModels::capeFrost)));
        list.add(new Cosmetic("cape:shadow", "Тень", Category.CAPE, r -> cape(r, CosmeticModels::capeShadow)));
        list.add(new Cosmetic("cape:royal", "Королевский", Category.CAPE, r -> cape(r, CosmeticModels::capeRoyal)));
        list.add(new Cosmetic("cape:rainbow", "Радуга", Category.CAPE, r -> cape(r, CosmeticModels::capeRainbow)));
        // ---------- крылья (рисуется одно, второе — зеркально) ----------
        list.add(new Cosmetic("wings:angel", "Ангельские", Category.WINGS,
                r -> wing(r, Sprites.ANGEL, Sprites.ANGEL_GRIP, ANGEL, 0.95f)));
        list.add(new Cosmetic("wings:demon", "Демонические", Category.WINGS,
                r -> wing(r, Sprites.DEMON, Sprites.DEMON_GRIP, DEMON, 0.95f)));
        list.add(new Cosmetic("wings:butterfly", "Бабочка", Category.WINGS,
                r -> wing(r, Sprites.BUTTERFLY, Sprites.BUTTERFLY_GRIP, BUTTERFLY, 0.9f)));
        list.add(new Cosmetic("wings:phoenix", "Феникс", Category.WINGS,
                r -> wing(r, Sprites.PHOENIXW, Sprites.PHOENIXW_GRIP, PHOENIX_WING, 0.95f)));
        list.add(new Cosmetic("wings:crystal", "Кристальные", Category.WINGS,
                r -> wing(r, Sprites.CRYSTAL, Sprites.CRYSTAL_GRIP, CRYSTAL_WING, 0.95f)));
        // ---------- на спину ----------
        list.add(new Cosmetic("back:backpack", "Рюкзак", Category.BACK, CosmeticModels::backpack));
        list.add(new Cosmetic("back:quiver", "Колчан", Category.BACK, CosmeticModels::quiver));
        list.add(new Cosmetic("back:jetpack", "Джетпак", Category.BACK, CosmeticModels::jetpack));
        list.add(new Cosmetic("back:guitar", "Гитара", Category.BACK, CosmeticModels::guitar));
        list.add(new Cosmetic("back:shield", "Щит рыцаря", Category.BACK, CosmeticModels::shield));
        // ---------- питомцы ----------
        list.add(new Cosmetic("pet:slime", "Слайм", Category.PET, CosmeticModels::slime));
        list.add(new Cosmetic("pet:ghost", "Призрак", Category.PET, CosmeticModels::ghost));
        list.add(new Cosmetic("pet:bee", "Пчёлка", Category.PET, CosmeticModels::bee));
        list.add(new Cosmetic("pet:fox", "Лисёнок", Category.PET, CosmeticModels::fox));
        list.add(new Cosmetic("pet:crystal", "Кристалл", Category.PET, CosmeticModels::crystalPet));
        // ---------- шапки ----------
        list.add(new Cosmetic("hat:crown", "Корона", Category.HAT, CosmeticModels::crown));
        list.add(new Cosmetic("hat:halo", "Нимб", Category.HAT, CosmeticModels::halo));
        list.add(new Cosmetic("hat:wizard", "Шляпа мага", Category.HAT, CosmeticModels::wizard));
        list.add(new Cosmetic("hat:cat", "Кошачьи ушки", Category.HAT, CosmeticModels::catEars));
        list.add(new Cosmetic("hat:tophat", "Цилиндр", Category.HAT, CosmeticModels::topHat));
        list.add(new Cosmetic("hat:horns", "Рожки", Category.HAT, CosmeticModels::horns));
        // ---------- мечи ----------
        list.add(new Cosmetic("sword:requiem", "Requiem of Hell", Category.SWORD,
                r -> r.spriteAt(Sprites.REQUIEM, REQUIEM, Sprites.REQUIEM_GRIP[0], Sprites.REQUIEM_GRIP[1], 1.2f)));
        list.add(new Cosmetic("sword:phoenix", "Phoenix Grace", Category.SWORD,
                r -> r.spriteAt(Sprites.PHOENIX, PHOENIX, Sprites.PHOENIX_GRIP[0], Sprites.PHOENIX_GRIP[1], 1.2f)));
        list.add(new Cosmetic("sword:pharaoh", "Pharaoh's Treasure", Category.SWORD,
                r -> r.spriteAt(Sprites.PHARAOH, PHARAOH, Sprites.PHARAOH_GRIP[0], Sprites.PHARAOH_GRIP[1], 1.2f)));
        list.add(new Cosmetic("sword:frost", "Frost Edge", Category.SWORD,
                r -> r.spriteAt(Sprites.FROST, FROST, Sprites.FROST_GRIP[0], Sprites.FROST_GRIP[1], 1.2f)));
        list.add(new Cosmetic("sword:katana", "Void Katana", Category.SWORD,
                r -> r.spriteAt(Sprites.KATANA, KATANA, Sprites.KATANA_GRIP[0], Sprites.KATANA_GRIP[1], 1.2f)));
        list.add(new Cosmetic("sword:partisan", "Partisan", Category.SWORD,
                r -> r.spriteAt(Sprites.PARTISAN, PARTISAN, Sprites.PARTISAN_GRIP[0], Sprites.PARTISAN_GRIP[1], 1.2f)));
        HolidayModels.addAll(list);
        CharacterModels.addAll(list);
        NewCharacterModels.addAll(list);
        return list;
    }

    // ==================== палитры ====================

    private static final int[] REQUIEM = pal('k', 0xFF1A1F26, 'h', 0xFF2E3640, 'm', 0xFF4A5560,
            'T', glow(0x2CF5C8), 't', 0xFF0E8C78, 'g', glow(0x9CFFF0));
    private static final int[] PHOENIX = pal('O', glow(0xFF8C1E), 'Y', glow(0xFFE65A), 'R', 0xFFDC3214,
            'g', 0xFFE8B54A, 'b', 0xFF3C1E14, 'k', 0xFF24120C);
    private static final int[] PHARAOH = pal('G', 0xFFF0BE3C, 'd', 0xFF96641A, 'y', 0xFFDCAA28,
            'B', glow(0x2878FF), 'b', 0xFF2A3A8A);
    private static final int[] FROST = pal('I', 0xFF96DCFF, 'i', 0xFF4696DC, 'W', glow(0xF0FAFF),
            'C', glow(0x5AC8FF), 'n', 0xFF1E325A);
    private static final int[] KATANA = pal('P', glow(0x9A3CFF), 'V', 0xFF1E0A32, 'p', 0xFF5A28AA,
            'g', 0xFF8F7CB3, 'k', 0xFF15101C);
    private static final int[] PARTISAN = pal('S', 0xFFC8CDD7, 's', 0xFF787D8C, 'W', 0xFFF5F8FF,
            'y', 0xFFDCAA28, 'R', glow(0xFF3B3B), 'r', 0xFF8C1414, 'w', 0xFF5C3A22, 'k', 0xFF2A1A10);

    private static final int[] ANGEL = pal('w', 0xFFF2F4F8, 'l', 0xFFC9CED8, 'W', glow(0xFFFFFF));
    private static final int[] DEMON = pal('m', 0xE85A1A24, 'b', 0xFF1E0E12, 'r', glow(0xFF3030));
    private static final int[] BUTTERFLY = pal('a', 0xB050C8FF, 'E', glow(0xD38BFF));
    private static final int[] PHOENIX_WING = pal('Y', glow(0xFFE066), 'O', glow(0xFF8A1F),
            'R', 0xFFD3361A, 'W', glow(0xFFF6C8));
    private static final int[] CRYSTAL_WING = pal('I', 0xC8A8E6FF, 'C', 0xC85AC8FF, 'W', glow(0xEAF8FF));

    private static final String[] SHIELD = {
            "ggggggggggg",
            "gbbbbwbbbbg",
            "gbbbbwbbbbg",
            "gbbbbwbbbbg",
            "gwwwwwwwwwg",
            "gbbbbwbbbbg",
            "gbbbbwbbbbg",
            "gbbbbwbbbbg",
            ".gbbbwbbbg.",
            ".gbbbwbbbg.",
            "..gbbwbbg..",
            "...gbwbg...",
            "....ggg....",
    };
    private static final int[] SHIELD_PAL = pal('g', 0xFFE2B33C, 'b', 0xFF2B4FA8, 'w', 0xFFF1F3F8);

    private static final String[] N_LOGO = {
            "XX..XX",
            "XXX.XX",
            "XXXXXX",
            "XX.XXX",
            "XX..XX",
    };

    // ==================== плащи ====================

    interface CapePattern {
        int color(int x, int y, float t);
    }

    private static int hash(int x, int y) {
        int h = x * 374761393 + y * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return (h ^ (h >>> 16)) & 0x7FFFFFFF;
    }

    // объёмный плащ 10x16x1, гнётся на 4 части и развевается от скорости
    static void cape(Draw r, CapePattern pattern) {
        CosmeticData d = r.d;
        float swing = 6f + Math.min(1f, d.speed * 4f) * 55f + (d.sneaking ? 14f : 0f) + (d.flying ? 25f : 0f);
        float wave = (float) Math.sin(r.t * 0.09f) * 2.5f * (0.4f + Math.min(1f, d.speed * 4f));
        r.push();
        r.move(0, 0, 0.15f);
        for (int seg = 0; seg < 4; seg++) {
            r.rotX(seg == 0 ? swing * 0.55f + wave * 0.5f : swing * 0.15f + wave * (0.3f + seg * 0.25f));
            for (int row = 0; row < 4; row++) {
                int y = seg * 4 + row;
                for (int x = 0; x < 10; x++) {
                    int skip = 0;
                    if (x > 0) skip |= 1;
                    if (x < 9) skip |= 2;
                    if (row > 0) skip |= 4;
                    if (row < 3) skip |= 8;
                    r.box(x - 5, row, 0, x - 4, row + 1, 1, pattern.color(x, y, r.t), skip);
                }
            }
            r.move(0, 4, 0);
        }
        r.pop();
    }

    private static boolean edge(int x, int y) {
        return x == 0 || x == 9 || y == 15;
    }

    private static int capeNux(int x, int y, float t) {
        int accent = 0xFF000000 | ClickGuiModule.ACCENT;
        int base = mix(accent, 0xFF0B1024, 0.35f + y / 30f);
        if (edge(x, y)) {
            return glow(mix(accent, 0xFFFFFFFF, 0.25f));
        }
        int lx = x - 2;
        int ly = y - 5;
        if (lx >= 0 && lx < 6 && ly >= 0 && ly < 5 && N_LOGO[ly].charAt(lx) == 'X') {
            float p = 0.75f + 0.25f * (float) Math.sin(t * 0.15f);
            return glow(mix(0xFFFFFFFF, accent, 0.25f * p));
        }
        return base;
    }

    private static int capeGalaxy(int x, int y, float t) {
        int h = hash(x, y);
        if (h % 9 == 0) {
            float tw = 0.5f + 0.5f * (float) Math.sin(t * 0.18f + (h % 100));
            return glow(mix(0xFF6A4FC9, 0xFFFFFFFF, tw));
        }
        float neb = (float) (Math.sin(x * 0.7 + y * 0.35 + t * 0.03) * 0.5 + 0.5);
        int base = mix(0xFF1B0B3A, 0xFF070314, y / 15f);
        return mix(base, 0xFF3A1D78, neb * 0.55f);
    }

    private static int capeFlame(int x, int y, float t) {
        float f = y / 15f + 0.28f * (float) Math.sin(x * 1.7f + t * 0.25f + y * 0.6f)
                + 0.12f * (float) Math.sin(x * 3.1f - t * 0.4f);
        if (f < 0.35f) return 0xFF3A0A05;
        if (f < 0.6f) return 0xFFC2280C;
        if (f < 0.82f) return glow(0xFF7A1A);
        return glow(0xFFD84A);
    }

    private static int capeSakura(int x, int y, float t) {
        int fy = Math.floorMod((int) (y - t * 0.05f), 16);
        int h = hash(x, fy);
        if (h % 11 == 0) {
            return glow(0xFFF0F6);
        }
        if (Math.abs(x - (y * 0.55f + 1)) < 0.6f && y < 12) {
            return 0xFF4A2A22;
        }
        if (h % 11 == 1 && y < 12) {
            return 0xFFFF7FAE;
        }
        return mix(0xFFF7A8C8, 0xFFC9557F, y / 15f);
    }

    private static int capeFrost(int x, int y, float t) {
        int cx = x - 5;
        int cy = y - 7;
        boolean flake = (cx == 0 || cx == -1) && Math.abs(cy) <= 3 || cy == 0 && cx >= -4 && cx <= 3
                || Math.abs(cx + 0.5f) == Math.abs(cy) && Math.abs(cy) <= 2 && cx != 0;
        if (flake) {
            return glow(0xF4FBFF);
        }
        int h = hash(x, y);
        if (h % 13 == 0 && Math.sin(t * 0.2f + h) > 0.3) {
            return glow(0xBDEBFF);
        }
        return mix(0xFFBFE8FF, 0xFF2F6CC8, y / 15f);
    }

    private static int capeShadow(int x, int y, float t) {
        if (edge(x, y)) {
            float p = 0.6f + 0.4f * (float) Math.sin(t * 0.12f + y * 0.4f);
            return glow(mix(0xFF2A0B4F, 0xFFB45CFF, p));
        }
        int h = hash(x, y);
        return h % 17 == 0 ? 0xFF1A1426 : 0xFF0B0B10;
    }

    private static int capeRoyal(int x, int y, float t) {
        if (y <= 1) {
            return (hash(x, y) % 5 == 0) ? 0xFF15151A : 0xFFF4F1EA;
        }
        if (edge(x, y)) {
            return 0xFFF2C14E;
        }
        if (x >= 3 && x <= 6 && y >= 6 && y <= 9 && !(x == 3 || x == 6) || (y == 7 || y == 8) && x >= 3 && x <= 6) {
            return glow(0xFFD86A);
        }
        return mix(0xFFB01828, 0xFF5E0A14, y / 15f);
    }

    private static int capeRainbow(int x, int y, float t) {
        float hue = y / 16f + t * 0.01f;
        int c = Draw.hsv(hue, 0.75f, 1f);
        return edge(x, y) ? mix(c, 0xFFFFFFFF, 0.4f) : glow(c);
    }

    // ==================== крылья ====================

    // одно крыло: корень в начале координат, крыло уходит в +X
    static void wing(Draw r, String[] sprite, int[] grip, int[] palette, float scale) {
        r.push();
        r.scale(scale);
        r.spriteAt(sprite, palette, grip[0], grip[1], 1f);
        r.pop();
    }

    // ==================== на спину (z=0 — поверхность спины, +Z — наружу) ====================

    static void backpack(Draw r) {
        int body = 0xFF7A4A2A;
        int dark = 0xFF5E3720;
        r.box(-4, 1.5f, 0, 4, 11.5f, 4.5f, body);
        r.box(-4.3f, 1, -0.2f, 4.3f, 4, 4.8f, dark);                 // клапан
        r.box(-3, 6.5f, 4.5f, 3, 10.5f, 5.6f, 0xFF8E5A34);           // карман
        r.box(-0.7f, 3.4f, 4.8f, 0.7f, 4.6f, 5.2f, 0xFFE0B040);      // застёжка
        r.box(-0.6f, 7.6f, 5.6f, 0.6f, 8.6f, 5.9f, 0xFFE0B040);
        r.box(-4.6f, -0.8f, 1, 4.6f, 1.6f, 3.6f, 0xFF4F7A3A);       // спальник
        r.box(-4.8f, -0.9f, 1.8f, -4.2f, 1.7f, 2.8f, 0xFF3A2414);   // ремешки
        r.box(4.2f, -0.9f, 1.8f, 4.8f, 1.7f, 2.8f, 0xFF3A2414);
    }

    static void quiver(Draw r) {
        r.push();
        r.move(0, 6, 0.4f);
        r.rotZ(30);
        int[] fl = {0xFFE04040, 0xFFF2F2F2, 0xFF3FA0E0, 0xFFF2F2F2};
        for (int i = 0; i < 4; i++) {
            float ax = -1.2f + i * 0.8f;
            float top = -10.5f + (i % 2) * 0.8f;
            r.box(ax - 0.25f, top, 1.5f - 0.25f, ax + 0.25f, -5, 1.5f + 0.25f, 0xFFC8A070);
            r.box(ax - 0.5f, top, 1.5f - 0.1f, ax + 0.5f, top + 2.2f, 1.5f + 0.1f, fl[i]);
        }
        r.box(-2, -6, 0, 2, 6.5f, 3.4f, 0xFF6B3E1F);
        r.box(-2.15f, -5.6f, -0.15f, 2.15f, -4.6f, 3.55f, 0xFFD8A640);
        r.box(-2.15f, 4.4f, -0.15f, 2.15f, 5.4f, 3.55f, 0xFFD8A640);
        r.box(-1, -2, 3.4f, 1, 2, 3.7f, 0xFF8A5530);
        r.pop();
    }

    static void jetpack(Draw r) {
        int metal = 0xFFA8B0BC;
        r.box(-1.2f, 2, 0, 1.2f, 9, 2.2f, 0xFF4A505A);                 // рама
        for (int side = -1; side <= 1; side += 2) {
            float x1 = side < 0 ? -4 : 0.8f;
            float x2 = side < 0 ? -0.8f : 4;
            r.box(x1, 1.5f, 0.3f, x2, 10, 3.5f, metal);
            r.box(x1 - 0.1f, 0.5f, 0.2f, x2 + 0.1f, 1.8f, 3.6f, 0xFFD03030);
            r.box(x1 - 0.1f, 5, 0.2f, x2 + 0.1f, 5.8f, 3.6f, 0xFF5A616C);
            float cx = (x1 + x2) / 2;
            r.box(cx - 1.1f, 10, 0.8f, cx + 1.1f, 11.4f, 3, 0xFF3A3F48);
            // пламя
            float boost = r.d.flying ? 2.2f : Math.min(1.5f, r.d.speed * 6f);
            float len = 2.2f + boost + (float) Math.sin(r.t * 0.9f + side * 1.7f) * 0.7f;
            r.glow(true);
            r.box(cx - 0.9f, 11.4f, 1, cx + 0.9f, 11.4f + len, 2.8f, 0xC0FF7A1A);
            r.box(cx - 0.5f, 11.4f, 1.4f, cx + 0.5f, 11.4f + len * 0.6f, 2.4f, 0xE0FFE36A);
            r.glow(false);
        }
    }

    static void guitar(Draw r) {
        r.push();
        r.move(0, 6, 0.4f);
        r.rotZ(-35);
        r.box(-3.6f, 1, 0, 3.6f, 7, 2, 0xFFC0392B);         // нижняя дека
        r.box(-2.7f, -2.5f, 0, 2.7f, 1, 2, 0xFFC0392B);     // верхняя дека
        r.box(-3.7f, 0.6f, -0.05f, 3.7f, 1.4f, 2.05f, 0xFF8E2A20);
        r.box(-1.1f, -1.6f, 1.95f, 1.1f, 0.6f, 2.15f, 0xFF140C08);   // голосник
        r.box(-1.5f, 4.2f, 1.95f, 1.5f, 4.9f, 2.3f, 0xFF2A1A10);     // подставка
        r.box(-0.8f, -12, 0.5f, 0.8f, -2.5f, 1.5f, 0xFF5C3A1E);     // гриф
        for (int i = 0; i < 4; i++) {
            r.box(-0.85f, -11 + i * 2.4f, 1.45f, 0.85f, -10.7f + i * 2.4f, 1.6f, 0xFFD8D8D8);
        }
        r.box(-1.3f, -15, 0.3f, 1.3f, -12, 1.7f, 0xFF2E1E12);       // голова
        r.box(-1.7f, -14.2f, 0.7f, 1.7f, -13.6f, 1.3f, 0xFFD8D8D8);
        r.box(-1.7f, -12.9f, 0.7f, 1.7f, -12.3f, 1.3f, 0xFFD8D8D8);
        r.pop();
    }

    static void shield(Draw r) {
        r.push();
        r.move(-5.5f, -0.5f, 0.3f);
        r.sprite(SHIELD, SHIELD_PAL, 0, 1.3f);
        r.pop();
        r.box(-1, 5, 1.3f, 1, 7, 1.8f, glow(0xFFE48A));
    }

    // ==================== питомцы (начало координат — центр питомца) ====================

    static void slime(Draw r) {
        float sq = Math.max(0f, (float) Math.sin(r.t * 0.25f)) * 0.14f;
        r.push();
        r.scale(1 + sq, 1 - sq, 1 + sq);
        r.cube(0, 0.5f, 0, 4, 4, 4, 0xFF3E9E2E);
        r.box(-2.2f, -1.4f, -3.6f, -0.8f, 0, -3.4f, 0xFF10240C);
        r.box(0.8f, -1.4f, -3.6f, 2.2f, 0, -3.4f, 0xFF10240C);
        r.box(-0.6f, 1.2f, -3.6f, 0.6f, 1.8f, -3.4f, 0xFF10240C);
        r.cube(0, 0, 0, 7, 7, 7, 0x9970D850);
        r.pop();
    }

    static void ghost(Draw r) {
        r.glow(true);
        r.cube(0, -1, 0, 6, 6, 6, 0xD8E8F0FF);
        for (int i = 0; i < 3; i++) {
            float wob = (float) Math.sin(r.t * 0.2f + i * 2f) * 0.6f;
            r.box(-3 + i * 2, 2, -3, -1 + i * 2, 3.5f + wob, 3, 0xC8E8F0FF);
        }
        r.glow(false);
        r.box(-2.2f, -2.4f, -3.2f, -0.9f, -0.6f, -3.0f, 0xFF1A1C2C);
        r.box(0.9f, -2.4f, -3.2f, 2.2f, -0.6f, -3.0f, 0xFF1A1C2C);
        r.box(-2.6f, -0.4f, -3.15f, -1.6f, 0.1f, -3.0f, 0xFFFF9EC4);
        r.box(1.6f, -0.4f, -3.15f, 2.6f, 0.1f, -3.0f, 0xFFFF9EC4);
    }

    static void bee(Draw r) {
        int y = 0xFFF5C531;
        int b = 0xFF2A2116;
        int[] stripes = {b, y, y, b, y, b, y};
        for (int i = 0; i < stripes.length; i++) {
            float z = -3.5f + i;
            r.box(-2.5f, -2.5f, z, 2.5f, 2.5f, z + 1, stripes[i], i == 0 ? 32 : i == stripes.length - 1 ? 16 : 48);
        }
        r.box(-2, -1.6f, -3.6f, -0.8f, 0.2f, -3.5f, 0xFF0C0C10);
        r.box(0.8f, -1.6f, -3.6f, 2, 0.2f, -3.5f, 0xFF0C0C10);
        r.box(-1.6f, -4.5f, -3.4f, -1.2f, -2.5f, -3.0f, b);
        r.box(1.2f, -4.5f, -3.4f, 1.6f, -2.5f, -3.0f, b);
        r.box(-0.4f, -0.4f, 3.5f, 0.4f, 0.4f, 4.6f, 0xFF1A1410);
        float flap = 25 + (float) Math.sin(r.t * 2.4f) * 30;
        for (int side = -1; side <= 1; side += 2) {
            r.push();
            r.move(side * 0.8f, -2.5f, -0.5f);
            r.rotZ(side * flap);
            r.glow(true);
            r.box(side < 0 ? -4 : 0, -0.15f, -1.5f, side < 0 ? 0 : 4, 0.15f, 2, 0x90E8F6FF);
            r.glow(false);
            r.pop();
        }
    }

    static void fox(Draw r) {
        int o = 0xFFE07B2E;
        int w = 0xFFF4EDE4;
        int d = 0xFF3B2A20;
        r.box(-2, -1, -2, 2, 2.5f, 3.5f, o);
        r.box(-1.6f, 1.5f, -1.8f, 1.6f, 2.6f, 1, w);
        r.box(-2.5f, -3.5f, -5.5f, 2.5f, 0.5f, -1.5f, o);
        r.box(-1.2f, -1.4f, -7, 1.2f, 0.5f, -5.5f, w);
        r.box(-0.5f, -1.4f, -7.3f, 0.5f, -0.6f, -7, 0xFF151010);
        r.box(-2.6f, -0.8f, -5.6f, -1.3f, 0.5f, -3.5f, w);
        r.box(1.3f, -0.8f, -5.6f, 2.6f, 0.5f, -3.5f, w);
        r.box(-1.9f, -2.6f, -5.6f, -0.9f, -1.6f, -5.5f, 0xFF151010);
        r.box(0.9f, -2.6f, -5.6f, 1.9f, -1.6f, -5.5f, 0xFF151010);
        float tw = (float) Math.sin(r.t * 0.15f) * 3f;
        r.box(-2.5f, -5.5f + tw * 0.05f, -4, -1, -3.5f, -3, o);
        r.box(1, -5.5f - tw * 0.05f, -4, 2.5f, -3.5f, -3, o);
        r.box(-2.1f, -5, -4.05f, -1.4f, -3.6f, -3.95f, d);
        r.box(1.4f, -5, -4.05f, 2.1f, -3.6f, -3.95f, d);
        for (int i = 0; i < 4; i++) {
            float lx = (i % 2 == 0) ? -1.8f : 0.8f;
            float lz = i < 2 ? -1.6f : 2;
            r.box(lx, 2.5f, lz, lx + 1, 4, lz + 1, d);
        }
        r.push();
        r.move(0, 0, 3.5f);
        r.rotY((float) Math.sin(r.t * 0.3f) * 22);
        r.rotX(-20);
        r.box(-1.2f, -1, 0, 1.2f, 1.4f, 3, o);
        r.box(-1.2f, -1, 3, 1.2f, 1.4f, 4.4f, w);
        r.pop();
    }

    static void crystalPet(Draw r) {
        r.push();
        r.rotY(r.t * 3f);
        r.glow(true);
        float[] widths = {1, 2.2f, 3.4f, 4.4f, 3.4f, 2.2f, 1};
        for (int i = 0; i < widths.length; i++) {
            float wv = widths[i];
            int c = i % 2 == 0 ? 0xF05CE1FF : 0xF0A8F0FF;
            r.box(-wv / 2, -4.2f + i * 1.2f, -wv / 2, wv / 2, -3 + i * 1.2f, wv / 2, c);
        }
        r.pop();
        for (int i = 0; i < 3; i++) {
            double a = r.t * 0.08 + i * Math.PI * 2 / 3;
            float x = (float) Math.cos(a) * 5;
            float z = (float) Math.sin(a) * 5;
            float y = (float) Math.sin(r.t * 0.1f + i) * 1.5f;
            r.cube(x, y, z, 1, 1, 1, 0xF0D9F6FF);
        }
        r.glow(false);
    }

    // ==================== шапки (y=0 — макушка, вверх = -Y) ====================

    static void crown(Draw r) {
        int gold = 0xFFF2C230;
        int dark = 0xFFC08A18;
        r.box(-4.6f, -2.6f, -4.6f, 4.6f, 0.4f, -3.6f, gold);
        r.box(-4.6f, -2.6f, 3.6f, 4.6f, 0.4f, 4.6f, gold);
        r.box(-4.6f, -2.6f, -3.6f, -3.6f, 0.4f, 3.6f, gold);
        r.box(3.6f, -2.6f, -3.6f, 4.6f, 0.4f, 3.6f, gold);
        r.box(-4.7f, -0.6f, -4.7f, 4.7f, 0.5f, 4.7f, dark, 4);
        for (int i = -1; i <= 1; i++) {
            float p = i * 3.1f;
            float hgt = i == 0 ? 2.6f : 1.8f;
            r.box(p - 0.6f, -2.6f - hgt, -4.6f, p + 0.6f, -2.6f, -3.6f, gold);
            r.box(p - 0.6f, -2.6f - hgt, 3.6f, p + 0.6f, -2.6f, 4.6f, gold);
            r.box(-4.6f, -2.6f - hgt, p - 0.6f, -3.6f, -2.6f, p + 0.6f, gold);
            r.box(3.6f, -2.6f - hgt, p - 0.6f, 4.6f, -2.6f, p + 0.6f, gold);
        }
        r.box(-0.8f, -2, -4.9f, 0.8f, -0.4f, -4.5f, glow(0xFF3355));
        r.box(-4.9f, -2, -0.8f, -4.5f, -0.4f, 0.8f, glow(0x3D8BFF));
        r.box(4.5f, -2, -0.8f, 4.9f, -0.4f, 0.8f, glow(0x3DFF8B));
        r.box(-0.8f, -2, 4.5f, 0.8f, -0.4f, 4.9f, glow(0xB45CFF));
    }

    static void halo(Draw r) {
        r.push();
        r.move(0, -3.5f + (float) Math.sin(r.t * 0.08f) * 0.6f, 0);
        r.rotX(-8);
        r.rotY(r.t * 1.5f);
        r.glow(true);
        int seg = 22;
        for (int i = 0; i < seg; i++) {
            double a = i * Math.PI * 2 / seg;
            float x = (float) Math.cos(a) * 4.4f;
            float z = (float) Math.sin(a) * 4.4f;
            r.cube(x, 0, z, 1.4f, 0.7f, 1.4f, 0xFFFFF2A6);
        }
        for (int i = 0; i < seg; i++) {
            double a = (i + 0.5) * Math.PI * 2 / seg;
            float x = (float) Math.cos(a) * 4.4f;
            float z = (float) Math.sin(a) * 4.4f;
            r.cube(x, 0, z, 2.3f, 1.2f, 2.3f, 0x40FFE27A);
        }
        r.glow(false);
        r.pop();
    }

    static void wizard(Draw r) {
        int purple = 0xFF5B35A6;
        int dark = 0xFF3F2378;
        r.box(-6.2f, -0.6f, -6.2f, 6.2f, 0.4f, 6.2f, dark);
        r.box(-4.3f, -2, -4.3f, 4.3f, -0.6f, 4.3f, 0xFFE2B33C);
        float z = 0;
        float y = -2;
        for (int i = 0; i < 6; i++) {
            float wv = 8.2f - i * 1.25f;
            float h = 2;
            z += i * 0.25f;
            r.box(-wv / 2, y - h, z - wv / 2, wv / 2, y, z + wv / 2, i % 2 == 0 ? purple : dark);
            y -= h;
        }
        r.box(-0.6f, y - 1.2f, z + 1, 0.6f, y, z + 2.2f, purple);
        r.cube(0, y - 2, z + 2.4f, 1.2f, 1.2f, 1.2f, glow(0xFFE066));
        r.cube(-1.6f, -4.5f, -3.95f, 0.8f, 0.8f, 0.2f, glow(0xFFE066));
        r.cube(1.4f, -7.5f, -2.9f, 0.6f, 0.6f, 0.2f, glow(0xFFE066));
        r.cube(0.3f, -10, -1.8f, 0.5f, 0.5f, 0.2f, glow(0xFFE066));
    }

    static void catEars(Draw r) {
        float tw = (float) Math.sin(r.t * 0.07f) * 4;
        for (int side = -1; side <= 1; side += 2) {
            r.push();
            r.move(side * 2.6f, 0, -1);
            r.rotZ(side * (12 + (side > 0 ? tw : 0)));
            int out = 0xFF2B2B33;
            r.box(-1.6f, -1.2f, -0.6f, 1.6f, 0.2f, 0.6f, out);
            r.box(-1.1f, -2.4f, -0.6f, 1.1f, -1.2f, 0.6f, out);
            r.box(-0.5f, -3.5f, -0.6f, 0.5f, -2.4f, 0.6f, out);
            r.box(-0.9f, -1.2f, -0.75f, 0.9f, 0, -0.6f, 0xFFFF9EC4);
            r.box(-0.4f, -2.2f, -0.75f, 0.4f, -1.2f, -0.6f, 0xFFFF9EC4);
            r.pop();
        }
    }

    static void topHat(Draw r) {
        r.push();
        r.rotZ(-6);
        r.box(-5.6f, -0.6f, -5.6f, 5.6f, 0.4f, 5.6f, 0xFF1A1A1F);
        r.box(-3.7f, -2.2f, -3.7f, 3.7f, -0.6f, 3.7f, 0xFFB3202B);
        r.box(-3.6f, -8.5f, -3.6f, 3.6f, -2.2f, 3.6f, 0xFF202027);
        r.box(-3.8f, -8.9f, -3.8f, 3.8f, -8.4f, 3.8f, 0xFF2A2A33);
        r.pop();
    }

    static void horns(Draw r) {
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < 6; i++) {
                float s = 1.9f - i * 0.25f;
                float x = side * (2.6f + i * 0.45f + i * i * 0.04f);
                float y = 0.3f - i * 1.05f;
                float z = -1.5f + i * 0.35f;
                int c = mix(0xFFB01C24, 0xFF1A0608, i / 5f);
                r.cube(x, y, z, s, 1.25f, s, i == 5 ? glow(0xFF4040) : c);
            }
        }
    }

    // ==================== манекен для меню ====================

    static void mannequin(Draw r) {
        int head = 0xFF5A6278;
        int body = 0xFF424A5E;
        int limb = 0xFF363D4E;
        r.box(-4, -8, -4, 4, 0, 4, head);
        r.box(-2.6f, -4.6f, -4.1f, -1, -3.4f, -4, glow(ClickGuiModule.ACCENT));
        r.box(1, -4.6f, -4.1f, 2.6f, -3.4f, -4, glow(ClickGuiModule.ACCENT));
        r.box(-4, 0, -2, 4, 12, 2, body);
        r.box(-8, 0, -2, -4, 12, 2, limb);
        r.box(4, 0, -2, 8, 12, 2, limb);
        r.box(-4, 12, -2, 0, 24, 2, alpha(mix(limb, 0xFF000000, 0.1f), 255));
        r.box(0, 12, -2, 4, 24, 2, alpha(mix(limb, 0xFF000000, 0.1f), 255));
    }

    private CosmeticModels() {
    }
}
