package com.nuxvisuals.mixin;

import com.nuxvisuals.module.impl.WorldCustomizer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// World Customizer: цвет неба и облаков
@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true, require = 0)
    private void nuxvisuals$sky(CallbackInfoReturnable<Object> cir) {
        replace(cir, WorldCustomizer.skyColor());
    }

    @Inject(method = "getCloudsColor", at = @At("RETURN"), cancellable = true, require = 0)
    private void nuxvisuals$clouds(CallbackInfoReturnable<Object> cir) {
        replace(cir, WorldCustomizer.cloudColor());
    }

    // в разных версиях цвет возвращается как int или как Vec3d — поддерживаем оба
    private static void replace(CallbackInfoReturnable<Object> cir, Integer rgb) {
        if (rgb == null) {
            return;
        }
        Object original = cir.getReturnValue();
        if (original instanceof Integer value) {
            cir.setReturnValue((value & 0xFF000000) | (rgb & 0xFFFFFF));
        } else if (original instanceof Vec3d) {
            cir.setReturnValue(new Vec3d(((rgb >> 16) & 255) / 255.0, ((rgb >> 8) & 255) / 255.0, (rgb & 255) / 255.0));
        }
    }
}
