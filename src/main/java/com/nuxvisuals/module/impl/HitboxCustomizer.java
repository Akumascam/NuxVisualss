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
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

// Цветные хитбоксы. Отдельно для игроков и мобов. Стены не просвечивает.
public class HitboxCustomizer extends Module {
    private final ColorSetting playerColor = add(new ColorSetting("Player Color", 0.0f, 0.0f));
    private final ColorSetting mobColor = add(new ColorSetting("Mob Color", 0.33f, 0.7f));
    private final BoolSetting rainbow = add(new BoolSetting("Rainbow", false));
    private final BoolSetting fill = add(new BoolSetting("Fill", true));
    private final SliderSetting fillOpacity = add(new SliderSetting("Fill Opacity", 0.15, 0.05, 0.6, 0.05)
            .visibleIf(() -> this.fill.get()));
    private final BoolSetting mobs = add(new BoolSetting("Show Mobs", true));
    private final BoolSetting self = add(new BoolSetting("Show Self (F5)", false));
    private final SliderSetting range = add(new SliderSetting("Range", 32, 8, 96, 4));

    public HitboxCustomizer() {
        super("hitbox", "Hitbox Customizer", Category.Visuals, false);
    }

    private int colorOf(Entity entity) {
        ColorSetting c = entity instanceof PlayerEntity ? playerColor : mobColor;
        return color(c, rainbow, entity.getId() * 0.13f);
    }

    @Override
    public void onRender(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
        MinecraftClient client = mc();
        if (client.world == null || client.player == null) {
            return;
        }
        Vec3d cam = camera.getPos();
        double maxDist = range.get() * range.get();

        List<Entity> targets = new ArrayList<>();
        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof LivingEntity) || entity.isInvisible()) {
                continue;
            }
            if (entity == client.player && (!self.get() || client.options.getPerspective().isFirstPerson())) {
                continue;
            }
            if (!(entity instanceof PlayerEntity) && !mobs.get()) {
                continue;
            }
            if (entity.squaredDistanceTo(cam) > maxDist) {
                continue;
            }
            targets.add(entity);
        }
        if (targets.isEmpty()) {
            return;
        }

        if (fill.get()) {
            VertexConsumer quads = consumers.getBuffer(RenderLayer.getDebugQuads());
            Matrix4f m = matrices.peek().getPositionMatrix();
            for (Entity entity : targets) {
                fillBox(quads, m, box(entity, cam, tickDelta), RenderUtil.withAlpha(colorOf(entity), fillOpacity.getFloat()));
            }
        }

        VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());
        MatrixStack.Entry entry = matrices.peek();
        for (Entity entity : targets) {
            lineBox(lines, entry, box(entity, cam, tickDelta), colorOf(entity));
        }
    }

    private static Box box(Entity entity, Vec3d cam, float tickDelta) {
        Vec3d lerped = entity.getLerpedPos(tickDelta);
        return entity.getBoundingBox()
                .offset(lerped.subtract(entity.getPos()))
                .offset(-cam.x, -cam.y, -cam.z);
    }

    private static void lineBox(VertexConsumer vc, MatrixStack.Entry e, Box b, int c) {
        float x1 = (float) b.minX, y1 = (float) b.minY, z1 = (float) b.minZ;
        float x2 = (float) b.maxX, y2 = (float) b.maxY, z2 = (float) b.maxZ;
        RenderUtil.line(vc, e, x1, y1, z1, x2, y1, z1, c);
        RenderUtil.line(vc, e, x2, y1, z1, x2, y1, z2, c);
        RenderUtil.line(vc, e, x2, y1, z2, x1, y1, z2, c);
        RenderUtil.line(vc, e, x1, y1, z2, x1, y1, z1, c);
        RenderUtil.line(vc, e, x1, y2, z1, x2, y2, z1, c);
        RenderUtil.line(vc, e, x2, y2, z1, x2, y2, z2, c);
        RenderUtil.line(vc, e, x2, y2, z2, x1, y2, z2, c);
        RenderUtil.line(vc, e, x1, y2, z2, x1, y2, z1, c);
        RenderUtil.line(vc, e, x1, y1, z1, x1, y2, z1, c);
        RenderUtil.line(vc, e, x2, y1, z1, x2, y2, z1, c);
        RenderUtil.line(vc, e, x2, y1, z2, x2, y2, z2, c);
        RenderUtil.line(vc, e, x1, y1, z2, x1, y2, z2, c);
    }

    private static void fillBox(VertexConsumer vc, Matrix4f m, Box b, int c) {
        float x1 = (float) b.minX, y1 = (float) b.minY, z1 = (float) b.minZ;
        float x2 = (float) b.maxX, y2 = (float) b.maxY, z2 = (float) b.maxZ;
        RenderUtil.quad(vc, m, x1, y1, z1, c, x2, y1, z1, c, x2, y1, z2, c, x1, y1, z2, c);
        RenderUtil.quad(vc, m, x1, y2, z1, c, x1, y2, z2, c, x2, y2, z2, c, x2, y2, z1, c);
        RenderUtil.quad(vc, m, x1, y1, z1, c, x1, y2, z1, c, x2, y2, z1, c, x2, y1, z1, c);
        RenderUtil.quad(vc, m, x1, y1, z2, c, x2, y1, z2, c, x2, y2, z2, c, x1, y2, z2, c);
        RenderUtil.quad(vc, m, x1, y1, z1, c, x1, y1, z2, c, x1, y2, z2, c, x1, y2, z1, c);
        RenderUtil.quad(vc, m, x2, y1, z1, c, x2, y2, z1, c, x2, y2, z2, c, x2, y1, z2, c);
    }
}
