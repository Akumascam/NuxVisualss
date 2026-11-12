package com.nuxvisuals.mixin;
import com.nuxvisuals.module.impl.SlotCooldowns;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(DrawContext.class)
public class DrawContextCooldownMixin {
    @Inject(method = "drawStackOverlay(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V", at = @At("RETURN"))
    private void nux$slotSeconds(TextRenderer tr, ItemStack stack, int x, int y, String count, CallbackInfo ci) {
        SlotCooldowns.draw((DrawContext) (Object) this, tr, stack, x, y);
    }
}
