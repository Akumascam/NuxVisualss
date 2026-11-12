package com.nuxvisuals.mixin;

import com.nuxvisuals.module.impl.ChatHelper;
import com.nuxvisuals.module.impl.NoRender;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudNoRenderMixin {
    @Inject(method = "renderOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private void nux$overlay(DrawContext context, Identifier texture, float opacity, CallbackInfo ci) {
        String path = texture.getPath();
        if ((path.contains("pumpkin") && NoRender.hide(m -> m.pumpkin))
                || (path.contains("powder_snow") && NoRender.hide(m -> m.powderSnow))) {
            ci.cancel();
        }
    }

    @Inject(method = "renderVignetteOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private void nux$vignette(CallbackInfo ci) {
        if (NoRender.hide(m -> m.vignette)) ci.cancel();
    }

    @Inject(method = {"renderPortalOverlay", "renderNauseaOverlay"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void nux$portal(CallbackInfo ci) {
        if (NoRender.hide(m -> m.portal)) ci.cancel();
    }

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void nux$scoreboard(CallbackInfo ci) {
        if (NoRender.hide(m -> m.scoreboard)) ci.cancel();
    }

    @Inject(method = "renderTitleAndSubtitle", at = @At("HEAD"), cancellable = true, require = 0)
    private void nux$titles(CallbackInfo ci) {
        if (NoRender.hide(m -> m.titles)) ci.cancel();
    }

    @Inject(method = "renderOverlayMessage", at = @At("HEAD"), cancellable = true, require = 0)
    private void nux$actionBar(CallbackInfo ci) {
        if (NoRender.hide(m -> m.actionBar)) ci.cancel();
    }

    // ChatHelper: чат не стирается при выходе с сервера / переходе между серверами (F3+D по-прежнему чистит)
    @Redirect(method = "clear", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud;clear(Z)V"), require = 0)
    private void nux$keepChat(ChatHud chat, boolean clearHistory) {
        if (!ChatHelper.keepChat()) {
            chat.clear(clearHistory);
        }
    }
}
