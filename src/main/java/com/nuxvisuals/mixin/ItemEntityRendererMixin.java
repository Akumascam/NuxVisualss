package com.nuxvisuals.mixin;

import com.nuxvisuals.module.impl.ItemPhysics;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Item Physics: предметы на земле лежат плашмя, а в полёте кувыркаются
@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {
    @Inject(method = "updateRenderState(Lnet/minecraft/entity/ItemEntity;Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;F)V",
            at = @At("TAIL"), require = 0)
    private void nuxvisuals$capture(ItemEntity entity, ItemEntityRenderState state, float tickDelta, CallbackInfo ci) {
        ItemPhysics.capture(entity, tickDelta);
    }

    @Redirect(method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V"),
            require = 0)
    private void nuxvisuals$translate(MatrixStack matrices, float x, float y, float z) {
        if (!ItemPhysics.translate(matrices)) {
            matrices.translate(x, y, z);
        }
    }

    @Redirect(method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;multiply(Lorg/joml/Quaternionf;)V"),
            require = 0)
    private void nuxvisuals$rotate(MatrixStack matrices, Quaternionf rotation) {
        if (!ItemPhysics.rotate(matrices)) {
            matrices.multiply(rotation);
        }
    }
}
