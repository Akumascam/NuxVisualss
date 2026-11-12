package com.nuxvisuals.mixin;

import com.nuxvisuals.cosmetics.CosmeticHolder;
import com.nuxvisuals.cosmetics.Cosmetics;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 3D-модели (раздел «Модели»): прячем обычное тело игрока, вместо него рисуется модель
@Mixin(PlayerEntityModel.class)
public abstract class PlayerEntityModelMixin {
    @Unique
    private boolean nuxvisuals$hidden;

    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V",
            at = @At("TAIL"), require = 0)
    private void nuxvisuals$hideBody(PlayerEntityRenderState state, CallbackInfo ci) {
        PlayerEntityModel model = (PlayerEntityModel) (Object) this;
        boolean hide = false;
        try {
            hide = ((CosmeticHolder) (Object) state).nuxvisuals$data().me
                    && !((CosmeticHolder) (Object) state).nuxvisuals$data().invisible
                    && Cosmetics.hidesBody();
        } catch (Exception ignored) {
        }
        if (hide) {
            model.head.visible = false;
            model.body.visible = false;
            model.rightArm.visible = false;
            model.leftArm.visible = false;
            model.rightLeg.visible = false;
            model.leftLeg.visible = false;
            nuxvisuals$hidden = true;
        } else if (nuxvisuals$hidden) {
            // возвращаем, только если прятали мы сами
            model.head.visible = true;
            model.body.visible = true;
            model.rightArm.visible = true;
            model.leftArm.visible = true;
            model.rightLeg.visible = true;
            model.leftLeg.visible = true;
            nuxvisuals$hidden = false;
        }
    }
}
