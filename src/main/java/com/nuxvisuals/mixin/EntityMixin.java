package com.nuxvisuals.mixin;

import com.nuxvisuals.module.impl.FreeLook;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Free Look: пока зажат обзор, мышь вращает камеру, а не игрока
@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true, require = 0)
    private void nuxvisuals$freeLook(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        if (FreeLook.active() && (Object) this == MinecraftClient.getInstance().player) {
            FreeLook.look(cursorDeltaX, cursorDeltaY);
            ci.cancel();
        }
    }
}
