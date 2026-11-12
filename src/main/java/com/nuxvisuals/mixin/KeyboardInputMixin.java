package com.nuxvisuals.mixin;

import com.nuxvisuals.module.impl.ShiftTap;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {
    @Inject(method = "tick", at = @At("RETURN"))
    private void nux$sneakPulse(CallbackInfo ci) {
        Input self = (Input) (Object) this;
        com.nuxvisuals.ui.AutoSwapWheelScreen.applyMovement(self);
        PlayerInput i = self.playerInput;
        boolean sneak = ShiftTap.sneakInput(i.sneak());
        if (sneak != i.sneak()) {
            self.playerInput = new PlayerInput(i.forward(), i.backward(), i.left(), i.right(),
                    i.jump(), sneak, i.sprint());
        }
    }
}
