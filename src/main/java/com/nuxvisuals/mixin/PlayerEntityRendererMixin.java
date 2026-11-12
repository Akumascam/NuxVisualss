package com.nuxvisuals.mixin;

import com.nuxvisuals.cosmetics.CosmeticHolder;
import com.nuxvisuals.cosmetics.Cosmetics;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Перед рендером игрока запоминаем, мы ли это, что в руке, скорость и т.д. — для 3D-косметики
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
    @Inject(method = "updateRenderState(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V",
            at = @At("TAIL"))
    private void nuxvisuals$cosmetics(AbstractClientPlayerEntity player, PlayerEntityRenderState state,
                                      float tickDelta, CallbackInfo ci) {
        try {
            var data = ((CosmeticHolder) state).nuxvisuals$data();
            Cosmetics.fill(data, player, tickDelta);
            // значок N у пользователей NuxVisuals (рядом с ником и донатом)
            if (state.displayName != null && com.nuxvisuals.badge.NuxUsers.isNux(player.getUuid(), player.getGameProfile().getName())) {
                state.displayName = com.nuxvisuals.badge.NuxUsers.decorate(state.displayName);
            }
        } catch (Exception ignored) {
        }
    }
}
