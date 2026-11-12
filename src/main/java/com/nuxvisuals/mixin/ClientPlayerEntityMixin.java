package com.nuxvisuals.mixin;

import com.nuxvisuals.module.impl.SlotLock;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// SlotLock: запрет выбрасывания предмета из руки (Q / Ctrl+Q)
@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {
    @Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true, require = 0)
    private void nuxvisuals$slotLockDrop(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
        if (SlotLock.blockDrop()) {
            cir.setReturnValue(false);
        }
    }
}
