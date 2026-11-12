package com.nuxvisuals.mixin;

import com.nuxvisuals.module.impl.Optimization;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Optimization: дальние сундуки/таблички/баннеры не рисуются
@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin {
    @Inject(
            method = "render(Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void nuxvisuals$cull(BlockEntity blockEntity, float tickDelta, MatrixStack matrices,
                                 VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        double max = Optimization.blockEntityCullSq();
        if (max < 0) {
            return;
        }
        Vec3d cam = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
        if (blockEntity.getPos().getSquaredDistance(cam) > max) {
            ci.cancel();
        }
    }
}
