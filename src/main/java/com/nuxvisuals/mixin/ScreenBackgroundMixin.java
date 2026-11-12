package com.nuxvisuals.mixin;

import com.nuxvisuals.ui.MultiplayerTheme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Тема NuxVisuals для экранов сетевой игры: вместо панорамы — фон NuxVisuals
@Mixin(Screen.class)
public abstract class ScreenBackgroundMixin {
    @Inject(method = "renderBackground(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void nuxvisuals$multiplayerTheme(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        if (MultiplayerTheme.applies(self)) {
            MultiplayerTheme.background(context, self);
            ci.cancel();
        }
    }
}