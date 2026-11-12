package com.nuxvisuals.module.impl;

import com.nuxvisuals.hud.HudIcons;
import com.nuxvisuals.hud.HudModule;
import com.nuxvisuals.hud.HudRender;
import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.ModuleManager;
import com.nuxvisuals.module.setting.BoolSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.Sprite;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Активные эффекты (бафы/дебафы): название, уровень, оставшееся время и полоска
public class PotionHud extends HudModule {
    private final BoolSetting hideVanilla = add(new BoolSetting("Hide Vanilla Icons", true));
    private final BoolSetting progress = add(new BoolSetting("Progress Bar", true));
    private final BoolSetting icons = add(new BoolSetting("Icons", true));
    private final BoolSetting effectColors = add(new BoolSetting("Effect Colors", true));

    private final Map<RegistryEntry<StatusEffect>, Integer> maxDuration = new HashMap<>();
    private final List<StatusEffectInstance> effects = new ArrayList<>();

    public PotionHud() {
        super("potions", "Effects", Category.Hud, true, 0.0, 0.3);
    }

    public static boolean hidesVanilla() {
        PotionHud hud = (PotionHud) ModuleManager.get("potions");
        return hud != null && hud.isEnabled() && hud.hideVanilla.get();
    }

    @Override
    protected boolean shouldShow(boolean editing) {
        return editing || !mc().player.getStatusEffects().isEmpty();
    }

    private static String roman(int level) {
        String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return level >= 0 && level < r.length ? r[level] : String.valueOf(level);
    }

    private static String name(StatusEffectInstance effect) {
        String name = effect.getEffectType().value().getName().getString();
        return effect.getAmplifier() > 0 ? name + " " + (effect.getAmplifier() + 1) : name;
    }

    private static String time(StatusEffectInstance effect) {
        if (effect.isInfinite()) {
            return "∞";
        }
        int seconds = effect.getDuration() / 20;
        if (seconds >= 3600) {
            return String.format("%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60);
        }
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    private static final int ROW = 26;

    private static String level(StatusEffectInstance e) {
        return e.getAmplifier() > 0 ? roman(e.getAmplifier() + 1) : "";
    }

    private static String baseName(StatusEffectInstance e) {
        return e.getEffectType().value().getName().getString();
    }

    @Override
    protected void measure(MinecraftClient client, boolean editing) {
        effects.clear();
        effects.addAll(client.player.getStatusEffects());
        effects.sort(Comparator.comparing((StatusEffectInstance e) -> !e.getEffectType().value().isBeneficial())
                .thenComparing(e -> -e.getDuration()));
        maxDuration.keySet().removeIf(type -> effects.stream().noneMatch(e -> e.getEffectType().equals(type)));
        for (StatusEffectInstance e : effects) {
            maxDuration.merge(e.getEffectType(), e.getDuration(), Math::max);
        }
        int w = 21 + textWidth("Effects") + 30;
        for (StatusEffectInstance e : effects) {
            String lv = level(e);
            int line = textWidth(baseName(e)) + (lv.isEmpty() ? 0 : textWidth(lv) + 10);
            w = Math.max(w, 34 + Math.max(line, textWidth(time(e)) + 20) + 14);
        }
        width = Math.max(130, w);
        height = 24 + Math.max(1, effects.size()) * (ROW + 3) + 3;
    }

    // Иконка из отдельного ванильного атласа status effects, не GUI-атласа.
    private static void icon(DrawContext context, StatusEffectInstance e, int x, int y, int size) {
        // Status effects have their own atlas; they are NOT GUI atlas sprites.
        Sprite sprite = mc().getStatusEffectSpriteManager().getSprite(e.getEffectType());
        context.drawSpriteStretched(RenderLayer::getGuiTextured, sprite, x, y, size, size);
    }

    @Override
    protected void draw(DrawContext context, MinecraftClient client, float tickDelta, boolean editing) {
        panel(context, 0, 0, width, height);
        header(context, HudIcons.FLASK, "Effects");
        String count = String.valueOf(effects.size());
        if (!effects.isEmpty()) {
            int cw = Math.max(12, textWidth(count) + 6);
            HudRender.rect(context, width - 8 - cw, 5, cw, 11, 5, alpha(accentColor(0), 0.25f));
            text(context, count, width - 8 - cw / 2 - textWidth(count) / 2, 7, 0xFFFFFFFF);
        }
        if (effects.isEmpty()) {
            text(context, "нет активных эффектов", 8, 26, 0xFF5E6678);
            return;
        }
        int y = 23;
        int cardW = width - 12;
        long now = System.currentTimeMillis();
        for (int i = 0; i < effects.size(); i++) {
            StatusEffectInstance e = effects.get(i);
            StatusEffect type = e.getEffectType().value();
            int color = effectColors.get() ? 0xFF000000 | type.getColor() : accentColor(i * 0.08f);
            boolean bad = !type.isBeneficial();
            boolean ending = !e.isInfinite() && e.getDuration() < 200;
            float pulse = ending ? 0.5f + 0.5f * (float) Math.sin(now / 150.0) : 0f;

            // карточка с лёгким оттенком цвета эффекта
            HudRender.rect(context, 6, y, cardW, ROW, 6, 0x14FFFFFF);
            HudRender.rect(context, 6, y, cardW, ROW, 6, alpha(color, 0.08f + 0.12f * pulse));
            // полоска-акцент слева
            HudRender.rect(context, 6, y + 5, 2, ROW - 10, 1, color);

            // иконка в цветном квадрате
            HudRender.rect(context, 11, y + 4, 18, 18, 5, alpha(color, 0.28f));
            if (icons.get()) {
                icon(context, e, 12, y + 5, 16);
            } else {
                HudRender.circle(context, 20, y + 13, 3, color);
            }

            // название + уровень
            int tx = 34;
            text(context, baseName(e), tx, y + 4, bad ? 0xFFFFB4B4 : 0xFFF1F4FA);
            String lv = level(e);
            if (!lv.isEmpty()) {
                int lx = tx + textWidth(baseName(e)) + 4;
                int lw = textWidth(lv) + 6;
                HudRender.rect(context, lx, y + 3, lw, 10, 4, alpha(color, 0.45f));
                text(context, lv, lx + 3, y + 4, 0xFFFFFFFF);
            }
            // время
            String t = time(e);
            int timeColor = ending ? NuxShapesBridge.blend(0xFFE8ECF5, 0xFFFF6B6B, pulse) : 0xFF9AA4B8;
            text(context, t, tx, y + 14, timeColor);

            // прогресс: тонкая полоса в нижней части карточки
            if (progress.get()) {
                int barX = tx + textWidth(t) + 6;
                int barW = 6 + cardW - barX - 6;
                float k = e.isInfinite() ? 1f : Math.max(0f, Math.min(1f, e.getDuration() / (float) Math.max(1, maxDuration.getOrDefault(e.getEffectType(), e.getDuration()))));
                if (barW > 8) {
                    HudRender.rect(context, barX, y + 17, barW, 3, 1, 0x26FFFFFF);
                    HudRender.rect(context, barX, y + 17, Math.max(3, Math.round(barW * k)), 3, 1, color);
                }
            }
            y += ROW + 3;
        }
    }

    // доступ к смешиванию цветов
    private static final class NuxShapesBridge {
        static int blend(int a, int b, float t) {
            return com.nuxvisuals.ui.NuxShapes.blend(a, b, t);
        }
    }
}
