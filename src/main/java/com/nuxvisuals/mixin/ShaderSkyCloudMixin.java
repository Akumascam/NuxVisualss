package com.nuxvisuals.mixin;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(WorldRenderer.class)
public class ShaderSkyCloudMixin {
 @Inject(method="renderClouds",at=@At("HEAD"),cancellable=true)
 private void nux$clouds(CallbackInfo ci){if(com.nuxvisuals.module.impl.ShaderSky.hideClouds())ci.cancel();}
}
