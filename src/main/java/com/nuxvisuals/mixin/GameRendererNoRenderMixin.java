package com.nuxvisuals.mixin;

import com.nuxvisuals.module.impl.NoRender;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererNoRenderMixin {
    @Inject(method = "showFloatingItem", at = @At("HEAD"), cancellable = true, require = 0)
    private void nux$totem(ItemStack stack, CallbackInfo ci) {
        if (stack != null && stack.isOf(Items.TOTEM_OF_UNDYING) && NoRender.hide(m -> m.totem)) ci.cancel();
    }
}
