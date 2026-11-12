package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.ColorSetting;
import com.nuxvisuals.module.setting.SliderSetting;
import com.nuxvisuals.render.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

// Highlighter: подсвечивает лежащие на земле трапки, исцелки, пласты и т.д. — свой цвет для каждого предмета
public class Highlighter extends Module {
    public static Highlighter INSTANCE;
    private final BoolSetting inventory = add(new BoolSetting("В инвентаре", true));
    private final BoolSetting ground = add(new BoolSetting("На земле", true));
    private record Target(BoolSetting enabled, ColorSetting color, Predicate<ItemStack> test) {
    }

    private final SliderSetting range = add(new SliderSetting("Range", 32, 8, 96, 4, " m"));
    private final BoolSetting fill = add(new BoolSetting("Fill", true));
    private final SliderSetting opacity = add(new SliderSetting("Opacity", 0.3, 0.05, 0.8, 0.05)).visibleIf(fill::get);
    private final BoolSetting outline = add(new BoolSetting("Outline", true));
    private final SliderSetting size = add(new SliderSetting("Box Size", 0.15, 0, 0.5, 0.05));
    private final List<Target> targets = new ArrayList<>();

    public Highlighter() {
        super("highlighter", "Highlighter", Category.Visuals, false);
        INSTANCE = this;
        target("Trap", 0.0f, s -> name(s).contains("трап") || s.isOf(Items.NETHERITE_SCRAP));
        target("Heal", 0.92f, s -> name(s).contains("исцел") || healPotion(s));
        target("Plast", 0.08f, s -> name(s).contains("пласт"));
        target("Disorient", 0.78f, s -> name(s).contains("дезор") || s.isOf(Items.ENDER_EYE));
        target("Dust", 0.15f, s -> name(s).contains("пыль") || s.isOf(Items.SUGAR));
        target("Fire Tornado", 0.03f, s -> name(s).contains("смерч") || s.isOf(Items.FIRE_CHARGE));
        target("Snowball", 0.55f, s -> name(s).contains("снеж") || s.isOf(Items.SNOWBALL));
        target("Aura", 0.13f, s -> name(s).contains("аура") || s.isOf(Items.PHANTOM_MEMBRANE));
        target("Sphere", 0.65f, s -> name(s).contains("сфер") || name(s).contains("шар") || s.isOf(Items.PLAYER_HEAD));
        target("Totem", 0.33f, s -> s.isOf(Items.TOTEM_OF_UNDYING) || name(s).contains("талисман"));
        target("Enchanted Apple", 0.12f, s -> s.isOf(Items.ENCHANTED_GOLDEN_APPLE));
        target("Golden Apple", 0.11f, s -> s.isOf(Items.GOLDEN_APPLE));
        target("Pearl", 0.45f, s -> s.isOf(Items.ENDER_PEARL));
        target("Netherite", 0.0f, s -> netherite(s.getItem()));
    }

    private void target(String name, float hue, Predicate<ItemStack> test) {
        BoolSetting on = add(new BoolSetting(name, true));
        ColorSetting color = add(new ColorSetting(name + " Color", hue, 0.8f).visibleIf(on::get));
        targets.add(new Target(on, color, test));
    }

    private static String name(ItemStack s) {
        return s.getName().getString().toLowerCase(Locale.ROOT);
    }

    private static boolean netherite(Item item) {
        return item == Items.NETHERITE_SWORD || item == Items.NETHERITE_HELMET || item == Items.NETHERITE_CHESTPLATE
                || item == Items.NETHERITE_LEGGINGS || item == Items.NETHERITE_BOOTS || item == Items.NETHERITE_INGOT;
    }

    private static boolean healPotion(ItemStack s) {
        PotionContentsComponent pc = s.get(DataComponentTypes.POTION_CONTENTS);
        if (pc == null) {
            return false;
        }
        for (StatusEffectInstance e : pc.getEffects()) {
            if (e.getEffectType().equals(StatusEffects.INSTANT_HEALTH)) {
                return true;
            }
        }
        return false;
    }

    private int colorFor(ItemStack s) {
        for (Target t : targets) {
            if (t.enabled().get() && t.test().test(s)) {
                return t.color().get();
            }
        }
        return 0;
    }

    public static void drawInventory(net.minecraft.client.gui.DrawContext context,
            net.minecraft.screen.slot.Slot slot, boolean front) {
        Highlighter self = INSTANCE;
        if (self == null || !self.isEnabled() || !self.inventory.get() || !slot.isEnabled()) return;
        ItemStack stack = slot.getStack();
        if (stack.isEmpty()) return;
        int c = self.colorFor(stack);
        if (c == 0) return;
        int x = slot.x, y = slot.y;
        if (!front) {
            // Behind the icon: stack counts and durability bars stay readable.
            context.fill(x, y, x + 16, y + 16, RenderUtil.withAlpha(c, self.opacity.getFloat()));
        } else {
            int border = RenderUtil.withAlpha(c, 0.95f);
            context.fill(x - 1, y - 1, x + 17, y, border);
            context.fill(x - 1, y + 16, x + 17, y + 17, border);
            context.fill(x - 1, y, x, y + 16, border);
            context.fill(x + 16, y, x + 17, y + 16, border);
        }
    }

    @Override
    public void onRender(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
        MinecraftClient client = mc();
        if (!ground.get() || client.world == null || client.player == null) {
            return;
        }
        double maxSq = range.get() * range.get();
        Vec3d cam = camera.getPos();
        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof ItemEntity item) || entity.squaredDistanceTo(client.player) > maxSq) {
                continue;
            }
            int c = colorFor(item.getStack());
            if (c == 0) {
                continue;
            }
            Vec3d lerp = entity.getLerpedPos(tickDelta);
            Box box = entity.getBoundingBox().offset(lerp.subtract(entity.getPos())).expand(size.get());
            drawBox(matrices, consumers, box, cam, c);
        }
    }

    private void drawBox(MatrixStack matrices, VertexConsumerProvider consumers, Box box, Vec3d cam, int c) {
        float x1 = (float) (box.minX - cam.x), y1 = (float) (box.minY - cam.y), z1 = (float) (box.minZ - cam.z);
        float x2 = (float) (box.maxX - cam.x), y2 = (float) (box.maxY - cam.y), z2 = (float) (box.maxZ - cam.z);
        if (fill.get()) {
            VertexConsumer vc = consumers.getBuffer(RenderLayer.getDebugQuads());
            Matrix4f m = matrices.peek().getPositionMatrix();
            int f = RenderUtil.withAlpha(c, opacity.getFloat());
            RenderUtil.quad(vc, m, x1, y1, z1, f, x2, y1, z1, f, x2, y1, z2, f, x1, y1, z2, f);
            RenderUtil.quad(vc, m, x1, y2, z1, f, x1, y2, z2, f, x2, y2, z2, f, x2, y2, z1, f);
            RenderUtil.quad(vc, m, x1, y1, z1, f, x1, y2, z1, f, x2, y2, z1, f, x2, y1, z1, f);
            RenderUtil.quad(vc, m, x1, y1, z2, f, x2, y1, z2, f, x2, y2, z2, f, x1, y2, z2, f);
            RenderUtil.quad(vc, m, x1, y1, z1, f, x1, y1, z2, f, x1, y2, z2, f, x1, y2, z1, f);
            RenderUtil.quad(vc, m, x2, y1, z1, f, x2, y2, z1, f, x2, y2, z2, f, x2, y1, z2, f);
        }
        if (outline.get()) {
            VertexConsumer vc = consumers.getBuffer(RenderLayer.getLines());
            MatrixStack.Entry e = matrices.peek();
            int l = RenderUtil.withAlpha(c, 1f);
            RenderUtil.line(vc, e, x1, y1, z1, x2, y1, z1, l);
            RenderUtil.line(vc, e, x2, y1, z1, x2, y1, z2, l);
            RenderUtil.line(vc, e, x2, y1, z2, x1, y1, z2, l);
            RenderUtil.line(vc, e, x1, y1, z2, x1, y1, z1, l);
            RenderUtil.line(vc, e, x1, y2, z1, x2, y2, z1, l);
            RenderUtil.line(vc, e, x2, y2, z1, x2, y2, z2, l);
            RenderUtil.line(vc, e, x2, y2, z2, x1, y2, z2, l);
            RenderUtil.line(vc, e, x1, y2, z2, x1, y2, z1, l);
            RenderUtil.line(vc, e, x1, y1, z1, x1, y2, z1, l);
            RenderUtil.line(vc, e, x2, y1, z1, x2, y2, z1, l);
            RenderUtil.line(vc, e, x2, y1, z2, x2, y2, z2, l);
            RenderUtil.line(vc, e, x1, y1, z2, x1, y2, z2, l);
        }
    }
}
