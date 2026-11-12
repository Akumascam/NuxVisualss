package com.nuxvisuals.module.impl;
import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.ModuleManager;
import com.nuxvisuals.module.setting.*;

public class ClickGuiModule extends Module {
    public static final int ACCENT = 0x5B8CFF;
    private final ColorSetting accent = add(new ColorSetting("Accent", 0.62f, 0.64f));
    private final ColorSetting panel = add(new ColorSetting("Panel", 0.62f, 0.45f, 0.12f));
    private final SliderSetting alpha = add(new SliderSetting("Panel Opacity", 0.85, 0.3, 1, 0.05));
    public final BoolSetting syncHud = add(new BoolSetting("Sync HUD Color", false));
    public ClickGuiModule() { super("click-gui", "Click GUI", Category.Utilities, true); }
    public static ClickGuiModule get() { return (ClickGuiModule) ModuleManager.get("click-gui"); }
    public int accentColor() { return accent.get() & 0xFFFFFF; }
    public int panelColor() {
        // Keep the panel dark enough for a readable, independent white text colour.
        return com.nuxvisuals.ui.NuxShapes.blend(0xFF10141E, panel.get() | 0xFF000000, 0.55f);
    }
    public int panelAlpha() { return Math.round(alpha.getFloat() * 255); }
    @Override public void setEnabled(boolean enabled) { super.setEnabled(true); }
}
