package com.nuxvisuals.ui;

import com.nuxvisuals.config.Config;
import com.nuxvisuals.friend.FriendManager;
import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.ModuleManager;
import com.nuxvisuals.module.impl.ClickGuiModule;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.ColorSetting;
import com.nuxvisuals.module.setting.KeySetting;
import com.nuxvisuals.module.Keys;
import com.nuxvisuals.module.setting.ModeSetting;
import com.nuxvisuals.module.setting.Setting;
import com.nuxvisuals.module.setting.SliderSetting;
import com.nuxvisuals.module.setting.StringSetting;
import com.nuxvisuals.render.RenderUtil;
import com.nuxvisuals.waypoint.Waypoint;
import com.nuxvisuals.waypoint.WaypointManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// ClickGUI NuxVisuals: категории слева, поиск, карточки модулей с настройками,
// выпадающими списками, палитрой цветов и биндами.
// ЛКМ по карточке — вкл/выкл, ПКМ — открыть/закрыть настройки, клик по «N/A» — назначить клавишу.
public class NuxClickGui extends Screen {
    // палитра
    // палитра (пересчитывается каждый кадр из модуля Click GUI)
    private static int PANEL = 0xF0121722;
    private static int CARD = 0xFF181E2B;
    private static int CARD_HOVER = 0xFF1D2433;
    private static int CARD_ON = 0xFF1B2742;
    private static int FIELD = 0xFF222A3B;
    private static int FIELD_HOVER = 0xFF2A3347;
    private static final int TEXT = 0xFFEDF1FA;
    private static final int SOFT = 0xFFC3CBDB;
    private static final int MUTED = 0xFF7C869A;
    private static final int DIM = 0xFF4E5668;
    private static final int LINE = 0x16FFFFFF;
    private static int ACCENT = 0xFF4F8BFF;
    private static int ACCENT_LIGHT = 0xFF7AA8FF;

    // Colours are rebuilt from saved settings every frame; text colour stays independent.
    private static final int BASE = 0xFF10141E;

    private static void updatePalette() {
        ClickGuiModule theme = ClickGuiModule.get();
        ACCENT = 0xFF000000 | (theme == null ? ClickGuiModule.ACCENT : theme.accentColor());
        int base = theme == null ? BASE : theme.panelColor();
        ACCENT_LIGHT = NuxShapes.blend(ACCENT, 0xFFFFFFFF, 0.3f);
        PANEL = ((theme == null ? 216 : theme.panelAlpha()) << 24) | (base & 0xFFFFFF);
        CARD = NuxShapes.blend(base, 0xFFFFFFFF, 0.05f);
        CARD_HOVER = NuxShapes.blend(base, 0xFFFFFFFF, 0.08f);
        CARD_ON = NuxShapes.blend(NuxShapes.blend(base, 0xFFFFFFFF, 0.05f), ACCENT, 0.13f);
        FIELD = NuxShapes.blend(base, 0xFFFFFFFF, 0.09f);
        FIELD_HOVER = NuxShapes.blend(base, 0xFFFFFFFF, 0.13f);
    }

    private static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0xFFFFFF);
    }

    private static final int[] PRESETS = {0x8187FF, 0xF5A524, 0xC7CBD1, 0x4F7BFF, 0xEF5350, 0xFFFFFF};

    // ---- иконки 7x7 ----
    private static final String[] ICON_VISUALS = {"...X...", ".X.X.X.", "..XXX..", "XXXXXXX", "..XXX..", ".X.X.X.", "...X..."};
    private static final String[] ICON_DISPLAY = {"X.XXXXX", ".......", "X.XXXXX", ".......", "X.XXXXX", ".......", "X.XXXXX"};
    private static final String[] ICON_UTILS = {"XX...XX", "X.X.X.X", ".XXXXX.", "..X.X..", ".XXXXX.", "X.X.X.X", "XX...XX"};
    private static final String[] ICON_EVENTS = {"XXXXXXX", "X.....X", "XXXXXXX", ".......", "XXXXXXX", "X.....X", "XXXXXXX"};
    private static final String[] ICON_WAYPOINT = {"..XXX..", ".X...X.", "X..X..X", "X.....X", ".X...X.", "..X.X..", "...X..."};
    private static final String[] ICON_COSMETICS = {"..XXX..", "...X...", "...X...", "..X.X..", ".X...X.", "X.XXX.X", "XXXXXXX"};
    private static final String[] ICON_FRIENDS = {".XX.XX.", "XXXXXXX", "XXXXXXX", "XXXXXXX", ".XXXXX.", "..XXX..", "...X..."};
    private static final String[] ICON_CONFIGS = {"XXX....", "XXXXXXX", "X.....X", "X.XXX.X", "X.....X", "X.XX..X", "XXXXXXX"};
    private static final String[] COSMETICS_ICON = {"X.X.X.X", ".......", ".......", ".......", ".......", ".......", "......."};
    private static final String[] ICON_CHECK = {"......X", ".....X.", "X...X..", ".X.X...", "..X...."};
    private static final String[] ICON_SEARCH = {".XXX..", "X...X.", "X...X.", ".XXX..", "....X.", ".....X"};
    private static final String[] ICON_DOWN = {"X...X", ".X.X.", "..X.."};
    private static final String[] ICON_UP = {"..X..", ".X.X.", "X...X"};
    private static final String[] ICON_EYE = {"..XXX..", ".X...X.", "X..X..X", ".X...X.", "..XXX.."};

    private static final Map<String, String> RU = new HashMap<>();

    static {
        NuxIcons.register(ICON_VISUALS, "visuals");
        NuxIcons.register(ICON_DISPLAY, "display");
        NuxIcons.register(ICON_UTILS, "utils");
        NuxIcons.register(ICON_WAYPOINT, "waypoint");
        NuxIcons.register(ICON_CONFIGS, "configs");
        NuxIcons.register(COSMETICS_ICON, "shirt");
        NuxIcons.register(ICON_CHECK, "check");
        NuxIcons.register(ICON_SEARCH, "search");
        NuxIcons.register(ICON_DOWN, "down");
        NuxIcons.register(ICON_UP, "up");
        NuxIcons.register(ICON_EYE, "eye");
    }

    static {
        String[][] pairs = {
                {"Strength", "Сила"}, {"Fire Overlay", "Огонь на экране"}, {"Underwater Overlay", "Эффект под водой"}, {"In-Wall Overlay", "Блок в голове"}, {"Pumpkin Overlay", "Тыква на голове"}, {"Powder Snow Overlay", "Рыхлый снег"}, {"Vignette", "Виньетка"}, {"Portal/Nausea", "Портал/тошнота"}, {"Totem Animation", "Анимация тотема"}, {"Boss Bar", "Полоса босса"}, {"Scoreboard", "Скорборд"}, {"Titles", "Титры (Title)"}, {"Action Bar", "Строка над хотбаром"}, {"Rain/Snow", "Дождь/снег"}, {"Rain Particles & Sound", "Брызги и звук дождя"}, {"Blindness/Darkness Fog", "Туман слепоты/тьмы"}, {"Keep Chat", "Не очищать чат"}, {"History Size", "Размер истории"}, {"Anti Duplicate", "Убирать дубли"}, {"Duplicates", "Дубли"}, {"Search Depth", "Глубина поиска дублей"}, {"Timestamps", "Время сообщений"}, {"Animation", "Анимация"}, {"Reveal Speed", "Скорость появления"}, {"Hide Nick", "Скрыть ник"}, {"Nick", "Ник"}, {"Custom Nick", "Свой ник"}, {"Hide Password", "Скрыть пароль"}, {"Reset Distance", "Сброс при телепорте"},
                {"Online Lyrics", "Текст из LRCLIB"}, {"Track Name Fallback", "Название, если нет текста"},
                {"Sync Offset", "Сдвиг синхронизации"}, {"Copies", "Копии вокруг игрока"},
                {"Previous/Next Lines", "Соседние строки"}, {"Text Shadow", "Тень текста"},
                {"Light Menu Background", "Лёгкий фон меню"}, {"Fast GUI Shapes", "Быстрые фигуры GUI"},
                {"Color", "Цвет"}, {"Rainbow", "Радуга"}, {"Background", "Фон"},
                {"Background Opacity", "Прозрачность фона"}, {"Scale", "Масштаб"}, {"Mode", "Режим"},
                {"Style", "Стиль"}, {"Size", "Размер"}, {"Radius", "Радиус"}, {"Height", "Высота"},
                {"Y Offset", "Смещение по Y"}, {"Opacity", "Прозрачность"}, {"Outline", "Обводка"},
                {"Spin", "Вращение"}, {"All Players", "Все игроки"}, {"Speed", "Скорость"}, {"Gap", "Отступ"},
                {"Thickness", "Толщина"}, {"Dynamic", "Динамический"}, {"Range", "Дальность"},
                {"Show Players", "Показывать игроков"}, {"Show Coords", "Показывать координаты"},
                {"Show Ping", "Показывать пинг"}, {"Direction", "Направление"}, {"Nether Coords", "Координаты Ада"},
                {"Mouse Buttons", "Кнопки мыши"}, {"Show CPS", "Показывать CPS"}, {"Space Bar", "Пробел"},
                {"Layout", "Расположение"}, {"Main Hand", "Предмет в руке"}, {"Durability", "Прочность"},
                {"Hide Vanilla Icons", "Скрыть ванильные иконки"}, {"Progress Bar", "Полоска времени"},
                {"Effect Colors", "Цвета эффектов"}, {"Show Number", "Показывать число"},
                {"Show Saturation", "Насыщение"}, {"Slot Numbers", "Номера слотов"},
                {"Smooth Select", "Плавный выбор"}, {"Zoom", "Приближение"}, {"Smooth", "Плавно"},
                {"Cinematic Camera", "Кинокамера"}, {"Hide HUD Modules", "Скрыть HUD-модули"},
                {"Side Line", "Боковая линия"}, {"Attack Cooldown", "Перезарядка удара"},
                {"Red On Target", "Красный на цели"}, {"Poison/Wither Tint", "Цвет при яде"},
                {"Beam", "Луч"}, {"Labels", "Подписи"}, {"Show Distance", "Показывать расстояние"},
                {"Sound", "Звук"}, {"Volume", "Громкость"}, {"Pitch", "Тон"}, {"Effect", "Эффект"},
                {"Particles", "Частицы"}, {"Accent", "Акцент"}, {"Panel", "Фон панели"}, {"Panel Opacity", "Прозрачность панели"}, {"Sync HUD Color", "Цвет HUD как у меню"}, {"Effect", "Эффект"}, {"Hands", "Руки"}, {"Second Color", "Второй цвет"}, {"Brightness", "Яркость"}, {"Offset X", "Сдвиг X"}, {"Offset Y", "Сдвиг Y"}, {"Offset Z", "Сдвиг Z"}, {"Snow Amount", "Кол-во снега"}, {"Snow Size", "Размер снежинок"}, {"Snow Speed", "Скорость снега"}, {"Wind", "Ветер"}, {"Frost Fog", "Зимняя дымка"}, {"Glow", "Свечение"}, {"Opacity", "Прозрачность"}, {"Shading", "Объём (тени)"}, {"Width", "Толщина"}, {"Landing Mark", "Точка падения"}, {"Highlight Hit", "Красный при попадании"}, {"Title", "Заголовок"}, {"Slot Background", "Фон слотов"}, {"Trap Size", "Размер трапки"}, {"Trap Height", "Высота трапки"}, {"Plast Width", "Ширина пласта"}, {"Plast Height", "Высота пласта"}, {"Disorientation Radius", "Радиус дезориентации"}, {"Dust Radius", "Радиус явной пыли"}, {"Fire Radius", "Радиус огн. смерча"}, {"Freeze Radius", "Радиус заморозки"}, {"Aura Radius", "Радиус божьей ауры"}, {"Detect By Item", "Узнавать по предмету"}, {"Key", "Клавиша"}, {"Scroll Adjust", "Колесо меняет зум"}, {"Fill", "Заливка"}, {"Show Nick", "Показывать ник"}, {"Show FPS", "Показывать FPS"}, {"Amount", "Количество"}, {"Count", "Количество"}, {"Wings", "Крылья"}, {"Wings Type", "Вид крыльев"}, {"Custom Color", "Свой цвет"}, {"Wings Color", "Цвет крыльев"}, {"Size", "Размер"}, {"Self", "Свои тотемы"},
        };
        RU.put("Trap Key", "Трапка");
        RU.put("Plast Key", "Пласт");
        RU.put("Disorientation Key", "Дезориентация");
        RU.put("Dust Key", "Явная пыль");
        RU.put("Fire Tornado Key", "Огненный смерч");
        RU.put("Freeze Key", "Снежок заморозки");
        RU.put("Aura Key", "Божья аура");
        RU.put("Pearl Key", "Жемчуг");
        RU.put("Chorus Key", "Хорус");
        RU.put("Wind Charge Key", "Заряд ветра");
        RU.put("Donat Potion 1 Key", "Донат-зелье 1");
        RU.put("Donat Potion 2 Key", "Донат-зелье 2");
        RU.put("Donat Potion 3 Key", "Донат-зелье 3");
        RU.put("Notify", "Уведомления");
        RU.put("HolyWorld: Взрывная трапка — клавиша", "Взрывная трапка");
        RU.put("HolyWorld: Взрывная трапка — режим", "Режим: Взрывная трапка");
        RU.put("HolyWorld: Взрывная трапка — названия", "Названия: Взрывная трапка");
        RU.put("HolyWorld: Трапка — клавиша", "Трапка");
        RU.put("HolyWorld: Трапка — режим", "Режим: Трапка");
        RU.put("HolyWorld: Трапка — названия", "Названия: Трапка");
        RU.put("HolyWorld: Снежок — клавиша", "Снежок");
        RU.put("HolyWorld: Снежок — режим", "Режим: Снежок");
        RU.put("HolyWorld: Снежок — названия", "Названия: Снежок");
        RU.put("HolyWorld: Стан — клавиша", "Стан");
        RU.put("HolyWorld: Стан — режим", "Режим: Стан");
        RU.put("HolyWorld: Стан — названия", "Названия: Стан");
        RU.put("HolyWorld: Взрывная штучка — клавиша", "Взрывная штучка");
        RU.put("HolyWorld: Взрывная штучка — режим", "Режим: Взрывная штучка");
        RU.put("HolyWorld: Взрывная штучка — названия", "Названия: Взрывная штучка");
        RU.put("HolyWorld: Тыква Джейка — клавиша", "Тыква Джейка");
        RU.put("HolyWorld: Тыква Джейка — режим", "Режим: Тыква Джейка");
        RU.put("HolyWorld: Тыква Джейка — названия", "Названия: Тыква Джейка");
        RU.put("Show Cover", "Обложка песни");
        RU.put("Show Progress", "Прогресс песни");
        RU.put("Show App", "Название плеера");
        RU.put("Hide When Idle", "Скрывать без музыки");
        RU.put("Play/Pause Key", "Пуск / пауза");
        RU.put("Next Key", "Следующий трек");
        RU.put("Previous Key", "Предыдущий трек");
        for (String[] p : pairs) {
            RU.put(p[0], p[1]);
        }
    }

    // запоминаем между открытиями
    private static Category selected = Category.Visuals;
    private static final Set<String> EXPANDED = new HashSet<>();

    private net.minecraft.client.util.SkinTextures profileSkin;private long profileSkinRefresh;
    private String search = "";
    private final GuiFavorites favorites=new GuiFavorites(Config.configsDir().getParent().resolve("gui.properties"));
    private int filter;private Module hoverModule,previousHover;private long hoverSince;
    private final Map<Module,String> searchIndex=new HashMap<>();
    private static final String[] FILTER_NAMES={"Все","Вкл","Избр","Бинды"};
    private static final String[] ICON_STAR={"...X...","...X...","XXXXXXX",".XXXXX.","..XXX..",".XX.XX.","XX...XX"};
    private GuiView.Layout layout(){return GuiView.layout(this.width,this.height);}
    private void resetView(){scroll=0;scrollAnim=0;dropdown=null;picker=null;}
    private String fit(String value,int width){return GuiView.fit(value,width,this::width);}

    private String cachedSearch;
    private Category cachedCategory;
    private List<Module> cachedModules = List.of();
    private static String wpName = "";
    private static String cfgName = "";
    private static String friendName = "";
    private String friendMessage;
    private boolean friendError;
    private long friendMessageTime;
    private String cfgMessage;
    private boolean cfgError;
    private long cfgMessageTime;
    private String cfgDelete;
    private static String wpX = "";
    private static String wpY = "";
    private static String wpZ = "";
    private String wpMessage;
    private long wpMessageTime;
    private int focusedField;
    private Waypoint colorEdit;
    private KeySetting bindingSetting;
    private StringSetting textSetting;
    private boolean searchFocused;
    private Module binding;
    private ModeSetting dropdown;
    private boolean dropdownSeen;
    private int ddX;
    private int ddY;
    private int ddW;
    private int ddBoxY;
    private ColorSetting picker;
    private Drag drag;
    private double scroll;
    private double scrollAnim;
    private int maxScroll;
    private int[] clip;
    private int mouseX;
    private int mouseY;
    private long lastFrame;
    private float dt;
    private final List<Hit> hits = new ArrayList<>();
    private final Map<String, Float> anim = new HashMap<>();
    private long openTime;
    private float openK = 1f;

    private interface Drag {
        void apply(double mx, double my);
    }

    private record Hit(int x1, int y1, int x2, int y2, Runnable left, Runnable right, Drag drag) {
        boolean contains(double mx, double my) {
            return mx >= x1 && mx < x2 && my >= y1 && my < y2;
        }
    }

    // открыть меню сразу на нужном модуле (ПКМ по элементу HUD)
    public static void openFor(Module module) {
        selected = module.getCategory();
        EXPANDED.add(module.getId());
        net.minecraft.client.MinecraftClient.getInstance().setScreen(new NuxClickGui());
    }

    public NuxClickGui() {
        super(Text.literal("NuxVisuals"));
    }

    // ---------- раскладка ----------

    private int panelW() {
        return layout().width();
    }

    private int panelH() {
        return layout().height();
    }

    private int px() {
        return this.width / 2 - panelW() / 2;
    }

    private int py() {
        return this.height / 2 - panelH() / 2 + Math.round((1f - openK) * 12f);
    }

    // ---------- помощники ----------

    private float anim(String key, float target, float speed) {
        float v = anim.getOrDefault(key, target);
        v += (target - v) * Math.min(1f, dt * speed);
        anim.put(key, v);
        return v;
    }

    private boolean hover(int x1, int y1, int x2, int y2) {
        if (clip != null && (mouseX < clip[0] || mouseX >= clip[2] || mouseY < clip[1] || mouseY >= clip[3])) {
            return false;
        }
        return mouseX >= x1 && mouseX < x2 && mouseY >= y1 && mouseY < y2;
    }

    private void hit(int x1, int y1, int x2, int y2, Runnable left, Runnable right, Drag drag) {
        if (clip != null) {
            x1 = Math.max(x1, clip[0]);
            y1 = Math.max(y1, clip[1]);
            x2 = Math.min(x2, clip[2]);
            y2 = Math.min(y2, clip[3]);
            if (x1 >= x2 || y1 >= y2) {
                return;
            }
        }
        hits.add(new Hit(x1, y1, x2, y2, left, right, drag));
    }

    private void text(DrawContext context, String s, int x, int y, int color) {
        NuxFont.draw(context, this.textRenderer, s, x, y, color);
    }

    private int width(String s) {
        return NuxFont.width(this.textRenderer, s);
    }

    private static void rect(DrawContext context, int x, int y, int w, int h, int r, int color) {
        NuxShapes.roundedRect(context, x, y, w, h, r, color);
    }

    private static String label(Setting setting) {
        return RU.getOrDefault(setting.getName(), setting.getName());
    }

    public static String keyName(int key) {
        if (key < 0) {
            return "N/A";
        }
        if (key >= Keys.MOUSE_OFFSET) {
            return "MOUSE" + (key - Keys.MOUSE_OFFSET + 1);
        }
        String name = InputUtil.Type.KEYSYM.createFromCode(key).getLocalizedText().getString();
        name = name.toUpperCase(Locale.ROOT);
        return name.length() > 9 ? name.substring(0, 9) : name;
    }

    public static String[] icon(Category category) {
        return switch (category) {
            case Visuals -> ICON_VISUALS;
            case Hud -> ICON_DISPLAY;
            case Utilities -> ICON_UTILS;
            case Waypoint -> ICON_WAYPOINT;
            case Friends -> ICON_FRIENDS;
            case Configs -> ICON_CONFIGS;
        };
    }

    private List<Module> modules(){
        List<Module> list=new ArrayList<>();String q=GuiView.normalize(search);
        for(Module m:ModuleManager.MODULES){
            if(q.isEmpty()&&m.getCategory()!=selected)continue;
            String hay=searchIndex.computeIfAbsent(m,v->GuiView.normalize(v.getName()+" "+v.getDescription()+" "+v.getId()));
            if(!GuiView.matches(hay,q)||!GuiView.visible(filter,m.isEnabled(),favorites.contains(m.getId()),m.getKey()>=0))continue;
            list.add(m);
        }
        list.sort(java.util.Comparator.comparing((Module m)->!favorites.contains(m.getId())).thenComparing(Module::getName,String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    // ---------- отрисовка ----------

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        if (this.client.world != null) {
            context.fillGradient(0, 0, this.width, this.height, 0x60050812, 0xA0020408);
            if (!com.nuxvisuals.module.impl.Optimization.lightMenu()) {
                NuxShapes.glow(context, this.width / 2f, this.height / 2f, Math.max(this.width, this.height) * 0.6f,
                        (ClickGuiModule.get() == null ? ClickGuiModule.ACCENT : ClickGuiModule.get().accentColor()), 26);
            }
        } else {
            NuxBackground.render(context, this.width, this.height);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        long now = System.currentTimeMillis();
        dt = lastFrame == 0 ? 1f : Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        hoverModule=null;
        hits.clear();
        clip = null;
        updatePalette();
        if (openTime == 0) {
            openTime = now;
        }
        float t = Math.min(1f, (now - openTime) / 260f);
        openK = 1f - (1f - t) * (1f - t) * (1f - t);

        int px = px();
        int py = py();
        int w = panelW();
        int h = panelH();

        if(!com.nuxvisuals.module.impl.Optimization.lightMenu()){
            for(int i=3;i>=1;i--)rect(context,px-i*2,py-i*2+3,w+i*4,h+i*4,11+i,0x14000000);
        }
        rect(context,px-1,py-1,w+2,h+2,12,0x22FFFFFF);
        rect(context,px,py,w,h,11,PANEL);
        context.fillGradient(px+125,py+1,px+w-10,py+48,withAlpha(ACCENT,14),0x00000000);

        drawHeader(context, px, py, w);
        drawSidebar(context, px, py, h);
        drawContent(context, px, py, w, h);
        drawDropdown(context,0,0,this.width,this.height);
        drawModuleTooltip(context);

    }

    // русские названия категорий
    private static String catName(Category c) {
        return switch (c) {
            case Visuals -> "Визуалы";
            case Hud -> "Интерфейс";
            case Utilities -> "Утилиты";
            case Waypoint -> "Метки";
            case Friends -> "Друзья";
            case Configs -> "Конфиги";
        };
    }

    private void drawModuleTooltip(DrawContext context){
        long now=System.currentTimeMillis();if(hoverModule!=previousHover){previousHover=hoverModule;hoverSince=now;}
        if(hoverModule==null||now-hoverSince<650||dropdown!=null||picker!=null||binding!=null)return;
        String desc=hoverModule.getDescription();int max=Math.min(220,this.width-24);List<String> lines=new ArrayList<>();
        lines.add(fit(hoverModule.getName(),max-16));String part="";
        for(String word:desc.split(" ")){if(width(part+" "+word)>max-16&&!part.isEmpty()){lines.add(part);part="";}part=part.isEmpty()?word:part+" "+word;if(lines.size()>=5)break;}
        if(!part.isEmpty()&&lines.size()<6)lines.add(fit(part,max-16));
        lines.add("СКМ — назначить бинд");int tw=0;for(String line:lines)tw=Math.max(tw,width(line));tw=Math.min(max,tw+16);int th=lines.size()*12+12;
        int x=Math.max(6,Math.min(this.width-tw-6,mouseX+12)),y=mouseY+16;if(y+th>this.height-6)y=Math.max(6,mouseY-th-8);
        rect(context,x-1,y-1,tw+2,th+2,8,0x25FFFFFF);rect(context,x,y,tw,th,7,0xF4111621);
        for(int line=0;line<lines.size();line++)text(context,fit(lines.get(line),tw-16),x+8,y+7+line*12,line==0?TEXT:SOFT);
    }

    private void bigText(DrawContext context, String s, int x, int y, int color, float scale) {
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(scale, scale, 1f);
        text(context, s, 0, 0, color);
        context.getMatrices().pop();
    }

    // шапка теперь встроена в боковую панель
    private void drawHeader(DrawContext context, int px, int py, int w) {
    }

    private void drawSidebar(DrawContext context, int px, int py, int h) {
        int sx = px + 6;
        int sy = py + 6;
        int sw = 114;
        int sh = h - 12;
        // утопленная «стеклянная» колонка
        rect(context, sx - 1, sy - 1, sw + 2, sh + 2, 10, 0x10FFFFFF);
        rect(context, sx, sy, sw, sh, 9, NuxShapes.blend(PANEL | 0xFF000000, 0xFF000000, 0.35f) & 0xE0FFFFFF);
        context.fillGradient(sx + 6, sy + 1, sx + sw - 6, sy + 40, withAlpha(ACCENT, 0x1C), 0x00000000);

        // логотип
        NuxIcons.logo(context, sx + 9, sy + 9, 20, ACCENT);
        text(context,"NuxVisuals",sx+35,sy+10,TEXT);
        text(context, "v1.1 · 1.21.4", sx + 35, sy + 20, DIM);

        text(context,"РАЗДЕЛЫ",sx+10,sy+37,DIM);
        int y0=sy+51;
        int row=Math.max(14,Math.min(22,(h-122)/Category.values().length));
        int selIndex = 0;
        for (Category c : Category.values()) {
            if (c == selected) break;
            selIndex++;
        }
        float pillY = anim("pillY", y0 + selIndex * row, 16f);
        float pillA = anim("pillA", search.isEmpty() ? 1f : 0f, 14f);
        if (pillA > 0.01f) {
            int p0 = Math.round(pillY);
            rect(context, sx + 5, p0 - 5, sw - 10, 18, 6, withAlpha(ACCENT, (int) (pillA * 0x30)));
            rect(context, sx + 5, p0 - 5, sw - 10, 9, 6, withAlpha(0xFFFFFF, (int) (pillA * 0x08)));
            rect(context, sx + 2, p0 - 1, 2, 10, 1, RenderUtil.withAlpha(ACCENT_LIGHT, pillA));
        }
        int y = y0;
        for (Category c : Category.values()) {
            boolean sel = c == selected && search.isEmpty();
            boolean hov = hover(sx + 5, y - 5, sx + sw - 5, y + 13);
            float a = anim("cat" + c.name(), sel ? 1f : hov ? 0.55f : 0f, 14f);
            int color = NuxShapes.blend(MUTED, TEXT, a);
            if (hov && !sel) {
                rect(context, sx + 5, y - 5, sw - 10, 18, 6, 0x0CFFFFFF);
            }
            // иконка в квадратике
            rect(context, sx + 10, y - 3, 13, 13, 4, sel ? withAlpha(ACCENT, 0x70) : 0x10FFFFFF);
            NuxIcons.draw(context, icon(c), sx + 13, y, 1, sel ? 0xFFFFFFFF : color);
            text(context, catName(c), sx + 28, y, color);
            int count = 0;
            for (Module m : ModuleManager.MODULES) {
                if (m.getCategory() == c && m.isEnabled() && !(m instanceof ClickGuiModule)) count++;
            }
            if (count > 0) {
                String cs = String.valueOf(count);
                int cw = Math.max(12, width(cs) + 7);
                int cx = sx + sw - 9 - cw;
                rect(context, cx, y - 1, cw, 10, 5, sel ? withAlpha(ACCENT, 0x80) : 0x14FFFFFF);
                text(context, cs, cx + cw / 2 - width(cs) / 2, y, sel ? TEXT : MUTED);
            }
            Category target = c;
            hit(sx + 5, y - 5, sx + sw - 5, y + 13, () -> {
                selected=target;search="";filter=0;resetView();
                dropdown = null;
                picker = null;
            }, null, null);
            y += row;
        }

        // Profile card: clean portrait, independent toolbar and no fake online indicator.
        int uy=sy+sh-48,ux=sx+5,uw=sw-10;
        rect(context,ux,uy,uw,43,7,0x12FFFFFF);
        try{
            long now=System.currentTimeMillis();if(profileSkin==null||now>=profileSkinRefresh){
                profileSkin=this.client.player!=null?this.client.player.getSkinTextures():this.client.getSkinProvider().getSkinTextures(this.client.getGameProfile());profileSkinRefresh=now+1000;
            }
            NuxAvatar.draw(context,profileSkin,ux+6,uy+5,22);
        }catch(Exception ignored){NuxIcons.draw(context,NuxIcons.PERSON,ux+10,uy+10,1,MUTED);}
        String user=fit(this.client.getSession().getUsername(),uw-40);
        text(context,user,ux+35,uy+7,TEXT);text(context,"Профиль",ux+35,uy+18,MUTED);
        text(context,"локально",ux+6,uy+32,MUTED);
        int gx=ux+uw-15,cosX=gx-17;
        boolean gh=hover(gx-3,uy+28,gx+12,uy+42),ch=hover(cosX-3,uy+28,cosX+12,uy+42);
        if(gh)rect(context,gx-3,uy+28,14,13,4,0x20FFFFFF);if(ch)rect(context,cosX-3,uy+28,14,13,4,0x20FFFFFF);
        NuxIcons.draw(context,NuxIcons.GEAR,gx,uy+31,1,gh?TEXT:MUTED);
        NuxIcons.draw(context,COSMETICS_ICON,cosX+1,uy+31,1,ch?TEXT:MUTED);
        hit(gx-3,uy+28,gx+12,uy+42,()->{ClickGuiModule gui=ClickGuiModule.get();if(gui!=null){selected=gui.getCategory();search="";filter=0;resetView();EXPANDED.add(gui.getId());}},null,null);
        hit(cosX-3,uy+28,cosX+12,uy+42,()->this.client.setScreen(new NuxCosmeticsScreen(this)),null,null);
    }

    private void drawSearch(DrawContext context, int x, int y, int w) {
        boolean focus = searchFocused;
        boolean hov = hover(x, y, x + w, y + 18);
        rect(context, x - 1, y - 1, w + 2, 20, 7, focus ? withAlpha(ACCENT, 0xA0) : hov ? 0x2AFFFFFF : 0x18FFFFFF);
        rect(context, x, y, w, 18, 6, NuxShapes.blend(PANEL | 0xFF000000, 0xFF000000, 0.3f));
        String shown = search;
        while (width(shown) > w - 36 && !shown.isEmpty()) shown = shown.substring(1);
        if (search.isEmpty() && !focus) {
            text(context,fit("Поиск модулей…",w-29),x+17,y+5,DIM);
        } else {
            text(context, shown, x + 17, y + 5, TEXT);
            if (focus && (System.currentTimeMillis() / 500) % 2 == 0) {
                int c = x + 17 + width(shown) + 1;
                context.fill(c, y + 4, c + 1, y + 13, ACCENT_LIGHT);
            }
        }
        NuxIcons.draw(context, ICON_SEARCH, x + 6, y + 6, 1, focus ? ACCENT_LIGHT : MUTED);
        hit(x, y, x + w, y + 18, () -> searchFocused = true, () -> {
            search = "";
            scroll = 0;
        }, null);
        if(!search.isEmpty()){
            rect(context,x+w-15,y+3,12,12,4,hover(x+w-15,y+2,x+w-2,y+16)?FIELD_HOVER:FIELD);
            text(context,"×",x+w-12,y+5,SOFT);
            hit(x+w-16,y+1,x+w,y+17,()->{search="";searchFocused=true;resetView();},null,null);
        }
    }

    private void drawContent(DrawContext context, int px, int py, int w, int h) {
        int cx = px + 134;
        int cw = px + w - 14 - cx;
        int cy=py+72;
        {
            List<Module> shownList=modules();String title=search.isEmpty()?catName(selected):"Поиск";
            int sw=Math.min(142,Math.max(74,cw/2));float titleScale=cw<280?1.15f:1.4f;
            bigText(context,fit(title,(int)((cw-sw-10)/titleScale)),cx,py+13,TEXT,titleScale);
            drawSearch(context,cx+cw-sw,py+12,sw);
            String info=shownList.size()+" модулей · ПКМ — настройки";
            if(search.isEmpty()&&selected==Category.Configs)info=Config.list().size()+" конфигов";
            if(search.isEmpty()&&selected==Category.Friends)info=FriendManager.size()+" друзей";
            if(search.isEmpty()&&selected==Category.Waypoint)info=WaypointManager.current().size()+" меток";
            text(context,fit(info,cw),cx,py+35,MUTED);
            boolean catalog=!(search.isEmpty()&&(selected==Category.Configs||selected==Category.Friends||selected==Category.Waypoint));
            if(catalog){int fx=cx;int bw=Math.max(28,Math.min(54,(cw-9)/4));
                for(int i=0;i<FILTER_NAMES.length;i++){int index=i;boolean sel=filter==i;
                    rect(context,fx,py+49,bw,16,5,sel?withAlpha(ACCENT,42):hover(fx,py+49,fx+bw,py+65)?FIELD_HOVER:FIELD);
                    text(context,FILTER_NAMES[i],fx+(bw-width(FILTER_NAMES[i]))/2,py+53,sel?TEXT:MUTED);
                    hit(fx,py+49,fx+bw,py+65,()->{filter=index;resetView();},null,null);fx+=bw+3;
                }
            }
        }
        int ch = py + h - 10 - cy;
        int columns=layout().columns();
        int colW=(cw-(columns-1)*8)/columns;

        scrollAnim += (scroll - scrollAnim) * Math.min(1f, dt * 16f);
        int off = (int) Math.round(scrollAnim);
        dropdownSeen = false;

        clip = new int[]{cx - 3, cy - 2, cx + cw + 3, cy + ch};
        context.enableScissor(clip[0], clip[1], clip[2], clip[3]);
        int[] colY=new int[columns];
        if (search.isEmpty() && selected == Category.Waypoint) {
            int hgt = drawWaypointCard(context, cx, cy - off, colW);
            colY[0] += hgt + 6;
        }
        if (search.isEmpty() && selected == Category.Friends) {
            int hgt = drawFriendsCard(context, cx, cy - off, cw);
            colY[0] += hgt + 6;
            for(int c=1;c<columns;c++)colY[c]+=hgt+6;
        }
        if (search.isEmpty() && selected == Category.Configs) {
            int hgt = drawConfigCard(context, cx, cy - off, cw);
            colY[0] += hgt + 6;
            for(int c=1;c<columns;c++)colY[c]+=hgt+6;
        }
        List<Module> list = modules();
        for (Module m : list) {
            int col=0;for(int c=1;c<columns;c++)if(colY[c]<colY[col])col=c;
            int x = cx + col * (colW + 8);
            int hgt = drawCard(context, m, x, cy + colY[col] - off, colW);
            colY[col] += hgt + 6;
        }
        context.disableScissor();
        clip = null;

        // мягкое затухание у нижнего края списка
        context.fillGradient(cx - 3, cy + ch - 10, cx + cw + 3, cy + ch, 0x00000000, withAlpha(PANEL, 0x60));

        if (list.isEmpty() && !(search.isEmpty() && (selected == Category.Waypoint || selected == Category.Configs || selected == Category.Friends))) {
            String s2 = search.isEmpty() ? filter==2?"Добавьте модули в избранное":"Нет модулей для этого фильтра" : "Ничего не найдено";
            NuxIcons.draw(context, ICON_SEARCH, cx + cw / 2 - 3, cy + ch / 2 - 16, 1, DIM);
            text(context, s2, cx + cw / 2 - width(s2) / 2, cy + ch / 2 - 4, MUTED);
            if (!search.isEmpty()) {
                String tip = "ПКМ по поиску — очистить";
                text(context, tip, cx + cw / 2 - width(tip) / 2, cy + ch / 2 + 8, DIM);
            }
        }

        int content=0;for(int yy:colY)content=Math.max(content,yy);content-=6;
        maxScroll = Math.max(0, content - ch);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        if (maxScroll > 0) {
            int barH = Math.max(16, ch * ch / (content + 1));
            int barY = cy + (int) ((ch - barH) * (scrollAnim / maxScroll));
            boolean barHover = hover(cx + cw, cy, cx + cw + 10, cy + ch);
            rect(context, cx + cw + 5, cy, 2, ch, 1, 0x0AFFFFFF);
            rect(context, cx + cw + 5, barY, 2, barH, 1, barHover ? ACCENT : 0x40FFFFFF);
        }
        if (!dropdownSeen) dropdown = null;
    }

    private int drawCard(DrawContext context, Module module, int x, int y, int w) {
        String id = module.getId();
        boolean on = module.isEnabled();
        List<Setting> settings = new ArrayList<>();
        for (Setting s : module.getSettings()) {
            if (s.isVisible()) settings.add(s);
        }
        boolean open = EXPANDED.contains(id) && !settings.isEmpty();
        int headerH=40;
        int bodyH = 0;
        if (open) {
            for (Setting s : settings) bodyH += settingHeight(s);
            bodyH += 8;
        }
        int h = headerH + bodyH;
        if (clip != null && (y + h < clip[1] || y > clip[3])) return h;

        boolean hov=hover(x,y,x+w,y+headerH);
        if(hov)hoverModule=module;
        float onA = anim("on" + id, on ? 1f : 0f, 12f);
        float hovA = anim("hv" + id, hov ? 1f : 0f, 16f);
        int bg = NuxShapes.blend(NuxShapes.blend(CARD, CARD_HOVER, hovA), CARD_ON, onA);
        // обводка: всегда тонкая, у включённых — акцентная
        int border = NuxShapes.blend(withAlpha(0xFFFFFF, 0x0E + (int) (hovA * 0x14)), withAlpha(ACCENT,0x38),onA);
        rect(context, x - 1, y - 1, w + 2, h + 2, 9, border);
        rect(context, x, y, w, h, 8, bg);
        if (onA > 0.02f) {
            int barH=Math.round(12*onA);
            rect(context, x + 3, y + headerH / 2 - barH / 2, 2, Math.max(2, barH), 1, RenderUtil.withAlpha(ACCENT_LIGHT, onA));
        }

        Runnable expand = () -> {
            if (!settings.isEmpty() && !EXPANDED.remove(id)) EXPANDED.add(id);
        };
        hit(x, y, x + w, y + headerH, module instanceof ClickGuiModule ? expand : module::toggle, expand, null);

        // справа: переключатель, бинд, стрелка настроек
        int right = x + w - 8;
        if (!(module instanceof ClickGuiModule)) {
            int tw = 20;
            NuxShapes.toggle(context, right - tw, y + 11, tw, 11, onA, 0xFF2C3447, ACCENT);
            right -= tw + 5;
        }
        boolean showBind=binding==module||module.getKey()>=0;
        if (showBind) {
            String bind = binding == module ? "..." : module.getKey() >= 0 ? keyName(module.getKey()) : "+ бинд";
            int bw = width(bind) + 8;
            int bx = right - bw;
            boolean bindHover = hover(bx, y + 10, bx + bw, y + 23);
            rect(context, bx, y + 10, bw, 13, 4, binding == module ? withAlpha(ACCENT, 0x50) : bindHover ? FIELD_HOVER : 0x14FFFFFF);
            text(context, bind, bx + 4, y + 13, binding == module ? ACCENT_LIGHT : module.getKey() >= 0 ? SOFT : DIM);
            hit(bx, y + 10, bx + bw, y + 23, () -> binding = module, () -> {
                module.setKey(-1);
                binding = null;
            }, null);
            right = bx - 5;
        }
        if (!settings.isEmpty()) {
            NuxIcons.draw(context,open?ICON_UP:ICON_DOWN,right-5,y+15,1,hov||open?ACCENT_LIGHT:DIM);
            hit(right-10,y+7,right+3,y+29,expand,expand,null);right-=11;
        }

        boolean favorite=favorites.contains(id);
        NuxIcons.star(context,x+9,y+7,11,favorite?0xFFFFD16B:hov?TEXT:0xFF9CA8BE);
        hit(x+5,y+3,x+22,y+22,()->{favorites.toggle(id);},null,null);
        String name=fit(module.getName(),Math.max(0,right-x-25));
        text(context,name,x+24,y+8,TEXT);
        String desc=module.getDescription().isEmpty()?"ПКМ — настройки · СКМ — бинд":module.getDescription();
        text(context,fit(desc,w-24),x+11,y+24,MUTED);

        if (open) {
            context.fill(x + 8, y + headerH - 1, x + w - 8, y + headerH, LINE);
            int sy = y + headerH + 4;
            for (Setting s : settings) {
                drawSetting(context, s, x + 10, sy, w - 20);
                sy += settingHeight(s);
            }
        }
        return h;
    }

    private int settingHeight(Setting s) {
        if(s instanceof com.nuxvisuals.module.setting.ActionSetting)return 20;
        if (s instanceof BoolSetting || s instanceof KeySetting) {
            return 18;
        }
        if (s instanceof SliderSetting) {
            return 28;
        }
        if (s instanceof ModeSetting) {
            return 35;
        }
        if (s instanceof StringSetting) {
            return 35;
        }
        if (s instanceof ColorSetting color) {
            return picker == color ? 32 + 78 : 32;
        }
        return 0;
    }

    private void drawSetting(DrawContext context, Setting s, int x, int y, int w) {
        String key = System.identityHashCode(s) + "";
        if(s instanceof com.nuxvisuals.module.setting.ActionSetting action){
            boolean hov=hover(x,y,x+w,y+17);rect(context,x,y,w,17,5,hov?FIELD_HOVER:FIELD);
            String title=action.getName();while(title.length()>1&&width(title)>w-12)title=title.substring(0,title.length()-1);
            text(context,title,x+6,y+5,TEXT);hit(x,y,x+w,y+17,action::activate,null,null);return;
        }
        if (s instanceof KeySetting ks) {
            String k = bindingSetting == ks ? "..." : keyName(ks.get());
            int kw = Math.max(26, width(k) + 10);
            int kx = x + w - kw;
            String itemLabel=label(s);
            int labelMax=Math.max(1,w-kw-6);
            if(width(itemLabel)>labelMax){
                while(itemLabel.length()>1&&width(itemLabel+"…")>labelMax)itemLabel=itemLabel.substring(0,itemLabel.length()-1);
                itemLabel+="…";
            }
            text(context,itemLabel,x,y+3,SOFT);
            boolean hov = hover(kx, y + 1, kx + kw, y + 13);
            rect(context, kx, y + 1, kw, 12, 4, bindingSetting == ks ? withAlpha(ACCENT, 0x50) : hov ? FIELD_HOVER : FIELD);
            text(context, k, kx + kw / 2 - width(k) / 2, y + 3, bindingSetting == ks ? ACCENT_LIGHT : SOFT);
            hit(kx, y + 1, kx + kw, y + 13, () -> bindingSetting = ks, () -> ks.set(-1), null);
            return;
        }
        if (s instanceof StringSetting str) {
            text(context,fit(label(s),w),x,y+2,TEXT);
            int fy = y + 13;
            boolean focus = textSetting == str;
            boolean hov = hover(x, fy, x + w, fy + 15);
            rect(context, x, fy, w, 15, 5, focus ? withAlpha(ACCENT, 0x90) : hov ? 0x26FFFFFF : 0x14FFFFFF);
            rect(context, x + 1, fy + 1, w - 2, 13, 4, hov || focus ? FIELD_HOVER : FIELD);
            String value = str.get();
            String shown = value;
            while (!shown.isEmpty() && width(shown) > w - 16) {
                shown = shown.substring(1);
            }
            if (value.isEmpty() && !focus) {
                text(context, "Введите текст...", x + 6, fy + 4, DIM);
            } else {
                text(context, shown, x + 6, fy + 4, TEXT);
                if (focus && (System.currentTimeMillis() / 500) % 2 == 0) {
                    int cx = x + 6 + width(shown) + 1;
                    context.fill(cx, fy + 3, cx + 1, fy + 12, ACCENT_LIGHT);
                }
            }
            hit(x, fy, x + w, fy + 15, () -> textSetting = str, () -> str.set(""), null);
            return;
        }
        if (s instanceof BoolSetting bool) {
            boolean rowHover = hover(x - 3, y, x + w + 3, y + 14);
            float rh = anim("bh" + key, rowHover ? 1f : 0f, 18f);
            if (rh > 0.02f) {
                rect(context, x - 3, y, w + 6, 14, 4, withAlpha(0xFFFFFF, Math.round(rh * 0x0C)));
            }
            float a = anim("b" + key, bool.get() ? 1f : 0f, 14f);
            text(context,fit(label(s),w-25),x,y+3,NuxShapes.blend(SOFT, TEXT, Math.max(a, rh)));
            int sx = x + w - 18;
            NuxShapes.toggle(context, sx, y + 2, 18, 10, a, 0xFF2C3447, ACCENT);
            hit(x, y, x + w, y + 14, bool::toggle, null, null);
        } else if (s instanceof SliderSetting slider) {
            text(context,fit(label(s),Math.max(0,w-width(slider.display())-8)),x,y+2,SOFT);
            String value = slider.display();
            text(context, value, x + w - width(value), y + 2, MUTED);
            int ty = y + 15;
            rect(context, x, ty, w, 4, 2, 0xFF2C3447);
            int fw = (int) Math.round(w * slider.progress());
            rect(context, x, ty, Math.max(4, fw), 4, 2, ACCENT);
            if (fw > 8) {
                rect(context, x + fw / 2, ty, fw - fw / 2, 4, 2, ACCENT_LIGHT);
                rect(context, x + fw / 2 - 2, ty, 4, 4, 0, NuxShapes.blend(ACCENT, ACCENT_LIGHT, 0.5f));
            }
            boolean hov = hover(x - 2, y + 10, x + w + 2, y + 22) || drag != null;
            if (hov) {
                NuxShapes.circle(context, x + fw, ty + 2, 6, withAlpha(ACCENT, 0x40));
            }
            NuxShapes.circle(context, x + fw, ty + 2, 4, ACCENT_LIGHT);
            NuxShapes.circle(context, x + fw, ty + 2, hov ? 3 : 2, 0xFFFFFFFF);
            hit(x - 3, y + 10, x + w + 3, y + 22, null, null,
                    (mx, my) -> slider.setFromProgress((mx - x) / (double) w));
        } else if (s instanceof ModeSetting mode) {
            text(context,fit(label(s),w),x,y+2,TEXT);
            int by = y + 13;
            boolean hov = hover(x, by, x + w, by + 15);
            rect(context, x, by, w, 15, 5, hov || dropdown == mode ? FIELD_HOVER : FIELD);
            text(context,fit(mode.get(),w-24),x+7,by+4,SOFT);
            NuxIcons.draw(context, dropdown == mode ? ICON_UP : ICON_DOWN, x + w - 11, by + 6, 1, SOFT);
            if (dropdown == mode) {
                dropdownSeen = true;
                ddX = x;
                ddW = w;
                ddBoxY = by;
                ddY = by + 16;
            }
            hit(x, by, x + w, by + 15, () -> dropdown = dropdown == mode ? null : mode,
                    () -> mode.cycle(1), null);
        } else if (s instanceof ColorSetting color) {
            text(context,fit(label(s),w),x,y+2,TEXT);
            int ry = y + 13;
            rect(context, x, ry, w, 15, 5, FIELD);
            int current = color.get() & 0xFFFFFF;

            // кружок текущего цвета — открывает палитру
            NuxShapes.circle(context, x + 8, ry + 7, 4, 0xFFFFFFFF);
            NuxShapes.circle(context, x + 8, ry + 7, 3, 0xFF000000 | current);
            hit(x, ry, x + 15, ry + 15, () -> picker = picker == color ? null : color, null, null);

            int sx = x + 21;
            for (int preset : PRESETS) {
                boolean selectedPreset = Math.abs(((preset >> 16) & 255) - ((current >> 16) & 255)) < 6
                        && Math.abs(((preset >> 8) & 255) - ((current >> 8) & 255)) < 6
                        && Math.abs((preset & 255) - (current & 255)) < 6;
                boolean hov = hover(sx - 4, ry + 2, sx + 4, ry + 12);
                NuxShapes.circle(context, sx, ry + 7, hov ? 4 : 3, 0xFF000000 | preset);
                if (selectedPreset) {
                    NuxShapes.circle(context, sx, ry + 7, 1, preset == 0xFFFFFF ? 0xFF222222 : 0xFFFFFFFF);
                }
                int p = preset;
                hit(sx - 4, ry + 2, sx + 5, ry + 13, () -> color.setRgb(p), null, null);
                sx += 10;
            }
            String hex = String.format("#%06X", current);
            if (sx + 4 + width(hex) < x + w - 4) {
                text(context, hex, x + w - 6 - width(hex), ry + 4, SOFT);
            }
            hit(sx, ry, x + w, ry + 15, () -> picker = picker == color ? null : color, null, null);

            if (picker == color) {
                int pyy = y + 31;
                rect(context, x, pyy, w, 76, 6, 0xFF151A25);
                text(context,"Палитра",x+w/2-width("Палитра")/2,pyy+4,SOFT);
                int sqX = x + 5;
                int sqY = pyy + 15;
                int sqW = w - 10;
                int sqH = 44;
                NuxShapes.svSquare(context, sqX, sqY, sqW, sqH, color.getHue());
                int kx = sqX + Math.round(color.getSaturation() * (sqW - 1));
                int ky = sqY + Math.round((1 - color.getBrightness()) * (sqH - 1));
                NuxShapes.circle(context, kx, ky, 3, 0xFF000000);
                NuxShapes.circle(context, kx, ky, 2, 0xFFFFFFFF);
                hit(sqX - 2, sqY - 2, sqX + sqW + 2, sqY + sqH + 2, null, null, (mx, my) -> {
                    color.setSaturation((float) ((mx - sqX) / (sqW - 1)));
                    color.setBrightness((float) (1 - (my - sqY) / (sqH - 1)));
                });

                int hueY = sqY + sqH + 6;
                NuxShapes.hueBar(context, sqX, hueY, sqW, 5);
                int hx = sqX + Math.round(color.getHue() * (sqW - 1));
                NuxShapes.circle(context, hx, hueY + 2, 3, 0xFF000000);
                NuxShapes.circle(context, hx, hueY + 2, 2, 0xFFFFFFFF);
                hit(sqX - 2, hueY - 3, sqX + sqW + 2, hueY + 8, null, null,
                        (mx, my) -> color.setHue((float) ((mx - sqX) / (sqW - 1))));
            }
        }
    }

    private static final int[] WP_COLORS = {0x8187FF, 0x4F8BFF, 0x38BDF8, 0x34D399, 0xFACC15, 0xF5A524, 0xEF5350, 0xF472B6};

    private void field(DrawContext context, int x, int y, int w, String value, String placeholder, int id) {
        boolean focus = focusedField == id;
        rect(context, x, y, w, 14, 4, focus ? withAlpha(ACCENT, 0x90) : 0x18FFFFFF);
        rect(context, x + 1, y + 1, w - 2, 12, 4, FIELD);
        String shown = value;
        while (width(shown) > w - 8 && !shown.isEmpty()) {
            shown = shown.substring(1);
        }
        if (value.isEmpty() && !focus) {
            text(context, placeholder, x + 4, y + 3, DIM);
        } else {
            text(context, shown, x + 4, y + 3, TEXT);
            if (focus && (System.currentTimeMillis() / 500) % 2 == 0) {
                int cx = x + 4 + width(shown) + 1;
                context.fill(cx, y + 2, cx + 1, y + 12, ACCENT_LIGHT);
            }
        }
        hit(x, y, x + w, y + 14, () -> focusedField = id, null, null);
    }

    private void addFromForm() {
        try {
            int x = Integer.parseInt(wpX.trim());
            int z = Integer.parseInt(wpZ.trim());
            int y;
            if (wpY.isBlank()) {
                y = this.client.player != null ? this.client.player.getBlockY() : 64;
            } else {
                y = Integer.parseInt(wpY.trim());
            }
            if (this.client.world == null) {
                wpMessage = "Зайдите в мир";
                return;
            }
            WaypointManager.add(wpName, x, y, z);
            wpName = "";
            wpX = "";
            wpY = "";
            wpZ = "";
            focusedField = 0;
            wpMessage = "Точка добавлена";
        } catch (NumberFormatException e) {
            wpMessage = "Введите X и Z числами";
        }
        wpMessageTime = System.currentTimeMillis();
    }

    // ---------- конфиги ----------

    private void cfgMsg(String message, boolean error) {
        cfgMessage = message;
        cfgError = error;
        cfgMessageTime = System.currentTimeMillis();
    }

    private void saveConfigFromForm() {
        String name = Config.clean(cfgName);
        if (name.isEmpty()) {
            cfgMsg("Введите название конфига", true);
            return;
        }
        if (Config.saveAs(name)) {
            cfgMsg("Конфиг «" + name + "» сохранён", false);
            cfgName = "";
            focusedField = 0;
        } else {
            cfgMsg("Не удалось сохранить", true);
        }
    }

    private boolean pill(DrawContext context, int x, int y, String label, boolean primary, Runnable action) {
        int w = width(label) + 10;
        boolean hov = hover(x, y, x + w, y + 13);
        int bg = primary ? (hov ? ACCENT_LIGHT : ACCENT) : (hov ? FIELD_HOVER : FIELD);
        rect(context, x, y, w, 13, 4, bg);
        NuxFont.drawOn(context,textRenderer,label,x+5,y+3,primary?ThemeContrast.ink(bg):(hov?TEXT:SOFT),bg);
        hit(x, y, x + w, y + 13, action, null, null);
        return hov;
    }

    // ---------- друзья ----------

    private void friendMsg(String message, boolean error) {
        friendMessage = message;
        friendError = error;
        friendMessageTime = System.currentTimeMillis();
    }

    private void addFriendFromForm() {
        String name = friendName.trim();
        if (name.isEmpty()) {
            friendMsg("Введите ник игрока", true);
            return;
        }
        if (!FriendManager.isValidName(name)) {
            friendMsg("Ник: 3-16 символов, латиница, цифры и _", true);
            return;
        }
        if (FriendManager.isFriend(name)) {
            friendMsg(name + " уже в друзьях", true);
            return;
        }
        FriendManager.add(name);
        friendName = "";
        focusedField = 0;
        friendMsg(name + " добавлен в друзья", false);
    }

    // игроки на сервере (из таба), которых ещё нет в друзьях
    private List<String> onlineNotFriends() {
        List<String> out = new ArrayList<>();
        if (this.client.getNetworkHandler() == null) {
            return out;
        }
        String me = this.client.getSession().getUsername();
        for (var entry : this.client.getNetworkHandler().getPlayerList()) {
            String n = entry.getProfile().getName();
            if (n == null || n.equalsIgnoreCase(me) || !FriendManager.isValidName(n) || FriendManager.isFriend(n)) {
                continue;
            }
            out.add(n);
        }
        out.sort(String.CASE_INSENSITIVE_ORDER);
        return out.size() > 24 ? out.subList(0, 24) : out;
    }

    private int drawFriendsCard(DrawContext context, int x, int y, int w) {
        List<String> friends = FriendManager.list();
        List<String> online = onlineNotFriends();
        int rowH = 22;

        // раскладка «чипсов» игроков онлайн — считаем заранее, чтобы знать высоту карточки
        int chipsX0 = x + 8;
        int chipsMax = x + w - 8;
        int chipRows = online.isEmpty() ? 0 : 1;
        int cxp = chipsX0;
        for (String n : online) {
            int cwid = width("+ " + n) + 10;
            if (cxp + cwid > chipsMax && cxp > chipsX0) {
                chipRows++;
                cxp = chipsX0;
            }
            cxp += cwid + 4;
        }
        int listH = Math.max(24, friends.size() * rowH);
        int onlineH = 16 + Math.max(12, chipRows * 16);
        int h = 28 + 18 + 12 + 4 + listH + 6 + onlineH + 4;

        rect(context, x, y, w, h, 7, CARD);
        NuxIcons.draw(context, ICON_FRIENDS, x + 8, y + 9, 1, ACCENT_LIGHT);
        text(context, "Друзья", x + 20, y + 8, TEXT);
        text(context, "на друзьях видны крылья", x + 20 + width("Друзья") + 8, y + 8, DIM);
        context.fill(x + 8, y + 24, x + w - 8, y + 25, LINE);

        // форма: ник + «Добавить»
        int fx = x + 8;
        int fy = y + 29;
        String addLabel = "Добавить";
        int aw = width(addLabel) + 10;
        field(context, fx, fy, w - 16 - aw - 4, friendName, "Ник игрока", 6);
        pill(context, x + w - 8 - aw, fy, addLabel, true, this::addFriendFromForm);
        fy += 17;
        if (friendMessage != null && System.currentTimeMillis() - friendMessageTime < 3000) {
            text(context, friendMessage, fx, fy, friendError ? 0xFFFF6B6B : ACCENT_LIGHT);
        }

        // список друзей
        int ry = y + 28 + 18 + 12 + 4;
        context.fill(x + 8, ry - 3, x + w - 8, ry - 2, LINE);
        if (friends.isEmpty()) {
            text(context, "Пока нет друзей — введи ник или нажми на игрока ниже", x + 8, ry + 6, DIM);
        }
        for (String name : friends) {
            var entry = this.client.getNetworkHandler() != null
                    ? this.client.getNetworkHandler().getPlayerListEntry(name) : null;
            boolean rowHover = hover(x + 4, ry, x + w - 4, ry + rowH - 2);
            if (rowHover) {
                rect(context, x + 4, ry, w - 8, rowH - 2, 5, 0x0CFFFFFF);
            }
            int tx = x + 12;
            if (entry != null) {
                try {
                    PlayerSkinDrawer.draw(context, entry.getSkinTextures(), x + 10, ry + 3, 14);
                } catch (Exception ignored) {
                }
                tx = x + 30;
            } else {
                rect(context, x + 10, ry + 3, 14, 14, 4, 0x14FFFFFF);
                NuxIcons.draw(context, ICON_FRIENDS, x + 14, ry + 7, 1, DIM);
                tx = x + 30;
            }
            text(context, name, tx, ry + 2, TEXT);
            text(context, entry != null ? "на сервере" : "не в сети", tx, ry + 11, entry != null ? 0xFF34D399 : DIM);

            String del = "Удалить";
            int dw = width(del) + 10;
            int bx = x + w - 10 - dw;
            boolean delHover = hover(bx, ry + 4, bx + dw, ry + 17);
            rect(context, bx, ry + 4, dw, 13, 4, delHover ? 0xFFE5484D : FIELD);
            text(context, del, bx + 5, ry + 7, delHover ? 0xFFFFFFFF : SOFT);
            String target = name;
            hit(bx, ry + 4, bx + dw, ry + 17, () -> {
                FriendManager.remove(target);
                friendMsg(target + " удалён из друзей", false);
            }, null, null);
            ry += rowH;
        }
        ry = y + 28 + 18 + 12 + 4 + listH + 6;

        // быстрое добавление: игроки на сервере
        context.fill(x + 8, ry - 3, x + w - 8, ry - 2, LINE);
        text(context, "Игроки на сервере — нажми, чтобы добавить", x + 8, ry + 2, MUTED);
        ry += 16;
        if (online.isEmpty()) {
            text(context, this.client.getNetworkHandler() == null ? "Зайди на сервер" : "Никого нет", x + 8, ry + 2, DIM);
        } else {
            int px0 = chipsX0;
            for (String n : online) {
                String label = "+ " + n;
                int cwid = width(label) + 10;
                if (px0 + cwid > chipsMax && px0 > chipsX0) {
                    ry += 16;
                    px0 = chipsX0;
                }
                String target = n;
                pill(context, px0, ry, label, false, () -> {
                    FriendManager.add(target);
                    friendMsg(target + " добавлен в друзья", false);
                });
                px0 += cwid + 4;
            }
        }
        return h;
    }

    private int drawConfigCard(DrawContext context, int x, int y, int w) {
        List<Config.Entry> list = Config.list();
        int rowH = 22;
        int h = 28 + 18 + 12 + 4 + Math.max(24, list.size() * rowH) + 4;
        rect(context, x, y, w, h, 7, CARD);
        NuxIcons.draw(context, ICON_CONFIGS, x + 8, y + 9, 1, ACCENT_LIGHT);
        text(context, "Конфиги", x + 20, y + 8, TEXT);
        String act = Config.active().isEmpty() ? "" : "активный: " + Config.active();
        text(context, act, x + 20 + width("Конфиги") + 8, y + 8, DIM);

        String folder = "Папка";
        int fw0 = width(folder) + 10;
        pill(context, x + w - 6 - fw0, y + 6, folder, false, Config::openFolder);
        context.fill(x + 8, y + 24, x + w - 8, y + 25, LINE);

        // форма: название + «Сохранить»
        int fx = x + 8;
        int fy = y + 29;
        String saveLabel = "Сохранить";
        int sw = width(saveLabel) + 10;
        field(context, fx, fy, w - 16 - sw - 4, cfgName, "Название нового конфига", 5);
        pill(context, x + w - 8 - sw, fy, saveLabel, true, this::saveConfigFromForm);
        fy += 17;
        if (cfgMessage != null && System.currentTimeMillis() - cfgMessageTime < 3000) {
            text(context, cfgMessage, fx, fy, cfgError ? 0xFFFF6B6B : ACCENT_LIGHT);
        }

        int ry = y + 28 + 18 + 12 + 4;
        context.fill(x + 8, ry - 3, x + w - 8, ry - 2, LINE);
        if (list.isEmpty()) {
            text(context, "Пока нет конфигов — введи название и нажми «Сохранить»", x + 8, ry + 6, DIM);
            return h;
        }
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm");
        for (Config.Entry e : list) {
            boolean current = e.name().equals(Config.active());
            boolean rowHover = hover(x + 4, ry, x + w - 4, ry + rowH - 2);
            if (current) {
                rect(context, x + 4, ry, w - 8, rowH - 2, 5, withAlpha(ACCENT, 0x22));
                rect(context, x + 4, ry + 5, 2, 10, 1, ACCENT);
            } else if (rowHover) {
                rect(context, x + 4, ry, w - 8, rowH - 2, 5, 0x0CFFFFFF);
            }
            text(context, e.name(), x + 12, ry + 2, current ? ACCENT_LIGHT : TEXT);
            text(context, (current ? "активный · " : "") + fmt.format(new java.util.Date(e.modified())),
                    x + 12, ry + 11, DIM);

            // кнопки справа: Загрузить, Сохранить (перезаписать), ×
            String name = e.name();
            int bx = x + w - 10;
            boolean confirm = name.equals(cfgDelete);
            String del = confirm ? "Удалить?" : "×";
            int dw = width(del) + (confirm ? 10 : 6);
            bx -= dw;
            boolean delHover = hover(bx, ry + 4, bx + dw, ry + 17);
            if (confirm) {
                rect(context, bx, ry + 4, dw, 13, 4, delHover ? 0xFFE5484D : 0xFFB83A3E);
                text(context, del, bx + 5, ry + 7, 0xFFFFFFFF);
            } else {
                text(context, del, bx + 2, ry + 6, delHover ? 0xFFFF6B6B : MUTED);
            }
            hit(bx, ry + 4, bx + dw, ry + 17, () -> {
                if (name.equals(cfgDelete)) {
                    Config.delete(name);
                    cfgDelete = null;
                    cfgMsg("Конфиг «" + name + "» удалён", false);
                } else {
                    cfgDelete = name;
                }
            }, null, null);

            String over = "Сохранить";
            bx -= width(over) + 10 + 4;
            pill(context, bx, ry + 4, over, false, () -> {
                if (Config.saveAs(name)) {
                    cfgMsg("Конфиг «" + name + "» перезаписан", false);
                }
            });
            String load = "Загрузить";
            bx -= width(load) + 10 + 4;
            pill(context, bx, ry + 4, load, true, () -> {
                if (Config.loadConfig(name)) {
                    cfgMsg("Конфиг «" + name + "» загружен", false);
                } else {
                    cfgMsg("Не удалось загрузить «" + name + "»", true);
                }
            });
            ry += rowH;
        }
        return h;
    }

    private int drawWaypointCard(DrawContext context, int x, int y, int w) {
        List<Waypoint> list = WaypointManager.current();
        int rowH = 24;
        int listH = 0;
        for (Waypoint wp : list) {
            listH += rowH + (colorEdit == wp ? 20 : 0);
        }
        int formH = 14 + 4 + 14 + 4 + 15 + 12;
        int h = 28 + formH + 4 + Math.max(24, listH) + 4;
        rect(context, x, y, w, h, 7, CARD);
        NuxIcons.draw(context, ICON_WAYPOINT, x + 8, y + 9, 1, ACCENT_LIGHT);
        text(context, "Точки", x + 20, y + 8, TEXT);
        text(context, String.valueOf(list.size()), x + 20 + width("Точки") + 5, y + 8, DIM);

        String add = "+ Здесь";
        int aw = width(add) + 10;
        int ax = x + w - 6 - aw;
        boolean addHover = hover(ax, y + 5, ax + aw, y + 19);
        rect(context, ax, y + 5, aw, 14, 5, addHover ? ACCENT_LIGHT : ACCENT);
        text(context, add, ax + 5, y + 8, 0xFFFFFFFF);
        hit(ax, y + 5, ax + aw, y + 19, WaypointManager::addHere, null, null);
        context.fill(x + 8, y + 24, x + w - 8, y + 25, LINE);

        // форма: название + координаты
        int fx = x + 8;
        int fw = w - 16;
        int fy = y + 29;
        field(context, fx, fy, fw, wpName, "Название", 1);
        fy += 18;
        int cw = (fw - 8) / 3;
        field(context, fx, fy, cw, wpX, "X", 2);
        field(context, fx + cw + 4, fy, cw, wpY, "Y", 3);
        field(context, fx + (cw + 4) * 2, fy, fw - (cw + 4) * 2, wpZ, "Z", 4);
        fy += 18;
        boolean btnHover = hover(fx, fy, fx + fw, fy + 15);
        rect(context, fx, fy, fw, 15, 5, btnHover ? FIELD_HOVER : FIELD);
        String btn = "Поставить по координатам";
        text(context, btn, fx + fw / 2 - width(btn) / 2, fy + 4, btnHover ? TEXT : SOFT);
        hit(fx, fy, fx + fw, fy + 15, this::addFromForm, null, null);
        fy += 17;
        if (wpMessage != null && System.currentTimeMillis() - wpMessageTime < 3000) {
            text(context, wpMessage, fx, fy, wpMessage.startsWith("Точка") ? ACCENT_LIGHT : 0xFFFF6B6B);
        }

        int ry = y + 28 + formH + 4;
        context.fill(x + 8, ry - 3, x + w - 8, ry - 2, LINE);
        if (list.isEmpty()) {
            text(context, "Пока нет точек (клавиша B — точка здесь)", x + 8, ry + 6, DIM);
            return h;
        }
        String dim = WaypointManager.dimension();
        for (Waypoint wp : new ArrayList<>(list)) {
            boolean here = wp.dimension.equals(dim);
            int color = 0xFF000000 | wp.color;
            boolean rowHover = hover(x + 4, ry, x + w - 4, ry + rowH - 2);
            if (rowHover) {
                rect(context, x + 4, ry, w - 8, rowH - 2, 5, 0x0CFFFFFF);
            }
            // кружок цвета — открывает выбор цвета
            boolean dotHover = hover(x + 6, ry + 3, x + 18, ry + 15);
            NuxShapes.circle(context, x + 12, ry + 9, dotHover ? 5 : 4, wp.visible ? color : 0xFF3A4255);
            hit(x + 6, ry + 3, x + 18, ry + 15, () -> colorEdit = colorEdit == wp ? null : wp, null, null);

            text(context, wp.name, x + 22, ry + 3, wp.visible ? TEXT : MUTED);
            String info = wp.x + " " + wp.y + " " + wp.z;
            if (!here) {
                info += "  · " + WaypointManager.dimensionName(wp.dimension);
            } else if (this.client.player != null) {
                double d = Math.sqrt(this.client.player.squaredDistanceTo(wp.x + 0.5, wp.y, wp.z + 0.5));
                info += "  · " + Math.round(d) + " м";
            }
            text(context, info, x + 22, ry + 12, DIM);

            int delX = x + w - 14;
            boolean delHover = hover(delX - 2, ry + 2, delX + 8, ry + 14);
            text(context, "×", delX, ry + 4, delHover ? 0xFFFF6B6B : MUTED);
            hit(delX - 2, ry + 2, delX + 8, ry + 14, () -> {
                WaypointManager.remove(wp);
                if (colorEdit == wp) {
                    colorEdit = null;
                }
            }, null, null);

            int eyeX = delX - 13;
            boolean eyeHover = hover(eyeX - 2, ry + 2, eyeX + 9, ry + 14);
            NuxIcons.draw(context, ICON_EYE, eyeX, ry + 5, 1, wp.visible ? (eyeHover ? TEXT : SOFT) : DIM);
            hit(eyeX - 2, ry + 2, eyeX + 9, ry + 14, () -> {
                wp.visible = !wp.visible;
                WaypointManager.save();
            }, null, null);
            ry += rowH;

            if (colorEdit == wp) {
                int sx = x + 14;
                for (int preset : WP_COLORS) {
                    boolean sel = (wp.color & 0xFFFFFF) == preset;
                    boolean hov = hover(sx - 5, ry, sx + 5, ry + 10);
                    NuxShapes.circle(context, sx, ry + 4, hov || sel ? 4 : 3, 0xFF000000 | preset);
                    if (sel) {
                        NuxShapes.circle(context, sx, ry + 4, 1, 0xFFFFFFFF);
                    }
                    int p = preset;
                    hit(sx - 5, ry - 1, sx + 5, ry + 10, () -> {
                        wp.color = p;
                        WaypointManager.save();
                    }, null, null);
                    sx += 11;
                }
                int hx = sx + 2;
                int hw = x + w - 10 - hx;
                if (hw > 10) {
                    NuxShapes.hueBar(context, hx, ry + 2, hw, 5);
                    hit(hx - 2, ry - 1, hx + hw + 2, ry + 10, null, null, (mx, my) -> {
                        float hue = (float) Math.max(0, Math.min(1, (mx - hx) / (double) (hw - 1)));
                        wp.color = RenderUtil.hsv(hue, 0.7f, 1f) & 0xFFFFFF;
                        WaypointManager.save();
                    });
                }
                ry += 20;
            }
        }
        return h;
    }

    private void drawDropdown(DrawContext context, int px, int py, int w, int h) {
        if (dropdown == null) {
            return;
        }
        String[] modes = dropdown.modes();
        int rowH = 13;
        int listH = modes.length * rowH + 4;
        int top = ddY;
        if (top + listH > py + h - 4) {
            top = ddBoxY - listH - 1;
        }
        // клик мимо списка закрывает его
        hits.add(new Hit(0, 0, this.width, this.height, () -> dropdown = null, () -> dropdown = null, null));
        rect(context, ddX - 1, top - 1, ddW + 2, listH + 2, 6, 0x30FFFFFF);
        rect(context, ddX, top, ddW, listH, 5, 0xFF1C2333);
        ModeSetting mode = dropdown;
        for (int i = 0; i < modes.length; i++) {
            int ry = top + 2 + i * rowH;
            boolean hov = mouseX >= ddX && mouseX < ddX + ddW && mouseY >= ry && mouseY < ry + rowH;
            if (hov) {
                rect(context, ddX + 2, ry, ddW - 4, rowH, 4, 0x18FFFFFF);
            }
            boolean cur = i == mode.index();
            text(context, modes[i], ddX + 7, ry + 3, cur ? ACCENT_LIGHT : SOFT);
            if (cur) {
                NuxIcons.draw(context, ICON_CHECK, ddX + ddW - 12, ry + 4, 1, ACCENT_LIGHT);
            }
            int index = i;
            hits.add(new Hit(ddX, ry, ddX + ddW, ry + rowH, () -> {
                mode.set(index);
                dropdown = null;
            }, null, null));
        }
    }

    // ---------- ввод ----------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if ((binding != null || bindingSetting != null) && button >= 2) {
            int code = Keys.MOUSE_OFFSET + button;
            if (binding != null) {
                binding.setKey(code);
            } else {
                bindingSetting.set(code);
            }
            binding = null;
            bindingSetting = null;
            return true;
        }
        if(button==2&&hoverModule!=null){binding=hoverModule;bindingSetting=null;textSetting=null;focusedField=0;dropdown=null;picker=null;drag=null;searchFocused=false;return true;}
        searchFocused = false;
        focusedField = 0;
        bindingSetting = null;
        textSetting = null;
        Module wasBinding = binding;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit hit = hits.get(i);
            if (!hit.contains(mx, my)) {
                continue;
            }
            if (wasBinding != null && binding == wasBinding) {
                binding = null;
            }
            if (button == 0) {
                if (hit.drag() != null) {
                    drag = hit.drag();
                    drag.apply(mx, my);
                } else if (hit.left() != null) {
                    hit.left().run();
                }
            } else if (button == 1 && hit.right() != null) {
                hit.right().run();
            }
            return true;
        }
        binding = null;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double deltaX, double deltaY) {
        if (drag != null) {
            drag.apply(mx, my);
            return true;
        }
        return super.mouseDragged(mx, my, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        drag = null;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontalAmount, double verticalAmount) {
        dropdown = null;
        scroll = Math.max(0, Math.min(maxScroll, scroll - verticalAmount * 26));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (bindingSetting != null) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
                bindingSetting.set(-1);
            } else if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
                bindingSetting.set(keyCode);
            }
            bindingSetting = null;
            return true;
        }
        if (textSetting != null) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                String v = textSetting.get();
                textSetting.set((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 ? "" : dropLast(v));
            } else if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER
                    || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_TAB) {
                textSetting = null;
            } else if (keyCode == GLFW.GLFW_KEY_V && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                textSetting.set(textSetting.get() + this.client.keyboard.getClipboard().replaceAll("[\\r\\n]", ""));
            }
            return true;
        }
        if (focusedField != 0) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                setField(focusedField, dropLast(getField(focusedField)));
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_TAB) {
                if (focusedField <= 4) {
                    focusedField = focusedField % 4 + 1;
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                if (focusedField == 5) {
                    saveConfigFromForm();
                } else if (focusedField == 6) {
                    addFriendFromForm();
                } else {
                    addFromForm();
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                focusedField = 0;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_V && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                for (char c : this.client.keyboard.getClipboard().toCharArray()) {
                    typeField(c);
                }
                return true;
            }
            return true;
        }
        if (binding != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
                binding.setKey(-1);
            } else {
                binding.setKey(keyCode);
            }
            binding = null;
            return true;
        }
        if(keyCode==GLFW.GLFW_KEY_F&&(modifiers&GLFW.GLFW_MOD_CONTROL)!=0&&binding==null){searchFocused=true;return true;}
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!search.isEmpty()) {
                    search = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0 ? "" : search.substring(0, search.length() - 1);
                    scroll = 0;
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_TAB) {
                searchFocused = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_V && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                String clip = this.client.keyboard.getClipboard().replaceAll("[\\r\\n]", "");
                search = (search + clip).substring(0, Math.min(24, (search + clip).length()));
                return true;
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (dropdown != null) {
                dropdown = null;
                return true;
            }
            if (picker != null) {
                picker = null;
                return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT && !searchFocused) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (binding != null || bindingSetting != null) {
            return true;
        }
        if (textSetting != null) {
            if (chr >= 32 && chr != 127) {
                textSetting.set(textSetting.get() + chr);
            }
            return true;
        }
        if (focusedField != 0) {
            typeField(chr);
            return true;
        }
        if (searchFocused && chr >= 32 && chr != 127 && search.length() < 24) {
            // Search accepts text only after explicitly clicking the search box.
            search += chr;
            scroll = 0;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    private String getField(int id) {
        return switch (id) {
            case 1 -> wpName;
            case 5 -> cfgName;
            case 6 -> friendName;
            case 2 -> wpX;
            case 3 -> wpY;
            default -> wpZ;
        };
    }

    private void setField(int id, String value) {
        switch (id) {
            case 1 -> wpName = value;
            case 5 -> cfgName = value;
            case 6 -> friendName = value;
            case 2 -> wpX = value;
            case 3 -> wpY = value;
            default -> wpZ = value;
        }
    }

    private static String dropLast(String s) {
        return s.isEmpty() ? s : s.substring(0, s.length() - 1);
    }

    // в координаты — только цифры и минус, в название — любой текст
    private void typeField(char c) {
        String v = getField(focusedField);
        if (focusedField == 5) {
            if ((Character.isLetterOrDigit(c) || c == ' ' || c == '-' || c == '_') && v.length() < 24) {
                setField(5, v + c);
            }
            return;
        }
        if (focusedField == 6) {
            if ((Character.isLetterOrDigit(c) && c < 128 || c == '_') && v.length() < 16) {
                setField(6, v + c);
            }
            return;
        }
        if (focusedField == 1) {
            if (c >= 32 && v.length() < 20) {
                setField(1, v + c);
            }
        } else if ((Character.isDigit(c) || (c == '-' && v.isEmpty())) && v.length() < 9) {
            setField(focusedField, v + c);
        }
    }

    @Override
    public void removed() {
        Config.save();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // ---------- совместимость: фигуры для других экранов ----------

    static void roundedRect(DrawContext context, int x, int y, int width, int height, int radius, int color) {
        NuxShapes.roundedRect(context, x, y, width, height, radius, color);
    }

    static void circle(DrawContext context, int centerX, int centerY, int radius, int color) {
        NuxShapes.circle(context, centerX, centerY, radius, color);
    }

    static int blend(int from, int to, float t) {
        return NuxShapes.blend(from, to, t);
    }
}
