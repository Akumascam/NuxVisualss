package com.nuxvisuals.module.impl;

import com.nuxvisuals.hud.HudIcons;
import com.nuxvisuals.hud.HudModule;
import com.nuxvisuals.module.Category;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Перезарядки предметов (эндер-жемчуг, плод хоруса, щит и т.д.)
public class Cooldowns extends HudModule {
    private record Row(ItemStack stack, String name, String time) {
    }

    private record Start(long time, float progress) {
    }

    private final List<Row> rows = new ArrayList<>();
    private final Map<Item, Start> starts = new HashMap<>();

    public Cooldowns() {
        super("cooldowns", "Cooldowns", Category.Hud, true, 0.0, 0.55);
    }

    private void collect(PlayerEntity player) {
        rows.clear();
        if (player == null) return;
        Set<net.minecraft.util.Identifier> seen = new HashSet<>();
        ItemCooldownManager manager = player.getItemCooldownManager();
        for (int i=0;i<player.getInventory().size();i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            int ticks=com.nuxvisuals.module.CooldownInfo.remainingTicks(manager,stack);
            if(ticks<=0 || !seen.add(manager.getGroup(stack))) continue;
            rows.add(new Row(stack,stack.getName().getString(),com.nuxvisuals.module.CooldownInfo.format(ticks)+"с"));
        }
    }

    @Override
    protected boolean shouldShow(boolean editing) {
        collect(mc().player);
        return editing || !rows.isEmpty();
    }

    @Override
    protected void measure(MinecraftClient client, boolean editing) {
        int w = 21 + textWidth("Cooldowns") + 30;
        for (Row r : rows) {
            w = Math.max(w, 21 + textWidth(r.name()) + 16 + textWidth(r.time()) + 8);
        }
        width = Math.max(110, w);
        height = 24 + Math.max(1, rows.size()) * 12 + 3;
    }

    @Override
    protected void draw(DrawContext context, MinecraftClient client, float tickDelta, boolean editing) {
        panel(context, 0, 0, width, height);
        header(context, HudIcons.CLOCK, "Cooldowns");
        if (rows.isEmpty()) {
            text(context, "нет перезарядок", 8, 25, 0xFF5E6678);
            return;
        }
        int y = 25;
        for (Row r : rows) {
            context.getMatrices().push();
            context.getMatrices().translate(7, y - 1, 0);
            context.getMatrices().scale(0.5f, 0.5f, 1f);
            context.drawItem(r.stack(), 0, 0);
            context.getMatrices().pop();
            text(context, r.name(), 21, y, 0xFFE8ECF5);
            text(context, r.time(), width - 8 - textWidth(r.time()), y, 0xFFE8ECF5);
            y += 12;
        }
    }
}
