package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.ChatUtil;
import com.nuxvisuals.module.InvUtil;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.KeySetting;
import com.nuxvisuals.module.setting.SliderSetting;
import com.nuxvisuals.module.setting.StringSetting;
import net.minecraft.client.MinecraftClient;

// BindCommands: на любую клавишу — любой текст или команда (с "/" отправляется как команда)
public class BindCommands extends Module {
    private static final int MAX = 10;

    private final SliderSetting count = add(new SliderSetting("Binds", 3, 1, MAX, 1));
    private final SliderSetting cooldown = add(new SliderSetting("Cooldown", 10, 0, 60, 1, " tick"));
    private final BoolSetting notify = add(new BoolSetting("Notify", false));
    private final com.nuxvisuals.module.setting.ModeSetting triggerMode = add(new com.nuxvisuals.module.setting.ModeSetting(
            "Режим действий", "Нажатие", "Нажатие", "Удержание"));
    private final KeySetting[] keys = new KeySetting[MAX];
    private final StringSetting[] texts = new StringSetting[MAX];

    private int wait;

    public BindCommands() {
        super("bind-commands", "BindCommands", Category.Utilities, false);
        String[] defaults = {"/near", "/spawn", "/home"};
        for (int i = 0; i < MAX; i++) {
            final int n = i;
            keys[i] = add(new KeySetting("Bind " + (i + 1) + " Key", -1)
                    .visibleIf(() -> n < this.count.getInt()));
            texts[i] = add(new StringSetting("Bind " + (i + 1) + " Text", i < defaults.length ? defaults[i] : "", 256)
                    .visibleIf(() -> n < this.count.getInt()));
        }
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (wait > 0) {
            wait--;
        }
        int active = count.getInt();
        for (int i = 0; i < MAX; i++) {
            // pressed() вызываем у всех, чтобы правильно отслеживать отпускание
            boolean pressed = keys[i].trigger(triggerMode.is("Удержание"), Math.max(10, cooldown.getInt()));
            if (!pressed || i >= active || wait > 0 || client.player == null) {
                continue;
            }
            if (ChatUtil.send(texts[i].get())) {
                wait = cooldown.getInt();
                if (notify.get()) {
                    InvUtil.actionBar("\u00a7bОтправлено: \u00a7f" + texts[i].get());
                }
            }
        }
    }
}
