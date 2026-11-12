package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.ModuleManager;

// Камера не дёргается при получении урона
public class NoHurtCam extends Module {
    public NoHurtCam() {
        super("no-hurt-cam", "No Hurt Cam", Category.Visuals, false);
    }

    public static boolean active() {
        Module m = ModuleManager.get("no-hurt-cam");
        return m != null && m.isEnabled();
    }
}
