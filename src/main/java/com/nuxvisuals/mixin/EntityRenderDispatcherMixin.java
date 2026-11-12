package com.nuxvisuals.mixin;

import com.nuxvisuals.module.impl.Optimization;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Optimization: дальние сущности (не игроки) не рисуются
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true, require = 0)
    private void nuxvisuals$cull(Entity entity, Frustum frustum, double x, double y, double z,
                                 CallbackInfoReturnable<Boolean> cir) {
        double max = Optimization.entityCullSq();
        if (max < 0 || entity instanceof PlayerEntity || entity == MinecraftClient.getInstance().getCameraEntity()) {
            return;
        }
        if (entity.squaredDistanceTo(x, y, z) > max) {
            cir.setReturnValue(false);
        }
    }
}
