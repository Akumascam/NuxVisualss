package com.nuxvisuals.mixin;
import net.minecraft.client.render.SkyRendering;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(SkyRendering.class)
public class ShaderSkyRenderingMixin {
 @Inject(method="renderSky(FFF)V",at=@At("HEAD"),cancellable=true)
 private void nux$sky(float red,float green,float blue,CallbackInfo ci){if(com.nuxvisuals.render.ShaderSkyRenderer.render())ci.cancel();}
 @Inject(method="renderGlowingSky",at=@At("HEAD"),cancellable=true)
 private void nux$vanillaGlow(CallbackInfo ci){if(com.nuxvisuals.render.ShaderSkyRenderer.applied())ci.cancel();}
 @Inject(method="renderSkyDark",at=@At("HEAD"),cancellable=true)
 private void nux$lowerSky(CallbackInfo ci){if(com.nuxvisuals.render.ShaderSkyRenderer.applied())ci.cancel();}
 @Inject(method="renderStars",at=@At("HEAD"),cancellable=true)
 private void nux$nativeStars(CallbackInfo ci){if(com.nuxvisuals.render.ShaderSkyRenderer.applied())ci.cancel();}
}
