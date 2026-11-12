package com.nuxvisuals.mixin;

import com.nuxvisuals.module.impl.NoRender;
import net.minecraft.client.render.WeatherRendering;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WeatherRendering.class)
public class WeatherRenderingMixin {
    @Inject(method = "renderPrecipitation(Lnet/minecraft/world/World;Lnet/minecraft/client/render/VertexConsumerProvider;IFLnet/minecraft/util/math/Vec3d;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void nux$weather(CallbackInfo ci) {
        if (NoRender.hide(m -> m.weather)) ci.cancel();
    }

    @Inject(method = "addParticlesAndSound", at = @At("HEAD"), cancellable = true, require = 0)
    private void nux$weatherParticles(CallbackInfo ci) {
        if (NoRender.hide(m -> m.weatherParticles)) ci.cancel();
    }
}
