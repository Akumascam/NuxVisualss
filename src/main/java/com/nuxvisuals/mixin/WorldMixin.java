package com.nuxvisuals.mixin;

import com.nuxvisuals.module.impl.WorldCustomizer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public abstract class WorldMixin {
    @Inject(method = "getRainGradient", at = @At("HEAD"), cancellable = true, require = 0)
    private void nuxvisuals$rain(float delta, CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof ClientWorld) {
            Float value = WorldCustomizer.rainOverride();
            if (value != null) {
                cir.setReturnValue(value);
            }
        }
    }

    @Inject(method = "getThunderGradient", at = @At("HEAD"), cancellable = true, require = 0)
    private void nuxvisuals$thunder(float delta, CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof ClientWorld) {
            Float value = WorldCustomizer.thunderOverride();
            if (value != null) {
                cir.setReturnValue(value);
            }
        }
    }
}
