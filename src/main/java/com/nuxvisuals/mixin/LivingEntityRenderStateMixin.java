package com.nuxvisuals.mixin;

import com.nuxvisuals.render.HitColorHolder;

import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public class LivingEntityRenderStateMixin implements HitColorHolder {
    @Unique
    private int nuxvisuals$hitTint = -1;

    @Override
    public int nuxvisuals$getHitTint() {
        return this.nuxvisuals$hitTint;
    }

    @Override
    public void nuxvisuals$setHitTint(int tint) {
        this.nuxvisuals$hitTint = tint;
    }
}
