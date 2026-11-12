package com.nuxvisuals.mixin;

import com.nuxvisuals.badge.NuxUsers;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Значок N в табе перед ником (и префиксом доната) у пользователей NuxVisuals
@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {
    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true, require = 0)
    private void nuxvisuals$badge(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        if (entry != null && entry.getProfile() != null
                && NuxUsers.isNux(entry.getProfile().getId(), entry.getProfile().getName())) {
            cir.setReturnValue(NuxUsers.decorate(cir.getReturnValue()));
        }
    }
}
