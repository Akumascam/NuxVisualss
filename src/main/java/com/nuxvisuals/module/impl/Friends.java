package com.nuxvisuals.module.impl;

import com.nuxvisuals.friend.FriendManager;
import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.ModuleManager;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.ColorSetting;
import com.nuxvisuals.module.setting.ModeSetting;
import com.nuxvisuals.module.setting.SliderSetting;
import net.minecraft.entity.player.PlayerEntity;

// Друзья: на моделях игроков из списка друзей рисуются заметные 3D-крылья (видишь только ты)
public class Friends extends Module {
    public final BoolSetting wings = add(new BoolSetting("Wings", true));
    public final ModeSetting type = add(new ModeSetting("Wings Type", "Angel",
            "Angel", "Demon", "Butterfly", "Phoenix", "Crystal"));
    public final BoolSetting customColor = add(new BoolSetting("Custom Color", true));
    public final ColorSetting color = add(new ColorSetting("Wings Color", 0.38f, 0.75f));
    public final BoolSetting glow = add(new BoolSetting("Glow", true));
    public final SliderSetting size = add(new SliderSetting("Size", 1.2, 0.6, 2.0, 0.1));

    public Friends() {
        super("friends", "Friends", Category.Friends, true);
    }

    public static Friends get() {
        Module m = ModuleManager.get("friends");
        return m instanceof Friends f ? f : null;
    }

    // рисовать ли крылья на этом игроке
    public static boolean wingsFor(PlayerEntity player) {
        Friends f = get();
        return f != null && f.isEnabled() && f.wings.get()
                && FriendManager.isFriend(player.getGameProfile().getName());
    }

    public String wingsId() {
        return switch (type.get()) {
            case "Demon" -> "wings:demon";
            case "Butterfly" -> "wings:butterfly";
            case "Phoenix" -> "wings:phoenix";
            case "Crystal" -> "wings:crystal";
            default -> "wings:angel";
        };
    }
}
