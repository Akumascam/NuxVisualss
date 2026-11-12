package com.nuxvisuals.mixin;

import com.nuxvisuals.module.impl.ItemPickup;
import com.nuxvisuals.module.impl.TotemTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Totem Tracker: ловим статус 35 (сработал тотем бессмертия)
@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onEntityStatus", at = @At("HEAD"), require = 0)
    private void nuxvisuals$totemPop(EntityStatusS2CPacket packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        // пакет сначала приходит в сетевой поток и перекидывается в главный — обрабатываем только в главном
        if (!client.isOnThread() || client.world == null || packet.getStatus() != 35) {
            return;
        }
        Entity entity = packet.getEntity(client.world);
        if (entity != null) {
            TotemTracker.onTotemPop(entity);
        }
    }

    // ItemPickup: пакет анимации подбора предмета (предмет ещё в мире)
    @Inject(method = "onItemPickupAnimation", at = @At("HEAD"), require = 0)
    private void nuxvisuals$itemPickup(ItemPickupAnimationS2CPacket packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!client.isOnThread() || client.world == null) {
            return;
        }
        ItemPickup.onPickup(packet.getEntityId(), packet.getCollectorEntityId(), packet.getStackAmount());
    }
}
