package com.nuxvisuals.mixin;

import com.nuxvisuals.module.impl.FreeLook;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Free Look: после того как игра поставила камеру, переставляем её сами —
// поворачиваем на свой угол и отодвигаем назад от глаз игрока (камера летает вокруг тебя)
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Shadow
    protected abstract void setPos(double x, double y, double z);

    @Inject(method = "update", at = @At("TAIL"), require = 0)
    private void nuxvisuals$freeLook(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView,
                                     float tickDelta, CallbackInfo ci) {
        if (!FreeLook.active() || focusedEntity == null) {
            return;
        }
        float yaw = FreeLook.yaw();
        float pitch = FreeLook.pitch();
        setRotation(yaw, pitch);
        Vec3d eye = focusedEntity.getCameraPosVec(tickDelta);
        if (!thirdPerson) {
            setPos(eye.x, eye.y, eye.z);
            return;
        }
        double distance = FreeLook.distance();
        Vec3d look = Vec3d.fromPolar(pitch, yaw);
        Vec3d target = eye.subtract(look.multiply(distance));
        // не залезать камерой в стены
        BlockHitResult hit = area.raycast(new RaycastContext(eye, target, RaycastContext.ShapeType.VISUAL,
                RaycastContext.FluidHandling.NONE, focusedEntity));
        if (hit != null && hit.getType() != HitResult.Type.MISS) {
            double d = Math.max(0.3, hit.getPos().distanceTo(eye) - 0.25);
            target = eye.subtract(look.multiply(d));
        }
        setPos(target.x, target.y, target.z);
    }
}
