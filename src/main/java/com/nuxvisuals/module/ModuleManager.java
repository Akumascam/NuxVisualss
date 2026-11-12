package com.nuxvisuals.module;

import com.nuxvisuals.module.impl.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

import java.util.List;

public final class ModuleManager {
    public static final List<Module> MODULES = List.of(
            // Visuals
            new AspectRatio(),
            new BlockOverlay(),
            new Crosshair(),
            new ChinaHat(),
            new CustomHand(),
            new FullBright(),
            new Halo(),
            new HandChams(),
            new FunTimeHelper(),
            new HitboxCustomizer(),
            new ItemPhysics(),
            new HitColor(),
            new Highlighter(),
            new FreeLook(),
            new JumpCircles(),
            new NoRender(),
            new NoHurtCam(),
            new Particles(),
            new SwingAnimation(),
            new Optimization(),
            new TargetESP(),
            new Trajectories(),
            new WorldCustomizer(),
            new ShaderSky(),
            new WorldLyrics(),

            // Display (HUD)
            new Watermark(),
            new NotificationsModule(),
            new PotionHud(),
            new Hotkeys(),
            new TargetHud(),
            new Cooldowns(),
            new SlotCooldowns(),
            new ConsumablesHud(),
            new ArmorDisplay(),
            new InventoryHud(),
            new MusicHud(),

            // Utils
            new ClickGuiModule(),
            new MotionBlur(),
            new Protected(),
            new ChatHelper(),
            new AutoSprint(),
            new DiscordRPC(),
            new AutoJump(),
            new Zoom(),
            new HitSound(),
            new KillEffect(),
            new AntiAfk(),
            new AutoClicker(),
            new FastExp(),
            new TapeMouse(),
            new ItemScroller(),
            new HitDistance(),
            new AuctionRelister(),
            new ServerHelper(),
            new AutoSwap(),
            new FakePlayer(),
            new TotemTracker(),
            new ShiftTap(),
            new BindCommands(),
            new SlotLock(),
            new AutoAccept(),
            new ElytraSwap(),
            new LowArmor(),
            new AutoEat(),
            new AutoNear(),
            new ItemPickup(),

            // Waypoint
            new Waypoints(),

            // Friends
            new Friends()
    );

    private static final java.util.Map<String, Module> BY_ID;

    static {
        java.util.Map<String, Module> index = new java.util.HashMap<>();
        for (Module module : MODULES) {
            if (index.put(module.getId(), module) != null) {
                throw new IllegalStateException("Duplicate module: " + module.getId());
            }
        }
        BY_ID = java.util.Map.copyOf(index);
        String[][] descriptions = {
                {"shader-sky", "Анимированное GLSL-небо: аврора, туманность, галактика, закат, облака"},
                {"motion-blur", "Размытие движения: сила и сброс истории, без размытия HUD"},
                {"consumables", "Все расходники из инвентаря и второй руки, объединённые по типу и компонентам"},
                {"slot-cooldowns", "Оставшееся время перезарядки прямо на слотах: хотбар, инвентарь и контейнеры"},
                {"world-lyrics", "Текст песни вокруг игрока: LRC, синхронизация, LRCLIB (Windows)"},
                {"aspect-ratio", "Растягивает картинку мира, как растянутое разрешение"},
                {"custom-hand", "Меняет положение и размер руки от первого лица"},
                {"full-bright", "Полная яркость — видно даже в темноте"},
                {"hitbox", "Красивые хитбоксы сущностей своим цветом"},
                {"particles", "Снежинки, доллары, звёзды и 3D-кубики вокруг"},
                {"optimization", "Больше FPS в один клик: Auto сам подбирает настройки под твой ПК. Работает вместе с Sodium"},
                {"target-esp", "Призраки, кристаллы, ромб, молнии или рамка на цели"},
                {"world", "Время суток по-русски, погода и зима во всех биомах"},
                {"armor", "Броня с полосками прочности"},
                {"potions", "Эффекты с иконками, уровнем, временем и полоской"},
                {"watermark", "Плашка: клиент, ник, FPS и пинг"},
                {"hotkeys", "Модули, на которые назначены клавиши"},
                {"target-hud", "Голова, ник и здоровье вашей цели"},
                {"cooldowns", "Перезарядка жемчуга, хоруса и других предметов"},
                {"auto-sprint", "Бег включается сам при движении вперёд"},
                {"auto-jump", "Автопрыжок на блоки"},
                {"zoom", "Приближение на любую клавишу (по умолчанию C), колесо меняет силу"},
                {"click-gui", "Цвета меню и синхронизация HUD; режим бинда — в настройках каждого модуля"},
                {"hand-chams", "3D-руки и все предметы: перламутр, плазма и молнии"},
                {"funtime", "FunTime: размер трапки, пласта и радиусы предметов в руке"},
                {"trajectories", "Траектория стрел, трезубца, перки и зелий"},
                {"inventory", "Инвентарь на экране"},
                {"block-overlay", "Цветная подсветка блока под прицелом"},
                {"no-hurt-cam", "Камера не дёргается при уроне"},
                {"no-render", "Убирает огонь, дождь, боссбар, скорборд и другое"}, {"chat-helper", "Не очищает чат, схлопывает повторы и пишет координаты смерти"},
                {"alerts", "Уведомления при включении модулей"},
                {"hit-sound", "Звук при ударе по сущности"},
                {"kill-effect", "Молния или частицы при убийстве"},
                {"waypoints", "Как выглядят точки в мире: луч, подписи, расстояние"},
                {"china-hat", "Полупрозрачная китайская шляпа над головой"},
                {"halo", "Светящийся нимб над головой"},
                {"jump-circles", "Круги на земле при прыжке"},
                {"anti-afk", "Периодически прыгает, чтобы не кикнуло за АФК"},
                {"auto-clicker", "Кликает сам: свой CPS, ЛКМ/ПКМ, режим удержания"},
                {"fast-exp", "Пузырьки опыта бросаются быстрее, задержка настраивается"},
                {"tape-mouse", "Держит или кликает кнопку мыши сама (фермы, рыбалка)"},
                {"item-scroller", "Колесо и Shift+протяжка для быстрого перекладывания"},
                {"swing-animation", "Анимации взмаха: от себя, к себе, вращение, блок и др."},
                {"crosshair", "Свой прицел: как в PvP-паках, круг, точка или свой"},
                {"music", "Что играет в Spotify/браузере: обложка, трек, управление (только Windows)"},
                {"hit-distance", "Круг 3 блока под ногами: загорается, когда цель в зоне удара"},
                {"ah-relister", "FunTime: заходит в /ah, перевыставляет предметы и выходит"},
                {"server-helper", "FunTime: предмет и бинд без доп. задержки; HolyWorld: нажатие/удержание"},
                {"auto-swap", "Зажми бинд, выбери предмет в круге; любые предметы из инвентаря"},
                {"free-look", "Зажми Alt — камера крутится вокруг тебя, персонаж не поворачивается"},
                {"hit-color", "Цель окрашивается в цвет при крите или ударе"},
                {"highlighter", "Подсвечивает предметы в инвентаре, сундуках и на земле — свой цвет для каждой категории"},
                {"shift-tap", "Коротко нажимает Shift после критического удара"},
                {"bind-commands", "Бинд любого текста или команды на клавишу"},
                {"slot-lock", "Блокирует выброс и перекладывание слотов, мечей, тотемов и др."},
                {"auto-accept", "Сам принимает запросы на телепорт от друзей и/или игроков"},
                {"elytra-swap", "По клавише меняет нагрудник и элитры"},
                {"low-armor", "Предупреждает о низкой прочности брони на экране или в чате"},
                {"auto-eat", "Сам ест и запивает инвиз, скорость и силу"},
                {"auto-near", "Автоматически прописывает /near"},
                {"item-pickup", "Пишет в чат, какой донатный предмет ты подобрал"},
                {"totem-tracker", "Пишет в чат, когда у игрока слетает тотем: зачарованный или нет"},
                {"friends", "Крылья на друзьях: тип, цвет, свечение и размер"},
                {"fake-player", "Манекен-копия тебя для тренировки ударов и критов"},
                {"item-physics", "Предметы лежат на земле плашмя и кувыркаются в воздухе"},
        };
        for (String[] d : descriptions) {
            Module m = get(d[0]);
            if (m != null) {
                m.setDescription(d[1]);
            }
        }
    }

    public static Module get(String id) {
        return BY_ID.get(id);
    }

    public static void tick(MinecraftClient client) {
        boolean allowed = client.currentScreen == null && client.player != null && client.world != null && client.isWindowFocused();
        for (Module module : MODULES) module.updateBind(module.getKey() >= 0 && Keys.isDown(module.getKey()), allowed);
        for (Module module : MODULES) {
            if (module.isEnabled()) {
                module.onTick(client);
            }
        }
    }

    public static void render(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
        for (Module module : MODULES) {
            if (module.isEnabled()) {
                matrices.push();
                try {
                    module.onRender(matrices, consumers, camera, tickDelta);
                } finally {
                    matrices.pop();
                }
            }
        }
    }

    private ModuleManager() {
    }
}
