package com.nuxvisuals.mixin;

import com.nuxvisuals.module.impl.AspectRatio;
import com.nuxvisuals.module.impl.Zoom;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Подменяет соотношение сторон в матрице проекции мира (эффект «растянутого» разрешения).
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @ModifyArg(
            method = "getBasicProjectionMatrix",
            at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;perspective(FFFF)Lorg/joml/Matrix4f;"),
            index = 1
    )
    private float nuxvisuals$aspectRatio(float aspect) {
        return AspectRatio.isActive() ? AspectRatio.get() : aspect;
    }

    // Zoom: уменьшаем угол обзора прямо в матрице проекции (тот же вызов, что и для Aspect Ratio)
    @ModifyArg(
            method = "getBasicProjectionMatrix",
            at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;perspective(FFFF)Lorg/joml/Matrix4f;"),
            index = 0
    )
    private float nuxvisuals$zoom(float fov) {
        return Zoom.apply(fov);
    }

    // World-only accumulation: renderWorld returns before the HUD/GUI pass.
    @Inject(method = "renderWorld", at = @At("RETURN"))
    private void nuxvisuals$motionBlur(CallbackInfo ci) {
        com.nuxvisuals.module.impl.MotionBlur.render();
    }

    // Убрать покачивание камеры при получении урона
    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true, require = 0)
    private void nuxvisuals$noHurtCam(CallbackInfo ci) {
        if (com.nuxvisuals.module.impl.NoHurtCam.active()) {
            ci.cancel();
        }
    }
}
