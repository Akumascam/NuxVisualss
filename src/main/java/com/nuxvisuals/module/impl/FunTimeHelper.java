package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.ColorSetting;
import com.nuxvisuals.module.setting.SliderSetting;
import com.nuxvisuals.render.Fx;
import com.nuxvisuals.render.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.Locale;

// Помощник для FunTime: когда держишь в руке трапку, пласт, дезориентацию и т.п.,
// показывает, где появится трапка/пласт и радиус действия предмета. Только отрисовка.
public class FunTimeHelper extends Module {
    private final SliderSetting trapSize = add(new SliderSetting("Trap Size", 5, 3, 9, 1));
    private final SliderSetting trapHeight = add(new SliderSetting("Trap Height", 5, 3, 9, 1));
    private final SliderSetting plastWidth = add(new SliderSetting("Plast Width", 5, 1, 9, 1));
    private final SliderSetting plastHeight = add(new SliderSetting("Plast Height", 3, 1, 9, 1));
    private final SliderSetting disRadius = add(new SliderSetting("Disorientation Radius", 10, 1, 20, 0.5));
    private final SliderSetting dustRadius = add(new SliderSetting("Dust Radius", 10, 1, 20, 0.5));
    private final SliderSetting fireRadius = add(new SliderSetting("Fire Radius", 10, 1, 20, 0.5));
    private final SliderSetting freezeRadius = add(new SliderSetting("Freeze Radius", 7, 1, 20, 0.5));
    private final SliderSetting auraRadius = add(new SliderSetting("Aura Radius", 2, 1, 20, 0.5));
    private final BoolSetting byItem = add(new BoolSetting("Detect By Item", true));
    private final ColorSetting color = add(new ColorSetting("Color", 0.55f, 0.7f));
    private final BoolSetting rainbow = add(new BoolSetting("Rainbow", false));

    private enum Kind { TRAP, PLAST, DISORIENT, DUST, FIRE, FREEZE, AURA }

    public FunTimeHelper() {
        super("funtime", "FunTime Helper", Category.Visuals, false);
    }

    private Kind kind(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        String name = stack.getName().getString().toLowerCase(Locale.ROOT);
        if (name.contains("трап")) return Kind.TRAP;
        if (name.contains("пласт")) return Kind.PLAST;
        if (name.contains("дезориент")) return Kind.DISORIENT;
        if (name.contains("явн") || name.contains("пыль")) return Kind.DUST;
        if (name.contains("смерч")) return Kind.FIRE;
        if (name.contains("заморо")) return Kind.FREEZE;
        if (name.contains("аур")) return Kind.AURA;
        if (!byItem.get()) {
            return null;
        }
        Item item = stack.getItem();
        if (item == Items.NETHERITE_SCRAP) return Kind.TRAP;
        if (item == Items.DRIED_KELP) return Kind.PLAST;
        if (item == Items.ENDER_EYE) return Kind.DISORIENT;
        if (item == Items.SUGAR) return Kind.DUST;
        if (item == Items.FIRE_CHARGE) return Kind.FIRE;
        if (item == Items.PHANTOM_MEMBRANE) return Kind.AURA;
        return null;
    }

    @Override
    public void onRender(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack stack = player.getMainHandStack();
        Kind kind = kind(stack);
        Hand hand = Hand.MAIN_HAND;
        if (kind == null) {
            stack = player.getOffHandStack();
            kind = kind(stack);
            hand = Hand.OFF_HAND;
        }
        if (kind == null) {
            return;
        }
        Vec3d cam = camera.getPos();
        Matrix4f m = matrices.peek().getPositionMatrix();
        VertexConsumer vc = consumers.getBuffer(RenderLayer.getDebugQuads());
        int base = color(color, rainbow, 0f);
        Vec3d feet = player.getLerpedPos(tickDelta);
        BlockPos block = BlockPos.ofFloored(feet);

        switch (kind) {
            case TRAP -> {
                int s = trapSize.getInt();
                int h = trapHeight.getInt();
                int half = s / 2;
                double x1 = block.getX() - half, z1 = block.getZ() - half, y1 = block.getY() - 1;
                box(vc, m, cam, x1, y1, z1, x1 + s, y1 + h, z1 + s, base);
            }
            case PLAST -> {
                int w = plastWidth.getInt();
                int h = plastHeight.getInt();
                float pitch = player.getPitch(tickDelta);
                if (Math.abs(pitch) > 50) {
                    // горизонтальная плита под/над игроком
                    int y = pitch > 0 ? block.getY() - 1 : block.getY() + 2;
                    int half = w / 2;
                    box(vc, m, cam, block.getX() - half, y, block.getZ() - half,
                            block.getX() - half + w, y + 1, block.getZ() - half + w, base);
                } else {
                    Direction f = player.getHorizontalFacing();
                    BlockPos c = block.offset(f, 2);
                    int half = w / 2;
                    double x1, z1, x2, z2;
                    if (f.getAxis() == Direction.Axis.X) {
                        x1 = c.getX();
                        x2 = x1 + 1;
                        z1 = c.getZ() - half;
                        z2 = z1 + w;
                    } else {
                        z1 = c.getZ();
                        z2 = z1 + 1;
                        x1 = c.getX() - half;
                        x2 = x1 + w;
                    }
                    box(vc, m, cam, x1, c.getY(), z1, x2, c.getY() + h, z2, base);
                }
            }
            case FREEZE -> {
                Trajectories.Throw t = new Trajectories.Throw(1.5f, 0.03f, 0.99f, 0.8f, 0f);
                Trajectories.Path path = Trajectories.simulate(player, t, tickDelta);
                if (path.end() != null) {
                    radius(vc, m, path.end().subtract(cam), freezeRadius.getFloat(), base);
                }
            }
            default -> {
                float r = switch (kind) {
                    case DISORIENT -> disRadius.getFloat();
                    case DUST -> dustRadius.getFloat();
                    case FIRE -> fireRadius.getFloat();
                    default -> auraRadius.getFloat();
                };
                radius(vc, m, feet.subtract(cam), r, base);
            }
        }
    }

    // полупрозрачный блок-контур: заливка + светлые рёбра
    private static void box(VertexConsumer vc, Matrix4f m, Vec3d cam, double x1, double y1, double z1,
                            double x2, double y2, double z2, int base) {
        float ax = (float) (x1 - cam.x), ay = (float) (y1 - cam.y), az = (float) (z1 - cam.z);
        float bx = (float) (x2 - cam.x), by = (float) (y2 - cam.y), bz = (float) (z2 - cam.z);
        Fx.box(vc, m, ax, ay, az, bx, by, bz, RenderUtil.withAlpha(base, 0.12f));
        int e = RenderUtil.withAlpha(base, 0.95f);
        float w = 0.04f;
        float[] xs = {ax, bx};
        float[] ys = {ay, by};
        float[] zs = {az, bz};
        for (float y : ys) {
            for (float z : zs) {
                Fx.ribbon(vc, m, ax, y, z, bx, y, z, w, e, e, 0, 0, 0);
            }
        }
        for (float x : xs) {
            for (float z : zs) {
                Fx.ribbon(vc, m, x, ay, z, x, by, z, w, e, e, 0, 0, 0);
            }
        }
        for (float x : xs) {
            for (float y : ys) {
                Fx.ribbon(vc, m, x, y, az, x, y, bz, w, e, e, 0, 0, 0);
            }
        }
    }

    // круг радиуса r на земле + лёгкая «стена» по краю
    private static void radius(VertexConsumer vc, Matrix4f m, Vec3d p, float r, int base) {
        float x = (float) p.x, y = (float) p.y + 0.03f, z = (float) p.z;
        int seg = Math.max(32, Math.round(r * 8));
        int ring = RenderUtil.withAlpha(base, 0.95f);
        int fill = RenderUtil.withAlpha(base, 0.1f);
        int wallBottom = RenderUtil.withAlpha(base, 0.35f);
        int wallTop = base & 0xFFFFFF;
        float t = 0.06f;
        for (int i = 0; i < seg; i++) {
            double a1 = i * Math.PI * 2 / seg;
            double a2 = (i + 1) * Math.PI * 2 / seg;
            float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            float c2 = (float) Math.cos(a2), s2 = (float) Math.sin(a2);
            Fx.quad(vc, m, x, y, z, fill, x + c1 * r, y, z + s1 * r, fill, x + c2 * r, y, z + s2 * r, fill, x, y, z, fill);
            Fx.quad(vc, m, x + c1 * r, y, z + s1 * r, ring, x + c2 * r, y, z + s2 * r, ring,
                    x + c2 * (r - t), y, z + s2 * (r - t), ring, x + c1 * (r - t), y, z + s1 * (r - t), ring);
            Fx.quad(vc, m, x + c1 * r, y, z + s1 * r, wallBottom, x + c2 * r, y, z + s2 * r, wallBottom,
                    x + c2 * r, y + 0.8f, z + s2 * r, wallTop, x + c1 * r, y + 0.8f, z + s1 * r, wallTop);
        }
    }
}
