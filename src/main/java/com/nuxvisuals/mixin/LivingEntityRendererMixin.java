package com.nuxvisuals.mixin;

import com.nuxvisuals.render.HitColorHolder;

import com.nuxvisuals.module.impl.HitColor;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Hit Color: окрашивает модель цели после крита/удара
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Unique
    private static int nuxvisuals$tint = -1;

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
            at = @At("TAIL"), require = 0)
    private void nuxvisuals$captureTint(LivingEntity entity, LivingEntityRenderState state, float tickDelta, CallbackInfo ci) {
        ((HitColorHolder) state).nuxvisuals$setHitTint(HitColor.tintFor(entity));
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"), require = 0)
    private void nuxvisuals$beginRender(LivingEntityRenderState state, MatrixStack matrices,
                                        VertexConsumerProvider consumers, int light, CallbackInfo ci) {
        nuxvisuals$tint = ((HitColorHolder) state).nuxvisuals$getHitTint();
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("RETURN"), require = 0)
    private void nuxvisuals$endRender(LivingEntityRenderState state, MatrixStack matrices,
                                      VertexConsumerProvider consumers, int light, CallbackInfo ci) {
        nuxvisuals$tint = -1;
    }

    // цвет модели (аргумент color у EntityModel.render)
    @ModifyArg(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"),
            index = 4, require = 0)
    private int nuxvisuals$tintColor(int color) {
        return nuxvisuals$tint == -1 ? color : HitColor.multiply(color, nuxvisuals$tint);
    }

    // убрать красную вспышку урона, пока горит своя подсветка
    @ModifyArg(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"),
            index = 3, require = 0)
    private int nuxvisuals$overlay(int overlay) {
        return nuxvisuals$tint != -1 && HitColor.replaceRed() ? OverlayTexture.DEFAULT_UV : overlay;
    }

    // запасной вариант: вызов через Model.render
    // цвет модели (аргумент color у EntityModel.render)
    @ModifyArg(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/Model;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"),
            index = 4, require = 0)
    private int nuxvisuals$tintColorModel(int color) {
        return nuxvisuals$tint == -1 ? color : HitColor.multiply(color, nuxvisuals$tint);
    }

    // убрать красную вспышку урона, пока горит своя подсветка
    @ModifyArg(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/Model;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"),
            index = 3, require = 0)
    private int nuxvisuals$overlayModel(int overlay) {
        return nuxvisuals$tint != -1 && HitColor.replaceRed() ? OverlayTexture.DEFAULT_UV : overlay;
    }
}
