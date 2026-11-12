package com.nuxvisuals.mixin;

import com.nuxvisuals.cosmetics.Cosmetics;
import com.nuxvisuals.module.impl.CustomHand;
import com.nuxvisuals.module.impl.HandChams;
import com.nuxvisuals.module.impl.SwingAnimation;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    @org.spongepowered.asm.mixin.Unique private boolean nux$matrixPushed;
    @Shadow
    private void applyEquipOffset(MatrixStack matrices, Arm arm, float equipProgress) {
    }

    @Shadow
    private void applySwingOffset(MatrixStack matrices, Arm arm, float swingProgress) {
    }

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void nuxvisuals$handStart(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand,
                                      float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices,
                                      VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        matrices.push();
        nux$matrixPushed=true;
        CustomHand.apply(matrices, player, hand);
        HandChams.firstPerson = true;

    }

    @Inject(method = "renderFirstPersonItem", at = @At("RETURN"))
    private void nuxvisuals$handEnd(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand,
                                    float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices,
                                    VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        HandChams.firstPerson = false;
        if(nux$matrixPushed){matrices.pop();nux$matrixPushed=false;}
    }

    // 3D Hands: подменяем буферы, в которые рисуется рука, на цветную заливку
    @ModifyVariable(method = {"renderArmHoldingItem", "renderArm"}, at = @At("HEAD"), argsOnly = true)
    private VertexConsumerProvider nuxvisuals$chams(VertexConsumerProvider vertexConsumers) {
        return HandChams.wrap(vertexConsumers);
    }

    // 3D Hands: то же для предметов в руках (только от первого лица)
    @ModifyVariable(method = "renderItem", at = @At("HEAD"), argsOnly = true, require = 0)
    private VertexConsumerProvider nuxvisuals$chamsItems(VertexConsumerProvider vertexConsumers) {
        return HandChams.firstPerson ? HandChams.wrapItems(vertexConsumers) : vertexConsumers;
    }

    @ModifyVariable(method = "renderFirstPersonMap", at = @At("HEAD"), argsOnly = true)
    private VertexConsumerProvider nux$mapEffects(VertexConsumerProvider providers) {
        return HandChams.wrapMap(providers);
    }

    // One queue per entire first-person frame (both arms/maps), then flush AFTER vanilla draw.
    @Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At("HEAD"))
    private void nux$effectFrameStart(float tickDelta, MatrixStack matrices,
            VertexConsumerProvider.Immediate providers, net.minecraft.client.network.ClientPlayerEntity player,
            int light, CallbackInfo ci) {
        HandChams.beginFrame();
    }

    @Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At("RETURN"))
    private void nux$effectFrameEnd(float tickDelta, MatrixStack matrices,
            VertexConsumerProvider.Immediate providers, net.minecraft.client.network.ClientPlayerEntity player,
            int light, CallbackInfo ci) {
        HandChams.flush(providers);
        providers.draw(net.minecraft.client.render.RenderLayer.getDebugQuads());
    }

    // Swing Animation: свои режимы взмаха
    @Inject(method = "applySwingOffset", at = @At("HEAD"), cancellable = true, require = 0)
    private void nuxvisuals$swing(MatrixStack matrices, Arm arm, float swingProgress, CallbackInfo ci) {
        if (SwingAnimation.apply(matrices, arm, swingProgress)) {
            ci.cancel();
        }
    }
}
