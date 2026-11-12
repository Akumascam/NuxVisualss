package com.nuxvisuals.mixin;

import com.nuxvisuals.cosmetics.CosmeticData;
import com.nuxvisuals.cosmetics.CosmeticHolder;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

// Храним в состоянии рендера игрока данные для 3D-косметики
@Mixin(PlayerEntityRenderState.class)
public abstract class PlayerEntityRenderStateMixin implements CosmeticHolder {
    @Unique
    private final CosmeticData nuxvisuals$cosmetics = new CosmeticData();

    @Override
    public CosmeticData nuxvisuals$data() {
        return nuxvisuals$cosmetics;
    }
}
