package com.nuxvisuals.mixin;

import com.nuxvisuals.module.impl.PotionHud;
import com.nuxvisuals.module.impl.Crosshair;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Прячет ванильные иконки эффектов, если включён HUD Effects
@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private void nuxvisuals$effects(CallbackInfo ci) {
        if (PotionHud.hidesVanilla()) {
            ci.cancel();
        }
    }

    // Crosshair: свой прицел вместо обычного
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true, require = 0)
    private void nuxvisuals$crosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (Crosshair.draw(context)) {
            ci.cancel();
        }
    }
}
