package com.nuxvisuals.mixin;

import com.nuxvisuals.module.impl.Highlighter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public class HandledScreenHighlighterMixin {
    @Inject(method = "drawSlot", at = @At("HEAD"))
    private void nux$inventoryBack(DrawContext context, Slot slot, CallbackInfo ci) {
        Highlighter.drawInventory(context, slot, false);
    }
    @Inject(method = "drawSlot", at = @At("RETURN"))
    private void nux$inventoryFront(DrawContext context, Slot slot, CallbackInfo ci) {
        Highlighter.drawInventory(context, slot, true);
    }
}
