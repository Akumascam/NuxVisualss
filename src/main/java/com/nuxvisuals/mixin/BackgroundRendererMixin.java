package com.nuxvisuals.mixin;

import com.nuxvisuals.module.impl.WorldCustomizer;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Fog;
import org.joml.Vector4f;
import com.nuxvisuals.module.impl.NoRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BackgroundRenderer.class)
public class BackgroundRendererMixin {
    // дальность и цвет тумана
    @Inject(method = "applyFog", at = @At("RETURN"), cancellable = true, require = 0)
    private static void nuxvisuals$fog(CallbackInfoReturnable<Fog> cir) {
        Fog fog = cir.getReturnValue();
        float[] distance = WorldCustomizer.fogDistance(fog.start(), fog.end());
        float[] color = WorldCustomizer.fogColor();
        if (distance == null && color == null) {
            return;
        }
        cir.setReturnValue(new Fog(
                distance != null ? distance[0] : fog.start(),
                distance != null ? distance[1] : fog.end(),
                fog.shape(),
                color != null ? color[0] : fog.red(),
                color != null ? color[1] : fog.green(),
                color != null ? color[2] : fog.blue(),
                fog.alpha()));
    }

    // цвет неба у горизонта, чтобы совпадал с туманом
    @Inject(method = "getFogColor", at = @At("RETURN"), cancellable = true, require = 0)
    private static void nuxvisuals$fogColor(CallbackInfoReturnable<Vector4f> cir) {
        float[] color = WorldCustomizer.fogColor();
        if (color != null) {
            Vector4f original = cir.getReturnValue();
            cir.setReturnValue(new Vector4f(color[0], color[1], color[2], original.w));
        }
    }

    // NoRender: без чёрного тумана слепоты и тьмы
    @Inject(method = "getFogModifier", at = @At("RETURN"), cancellable = true, require = 0)
    private static void nuxvisuals$noBlindFog(CallbackInfoReturnable<Object> cir) {
        if (cir.getReturnValue() != null && NoRender.hide(m -> m.blindness)) {
            cir.setReturnValue(null);
        }
    }
}
