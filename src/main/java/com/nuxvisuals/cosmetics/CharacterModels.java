package com.nuxvisuals.cosmetics;

import java.util.List;

import static com.nuxvisuals.cosmetics.Draw.glow;
import static com.nuxvisuals.cosmetics.Draw.pal;

// 3D-модели «вместо скина». Каждая часть рисуется в координатах своей части тела:
// голова: x -4..4, y -8..0 (вверх = -Y), z -4..4 (лицо смотрит в -Z)
// тело:   x -4..4, y 0..12, z -2..2
// руки:   правая x -3..1, левая x -1..3, y -2..10;  ноги: x -2..2, y 0..12
final class CharacterModels {

    static void addAll(List<Cosmetic> list) {
        list.add(new Cosmetic("model:peter", "Питер Гриффин", Category.MODEL, CharacterModels::peter));
        list.add(new Cosmetic("model:miku", "Мику", Category.MODEL, CharacterModels::miku, "Аниме"));
        list.add(new Cosmetic("model:neko", "Неко", Category.MODEL, CharacterModels::neko, "Аниме"));
        list.add(new Cosmetic("model:ninja", "Ниндзя", Category.MODEL, CharacterModels::ninja, "Аниме"));
        list.add(new Cosmetic("model:pumpkin", "Тыквоголовый", Category.MODEL, CharacterModels::pumpkinMan, "Хеллоуин"));
        list.add(new Cosmetic("model:skeleton", "Скелет", Category.MODEL, CharacterModels::skeleton, "Хеллоуин"));
        list.add(new Cosmetic("model:snowman", "Снеговик", Category.MODEL, CharacterModels::snowman, "Зима"));
        list.add(new Cosmetic("model:santa", "Санта", Category.MODEL, CharacterModels::santa, "Зима"));
    }

    // лицо 8x8 на передней стороне головы (z — передняя грань головы)
    private static void face(Draw r, String[] rows, int[] palette, float z) {
        r.push();
        r.move(-4, -8, z - 0.25f);
        r.sprite(rows, palette, 0, 0.25f);
        r.pop();
    }

    private static boolean isArm(Draw r) {
        return r.part == Rig.Anchor.RIGHT_ARM || r.part == Rig.Anchor.LEFT_ARM;
    }

    // x-координаты руки (правая/левая) с учётом ширины
    private static float armX1(Draw r, float w) {
        return r.part == Rig.Anchor.RIGHT_ARM ? 1 - w : -1;
    }

    private static float armX2(Draw r, float w) {
        return r.part == Rig.Anchor.RIGHT_ARM ? 1 : -1 + w;
    }

    // ==================== Питер Гриффин ====================

    private static final String[] PETER_FACE = {
            "........",
            "........",
            ".gggggg.",
            "gwwggwwg",
            "gwkggkwg",
            ".gg..gg.",
            "...nn...",
            "........",
    };
    private static final int[] PETER_PAL = pal('g', 0xFF2A2A2A, 'w', 0xFFFFFFFF, 'k', 0xFF111111, 'n', 0xFFE0A884);

    static void peter(Draw r) {
        int skin = 0xFFF3C9A0;
        int shirt = 0xFFF2F2F2;
        int pants = 0xFF3E7A3A;
        switch (r.part) {
            case HEAD -> {
                r.box(-4.5f, -8.5f, -4.5f, 4.5f, 0, 4.5f, skin);
                r.box(-4.7f, -9.6f, -4.2f, 4.7f, -7.4f, 4.7f, 0xFF5A3A1E);     // волосы
                r.box(-4.7f, -7.4f, 2.5f, 4.7f, -3.5f, 4.7f, 0xFF5A3A1E);
                r.box(-3.6f, -2.4f, -5.8f, 3.6f, 0.6f, -4.4f, skin);            // большой подбородок
                r.box(-0.25f, -1.6f, -5.9f, 0.25f, 0.4f, -5.8f, 0xFFD9A57C);
                r.box(-1.6f, -3.0f, -4.75f, 1.6f, -2.6f, -4.5f, 0xFFB05A4A);    // рот
                face(r, PETER_FACE, PETER_PAL, -4.5f);
                r.box(-0.7f, -4.4f, -5.5f, 0.7f, -3.2f, -4.5f, 0xFFE8B48E);     // нос
            }
            case BODY -> {
                r.box(-5, 0, -3, 5, 12, 3, shirt);
                r.box(-4.4f, 4, -4.2f, 4.4f, 11.5f, -3, shirt);                 // живот
                r.box(-5.1f, 10.5f, -4.3f, 5.1f, 12, 3.1f, 0xFF3A3A3A);          // ремень
                r.box(-0.8f, 10.6f, -4.4f, 0.8f, 11.8f, -4.3f, 0xFFD8B04A);
                r.box(-1.5f, 0, -3.15f, 1.5f, 1.2f, -3, 0xFFE0E0E0);             // воротник
            }
            case RIGHT_ARM, LEFT_ARM -> {
                float x1 = r.part == Rig.Anchor.RIGHT_ARM ? -4 : 0;
                float x2 = x1 + 4;
                r.box(x1, -2, -2.2f, x2, 4, 2.2f, shirt);
                r.box(x1 + 0.2f, 4, -2, x2 - 0.2f, 10, 2, skin);
            }
            case RIGHT_LEG, LEFT_LEG -> {
                r.box(-2.2f, 0, -2.4f, 2.2f, 10.5f, 2.4f, pants);
                r.box(-2.2f, 10.5f, -3.4f, 2.2f, 12, 2.4f, 0xFF1C1C1C);
            }
        }
    }

    // ==================== Мику ====================

    private static final String[] MIKU_FACE = {
            "........",
            "........",
            "........",
            ".kk..kk.",
            ".tw..tw.",
            ".tt..tt.",
            "........",
            "...pp...",
    };
    private static final int[] MIKU_PAL = pal('k', 0xFF1A2A30, 't', 0xFF2BB5AB, 'w', 0xFFFFFFFF, 'p', 0xFFE07A8C);

    static void miku(Draw r) {
        int skin = 0xFFFBE1D2;
        int hair = 0xFF39C5BB;
        int dark = 0xFF2A9A92;
        switch (r.part) {
            case HEAD -> {
                r.box(-4, -8, -4, 4, 0, 4, skin);
                face(r, MIKU_FACE, MIKU_PAL, -4);
                r.box(-4.4f, -8.6f, -4.4f, 4.4f, -6.6f, 4.4f, hair);           // макушка
                r.box(-4.4f, -6.6f, -4.6f, 4.4f, -5.6f, -4.0f, hair);           // чёлка
                r.box(-1, -5.6f, -4.6f, 1, -4.9f, -4.0f, hair);
                r.box(-4.4f, -6.6f, 2.5f, 4.4f, 1.5f, 4.4f, hair);              // волосы сзади
                r.box(-4.5f, -6.6f, -4.2f, -3.9f, -1, 2.5f, hair);              // по бокам
                r.box(3.9f, -6.6f, -4.2f, 4.5f, -1, 2.5f, hair);
                // два длинных хвостика
                float sway = (float) Math.sin(r.t * 0.12f) * 4f;
                for (int side = -1; side <= 1; side += 2) {
                    r.push();
                    r.move(side * 4.6f, -7, 1.5f);
                    r.box(-1.2f, -0.8f, -1.2f, 1.2f, 0.8f, 1.2f, 0xFFE0245E);   // резинка
                    r.rotZ(side * -8);
                    r.rotX(8 + sway);
                    r.box(side < 0 ? -3.4f : 0.4f, 0, -1.4f, side < 0 ? -0.4f : 3.4f, 12, 1.6f, hair);
                    r.box(side < 0 ? -3.0f : 0.8f, 12, -1.0f, side < 0 ? -0.8f : 3.0f, 18, 1.2f, dark);
                    r.pop();
                }
            }
            case BODY -> {
                r.box(-4, 0, -2, 4, 7, 2, 0xFFB9C3C8);                           // кофта
                r.box(-0.8f, 0.5f, -2.25f, 0.8f, 5, -2, hair);                   // галстук
                r.box(-4.7f, 7, -2.7f, 4.7f, 12, 2.7f, 0xFF1E1E26);              // юбка
                r.box(-4.8f, 7, -2.8f, 4.8f, 7.6f, 2.8f, hair);
            }
            case RIGHT_ARM, LEFT_ARM -> {
                float x1 = armX1(r, 3);
                float x2 = armX2(r, 3);
                r.box(x1, -2, -1.5f, x2, 3, 1.5f, skin);
                r.box(x1 - 0.4f, 3, -1.9f, x2 + 0.4f, 10, 1.9f, 0xFF1E1E26);    // рукава
                r.box(x1 - 0.45f, 3, -1.95f, x2 + 0.45f, 3.8f, 1.95f, hair);
            }
            case RIGHT_LEG, LEFT_LEG -> {
                r.box(-1.8f, 0, -1.8f, 1.8f, 4, 1.8f, skin);
                r.box(-2.0f, 4, -2.0f, 2.0f, 12, 2.0f, 0xFF1E1E26);              // сапоги
                r.box(-2.05f, 4, -2.05f, 2.05f, 4.8f, 2.05f, hair);
            }
        }
    }

    // ==================== Неко (кошкодевочка-горничная) ====================

    private static final String[] NEKO_FACE = {
            "........",
            "........",
            "........",
            ".kk..kk.",
            ".mw..mw.",
            ".mm..mm.",
            "...ww...",
            "..p..p..",
    };
    private static final int[] NEKO_PAL = pal('k', 0xFF2A1A2A, 'm', 0xFFB04ACF, 'w', 0xFFFFFFFF, 'p', 0xFFFF8FB0);

    static void neko(Draw r) {
        int skin = 0xFFFDE6DA;
        int hair = 0xFFF7A1C4;
        int dress = 0xFF22202A;
        switch (r.part) {
            case HEAD -> {
                r.box(-4, -8, -4, 4, 0, 4, skin);
                face(r, NEKO_FACE, NEKO_PAL, -4);
                r.box(-4.4f, -8.6f, -4.4f, 4.4f, -6.6f, 4.4f, hair);
                r.box(-4.4f, -6.6f, -4.6f, 4.4f, -5.7f, -4.0f, hair);
                r.box(-4.4f, -6.6f, 2.4f, 4.4f, 2.5f, 4.4f, hair);
                r.box(-4.5f, -6.6f, -4.2f, -3.9f, 0.5f, 2.4f, hair);
                r.box(3.9f, -6.6f, -4.2f, 4.5f, 0.5f, 2.4f, hair);
                r.box(-3, -8.9f, -4.5f, 3, -8.3f, -2.5f, 0xFFFFFFFF);            // ободок горничной
                float tw = (float) Math.sin(r.t * 0.09f) * 5;
                for (int side = -1; side <= 1; side += 2) {
                    r.push();
                    r.move(side * 2.8f, -8.4f, -0.5f);
                    r.rotZ(side * (14 + (side > 0 ? tw : 0)));
                    r.box(-1.7f, -1.3f, -0.6f, 1.7f, 0.1f, 0.6f, hair);
                    r.box(-1.1f, -2.6f, -0.6f, 1.1f, -1.3f, 0.6f, hair);
                    r.box(-0.5f, -3.7f, -0.6f, 0.5f, -2.6f, 0.6f, hair);
                    r.box(-0.9f, -1.3f, -0.75f, 0.9f, 0, -0.6f, 0xFFFFD6E6);
                    r.pop();
                }
            }
            case BODY -> {
                r.box(-4, 0, -2, 4, 8, 2, dress);
                r.box(-3, 1, -2.2f, 3, 8, -2, 0xFFF4F4F8);                       // фартук
                r.box(-1, 0, -2.3f, 1, 1.2f, -2, 0xFFE0245E);                    // бантик
                r.box(-5, 8, -3, 5, 12, 3, dress);                               // пышная юбка
                r.box(-3.4f, 8, -3.2f, 3.4f, 11.4f, -3, 0xFFF4F4F8);
                r.box(-5.1f, 11.4f, -3.1f, 5.1f, 12, 3.1f, 0xFFF4F4F8);
                // хвост
                r.push();
                r.move(0, 9, 3);
                r.rotX(-35 + (float) Math.sin(r.t * 0.1f) * 10);
                r.rotZ((float) Math.sin(r.t * 0.07f) * 18);
                r.box(-0.7f, -0.7f, 0, 0.7f, 0.7f, 6, hair);
                r.rotX(-40);
                r.box(-0.7f, -0.7f, 5.5f, 0.7f, 0.7f, 9, hair);
                r.box(-0.8f, -0.8f, 9, 0.8f, 0.8f, 10.5f, 0xFFFFFFFF);
                r.pop();
            }
            case RIGHT_ARM, LEFT_ARM -> {
                float x1 = armX1(r, 3);
                float x2 = armX2(r, 3);
                r.box(x1, -2, -1.5f, x2, 4, 1.5f, dress);
                r.box(x1 - 0.2f, 1, -1.7f, x2 + 0.2f, 2, 1.7f, 0xFFF4F4F8);
                r.box(x1, 4, -1.5f, x2, 10, 1.5f, skin);
                r.box(x1 - 0.1f, 8.5f, -1.6f, x2 + 0.1f, 9.3f, 1.6f, 0xFFF4F4F8);
            }
            case RIGHT_LEG, LEFT_LEG -> {
                r.box(-1.8f, 0, -1.8f, 1.8f, 10.5f, 1.8f, 0xFFF4F4F8);           // чулки
                r.box(-1.9f, 2, -1.9f, 1.9f, 2.7f, 1.9f, 0xFFF7A1C4);
                r.box(-2, 10.5f, -2.6f, 2, 12, 2, 0xFF1A1A1A);
            }
        }
    }

    // ==================== Ниндзя ====================

    private static final String[] NINJA_FACE = {
            "........",
            "........",
            "........",
            "........",
            ".ww..ww.",
            ".wk..kw.",
            "........",
            "........",
    };
    private static final int[] NINJA_PAL = pal('w', 0xFFFFFFFF, 'k', 0xFF111111);

    static void ninja(Draw r) {
        int suit = 0xFF1C1E26;
        int red = 0xFFD32F2F;
        switch (r.part) {
            case HEAD -> {
                r.box(-4, -8, -4, 4, 0, 4, suit);
                r.box(-4.05f, -4.6f, -4.1f, 4.05f, -2.6f, -4.0f, 0xFFF0C9A8);   // полоска кожи у глаз
                face(r, NINJA_FACE, NINJA_PAL, -4.1f);
                r.box(-4.2f, -6.4f, -4.2f, 4.2f, -5.4f, 4.2f, red);              // повязка
                float wave = (float) Math.sin(r.t * 0.2f) * 10;
                r.push();
                r.move(0, -5.9f, 4.2f);
                r.rotX(30 + wave);
                r.box(-1.6f, -0.5f, 0, -0.4f, 0.5f, 6, red);
                r.box(0.4f, -0.5f, 0, 1.6f, 0.5f, 5, red);
                r.pop();
            }
            case BODY -> {
                r.box(-4, 0, -2, 4, 12, 2, suit);
                r.box(-4.1f, 7.5f, -2.1f, 4.1f, 8.8f, 2.1f, red);                // пояс
                r.push();                                                        // катана за спиной
                r.move(0, 6, 2.6f);
                r.rotZ(40);
                r.box(-0.5f, -9, -0.3f, 0.5f, 4, 0.3f, 0xFFD0D6E0);
                r.box(-1.5f, 4, -0.6f, 1.5f, 4.8f, 0.6f, 0xFF8C7A3A);
                r.box(-0.6f, 4.8f, -0.5f, 0.6f, 9, 0.5f, red);
                r.pop();
            }
            case RIGHT_ARM, LEFT_ARM -> {
                float x1 = armX1(r, 4);
                float x2 = armX2(r, 4);
                r.box(x1, -2, -2, x2, 10, 2, suit);
                r.box(x1 - 0.1f, 6, -2.1f, x2 + 0.1f, 9, 2.1f, 0xFF2E313C);
            }
            case RIGHT_LEG, LEFT_LEG -> {
                r.box(-2, 0, -2, 2, 12, 2, suit);
                r.box(-2.1f, 8, -2.1f, 2.1f, 11, 2.1f, 0xFF2E313C);
            }
        }
    }

    // ==================== Тыквоголовый (Хеллоуин) ====================

    private static final String[] PUMPKIN_FACE = {
            "........",
            "........",
            ".yy..yy.",
            ".yyy.yy.",
            "...yy...",
            ".yyyyyy.",
            "..y.y.y.",
            "........",
    };
    private static final int[] PUMPKIN_PAL = pal('y', glow(0xFFD23A));

    static void pumpkinMan(Draw r) {
        int suit = 0xFF1A1420;
        switch (r.part) {
            case HEAD -> {
                pumpkinHead(r, 0);
            }
            case BODY -> {
                r.box(-4, 0, -2, 4, 12, 2, suit);
                r.box(-1.2f, 0, -2.1f, 1.2f, 12, -2, 0xFF5B2A86);
                r.box(-4.1f, 0, -2.2f, -2, 3, 2.2f, 0xFF2A1E36);
                r.box(2, 0, -2.2f, 4.1f, 3, 2.2f, 0xFF2A1E36);
            }
            case RIGHT_ARM, LEFT_ARM -> {
                float x1 = armX1(r, 4);
                float x2 = armX2(r, 4);
                r.box(x1, -2, -2, x2, 8, 2, suit);
                r.box(x1 + 0.2f, 8, -1.8f, x2 - 0.2f, 10, 1.8f, 0xFFF0F0F0);
            }
            case RIGHT_LEG, LEFT_LEG -> {
                r.box(-2, 0, -2, 2, 11, 2, 0xFF2A2430);
                r.box(-2.1f, 11, -2.8f, 2.1f, 12, 2.1f, 0xFF111111);
            }
        }
    }

    // тыква на голове (используется и шапкой)
    static void pumpkinHead(Draw r, float y) {
        int o = 0xFFE8761C;
        int d = 0xFFB8530E;
        r.push();
        r.move(0, y, 0);
        r.box(-4.8f, -8.8f, -4.8f, 4.8f, 0.4f, 4.8f, o);
        r.box(-2.2f, -8.9f, -4.9f, -1.6f, 0.3f, 4.9f, d);
        r.box(1.6f, -8.9f, -4.9f, 2.2f, 0.3f, 4.9f, d);
        r.box(-4.9f, -8.9f, -2.2f, 4.9f, 0.3f, -1.6f, d);
        r.box(-4.9f, -8.9f, 1.6f, 4.9f, 0.3f, 2.2f, d);
        r.box(-0.7f, -10.6f, -0.7f, 0.7f, -8.8f, 0.7f, 0xFF3F7A2A);
        r.box(-0.7f, -10.6f, -0.7f, 2.2f, -9.9f, 0.7f, 0xFF3F7A2A);
        r.push();
        r.move(-4, -8.5f, -4.8f - 0.25f);
        r.scale(1f, 1.05f, 1f);
        r.sprite(PUMPKIN_FACE, PUMPKIN_PAL, 0, 0.3f);
        r.pop();
        r.pop();
    }

    // ==================== Скелет (Хеллоуин) ====================

    private static final String[] SKULL_FACE = {
            "........",
            "........",
            "........",
            ".kk..kk.",
            ".kr..rk.",
            "...kk...",
            "..wkwkw.",
            "........",
    };
    private static final int[] SKULL_PAL = pal('k', 0xFF151515, 'r', glow(0xFF3030), 'w', 0xFFE8E6DC);

    static void skeleton(Draw r) {
        int bone = 0xFFE8E6DC;
        int dark = 0xFFB8B4A6;
        switch (r.part) {
            case HEAD -> {
                r.box(-4, -8, -4, 4, 0, 4, bone);
                face(r, SKULL_FACE, SKULL_PAL, -4);
            }
            case BODY -> {
                r.box(-0.8f, 0, -0.8f, 0.8f, 12, 0.8f, dark);                   // позвоночник
                for (int i = 0; i < 4; i++) {
                    float y = 1.5f + i * 1.8f;
                    r.box(-3.6f, y, -1.6f, 3.6f, y + 0.9f, 1.6f, bone);         // рёбра
                }
                r.box(-3.4f, 10, -1.4f, 3.4f, 12, 1.4f, bone);                   // таз
            }
            case RIGHT_ARM, LEFT_ARM -> {
                float cx = r.part == Rig.Anchor.RIGHT_ARM ? -1 : 1;
                r.box(cx - 1.6f, -2, -1.6f, cx + 1.6f, 0, 1.6f, bone);
                r.box(cx - 0.8f, 0, -0.8f, cx + 0.8f, 10, 0.8f, bone);
                r.box(cx - 1.0f, 4, -1.0f, cx + 1.0f, 4.8f, 1.0f, dark);
            }
            case RIGHT_LEG, LEFT_LEG -> {
                r.box(-0.8f, 0, -0.8f, 0.8f, 12, 0.8f, bone);
                r.box(-1.0f, 5.6f, -1.0f, 1.0f, 6.4f, 1.0f, dark);
                r.box(-1.2f, 11, -2.4f, 1.2f, 12, 1, bone);
            }
        }
    }

    // ==================== Снеговик (Зима) ====================

    private static final String[] SNOW_FACE = {
            "........",
            "........",
            "........",
            ".kk..kk.",
            ".kk..kk.",
            "........",
            ".k....k.",
            "..kkkk..",
    };
    private static final int[] SNOW_PAL = pal('k', 0xFF1A1A1A);

    static void snowman(Draw r) {
        int snow = 0xFFF4F8FF;
        int shade = 0xFFDCE6F2;
        switch (r.part) {
            case HEAD -> {
                r.box(-4, -8, -4, 4, 0, 4, snow);
                face(r, SNOW_FACE, SNOW_PAL, -4);
                r.box(-0.6f, -4, -7, 0.6f, -3, -4, 0xFFF08A24);                  // морковка
                r.box(-5.5f, -8.6f, -5.5f, 5.5f, -7.8f, 5.5f, 0xFF1A1A1F);       // цилиндр
                r.box(-3.5f, -13, -3.5f, 3.5f, -8.6f, 3.5f, 0xFF1A1A1F);
                r.box(-3.6f, -9.8f, -3.6f, 3.6f, -8.6f, 3.6f, 0xFFC62828);
            }
            case BODY -> {
                r.box(-5, 0, -3.5f, 5, 12, 3.5f, snow);
                r.box(-5.6f, 4, -4, 5.6f, 11, 4, shade);
                r.box(-4.6f, -0.4f, -3.0f, 4.6f, 1.4f, 3.0f, 0xFFC62828);       // шарф
                r.box(1.8f, 1.4f, -3.2f, 3.4f, 7, -2.8f, 0xFFC62828);
                for (int i = 0; i < 3; i++) {
                    r.box(-0.6f, 3 + i * 2.5f, -4.2f, 0.6f, 4.2f + i * 2.5f, -3.9f, 0xFF1A1A1A);
                }
            }
            case RIGHT_ARM, LEFT_ARM -> {
                float cx = r.part == Rig.Anchor.RIGHT_ARM ? -1 : 1;
                int wood = 0xFF6B4423;
                r.box(cx - 0.5f, -1, -0.5f, cx + 0.5f, 10, 0.5f, wood);         // руки-ветки
                r.box(cx - 0.4f, 6, -0.4f, cx + (cx < 0 ? -2.4f : 2.4f), 6.8f, 0.4f, wood);
            }
            case RIGHT_LEG, LEFT_LEG -> {
                r.box(-2.6f, 0, -3.4f, 2.6f, 12, 3.4f, snow);
                r.box(-2.7f, 3, -3.6f, 2.7f, 9, 3.6f, shade);
            }
        }
    }

    // ==================== Санта (Зима) ====================

    private static final String[] SANTA_FACE = {
            "........",
            "........",
            "........",
            ".kk..kk.",
            "........",
            "..pppp..",
            ".wwwwww.",
            "wwwwwwww",
    };
    private static final int[] SANTA_PAL = pal('k', 0xFF1A1A1A, 'p', 0xFFF5B8A0, 'w', 0xFFF6F6F6);

    static void santa(Draw r) {
        int red = 0xFFC62828;
        int white = 0xFFF6F6F6;
        int skin = 0xFFF8D2B8;
        switch (r.part) {
            case HEAD -> {
                r.box(-4, -8, -4, 4, 0, 4, skin);
                face(r, SANTA_FACE, SANTA_PAL, -4);
                r.box(-4.2f, -2.5f, -4.6f, 4.2f, 2.5f, -1, white);               // борода
                r.box(-3, 2.5f, -4.4f, 3, 4, -2, white);
                r.box(-4.5f, -8.8f, -4.5f, 4.5f, -7.2f, 4.5f, white);            // колпак
                r.box(-4, -11, -4, 4, -8.8f, 4, red);
                r.box(-3, -12.6f, -2, 3, -11, 4, red);
                r.box(-2, -13.6f, 1, 2, -12.6f, 4.5f, red);
                r.box(-1.2f, -14.2f, 4, 1.2f, -12, 6.2f, white);
            }
            case BODY -> {
                r.box(-4.6f, 0, -3, 4.6f, 12, 3, red);
                r.box(-1, 0, -3.1f, 1, 12, -3, white);
                r.box(-4.7f, 7, -3.2f, 4.7f, 8.5f, 3.2f, 0xFF1A1A1A);            // ремень
                r.box(-1.2f, 6.8f, -3.3f, 1.2f, 8.7f, -3.2f, 0xFFE2B33C);
            }
            case RIGHT_ARM, LEFT_ARM -> {
                float x1 = armX1(r, 4);
                float x2 = armX2(r, 4);
                r.box(x1, -2, -2, x2, 7.5f, 2, red);
                r.box(x1 - 0.2f, 7.5f, -2.2f, x2 + 0.2f, 8.6f, 2.2f, white);
                r.box(x1 + 0.2f, 8.6f, -1.8f, x2 - 0.2f, 10, 1.8f, 0xFF1A1A1A);
            }
            case RIGHT_LEG, LEFT_LEG -> {
                r.box(-2, 0, -2, 2, 7, 2, red);
                r.box(-2.2f, 7, -2.8f, 2.2f, 12, 2.2f, 0xFF1A1A1A);
            }
        }
    }

    private CharacterModels() {
    }
}
