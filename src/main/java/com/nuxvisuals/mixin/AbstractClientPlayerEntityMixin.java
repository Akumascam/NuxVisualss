package com.nuxvisuals.mixin;

import com.nuxvisuals.cosmetics.Category;
import com.nuxvisuals.cosmetics.Cosmetics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Когда надет 3D-плащ — прячем обычный плащ (чтобы не было двух плащей)
@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {
    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true, require = 0)
    private void nuxvisuals$hideCape(CallbackInfoReturnable<SkinTextures> cir) {
        try {
            SkinTextures original = cir.getReturnValue();
            if (original == null || original.capeTexture() == null || Cosmetics.equipped(Category.CAPE) == null) {
                return;
            }
            AbstractClientPlayerEntity self = (AbstractClientPlayerEntity) (Object) this;
            MinecraftClient client = MinecraftClient.getInstance();
            if (self != client.player) {
                return;
            }
            cir.setReturnValue(new SkinTextures(original.texture(), original.textureUrl(), null,
                    original.elytraTexture(), original.model(), original.secure()));
        } catch (Exception ignored) {
        }
    }
}
