package com.nuxvisuals.mixin;

import com.nuxvisuals.module.impl.Zoom;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Колесо мыши во время зума меняет приближение, а не слот хотбара
@Mixin(Mouse.class)
public class MouseMixin {
    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true, require = 0)
    private void nuxvisuals$zoomScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (net.minecraft.client.MinecraftClient.getInstance().currentScreen == null && Zoom.onScroll(vertical)) {
            ci.cancel();
            return;
        }
        if(com.nuxvisuals.module.impl.AutoSwap.onScroll(vertical))ci.cancel();
    }

    // Free Look (запасной путь): движение мыши крутит камеру, а не игрока
    @Redirect(method = "updateMouse", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"), require = 0)
    private void nuxvisuals$freeLook(net.minecraft.client.network.ClientPlayerEntity player, double dx, double dy) {
        if (com.nuxvisuals.module.impl.FreeLook.active()) {
            com.nuxvisuals.module.impl.FreeLook.look(dx, dy);
        } else {
            player.changeLookDirection(dx, dy);
        }
    }
}
