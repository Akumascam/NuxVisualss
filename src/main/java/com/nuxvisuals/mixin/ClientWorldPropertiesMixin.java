package com.nuxvisuals.mixin;

import com.nuxvisuals.module.impl.WorldCustomizer;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.Properties.class)
public class ClientWorldPropertiesMixin {
    @Inject(method = "getTimeOfDay", at = @At("HEAD"), cancellable = true, require = 0)
    private void nuxvisuals$time(CallbackInfoReturnable<Long> cir) {
        Long time = WorldCustomizer.timeOverride();
        if (time != null) {
            cir.setReturnValue(time);
        }
    }
}
