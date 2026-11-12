package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.ColorSetting;
import com.nuxvisuals.module.setting.SliderSetting;
import com.nuxvisuals.render.RenderUtil;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;

// Цветная подсветка блока, на который вы смотрите
public class BlockOverlay extends Module {
    private final ColorSetting color = add(new ColorSetting("Color", 0.62f, 0.6f));
    private final BoolSetting rainbow = add(new BoolSetting("Rainbow", false));
    private final BoolSetting fill = add(new BoolSetting("Fill", true));
    private final SliderSetting opacity = add(new SliderSetting("Opacity", 0.25, 0.05, 0.8, 0.05)).visibleIf(fill::get);
    private final BoolSetting outline = add(new BoolSetting("Outline", true));

    public BlockOverlay() {
        super("block-overlay", "Block Overlay", Category.Visuals, false);
    }

    @Override
    public void onRender(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
        MinecraftClient client = mc();
        if (client.world == null || !(client.crosshairTarget instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        BlockState state = client.world.getBlockState(pos);
        VoxelShape shape = state.getOutlineShape(client.world, pos);
        if (shape.isEmpty()) {
            return;
        }
        Vec3d cam = camera.getPos();
        Box box = shape.getBoundingBox().offset(pos).expand(0.002);
        float x1 = (float) (box.minX - cam.x), y1 = (float) (box.minY - cam.y), z1 = (float) (box.minZ - cam.z);
        float x2 = (float) (box.maxX - cam.x), y2 = (float) (box.maxY - cam.y), z2 = (float) (box.maxZ - cam.z);
        int c = color(color, rainbow, 0);

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
