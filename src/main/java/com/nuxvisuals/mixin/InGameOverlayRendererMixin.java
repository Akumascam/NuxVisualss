package com.nuxvisuals.mixin;

import com.nuxvisuals.module.impl.NoRender;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// NoRender: огонь, вода и блок в голове на экране
@Mixin(InGameOverlayRenderer.class)
public class InGameOverlayRendererMixin {
    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private static void nuxvisuals$noFire(CallbackInfo ci) {
        if (NoRender.hide(m -> m.fire)) ci.cancel();
    }

    @Inject(method = "renderUnderwaterOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private static void nuxvisuals$noWater(CallbackInfo ci) {
        if (NoRender.hide(m -> m.water)) ci.cancel();
    }

    @Inject(method = "renderInWallOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private static void nuxvisuals$noInWall(CallbackInfo ci) {
        if (NoRender.hide(m -> m.inWall)) ci.cancel();
    }
}
