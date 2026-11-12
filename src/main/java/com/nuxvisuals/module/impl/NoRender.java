package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.BoolSetting;

/** Отключает отдельные элементы экрана и мира. Каждый пункт включается отдельно. */
public class NoRender extends Module {
    public static NoRender INSTANCE;

    // экран
    public final BoolSetting fire = add(new BoolSetting("Fire Overlay", true));
    public final BoolSetting water = add(new BoolSetting("Underwater Overlay", false));
    public final BoolSetting inWall = add(new BoolSetting("In-Wall Overlay", true));
    public final BoolSetting pumpkin = add(new BoolSetting("Pumpkin Overlay", true));
    public final BoolSetting powderSnow = add(new BoolSetting("Powder Snow Overlay", true));
    public final BoolSetting vignette = add(new BoolSetting("Vignette", true));
    public final BoolSetting portal = add(new BoolSetting("Portal/Nausea", true));
    public final BoolSetting totem = add(new BoolSetting("Totem Animation", false));
    // HUD
    public final BoolSetting bossBar = add(new BoolSetting("Boss Bar", false));
    public final BoolSetting scoreboard = add(new BoolSetting("Scoreboard", false));
    public final BoolSetting titles = add(new BoolSetting("Titles", false));
    public final BoolSetting actionBar = add(new BoolSetting("Action Bar", false));
    // мир
    public final BoolSetting weather = add(new BoolSetting("Rain/Snow", true));
    public final BoolSetting weatherParticles = add(new BoolSetting("Rain Particles & Sound", true));
    public final BoolSetting blindness = add(new BoolSetting("Blindness/Darkness Fog", true));

    public NoRender() {
        super("no-render", "NoRender", Category.Visuals, false);
        INSTANCE = this;
    }

    /** true, если модуль включён и выбранный пункт нужно скрыть. */
    public static boolean hide(java.util.function.Function<NoRender, BoolSetting> option) {
        NoRender m = INSTANCE;
        return m != null && m.isEnabled() && option.apply(m).get();
    }
}
