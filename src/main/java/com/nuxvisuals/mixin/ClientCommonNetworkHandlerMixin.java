package com.nuxvisuals.mixin;

import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Пинг: ванилла откладывает ответ на KeepAlive, пока окно «заморожено» (перетаскивание, сворачивание,
// долгий кадр) — сервер видит это как скачок пинга. Отвечаем сразу из сетевого потока.
@Mixin(ClientCommonNetworkHandler.class)
public abstract class ClientCommonNetworkHandlerMixin {
    @Inject(method = "onKeepAlive", at = @At("HEAD"), cancellable = true, require = 0)
    private void nuxvisuals$fastKeepAlive(KeepAliveS2CPacket packet, CallbackInfo ci) {
        ((ClientCommonNetworkHandler) (Object) this).sendPacket(new KeepAliveC2SPacket(packet.getId()));
        ci.cancel();
    }
}