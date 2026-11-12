package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.ModeSetting;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.SimpleOption;

// Оптимизация «в один клик».
// Режимы: Auto — сам следит за FPS и включает твики ступенями, пока не станет плавно;
//         Balanced — заметный прирост без потери картинки; Max FPS — всё на максимум скорости.
// Всё, что поменяли, возвращается при выключении модуля. Если стоит Sodium — работаем вместе с ним.
public class Optimization extends Module {
    private final ModeSetting mode = add(new ModeSetting("Mode", "Auto", "Auto", "Balanced", "Max FPS"));
    private final BoolSetting background = add(new BoolSetting("Save FPS In Background", true));

    public static Optimization INSTANCE;

    /** 0 — выкл, 1 — лёгкий, 2 — средний, 3 — максимум. */
    private int tier;
    private int autoTier = 1;
    private long tierChangedAt;
    private long fpsWindowStart;
    private int fpsSum, fpsSamples;

    // исходные настройки игрока
    private boolean saved;
    private GraphicsMode graphics0;
    private CloudRenderMode clouds0;
    private boolean shadows0, ao0, vsync0, fpsLimited;
    private Object particles0;
    private Object[] particleValues;
    private int biomeBlend0, view0, maxFps0;
    private double entityDistance0, distortion0;

    public Optimization() {
        super("optimization", "Optimization", Category.Visuals, true);
        INSTANCE = this;
    }

    public static boolean sodium() {
        return FabricLoader.getInstance().isModLoaded("sodium");
    }

    private static int level() {
        return INSTANCE != null && INSTANCE.isEnabled() ? INSTANCE.tier : 0;
    }

    public static boolean fastGui() {
        return level() >= 1;
    }

    public static boolean lightMenu() {
        return level() >= 2;
    }

    public static double entityCullSq() {
        int t = level();
        double r = t >= 3 ? 40 : t == 2 ? 56 : t == 1 ? 72 : -1;
        return r < 0 ? -1 : r * r;
    }

    public static double blockEntityCullSq() {
        int t = level();
        double r = t >= 3 ? 24 : t == 2 ? 36 : t == 1 ? 48 : -1;
        return r < 0 ? -1 : r * r;
    }

    /** Текущая ступень для подписи в меню. */
    public String status() {
        return switch (tier) { case 3 -> "максимум"; case 2 -> "средний"; case 1 -> "лёгкий"; default -> "выкл"; };
    }

    @Override
    public void onEnable() {
        autoTier = 1;
        tierChangedAt = System.currentTimeMillis();
        fpsWindowStart = 0;
    }

    @Override
    public void onTick(MinecraftClient client) {
        GameOptions o = client.options;
        if (!saved) {
            graphics0 = o.getGraphicsMode().getValue();
            clouds0 = o.getCloudRenderMode().getValue();
            shadows0 = o.getEntityShadows().getValue();
            particles0 = o.getParticles().getValue();
            particleValues = ((Enum<?>) particles0).getDeclaringClass().getEnumConstants();
            ao0 = o.getAo().getValue();
            biomeBlend0 = o.getBiomeBlendRadius().getValue();
            entityDistance0 = o.getEntityDistanceScaling().getValue();
            vsync0 = o.getEnableVsync().getValue();
            distortion0 = o.getDistortionEffectScale().getValue();
            view0 = o.getViewDistance().getValue();
            maxFps0 = o.getMaxFps().getValue();
            saved = true;
        }

        tier = switch (mode.get()) {
            case "Max FPS" -> 3;
            case "Balanced" -> 1;
            default -> autoTier();
        };
        if (client.world == null) {
            tier = Math.min(tier, 1); // в меню ничего тяжёлого не трогаем
        }

        boolean reload = false;
        // 1 — лёгкий: то, что почти не видно, но много ест
        set(o.getEntityShadows(), tier >= 1 ? Boolean.FALSE : shadows0);
        set(o.getDistortionEffectScale(), tier >= 1 ? 0.0 : distortion0);
        set(o.getEnableVsync(), tier >= 1 ? Boolean.FALSE : vsync0);
        set(o.getCloudRenderMode(), tier >= 3 ? CloudRenderMode.OFF : tier >= 1 && clouds0 == CloudRenderMode.FANCY ? CloudRenderMode.FAST : clouds0);
        setParticles(o, tier >= 3 ? 2 : tier >= 1 ? 1 : -1);
        reload |= set(o.getBiomeBlendRadius(), tier >= 2 ? 0 : tier >= 1 ? Math.min(biomeBlend0, 1) : biomeBlend0);
        // 2 — средний
        reload |= set(o.getGraphicsMode(), tier >= 2 && graphics0 != GraphicsMode.FAST ? GraphicsMode.FAST : graphics0);
        set(o.getEntityDistanceScaling(), tier >= 3 ? Math.min(entityDistance0, 0.5) : tier >= 2 ? Math.min(entityDistance0, 0.75) : entityDistance0);
        // 3 — максимум
        reload |= set(o.getAo(), tier >= 3 ? Boolean.FALSE : ao0);
        set(o.getViewDistance(), tier >= 3 ? Math.min(view0, 8) : view0);

        // фон / свёрнутое окно — 30 FPS, меньше нагрев и пинг не страдает
        boolean limit = background.get() && !client.isWindowFocused();
        if (limit) {
            if (!fpsLimited) {
                maxFps0 = o.getMaxFps().getValue();
                fpsLimited = true;
            }
            set(o.getMaxFps(), Math.min(maxFps0, 30));
        } else if (fpsLimited) {
            set(o.getMaxFps(), maxFps0);
            fpsLimited = false;
        }

        if (reload && client.worldRenderer != null && client.world != null) {
            client.worldRenderer.reload();
        }
    }

    // Auto: каждые 4 секунды смотрим средний FPS. Ниже 55 — поднимаем ступень (не чаще раза в 8 с).
    // Если стабильно выше 160 на средней/максимальной — через 60 с пробуем ступень ниже.
    private int autoTier() {
        MinecraftClient mc = mc();
        long now = System.currentTimeMillis();
        if (mc.world == null || !mc.isWindowFocused() || mc.currentScreen != null) {
            fpsWindowStart = 0;
            return autoTier;
        }
        if (fpsWindowStart == 0) {
            fpsWindowStart = now; fpsSum = 0; fpsSamples = 0;
        }
        fpsSum += mc.getCurrentFps();
        fpsSamples++;
        if (now - fpsWindowStart >= 4000 && fpsSamples > 0) {
            int avg = fpsSum / fpsSamples;
            int cap = mc.options.getMaxFps().getValue();
            boolean capped = cap < 260 && avg >= cap - 5;
            if (avg < 55 && !capped && autoTier < 3 && now - tierChangedAt > 8000) {
                autoTier++;
                tierChangedAt = now;
            } else if (avg > 160 && autoTier > 1 && now - tierChangedAt > 60000) {
                autoTier--;
                tierChangedAt = now;
            }
            fpsWindowStart = now; fpsSum = 0; fpsSamples = 0;
        }
        return autoTier;
    }

    @Override
    public void onDisable() {
        if (!saved) {
            return;
        }
        MinecraftClient client = mc();
        GameOptions o = client.options;
        boolean reload = false;
        reload |= set(o.getGraphicsMode(), graphics0);
        set(o.getCloudRenderMode(), clouds0);
        set(o.getEntityShadows(), shadows0);
        setParticles(o, -1);
        reload |= set(o.getAo(), ao0);
        reload |= set(o.getBiomeBlendRadius(), biomeBlend0);
        set(o.getEntityDistanceScaling(), entityDistance0);
        set(o.getEnableVsync(), vsync0);
        set(o.getDistortionEffectScale(), distortion0);
        set(o.getViewDistance(), view0);
        if (fpsLimited) {
            set(o.getMaxFps(), maxFps0);
            fpsLimited = false;
        }
        saved = false;
        tier = 0;
        if (reload && client.worldRenderer != null && client.world != null) {
            client.worldRenderer.reload();
        }
    }

    // ParticlesMode: ALL, DECREASED, MINIMAL. index = -1 — вернуть исходное.
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void setParticles(GameOptions o, int index) {
        SimpleOption option = o.getParticles();
        int base = ((Enum<?>) particles0).ordinal();
        Object value = index < 0 ? particles0 : particleValues[Math.max(index, base)];
        if (value != null && !option.getValue().equals(value)) {
            option.setValue(value);
        }
    }

    private static <T> boolean set(SimpleOption<T> option, T value) {
        if (!option.getValue().equals(value)) {
            option.setValue(value);
            return true;
        }
        return false;
    }
}
