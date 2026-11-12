package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.ModeSetting;
import com.nuxvisuals.module.setting.SliderSetting;

// Растягивание мира. Само растягивание делает GameRendererMixin.
public class AspectRatio extends Module {
    public static AspectRatio INSTANCE;

    private static final String[] NAMES = {"Custom", "1:1", "5:4", "4:3", "16:10", "16:9"};
    private static final double[] VALUES = {0, 1.0, 1.25, 4.0 / 3.0, 1.6, 16.0 / 9.0};

    private final ModeSetting preset = add(new ModeSetting("Preset", "4:3", NAMES));
    private final SliderSetting ratio = add(new SliderSetting("Ratio", 4.0 / 3.0, 0.8, 2.5, 0.01));

    public AspectRatio() {
        super("aspect-ratio", "Aspect Ratio", Category.Visuals, false);
        INSTANCE = this;
        preset.onChange(() -> {
            if (preset.index() > 0) {
                ratio.setSilently(VALUES[preset.index()]);
            }
        });
        ratio.onChange(() -> {
            int match = 0;
            for (int i = 1; i < VALUES.length; i++) {
                if (Math.abs(VALUES[i] - ratio.get()) < 0.006) {
                    match = i;
                }
            }
            preset.setSilently(match);
        });
    }

    public static boolean isActive() {
        return INSTANCE != null && INSTANCE.isEnabled();
    }

    public static float get() {
        return INSTANCE.ratio.getFloat();
    }
}
