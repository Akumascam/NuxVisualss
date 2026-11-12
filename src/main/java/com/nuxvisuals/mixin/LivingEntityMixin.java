package com.nuxvisuals.mixin;

import com.nuxvisuals.module.impl.KillEffect;
import com.nuxvisuals.module.impl.SwingAnimation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Swing Animation: скорость взмаха своей руки; Kill Effect: статус смерти от сервера
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "getHandSwingDuration", at = @At("RETURN"), cancellable = true, require = 0)
    private void nuxvisuals$swingSpeed(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this == MinecraftClient.getInstance().player) {
            int d = SwingAnimation.swingDuration(cir.getReturnValue());
            if (d != cir.getReturnValue()) {
                cir.setReturnValue(d);
            }
        }
    }

    // EntityStatuses.PLAY_DEATH_SOUND_OR_ADD_PROJECTILE_HIT_PARTICLES = 3 — сущность умерла
    @Inject(method = "handleStatus", at = @At("HEAD"), require = 0)
    private void nuxvisuals$deathStatus(byte status, CallbackInfo ci) {
        if (status == 3) {
            KillEffect.onDeathStatus((LivingEntity) (Object) this);
        }
    }
}
